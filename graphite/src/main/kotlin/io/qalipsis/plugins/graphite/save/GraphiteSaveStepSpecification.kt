package io.qalipsis.plugins.graphite.save

import io.qalipsis.api.annotations.Spec
import io.qalipsis.api.context.StepContext
import io.qalipsis.api.steps.AbstractStepSpecification
import io.qalipsis.api.steps.StepMonitoringConfiguration
import io.qalipsis.api.steps.StepSpecification
import io.qalipsis.plugins.graphite.GraphiteStepSpecification
import io.qalipsis.plugins.graphite.GraphiteConnectionSpecification
import io.qalipsis.plugins.graphite.GraphiteConnectionSpecificationImpl

/**
 * Specification for a [io.qalipsis.plugins.graphite.save.GraphiteSaveStep] to save data to a Graphite.
 *
 * @author Palina Bril
 */
interface GraphiteSaveStepSpecification<I> :
    StepSpecification<I, I, GraphiteSaveStepSpecification<I>>,
    GraphiteStepSpecification<I, I, GraphiteSaveStepSpecification<I>> {

    /**
     * Configures the connection to the Graphite server.
     */
    fun connect(connectionConfiguration: GraphiteConnectionSpecification.() -> Unit)

    /**
     * Defines the statement to execute when saving.
     */
    fun records(recordsFactory: suspend (ctx: StepContext<*, *>, input: I) -> List<String>)

    /**
     * Configures the monitoring of the save step.
     */
    fun monitoring(monitoringConfig: StepMonitoringConfiguration.() -> Unit)

}

/**
 * Implementation of [GraphiteSaveStepSpecification].
 *
 */
@Spec
internal class GraphiteSaveStepSpecificationImpl<I> :
    GraphiteSaveStepSpecification<I>,
    AbstractStepSpecification<I, I, GraphiteSaveStepSpecification<I>>() {

    internal var connectionConfig = GraphiteConnectionSpecificationImpl()

    internal var records: (suspend (ctx: StepContext<*, *>, input: I) -> List<String>) =  { _, _ -> emptyList() }

    internal var monitoringConfig = StepMonitoringConfiguration()

    override fun connect(connectionConfiguration: GraphiteConnectionSpecification.() -> Unit) {
        connectionConfig.connectionConfiguration();
    }

    override fun records(recordsFactory: suspend (ctx: StepContext<*, *>, input: I) -> List<String>) {
        this.records = recordsFactory
    }

    override fun monitoring(monitoringConfig: StepMonitoringConfiguration.() -> Unit) {
        this.monitoringConfig.monitoringConfig()
    }
}

/**
 * Saves messages into Graphite.
 *
 */
fun <I> GraphiteStepSpecification<*, I, *>.save(
    configurationBlock: GraphiteSaveStepSpecification<I>.() -> Unit
): GraphiteSaveStepSpecification<I> {
    val step = GraphiteSaveStepSpecificationImpl<I>()
    step.configurationBlock()

    this.add(step)
    return step
}