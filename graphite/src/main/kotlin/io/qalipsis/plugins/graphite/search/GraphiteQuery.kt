/*
 * Copyright 2024 AERIS IT Solutions GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package io.qalipsis.plugins.graphite.search

import java.time.Instant

/**
 * A request builder for specifying poll request more explicitly.
 *
 * @property targets specifies the targets of metrics to requests, several targets can be separated by a pipe `|`
 * @property from specifies the beginning of the relative or absolute time period to graph
 * @property until specifies the end of the relative or absolute time period to graph
 * @property noNullPoints removes all null datapoints from the series returned
 *
 * @author rklymenko
 */
data class GraphiteQuery internal constructor(
    private val targets: MutableList<String> = mutableListOf(),
    private var from: String = "",
    private var until: String = "",
    private var noNullPoints: String = "True"
) {

    constructor(vararg targets: String) : this() {
        this.targets += targets.flatMap { target -> target.split('|') }
    }

    constructor(tagName: String, tagPattern: String) : this() {
        withTargetFromSeriesByTag(tagName, tagPattern)
    }

    /**
     * Adds a new target to the request.
     */
    fun withTarget(target: String): GraphiteQuery {
        targets += target.split('|')
        return this
    }

    /**
     * Adds a new target defined on a series by tag to the request.
     */
    fun withTargetFromSeriesByTag(tagName: String, tagPattern: String): GraphiteQuery {
        targets += "seriesByTag('$tagName=$tagPattern')"
        return this
    }

    /**
     * Defines the min date of the data to retrieve.
     * The argument [from] should match the specification of the render API of Graphite.
     */
    fun from(from: String): GraphiteQuery {
        this.from = from
        return this
    }

    /**
     * Defines the min date of the data to retrieve.
     */
    fun from(from: Long): GraphiteQuery {
        this.from = from.toString()
        return this
    }

    /**
     * Defines the min date of the data to retrieve.
     */
    fun from(from: Instant): GraphiteQuery {
        require(from > Instant.EPOCH) { "Using the from argument with an Instant is only supported for dates after the epoch" }
        this.from = from.epochSecond.toString()
        return this
    }

    /**
     * Defines the min date of the data to retrieve.
     */
    fun from(from: GraphiteMetricsTime): GraphiteQuery {
        this.from = from.toQueryString()
        return this
    }

    /**
     * Defines the max date of the data to retrieve.
     * The argument [until] should match the specification of the render API of Graphite.
     */
    fun until(until: String): GraphiteQuery {
        this.until = until
        return this
    }

    /**
     * Defines the max date of the data to retrieve.
     */
    fun until(until: Long): GraphiteQuery {
        this.until = until.toString()
        return this
    }

    /**
     * Defines the max date of the data to retrieve.
     */
    fun until(until: Instant): GraphiteQuery {
        require(until > Instant.EPOCH) { "Using the until argument with an Instant is only supported for dates after the epoch" }
        this.until = until.epochSecond.toString()
        return this
    }

    /**
     * Defines the max date of the data to retrieve.
     */
    fun until(until: GraphiteMetricsTime): GraphiteQuery {
        this.until = until.toQueryString()
        return this
    }

    /**
     * Specifies whether the time buckets without values should be filled with null points.
     * When [Boolean::True] is provided, only the existing non-null values are returned.
     */
    fun noNullPoints(disableNullPoints: Boolean): GraphiteQuery {
        this.noNullPoints = if (disableNullPoints) "True" else "False"
        return this
    }

    /**
     * Build a graphite query for retrieval of data points.
     */
    internal fun build(): String {
        val queryBuilder = StringBuilder("?")
        queryBuilder.append(targets.joinToString(separator = "&target=", prefix = "target="))
        queryBuilder.append("&format=json")
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
    val unit: GraphiteMetricsTimeUnit
) {
    fun toQueryString() = "$amount${unit.getGraphiteTimeUnitCode()}"
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
            MONTHS -> "mon"
            YEARS -> "y"
        }
}