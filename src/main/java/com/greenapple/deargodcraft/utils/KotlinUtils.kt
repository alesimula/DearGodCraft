package com.greenapple.deargodcraft.utils

import com.google.gson.Gson
import org.intellij.lang.annotations.Language
import java.io.*
import java.util.*
import kotlin.reflect.KClass

interface WrappedClass<O> {val value: O}
private inline class WrappedClassImpl<O>(private val wrapped: Any?): WrappedClass<O> {override val value: O get() = wrapped as O}
fun <O: Any?> wrappedClass(value: O): WrappedClass<O> = WrappedClassImpl(value)

infix fun KClass<*>.extends(klass: KClass<*>) = klass.java.isAssignableFrom(java)

inline fun <reified O: Any> O.deepClone(): O = Gson().let { gson-> gson.fromJson<O>(gson.toJson(this, O::class.java), O::class.java)}
inline fun <reified O: Any, reified R: O> O.deepCloneTo(klass: KClass<R>): R = Gson().let { gson-> gson.fromJson<R>(gson.toJson(this, O::class.java), R::class.java)}

/** Read the object from Base64 string. */
@Throws(IOException::class, ClassNotFoundException::class)
fun <O: Serializable> KClass<O>.decodeFromString(string: String): O = ObjectInputStream(ByteArrayInputStream(Base64.getDecoder().decode(string))).use {
    it.readObject() as O
}

infix fun CharSequence.matches(@Language("regexp") regex: String): Boolean = this matches Regex(regex)

/** Write the object to a Base64 string. */
@Throws(IOException::class)
fun Serializable.encodeToString(): String = ByteArrayOutputStream().use {byteStream->
    ObjectOutputStream(byteStream).use {it.writeObject(this)}
    Base64.getEncoder().encodeToString(byteStream.toByteArray())
}