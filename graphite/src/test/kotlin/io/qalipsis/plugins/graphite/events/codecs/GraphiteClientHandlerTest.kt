package io.qalipsis.plugins.graphite.events.codecs

import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import io.mockk.confirmVerified
import io.netty.channel.ChannelHandlerContext
import io.qalipsis.plugins.graphite.events.GraphiteEventsConfiguration
import io.qalipsis.plugins.graphite.events.model.EventsBuffer
import io.qalipsis.test.mockk.WithMockk
import jakarta.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.junit.jupiter.api.Test
import io.mockk.impl.annotations.RelaxedMockK
import io.qalipsis.api.events.Event
import io.qalipsis.api.events.EventLevel
import io.qalipsis.plugins.graphite.events.model.GraphiteProtocolType
import io.qalipsis.test.mockk.coVerifyExactly
import io.qalipsis.test.mockk.coVerifyNever
import io.qalipsis.test.mockk.coVerifyOnce
import java.time.Duration

/**
 * @author rklymenko
 */
@MicronautTest
@WithMockk
internal class GraphiteClientHandlerTest {

    @Inject
    private lateinit var configuration: GraphiteEventsConfiguration

    @RelaxedMockK
    private lateinit var ctx: ChannelHandlerContext

    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Test
    fun `should not write to buffer`() {
        //given
        val eventsBuffer = EventsBuffer()
        val graphiteClientHandler = GraphiteClientHandler(eventsBuffer, configuration, coroutineScope)

        //when
        graphiteClientHandler.channelActive(ctx)
        Thread.sleep(1_000)

        //then
        coVerifyNever {
            ctx.writeAndFlush(any())
        }
        confirmVerified(ctx)
    }

    @Test
    fun `should write to buffer single entity using pickle protocol`() {
        //given
        val eventsBuffer = EventsBuffer()
        eventsBuffer.addAll(listOf(Event("boo", EventLevel.DEBUG)))
        val graphiteClientHandler = GraphiteClientHandler(eventsBuffer, configuration, coroutineScope)

        //when
        graphiteClientHandler.channelActive(ctx)
        Thread.sleep(1_000)

        //then
        coVerifyOnce {
            ctx.writeAndFlush(any())
        }
        confirmVerified(ctx)
    }

    @Test
    fun `should write to buffer multiple entities using pickle protocol`() {
        //given
        val eventsBuffer = EventsBuffer()
        eventsBuffer.addAll(listOf(Event("boo", EventLevel.DEBUG), Event("boo2", EventLevel.DEBUG)))
        val graphiteClientHandler = GraphiteClientHandler(eventsBuffer, configuration, coroutineScope)

        //when
        graphiteClientHandler.channelActive(ctx)
        Thread.sleep(1_000)

        //then
        coVerifyOnce {
            ctx.writeAndFlush(any())
        }
        confirmVerified(ctx)
    }

    @Test
    fun `should write to buffer single entity using plaintext protocol`() {
        //given
        val eventsBuffer = EventsBuffer()
        eventsBuffer.addAll(listOf(Event("boo", EventLevel.DEBUG)))
        val configuration = object: GraphiteEventsConfiguration{
            override val host: String
                get() = configuration.host
            override val port: Int
                get() = configuration.port
            override val protocol: String
                get() = GraphiteProtocolType.plaintext.name
            override val batchSize: Int
                get() = 1
            override val batchFlushIntervalSeconds: Duration
                get() = Duration.ofSeconds(1)
            override val minLogLevel: String
                get() = "INFO"
        }
        val graphiteClientHandler = GraphiteClientHandler(eventsBuffer, configuration,
            coroutineScope)

        //when
        graphiteClientHandler.channelActive(ctx)
        Thread.sleep(1_000)

        //then
        coVerifyOnce {
            ctx.writeAndFlush(any())
        }
        confirmVerified(ctx)
    }

    @Test
    fun `should write to buffer multiple entities using plaintext protocol`() {
        //given
        val eventsBuffer = EventsBuffer()
        eventsBuffer.addAll(listOf(Event("boo", EventLevel.DEBUG), Event("boo2", EventLevel.DEBUG)))
        val configuration = object: GraphiteEventsConfiguration{
            override val host: String
                get() = configuration.host
            override val port: Int
                get() = configuration.port
            override val protocol: String
                get() = GraphiteProtocolType.plaintext.name
            override val batchSize: Int
                get() = 1
            override val batchFlushIntervalSeconds: Duration
                get() = Duration.ofSeconds(1)
            override val minLogLevel: String
                get() = "INFO"
        }
        val graphiteClientHandler = GraphiteClientHandler(eventsBuffer, configuration,
            coroutineScope)

        //when
        graphiteClientHandler.channelActive(ctx)
        Thread.sleep(2_000)

        //then
        coVerifyExactly(2) {
            ctx.writeAndFlush(any())
        }
        confirmVerified(ctx)
    }
}