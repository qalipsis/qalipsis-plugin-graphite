package io.qalipsis.plugins.graphite.renderapi

/**
 * @author rklymenko
 */
internal class GraphiteMetricsRequestBuilder(val query: String) {
    private var format = GraphiteRenderFormat.json
    private var from = ""
    private var until = ""
    private var aggregateFunction = GraphiteRenderAggregationFuncName.none

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

    fun aggregateFunction(aggregateFunction: GraphiteRenderAggregationFuncName): GraphiteMetricsRequestBuilder {
        this.aggregateFunction = aggregateFunction
        return this
    }

    //aggregate(host.cpu-[0-7].cpu-{user,system}.value, "sum")
    fun build(): String {
        val queryBuilder = StringBuilder("/render?target=")
        if (aggregateFunction == GraphiteRenderAggregationFuncName.none) {
            queryBuilder.append("$query&format=$format")
        } else {
            queryBuilder.append("aggregate($query,%22${aggregateFunction.name}%22)")
            queryBuilder.append("&format=$format")
        }
        if(from.isNotEmpty()) {
            queryBuilder.append("&from=$from")
        }
        if(until.isNotEmpty()) {
            queryBuilder.append("&until=$until")
        }
        return queryBuilder.toString()
    }
}
data class GraphiteMetricsTime(val amount: Long, val unit: GraphiteMetricsTimeUnit) {
    fun toQueryString() = "-$amount${unit.getGraphiteTimeUnitCode()}"
}
enum class GraphiteMetricsTimeUnit {
    seconds, minutes, hours, days, weeks, months, years;

    fun getGraphiteTimeUnitCode() =
        when(this) {
            seconds -> "s"
            minutes -> "min"
            hours -> "h"
            days -> "d"
            weeks -> "w"
            months -> "m"
            years -> "y"
        }
}
enum class GraphiteRenderFormat {
    json, csv
}
enum class GraphiteRenderAggregationFuncName {
    none, total, sum, min, max
}