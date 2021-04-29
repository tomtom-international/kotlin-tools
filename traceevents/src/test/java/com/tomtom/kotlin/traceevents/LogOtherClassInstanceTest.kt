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

import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

class LogOtherClassInstanceTest {

    @Before
    fun setUp() {
        setUpTracerTest()
    }

    @Test
    fun `tracer declared in instance in other class`() {
        val tracerFromInstance = Tracer.Factory.create<LogInstanceTest.TestEvents>(this)
        val actual = captureStdoutReplaceTime(TIME) {
            tracerFromInstance.someEvent("test2")
        }
        val expected =
            "$TIME DEBUG LogOtherClassInstanceTest: event=someEvent(test2), taggingClass=LogOtherClassInstanceTest\n"
        assertEquals(expected, actual)
    }

    @Test
    fun `tracer declared with this from other class`() {
        val actual = LogInstanceTest.captureTracerWithOwnerFromOtherClass(this)
        val expected =
            "$TIME DEBUG LogInstanceTest: event=someEvent(test3), taggingClass=LogOtherClassInstanceTest\n"
        assertEquals(expected, actual)
    }
}
