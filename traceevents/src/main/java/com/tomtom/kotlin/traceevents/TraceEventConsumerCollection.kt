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
 * This class registers event tracer consumers and finds (and caches) the functions in those
 * consumers to call (by inspection) when events need to be handled.
 */
public class TraceEventConsumerCollection {

    /**
     * Add a trace event consumer, possibly for a specific context only (specified as a regex).
     * Trace event consumers are called at most once for a single event (even if they are
     * added multiple times, for different (possibly overlapping) context regex's).
     *
     * @param traceEventConsumer Consumer to add.
     * @param contextRegex If the regex matches the trace event context, the consumer will be called.
     *                     If the regex is null, the consumer gets all events.
     */
    public fun add(traceEventConsumer: TraceEventConsumer, contextRegex: Regex?) {
        traceEventsConsumersWithContext.add(TraceEventConsumerWithContext(traceEventConsumer, contextRegex))
    }


    /**
     * Remove a trace event consumer. The original regex specified when adding the consumer may be
     * specified, to remove the consumer only for a specific context. The caller needs to make sure
     * exactly the same regex is used as when adding the consumer, or the consumer won't be removed.
     *
     * @param traceEventConsumer Consumer to remove.
     * @param contextRegex Only the consumer for the given regex is removed. The consumer is removed
     *                     for all contexts if the regex is null.
     */
    public fun remove(traceEventConsumer: TraceEventConsumer, contextRegex: Regex? = null) {
        traceEventsConsumersWithContext.removeAll {
            it.traceEventConsumer == traceEventConsumer &&
                    (contextRegex == null || it.contextRegex == contextRegex)
        }
    }

    /**
     * List all consumers, for a specific regex. The regex specified has to be exactly the same
     * regex as used when adding the consumer(s).
     *
     * @param contextRegex Regex used when adding the consumer(s), or null to list all consumers.
     */
    public fun all(contextRegex: Regex? = null): Iterable<TraceEventConsumer> {
        return traceEventsConsumersWithContext.asIterable()
            .filter { contextRegex == null || it.contextRegex == contextRegex }
            .map { it.traceEventConsumer }
    }

    /**
     * Send a single trace event to the correct consumers. A single consumer will get a single
     * trace event at most once, regardless of how often the consumer was added.
     *
     * @param traceEvent Trace event to consume.
     */
    public suspend fun consumeTraceEvent(traceEvent: TraceEvent) {
        // Make sure we call each trace event consumer at most once.
        val calledConsumers = mutableSetOf<TraceEventConsumer>()

        // Trace events are offered to every handler that can handle the event.
        for (traceEventConsumerWithContext in traceEventsConsumersWithContext) {

            /**
             * Check what type of trace event consumer we're dealing with. There are two types of
             * consumer: generic trace event handlers, that will be offered all events always, and
             * [TraceEventListener]-derived trace event consumer, that can only deal with the events they
             * define as implemented functions.
             */
            if (!calledConsumers.contains(traceEventConsumerWithContext.traceEventConsumer) &&
                traceEventConsumerWithContext.contextRegex == null ||
                traceEventConsumerWithContext.contextRegex!!.matches(traceEvent.context)
            ) {
                calledConsumers.add(traceEventConsumerWithContext.traceEventConsumer)
                if (traceEventConsumerWithContext.traceEventConsumer is GenericTraceEventConsumer) {
                    traceEventConsumerWithContext.traceEventConsumer.consumeTraceEvent(traceEvent)
                } else {
                    handleSpecificConsumer(traceEventConsumerWithContext.traceEventConsumer, traceEvent)
                }
            }
        }
    }

    private fun handleSpecificConsumer(
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

    private fun findFunctionByInspection(
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

    private fun findAndCacheFunction(
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

    private data class TraceEventConsumerWithContext(
        val traceEventConsumer: TraceEventConsumer,
        val contextRegex: Regex?
    )

    /**
     * The set of consumers needs to be thread-safe as it is used from both the caller
     * thread and the event processor thread. For lack of a ConcurrentSet, we use
     * the keys set of a [ConcurrentHashMap].
     */
    private val traceEventsConsumersWithContext = ConcurrentHashMap.newKeySet<TraceEventConsumerWithContext>()

    /**
     * The cached event functions are only used on the event processor thread.
     * No need to be thread-safe.
     */
    private val traceEventFunctions = HashMap<Key, Method>()

    private companion object {
        val TAG = TraceEventConsumerCollection::class.simpleName!!
    }
}
