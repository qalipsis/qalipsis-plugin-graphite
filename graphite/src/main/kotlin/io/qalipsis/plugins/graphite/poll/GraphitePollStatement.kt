package io.qalipsis.plugins.influxdb.poll

import io.qalipsis.plugins.graphite.renderapi.GraphiteMetricsRequestBuilder
import io.qalipsis.plugins.graphite.renderapi.model.GraphiteRenderApiJsonResponse

/**
 * InfluxDb statement for polling, integrating the ability to be internally modified when a tie-breaker is set.
 *
 * @property tieBreaker - tie breaker instant
 * @author Sandro Mamukelashvili
 */
internal class GraphitePollStatement(
    private val query: String
) : PollStatement {

    private var tieBreaker: Map<String, Long> = mapOf()

    override fun saveTiebreaker(records: List<GraphiteRenderApiJsonResponse>) {
        if (records.isNotEmpty()) {

            records.forEach { graphiteRecords ->
                val target = graphiteRecords.target
                var maxRecord: Float? = null
                for (record in graphiteRecords.dataPoints[1])
                    if(record > maxRecord) {
                        maxRecord = record
                    }
            }

        }
    }

    override fun getNextQuery(): GraphiteMetricsRequestBuilder {

    }

    private fun withRange(): String {
        return if (rangeStartStatementRange != null) {
            query.replaceRange(rangeStartStatementRange, "start: $tieBreaker")
        } else {
            "$query |> range(start: $tieBreaker)"
        }
    }

    private fun withValueFilter(): String {
        return if (filterStatementEndRange != null) {
            query.replaceRange(filterStatementEndRange, "(r._value $comparatorClause $tieBreaker and ")
        } else {
            "$query |> filter(fn: (r) => (r._value $comparatorClause $tieBreaker))"
        }
    }

    private fun withPropertyFilter(): String {
        return if (filterStatementEndRange != null) {
            query.replaceRange(filterStatementEndRange, """(r["$tieBreakerField"] $comparatorClause $tieBreaker and """)
        } else {
            """$query |> filter(fn: (r) => (r["$tieBreakerField"] $comparatorClause $tieBreaker))"""
        }
    }

    override fun reset() {
        tieBreaker = null
    }

    private companion object {

        /**
         * Regex to extract the filter statement of a Flux query.
         */
        val FILTER_STATEMENT_REGEX = Regex("\\|> filter([^>]*>\\s*\\()")

        /**
         * Regex to extract the sort statement of a Flux query.
         */
        val SORTING_STATEMENT_REGEX = Regex("\\|> sort\\([^)]*\\)")

        /**
         * Regex to extract the sort fields of a sort function.
         */
        val SORTING_FIELDS_STATEMENT_REGEX = Regex("\"[^\"]+\"")

        /**
         * Regex to extract the sort fields of a sort function.
         */
        val SORTING_DIRECTION_STATEMENT_REGEX = Regex("desc:\\s*(true|false)")

        /**
         * Regex to extract the range statement.
         */
        val RANGE_STATEMENT_REGEX = Regex("\\|> range\\([^)]*\\)")

        /**
         * Regex to extract the start of a range statement.
         */
        val RANGE_START_STATEMENT_REGEX = Regex("start:\\s*[^,)]+")

        /**
         * Name of the field in a record containing the time of the Point.
         */
        const val TIME_FIELD = "_time"

        /**
         * Name of the field in a record containing the value of the Point.
         */
        const val VALUE_FIELD = "_value"
    }
}
