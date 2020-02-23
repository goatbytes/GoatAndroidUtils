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

package io.goatbytes.android.util.content

import android.content.Context
import android.content.pm.PackageItemInfo
import io.goatbytes.android.app

/**
 * Loads application names faster by saving the name to in-memory and disk cache
 */
object ApplicationLabel {

    private object CACHE {

        private const val name = "ApplicationLabelCache"

        private val cache by lazy { mutableMapOf<String, String>() }

        private val store by lazy { app.getSharedPreferences(name, Context.MODE_PRIVATE) }

        operator fun set(key: String, value: String): Unit =
            store.edit().putString(key, value).apply().also { cache[key] = value }

        operator fun get(key: String): String? =
            cache[key] ?: store.getString(key, null)?.apply { cache[key] = this }

    }

    private val cache = CACHE

    private fun <T : Any> tryOrNull(block: () -> T?): T? = try {
        block()
    } catch (e: Exception) {
        null
    }

    /**
     * Retrieve the current textual label associated with the app.
     *
     * @param packageName The package name of the app
     * @return A string containing the application's label.
     */
    operator fun get(packageName: String): String? = packageName.let { key ->
        cache[key] ?: tryOrNull {
            get(
                app.packageManager.getApplicationInfo(
                    key,
                    0
                )
            )
        }
    }

    /**
     * Retrieve the current textual label associated with the app.
     *
     * @param appInfo The [ApplicationInfo]
     * @return A string containing the application's label.
     */
    operator fun get(appInfo: PackageItemInfo): String? = appInfo.packageName.let { key ->
        cache[key] ?: tryOrNull {
            appInfo.loadLabel(app.packageManager).toString().also { value ->
                cache[key] =
                    value
            }
        }
    }

}