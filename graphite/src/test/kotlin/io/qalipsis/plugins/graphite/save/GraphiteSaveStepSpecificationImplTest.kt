package io.qalipsis.plugins.graphite.save

import assertk.all
import assertk.assertThat
import assertk.assertions.*
import io.aerisconsulting.catadioptre.getProperty
import io.netty.channel.nio.NioEventLoopGroup
import io.qalipsis.api.context.StepContext
import io.qalipsis.api.steps.DummyStepSpecification
import io.qalipsis.api.steps.StepMonitoringConfiguration
import io.qalipsis.plugins.graphite.graphite
import io.qalipsis.test.mockk.relaxedMockk
import kotlinx.coroutines.test.runBlockingTest
import org.junit.jupiter.api.Test


/**
 *
 * @author Palina Bril
 */
internal class GraphiteSaveStepSpecificationImplTest {

    private val recordSupplier: (suspend (ctx: StepContext<*, *>, input: Any?) -> List<String>) = { _, _ ->
        listOf(
            "foo 1.1\n", "foo 1.2\n", "foo 1.3\n"
        )
    }

    @Test
    fun `should add minimal configuration for the step`() = runBlockingTest {
        val previousStep = DummyStepSpecification()
        previousStep.graphite().save {
            name = "my-save-step"
            connect {
                server("localhost", 8080)
                basic(NioEventLoopGroup())
            }
            query {
                messages = recordSupplier
            }

        }

        assertThat(previousStep.nextSteps[0]).isInstanceOf(GraphiteSaveStepSpecificationImpl::class).all {
            prop("name") { GraphiteSaveStepSpecificationImpl<*>::name.call(it) }.isEqualTo("my-save-step")
            prop(GraphiteSaveStepSpecificationImpl<*>::clientBuilder).isNotNull()
            prop(GraphiteSaveStepSpecificationImpl<*>::queryConfiguration).all {
                prop(GraphiteSaveMessageConfiguration<*>::messages).isEqualTo(recordSupplier)
            }
            prop(GraphiteSaveStepSpecificationImpl<*>::monitoringConfig).isNotNull().all {
                prop(StepMonitoringConfiguration::events).isFalse()
                prop(StepMonitoringConfiguration::meters).isFalse()
            }
        }

        val step: GraphiteSaveStepSpecificationImpl<*> =
            previousStep.nextSteps[0] as GraphiteSaveStepSpecificationImpl<*>

        val messages =
            step.queryConfiguration.getProperty<suspend (ctx: StepContext<*, *>, input: Int) -> String>("messages")
        assertThat(messages(relaxedMockk(), relaxedMockk())).isEqualTo(
            listOf(
                "foo 1.1\n", "foo 1.2\n", "foo 1.3\n"
            )
        )
    }


    @Test
    fun `should add a complete configuration for the step`() = runBlockingTest {
        val previousStep = DummyStepSpecification()
        previousStep.graphite().save {
            name = "my-save-step"
            connect {
                server("localhost", 8080)
                basic(NioEventLoopGroup())
            }
            query {
                messages = recordSupplier
            }
            monitoring {
                events = true
                meters = true
            }
        }

        assertThat(previousStep.nextSteps[0]).isInstanceOf(GraphiteSaveStepSpecificationImpl::class).all {
            prop("name") { GraphiteSaveStepSpecificationImpl<*>::name.call(it) }.isEqualTo("my-save-step")
            prop(GraphiteSaveStepSpecificationImpl<*>::clientBuilder).isNotNull()
            prop(GraphiteSaveStepSpecificationImpl<*>::queryConfiguration).all {
                prop(GraphiteSaveMessageConfiguration<*>::messages).isEqualTo(recordSupplier)
            }
            prop(GraphiteSaveStepSpecificationImpl<*>::monitoringConfig).all {
                prop(StepMonitoringConfiguration::events).isTrue()
                prop(StepMonitoringConfiguration::meters).isTrue()
            }
        }

        val step: GraphiteSaveStepSpecificationImpl<*> =
            previousStep.nextSteps[0] as GraphiteSaveStepSpecificationImpl<*>

        val messages =
            step.queryConfiguration.getProperty<suspend (ctx: StepContext<*, *>, input: Int) -> String>("messages")
        assertThat(messages(relaxedMockk(), relaxedMockk())).isEqualTo(
            listOf(
                "foo 1.1\n", "foo 1.2\n", "foo 1.3\n"
            )
        )
    }
}
