package com.tomtom.kotlin.traceevents

import java.time.LocalDateTime

interface Log {

    enum class Level { VERBOSE, DEBUG, INFO, WARN, ERROR }

    /**
     * Implement this interface for your own logger and set [log] accordingly.
     */
    interface Logger {
        fun log(level: Level, tag: String?, message: String, e: Throwable? = null)
    }

    companion object {
        /**
         * Call this function if you need to use a different logger than the default
         * implementation, which uses [println].
         */
        fun setLogger(logger: Logger = DefaultLoggerToStdout) {
            theLogger = logger
        }

        /**
         * Function to log a string message with a log level.
         */
        internal fun log(level: Level, tag: String?, message: String, e: Throwable? = null) =
                theLogger.log(level, tag, message, e)

        /**
         * Default implementation for [log].
         */
        internal object DefaultLoggerToStdout : Logger {
            override fun log(level: Level, tag: String?, message: String, e: Throwable?) {
                val exceptionMsg = e?.message?.let { ", $it" } ?: ""
                println("${LocalDateTime.now()}: [$level] $tag: $message$exceptionMsg}")
            }
        }

        internal var theLogger: Logger = DefaultLoggerToStdout

    }
}
