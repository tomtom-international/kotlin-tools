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
