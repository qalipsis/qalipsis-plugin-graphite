package io.qalipsis.plugins.graphite.save

import io.qalipsis.api.annotations.Spec
import io.qalipsis.api.context.StepContext
import io.qalipsis.api.steps.AbstractStepSpecification
import io.qalipsis.api.steps.StepMonitoringConfiguration
import io.qalipsis.api.steps.StepSpecification
import io.qalipsis.plugins.graphite.GraphiteClient
import io.qalipsis.plugins.graphite.GraphiteStepConnectionImpl
import io.qalipsis.plugins.graphite.GraphiteStepSpecification

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
    fun connect(connectionConfiguration: GraphiteStepConnectionImpl.() -> Unit)

    /**
     * Defines the statement to execute when saving.
     */
    fun query(queryConfig: GraphiteSaveMessageConfiguration<I>.() -> Unit)

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

    internal var connectionConfig = GraphiteStepConnectionImpl()

    internal lateinit var clientBuilder: (() -> GraphiteClient)

    internal var queryConfiguration = GraphiteSaveMessageConfiguration<I>()

    internal var monitoringConfig = StepMonitoringConfiguration()

    override fun connect(connectionConfiguration: GraphiteStepConnectionImpl.() -> Unit) {
        connectionConfig.connectionConfiguration();
        clientBuilder = {
            GraphiteClient(protocolType = ,
            host = ,
            port = ,
            workerGroup = )
        }
    }

    override fun query(queryConfig: GraphiteSaveMessageConfiguration<I>.() -> Unit) {
        queryConfiguration.queryConfig()
    }

    override fun monitoring(monitoringConfig: StepMonitoringConfiguration.() -> Unit) {
        this.monitoringConfig.monitoringConfig()
    }
}

/**
 * Configuration of routing and generation of messages to save in Graphite.
 *
 * @property messages closure to generate a list of messages for save
 *
 */
@Spec
data class GraphiteSaveMessageConfiguration<I>(
    var messages: suspend (ctx: StepContext<*, *>, input: I) -> List<String> = { _, _ -> listOf() }
)

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