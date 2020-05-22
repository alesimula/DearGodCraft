package com.greenapple.deargodcraft.event

import net.minecraftforge.fml.common.Mod
import org.apache.logging.log4j.LogManager

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
object RegistryEvents {
    private val LOGGER = LogManager.getLogger()

    /*@JvmStatic @SubscribeEvent
    fun onFluidTextureRegistry(event: TextureStitchEvent.Pre) = event.registerFluidTextures(Glacia.Fluids)*/
}