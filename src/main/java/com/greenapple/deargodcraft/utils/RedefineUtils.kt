package com.greenapple.deargodcraft.utils

import com.ea.agentloader.AgentLoader
import com.greenapple.deargodcraft.delegate.reflectField
import com.sun.nio.zipfs.ZipPath
import cpw.mods.modlauncher.api.INameMappingService
import javassist.*
import javassist.bytecode.AccessFlag
import net.minecraftforge.fml.ModLoadingContext
import net.minecraftforge.fml.common.ObfuscationReflectionHelper
import net.minecraftforge.fml.loading.moddiscovery.ModFileInfo
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.LogManager
import java.io.Serializable
import java.lang.instrument.ClassDefinition
import java.lang.instrument.Instrumentation
import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributes
import java.util.concurrent.atomic.AtomicLong
import kotlin.reflect.KClass

private typealias TMethodNoReturn<O> = TMethod<O, Unit>
private typealias TMethod<O, R> = O.(@ParameterName("args") Array<out Any?>) -> R
private class TPrimitive<T: Any> (clazz: KClass<T>, val ctPrimitive: CtClass): KClass<T> by clazz

/**
 * Utility to redefine methods in a class
 * Uses SRG method names
 */
object RedefineUtils {
    fun interface IMethod<O, R>: (O, Array<out Any?>) -> R, Serializable {
        override fun invoke(receiver: O, vararg args: Any?): R
    }
    val void: KClass<Void> = TPrimitive(Void.TYPE.kotlin, CtClass.voidType)
    val byte: KClass<Byte> = TPrimitive(Byte::class, CtClass.byteType)
    val char: KClass<Char> = TPrimitive(Char::class, CtClass.charType)
    val boolean: KClass<Boolean> = TPrimitive(Boolean::class, CtClass.booleanType)
    val short: KClass<Short> = TPrimitive(Short::class, CtClass.shortType)
    val int: KClass<Int> = TPrimitive(Int::class, CtClass.intType)
    val long: KClass<Long> = TPrimitive(Long::class, CtClass.longType)
    val float: KClass<Float> = TPrimitive(Float::class, CtClass.floatType)
    val double: KClass<Double> = TPrimitive(Double::class, CtClass.doubleType)

    private class RedefineAgent {
        companion object {
            @JvmStatic private lateinit var instrumentation: Instrumentation
            @JvmSynthetic fun getInstrumentation() =
                    Thread.currentThread().contextClassLoader.loadClass(RedefineAgent::class.java.name).getDeclaredField(::instrumentation.name).apply {isAccessible = true}[null] as Instrumentation
            @JvmStatic fun agentmain(agentArgs: String?, instr: Instrumentation) {
                instrumentation = instr
            }
        }
    }
    private var ClassLoader?.sclKt : ClassLoader by reflectField("scl")
    private var ClassLoader?.sysPathsKt : Array<String>? by reflectField("sys_paths")

    private fun ZipPath.extractTo(target: Path) = this.toAbsolutePath().apply {Files.walkFileTree(this, object : SimpleFileVisitor<Path>() {
        override fun preVisitDirectory(dir: Path, attrs: BasicFileAttributes) = FileVisitResult.CONTINUE.also {
            Files.createDirectories(target.resolve(relativize(dir).toString()))
        }
        override fun visitFile(file: Path, attrs: BasicFileAttributes) = FileVisitResult.CONTINUE.also {
            Files.copy(file, target.resolve(relativize(file).toString()), StandardCopyOption.REPLACE_EXISTING)
        }
    })}

    private val ARCHITECTURE = System.getProperty("os.arch").run {when(this) {
        "powerpc64", "ppc64" -> if (System.getProperty("sun.cpu.endian") == "little") "ppc64le" else "ppc64"
        "x86", "i486", "i586", "i686" -> "i386"
        "x86_64" -> "amd64"
        "aarch32" -> "arm"
        "arm64" -> "aarch64"
        "powerpc" -> "ppc"
        else -> when {
            matches("armv[1-7](\\D|$).*") -> "arm"
            matches("armv\\d.*") -> "aarch64"
            else -> this
        }
    }}

    @Throws(SecurityException::class)
    private fun withLibPath(path: String, block: ()->Unit) = System.getProperty("java.library.path").let { oldLibPath ->
        System.setProperty("java.library.path", if (oldLibPath?.isNotEmpty() == true) "$path${System.getProperty("path.separator")}$oldLibPath" else path)
        (null as ClassLoader?).sysPathsKt = null
        block.invoke()
        System.setProperty("java.library.path", oldLibPath ?: "")
        (null as ClassLoader?).sysPathsKt = null
    }

    @JvmStatic val INSTRUMENTATION: Instrumentation by lazy {
        val libDir = (ModLoadingContext.get().activeContainer.modInfo.owningFile as ModFileInfo).file.run {locator.findPath(this, "natives\\$ARCHITECTURE")}
        val tempLibDir = createTempDir()
        (libDir as? ZipPath)?.extractTo(tempLibDir.toPath()) ?: libDir.toFile().copyRecursively(tempLibDir, true)
        withLibPath(tempLibDir.path) { ClassLoader.getSystemClassLoader().let { defaultClassLoader ->
            (null as ClassLoader?).sclKt = Thread.currentThread().contextClassLoader
            AgentLoader.loadAgentClass(RedefineAgent::class.java.name, null)
            (null as ClassLoader?).sclKt = defaultClassLoader
        }}
        RedefineAgent.getInstrumentation()
    }

    class FallBackException : Exception()
    fun fallback(): Nothing = throw FallBackException()
}

private val ATOMIC_INDEX = AtomicLong(0)
private val LOGGER by lazy {LogManager.getLogger(RedefineUtils::class.java)}
private val CLASS_POOL by lazy {ClassPool.getDefault().apply {appendClassPath(LoaderClassPath(Thread.currentThread().contextClassLoader))}}
private val KClass<*>.ctClass: CtClass? get() = (this as? TPrimitive)?.ctPrimitive ?: CLASS_POOL[javaObjectType.name]

private inline fun <O : Any> KClass<O>.editClassDef(block: CtClass.(@ParameterName("instrumentation") Instrumentation) -> Unit) = try {
    ctClass?.apply {
        defrost()
        block(this, RedefineUtils.INSTRUMENTATION)
        RedefineUtils.INSTRUMENTATION.redefineClasses(ClassDefinition(this@editClassDef.java, this.toBytecode()))
    } ?: LOGGER.log(Level.ERROR, "Class ${this.java.name} not found")
} catch (ex: Exception) {LOGGER.log(Level.ERROR, "Failed redefining class ${this.java.name}", ex)}

private inline fun <O : Any> KClass<O>.editMethodDef(methodName: String, vararg args: KClass<*>, block: CtMethod.(@ParameterName("instrumentation") Instrumentation) -> Unit) = editClassDef { instrumentation->
    if (args.isEmpty()) block(getDeclaredMethod(ObfuscationReflectionHelper.remapName(INameMappingService.Domain.METHOD, methodName)), instrumentation)
    else block(getDeclaredMethod(ObfuscationReflectionHelper.remapName(INameMappingService.Domain.METHOD, methodName), *args.map {it.ctClass}.toTypedArray()), instrumentation)
}

private fun <O : Any> CtMethod.newStaticMethodCall(function: TMethod<O?, Any?>, shouldReturn: Boolean = false) = StringBuffer().apply {
    val extrasClass = "__GreenappleRedefineAgent__\$extras_${ATOMIC_INDEX.getAndIncrement()}"
    CLASS_POOL.makeClass(extrasClass).apply {
        addField(CtField.make("public static ${RedefineUtils.IMethod::class.java.name} __callable__ = null;\n", this))
        toClass().getDeclaredField("__callable__").set(null, RedefineUtils.IMethod(function))
    }
    val isStatic = ((modifiers and AccessFlag.STATIC) == AccessFlag.STATIC)
    append("{\nObject __returnValue__ = $extrasClass.__callable__.invoke(${if (isStatic) "null" else "$0"}, \$args);\n")
    if (shouldReturn && returnType.name != "void")
        append("return ((${(returnType as? CtPrimitiveType)?.wrapperName ?: returnType.name})(__returnValue__))${if (returnType.isPrimitive) ".${returnType.name}Value()" else ""};\n")
    else if (shouldReturn) append("return;\n")
    append("}")
}.toString()

private fun String.wrapTryOrFallback() = "try $this catch (${RedefineUtils.FallBackException::class.java.name} __fallbackException__) {}"


/**
 * Appends a function at end of a method
 */
fun <O : Any> KClass<O>.addMethodAfter(methodName: String, vararg args: KClass<*>, function: TMethodNoReturn<O?>) = editMethodDef(methodName, *args) {
    insertAfter(newStaticMethodCall(function))
}

/**
 * Appends a function at the start of a method
 */
fun <O : Any> KClass<O>.addMethodBefore(methodName: String, vararg args: KClass<*>, function: TMethodNoReturn<O?>) = editMethodDef(methodName, *args) {
    insertBefore(newStaticMethodCall(function))
}

/**
 * Replaces a method
 * Must call RedefineUtils.fallback() to revert to default behaviour
 */
fun <O : Any> KClass<O>.replaceMethodOrFallback(methodName: String, vararg args: KClass<*>, function: TMethod<O?, Any?>) = editMethodDef(methodName, *args) {
    insertBefore(newStaticMethodCall(function, true).wrapTryOrFallback())
}

/**
 * Replaces a method
 */
fun <O : Any> KClass<O>.replaceMethod(methodName: String, vararg args: KClass<*>, function: TMethod<O?, Any?>) = editMethodDef(methodName, *args) {
    setBody(newStaticMethodCall(function, true))
}