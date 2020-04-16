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
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

class AnnotationsTest {

    interface FunctionAnnotationEvents : TraceEventListener {
        @TraceLogLevel(LogLevel.ERROR)
        fun eventError()

        @TraceOptions(includeTaggingClass = true)
        fun eventTaggingClass()
    }

    @TraceLogLevel(LogLevel.ERROR)
    @TraceOptions(includeTaggingClass = true)
    interface InterfaceAnnotationsEvents : TraceEventListener {
        fun event()
    }

    @TraceLogLevel(LogLevel.INFO)
    @TraceOptions(includeTaggingClass = true)
    interface MixedAnnotationsEvents : TraceEventListener {
        @TraceLogLevel(LogLevel.ERROR)
        fun event()
    }

    @Before
    fun setUp() {
        TraceLog.setLogger()
    }

    @Test
    fun `check function level annotations`() {
        val tracer = Tracer.Factory.create<FunctionAnnotationEvents>(this)
        val actual = captureStdoutReplaceTime(TIME) {
            tracer.eventError()
            tracer.eventTaggingClass()
        }
        val expected = "$TIME ERROR AnnotationsTest: event=eventError()\n" +
            "$TIME DEBUG AnnotationsTest: event=eventTaggingClass(), taggingClass=AnnotationsTest\n"
        assertEquals(expected, actual, NUMBER)
    }

    @Test
    fun `check interface level annotations`() {
        val tracer = Tracer.Factory.create<InterfaceAnnotationsEvents>(this)
        val actual = captureStdoutReplaceTime(TIME) {
            tracer.event()
        }
        val expected = "$TIME ERROR AnnotationsTest: event=event(), taggingClass=AnnotationsTest\n"
        assertEquals(expected, actual, NUMBER)
    }

    @Test
    fun `check mixed level annotations`() {
        val tracer = Tracer.Factory.create<MixedAnnotationsEvents>(this)
        val actual = captureStdoutReplaceTime(TIME) {
            tracer.event()
        }
        val expected = "$TIME ERROR AnnotationsTest: event=event(), taggingClass=AnnotationsTest\n"
        assertEquals(expected, actual, NUMBER)
    }
}
