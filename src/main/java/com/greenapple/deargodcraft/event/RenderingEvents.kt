package com.greenapple.deargodcraft.event

import com.greenapple.deargodcraft.DearGodCraft
import net.minecraft.advancements.Advancement
import net.minecraft.advancements.AdvancementRewards
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.advancements.AdvancementsScreen
import net.minecraft.client.renderer.entity.PlayerRenderer
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.util.ResourceLocation
import net.minecraftforge.client.event.GuiScreenEvent
import net.minecraftforge.client.event.RenderHandEvent
import net.minecraftforge.client.event.RenderPlayerEvent
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.registries.GameData

class RenderingEvents {
    //private val MODEL_SNOWMAN = ModelPlayerSnowMan(0)
    private val TEXTURE_SNOWMAN = ResourceLocation(DearGodCraft.MODID, "textures/entity/player_snowman.png")

    private fun PlayerRenderer.onRenderPlayer(player: PlayerEntity) {
        /*if (!player.isInWater) player.getActivePotionEffect(DearGodCraft.Effects.MORPH_SNOWMAN)?.also { effect ->
            effect.durationKt = 72011
            morph(player, MODEL_SNOWMAN, TEXTURE_SNOWMAN)
        }
        else if (morph(player)) player.removePotionEffect(DearGodCraft.Effects.MORPH_SNOWMAN)*/
    }

    @SubscribeEvent
    fun onRenderBlock(event: RenderWorldLastEvent) {
        //DearGodCraft.Blocks.GLACIAL_TREE_LEAVES.renderType
    }

    @SubscribeEvent
    fun onRenderPlayerThirdPerson(event: RenderPlayerEvent.Pre) {
        event.renderer.onRenderPlayer(event.player)
    }

    @SubscribeEvent
    fun onRenderPlayerFirstPerson(event: RenderHandEvent) = Minecraft.getInstance().apply {player?.let {player->
        val renderer = renderManager.getRenderer(player) as PlayerRenderer
        renderer.onRenderPlayer(player)
    }}

    val glaciaAdvancement = Advancement(GameData.checkPrefix("glacia/root", false), null, null, AdvancementRewards.EMPTY, mapOf(), arrayOf())

    @SubscribeEvent
    fun onRenderGui(event: GuiScreenEvent) = (event.gui as? AdvancementsScreen)?.apply {
        /*val glaciaTabGui = tabsKt[glaciaAdvancement]
        if (glaciaTabGui != null && selectedTabKt == glaciaTabGui && this !is GuiGlaciaAdvancementScreen) {
            Minecraft.getInstance().displayGuiScreen(GuiGlaciaAdvancementScreen(glaciaTabGui.screen.clientAdvancementManagerKt))
        }
        else if (glaciaTabGui != null && selectedTabKt != glaciaTabGui && this is GuiGlaciaAdvancementScreen) {
            Minecraft.getInstance().displayGuiScreen(AdvancementsScreen(glaciaTabGui.screen.clientAdvancementManagerKt))
        }*/
    }
}