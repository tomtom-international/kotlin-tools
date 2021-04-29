/*
 * Copyright (C) 2012-2021, TomTom (http://tomtom.com).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.tomtom.kotlin.traceevents

import com.tomtom.kotlin.traceevents.TraceLog.LogLevel
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
import kotlin.test.assertEquals

class TracerTest {

    interface MyEvents : TraceEventListener {
        fun eventNoArgs()
        fun eventString(message: String)

        @TraceLogLevel(LogLevel.ERROR)
        fun eventIntsString(number1: Int, number2: Int, message: String)
        fun eventNullable(value: Int?)
        fun eventList(list: List<Int?>)
        fun eventArray(array: Array<Int?>)
    }

    class SpecificConsumer : MyEvents, TraceEventConsumer {
        override fun eventNoArgs() {}
        override fun eventString(message: String) {}
        override fun eventIntsString(number1: Int, number2: Int, message: String) {}
        override fun eventNullable(value: Int?) {}
        override fun eventList(list: List<Int?>) {}
        override fun eventArray(array: Array<Int?>) {}
    }

    class GenericConsumer : GenericTraceEventConsumer, TraceEventConsumer {
        override suspend fun consumeTraceEvent(traceEvent: TraceEvent) =
            TraceLog.log(LogLevel.DEBUG, "TAG", "${traceEvent.eventName}")
    }

    interface WrongListener : TraceEventListener {
        fun v()
        fun d(msg: Int)
        fun i(msg: String, e: Int)
        fun w(msg: String, e: Throwable, rest: Int)
    }

    class WrongConsumer : WrongListener, TraceEventConsumer {
        override fun v() = TraceLog.log(LogLevel.VERBOSE, "TAG", "v()")
        override fun d(msg: Int) = TraceLog.log(LogLevel.DEBUG, "TAG", "d()")
        override fun i(msg: String, e: Int) = TraceLog.log(LogLevel.INFO, "TAG", "i()")
        override fun w(msg: String, e: Throwable, rest: Int) =
            TraceLog.log(LogLevel.WARN, "TAG", "w()")
    }

    companion object {
        val TAG = TracerTest::class.simpleName
        val sut = Tracer.Factory.create<TracerTest.MyEvents>(this)
        val sutWrong = Tracer.Factory.create<TracerTest.WrongListener>(this)
        val sutWithoutToString = Tracer.Factory.create<TracerTest.ListenerWithoutToString>(this)
    }

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
            Tracer.resetToDefaults()
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
            consumer.consumeTraceEvent(traceEq(CONTEXT_REGEX_ANY, LogLevel.DEBUG, "eventNoArgs"))
            consumer.consumeTraceEvent(traceEq(CONTEXT_REGEX_ANY, LogLevel.DEBUG, "eventString", "xyz"))
            consumer.consumeTraceEvent(traceEq(CONTEXT_REGEX_ANY, LogLevel.ERROR, "eventIntsString", 10, 20, "abc"))
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
    fun `correct log function arguments`() {
        // GIVEN
        val consumer = spyk(SpecificConsumer())
        Tracer.addTraceEventConsumer(consumer)
        val e = Throwable()

        // WHEN
        sut.d("abc")
        sut.d("null", null)
        sut.d("non-null", e)

        // THEN
        verifySequence {
            consumer.d(eq("abc"))
            consumer.d(eq("null"), isNull())
            consumer.d(eq("non-null"), eq(e))
        }
    }

    @Test
    fun `wrong arguments for v,d,i,w`() {
        // GIVEN
        val consumer = spyk(WrongConsumer())
        Tracer.addTraceEventConsumer(consumer)
        val e = Exception()

        // WHEN
        sutWrong.v()
        sutWrong.d(1)
        sutWrong.i("test", 2)
        sutWrong.w("test", e, 3)

        // THEN
        verifySequence {
            consumer.incorrectLogSignatureFound()
            consumer.v()
            consumer.incorrectLogSignatureFound()
            consumer.d(eq(1))
            consumer.incorrectLogSignatureFound()
            consumer.i(eq("test"), eq(2))
            consumer.incorrectLogSignatureFound()
            consumer.w(eq("test"), eq(e), 3)
        }
    }

    @Test
    fun `nullable arguments`() {
        // GIVEN
        val consumer = spyk(TracerTest.SpecificConsumer())
        Tracer.addTraceEventConsumer(consumer)

        // WHEN
        sut.eventNullable(1)
        sut.eventNullable(null)
        sut.eventNullable(2)

        // THEN
        verifySequence {
            consumer.eventNullable(eq(1))
            consumer.eventNullable(isNull())
            consumer.eventNullable(eq(2))
        }
    }

    @Test
    fun `list with null objects`() {
        // GIVEN
        val consumer = spyk(TracerTest.SpecificConsumer())
        Tracer.addTraceEventConsumer(consumer)

        // WHEN
        sut.eventList(listOf(1, 2))
        sut.eventList(listOf(3, null))
        sut.eventList(listOf(null, 4))
        sut.eventList(listOf(null, null))

        // THEN
        verifySequence {
            consumer.eventList(eq(listOf<Int?>(1, 2)))
            consumer.eventList(eq(listOf<Int?>(3, null)))
            consumer.eventList(eq(listOf<Int?>(null, 4)))
            consumer.eventList(eq(listOf<Int?>(null, null)))
        }
    }

    @Test
    fun `array with null objects`() {
        // GIVEN
        val consumer = spyk(TracerTest.SpecificConsumer())
        Tracer.addTraceEventConsumer(consumer)

        // WHEN
        sut.eventArray(arrayOf(1, 2))
        sut.eventArray(arrayOf(3, null))
        sut.eventArray(arrayOf(null, 4))
        sut.eventArray(arrayOf(null, null))

        // THEN
        verifySequence {
            consumer.eventArray(eq(arrayOf<Int?>(1, 2)))
            consumer.eventArray(eq(arrayOf<Int?>(3, null)))
            consumer.eventArray(eq(arrayOf<Int?>(null, 4)))
            consumer.eventArray(eq(arrayOf<Int?>(null, null)))
        }
    }

    class ClassWithoutToString {
        @Suppress("unused")
        val x = 1

        @Suppress("unused")
        val y = 2
    }

    class AnotherClassWithoutToString {
        val a = 100
    }

    interface ListenerWithoutToString : TraceEventListener {
        fun eventWithoutToString(classWithoutToString: Any)
    }

    class ConsumerWithoutToString : ListenerWithoutToString, TraceEventConsumer {
        override fun eventWithoutToString(classWithoutToString: Any) {}
    }

    @Test
    fun `register toString`() {
        // GIVEN
        val consumerWithoutToString = spyk(ConsumerWithoutToString())
        Tracer.addTraceEventConsumer(consumerWithoutToString)
        val objectWithoutToString = ClassWithoutToString()
        val anotherObjectWithoutToString = AnotherClassWithoutToString()

        Tracer.registerToString<ClassWithoutToString> { "($x, $y)" }

        // WHEN
        sutWithoutToString.eventWithoutToString(objectWithoutToString)
        sutWithoutToString.eventWithoutToString(anotherObjectWithoutToString)

        // THEN
        coVerifySequence {
            consumerWithoutToString.eventWithoutToString(eq(objectWithoutToString))
            consumerWithoutToString.eventWithoutToString(eq(anotherObjectWithoutToString))
        }
    }

    @Test
    fun `do not register toString for Boolean`() {
        assertEquals("true", Tracer.convertToStringUsingRegistry(true))
        assertEquals("false", Tracer.convertToStringUsingRegistry(false))
    }

    @Test
    fun `register toString for Boolean`() {
        Tracer.registerToString<Boolean> { if (this) "T" else "F" }
        assertEquals("T", Tracer.convertToStringUsingRegistry(true))
        assertEquals("F", Tracer.convertToStringUsingRegistry(false))
    }

    class SomeClass {
        val x = 10
    }

    @Test
    fun `register toString for SomeClass`() {
        Tracer.registerToString<SomeClass> { "x=$x" }
        assertEquals("x=10", Tracer.convertToStringUsingRegistry(SomeClass()))
    }

    @Test
    fun `equals and hashCode`() {
        EqualsVerifier.forClass(TraceEvent::class.java).verify()
    }
}
