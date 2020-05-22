package com.greenapple.deargodcraft.delegate

import com.greenapple.deargodcraft.utils.decodeFromString
import com.greenapple.deargodcraft.utils.extends
import net.minecraftforge.fml.common.ObfuscationReflectionHelper
import sun.reflect.ConstructorAccessor
import java.io.Serializable
import java.lang.reflect.Constructor
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import java.util.concurrent.atomic.AtomicInteger
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

var Field.modifiersKt : Int by reflectField("modifiers")
private val Constructor<*>.constructorAccessorKt : ConstructorAccessor? by reflectField("constructorAccessor")
private val constructorAccessorRetriever by lazy {Constructor::class.java.getDeclaredMethod("acquireConstructorAccessor").apply {isAccessible = true}}

fun interface IFunction<O>: (Array<out Any?>) -> O, Serializable {
    override operator fun invoke(vararg args: Any?): O
    companion object {
        @JvmStatic fun <O> readFromString(string: String) = IFunction::class.decodeFromString(string) as IFunction<O>
    }
}
typealias IConstructor<O> = IFunction<O>

private class ReflectEnumConstructor<This: Enum<*>> (thisRef: KClass<This>, vararg args: KClass<*>) : IDelegateVal<Any?, IConstructor<This>> {
    private val id = AtomicInteger()
    private val constructor = IConstructor {args-> constructorAccessor.newInstance(arrayOf("${this::class.qualifiedName}_${id.getAndIncrement()}", -1, *args)) as This}
    private val constructorAccessor by lazy {thisRef.java.run {
        (if (args.isEmpty()) runCatching {getDeclaredConstructor(String::class.java, Int::class.java)}.getOrElse {declaredConstructors[0]} else getDeclaredConstructor(String::class.java, Int::class.java, *args.map {it.java}.toTypedArray())).run {
            isAccessible = true
            (constructorAccessorKt ?: constructorAccessorRetriever(this)) as ConstructorAccessor
        }
    }}

    override operator fun getValue(thisRef:Any?, property:KProperty<*>) = constructor
}
private class ReflectConstructor<This: Enum<*>> (thisRef: KClass<This>, vararg args: KClass<*>) : IDelegateVal<Any?, IConstructor<This>> {
    private val constructor = IConstructor {args-> constructorAccessor.newInstance(*args) as This}
    private val constructorAccessor by lazy {thisRef.java.run {
        (if (args.isEmpty()) runCatching {getDeclaredConstructor()}.getOrElse {declaredConstructors[0]} else getDeclaredConstructor(*args.map {it.java}.toTypedArray())).apply {
            isAccessible = true
        }
    }}

    override operator fun getValue(thisRef:Any?, property:KProperty<*>) = constructor
}

@Suppress("NON_PUBLIC_CALL_FROM_PUBLIC_INLINE")
inline fun <reified This: Enum<*>>reflectConstructor(vararg args: KClass<*>): IDelegateVal<Any?, IConstructor<This>> = reflectConstructor(This::class, *args)
private fun <This: Enum<*>>reflectConstructor(klass: KClass<This>, vararg args: KClass<*>): IDelegateVal<Any?, IConstructor<This>> = if (klass extends Enum::class) ReflectEnumConstructor(klass, *args) else ReflectConstructor(klass, *args)
inline fun <reified This: Any,Return> reflectField(name: String, isFinal: Boolean=false) = object : IDelegateVar<This?, Return> {
    private val field by lazy {ObfuscationReflectionHelper.findField(This::class.java, name).apply {if (isFinal) modifiersKt = modifiers and Modifier.FINAL.inv()}}

    @Suppress("UNCHECKED_CAST")
    override operator fun getValue(thisRef:This?,property:KProperty<*>) = field.get(thisRef) as Return
    override operator fun setValue(thisRef:This?, property: KProperty<*>, value: Return) = field.set(thisRef, value)
}