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

class FileExtensionComparator(
    direction: Direction = Direction.ASC, dirsFirst: Boolean = true
) : FolderFirstComparator<File>(direction, dirsFirst) {

    override fun comparison(f1: File, f2: File): Int =
        if (f1.isDirectory && !f2.isDirectory) -1
        else if (!f1.isDirectory && f2.isDirectory) 1
        else when (direction) {
            Direction.ASC -> f1 alphanum f2
            Direction.DESC -> f2 alphanum f1
        }

    private infix fun File.alphanum(other: File): Int =
        AlphanumComparator.compare(extension, other.extension)

}