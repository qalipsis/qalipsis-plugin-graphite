package io.qalipsis.plugins.graphite.renderapi.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.qalipsis.plugins.graphite.events.GraphiteEventsConfiguration
import io.qalipsis.plugins.graphite.renderapi.GraphiteMetricsRequestBuilder
import io.qalipsis.plugins.graphite.renderapi.GraphiteRenderFormat
import io.qalipsis.plugins.graphite.renderapi.model.GraphiteRenderApiJsonResponse
import jakarta.inject.Singleton
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import javax.annotation.PostConstruct

/**
 * @author rklymenko
 */
@Singleton
internal class GraphiteRenderApiService(private val graphiteEventsConfiguration: GraphiteEventsConfiguration,
                               private val objectMapper: ObjectMapper) {

    private val httpClient = createSimpleHttpClient()

    private val stringHttpHandler = HttpResponse.BodyHandlers.ofString()

    private lateinit var graphiteUrl: String

    @PostConstruct
    fun setUp() {
        graphiteUrl = constructGraphiteUrl()
    }

    fun queryObject(graphiteMetricsRequestBuilder: GraphiteMetricsRequestBuilder): List<GraphiteRenderApiJsonResponse> {
        graphiteMetricsRequestBuilder.format(GraphiteRenderFormat.json)
        val response = queryCustomFormatAsString(graphiteMetricsRequestBuilder)
        val result = objectMapper.readValue<List<GraphiteRenderApiJsonResponse>>(response)
        return result
    }

    fun queryCustomFormatAsString(graphiteMetricsRequestBuilder: GraphiteMetricsRequestBuilder): String {
        val requestUri = graphiteUrl + graphiteMetricsRequestBuilder.build()
        return httpClient.send(generateHttpGet(requestUri), stringHttpHandler).body()
    }//generateHttpGet("http://localhost:49332/render?target=aggregate(my.statistics.1,%22total%22)")

    private fun constructGraphiteUrl() = "http://${graphiteEventsConfiguration.host}:${graphiteEventsConfiguration.httpPort}"

    private fun generateHttpGet(uri: String) =
        HttpRequest.newBuilder()
            .GET()
            .uri(URI.create(uri))
            .build()

    private fun createSimpleHttpClient() =
        HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .build()
}