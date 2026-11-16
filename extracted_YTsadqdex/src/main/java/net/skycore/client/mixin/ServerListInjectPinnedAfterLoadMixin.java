package net.skycore.client.mixin;

import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerList;
import net.skycore.client.util.ApplyIdentityHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerList.class)
public abstract class ServerListInjectPinnedAfterLoadMixin {
   @Inject(method = "load", at = @At("RETURN"))
   private void skycore$injectPinnedAfterLoad(CallbackInfo callbackInfo) {
      ApplyIdentityHelper.ensurePresent((ServerList)(Object)this);
   }

   @Inject(method = "save", at = @At("HEAD"))
   private void skycore$injectPinnedBeforeSave(CallbackInfo callbackInfo) {
      ApplyIdentityHelper.ensurePresent((ServerList)(Object)this);
   }

   @Inject(method = "add", at = @At("TAIL"))
   private void skycore$dedupePinnedAfterAdd(ServerData serverData, boolean condition, CallbackInfo callbackInfo) {
      if (!condition) {
         ApplyIdentityHelper.ensurePresent((ServerList)(Object)this);
      }
   }

   @Inject(method = "remove", at = @At("HEAD"), cancellable = true)
   private void skycore$blockPinnedRemove(ServerData serverData, CallbackInfo callbackInfo) {
      if (ApplyIdentityHelper.matches(serverData)) {
         callbackInfo.cancel();
      }
   }

   @Inject(method = "swap", at = @At("HEAD"), cancellable = true)
   private void skycore$blockPinnedSwap(int value, int value2, CallbackInfo callbackInfo) {
      ServerList elements = (ServerList)(Object)this;
      if ((value == 0 || value2 == 0) && ApplyIdentityHelper.isFirstPinned(elements)) {
         callbackInfo.cancel();
      }
   }

   @Inject(method = "replace", at = @At("HEAD"), cancellable = true)
   private void skycore$blockPinnedReplace(int value, ServerData serverData, CallbackInfo callbackInfo) {
      ServerList elements = (ServerList)(Object)this;
      if (value == 0 && ApplyIdentityHelper.isFirstPinned(elements)) {
         callbackInfo.cancel();
      } else {
         if (ApplyIdentityHelper.matches(serverData)) {
            ApplyIdentityHelper.applyIdentity(serverData);
         }
      }
   }
}
