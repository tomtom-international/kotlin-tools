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
import io.mockk.MockKMatcherScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import kotlin.reflect.jvm.jvmName

/**
 * Utility function to check if a trace events matches the given parameters.
 */
fun MockKMatcherScope.traceEq(
    context: String,
    traceThreadLocalContext: Map<String, Any?>? = null,
    logLevel: LogLevel,
    functionName: String,
    vararg args: Any
) =
    match<TraceEvent> { traceEvent ->

        traceEvent.logLevel == logLevel &&
                traceEvent.taggingClassName == TracerTest::class.jvmName &&
                traceEvent.context == context &&
                traceEvent.traceThreadLocalContext == traceThreadLocalContext &&
                traceEvent.interfaceName == TracerTest.MyEvents::class.jvmName &&
                traceEvent.eventName == functionName &&
                traceEvent.args.map { it?.javaClass } == args.map { it.javaClass } &&
                traceEvent.args.contentDeepEquals(args)
    }

/**
 * Function to capture stdout output to a string.
 */
fun captureStdout(block: () -> Unit) =
    ByteArrayOutputStream().use { outputStream ->
        PrintStream(outputStream).use { printStream ->
            val previousOut = System.out
            System.setOut(printStream)
            block()
            System.setOut(previousOut)
        }
        val output = outputStream.toString()
        println(output)
        output
    }

/**
 * Function to capture output and replace times.
 */
fun captureStdoutReplaceTime(
    timeReplacement: String,
    block: () -> Unit
) = replaceTime(captureStdout { block() }, timeReplacement)

/**
 * Function to replace a time pattern.
 */
fun replaceTime(msg: String?, replaceWith: String) = msg?.replace(
    "\\[[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}([.][0-9]+){0,1}\\]".toRegex(),
    replaceWith
) ?: ""

/**
 * Function to replace a number pattern.
 */
fun replaceNumber(msg: String?, replaceWith: String) = msg?.replace(
    "\\b[0-9]+\\b".toRegex(RegexOption.MULTILINE),
    replaceWith
) ?: ""

fun setUpTracerTest() {

    /**
     * For every test case remove all consumer, cancel the processor (which will be restarted
     * at next add consumer) and flush all events. Make sure that when the processor starts,
     * it starts on the thread of this test and clears all thread-local data.
     */
    runBlocking {
        TraceThreadLocalContext.clear()
        TraceLog.setLogger()
        Tracer.eventProcessorScope = CoroutineScope(Dispatchers.Unconfined)
        Tracer.setTraceEventLoggingMode(Tracer.Companion.LoggingMode.SYNC)
        Tracer.enableTraceEventLogging(true)
        Tracer.removeAllTraceEventConsumers()
        Tracer.cancelAndJoinEventProcessor()
        Tracer.flushTraceEvents()
        Tracer.resetToDefaults()
    }
}

/**
 * Replacements for times and numbers.
 */
const val TIME = "[TIME]"
const val NUMBER = "[NUMBER]"
