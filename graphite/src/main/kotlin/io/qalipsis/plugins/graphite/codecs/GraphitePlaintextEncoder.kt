package io.qalipsis.plugins.graphite.codecs

import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.MessageToByteEncoder
import io.qalipsis.api.events.Event
import mu.KotlinLogging

/**
 * Implementation of [MessageToByteEncoder] for [graphite][https://github.com/graphite-project] plaintext protocol.
 * Receives a list of [Event], encodes them to pickle format and then sends it to [ChannelHandlerContext] for further processing.
 *
 * @author rklymenko
 */
internal class GraphitePlaintextEncoder : MessageToByteEncoder<List<Event>>() {

    private val SEMICOLON = ";"
    private val EQ = "="

    /**
     * Encodes incoming list of [Event] to [plaintext][https://graphite.readthedocs.io/en/latest/feeding-carbon.html#the-plaintext-protocol]
     * Sends encoded messages one by one to [ChannelHandlerContext]
     */
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

    /**
     * Receives an [Event] and encodes it to [plaintext][https://graphite.readthedocs.io/en/latest/feeding-carbon.html#the-plaintext-protocol]
     */
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