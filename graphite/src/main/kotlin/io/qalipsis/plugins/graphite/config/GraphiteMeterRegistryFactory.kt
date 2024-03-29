/*
 * Copyright 2022 AERIS IT Solutions GmbH
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

package io.qalipsis.plugins.graphite.config

import io.micrometer.core.instrument.Clock
import io.micrometer.core.instrument.util.HierarchicalNameMapper
import io.micrometer.graphite.GraphiteConfig
import io.micrometer.graphite.GraphiteMeterRegistry
import io.micronaut.context.annotation.Factory
import io.micronaut.context.annotation.Requirements
import io.micronaut.context.annotation.Requires
import io.micronaut.context.env.Environment
import io.micronaut.core.naming.conventions.StringConvention
import io.micronaut.core.util.StringUtils
import io.qalipsis.api.config.MetersConfig
import io.qalipsis.api.meters.MeterRegistryConfiguration
import io.qalipsis.api.meters.MeterRegistryFactory
import jakarta.inject.Singleton
import java.time.Duration
import java.util.Properties

/**
 * Configuration for the export of micrometer [io.micrometer.core.instrument.Meter] to Graphite.
 *
 * @author Palina Bril
 */
@Factory
@Requirements(
    Requires(property = MetersConfig.EXPORT_ENABLED, notEquals = StringUtils.FALSE),
    Requires(property = GraphiteMeterRegistryFactory.GRAPHITE_ENABLED, notEquals = StringUtils.FALSE)
)
internal class GraphiteMeterRegistryFactory(environment: Environment) : MeterRegistryFactory {

    private val properties = Properties()

    init {
        properties.putAll(environment.getProperties(MetersConfig.EXPORT_CONFIGURATION, StringConvention.RAW))
        properties.putAll(environment.getProperties(MetersConfig.EXPORT_CONFIGURATION, StringConvention.CAMEL_CASE))
    }

    @Singleton
    fun graphiteRegistry(): GraphiteMeterRegistry {

        return GraphiteMeterRegistry(
            object : GraphiteConfig {
                override fun prefix() = "graphite"
                override fun get(key: String): String? {
                    return properties.getProperty(key)
                }
            },
            Clock.SYSTEM, HierarchicalNameMapper.DEFAULT
        )
    }

    override fun getRegistry(configuration: MeterRegistryConfiguration): GraphiteMeterRegistry {
        return GraphiteMeterRegistry(
            object : GraphiteConfig {
                override fun prefix() = "graphite"
                override fun step(): Duration = configuration.step ?: super.step()
                override fun get(key: String): String? {
                    return properties.getProperty(key)
                }
            },
            Clock.SYSTEM, HierarchicalNameMapper.DEFAULT
        )
    }

    companion object {

        private const val GRAPHITE_CONFIGURATION = "${MetersConfig.EXPORT_CONFIGURATION}.graphite"

        internal const val GRAPHITE_ENABLED = "$GRAPHITE_CONFIGURATION.enabled"
    }
}
