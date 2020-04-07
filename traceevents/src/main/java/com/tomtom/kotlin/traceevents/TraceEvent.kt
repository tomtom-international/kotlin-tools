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

import java.time.LocalDateTime

/**
 * Data class to store a single trace event.
 *
 * @param dateTime Event time.
 * @param logLevel Log level (from [LogLevel] annotation).
 * @param ownerClass Class logging the event.
 * @param interfaceName Interface name, derived from [TraceEventListener].
 * @param functionName Function name in interface, which represents the trace event name.
 * @param args Trace event arguments. Specified as array, to avoid expensive array/list conversions.
 */
data class TraceEvent(
    val dateTime: LocalDateTime,
    val logLevel: Log.Level,
    val ownerClass: String,
    val interfaceName: String,
    val functionName: String,
    val args: Array<Any?>
) {
    /**
     * Need to override the `equals` and `hashCode` functions, as the class contains
     * an `Array`. Otherwise, `equals` would always return `false`.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TraceEvent

        if (dateTime != other.dateTime) return false
        if (logLevel != other.logLevel) return false
        if (ownerClass != other.ownerClass) return false
        if (interfaceName != other.interfaceName) return false
        if (functionName != other.functionName) return false
        if (!args.contentDeepEquals(other.args)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = dateTime.hashCode()
        result = 31 * result + logLevel.hashCode()
        result = 31 * result + ownerClass.hashCode()
        result = 31 * result + interfaceName.hashCode()
        result = 31 * result + functionName.hashCode()
        result = 31 * result + args.contentDeepHashCode()
        return result
    }
}
