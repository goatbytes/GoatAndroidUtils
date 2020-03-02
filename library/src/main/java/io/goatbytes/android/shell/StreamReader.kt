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
package io.goatbytes.android.shell

import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader

/**
 * Thread utility class continuously reading from an InputStream
 */
class StreamReader : Thread {

    /**
     * Line callback interface
     */
    interface OnLineListener {
        /**
         * Line callback
         *
         * This callback should process the line as quickly as possible. Delays in this callback may pause the
         * native process or even result in a deadlock
         *
         * @param line
         * String that was gobbled
         */
        fun onLine(line: String?)
    }

    private val reader: BufferedReader
    private var writer: MutableList<String>? = null
    private var listener: OnLineListener? = null

    /**
     * StreamGobbler constructor
     *
     * We use this class because shell STDOUT and STDERR should be read as quickly as possible to prevent a
     * deadlock from occurring, or Process.waitFor() never returning (as the buffer is full, pausing the native
     * process)
     *
     * @param inputStream InputStream to read from
     * @param outputList List<String> to write to, or null
     */
    constructor(
        inputStream: InputStream,
        outputList: MutableList<String>?
    ) {
        reader = BufferedReader(InputStreamReader(inputStream))
        writer = outputList
    }

    /**
     * StreamGobbler constructor
     *
     * We use this class because shell STDOUT and STDERR should be read as quickly as possible to prevent a
     * deadlock from occurring, or Process.waitFor() never returning (as the buffer is full, pausing the native
     * process)
     *
     * @param inputStream InputStream to read from
     * @param onLineListener OnLineListener callback
     */
    constructor(inputStream: InputStream, onLineListener: OnLineListener) {
        reader = BufferedReader(InputStreamReader(inputStream))
        listener = onLineListener
    }

    override fun run() {
        try {
            reader.forEachLine { line ->
                writer?.add(line)
                listener?.onLine(line)
            }
        } catch (e: IOException) {
            // reader probably closed, expected exit condition
        }
    }

}