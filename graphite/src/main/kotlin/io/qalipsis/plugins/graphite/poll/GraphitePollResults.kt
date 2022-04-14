package io.qalipsis.plugins.influxdb.poll

import io.qalipsis.plugins.graphite.renderapi.model.GraphiteRenderApiJsonResponse


/**
 * Wrapper for the result of poll in InfluxDb.
 *
 * @property results list of InfluxDb records.
 * @property meters of the poll step.
 *
 * @author Alex Averyanov
 */
class GraphitePollResults(
    val results: List<GraphiteRenderApiJsonResponse>,
    val meters: GraphiteQueryMeters
) : Iterable<GraphiteRenderApiJsonResponse> {

    override fun iterator(): Iterator<GraphiteRenderApiJsonResponse> {
        return results.iterator()
    }
}
