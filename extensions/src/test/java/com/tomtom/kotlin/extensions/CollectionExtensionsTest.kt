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

package com.tomtom.kotlin.extensions

import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Test

internal class CollectionExtensionsTest {

    @Test
    fun `empty collection has no duplicates`() {
        assertFalse(emptyList<Int>().containsDuplicates())
        assertTrue(emptyList<Int>().containsNoDuplicates())
    }

    @Test
    fun `collection without duplicates`() {
        assertFalse(listOf(1).containsDuplicates())
        assertTrue(listOf(1).containsNoDuplicates())
        assertFalse(listOf(1, 2, 3, 4, 5).containsDuplicates())
        assertTrue(listOf(1, 2, 3, 4, 5).containsNoDuplicates())
    }

    @Test
    fun `collection with duplicates`() {
        assertTrue(listOf(1, 1).containsDuplicates())
        assertFalse(listOf(1, 1).containsNoDuplicates())
        assertTrue(listOf(1, 2, 3, 2, 1).containsDuplicates())
        assertFalse(listOf(1, 2, 3, 2, 1).containsNoDuplicates())
    }
}
