package deluxe.mixin;

import deluxe.client.module.misc.StreamerModModule;
import deluxe.client.module.misc.YTMODEModule;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.gui.hud.BossBarHud;
import net.minecraft.entity.boss.BossBar.Color;
import net.minecraft.entity.boss.BossBar.Style;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(BossBarHud.class)
public abstract class BossBarHudMixin2 {
   @ModifyExpressionValue(
      method = "render(Lnet/minecraft/client/gui/DrawContext;)V",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/hud/ClientBossBar;getName()Lnet/minecraft/text/Text;")
   )
   private Text maskDuelBossBarTitle(Text text) {
      if (YTMODEModule.method1426().method1425()) {
         return YTMODEModule.method1426().method1422();
      } else {
         return !StreamerModModule.method0449().isEnabled() ? text : StreamerModModule.method0449().method0451();
      }
   }

   @ModifyExpressionValue(
      method = "renderBossBar(Lnet/minecraft/client/gui/DrawContext;IILnet/minecraft/entity/boss/BossBar;)V",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/boss/BossBar;getPercent()F")
   )
   private float freezeDuelBossBarProgress(float local8364_1) {
      return !StreamerModModule.method0449().isEnabled() && !YTMODEModule.method1426().method1425() ? local8364_1 : 1.0F;
   }

   @ModifyExpressionValue(
      method = "renderBossBar(Lnet/minecraft/client/gui/DrawContext;IILnet/minecraft/entity/boss/BossBar;I[Lnet/minecraft/util/Identifier;[Lnet/minecraft/util/Identifier;)V",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/boss/BossBar;getColor()Lnet/minecraft/entity/boss/BossBar$Color;")
   )
   private Color recolorDuelBossBar(Color color) {
      if (YTMODEModule.method1426().method1425()) {
         return YTMODEModule.method1426().method1424();
      } else {
         return !StreamerModModule.method0449().isEnabled() ? color : Color.BLUE;
      }
   }

   @ModifyExpressionValue(
      method = "renderBossBar(Lnet/minecraft/client/gui/DrawContext;IILnet/minecraft/entity/boss/BossBar;I[Lnet/minecraft/util/Identifier;[Lnet/minecraft/util/Identifier;)V",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/boss/BossBar;getStyle()Lnet/minecraft/entity/boss/BossBar$Style;")
   )
   private Style notchDuelBossBar(Style style) {
      return !StreamerModModule.method0449().isEnabled() && !YTMODEModule.method1426().method1425() ? style : Style.NOTCHED_20;
   }
}
