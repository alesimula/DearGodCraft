@file:Suppress("UNUSED_PARAMETER")
package com.greenapple.deargodcraft

import com.greenapple.deargodcraft.event.BlockEvents
import com.greenapple.deargodcraft.event.PlayerEvents
import com.greenapple.deargodcraft.event.RenderingEvents
import com.greenapple.deargodcraft.event.WorldEvents
import com.greenapple.deargodcraft.utils.*
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent
import net.minecraftforge.fml.event.lifecycle.InterModEnqueueEvent
import net.minecraftforge.fml.event.lifecycle.InterModProcessEvent
import net.minecraftforge.fml.event.server.FMLServerStartingEvent
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext
import org.apache.logging.log4j.LogManager

@Mod(DearGodCraft.MODID)
class DearGodCraft {

    companion object {
        const val MODID = "greenapple_deargodcraft"
        @JvmStatic private val LOGGER = LogManager.getLogger()
    }

    init {
        ModOverrides.overrideMethods()
        // Listeners
        FMLJavaModLoadingContext.get().modEventBus.let { bus ->
            // Register the setup method for modloading
            bus.addListenerKt(::setup)
            // Register the enqueueIMC method for modloading
            bus.addListenerKt(::enqueueIMC)
            // Register the processIMC method for modloading
            bus.addListenerKt(::processIMC)
            // Register the doClientStuff method for modloading
            bus.addListenerKt(::doClientStuff)
        }
        // Common events
        MinecraftForge.EVENT_BUS.run {
            register(this@DearGodCraft)
            register(PlayerEvents())
            register(WorldEvents())
            register(BlockEvents())
        }
        // Client side events
        MinecraftForge.EVENT_BUS.runClient {
            register(RenderingEvents())
        }
    }

    private fun setup(event: FMLCommonSetupEvent) {
    }

    private fun doClientStuff(event: FMLClientSetupEvent) {
    }

    private fun enqueueIMC(event: InterModEnqueueEvent) {
    }

    private fun processIMC(event: InterModProcessEvent) {
    }

    @SubscribeEvent
    fun onServerStarting(event: FMLServerStartingEvent) {
    }
}
