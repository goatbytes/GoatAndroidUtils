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

package io.goatbytes.android.util.app

import android.app.Activity
import android.app.Application
import android.app.Application.ActivityLifecycleCallbacks
import android.os.Bundle
import io.goatbytes.android.app

/**
 * This is a convenience class that wraps the ActivityLifecycleCallbacks registration.
 * It provides an abstract Callbacks class that reduces required boilerplate code in your callbacks.
 */
class ActivityLifecycleManager(private val application: Application = app) {

    private val callbacks = mutableSetOf<ActivityLifecycleCallbacks>()

    /**
     * Register the activity lifecycle callbacks.
     *
     * @param callback The callbacks
     */
    fun register(callback: ActivityLifecycleCallbacks) {
        application.registerActivityLifecycleCallbacks(callback)
        callbacks.add(callback)
    }

    /**
     * Unregisters all previously registered callbacks on the application context.
     */
    fun reset() {
        for (callback in callbacks) application.unregisterActivityLifecycleCallbacks(callback)
        callbacks.clear()
    }

    /**
     * Override the methods corresponding to the activity.
     */
    interface Callback : ActivityLifecycleCallbacks {
        override fun onActivityCreated(activity: Activity?, bundle: Bundle?) {}
        override fun onActivityStarted(activity: Activity?) {}
        override fun onActivityResumed(activity: Activity?) {}
        override fun onActivityPaused(activity: Activity?) {}
        override fun onActivityStopped(activity: Activity?) {}
        override fun onActivitySaveInstanceState(activity: Activity?, bundle: Bundle?) {}
        override fun onActivityDestroyed(activity: Activity?) {}
    }

}