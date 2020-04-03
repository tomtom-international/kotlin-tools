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
interface TraceEventConsumer
