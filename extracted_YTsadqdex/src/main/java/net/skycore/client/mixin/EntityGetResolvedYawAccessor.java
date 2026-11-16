package net.skycore.client.mixin;

import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Entity.class)
public interface EntityGetResolvedYawAccessor {
   @Accessor(value = "skycore$resolvedYaw", remap = false)
   float skycore$getResolvedYaw();

   @Accessor(value = "skycore$resolvedPitch", remap = false)
   float skycore$getResolvedPitch();

   @Accessor(value = "skycore$resolvedYaw", remap = false)
   void skycore$setResolvedYaw(float value);

   @Accessor(value = "skycore$resolvedPitch", remap = false)
   void skycore$setResolvedPitch(float value);
}
