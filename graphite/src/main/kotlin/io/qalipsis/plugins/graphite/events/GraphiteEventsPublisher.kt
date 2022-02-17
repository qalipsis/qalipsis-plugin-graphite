package io.qalipsis.plugins.graphite.events

import io.micronaut.context.annotation.Requires
import io.netty.bootstrap.Bootstrap
import io.netty.channel.ChannelFuture
import io.netty.channel.ChannelInitializer
import io.netty.channel.ChannelOption
import io.netty.channel.EventLoopGroup
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioSocketChannel
import io.qalipsis.api.Executors
import io.qalipsis.api.events.AbstractBufferedEventsPublisher
import io.qalipsis.api.events.Event
import io.qalipsis.api.events.EventLevel
import io.qalipsis.plugins.graphite.events.codecs.GraphiteClientHandler
import io.qalipsis.plugins.graphite.events.codecs.GraphitePickleEncoder
import io.qalipsis.plugins.graphite.events.codecs.GraphitePlaintextEncoder
import io.qalipsis.plugins.graphite.events.model.GraphiteProtocolType
import jakarta.inject.Named
import jakarta.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import mu.KotlinLogging
import java.lang.UnsupportedOperationException

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
    EventLevel.valueOf(configuration.minLogLevel),
    configuration.batchFlushIntervalSeconds,
    configuration.batchSize,
    coroutineScope
) {

    private val graphiteClients = List(configuration.amountOfClients) { GraphiteClient() }
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
            it.eventChannel.close()
        }
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
                    ch.pipeline().addLast(
                        encoder,
                        GraphiteClientHandler(graphiteClient.eventChannel, coroutineScope)
                    )
                }
            }).option(ChannelOption.SO_KEEPALIVE, true)
            graphiteClient.channelFuture = b.connect(configuration.host, configuration.port).sync()
            startUpdateKeepAliveTask(graphiteClient)
            log.info { "Graphite connection established. Host: " + configuration.host + ", port: " + configuration.port + ", protocol: " + configuration.protocol }
        } catch (e: Exception) {
            log.warn { "Graphite connection was lost due to: " + e.message }
            throw RuntimeException(e)
        }
    }

    /**
     * Starts keep-alive updates for a given [GraphiteClient].
     */
    private fun startUpdateKeepAliveTask(graphiteClient: GraphiteClient) {
        coroutineScope.launch {
            while(!graphiteClient.channelFuture.isDone) {
                Thread.sleep(KEEP_ALIVE_UPDATE_MS)
                if(graphiteClient.channelFuture.isSuccess) {
                    graphiteClient.channelFuture.channel().writeAndFlush("")
                }
            }
        }
    }

    private fun resolveProtocolEncoder() = when (configuration.protocol) {
        GraphiteProtocolType.plaintext.name -> GraphitePlaintextEncoder()
        GraphiteProtocolType.pickle.name -> GraphitePickleEncoder()
        else -> throw UnsupportedOperationException("Unknown graphite protocol: " + configuration.protocol)
    }

    companion object {
        @JvmStatic
        private val log = KotlinLogging.logger {}
        private val KEEP_ALIVE_UPDATE_MS = 10_000L
    }

}
internal class GraphiteClient {
    val eventChannel = Channel<List<Event>>()
    lateinit var channelFuture: ChannelFuture
}
internal interface GraphiteClientLoadBalancer {
    suspend fun send(events: List<Event>);
}
internal class GraphiteClientRoundRobinLoadBalancer(val graphiteClients: List<GraphiteClient>): GraphiteClientLoadBalancer {
    private var nextClientIndx = 0

    override suspend fun send(events: List<Event>) {
        graphiteClients[nextClientIndx].eventChannel.send(events)
        nextClientIndx += 1
        if(nextClientIndx == graphiteClients.size) nextClientIndx = 0
    }
}