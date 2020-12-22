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
package com.tomtom.kotlin.memoization

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class FunctionMemoizationExtensionsTest {
    private var callCounter = 0
    private val testString = "Test"

    @Test
    fun `0 arg memoized function - should be able to cache null output`() {
        // given
        val function: () -> String? = {
            callCounter++
            null
        }
        val memoizedFunction = function.memoize()
        // when
        val actual1 = memoizedFunction()
        val actual2 = memoizedFunction()
        // then
        assertNull(actual1)
        assertNull(actual2)
        assertEquals(1, callCounter)
    }

    @Test
    fun `1 arg memoized function - should be able to cache null output`() {
        // given
        val function: (Int) -> String? = {
            callCounter++
            null
        }
        val memoizedFunction = function.memoize(100)
        // when
        val actual1 = memoizedFunction(1)
        val actual2 = memoizedFunction(1)
        // then
        assertNull(actual1)
        assertNull(actual2)
        assertEquals(1, callCounter)
    }

    @Test
    fun `2 arg memoized function - should be able to cache null output`() {
        // given
        val function: (Int, Int) -> String? = { _, _ ->
            callCounter++
            null
        }
        val memoizedFunction = function.memoize(cacheSize = 1)
        // when
        val actual1 = memoizedFunction(1, 2)
        val actual2 = memoizedFunction(1, 2)
        // then
        assertNull(actual1)
        assertNull(actual2)
        assertEquals(1, callCounter)
    }

    @Test
    fun `3 arg memoized function - should be able to cache null output`() {
        // given
        val function: (Int, Int, Int) -> String? = { _, _, _ ->
            callCounter++
            null
        }
        val memoizedFunction = function.memoize(cacheSize = 1)
        // when
        val actual1 = memoizedFunction(1, 2, 3)
        val actual2 = memoizedFunction(1, 2, 3)
        // then
        assertNull(actual1)
        assertNull(actual2)
        assertEquals(1, callCounter)
    }

    @Test
    fun `4 arg memoized function - should be able to cache null output`() {
        // given
        val function: (Int, Int, Int, Int) -> String? = { _, _, _, _ ->
            callCounter++
            null
        }
        val memoizedFunction = function.memoize(cacheSize = 1)
        // when
        val actual1 = memoizedFunction(1, 2, 3, 4)
        val actual2 = memoizedFunction(1, 2, 3, 4)
        // then
        assertNull(actual1)
        assertNull(actual2)
        assertEquals(1, callCounter)
    }

    @Test
    fun `0 arg memoized function - should call function only once`() {
        // given
        val function: () -> String? = {
            callCounter++
            testString
        }
        val memoizedFunction = function.memoize()
        // when
        val actual1 = memoizedFunction()
        val actual2 = memoizedFunction()
        val actual3 = memoizedFunction()
        // then
        assertEquals(testString, actual1)
        assertEquals(testString, actual2)
        assertEquals(testString, actual3)
        assertEquals(1, callCounter)
    }

    @Test
    fun `1 arg memoized function - should call function only once when using same input`() {
        // given
        val function: (Int) -> String? = {
            callCounter++
            testString
        }
        val memoizedFunction = function.memoize(cacheSize = 1)
        // when
        val actual1 = memoizedFunction(1)
        val actual2 = memoizedFunction(1)
        val actual3 = memoizedFunction(1)
        // then
        assertEquals(testString, actual1)
        assertEquals(testString, actual2)
        assertEquals(testString, actual3)
        assertEquals(1, callCounter)
    }

    @Test
    fun `2 arg memoized function - should call function only once when using same input`() {
        // given
        val function: (Int, Int) -> String? = { _, _ ->
            callCounter++
            testString
        }
        val memoizedFunction = function.memoize(cacheSize = 1)
        // when
        val actual1 = memoizedFunction(1, 2)
        val actual2 = memoizedFunction(1, 2)
        val actual3 = memoizedFunction(1, 2)
        // then
        assertEquals(testString, actual1)
        assertEquals(testString, actual2)
        assertEquals(testString, actual3)
        assertEquals(1, callCounter)
    }

    @Test
    fun `3 arg memoized function - should call function only once when using same input`() {
        // given
        val function: (Int, Int, Int) -> String? = { _, _, _ ->
            callCounter++
            testString
        }
        val memoizedFunction = function.memoize(cacheSize = 1)
        // when
        val actual1 = memoizedFunction(1, 2, 3)
        val actual2 = memoizedFunction(1, 2, 3)
        val actual3 = memoizedFunction(1, 2, 3)
        // then
        assertEquals(testString, actual1)
        assertEquals(testString, actual2)
        assertEquals(testString, actual3)
        assertEquals(1, callCounter)
    }

    @Test
    fun `4 arg memoized function - should call function only once when using same input`() {
        // given
        val function: (Int, Int, Int, Int) -> String? = { _, _, _, _ ->
            callCounter++
            testString
        }
        val memoizedFunction = function.memoize(cacheSize = 1)
        // when
        val actual1 = memoizedFunction(1, 2, 3, 4)
        val actual2 = memoizedFunction(1, 2, 3, 4)
        val actual3 = memoizedFunction(1, 2, 3, 4)
        // then
        assertEquals(testString, actual1)
        assertEquals(testString, actual2)
        assertEquals(testString, actual3)
        assertEquals(1, callCounter)
    }

    @Test
    fun `1 arg memoized function - should call function 3 times when using 3 different inputs`() {
        // given
        val function: (Int) -> String? = {
            callCounter++
            testString + it
        }
        val memoizedFunction = function.memoize(2)
        // when
        val actual1 = memoizedFunction(1)
        val actual2 = memoizedFunction(2)
        val actual3 = memoizedFunction(3)
        // then
        assertEquals(testString + 1, actual1)
        assertEquals(testString + 2, actual2)
        assertEquals(testString + 3, actual3)
        assertEquals(3, callCounter)
    }

    @Test
    fun `2 arg memoized function - should call function 3 times when using 3 different inputs`() {
        // given
        val function: (Int, Int) -> String? = { p1, p2 ->
            callCounter++
            testString + p1 + p2
        }
        val memoizedFunction = function.memoize(2)
        // when
        val actual1 = memoizedFunction(1, 1)
        val actual2 = memoizedFunction(2, 1)
        val actual3 = memoizedFunction(2, 2)
        // then
        assertEquals(testString + 1 + 1, actual1)
        assertEquals(testString + 2 + 1, actual2)
        assertEquals(testString + 2 + 2, actual3)
        assertEquals(3, callCounter)
    }

    @Test
    fun `3 arg memoized function - should call function 4 times when using 4 different inputs`() {
        // given
        val function: (Int, Int, Int) -> String? = { p1, p2, p3 ->
            callCounter++
            testString + p1 + p2 + p3
        }
        val memoizedFunction = function.memoize(3)
        // when
        val actual1 = memoizedFunction(1, 1, 1)
        val actual2 = memoizedFunction(2, 1, 1)
        val actual3 = memoizedFunction(2, 2, 1)
        val actual4 = memoizedFunction(2, 2, 2)

        // then
        assertEquals(testString + 1 + 1 + 1, actual1)
        assertEquals(testString + 2 + 1 + 1, actual2)
        assertEquals(testString + 2 + 2 + 1, actual3)
        assertEquals(testString + 2 + 2 + 2, actual4)
        assertEquals(4, callCounter)
    }

    @Test
    fun `4 arg memoized function - should call function 5 times when using 5 different inputs`() {
        // given
        val function: (Int, Int, Int, Int) -> String? = { p1, p2, p3, p4 ->
            callCounter++
            testString + p1 + p2 + p3 + p4
        }
        val memoizedFunction = function.memoize(3)
        // when
        val actual1 = memoizedFunction(1, 1, 1, 1)
        val actual2 = memoizedFunction(2, 1, 1, 1)
        val actual3 = memoizedFunction(2, 2, 1, 1)
        val actual4 = memoizedFunction(2, 2, 2, 1)
        val actual5 = memoizedFunction(2, 2, 2, 2)

        // then
        assertEquals(testString + 1 + 1 + 1 + 1, actual1)
        assertEquals(testString + 2 + 1 + 1 + 1, actual2)
        assertEquals(testString + 2 + 2 + 1 + 1, actual3)
        assertEquals(testString + 2 + 2 + 2 + 1, actual4)
        assertEquals(testString + 2 + 2 + 2 + 2, actual5)
        assertEquals(5, callCounter)
    }

    @Test
    fun `1 arg memoized function - should call function 2 times when value evicted from the cache`() {
        // given
        var callCounterForOne = 0
        val function: (Int) -> String? = {
            if (it == 1) callCounterForOne++
            testString + it
        }
        val memoizedFunction = function.memoize(2)
        // when
        val actual1 = memoizedFunction(1)
        val actual2 = memoizedFunction(2)
        // getting 3 should remove result 1
        val actual3 = memoizedFunction(3)
        val actual4 = memoizedFunction(1)

        // then
        assertEquals(testString + 1, actual1)
        assertEquals(testString + 2, actual2)
        assertEquals(testString + 3, actual3)
        assertEquals(testString + 1, actual4)
        assertEquals(2, callCounterForOne)
    }

    @Test
    fun `2 arg memoized function - should call function 2 times when value evicted from the cache`() {
        // given
        var callCounterForOne = 0
        val function: (Int, Int) -> String? = { p1, p2 ->
            if (p1 == 1 && p2 == 1) callCounterForOne++
            testString + p1 + p2
        }
        val memoizedFunction = function.memoize(2)
        // when
        val actual1 = memoizedFunction(1, 1)
        val actual2 = memoizedFunction(2, 1)
        // getting 3 should remove result 1
        val actual3 = memoizedFunction(2, 2)
        val actual4 = memoizedFunction(1, 1)

        // then
        assertEquals(testString + 1 + 1, actual1)
        assertEquals(testString + 2 + 1, actual2)
        assertEquals(testString + 2 + 2, actual3)
        assertEquals(testString + 1 + 1, actual4)
        assertEquals(2, callCounterForOne)
    }

    @Test
    fun `3 arg memoized function - should call function 2 times when value evicted from the cache`() {
        // given
        var callCounterForOne = 0
        val function: (Int, Int, Int) -> String? = { p1, p2, p3 ->
            if (p1 == 1 && p2 == 1 && p3 == 1) callCounterForOne++
            testString + p1 + p2 + p3
        }
        val memoizedFunction = function.memoize(2)
        // when
        val actual1 = memoizedFunction(1, 1, 1)
        val actual2 = memoizedFunction(2, 1, 1)
        // getting 3 should remove result 1
        val actual3 = memoizedFunction(2, 2, 1)
        val actual4 = memoizedFunction(1, 1, 1)

        // then
        assertEquals(testString + 1 + 1 + 1, actual1)
        assertEquals(testString + 2 + 1 + 1, actual2)
        assertEquals(testString + 2 + 2 + 1, actual3)
        assertEquals(testString + 1 + 1 + 1, actual4)
        assertEquals(2, callCounterForOne)
    }

    @Test
    fun `4 arg memoized function - should call function 2 times when value evicted from the cache`() {
        // given
        var callCounterForOne = 0
        val function: (Int, Int, Int, Int) -> String? = { p1, p2, p3, p4 ->
            if (p1 == 1 && p2 == 1 && p3 == 1) callCounterForOne++
            testString + p1 + p2 + p3 + p4
        }
        val memoizedFunction = function.memoize(2)
        // when
        val actual1 = memoizedFunction(1, 1, 1, 1)
        val actual2 = memoizedFunction(2, 1, 1, 1)
        // getting 3 should remove result 1
        val actual3 = memoizedFunction(2, 2, 1, 1)
        val actual4 = memoizedFunction(1, 1, 1, 1)

        // then
        assertEquals(testString + 1 + 1 + 1 + 1, actual1)
        assertEquals(testString + 2 + 1 + 1 + 1, actual2)
        assertEquals(testString + 2 + 2 + 1 + 1, actual3)
        assertEquals(testString + 1 + 1 + 1 + 1, actual4)
        assertEquals(2, callCounterForOne)
    }

    @Test
    fun `1 arg memoized function - should be able to cache null input`() {
        // given
        val function: (Int?) -> String? = {
            callCounter++
            testString + it
        }
        val memoizedFunction = function.memoize(2)
        // when
        val actual1 = memoizedFunction(null)
        val actual2 = memoizedFunction(1)

        // then
        assertEquals(testString + null, actual1)
        assertEquals(testString + 1, actual2)
        assertEquals(2, callCounter)
    }

    @Test
    fun `2 arg memoized function - should be able to cache null input`() {
        // given
        val function: (Int?, Int?) -> String? = { p1, p2 ->
            callCounter++
            testString + p1 + p2
        }
        val memoizedFunction = function.memoize(2)
        // when
        val actual1 = memoizedFunction(null, 1)
        val actual2 = memoizedFunction(null, null)
        val actual3 = memoizedFunction(1, 1)

        // then
        assertEquals(testString + null + 1, actual1)
        assertEquals(testString + null + null, actual2)
        assertEquals(testString + 1 + 1, actual3)
        assertEquals(3, callCounter)
    }

    @Test
    fun `3 arg memoized function - should be able to cache null input`() {
        // given
        val function: (Int?, Int?, Int?) -> String? = { p1, p2, p3 ->
            callCounter++
            testString + p1 + p2 + p3
        }
        val memoizedFunction = function.memoize(2)
        // when
        val actual1 = memoizedFunction(null, 1, 1)
        val actual2 = memoizedFunction(null, null, 1)
        val actual3 = memoizedFunction(null, null, null)
        val actual4 = memoizedFunction(1, 1, 1)

        // then
        assertEquals(testString + null + 1 + 1, actual1)
        assertEquals(testString + null + null + 1, actual2)
        assertEquals(testString + null + null + null, actual3)
        assertEquals(testString + 1 + 1 + 1, actual4)
        assertEquals(4, callCounter)
    }

    @Test
    fun `4 arg memoized function - should be able to cache null input`() {
        // given
        val function: (Int?, Int?, Int?, Int?) -> String? = { p1, p2, p3, p4 ->
            callCounter++
            testString + p1 + p2 + p3 + p4
        }
        val memoizedFunction = function.memoize(2)
        // when
        val actual1 = memoizedFunction(null, 1, 1, 1)
        val actual2 = memoizedFunction(null, null, 1, 1)
        val actual3 = memoizedFunction(null, null, null, 1)
        val actual4 = memoizedFunction(null, null, null, null)
        val actual5 = memoizedFunction(1, 1, 1, 1)

        // then
        assertEquals(testString + null + 1 + 1 + 1, actual1)
        assertEquals(testString + null + null + 1 + 1, actual2)
        assertEquals(testString + null + null + null + 1, actual3)
        assertEquals(testString + null + null + null + null, actual4)
        assertEquals(testString + 1 + 1 + 1 + 1, actual5)
        assertEquals(5, callCounter)
    }

    @Test
    fun `1 arg memoized function should memoize 100 results`() {
        // given
        val function: (Int) -> Int = {
            callCounter++
            it
        }
        val memoizedFunction = function.memoize(100)
        // when
        for (iteration in 1..100) {
            for (arg in 1..100) {
                assertEquals(arg, memoizedFunction(arg))
            }
        }
        // then
        assertEquals(100, callCounter)
    }

    @Test
    fun `2 arg memoized function should memoize 100 results`() {
        // given
        val function: (Int, Int) -> Int = { arg1, arg2 ->
            callCounter++
            arg1 + arg2
        }
        val memoizedFunction = function.memoize(100)
        // when
        for (iteration in 1..100) {
            for (arg in 1..100) {
                val arg2 = arg % 10
                assertEquals(arg + arg2, memoizedFunction(arg, arg2))
            }
        }
        // then
        assertEquals(100, callCounter)
    }

    @Test
    fun `3 arg memoized function should memoize 100 results`() {
        // given
        val function: (Int, Int, Int) -> Int = { arg1, arg2, arg3 ->
            callCounter++
            arg1 + arg2 + arg3
        }
        val memoizedFunction = function.memoize(100)
        // when
        for (iteration in 1..100) {
            for (arg in 1..100) {
                val arg2 = arg % 10
                val arg3 = arg % 25
                assertEquals(arg + arg2 + arg3, memoizedFunction(arg, arg2, arg3))
            }
        }
        // then
        assertEquals(100, callCounter)
    }

    @Test
    fun `4 arg memoized function should memoize 100 results`() {
        // given
        val function: (Int, Int, Int, Int) -> Int = { arg1, arg2, arg3, arg4 ->
            callCounter++
            arg1 + arg2 + arg3 + arg4
        }
        val memoizedFunction = function.memoize(100)
        // when
        for (iteration in 1..100) {
            for (arg in 1..100) {
                val arg2 = arg % 10
                val arg3 = arg % 25
                val arg4 = arg % 50
                assertEquals(arg + arg2 + arg3 + arg4, memoizedFunction(arg, arg2, arg3, arg4))
            }
        }
        // then
        assertEquals(100, callCounter)
    }
}
