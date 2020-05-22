/**
 * Delegates that implement extension properties with backing fields
 */
package com.greenapple.deargodcraft.delegate

import com.greenapple.deargodcraft.collections.ConcurrentIdentityHashMap
import com.greenapple.deargodcraft.utils.WrappedClass
import com.greenapple.deargodcraft.utils.wrappedClass
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KProperty

inline fun <This: Any, Return> fieldProperty(identity: Boolean = true, crossinline referenceProvider: (This.()->Any?) = {this}, crossinline initializer: This.()->Return) = object : IDelegateVar<This, Return> {
    private val values : MutableMap<Any?, WrappedClass<Return>> = if (identity) ConcurrentIdentityHashMap() else ConcurrentHashMap()
    private fun This.initialize(property: KProperty<*>) = runCatching {initializer()}.getOrElse {throw ExceptionInInitializerError("${this::class.java.simpleName}: field ${property.name} not initialized").initCause(it)}

    override fun getValue(thisRef: This, property: KProperty<*>): Return = values.computeIfAbsent(referenceProvider(thisRef)) {wrappedClass(thisRef.initialize(property))}.value
    override fun setValue(thisRef: This, property: KProperty<*>, value: Return) {values.compute(referenceProvider(thisRef)) {_, old-> if (old == null || old.value != value) wrappedClass(value) else old}}
}
inline fun <This: Any, Return> lazyProperty(reference: Boolean = true, crossinline referenceProvider: (This.()->Any?) = {this}, crossinline initializer: This.()->Return) = fieldProperty(reference, referenceProvider, initializer) as IDelegateVal<This, Return>
inline fun <This: Any, Return> staticProperty(crossinline initializer: This.()->Return) = object : IDelegateVal<This, Return> {
    private var value: WrappedClass<Return>? = null
    override fun getValue(thisRef: This, property: KProperty<*>): Return = value?.value ?: synchronized(this) {value?.value ?: initializer(thisRef).also {value = wrappedClass(it)}}
}