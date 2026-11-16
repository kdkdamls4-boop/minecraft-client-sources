package net.skycore.client.mixin;

import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerInput;
import net.skycore.client.SkycoreClient;
import net.skycore.client.event.ContainerClickEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MultiPlayerGameMode.class)
public abstract class MultiPlayerGameModeClickSlotMixin {
   @Inject(method = "handleContainerInput", at = @At("HEAD"), cancellable = true)
   private void skycore$clickSlot(int value, int value2, int value3, ContainerInput containerInput, Player player, CallbackInfo callbackInfo) {
      ContainerClickEvent containerClickEvent = new ContainerClickEvent(value, value2, value3, containerInput);
      SkycoreClient.events().post(containerClickEvent);
      if (containerClickEvent.isCancelled()) {
         callbackInfo.cancel();
      }
   }
}
