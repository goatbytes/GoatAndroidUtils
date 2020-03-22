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

package io.goatbytes.android.globals

import android.app.Activity
import android.content.Context
import android.content.res.Resources
import io.goatbytes.android.app
import io.goatbytes.android.util.app.ActivityMonitor.currentActivity

// region Application

/** The current activity */
val activity: Activity? get() = currentActivity

/** The current activity if not null, otherwise the application context */
fun context(): Context = currentActivity ?: app

/** The current activity resources if the activity is not null, othwerwise the application resources */
fun resources(): Resources = context().resources

// endregion

// region Time

/** Number of milliseconds in a standard second. */
const val MILLIS_PER_SECOND = 1000L

/** Number of milliseconds in a standard minute. */
const val MILLIS_PER_MINUTE = 60 * MILLIS_PER_SECOND

/** Number of milliseconds in a standard hour. */
const val MILLIS_PER_HOUR = 60 * MILLIS_PER_MINUTE

/** Number of milliseconds in a standard day. */
const val MILLIS_PER_DAY = 24 * MILLIS_PER_HOUR

// endregion

// region String

/** A String for a space character. */
val String.Companion.SPACE: String get() = " "

/** The empty String "". */
val String.Companion.EMPTY: String get() = ""

/** A String for linefeed LF ("\n"). */
val String.Companion.LF: String get() = "\n"

/** A String for carriage return CR ("\r"). */
val String.Companion.CR: String get() = "\r"

// endregion

// region Char

/** `\u0000` null control character ('\0'), abbreviated NUL. */
val Char.Companion.NUL: Char get() = '\u0000'

// endregion
