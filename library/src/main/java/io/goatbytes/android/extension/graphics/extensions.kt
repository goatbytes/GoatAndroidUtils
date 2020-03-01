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

@file:Suppress("NOTHING_TO_INLINE", "WRONG_ANNOTATION_TARGET_WITH_USE_SITE_TARGET_ON_TYPE")

package io.goatbytes.android.extension.graphics

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import androidx.annotation.ColorInt
import androidx.annotation.FloatRange
import androidx.annotation.Px
import androidx.core.graphics.ColorUtils
import io.goatbytes.android.globals.context
import java.io.File
import java.io.FileOutputStream
import java.util.*
import java.util.regex.Pattern
import kotlin.math.roundToInt

// region Bitmap

/** Create a [BitmapDrawable] from this [Bitmap]. */
inline fun Bitmap.toDrawable(resources: Resources = context().resources) =
    BitmapDrawable(resources, this)

/**
 * Extension method to save Bitmap to specified file.
 */
inline fun Bitmap.saveTo(
    file: File,
    format: Bitmap.CompressFormat = Bitmap.CompressFormat.PNG,
    quality: Int = 100
) {
    if (!file.exists()) {
        file.createNewFile()
    }
    val stream = FileOutputStream(file)
    compress(format, quality, stream)
    stream.flush()
    stream.close()
}

/** Create a [ColorDrawable] from this color value. */
inline fun @receiver:ColorInt Int.toDrawable() = ColorDrawable(this)

/** Return a [Bitmap] representation of this [Drawable].*/
inline val Drawable?.bitmap: Bitmap? get() = this?.toBitmap()

/**
 * Return a [Bitmap] representation of this [Drawable].
 *
 * If this instance is a [BitmapDrawable] and the [width], [height], and [config] match, the
 * underlying [Bitmap] instance will be returned directly. If any of those three properties differ
 * then a new [Bitmap] is created. For all other [Drawable] types, a new [Bitmap] is created.
 *
 * @param width Width of the desired bitmap. Defaults to [Drawable.getIntrinsicWidth].
 * @param height Height of the desired bitmap. Defaults to [Drawable.getIntrinsicHeight].
 * @param config Bitmap config of the desired bitmap. Null attempts to use the native config, if
 * any. Defaults to [Config.ARGB_8888] otherwise.
 */
fun Drawable.toBitmap(
    @Px width: Int = intrinsicWidth,
    @Px height: Int = intrinsicHeight,
    config: Bitmap.Config? = null
): Bitmap {
    if (this is BitmapDrawable) {
        if (config == null || bitmap.config == config) {
            // Fast-path to return original. Bitmap.createScaledBitmap will do this check, but it
            // involves allocation and two jumps into native code so we perform the check ourselves.
            if (width == intrinsicWidth && height == intrinsicHeight) {
                return bitmap
            }
            return Bitmap.createScaledBitmap(bitmap, width, height, true)
        }
    }

    val oldLeft = bounds.left
    val oldTop = bounds.top
    val oldRight = bounds.right
    val oldBottom = bounds.bottom

    val bitmap = Bitmap.createBitmap(width, height, config ?: Bitmap.Config.ARGB_8888)
    setBounds(0, 0, width, height)
    draw(Canvas(bitmap))

    setBounds(oldLeft, oldTop, oldRight, oldBottom)
    return bitmap
}

fun Drawable.rotate(angle: Float): Drawable {
    val px = bounds.width() / 2f
    val py = bounds.height() / 2f
    return object : LayerDrawable(arrayOf(this)) {
        override fun draw(canvas: Canvas) {
            canvas.save()
            canvas.rotate(angle, px, py)
            super.draw(canvas)
            canvas.restore()
        }
    }
}

/**
 * Returns the value of the pixel at the specified location. The returned value
 * is a [color int][android.graphics.Color] in the sRGB color space.
 */
inline operator fun Bitmap.get(x: Int, y: Int): Int = getPixel(x, y)


/**
 * Writes the specified [color int][android.graphics.Color] into the bitmap
 * (assuming it is mutable) at the specified `(x, y)` coordinate. The specified
 * color is converted from sRGB to the bitmap's color space if needed.
 */
inline operator fun Bitmap.set(x: Int, y: Int, @ColorInt color: Int): Unit = setPixel(x, y, color)

// endregion

// region Color

/**
 * Darkens the color by a given factor.
 *
 * @param factor The factor to darken the color.
 * @return A darker version of the color integer.
 */
@ColorInt
fun @receiver:ColorInt Int.darker(@FloatRange(from = 0.0, to = 1.0) factor: Float): Int =
    arrayOf(red, green, blue).map { (it * factor).toInt().coerceAtLeast(0) }.let { (r, g, b) ->
        Color.argb(alpha, r, g, b)
    }

/**
 * Lightens the color by a given factor.
 *
 * @param factor The factor to lighten the color.
 * @return A lighter version of the color integer.
 */
@ColorInt
fun @receiver:ColorInt Int.lighter(@FloatRange(from = 0.0, to = 1.0) factor: Float): Int =
    arrayOf(red, green, blue).map {
        ((it * (1 - factor) / 255 + factor) * 255).toInt().coerceAtLeast(0)
    }.let { (r, g, b) -> Color.argb(alpha, r, g, b) }

/**
 * Manipulate the alpha bytes of the color
 *
 * @param factor 0.0f - 1.0f
 * @return The new color value
 */
@ColorInt
fun @receiver:ColorInt Int.adjustAlpha(@FloatRange(from = 0.0, to = 1.0) factor: Float): Int =
    Color.argb((this.alpha * factor).roundToInt(), red, green, blue)

/**
 * Remove alpha from the color
 *
 * @return The color without any transparency
 */
@ColorInt
fun @receiver:ColorInt Int.stripAlpha(): Int = Color.rgb(red, green, blue)

/**
 * Returns the luminance of a color as a float between 0.0 and 1.0.
 */
inline fun @receiver:ColorInt Int.calculateLuminance(): Double = ColorUtils.calculateLuminance(this)

/**
 * Returns `true` if the luminance of the color is less than or equal to the luminance factor
 *
 * @param color The color to calculate the luminance.
 * @param luminance Value from 0-1. 1 = white. 0 = black. Default is 0.5.
 * @return `true` if the color is light
 */
fun @receiver:ColorInt Int.isLightColor(
    @FloatRange(from = 0.0, to = 1.0) luminance: Double = 0.5
): Boolean = calculateLuminance() <= luminance

/**
 * Returns `true` if the luminance of the color is less than or equal to the luminance factor
 *
 * @param luminance Value from 0-1. 1 = white. 0 = black. Default is 0.5.
 * @return `true` if the color is dark
 */
fun @receiver:ColorInt Int.isDarkColor(
    @FloatRange(
        from = 0.0,
        to = 1.0
    ) luminance: Double = 0.5
): Boolean = calculateLuminance() > luminance

/**
 * Return the alpha component of a color int. This is equivalent to calling:
 * ```
 * Color.alpha(myInt)
 * ```
 */
inline val @receiver:ColorInt Int.alpha get() = (this shr 24) and 0xff

/**
 * Return the red component of a color int. This is equivalent to calling:
 * ```
 * Color.red(myInt)
 * ```
 */
inline val @receiver:ColorInt Int.red get() = (this shr 16) and 0xff

/**
 * Return the green component of a color int. This is equivalent to calling:
 * ```
 * Color.green(myInt)
 * ```
 */
inline val @receiver:ColorInt Int.green get() = (this shr 8) and 0xff

/**
 * Return the blue component of a color int. This is equivalent to calling:
 * ```
 * Color.blue(myInt)
 * ```
 */
inline val @receiver:ColorInt Int.blue get() = this and 0xff

/**
 * Return the alpha component of a color int. This is equivalent to calling:
 * ```
 * Color.alpha(myInt)
 * ```
 *
 * This method allows to use destructuring declarations when working with colors,
 * for example:
 * ```
 * val (alpha, red, green, blue) = myColor
 * ```
 */
inline operator fun @receiver:ColorInt Int.component1() = (this shr 24) and 0xff

/**
 * Return the red component of a color int. This is equivalent to calling:
 * ```
 * Color.red(myInt)
 * ```
 *
 * This method allows to use destructuring declarations when working with colors,
 * for example:
 * ```
 * val (alpha, red, green, blue) = myColor
 * ```
 */
inline operator fun @receiver:ColorInt Int.component2() = (this shr 16) and 0xff

/**
 * Return the green component of a color int. This is equivalent to calling:
 * ```
 * Color.green(myInt)
 * ```
 *
 * This method allows to use destructuring declarations when working with colors,
 * for example:
 * ```
 * val (alpha, red, green, blue) = myColor
 * ```
 */
inline operator fun @receiver:ColorInt Int.component3() = (this shr 8) and 0xff

/**
 * Return the blue component of a color int. This is equivalent to calling:
 * ```
 * Color.blue(myInt)
 * ```
 *
 * This method allows to use destructuring declarations when working with colors,
 * for example:
 * ```
 * val (alpha, red, green, blue) = myColor
 * ```
 */
inline operator fun @receiver:ColorInt Int.component4() = this and 0xff

/**
 * Convert a color integer to to a hex string
 *
 * @param alpha Set to true to preserve the alpha.
 * @return The color as #RRGGBB if alpha was false or #AARRGGBB if alpha was true
 */
inline fun @receiver:ColorInt Int.toHexColor(alpha: Boolean = false): String = "#" + if (alpha) {
    String.format("%08X", (this))
} else {
    String.format("%06X", 0xFFFFFF and this)
}

/**
 * Return a corresponding [Int] color of this [String].
 *
 * @throws IllegalArgumentException if this [String] cannot be parsed to a color integer.
 */
@ColorInt
inline fun String.toColorInt(): Int = argb().let { (a, r, g, b) -> Color.argb(a, r, g, b) }

/**
 * Return a corresponding [Int] color of this [String].
 *
 * @throws IllegalArgumentException if this [String] cannot be parsed to a color integer.
 */
@get:ColorInt
inline val String.color: Int
    get() = argb().let { (a, r, g, b) -> Color.argb(a, r, g, b) }

/**
 * Convert an HTML color string like '#0099CC' into an array
 *
 * @return An array of color values for alpha, red, green, blue components in the range 0..255.
 * @throws IllegalStateException if the string cannot be parsed to a color integer
 */
fun String.argb(): IntArray = (if (this[0] == '#') {
    substring(1)
} else when (toLowerCase(Locale.ROOT)) {
    // system colors
    "darkgray", "darkgrey" -> "FF444444"
    "gray", "grey" -> "FF888888"
    "lightgray", "lightgrey" -> "FFCCCCCC"
    "black" -> "FF000000"
    "white" -> "FFFFFFFF"
    "red" -> "FFFF0000"
    "green" -> "FF00FF00"
    "blue" -> "FF0000FF"
    "yellow" -> "FFFFFF00"
    "cyan" -> "FF00FFFF"
    "magenta" -> "FFFF00FF"
    "aqua" -> "FF00FFFF"
    "fuchsia" -> "FFFF00FF"
    "lime" -> "FF00FF00"
    "maroon" -> "FF800000"
    "navy" -> "FF000080"
    "olive" -> "FF808000"
    "purple" -> "FF800080"
    "silver" -> "FFC0C0C0"
    "teal" -> "FF008080"
    "transparent" -> "00000000"
    // Check if this is a hex color code without the leading '#' and return the string or throw
    else -> material(this) ?: Pattern.compile("^[0-9a-fA-F]{0,8}\$").matcher(this)
        .takeIf { it.matches() }?.group() ?: throw IllegalStateException("Unknown color")
}).let { (a, r, g, b) -> intArrayOf(a, r, g, b) }

private inline operator fun String.component1(): Int = when (length) {
    in 0..6 -> 255
    7 -> Integer.parseInt(substring(0, 1), 16)
    8 -> Integer.parseInt(substring(0, 2), 16)
    else -> 0
}

private inline operator fun String.component2(): Int = when (length) {
    3, 5 -> Integer.parseInt(substring(0, 1), 16)
    6 -> Integer.parseInt(substring(0, 2), 16)
    7 -> Integer.parseInt(substring(1, 3), 16)
    8 -> Integer.parseInt(substring(2, 4), 16)
    else -> 0
}

private inline operator fun String.component3(): Int = when (length) {
    1, 2 -> Integer.parseInt(this, 16)
    3 -> Integer.parseInt(substring(1, 2), 16)
    4 -> Integer.parseInt(substring(0, 2), 16)
    5 -> Integer.parseInt(substring(1, 3), 16)
    6 -> Integer.parseInt(substring(2, 4), 16)
    7 -> Integer.parseInt(substring(3, 5), 16)
    8 -> Integer.parseInt(substring(4, 6), 16)
    else -> 0
}

private inline operator fun String.component4(): Int = when (length) {
    3 -> Integer.parseInt(substring(2, 3), 16)
    4 -> Integer.parseInt(substring(2, 4), 16)
    5 -> Integer.parseInt(substring(3, 5), 16)
    6 -> Integer.parseInt(substring(4, 6), 16)
    7 -> Integer.parseInt(substring(5, 7), 16)
    8 -> Integer.parseInt(substring(6, 8), 16)
    else -> 0
}


private fun material(color: String): String? = when (color.toLowerCase(Locale.ROOT)) {
    // AMBER
    "amber_50" -> "FFF8E1"
    "amber_100" -> "FFECB3"
    "amber_200" -> "FFE082"
    "amber_300" -> "FFD54F"
    "amber_400" -> "FFCA28"
    "amber_500" -> "FFC107"
    "amber_600" -> "FFB300"
    "amber_700" -> "FFA000"
    "amber_800" -> "FF8F00"
    "amber_900" -> "FF6F00"
    "amber_A100" -> "FFE57F"
    "amber_A200" -> "FFD740"
    "amber_A400" -> "FFC400"
    "amber_A700" -> "FFAB00"
    // BLUE
    "blue_50" -> "E3F2FD"
    "blue_100" -> "BBDEFB"
    "blue_200" -> "90CAF9"
    "blue_300" -> "64B5F6"
    "blue_400" -> "42A5F5"
    "blue_500" -> "2196F3"
    "blue_600" -> "1E88E5"
    "blue_700" -> "1976D2"
    "blue_800" -> "1565C0"
    "blue_900" -> "0D47A1"
    "blue_A100" -> "82B1FF"
    "blue_A200" -> "448AFF"
    "blue_A400" -> "2979FF"
    "blue_A700" -> "2962FF"
    // BLUE GREY
    "blue_grey_50" -> "ECEFF1"
    "blue_grey_100" -> "CFD8DC"
    "blue_grey_200" -> "B0BEC5"
    "blue_grey_300" -> "90A4AE"
    "blue_grey_400" -> "78909C"
    "blue_grey_500" -> "607D8B"
    "blue_grey_600" -> "546E7A"
    "blue_grey_700" -> "455A64"
    "blue_grey_800" -> "37474F"
    "blue_grey_900" -> "263238"
    // BROWN
    "brown_50" -> "EFEBE9"
    "brown_100" -> "D7CCC8"
    "brown_200" -> "BCAAA4"
    "brown_300" -> "A1887F"
    "brown_400" -> "8D6E63"
    "brown_500" -> "795548"
    "brown_600" -> "6D4C41"
    "brown_700" -> "5D4037"
    "brown_800" -> "4E342E"
    "brown_900" -> "3E2723"
    // CYAN
    "cyan_50" -> "E0F7FA"
    "cyan_100" -> "B2EBF2"
    "cyan_200" -> "80DEEA"
    "cyan_300" -> "4DD0E1"
    "cyan_400" -> "26C6DA"
    "cyan_500" -> "00BCD4"
    "cyan_600" -> "00ACC1"
    "cyan_700" -> "0097A7"
    "cyan_800" -> "00838F"
    "cyan_900" -> "006064"
    "cyan_A100" -> "84FFFF"
    "cyan_A200" -> "18FFFF"
    "cyan_A400" -> "00E5FF"
    "cyan_A700" -> "00B8D4"
    // DEEP ORANGE
    "deep_orange_50" -> "FBE9E7"
    "deep_orange_100" -> "FFCCBC"
    "deep_orange_200" -> "FFAB91"
    "deep_orange_300" -> "FF8A65"
    "deep_orange_400" -> "FF7043"
    "deep_orange_500" -> "FF5722"
    "deep_orange_600" -> "F4511E"
    "deep_orange_700" -> "E64A19"
    "deep_orange_800" -> "D84315"
    "deep_orange_900" -> "BF360C"
    "deep_orange_A100" -> "FF9E80"
    "deep_orange_A200" -> "FF6E40"
    "deep_orange_A400" -> "FF3D00"
    "deep_orange_A700" -> "DD2C00"
    // DEEP PURPLE
    "deep_purple_50" -> "EDE7F6"
    "deep_purple_100" -> "D1C4E9"
    "deep_purple_200" -> "B39DDB"
    "deep_purple_300" -> "9575CD"
    "deep_purple_400" -> "7E57C2"
    "deep_purple_500" -> "673AB7"
    "deep_purple_600" -> "5E35B1"
    "deep_purple_700" -> "512DA8"
    "deep_purple_800" -> "4527A0"
    "deep_purple_900" -> "311B92"
    "deep_purple_A100" -> "B388FF"
    "deep_purple_A200" -> "7C4DFF"
    "deep_purple_A400" -> "651FFF"
    "deep_purple_A700" -> "6200EA"
    // GREEN
    "green_50" -> "E8F5E9"
    "green_100" -> "C8E6C9"
    "green_200" -> "A5D6A7"
    "green_300" -> "81C784"
    "green_400" -> "66BB6A"
    "green_500" -> "4CAF50"
    "green_600" -> "43A047"
    "green_700" -> "388E3C"
    "green_800" -> "2E7D32"
    "green_900" -> "1B5E20"
    "green_A100" -> "B9F6CA"
    "green_A200" -> "69F0AE"
    "green_A400" -> "00E676"
    "green_A700" -> "00C853"
    // GREY
    "grey_100" -> "F5F5F5"
    "grey_200" -> "EEEEEE"
    "grey_300" -> "E0E0E0"
    "grey_400" -> "BDBDBD"
    "grey_50" -> "FAFAFA"
    "grey_500" -> "9E9E9E"
    "grey_600" -> "757575"
    "grey_700" -> "616161"
    "grey_800" -> "424242"
    "grey_900" -> "212121"
    // INDIGO
    "indigo_50" -> "E8EAF6"
    "indigo_100" -> "C5CAE9"
    "indigo_200" -> "9FA8DA"
    "indigo_300" -> "7986CB"
    "indigo_400" -> "5C6BC0"
    "indigo_500" -> "3F51B5"
    "indigo_600" -> "3949AB"
    "indigo_700" -> "303F9F"
    "indigo_800" -> "283593"
    "indigo_900" -> "1A237E"
    "indigo_A100" -> "8C9EFF"
    "indigo_A200" -> "536DFE"
    "indigo_A400" -> "3D5AFE"
    "indigo_A700" -> "304FFE"
    // LIGHT BLUE
    "light_blue_50" -> "E1F5FE"
    "light_blue_100" -> "B3E5FC"
    "light_blue_200" -> "81D4FA"
    "light_blue_300" -> "4FC3F7"
    "light_blue_400" -> "29B6F6"
    "light_blue_500" -> "03A9F4"
    "light_blue_600" -> "039BE5"
    "light_blue_700" -> "0288D1"
    "light_blue_800" -> "0277BD"
    "light_blue_900" -> "01579B"
    "light_blue_A100" -> "80D8FF"
    "light_blue_A200" -> "40C4FF"
    "light_blue_A400" -> "00B0FF"
    "light_blue_A700" -> "0091EA"
    // LIGHT GREEN
    "light_green_50" -> "F1F8E9"
    "light_green_100" -> "DCEDC8"
    "light_green_200" -> "C5E1A5"
    "light_green_300" -> "AED581"
    "light_green_400" -> "9CCC65"
    "light_green_500" -> "8BC34A"
    "light_green_600" -> "7CB342"
    "light_green_700" -> "689F38"
    "light_green_800" -> "558B2F"
    "light_green_900" -> "33691E"
    "light_green_A100" -> "CCFF90"
    "light_green_A200" -> "B2FF59"
    "light_green_A400" -> "76FF03"
    "light_green_A700" -> "64DD17"
    // LIME
    "lime_50" -> "F9FBE7"
    "lime_100" -> "F0F4C3"
    "lime_200" -> "E6EE9C"
    "lime_300" -> "DCE775"
    "lime_400" -> "D4E157"
    "lime_500" -> "CDDC39"
    "lime_600" -> "C0CA33"
    "lime_700" -> "AFB42B"
    "lime_800" -> "9E9D24"
    "lime_900" -> "827717"
    "lime_A100" -> "F4FF81"
    "lime_A200" -> "EEFF41"
    "lime_A400" -> "C6FF00"
    "lime_A700" -> "AEEA00"
    // ORANGE
    "orange_50" -> "FFF3E0"
    "orange_100" -> "FFE0B2"
    "orange_200" -> "FFCC80"
    "orange_300" -> "FFB74D"
    "orange_400" -> "FFA726"
    "orange_500" -> "FF9800"
    "orange_600" -> "FB8C00"
    "orange_700" -> "F57C00"
    "orange_800" -> "EF6C00"
    "orange_900" -> "E65100"
    "orange_A100" -> "FFD180"
    "orange_A200" -> "FFAB40"
    "orange_A400" -> "FF9100"
    "orange_A700" -> "FF6D00"
    // PINK
    "pink_50" -> "FCE4EC"
    "pink_100" -> "F8BBD0"
    "pink_200" -> "F48FB1"
    "pink_300" -> "F06292"
    "pink_400" -> "EC407A"
    "pink_500" -> "E91E63"
    "pink_600" -> "D81B60"
    "pink_700" -> "C2185B"
    "pink_800" -> "AD1457"
    "pink_900" -> "880E4F"
    "pink_A100" -> "FF80AB"
    "pink_A200" -> "FF4081"
    "pink_A400" -> "F50057"
    "pink_A700" -> "C51162"
    // PURPLE
    "purple_50" -> "F3E5F5"
    "purple_100" -> "E1BEE7"
    "purple_200" -> "CE93D8"
    "purple_300" -> "BA68C8"
    "purple_400" -> "AB47BC"
    "purple_500" -> "9C27B0"
    "purple_600" -> "8E24AA"
    "purple_700" -> "7B1FA2"
    "purple_800" -> "6A1B9A"
    "purple_900" -> "4A148C"
    "purple_A100" -> "EA80FC"
    "purple_A200" -> "E040FB"
    "purple_A400" -> "D500F9"
    "purple_A700" -> "AA00FF"
    // RED
    "red_50" -> "FFEBEE"
    "red_100" -> "FFCDD2"
    "red_200" -> "EF9A9A"
    "red_300" -> "E57373"
    "red_400" -> "EF5350"
    "red_500" -> "F44336"
    "red_600" -> "E53935"
    "red_700" -> "D32F2F"
    "red_800" -> "C62828"
    "red_900" -> "B71C1C"
    "red_A100" -> "FF8A80"
    "red_A200" -> "FF5252"
    "red_A400" -> "FF1744"
    "red_A700" -> "D50000"
    // TEAL
    "teal_50" -> "E0F2F1"
    "teal_100" -> "B2DFDB"
    "teal_200" -> "80CBC4"
    "teal_300" -> "4DB6AC"
    "teal_400" -> "26A69A"
    "teal_500" -> "009688"
    "teal_600" -> "00897B"
    "teal_700" -> "00796B"
    "teal_800" -> "00695C"
    "teal_900" -> "004D40"
    "teal_A100" -> "A7FFEB"
    "teal_A200" -> "64FFDA"
    "teal_A400" -> "1DE9B6"
    "teal_A700" -> "00BFA5"
    // YELLOW
    "yellow_50" -> "FFFDE7"
    "yellow_100" -> "FFF9C4"
    "yellow_200" -> "FFF59D"
    "yellow_300" -> "FFF176"
    "yellow_400" -> "FFEE58"
    "yellow_500" -> "FFEB3B"
    "yellow_600" -> "FDD835"
    "yellow_700" -> "FBC02D"
    "yellow_800" -> "F9A825"
    "yellow_900" -> "F57F17"
    "yellow_A100" -> "FFFF8D"
    "yellow_A200" -> "FFFF00"
    "yellow_A400" -> "FFEA00"
    "yellow_A700" -> "FFD600"
    // UNKNOWN
    else -> null
}

// endregion
