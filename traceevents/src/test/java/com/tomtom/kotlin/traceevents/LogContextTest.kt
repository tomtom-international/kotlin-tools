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

import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

internal class LogContextTest {

    @Before
    fun setUp() {
        setUpTracerTest()
    }

    interface TestEvents : TraceEventListener {
        fun someEvent(message: String)
    }

    @Test
    fun `tracer declared in companion object`() {
        val actual = captureStdoutReplaceTime(TIME) {
            tracerFromCompanionObject.someEvent("test1")
        }
        val expected =
            "$TIME DEBUG LogContextTest: event=someEvent(test1), context=$CONTEXT_VALUE\n"
        assertEquals(expected, actual)
    }

    @Test
    fun `tracer declared in instance`() {
        val tracerFromInstance = Tracer.Factory.create<TestEvents>(this, context = CONTEXT_VALUE)
        val actual = captureStdoutReplaceTime(TIME) {
            tracerFromInstance.someEvent("test2")
        }
        val expected =
            "$TIME DEBUG LogContextTest: event=someEvent(test2), context=$CONTEXT_VALUE\n"
        assertEquals(expected, actual)
    }

    companion object {
        private const val CONTEXT_VALUE = "42"

        val tracerFromCompanionObject = Tracer.Factory.create<TestEvents>(
                this,
                context = CONTEXT_VALUE
        )
    }
}
