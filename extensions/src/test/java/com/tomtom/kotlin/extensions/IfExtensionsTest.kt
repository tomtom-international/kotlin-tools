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

import io.mockk.spyk
import io.mockk.verify
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.junit.Test

internal class IfExtensionsTest {

    @Test
    fun `ifTrue on true`() {
        // GIVEN
        val sut = true
        val ifTrueBlock = spyk({ 1 })

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
        val ifTrueBlock = spyk({ 1 })

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
        val ifTrueBlock = spyk({ 1 })

        // WHEN
        val result = sut.ifTrue(ifTrueBlock)

        // THEN
        assertNull(result)
        verify(exactly = 0) { ifTrueBlock.invoke() }
    }

    @Test
    fun `ifNull on null`() {
        // GIVEN
        val sut: Int? = null
        val ifNullBlock = spyk({ 1 })

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
        val ifNullBlock = spyk({ B })

        // WHEN
        val result: Base = sut.ifNull(ifNullBlock)

        // THEN
        assertEquals(A, result)
        verify(exactly = 0) { ifNullBlock.invoke() }
    }
}
