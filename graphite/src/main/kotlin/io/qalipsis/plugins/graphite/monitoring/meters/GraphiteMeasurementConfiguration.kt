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

package io.qalipsis.plugins.graphite.monitoring.meters

import io.micronaut.context.annotation.ConfigurationProperties
import io.micronaut.context.annotation.Requires
import io.qalipsis.plugins.graphite.GraphiteProtocol
import io.qalipsis.plugins.graphite.monitoring.meters.GraphiteMeasurementConfiguration.Companion.GRAPHITE_CONFIGURATION
import javax.validation.constraints.NotBlank
import javax.validation.constraints.Positive

/**
 * ConfigurationProperties implementation for application properties.
 *
 * @author Francisca Eze
 */
@Requires(property = "$GRAPHITE_CONFIGURATION.enabled", value = "true")
@ConfigurationProperties(GRAPHITE_CONFIGURATION)
internal interface GraphiteMeasurementConfiguration {

    @get:NotBlank
    val host: String

    @get:Positive
    val port: Int

    val protocol: GraphiteProtocol

    @get:Positive
    val publishers: Int

    @get:Positive
    val batchSize: Int

    @get:NotBlank
    val prefix: String

    companion object {
        const val GRAPHITE_CONFIGURATION = "meters.export.graphite"
    }
}