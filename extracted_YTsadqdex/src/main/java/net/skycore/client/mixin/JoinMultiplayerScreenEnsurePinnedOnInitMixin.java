package net.skycore.client.mixin;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.multiplayer.ServerSelectionList;
import net.minecraft.client.gui.screens.multiplayer.ServerSelectionList.Entry;
import net.minecraft.client.gui.screens.multiplayer.ServerSelectionList.OnlineServerEntry;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerList;
import net.skycore.client.util.ApplyIdentityHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(JoinMultiplayerScreen.class)
public abstract class JoinMultiplayerScreenEnsurePinnedOnInitMixin {
   @Shadow
   protected ServerSelectionList serverSelectionList;
   @Shadow
   private Button editButton;
   @Shadow
   private Button deleteButton;
   @Shadow
   private ServerList servers;

   @Inject(method = "init", at = @At("RETURN"))
   private void skycore$ensurePinnedOnInit(CallbackInfo callbackInfo) {
      if (this.servers != null) {
         ApplyIdentityHelper.ensurePresent(this.servers);
      }
   }

   @Inject(method = "onSelectedChange", at = @At("RETURN"))
   private void skycore$disablePinnedActions(CallbackInfo callbackInfo) {
      ServerData serverData = this.skycore$selectedServerData();
      if (serverData != null && ApplyIdentityHelper.matches(serverData)) {
         this.editButton.active = false;
         this.deleteButton.active = false;
      }
   }

   @Inject(method = "deleteCallback", at = @At("HEAD"), cancellable = true)
   private void skycore$blockPinnedDelete(boolean condition, CallbackInfo callbackInfo) {
      if (condition) {
         ServerData serverData = this.skycore$selectedServerData();
         if (serverData != null && ApplyIdentityHelper.matches(serverData)) {
            callbackInfo.cancel();
         }
      }
   }

   @Inject(method = "editServerCallback", at = @At("HEAD"), cancellable = true)
   private void skycore$blockPinnedEdit(boolean condition, CallbackInfo callbackInfo) {
      if (condition) {
         ServerData serverData = this.skycore$selectedServerData();
         if (serverData != null && ApplyIdentityHelper.matches(serverData)) {
            callbackInfo.cancel();
         }
      }
   }

   private ServerData skycore$selectedServerData() {
      if (this.serverSelectionList == null) {
         return null;
      }

      Entry selected = this.serverSelectionList.getSelected();
      return selected instanceof OnlineServerEntry onlineServerEntry ? onlineServerEntry.getServerData() : null;
   }
}
