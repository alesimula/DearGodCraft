package com.greenapple.deargodcraft.utils

import com.greenapple.deargodcraft.delegate.reflectField
import net.minecraft.block.Block
import net.minecraft.block.BlockState
import net.minecraft.potion.EffectInstance
import net.minecraft.state.IProperty

typealias PropertyPair<T>  = Pair<IProperty<T>, Comparable<T>>

//fun Block.overrideLightValue(value: Int? = null) = stateContainer.validStates.forEach {it.lightLevelKt = value ?: getLightValue(it)}
fun Block.overrideLightValue(value: Int? = null, vararg states: PropertyPair<*>) = stateContainer.validStates.forEach {
    if (states.fold(true) { old, (state, value) -> (old || return@fold false) && it.get(state) == value}) it.lightLevelKt = value ?: getLightValue(it)
}
var BlockState.lightLevelKt : Int by reflectField("field_215708_d", true)

//Effect
var EffectInstance.durationKt : Int by reflectField("field_76460_b")