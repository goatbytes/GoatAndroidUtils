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

package io.goatbytes.android.util.benchmark

import android.util.Log
import kotlin.math.roundToLong
import kotlin.math.sqrt

fun (() -> Unit).benchmark(
    name: String = Benchmark.defaultTag
): Benchmark.Result = Benchmark.begin(name).also { this() }.end(name)

/**
 * Fast & simple benchmarking framework
 *
 * Usage:
 *
 * ```
 * for (i in 0 until 100) {
 *   Benchmark.begin("benchmark-name")
 *   // Code you want to benchmark
 *   Benchmark.end("benchmark-name").log()
 * }
 * Benchmark.analyze("benchmark-name").log()
 * ```
 *
 * Compare & print results of all Benchmarks
 *
 * ```
 * Benchmark.compare(Benchmark.Stat.STANDARD_DEVIATION).log()
 * ```
 *
 * Compare & print results of specific Benchmarks
 *
 * ```
 * Benchmark.compare(Benchmark.Stat.RANGE, Benchmark.Order.ASCENDING, "benchmark-one", "benchmark-two").log();
 * ```
 *
 * Configuration
 *
 * Statistics to calculate.  By default all are enabled.
 *
 * ```
 * Benchmark.stats(Benchmark.Stat.AVERAGE, Benchmark.Stat.STANDARD_DEVIATION)
 * ```
 *
 * Sets the precision for all future benchmarks.  Default is Milliseconds
 *
 * ```
 * Benchmark.defaultPrecision(Benchmark.Precision.MICRO)
 * ```
 *
 * Based on the Java library [Benchit](https://github.com/T-Spoon/Benchit) by Oisín O'Neill.
 */
object Benchmark {

    private val starts = hashMapOf<String, Long>()
    private val benchmarks = hashMapOf<String, Result>()
    private val stats = mutableSetOf(Stat.AVERAGE, Stat.RANGE, Stat.STANDARD_DEVIATION)
    private var defaultPrecision = Precision.MILLI

    internal inline val defaultTag: String get() = Thread.currentThread().stackTrace[2].methodName

    var logger: BenchmarkLogger = Loggers.ANDROID

    fun stats(vararg stat: Stat): Benchmark = apply {
        stats.clear()
        stats.addAll(stat)
    }

    fun clear(): Benchmark = apply {
        benchmarks.clear()
        stats.clear()
    }

    fun begin(): Benchmark = begin(defaultTag)

    fun end(): Result = end(defaultTag)

    fun analyze(): Analysis = analyze(defaultTag)

    fun begin(tag: String): Benchmark = apply {
        starts[tag] = System.nanoTime()
    }

    fun defaultPrecision(precision: Precision): Benchmark = apply {
        defaultPrecision = precision
    }

    @Throws(InvalidTagError::class)
    fun end(tag: String): Result {
        val end = System.nanoTime()
        val start = starts[tag] ?: throw InvalidTagError(tag)
        val taken = end - start
        return benchmarks[tag]?.apply { add(taken) }
            ?: Result(tag, taken).apply { benchmarks[tag] = this }
    }

    @Throws(InvalidTagError::class)
    fun analyze(tag: String): Analysis {
        return Analysis(benchmarks[tag] ?: throw InvalidTagError(tag))
    }

    @Throws(InvalidTagError::class)
    fun compare(stat: Stat, order: Order = Order.ASCENDING, vararg tags: String): Comparison {
        val results = mutableListOf<Analysis>()
        val keys: Array<out String> = if (tags.isEmpty()) benchmarks.keys.toTypedArray() else tags
        for (key in keys) {
            results.add(analyze(key))
        }
        return Comparison(stat, order, results)
    }

    interface BenchmarkLogger {

        fun log(message: String)

        fun log(type: String, tag: String, result: String): Unit =
            log("$type [$tag] --> $result")

        fun log(tag: String, stats: List<Pair<String, String>>): Unit =
            log(buildString {
                append("[$tag] --> ")
                var separator = ""
                for ((name, value) in stats) {
                    append("$separator$name[$value]"); separator = ", "
                }
            })
    }

    object Loggers {

        private val Any.tag: String
            get() = (if (this is Class<*>) this else this::class.java).run {
                var klass: Class<*> = this
                while (klass.isAnonymousClass) {
                    klass = klass.enclosingClass ?: break
                }
                klass.simpleName
            }

        val ANDROID = object : BenchmarkLogger {
            override fun log(message: String) {
                Log.d(tag, message)
            }
        }

        val SYSTEM = object : BenchmarkLogger {
            override fun log(message: String) {
                println(message)
            }
        }
    }

    interface LoggableBenchmark {
        fun log(): LoggableBenchmark
    }

    class Result(
        val tag: String,
        time: Long,
        var precision: Precision = defaultPrecision
    ) : LoggableBenchmark {

        private val result: Long get() = (times[times.size - 1] / precision.divider).roundToLong()

        internal val times = mutableListOf<Long>()

        init {
            add(time)
        }

        internal fun add(time: Long) = times.add(time)

        override fun log(): Result = apply {
            logger.log("Result", tag, result.toString())
        }

    }

    class Analysis private constructor(
        private val tag: String,
        private val times: List<Long>,
        private val precision: Precision
    ) : LoggableBenchmark {

        constructor(benchmark: Result) : this(
            benchmark.tag,
            benchmark.times,
            benchmark.precision
        )

        private val average: Double = run {
            val size = times.size
            var total = 0L
            for (i in 0 until size) {
                total += times[i]
            }
            total / size.toDouble()
        }

        private val deviation: Double = run {
            val mean = average
            var temp = 0.0
            val size = times.size
            for (i in 0 until size) {
                val t = times[i]
                temp += (mean - t) * (mean - t)
            }
            sqrt(temp / size)
        }

        private val range: LongArray = run {
            val size = times.size
            var min = times[0]
            var max = times[0]
            for (i in 0 until size) {
                val time = times[i]
                if (time > max) {
                    max = time
                } else if (time < min) {
                    min = time
                }
            }
            longArrayOf(min, max)
        }

        private val min = range[0]

        private val max = range[1]

        override fun log(): Analysis = apply {
            val stats = mutableListOf<Pair<String, String>>()
            stats.add(Pair("Sample Size", "${times.size}"))
            val unit = precision.unit
            if (Benchmark.stats.contains(Stat.AVERAGE)) {
                stats.add(Pair("Average", "${time(average)}$unit"))
            }
            if (Benchmark.stats.contains(Stat.RANGE)) {
                stats.add(Pair("Range", "${time(min)}$unit --> ${time(max)}$unit"))
            }
            if (Benchmark.stats.contains(Stat.STANDARD_DEVIATION)) {
                stats.add(Pair("Deviation", "${time(deviation)}$unit"))
            }
            logger.log(tag, stats)
        }

        fun getStat(stat: Stat): Double = when (stat) {
            Stat.AVERAGE -> average
            Stat.RANGE -> (max - min).toDouble()
            Stat.STANDARD_DEVIATION -> deviation
        }

        private fun time(stat: Long): Long = (stat / precision.divider).roundToLong()

        private fun time(stat: Double): Long = (stat / precision.divider).roundToLong()

    }

    class Comparison(stat: Stat, order: Order, private val results: List<Analysis>) :
        LoggableBenchmark {

        private val headline =
            "Comparison Results: Number of Benchmarks[${results.size}], Ordered By[${stat.name} ${order.name}]"

        init {
            results.sortedWith(BenchmarkComparator(stat, order))
        }

        override fun log(): Comparison = apply {
            log("------")
            log(headline)
            log("------")
            results.forEach(::log)
            log("------")
        }

        private fun log(message: String) {
            logger.log(message)
        }

        private fun log(result: Analysis) {
            result.log()
        }

    }

    class BenchmarkComparator(private val stat: Stat, private val order: Order) :
        Comparator<Analysis> {

        override fun compare(left: Analysis, right: Analysis): Int {
            val statLeft = left.getStat(stat)
            val starRight = right.getStat(stat)
            return if (order == Order.ASCENDING) {
                statLeft.compareTo(starRight)
            } else {
                starRight.compareTo(statLeft)
            }
        }

    }

    enum class Stat {
        AVERAGE, RANGE, STANDARD_DEVIATION
    }

    enum class Order {
        ASCENDING, DESCENDING
    }

    enum class Precision(var unit: String, var divider: Double) {
        NANO("ns", 1.00),
        MICRO("µs", 1_000.00),
        MILLI("ms", 1_000_000.00),
        SECOND("s", 1_000_000_000.00);
    }

    class InvalidTagError(tag: String) :
        RuntimeException("Benchmark with tag [$tag] does not exist")

}