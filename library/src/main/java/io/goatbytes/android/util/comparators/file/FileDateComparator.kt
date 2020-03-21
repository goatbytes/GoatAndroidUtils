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

import java.io.File

class FileDateComparator(
    direction: Direction = Direction.ASC, dirsFirst: Boolean = true
) : FolderFirstComparator<File>(direction, dirsFirst) {

    override fun comparison(f1: File, f2: File): Int =
        arrayOf(f1.lastModified(), f2.lastModified()).let { (time1, time2) ->
            when (direction) {
                Direction.ASC -> when {
                    time1 > time2 -> 1; time1 < time2 -> -1; else -> 0
                }
                Direction.DESC -> when {
                    time1 < time2 -> 1; time1 > time2 -> -1; else -> 0
                }
            }
        }

}