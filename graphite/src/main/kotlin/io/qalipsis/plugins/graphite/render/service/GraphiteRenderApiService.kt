package io.qalipsis.plugins.graphite.render.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.qalipsis.plugins.graphite.render.model.GraphiteMetricsRequestBuilder
import io.qalipsis.plugins.graphite.render.model.GraphiteRenderApiJsonResponse
import io.qalipsis.plugins.graphite.render.model.GraphiteRenderFormat
import jakarta.inject.Singleton

/**
 * TODO The configuration of the service should be simpler and support any root URL (with/without https, path prefix...)
 * Pass the URL as constructor.
 * Path optional username / password in an "Authentication" object in the constructor.
 *
 *  The class GraphiteRenderApiConfiguration is to remove.
 *
 * @author rklymenko
 */
@Singleton
internal class GraphiteRenderApiService(
    private val serverUrl: String,
    private val objectMapper: ObjectMapper,
    private val httpClient: HttpClient,
    private val baseAuth: String? = null
) {

    suspend fun getAsJson(graphiteMetricsRequestBuilder: GraphiteMetricsRequestBuilder): List<GraphiteRenderApiJsonResponse> {
        graphiteMetricsRequestBuilder.format(GraphiteRenderFormat.JSON)
        val response = queryCustomFormatAsString(graphiteMetricsRequestBuilder)
        return objectMapper.readValue(response)
    }

    suspend fun queryCustomFormatAsString(graphiteMetricsRequestBuilder: GraphiteMetricsRequestBuilder): String {
        val requestUri = serverUrl + graphiteMetricsRequestBuilder.build()
        return httpClient.get(requestUri) {
            headers {
                baseAuth?.let { append("Authorization", "Basic $it") }
            }
        }.body()
    }

    fun close() {
        httpClient.close()
    }

}