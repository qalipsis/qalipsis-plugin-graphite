package io.qalipsis.plugins.graphite.events.model

import io.qalipsis.api.events.Event
import io.qalipsis.api.events.EventLevel
import org.junit.Assert
import org.junit.jupiter.api.Test

/**
 * @author rklymenko
 */
class EventsBufferTest {

    @Test
    fun `should add and poll single event`() {
        //given
        val eventsBuffer = EventsBuffer()
        val event = Event("boo", EventLevel.DEBUG)

        //then
        Assert.assertEquals(0, eventsBuffer.size())

        //when
        eventsBuffer.addAll(listOf(event))

        //then
        Assert.assertEquals(1, eventsBuffer.size())
        Assert.assertEquals(event, eventsBuffer.poll()!![0])
        Assert.assertEquals(0, eventsBuffer.size())
    }

    @Test
    fun `should add and poll list of events`() {
        //given
        val eventsBuffer = EventsBuffer()
        val events = listOf(Event("boo", EventLevel.DEBUG), Event("boo2", EventLevel.DEBUG))

        //then
        Assert.assertEquals(0, eventsBuffer.size())

        //when
        eventsBuffer.addAll(events)

        //then
        Assert.assertEquals(events.size, eventsBuffer.size())
        Assert.assertEquals(events[0], eventsBuffer.poll()!![0])
        Assert.assertEquals(1, eventsBuffer.size())
        Assert.assertEquals(events[1], eventsBuffer.poll()!![0])
        Assert.assertEquals(0, eventsBuffer.size())
    }

    @Test
    fun `should add, copy and clear a list of events`() {
        //given
        val eventsBuffer = EventsBuffer()
        val events = listOf(Event("boo", EventLevel.DEBUG), Event("boo2", EventLevel.DEBUG))

        //then
        Assert.assertEquals(0, eventsBuffer.size())

        //when
        eventsBuffer.addAll(events)

        //then
        Assert.assertEquals(events.size, eventsBuffer.size())
        Assert.assertEquals(events, eventsBuffer.copyAndClear())
        Assert.assertEquals(0, eventsBuffer.size())
    }
}