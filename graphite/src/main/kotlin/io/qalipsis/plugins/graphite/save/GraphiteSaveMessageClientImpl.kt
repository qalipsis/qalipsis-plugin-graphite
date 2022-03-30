package io.qalipsis.plugins.graphite.save

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import io.qalipsis.api.context.StepStartStopContext
import io.qalipsis.api.events.EventsLogger
import io.qalipsis.api.lang.tryAndLog
import io.qalipsis.api.logging.LoggerHelper.logger
import io.qalipsis.plugins.graphite.GraphiteClient
import java.time.Duration
import java.util.concurrent.TimeUnit

/**
 * Implementation of [GraphiteSaveMessageClient].
 * Client to query to Graphite.
 *
 * @property clientBuilder supplier for the Graphite client.
 * @property eventsLogger the logger for events to track what happens during save step execution.
 * @property meterRegistry registry for the meters.
 *
 * @author Palina Bril
 */
internal class GraphiteSaveMessageClientImpl(
    private val clientBuilder: () -> GraphiteClient,
    private val eventsLogger: EventsLogger?,
    private val meterRegistry: MeterRegistry?
) : GraphiteSaveMessageClient {

    private lateinit var client: GraphiteClient

    private val eventPrefix = "graphite.save"

    private val meterPrefix = "graphite-save"

    private var messageCounter: Counter? = null

    private var timeToResponse: Timer? = null

    private var successCounter: Counter? = null

    override suspend fun start(context: StepStartStopContext) {
        client = clientBuilder()
        client.start()
        meterRegistry?.apply {
            val tags = context.toMetersTags()
            messageCounter = counter("$meterPrefix-saving-messages", tags)
            timeToResponse = timer("$meterPrefix-time-to-response", tags)
            successCounter = counter("$meterPrefix-successes", tags)
        }
    }

    override suspend fun execute(
        messages: List<String>,
        contextEventTags: Map<String, String>
    ): GraphiteSaveQueryMeters {
        eventsLogger?.debug("$eventPrefix.saving-messages", messages.size, tags = contextEventTags)
        messageCounter?.increment(messages.size.toDouble())
        val requestStart = System.nanoTime()
        client.publish(messages)
        val timeToResponseNano = System.nanoTime() - requestStart
        val timeToResponse = Duration.ofNanos(timeToResponseNano)
        eventsLogger?.info("${eventPrefix}.time-to-response", timeToResponse, tags = contextEventTags)
        eventsLogger?.info("${eventPrefix}.successes", messages.size, tags = contextEventTags)
        successCounter?.increment(messages.size.toDouble())
        this.timeToResponse?.record(timeToResponseNano, TimeUnit.NANOSECONDS)
        return GraphiteSaveQueryMeters(
            timeToResult = timeToResponse,
            savedMessages = messages.size
        )
    }

    override suspend fun stop(context: StepStartStopContext) {
        meterRegistry?.apply {
            remove(messageCounter!!)
            remove(timeToResponse!!)
            remove(successCounter!!)
            messageCounter = null
            timeToResponse = null
            successCounter = null
        }
        tryAndLog(log) {
            client.close()
        }
    }

    companion object {
        private val log = logger()
    }
}
