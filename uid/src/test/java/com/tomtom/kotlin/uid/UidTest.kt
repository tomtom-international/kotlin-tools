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
package com.tomtom.kotlin.uid

import nl.jqno.equalsverifier.EqualsVerifier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UidTest {

    // Below is some code that does NOT compile to show the type safety features of Uid.

    // fun compileTimeTypeSafetyExamples() {
    //   var idInt = Uid<Int>()
    //   var idLong = Uid<Long>()
    //   idInt == idLong         // This is OK.
    //   idInt = idLong          // This does not compile
    // }

    @Test
    fun testEquals() {
        EqualsVerifier.forClass(Uid::class.java)
            .withNonnullFields("uuid")
            .verify()
    }

    @Test
    fun testCreate() {
        val size = 100
        val map: MutableMap<String, Boolean> = HashMap(size)
        for (i in 0 until size) {
            val id = Uid.new<Any>()
            assertNull(map.put(id.toString(), true))
        }
    }

    @Test
    fun testIsValid() {
        assertNotNull(Uid.fromStringIfValid<Any>("d32b6789-bfbb-4194-87f3-72ce34609902"))
        assertNotNull(Uid.fromStringIfValid<Any>("0-0-0-0-0"))
        assertNull(Uid.fromStringIfValid<Any>("0-0-0-0-0-0"))
        assertNull(Uid.fromStringIfValid<Any>("d32b6789bfbb419487f372ce34609902"))
        assertNull(Uid.fromStringIfValid<Any>(""))
        assertNull(Uid.fromStringIfValid<Any>(null))
    }

    @Test
    fun testFromString() {
        val a = Uid.fromString<Any>("d32b6789-bfbb-4194-87f3-72ce34609902")
        val s = "d32b6789-bfbb-4194-87f3-72ce34609902"
        val b = Uid.fromString<Any>(s)
        assertEquals(a, b)
    }

    @Test
    fun testToString() {
        val x = Uid.fromString<Any>("1-2-3-4-5")
        assertEquals("00000001-0002-0003-0004-000000000005", x.toString())
    }

    @Suppress("UNCHECKED_CAST")
    @Test
    fun testAs() {
        val a = Uid.new<Long>()
        val b = a as Uid<Int>
        assertTrue(a.equals(b))
    }

    @Test
    fun testMatchesFromString() {
        val a: Uid<String> = Uid.fromString("0-0-0-0-0")
        assertTrue(a.matchesFromString("0000-0000-000-0-0"))
    }

    @Test
    fun testFromHexString() {
        val s = "d32b6789-bfbb-4194-87f3-72ce34609902"
        val a: Uid<Any> = Uid.fromString(s)
        val b: Uid<Any> = Uid.fromHexString(s.replace("-".toRegex(), ""))
        assertEquals(a, b)
    }

    @Test
    fun testToHexString1() {
        val a = Uid.new<Any>()
        assertEquals(a.toString().replace("-", ""), a.toHexString())
    }

    @Test
    fun testToHexString2() {
        val a = Uid.fromString<Any>("1-2-3-4-5")
        assertEquals("00000001000200030004000000000005", a.toHexString())
    }

    @Test
    fun testToAppendedHexString() {
        val s = "00000000-0000-0001-0000-000000000001"
        val a: Uid<Any> = Uid.fromString(s)
        assertEquals(s.replace("-".toRegex(), ""), a.toHexString())
    }

    @Test
    fun testNegativeHexId() {
        val s = "ffffffff-ffff-ffff-ffff-ffffffffffff"
        val a: Uid<Any> = Uid.fromString(s)
        val b: Uid<Any> = Uid.fromHexString(s.replace("-".toRegex(), ""))
        assertEquals(a, b)
    }
}
