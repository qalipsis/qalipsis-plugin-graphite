package io.qalipsis.plugins.graphite

import io.netty.channel.EventLoopGroup
import javax.validation.constraints.NotBlank

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
    fun basic(protocolType: GraphiteProtocol, workerGroup: EventLoopGroup)

}


class GraphiteStepConnectionImpl : GraphiteStepConnection {

    @field:NotBlank
    var url = "http://127.0.0.1:8086"

    var protocolType: GraphiteProtocol = GraphiteProtocol.PICKLE
    @field:NotBlank
    var host: String = ""
    @field:NotBlank
    var port: Int = 1
    var workerGroup: EventLoopGroup

    override fun server(host: String, port: Int) {
        this.host = host
        this.port = port
    }

    override fun basic(protocolType: GraphiteProtocol, workerGroup: EventLoopGroup) {
        this.protocolType = protocolType
        this.workerGroup = workerGroup
    }
}