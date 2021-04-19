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
import java.lang.reflect.Method
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass
import kotlin.reflect.full.allSuperclasses
import kotlin.reflect.full.declaredFunctions
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.isSuperclassOf
import kotlin.reflect.full.valueParameters
import kotlin.reflect.jvm.javaMethod

/**
 * This class registers event tracer consumer and finds (and caches) the functions in those
 * consumer to call (by inspection) when events need to be handled.
 */
class TraceEventConsumerCollection {

    fun add(traceEventConsumer: TraceEventConsumer) {
        traceEventsConsumers.add(traceEventConsumer)
    }

    fun remove(traceEventConsumer: TraceEventConsumer) {
        traceEventsConsumers.remove(traceEventConsumer)
    }

    fun all(): Iterable<TraceEventConsumer> {
        return traceEventsConsumers.asIterable()
    }

    suspend fun consumeTraceEvent(traceEvent: TraceEvent) {

        // Trace events are offered to every handler that can handle the event.
        for (traceEventConsumer in traceEventsConsumers) {

            /**
             * Check what type of trace event consumer we're dealing with. There are two types of
             * consumer: generic trace event handlers, that will be offered all events always, and
             * [TraceEventListener]-derived trace event consumer, that can only deal with the events they
             * define as implemented functions.
             */
            if (traceEventConsumer is GenericTraceEventConsumer) {
                traceEventConsumer.consumeTraceEvent(traceEvent)
            } else {
                handleSpecificConsumer(traceEventConsumer, traceEvent)
            }
        }
    }

    private suspend fun handleSpecificConsumer(
        traceEventConsumer: TraceEventConsumer,
        traceEvent: TraceEvent
    ) {
        try {
            val traceEventListener = Class.forName(traceEvent.interfaceName).kotlin
            if (traceEventListener.isSubclassOf(TraceEventListener::class)) {
                // Do not log an error message when the consumer is not a subclass of the trace
                // event listener type.
                if (traceEventConsumer::class.isSubclassOf(traceEventListener)) {

                    @Suppress("UNCHECKED_CAST")
                    traceEventListener as KClass<out TraceEventListener>
                    val method = findAndCacheFunction(
                        traceEventConsumer::class,
                        traceEventListener,
                        traceEvent.eventName,
                        traceEvent.args.size
                    )
                    if (method != null) {
                        try {
                            method.invoke(traceEventConsumer, *traceEvent.args)
                        } catch (e: Exception) {

                            // Catch all exceptions, to avoid killing the event processor.
                            TraceLog.log(
                                LogLevel.ERROR, TAG, "Cannot invoke consumer, " +
                                    "method=${method.name}, " +
                                    "args=(${traceEvent.args.joinToString()})", e
                            )
                        }
                    } else {
                        TraceLog.log(
                            LogLevel.ERROR, TAG, "Method not found, " +
                                "traceEventConsumer=${traceEventConsumer::class}, " +
                                "traceEventListener=$traceEventListener, " +
                                "traceEvent.functionName=${traceEvent.eventName}, " +
                                "traceEvent.args.size=${traceEvent.args.size}"
                        )
                    }
                }
            } else {

                // Error: the trace event was not from a [TraceEvents]-derived class.
                TraceLog.log(
                    LogLevel.ERROR,
                    TAG,
                    "Event is not a subclass of ${TraceEventListener::class}, " +
                        "traceEventListener=${traceEvent.interfaceName}"
                )
            }

            // Catch (unexpected) exceptions, just to avoid killing the event processor.
        } catch (e: ClassNotFoundException) {
            TraceLog.log(
                LogLevel.ERROR, TAG, "Class not found, " +
                    "traceEventListener=${traceEvent.interfaceName}", e
            )
        } catch (e: LinkageError) {
            TraceLog.log(
                LogLevel.ERROR, TAG, "Linkage error, " +
                    "traceEventListener=${traceEvent.interfaceName}", e
            )
        } catch (e: ExceptionInInitializerError) {
            TraceLog.log(
                LogLevel.ERROR, TAG, "Initialization error, " +
                    "traceEventListener=${traceEvent.interfaceName}", e
            )
        }
    }

    private suspend fun findFunctionByInspection(
        traceEventsConsumerClass: KClass<*>,
        traceEventListenerInterface: KClass<out TraceEventListener>,
        traceEventFunction: String,
        nrArgs: Int
    ): Method? {

        // First, check if the function is defined in the interface of the tracer.
        if (traceEventsConsumerClass.isSuperclassOf(traceEventListenerInterface)) {
            traceEventsConsumerClass.declaredFunctions
                .filter { it.name == traceEventFunction && it.valueParameters.size == nrArgs }
                .map { it.javaMethod }
                .firstOrNull()?.let { return it }
        }

        // Otherwise, recursively find the method from a super class.
        traceEventsConsumerClass.allSuperclasses.mapNotNull {
            findFunctionByInspection(
                it,
                traceEventListenerInterface,
                traceEventFunction,
                nrArgs
            )
        }
            .firstOrNull()?.let { return it }

        // Or fail, if the method isn't found.
        return null
    }

    private suspend fun findAndCacheFunction(
        traceEventConsumerClass: KClass<out TraceEventConsumer>,
        traceEventListenerInterface: KClass<out TraceEventListener>,
        traceEventFunction: String,
        nrArgs: Int
    ): Method? {

        // First, try the cache.
        val key = Key(
            traceEventConsumerClass, traceEventListenerInterface, traceEventFunction,
            nrArgs
        )
        traceEventFunctions[key]?.let { return it }

        // Otherwise, try inspection.
        findFunctionByInspection(
            traceEventConsumerClass,
            traceEventListenerInterface,
            traceEventFunction,
            nrArgs
        )?.let {

            // And remember in cache.
            traceEventFunctions[key] = it
            return it
        }

        // Or fail.
        return null
    }

    // Key for cached map of trace event functions.
    private data class Key(
        val traceEventConsumerClass: KClass<out TraceEventConsumer>,
        val traceEventListenerInterface: KClass<out TraceEventListener>,
        val traceEventFunction: String,
        val nrArgs: Int
    )

    /**
     * The set of consumers needs to be thread-safe as it is used from both the caller
     * thread and the event processor thread. For lack of a ConcurrentSet, we use
     * the keys set of a [ConcurrentHashMap].
     */
    private val traceEventsConsumers = ConcurrentHashMap.newKeySet<TraceEventConsumer>()

    /**
     * The cached event functions are only used on the event processor thread.
     * No need to be thread-safe.
     */
    private val traceEventFunctions = HashMap<Key, Method>()

    companion object {
        val TAG = TraceEventConsumerCollection::class.simpleName!!
    }
}
