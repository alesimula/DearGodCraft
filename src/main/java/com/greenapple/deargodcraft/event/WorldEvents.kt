package com.greenapple.deargodcraft.event

import com.greenapple.deargodcraft.delegate.reflectConstructor
import com.greenapple.deargodcraft.utils.registry
import net.minecraft.client.Minecraft
import net.minecraft.entity.*
import net.minecraft.util.math.BlockPos
import net.minecraftforge.event.entity.living.LivingSpawnEvent
import net.minecraftforge.eventbus.api.Event
import net.minecraftforge.eventbus.api.SubscribeEvent
import kotlin.random.Random

class WorldEvents {
    companion object {
        val creatures by lazy {arrayOf(EntityClassification.MONSTER, EntityClassification.CREATURE, EntityClassification.WATER_CREATURE).let {creatureClassifications ->
            EntityType::class.registry.values.filter {it.classification in creatureClassifications}
        }}
    }
    /*@SubscribeEvent
    fun onWorldLoad(event: WorldEvent.Load) = event.runClient {
        if (world.dimension.type.id == DearGodCraft.DIMENSION.type.id) {
            Blocks.TORCH.overrideLightValue(0)
            Blocks.WALL_TORCH.overrideLightValue(0)
        }
    }

    @SubscribeEvent
    fun onWorldUnload(event: WorldEvent.Unload) = event.runClient {
        if (world.dimension.type.id == DearGodCraft.DIMENSION.type.id) {
            Blocks.TORCH.overrideLightValue()
            Blocks.WALL_TORCH.overrideLightValue()
        }
    }*/

    tailrec fun List<EntityType<*>>.weightedRandom(): EntityType<*> = when(val type = random()) {
        EntityType.WITHER -> if (Random.nextInt(0, 100) == 1) type else weightedRandom()
        EntityType.ENDERMAN -> if (Random.nextInt(0, 100) == 1) type else weightedRandom()
        EntityType.ELDER_GUARDIAN -> if (Random.nextInt(0, 100) == 1) type else weightedRandom()
        else -> type
    }

    private val newSpawnReason by reflectConstructor<SpawnReason>()
    private val SPAWN_REASON_DEARGOD = newSpawnReason()

    @SubscribeEvent
    fun onEntitySpawn(event: LivingSpawnEvent.SpecialSpawn) = event.takeIf {event.entity is MobEntity && event.spawnReason !== SPAWN_REASON_DEARGOD}?.run {
        val world = kotlin.runCatching {Minecraft.getInstance().world}.getOrNull()
        if (world != null) runCatching {
            creatures.weightedRandom().spawn(event.entity.world, null, null, BlockPos(event.x, event.y, event.z), SPAWN_REASON_DEARGOD, true, false)
        }
        isCanceled = true
        result = Event.Result.DENY
    }
}