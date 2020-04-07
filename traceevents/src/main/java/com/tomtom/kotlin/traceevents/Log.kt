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

import com.tomtom.kotlin.traceevents.Log.Companion.DefaultLoggerToStdout.log
import com.tomtom.kotlin.traceevents.Log.Companion.setLogger
import com.tomtom.kotlin.traceevents.Log.Logger
import java.time.LocalDateTime

/**
 * This defines an interface [Logger], which may be implemented and injected using
 * [setLogger] to modify the behavior of the simple text message loggers.
 */
interface Log {

    enum class Level { VERBOSE, DEBUG, INFO, WARN, ERROR }

    /**
     * Implement this interface for your own logger and set [log] accordingly.
     */
    interface Logger {
        fun log(level: Level, tag: String, message: String, e: Throwable? = null)
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
        internal fun log(level: Level, tag: String, message: String, e: Throwable? = null) =
            theLogger.log(level, tag, message, e)

        /**
         * Default implementation for [log].
         */
        internal object DefaultLoggerToStdout : Logger {
            override fun log(level: Level, tag: String, message: String, e: Throwable?) {
                val exceptionMsg = e?.message?.let { ", $it" } ?: ""
                println("${LocalDateTime.now()}: [$level] $tag: $message$exceptionMsg")
            }
        }

        internal var theLogger: Logger = DefaultLoggerToStdout
    }
}
