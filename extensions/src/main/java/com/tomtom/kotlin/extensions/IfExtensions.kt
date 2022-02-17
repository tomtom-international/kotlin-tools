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
 * Executes [block] if `this` is `true`, and returns the result. If `this` is `false` or `null`, it
 * returns `null`.
 *
 * This can be used to replace:
 *
 * ```kotlin
 * if (someCondition) someValue else null
 * ```
 *
 * with a more functional-programming-friendly syntax:
 *
 * ```kotlin
 * someCondition.ifTrue { someValue }
 * ```
 *
 * It may be inefficient to invert the logic to `someValue.takeIf { someCondition }` when
 * `someValue` isn't readily available.
 */
fun <T> Boolean?.ifTrue(block: () -> T): T? =
    if (this == true) block() else null

/**
 * Returns `this` if it is not null. Otherwise, it executes [block] and returns the result.
 * This function behaves the same as the Elvis operator `?:`.
 *
 * This can be used to replace:
 *
 * ```kotlin
 * (
 *     someComponent.getSomeNullableValue()
 *         ?: someComponent.getFallbackValue()
 *     )
 *     .doSomethingWithTheValue()
 * ```
 *
 * or:
 *
 * ```kotlin
 * someComponent.getSomeNullableValue()
 *     .let { it ?: someComponent.getFallbackValue() }
 *     .doSomethingWithTheValue()
 * ```
 *
 * with a more functional-programming-friendly syntax:
 *
 * ```kotlin
 * someComponent.getSomeNullableValue()
 *     .ifNull { someComponent.getFallbackValue() }
 *     .doSomethingWithTheValue()
 * ```
 *
 * This method is not intended to replace all Elvis operators; `?;` is fine in many cases. In
 * chain-calling constructs however, the Elvis operator can easily lead to reduced readability.
 * `let` helps with this, but adds boilerplate that distracts from the functional flow.
 */
fun <T> T?.ifNull(block: () -> T) : T =
    this ?: block()
