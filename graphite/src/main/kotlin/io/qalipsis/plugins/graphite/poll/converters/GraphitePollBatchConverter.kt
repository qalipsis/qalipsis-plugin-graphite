package io.qalipsis.plugins.graphite.poll.converters

import io.qalipsis.api.context.StepOutput
import io.qalipsis.api.lang.tryAndLogOrNull
import io.qalipsis.api.logging.LoggerHelper.logger
import io.qalipsis.api.steps.datasource.DatasourceObjectConverter
import io.qalipsis.plugins.graphite.poll.GraphitePollResult
import io.qalipsis.plugins.graphite.poll.GraphiteQueryResult
import java.util.concurrent.atomic.AtomicLong

/**
 * Implementation of [DatasourceObjectConverter], that reads a batch of Graphite result
 *
 * @author Teyyihan Aksu
 */
internal class GraphitePollBatchConverter : DatasourceObjectConverter<GraphiteQueryResult, GraphitePollResult> {

    override suspend fun supply(
        offset: AtomicLong,
        value: GraphiteQueryResult,
        output: StepOutput<GraphitePollResult>
    ) {
        tryAndLogOrNull(log) {
            output.send(
                GraphitePollResult(
                    results = value.results,
                    meters = value.meters
                )
            )
        }
    }

    companion object {
        @JvmStatic
        private val log = logger()
    }
}