package com.greenapple.deargodcraft.event

import com.greenapple.deargodcraft.DearGodCraft
import com.greenapple.deargodcraft.delegate.reflectField
import com.greenapple.deargodcraft.timedRandom
import com.greenapple.deargodcraft.utils.morph
import com.greenapple.deargodcraft.utils.registry
import net.minecraft.advancements.Advancement
import net.minecraft.advancements.AdvancementRewards
import net.minecraft.client.Minecraft
import net.minecraft.client.entity.player.AbstractClientPlayerEntity
import net.minecraft.client.gui.advancements.AdvancementsScreen
import net.minecraft.client.renderer.entity.BipedRenderer
import net.minecraft.client.renderer.entity.PlayerRenderer
import net.minecraft.entity.EntityType
import net.minecraft.entity.MobEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.util.ResourceLocation
import net.minecraft.world.World
import net.minecraftforge.client.event.GuiScreenEvent
import net.minecraftforge.client.event.RenderHandEvent
import net.minecraftforge.client.event.RenderPlayerEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.client.registry.IRenderFactory
import net.minecraftforge.fml.client.registry.RenderingRegistry
import net.minecraftforge.registries.GameData

class RenderingEvents {
    //private val MODEL_SNOWMAN = ModelPlayerSnowMan(0)
    private val TEXTURE_SNOWMAN = ResourceLocation(DearGodCraft.MODID, "textures/entity/player_snowman.png")

    private val RenderingRegistry.entityRenderersKt: MutableMap<EntityType<*>, IRenderFactory<*>> by reflectField("entityRenderers")
    private val RenderingRegistry?.instanceKt: RenderingRegistry by reflectField("INSTANCE")

    private val World.bipedTextures by lazy {
        val minecraft = Minecraft.getInstance()
        val world by lazy {Minecraft.getInstance().world as World}
        val renderers = minecraft.renderManager.renderers
        EntityType::class.registry.values.mapNotNullTo(mutableListOf()) { type->
            (renderers[type] as? BipedRenderer<MobEntity, *>)?.run {runCatching {getEntityTexture(null)}.getOrElse {
                (type.create(world) as? MobEntity)?.let { tempEntity ->
                    val texture = getEntityTexture(tempEntity)
                    tempEntity.remove()
                    texture
                }
            }}
        }
    }

    private var playerTextureLoaded = false

    private fun PlayerRenderer.onRenderPlayer(player: PlayerEntity) {
        val clientPlayer = player as? AbstractClientPlayerEntity
        morph(player, null, player.world.bipedTextures.apply {if (!playerTextureLoaded && clientPlayer!=null) {playerTextureLoaded = true; add(clientPlayer.locationSkin)}}.timedRandom)
        /*if (!player.isInWater) player.getActivePotionEffect(DearGodCraft.Effects.MORPH_SNOWMAN)?.also { effect ->
            effect.durationKt = 72011
            morph(player, MODEL_SNOWMAN, TEXTURE_SNOWMAN)
        }
        else if (morph(player)) player.removePotionEffect(DearGodCraft.Effects.MORPH_SNOWMAN)*/
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