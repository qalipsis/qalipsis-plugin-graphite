package io.qalipsis.plugins.graphite.events.codecs

import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.qalipsis.api.events.Event
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
import mu.KotlinLogging

/**
 * @author rklymenko
 */
internal class GraphiteClientHandler(
    val eventChannel: Channel<List<Event>>,
    val coroutineScope: CoroutineScope
) : ChannelInboundHandlerAdapter() {

    override fun channelActive(ctx: ChannelHandlerContext) {
        coroutineScope.launch {
            while(!ctx.isRemoved) {
                select<Unit> {
                    eventChannel.onReceive { value ->
                        ctx.writeAndFlush(value)
                    }
                }
            }
        }
    }

    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
        log.error { "Graphite error occured: " + cause }
        ctx.close()
    }

    companion object {
        @JvmStatic
        private val log = KotlinLogging.logger {}
    }
}