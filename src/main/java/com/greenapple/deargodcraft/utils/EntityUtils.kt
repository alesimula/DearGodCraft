package com.greenapple.deargodcraft.utils

import com.greenapple.deargodcraft.delegate.reflectField
import net.minecraft.entity.player.ServerPlayerEntity

var ServerPlayerEntity.lastExperienceKt : Int by reflectField("field_71144_ck")
var ServerPlayerEntity.lastHealthKt : Float by reflectField("field_71149_ch")
var ServerPlayerEntity.lastFoodLevelKt : Int by reflectField("field_71146_ci")