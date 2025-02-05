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

import kotlinx.coroutines.*
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import java.io.ByteArrayOutputStream
import java.io.PrintStream


/**
 * Function to capture stdout output to a string.
 */
internal fun captureStdout(block: () -> Unit) =
    ByteArrayOutputStream().use { outputStream ->
        PrintStream(outputStream).use { printStream ->
            val previousOut = System.out
            System.setOut(printStream)
            block()
            System.setOut(previousOut)
        }
        val output = outputStream.toString()
        println(output)
        // Trim \r characters to ensure the value can be used for test assertions on Windows.
        output.replace("\r", "")
    }

/**
 * Function to capture output and replace times.
 */
internal fun captureStdoutReplaceTime(
    timeReplacement: String,
    block: () -> Unit
) = replaceTime(captureStdout { block() }, timeReplacement)

/**
 * Function to replace a time pattern.
 */
internal fun replaceTime(msg: String?, replaceWith: String) = msg?.replace(
    "\\[[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}([.][0-9]+)?]".toRegex(),
    replaceWith
) ?: ""

/**
 * Function to replace a number pattern.
 */
internal fun replaceNumber(msg: String?, replaceWith: String) = msg?.replace(
    "\\b[0-9]+\\b".toRegex(RegexOption.MULTILINE),
    replaceWith
) ?: ""

internal fun setUpTracerTest() {
    /**
     * For every test case remove all consumer, cancel the processor (which will be restarted
     * at next add consumer) and flush all events. Make sure that when the processor starts,
     * it starts on the thread of this test and clears all thread-local data.
     */
    runBlocking {
        TraceThreadLocalContext.clear()
        TraceLog.setLogger()
        Tracer.setTraceEventLoggingMode(Tracer.Companion.LoggingMode.SYNC)
        Tracer.enableTraceEventLogging(true)
        Tracer.removeAllTraceEventConsumers()
        advanceEventProcessorScopeUntilIdleAsync {
            Tracer.cancelAndJoinEventProcessor()
        }
        Tracer.eventProcessorScope = CoroutineScope(Dispatchers.Unconfined)
        Tracer.flushTraceEvents()
        Tracer.resetToDefaults()
    }
}

/**
 * Dispatches coroutine jobs launched on the [Tracer.eventProcessorScope] once [block] is
 * suspended. This is necessary to unsuspend [block] when using a [TestScope]. Because [TestScope]
 * does not dispatch jobs until calling a method like [TestScope.advanceUntilIdle], any [block]
 * waiting for [Tracer.eventProcessorScope] would wait indefinitely without using this method.
 *
 * If [Tracer.eventProcessorScope] is not a [TestScope], [block] executes as usual.
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal suspend fun advanceEventProcessorScopeUntilIdleAsync(
    block: suspend CoroutineScope.() -> Unit
) {
    coroutineScope {
        val advanceUntilIdleResult =
            Tracer.eventProcessorScope
                .let { it as? TestScope }
                ?.let { async { it.advanceUntilIdle() } }

        block()

        advanceUntilIdleResult?.await()
    }
}

// Replacements for times and numbers.
internal const val TIME = "[TIME]"
internal const val NUMBER = "[NUMBER]"
