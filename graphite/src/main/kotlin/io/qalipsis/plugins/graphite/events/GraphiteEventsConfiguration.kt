package io.qalipsis.plugins.graphite.events

import io.micronaut.context.annotation.ConfigurationProperties
import io.micronaut.context.annotation.Requires
import io.qalipsis.api.constraints.PositiveDuration
import io.qalipsis.api.events.EventLevel
import io.qalipsis.plugins.graphite.events.model.GraphiteProtocol
import java.time.Duration
import javax.validation.constraints.Min
import javax.validation.constraints.NotNull

/**
 * ConfigurationProperties implementation for application properties
 *
 * @author rklymenko
 */
@Requires(property = "events.export.graphite.enabled", value = "true")
@ConfigurationProperties("events.export.graphite")
internal interface GraphiteEventsConfiguration {
    @get:NotNull
    val minLevel: EventLevel
    @get:NotNull
    val host: String
    @get:NotNull
    val port: Int
    @get:NotNull
    val protocol: GraphiteProtocol
    @get:Min(1)
    val batchSize: Int
    @get:NotNull
    val lingerPeriod: Duration
    @get:Min(1)
    val publishers: Int
}