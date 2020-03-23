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

package io.goatbytes.android.util.io

import org.junit.Assert.assertEquals
import org.junit.Test

class PermissionParserTest {

    @Test
    fun `should convert symbolic to octal notation`() {
        permissions.forEach {
            assertEquals(it.octal, PermissionParser.parse(it.symbolic).octal)
        }
    }

    @Test
    fun `should convert octal to symbolic notation`() {
        permissions.forEach {
            assertEquals(it.symbolic, PermissionParser.parse(it.octal).permissions)
        }
    }

    @Test(expected = PermissionParser.PermissionParserError::class)
    fun `show throw exception given invalid input`() {
        PermissionParser("invalid permission string")
    }

    @Test fun `should parse file type`() {
        assertEquals(PermissionParser.TYPE_REGULAR, PermissionParser.parse("-rwxrw-rw-").type)
        assertEquals(PermissionParser.TYPE_DIRECTORY, PermissionParser.parse("drwxrw-rw-").type)
        assertEquals(PermissionParser.TYPE_SYMBOLIC_LINK, PermissionParser.parse("lrwxrw-rw-").type)
    }

    @Test fun `should parse owner permissions`() {
        arrayOf("rws", "rwx", "rw-", "r--", "---").forEach { perms ->
            val parser = PermissionParser.parse("$perms------")
            val group = parser.groups.owner
            assertEquals(perms, group.symbolic)
        }
    }

    @Test fun `should parse group permissions`() {
        arrayOf("rws", "rwx", "rw-", "r--", "---").forEach { perms ->
            val parser = PermissionParser.parse("---$perms---")
            val group = parser.groups.group
            assertEquals(perms, group.symbolic)
        }
    }

    @Test fun `should parse other permissions`() {
        arrayOf("rwt", "rwx", "rw-", "r--", "---").forEach { perms ->
            val parser = PermissionParser.parse("------$perms")
            val group = parser.groups.other
            assertEquals(perms, group.symbolic)
        }
    }

    private val permissions = arrayOf(
        PermissionStrings("0644", "rw-r--r--"),
        PermissionStrings("0755", "rwxr-xr-x"),
        PermissionStrings("0755", "rwxr-xr-x"),
        PermissionStrings("0700", "rwx------"),
        PermissionStrings("0775", "rwxrwxr-x"),
        PermissionStrings("0700", "rwx------"),
        PermissionStrings("0755", "rwxr-xr-x"),
        PermissionStrings("0700", "rwx------"),
        PermissionStrings("0700", "rwx------"),
        PermissionStrings("0700", "rwx------"),
        PermissionStrings("0700", "rwx------"),
        PermissionStrings("0755", "rwxr-xr-x"),
        PermissionStrings("0700", "rwx------"),
        PermissionStrings("0700", "rwx------"),
        PermissionStrings("0700", "rwx------"),
        PermissionStrings("0755", "rwxr-xr-x"),
        PermissionStrings("0700", "rwx------"),
        PermissionStrings("0755", "rwxr-xr-x"),
        PermissionStrings("0700", "rwx------"),
        PermissionStrings("0755", "rwxr-xr-x"),
        PermissionStrings("0755", "rwxr-xr-x"),
        PermissionStrings("0755", "rwxr-xr-x"),
        PermissionStrings("0644", "rw-r--r--"),
        PermissionStrings("0644", "rw-r--r--"),
        PermissionStrings("0775", "rwxrwxr-x"),
        PermissionStrings("0755", "rwxr-xr-x"),
        PermissionStrings("0755", "rwxr-xr-x")
    )

    class PermissionStrings(val octal: String, val symbolic: String)


}