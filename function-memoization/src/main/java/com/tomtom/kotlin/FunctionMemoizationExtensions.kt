/*
 * Copyright 2020-present, TomTom N.V.
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

package com.tomtom.kotlin

/**
 * Creates a memoized function that will return cached result when the same inputs occurs again.
 */
fun <R> (() -> R).memoize(): () -> R =
    object : () -> R {
        val result by lazy(LazyThreadSafetyMode.NONE) { this@memoize() }
        override fun invoke(): R = result
    }

/**
 * Creates a LRU style cached function that will return cached result when the same inputs occurs again.
 */
fun <P1, R> ((P1) -> R).memoize(cacheSize: Int): (P1) -> R =
    Function1Cache(this, cacheSize = cacheSize)

/**
 * Creates a memoized function that will return cached result when the same inputs occurs again.
 */
fun <P1, R> ((P1) -> R).memoize(): (P1) -> R =
    Function1Cache(this)

/**
 * Creates a LRU style cached function that will return cached result when the same inputs occurs again.
 */
fun <P1, P2, R> ((P1, P2) -> R).memoize(cacheSize: Int): (P1, P2) -> R =
    object : (P1, P2) -> R {
        private val cache = { pair: Pair<P1, P2> ->
            this@memoize(pair.first, pair.second)
        }.memoize(cacheSize = cacheSize)

        override fun invoke(p1: P1, p2: P2): R = cache.invoke(Pair(p1, p2))
    }

/**
 * Creates a memoized function that will return cached result when the same inputs occurs again.
 */
fun <P1, P2, R> ((P1, P2) -> R).memoize(): (P1, P2) -> R =
    object : (P1, P2) -> R {
        private val cache = { pair: Pair<P1, P2> ->
            this@memoize(pair.first, pair.second)
        }.memoize()

        override fun invoke(p1: P1, p2: P2): R = cache.invoke(Pair(p1, p2))
    }


/**
 * Creates a LRU style cached function that will return cached result when the same inputs occurs again.
 */
fun <P1, P2, P3, R> ((P1, P2, P3) -> R).memoize(cacheSize: Int): (P1, P2, P3) -> R =
    object : (P1, P2, P3) -> R {
        private val cache = { triple: Triple<P1, P2, P3> ->
            this@memoize(triple.first, triple.second, triple.third)
        }.memoize(cacheSize = cacheSize)

        override fun invoke(p1: P1, p2: P2, p3: P3): R = cache.invoke(Triple(p1, p2, p3))
    }

/**
 * Creates a memoized function that will return cached result when the same inputs occurs again.
 */
fun <P1, P2, P3, R> ((P1, P2, P3) -> R).memoize(): (P1, P2, P3) -> R =
    object : (P1, P2, P3) -> R {
        private val cache = { triple: Triple<P1, P2, P3> ->
            this@memoize(triple.first, triple.second, triple.third)
        }.memoize()

        override fun invoke(p1: P1, p2: P2, p3: P3): R = cache.invoke(Triple(p1, p2, p3))
    }

/**
 * Creates a LRU style cached function that will return cached result when the same inputs occurs again.
 */
fun <P1, P2, P3, P4, R> ((P1, P2, P3, P4) -> R).memoize(cacheSize: Int): (P1, P2, P3, P4) -> R =
    object : (P1, P2, P3, P4) -> R {
        private val cache = { quadruple: Quadruple<P1, P2, P3, P4> ->
            this@memoize(quadruple.first, quadruple.second, quadruple.third, quadruple.fourth)
        }.memoize(cacheSize = cacheSize)

        override fun invoke(p1: P1, p2: P2, p3: P3, p4: P4): R = cache.invoke(Quadruple(p1, p2, p3, p4))
    }

/**
 * Creates a memoized function that will return cached result when the same inputs occurs again.
 */
fun <P1, P2, P3, P4, R> ((P1, P2, P3, P4) -> R).memoize(): (P1, P2, P3, P4) -> R =
    object : (P1, P2, P3, P4) -> R {
        private val cache = { quadruple: Quadruple<P1, P2, P3, P4> ->
            this@memoize(quadruple.first, quadruple.second, quadruple.third, quadruple.fourth)
        }.memoize()

        override fun invoke(p1: P1, p2: P2, p3: P3, p4: P4): R = cache.invoke(Quadruple(p1, p2, p3, p4))
    }

private class Function1Cache<P1, R>(
    private val originalFunction: (P1) -> R,
    // no cache size specified means no limit
    private val cacheSize: Int? = null
) : (P1) -> R {
    private val map = LinkedHashMap<P1, R>(cacheSize ?: 16, 0.75f, true)

    override fun invoke(param1: P1): R {
        return if (map.containsKey(param1)) {
            @Suppress("UNCHECKED_CAST")
            map[param1] as R
        } else {
            val value = originalFunction(param1)
            map[param1] = value
            if (cacheSize != null && map.size > cacheSize) {
                // remove last not accessed value
                map.remove(map.keys.first())
            }
            value
        }
    }
}

private data class Quadruple<out A, out B, out C, out D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)
