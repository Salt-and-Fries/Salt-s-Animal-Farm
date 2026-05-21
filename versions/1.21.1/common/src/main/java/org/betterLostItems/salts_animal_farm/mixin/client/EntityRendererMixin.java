package org.betterLostItems.salts_animal_farm.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Animal;
import org.betterLostItems.salts_animal_farm.Salts_animal_farm;
import org.betterLostItems.salts_animal_farm.client.AnimalFarmClientDebug;
import org.betterLostItems.salts_animal_farm.entity.SaltsAnimalFarmConfigLists;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin<T extends Entity> {
    @Shadow
    protected abstract void renderNameTag(T entity, Component displayName, PoseStack poseStack, MultiBufferSource buffer, int packedLight, float tickDelta);

    @Inject(method = "render", at = @At("TAIL"))
    private void salts_animal_farm$renderDebugLabels(T entity, float entityYaw, float tickDelta, PoseStack poseStack, MultiBufferSource buffer, int packedLight, CallbackInfo ci) {
        if (!Salts_animal_farm.CONFIG.modEnabled()
                || !AnimalFarmClientDebug.shouldRenderDebugFarmData()
                || !(entity instanceof Animal animal)
                || !SaltsAnimalFarmConfigLists.isFarmAnimal(animal)) {
            return;
        }

        List<Component> lines = AnimalFarmClientDebug.getEntityDebugLines(entity.getId());

        for (int i = 0; i < lines.size(); i++) {
            poseStack.pushPose();
            poseStack.translate(0.0D, (lines.size() - 1 - i) * 0.25D, 0.0D);
            renderNameTag(entity, lines.get(i), poseStack, buffer, packedLight, tickDelta);
            poseStack.popPose();
        }
    }
}
