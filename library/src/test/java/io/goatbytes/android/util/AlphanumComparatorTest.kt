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

package io.goatbytes.android.util

import org.junit.Assert.assertEquals
import org.junit.Test

class AlphanumComparatorTest {

    @Test
    fun `should sort list with alphanum comparator`() {
        val sorted = unsortedArray.sortedWith(AlphanumComparator)
        for (i in sortedArray.indices) {
            assertEquals(sortedArray[i], sorted[i])
        }
    }

    private val unsortedArray: Array<String>
        get() = arrayOf(
            "1000X Radonius Maximus",
            "10X Radonius",
            "200X Radonius",
            "20X Radonius",
            "20X Radonius Prime",
            "30X Radonius",
            "40X Radonius",
            "Allegia 50 Clasteron",
            "Allegia 500 Clasteron",
            "Allegia 50B Clasteron",
            "Allegia 51 Clasteron",
            "Allegia 6R Clasteron",
            "Alpha 100",
            "Alpha 2",
            "Alpha 200",
            "Alpha 2A",
            "Alpha 2A-8000",
            "Alpha 2A-900",
            "Callisto Morphamax",
            "Callisto Morphamax 500",
            "Callisto Morphamax 5000",
            "Callisto Morphamax 600",
            "Callisto Morphamax 6000 SE",
            "Callisto Morphamax 6000 SE2",
            "Callisto Morphamax 700",
            "Callisto Morphamax 7000",
            "Xiph Xlater 10000",
            "Xiph Xlater 2000",
            "Xiph Xlater 300",
            "Xiph Xlater 40",
            "Xiph Xlater 5",
            "Xiph Xlater 50",
            "Xiph Xlater 500",
            "Xiph Xlater 5000",
            "Xiph Xlater 58"
        )

    private val sortedArray: Array<String>
        get() = arrayOf(
            "10X Radonius",
            "20X Radonius",
            "20X Radonius Prime",
            "30X Radonius",
            "40X Radonius",
            "200X Radonius",
            "1000X Radonius Maximus",
            "Allegia 6R Clasteron",
            "Allegia 50 Clasteron",
            "Allegia 50B Clasteron",
            "Allegia 51 Clasteron",
            "Allegia 500 Clasteron",
            "Alpha 2",
            "Alpha 2A",
            "Alpha 2A-900",
            "Alpha 2A-8000",
            "Alpha 100",
            "Alpha 200",
            "Callisto Morphamax",
            "Callisto Morphamax 500",
            "Callisto Morphamax 600",
            "Callisto Morphamax 700",
            "Callisto Morphamax 5000",
            "Callisto Morphamax 6000 SE",
            "Callisto Morphamax 6000 SE2",
            "Callisto Morphamax 7000",
            "Xiph Xlater 5",
            "Xiph Xlater 40",
            "Xiph Xlater 50",
            "Xiph Xlater 58",
            "Xiph Xlater 300",
            "Xiph Xlater 500",
            "Xiph Xlater 2000",
            "Xiph Xlater 5000",
            "Xiph Xlater 10000"
        )

}