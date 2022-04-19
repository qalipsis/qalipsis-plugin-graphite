package io.qalipsis.plugins.graphite.render

import io.micronaut.context.annotation.ConfigurationProperties
import io.micronaut.context.annotation.Requires
import javax.validation.constraints.NotBlank
import javax.validation.constraints.Positive

/**
 * ConfigurationProperties implementation for application properties
 *
 * @author rklymenko
 */
@Requires(property = "render.api.export.graphite.enabled", value = "true")
@ConfigurationProperties("render.api.export.graphite")
internal class GraphiteRenderApiConfiguration(
    @get:NotBlank
    val host: String,
    @get:Positive
    val port: Int
)