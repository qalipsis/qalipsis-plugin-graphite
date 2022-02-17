package io.qalipsis.plugins.graphite.events.codecs

import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.handler.codec.MessageToByteEncoder
import io.qalipsis.api.events.Event
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
import mu.KotlinLogging

/**
 * Implementation of [ChannelInboundHandlerAdapter].
 * GraphiteClientHandler creates a channel listener and reads data from [Channel]
 * then writes it to [ChannelHandlerContext] for further [MessageToByteEncoder] processing.
 *
 * @author rklymenko
 */
internal class GraphiteClientHandler(
    val eventChannel: Channel<List<Event>>,
    val coroutineScope: CoroutineScope
) : ChannelInboundHandlerAdapter() {

    /**
     * Activates a channel to listen and write from [Channel] to [ChannelHandlerContext]
     */
    override fun channelActive(ctx: ChannelHandlerContext) {
        coroutineScope.launch {
            while (!ctx.isRemoved) {
                select<Unit> {
                    eventChannel.onReceive { value ->
                        ctx.writeAndFlush(value)
                    }
                }
            }
        }
    }

    /**
     * Handles and logs exceptions
     */
    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
        log.error { "Graphite error occured: " + cause }
    }

    companion object {
        @JvmStatic
        private val log = KotlinLogging.logger {}
    }
}