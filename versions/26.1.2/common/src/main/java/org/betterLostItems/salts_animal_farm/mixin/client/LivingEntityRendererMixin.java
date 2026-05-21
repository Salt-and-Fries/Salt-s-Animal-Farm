package org.betterLostItems.salts_animal_farm.mixin.client;

import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.util.ARGB;
import org.betterLostItems.salts_animal_farm.client.AnimalFarmClientDebug;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {
    private static final int SICK_GREEN_TINT = 0xFFA6E8A6;

    @Inject(method = "getModelTint", at = @At("RETURN"), cancellable = true)
    private void salts_animal_farm$tintSickFarmAnimals(LivingEntityRenderState state, CallbackInfoReturnable<Integer> cir) {
        if (AnimalFarmClientDebug.isSick(state)) {
            cir.setReturnValue(ARGB.multiply(cir.getReturnValue(), SICK_GREEN_TINT));
        }
    }
}
