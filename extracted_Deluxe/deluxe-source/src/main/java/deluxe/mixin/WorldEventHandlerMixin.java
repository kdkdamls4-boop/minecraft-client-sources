package deluxe.mixin;

import deluxe.client.event.EventBus;
import deluxe.client.util.ServerItemType;
import deluxe.client.util.PlayerItemCooldowns;
import deluxe.client.ui.hud.PotionsHud;
import deluxe.client.event.WorldSoundEvent;

import net.minecraft.client.world.WorldEventHandler;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldEventHandler.class)
public class WorldEventHandlerMixin {
   @Inject(method = {"processWorldEvent(ILnet/minecraft/util/math/BlockPos;I)V", "processGlobalEvent(ILnet/minecraft/util/math/BlockPos;I)V"}, at = @At("HEAD"))
   private void firePotionSplash(int local8253_1, BlockPos blockPos, int local8253_3, CallbackInfo callbackInfo) {
      if (local8253_1 == 2002 || local8253_1 == 2007) {
         ServerItemType serverItemType = ServerItemType.method2466(blockPos);
         if (serverItemType == null) {
            serverItemType = ServerItemType.method2465(local8253_3);
         }

         if (serverItemType != null) {
            PotionsHud.method3205(serverItemType, blockPos);
            PlayerItemCooldowns.method2682(serverItemType, blockPos);
         }

         if (EventBus.getInstance().hasListeners(WorldSoundEvent.class)) {
            EventBus.getInstance().post(new WorldSoundEvent(local8253_3, blockPos));
         }
      }
   }
}
