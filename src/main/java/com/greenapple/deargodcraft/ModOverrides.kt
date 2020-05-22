package com.greenapple.deargodcraft

import com.greenapple.deargodcraft.delegate.reflectField
import com.greenapple.deargodcraft.utils.*
import net.minecraft.block.Block
import net.minecraft.block.BlockState
import net.minecraft.block.Blocks
import net.minecraft.client.world.ClientWorld
import net.minecraft.entity.passive.AnimalEntity
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.MathHelper
import net.minecraft.util.math.Vec3d
import net.minecraft.util.palette.IPalette
import net.minecraft.util.palette.PalettedContainer
import net.minecraft.world.IBlockReader
import net.minecraft.world.IWorld
import net.minecraft.world.gen.IWorldGenerationBaseReader
import net.minecraft.world.gen.feature.AbstractTreeFeature
import kotlin.math.abs

/** storage **/
private val PalettedContainer<*>.storageKt: net.minecraft.util.BitArray by reflectField("field_186021_b")
/** palette **/
private val <T> PalettedContainer<T>.paletteKt: IPalette<T> by reflectField("field_186022_c")
/** defaultState **/
private val <T> PalettedContainer<T>.defaultStateKt: T by reflectField("field_205526_g")

// <editor-fold defaultstate="collapsed" desc="Hashing methods">
object ModOverrides {
    private var timeSeed = System.currentTimeMillis()
    fun updateTimeSeed() = System.currentTimeMillis().run {
        if (this - timeSeed > 3000) timeSeed = this
    }
    private fun Long.hash(seed: Int) = run {
        var hash = this
        hash = hash.inv() + (hash shl 7)
        hash = (hash + 0x67726563) xor hash.ushr(21)
        hash += ((hash xor 0x6e617070) shl 4) + (hash shl 9)
        hash = (hash + timeSeed) xor hash.ushr(16)
        hash += ((hash xor 0x6c657321) shl 1) + (hash shl 3)
        hash = (hash + seed) xor hash.ushr(24)
        hash += ((hash xor 0x69656e64) shl 40)
        hash
    }
    val Long.hash get() = hash(0x68696672)
    val randomLongTimed get() = 10L.hash
    val randomIntTimed get() = randomLongTimed.toInt()
    private inline fun Block.hashId(seed: Int) = (abs(id.toLong().hash(seed)) % block.registry.entries.size).toInt()
    private inline fun Block.hashBlock(seed: Int) = block.registry.getValue(block.hashId(seed))
    private inline fun BlockState.hashBlock(seed: Int) = block.hashBlock(seed)
    private fun BlockState.isCompatible(other: BlockState) = ((!isSolid || other.isSolid) && (isTransparent || !other.isTransparent)
            && (!material.isOpaque || other.material.isOpaque) && (canProvidePower() || !other.canProvidePower())
            && (runCatching {!isCollisionShapeOpaque(null, null)}.getOrDefault(false) || runCatching {other.isCollisionShapeOpaque(null, null)}.getOrDefault(true))
            && other.block !in voidBlocks)
    val voidBlocks = listOf(Blocks.AIR, Blocks.CAVE_AIR, Blocks.VOID_AIR)
    fun BlockState.hashState(seed: Int) = when (block) {
        Blocks.WATER -> Blocks.LAVA.defaultState
        Blocks.LAVA -> Blocks.WATER.defaultState
        in voidBlocks -> this
        else -> hashBlock(seed).defaultState.let { new ->
            if (isCompatible(new)) new else new.hashBlock(seed).defaultState.let { new ->
                if (isCompatible(new)) new else new.hashBlock(seed).defaultState.let { new ->
                    if (isCompatible(new)) new else new.hashBlock(seed).defaultState.let { new ->
                        if (isCompatible(new)) new else this
                    }
                }
            }
        }
    }
}
// </editor-fold>

val <E> List<E>.timedRandom: E? get() = if (size > 0) get(ModOverrides.randomIntTimed % size) else null

fun ModOverrides.overrideMethods() {
    /** doSwap **/
    PalettedContainer::class.replaceMethodOrFallback("func_222643_a") { (index, state) -> this!!
        if (state !is BlockState) RedefineUtils.fallback()
        index as Int
        this as PalettedContainer<BlockState>
        val i = paletteKt.idFor(state.hashState(this.hashCode()))
        val j = storageKt.swapAt(index, i)
        val t = paletteKt[j]
        (t ?: defaultStateKt)
    }

    /** getSkyColor **/
    ClientWorld::class.replaceMethod("func_228318_a_") { (blockPos, partialTicks) -> this!!
        blockPos as BlockPos
        partialTicks as Float
        val f = getCelestialAngle(partialTicks)
        var f1 = MathHelper.cos(f * (Math.PI.toFloat() * 2f)) * 2.0f + 0.5f
        f1 = MathHelper.clamp(f1, 0.0f, 1.0f)
        val biome = getBiome(blockPos)
        val i = biome.skyColor
        val red = ((i shr 16 and 255).toFloat() / 255.0f)*f1
        val green = ((i shr 8 and 255).toFloat() / 255.0f)*f1
        val blue = ((i and 255).toFloat() / 255.0f)*f1
        val halfR = red/2.0
        val halfG = green/2.0
        val halfB = blue/2.0
        when ((10L.hash % 3).toInt()) {
            0 -> Vec3d(halfR + halfR.toLong().hash % halfR, green - (green.toLong().hash % (green / 3.0)), halfB + halfB.toLong().hash % halfB)
            1 -> Vec3d(red - (red.toLong().hash % (red / 3.0)), halfG + halfG.toLong().hash % halfG, halfB + halfB.toLong().hash % halfB)
            else -> Vec3d(halfR + halfR.toLong().hash % halfR, halfG + halfG.toLong().hash % halfG, blue - (blue.toLong().hash % (blue / 3.0)))
        }
    }

    /** canAnimalSpawn **/
    AnimalEntity::class.replaceMethod("func_223316_b") { (_, world, _, pos, _)->
        (world as IWorld).getBlockState((pos as BlockPos).down()).isNormalCube(world, pos) && world.getLightSubtracted(pos, 0) > 8
    }

    /** isSoilOrFarm **/
    AbstractTreeFeature::class.replaceMethod("isSoilOrFarm") { (reader, pos, sapling) ->
        reader as IWorldGenerationBaseReader
        pos as BlockPos
        sapling as net.minecraftforge.common.IPlantable
        reader.hasBlockState(pos) {state ->
            (state.block !in voidBlocks && runCatching {state.isNormalCube(reader as? IBlockReader, pos)}.getOrDefault(false))
        }
    }

    /** canSustainPlant **/
    Block::class.replaceMethod("canSustainPlant") { (state, world, pos) ->
        state as BlockState
        world as IBlockReader
        pos as BlockPos
        (state.block !in voidBlocks && state.isNormalCube(world, pos))
    }
}