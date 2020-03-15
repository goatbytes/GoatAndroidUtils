/*
 * MIT License
 *
 * Copyright (C) 2020 goatbytes.io
 * Copyright (c) 2017 Kizito Nwose
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package io.goatbytes.android.util.time


import android.os.Handler
import java.io.Serializable
import java.util.*
import kotlin.concurrent.schedule
import kotlin.concurrent.scheduleAtFixedRate
import kotlin.math.roundToLong

/*
 * Type-safe time calculations in Kotlin, powered by generics.
 *
 * https://github.com/kizitonwose/Time
 */

interface TimeUnit {
    val timeIntervalRatio: Double
    fun <OtherUnit : TimeUnit> conversionRate(otherTimeUnit: OtherUnit): Double {
        return timeIntervalRatio / otherTimeUnit.timeIntervalRatio
    }
}


class Interval<out T : TimeUnit>(value: Number, factory: () -> T) : Serializable {

    companion object {
        inline operator fun <reified K : TimeUnit> invoke(value: Number) = Interval(value) {
            K::class.java.newInstance()
        }
    }

    val unit: T = factory()

    val value = value.toDouble()

    val longValue = this.value.roundToLong()

    val inYears: Interval<Year>
        get() = converted()

    val inWeeks: Interval<Week>
        get() = converted()

    val inDays: Interval<Day>
        get() = converted()

    val inHours: Interval<Hour>
        get() = converted()

    val inMinutes: Interval<Minute>
        get() = converted()

    val inSeconds: Interval<Second>
        get() = converted()

    val inMilliseconds: Interval<Millisecond>
        get() = converted()

    val inMicroseconds: Interval<Microsecond>
        get() = converted()

    val inNanoseconds: Interval<Nanosecond>
        get() = converted()


    inline fun <reified OtherUnit : TimeUnit> converted(): Interval<OtherUnit> {
        val otherInstance = OtherUnit::class.java.newInstance()
        return Interval(value * unit.conversionRate(otherInstance))
    }

    operator fun plus(other: Interval<TimeUnit>): Interval<T> {
        val newValue = value + other.value * other.unit.conversionRate(unit)
        return Interval(newValue) { unit }
    }

    operator fun minus(other: Interval<TimeUnit>): Interval<T> {
        val newValue = value - other.value * other.unit.conversionRate(unit)
        return Interval(newValue) { unit }
    }

    operator fun times(other: Number): Interval<T> {
        return Interval(value * other.toDouble()) { unit }
    }

    operator fun div(other: Number): Interval<T> {
        return Interval(value / other.toDouble()) { unit }
    }

    operator fun inc() = Interval(value + 1) { unit }

    operator fun dec() = Interval(value - 1) { unit }

    operator fun compareTo(other: Interval<TimeUnit>) =
        inMilliseconds.value.compareTo(other.inMilliseconds.value)

    operator fun contains(other: Interval<TimeUnit>) =
        inMilliseconds.value >= other.inMilliseconds.value

    override fun equals(other: Any?): Boolean {
        if (other == null || other !is Interval<TimeUnit>) return false
        return compareTo(other) == 0
    }

    override fun hashCode() = inMilliseconds.value.hashCode()

    override fun toString(): String {
        val unitString = unit::class.java.simpleName.toLowerCase()
        val isWhole = value % 1 == 0.0
        return (if (isWhole) longValue.toString() else value.toString())
            .plus(" ")
            .plus(if (value == 1.0) unitString else unitString.plus("s"))
    }
}

class Year : TimeUnit {
    override val timeIntervalRatio = 31_556_952.0
}

class Week : TimeUnit {
    override val timeIntervalRatio = 604_800.0
}

class Day : TimeUnit {
    override val timeIntervalRatio = 86_400.0
}

class Hour : TimeUnit {
    override val timeIntervalRatio = 3_600.0
}

class Minute : TimeUnit {
    override val timeIntervalRatio = 60.0
}

class Second : TimeUnit {
    override val timeIntervalRatio = 1.0
}

class Millisecond : TimeUnit {
    override val timeIntervalRatio = 0.001
}

class Microsecond : TimeUnit {
    override val timeIntervalRatio = 0.000001
}

class Nanosecond : TimeUnit {
    override val timeIntervalRatio = 1e-9
}

inline val <reified T : TimeUnit> Interval<T>.milliseconds get() = inMilliseconds.longValue

val Number.years: Interval<Year>
    get() = Interval(this)

val Number.weeks: Interval<Week>
    get() = Interval(this)

val Number.days: Interval<Day>
    get() = Interval(this)

val Number.hours: Interval<Hour>
    get() = Interval(this)

val Number.minutes: Interval<Minute>
    get() = Interval(this)

val Number.seconds: Interval<Second>
    get() = Interval(this)

val Number.milliseconds: Interval<Millisecond>
    get() = Interval(this)

val Number.microseconds: Interval<Microsecond>
    get() = Interval(this)

val Number.nanoseconds: Interval<Nanosecond>
    get() = Interval(this)

//region Calendar
operator fun Calendar.plus(other: Interval<TimeUnit>): Calendar = (clone() as Calendar).apply {
    timeInMillis += other.inMilliseconds.longValue
}

operator fun Calendar.minus(other: Interval<TimeUnit>): Calendar = (clone() as Calendar).apply {
    timeInMillis -= other.inMilliseconds.longValue
}
//endregion

//region Thread
fun Thread.sleep(interval: Interval<TimeUnit>) = Thread.sleep(interval.inMilliseconds.longValue)
//endregion


//region Java Timer
fun Timer.schedule(task: TimerTask, period: Interval<TimeUnit>) =
    schedule(task, period.inMilliseconds.longValue)

fun Timer.schedule(task: TimerTask, delay: Interval<TimeUnit>, period: Interval<TimeUnit>) =
    schedule(task, delay.inMilliseconds.longValue, period.inMilliseconds.longValue)

fun Timer.schedule(task: TimerTask, firstTime: Date, period: Interval<TimeUnit>) =
    schedule(task, firstTime, period.inMilliseconds.longValue)

fun Timer.scheduleAtFixedRate(
    task: TimerTask,
    delay: Interval<TimeUnit>,
    period: Interval<TimeUnit>
) = scheduleAtFixedRate(task, delay.inMilliseconds.longValue, period.inMilliseconds.longValue)

fun Timer.scheduleAtFixedRate(task: TimerTask, firstTime: Date, period: Interval<TimeUnit>) =
    scheduleAtFixedRate(task, firstTime, period.inMilliseconds.longValue)
//endregion


//region Kotlin Timer
inline fun Timer.schedule(
    delay: Interval<TimeUnit>,
    crossinline action: TimerTask.() -> Unit
): TimerTask = schedule(delay.inMilliseconds.longValue, action)

inline fun Timer.schedule(
    delay: Interval<TimeUnit>,
    period: Interval<TimeUnit>,
    crossinline action: TimerTask.() -> Unit
): TimerTask = schedule(delay.inMilliseconds.longValue, period.inMilliseconds.longValue, action)

inline fun Timer.schedule(
    time: Date,
    period: Interval<TimeUnit>,
    crossinline action: TimerTask.() -> Unit
): TimerTask = schedule(time, period.inMilliseconds.longValue, action)

inline fun Timer.scheduleAtFixedRate(
    delay: Interval<TimeUnit>,
    period: Interval<TimeUnit>,
    crossinline action: TimerTask.() -> Unit
): TimerTask =
    scheduleAtFixedRate(delay.inMilliseconds.longValue, period.inMilliseconds.longValue, action)

inline fun Timer.scheduleAtFixedRate(
    time: Date,
    period: Interval<TimeUnit>,
    crossinline action: TimerTask.() -> Unit
): TimerTask = scheduleAtFixedRate(time, period.inMilliseconds.longValue, action)
//endregion

//region Handler
fun Handler.postDelayed(r: Runnable, delay: Interval<TimeUnit>) =
    postDelayed(r, delay.inMilliseconds.longValue)

fun Handler.postDelayed(r: () -> Unit, delay: Interval<TimeUnit>) =
    postDelayed(r, delay.inMilliseconds.longValue)
//endregion