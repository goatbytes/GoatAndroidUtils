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

import android.os.Handler
import android.os.Looper
import androidx.annotation.WorkerThread
import io.goatbytes.android.shell.Shell.ExitCode.Internal.SHELL_WRONG_UID
import java.io.*
import java.util.*
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.regex.Pattern


class Shell {

    companion object {

        internal val AVAILABLE_TEST_COMMANDS = arrayOf("echo -BOC-", "id")

        /**
         * See if the shell is alive, and if so, check the UID
         *
         * @param stdout Standard output from running AVAILABLE_TEST_COMMANDS
         * @param checkForRoot true if we are expecting this shell to be running as root
         * @return true on success, false on error
         */
        internal fun parseAvailableResult(stdout: List<String>?, checkForRoot: Boolean): Boolean {
            if (stdout == null) return false
            for (line in stdout) {
                return if (line.contains("uid=")) {
                    // id command is working, let's see if we are actually root
                    !checkForRoot || line.contains("uid=0")
                } else if (line.contains("-BOC-")) {
                    // if we end up here, at least the su command starts some kind of shell,
                    // let's hope it has root privileges -
                    // no way to know without additional native binaries
                    true
                } else continue
            }
            return false
        }

        /**
         * Attempts to deduce if the shell command refers to a su shell
         *
         * @param shell Shell command to run
         * @return Shell command appears to be su
         */
        internal fun isSU(shell: String): Boolean =
            Pattern.compile("^(.+\\/)?(\\w+)").matcher(shell).group(2) == "su"

        /**
         * This code is adapted from java.lang.ProcessBuilder.start().
         *
         * The problem is that Android doesn't allow us to modify the map returned by ProcessBuilder.environment(), even
         * though the JavaDoc indicates that it should. This is because it simply returns the SystemEnvironment object that
         * System.getenv() gives us. The relevant portion in the source code is marked as "// android changed", so
         * presumably it's not the case in the original version of the Apache Harmony project.
         *
         * @param environment Map of all environment variables
         * @return new [Process] instance.
         * @throws IOException if the requested program could not be executed.
         */
        @WorkerThread
        @Throws(IOException::class)
        fun String.exec(environment: Array<String> = emptyArray()): Process =
            Runtime.getRuntime().exec(
                this, if (environment.isNotEmpty()) {
                    mutableListOf<String>().apply {
                        System.getenv().forEach { (key, value) -> add("$key=$value") }
                        environment.forEach loop@{ env ->
                            val split = env.indexOf('=').takeIf { it >= 0 } ?: return@loop
                            val key = env.substring(0, split)
                            val value = env.substring(split + 1)
                            add("$key=$value")
                        }
                    }.toTypedArray()
                } else null
            )

        /**
         * This code is adapted from java.lang.ProcessBuilder.start().
         *
         * The problem is that Android doesn't allow us to modify the map returned by ProcessBuilder.environment(), even
         * though the JavaDoc indicates that it should. This is because it simply returns the SystemEnvironment object that
         * System.getenv() gives us. The relevant portion in the source code is marked as "// android changed", so
         * presumably it's not the case in the original version of the Apache Harmony project.
         *
         * @param environment List of all environment variables (in 'key=value' format)
         * @return new [Process] instance.
         * @throws IOException if the requested program could not be executed.
         */
        @WorkerThread
        @Throws(IOException::class)
        fun String.exec(environment: Map<String, String> = emptyMap()): Process =
            Runtime.getRuntime().exec(
                this, if (environment.isNotEmpty()) {
                    mutableListOf<String>().apply {
                        System.getenv().forEach { (key, value) -> add("$key=$value") }
                        environment.forEach { (key, value) -> add("$key=$value") }
                    }.toTypedArray()
                } else null
            )

    }

    class Interactive(val options: Builder) {

        private val handler: Handler? =
            if (Looper.myLooper() != null && options.handler == null && options.autoHandler) {
                Handler()
            } else {
                options.handler
            }

        private val idleSync = Any()
        private val callbackSync = Any()

        @Volatile
        var lastMarkerStdout: String? = null

        @Volatile
        var lastMarkerStderr: String? = null

        @Volatile
        var command: Command? = null

        @Volatile
        private var buffer: List<String>? = null

        @Volatile
        private var running = false

        @Volatile // read/write only synchronized
        private var idle = true

        @Volatile
        private var closed = true

        @Volatile
        private var callbacks = 0

        @Volatile
        private var watchdogCount = 0

        @Volatile
        var lastExitCode = 0

        lateinit var process: Process
        private var stdin: DataOutputStream? = null
        private var stdout: StreamGobbler? = null
        private var stderr: StreamGobbler? = null
        private val watchdog: ScheduledThreadPoolExecutor? = null

        init {
            options.run {
                if (onCommandResultListener != null) {
                    // Allow up to 60 seconds for Superuser dialog, then enable the user-specified
                    // timeout for all subsequent operations
                    watchdogTimeout = 60

                    val resultListener = object : OnCommandResultListener {
                        override fun onCommandResult(
                            commandCode: Int, exitCode: Int, output: List<String>?
                        ) {
                            val code = if (exitCode == ExitCode.SUCCESS
                                && !parseAvailableResult(output, isSU(shell))
                            ) ExitCode.Internal.SHELL_EXEC_FAILED else exitCode
                            watchdogTimeout = options.watchdogTimeout
                            onCommandResultListener?.onCommandResult(0, code, output)
                        }
                    }

                    commands.add(0, Command(AVAILABLE_TEST_COMMANDS, 0, resultListener))
                }
            }

            if (!open()) options.onCommandResultListener?.onCommandResult(0, SHELL_WRONG_UID, null)
        }

        /**
         * Internal call that launches the shell, starts gobbling, and starts executing commands. See
         * [Shell.Interactive]
         *
         * @return Opened successfully ?
         */
        @Synchronized
        private fun open(): Boolean {
            return try {
                // setup our process, retrieve stdin stream, and stdout/stderr gobblers
                process = options.shell.exec(options.environment)
                stdin = DataOutputStream(process.outputStream)
                stdout = StreamGobbler(process.inputStream, object : OnLineListener {
                    override fun onLine(line: String) {
                        synchronized(this@Interactive) {
                            if (command == null) {
                                return
                            }
                            var contentPart: String? = line
                            var markerPart: String? = null
                            val markerIndex = line.indexOf(command!!.marker)
                            if (markerIndex == 0) {
                                contentPart = null
                                markerPart = line
                            } else if (markerIndex > 0) {
                                contentPart = line.substring(0, markerIndex)
                                markerPart = line.substring(markerIndex)
                            }
                            if (contentPart != null) {
                                addBuffer(contentPart)
                                processLine(contentPart, onStdoutLineListener)
                                processLine(contentPart, command!!.onCommandLineListener)
                            }
                            if (markerPart != null) {
                                try {
                                    lastExitCode = Integer.valueOf(
                                        markerPart.substring(command!!.marker.length + 1),
                                        10
                                    )
                                } catch (e: Exception) {
                                    // this really shouldn't happen
                                    e.printStackTrace()
                                }
                                lastMarkerStdout = command!!.marker
                                processMarker()
                            }
                        }
                    }
                })
                stderr = StreamGobbler(process.getErrorStream(), object : OnLineListener() {
                    override fun onLine(line: String) {
                        synchronized(this@Interactive) {
                            if (command == null) {
                                return
                            }
                            var contentPart: String? = line
                            val markerIndex = line.indexOf(command!!.marker)
                            if (markerIndex == 0) {
                                contentPart = null
                            } else if (markerIndex > 0) {
                                contentPart = line.substring(0, markerIndex)
                            }
                            if (contentPart != null) {
                                if (options.wantStderr) addBuffer(contentPart)
                                processLine(contentPart, onStderrLineListener)
                            }
                            if (markerIndex >= 0) {
                                lastMarkerStderr = command!!.marker
                                processMarker()
                            }
                        }
                    }
                })

                // start gobbling and write our commands to the shell
                stdout?.start()
                stderr?.start()
                running = true
                closed = false
                runNextCommand()
                true
            } catch (e: IOException) {
                // shell probably not found
                false
            }
        }

        /**
         * Builder class for [Shell.Interactive]
         *
         * @param environment Environmental variables to add to the shell instance
         * @param commands Commands to execute
         * @param onStdoutLineListener The callback called for every line output to stdout by the shell
         * @param onStderrLineListener The callback called for every line output to stderr by the shell
         * @param handler Set a custom handler that will be used to post all callbacks to
         * @param autoHandler Automatically create a handler if possible ? Default to true
         * @param wantStderr Set if error output should be appended to command block result output
         * @param shell Set shell binary to use. Usually "sh" or "su", do not use a full path
         * @param watchdogTimeout Enable command timeout callback
         */
        class Builder(
            var environment: MutableMap<String, String> = mutableMapOf(),
            var commands: MutableList<Command> = mutableListOf(),
            var onStdoutLineListener: OnLineListener? = null,
            var onStderrLineListener: OnLineListener? = null,
            var handler: Handler? = null,
            var autoHandler: Boolean = true,
            var wantStderr: Boolean = false,
            var shell: String = "sh",
            var watchdogTimeout: Int = 0,
            var onCommandResultListener: OnCommandResultListener? = null
        ) {

            /**
             * Add or update an environment variable
             *
             * @param key Key of the environment variable
             * @param value Value of the environment variable
             * @return This Builder object for method chaining
             */
            fun addEnvironment(key: String, value: String) = apply {
                environment[key] = value
            }

            /**
             * Add or update environment variables
             *
             * @param env Map of environment variables
             * @return This Builder object for method chaining
             */
            fun addEnvironment(env: Map<String, String>) = apply {
                environment.putAll(env)
            }

            /**
             * Convenience function to set "sh" as used shell
             *
             * @return This Builder object for method chaining
             */
            fun useSH() = apply {
                shell = "sh"
            }

            /**
             * Convenience function to set "su" as used shell
             *
             * @return This Builder object for method chaining
             */
            fun useSU() = apply {
                shell = "su"
            }

            /**
             * Add a command to execute
             *
             * @param command Command to execute
             * @return This Builder object for method chaining
             */
            fun addCommand(command: String): Builder = apply {
                addCommand(command, 0, null)
            }

            /**
             *
             * Add a command to execute, with a callback to be called on completion
             *
             *
             * The thread on which the callback executes is dependent on various factors, see [Shell.Interactive]
             * for further details
             *
             * @param command Command to execute
             * @param code User-defined value passed back to the callback
             * @param listener Callback to be called on completion
             * @return This Builder object for method chaining
             */
            fun addCommand(
                command: String, code: Int, listener: OnCommandResultListener?
            ): Builder = apply {
                addCommand(arrayOf(command), code, listener)
            }

            /**
             * Add commands to execute
             *
             * @param commands Commands to execute
             * @return This Builder object for method chaining
             */
            fun addCommand(commands: List<String>): Builder = apply {
                addCommand(commands, 0, null)
            }

            /**
             *
             * Add commands to execute, with a callback to be called on completion (of all commands)
             *
             *
             * The thread on which the callback executes is dependent on various factors, see [Shell.Interactive]
             * for further details
             *
             * @param commands Commands to execute
             * @param code User-defined value passed back to the callback
             * @param listener Callback to be called on completion (of all commands)
             * @return This Builder object for method chaining
             */
            fun addCommand(
                commands: List<String>, code: Int, listener: OnCommandResultListener?
            ): Builder = apply {
                addCommand(commands.toTypedArray(), code, listener)
            }

            /**
             * Add commands to execute
             *
             * @param commands Commands to execute
             * @return This Builder object for method chaining
             */
            fun addCommand(commands: Array<String>): Builder = apply {
                addCommand(commands, 0, null)
            }

            /**
             *
             * Add commands to execute, with a callback to be called on completion (of all commands)
             *
             *
             * The thread on which the callback executes is dependent on various factors,
             * see [Shell.Interactive] for further details
             *
             * @param commands Commands to execute
             * @param code User-defined value passed back to the callback
             * @param listener Callback to be called on completion (of all commands)
             * @return This Builder object for method chaining
             */
            fun addCommand(
                commands: Array<String>, code: Int, listener: OnCommandResultListener?
            ): Builder = apply {
                this.commands.add(Command(commands, code, listener, null))
            }

            /**
             * Construct a [Shell.Interactive] instance, try to start the shell, and call listener
             * to report success or failure if provided
             *
             * @param listener Callback to return shell open status. Null by default
             */
            @WorkerThread
            fun open(listener: OnCommandResultListener? = null) {
                onCommandResultListener = listener
                Interactive(this)
            }

        }

    }

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
            output: List<String>?
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
    class Command(
        private val commands: Array<String>,
        private val code: Int,
        val onCommandResultListener: OnCommandResultListener? = null,
        val onCommandLineListener: OnCommandLineListener? = null
    ) {

        internal val marker: String

        init {
            marker = UUID.randomUUID().toString() + String.format("-%08x", ++commandCounter)
        }

        companion object {
            private var commandCounter = 0
        }
    }

}