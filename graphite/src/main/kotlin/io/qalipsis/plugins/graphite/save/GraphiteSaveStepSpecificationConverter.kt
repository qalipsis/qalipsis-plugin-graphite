/*
 * Copyright 2022 AERIS IT Solutions GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package io.qalipsis.plugins.graphite.save

import io.qalipsis.api.annotations.StepConverter
import io.qalipsis.api.events.EventsLogger
import io.qalipsis.api.lang.supplyIf
import io.qalipsis.api.meters.CampaignMeterRegistry
import io.qalipsis.api.steps.StepCreationContext
import io.qalipsis.api.steps.StepSpecification
import io.qalipsis.api.steps.StepSpecificationConverter
import io.qalipsis.plugins.graphite.GraphiteProtocol
import io.qalipsis.plugins.graphite.client.GraphiteRecord
import io.qalipsis.plugins.graphite.client.GraphiteTcpClient
import io.qalipsis.plugins.graphite.client.codecs.PickleEncoder
import io.qalipsis.plugins.graphite.client.codecs.PlaintextEncoder

/**
 * [StepSpecificationConverter] from [GraphiteSaveStepSpecificationImpl] to [GraphiteSaveStep]
 * to use the Save API.
 *
 * @author Palina Bril
 */
@StepConverter
internal class GraphiteSaveStepSpecificationConverter(
    private val meterRegistry: CampaignMeterRegistry,
    private val eventsLogger: EventsLogger
) : StepSpecificationConverter<GraphiteSaveStepSpecificationImpl<*>> {

    override fun support(stepSpecification: StepSpecification<*, *, *>): Boolean {
        return stepSpecification is GraphiteSaveStepSpecificationImpl
    }

    override suspend fun <I, O> convert(creationContext: StepCreationContext<GraphiteSaveStepSpecificationImpl<*>>) {
        val spec = creationContext.stepSpecification
        val stepId = spec.name
        val clientBuilder = {
            val encoder = when (spec.connectionConfig.protocol) {
                GraphiteProtocol.PLAINTEXT -> PlaintextEncoder()
                GraphiteProtocol.PICKLE -> PickleEncoder()
            }
            GraphiteTcpClient<GraphiteRecord>(
                host = spec.connectionConfig.host,
                port = spec.connectionConfig.port,
                encoders = listOf(encoder),
                workerGroup = spec.connectionConfig.nettyWorkerGroup(),
                channelClass = spec.connectionConfig.nettyChannelClass
            )
        }

        val step = GraphiteSaveStep(
            id = stepId,
            retryPolicy = spec.retryPolicy,
            clientBuilder = clientBuilder,
            eventsLogger = supplyIf(spec.monitoringConfig.events) { eventsLogger },
            meterRegistry = supplyIf(spec.monitoringConfig.meters) { meterRegistry },
            messageFactory = spec.records
        )
        creationContext.createdStep(step)
    }
}
