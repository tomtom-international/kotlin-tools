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
import io.mockk.coVerifySequence
import io.mockk.spyk
import org.junit.Before
import org.junit.Test
import kotlin.reflect.jvm.jvmName
import kotlin.test.assertEquals

internal class NamedParametersTest {

    interface MyEvents : TraceEventListener {
        fun event()
        fun event(message: String, someInt: Int, anyList: List<Int>)
    }

    class SpecificConsumer : MyEvents, TraceEventConsumer {
        override fun event() {}
        override fun event(message: String, someInt: Int, anyList: List<Int>) {

        }
    }

    class GenericConsumer : GenericTraceEventConsumer, TraceEventConsumer {
        override suspend fun consumeTraceEvent(traceEvent: TraceEvent) =
            TraceLog.log(LogLevel.DEBUG, "TAG", "${traceEvent.getNamedParametersMap()}")
    }

    @Before
    fun setUp() {
        setUpTracerTest()
    }

    @Test
    fun `use named parameters in specific consumer`() {
        // GIVEN
        val consumer = spyk(SpecificConsumer())
        Tracer.addTraceEventConsumer(consumer)

        // WHEN
        sut.event()
        sut.event(anyList = listOf(1, 2, 3), message = "my string", someInt = 123)
        sut.event(message = "more", someInt = 456, anyList = listOf(5))
        sut.event("final", 789, listOf())

        // THEN
        coVerifySequence {
            consumer.event()
            consumer.event("my string", 123, listOf(1, 2, 3))
            consumer.event("more", 456, listOf(5))
            consumer.event("final", 789, listOf())
        }
    }

    @Test
    fun `use named parameters in generic consumer`() {
        // GIVEN
        val consumer = spyk(GenericConsumer())
        Tracer.addTraceEventConsumer(consumer)

        // WHEN
        sut.event()
        sut.event(anyList = listOf(1, 2, 3), message = "my string", someInt = 123)
        sut.event(message = "more", someInt = 456, anyList = listOf(5))
        sut.event("final", 789, listOf())

        // THEN
        coVerifySequence {
            consumer.consumeTraceEvent(traceEq("", null, LogLevel.DEBUG, "event"))
            consumer.consumeTraceEvent(traceEq("", null, LogLevel.DEBUG, "event", "my string", 123, listOf(1, 2, 3)))
            consumer.consumeTraceEvent(traceEq("", null, LogLevel.DEBUG, "event", "more", 456, listOf(5)))
            consumer.consumeTraceEvent(traceEq("", null, LogLevel.DEBUG, "event", "final", 789, listOf<Int>()))
        }
    }

    @Test
    fun `use named parameters in generic consumer with map`() {
        // GIVEN
        val consumer = spyk(GenericConsumer())
        Tracer.addTraceEventConsumer(consumer)

        val actual = captureStdoutReplaceTime(TIME) {

            // WHEN
            sut.event(anyList = listOf(1, 2, 3), message = "my string", someInt = 123)
            sut.event(message = "more", someInt = 456, anyList = listOf(5))
            sut.event("final", 789, listOf())
        }

        // THEN
        val expected =
            "$TIME DEBUG NamedParametersTest: event=event(my string, 123, [1, 2, 3])\n" +
                "$TIME DEBUG TAG: {message=my string, someInt=123, anyList=[1, 2, 3]}\n" +
                "$TIME DEBUG NamedParametersTest: event=event(more, 456, [5])\n" +
                "$TIME DEBUG TAG: {message=more, someInt=456, anyList=[5]}\n" +
                "$TIME DEBUG NamedParametersTest: event=event(final, 789, [])\n" +
                "$TIME DEBUG TAG: {message=final, someInt=789, anyList=[]}\n"
        assertEquals(expected, actual)
    }

    companion object {
        val sut = Tracer.Factory.create<MyEvents>(this)

        fun MockKMatcherScope.traceEq(
            context: String,
            traceThreadLocalContext: Map<String, Any?>? = null,
            logLevel: LogLevel,
            functionName: String,
            vararg args: Any
        ) =
            match<TraceEvent> { traceEvent ->
                val eq = traceEvent.logLevel == logLevel &&
                    traceEvent.taggingClassName == NamedParametersTest::class.jvmName &&
                    traceEvent.context == context &&
                    traceEvent.traceThreadLocalContext == traceThreadLocalContext &&
                    traceEvent.interfaceName == MyEvents::class.jvmName &&
                    traceEvent.eventName == functionName &&
                    traceEvent.args.map { it?.javaClass } == args.map { it.javaClass } &&
                    traceEvent.args.contentDeepEquals(args)
                eq
            }
    }
}
