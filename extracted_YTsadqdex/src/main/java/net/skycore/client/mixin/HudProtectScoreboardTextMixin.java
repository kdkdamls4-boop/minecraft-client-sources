package net.skycore.client.mixin;

import net.skycore.client.profile.LocalProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Hud;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.skycore.client.module.misc.StreamerModeModule;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(Hud.class)
public abstract class HudProtectScoreboardTextMixin {
   @ModifyArgs(
      method = "displayScoreboardSidebar",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;text(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;IIIZ)V"
      )
   )
   private void skycore$protectScoreboardText(Args args) {
      Component text = args.get(1);
      Component text2 = StreamerModeModule.protectScoreboardText(text);
      if (LocalProfile.DISPLAY_NAME.equals(text2.getString())) {
         MutableComponent mutableComponent = Component.literal(LocalProfile.DISPLAY_NAME).withColor(16750848);
         args.set(1, mutableComponent);
         int value = args.<Integer>get(2);
         int instance = Minecraft.getInstance().getWindow().getGuiScaledWidth() - 3;
         int instance2 = Minecraft.getInstance().font.width(mutableComponent);
         args.set(2, value + (instance - value - instance2) / 2);
         args.set(4, -24576);
      } else {
         args.set(1, text2);
      }
   }
}
