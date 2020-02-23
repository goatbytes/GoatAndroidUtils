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

package io.goatbytes.android.delegates

import android.os.Bundle
import androidx.fragment.app.Fragment
import io.goatbytes.android.extension.os.put
import io.goatbytes.android.extension.os.set
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

@Suppress("unused")
fun <T : Any> Fragment.arg(): ReadWriteProperty<Fragment, T> = FragmentArgumentDelegate()

@Suppress("unused")
fun <T : Any> Fragment.nullableArg(): ReadWriteProperty<Fragment, T?> =
    FragmentNullableArgumentDelegate()

class FragmentArgumentDelegate<T : Any> : ReadWriteProperty<Fragment, T> {

    @Suppress("UNCHECKED_CAST")
    override fun getValue(thisRef: Fragment, property: KProperty<*>): T {
        return thisRef.requireArguments()[property.name] as T
    }

    override fun setValue(thisRef: Fragment, property: KProperty<*>, value: T) {
        (thisRef.arguments ?: Bundle().also(thisRef::setArguments))[property.name] = value
    }

}

class FragmentNullableArgumentDelegate<T : Any?> : ReadWriteProperty<Fragment, T?> {

    @Suppress("UNCHECKED_CAST")
    override fun getValue(thisRef: Fragment, property: KProperty<*>): T? {
        return thisRef.arguments?.get(property.name) as? T
    }

    override fun setValue(thisRef: Fragment, property: KProperty<*>, value: T?) {
        val args = thisRef.arguments ?: Bundle().also(thisRef::setArguments)
        val key = property.name
        value?.let { args.put(key, it) } ?: args.remove(key)
    }

}
