package net.skycore.client.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.phys.Vec3;
import net.skycore.client.module.combat.ElytraTargetModule;
import net.skycore.client.module.movement.SuperFireworkModule;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(FireworkRocketEntity.class)
public abstract class FireworkRocketEntityCaptureLookMixin {
   @Shadow
   private LivingEntity attachedToEntity;
   @Unique
   private Vec3 skycore$look;
   @Unique
   private int skycore$fireworkAge;

   @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getLookAngle()Lnet/minecraft/world/phys/Vec3;"))
   private Vec3 skycore$captureLook(LivingEntity skycore$look2) {
      this.skycore$look = skycore$look2.getLookAngle();
      return this.skycore$look;
   }

   @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/Vec3;add(DDD)Lnet/minecraft/world/phys/Vec3;", ordinal = 0))
   private Vec3 skycore$elytraBoost(Vec3 position, double value, double value2, double value3) {
      SuperFireworkModule iNSTANCE = SuperFireworkModule.INSTANCE;
      Minecraft instance = Minecraft.getInstance();
      LocalPlayer player = instance.player;
      ElytraTargetModule.INSTANCE.onFirework(this.attachedToEntity);
      if (this.attachedToEntity == player && iNSTANCE.shouldBoost() && this.skycore$look != null) {
         float[] floatValues = iNSTANCE.resolveBoost(this.skycore$fireworkAge++);
         float value4 = floatValues[0];
         float value5 = floatValues[1];
         Vec3 look = this.skycore$look;
         return position.add(
            look.x * 0.1 + (look.x * value4 - position.x) * 0.5,
            look.y * 0.1 + (look.y * value5 - position.y) * 0.5,
            look.z * 0.1 + (look.z * value4 - position.z) * 0.5
         );
      } else {
         this.skycore$fireworkAge = 0;
         return position.add(value, value2, value3);
      }
   }
}
