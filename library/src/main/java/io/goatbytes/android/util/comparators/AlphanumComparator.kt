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

package io.goatbytes.android.util.comparators

/**
 * The alpha-num Algorithm is an improved sorting algorithm for strings containing numbers.
 * Instead of sorting numbers in ASCII order like a standard sort, this algorithm sorts numbers
 * in numeric order.
 */
object AlphanumComparator : Comparator<String> {

    override fun compare(s1: String, s2: String): Int {
        var thisMarker = 0
        var thatMarker = 0
        val s1Length = s1.length
        val s2Length = s2.length
        while (thisMarker < s1Length && thatMarker < s2Length) {
            val thisChunk =
                getChunk(
                    s1,
                    s1Length,
                    thisMarker
                )
            thisMarker += thisChunk.length
            val thatChunk =
                getChunk(
                    s2,
                    s2Length,
                    thatMarker
                )
            thatMarker += thatChunk.length
            // If both chunks contain numeric characters, sort them numerically
            var result: Int
            if (isDigit(
                    thisChunk[0]
                ) && isDigit(
                    thatChunk[0]
                )
            ) {
                // Simple chunk comparison by length.
                val thisChunkLength = thisChunk.length
                result = thisChunkLength - thatChunk.length
                // If equal, the first different number counts
                if (result == 0) {
                    for (i in 0 until thisChunkLength) {
                        result = thisChunk[i] - thatChunk[i]
                        if (result != 0) {
                            return result
                        }
                    }
                }
            } else {
                result = thisChunk.compareTo(thatChunk, ignoreCase = true)
            }
            if (result != 0) return result
        }
        return s1Length - s2Length
    }

    private fun isDigit(ch: Char): Boolean = ch.toInt() in 48..57

    /** Length of string is passed in for improved efficiency (only need to calculate it once)  */
    private fun getChunk(s: String, slength: Int, m: Int): String = buildString {
        var marker = m
        var c = s[marker]
        append(c)
        marker++
        if (isDigit(c)) {
            while (marker < slength) {
                c = s[marker]
                if (!isDigit(
                        c
                    )
                ) break
                append(c)
                marker++
            }
        } else {
            while (marker < slength) {
                c = s[marker]
                if (isDigit(
                        c
                    )
                ) break
                append(c)
                marker++
            }
        }
    }

}

