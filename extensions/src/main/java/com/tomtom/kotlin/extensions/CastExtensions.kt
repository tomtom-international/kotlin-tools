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

/**
 * Casts `this` to `T`, throwing a [ClassCastException] if the cast fails.
 *
 * This can be used to replace:
 *
 * ```kotlin
 * (
 *     myObject
 *         .chainCall()
 *         .chainCall()
 *         as MyClass
 *  ).chainCall()
 * ```
 *
 * or:
 *
 * ```kotlin
 * myObject
 *     .chainCall()
 *     .chainCall()
 *     .let { it as MyClass }
 *     .chainCall()
 * ```
 *
 * with a more functional-programming-friendly syntax:
 *
 * ```kotlin
 * myObject
 *     .chainCall()
 *     .chainCall()
 *     .cast<MyClass>()
 *     .chainCall()
 * ```
 *
 * This method is not intended to replace all casts; the `as` operator is fine in many cases. In
 * chain-calling constructs however, the `as` operator can easily lead to reduced readability. `let`
 * helps with this, but adds boilerplate that distracts from the functional flow.
 */
public inline fun <reified T> Any.cast(): T =
    this as T

/**
 * Casts `this` to `T`, returning `null` if the cast fails.
 *
 * This can be used to replace:
 *
 * ```kotlin
 * (
 *     myObject
 *         .chainCall()
 *         .chainCall()
 *         as? MyClass
 *  )?.chainCall()
 * ```
 *
 * or:
 *
 * ```kotlin
 * myObject
 *     .chainCall()
 *     .chainCall()
 *     .let { it as? MyClass }
 *     ?.chainCall()
 * ```
 *
 * with a more functional-programming-friendly syntax:
 *
 * ```kotlin
 * myObject
 *     .chainCall()
 *     .chainCall()
 *     .safeCast<MyClass>()
 *     ?.chainCall()
 * ```
 *
 * This method is not intended to replace all casts; the `as?` operator is fine in many cases. In
 * chain-calling constructs however, the `as?` operator can easily lead to reduced readability.
 * `let` helps with this, but adds boilerplate that distracts from the functional flow.
 */
public inline fun <reified T> Any.safeCast(): T? =
    this as? T
