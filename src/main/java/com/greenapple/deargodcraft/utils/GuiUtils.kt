package com.greenapple.deargodcraft.utils

import com.greenapple.deargodcraft.delegate.reflectField
import net.minecraft.advancements.Advancement
import net.minecraft.client.gui.advancements.AdvancementTabGui
import net.minecraft.client.gui.advancements.AdvancementsScreen
import net.minecraft.client.multiplayer.ClientAdvancementManager

val AdvancementsScreen.tabsKt : Map<Advancement, AdvancementTabGui> by reflectField("field_191947_i", true)
val AdvancementsScreen.clientAdvancementManagerKt : ClientAdvancementManager by reflectField("field_191946_h", true)
val AdvancementsScreen.selectedTabKt : AdvancementTabGui by reflectField("field_191940_s")
val AdvancementsScreen.tabPageKt : Int by reflectField("tabPage") //Not in McpToSrg?