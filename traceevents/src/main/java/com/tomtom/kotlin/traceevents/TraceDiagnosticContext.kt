package com.tomtom.kotlin.traceevents


/**
 * ThreadLocal trace context, basically copied from the basic MDC implementation
 */
object TraceDiagnosticContext {

    private val inheritableThreadLocal: InheritableThreadLocal<MutableMap<String, Any>?> = object : InheritableThreadLocal<MutableMap<String, Any>?>() {
        fun childValue(parentValue: Map<String, Any>?): Map<String, Any>? {
            return parentValue?.let { HashMap(it) }
        }
    }

    fun put(key: String, value: Any) {
        var map = inheritableThreadLocal?.get()
        if (map == null) {
            map = HashMap()
            inheritableThreadLocal.set(map)
        }
        map[key] = value
    }

    fun get(key: String): Any? {
        val map = inheritableThreadLocal?.get()
        return map?.get(key)
    }

    fun remove(key: String) {
        val map = inheritableThreadLocal?.get()
        map?.remove(key)
    }

    fun clear() {
        val map = inheritableThreadLocal.get()
        map?.clear()
        inheritableThreadLocal.remove()
    }

    val keys: Set<Any>?
        get() {
            val map = inheritableThreadLocal.get()
            return map?.keys
        }

    fun getCopyOfContextMap(): Map<String, Any>? {
        return inheritableThreadLocal.get()?.let { HashMap(it) }
    }

    fun setContextMap(contextMap: Map<String, String>) {
        inheritableThreadLocal.set(HashMap(contextMap))
    }
}
