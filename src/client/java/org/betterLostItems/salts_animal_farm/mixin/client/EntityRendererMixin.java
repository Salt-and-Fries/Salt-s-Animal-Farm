package org.betterLostItems.salts_animal_farm.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityAttachment;
import net.minecraft.world.entity.animal.Animal;
import org.betterLostItems.salts_animal_farm.client.AnimalFarmClientDebug;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin<T extends Entity, S extends EntityRenderState> {
    @Shadow
    protected abstract <RS extends EntityRenderState> void submitNameDisplay(RS state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState cameraRenderState, int yOffset);

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void salts_animal_farm$showFarmAnimalDebugData(T entity, S state, float tickDelta, CallbackInfo ci) {
        if (!AnimalFarmClientDebug.shouldRenderDebugFarmData()
                || !(entity instanceof Animal)) {
            AnimalFarmClientDebug.clearDebugLines(state);
            return;
        }

        List<Component> lines = AnimalFarmClientDebug.getEntityDebugLines(entity.getId());

        if (lines.isEmpty()) {
            AnimalFarmClientDebug.clearDebugLines(state);
            return;
        }

        AnimalFarmClientDebug.setDebugLines(state, lines);
        state.nameTag = null;

        if (state.nameTagAttachment == null) {
            state.nameTagAttachment = entity.getAttachments().getNullable(EntityAttachment.NAME_TAG, 0, entity.getYRot(tickDelta));
        }
    }

    @Inject(
            method = "submitNameDisplay(Lnet/minecraft/client/renderer/entity/state/EntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void salts_animal_farm$submitStackedFarmDebugLines(S state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState cameraRenderState, CallbackInfo ci) {
        List<Component> lines = AnimalFarmClientDebug.getDebugLines(state);

        if (lines.isEmpty()) {
            return;
        }

        Component originalNameTag = state.nameTag;
        Component originalScoreText = state.scoreText;
        state.scoreText = null;

        for (int i = 0; i < lines.size(); i++) {
            state.nameTag = lines.get(i);
            submitNameDisplay(state, poseStack, submitNodeCollector, cameraRenderState, -(lines.size() - 1 - i) * 10);
        }

        state.nameTag = originalNameTag;
        state.scoreText = originalScoreText;
        ci.cancel();
    }

}
