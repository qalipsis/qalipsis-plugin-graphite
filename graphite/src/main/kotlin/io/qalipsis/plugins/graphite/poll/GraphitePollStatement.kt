package io.qalipsis.plugins.influxdb.poll

import io.qalipsis.plugins.graphite.renderapi.GraphiteMetricsRequestBuilder
import io.qalipsis.plugins.graphite.renderapi.GraphiteMetricsTime
import io.qalipsis.plugins.graphite.renderapi.GraphiteMetricsTimeSignUnit
import io.qalipsis.plugins.graphite.renderapi.GraphiteMetricsTimeUnit
import io.qalipsis.plugins.graphite.renderapi.model.GraphiteRenderApiJsonResponse
import kotlin.math.min

/**
 * InfluxDb statement for polling, integrating the ability to be internally modified when a tie-breaker is set.
 *
 * @property tieBreaker - tie breaker instant
 * @author Sandro Mamukelashvili
 */
internal class GraphitePollStatement(
    private val graphiteTarget: String
) {

    private val defaultSeconds = 60L

    private val maxSecondsToQuery = 60L

    private var tieBreaker: HashMap<String, Long> = hashMapOf()

    fun saveTiebreaker(records: List<GraphiteRenderApiJsonResponse>) {
        if (records.isNotEmpty()) {

            records.forEach { graphiteRecords ->
                val target = graphiteRecords.target
                var maxRecord: Float? = null
                for (datapoint in graphiteRecords.dataPoints) {
                    if (maxRecord == null) {
                        maxRecord = datapoint.getOrNull(1)
                    }
                    else {
                        val timestamp = datapoint.getOrNull(1) ?: continue
                        if (timestamp > maxRecord) {
                            maxRecord = timestamp
                        }
                    }
                }
                if (maxRecord != null) {
                    tieBreaker[target] = maxRecord.toLong()
                }
            }

        }
    }

    fun getNextQuery(): GraphiteMetricsRequestBuilder {

        val currentTimestamp = System.currentTimeMillis() / 1000

        return GraphiteMetricsRequestBuilder(graphiteTarget).from(
            GraphiteMetricsTime(
                // tieBreaker is of type String -> Long
                if (!tieBreaker.containsKey(graphiteTarget)) defaultSeconds else min(currentTimestamp - tieBreaker[graphiteTarget]!!, maxSecondsToQuery),
                GraphiteMetricsTimeSignUnit.minus,
                GraphiteMetricsTimeUnit.seconds
            )
        ).until(
            GraphiteMetricsTime(0, GraphiteMetricsTimeSignUnit.minus, GraphiteMetricsTimeUnit.minutes)
        )

    }
}
