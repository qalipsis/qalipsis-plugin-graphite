package io.qalipsis.plugins.influxdb.poll

import io.qalipsis.plugins.graphite.renderapi.GraphiteMetricsRequestBuilder
import io.qalipsis.plugins.graphite.renderapi.model.GraphiteRenderApiJsonResponse


/**
 * statement for polling, integrating the ability to be internally modified when a tie-breaker is set.
 *
 * @author Sandro Mamukelashvili
 */
internal interface PollStatement {

    /**
     * Sort the records, which are not exactly sorted from InfluxDB in some conditions, and saves the tie-breaker for a
     * future call.
     */
    fun saveTiebreaker(records: List<GraphiteRenderApiJsonResponse>)

    /**
     * Changes the query following the first when the tie-breaker is already known
     * By default sort with desc = false (ascending order). If you need descending order - desc should be true.
     */
    fun getNextQuery(): GraphiteMetricsRequestBuilder

    /**
     * Resets the instance into the initial state to be ready for a new poll sequence starting from scratch.
     */
    fun reset()

}