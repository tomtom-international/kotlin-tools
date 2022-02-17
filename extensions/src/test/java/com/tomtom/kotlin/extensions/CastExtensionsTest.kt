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

import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import org.junit.Test

internal class CastExtensionsTest {

    @Test
    fun `successful cast`() {
        // GIVEN
        val sut: Number = 1

        // WHEN
        val result: Int = sut.cast<Int>()

        // THEN
        assertEquals(1, result)
    }

    @Test
    fun `unsuccessful cast`() {
        // GIVEN
        val sut: Number = 1

        // WHEN-THEN
        assertFailsWith<ClassCastException> {
            sut.cast<Long>()
        }
    }

    @Test
    fun `successful safeCast`() {
        // GIVEN
        val sut: Number = 1

        // WHEN
        val result: Int? = sut.safeCast<Int>()

        // THEN
        assertEquals(1, result)
    }

    @Test
    fun `unsuccessful safeCast`() {
        // GIVEN
        val sut: Number = 1

        // WHEN
        val result: Long? = sut.safeCast<Long>()

        // THEN
        assertNull(result)
    }
}
