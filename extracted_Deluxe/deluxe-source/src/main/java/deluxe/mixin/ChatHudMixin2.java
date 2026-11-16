package deluxe.mixin;

import deluxe.client.module.misc.AutoAcceptModule;
import deluxe.client.module.misc.AutoMineModule;
import deluxe.client.module.misc.AutoVillagerModule;
import deluxe.client.module.misc.AutoWardenModule;
import deluxe.client.module.render.SmoothFeatureModule;
import deluxe.client.module.misc.StreamerModModule;
import deluxe.client.module.misc.YTMODEModule;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.MessageIndicator;
import net.minecraft.network.message.MessageSignatureData;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatHud.class)
public abstract class ChatHudMixin2 {
   @Inject(method = "addMessage(Lnet/minecraft/text/Text;)V", at = @At("HEAD"), cancellable = true)
   private void muteBaritoneChat(Text text, CallbackInfo callbackInfo) {
      if (AutoMineModule.method3460().isEnabled() && AutoMineModule.method3460().method3500(text.getString())) {
         callbackInfo.cancel();
      }
   }

   @Inject(method = "addMessage(Lnet/minecraft/text/Text;)V", at = @At("HEAD"), cancellable = true)
   private void muteVillagerBaritoneChat(Text text, CallbackInfo callbackInfo) {
      if (AutoVillagerModule.method2015() && AutoMineModule.method3460().method3500(text.getString())) {
         callbackInfo.cancel();
      }
   }

   @Inject(method = "addMessage(Lnet/minecraft/text/Text;)V", at = @At("HEAD"), cancellable = true)
   private void muteWardenBaritoneChat(Text text, CallbackInfo callbackInfo) {
      if (AutoWardenModule.method1542() && AutoMineModule.method3460().method3500(text.getString())) {
         callbackInfo.cancel();
      }
   }

   @Inject(method = "addMessage(Lnet/minecraft/text/Text;)V", at = @At("HEAD"), cancellable = true)
   private void muteServerGreetingChat(Text text, CallbackInfo callbackInfo) {
      if (YTMODEModule.method1426().isEnabled() && YTMODEModule.method1426().method1428(text.getString())
         || StreamerModModule.method0449().isEnabled() && StreamerModModule.method0449().method0452(text.getString())) {
         callbackInfo.cancel();
      }
   }

   @Inject(method = "addMessage(Lnet/minecraft/text/Text;)V", at = @At("HEAD"))
   private void acceptChatInvites(Text text, CallbackInfo callbackInfo) {
      AutoAcceptModule.method3705().method3706(text.getString());
   }

   @Inject(
      method = "addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;Lnet/minecraft/client/gui/hud/MessageIndicator;)V",
      at = @At("HEAD"),
      cancellable = true
   )
   private void stackDuplicateChatSpam(Text text, MessageSignatureData messageSignatureData, MessageIndicator messageIndicator, CallbackInfo callbackInfo) {
      if (SmoothFeatureModule.method3278().method3285((ChatHud)(Object)this, text)) {
         callbackInfo.cancel();
      }
   }

   @Inject(
      method = "addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;Lnet/minecraft/client/gui/hud/MessageIndicator;)V",
      at = @At("TAIL")
   )
   private void slideChatOnNewLine(Text text, MessageSignatureData messageSignatureData, MessageIndicator messageIndicator, CallbackInfo callbackInfo) {
      SmoothFeatureModule.method3278().method3276();
   }

   @Inject(method = "render(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/font/TextRenderer;IIIZZ)V", at = @At("HEAD"))
   private void beginChatSlideRender(
      DrawContext drawContext,
      TextRenderer textRenderer,
      int local8323_3,
      int local8323_4,
      int local8323_5,
      boolean local8323_6,
      boolean local8323_7,
      CallbackInfo callbackInfo
   ) {
      SmoothFeatureModule.method3278().method3284((ChatHud)(Object)this, drawContext);
   }

   @Inject(method = "render(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/font/TextRenderer;IIIZZ)V", at = @At("RETURN"))
   private void endChatSlideRender(
      DrawContext drawContext,
      TextRenderer textRenderer,
      int local8324_3,
      int local8324_4,
      int local8324_5,
      boolean local8324_6,
      boolean local8324_7,
      CallbackInfo callbackInfo
   ) {
      SmoothFeatureModule.method3278().method3280(drawContext);
   }
}
