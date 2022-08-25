/*
 * Copyright (C) 2012-2022, TomTom (http://tomtom.com).
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
import io.mockk.MockKMatcherScope
import io.mockk.coVerify
import io.mockk.coVerifySequence
import io.mockk.spyk
import io.mockk.verify
import io.mockk.verifySequence
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import kotlin.reflect.jvm.jvmName
import kotlin.test.assertEquals

internal class TracerTest {

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
            TraceLog.log(LogLevel.DEBUG, "TAG", traceEvent.eventName)
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

    @Before
    fun setUp() {
        setUpTracerTest()
    }

    @Test
    fun `specific consumer`() {
        // GIVEN
        val consumer = spyk(SpecificConsumer())
        Tracer.addTraceEventConsumer(consumer)

        // WHEN
        sut.eventNoArgs()
        sut.eventString("abc")
        sut.eventIntsString(10, 20, "abc")
        sut.eventIntsString(number1 = 10, number2 = 20, message = "abc")
        sut.eventIntsString(message = "abc", number1 = 10, number2 = 20)

        // THEN
        coVerifySequence {
            consumer.eventNoArgs()
            consumer.eventString("abc")
            consumer.eventIntsString(10, 20, "abc")
            consumer.eventIntsString(10, 20, "abc")
            consumer.eventIntsString(10, 20, "abc")
        }
    }

    @Test
    fun `specific consumer with no context`() {
        // GIVEN
        val consumer = spyk(SpecificConsumer())
        Tracer.addTraceEventConsumer(consumer)  // Should get all events.

        // WHEN
        sut.eventNoArgs()
        sutMain.eventString("abc")
        sutOther.eventIntsString(10, 20, "abc")

        // THEN
        coVerifySequence {
            consumer.eventNoArgs()
            consumer.eventString("abc")
            consumer.eventIntsString(10, 20, "abc")
        }
    }

    @Test
    fun `specific consumer with regex matching a tracer`() {
        // GIVEN
        val consumer = spyk(SpecificConsumer())
        Tracer.addTraceEventConsumer(consumer, Regex(".*main.*"))

        // WHEN
        sut.eventNoArgs()
        sutMain.eventString("abc")
        sutOther.eventIntsString(10, 20, "abc")

        // THEN
        coVerifySequence {
            consumer.eventString("abc")
        }
    }

    @Test
    fun `specific consumer with regex non matching any tracer`() {
        // GIVEN
        val consumer = spyk(SpecificConsumer())
        Tracer.addTraceEventConsumer(consumer, Regex("main"))

        // WHEN
        sut.eventNoArgs()
        sutMain.eventString("abc")
        sutOther.eventIntsString(10, 20, "abc")

        // THEN
        verify(exactly = 0) {
            consumer.eventNoArgs()
            consumer.eventString(any())
            consumer.eventIntsString(any(), any(), any())
        }
    }

    @Test
    fun `generic consumer with no context`() {
        // GIVEN
        val consumer = spyk(GenericConsumer())
        Tracer.addTraceEventConsumer(consumer)

        // WHEN
        sut.eventNoArgs()
        sut.eventString("xyz")
        sut.eventIntsString(10, 20, "abc")
        sut.eventIntsString(number1 = 10, number2 = 20, message = "abc")
        sut.eventIntsString(message = "abc", number1 = 10, number2 = 20)

        // THEN
        coVerifySequence {
            consumer.consumeTraceEvent(traceEq("", null, LogLevel.DEBUG, "eventNoArgs"))
            consumer.consumeTraceEvent(traceEq("", null, LogLevel.DEBUG, "eventString", "xyz"))
            consumer.consumeTraceEvent(
                traceEq(
                    "",
                    null,
                    LogLevel.ERROR,
                    "eventIntsString",
                    10,
                    20,
                    "abc"
                )
            )
            consumer.consumeTraceEvent(
                traceEq(
                    "",
                    null,
                    LogLevel.ERROR,
                    "eventIntsString",
                    10,
                    20,
                    "abc"
                )
            )
            consumer.consumeTraceEvent(
                traceEq(
                    "",
                    null,
                    LogLevel.ERROR,
                    "eventIntsString",
                    10,
                    20,
                    "abc"
                )
            )
        }
    }

    @Test
    fun `generic consumer with context that matches a tracer`() {
        // GIVEN
        val consumer = spyk(GenericConsumer())
        Tracer.addTraceEventConsumer(consumer, Regex(".*main.*"))

        // WHEN
        sut.eventNoArgs()
        sutMain.eventString("xyz")
        sutOther.eventIntsString(10, 20, "abc")

        // THEN
        coVerifySequence {
            consumer.consumeTraceEvent(
                traceEq(
                    "the main tracer",
                    null,
                    LogLevel.DEBUG,
                    "eventString",
                    "xyz"
                )
            )
        }
    }

    @Test
    fun `generic consumer with context that matches no tracer`() {
        // GIVEN
        val consumer = spyk(GenericConsumer())
        Tracer.addTraceEventConsumer(consumer, Regex("main"))

        // WHEN
        sut.eventNoArgs()
        sutMain.eventString("xyz")
        sutOther.eventIntsString(10, 20, "abc")

        // THEN
        coVerify(exactly = 0) {
            consumer.consumeTraceEvent(any())
        }
    }

    @Test
    fun `generic consumer with thread-local context`() {
        // GIVEN
        val consumer = spyk(GenericConsumer())
        Tracer.addTraceEventConsumer(consumer)

        // WHEN
        sut.eventNoArgs()
        TraceThreadLocalContext.put("id", 123)
        sut.eventNoArgs()


        // THEN
        coVerifySequence {
            consumer.consumeTraceEvent(traceEq("", null, LogLevel.DEBUG, "eventNoArgs"))
            consumer.consumeTraceEvent(
                traceEq(
                    "",
                    HashMap(mapOf("id" to 123)),
                    LogLevel.DEBUG,
                    "eventNoArgs"
                )
            )
        }
    }

    @Test
    fun `duplicate events`() {
        // GIVEN
        val consumer = spyk(SpecificConsumer())
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
        val consumer = spyk(SpecificConsumer())
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
        val consumer = spyk(SpecificConsumer())
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
        val consumer1 = spyk(SpecificConsumer())
        val consumer2 = spyk(SpecificConsumer())
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
            consumer.eventString("abc")
            consumer.eventIntsString(10, 20, "abc")
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
            consumer.eventString("abc")
            consumer.eventIntsString(10, 20, "abc")

            // Make sure we only have the last added events here.
            @Suppress("ReplaceCallWithBinaryOperator", "UnusedEquals")
            consumer.equals(consumer)   // <-- This call is added because `remove` uses it.
            consumer.eventIntsString(11, 22, "xyz")
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
            consumer.d("abc")
            consumer.d("null", isNull())
            consumer.d("non-null", e)
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
            consumer.d(1)
            consumer.incorrectLogSignatureFound()
            consumer.i("test", 2)
            consumer.incorrectLogSignatureFound()
            consumer.w("test", e, 3)
        }
    }

    @Test
    fun `nullable arguments`() {
        // GIVEN
        val consumer = spyk(SpecificConsumer())
        Tracer.addTraceEventConsumer(consumer)

        // WHEN
        sut.eventNullable(1)
        sut.eventNullable(null)
        sut.eventNullable(2)

        // THEN
        verifySequence {
            consumer.eventNullable(1)
            consumer.eventNullable(isNull())
            consumer.eventNullable(2)
        }
    }

    @Test
    fun `list with null objects`() {
        // GIVEN
        val consumer = spyk(SpecificConsumer())
        Tracer.addTraceEventConsumer(consumer)

        // WHEN
        sut.eventList(listOf(1, 2))
        sut.eventList(listOf(3, null))
        sut.eventList(listOf(null, 4))
        sut.eventList(listOf(null, null))

        // THEN
        verifySequence {
            consumer.eventList(listOf<Int?>(1, 2))
            consumer.eventList(listOf<Int?>(3, null))
            consumer.eventList(listOf<Int?>(null, 4))
            consumer.eventList(listOf<Int?>(null, null))
        }
    }

    @Test
    fun `array with null objects`() {
        // GIVEN
        val consumer = spyk(SpecificConsumer())
        Tracer.addTraceEventConsumer(consumer)

        // WHEN
        sut.eventArray(arrayOf(1, 2))
        sut.eventArray(arrayOf(3, null))
        sut.eventArray(arrayOf(null, 4))
        sut.eventArray(arrayOf(null, null))

        // THEN
        verifySequence {
            consumer.eventArray(arrayOf<Int?>(1, 2))
            consumer.eventArray(arrayOf<Int?>(3, null))
            consumer.eventArray(arrayOf<Int?>(null, 4))
            consumer.eventArray(arrayOf<Int?>(null, null))
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
            consumerWithoutToString.eventWithoutToString(objectWithoutToString)
            consumerWithoutToString.eventWithoutToString(anotherObjectWithoutToString)
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
    fun `TAG matches class name`() {
        assertEquals(Tracer::class.simpleName, Tracer.TAG)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `wait for trace events to be processed`() = runTest {
        // GIVEN
        Tracer.eventProcessorScope = TestScope()

        val consumer = spyk(SpecificConsumer())
        Tracer.addTraceEventConsumer(consumer)

        sut.eventNoArgs()
        sut.eventString("abc")

        coVerify(exactly = 0) {
            consumer.eventNoArgs()
            consumer.eventString("abc")
        }

        // WHEN
        advanceEventProcessorScopeUntilIdleAsync {
            Tracer.waitForEmptyTraceEventsQueue()
        }

        // THEN
        coVerifySequence {
            consumer.eventNoArgs()
            consumer.eventString("abc")
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `wait for trace events to be flushed`() = runTest {
        // GIVEN
        Tracer.eventProcessorScope = TestScope()

        val consumer = spyk(SpecificConsumer())
        Tracer.addTraceEventConsumer(consumer)

        sut.eventNoArgs()
        sut.eventString("abc")

        // WHEN
        val flushTraceEventsResult = async {
            advanceEventProcessorScopeUntilIdleAsync {
                Tracer.flushTraceEvents()
            }
        }
        Tracer.waitForEmptyTraceEventsQueue()

        // THEN
        flushTraceEventsResult.await()
        coVerify(exactly = 0) {
            consumer.eventNoArgs()
            consumer.eventString("abc")
        }
    }

    companion object {
        val sut = Tracer.Factory.create<MyEvents>(this)
        val sutMain = Tracer.Factory.create<MyEvents>(this, "the main tracer")
        val sutOther = Tracer.Factory.create<MyEvents>(this, "another tracer")
        val sutWrong = Tracer.Factory.create<WrongListener>(this)
        val sutWithoutToString = Tracer.Factory.create<ListenerWithoutToString>(this)

        fun MockKMatcherScope.traceEq(
            context: String,
            traceThreadLocalContext: Map<String, Any?>? = null,
            logLevel: LogLevel,
            functionName: String,
            vararg args: Any
        ) =
            match<TraceEvent> { traceEvent ->
                traceEvent.logLevel == logLevel &&
                        traceEvent.taggingClassName == TracerTest::class.jvmName &&
                        traceEvent.context == context &&
                        traceEvent.traceThreadLocalContext == traceThreadLocalContext &&
                        traceEvent.interfaceName == MyEvents::class.jvmName &&
                        traceEvent.eventName == functionName &&
                        traceEvent.args.map { it?.javaClass } == args.map { it.javaClass } &&
                        traceEvent.args.contentDeepEquals(args)
            }
    }
}
