/*
 * Copyright (C) 2019 goatbytes.io
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.goatbytes.android.util

import android.os.Looper
import java.util.*


fun <T : Any> coalesce(vararg options: T?): T? = options.firstOrNull { it != null }

inline fun <reified T : Any> tryOrNull(block: () -> T): T? = try {
    block()
} catch (e: Exception) {
    null
}

inline fun tryOrIgnore(block: () -> Unit) = try {
    block()
} catch (e: Exception) {
    // ignored
}

inline operator fun <reified T : Any> Any.get(vararg array: T): Array<T> = arrayOf(*array)

fun <T1 : Any, T2 : Any, R : Any> safeLet(
    p1: T1?, p2: T2?, block: (T1, T2) -> R?
): R? = if (p1 != null && p2 != null)
    block(p1, p2) else null

fun <T1 : Any, T2 : Any, T3 : Any, R : Any> safeLet(
    p1: T1?, p2: T2?, p3: T3?, block: (T1, T2, T3) -> R?
): R? = if (p1 != null && p2 != null && p3 != null)
    block(p1, p2, p3) else null

fun <T1 : Any, T2 : Any, T3 : Any, T4 : Any, R : Any> safeLet(
    p1: T1?, p2: T2?, p3: T3?, p4: T4?, block: (T1, T2, T3, T4) -> R?
): R? = if (p1 != null && p2 != null && p3 != null && p4 != null)
    block(p1, p2, p3, p4) else null

fun <T1 : Any, T2 : Any, T3 : Any, T4 : Any, T5 : Any, R : Any> safeLet(
    p1: T1?, p2: T2?, p3: T3?, p4: T4?, p5: T5?, block: (T1, T2, T3, T4, T5) -> R?
): R? = if (p1 != null && p2 != null && p3 != null && p4 != null && p5 != null)
    block(p1, p2, p3, p4, p5) else null

fun <T1 : Any, T2 : Any, T3 : Any, T4 : Any, T5 : Any, T6 : Any, R : Any> safeLet(
    p1: T1?, p2: T2?, p3: T3?, p4: T4?, p5: T5?, p6: T6?, block: (T1, T2, T3, T4, T5, T6) -> R?
): R? = if (p1 != null && p2 != null && p3 != null && p4 != null && p5 != null && p6 != null)
    block(p1, p2, p3, p4, p5, p6) else null

fun <T1 : Any, T2 : Any, T3 : Any, T4 : Any, T5 : Any, T6 : Any, T7 : Any, R : Any> safeLet(
    p1: T1?, p2: T2?, p3: T3?, p4: T4?, p5: T5?, p6: T6?, p7: T7?,
    block: (T1, T2, T3, T4, T5, T6, T7) -> R?
): R? =
    if (p1 != null && p2 != null && p3 != null && p4 != null && p5 != null && p6 != null && p7 != null)
        block(p1, p2, p3, p4, p5, p6, p7) else null

fun <T : Any, R : Any> whenAllNotNull(vararg options: T?, block: (List<T>) -> R) {
    if (options.all { it != null }) {
        block(options.filterNotNull())
    }
}

fun <T : Any, R : Any> whenAnyNotNull(vararg options: T?, block: (List<T>) -> R) {
    if (options.any { it != null }) {
        block(options.filterNotNull())
    }
}

fun now(): Date = Date()

fun nowMillis(): Long = System.currentTimeMillis()

inline val isOnMainThread: Boolean get() = Looper.myLooper() == Looper.getMainLooper()
