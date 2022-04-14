package io.qalipsis.plugins.influxdb.poll

import io.qalipsis.plugins.graphite.renderapi.model.GraphiteRenderApiJsonResponse


/**
 * Wrapper for the result of poll in Graphite.
 *
 * @property results list of Graphite records.
 * @property meters of the poll step.
 *
 * @author Sandro Mamukelashvili
 */
class GraphitePollResults(
    val results: List<GraphiteRenderApiJsonResponse>,
    val meters: GraphiteQueryMeters
) : Iterable<GraphiteRenderApiJsonResponse> {

    override fun iterator(): Iterator<GraphiteRenderApiJsonResponse> {
        return results.iterator()
    }
}
