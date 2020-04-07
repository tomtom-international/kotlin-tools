/*
 * Copyright (c) 2020 - 2020 TomTom N.V. All rights reserved.
 *
 * This software is the proprietary copyright of TomTom N.V. and its subsidiaries and may be
 * used for internal evaluation purposes or commercial use strictly subject to separate
 * licensee agreement between you and TomTom. If you are the licensee, you are only permitted
 * to use this Software in accordance with the terms of your license agreement. If you are
 * not the licensee then you are not authorised to use this software in any manner and should
 * immediately return it to TomTom N.V.
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
            traceEvent.args.map { it.javaClass } == args.map { it.javaClass } &&
            traceEvent.args.contentDeepEquals(args)
    }
