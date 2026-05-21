package org.betterLostItems.salts_animal_farm.mixin;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(LivingEntity.class)
public interface LivingEntitySoundAccessor {
    @Invoker("getHurtSound")
    SoundEvent salts_animal_farm$getHurtSound(DamageSource damageSource);
}
