package net.skycore.client.mixin;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;
import net.skycore.client.module.movement.ElytraMotionModule;
import net.skycore.client.util.SynchronizeHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(LocalPlayer.class)
public abstract class LocalPlayerElytraMotionMixin {
   @ModifyVariable(method = "move", at = @At("HEAD"), argsOnly = true, ordinal = 0)
   private Vec3 skycore$elytraMotion(Vec3 position) {
      return !SynchronizeHelper.isActive() ? position : ElytraMotionModule.INSTANCE.onMove(position);
   }
}
