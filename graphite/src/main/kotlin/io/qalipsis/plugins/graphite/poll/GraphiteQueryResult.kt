package io.qalipsis.plugins.influxdb.poll

import io.qalipsis.plugins.graphite.renderapi.model.GraphiteRenderApiJsonResponse

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
