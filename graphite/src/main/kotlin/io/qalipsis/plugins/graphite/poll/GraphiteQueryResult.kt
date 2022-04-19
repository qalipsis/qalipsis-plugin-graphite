package io.qalipsis.plugins.graphite.poll

import io.qalipsis.plugins.graphite.render.model.GraphiteRenderApiJsonResponse

/**
 * A wrapper for meters and documents.
 *
 * @property results result of search query procedure in InfluxDb
 * @property meters meters of the query
 *
 * @author Sandro Mamukelashvili
 */
class GraphiteQueryResult(
    val results: List<GraphiteRenderApiJsonResponse>,
    val meters: GraphiteQueryMeters
)
