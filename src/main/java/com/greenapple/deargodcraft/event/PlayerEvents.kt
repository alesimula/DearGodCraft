package com.greenapple.deargodcraft.event

import com.greenapple.deargodcraft.ModOverrides
import net.minecraftforge.event.TickEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.event.TickEvent.PlayerTickEvent
import net.minecraftforge.eventbus.api.EventPriority

class PlayerEvents {

    @SubscribeEvent(priority = EventPriority.NORMAL, receiveCanceled = true)
    fun onPlayerTickEvent(event: PlayerTickEvent) = event.player.takeIf {event.phase === TickEvent.Phase.START && it.world.isRemote}?.apply {
        ModOverrides.updateTimeSeed()
    }
}