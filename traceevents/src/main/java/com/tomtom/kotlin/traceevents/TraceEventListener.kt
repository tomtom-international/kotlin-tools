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

/**
 * Tagging-interface for all interfaces that are used to define trace events.
 * This makes it easy to find all trace events in source code, simply by searching for
 * derived classes of [TraceEventListener].
 *
 * The methods [v], [d], [i], [w] and [e] mimic the ones from Android Log. The `TAG`, used
 * in Android is automatically supplied by the tracer.
 *
 * Normally, you should use type safe event tracing, defining your own trace functions, but
 * these functions may be a convenient start to replace all `Log.log(Log.Level.DEBUG,TAG, ...)`
 * statements with:
 * ```
 * private val tracer = Tracer.Factory.create<TraceEventListener>(this)
 * ...
 * tracer.d("A debug message")

 * ```
 */
interface TraceEventListener {

    // Verbose
    fun v(message: String, e: Throwable? = null) {}

    // Debug
    fun d(message: String, e: Throwable? = null) {}

    // Info
    fun i(message: String, e: Throwable? = null) {}

    // Warn
    fun w(message: String, e: Throwable? = null) {}

    // Error
    fun e(message: String, e: Throwable? = null) {}

    // Called if an incorrect log function signature was used by a subclass.
    fun incorrectLogSignatureFound() {}
}
