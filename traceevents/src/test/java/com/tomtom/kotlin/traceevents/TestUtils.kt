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
import io.mockk.MockKMatcherScope
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import kotlin.reflect.jvm.jvmName

/**
 * Utility function to check if a trace events matches the given parameters.
 */
fun MockKMatcherScope.traceEq(
    logLevel: LogLevel,
    functionName: String,
    vararg args: Any
) =
    match<TraceEvent> { traceEvent ->
        traceEvent.logLevel == logLevel &&
            traceEvent.calledFromClass == TracerTest::class.jvmName &&
            traceEvent.interfaceName == TracerTest.MyEvents::class.jvmName &&
            traceEvent.eventName == functionName &&
            traceEvent.args.map { it?.javaClass } == args.map { it.javaClass } &&
            traceEvent.args.contentDeepEquals(args)
    }

fun captureStdout(block: () -> Unit) =
    ByteArrayOutputStream().use { outputStream ->
        PrintStream(outputStream).use { printStream ->
            val previousOut = System.out
            System.setOut(printStream);
            block()
            System.setOut(previousOut)
        }
        outputStream.toString()
    }

fun captureStdoutReplaceTime(
    timeReplacement: String,
    block: () -> Unit
) = replaceTime(captureStdout { block() }, timeReplacement)

fun replaceTime(msg: String?, replaceWith: String) = msg?.replace(
    "\\[[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}[.][0-9]+\\]".toRegex(),
    replaceWith
) ?: ""

fun replaceNumber(msg: String?, replaceWith: String) = msg?.replace(
    "\\b[0-9]+\\b".toRegex(RegexOption.MULTILINE),
    replaceWith
) ?: ""
