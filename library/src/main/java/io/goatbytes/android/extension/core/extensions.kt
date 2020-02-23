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

@file:Suppress("NOTHING_TO_INLINE")

package io.goatbytes.android.extension.core

import android.text.Html
import android.text.Spanned
import android.text.TextUtils
import androidx.core.text.HtmlCompat
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.*

// region Boolean

inline infix fun <T : Any> Boolean.then(value: T?): T? = if (this) value else null

inline infix fun <reified T> Boolean.then(function: () -> T): T? = if (this) function() else null

// endregion

// region Collections

fun <T : Any> Collection<T?>.coalesce(): T? = firstOrNull { it != null }

fun <T : Any, R : Any> Collection<T?>.whenAllNotNull(block: (List<T>) -> R) {
    if (all { it != null }) block(filterNotNull())
}

fun <T : Any, R : Any> Collection<T?>.whenAnyNotNull(block: (List<T>) -> R) {
    if (any { it != null }) block(filterNotNull())
}

fun <T> Array<T>.swap(i: Int, j: Int): Array<T> {
    return apply {
        val aux = this[i]
        this[i] = this[j]
        this[j] = aux
    }
}

fun <T> MutableList<T>.swap(i: Int, j: Int): MutableList<T> {
    return apply {
        val aux = this[i]
        this[i] = this[j]
        this[j] = aux
    }
}

fun <T> MutableCollection<T>.replace(src: Collection<T>) {
    clear()
    addAll(src)
}

// endregion

// region Strings

/**
 * Supported algorithms on Android:
 *
 * Algorithm      Supported API Levels
 * MD5            1+
 * SHA-1          1+
 * SHA-224        1-8,22+
 * SHA-256        1+
 * SHA-384        1+
 * SHA-512        1+
 */
@Throws(NoSuchAlgorithmException::class)
fun String.hash(type: String): String = MessageDigest.getInstance(type)
    .digest(toByteArray()).let { bytes ->
        buildString(bytes.size * 2) {
            val chars = "0123456789ABCDEF"
            bytes.forEach {
                val i = it.toInt()
                append(chars[i shr 4 and 0x0f])
                append(chars[i and 0x0f])
            }
        }
    }

val String.MD5 get() = hash("MD5")

val String.SHA_256 get() = hash("SHA-256")

val String.SHA_512 get() = hash("SHA-512")

/**
 * @return A new string with only digits from the original string
 */
val String.digits: String
    get() = String(toCharArray().filter { Character.isDigit(it) }.toCharArray())

/**
 * @return displayable styled text from the provided HTML string with the legacy flags
 */
val String.html: Spanned get() = HtmlCompat.fromHtml(this, HtmlCompat.FROM_HTML_MODE_LEGACY)

/**
 * Returns a [Spanned] from parsing this string as HTML.
 *
 * @param flags Additional option to set the behavior of the HTML parsing. Default is set to
 * [Html.FROM_HTML_MODE_LEGACY] which was introduced in API 24.
 * @param imageGetter Returns displayable styled text from the provided HTML string.
 * @param tagHandler Notified when HTML tags are encountered a tag the parser does
 * not know how to interpret.
 *
 * @see Html.fromHtml
 */
inline fun String.parseAsHtml(
    flags: Int = HtmlCompat.FROM_HTML_MODE_LEGACY,
    imageGetter: Html.ImageGetter? = null,
    tagHandler: Html.TagHandler? = null
): Spanned = HtmlCompat.fromHtml(this, flags, imageGetter, tagHandler)

/**
 * Html-encode the string.
 *
 * @see TextUtils.htmlEncode
 */
inline fun String.htmlEncode(): String = TextUtils.htmlEncode(this)

/** Prints the string and the line separator to the standard output stream. */
fun String.println() = println(this)

/** Prints the string to the standard output stream. */
fun String.print() = print(this)

/**
 * Returns a copy of this string having its first letter titlecased preferring [Char.toTitleCase]
 * (if different from [Char.toUpperCase]) or by [String.toUpperCase] using the specified [locale],
 * or the original string, if it's empty or already starts with an upper case letter.
 *
 * @see [kotlin.text.capitalize]
 */
fun String.capitalize(locale: Locale): String {
    if (isNotEmpty()) {
        val firstChar = this[0]
        if (firstChar.isLowerCase()) {
            return buildString {
                val titleChar = firstChar.toTitleCase()
                if (titleChar != firstChar.toUpperCase()) {
                    append(titleChar)
                } else {
                    append(this@capitalize.substring(0, 1).toUpperCase(locale))
                }
                append(this@capitalize.substring(1))
            }
        }
    }
    return this
}

// endregion

// region ByteArray

fun ByteArray.toInt32(order: ByteOrder = ByteOrder.nativeOrder()): Int =
    ByteBuffer.wrap(copyOf()).order(order).asIntBuffer().get()

fun ByteArray.toInt16(order: ByteOrder = ByteOrder.nativeOrder()): Short =
    ByteBuffer.wrap(copyOf()).order(order).asShortBuffer().get()

fun ByteArray.toInt8(order: ByteOrder = ByteOrder.nativeOrder()): Byte =
    ByteBuffer.wrap(copyOf()).order(order).asReadOnlyBuffer().get()

fun ByteArray.toFloat(order: ByteOrder = ByteOrder.nativeOrder()): Float =
    ByteBuffer.wrap(copyOf()).order(order).asFloatBuffer().get()

fun ByteArray.toDouble(order: ByteOrder = ByteOrder.nativeOrder()): Double =
    ByteBuffer.wrap(copyOf()).order(order).asDoubleBuffer().get()

fun ByteArray.toChar(order: ByteOrder = ByteOrder.nativeOrder()): Char =
    ByteBuffer.wrap(copyOf()).order(order).asCharBuffer().get()

fun ByteArray.toByteBuffer(order: ByteOrder = ByteOrder.nativeOrder()): ByteBuffer =
    ByteBuffer.wrap(copyOf()).order(order).asReadOnlyBuffer()

inline fun <reified T : Number> ByteArray.read(range: IntRange = 0 until size): T =
    copyOfRange(range.first, range.last + 1).run {
        (when (T::class) {
            Float::class -> toFloat()
            Double::class -> toDouble()
            Byte::class -> toInt8()
            Short::class -> toInt16()
            Int::class -> toInt32()
            Long::class -> toInt32().toLong() as T
            else -> throw IllegalArgumentException("${T::class.java.simpleName} is not supported")
        }) as T
    }

inline operator fun <reified T : Number> ByteArray.get(rng: IntRange = 0 until size): T = read(rng)

// endregion
