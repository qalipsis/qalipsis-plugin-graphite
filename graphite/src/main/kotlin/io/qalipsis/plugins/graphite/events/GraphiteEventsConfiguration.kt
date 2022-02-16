package io.qalipsis.plugins.graphite.events

import io.micronaut.context.annotation.ConfigurationProperties
import io.micronaut.context.annotation.Requires
import java.time.Duration

/**
 * @author rklymenko
 */
@Requires(property = "metrics.graphite.enabled", value = "true")
@ConfigurationProperties("metrics.graphite")
internal interface GraphiteEventsConfiguration {
    val host: String
    val port: Int
    val protocol: String
    val batchSize: Int
    val batchFlushIntervalSeconds: Duration
    val minLogLevel: String
}