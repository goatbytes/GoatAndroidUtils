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

abstract class FolderFirstComparator<T : File>(
    override val direction: Direction = Direction.ASC,
    override val dirsFirst: Boolean = true
) : FileComparator<T> {

    override fun compare(f1: T, f2: T): Int {
        if (dirsFirst) {
            val isLhsDir = f1.isDirectory
            val isRhsDir = f2.isDirectory
            if ((isLhsDir || isRhsDir) && (!isLhsDir || !isRhsDir)) {
                return if (isLhsDir) -1 else 1
            }
        }
        return comparison(f1, f2)
    }

    internal abstract fun comparison(f1: T, f2: T): Int

}