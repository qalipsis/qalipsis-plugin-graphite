package io.qalipsis.plugins.graphite.events

import io.micronaut.context.annotation.Requires
import io.netty.bootstrap.Bootstrap
import io.netty.channel.Channel
import io.netty.channel.ChannelInitializer
import io.netty.channel.ChannelOption
import io.netty.channel.EventLoopGroup
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioSocketChannel
import io.qalipsis.api.Executors
import io.qalipsis.api.events.AbstractBufferedEventsPublisher
import io.qalipsis.api.events.Event
import io.qalipsis.plugins.graphite.events.codecs.GraphitePickleEncoder
import io.qalipsis.plugins.graphite.events.codecs.GraphitePlaintextEncoder
import io.qalipsis.plugins.graphite.events.model.GraphiteProtocolType
import jakarta.inject.Named
import jakarta.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mu.KotlinLogging
import java.lang.UnsupportedOperationException
import java.util.UUID

/**
 * Implementation of [AbstractBufferedEventsPublisher] for [graphite][https://github.com/graphite-project].
 * Creates a list of [GraphiteClient] based on configuration from [GraphiteEventsConfiguration].
 * Size may be configured by amountOfClients field.
 *
 * @author rklymenko
 */
@Singleton
@Requires(beans = [GraphiteEventsConfiguration::class])
internal class GraphiteEventsPublisher(
    @Named(Executors.BACKGROUND_EXECUTOR_NAME) private val coroutineScope: CoroutineScope,
    private val configuration: GraphiteEventsConfiguration
) : AbstractBufferedEventsPublisher(
    configuration.minLogLevel,
    configuration.batchFlushIntervalSeconds,
    configuration.batchSize,
    coroutineScope
) {

    private val graphiteClients = MutableList(configuration.amountOfClients) { GraphiteClient() }
    private val graphiteClientsLoadBalancer = GraphiteClientRoundRobinLoadBalancer(graphiteClients)
    private lateinit var workerGroup: EventLoopGroup

    /**
     * Builds clients and starts [AbstractBufferedEventsPublisher].
     */
    override fun start() {
        buildClients()
        super.start()
    }

    /**
     * Stops [AbstractBufferedEventsPublisher].
     * Closes list of [GraphiteClient] and [workerGroup].
     */
    override fun stop() {
        super.stop()
        graphiteClients.forEach {
            it.channel.close()
            log.info { "GraphiteClient $it.id was closed." }
        }
        graphiteClients.clear()
        workerGroup?.shutdownGracefully()
    }

    /**
     * Publishes a list of [Event] to [GraphiteClientRoundRobinLoadBalancer].
     */
    override suspend fun publish(values: List<Event>) {
        graphiteClientsLoadBalancer.send(values)
    }

    /**
     * Builds clients with a configuration from [GraphiteEventsConfiguration].
     */
    private fun buildClients() {
        workerGroup = NioEventLoopGroup(configuration.amountOfClients)
        graphiteClients.forEach {
            buildClient(it)
            startUpdateKeepAliveTask(it)
        }
    }

    /**
     * Builds a client with a configuration from [GraphiteEventsConfiguration].
     * Decides on encoding protocol using [GraphiteEventsConfiguration].
     * Opens tcp channel.
     * Starts [startUpdateKeepAliveTask] for keep-alive updates.
     */
    private fun buildClient(graphiteClient: GraphiteClient) {
        try {
            val encoder = resolveProtocolEncoder()
            val b = Bootstrap()
            b.group(workerGroup)
            b.channel(NioSocketChannel::class.java).option(ChannelOption.SO_KEEPALIVE, true)
            b.handler(object : ChannelInitializer<SocketChannel>() {
                override fun initChannel(ch: SocketChannel) {
                    ch.pipeline().addLast(encoder)
                }
            }).option(ChannelOption.SO_KEEPALIVE, true)
            graphiteClient.channel = b.connect(configuration.host, configuration.port).sync().channel()
            log.info { "Graphite connection established for client ${graphiteClient.id}. Host: " + configuration.host + ", port: " + configuration.port + ", protocol: " + configuration.protocol }
        } catch (e: Exception) {
            graphiteClients.remove(graphiteClient)
            log.error(e) { "Graphite connection for client ${graphiteClient.id} was lost due to: " + e.message }
            throw RuntimeException(e)
        }
    }

    /**
     * Starts keep-alive updates for a given [GraphiteClient].
     */
    private fun startUpdateKeepAliveTask(graphiteClient: GraphiteClient) {
        coroutineScope.launch {
            while(graphiteClient.channel.isOpen) {
                delay(KEEP_ALIVE_UPDATE_MS)
                graphiteClient.channel.writeAndFlush("")
            }
        }
    }

    private fun resolveProtocolEncoder() = when (configuration.protocol) {
        GraphiteProtocolType.plaintext -> GraphitePlaintextEncoder()
        GraphiteProtocolType.pickle -> GraphitePickleEncoder()
    }

    companion object {
        @JvmStatic
        private val log = KotlinLogging.logger {}
        private val KEEP_ALIVE_UPDATE_MS = 10_000L
    }

}

/**
 * An abstraction over [Channel].
 *
 * @author rklymenko
 */
internal class GraphiteClient {
    val id = UUID.randomUUID()
    lateinit var channel: Channel
}

/**
 * An interface for [GraphiteClient] load balancing.
 *
 * @author rklymenko
 */
internal interface GraphiteClientLoadBalancer {
    suspend fun send(events: List<Event>);
}

/**
 * An implementation of [GraphiteClientLoadBalancer]
 * Load balances [Event] requests between list of [GraphiteClient] using round-robin strategy.
 *
 * @author rklymenko
 */
internal class GraphiteClientRoundRobinLoadBalancer(val graphiteClients: List<GraphiteClient>): GraphiteClientLoadBalancer {
    private var nextClientIndx = 0

    override suspend fun send(events: List<Event>) {
        if(graphiteClients.isEmpty()) {
            throw RuntimeException("There's no active graphiteClients to process " + events)
        }
        if(events.isNotEmpty()) {
            graphiteClients[nextClientIndx].channel.writeAndFlush(events)
            nextClientIndx += 1
            if(nextClientIndx == graphiteClients.size) nextClientIndx = 0
        }
    }
}