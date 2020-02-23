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
import android.os.Bundle
import io.goatbytes.android.app
import java.lang.ref.WeakReference

/**
 * Monitors the current activity using [ActivityLifecycleManager.Callback].
 */
object ActivityMonitor {

    private var activity: WeakReference<Activity>? = null

    lateinit var manager: ActivityLifecycleManager

    var currentActivity: Activity?
        /**
         * @return The current foreground Activity if available, may be null.
         */
        get() = activity?.get()
        /**
         * @param value The current [Activity]
         */
        private set(value) {
            activity = if (value != null) WeakReference(value) else null
        }

    fun requireActivity(): Activity = currentActivity
        ?: throw IllegalStateException("Error getting the current activity. Did you register the ActivityMonitor?")

    /**
     * Register the [ActivityMonitor] instance.
     * You should only call register once per application.
     *
     * @param application The current application.
     */
    fun register(application: Application = app) {
        manager = ActivityLifecycleManager(application).apply {
            register(object : ActivityLifecycleManager.Callback {
                override fun onActivityCreated(activity: Activity?, bundle: Bundle?) {
                    currentActivity = activity
                }

                override fun onActivityStarted(activity: Activity?) {
                    currentActivity = activity
                }

                override fun onActivityResumed(activity: Activity?) {
                    currentActivity = activity
                }
            })
        }
    }

}