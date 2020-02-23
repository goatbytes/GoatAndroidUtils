/*
 * Copyright (C) 2019 goatbytes.io
 * Copyright (C) 2017 The Android Open Source Project
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

package io.goatbytes.android.extension.os

import android.os.Binder
import android.os.Bundle
import android.os.Parcelable
import android.util.Size
import android.util.SizeF
import java.io.Serializable

/**
 * Operator function to set the put an item in a bundle
 *
 *     bundle["key"] = value
 */
inline operator fun Bundle.set(key: String, value: Any?): Unit =
    put(key, value)

/**
 * Operator function to get a bundle value with a default non-null value
 *
 *     val foo: String = bundle["key", "defualt value"]
 */
inline operator fun <reified T : Any> Bundle.get(key: String, defValue: T): T =
    get(key) as? T ?: defValue

/**
 * Returns a new [Bundle] with the given key/value pairs as elements.
 *
 * @throws IllegalArgumentException When a value is not a supported type of [Bundle].
 */
inline fun Bundle.put(key: String, value: Any?) {
    when (value) {
        null -> putString(key, null) // Any nullable type will suffice.

        // Scalars
        is Boolean -> putBoolean(key, value)
        is Byte -> putByte(key, value)
        is Char -> putChar(key, value)
        is Double -> putDouble(key, value)
        is Float -> putFloat(key, value)
        is Int -> putInt(key, value)
        is Long -> putLong(key, value)
        is Short -> putShort(key, value)

        // References
        is Bundle -> putBundle(key, value)
        is String -> putString(key, value)
        is CharSequence -> putCharSequence(key, value)
        is Parcelable -> putParcelable(key, value)

        // Scalar arrays
        is BooleanArray -> putBooleanArray(key, value)
        is ByteArray -> putByteArray(key, value)
        is CharArray -> putCharArray(key, value)
        is DoubleArray -> putDoubleArray(key, value)
        is FloatArray -> putFloatArray(key, value)
        is IntArray -> putIntArray(key, value)
        is LongArray -> putLongArray(key, value)
        is ShortArray -> putShortArray(key, value)

        // Reference arrays
        is Array<*> -> {
            val componentType = value::class.java.componentType!!
            @Suppress("UNCHECKED_CAST") // Checked by reflection.
            when {
                Parcelable::class.java.isAssignableFrom(componentType) -> {
                    putParcelableArray(key, value as Array<Parcelable>)
                }
                String::class.java.isAssignableFrom(componentType) -> {
                    putStringArray(key, value as Array<String>)
                }
                CharSequence::class.java.isAssignableFrom(componentType) -> {
                    putCharSequenceArray(key, value as Array<CharSequence>)
                }
                Serializable::class.java.isAssignableFrom(componentType) -> {
                    putSerializable(key, value)
                }
                else -> {
                    val valueType = componentType.canonicalName
                    throw IllegalArgumentException("Illegal value array type $valueType for key \"$key\"")
                }
            }
        }
        is ArrayList<*> -> {
            val componentType = value::class.java.componentType!!
            @Suppress("UNCHECKED_CAST") // Checked by reflection.
            when {
                Parcelable::class.java.isAssignableFrom(componentType) -> {
                    putParcelableArrayList(key, value as ArrayList<Parcelable>)
                }
                Int::class.java.isAssignableFrom(componentType) -> {
                    putIntegerArrayList(key, value as ArrayList<Int>)
                }
                String::class.java.isAssignableFrom(componentType) -> {
                    putStringArrayList(key, value as ArrayList<String>)
                }
                CharSequence::class.java.isAssignableFrom(componentType) -> {
                    putCharSequenceArrayList(key, value as ArrayList<CharSequence>)
                }
                else -> {
                    val valueType = componentType.canonicalName
                    throw IllegalArgumentException("Illegal value array type $valueType for key \"$key\"")
                }
            }
        }

        // Last resort. Also we must check this after Array<*> as all arrays are serializable.
        is Serializable -> putSerializable(key, value)
        is Binder -> putBinder(key, value)
        is Size -> putSize(key, value)
        is SizeF -> putSizeF(key, value)
        else -> {
            val valueType = value.javaClass.canonicalName
            throw IllegalArgumentException("Illegal value type $valueType for key \"$key\"")
        }
    }
}

inline fun Bundle.forEach(action: (key: String, value: Any?) -> Unit) {
    keySet().forEach { key ->
        action(key, get(key))
    }
}
