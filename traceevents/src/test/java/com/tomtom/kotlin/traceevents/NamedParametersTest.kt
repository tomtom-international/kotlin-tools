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
import io.mockk.coVerify
import io.mockk.coVerifySequence
import io.mockk.spyk
import io.mockk.verify
import io.mockk.verifySequence
import kotlinx.coroutines.runBlocking
import nl.jqno.equalsverifier.EqualsVerifier
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

class NamedParametersTest {

    interface MyEvents : TraceEventListener {
        fun eventNoArgs()
        fun eventWithParameters(message: String, someInt: Int, anyList: List<Int>)
    }

    class SpecificConsumer : MyEvents, TraceEventConsumer {
        override fun eventNoArgs() {}
        override fun eventWithParameters(message: String, someInt: Int, anyList: List<Int>) {

        }
    }

    class GenericConsumer : GenericTraceEventConsumer, TraceEventConsumer {
        override suspend fun consumeTraceEvent(traceEvent: TraceEvent) =
            TraceLog.log(LogLevel.DEBUG, "TAG", "${traceEvent}")
    }

    companion object {
        val TAG = TracerTest::class.simpleName
        val sut = Tracer.Factory.create<MyEvents>(this)
    }

    @Before
    fun setUp() {
        setUpTracerTest()
    }

    @Test
    fun `use named parameters in a pecific consumer`() {
        // GIVEN
        val consumer = spyk(SpecificConsumer())
        Tracer.addTraceEventConsumer(consumer)

        // WHEN
        sut.eventNoArgs()
        sut.eventWithParameters(message = "my string", someInt = 123, anyList = listOf(1, 2, 3))
        sut.eventWithParameters("my string", 123, listOf(1, 2, 3))

        // THEN
        coVerifySequence {
            consumer.eventNoArgs()
            consumer.eventWithParameters(eq("my string"), eq(123), eq(listOf(1, 2, 3)))
        }
    }

    @Test
    fun `use named parameters generic consumer with no context`() {
        // GIVEN
        val consumer = spyk(GenericConsumer())
        Tracer.addTraceEventConsumer(consumer)

        // WHEN
        sut.eventNoArgs()
        sut.eventWithParameters("my string", 123, listOf(1, 2, 3))

        // THEN
        coVerifySequence {
            consumer.consumeTraceEvent(traceEq("", null, LogLevel.DEBUG, "eventNoArgs"))
            consumer.consumeTraceEvent(traceEq("", null, LogLevel.ERROR,
                "eventWithParameters", "my string", 123, listOf(1, 2, 3)))
        }
    }
}
