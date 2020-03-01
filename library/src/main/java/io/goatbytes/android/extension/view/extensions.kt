/*
 * Copyright (C) 2019 goatbytes.io
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

package io.goatbytes.android.extension.view

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.view.*
import android.view.animation.*
import android.view.inputmethod.InputMethodManager
import androidx.annotation.*
import androidx.core.content.ContextCompat
import androidx.core.graphics.applyCanvas
import androidx.core.view.ViewCompat
import androidx.core.view.updatePadding
import androidx.core.view.updatePaddingRelative
import io.goatbytes.android.extension.content.dp

// region Menu

/** Returns the number of items in this menu. */
inline val Menu.size get() = size()

/** Returns true if this menu contains no items. */
inline fun Menu.isEmpty() = size() == 0

/** Returns true if this menu contains one or more items. */
inline fun Menu.isNotEmpty() = size() != 0

/** Performs the given action on each item in this menu. */
inline fun Menu.forEach(action: (item: MenuItem) -> Unit) {
    for (index in 0 until size()) {
        action(getItem(index))
    }
}

/** Performs the given action on each item in this menu, providing its sequential index. */
inline fun Menu.forEachIndexed(action: (index: Int, item: MenuItem) -> Unit) {
    for (index in 0 until size()) {
        action(index, getItem(index))
    }
}

/** Returns a [MutableIterator] over the items in this menu. */
operator fun Menu.iterator() = object : MutableIterator<MenuItem> {
    private var index = 0
    override fun hasNext() = index < size()
    override fun next() = getItem(index++) ?: throw IndexOutOfBoundsException()
    override fun remove() = removeItem(--index)
}

/** Returns a [Sequence] over the items in this menu. */
val Menu.children: Sequence<MenuItem>
    get() = object : Sequence<MenuItem> {
        override fun iterator() = this@children.iterator()
    }

// endregion

// region Views

// ---- Animations


/**
 * Animate a view expanding, like an accordion
 */
fun <V : View> V.expand(onAnimationEnd: (v: V) -> Unit = {}) {
    measure(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    val targetHeight = measuredHeight
    layoutParams.height = 0
    visibility = View.VISIBLE
    object : Animation() {
        override fun applyTransformation(interpolatedTime: Float, t: Transformation?) {
            layoutParams.height = if (interpolatedTime == 1f)
                ViewGroup.LayoutParams.WRAP_CONTENT
            else
                (targetHeight * interpolatedTime).toInt()
            requestLayout()
        }

        override fun willChangeBounds(): Boolean = true
    }.apply {
        duration = (targetHeight / context.resources.displayMetrics.density).toLong()
        setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation?) {
            }

            override fun onAnimationRepeat(animation: Animation?) {
            }

            override fun onAnimationEnd(animation: Animation?) {
                onAnimationEnd(this@expand)
            }
        })
    }.also { anim ->
        startAnimation(anim)
    }
}

/** Animate a view collapsing, like an accordion */
fun <V : View> V.collapse(onAnimationEnd: (v: V) -> Unit = {}) {
    val initialHeight = measuredHeight
    object : Animation() {
        override fun willChangeBounds(): Boolean = true
        override fun applyTransformation(interpolatedTime: Float, t: Transformation?) {
            if (interpolatedTime == 1f) {
                visibility = View.GONE
            } else {
                layoutParams.height = initialHeight - (initialHeight * interpolatedTime).toInt()
                requestLayout()
            }
        }
    }.apply {
        duration = (initialHeight / context.resources.displayMetrics.density).toLong()
        setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation?) {
            }

            override fun onAnimationRepeat(animation: Animation?) {
            }

            override fun onAnimationEnd(animation: Animation?) {
                onAnimationEnd(this@collapse)
            }
        })
    }.also { anim ->
        startAnimation(anim)
    }
}

/**
 * Play a bounce animation on a view.
 */
fun <V : View> V.bounce(
    duration: Long = 500,
    onAnimationEnd: (v: V) -> Unit = {}
) {
    ObjectAnimator.ofPropertyValuesHolder(
        this,
        PropertyValuesHolder.ofFloat(View.SCALE_X, 0.9f, 1.0f),
        PropertyValuesHolder.ofFloat(View.SCALE_Y, 0.9f, 1.0f)
    ).apply {
        this.duration = duration
        this.interpolator = OvershootInterpolator()
        addListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator?) {
            }

            override fun onAnimationCancel(animation: Animator?) {
            }

            override fun onAnimationRepeat(animation: Animator?) {
            }

            override fun onAnimationEnd(animation: Animator?) {
                onAnimationEnd(this@bounce)
            }

        })
    }.start()
}

/**
 * Fade a view in. The view's visibility is set to {@link View#VISIBLE} when the animation starts.
 *
 * @param duration The duration of the fade animation.
 */
fun <V : View> V.fadeIn(
    duration: Long = 500,
    onAnimationEnd: (v: V) -> Unit = {}
) {
    setLayerType(View.LAYER_TYPE_HARDWARE, null)
    animate().alpha(1f)
        .setInterpolator(DecelerateInterpolator())
        .setDuration(duration)
        .setListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator?) {
                setLayerType(View.LAYER_TYPE_NONE, null)
                onAnimationEnd(this@fadeIn)
            }

            override fun onAnimationStart(animation: Animator?) {
                visibility = View.VISIBLE
            }
        })
}

/**
 * Fade the view out.
 *
 * @param duration
 *     The duration of the animation.
 * @param onAnimationEnd Executed after the animation is complete.
 */
fun <V : View> V.fadeOut(
    duration: Long = 500,
    onAnimationEnd: (v: V) -> Unit = {}
) {
    setLayerType(View.LAYER_TYPE_HARDWARE, null)
    animate().alpha(0f).setInterpolator(DecelerateInterpolator()).setDuration(duration)
        .setListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator?) {
                setLayerType(View.LAYER_TYPE_NONE, null)
                onAnimationEnd(this@fadeOut)
            }
        })
}

/** Shake the view */
fun <V : View> V.shake(
    duration: Long = 500,
    onAnimationEnd: (v: V) -> Unit = {}
) {
    val values = floatArrayOf(0.0f, 25.0f, -25.0f, 25.0f, -25.0f, 15.0f, -15.0f, 6.0f, -6.0f, 0.0f)
    ObjectAnimator.ofFloat(this, View.TRANSLATION_X, *values).apply {
        this.interpolator = AccelerateInterpolator()
        this.duration = duration
        addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationStart(animation: Animator?) {
                setLayerType(View.LAYER_TYPE_HARDWARE, null)
            }

            override fun onAnimationCancel(animation: Animator?) {
                setLayerType(View.LAYER_TYPE_NONE, null)
            }

            override fun onAnimationEnd(animation: Animator?) {
                setLayerType(View.LAYER_TYPE_NONE, null)
                onAnimationEnd(this@shake)
            }
        })
    }.start()
}

/**
 * Slide the view down.
 *
 * @param duration The duration of the animation. Default is 500ms
 * @param onAnimationEnd Run after the animation ends.
 * @return The [ViewPropertyAnimator] for chaining animations.
 */
fun <V : View> V.slideDown(
    duration: Long = 500,
    onAnimationEnd: (v: V) -> Unit = {}
): ViewPropertyAnimator = animate().translationY(((parent as View).bottom - top).toFloat())
    .setInterpolator(DecelerateInterpolator(2f)).setDuration(duration)
    .setListener(object : AnimatorListenerAdapter() {
        override fun onAnimationEnd(animation: Animator?) {
            onAnimationEnd(this@slideDown)
        }
    })

/**
 * Slide fade in animation.
 *
 * @param duration The duration of the animation. Default is 500ms.
 * @return The [ObjectAnimator]
 */
fun <V : View> V.slideFadeIn(
    duration: Long = 500,
    onAnimationEnd: (v: V) -> Unit = {}
): ObjectAnimator = ObjectAnimator.ofPropertyValuesHolder(
    this,
    PropertyValuesHolder.ofFloat(View.ALPHA, 0.00f, 1.0f),
    PropertyValuesHolder.ofFloat(View.TRANSLATION_Y, 16f.dp, 0.0f)
).apply {
    this.duration = duration
    this.interpolator = DecelerateInterpolator()
    addListener(object : AnimatorListenerAdapter() {
        override fun onAnimationStart(animation: Animator?) {
            visibility = View.VISIBLE
        }

        override fun onAnimationEnd(animation: Animator?) {
            onAnimationEnd(this@slideFadeIn)
        }
    })
}


/**
 * Slide up a view. The animation's duration is 500ms.
 *
 * @receiver The view to animate.
 */
fun <V : View> V.slideUp(
    duration: Long = 500,
    onAnimationEnd: (v: V) -> Unit = {}
) {
    translationY = (parent as View).bottom - top.toFloat()
    animate().translationY(0f).setInterpolator(DecelerateInterpolator(2f))
        .setDuration(duration).setListener(object : AnimatorListenerAdapter() {
            override fun onAnimationStart(animator: Animator) {
                visibility = View.VISIBLE
            }

            override fun onAnimationEnd(animation: Animator?) {
                onAnimationEnd(this@slideUp)
            }
        })
}

/**
 * Rotate the view.
 *
 * @param fromDegrees degrees to rotate -360...360
 * @param toDegrees degrees to rotate -360...360
 * @param duration The duration of the animation. Default is 500ms
 */
fun <V : View> V.rotate(
    @FloatRange(from = -360.0, to = 360.0) fromDegrees: Float,
    @FloatRange(from = -360.0, to = 360.0) toDegrees: Float,
    duration: Long = 500,
    onAnimationEnd: (v: V) -> Unit = {}
) {
    startAnimation(
        RotateAnimation(
            fromDegrees, toDegrees,
            Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f
        ).apply {
            this.duration = duration
            this.fillAfter = true
            setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationStart(animation: Animation?) {
                }

                override fun onAnimationRepeat(animation: Animation?) {
                }

                override fun onAnimationEnd(animation: Animation?) {
                    onAnimationEnd(this@rotate)
                }
            })
        })
}


// ---- Bitmap

/**
 * Return a [Bitmap] representation of this [View].
 *
 * The resulting bitmap will be the same width and height as this view's current layout
 * dimensions. This does not take into account any transformations such as scale or translation.
 *
 * Note, this will use the software rendering pipeline to draw the view to the bitmap. This may
 * result with different drawing to what is rendered on a hardware accelerated canvas (such as
 * the device screen).
 *
 * If this view has not been laid out this method will throw a [IllegalStateException].
 *
 * @param config Bitmap config of the desired bitmap. Defaults to [Bitmap.Config.ARGB_8888].
 */
fun View.toBitmap(config: Bitmap.Config = Bitmap.Config.ARGB_8888): Bitmap {
    if (!ViewCompat.isLaidOut(this)) {
        throw IllegalStateException("View needs to be laid out before calling drawToBitmap()")
    }
    return Bitmap.createBitmap(width, height, config).applyCanvas {
        translate(-scrollX.toFloat(), -scrollY.toFloat())
        draw(this)
    }
}

// ---- Keyboard

/**
 * Extension method to show a keyboard for View.
 */
fun View.showKeyboard() = apply {
    requestFocus().also {
        (context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
            .showSoftInput(this, 0)
    }
}

/**
 * Try to hide the keyboard and returns true if it worked
 */
fun View.hideKeyboard() = apply {
    try {
        (context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
            .hideSoftInputFromWindow(windowToken, 0)
    } catch (ignored: RuntimeException) {
    }
}

// ---- Layout Inflater

/** Obtains the LayoutInflater from this view's context. */
val View.inflater: LayoutInflater get() = LayoutInflater.from(context)

/** Inflate a new view hierarchy from the specified xml resource */
infix fun ViewGroup.inflate(@LayoutRes id: Int): View = inflater.inflate(id, this, false)

/**
 * Inflate and add a view to a ViewGroup
 *
 * @param layoutRes The layout resource to inflate
 */
infix fun ViewGroup.addView(@LayoutRes layoutRes: Int): View =
    inflate(layoutRes).also { addView(it) }

// ---- Layout Changes

/**
 * Performs the given action when this view is next laid out.
 *
 * The action will only be invoked once on the next layout and then removed.
 *
 * @see doOnLayout
 */
inline fun <reified T : View> T.doOnNextLayout(crossinline action: (view: T) -> Unit): Unit =
    addOnLayoutChangeListener(object : View.OnLayoutChangeListener {
        override fun onLayoutChange(
            view: View,
            left: Int,
            top: Int,
            right: Int,
            bottom: Int,
            oldLeft: Int,
            oldTop: Int,
            oldRight: Int,
            oldBottom: Int
        ) {
            view.removeOnLayoutChangeListener(this)
            action(view as T)
        }
    })


/**
 * Performs the given action when this view is laid out. If the view has been laid out and it
 * has not requested a layout, the action will be performed straight away, otherwise the
 * action will be performed after the view is next laid out.
 *
 * The action will only be invoked once on the next layout and then removed.
 *
 * @see doOnNextLayout
 */
inline fun <reified T : View> T.doOnLayout(crossinline action: (view: T) -> Unit): Unit =
    if (ViewCompat.isLaidOut(this) && !isLayoutRequested) {
        action(this)
    } else {
        doOnNextLayout { action(it) }
    }

/**
 * Extension method to run a block of code after a view has been inflated and measured
 */
inline fun <T : View> T.onGlobalLayout(crossinline action: T.() -> Unit): Unit =
    viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
        override fun onGlobalLayout() {
            if (measuredWidth > 0 && measuredHeight > 0) {
                viewTreeObserver.removeOnGlobalLayoutListener(this)
                action()
            }
        }
    })

/**
 * Version of [View.postDelayed] which re-orders the parameters, allowing the action to be placed
 * outside of parentheses.
 *
 * ```
 * view.postDelayed(200) {
 *     doSomething()
 * }
 * ```
 *
 * @return the created Runnable
 */
inline fun <T : View> T.postDelayed(
    delayInMillis: Long,
    crossinline action: (view: T) -> Unit
): Runnable {
    val runnable = Runnable { action(this) }
    postDelayed(runnable, delayInMillis)
    return runnable
}

// ---- Layout Params

/**
 * Executes [block] with the View's layoutParams and reassigns the layoutParams with the
 * updated version.
 *
 * @see View.getLayoutParams
 * @see View.setLayoutParams
 **/
inline fun View.updateLayoutParams(block: ViewGroup.LayoutParams.() -> Unit) {
    updateLayoutParams<ViewGroup.LayoutParams>(block)
}

/**
 * Executes [block] with a typed version of the View's layoutParams and reassigns the
 * layoutParams with the updated version.
 *
 * @see View.getLayoutParams
 * @see View.setLayoutParams
 **/
@JvmName("updateLayoutParamsTyped")
inline fun <reified T : ViewGroup.LayoutParams> View.updateLayoutParams(block: T.() -> Unit) {
    val params = layoutParams as T
    block(params)
    layoutParams = params
}

// ---- Listeners

/**
 * Set an [View.setOnClickListener] on a view
 */
inline fun <reified T : View> T.onClick(crossinline block: (T) -> Unit): T =
    apply { setOnClickListener { block(it as T) } }

// ---- Margins

/**
 * Returns the left margin if this view's [ViewGroup.LayoutParams] is a [ViewGroup.MarginLayoutParams],
 * otherwise 0.
 *
 * @see ViewGroup.MarginLayoutParams
 */
@get:Px
@setparam:Px
inline var View.marginLeft: Int
    get() = (layoutParams as? ViewGroup.MarginLayoutParams)?.leftMargin ?: 0
    set(value) = updateMargins(left = value)

/**
 * Returns the right margin if this view's [ViewGroup.LayoutParams] is a [ViewGroup.MarginLayoutParams],
 * otherwise 0.
 *
 * @see ViewGroup.MarginLayoutParams
 */
@get:Px
@setparam:Px
inline var View.marginRight: Int
    get() = (layoutParams as? ViewGroup.MarginLayoutParams)?.rightMargin ?: 0
    set(value) = updateMargins(right = value)

/**
 * Returns the top margin if this view's [ViewGroup.LayoutParams] is a [ViewGroup.MarginLayoutParams],
 * otherwise 0.
 *
 * @see ViewGroup.MarginLayoutParams
 */
@get:Px
@setparam:Px
inline var View.marginTop: Int
    get() = (layoutParams as? ViewGroup.MarginLayoutParams)?.topMargin ?: 0
    set(value) = updateMarginsRelative(top = value)

/**
 * Returns the bottom margin if this view's [ViewGroup.LayoutParams] is a [ViewGroup.MarginLayoutParams],
 * otherwise 0.
 *
 * @see ViewGroup.MarginLayoutParams
 */
@get:Px
@setparam:Px
inline var View.marginBottom: Int
    get() = (layoutParams as? ViewGroup.MarginLayoutParams)?.bottomMargin ?: 0
    set(value) = updateMarginsRelative(bottom = value)
/**
 * Returns the start margin if this view's [ViewGroup.LayoutParams] is a [ViewGroup.MarginLayoutParams],
 * otherwise 0.
 *
 * @see ViewGroup.MarginLayoutParams
 */
@get:Px
@setparam:Px
inline var View.marginStart: Int
    get() = (layoutParams as? ViewGroup.MarginLayoutParams)?.marginStart ?: 0
    set(value) = updateMarginsRelative(start = value)

/**
 * Returns the end margin if this view's [ViewGroup.LayoutParams] is a [ViewGroup.MarginLayoutParams],
 * otherwise 0.
 *
 * @see ViewGroup.MarginLayoutParams
 */
@get:Px
@setparam:Px
inline var View.marginEnd: Int
    get() = (layoutParams as? ViewGroup.MarginLayoutParams)?.marginEnd ?: 0
    set(value) = updateMarginsRelative(end = value)

/**
 * Sets the margins, in pixels. This version of the method allows using named parameters
 * to just set one or more margins.
 *
 * @param left the left margin size
 * @param top the top margin size
 * @param right the right margin size
 * @param bottom the bottom margin size
 *
 * @see ViewGroup.MarginLayoutParams.setMargins
 */
inline fun View.updateMargins(
    @Px left: Int = marginLeft,
    @Px top: Int = marginTop,
    @Px right: Int = marginRight,
    @Px bottom: Int = marginBottom
) {
    (layoutParams as? ViewGroup.MarginLayoutParams)?.run {
        setMargins(left, top, right, bottom)
        layoutParams = this
    }
}

/**
 * Sets the margins, in pixels. This version of the method allows using named parameters
 * to just set one or more margins.
 *
 * @param start the start margin size
 * @param top the top margin size
 * @param end the end margin size
 * @param bottom the bottom margin size
 *
 * @see ViewGroup.MarginLayoutParams.setMargins
 */
inline fun View.updateMarginsRelative(
    @Px start: Int = marginStart,
    @Px top: Int = marginTop,
    @Px end: Int = marginEnd,
    @Px bottom: Int = marginBottom
) {
    (layoutParams as? ViewGroup.MarginLayoutParams)?.run {
        marginStart = start; topMargin = top; marginEnd = end; bottomMargin = bottom
        layoutParams = this
    }
}

// ---- Padding

/**
 * Updates this view's padding. This version of the method allows using named parameters
 * to just set one or more axes.
 *
 * @see View.setPadding
 */
inline fun View.updatePadding(
    @Px left: Int = paddingLeft,
    @Px top: Int = paddingTop,
    @Px right: Int = paddingRight,
    @Px bottom: Int = paddingBottom
): Unit = setPadding(left, top, right, bottom)

/**
 * Updates this view's relative padding. This version of the method allows using named parameters
 * to just set one or more axes.
 *
 * @see View.setPaddingRelative
 */
inline fun View.updatePaddingRelative(
    @Px start: Int = paddingStart,
    @Px top: Int = paddingTop,
    @Px end: Int = paddingEnd,
    @Px bottom: Int = paddingBottom
): Unit = setPaddingRelative(start, top, end, bottom)

/**
 * Sets the view's padding. This version of the method sets all axes to the provided size.
 *
 * @see View.setPadding
 */
inline fun View.setPadding(@Px size: Int): Unit = setPadding(size, size, size, size)

/**
 * Returns the left padding of the view.
 *
 * @see View.getPaddingLeft
 * @see updatePadding
 */
@setparam:Px
@get:Px
var View.leftPadding: Int
    get() = paddingLeft
    set(value) = updatePadding(left = value)

/**
 * Returns the right padding of the view.
 *
 * @see View.getPaddingRight
 * @see updatePadding
 */
@setparam:Px
@get:Px
var View.rightPadding: Int
    get() = paddingRight
    set(value) = updatePadding(right = value)

/**
 * Returns the start padding of the view.
 *
 * @see View.getPaddingStart
 * @see updatePaddingRelative
 */
@setparam:Px
@get:Px
var View.startPadding: Int
    get() = paddingStart
    set(value) = updatePaddingRelative(start = value)

/**
 * Returns the end padding of the view.
 *
 * @see View.getPaddingLeft
 * @see updatePadding
 */
@setparam:Px
@get:Px
var View.endPadding: Int
    get() = paddingEnd
    set(value) = updatePaddingRelative(end = value)

/**
 * Returns the top padding of the view.
 *
 * @see View.getPaddingTop
 * @see updatePaddingRelative
 */
@setparam:Px
@get:Px
var View.topPadding: Int
    get() = paddingTop
    set(value) = updatePaddingRelative(top = value)

/**
 * Returns the bottom padding of the view.
 *
 * @see View.getPaddingBottom
 * @see updatePaddingRelative
 */
@setparam:Px
@get:Px
var View.bottomPadding: Int
    get() = paddingBottom
    set(value) = updatePaddingRelative(bottom = value)

// ---- Resources

/**
 * Retrieve a dimensional for a particular resource ID.
 *
 * @param id The desired resource identifier
 * @return Resource dimension value multiplied by the appropriate metric to convert to pixels.
 * @throws Resources.NotFoundException Throws NotFoundException if the given ID does not exist.
 *
 * @see getDimensionPixelOffset
 * @see getDimensionPixelSize
 */
fun View.getDimension(@DimenRes id: Int): Float = resources.getDimension(id)

/**
 * Retrieve a dimensional for a particular resource ID for use as an offset in raw pixels.
 * This is the same as [getDimension], except the returned value is converted to integer pixels
 * for you.  An offset conversion involves simply truncating the base value to an integer.
 *
 * @param id The desired resource identifier
 * @return Resource dimension value multiplied by the appropriate metric and truncated to pixels.
 * @throws Resources.NotFoundException Throws NotFoundException if the given ID does not exist.
 *
 * @see getDimension
 * @see getDimensionPixelSize
 */
fun View.getDimensionPixelOffset(@DimenRes id: Int): Int = resources.getDimensionPixelOffset(id)

/**
 * Retrieve a dimensional for a particular resource ID for use as a size in raw pixels.
 * This is the same as [getDimension], except the returned value is converted to integer pixels
 * for use as a size.  A size conversion involves rounding the base value, and ensuring that a
 * non-zero base value is at least one pixel in size.
 *
 * @param id The desired resource identifier
 * @return Resource dimension value multiplied by the appropriate metric and truncated to pixels.
 * @throws Resources.NotFoundException Throws NotFoundException if the given ID does not exist.
 *
 * @see getDimension
 * @see getDimensionPixelOffset
 */
fun View.getDimensionPixelSize(@DimenRes id: Int): Int = resources.getDimensionPixelSize(id)

/**
 * Returns a color associated with a particular resource ID
 *
 * @param id The desired resource identifier
 * @return A single color value in the form 0xAARRGGBB.
 * @throws Resources.NotFoundException if the given ID does not exist.
 */
fun View.getColor(@ColorRes id: Int): Int = ContextCompat.getColor(context, id)

/**
 * Returns a drawable object associated with a particular resource ID.
 *
 * @param id The desired resource identifier
 * @return Drawable An object that can be used to draw this resource.
 */
fun View.getDrawable(@DrawableRes id: Int): Drawable? = ContextCompat.getDrawable(context, id)

/**
 * Return the string value associated with a particular resource ID.
 *
 * @param id The desired resource identifier
 * @throws Resources.NotFoundException Throws NotFoundException if the given ID does not exist.
 * @return String The string data associated with the resource, stripped of styled text information.
 */
fun View.getString(@StringRes id: Int): String = resources.getString(id)

// ----  Visibility

/**
 * Set the visibility of a view to [View.GONE]
 */
fun View.gone() = apply { visibility = View.GONE }

/**
 * Set the visibility of a view to [View.INVISIBLE]
 */
fun View.hide() = apply { visibility = View.INVISIBLE }

/**
 * Set the visibility of a view to [View.VISIBLE]
 */
fun View.show() = apply { visibility = View.VISIBLE }

/**
 * Returns true when this view's visibility is [View.VISIBLE], false otherwise.
 *
 * ```
 * if (view.isVisible) {
 *     // Behavior...
 * }
 * ```
 *
 * Setting this property to true sets the visibility to [View.VISIBLE], false to [View.GONE].
 *
 * ```
 * view.isVisible = true
 * ```
 */
inline var View.isVisible: Boolean
    get() = visibility == View.VISIBLE
    set(value) {
        visibility = if (value) View.VISIBLE else View.GONE
    }

/**
 * Returns true when this view's visibility is [View.INVISIBLE], false otherwise.
 *
 * ```
 * if (view.isInvisible) {
 *     // Behavior...
 * }
 * ```
 *
 * Setting this property to true sets the visibility to [View.INVISIBLE], false to [View.VISIBLE].
 *
 * ```
 * view.isInvisible = true
 * ```
 */
inline var View.isInvisible: Boolean
    get() = visibility == View.INVISIBLE
    set(value) {
        visibility = if (value) View.INVISIBLE else View.VISIBLE
    }

/**
 * Returns true when this view's visibility is [View.GONE], false otherwise.
 *
 * ```
 * if (view.isGone) {
 *     // Behavior...
 * }
 * ```
 *
 * Setting this property to true sets the visibility to [View.GONE], false to [View.VISIBLE].
 *
 * ```
 * view.isGone = true
 * ```
 */
inline var View.isGone: Boolean
    get() = visibility == View.GONE
    set(value) {
        visibility = if (value) View.GONE else View.VISIBLE
    }

/**
 * Set the visibility of all views in the array to [View.GONE]
 */
fun <T : View> Array<T>.gone() = forEach { it.gone() }

/**
 * Set the visibility of all views in the array to [View.VISIBLE]
 */
fun <T : View> Array<T>.show() = forEach { it.show() }

/**
 * Set the visibility of all views in the array to [View.INVISIBLE]
 */
fun <T : View> Array<T>.hide() = forEach { it.hide() }

// ---- Activity Context


private fun Context.activity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.activity()
    else -> null
}

val View.activity: Activity? get() = context?.activity()

fun View.requireActivity(): Activity = context?.activity() ?: throw IllegalStateException(
    "The unwrapped context of view $this is not an Activity"
)

// endregion

// region ViewGroup

/**
 * Returns the view at [index].
 *
 * @throws IndexOutOfBoundsException if index is less than 0 or greater than or equal to the count.
 */
operator fun ViewGroup.get(index: Int) =
    getChildAt(index) ?: throw IndexOutOfBoundsException("Index: $index, Size: $childCount")

/** Returns `true` if [view] is found in this view group. */
inline operator fun ViewGroup.contains(view: View) = indexOfChild(view) != -1

/** Adds [view] to this view group. */
inline operator fun ViewGroup.plusAssign(view: View) = addView(view)

/** Removes [view] from this view group. */
inline operator fun ViewGroup.minusAssign(view: View) = removeView(view)

/** Returns the number of views in this view group. */
inline val ViewGroup.size get() = childCount

/** Returns the view at 0 or throws an [IndexOutOfBoundsException] */
@Throws(IndexOutOfBoundsException::class)
inline fun ViewGroup.first(): View = this[0]

/** Returns the view at 0 or null */
inline fun ViewGroup.firstOrNull(): View? = try {
    getChildAt(0)
} catch (e: Exception) {
    null
}

/** Returns true if this view group contains no views. */
inline fun ViewGroup.isEmpty() = childCount == 0

/** Returns true if this view group contains one or more views. */
inline fun ViewGroup.isNotEmpty() = childCount != 0

/** Performs the given action on each view in this view group. */
inline fun ViewGroup.forEach(action: (view: View) -> Unit) {
    for (index in 0 until childCount) {
        action(getChildAt(index))
    }
}

/** Performs the given action on each view in this view group, providing its sequential index. */
inline fun ViewGroup.forEachIndexed(action: (index: Int, view: View) -> Unit) {
    for (index in 0 until childCount) {
        action(index, getChildAt(index))
    }
}

/** Returns a [MutableIterator] over the views in this view group. */
operator fun ViewGroup.iterator() = object : MutableIterator<View> {
    private var index = 0
    override fun hasNext() = index < childCount
    override fun next() = getChildAt(index++) ?: throw IndexOutOfBoundsException()
    override fun remove() = removeViewAt(--index)
}

/** Returns a [Sequence] over the child views in this view group. */
val ViewGroup.children: Sequence<View>
    get() = object : Sequence<View> {
        override fun iterator() = this@children.iterator()
    }

// endregion


