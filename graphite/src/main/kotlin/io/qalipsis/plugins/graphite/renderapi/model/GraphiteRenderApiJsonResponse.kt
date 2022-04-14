package io.qalipsis.plugins.graphite.renderapi.model

/**
 * @author rklymenko
 */
data class GraphiteRenderApiJsonResponse(
    val target: String, val tags: Map<String, String>, val
    dataPoints: List<List<Float>>
) {
    constructor() : this("", emptyMap(), emptyList())
}