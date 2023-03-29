package io.qalipsis.plugins.graphite.poll.model.events.model

/**
 * Enumerates supported [graphite][https://github.com/graphite-project] transport protocols.
 *
 * @author rklymenko
 */
internal enum class GraphiteProtocolType {
    plaintext, pickle
}