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

package io.goatbytes.android.util.comparators.file

import io.goatbytes.android.util.comparators.AlphanumComparator
import java.io.File

open class FileSizeComparator(
    direction: Direction = Direction.ASC, dirsFirst: Boolean = true
) : FolderFirstComparator<File>(direction, dirsFirst) {

    override fun comparison(f1: File, f2: File): Int =
        if (f1.isDirectory && f2.isDirectory) f1 alphanum f2 else {
            arrayOf(getSize(f1), getSize(f2)).let { (size1, size2) ->
                when (direction) {
                    Direction.ASC -> when {
                        size1 > size2 -> 1; size1 < size2 -> -1; else -> f1 alphanum f2
                    }
                    Direction.DESC -> when {
                        size1 < size2 -> 1; size1 > size2 -> -1; else -> f1 alphanum f2
                    }
                }
            }
        }

    /**
     * Get the size of a file.
     *
     * @param f the file to calculate the size of
     * @return the size of the file in bytes
     */
    open fun getSize(f: File): Long = f.length()

    private infix fun File.alphanum(other: File): Int = AlphanumComparator.compare(name, other.name)

}