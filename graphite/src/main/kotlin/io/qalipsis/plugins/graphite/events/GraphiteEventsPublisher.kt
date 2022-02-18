package io.qalipsis.plugins.graphite.events

import io.micronaut.context.annotation.Requires
import io.netty.channel.EventLoopGroup
import io.netty.channel.nio.NioEventLoopGroup
import io.qalipsis.api.Executors
import io.qalipsis.api.events.AbstractBufferedEventsPublisher
import io.qalipsis.api.events.Event
import io.qalipsis.api.pool.FixedPool
import jakarta.inject.Named
import jakarta.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking

/**
 *
 */
@Singleton
@Requires(beans = [GraphiteEventsConfiguration::class])
internal class GraphiteEventsPublisher(
    @Named(Executors.BACKGROUND_EXECUTOR_NAME) private val coroutineScope: CoroutineScope,
    private val configuration: GraphiteEventsConfiguration
) : AbstractBufferedEventsPublisher(
    configuration.minLogLevel,
    configuration.batchFlushIntervalSeconds,
    configuration.batchSize,
    coroutineScope
) {

    private lateinit var workerGroup: EventLoopGroup

    private lateinit var clients: FixedPool<GraphiteClient>

    override fun start() {
        workerGroup = NioEventLoopGroup()
        clients = FixedPool(configuration.amountOfClients,
            checkOnAcquire = true,
            checkOnRelease = true,
            healthCheck = { it.isOpen }) {
            GraphiteClient(
                configuration.protocol,
                configuration.host,
                configuration.port,
                workerGroup
            ).start()
        }
        runBlocking(coroutineScope.coroutineContext) {
            clients.awaitReadiness()
        }
        super.start()
    }

    override fun stop() {
        val shotdownFuture = workerGroup.shutdownGracefully()
        while (!shotdownFuture.isDone) {
            Thread.sleep(200)
        }
        runBlocking(coroutineScope.coroutineContext) {
            clients.close()
        }
        super.stop()
    }

    override suspend fun publish(values: List<Event>) {
        clients.withPoolItem { client ->
            client.publish(values)
        }
    }

}
