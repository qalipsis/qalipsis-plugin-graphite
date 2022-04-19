package io.qalipsis.plugins.graphite.render

import io.qalipsis.api.annotations.Spec

@Spec
interface GraphiteSearchConnectionSpecification {

    fun server(url: String)

    fun basicAuthentication(username: String, password: String)

}