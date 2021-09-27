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

/**
 * This class allows storing thread-local trace context.
 *
 * Inspired by basic MDC implementation from org.org.slf4j.helpers.BasicMDCAdapter.
 * This implementation allows storing any object type, not just strings, in a map.
 */
object TraceThreadLocalContext {

    private val inheritableThreadLocal = object : InheritableThreadLocal<MutableMap<String, Any>?>() {
        fun childValue(parentValue: Map<String, Any>?) = parentValue?.let { HashMap(it) }
    }

    val keys: Set<Any>?
        get() = inheritableThreadLocal.get()?.keys

    fun put(key: String, value: Any) =
        inheritableThreadLocal.get()?.let { it[key] = value }
            ?: inheritableThreadLocal.set(HashMap(mapOf(key to value)))

    fun get(key: String) = inheritableThreadLocal.get()?.get(key)

    fun remove(key: String) = inheritableThreadLocal.get()?.remove(key)

    fun clear() {
        inheritableThreadLocal.get()?.clear()
        inheritableThreadLocal.remove()
    }

    fun getCopyOfContextMap(): Map<String, Any>? = inheritableThreadLocal.get()?.let { HashMap(it) }

    fun setContextMap(contextMap: Map<String, Any>) = inheritableThreadLocal.set(HashMap(contextMap))
}
