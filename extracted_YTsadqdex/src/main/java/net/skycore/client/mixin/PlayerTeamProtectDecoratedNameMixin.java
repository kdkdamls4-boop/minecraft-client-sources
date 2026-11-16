package net.skycore.client.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.scores.PlayerTeam;
import net.skycore.client.module.misc.StreamerModeModule;
import net.skycore.client.util.ReplaceLiteralHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerTeam.class)
public abstract class PlayerTeamProtectDecoratedNameMixin {
   @ModifyArg(
      method = "getFormattedName(Lnet/minecraft/network/chat/Component;)Lnet/minecraft/network/chat/MutableComponent;",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/network/chat/MutableComponent;append(Lnet/minecraft/network/chat/Component;)Lnet/minecraft/network/chat/MutableComponent;",
         ordinal = 1
      ),
      index = 0
   )
   private Component skycore$protectDecoratedName(Component text) {
      Minecraft instance = Minecraft.getInstance();
      if (instance.player == null) {
         return text;
      }

      String scoreboardName = instance.player.getScoreboardName();
      String customName = StreamerModeModule.getCustomName(scoreboardName);
      return scoreboardName.equals(customName) ? text : ReplaceLiteralHelper.replaceLiteral(text, scoreboardName, customName);
   }

   @Inject(
      method = "getFormattedName(Lnet/minecraft/network/chat/Component;)Lnet/minecraft/network/chat/MutableComponent;",
      at = @At("RETURN"),
      cancellable = true
   )
   private void skycore$maskOwnDonation(Component text, CallbackInfoReturnable<MutableComponent> callbackInfoReturnable) {
      if (Minecraft.getInstance().player != null) {
         MutableComponent returnValue = (MutableComponent)callbackInfoReturnable.getReturnValue();
         String string = StreamerModeModule.maskOwnNametag(returnValue.getString(), text.getString());
         if (!string.equals(returnValue.getString())) {
            callbackInfoReturnable.setReturnValue(Component.empty().append(ReplaceLiteralHelper.replaceLiteral(returnValue, returnValue.getString(), string)));
         }
      }
   }
}
