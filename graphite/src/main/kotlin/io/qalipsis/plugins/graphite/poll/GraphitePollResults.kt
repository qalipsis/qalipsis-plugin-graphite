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

package io.qalipsis.plugins.graphite.poll

import io.qalipsis.plugins.graphite.render.model.GraphiteRenderApiJsonResponse

/**
 * Wrapper for the result of poll from Graphite.
 *
 * @property results list of Graphite records.
 * @property meters of the poll step.
 *
 * @author Teyyihan Aksu
 */
class GraphitePollResults(
    val results: List<GraphiteRenderApiJsonResponse>,
    val meters: GraphiteQueryMeters
): Iterable<GraphiteRenderApiJsonResponse> {

    override fun iterator(): Iterator<GraphiteRenderApiJsonResponse> {
        return results.iterator()
    }
}
