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
import java.util.*


class Shell {




    ///////

    /**
     * Exception thrown when a shell could not be opened.
     */
    open class NotFoundException : RuntimeException {
        /**
         * Constructs an NotFoundException with no detail message.
         */
        constructor()

        /**
         * Constructs an NotFoundException with the specified detail message.
         *
         * @param message the detail message.
         */
        constructor(message: String) : super(message)

        /**
         * Constructs a new exception with the specified detail message and cause.
         *
         * @param  message the detail message.
         * @param  cause the cause of this exception.
         */
        constructor(message: String, cause: Throwable?) : super(message, cause)

        /**
         * Constructs a new exception with the specified cause
         *
         * @param  cause the cause of this exception.
         */
        constructor(cause: Throwable?) : super(cause)

    }

    /**
     * Line callback interface
     */
    interface OnLineListener {
        /**
         * Line callback
         *
         * This callback should process the line as quickly as possible.
         * Delays in this callback may pause the native process or even result in a deadlock.
         *
         * @param line String that was read
         */
        fun onLine(line: String)
    }

    /**
     * Thread utility class continuously reading from an InputStream
     */
    class StreamGobbler : Thread {

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
         * @param stream  InputStream to read from
         * @param writer List<String> to write to, or null
         */
        constructor(stream: InputStream, writer: MutableList<String>?) : this(stream, writer, null)

        /**
         *
         * StreamGobbler constructor
         *
         * We use this class because shell STDOUT and STDERR should be read as quickly as possible to prevent a
         * deadlock from occurring, or Process.waitFor() never returning (as the buffer is full, pausing the native
         * process)
         *
         * @param stream  InputStream to read from
         * @param listener OnLineListener callback
         */
        constructor(stream: InputStream, listener: OnLineListener) : this(stream, null, listener)

        private constructor(
            stream: InputStream,
            writer: MutableList<String>? = null,
            listener: OnLineListener? = null
        ) {
            this.reader = BufferedReader(InputStreamReader(stream))
            this.listener = listener
            this.writer = writer
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

    class Result(stdout: List<String>, stderr: List<String>, val exitCode: Int) : Streams<String> {

        val streams = object : Streams<List<String>> {
            override val stdout = stdout
            override val stderr = stderr
        }

        override val stdout: String get() = streams.stdout.string

        override val stderr: String get() = streams.stderr.string

        override fun toString(): String = stdout

        fun forEach(action: (line: String) -> Unit, stream: Stream = Stream.OUT) {
            when (stream) {
                Stream.OUT -> streams.stdout
                Stream.ERR -> streams.stderr
                else -> throw UnsupportedOperationException()
            }.forEach(action)
        }

        private val List<String>.string: String
            get() = StringBuilder().let { sb ->
                var separator = '\u0000'
                forEach { line -> sb.append(separator).append(line).also { separator = '\n' } }
            }.toString()

    }

    interface Streams<T> {
        val stdout: T
        val stderr: T
    }

    enum class Stream {
        IN, OUT, ERR
    }

    object ExitCode {
        const val SUCCESS = 0
        const val TERMINATED = 130
        const val COMMAND_NOT_EXECUTABLE = 126
        const val COMMAND_NOT_FOUND = 127

        internal object Internal {
            internal const val WATCHDOG_EXIT = -1
            internal const val SHELL_DIED = -2
            internal const val SHELL_EXEC_FAILED = -3
            internal const val SHELL_WRONG_UID = -4
            internal const val SHELL_NOT_FOUND = -5
        }

    }

    /**
     * Command result callback, notifies the recipient of the completion of a command block, including the (last) exit
     * code, and the full output
     */
    interface OnCommandResultListener {
        /**
         * Command result callback
         *
         * Depending on how and on which thread the shell was created, this callback may be executed on one of the
         * gobbler threads. In that case, it is important the callback returns as quickly as possible, as delays in this
         * callback may pause the native process or even result in a deadlock
         *
         * See [Shell.Interactive] for threading details
         *
         * @param commandCode Value previously supplied to addCommand
         * @param exitCode Exit code of the last command in the block
         * @param output All output generated by the command block
         */
        fun onCommandResult(
            commandCode: Int,
            exitCode: Int,
            output: List<String>
        )
    }

    /**
     * Command per line callback for parsing the output line by line without buffering It also notifies the recipient
     * of the completion of a command block, including the (last) exit code.
     */
    interface OnCommandLineListener : OnLineListener {
        /**
         * Command result callback
         *
         * Depending on how and on which thread the shell was created, this callback may be executed on one of the
         * gobbler threads. In that case, it is important the callback returns as quickly as possible, as delays in this
         * callback may pause the native process or even result in a deadlock
         *
         * See [Shell.Interactive] for threading details
         *
         * @param commandCode Value previously supplied to addCommand
         * @param exitCode Exit code of the last command in the block
         */
        fun onCommandResult(commandCode: Int, exitCode: Int)
    }

    /**
     * Internal class to store command block properties
     */
    private class Command(
        private val commands: Array<String>,
        private val code: Int,
        val onCommandResultListener: OnCommandResultListener,
        val onCommandLineListener: OnCommandLineListener
    ) {

        private val marker: String

        init {
            marker = UUID.randomUUID().toString() + String.format("-%08x", ++commandCounter)
        }

        companion object {
            private var commandCounter = 0
        }
    }

}