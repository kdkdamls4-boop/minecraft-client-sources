package net.skycore.client.mixin;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.multiplayer.ServerSelectionList.OnlineServerEntry;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerList;
import net.skycore.client.util.ApplyIdentityHelper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(OnlineServerEntry.class)
public abstract class ServerSelectionListOnlineServerEntryHighlightPinnedMixin {
   @Shadow
   @Final
   private JoinMultiplayerScreen screen;
   @Shadow
   @Final
   private ServerData serverData;

   @Inject(method = "extractContent", at = @At("HEAD"))
   private void skycore$highlightPinned(GuiGraphicsExtractor guiGraphicsExtractor, int value, int value2, boolean condition, float value3, CallbackInfo callbackInfo) {
      if (ApplyIdentityHelper.matches(this.serverData)) {
         ApplyIdentityHelper.applyIdentity(this.serverData);
         LayoutElement layoutElement = (LayoutElement)this;
         int x = layoutElement.getX();
         int y = layoutElement.getY();
         int width = x + layoutElement.getWidth();
         int height = y + layoutElement.getHeight();
         guiGraphicsExtractor.fill(x, y, width, height, 956256336);
      }
   }

   @Inject(method = "swap", at = @At("HEAD"), cancellable = true)
   private void skycore$blockPinnedReorder(int value, int value2, CallbackInfo callbackInfo) {
      ServerList servers = this.screen.getServers();
      if (ApplyIdentityHelper.matches(this.serverData)) {
         callbackInfo.cancel();
      } else {
         if ((value == 0 || value2 == 0) && ApplyIdentityHelper.isFirstPinned(servers)) {
            callbackInfo.cancel();
         }
      }
   }
}
