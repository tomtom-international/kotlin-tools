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

import java.io.Serializable
import java.util.UUID


/**
 * Generic immutable unique ID class. Really just an abstraction of UUIDs. Used to uniquely
 * identify things.
 *
 * The class has a generic type T to allow creating typesafe IDs, like Uid<Message> or Uid<Person>.
 * The class represents UUIDs as Strings internally, to avoid loads of UUID to String conversions
 * all the time. This makes the class considerably faster in use than the regular [UUID] class.
 *
 * @param T Type tag for ID, to make IDs type-safe.
 */
data class Uid<T> private constructor(private val uuid: String) : Serializable {

    /**
     * Returns a hex string representation of this Uid. Opposite of [fromHexString].
     * This representation is shorthand, it does not include dashes.
     *
     * @return Hex string representation of ID, exactly 32 characters long.
     */
    fun toHexString(): String {
        val compactUuid = UUID.fromString(uuid)
        val msb = java.lang.Long.toHexString(compactUuid.mostSignificantBits)
        val lsb = java.lang.Long.toHexString(compactUuid.leastSignificantBits)
        return "${msb.padStart(16, '0')}${lsb.padStart(16, '0')}"
    }

    /**
     * Method converts given String representation to [Uid] and compares it with this instance.
     * A String value of "0-0-0-0-0" would match a [Uid] of "00000-0000-0000-000000000-00" or so.
     *
     * @param uid String representation of the Uid.
     * @return True in case String representation matches instance. False otherwise.
     */
    fun matchesFromString(uid: String) = this == fromString<T>(uid)

    /**
     * Returns the string representation of this Uid. Opposite of [fromString].
     *
     * @return String representation of ID.
     */
    override fun toString() = uuid

    companion object {
        private val serialVersionUID = 1L
        private const val UUID_DASH = '-'
        private const val UUID_MIN_LENGTH = 9
        private const val UUID_MAX_LENGTH = 36
        private val UUID_DASH_POS = intArrayOf(8, 13, 18, 23)

        /**
         * Create a new, unique UUID-based ID.
         */
        fun <T> new() = Uid<T>(UUID.randomUUID().toString())

        /**
         * Instantiates a [Uid] with a string. Mainly used when de-serializing existing entities.
         *
         * The format of 'uuid' is checked to comply with a standard UUID format, which is:
         * - Dashes at positions 8, 13, 18, 23 (base 0).
         * - Characters 0-9 and a-f (lowercase only).
         *
         * If this format is used, the creation of the [Uid] is very fast. If an alternative format
         * us used, as accepted by [fromString], the call is much more expensive.
         *
         * @param uuidAsString An existing string representation of a UUID.
         * @throws [IllegalArgumentException] If name does not conform to the string representation
         * as described in [UUID.toString]. Use [isValid] to make sure the string is valid.
         */
        fun <T> fromString(uuidAsString: String): Uid<T> {
            /**
             * This code has been optimized to NOT just call [UUID.fromString] to convert the
             * UUID-String into a String (and catch an [IllegalArgumentException]).
             *
             * If the UUID does not comply, the expensive call to [UUID.fromString] is made after all.
             */
            val length = uuidAsString.length
            require(length in UUID_MIN_LENGTH..UUID_MAX_LENGTH) {
                "Length of UUID must be [" + UUID_MIN_LENGTH + ", " +
                    UUID_MAX_LENGTH + "], but is " + uuidAsString.length + ", uuid=" + uuidAsString
            }

            // Check dashes.
            val convertedUuidString = if (areDashesAtCorrectPosition(uuidAsString)) {
                uuidAsString.toLowerCase().also {
                    require(onlyContainsValidUuidCharacters(it)) {
                        "Incorrect UUID format, uuid=$uuidAsString"
                    }
                }
            } else {
                UUID.fromString(uuidAsString).toString().toLowerCase()
            }
            return Uid<T>(convertedUuidString)
        }

        /**
         * Returns an ID if it is a valid UUID, or `null` if it's not.
         *
         * @param id String representation of UUID.
         * @return Valid UUID, or null.
         */
        fun <T> fromStringIfValid(id: String?) =
            if (id == null) {
                null
            } else try {
                fromString<T>(id)
            } catch (ignored: IllegalArgumentException) {
                null
            }

        /**
         * Instantiates a [Uid] with given ID as hex-formatted string. Opposite of [toHexString].
         *
         * @param <T> Uid type.
         * @param id  Hex string representation of ID, must be exactly 32 characters long.
         * @return Uid.
         * @throws [IllegalArgumentException] If name does not conform to the string
         * representation as described in [UUID.toString]. Use [isValid] to make sure the
         * string is valid.
        </T> */
        fun <T> fromHexString(id: String): Uid<T> {
            require(id.length == 32)
            val msb: Long =
                id.substring(0, 8).toLong(16) shl 32 or id.substring(8, 16).toLong(16)
            val lsb: Long =
                id.substring(16, 24).toLong(16) shl 32 or id.substring(24, 32).toLong(16)
            return Uid<T>(UUID(msb, lsb).toString())
        }

        /**
         * Return if string contains valid UUID characters only.
         * Must be converted to lowercase already.
         *
         * @param uuid Input UUID. Should be lowercase already.
         * @return True if valid characters only.
         */
        private fun onlyContainsValidUuidCharacters(uuid: String) =
            uuid.toCharArray().all { it in '0'..'9' || it in 'a'..'f' || it == UUID_DASH }

        /**
         * Checks if the dashes are at the right positions for a UUID.
         *
         * @param s Input string.
         * @return True if the dashes are correctly placed.
         */
        private fun areDashesAtCorrectPosition(s: String) =
            UUID_DASH_POS.all { s.length > it && s[it] == '-' }
    }
}
