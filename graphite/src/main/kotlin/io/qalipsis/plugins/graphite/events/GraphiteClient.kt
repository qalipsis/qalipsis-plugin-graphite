package io.qalipsis.plugins.graphite.events

import io.netty.bootstrap.Bootstrap
import io.netty.channel.ChannelFuture
import io.netty.channel.ChannelInitializer
import io.netty.channel.ChannelOption
import io.netty.channel.EventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioSocketChannel
import io.qalipsis.api.events.Event
import io.qalipsis.api.io.Closeable
import io.qalipsis.api.logging.LoggerHelper.logger
import io.qalipsis.api.sync.ImmutableSlot
import io.qalipsis.plugins.graphite.events.codecs.GraphitePickleEncoder
import io.qalipsis.plugins.graphite.events.codecs.GraphitePlaintextEncoder
import io.qalipsis.plugins.graphite.events.model.GraphiteProtocol
import kotlinx.coroutines.runBlocking

/**
 *
 */
internal class GraphiteClient(
    private val protocolType: GraphiteProtocol,
    private val host: String,
    private val port: Int,
    private val workerGroup: EventLoopGroup
) : Closeable {

    private var started = false

    private lateinit var channelFuture: ChannelFuture

    val isOpen: Boolean
        get() = started && channelFuture.channel().isOpen

    suspend fun start(): GraphiteClient {
        started = false
        val readinessLatch = ImmutableSlot<Result<Unit>>()

        val b = Bootstrap()
        b.group(workerGroup)
        b.channel(NioSocketChannel::class.java).option(ChannelOption.SO_KEEPALIVE, true)
        b.handler(object : ChannelInitializer<SocketChannel>() {
            override fun initChannel(ch: SocketChannel) {
                ch.pipeline().addLast(resolveProtocolEncoder())
            }
        }).option(ChannelOption.SO_KEEPALIVE, true)

        channelFuture = b.connect(host, port).addListener {
            runBlocking {
                if (it.isSuccess) {
                    readinessLatch.set(Result.success(Unit))
                } else {
                    readinessLatch.set(Result.failure(it.cause()))
                }
            }
        }
        log.info { "Graphite connection established. Host: $host, port: $port, protocol: $protocolType" }

        readinessLatch.get().getOrThrow()
        started = true
        return this
    }

    suspend fun publish(values: List<Event>) {
        val readinessLatch = ImmutableSlot<Result<Unit>>()
        channelFuture.channel().writeAndFlush(values).addListener {
            runBlocking {
                if (it.isSuccess) {
                    readinessLatch.set(Result.success(Unit))
                } else {
                    readinessLatch.set(Result.failure(it.cause()))
                }
            }
        }
        readinessLatch.get().getOrThrow()
    }

    private fun resolveProtocolEncoder() = when (protocolType) {
        GraphiteProtocol.PLAINTEXT -> GraphitePlaintextEncoder()
        GraphiteProtocol.PICKLE -> GraphitePickleEncoder()
    }

    override suspend fun close() {
        started = false
        channelFuture.channel().closeFuture()
    }

    companion object {

        private val log = logger()
    }

}
