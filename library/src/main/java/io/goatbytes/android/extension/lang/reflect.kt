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

package io.goatbytes.android.extension.lang

import java.lang.reflect.AccessibleObject
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.lang.reflect.Modifier

/**
 * Dynamically invoke a method associated with this object or class
 *
 * @param name The name of the method to invoke.
 * @param types The parameter types of the requested method.
 * @param args The arguments to the method
 * @param <T> The method's return type
 * @return The result of dynamically invoking this method.
 */
inline fun <reified T : Any> Any?.invoke(
    name: String,
    types: Array<Class<*>> = emptyArray(),
    vararg args: Any
): T? = try {
    val method = if (this is Method) this else method(name, *types)
    method?.invoke(this, *args) as? T
} catch (e: Exception) {
    null
}

/**
 * Set the field's value to the specified value.
 *
 * @param obj The object whose field should be modified
 * @param value The new value for the field of `obj` being modified
 * @return `true` if the field was set successfully.
 */
fun Field.setValue(obj: Any?, value: Any?): Boolean = try {
    if (!isAccessible) isAccessible = true
    if (Modifier.isFinal(modifiers)) {
        Field::class.java.getDeclaredField("modifiers").let {
            it.isAccessible = true
            it.setInt(this, modifiers and Modifier.FINAL.inv())
        }
    }
    set(obj, value); true
} catch (e: IllegalAccessException) {
    false
}

/**
 * Get the value from a field associated with this object or class
 *
 * @param name The name of the field
 * @param <T> The field's type
 * @return The value of the field in the specified object.
 */
inline fun <reified T : Any> Any?.getFieldValue(name: String): T? = try {
    val field: Field? = if (this is Field) this else field(name)
    field?.get(this) as? T
} catch (e: IllegalAccessException) {
    null
}

/**
 * Get a method from the object or class
 *
 * @param name The requested method's name
 * @param types The parameter types of the requested method.
 * @return A [Method] object which represents the method matching the specified name and parameter types
 */
fun Any?.method(name: String, vararg types: Class<*>): Method? {
    if (this == null) return null
    val key = cacheKey(this, name, *types)
    (cache[key] as? Method)?.let { return it }

    var klass: Class<*>? = this as? Class<*> ?: this.javaClass
    while (klass != null) {
        try {
            return klass.getDeclaredMethod(name, *types).apply {
                isAccessible = true
                cache[key] = this
            }
        } catch (ignored: NoSuchMethodException) {
        }
        klass = klass.superclass
    }

    return null
}

/**
 * Get a field from the object or class.
 *
 * @param name The name of the field
 * @return A [Field] object for the field with the given name which is declared in the class.
 */
fun Any?.field(name: String): Field? {
    if (this == null) return null
    val key = cacheKey(this, name)
    (cache[key] as? Field)?.let { return it }

    var klass: Class<*>? = this as? Class<*> ?: this.javaClass
    while (klass != null) {
        try {
            return klass.getDeclaredField(name).apply {
                isAccessible = true
                cache[key] = this
            }
        } catch (ignored: NoSuchFieldException) {
        }
        klass = klass.superclass
    }

    return null
}

private val cache by lazy { mutableMapOf<String, AccessibleObject>() }

private fun cacheKey(obj: Any, name: String, vararg types: Class<*>): String =
    buildString {
        val klass = obj as? Class<*> ?: obj::class.java
        append(klass.name).append('#').append(name)
        if (types.isNotEmpty()) {
            var separator = ""
            append('(')
            for (type in types) {
                append(separator).append(type.name)
                separator = ", "
            }
            append(')')
        }
    }
