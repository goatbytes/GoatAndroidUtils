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
import java.util.*

fun List<File>.comparator(
    type: SortType,
    direction: Direction = Direction.ASC,
    dirsFirst: Boolean = true
): FileComparator<File> = when (type) {
    SortType.NAME -> FileNameComparator(direction, dirsFirst)
    SortType.DATE -> FileDateComparator(direction, dirsFirst)
    SortType.SIZE -> FileSizeComparator(direction, dirsFirst)
    SortType.EXT -> FileExtensionComparator(direction, dirsFirst)
}

fun Array<File>.comparator(
    type: SortType,
    direction: Direction = Direction.ASC,
    dirsFirst: Boolean = true
): FileComparator<File> = when (type) {
    SortType.NAME -> FileNameComparator(direction, dirsFirst)
    SortType.DATE -> FileDateComparator(direction, dirsFirst)
    SortType.SIZE -> FileSizeComparator(direction, dirsFirst)
    SortType.EXT -> FileExtensionComparator(direction, dirsFirst)
}

fun List<File>.sortUsing(
    type: SortType,
    direction: Direction = Direction.ASC,
    dirsFirst: Boolean = true
) {
    Collections.sort(this, comparator(type, direction, dirsFirst))
}

fun Array<File>.sortUsing(
    type: SortType,
    direction: Direction = Direction.ASC,
    dirsFirst: Boolean = true
) {
    Arrays.sort(this, comparator(type, direction, dirsFirst))
}