package io.qalipsis.plugins.graphite.renderapi.service

import com.fasterxml.jackson.databind.ObjectMapper
import io.micronaut.http.HttpStatus
import io.qalipsis.api.events.Event
import io.qalipsis.api.events.EventLevel
import io.qalipsis.plugins.graphite.events.GraphiteEventsConfiguration
import io.qalipsis.plugins.graphite.events.GraphiteEventsPublisher
import io.qalipsis.plugins.graphite.events.model.GraphiteProtocolType
import io.qalipsis.plugins.graphite.renderapi.GraphiteMetricsRequestBuilder
import io.qalipsis.plugins.graphite.renderapi.GraphiteMetricsTime
import io.qalipsis.plugins.graphite.renderapi.GraphiteMetricsTimeSignUnit
import io.qalipsis.plugins.graphite.renderapi.GraphiteMetricsTimeUnit
import io.qalipsis.plugins.graphite.renderapi.GraphiteRenderAggregationFuncName
import io.qalipsis.test.coroutines.TestDispatcherProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import org.junit.Assert
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import org.junit.jupiter.api.extension.RegisterExtension
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.HostPortWaitStrategy
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.util.concurrent.TimeUnit

/**
 * @author rklymenko
 */
@Testcontainers
@Timeout(3, unit = TimeUnit.MINUTES)
class GraphiteRenderApiServiceIntegrationTest {

    @JvmField
    @RegisterExtension
    val testDispatcherProvider = TestDispatcherProvider()

    private lateinit var configuration: GraphiteEventsConfiguration

    private val container = CONTAINER

    private var containerHttpPort = -1

    private val httpClient = createSimpleHttpClient()

    private var protocolPort = -1

    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val objectMapper = ObjectMapper()

    private lateinit var graphiteEventsPublisher: GraphiteEventsPublisher

    companion object {

        const val GRAPHITE_IMAGE_NAME = "graphiteapp/graphite-statsd:latest"
        const val HTTP_PORT = 80
        const val GRAPHITE_PLAINTEXT_PORT = 2003
        const val GRAPHITE_PICKLE_PORT = 2004
        const val LOCALHOST_HOST = "localhost"

        @Container
        @JvmStatic
        private val CONTAINER = GenericContainer<Nothing>(
            DockerImageName.parse(GRAPHITE_IMAGE_NAME)
        ).apply {
            setWaitStrategy(HostPortWaitStrategy())
            withExposedPorts(HTTP_PORT, GRAPHITE_PLAINTEXT_PORT, GRAPHITE_PICKLE_PORT)
            withAccessToHost(true)
            withStartupTimeout(Duration.ofSeconds(60))
            withCreateContainerCmdModifier { it.hostConfig!!.withMemory((512 * 1e20).toLong()).withCpuCount(2) }
        }
    }

    @BeforeAll
    fun setUp() {
        containerHttpPort = container.getMappedPort(HTTP_PORT)
        val request = generateHttpGet("http://$LOCALHOST_HOST:${containerHttpPort}/render")
        while(httpClient.send(request, HttpResponse.BodyHandlers.ofString()).statusCode() != HttpStatus.OK.code) {
            Thread.sleep(1_000)
        }

        protocolPort = container.getMappedPort(GRAPHITE_PICKLE_PORT)
        configuration = object: GraphiteEventsConfiguration{
            override val host: String
                get() = "$LOCALHOST_HOST"
            override val port: Int
                get() = protocolPort
            override val httpPort: Int
                get() = containerHttpPort
            override val protocol: GraphiteProtocolType
                get() = GraphiteProtocolType.pickle
            override val batchSize: Int
                get() = 1
            override val batchFlushIntervalSeconds: Duration
                get() = Duration.ofSeconds(1)
            override val minLogLevel: EventLevel
                get() = EventLevel.INFO
            override val amountOfClients: Int
                get() = 2
        }

        graphiteEventsPublisher = GraphiteEventsPublisher(
            coroutineScope,
            configuration
        )
        graphiteEventsPublisher.start()
    }

    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    fun `should read messages by key`() = testDispatcherProvider.runTest {
        //given
        val graphiteRenderApi = GraphiteRenderApiService(configuration, objectMapper)
        graphiteRenderApi.setUp()
        val messageKey = "exact.key.1"

        //when
        graphiteEventsPublisher.publish(Event(messageKey, EventLevel.INFO, value = 123))
        Thread.sleep(5_000)

        //then
        val requestBuilder = GraphiteMetricsRequestBuilder(messageKey)
        while(graphiteRenderApi.queryObject(requestBuilder).size != 1) {
            Thread.sleep(200)
        }
    }

    @Test
    @Timeout(value = 20, unit = TimeUnit.SECONDS)
    fun `should read messages by key and regex`() = testDispatcherProvider.runTest {
        //given
        val graphiteRenderApi = GraphiteRenderApiService(configuration, objectMapper)
        graphiteRenderApi.setUp()
        val messageKey = "regex.key."
        val regex = "*"
        val range = (1.. 10)

        //when
        async {
            graphiteEventsPublisher.publish(range.map { Event(messageKey + it, EventLevel.INFO, value = 123) })
        }
        Thread.sleep(5_000)

        //then
        val requestBuilder = GraphiteMetricsRequestBuilder(messageKey + regex)
        while(graphiteRenderApi.queryObject(requestBuilder).size != 10) {
            Thread.sleep(200)
        }
    }

    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    fun `should read message by key and time interval`() = testDispatcherProvider.runTest {
        //given
        val graphiteRenderApi = GraphiteRenderApiService(configuration, objectMapper)
        graphiteRenderApi.setUp()
        val messageKey = "exact.key.interval.1"

        //when
        graphiteEventsPublisher.publish(Event(messageKey, EventLevel.INFO, value = 123))
        Thread.sleep(5_000)

        //then
        val requestBuilder = GraphiteMetricsRequestBuilder(messageKey).from(GraphiteMetricsTime(1, GraphiteMetricsTimeSignUnit.minus, GraphiteMetricsTimeUnit.minutes)).until(GraphiteMetricsTime(0, GraphiteMetricsTimeSignUnit.minus, GraphiteMetricsTimeUnit.minutes))
        while(graphiteRenderApi.queryObject(requestBuilder).size != 1) {
            Thread.sleep(200)
        }
    }

    @Test
    fun `should not read message by key outside of time interval with absolute time parameters`() = testDispatcherProvider.runTest {
        //given
        val graphiteRenderApi = GraphiteRenderApiService(configuration, objectMapper)
        graphiteRenderApi.setUp()
        val messageKey = "exact.key.wrong-interval-abs.1"

        //when
        graphiteEventsPublisher.publish(Event(messageKey, EventLevel.INFO, value = 123))
        Thread.sleep(5_000)

        //then
        val requestBuilder = GraphiteMetricsRequestBuilder(messageKey).from(1).until(2)
        Assert.assertTrue(graphiteRenderApi.queryObject(requestBuilder).isEmpty())
    }

    @Test
    fun `should not read message by key outside of time interval with relative time parameters`() = testDispatcherProvider.runTest {
        //given
        val graphiteRenderApi = GraphiteRenderApiService(configuration, objectMapper)
        graphiteRenderApi.setUp()
        val messageKey = "exact.key.wrong-interval-rel.1"

        //when
        graphiteEventsPublisher.publish(Event(messageKey, EventLevel.INFO, value = 123))
        Thread.sleep(5_000)

        //then
        val requestBuilder = GraphiteMetricsRequestBuilder(messageKey).from(GraphiteMetricsTime(3, GraphiteMetricsTimeSignUnit.minus, GraphiteMetricsTimeUnit.years)).until(GraphiteMetricsTime(2, GraphiteMetricsTimeSignUnit.minus, GraphiteMetricsTimeUnit.years))
        Assert.assertTrue(graphiteRenderApi.queryObject(requestBuilder).isEmpty())
    }

    @Test
    fun `should read total by key`() = testDispatcherProvider.runTest {
        //given
        val graphiteRenderApi = GraphiteRenderApiService(configuration, objectMapper)
        graphiteRenderApi.setUp()
        val messageKey = "exact.key.total.1"

        //when
        graphiteEventsPublisher.publish(Event(messageKey, EventLevel.INFO, value = 123))
        Thread.sleep(5_000)

        //then
        val requestBuilder = GraphiteMetricsRequestBuilder(messageKey).aggregateFunction(
            GraphiteRenderAggregationFuncName.total)
        val result = graphiteRenderApi.queryObject(requestBuilder)
        Assert.assertTrue(result.size == 1)
        Assert.assertEquals("${GraphiteRenderAggregationFuncName.total.name}Series($messageKey)", result[0].target)
        Assert.assertEquals(messageKey, result[0].tags["name"])
        Assert.assertEquals(GraphiteRenderAggregationFuncName.total.name, result[0].tags["aggregatedBy"])
    }

    @Test
    fun `should read sum by key`() = testDispatcherProvider.runTest {
        //given
        val graphiteRenderApi = GraphiteRenderApiService(configuration, objectMapper)
        graphiteRenderApi.setUp()
        val messageKey = "exact.key.sum.1"

        //when
        graphiteEventsPublisher.publish(Event(messageKey, EventLevel.INFO, value = 123))
        Thread.sleep(5_000)

        //then
        val requestBuilder = GraphiteMetricsRequestBuilder(messageKey).aggregateFunction(
            GraphiteRenderAggregationFuncName.sum)
        val result = graphiteRenderApi.queryObject(requestBuilder)
        Assert.assertTrue(result.size == 1)
        Assert.assertEquals("${GraphiteRenderAggregationFuncName.sum.name}Series($messageKey)", result[0].target)
        Assert.assertEquals(messageKey, result[0].tags["name"])
        Assert.assertEquals(GraphiteRenderAggregationFuncName.sum.name, result[0].tags["aggregatedBy"])
    }

    @Test
    fun `should read max by key`() = testDispatcherProvider.runTest {
        //given
        val graphiteRenderApi = GraphiteRenderApiService(configuration, objectMapper)
        graphiteRenderApi.setUp()
        val messageKey = "exact.key.max.1"

        //when
        graphiteEventsPublisher.publish(Event(messageKey, EventLevel.INFO, value = 123))
        Thread.sleep(5_000)

        //then
        val requestBuilder = GraphiteMetricsRequestBuilder(messageKey).aggregateFunction(
            GraphiteRenderAggregationFuncName.max)
        val result = graphiteRenderApi.queryObject(requestBuilder)
        Assert.assertTrue(result.size == 1)
        Assert.assertEquals("${GraphiteRenderAggregationFuncName.max.name}Series($messageKey)", result[0].target)
        Assert.assertEquals(messageKey, result[0].tags["name"])
        Assert.assertEquals(GraphiteRenderAggregationFuncName.max.name, result[0].tags["aggregatedBy"])
    }

    @Test
    fun `should read min by key`() = testDispatcherProvider.runTest {
        //given
        val graphiteRenderApi = GraphiteRenderApiService(configuration, objectMapper)
        graphiteRenderApi.setUp()
        val messageKey = "exact.key.min.1"

        //when
        graphiteEventsPublisher.publish(Event(messageKey, EventLevel.INFO, value = 123))
        Thread.sleep(5_000)

        //then
        val requestBuilder = GraphiteMetricsRequestBuilder(messageKey).aggregateFunction(
            GraphiteRenderAggregationFuncName.min)
        val result = graphiteRenderApi.queryObject(requestBuilder)
        Assert.assertTrue(result.size == 1)
        Assert.assertEquals("${GraphiteRenderAggregationFuncName.min.name}Series($messageKey)", result[0].target)
        Assert.assertEquals(messageKey, result[0].tags["name"])
        Assert.assertEquals(GraphiteRenderAggregationFuncName.min.name, result[0].tags["aggregatedBy"])
    }

    protected fun generateHttpGet(uri: String) =
        HttpRequest.newBuilder()
            .GET()
            .uri(URI.create(uri))
            .build()

    protected fun createSimpleHttpClient() =
        HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .build()
}