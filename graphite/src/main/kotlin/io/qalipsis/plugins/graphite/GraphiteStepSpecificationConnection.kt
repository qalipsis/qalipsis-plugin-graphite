package io.qalipsis.plugins.graphite

import io.netty.channel.EventLoopGroup
import javax.validation.constraints.NotBlank
import javax.validation.constraints.NotNull

/**
 * Interface to establish a connection with Graphite
 */
interface GraphiteStepSpecificationConnection {
    /**
     * Configures the servers settings.
     */
    fun server(host: String, port: Int)

    /**
     * Configures the basic connection.
     */
    fun workerGroup(workerGroupConfigurer: () -> EventLoopGroup)
}

internal class GraphiteStepSpecificationConnectionImpl : GraphiteStepSpecificationConnection {

    @field:NotBlank
    var host: String = "localhost"

    @field:NotNull
    var port: Int = 8080

    lateinit var workerGroup: () -> EventLoopGroup

    override fun server(host: String, port: Int) {
        this.host = host
        this.port = port
    }

    override fun workerGroup(workerGroupConfigurer: () -> EventLoopGroup) {
        this.workerGroup = workerGroupConfigurer
    }
}