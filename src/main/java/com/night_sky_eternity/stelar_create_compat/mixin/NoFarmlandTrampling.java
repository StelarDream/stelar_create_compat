package com.night_sky_eternity.stelar_create_compat.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.FarmBlock;

@Mixin(FarmBlock.class)
public class NoFarmlandTrampling {

    @Inject(method = "fallOn", at = @At("HEAD"), cancellable = true)
    private void preventTrampling(Level level, net.minecraft.world.level.block.state.BlockState state,
                                  BlockPos pos, Entity entity, float fallDistance, CallbackInfo ci) {
        ci.cancel();
    }
}
