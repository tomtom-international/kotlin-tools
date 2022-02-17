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

/**
 * Tagging-interface for all interfaces that are used to define trace events consumers.
 *
 * Every trace event consumer must derive from the [TraceEventConsumer] interface.
 * Non-generic trace event consumers (so, those not derived from [GenericTraceEventConsumer]),
 * must also derive from the [TraceEventListener] interface they implement.
 *
 * For example:
 *
 * ```
 * // If this interface defines the trace events used in an application...
 * interface MyEvents : TraceEventListener {
 *     fun somethingHappened()
 * }
 *
 * // ...then this class defines a specific trace event consumer for those
 * // events:
 * class MyEventsConsumer : MyEvents, TraceEventConsumer {
 *     fun somethingHappened() {
 *         ... // Code that specifies what to do if the event happens.
 *     }
 * }
 * ```
 */
public interface TraceEventConsumer
