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

import io.mockk.every
import io.mockk.spyk
import io.mockk.verify
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

internal class IfExtensionsTest {

    @Test
    fun `ifTrue on true`() {
        // GIVEN
        val sut = true
        // TODO: replace with `spyk({ 1 })` when https://github.com/mockk/mockk/issues/1033 is fixed.
        val ifTrueBlock: () -> Int = spyk {
            every { this@spyk.invoke() } answers { 1 }
        }

        // WHEN
        val result = sut.ifTrue(ifTrueBlock)

        // THEN
        assertEquals(1, result)
        verify { ifTrueBlock.invoke() }
    }

    @Test
    fun `ifTrue on false`() {
        // GIVEN
        val sut = false
        // TODO: replace with `spyk({ 1 })` when https://github.com/mockk/mockk/issues/1033 is fixed.
        val ifTrueBlock: () -> Int = spyk {
            every { this@spyk.invoke() } answers { 1 }
        }

        // WHEN
        val result = sut.ifTrue(ifTrueBlock)

        // THEN
        assertNull(result)
        verify(exactly = 0) { ifTrueBlock.invoke() }
    }

    @Test
    fun `ifTrue on null`() {
        // GIVEN
        val sut: Boolean? = null
        // TODO: replace with `spyk({ 1 })` when https://github.com/mockk/mockk/issues/1033 is fixed.
        val ifTrueBlock: () -> Int = spyk {
            every { this@spyk.invoke() } answers { 1 }
        }

        // WHEN
        val result = sut.ifTrue(ifTrueBlock)

        // THEN
        assertNull(result)
        verify(exactly = 0) { ifTrueBlock.invoke() }
    }

    @Test
    fun `ifTrue contract returnsNotNull`() {
        // GIVEN
        val sut: Boolean? = null

        @Suppress("unused")
        fun Any.callOnNonNull() {
        }

        // WHEN ifTrue returns something other than null
        if (sut.ifTrue { 1 } != null) {

            // THEN no null-check is needed on sut
            sut.callOnNonNull()
        } else {

            // AND in else it requires a null-check with ?
            sut?.callOnNonNull()
        }
    }

    @Test
    fun `ifNull on null`() {
        // GIVEN
        val sut: Int? = null
        // TODO: replace with `spyk({ 1 })` when https://github.com/mockk/mockk/issues/1033 is fixed.
        val ifNullBlock: () -> Int = spyk {
            every { this@spyk.invoke() } answers { 1 }
        }

        // WHEN
        val result = sut.ifNull(ifNullBlock)

        // THEN
        assertEquals(1, result)
        verify { ifNullBlock.invoke() }
    }

    private sealed class Base
    private object A : Base()
    private object B : Base()

    @Test
    fun `ifNull with different types`() {
        // GIVEN
        val sut = A
        // TODO: replace with `spyk({ B })` when https://github.com/mockk/mockk/issues/1033 is fixed.
        val ifNullBlock: () -> Base = spyk {
            every { this@spyk.invoke() } answers { B }
        }

        // WHEN
        val result: Base = sut.ifNull(ifNullBlock)

        // THEN
        assertEquals(A, result)
        verify(exactly = 0) { ifNullBlock.invoke() }
    }
}
