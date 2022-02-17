package io.qalipsis.plugins.graphite.renderapi.model

/**
 * @author rklymenko
 */
internal data class GraphiteRenderApiJsonResponse(val target: String, val tags: Map<String, String>, val datapoints: List<List<Integer>>) {
    constructor(): this("", emptyMap(), emptyList())
}