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
