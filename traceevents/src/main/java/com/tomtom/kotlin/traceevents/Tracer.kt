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

import com.tomtom.kotlin.traceevents.TraceLog.LogLevel
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import java.io.PrintWriter
import java.io.StringWriter
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

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
 */
class Tracer private constructor(
    private val ownerClassName: String
) : InvocationHandler {

    private val tagOwnerClass = getTagFromOwnerClassName(ownerClassName)

    class Factory {
        companion object {

            /**
             * Get an event logger for a specific class. Should be created once per class rather
             * than instance. I.e., it should be created from within a class's `companion object`.
             *
             * @param T The interface type containing the events that may be traced.
             *
             * @param companionObject Companion object of the class using the event logger,
             *     specified as `this` from the companion object.
             * @return [TraceEventListener]-derived object, normally called the "tracer", to be used
             *     as `tracer.someEvent()`.
             */
            inline fun <reified T : TraceEventListener> create(companionObject: Any) =
                createForListener<T>(companionObject::class, T::class)

            /**
             * Called by reified inline function (must be public).
             */
            fun <T : TraceEventListener> createForListener(
                companionClass: KClass<*>,
                traceEventListener: KClass<out TraceEventListener>
            ): T = createForListenerAndLogger<T>(companionClass, traceEventListener)

            /**
             * Same as [create], but does not specify any event handlers. Only log functions
             * 'v', 'd', 'i', 'w' and 'e' are allowed.
             */
            fun createLoggerOnly(companionObject: Any) =
                createForListenerAndLogger<TraceEventListener>(
                    companionObject::class, TraceEventListener::class, true
                )

            /**
             * Private method to create tracer for listener.
             *
             * @param isLoggerOnly Specifies the tracer was explicitly created with the
             * [createLoggerOnly] function.
             */
            @Suppress("UNCHECKED_CAST")
            private fun <T : TraceEventListener> createForListenerAndLogger(
                companionClass: KClass<*>,
                traceEventListener: KClass<out TraceEventListener>,
                isLoggerOnly: Boolean = false
            ): T {
                require(companionClass.isCompanion) {
                    "Tracers may only be created from companion objects, with `this` to prevent " +
                        "duplicate instances, companionClass=$companionClass"
                }
                require(isLoggerOnly || traceEventListener != TraceEventListener::class) {
                    "Derive an interface from TraceEventListener, or use createLoggerOnly()"
                }

                val ownerClass = companionClass.javaObjectType.enclosingClass?.kotlin!!
                return Proxy.newProxyInstance(
                    ownerClass.java.classLoader,
                    arrayOf<Class<*>?>(traceEventListener.java),
                    Tracer(ownerClass.java.name)
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
        val logLevelAnnotation =
            method.getDeclaredAnnotation(TraceLogLevel::class.java)?.logLevel ?: LogLevel.DEBUG
        val logStackTraceAnnotation =
            method.getDeclaredAnnotation(TraceLogLevel::class.java)?.logStackTrace ?: true
        val includeOwnerClassAnnotation =
            method.getDeclaredAnnotation(TraceLogLevel::class.java)?.includeOwnerClass ?: false

        /**
         * Skip event when the method is a standard (possibly auto-generated) class method.
         * Methods like `toString()` may be implicitly called in a debugger session. Don't search
         * for consumers for those.
         */
        if (!enabled || method.name in arrayOf("equals", "hashCode", "toString")) {
            return null
        }

        // Send the event to the event processor consumer, non-blocking.
        val now = LocalDateTime.now()
        val event = TraceEvent(
            now,
            logLevelAnnotation,
            ownerClassName,
            method.declaringClass.name,
            method.name,
            args ?: arrayOf<Any?>()
        )

        /**
         * Log the event to the standard logger here, rather than on a consumer thread, to make
         * sure the order of events and other log messages remains logical.
         */
        if (loggingMode == LoggingMode.SYNC) {
            val logLevel = simpleLogFunctionNameToLogLevel(event.functionName)
            if (logLevel != null) {
                if (!useSimpleLogFunction(logLevel, tagOwnerClass, args)) {

                    // Signal the listener an incorrect signature was found.
                    proxy.incorrectLogSignatureFound()
                }
            } else {

                // Only format the message for non-standard Log events. Use the annotated log level.
                TraceLog.log(
                    logLevelAnnotation, tagOwnerClass,
                    "event=${createLogMessage(
                        event,
                        includeTime = false,
                        logStackTrace = logStackTraceAnnotation,
                        includeOwnerClass = includeOwnerClassAnnotation
                    )}"
                )
            }
        }
        offerTraceEvent(event, now)
        return null
    }

    /**
     * Offer event to processing queue. If trace logging is set to [LoggingMode.SYNC], this
     * function also outputs the trace using a logger, in this thread.
     */
    private fun offerTraceEvent(
        event: TraceEvent,
        now: LocalDateTime?
    ) {
        if (!traceEventChannel.offer(event)) {
            when (loggingMode) {
                LoggingMode.SYNC ->

                    // Don't repeat the event if it was logged already by the logger. If the event
                    // was a simple log event, don't even mention the overflow (not useful).
                    if (simpleLogFunctionNameToLogLevel(event.functionName) == null) {
                        TraceLog.log(
                            LogLevel.DEBUG,
                            tagOwnerClass,
                            "Event lost, event=(see previous line)"
                        )
                    }

                LoggingMode.ASYNC ->

                    // Only format the message for lost events that weren't logged already.
                    TraceLog.log(
                        LogLevel.WARN,
                        tagOwnerClass,
                        "Event lost, event=${createLogMessage(
                            event,
                            includeTime = true,
                            logStackTrace = true,
                            includeOwnerClass = true
                        )}"
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
                tagOwnerClass,
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
            val tagOwnerClass = getTagFromOwnerClassName(traceEvent.ownerClass)
            val logLevel = simpleLogFunctionNameToLogLevel(traceEvent.functionName)
            if (logLevel != null) {

                // Don't reformat the message if this is a standard log message.
                if (!useSimpleLogFunction(logLevel, tagOwnerClass, traceEvent.args)) {

                    // Signal the listener an incorrect signature was found.
                    incorrectLogSignatureFound()
                }
            } else {
            }
        }
    }

    companion object {
        val TAG = Tracer::class.simpleName!!

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
         *  Specifies if logging trace events should be done on the caller's thread or not.
         */
        enum class LoggingMode { SYNC, ASYNC }

        internal var loggingMode = LoggingMode.SYNC

        /**
         * Default handler for asynchronous logging.
         */
        internal val loggingTraceEventConsumer = LoggingTraceEventConsumer()

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
        internal lateinit var eventProcessorJob: Job

        /**
         * Channel definition for the channel that queues traces.
         */
        private const val CHANNEL_CAPACITY = 10000
        internal val traceEventChannel = Channel<TraceEvent>(CHANNEL_CAPACITY)
        internal val traceEventConsumers = TraceEventConsumerCollection()

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
         */
        fun addTraceEventConsumer(traceEventConsumer: TraceEventConsumer) {
            traceEventConsumers.add(traceEventConsumer)

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
         */
        fun removeTraceEventConsumer(traceEventConsumer: TraceEventConsumer) {
            traceEventConsumers.remove(traceEventConsumer)
        }

        /**
         * Remove all event consumers.
         */
        fun removeAllTraceEventConsumers() {
            traceEventConsumers.all().asSequence().forEach { removeTraceEventConsumer(it) }
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
                while (traceEventChannel.poll() != null) {
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
                        "args=${args?.joinToString {
                            it?.let {
                                it.javaClass.simpleName + ":" + it.toString()
                            } ?: "null"
                        }}"
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

        internal fun getTagFromOwnerClassName(ownerClassName: String): String {
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
            logStackTrace: Boolean,
            includeOwnerClass: Boolean
        ): String {
            val sb = StringBuilder()
            if (includeTime) {
                sb.append("[${traceEvent.dateTime.format(DateTimeFormatter.ISO_DATE_TIME)}] ")
            }
            sb.append(
                "${traceEvent.functionName}(${traceEvent.args.joinToString {
                    convertToStringUsingRegistry(it)
                }})"
            )
            if (includeOwnerClass) {
                sb.append(", from ${traceEvent.ownerClass}")
            }
            if (logStackTrace && !traceEvent.args.isEmpty()) {
                (traceEvent.args.last() as? Throwable)?.let {
                    sb.append("\n")
                    sb.append(formatThrowable(it, logStackTrace))
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

        init {

            // Always switch on synchronous logging by default.
            setTraceEventLoggingMode(LoggingMode.SYNC)
            enableTraceEventLogging(true)
            resetToDefaults()
        }
    }
}
