/*
 * Copyright (C) 2020-2020, TomTom (http://tomtom.com).
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
import com.tomtom.kotlin.traceevents.TraceLog.Logger
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LogOtherClassTest {

    @Before
    fun setUp() {
        TraceLog.setLogger()
    }

    @Test
    fun `log event with called-from other class`() {
        val actual = captureStdoutReplaceTime(TIME) {
            sut.withCalledFromClass("test")
        }

        println(actual)
        // Cut off just enough to NOT include line numbers of source code as they may change.
        val expected =
            "$TIME DEBUG LogOtherClassTest: event=withCalledFromClass(test), class=com.tomtom.kotlin.traceevents.LogOtherClassTest\n"
        assertEquals(expected, actual)
    }

    @Test
    fun `log event with called-from other file`() {
        val actual = captureStdoutReplaceTime(TIME) {
            sut.withCalledFromFile("test")
        }

        println(actual)
        // Cut off just enough to NOT include line numbers of source code as they may change.
        val expected =
            "$TIME VERBOSE LogOtherClassTest: event=withCalledFromFile(test), file=LogOtherClassTest.kt:invoke($NUMBER)\n"
        assertEquals(expected, replaceNumber(actual, NUMBER),
            "Perhaps you should increase STACK_TRACE_DEPTH?")
    }

    @Test
    fun `log event with event interface from other class`() {
        val actual = captureStdoutReplaceTime(TIME) {
            sut.withEventInterface("test")
        }

        println(actual)
        // Cut off just enough to NOT include line numbers of source code as they may change.
        val expected =
            "$TIME DEBUG LogOtherClassTest: event=withEventInterface(test), interface=com.tomtom.kotlin.traceevents.LogTest\$TestEvents\n"
        assertEquals(expected, actual)
    }

    companion object {
        const val TIME = "[TIME]"
        const val NUMBER = "[NUMBER]"
        val sut = Tracer.Factory.create<LogTest.TestEvents>(this)
    }
}
