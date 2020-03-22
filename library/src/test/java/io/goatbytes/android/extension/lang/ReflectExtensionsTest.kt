/*
 * Copyright (C) 2020 goatbytes.io
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

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class ReflectExtensionsTest {

    @Test
    fun `should get method via reflection`() {
        val method = StringBuilder().method("append", Int::class.java)
        val expectedName = "append"
        val expectedReturnType = StringBuilder::class.java
        assertEquals(expectedName, method?.name)
        assertEquals(expectedReturnType, method?.returnType)
    }

    @Test
    fun `should get field via reflection`() {
        val field = Class.forName("java.lang.Boolean").field("TRUE")

        val expectedType = "java.lang.Boolean"
        val expectedName = "TRUE"

        assertNotNull(field)
        assertEquals(expectedName, field?.name)
        assertEquals(expectedType, field?.type?.name)
    }

    @Test
    fun `should set field via reflection`() {
        val field = Class.forName("java.lang.Boolean").field("TRUE")
        checkNotNull(field)
        field.setValue(null, java.lang.Boolean.FALSE)
        assertEquals(java.lang.Boolean.FALSE, java.lang.Boolean.TRUE)
    }

    @Test
    fun `should invoke method via reflection`() {
        val obj = "foo"
        val types = arrayOf<Class<*>>(CharSequence::class.java, CharSequence::class.java)
        val result = obj.invoke<String>("replace", types, "foo", "bar")
        assertEquals("bar", result)

        val string = "baz"
        val length = string.invoke<Int>("length")
        assertEquals(string.length, length)
    }


}