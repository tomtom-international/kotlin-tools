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
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LogTest {

    class LoggerCalled : Logger {
        var called = false
        override fun log(logLevel: LogLevel, tag: String, message: String, e: Throwable?) {
            println("TEST1-LOG: $logLevel, $tag, $message, $e")
            called = true
        }
    }

    class LoggerCheckString : Logger {
        var lastLogLevel: LogLevel? = null
        var lastTag: String? = null
        var lastMessage: String? = null
        var lastE: Throwable? = null

        override fun log(logLevel: LogLevel, tag: String, message: String, e: Throwable?) {
            println("TEST2-LOG: $logLevel, $tag, $message, $e")
            lastLogLevel = logLevel
            lastTag = tag
            lastMessage = message
            lastE = e
        }
    }

    interface AllArgsEvent : TraceEventListener {
        @TraceLogLevel(LogLevel.ERROR)
        fun someEvent(aInt: Int?, aString: String?, aArray: Array<Int?>?, aList: List<Int?>?)
    }

    @Test
    fun `specific consumer`() {
        val logger = LoggerCalled()
        assertFalse(logger.called)

        sut.d("text 1")
        assertFalse(logger.called)

        TraceLog.setLogger(logger)

        sut.d("text 2")
        assertTrue(logger.called)

        logger.called = false
        TraceLog.setLogger()
        sut.d("text 3")
        assertFalse(logger.called)
    }

    @Test
    fun `check simple logger parameters`() {
        val logger = LoggerCheckString()
        TraceLog.setLogger(logger)

        sut.v("verbose")
        assertEquals(LogLevel.VERBOSE, logger.lastLogLevel)
        assertEquals("verbose", logger.lastMessage)

        sut.d("debug")
        assertEquals(LogLevel.DEBUG, logger.lastLogLevel)
        assertEquals("debug", logger.lastMessage)

        sut.i("info")
        assertEquals(LogLevel.INFO, logger.lastLogLevel)
        assertEquals("info", logger.lastMessage)

        sut.w("warn")
        assertEquals(LogLevel.WARN, logger.lastLogLevel)
        assertEquals("warn", logger.lastMessage)

        sut.e("error")
        assertEquals(LogLevel.ERROR, logger.lastLogLevel)
        assertEquals("error", logger.lastMessage)

        sut.d("text1")
        assertEquals(LogLevel.DEBUG, logger.lastLogLevel)
        assertEquals("LogTest", logger.lastTag)
        assertEquals("text1", logger.lastMessage)
        assertEquals(null, logger.lastE)

        sut.d("text2", null)
        assertEquals(LogLevel.DEBUG, logger.lastLogLevel)
        assertEquals("LogTest", logger.lastTag)
        assertEquals("text2", logger.lastMessage)
        assertEquals(null, logger.lastE)

        val e = IllegalStateException()
        sut.d("text3", e)
        assertEquals(LogLevel.DEBUG, logger.lastLogLevel)
        assertEquals("LogTest", logger.lastTag)
        assertEquals("text3", logger.lastMessage)
        assertEquals(e, logger.lastE)
    }

    @Test
    fun `check trace event logger parameters`() {
        val logger = LoggerCheckString()
        TraceLog.setLogger(logger)

        sut.someEvent(null, null, null, null)
        assertEquals(LogLevel.ERROR, logger.lastLogLevel)
        assertEquals("LogTest", logger.lastTag)
        assertEquals(null, logger.lastE)
        assertEquals(
            "event=$TIME someEvent(null, null, null, null), from com.tomtom.kotlin.traceevents.LogTest",
            replaceTime(logger.lastMessage, TIME)
        )

        sut.someEvent(1, "text1", arrayOfNulls(0), listOf<Int?>())
        assertEquals(
            "event=$TIME someEvent(1, text1, [], []), from com.tomtom.kotlin.traceevents.LogTest",
            replaceTime(logger.lastMessage, TIME)
        )

        sut.someEvent(2, "text2", arrayOf(null), listOf<Int?>(null))
        assertEquals(
            "event=$TIME someEvent(2, text2, [null], [null]), from com.tomtom.kotlin.traceevents.LogTest",
            replaceTime(logger.lastMessage, TIME)
        )

        sut.someEvent(3, "text3", arrayOf(1), listOf<Int?>(2))
        assertEquals(
            "event=$TIME someEvent(3, text3, [1], [2]), from com.tomtom.kotlin.traceevents.LogTest",
            replaceTime(logger.lastMessage, TIME)
        )

        sut.someEvent(4, "text4", arrayOf(1, 2), listOf<Int?>(3, 4))
        assertEquals(
            "event=$TIME someEvent(4, text4, [1, 2], [3, 4]), from com.tomtom.kotlin.traceevents.LogTest",
            replaceTime(logger.lastMessage, TIME)
        )
    }

    @Test
    fun `log message to stdout`() {
        val outputStream = ByteArrayOutputStream()
        val printStream = PrintStream(outputStream)
        val previousOut = System.out
        System.setOut(printStream);
        loggerOnly.d("test1")
        System.setOut(previousOut)
        printStream.close()
        val actual = replaceTime(outputStream.toString(), TIME) ?: ""
        assertEquals("$TIME: [DEBUG] LogTest: test1\n", actual)
    }

    @Test
    fun `log message with exception to stdout`() {
        val outputStream = ByteArrayOutputStream()
        val printStream = PrintStream(outputStream)
        val previousOut = System.out
        System.setOut(printStream);
        loggerOnly.d("test1", IllegalStateException("error1", NullPointerException()))
        System.setOut(previousOut)
        printStream.close()

        // Cut off just enough to NOT include line numbers of source code as they may change.
        val prefix =
            "$TIME: [DEBUG] LogTest: test1, java.lang.IllegalStateException: error1\n" +
                "\tat com.tomtom.kotlin.traceevents.LogTest.log message with exception to stdout(LogTest.kt:"
        val suffix =
            ")\n" +
                "Caused by: java.lang.NullPointerException\n" +
                "\t... $NUMBER more\n" +
                "\n"

        val actual = replaceTime(outputStream.toString(), TIME) ?: ""
        assertTrue(actual.startsWith(prefix))
        assertTrue(replaceNumber(actual, NUMBER).endsWith(suffix))
    }

    companion object {
        const val TIME = "[TIME]"
        const val NUMBER = "[NUMBER]"
        val sut = Tracer.Factory.create<AllArgsEvent>(this)
        val loggerOnly = Tracer.Factory.createLoggerOnly(this)
    }
}
