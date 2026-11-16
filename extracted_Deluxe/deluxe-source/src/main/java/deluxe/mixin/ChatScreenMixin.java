package deluxe.mixin;

import deluxe.client.DeluxeClient;
import deluxe.client.ui.hud.HudElementTypes;
import deluxe.client.ui.hud.WatermarkHud;
import deluxe.client.util.IrcClient;
import deluxe.client.ui.hud.HudManager;
import ru.dreamix.fabricloader.VMBridge;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChatScreen.class)
public abstract class ChatScreenMixin extends Screen {
   @Shadow
   protected TextFieldWidget chatField;

   protected ChatScreenMixin(Text text) {
      super(text);
   }

   @Inject(method = "sendMessage(Ljava/lang/String;Z)V", at = @At("HEAD"), cancellable = true)
   private void dispatchClientCommand(String local8106_1, boolean local8106_2, CallbackInfo callbackInfo) {
      if (DeluxeClient.inst().commandDispatcher() != null && DeluxeClient.inst().commandDispatcher().method1898(local8106_1)) {
         this.client.inGameHud.getChatHud().addToMessageHistory(local8106_1);
         callbackInfo.cancel();
      }
   }

   @Inject(method = "sendMessage(Ljava/lang/String;Z)V", at = @At("HEAD"), cancellable = true)
   private void sendIrcMessage(String local8107_1, boolean local8107_2, CallbackInfo callbackInfo) {
      if (IrcClient.method2834().method2826() && local8107_1.startsWith("@")) {
         IrcClient.method2834().method2820(local8107_1.substring(1).trim());
         this.client.inGameHud.getChatHud().addToMessageHistory(local8107_1);
         callbackInfo.cancel();
      }
   }

   @Inject(method = "mouseClicked(Lnet/minecraft/client/gui/Click;Z)Z", at = @At("HEAD"), cancellable = true)
   private void dragClick(Click click, boolean local8108_2, CallbackInfoReturnable<Boolean> local8108_3) {
      if (HudElementTypes.method4840(click.x(), click.y(), click.button())) {
         local8108_3.setReturnValue(true);
      } else if (WatermarkHud.method2500(click.x(), click.y(), click.button())) {
         local8108_3.setReturnValue(true);
      } else {
         if (HudManager.method3401(click.x(), click.y(), click.button())) {
            local8108_3.setReturnValue(true);
         }
      }
   }

   @Inject(method = "removed()V", at = @At("HEAD"))
   private void dragRemoved(CallbackInfo callbackInfo) {
      WatermarkHud.method2506();
      HudManager.method3423();
   }

   @Override
   public boolean mouseReleased(Click click) {
      HudElementTypes.method4846();
      WatermarkHud.method2506();
      HudManager.method3396(click.button());
      return super.mouseReleased(click);
   }

   static {
      VMBridge.identifyClass(ChatScreenMixin.class, "CVis2JDr");
   }
}
