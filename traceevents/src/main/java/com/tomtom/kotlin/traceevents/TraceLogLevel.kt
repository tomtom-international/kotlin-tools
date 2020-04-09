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

/**
 * This annotation can be used to annotate methods in [TraceEventListener].
 *
 * @param logLevel Specifies the log level at which the default logging trace event consumer
 * will log the event to [TraceLog]. Supported log levels are specified in [LogLevel].
 * Default value is [LogLevel.DEBUG].
 *
 * @param logStackTrace Specifies if whether a stack trace should be included if the last parameter
 * of the event is derived from [Throwable]. If false, only the exception message is shown.
 * Default value is true.
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class TraceLogLevel(val logLevel: LogLevel, val logStackTrace: Boolean = true)
