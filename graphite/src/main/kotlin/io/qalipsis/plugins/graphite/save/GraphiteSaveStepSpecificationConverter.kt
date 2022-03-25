package io.qalipsis.plugins.graphite.save

import io.micrometer.core.instrument.MeterRegistry
import io.qalipsis.api.annotations.StepConverter
import io.qalipsis.api.context.StepContext
import io.qalipsis.api.events.EventsLogger
import io.qalipsis.api.lang.supplyIf
import io.qalipsis.api.steps.StepCreationContext
import io.qalipsis.api.steps.StepSpecification
import io.qalipsis.api.steps.StepSpecificationConverter

/**
 * [StepSpecificationConverter] from [GraphiteSaveStepSpecificationImpl] to [GraphiteSaveStep]
 * to use the Save API.
 *
 * @author Palina Bril
 */
@StepConverter
internal class GraphiteSaveStepSpecificationConverter(
    private val meterRegistry: MeterRegistry,
    private val eventsLogger: EventsLogger
) : StepSpecificationConverter<GraphiteSaveStepSpecificationImpl<*>> {

    override fun support(stepSpecification: StepSpecification<*, *, *>): Boolean {
        return stepSpecification is GraphiteSaveStepSpecificationImpl
    }

    override suspend fun <I, O> convert(creationContext: StepCreationContext<GraphiteSaveStepSpecificationImpl<*>>) {
        val spec = creationContext.stepSpecification
        val stepId = spec.name

        @Suppress("UNCHECKED_CAST")
        val step = GraphiteSaveStep(
            id = stepId,
            retryPolicy = spec.retryPolicy,
            graphiteSaveMessageClient = GraphiteSaveMessageClientImpl(
                spec.clientBuilder,
                eventsLogger = supplyIf(spec.monitoringConfig.events) { eventsLogger },
                meterRegistry = supplyIf(spec.monitoringConfig.meters) { meterRegistry }
            ),
            messageFactory = spec.queryConfiguration.messages as suspend (ctx: StepContext<*, *>, input: I) -> List<String>,
        )
        creationContext.createdStep(step)
    }
}