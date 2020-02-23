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

package io.goatbytes.android.extension.widget

import android.content.Context
import android.graphics.drawable.Drawable
import android.text.Editable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextWatcher
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.URLSpan
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.IntRange
import androidx.annotation.StringRes
import androidx.core.text.HtmlCompat
import io.goatbytes.android.app

// region TextView

/**
 * @return the text that TextView is displaying as a string
 */
val TextView.value get() = text.toString()

/**
 * Convert a string to a Spannable that formats it into HTML and set the Spannable on a TextView
 *
 * @param resid The string resource that is formatted with supported HTML tags
 */
fun TextView.setHtml(@StringRes resid: Int, flag: Int = HtmlCompat.FROM_HTML_MODE_LEGACY) {
    text = HtmlCompat.fromHtml(app.getString(resid), flag)
}

/**
 * Convert a string to a Spannable that formats it into HTML and set the Spannable on a TextView
 *
 * @param html The string that is formatted with supported HTML tags
 */
fun TextView.setHtml(html: String, flag: Int = HtmlCompat.FROM_HTML_MODE_LEGACY) {
    text = HtmlCompat.fromHtml(html, flag)
}

fun TextView.setHtml(html: String, onClick: (url: String) -> Unit) =
    setHtml(HtmlCompat.fromHtml(html, HtmlCompat.FROM_HTML_MODE_LEGACY), onClick)

fun TextView.setHtml(html: Spanned, onClick: (url: String) -> Unit) {
    fun makeClickable(sb: SpannableStringBuilder, urlSpan: URLSpan) {
        val start = sb.getSpanStart(urlSpan)
        val end = sb.getSpanEnd(urlSpan)
        val flags = sb.getSpanFlags(urlSpan)
        val clickable = object : ClickableSpan() {
            override fun onClick(view: View) {
                onClick(urlSpan.url)
            }
        }
        sb.setSpan(clickable, start, end, flags)
        sb.removeSpan(urlSpan)
    }

    val sb = SpannableStringBuilder(html)
    val urls = sb.getSpans(0, html.length, URLSpan::class.java)
    for (urlSpan in urls) {
        makeClickable(sb, urlSpan)
    }
    text = sb
    movementMethod = LinkMovementMethod.getInstance()
}

/**
 * Convert a string to a Spannable that formats it into HTML and set the Spannable on a TextView
 */
var TextView.html: String
    get() = text.toString()
    set(value) = setHtml(value)


private fun TextView.setCompoundDrawablesWithIntrinsicBounds(
    drawable: Drawable?, @IntRange(from = 0, to = 3) index: Int
) {
    val drawables = compoundDrawables
    drawables[index] = drawable
    setCompoundDrawablesWithIntrinsicBounds(drawables[0], drawables[1], drawables[2], drawables[3])
}

var TextView.drawableLeft: Drawable?
    get() = compoundDrawables[0]
    set(value) = setCompoundDrawablesWithIntrinsicBounds(value, 0)

var TextView.drawableTop: Drawable?
    get() = compoundDrawables[1]
    set(value) = setCompoundDrawablesWithIntrinsicBounds(value, 1)

var TextView.drawableRight: Drawable?
    get() = compoundDrawables[2]
    set(value) = setCompoundDrawablesWithIntrinsicBounds(value, 2)

var TextView.drawableBottom: Drawable?
    get() = compoundDrawables[3]
    set(value) = setCompoundDrawablesWithIntrinsicBounds(value, 3)

/**
 * Add an action which will be invoked before the text changed.
 *
 * @return the [TextWatcher] added to the TextView
 */
inline fun TextView.doBeforeTextChanged(
    crossinline action: (
        text: CharSequence,
        start: Int,
        count: Int,
        after: Int
    ) -> Unit
) = addTextChangedListener(beforeTextChanged = action)

/**
 * Add an action which will be invoked when the text is changing.
 *
 * @return the [TextWatcher] added to the TextView
 */
inline fun TextView.doOnTextChanged(
    crossinline action: (
        text: CharSequence,
        start: Int,
        count: Int,
        after: Int
    ) -> Unit
) = addTextChangedListener(onTextChanged = action)

/**
 * Add an action which will be invoked after the text changed.
 *
 * @return the [TextWatcher] added to the TextView
 */
inline fun TextView.doAfterTextChanged(
    crossinline action: (text: Editable) -> Unit
) = addTextChangedListener(afterTextChanged = action)

/**
 * Add a text changed listener to this TextView using the provided actions
 *
 * @return the [TextWatcher] added to the TextView
 */
inline fun TextView.addTextChangedListener(
    crossinline beforeTextChanged: (
        text: CharSequence,
        start: Int,
        count: Int,
        after: Int
    ) -> Unit = { _, _, _, _ -> },
    crossinline onTextChanged: (
        text: CharSequence,
        start: Int,
        count: Int,
        after: Int
    ) -> Unit = { _, _, _, _ -> },
    crossinline afterTextChanged: (text: Editable) -> Unit = {}
): TextWatcher {
    val textWatcher = object : TextWatcher {
        override fun afterTextChanged(s: Editable) {
            afterTextChanged.invoke(s)
        }

        override fun beforeTextChanged(text: CharSequence, start: Int, count: Int, after: Int) {
            beforeTextChanged.invoke(text, start, count, after)
        }

        override fun onTextChanged(text: CharSequence, start: Int, before: Int, count: Int) {
            onTextChanged.invoke(text, start, before, count)
        }
    }
    addTextChangedListener(textWatcher)

    return textWatcher
}

// endregion

// region Toasts


private val String.duration: Int get() = if (length >= 50) Toast.LENGTH_LONG else Toast.LENGTH_SHORT

/**
 * Convenience function to show the string as a toast message.
 *
 * @param duration Either [Toast.LENGTH_SHORT] or [Toast.LENGTH_LONG].
 * Default value is [Toast.LENGTH_SHORT] if the message is less than 50 characters
 */
fun String.showToast(duration: Int = this.duration): Unit =
    Toast.makeText(app, this, duration).show()

/** Show a [Toast] message with the duration of [Toast.LENGTH_SHORT] */
fun String.showShortToast(): Unit =
    Toast.makeText(app, this, Toast.LENGTH_SHORT).show()

/** Show a [Toast] message with the duration of [Toast.LENGTH_LONG] */
fun String.showLongToast(): Unit =
    Toast.makeText(app, this, Toast.LENGTH_LONG).show()

/** Make a [Toast] message with the duration of [Toast.LENGTH_SHORT] */
fun String.makeShortToast(): Toast =
    Toast.makeText(app, this, Toast.LENGTH_SHORT)

/** Make a [Toast] message with the duration of [Toast.LENGTH_LONG] */
fun String.makeLongToast(): Toast =
    Toast.makeText(app, this, Toast.LENGTH_LONG)

/**
 * Convenience function to make a toast message.
 *
 * @param duration Either [Toast.LENGTH_SHORT] or [Toast.LENGTH_LONG].
 * Default value is [Toast.LENGTH_SHORT] if the message is less than 50 characters
 */
fun String.makeToast(duration: Int = this.duration): Toast =
    Toast.makeText(app, this, duration)

/**
 * Convenience function to show the string as a toast message.
 *
 * @param duration Either [Toast.LENGTH_SHORT] or [Toast.LENGTH_LONG].
 * Default value is [Toast.LENGTH_SHORT] if the message is less than 50 characters
 */
fun @receiver:StringRes Int.showToast(duration: Int = app.getString(this).duration): Unit =
    Toast.makeText(app, this, duration).show()

/** Show a [Toast] message with the duration of [Toast.LENGTH_LONG] */
fun @receiver:StringRes Int.showLongToast(): Unit =
    Toast.makeText(app, this, Toast.LENGTH_SHORT).show()

/** Show a [Toast] message with the duration of [Toast.LENGTH_SHORT] */
fun @receiver:StringRes Int.showShortToast(): Unit =
    Toast.makeText(app, this, Toast.LENGTH_LONG).show()

/**
 * Convenience function to show the string as a toast message.
 *
 * @param duration Either [Toast.LENGTH_SHORT] or [Toast.LENGTH_LONG].
 * Default value is [Toast.LENGTH_SHORT] if the message is less than 50 characters
 */
fun Context.showToast(
    text: String, duration: Int = if (text.length >= 50) Toast.LENGTH_LONG else Toast.LENGTH_SHORT
): Unit = Toast.makeText(this, text, duration).show()

// endregion
