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
 * This is an interface to catch events from trace event loggers. See [Tracer] for a
 * more detailed explanation on how to use this class (and how to create specific trace event
 * handlers as well).
 */
interface GenericTraceEventConsumer : TraceEventListener, TraceEventConsumer {

    /**
     * The function [consumeTraceEvent] logs a trace event as type-safe objects (including the
     * method name of the calling function).
     *
     * @param traceEvent Trace event.
     */
    suspend fun consumeTraceEvent(traceEvent: TraceEvent)
}
