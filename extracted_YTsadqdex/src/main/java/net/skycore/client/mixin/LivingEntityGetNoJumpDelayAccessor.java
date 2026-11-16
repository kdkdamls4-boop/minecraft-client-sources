package net.skycore.client.mixin;

import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LivingEntity.class)
public interface LivingEntityGetNoJumpDelayAccessor {
   @Accessor("noJumpDelay")
   int skycore$getNoJumpDelay();

   @Accessor("jumping")
   boolean skycore$isJumping();

   @Accessor("noJumpDelay")
   void skycore$setNoJumpDelay(int value);
}
