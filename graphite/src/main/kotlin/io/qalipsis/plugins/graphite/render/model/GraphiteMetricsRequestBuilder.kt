package io.qalipsis.plugins.graphite.render.model

import io.qalipsis.plugins.graphite.poll.model.GraphiteQuery

/**
 * @author rklymenko
 */
internal data class GraphiteMetricsRequestBuilder(
    private val target: String,
    private var from: String = "",
    private var until: String = "",
    private var noNullPoints: String = "True",
    private var aggregateFunction: GraphiteRenderAggregationFuncName = GraphiteRenderAggregationFuncName.NONE,
    private var format: GraphiteRenderFormat = GraphiteRenderFormat.JSON,
) {

    constructor(query: GraphiteQuery) : this(
        target = query.target,
        from = query.from,
        aggregateFunction = query.aggregateFunction
    ) {
        noNullPoints(query.noNullPoints)
    }

    fun format(format: GraphiteRenderFormat): GraphiteMetricsRequestBuilder {
        this.format = format
        return this
    }

    fun from(from: Long): GraphiteMetricsRequestBuilder {
        this.from = from.toString()
        return this
    }

    fun from(from: GraphiteMetricsTime): GraphiteMetricsRequestBuilder {
        this.from = from.toQueryString()
        return this
    }

    fun until(until: Long): GraphiteMetricsRequestBuilder {
        this.until = until.toString()
        return this
    }

    fun until(until: GraphiteMetricsTime): GraphiteMetricsRequestBuilder {
        this.until = until.toQueryString()
        return this
    }

    fun noNullPoints(disableNullPoints: Boolean): GraphiteMetricsRequestBuilder {
        this.noNullPoints = if (disableNullPoints) "True" else "False"
        return this
    }

    fun aggregateFunction(aggregateFunction: GraphiteRenderAggregationFuncName): GraphiteMetricsRequestBuilder {
        this.aggregateFunction = aggregateFunction
        return this
    }

    /**
     * Build a graphite query for retrieval or aggregation like aggregate(host.cpu-[0-7].cpu-{user,system}.value, "sum").
     *
     */
    fun build(): String {
        val queryBuilder = StringBuilder("/render?target=")
        val serializedFormat = format.toString().lowercase()
        if (aggregateFunction == GraphiteRenderAggregationFuncName.NONE) {
            queryBuilder.append(target)
        } else {
            queryBuilder.append("aggregate($target,%22${aggregateFunction.name.lowercase()}%22)")
        }
        queryBuilder.append("&format=$serializedFormat")
        if (from.isNotEmpty()) {
            queryBuilder.append("&from=$from")
        }
        if (until.isNotEmpty()) {
            queryBuilder.append("&until=$until")
        }
        if (noNullPoints.isNotEmpty()) {
            queryBuilder.append("&noNullPoints=$noNullPoints")
        }
        return queryBuilder.toString()
    }
}

/**
 * A wrapper class for specifying poll time more explicitly
 */
data class GraphiteMetricsTime(
    val amount: Long,
    val sign: GraphiteMetricsTimeSignUnit,
    val unit: GraphiteMetricsTimeUnit
) {
    fun toQueryString() = "${sign}$amount${unit.getGraphiteTimeUnitCode()}"
}

enum class GraphiteMetricsTimeSignUnit {
    MINUS, PLUS;

    override fun toString() = if (this == MINUS) "-" else "+"
}

enum class GraphiteMetricsTimeUnit {
    SECONDS, MINUTES, HOURS, DAYS, WEEKS, MONTHS, YEARS;

    fun getGraphiteTimeUnitCode() =
        when (this) {
            SECONDS -> "s"
            MINUTES -> "min"
            HOURS -> "h"
            DAYS -> "d"
            WEEKS -> "w"
            MONTHS -> "m"
            YEARS -> "y"
        }
}

enum class GraphiteRenderFormat {
    JSON, CSV
}

enum class GraphiteRenderAggregationFuncName {
    NONE, TOTAL, SUM, MIN, MAX
}