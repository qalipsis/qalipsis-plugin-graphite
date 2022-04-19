package io.qalipsis.plugins.graphite.poll.model

import io.qalipsis.plugins.graphite.render.model.GraphiteMetricsTime
import io.qalipsis.plugins.graphite.render.model.GraphiteRenderAggregationFuncName

// TODO Add documentation everywhere.
/**
 * Wrapper class to describe structure of a graphite query
 */
data class GraphiteQuery(val target: String) {

    var from = ""

    var noNullPoints = true

    var aggregateFunction = GraphiteRenderAggregationFuncName.NONE

    /**
     * method to specify the time point from which to poll data
     *
     * @param from which specifies the particular time point from which to start a poll
     */
    fun from(from: Long): GraphiteQuery {
        this.from = from.toString()
        return this
    }

    /**
     * method to specify the time from which to poll data
     *
     * @param from which specifies the particular time point from which to start a poll
     */
    fun from(from: GraphiteMetricsTime): GraphiteQuery {
        this.from = from.toQueryString()
        return this
    }

    /**
     * toggles null points
     *
     * @param disableNullPoints a boolean to specify if null points should be enabled or disabled
     */
    fun noNullPoints(disableNullPoints: Boolean): GraphiteQuery {
        this.noNullPoints = disableNullPoints
        return this
    }

    /**
     * intermediate method called on query to perform grouping based on the params entered
     *
     * @param aggregateFunction which is an enum of the type of method to be called
     */
    fun aggregateFunction(aggregateFunction: GraphiteRenderAggregationFuncName): GraphiteQuery {
        this.aggregateFunction = aggregateFunction
        return this
    }
}