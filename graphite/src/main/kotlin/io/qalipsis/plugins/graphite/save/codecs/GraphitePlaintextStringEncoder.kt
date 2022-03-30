package io.qalipsis.plugins.graphite.save.codecs

import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.MessageToByteEncoder
import mu.KotlinLogging.logger

/**
 * Implementation of [MessageToByteEncoder] for [graphite][https://github.com/graphite-project] plaintext string protocol.
 * Receives a list of [String], encodes them to pickle format and then sends it to [ChannelHandlerContext] for further processing.
 *
 * @author Palina Bril
 */
internal class GraphitePlaintextStringEncoder : MessageToByteEncoder<List<String>>() {

    /**
     * Encodes incoming list of [String] to [plaintext][https://graphite.readthedocs.io/en/latest/feeding-carbon.html#the-plaintext-protocol]
     * Sends encoded messages one by one to [ChannelHandlerContext]
     */
    override fun encode(ctx: ChannelHandlerContext, messages: List<String>, out: ByteBuf) {
        log.info { "Encoding messages: " + messages }
        messages.forEach {
            out.writeBytes(it.toByteArray())
        }
        ctx.writeAndFlush(out)
        out.retain()
        log.info { "Messages flushed: " + messages }
    }

    companion object {
        private val log = logger {}
    }
}