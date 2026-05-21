package org.betterLostItems.salts_animal_farm.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import org.betterLostItems.salts_animal_farm.Salts_animal_farm;
import org.betterLostItems.salts_animal_farm.client.AnimalFarmClientDebug;
import org.betterLostItems.salts_animal_farm.entity.SaltsAnimalFarmConfigLists;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {
    private static final int SICK_GREEN_TINT = 0xFFA6E8A6;

    @Unique
    private LivingEntity salts_animal_farm$renderedEntity;

    @Inject(method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V", at = @At("HEAD"))
    private void salts_animal_farm$captureRenderedEntity(LivingEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight, CallbackInfo ci) {
        salts_animal_farm$renderedEntity = entity;
    }

    @Inject(method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V", at = @At("RETURN"))
    private void salts_animal_farm$clearRenderedEntity(LivingEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight, CallbackInfo ci) {
        salts_animal_farm$renderedEntity = null;
    }

    @ModifyArg(
            method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/EntityModel;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;III)V"),
            index = 4
    )
    private int salts_animal_farm$tintSickFarmAnimals(int color) {
        if (salts_animal_farm$isSickFarmAnimal()) {
            return multiplyArgb(color, SICK_GREEN_TINT);
        }

        return color;
    }

    @Unique
    private boolean salts_animal_farm$isSickFarmAnimal() {
        return Salts_animal_farm.CONFIG.modEnabled()
                && salts_animal_farm$renderedEntity instanceof Animal animal
                && SaltsAnimalFarmConfigLists.isFarmAnimal(animal)
                && AnimalFarmClientDebug.isEntitySick(animal.getId());
    }

    @Unique
    private static int multiplyArgb(int first, int second) {
        int alpha = ((first >>> 24) * (second >>> 24)) / 255;
        int red = (((first >>> 16) & 0xFF) * ((second >>> 16) & 0xFF)) / 255;
        int green = (((first >>> 8) & 0xFF) * ((second >>> 8) & 0xFF)) / 255;
        int blue = ((first & 0xFF) * (second & 0xFF)) / 255;
        return alpha << 24 | red << 16 | green << 8 | blue;
    }
}
