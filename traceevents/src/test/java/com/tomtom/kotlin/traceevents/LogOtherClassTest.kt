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

import com.tomtom.kotlin.traceevents.LogTest.Companion.tracerFromLogTest
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

class LogOtherClassTest {

    @Before
    fun setUp() {
        TraceLog.setLogger()
    }

    @Test
    fun `log event with tagging class, from other class`() {
        val actual = captureStdoutReplaceTime(TIME) {
            tracerFromLogTest.withCalledFromClass("test")
        }
        val expected =
            "$TIME DEBUG LogTest: event=withCalledFromClass(test), taggingClass=LogTest\n"
        assertEquals(expected, actual)
    }

    @Test
    fun `log event with file location, from other class`() {
        val actual = captureStdoutReplaceTime(TIME) {
            tracerFromLogTest.withCalledFromFile("test")
        }
        val expected =
            "$TIME VERBOSE LogTest: event=withCalledFromFile(test), fileLocation=LogOtherClassTest.kt:invoke($NUMBER)\n"
        assertEquals(
            expected, replaceNumber(actual, NUMBER),
            "Perhaps you should increase STACK_TRACE_DEPTH?"
        )
    }

    @Test
    fun `log event with event interface, from other class`() {
        val actual = captureStdoutReplaceTime(TIME) {
            tracerFromLogTest.withEventInterface("test")
        }
        val expected =
            "$TIME DEBUG LogTest: event=withEventInterface(test), eventInterface=com.tomtom.kotlin.traceevents.LogTest\$TestEvents\n"
        assertEquals(expected, actual)
    }
}
