package net.skycore.client.mixin;

import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LocalPlayer.class)
public interface LocalPlayerSetWasSprintingAccessor {
   @Accessor("wasSprinting")
   void skycore$setWasSprinting(boolean condition);

   @Accessor("wasSprinting")
   boolean skycore$getWasSprinting();
}
