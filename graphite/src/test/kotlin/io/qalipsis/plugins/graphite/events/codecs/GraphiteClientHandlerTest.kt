package io.qalipsis.plugins.graphite.events.codecs

import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.netty.channel.ChannelHandlerContext
import io.qalipsis.test.mockk.WithMockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.junit.jupiter.api.Test
import io.mockk.impl.annotations.RelaxedMockK
import io.qalipsis.api.events.Event
import io.qalipsis.api.events.EventLevel
import io.qalipsis.test.mockk.coVerifyExactly
import io.qalipsis.test.mockk.coVerifyNever
import io.qalipsis.test.mockk.coVerifyOnce
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

/**
 * @author rklymenko
 */
@MicronautTest
@WithMockk
internal class GraphiteClientHandlerTest {

    @RelaxedMockK
    private lateinit var ctx: ChannelHandlerContext

    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Test
    fun `should not write to buffer`() {
        //given
        val eventChannel = Channel<List<Event>>()
        val graphiteClientHandler = GraphiteClientHandler(eventChannel, coroutineScope)

        //when
        graphiteClientHandler.channelActive(ctx)
        Thread.sleep(1_000)

        //then
        coVerify {
            ctx.isRemoved
        }
        coVerifyNever {
            ctx.writeAndFlush(any())
        }
        confirmVerified(ctx)
    }

    @Test
    fun `should write to buffer single entity using pickle protocol`() {
        //given
        val eventChannel = Channel<List<Event>>()
        coroutineScope.launch {
            eventChannel.send(listOf(Event("boo", EventLevel.DEBUG)))
        }
        val graphiteClientHandler = GraphiteClientHandler(eventChannel, coroutineScope)

        //when
        graphiteClientHandler.channelActive(ctx)
        Thread.sleep(1_000)

        //then
        coVerify {
            ctx.isRemoved
        }
        coVerifyOnce {
            ctx.writeAndFlush(any())
        }
        confirmVerified(ctx)
    }

    @Test
    fun `should write to buffer multiple entities using pickle protocol`() {
        //given
        val eventChannel = Channel<List<Event>>()
        coroutineScope.launch {
            eventChannel.send(listOf(Event("boo", EventLevel.DEBUG), Event("boo2", EventLevel.DEBUG)))
        }
        val graphiteClientHandler = GraphiteClientHandler(eventChannel, coroutineScope)

        //when
        graphiteClientHandler.channelActive(ctx)
        Thread.sleep(1_000)

        //then
        coVerify {
            ctx.isRemoved
        }
        coVerifyOnce {
            ctx.writeAndFlush(any())
        }
        confirmVerified(ctx)
    }

    @Test
    fun `should write to buffer single entity using plaintext protocol`() {
        //given
        val eventChannel = Channel<List<Event>>()
        coroutineScope.launch {
            eventChannel.send(listOf(Event("boo", EventLevel.DEBUG)))
        }
        val graphiteClientHandler = GraphiteClientHandler(eventChannel, coroutineScope)

        //when
        graphiteClientHandler.channelActive(ctx)
        Thread.sleep(1_000)

        //then
        coVerify {
            ctx.isRemoved
        }
        coVerifyOnce {
            ctx.writeAndFlush(any())
        }
        confirmVerified(ctx)
    }

    @Test
    fun `should write to buffer multiple entities using plaintext protocol`() {
        //given
        val eventChannel = Channel<List<Event>>()
        coroutineScope.launch {
            eventChannel.send(listOf(Event("boo", EventLevel.DEBUG), Event("boo2", EventLevel.DEBUG)))
        }
        val graphiteClientHandler = GraphiteClientHandler(eventChannel, coroutineScope)

        //when
        graphiteClientHandler.channelActive(ctx)
        Thread.sleep(2_000)

        //then
        coVerify {
            ctx.isRemoved
        }
        coVerifyExactly(1) {
            ctx.writeAndFlush(any())
        }
        confirmVerified(ctx)
    }
}