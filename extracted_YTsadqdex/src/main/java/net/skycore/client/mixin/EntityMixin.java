package net.skycore.client.mixin;

import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(Entity.class)
public abstract class EntityMixin {
   @Unique
   private float skycore$resolvedYaw;
   @Unique
   private float skycore$resolvedPitch;
}
