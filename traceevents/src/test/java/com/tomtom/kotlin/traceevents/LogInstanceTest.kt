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

class LogInstanceTest {

    @Before
    fun setUp() {
        setUpTracerTest()
    }

    interface TestEvents : TraceEventListener {
        @TraceOptions(includeTaggingClass = true)
        fun someEvent(message: String)
    }

    @Test
    fun `tracer declared in companion object`() {
        val actual = captureStdoutReplaceTime(TIME) {
            tracerFromCompanionObject.someEvent("test1")
        }
        val expected =
            "$TIME DEBUG LogInstanceTest: event=someEvent(test1), taggingClass=LogInstanceTest\n"
        assertEquals(expected, actual)
    }

    @Test
    fun `tracer declared in instance`() {
        val tracerFromInstance = Tracer.Factory.create<TestEvents>(this)
        val actual = captureStdoutReplaceTime(TIME) {
            tracerFromInstance.someEvent("test2")
        }
        val expected =
            "$TIME DEBUG LogInstanceTest: event=someEvent(test2), taggingClass=LogInstanceTest\n"
        assertEquals(expected, actual)
    }

    companion object {
        val tracerFromCompanionObject = Tracer.Factory.create<TestEvents>(this)

        fun captureTracerWithOwnerFromOtherClass(ownerObject: Any): String {
            val tracerFromInstance = Tracer.Factory.create<LogInstanceTest.TestEvents>(ownerObject)
            return captureStdoutReplaceTime(TIME) {
                tracerFromInstance.someEvent("test3")
            }
        }
    }
}
