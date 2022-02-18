package io.qalipsis.plugins.graphite.events

import io.micronaut.context.annotation.ConfigurationProperties
import io.micronaut.context.annotation.Requires
import io.qalipsis.api.events.EventLevel
import io.qalipsis.plugins.graphite.events.model.GraphiteProtocolType
import java.time.Duration

/**
 * ConfigurationProperties implementation for application properties
 *
 * @author rklymenko
 */
@Requires(property = "metrics.graphite.enabled", value = "true")
@ConfigurationProperties("metrics.graphite")
internal interface GraphiteEventsConfiguration {
    val host: String
    val port: Int
    val protocol: GraphiteProtocolType
    val batchSize: Int
    val batchFlushIntervalSeconds: Duration
    val minLogLevel: EventLevel
    val amountOfClients: Int
}