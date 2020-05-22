package com.greenapple.deargodcraft.utils

import com.greenapple.deargodcraft.delegate.reflectField
import com.greenapple.deargodcraft.delegate.staticProperty
import com.mojang.authlib.minecraft.MinecraftProfileTexture
import com.mojang.blaze3d.matrix.MatrixStack
import com.mojang.blaze3d.vertex.IVertexBuilder
import net.minecraft.client.entity.player.AbstractClientPlayerEntity
import net.minecraft.client.network.play.NetworkPlayerInfo
import net.minecraft.client.renderer.entity.LivingRenderer
import net.minecraft.client.renderer.entity.PlayerRenderer
import net.minecraft.client.renderer.entity.layers.BipedArmorLayer
import net.minecraft.client.renderer.entity.layers.LayerRenderer
import net.minecraft.client.renderer.entity.model.BipedModel
import net.minecraft.client.renderer.entity.model.EntityModel
import net.minecraft.client.renderer.entity.model.PlayerModel
import net.minecraft.entity.Entity
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.util.ResourceLocation

private var <T: LivingEntity, M: EntityModel<T>> LivingRenderer<T, M>.entityModelKt : M by reflectField("field_77045_g")
private var <T: LivingEntity, M: EntityModel<T>> LivingRenderer<T, M>.layerRenderersKt : MutableList<LayerRenderer<T, M>> by reflectField("field_177097_h")
private var AbstractClientPlayerEntity.playerInfoKt : NetworkPlayerInfo by reflectField("field_175157_a")
private var NetworkPlayerInfo.playerTexturesKt : MutableMap<MinecraftProfileTexture.Type, ResourceLocation> by reflectField("field_187107_a")
private var NetworkPlayerInfo.playerTexturesLoadedKt : Boolean by reflectField("field_178864_d")

private fun <T: Entity, M: EntityModel<T>> LayerRenderer<*, *>.autoCast() = this as LayerRenderer<T, M>
private val BipedArmorLayer<*, *, *>.LAYER_ARMOR_DEFAULT : BipedArmorLayer<*,*,*> by staticProperty {this}
private val PlayerRenderer.MODEL_PLAYER_DEFAULT : PlayerModel<AbstractClientPlayerEntity> by staticProperty {entityModel}
private val MODEL_ARMOR_EMPTY = object : BipedModel<AbstractClientPlayerEntity>(1f) {
    override fun setRotationAngles(entityIn: AbstractClientPlayerEntity, limbSwing: Float, limbSwingAmount: Float, ageInTicks: Float, netHeadYaw: Float, headPitch: Float) {}
    override fun render(stack: MatrixStack, vertexBuilder: IVertexBuilder, p_225598_3_: Int, p_225598_4_: Int, p_225598_5_: Float, p_225598_6_: Float, p_225598_7_: Float, p_225598_8_: Float) {}
    //override fun postRenderArm(scale: Float, side: HandSide) {}
}

/**
 * Morphs a player with given model and texture
 * Not passing model and texture will morph the player to its original form
 * @return true if the model changed, false otherwise
 */
fun PlayerRenderer.morph(player: PlayerEntity, model: PlayerModel<AbstractClientPlayerEntity>?=null, texture: ResourceLocation?=null) = (model?:MODEL_PLAYER_DEFAULT).let { newModel ->
    MODEL_PLAYER_DEFAULT
    if (texture != null) (player as? AbstractClientPlayerEntity)?.playerInfoKt?.playerTexturesKt?.apply {
        this[MinecraftProfileTexture.Type.SKIN] = texture
    }
    if (entityModel !== newModel) {
        if (texture == null) (player as? AbstractClientPlayerEntity)?.playerInfoKt?.playerTexturesKt?.apply {
            this.remove(MinecraftProfileTexture.Type.SKIN)
            player.playerInfoKt.playerTexturesLoadedKt = false
            this[MinecraftProfileTexture.Type.SKIN] = player.locationSkin
        }
        layerRenderersKt.let {renderers-> renderers.forEachIndexed {index, layerRenderer ->
            (layerRenderer as? BipedArmorLayer<*, *, *>)?.apply {
                LAYER_ARMOR_DEFAULT
                //if (model != null) renderers[index] = BipedArmorLayer(renderer, modelPlayerSnowManArmor, modelPlayerSnowManArmor2)
                if (model != null) renderers[index] = BipedArmorLayer(this@morph, MODEL_ARMOR_EMPTY, MODEL_ARMOR_EMPTY)
                else renderers[index] = LAYER_ARMOR_DEFAULT.autoCast()
                return@forEachIndexed
            }
        }}
        entityModelKt = newModel
        true
    }
    else false
}