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

import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SetLoggerTest {

    private val sut = Tracer.Factory.create<TraceEventListener>(this::class)

    object MyLogger : Log.Logger {
        override fun log(level: Log.Level, tag: String, message: String, e: Throwable?) {
            println("LOG: $level, $tag, $message, $e")
            called = true
        }
    }

    @Test
    fun `specific consumer`() {
        assertFalse(called)

        sut.d("text 1")
        assertFalse(called)

        Log.setLogger(SetLoggerTest.MyLogger)

        sut.d("text 2")
        assertTrue(called)

        called = false
        Log.setLogger()
        sut.d("text 3")
        assertFalse(called)
    }

    companion object {
        var called = false
    }
}
