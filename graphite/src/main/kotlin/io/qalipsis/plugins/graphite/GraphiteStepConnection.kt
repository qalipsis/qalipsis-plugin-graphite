package io.qalipsis.plugins.graphite

import io.netty.channel.EventLoopGroup
import io.netty.channel.nio.NioEventLoopGroup
import javax.validation.constraints.NotBlank
import javax.validation.constraints.NotNull

/**
 * Interface to establish a connection with Graphite
 */
interface GraphiteStepConnection {
    /**
     * Configures the servers settings.
     */
    fun server(host: String, port: Int)

    /**
     * Configures the basic connection.
     */
    fun basic(workerGroup: EventLoopGroup)
}

class GraphiteStepConnectionImpl : GraphiteStepConnection {

    @field:NotBlank
    var host: String = "localhost"

    @field:NotNull
    var port: Int = 8080

    @field:NotNull
    var workerGroup: EventLoopGroup = NioEventLoopGroup()

    override fun server(host: String, port: Int) {
        this.host = host
        this.port = port
    }

    override fun basic(workerGroup: EventLoopGroup) {
        this.workerGroup = workerGroup
    }
}