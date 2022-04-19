package io.qalipsis.plugins.graphite.poll

import io.qalipsis.plugins.graphite.poll.model.GraphiteQuery
import io.qalipsis.plugins.graphite.render.model.GraphiteMetricsRequestBuilder
import io.qalipsis.plugins.graphite.render.model.GraphiteRenderApiJsonResponse

/**
 * Graphite statement for polling, integrating the ability to be internally modified when a tie-breaker is set.
 *
 * @property tieBreaker - tie breaker instant
 * @author Sandro Mamukelashvili
 */
internal class GraphitePollStatement(
    private val graphiteQuery: GraphiteQuery
) {

    private var tieBreaker: Long? = null

    fun saveTiebreaker(records: List<GraphiteRenderApiJsonResponse>) {
        // We assume that the records are ordered chronologically.
        records.asSequence().flatMap { record -> record.dataPoints }
            .mapNotNull { it.timestamp }
            .sortedByDescending { it }
            .firstOrNull()?.let { latestTimestamp ->
                tieBreaker = latestTimestamp
            }
    }

    fun getNextQuery(): GraphiteMetricsRequestBuilder {
        return if (tieBreaker != null) {
            GraphiteMetricsRequestBuilder(graphiteQuery).from(tieBreaker!!)
        } else {
            GraphiteMetricsRequestBuilder(graphiteQuery)
        }
    }

    fun reset() {
        tieBreaker = null
    }

}
