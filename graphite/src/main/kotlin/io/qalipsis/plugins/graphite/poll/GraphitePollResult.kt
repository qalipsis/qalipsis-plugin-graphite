package io.qalipsis.plugins.graphite.poll

import io.qalipsis.plugins.graphite.render.model.GraphiteRenderApiJsonResponse

/**
 * Wrapper for the result of poll from Graphite.
 *
 * @property results list of Graphite records.
 * @property meters of the poll step.
 *
 * @author Teyyihan Aksu
 */
class GraphitePollResult(
    val results: List<GraphiteRenderApiJsonResponse>,
    val meters: GraphiteQueryMeters
): Iterable<GraphiteRenderApiJsonResponse> {

    override fun iterator(): Iterator<GraphiteRenderApiJsonResponse> {
        return results.iterator()
    }
}
