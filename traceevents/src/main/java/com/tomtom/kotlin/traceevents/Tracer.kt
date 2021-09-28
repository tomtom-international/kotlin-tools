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
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import java.io.PrintWriter
import java.io.StringWriter
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.jvm.jvmName
import kotlin.reflect.jvm.kotlinFunction


/**
 * GENERAL DOCUMENTATION ON TRACE EVENTS
 * -------------------------------------
 *
 * See [/docs/logging-and-tracing.md]
 *
 * TRACE EVENTS CONSUMERS
 * ----------------------
 *
 * There are 2 types of trace events consumers:
 *
 * 1. Generic trace event consumers, derived both from [GenericTraceEventConsumer] and from a
 *  [TraceEventListener] interface.
 *
 * These consumers receive every event thrown in the system. They receive the event information
 * as part of their [GenericTraceEventConsumer.consumeTraceEvent] implementation.
 *
 * Generic consumers typically forward events to another system, such as the Android [TraceLog], store
 * them in a database, or perhaps even send them across application (or machine) boundaries.
 *
 * 2. Specific trace event consumers, that implement a specific [TraceEventListener] interface.
 *
 * For example, you could implement the `MyTraceEvents` interface (see above) in a class called
 * `MyTraceEventsConsumer` and register it as a trace events consumer. From then on, whenever
 * a function from the MyTraceEvents interface is called, the corresponding implementation in
 * `MyTraceEventsConsumer` will be called (asynchronously).
 *
 * Specific consumers typically provide specific handler code for specific events. They react
 * on specific events, rather than forward them. For example, switching on a red light on an
 * alarm dashboard, when the event `temperatureTooHigh()` is received.
 *
 * ADVANCED EXAMPLES
 * -----------------
 *
 * Advanced examples of using this trace event mechanism are:
 *
 * - Sending events to a simulation system, which simulates the environment of the system.
 * For example, an event that the cabin temperature has been set, may be processed by a
 * simulator which uses a trace event consumer to receive such messages.
 *
 * - Displaying events on a dashboard, to gain more insight in the current status of the system,
 * rather than having only a scrolling log to look at.
 *
 * - Collecting or sending system usage data for analytics. Developers can define all sorts of
 * semantic events, and the system may collect them easily in a database, for later processing.
 *
 * HOW TO USE THE TRACER (FACTORY) CLASS
 * -------------------------------------
 *
 * The [Tracer.Factory] creates a `tracer` object that implements the event trace functions defined
 * in a [TraceEventListener] interface. The function implementations send the serialized function
 * call and arguments to an event queue, to be processed asynchronously.
 *
 * The event queue is processed asynchronously by trace event consumers (as a co-routine)
 * to make sure their processing never blocks the main thread. Processing of trace events may
 * be throttled to make sure sending many threads in succession does not overload the system.
 * If the event queue overflows, events are lost, rather than blocking the system. (Note that
 * if the event queue overflows, something weird is going on like sending 1000s of messages per
 * second - which you probably shouldn't do; note that this situation is logged to Android [TraceLog]).
 *
 * The events processor is enabled at start-up, but may be suspended at any time using
 * [Tracer.enableTraceEventLogging]. When the event processor is suspended, trace events in the
 * event queue are discarded and lost until the event processor is enabled again and new events
 * are processed again.
 *
 * Example of usage:
 *
 * ```
 * class MyClass {
 *
 *     // Create an event logger for events. The events are defined in interface Tracer.
 *     private val tracer = Tracer.Factory.create<MyClassEvents>(this::class)
 *
 *     // Define a type-safe event interface (no implementation required).
 *     interface MyClassEvents : TraceEventListener {
 *         fun routePlanned(origin: Location, destination: Location, user: User?)
 *     }
 *
 *     // Now, you can trace (log) the event in a function like this:
 *     fun someFunction() {
 *         ...
 *
 *         // This asynchronously sends the event to the event processor. The event processor
 *         // sends it to trace event handlers. By default, the logging event handler is provided,
 *         // which just sends the event to the system log.
 *
 *         tracer.routePlanned(from, to, loggedInUser)
 *
 *         ...
 *     }
 * }
 * ```
 *
 * DISAMBIGUATION OF TRACE EVENT TRACERS
 * -------------------------------------
 *
 * Sometimes multiple tracers may exist for a single class (if multiple instances of the tracer are initiated).
 * In those cases, it may be necessary to be able disambiguate the tracer that the trace events came from.
 * This is solved by adding a `context` string to the `create` method. This context string is passed to
 * trace event consumers. Alternatively, trace event consumers can specify a regular expression to make sure
 * they only get the trace events for the specified tracer context(s).
 *
 * ```
 * // Declare 2 tracers.
 * val tracerMain = Tracer.Factory.create<SomeClass>(this::class, "main loop")
 * val tracerSec  = Tracer.Factory.create<SomeClass>(this::class, "secondary")
 *
 * // Declare a consumer for main loop events only.
 * val consumerSpecific = MyEventConsumerForSomeClass()
 * Tracer.addTraceEventConsumer(consumerSpecific, Regex("main.*"));
 *
 * // Declare a consumer for all events.
 * val consumerAll = MyEventConsumerForSomeClass()
 * Tracer.addTraceEventConsumer(consumerAll);
 * ```
 *
 * Note that only `GenericTraceEventConsumer`s are able to retrieve the context passed by the tracer (as it is
 * part of the `TraceEvent` data object. Specific `TraceEventConsumer`s (that implement the original tracer
 * interface), cannot access the context.
 *
 */
class Tracer private constructor(
    private val tracerClassName: String,
    private val taggingClassName: String,
    private val context: String
) : InvocationHandler {
    private val logTag = stripPackageFromClassName(tracerClassName)
    private val parameterNamesCache: ConcurrentHashMap<Method, Array<String>> = ConcurrentHashMap()

    class Factory {
        companion object {

            /**
             * Get an event logger for a specific class. Should normally be created once per class
             * rather than per instance. I.e., it should normally be created from within a class's
             * `companion object`.
             *
             * @param T The interface type containing the events that may be traced.
             *
             * @param taggingObject Owner object of the class using the event logger,
             *     specified as `this` from the companion object or an instance.
             * @param context Optional string to disambiguate tracers with the same tagging object.
             * @return [TraceEventListener]-derived object, normally called the "tracer", to be used
             *     as `tracer.someEvent()`.
             */
            inline fun <reified T : TraceEventListener> create(
                taggingObject: Any,
                context: String = ""
            ) =
                createForListener_internal<T>(
                    tracerClassName = getTraceClassName_internal(Throwable()),
                    taggingClass = taggingObject::class,
                    traceEventListener = T::class,
                    context = context
                )

            /**
             * Same as [create], but does not specify any event handlers. Only log functions
             * 'v', 'd', 'i', 'w' and 'e' are allowed.
             *
             * Must be inline to make sure the [Throwable] is created at the correct source code
             * location.
             */
            @Suppress("NOTHING_TO_INLINE")
            inline fun createLoggerOnly(taggingObject: Any, context: String = "") =
                createForListenerAndLogger_internal<TraceEventListener>(
                    tracerClassName = getTraceClassName_internal(Throwable()),
                    taggingClass = taggingObject::class,
                    traceEventListener = TraceEventListener::class,
                    isLoggerOnly = true,
                    context = context
                )

            /**
             * Helper function to get the creator class name.
             * Called by inline function (must be public).
             */
            fun getTraceClassName_internal(throwable: Throwable) =
                throwable.stackTrace[0].className.replace("\$Companion", "")

            /**
             * Helper function to create event listener.
             * Called by inline function (must be public).
             */
            fun <T : TraceEventListener> createForListener_internal(
                tracerClassName: String,
                taggingClass: KClass<*>,
                traceEventListener: KClass<out TraceEventListener>,
                context: String
            ): T = createForListenerAndLogger_internal<T>(
                tracerClassName = tracerClassName,
                taggingClass = taggingClass,
                traceEventListener = traceEventListener,
                isLoggerOnly = false,
                context = context
            )

            /**
             * Method to create tracer for listener.
             * Called by inline function (must be public).
             *
             * @param isLoggerOnly Specifies the tracer was explicitly created with the
             * [createLoggerOnly] function.
             */
            @Suppress("UNCHECKED_CAST")
            fun <T : TraceEventListener> createForListenerAndLogger_internal(
                tracerClassName: String,
                taggingClass: KClass<*>,
                traceEventListener: KClass<out TraceEventListener>,
                isLoggerOnly: Boolean,
                context: String
            ): T {
                require(isLoggerOnly || traceEventListener != TraceEventListener::class) {
                    "Derive an interface from TraceEventListener, or use createLoggerOnly()"
                }
                val taggingClassTopLevel =
                    if (taggingClass.isCompanion) {
                        // Don't use the companion object class, but the top-level class.
                        taggingClass.javaObjectType.enclosingClass?.kotlin!!
                    } else {
                        taggingClass.javaObjectType.kotlin
                    }
                return Proxy.newProxyInstance(
                    traceEventListener.java.classLoader,
                    arrayOf<Class<*>?>(traceEventListener.java),
                    Tracer(
                        tracerClassName = tracerClassName,
                        taggingClassName = taggingClassTopLevel.jvmName,
                        context = context
                    )
                ) as T
            }
        }
    }

    /**
     * This is the 'invoke' function that gets called whenever the interface of an event logger is
     * called. The invoke function itself needs to return as soon as possible and cause little
     * overhead for the system. Any actions taken as a result of the event should be scheduled onto
     * another thread (and throttled if needed).
     *
     * @param proxy Proxied object.
     * @param method Method being called.
     * @param args Additional arguments to method, may be null.
     * @return Always null; the signature of events functions must be void/Unit.
     */
    override fun invoke(proxy: Any, method: Method, args: Array<Any?>?): Any? {

        /**
         * The [proxy] is always a [TraceEventListener] as [Factory.create] creates a proxy
         * object for a subclass of that interface.
         */
        proxy as TraceEventListener
        val logLevelAnnotation = logLevelFromAnnotation(method)
        val includeExceptionStackTraceAnnotation = includeExceptionStackTraceFromAnnotation(method)
        val includeTaggingClassAnnotation = includeTaggingClassAnnotation(method)
        val includeFileLocationAnnotation = includeFileLocationAnnotation(method)
        val includeEventInterfaceAnnotation = includeEventInterfaceAnnotation(method)

        /**
         * Skip event when the method is a standard (possibly auto-generated) class method.
         * Methods like `toString()` may be implicitly called in a debugger session. Don't search
         * for consumers for those.
         */
        if (!enabled || method.name in arrayOf("equals", "hashCode", "toString")) {
            return null
        }

        /**
         * Get the parameter names for the method.
         * For performance reasons, make sure this code is normally executed only once per method call. Note that
         * this method may be called by multiple threads, so it must be thread-safe as well.
         * Also make sure the parameter names are only provided if there is exactly 1 name for each parameter only.
         */
        val parameterNames = parameterNamesCache[method] ?: method.kotlinFunction
            ?.parameters
            ?.mapNotNull { it.name }
            ?.takeIf { it.size == args?.size }
            ?.toTypedArray()
            ?.also { parameterNamesCache[method] = it }

        // Send the event to the event processor consumer, non-blocking.
        val now = LocalDateTime.now()
        val event = TraceEvent(
            dateTime = now,
            logLevel = logLevelAnnotation,
            tracerClassName = tracerClassName,
            taggingClassName = taggingClassName,
            context = context,
            traceThreadLocalContext = TraceThreadLocalContext.getCopyOfContextMap(),
            interfaceName = method.declaringClass.name,
            stackTraceHolder = Throwable(),
            eventName = method.name,
            args = args ?: arrayOf(),
            parameterNames = parameterNames
        )

        /**
         * Log the event to the standard logger here, rather than on a consumer thread, to make
         * sure the order of events and other log messages remains logical.
         */
        if (loggingMode == LoggingMode.SYNC) {
            val logLevel = simpleLogFunctionNameToLogLevel(event.eventName)
            if (logLevel != null) {
                if (!useSimpleLogFunction(logLevel, logTag, args)) {

                    // Signal the listener an incorrect signature was found.
                    proxy.incorrectLogSignatureFound()
                }
            } else {

                // Only format the message for non-standard Log events. Use the annotated log level.
                TraceLog.log(
                    logLevelAnnotation, logTag,
                    "event=${
                        createLogMessage(
                            event,
                            includeTime = false,
                            includeExceptionStackTrace = includeExceptionStackTraceAnnotation,
                            includeTaggingClass = includeTaggingClassAnnotation,
                            includeFileLocation = includeFileLocationAnnotation,
                            includeEventInterface = includeEventInterfaceAnnotation
                        )
                    }"
                )
            }
        }
        offerTraceEvent(event, now)
        return null
    }

    // Helpers to get the annotation values. Function level overrides interface level.
    private fun logLevelFromAnnotation(method: Method) =
        method.getDeclaredAnnotation(TraceLogLevel::class.java)?.logLevel
            ?: method.declaringClass.getDeclaredAnnotation(TraceLogLevel::class.java)?.logLevel
            ?: LogLevel.DEBUG

    private fun includeExceptionStackTraceFromAnnotation(method: Method) =
        method.getDeclaredAnnotation(TraceOptions::class.java)?.includeExceptionStackTrace
            ?: method.declaringClass.getDeclaredAnnotation(TraceOptions::class.java)?.includeExceptionStackTrace
            ?: true

    private fun includeTaggingClassAnnotation(method: Method) =
        method.getDeclaredAnnotation(TraceOptions::class.java)?.includeTaggingClass
            ?: method.declaringClass.getDeclaredAnnotation(TraceOptions::class.java)?.includeTaggingClass
            ?: false

    private fun includeFileLocationAnnotation(method: Method) =
        method.getDeclaredAnnotation(TraceOptions::class.java)?.includeFileLocation
            ?: method.declaringClass.getDeclaredAnnotation(TraceOptions::class.java)?.includeFileLocation
            ?: false

    private fun includeEventInterfaceAnnotation(method: Method) =
        method.getDeclaredAnnotation(TraceOptions::class.java)?.includeEventInterface
            ?: method.declaringClass.getDeclaredAnnotation(TraceOptions::class.java)?.includeEventInterface
            ?: false

    /**
     * Offer [event] to the processing queue. If trace logging is set to [LoggingMode.SYNC], this
     * function also outputs the trace using a logger, in this thread.
     */
    private fun offerTraceEvent(
        event: TraceEvent,
        now: LocalDateTime?
    ) {
        if (!traceEventChannel.trySend(event).isSuccess) {
            when (loggingMode) {
                LoggingMode.SYNC ->

                    // Don't repeat the event if it was logged already by the logger. If the event
                    // was a simple log event, don't even mention the overflow (not useful).
                    if (simpleLogFunctionNameToLogLevel(event.eventName) == null) {
                        TraceLog.log(
                            LogLevel.DEBUG,
                            logTag,
                            "Event lost, event=(see previous line)"
                        )
                    }

                LoggingMode.ASYNC ->

                    // Only format the message for lost events that weren't logged already.
                    TraceLog.log(
                        LogLevel.WARN,
                        logTag,
                        "Event lost, event=${
                            createLogMessage(
                                event,
                                includeTime = true,
                                includeExceptionStackTrace = true,
                                includeTaggingClass = true,
                                includeFileLocation = true,
                                includeEventInterface = true
                            )
                        }"
                    )
            }
            ++nrLostTraceEventsSinceLastMsg
            ++nrLostTraceEventsTotal
        }

        // If we lost events, write a log message to indicate so, but at most once every x seconds.
        if (nrLostTraceEventsSinceLastMsg > 0 &&
            timeLastLostTraceEvent.plusSeconds(LIMIT_WARN_SECS).isBefore(now)
        ) {
            TraceLog.log(
                LogLevel.WARN,
                logTag,
                "Trace event channel is full, " +
                    "nrLostTraceEventsSinceLastMsg=$nrLostTraceEventsSinceLastMsg, " +
                    "nrLostTraceEventsTotal=$nrLostTraceEventsTotal"
            )
            nrLostTraceEventsSinceLastMsg = 0L
            timeLastLostTraceEvent = now
        }
    }

    /**
     * Trace event handler that writes events to standard logger. This logger is supplied as
     * a default trace event handler for asynchronous logging. It is enabled with
     * [Tracer.setTraceEventLoggingMode].
     */
    internal class LoggingTraceEventConsumer : GenericTraceEventConsumer {

        override suspend fun consumeTraceEvent(traceEvent: TraceEvent) {
            val tagOwnerClass = stripPackageFromClassName(traceEvent.taggingClassName)
            val logLevel = simpleLogFunctionNameToLogLevel(traceEvent.eventName)
            if (logLevel != null) {

                // Don't reformat the message if this is a standard log message.
                if (!useSimpleLogFunction(logLevel, tagOwnerClass, traceEvent.args)) {

                    // Signal the listener an incorrect signature was found.
                    incorrectLogSignatureFound()
                }
            }
        }
    }

    companion object {
        private val TAG = Tracer::class.simpleName!!
        private const val STACK_TRACE_DEPTH = 5L

        /**
         * Names of simple, predefined log functions (from standard loggers).
         */
        private const val FUN_VERBOSE = "v"
        private const val FUN_DEBUG = "d"
        private const val FUN_INFO = "i"
        private const val FUN_WARN = "w"
        private const val FUN_ERROR = "e"

        /**
         * Set to true to start processing, false to discard events.
         */
        internal var enabled = true

        /**
         * Specifies whether logging trace events should be done on the caller's thread or not.
         */
        enum class LoggingMode { SYNC, ASYNC }

        internal var loggingMode = LoggingMode.SYNC

        /**
         * Default handler for asynchronous logging.
         */
        private val loggingTraceEventConsumer = LoggingTraceEventConsumer()

        /**
         * The co-routine scope of the event processor is internal, not private, to
         * increase testability of this module. For example, in test cases you may want
         * to set this to the co-routine scope of the test cases.
         */
        internal var eventProcessorScope = CoroutineScope(Dispatchers.IO)

        /**
         * This is the event processor job, running in co-routine scope
         * [eventProcessorScope].
         */
        private lateinit var eventProcessorJob: Job

        /**
         * Capacity definition for the channel that queues traces.
         */
        private const val CHANNEL_CAPACITY = 10000
        internal val traceEventChannel = Channel<TraceEvent>(CHANNEL_CAPACITY)
        private val traceEventConsumers = TraceEventConsumerCollection()

        /**
         * Parameters to indicate and deal with loss of traces.
         */
        private const val LIMIT_WARN_SECS = 10L
        private var nrLostTraceEventsSinceLastMsg = 0L
        private var nrLostTraceEventsTotal = 0L
        private var timeLastLostTraceEvent = LocalDateTime.now().minusSeconds(LIMIT_WARN_SECS)

        /**
         * Registry of [toString] functions for classes, to be used instead of their own variants.
         */
        private val toStringRegistry = mutableMapOf<String, Any.() -> String>()

        /**
         * Register a handler for the [toString] method of a class.
         */
        inline fun <reified T : Any> registerToString(noinline toStringFun: T.() -> String) {
            addToRegisteredFunctions(T::class, toStringFun)
        }

        @Suppress("UNCHECKED_CAST")
        fun <T> addToRegisteredFunctions(clazz: KClass<*>, toStringFun: T.() -> String) {
            toStringRegistry[clazz.toString()] = toStringFun as Any.() -> String
        }

        /**
         * Reset all [toString] handlers.
         */
        fun resetToDefaults() {
            toStringRegistry.clear()
            registerToString<Array<*>> { "[${joinToString()}]" }
        }

        /**
         * Add a trace event consumer for trace processing. This also starts the event processor
         * if it wasn't started already. (No need for the processor to run if there are no
         * consumers.)
         *
         * @param traceEventConsumer Trace event consumer to receive trace events.
         * @param contextRegex Regular expression to filter trace events. Only events from tracers with a context
         * that matches the regular expression will be received. If omitted, or `null`, all events will be received.
         */
        fun addTraceEventConsumer(
            traceEventConsumer: TraceEventConsumer,
            contextRegex: Regex? = null
        ) {
            traceEventConsumers.add(traceEventConsumer, contextRegex)

            /**
             * The event processor is started only when the first consumer is registered and stays
             * active for the lifetime of the application.
             */
            if (!::eventProcessorJob.isInitialized || eventProcessorJob.isCancelled) {
                eventProcessorJob =
                    eventProcessorScope.launch(CoroutineName("processTraceEvents")) {
                        processTraceEvents()
                    }
            }
        }

        /**
         * Remove a trace event consumer. The event processor should not be stopped if there are
         * no consumers left, because it holds the event queue and you may lose events then.
         *
         * @param traceEventConsumer Trace event consumer to remove.
         * @param contextRegex If this is `null`, all trace event consumers for the same tracers will be removed.
         * Otherwise only the specific trace event consumer for that regular expression is removed.
         */
        fun removeTraceEventConsumer(
            traceEventConsumer: TraceEventConsumer,
            contextRegex: Regex? = null
        ) {
            traceEventConsumers.remove(traceEventConsumer, contextRegex)
        }

        /**
         * Remove all event consumers.
         *
         * @param contextRegex If this is `null`, all trace event consumers will be removed. Otherwise,
         * trace event consumer for specific contexts are removed.
         */
        fun removeAllTraceEventConsumers(contextRegex: Regex? = null) {
            traceEventConsumers.all().asSequence()
                .forEach { removeTraceEventConsumer(it, contextRegex) }
        }

        /**
         * Blocking call to eat all events from the event queue until empty. The event processor is
         * temporarily disabled (and reactivated afterwards).
         */
        suspend fun flushTraceEvents() {

            // Suspend processor, start discarding events.
            val wasEnabled = enabled
            enabled = false

            eventProcessorScope.launch(CoroutineName("flushTraceEvents")) {
                while (traceEventChannel.tryReceive().getOrNull() != null) {
                    // Loop until queue is empty.
                }
            }.join()

            // Re-activate event processor if needed.
            enabled = wasEnabled
        }

        /**
         * Enable or disable trace event logging altogether.
         */
        fun enableTraceEventLogging(enable: Boolean) {
            enabled = enable
        }

        /**
         * Set trace logging to sync (events are logged to logger in same thread as event caller)
         * or async (events are queued and logged in a separate thread).
         * Default is async, so event processing doesn't get in the way of the caller thread.
         */
        fun setTraceEventLoggingMode(loggingMode: LoggingMode) {
            this.loggingMode = loggingMode
            when (loggingMode) {
                LoggingMode.ASYNC -> addTraceEventConsumer(loggingTraceEventConsumer)
                LoggingMode.SYNC -> removeTraceEventConsumer(loggingTraceEventConsumer)
            }
        }

        /**
         * This internal method cancels the event processor to assist in the testability of
         * this module. It cancels the event processor, which will be restarted when a
         * new consumer is added.
         */
        internal suspend fun cancelAndJoinEventProcessor() {
            if (::eventProcessorJob.isInitialized) {
                eventProcessorJob.cancelAndJoin()
            }
            TraceLog.log(LogLevel.DEBUG, TAG, "Cancelled trace event processor")
        }

        /**
         * Event processor which takes elements from the event queue and processes them one by one.
         * Event queue handling runs in same scope as [flushTraceEvents].
         */
        private suspend fun processTraceEvents() {
            TraceLog.log(LogLevel.DEBUG, TAG, "Started trace event processor")
            for (traceEvent in traceEventChannel) {

                /**
                 * If the event processor is disabled, it simply discards all events
                 * from the queue, until it is enabled again.
                 */
                if (enabled) {
                    traceEventConsumers.consumeTraceEvent(traceEvent)
                }
            }
        }

        internal fun useSimpleLogFunction(
            logLevel: LogLevel,
            tag: String,
            args: Array<Any?>?
        ): Boolean {

            /**
             * The first 2 arguments of a log function must be String and Throwable. This
             * can only happen if you override the log functions with an incorrect signature.
             * Nevertheless, we want to be robust against this sort of mistake as we cannot
             * resolve this compile-time.
             */
            if (args == null || args.isEmpty() ||
                args[0] == null || args[0]!!::class != String::class ||
                (args.size == 2 &&
                    args[1] != null && !args[1]!!::class.isSubclassOf(Throwable::class)) ||
                args.size > 2
            ) {
                TraceLog.log(
                    LogLevel.ERROR,
                    TAG,
                    "Incorrect log call, expected arguments (String, Throwable), " +
                        "args=${
                            args?.joinToString {
                                it?.let {
                                    it.javaClass.simpleName + ":" + it.toString()
                                } ?: "null"
                            }
                        }"
                )
                return false
            }
            val message = args[0] as String
            val e: Throwable? = args.let {
                if (args.size == 2 && args[1] != null) args[1] as Throwable else null
            }
            TraceLog.log(logLevel, tag, message, e)
            return true
        }

        internal fun simpleLogFunctionNameToLogLevel(name: String): LogLevel? =
            when (name) {
                FUN_VERBOSE -> LogLevel.VERBOSE
                FUN_DEBUG -> LogLevel.DEBUG
                FUN_INFO -> LogLevel.INFO
                FUN_WARN -> LogLevel.WARN
                FUN_ERROR -> LogLevel.ERROR
                else -> null
            }

        internal fun stripPackageFromClassName(ownerClassName: String): String {
            val indexPeriod = ownerClassName.lastIndexOf('.')
            if (indexPeriod >= 0 && ownerClassName.length > indexPeriod) {
                return ownerClassName.substring(indexPeriod + 1)
            } else {
                return ownerClassName
            }
        }

        internal fun convertToStringUsingRegistry(item: Any?) =
            if (item == null) {
                "null"
            } else {
                // Try registered function first.
                toStringRegistry[item::class.toString()]?.invoke(item)
                    ?: if (item.javaClass.getMethod("toString").declaringClass == Any::class.java) {

                        // Only default [toString] is available, from [Any].
                        "${item.javaClass.simpleName}(...)"
                    } else {

                        // Use [toString] as defined in class itself.
                        item.toString()
                    }
            }

        /**
         * Create a trace event message that can be logged.
         */
        internal fun createLogMessage(
            traceEvent: TraceEvent,
            includeTime: Boolean,
            includeExceptionStackTrace: Boolean,
            includeTaggingClass: Boolean,
            includeFileLocation: Boolean,
            includeEventInterface: Boolean
        ): String {
            val sb = StringBuilder()

            // Timestamp.
            if (includeTime) {
                sb.append("[${traceEvent.dateTime.format(DateTimeFormatter.ISO_DATE_TIME)}] ")
            }

            // Event.
            sb.append(
                "${traceEvent.eventName}(${
                    traceEvent.args.joinToString {
                        convertToStringUsingRegistry(it)
                    }
                })"
            )

            // Called-from file location.
            if (includeFileLocation) {
                val fileLocation = if (traceEvent.stackTraceHolder != null) {
                    getSourceCodeLocation(traceEvent.stackTraceHolder)
                } else {
                    "unavailable"
                }
                sb.append(", fileLocation=$fileLocation")
            }

            // Source class name.
            if (includeTaggingClass) {
                sb.append(", taggingClass=${stripPackageFromClassName(traceEvent.taggingClassName)}")
            }

            // Event interface name.
            if (includeEventInterface) {
                sb.append(", eventInterface=${traceEvent.interfaceName}")
            }

            // Context.
            if (traceEvent.context.isNotEmpty()) {
                sb.append(", context=${traceEvent.context}")
            }

            // Thread-local context
            if (traceEvent.traceThreadLocalContext != null) {
                sb.append(", traceThreadLocalContext=${traceEvent.traceThreadLocalContext}")
            }

            // Stack trace for last parameter, if it's an exception.
            if (includeExceptionStackTrace && traceEvent.args.isNotEmpty()) {
                (traceEvent.args.last() as? Throwable)?.let {
                    sb.append("\n")
                    sb.append(formatThrowable(it, includeExceptionStackTrace))
                }
            }
            return sb.toString()
        }

        /**
         * Format a [Throwable]
         */
        internal fun formatThrowable(e: Throwable, logStackTrace: Boolean) =
            if (logStackTrace) {
                val stringWriter = StringWriter()
                val printWriter = PrintWriter(stringWriter)
                e.printStackTrace(printWriter)
                printWriter.flush()
                stringWriter.toString()
            } else {
                e.message
            }

        private fun getSourceCodeLocation(stackTraceHolder: Throwable): String {

            /**
             * For Java 9, we suggest the following optimization:
             *
             * Use the [StackWalker] instead of [Thread.getStackTrace], because it's much, much
             * faster to not get all frames.
             *
             * Get the stack up to a maximum of [STACK_TRACE_DEPTH] levels deep. Our caller must be
             * in those first calls. If it's not, the unit tests fails and informs the developer
             * the reconsider the value of [STACK_TRACE_DEPTH].
             *
             * <pre>
             *     val stack = StackWalker.getInstance().walk {
             *         frames -> frames.limit(STACK_TRACE_DEPTH).collect(Collectors.toList())
             * </pre>
             *
             * For now, use the slower Java 8 version:
             */
            val stack = stackTraceHolder.stackTrace

            // Find our own 'invoke' function call on the stack that called this function.
            var i = 0
            while (i < stack.size &&
                stack[i].className != "com.tomtom.kotlin.traceevents.Tracer" &&
                stack[i].methodName != "invoke"
            ) i++

            // The function call 2 levels deeper is the actual caller function.
            return if (i < stack.size - 2) {

                /**
                 * Skip the com.sun.proxy function call and get the info from the next item.
                 * Note that the filename and line number may not be present, e.g. for
                 * generated code.
                 */
                val item = stack[i + 2]
                "${item.fileName ?: "(unknown)"}:${item.methodName}(" +
                    "${if (item.lineNumber >= 0) item.lineNumber.toString() else "unknown"})"
            } else {

                // This shouldn't happen, but we certainly shouldn't throw here.
                "(error: can't find `invoke` function on stack)"
            }
        }

        init {

            // Always switch on synchronous logging by default.
            setTraceEventLoggingMode(LoggingMode.SYNC)
            enableTraceEventLogging(true)
            resetToDefaults()
        }
    }
}
