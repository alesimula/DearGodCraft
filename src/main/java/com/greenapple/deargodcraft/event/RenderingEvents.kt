package com.greenapple.deargodcraft.event

import com.greenapple.deargodcraft.DearGodCraft
import com.greenapple.deargodcraft.delegate.reflectField
import com.greenapple.deargodcraft.timedRandom
import com.greenapple.deargodcraft.utils.morph
import com.greenapple.deargodcraft.utils.registry
import com.mojang.blaze3d.matrix.MatrixStack
import com.mojang.blaze3d.vertex.IVertexBuilder
import net.minecraft.advancements.Advancement
import net.minecraft.advancements.AdvancementRewards
import net.minecraft.client.Minecraft
import net.minecraft.client.entity.player.AbstractClientPlayerEntity
import net.minecraft.client.gui.advancements.AdvancementsScreen
import net.minecraft.client.renderer.entity.BipedRenderer
import net.minecraft.client.renderer.entity.LivingRenderer
import net.minecraft.client.renderer.entity.MobRenderer
import net.minecraft.client.renderer.entity.PlayerRenderer
import net.minecraft.client.renderer.entity.model.BipedModel
import net.minecraft.client.renderer.entity.model.EntityModel
import net.minecraft.client.renderer.entity.model.PlayerModel
import net.minecraft.client.renderer.model.ModelRenderer
import net.minecraft.entity.EntityType
import net.minecraft.entity.LivingEntity
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
import java.lang.reflect.Field

private val playerModelRenderers: List<Field> by lazy {PlayerModel::class.java.declaredFields.filter {property -> property.isAccessible = true; ModelRenderer::class.java.isAssignableFrom(property.type)}}
private val bipedModelRenderers: List<Field> by lazy {BipedModel::class.java.declaredFields.filter {property -> property.isAccessible = true; ModelRenderer::class.java.isAssignableFrom(property.type)}}
private fun PlayerModel<*>.hidePlayerRenderers() = playerModelRenderers.forEach {(it.get(this) as ModelRenderer).showModel = false}
private fun BipedModel<*>.hideBipedRenderers() = bipedModelRenderers.forEach {(it.get(this) as ModelRenderer).showModel = false}

class RenderingEvents {
    //private val MODEL_SNOWMAN = ModelPlayerSnowMan(0)
    private val TEXTURE_SNOWMAN = ResourceLocation(DearGodCraft.MODID, "textures/entity/player_snowman.png")

    private val RenderingRegistry.entityRenderersKt: MutableMap<EntityType<*>, IRenderFactory<*>> by reflectField("entityRenderers")
    private val RenderingRegistry?.instanceKt: RenderingRegistry by reflectField("INSTANCE")

    private val World.bipedTextures: MutableList<Pair<PlayerModel<*>, ResourceLocation>> by lazy {
        val minecraft = Minecraft.getInstance()
        val world by lazy {Minecraft.getInstance().world as World}
        val renderers = minecraft.renderManager.renderers
        WorldEvents.creatures.mapNotNullTo(mutableListOf()) { type->
            (renderers[type] as? MobRenderer<MobEntity, *>)?.run {runCatching {null!!}.getOrElse {
                (type.create(world) as? MobEntity)?.let {tempEntity ->
                    val texture = getEntityTexture(tempEntity)
                    tempEntity.remove()
                    Pair(WrappingPlayerModel(entityModel, tempEntity), texture)
                }
            }}
        }
    }

    class WrappingPlayerModel<E: LivingEntity>(val wrappedModel: EntityModel<E>, val mockEntity: E): PlayerModel<E>(0F, false) {
        init {
            hidePlayerRenderers()
            hideBipedRenderers()
            /*if (wrappedModel is BipedModel<*>) wrappedModel.hideBipedRenderers()
            else hideBipedRenderers()*/
        }

        override fun render(matrixStackIn: MatrixStack, bufferIn: IVertexBuilder, packedLightIn: Int, packedOverlayIn: Int, red: Float, green: Float, blue: Float, alpha: Float) {
            wrappedModel.render(matrixStackIn, bufferIn, packedLightIn, packedOverlayIn, red, green, blue, alpha)
        }
        override fun setLivingAnimations(entity: E, limbSwing: Float, limbSwingAmount: Float, partialTick: Float) {
            wrappedModel.setLivingAnimations(mockEntity, limbSwing, limbSwingAmount, partialTick)
        }
        override fun setRotationAngles(entity: E, limbSwing: Float, limbSwingAmount: Float, ageInTicks: Float, netHeadYaw: Float, headPitch: Float) {
            wrappedModel.setRotationAngles(mockEntity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch)
        }
    }

    private var playerTextureLoaded = false

    private fun PlayerRenderer.onRenderPlayer(player: PlayerEntity) {
        val clientPlayer = player as? AbstractClientPlayerEntity
        val newModelPairList = player.world.bipedTextures.apply {if (!playerTextureLoaded && clientPlayer!=null) {playerTextureLoaded = true; add(Pair(this@onRenderPlayer.entityModel, clientPlayer.locationSkin))}}
        val random = newModelPairList.timedRandom
        morph(player, random?.first as PlayerModel<AbstractClientPlayerEntity>?, random?.second)
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