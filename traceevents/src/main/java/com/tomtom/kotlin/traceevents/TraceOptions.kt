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

/**
 * This annotation can be used to annotate functions in for a trace event listener.
 * Both interfaces and individual functions can be annotated. Annotations at the function level
 * override those at the interface level.
 *
 * @param includeExceptionStackTrace Specifies whether a stack trace should be included if the last
 * parameter of the event is derived from [Throwable]. If false, only the exception message is
 * shown.
 * Default value is true.
 *
 * @param includeTaggingClass Specifies whether owner class of the event definition should be logged
 * or not.
 * Default is false.

 * @param includeFileLocation Specifies whether the filename and line number of the caller
 * should be logged or not.
 * Default is false.
 *
 * @param includeEventInterface Specifies whether the interface name of the [TraceEventListener]
 * interface should be logged or not.
 * Default is false.
 *
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
annotation class TraceOptions(
    val includeExceptionStackTrace: Boolean = true,
    val includeTaggingClass: Boolean = false,
    val includeFileLocation: Boolean = false,
    val includeEventInterface: Boolean = false
)
