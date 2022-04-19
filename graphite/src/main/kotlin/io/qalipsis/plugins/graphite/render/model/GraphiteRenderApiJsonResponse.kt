package io.qalipsis.plugins.graphite.render.model

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * @author rklymenko
 */
data class GraphiteRenderApiJsonResponse(
    val target: String,
    val tags: Map<String, String>,
    @JsonProperty("datapoints")
    @JsonFormat(shape = JsonFormat.Shape.ARRAY)
    val dataPoints: List<GraphiteRenderApiJsonPairResponse>
)