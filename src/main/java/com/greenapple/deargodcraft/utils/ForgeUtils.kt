package com.greenapple.deargodcraft.utils

import com.greenapple.deargodcraft.delegate.lazyProperty
import net.minecraft.inventory.IInventory
import net.minecraft.item.ItemStack
import net.minecraft.util.ResourceLocation
import net.minecraft.util.text.TranslationTextComponent
import net.minecraftforge.eventbus.api.Event
import net.minecraftforge.eventbus.api.IEventBus
import net.minecraftforge.fml.loading.FMLEnvironment
import net.minecraftforge.registries.ForgeRegistry
import net.minecraftforge.registries.ForgeRegistryEntry
import net.minecraftforge.registries.RegistryManager

inline fun <E : Event>IEventBus.addListenerKt(crossinline method: (E) -> Any) = addListener<E> { event-> method.invoke(event)}

private val <T : ForgeRegistryEntry<T>, C : Class<T>> C.registry : ForgeRegistry<T> by lazyProperty {RegistryManager.ACTIVE.getRegistry<T>(this) as ForgeRegistry<T>}
val <T : ForgeRegistryEntry<T>> T.registry; get() = this.registryType.registry
val <T : ForgeRegistryEntry<T>> T.id; get() = this.registry.getID(this)

operator fun ResourceLocation.plus(extra: String) = ResourceLocation(namespace, "${path}_$extra")
operator fun ResourceLocation.rem(extra: String) = ResourceLocation(namespace, "$extra/$path")

val TranslationTextComponent?.modKey by lazy {"§5§r§e§e§n§a§7§7§l§e§r"}

val IInventory.iterable get() = object: MutableIterable<ItemStack> {
    override fun iterator() = object : MutableIterator<ItemStack> {
        var index = 0
        override fun hasNext() = index < sizeInventory
        override fun next() = if (hasNext()) getStackInSlot(index++) else throw NoSuchElementException("Iterating inventory over its size")
        override fun remove() {if(index > 0) removeStackFromSlot(index-1) else throw IllegalStateException("You can't delete element before first next() method call")}
    }
}

inline fun <R> runClient(block: () -> R): R? = if (FMLEnvironment.dist.isClient) block() else null
inline fun <R> runServer(block: () -> R): R? = if (FMLEnvironment.dist.isDedicatedServer) block() else null
inline fun <T, R> T.runClient(block: T.() -> R): R? = if (FMLEnvironment.dist.isClient) block() else null
inline fun <T, R> T.runServer(block: T.() -> R): R? = if (FMLEnvironment.dist.isDedicatedServer) block() else null