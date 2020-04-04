/*
 * Copyright (c) 2020 - 2020 TomTom N.V. All rights reserved.
 *
 * This software is the proprietary copyright of TomTom N.V. and its subsidiaries and may be
 * used for internal evaluation purposes or commercial use strictly subject to separate
 * licensee agreement between you and TomTom. If you are the licensee, you are only permitted
 * to use this Software in accordance with the terms of your license agreement. If you are
 * not the licensee then you are not authorised to use this software in any manner and should
 * immediately return it to TomTom N.V.
 */

package com.tomtom.kotlin.traceevents

import io.mockk.coVerifySequence
import io.mockk.spyk
import io.mockk.verify
import io.mockk.verifySequence
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import nl.jqno.equalsverifier.EqualsVerifier
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.eq

class TracerTest {

    interface MyEvents : TraceEventListener {

        fun eventNoArgs()

        fun eventString(message: String)

        @LogLevel(Log.Level.ERROR)
        fun eventIntsString(number1: Int, number2: Int, message: String)

        /**
         * Overload a reserved name for Log functions.
         */
        fun d()
    }

    class SpecificConsumer : MyEvents, TraceEventConsumer {
        override fun eventNoArgs() =
                Log.log(Log.Level.DEBUG, "TAG", "eventNoArgs()")

        override fun eventString(message: String) =
                Log.log(Log.Level.DEBUG, "TAG", "eventString()")

        override fun eventIntsString(number1: Int, number2: Int, message: String) =
                Log.log(Log.Level.DEBUG, "TAG", "eventIntsString()")

        override fun d() =
                Log.log(Log.Level.DEBUG, "TAG", "d()")
    }

    class GenericConsumer : GenericTraceEventConsumer, TraceEventConsumer {
        override suspend fun consumeTraceEvent(traceEvent: TraceEvent) =
                Log.log(Log.Level.DEBUG, "TAG", "${traceEvent.functionName}")
    }

    private val TAG = TracerTest::class.simpleName

    private val sut = Tracer.Factory.create<TracerTest.MyEvents>(this::class)

    @Before
    fun setUp() {

        /**
         * For every test case remove all consumer, cancel the processor (which will be restarted
         * at next add consumer) and flush all events. Make sure that when the processor starts,
         * it starts on the thread of this test.
         */
        runBlocking {
            Tracer.eventProcessorScope = CoroutineScope(Dispatchers.Unconfined)
            Tracer.setTraceEventLoggingMode(Tracer.Companion.LoggingMode.SYNC)
            Tracer.enableTraceEventLogging(true)
            Tracer.removeAllTraceEventConsumers()
            Tracer.cancelAndJoinEventProcessor()
            Tracer.flushTraceEvents()
        }
    }

    @Test
    fun `specific consumer`() {
        // GIVEN
        val consumer = spyk(TracerTest.SpecificConsumer())
        Tracer.addTraceEventConsumer(consumer)

        // WHEN
        sut.eventNoArgs()
        sut.eventString("abc")
        sut.eventIntsString(10, 20, "abc")

        // THEN
        coVerifySequence {
            consumer.eventNoArgs()
            consumer.eventString(eq("abc"))
            consumer.eventIntsString(eq(10), eq(20), eq("abc"))
        }
    }

    @Test
    fun `generic consumer`() {
        // GIVEN
        val consumer = spyk(TracerTest.GenericConsumer())
        Tracer.addTraceEventConsumer(consumer)

        // WHEN
        sut.eventNoArgs()
        sut.eventString("xyz")
        sut.eventIntsString(10, 20, "abc")

        // THEN
        coVerifySequence {
            consumer.consumeTraceEvent(traceEq(Log.Level.DEBUG, "eventNoArgs"))
            consumer.consumeTraceEvent(traceEq(Log.Level.DEBUG, "eventString", "xyz"))
            consumer.consumeTraceEvent(traceEq(Log.Level.ERROR, "eventIntsString", 10, 20, "abc"))
        }
    }

    @Test
    fun `duplicate events`() {
        // GIVEN
        val consumer = spyk(TracerTest.SpecificConsumer())
        Tracer.addTraceEventConsumer(consumer)

        // WHEN
        sut.eventNoArgs()
        sut.eventNoArgs()

        // THEN
        verify(exactly = 2) { consumer.eventNoArgs() }
    }

    @Test
    fun `remove consumer`() {
        // GIVEN
        val consumer = spyk(TracerTest.SpecificConsumer())
        Tracer.addTraceEventConsumer(consumer)
        Tracer.removeTraceEventConsumer(consumer)

        // WHEN
        sut.eventNoArgs()

        // THEN
        verify(exactly = 0) { consumer.eventNoArgs() }
    }

    @Test
    fun `duplicate consumer`() {
        // GIVEN
        val consumer = spyk(TracerTest.SpecificConsumer())
        Tracer.addTraceEventConsumer(consumer)
        Tracer.addTraceEventConsumer(consumer)

        // WHEN
        sut.eventNoArgs()

        // THEN
        verify(exactly = 1) { consumer.eventNoArgs() }
    }

    @Test
    fun `multiple consumers`() {
        // GIVEN
        val consumer1 = spyk(TracerTest.SpecificConsumer())
        val consumer2 = spyk(TracerTest.SpecificConsumer())
        Tracer.addTraceEventConsumer(consumer1)
        Tracer.addTraceEventConsumer(consumer2)

        // WHEN
        sut.eventNoArgs()

        // THEN
        verify(exactly = 1) {
            consumer1.eventNoArgs()
            consumer2.eventNoArgs()
        }
    }

    @Test
    fun `flush events`() {
        // GIVEN
        val consumer = spyk(SpecificConsumer())
        Tracer.addTraceEventConsumer(consumer)

        // WHEN
        runBlocking {
            sut.eventNoArgs()
            sut.eventString("abc")
            sut.eventIntsString(10, 20, "abc")
        }

        // THEN
        coVerifySequence {
            consumer.eventNoArgs()
            consumer.eventString(eq("abc"))
            consumer.eventIntsString(eq(10), eq(20), eq("abc"))
        }

        // WHEN
        runBlocking {

            // Remove event consumer and stop processor.
            Tracer.removeTraceEventConsumer(consumer)
            Tracer.cancelAndJoinEventProcessor()

            // Add more events.
            sut.eventNoArgs()
            sut.eventString("def")
            sut.eventIntsString(100, 200, "def")

            // Flush last 3 events.
            Tracer.flushTraceEvents()

            // And add 1 event.
            sut.eventIntsString(11, 22, "xyz")
        }
        Tracer.addTraceEventConsumer(consumer)

        // THEN
        coVerifySequence {
            consumer.eventNoArgs()
            consumer.eventString(eq("abc"))
            consumer.eventIntsString(eq(10), eq(20), eq("abc"))

            // Make sure we only have the last added event here.
            consumer.eventIntsString(eq(11), eq(22), eq("xyz"))
        }
    }

    @Test
    fun `wrong log function arguments`() {
        // GIVEN
        val consumer = spyk(SpecificConsumer())
        Tracer.addTraceEventConsumer(consumer)

        // WHEN
        sut.d("abc")
        sut.d()
        sut.d("xyz")

        // THEN
        verifySequence {
            consumer.d(eq("abc"))
            consumer.incorrectLogSignatureFound()
            consumer.d()
            consumer.d(eq("xyz"))
        }
    }

    @Test
    fun `equals and hashCode`() {
        EqualsVerifier.forClass(TraceEvent::class.java).verify()
    }
}
