package net.skycore.client.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.skycore.client.event.ChatCommandHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatScreen.class)
public abstract class ChatScreenHandleFriendCommandMixin {
   @Inject(method = "handleChatInput", at = @At("HEAD"), cancellable = true)
   private void skycore$handleFriendCommand(String text, boolean condition, CallbackInfo callbackInfo) {
      ChatScreen chatScreen = (ChatScreen)(Object)this;
      String text2 = chatScreen.normalizeChatMessage(text);
      if (ChatCommandHandler.handle(text2)) {
         if (condition) {
            Minecraft.getInstance().gui.hud.getChat().addRecentChat(text2);
         }

         callbackInfo.cancel();
      }
   }
}
