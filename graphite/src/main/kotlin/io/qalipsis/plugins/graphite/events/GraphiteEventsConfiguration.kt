package io.qalipsis.plugins.graphite.events

import io.micronaut.context.annotation.ConfigurationProperties
import io.micronaut.context.annotation.Requires
import io.qalipsis.api.events.EventLevel
import io.qalipsis.plugins.graphite.events.model.GraphiteProtocol
import java.time.Duration

/**
 * ConfigurationProperties implementation for application properties
 *
 * @author rklymenko
 */
@Requires(property = "events.export.graphite.enabled", value = "true")
@ConfigurationProperties("events.export.graphite")
internal interface GraphiteEventsConfiguration {
    val minLevel: EventLevel
    val host: String
    val port: Int
    val protocol: GraphiteProtocol
    val batchSize: Int
    val lingerPeriod: Duration
    val publishers: Int
}