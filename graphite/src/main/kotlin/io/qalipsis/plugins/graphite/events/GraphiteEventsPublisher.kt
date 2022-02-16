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
 *
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

    private val eventChannel = Channel<List<Event>>()

    private lateinit var workerGroup: EventLoopGroup

    private lateinit var channelFuture: ChannelFuture

    override fun start() {
        buildClient()
        super.start()
    }

    private fun buildClient() {
        val host = configuration.host
        val port = configuration.port
        val encoder = resolveProtocolEncoder()
        workerGroup = NioEventLoopGroup()

        try {
            val b = Bootstrap()
            b.group(workerGroup)
            b.channel(NioSocketChannel::class.java).option(ChannelOption.SO_KEEPALIVE, true)
            b.handler(object : ChannelInitializer<SocketChannel>() {
                override fun initChannel(ch: SocketChannel) {
                    ch.pipeline().addLast(
                        encoder,
                        GraphiteClientHandler(eventChannel, coroutineScope)
                    )
                }
            }).option(ChannelOption.SO_KEEPALIVE, true)
            channelFuture = b.connect(host, port).sync()
            startUpdateKeepAliveTask()
            log.info { "Graphite connection established. Host: " + configuration.host + ", port: " + configuration.port + ", protocol: " + configuration.protocol }
        } catch (e: Exception) {
            log.warn { "Graphite connection was lost due to: " + e.message }
            throw RuntimeException(e)
        }
    }

    fun startUpdateKeepAliveTask() {
        coroutineScope.launch {
            while(true) {
                Thread.sleep(KEEP_ALIVE_UPDATE_MS)
                if(channelFuture.isSuccess) {
                    channelFuture.channel().writeAndFlush("")
                }
            }
        }
    }

    override fun stop() {
        super.stop()
        eventChannel.close()
        workerGroup?.shutdownGracefully()
    }

    override suspend fun publish(values: List<Event>) {
        eventChannel.send(values)
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
