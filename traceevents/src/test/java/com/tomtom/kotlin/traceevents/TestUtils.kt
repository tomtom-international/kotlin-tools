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

import io.mockk.MockKMatcherScope
import kotlin.reflect.jvm.jvmName

/**
 * Utility function to check if a trace events matches the given parameters.
 */
internal fun MockKMatcherScope.traceEq(
    logLevel: Log.Level,
    functionName: String,
    vararg args: Any
) =
    match<TraceEvent> { traceEvent ->
        traceEvent.logLevel == logLevel &&
            traceEvent.ownerClass == TracerTest::class.jvmName &&
            traceEvent.interfaceName == TracerTest.MyEvents::class.jvmName &&
            traceEvent.functionName == functionName &&
            traceEvent.args.map { it?.javaClass } == args.map { it.javaClass } &&
            traceEvent.args.contentDeepEquals(args)
    }
