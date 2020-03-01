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

@file:Suppress("NOTHING_TO_INLINE")

package io.goatbytes.android.extension.content

import android.app.Activity
import android.app.AlarmManager
import android.app.Application
import android.app.NotificationManager
import android.content.ActivityNotFoundException
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.AssetManager
import android.content.res.ColorStateList
import android.content.res.Resources
import android.content.res.TypedArray
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.location.LocationManager
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.os.Vibrator
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.annotation.*
import androidx.core.content.ContextCompat
import io.goatbytes.android.globals.context
import java.io.Serializable
import java.nio.charset.Charset
import kotlin.math.roundToInt

// region AssetManager

/**
 * String from Assets file
 *
 * @param fileName The name of the asset to open.  This name can be hierarchical.
 * @return String object
 */
fun AssetManager.fileAsString(fileName: String): String =
    open(fileName).use { it.readBytes().toString(Charset.defaultCharset()) }

/**
 * String from Assets file
 *
 * @param fileName The name of the asset to open.  This name can be hierarchical.
 * @return String object
 */
fun Context.assetAsString(fileName: String): String =
    assets.open(fileName).use { it.readBytes().toString(Charset.defaultCharset()) }

// endregion

// region Resources

/**
 * String from raw resource identifier
 *
 * @param id The resource identifier to open
 * @return String object
 */
fun Context.rawResAsString(@RawRes id: Int): String =
    resources.openRawResource(id).use { it.readBytes().toString(Charset.defaultCharset()) }

/**
 * String from raw resource identifier
 *
 * @param id The resource identifier to open
 * @return String object
 */
fun Resources.rawResAsString(@RawRes id: Int): String =
    openRawResource(id).use { it.readBytes().toString(Charset.defaultCharset()) }


// endregion

// region Inflater

/** Obtains the LayoutInflater from this context. */
val Context.inflater: LayoutInflater get() = LayoutInflater.from(this)

/**
 * Inflate a new view hierarchy from the specified xml resource.
 * Throws [InflateException] if there is an error.
 *
 * @param resource ID for an XML layout resource to load
 * @param root Optional view to be the parent of the generated hierarchy
 * @param attachToRoot Whether the inflated hierarchy should be attached to the root parameter?
 * @return The root View of the inflated hierarchy.
 */
fun Context.inflate(
    @LayoutRes resource: Int,
    root: ViewGroup? = null,
    attachToRoot: Boolean = false
): View = inflater.inflate(resource, root, attachToRoot)

// endregion

// region Network

/**
 * @return True if a network connection is available.
 */
@get:RequiresPermission(android.Manifest.permission.ACCESS_NETWORK_STATE)
val Context.isNetworkAvailable: Boolean
    get() = (getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager).run {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            getSystemService<ConnectivityManager>()
            val capabilities = getNetworkCapabilities(activeNetwork) ?: return@run false
            arrayOf(
                NetworkCapabilities.TRANSPORT_WIFI,
                NetworkCapabilities.TRANSPORT_CELLULAR,
                NetworkCapabilities.TRANSPORT_ETHERNET
            ).firstOrNull { capabilities.hasTransport(it) } == null
        } else {
            @Suppress("DEPRECATION")
            activeNetworkInfo?.isConnectedOrConnecting == true
        }
    }

// endregion

// region Intents

/**
 * Extension method to get a new Intent for an Activity class
 */
inline fun <reified T : Activity> Context.intent(): Intent =
    Intent(this, T::class.java)

/**
 * Create an intent for [T] and apply a lambda on it
 */
inline fun <reified T : Any> Context.intent(body: Intent.() -> Unit): Intent =
    Intent(this, T::class.java).apply(body)

/**
 * Open a URL
 *
 * @param url The URL to open
 * @param onActivityNotFound callback to be invoked if no activity could open the provided URL
 */
fun Context.openUrl(
    url: String,
    onActivityNotFound: (url: String) -> Unit = { }
) = try {
    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
} catch (e: ActivityNotFoundException) {
    onActivityNotFound(url)
}

/**
 * Open the Google Play Store to an app's details page
 *
 * @param pname The package name of the application to view on the Play Store
 * @param onActivityNotFound The callback to be invoked if no activity could open the Play Store
 */
fun Context.openPlayStore(
    pname: String = packageName,
    onActivityNotFound: (url: String) -> Unit = { }
) = "https://play.google.com/store/apps/details?id=$pname".let { url ->
    try {
        startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                setPackage("com.android.vending")
            }
        )
    } catch (e: ActivityNotFoundException) {
        openUrl(url, onActivityNotFound)
    }
}

// endregion

// region Services

/**
 * Return the handle to a system-level service by class.
 *
 * @see ContextCompat.getSystemService
 */
inline fun <reified T : Any> Context.getSystemService(): T? =
    ContextCompat.getSystemService(this, T::class.java)

/** Get the system alarm service. */
val Context.alarmManager get() = getSystemService(Context.ALARM_SERVICE) as AlarmManager

/** Get the AudioManager provides access to volume and ringer mode control. */
val Context.audioManager get() = getSystemService(Context.AUDIO_SERVICE) as AudioManager

/** Get the interface to the clipboard service, for placing and retrieving text in the global clipboard. */
val Context.clipboardManager get() = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

/** Get the service which arbitrates interaction between applications and the current input method. */
val Context.inputMethodManager get() = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

/** Get the service for inflating layout resources in this context. */
val Context.layoutInflater get() = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

/** Get the service which provides access to the system location services. */
val Context.locationManager get() = getSystemService(Context.LOCATION_SERVICE) as LocationManager

/** Get the service for informing the user of background events */
val Context.notificationManager get() = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

/** Get the service for interacting with the vibration hardware. */
val Context.vibrator get() = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

// endregion

// region Context Convenience Functions

inline fun <reified T : Application> Context.app(): T = this.applicationContext as T

/**
 * Return a [TypedArray] holding the values defined by the attribute resource ID
 *
 * @param attrId The attribute ID
 * @param obtain A [TypedArray] holding the attribute values.
 * @return The result of [obtain]
 */
fun <T> Context.obtainAttrValue(@AttrRes attrId: Int, obtain: (typedArray: TypedArray) -> T): T =
    theme.obtainStyledAttributes(arrayOf(attrId).toIntArray()).let { typedArray ->
        obtain(typedArray).also { typedArray.recycle() }
    }

/**
 * Check if an application is installed on the device.
 *
 * @param packageName The full name (i.e. com.google.apps.contacts) of an application.
 * @return True if the package name is installed on the device
 */
fun Context.isPackageInstalled(packageName: String): Boolean = try {
    packageManager.getApplicationInfo(packageName, 0); true
} catch (e: PackageManager.NameNotFoundException) {
    false
}

// endregion

// region Intents

fun Intent.isAvailable(): Boolean = context().packageManager.queryIntentActivities(
    this, PackageManager.MATCH_DEFAULT_ONLY
).isNotEmpty()

/**
 * Operator function to set the put an item in a bundle
 *
 *     bundle["key"] = value
 */
inline operator fun Intent.set(key: String, value: Any?): Unit =
    put(key, value)

/**
 * Operator function to get a bundle value with a default non-null value
 *
 *     val foo: String = bundle["key", "defualt value"]
 */
inline operator fun <reified T : Any> Intent.get(key: String, defValue: T): T =
    get(key) ?: defValue

/**
 * Operator function to get a bundle value
 *
 *     val foo: String? = bundle["key"]
 */
inline operator fun <reified T : Any> Intent.get(key: String): T? =
    extras?.get(key) as? T

/**
 * Returns a new [Intent] with the given key/value pairs as extras.
 *
 * @throws IllegalArgumentException When a value is not a supported type of [Bundle].
 */
fun Intent.put(key: String, value: Any?) {
    when (value) {
        null -> putExtra(key, null as String?) // Any nullable type will suffice.

        // Scalars
        is Boolean -> putExtra(key, value)
        is Byte -> putExtra(key, value)
        is Char -> putExtra(key, value)
        is Double -> putExtra(key, value)
        is Float -> putExtra(key, value)
        is Int -> putExtra(key, value)
        is Long -> putExtra(key, value)
        is Short -> putExtra(key, value)

        // References
        is Bundle -> putExtra(key, value)
        is String -> putExtra(key, value)
        is CharSequence -> putExtra(key, value)
        is Parcelable -> putExtra(key, value)

        // Scalar arrays
        is BooleanArray -> putExtra(key, value)
        is ByteArray -> putExtra(key, value)
        is CharArray -> putExtra(key, value)
        is DoubleArray -> putExtra(key, value)
        is FloatArray -> putExtra(key, value)
        is IntArray -> putExtra(key, value)
        is LongArray -> putExtra(key, value)
        is ShortArray -> putExtra(key, value)

        // Reference arrays
        is Array<*> -> {
            val componentType = value::class.java.componentType!!
            @Suppress("UNCHECKED_CAST") // Checked by reflection.
            when {
                Parcelable::class.java.isAssignableFrom(componentType) -> {
                    putExtra(key, value as Array<Parcelable>)
                }
                String::class.java.isAssignableFrom(componentType) -> {
                    putExtra(key, value as Array<String>)
                }
                CharSequence::class.java.isAssignableFrom(componentType) -> {
                    putExtra(key, value as Array<CharSequence>)
                }
                Serializable::class.java.isAssignableFrom(componentType) -> {
                    putExtra(key, value)
                }
                else -> {
                    val valueType = componentType.canonicalName
                    throw IllegalArgumentException("Illegal value array type $valueType for key \"$key\"")
                }
            }
        }
        is ArrayList<*> -> {
            val componentType = value::class.java.componentType!!
            @Suppress("UNCHECKED_CAST") // Checked by reflection.
            when {
                Parcelable::class.java.isAssignableFrom(componentType) -> {
                    putParcelableArrayListExtra(key, value as ArrayList<Parcelable>)
                }
                Int::class.java.isAssignableFrom(componentType) -> {
                    putIntegerArrayListExtra(key, value as ArrayList<Int>)
                }
                String::class.java.isAssignableFrom(componentType) -> {
                    putStringArrayListExtra(key, value as ArrayList<String>)
                }
                CharSequence::class.java.isAssignableFrom(componentType) -> {
                    putCharSequenceArrayListExtra(key, value as ArrayList<CharSequence>)
                }
                else -> {
                    val valueType = componentType.canonicalName
                    throw IllegalArgumentException("Illegal value array type $valueType for key \"$key\"")
                }
            }
        }

        // Last resort. Also we must check this after Array<*> as all arrays are serializable.
        is Serializable -> putExtra(key, value)

        else -> {
            val valueType = value.javaClass.canonicalName
            throw IllegalArgumentException("Illegal value type $valueType for key \"$key\"")
        }

    }
}

// endregion

// region Resources

/**
 * Extension method to find a device height in pixels
 */
inline val Context.displayHeight: Int get() = resources.displayMetrics.heightPixels

/**
 * Extension method to find a device width in pixels
 */
inline val Context.displayWidth: Int get() = resources.displayMetrics.widthPixels

/** Convert device-independent pixels to pixels */
val Float.dp: Float get() = this * context().resources.displayMetrics.density
/** Convert scale-independent pixels to pixels */
val Float.sp: Float get() = this * context().resources.displayMetrics.scaledDensity
/** Convert pixels to device-independent pixels */
val Float.toDp: Float get() = this / context().resources.displayMetrics.density
/** Convert pixels to device-independent pixels */
val Float.toSp: Float get() = this / context().resources.displayMetrics.scaledDensity
/** Convert device-independent pixels to pixels */
val Float.dip get() = dp

/** Convert device-independent pixels to pixels */
val Int.dp: Int get() = this.toFloat().dp.roundToInt()
/** Convert scale-independent pixels to pixels */
val Int.sp: Int get() = this.toFloat().sp.roundToInt()
/** Convert pixels to device-independent pixels */
val Int.toDp: Int get() = this.toFloat().toDp.roundToInt()
/** Convert pixels to scale-independent pixels */
val Int.toSp: Int get() = this.toFloat().toSp.roundToInt()
/** Convert device-independent pixels to pixels */
val Int.dip get() = dp

/**
 * Returns a localized string from the application's package's default string table.
 *
 * @param resId Resource id for the string
 * @return The string data associated with the resource, stripped of styled
 *         text information.
 */
fun @receiver:StringRes Int.string(context: Context = context()): String {
    return context.getString(this)
}

/**
 * Returns a color associated with a particular resource ID
 *
 * @param context context used to get the application's resources.
 * @return A single color value in the form 0xAARRGGBB.
 * @throws Resources.NotFoundException if the given ID does not exist.
 */
@ColorInt
fun @receiver:ColorRes Int.color(context: Context = context()): Int {
    return ContextCompat.getColor(context, this)
}

/**
 * Returns a color state list associated with a particular resource ID.
 *
 * @param context The context to use to style the color as of [Build.VERSION_CODES.M]
 * @return A color state list, or {@code null} if the resource could not be resolved.
 * @throws Resources.NotFoundException if the given ID does not exist.
 */
fun @receiver:ColorRes Int.colors(context: Context = context()): ColorStateList? {
    return ContextCompat.getColorStateList(context, this)
}

/**
 * Returns a drawable object associated with a particular resource ID.
 *
 * @param context The context to use to style the color as of [Build.VERSION_CODES.LOLLIPOP]
 * @return Drawable An object that can be used to draw this resource.
 */
fun @receiver:DrawableRes Int.drawable(context: Context = context()): Drawable? {
    return ContextCompat.getDrawable(context, this)
}

/**
 * Return the int array associated with a particular resource ID.
 *
 * @param context context used to get the application's resources.
 * @return The int array associated with the resource.
 * @throws Resources.NotFoundException Throws NotFoundException if the given ID does not exist.
 */
fun @receiver:ArrayRes Int.intArray(context: Context = context()): IntArray {
    return context.resources.getIntArray(this)
}

/**
 * Return the String array associated with a particular resource ID.
 *
 * @param context context used to get the application's resources.
 * @return The String array associated with the resource.
 * @throws Resources.NotFoundException Throws NotFoundException if the given ID does not exist.
 */
fun @receiver:ArrayRes Int.stringArray(context: Context = context()): Array<String> {
    return context.resources.getStringArray(this)
}

/**
 * Return the styled text array associated with a particular resource ID.
 *
 * @param context context used to get the application's resources.
 * @return The styled text array associated with the resource.
 * @throws Resources.NotFoundException Throws NotFoundException if the given ID does not exist.
 */
fun @receiver:ArrayRes Int.textArray(context: Context = context()): Array<CharSequence> {
    return context.resources.getTextArray(this)
}

/**
 * Return the type name for a given resource identifier.
 *
 * @param context context used to get the application's resources.
 * @return A string holding the type name of the resource.
 * @throws Resources.NotFoundException Throws NotFoundException if the given ID does not exist.
 */
fun @receiver:AnyRes Int.typeName(context: Context = context()): String {
    return context.resources.getResourceTypeName(this)
}

/**
 * Return the entry name for a given resource identifier.
 *
 * @param context context used to get the application's resources.
 * @return A string holding the entry name of the resource.
 * @throws Resources.NotFoundException Throws NotFoundException if the given ID does not exist.
 */
fun @receiver:AnyRes Int.name(context: Context = context()): String {
    return context.resources.getResourceEntryName(this)
}

/**
 * Retrieve a dimensional for a particular resource ID.
 *
 * @param context context used to get the application's resources.
 * @return Resource dimension value multiplied by the appropriate metric to convert to pixels.
 * @throws Resources.NotFoundException Throws NotFoundException if the given ID does not exist.
 */
@Px
fun @receiver:DimenRes Int.dimension(context: Context = context()): Float {
    return context.resources.getDimension(this)
}

/**
 * Retrieve a dimensional for a particular resource ID for use as an offset in raw pixels.
 * This is the same as [Int.dimension], except the returned value is converted to
 * integer pixels for you. An offset conversion involves simply truncating the base value
 * to an integer.
 *
 * @param context context used to get the application's resources.
 * @return Resource dimension value multiplied by the appropriate metric and truncated to integer pixels.
 * @throws Resources.NotFoundException Throws NotFoundException if the given ID does not exist.
 */
@Px
fun @receiver:DimenRes Int.dimensionPixelOffset(context: Context = context()): Int {
    return context.resources.getDimensionPixelOffset(this)
}

/**
 * Retrieve a dimensional for a particular resource ID for use as a size in raw pixels.
 * This is the same as [Int.dimension], except the returned value is converted to
 * integer pixels for use as a size. A size conversion involves rounding the base value,
 * and ensuring that a non-zero base value is at least one pixel in size.
 *
 * @param context context used to get the application's resources.
 * @return Resource dimension value multiplied by the appropriate metric and truncated to integer pixels.
 * @throws Resources.NotFoundException Throws NotFoundException if the given ID does not exist.
 */
@Px
fun @receiver:DimenRes Int.dimensionPixelSize(context: Context = context()): Int {
    return context.resources.getDimensionPixelSize(this)
}

/**
 * Return the Typeface value associated with a particular resource ID.
 *
 * @param context context used to get the application's resources.
 * @return Typeface The Typeface data associated with the resource.
 * @throws Resources.NotFoundException Throws NotFoundException if the given ID does not exist.
 */
@RequiresApi(Build.VERSION_CODES.O)
fun @receiver:FontRes Int.font(context: Context = context()): Typeface {
    return context.resources.getFont(this)
}

/**
 * Return a boolean associated with a particular resource ID.  This can be used with any integral
 * resource value, and will return true if it is non-zero.
 *
 * @param context context used to get the application's resources.
 * @return Returns the boolean value contained in the resource.
 * @throws Resources.NotFoundException Throws NotFoundException if the given ID does not exist.
 */
fun @receiver:BoolRes Int.bool(context: Context = context()): Boolean {
    return context.resources.getBoolean(this)
}

/**
 * Return an integer associated with a particular resource ID.
 *
 * @param context context used to get the application's resources.
 * @return Returns the integer value contained in the resource.
 * @throws Resources.NotFoundException Throws NotFoundException if the given ID does not exist.
 */
fun @receiver:IntegerRes Int.integer(context: Context = context()): Int {
    return context.resources.getInteger(this)
}

/**
 * Retrieve a floating-point value for a particular resource ID.
 *
 * @param context context used to get the application's resources.
 * @return Returns the floating-point value contained in the resource.
 * @throws Resources.NotFoundException Throws NotFoundException if the given ID does not exist
 * or is not a floating-point value.
 */
fun @receiver:DimenRes Int.float(context: Context = context()): Float {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        context.resources.getFloat(this)
    } else {
        TypedValue().also { value ->
            context.resources.getValue(this, value, true)
        }.float
    }
}

// endregion
