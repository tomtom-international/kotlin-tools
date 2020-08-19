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
package com.tomtom.kotlin.uid

import com.tomtom.kotlin.uid.Uid
import nl.jqno.equalsverifier.EqualsVerifier
import org.junit.Assert
import org.junit.Test

class UidTest {
    @Test
    fun testCreate() {
        val size = 100
        val map: MutableMap<String, Boolean> = HashMap(size)
        for (i in 0 until size) {
            val id: Uid<*> = Uid<Any>()
            Assert.assertNull(map.put(id.toString(), true))
        }
        Assert.assertTrue(true)
    }

    @Test
    fun testEquals() {
        EqualsVerifier.forClass(Uid::class.java)
            .withNonnullFields("uuid")
            .verify()
    }

    @Test
    fun testIsValid() {
        Assert.assertTrue(Uid.isValid("d32b6789-bfbb-4194-87f3-72ce34609902"))
        Assert.assertTrue(Uid.isValid("0-0-0-0-0"))
        Assert.assertFalse(Uid.isValid("0-0-0-0-0-0"))
        Assert.assertFalse(Uid.isValid("d32b6789bfbb419487f372ce34609902"))
        Assert.assertFalse(Uid.isValid(""))
        Assert.assertFalse(Uid.isValid(null))
    }

    @Test
    fun testFromString() {
        val a: Uid<Any> = Uid.fromString("d32b6789-bfbb-4194-87f3-72ce34609902")
        val s = "d32b6789-bfbb-4194-87f3-72ce34609902"
        val b: Uid<Any> = Uid.fromString(s)
        Assert.assertEquals(a, b)
    }

    @Test
    fun testToString() {
        val x: Uid<Any> = Uid()
        try {
        } catch (ignored: Exception) {
            Assert.fail()
        }
    }

    @Test
    fun testAs() {
        val a: Uid<Long> = Uid()
        val b: Uid<Int> = a as Uid<Int>
        Assert.assertTrue(a.equals(b))
    }

    @Test
    fun testMatchesFromString() {
        val a: Uid<String> = Uid.fromString("0-0-0-0-0")
        Assert.assertTrue(a.matchesFromString("0000-0000-000-0-0"))
    }

    @Test
    fun testFromHexString() {
        val s = "d32b6789-bfbb-4194-87f3-72ce34609902"
        val a: Uid<Any> = Uid.fromString(s)
        val b: Uid<Any> = Uid.fromHexString(s.replace("-".toRegex(), ""))
        Assert.assertEquals(a, b)
    }

    @Test
    fun testToHexString() {
        val a: Uid<Any> = Uid()
        Assert.assertEquals(a.toString().replace("-", ""), a.toHexString())
    }

    @Test
    fun testToAppendedHexString() {
        val s = "00000000-0000-0001-0000-000000000001"
        val a: Uid<Any> = Uid.fromString(s)
        Assert.assertEquals(s.replace("-".toRegex(), ""), a.toHexString())
    }

    @Test
    fun testNegativeHexId() {
        val s = "ffffffff-ffff-ffff-ffff-ffffffffffff"
        val a: Uid<Any> = Uid.fromString(s)
        val b: Uid<Any> = Uid.fromHexString(s.replace("-".toRegex(), ""))
        Assert.assertEquals(a, b)
    }
}
