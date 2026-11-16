package net.skycore.client.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Hud;
import net.skycore.client.SkycoreClient;
import net.skycore.client.module.render.DuplisheModule;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(Hud.class)
public abstract class HudDuplisheHeartsMixin {
   @ModifyArgs(
      method = "extractPlayerHealth",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/gui/Hud;extractHearts(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/world/entity/player/Player;IIIIFIIIZ)V"
      )
   )
   private void skycore$duplisheHearts(Args args) {
      if (args.get(1) == Minecraft.getInstance().player) {
         SkycoreClient.modules().get(DuplisheModule.class).ifPresent(duplisheModule -> {
            if (duplisheModule.hasVisualDamage()) {
               int value = args.<Integer>get(7);
               int value2 = args.<Integer>get(8);
               args.set(7, duplisheModule.displayedHealth(value));
               args.set(8, duplisheModule.displayedHealth(value2));
               if (duplisheModule.peckAmount() > 0.15F) {
                  args.set(10, true);
               }
            }
         });
      }
   }
}
