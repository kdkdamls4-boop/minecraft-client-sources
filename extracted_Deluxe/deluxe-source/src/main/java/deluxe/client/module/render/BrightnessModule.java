package deluxe.client.module.render;

import deluxe.client.event.EventListener;
import deluxe.client.setting.LegacyNumberSetting;
import deluxe.client.setting.ModeSetting;
import deluxe.client.module.Module;
import deluxe.client.module.ModuleCategory;
import deluxe.client.module.ModuleInfo;
import deluxe.client.event.PlayerTickEvent;

import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;

@ModuleInfo(name = "Brightness", description = "Убирает темноту: поднимает гамму мира или вешает ночное зрение.", category = ModuleCategory.RENDER)
public final class BrightnessModule extends Module {
   private static BrightnessModule brightnessModule;
   public ModeSetting modeSetting;
   public LegacyNumberSetting legacyNumberSetting;
   private boolean field01457;
   private final EventListener<PlayerTickEvent> field01458 = playerTickEvent -> {
      if (mc.player != null) {
         if (this.modeSetting.isSelected("Зелье")) {
            mc.player.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, 300, 0, false, false));
            this.field01457 = true;
         } else if (this.field01457) {
            mc.player.removeStatusEffect(StatusEffects.NIGHT_VISION);
            this.field01457 = false;
         }
      }
   };

   public static BrightnessModule method0820() {
      if (brightnessModule == null) {
         brightnessModule = new BrightnessModule();
      }

      return brightnessModule;
   }

   @Override
   protected void onDisable() {
      if (mc.player != null && this.field01457) {
         mc.player.removeStatusEffect(StatusEffects.NIGHT_VISION);
      }

      this.field01457 = false;
   }

   private BrightnessModule() {
      this.modeSetting = this.addSetting(new ModeSetting("Освещение", "Чем светить в темноте.", "Гамма", "Гамма", "Зелье"));
      this.legacyNumberSetting = this.addSetting(new LegacyNumberSetting("Сила гаммы", "Во сколько раз поднять яркость мира.", 5.0, 1.0, 15.0, 0.1));
      this.legacyNumberSetting.withVisibility(() -> this.modeSetting.isSelected("Гамма"));
   }
}
