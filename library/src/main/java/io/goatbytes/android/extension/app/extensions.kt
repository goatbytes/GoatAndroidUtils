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

package io.goatbytes.android.extension.app

import android.app.Activity
import android.app.Service
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Point
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.view.inputmethod.InputMethodManager
import androidx.annotation.IdRes
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction


// region Activity


/**
 * Get the orientation of the screen.
 *
 * @param activity
 *     The current Activity.
 * @return One of the following values:
 *
 * <ul>
 * <li>{@link ActivityInfo#SCREEN_ORIENTATION_LANDSCAPE}</li>
 * <li>{@link ActivityInfo#SCREEN_ORIENTATION_REVERSE_LANDSCAPE}</li>
 * <li>{@link ActivityInfo#SCREEN_ORIENTATION_PORTRAIT}</li>
 * <li>{@link ActivityInfo#SCREEN_ORIENTATION_REVERSE_PORTRAIT}</li>
 * </ul>
 */
val Activity.screenOrientation: Int
    get() = (getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay.rotation.let { rotation ->
        when (resources.configuration.orientation) {
            Configuration.ORIENTATION_LANDSCAPE -> when (rotation) {
                Surface.ROTATION_0, Surface.ROTATION_90 -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                else -> ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
            }
            Configuration.ORIENTATION_PORTRAIT -> when (rotation) {
                Surface.ROTATION_0, Surface.ROTATION_270 -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                else -> ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT
            }
            else -> when (rotation) {
                Surface.ROTATION_0, Surface.ROTATION_270 -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                else -> ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT
            }
        }
    }

/**
 * Lock the screen orientation.
 *
 * @param activity
 *     The current Activity.
 * @see #unlockOrientation(Activity)
 */
fun Activity.lockOrientation() {
    requestedOrientation = screenOrientation
}

/**
 * Unlock the screen orientation
 *
 * @param activity
 *     The current Activity
 * @see #lockOrientation(Activity)
 */
fun Activity.unlockOrientation() {
    requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
}

val Context.window: Point
    get() = (getSystemService(Context.WINDOW_SERVICE) as WindowManager).let { wm ->
        Point().apply { wm.defaultDisplay.getSize(this) }
    }

val Activity.windowSize: Point
    get() = (getSystemService(Context.WINDOW_SERVICE) as WindowManager).let { wm ->
        Point().apply { wm.defaultDisplay.getSize(this) }
    }

val Activity.screenHeight get() = windowSize.y

val Activity.screenWidth get() = windowSize.x

/**
 * Get the orientation of the screen.
 *
 * @return One of the following values:
 *
 * <ul>
 * <li>{@link ActivityInfo#SCREEN_ORIENTATION_LANDSCAPE}</li>
 * <li>{@link ActivityInfo#SCREEN_ORIENTATION_REVERSE_LANDSCAPE}</li>
 * <li>{@link ActivityInfo#SCREEN_ORIENTATION_PORTRAIT}</li>
 * <li>{@link ActivityInfo#SCREEN_ORIENTATION_REVERSE_PORTRAIT}</li>
 * </ul>
 */
val Activity.orientation: Int
    get() = (getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay.rotation.let { rotation ->
        when (resources.configuration.orientation) {
            Configuration.ORIENTATION_LANDSCAPE -> when (rotation) {
                Surface.ROTATION_0, Surface.ROTATION_90 -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                else -> ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
            }
            Configuration.ORIENTATION_PORTRAIT -> when (rotation) {
                Surface.ROTATION_0, Surface.ROTATION_270 -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                else -> ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT
            }
            else -> when (rotation) {
                Surface.ROTATION_0, Surface.ROTATION_270 -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                else -> ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT
            }
        }
    }

/** Get the root view from the activity */
inline val Activity.content: ViewGroup
    get() = findViewById(android.R.id.content)
        ?: window.decorView.findViewById(android.R.id.content)

/** Launch a new activity. */
inline fun <reified T : Activity> Activity.startActivity(): Unit =
    startActivity(Intent(this, T::class.java))

/** Launch a new activity with the applied intent. */
inline fun <reified T : Activity> Activity.startActivity(action: Intent.() -> Unit): Unit =
    startActivity(Intent(this, T::class.java).apply(action))

/** Launch an activity for which you would like a result when it finished. */
inline fun <reified T : Activity> Activity.startActivityForResult(requestCode: Int): Unit =
    startActivityForResult(Intent(this, T::class.java), requestCode)

/** Launch an activity for which you would like a result when it finished. */
inline fun <reified T : Activity> Activity.startActivityForResult(
    action: Intent.() -> Unit, requestCode: Int
): Unit = startActivityForResult(Intent(this, T::class.java).apply(action), requestCode)

/** Request that a given application service be started. */
inline fun <reified T : Service> Activity.startService(): ComponentName? =
    startService(Intent(this, T::class.java))

/** Finds a fragment that was identified by the given tag */
inline fun <reified T : Fragment> FragmentActivity.findFragmentByTag(tag: String): T? =
    supportFragmentManager.findFragmentByTag(tag) as? T

/** Finds a fragment that was identified by the given id */
inline fun <reified T : Fragment> FragmentActivity.findFragmentById(@IdRes id: Int): T? =
    supportFragmentManager.findFragmentById(id) as? T

/** Commit edit operations on a new fragment transaction */
inline fun FragmentActivity.fragmentTransaction(
    function: FragmentTransaction.() -> FragmentTransaction
): Int = supportFragmentManager.beginTransaction().function().commit()

/** Hide the soft keyboard */
fun Activity.hideKeyboard(): Boolean {
    (currentFocus ?: window.decorView ?: View(this)).let { v ->
        return try {
            (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
                .hideSoftInputFromWindow(v.windowToken, 0)
        } catch (_: Exception) {
            false
        }
    }
}

// endregion

// region Fragment

/** Obtains the LayoutInflater from this fragment's context. */
val Fragment.inflater: LayoutInflater get() = LayoutInflater.from(requireContext())

/** Destroy the hold activity */
fun Fragment.finish(): Unit = requireActivity().finish()

/** Launch a new activity. */
inline fun <reified T : Activity> Fragment.startActivity(): Unit =
    startActivity(Intent(activity, T::class.java))

/** Launch a new activity with the applied intent. */
inline fun <reified T : Activity> Fragment.startActivity(action: Intent.() -> Unit): Unit =
    startActivity(Intent(activity, T::class.java).apply(action))

/** Launch an activity for which you would like a result when it finished. */
inline fun <reified T : Activity> Fragment.startActivityForResult(requestCode: Int): Unit =
    startActivityForResult(Intent(activity, T::class.java), requestCode)

/** Launch an activity for which you would like a result when it finished. */
inline fun <reified T : Activity> Fragment.startActivityForResult(
    action: Intent.() -> Unit, requestCode: Int
): Unit = startActivityForResult(Intent(activity, T::class.java).apply(action), requestCode)

/** Request that a given application service be started. */
inline fun <reified T : Service> Fragment.startService(): ComponentName? =
    requireActivity().startService(Intent(activity, T::class.java))

/** Set arguments to fragment and return current instance */
inline fun <reified T : Fragment> T.withArguments(action: Bundle.() -> Unit): T = apply {
    this.arguments = Bundle().also(action)
}

/** Commit edit operations on a new child fragment transaction */
inline fun Fragment.childFragmentTransaction(
    function: FragmentTransaction.() -> FragmentTransaction
): Int = childFragmentManager.beginTransaction().function().commit()

/** Commit edit operations on a new fragment transaction */
inline fun Fragment.fragmentTransaction(
    function: FragmentTransaction.() -> FragmentTransaction
): Int = parentFragmentManager.beginTransaction().function().commit()

/** Open an URL */
fun Fragment.openUrl(url: String, onActivityNotFound: (url: String) -> Unit = { }) = try {
    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
} catch (e: ActivityNotFoundException) {
    onActivityNotFound(url)
}

/** Runs a FragmentTransaction, then calls commit(). */
private inline fun FragmentManager.transact(action: FragmentTransaction.() -> Unit): Int =
    beginTransaction().apply { action() }.commit()

/** Clear all fragments in the backstack */
fun FragmentManager.clearBackStack() {
    if (backStackEntryCount > 0) {
        val entry = getBackStackEntryAt(0)
        popBackStack(entry.id, FragmentManager.POP_BACK_STACK_INCLUSIVE)
    }
}

// endregion
