/*
 *  Copyright (c) 2020 - 2020 TomTom N.V. All rights reserved.
 *
 *  This software is the proprietary copyright of TomTom N.V. and its subsidiaries and may be
 *  used for internal evaluation purposes or commercial use strictly subject to separate
 *  licensee agreement between you and TomTom. If you are the licensee, you are only permitted
 *  to use this Software in accordance with the terms of your license agreement. If you are
 *  not the licensee then you are not authorised to use this software in any manner and should
 *  immediately return it to TomTom N.V.
 */

package com.tomtom.kotlin.traceevents

import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SetLoggerTest {

    private val sut = Tracer.Factory.create<TraceEventListener>(this::class)

    object MyLogger : Log.Logger {
        override fun log(level: Log.Level, tag: String?, message: String, e: Throwable?) {
            println("LOG: $level, $tag, $message, $e")
            called = true
        }
    }

    @Test
    fun `specific consumer`() {
        assertFalse(called)

        sut.d("text 1")
        assertFalse(called)

        Log.setLogger(SetLoggerTest.MyLogger)

        sut.d("text 2")
        assertTrue(called)

        called = false
        Log.setLogger()
        sut.d("text 3")
        assertFalse(called)
    }

    companion object {
        var called = false
    }
}
