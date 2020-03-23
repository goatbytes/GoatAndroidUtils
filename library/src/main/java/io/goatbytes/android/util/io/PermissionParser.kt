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

import java.util.regex.Matcher
import java.util.regex.Pattern

class PermissionParser(input: String) {

    private val matcher: Matcher by lazy {
        SYMBOLIC_PATTERN.matcher(symbolic).also { require(it.find()) }
    }

    val notation: Notation

    val octal: String

    val symbolic: String

    val type: Char? get() = matcher.group(INDEX_FILE_TYPE)?.getOrNull(0)

    val owner: String get() = matcher.group(INDEX_OWNER) ?: throw PermissionParserError()

    val group: String get() = matcher.group(INDEX_GROUP) ?: throw PermissionParserError()

    val other: String get() = matcher.group(INDEX_OTHER) ?: throw PermissionParserError()

    val permissions: String get() = matcher.group(INDEX_PERMISSION) ?: throw PermissionParserError()

    val groups: PermissionGroups by lazy {
        object : PermissionGroups {
            override val owner: Permission.Owner get() = create()
            override val group: Permission.Group get() = create()
            override val other: Permission.Other get() = create()
        }
    }

    init {
        val symbolicMatcher = SYMBOLIC_PATTERN.matcher(input)
        val octalMatcher = OCTAL_PATTERN.matcher(input)
        when {
            octalMatcher.matches() -> {
                octal = input
                symbolic = convertOctalToSymbolicNotation(octal)
                notation = Notation.OCTAL
            }
            symbolicMatcher.find() -> {
                symbolic = symbolicMatcher.group(0) ?: throw PermissionParserError()
                octal = convertSymbolicToOctalNotation(symbolic)
                notation = Notation.SYMBOLIC
            }
            else -> throw PermissionParserError("'$input' is not a valid linux permission")
        }
    }

    override fun toString(): String = permissions

    private inline fun <reified T : Permission> create(): T = (when (T::class) {
        Permission.Owner::class -> owner.let {
            Permission.Owner(
                it[0] != '-',
                it[1] != '-',
                it[2] != '-',
                it[2] == 's' || it[2] == 'S'
            )
        }
        Permission.Group::class -> group.let {
            Permission.Group(
                it[0] != '-',
                it[1] != '-',
                it[2] != '-',
                it[2] == 's' || it[2] == 'S'
            )
        }
        Permission.Other::class -> other.let {
            Permission.Other(
                it[0] != '-',
                it[1] != '-',
                it[2] != '-',
                it[2] == 't' || it[2] == 'T'
            )
        }
        else -> throw UnsupportedOperationException("Cannot create ${T::class.java.simpleName}")
    }) as T

    enum class Notation { OCTAL, SYMBOLIC; }

    interface PermissionGroups {
        val owner: Permission.Owner
        val group: Permission.Group
        val other: Permission.Other
    }

    sealed class Permission(
        val read: Boolean, val write: Boolean, val execute: Boolean, val special: Boolean
    ) {

        val symbolic: String
            get() = StringBuilder().apply {
                append(if (read) 'r' else '-')
                append(if (write) 'w' else '-')
                append(
                    if (special)
                        if (this@Permission::class == Other::class) 't' else 's'
                    else if (execute) 'x' else '-'
                )
            }.toString()

        override fun toString(): String = symbolic

        class Owner(
            read: Boolean, write: Boolean, execute: Boolean, setuid: Boolean
        ) : Permission(read, write, execute, setuid)

        class Group(
            read: Boolean, write: Boolean, execute: Boolean, setgid: Boolean
        ) : Permission(read, write, execute, setgid)

        class Other(
            read: Boolean, write: Boolean, execute: Boolean, sticky: Boolean
        ) : Permission(read, write, execute, sticky)
    }

    class PermissionParserError(message: String = "Error parsing permissions") : Exception(message)

    companion object {

        @Throws(PermissionParserError::class)
        fun parse(input: String) = PermissionParser(input)

        private const val OCTAL_REGEX = "^[0-7]{3,4}\$"

        private const val SYMBOLIC_REGEX =
            "^([bcdpl\\-sw?])?((([r\\-])([w\\-])([xsS\\-]))(([r\\-])([w\\-])([xsS\\-]))(([r\\-])([w\\-])([xtT\\-])))"
        // -----------------------read----write---execute----read----write---execute------read----write---execute---
        // [     file type    ][           owner           ][           group           ][         public          ]
        // ---------------------------------------------------------------------------------------------------------

        private val OCTAL_PATTERN = Pattern.compile(OCTAL_REGEX)
        private val SYMBOLIC_PATTERN = Pattern.compile(SYMBOLIC_REGEX)

        private const val INDEX_FILE_TYPE = 1
        private const val INDEX_PERMISSION = 2
        private const val INDEX_OWNER = 3
        private const val INDEX_GROUP = 7
        private const val INDEX_OTHER = 11

        // file mode:
        const val S_IRGRP = 32      // read permission, group
        const val S_IROTH = 4       // read permission, others
        const val S_IRUSR = 256     // read permission, owner
        const val S_IRWXG = 56      // read, write, execute/search by group
        const val S_IRWXO = 7       // read, write, execute/search by others
        const val S_IRWXU = 448     // read, write, execute/search by owner
        const val S_ISGID = 1024    // set-group-ID on execution
        const val S_ISUID = 2048    // set-user-ID on execution
        const val S_ISVTX = 512     // on directories, restricted deletion flag
        const val S_IWGRP = 16      // write permission, group
        const val S_IWOTH = 2       // write permission, others
        const val S_IWUSR = 128     // write permission, owner
        const val S_IXGRP = 8       // execute/search permission, group
        const val S_IXOTH = 1       // execute/search permission, others
        const val S_IXUSR = 64      // execute/search permission, owner

        // file type bits:
        const val S_IFMT = 61440    // type of file
        const val S_IFBLK = 24576   // block special
        const val S_IFCHR = 8192    // character special
        const val S_IFDIR = 16384   // directory
        const val S_IFIFO = 4096    // FIFO special
        const val S_IFLNK = 40960   // symbolic link
        const val S_IFREG = 32768   // regular
        const val S_IFSOCK = 49152  // socket special
        const val S_IFWHT = 57344   // whiteout special

        // file types:
        const val TYPE_BLOCK_SPECIAL = 'b'
        const val TYPE_CHARACTER_SPECIAL = 'c'
        const val TYPE_DIRECTORY = 'd'
        const val TYPE_FIFO = 'p'
        const val TYPE_SYMBOLIC_LINK = 'l'
        const val TYPE_REGULAR = '-'
        const val TYPE_SOCKET = 's'
        const val TYPE_WHITEOUT = 'w'
        const val TYPE_UNKNOWN = '?'

        // ownership flags:
        const val OWNER = 'u'
        const val GROUP = 'g'
        const val OTHER = 'a'

        /**
         *
         * Converts the numeric to the symbolic permission notation.
         *
         * Example: `convertOctalToSymbolicNotation("644")` would return "rw-r--r--"
         *
         * @param mode
         * An octal (base-8) notation as shown by `stat -c %a`. This notation consists of at
         * least three digits. Each of the three rightmost digits represents a different component of
         * the permissions: owner, group, and others.
         * @return the symbolic notation of the permission.
         */
        fun convertOctalToSymbolicNotation(mode: String): String {
            require(OCTAL_PATTERN.matcher(mode).matches()) { "Invalid octal permissions '$mode'" }

            val chars: CharArray
            val special: String

            when (mode.length) {
                4 -> {
                    special = when (mode[0]) {
                        '0' -> "---"
                        '1' -> "--t"
                        '2' -> "-s-"
                        '3' -> "-st"
                        '4' -> "s--"
                        '5' -> "s-t"
                        '6' -> "ss-"
                        '7' -> "sst"
                        else -> "---"
                    }
                    chars = mode.substring(1).toCharArray()
                }
                else -> {
                    special = "---"
                    chars = mode.toCharArray()
                }
            }

            var permissions = ""
            for (i in 0..2) {
                val s = special[i]
                when (chars[i]) {
                    '0' -> permissions += if (s == '-') "---" else "--" + Character.toUpperCase(s)
                    '1' -> permissions += if (s == '-') "--x" else "--$s"
                    '2' -> permissions += "-w-"
                    '3' -> permissions += if (s == '-') "-wx" else "-w$s"
                    '4' -> permissions += if (s == '-') "r--" else "r-" + Character.toUpperCase(s)
                    '5' -> permissions += if (s == '-') "r-x" else "r-$s"
                    '6' -> permissions += "rw-"
                    '7' -> permissions += if (s == '-') "rwx" else "rw$s"
                }
            }

            return permissions
        }

        /**
         *
         * Converts the symbolic to the numeric permission notation.
         *
         * Example: `convertSymbolicToNumericNotation("rwxr-xr-x")` would return "755"
         *
         * @param permissions The first character (optional) indicates the file type and is not related to
         * permissions.
         * The remaining nine characters are in three sets, each representing a class of
         * permissions as three characters. The first set represents the user class. The second set
         * represents the group class. The third set represents the others class. Examples:
         * "-rwxr-xr-x", "rw-r--r--", "drwxr-xr-x"
         * @return the mode
         */
        fun convertSymbolicToOctalNotation(permissions: String): String {
            val m = SYMBOLIC_PATTERN.matcher(permissions)
            require(m.find()) { "Invalid permission string: $permissions" }
            val special = permissions.modeSpecial()
            val owner = m.group(INDEX_OWNER)!!.mode()
            val group = m.group(INDEX_GROUP)!!.mode()
            val other = m.group(INDEX_OTHER)!!.mode()
            return "$special$owner$group$other"
        }

        /**
         * Converts the file permissions mode to the numeric notation.
         *
         * @param st_mode Mode (permissions) of file. Corresponds to C's `struct stat` from `<stat.h>`.
         * See [android.system.StructStat.st_mode]
         * @return The permission represented as a numeric notation.
         */
        fun toNumericNotation(st_mode: Int): String {
            var i = 0
            // --- owner ---------------------------------------
            i += if (st_mode and S_IRUSR != 0) S_IRUSR else 0
            i += if (st_mode and S_IWUSR != 0) S_IWUSR else 0
            when (st_mode and (S_IXUSR or S_ISUID)) {
                S_IXUSR -> i += S_IXUSR
                S_ISUID -> i += S_ISUID
                S_IXUSR or S_ISUID -> i += S_IXUSR + S_ISUID
            }
            // --- group ---------------------------------------
            i += if (st_mode and S_IRGRP != 0) S_IRGRP else 0
            i += if (st_mode and S_IWGRP != 0) S_IWGRP else 0
            when (st_mode and (S_IXGRP or S_ISGID)) {
                S_IXGRP -> i += S_IXGRP
                S_ISGID -> i += S_ISGID
                S_IXGRP or S_ISGID -> i += S_IXGRP + S_ISGID
            }
            // --- other ---------------------------------------
            i += if (st_mode and S_IROTH != 0) S_IROTH else 0
            i += if (st_mode and S_IWOTH != 0) S_IWOTH else 0
            when (st_mode and (S_IXOTH or S_ISVTX)) {
                S_IXOTH -> i += S_IXOTH
                S_ISVTX -> i += S_ISVTX
                S_IXOTH or S_ISVTX -> i += S_IXOTH + S_ISVTX
            }
            return Integer.toOctalString(i)
        }

        /**
         * Converts the file permissions mode to the symbolic notation.
         *
         * @param st_mode Mode (permissions) of file. Corresponds to C's `struct stat` from `<stat.h>`.
         * See [android.system.StructStat.st_mode]
         * @return The permission represented as a symbolic notation.
         */
        fun toSymbolicNotation(st_mode: Int): String {
            var p = ""

            p += when (st_mode and S_IFMT) {
                S_IFDIR -> TYPE_DIRECTORY
                S_IFCHR -> TYPE_CHARACTER_SPECIAL
                S_IFBLK -> TYPE_BLOCK_SPECIAL
                S_IFREG -> TYPE_REGULAR
                S_IFLNK -> TYPE_SYMBOLIC_LINK
                S_IFSOCK -> TYPE_SOCKET
                S_IFIFO -> TYPE_FIFO
                S_IFWHT -> TYPE_WHITEOUT
                else -> TYPE_UNKNOWN
            }

            /* owner */
            p += if (st_mode and S_IRUSR != 0) 'r' else '-'
            p += if (st_mode and S_IWUSR != 0) 'w' else '-'
            p += when (st_mode and (S_IXUSR or S_ISUID)) {
                S_IXUSR -> 'x'
                S_ISUID -> 'S'
                S_IXUSR or S_ISUID -> 's'
                else -> '-'
            }

            /* group */
            p += if (st_mode and S_IRGRP != 0) 'r' else '-'
            p += if (st_mode and S_IWGRP != 0) 'w' else '-'
            p += when (st_mode and (S_IXGRP or S_ISGID)) {
                S_IXGRP -> 'x'
                S_ISGID -> 'S'
                S_IXGRP or S_ISGID -> 's'
                else -> '-'
            }

            /* other */
            p += if (st_mode and S_IROTH != 0) 'r' else '-'
            p += if (st_mode and S_IWOTH != 0) 'w' else '-'
            p += when (st_mode and (S_IXOTH or S_ISVTX)) {
                S_IXOTH -> 'x'
                S_ISVTX -> 'T'
                S_IXOTH or S_ISVTX -> 't'
                else -> '-'
            }
            return p
        }

        private fun String.modeSpecial(): Int {
            val m = SYMBOLIC_PATTERN.matcher(this)
            require(m.find())
            val permission = m.group(INDEX_PERMISSION)?.toLowerCase()
            require(permission != null)
            var mode = 0
            if (permission[2] == 's') mode += 4
            if (permission[5] == 's') mode += 2
            if (permission[8] == 't') mode += 1
            return mode
        }

        private fun String.mode(): Int {
            require(matches("[rwxstST\\-]{3}".toRegex()))
            var mode = 0
            if (this[0] == 'r') mode += 4
            if (this[1] == 'w') mode += 2
            when (this[2]) {
                'x', 's', 't', 'S', 'T' -> mode += 1
            }
            return mode
        }

    }

}