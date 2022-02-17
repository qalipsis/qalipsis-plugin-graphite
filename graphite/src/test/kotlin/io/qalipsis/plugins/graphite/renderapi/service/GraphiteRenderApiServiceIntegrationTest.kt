package io.qalipsis.plugins.graphite.renderapi.service

import com.fasterxml.jackson.databind.ObjectMapper
import io.micronaut.http.HttpStatus
import io.qalipsis.api.events.Event
import io.qalipsis.api.events.EventLevel
import io.qalipsis.plugins.graphite.events.GraphiteEventsConfiguration
import io.qalipsis.plugins.graphite.events.GraphiteEventsPublisher
import io.qalipsis.plugins.graphite.events.model.GraphiteProtocolType
import io.qalipsis.plugins.graphite.renderapi.GraphiteMetricsRequestBuilder
import io.qalipsis.test.coroutines.TestDispatcherProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
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
        val protocolName = GraphiteProtocolType.pickle.name
        configuration = object: GraphiteEventsConfiguration{
            override val host: String
                get() = "$LOCALHOST_HOST"
            override val port: Int
                get() = protocolPort
            override val httpPort: Int
                get() = containerHttpPort
            override val protocol: String
                get() = protocolName
            override val batchSize: Int
                get() = 1
            override val batchFlushIntervalSeconds: Duration
                get() = Duration.ofSeconds(1)
            override val minLogLevel: String
                get() = "INFO"
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
        val graphiteRenderApi = GraphiteRenderApiService(configuration, objectMapper)
        graphiteRenderApi.setUp()
        val messageKey = "exact.key.1"
        graphiteEventsPublisher.publish(Event(messageKey, EventLevel.INFO, value = 123))
        Thread.sleep(5_000)
        val requestBuilder = GraphiteMetricsRequestBuilder(messageKey)
        while(graphiteRenderApi.queryObject(requestBuilder).size != 1) {
            Thread.sleep(200)
        }
    }

    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    fun `should read messages by key and regex`() = testDispatcherProvider.runTest {
        val graphiteRenderApi = GraphiteRenderApiService(configuration, objectMapper)
        graphiteRenderApi.setUp()
        val messageKey = "regex.key."
        val regex = "*"
        val range = (1.. 10)
        async {
            graphiteEventsPublisher.publish(range.map { Event(messageKey + it, EventLevel.INFO, value = 123) })
        }
        Thread.sleep(5_000)
        val requestBuilder = GraphiteMetricsRequestBuilder(messageKey + regex)
        while(graphiteRenderApi.queryObject(requestBuilder).size != 10) {
            Thread.sleep(200)
        }
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