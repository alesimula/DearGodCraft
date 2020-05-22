package com.greenapple.deargodcraft.event

import net.minecraftforge.event.world.BlockEvent
import net.minecraftforge.eventbus.api.SubscribeEvent

class BlockEvents {
    @SubscribeEvent
    fun onNeighbourUpdate(event: BlockEvent.NeighborNotifyEvent) = event.run {
        /*if (state.block == Blocks.FIRE && world.dimension.type.id == DearGodCraft.DIMENSION.type.id) {
            world.removeBlock(pos, false)
        }*/
    }
}