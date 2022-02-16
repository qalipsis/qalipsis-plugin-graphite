package io.qalipsis.plugins.graphite.events.codecs

import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.MessageToByteEncoder
import io.qalipsis.api.events.Event
import mu.KotlinLogging

/**
 * @author rklymenko
 */
internal class GraphitePlaintextEncoder : MessageToByteEncoder<List<Event>>() {

    private val SEMICOLON = ";"
    private val EQ = "="

    override fun encode(
        ctx: ChannelHandlerContext,
        events: List<Event>, out: ByteBuf
    ) {
        log.info { "Encoding events: " + events }
        events.forEach {
            out.writeBytes(generatePayload(it).toByteArray())
        }
        ctx.writeAndFlush(out)
        out.retain()
        log.info { "Events flushed: " + events }
    }

    private fun generatePayload(event: Event): String {
        val payload = StringBuilder()
        payload.append(event.name)
        for (tag in event.tags) {
            payload.append(SEMICOLON)
            payload.append(tag.key)
            payload.append(EQ)
            payload.append(tag.value)
        }
        payload.append(" ")
        payload.append(event.value)
        payload.append(" ")
        payload.append(event.timestamp.toEpochMilli() / 1000)
        payload.append("\n")
        return payload.toString()
    }

    companion object {
        @JvmStatic
        private val log = KotlinLogging.logger {}
    }
}