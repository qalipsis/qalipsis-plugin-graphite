package io.qalipsis.plugins.graphite.events

import io.micronaut.context.annotation.ConfigurationProperties
import io.micronaut.context.annotation.Requires

/**
 * @author rklymenko
 */
@Requires(property = "metrics.graphite.enabled", value = "true")
@ConfigurationProperties("metrics.graphite")
interface GraphiteEventsConfiguration {
    val host: String
    val port: Int
    val protocol: String
    val batchSize: Int
    val batchFlushIntervalSeconds: Long
    val minLogLevel: String
}