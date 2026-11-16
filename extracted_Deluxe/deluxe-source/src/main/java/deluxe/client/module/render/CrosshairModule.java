package deluxe.client.module.render;

import deluxe.client.setting.BooleanSetting;
import deluxe.client.util.Colors;
import deluxe.client.event.EventListener;
import deluxe.client.event.HudRenderEvent;
import deluxe.client.setting.LegacyNumberSetting;
import deluxe.client.module.Module;
import deluxe.client.module.ModuleCategory;
import deluxe.client.module.ModuleInfo;
import deluxe.client.util.GuiScale;
import deluxe.client.setting.ColorSetting;
import deluxe.client.render.Render2D;

import net.minecraft.util.hit.HitResult.Type;

@ModuleInfo(
   name = "Crosshair",
   description = "Свой прицел вместо ванильного: крест с плавным кулдауном удара и краснением по цели.",
   category = ModuleCategory.RENDER
)
public final class CrosshairModule extends Module {
   private static CrosshairModule crosshairModule;
   public ColorSetting colorSetting;
   public LegacyNumberSetting field10461;
   public LegacyNumberSetting field10462;
   public LegacyNumberSetting field10463;
   public BooleanSetting field10464;
   public LegacyNumberSetting field10465;
   public BooleanSetting field10466;
   public BooleanSetting field10467;
   public BooleanSetting field10468;
   public LegacyNumberSetting field10469;
   private float field10470;
   private float field10471;
   private final EventListener<HudRenderEvent> field10472 = hudRenderEvent -> {
      if (mc.player != null
         && mc.world != null
         && mc.options.getPerspective().isFirstPerson()
         && !CameraFeatureModule.method0782().method0793()) {
         float local4902_2 = hudRenderEvent.method4462() / 2.0F * GuiScale.method0797() / GuiScale.method0794();
         float local4902_3 = hudRenderEvent.method4460() / 2.0F * GuiScale.method0797() / GuiScale.method0794();
         this.field10470 = this.field10470
            + ((this.field10464.isChecked() ? 1.0F - mc.player.getAttackCooldownProgress(hudRenderEvent.method4461()) : 0.0F) - this.field10470) * 0.3F;
         this.field10471 = this.field10471
            + ((mc.crosshairTarget != null && mc.crosshairTarget.getType() == Type.ENTITY ? 1.0F : 0.0F) - this.field10471) * 0.3F;
         int local4902_4 = Colors.method3723(this.colorSetting.getValue(), -51401, this.field10471);
         float local4902_5 = this.field10461.getValue() + this.field10465.getValue() * this.field10470;
         if (!this.field10467.isChecked()) {
            this.method3071(
               local4902_2 - this.field10463.getValue() / 2.0F,
               local4902_3 - local4902_5 - this.field10462.getValue(),
               this.field10463.getValue(),
               this.field10462.getValue(),
               local4902_4
            );
         }

         this.method3071(
            local4902_2 - this.field10463.getValue() / 2.0F,
            local4902_3 + local4902_5,
            this.field10463.getValue(),
            this.field10462.getValue(),
            local4902_4
         );
         this.method3071(
            local4902_2 - local4902_5 - this.field10462.getValue(),
            local4902_3 - this.field10463.getValue() / 2.0F,
            this.field10462.getValue(),
            this.field10463.getValue(),
            local4902_4
         );
         this.method3071(
            local4902_2 + local4902_5,
            local4902_3 - this.field10463.getValue() / 2.0F,
            this.field10462.getValue(),
            this.field10463.getValue(),
            local4902_4
         );
         if (this.field10468.isChecked()) {
            this.method3071(
               local4902_2 - this.field10469.getValue() / 2.0F,
               local4902_3 - this.field10469.getValue() / 2.0F,
               this.field10469.getValue(),
               this.field10469.getValue(),
               local4902_4
            );
         }
      }
   };

   private CrosshairModule() {
      this.colorSetting = this.addSetting(new ColorSetting("Цвет", "Цвет прицела, при наведении на энтити краснеет."));
      this.field10461 = this.addSetting(new LegacyNumberSetting("Отступ", "Просвет от центра до линий.", 2.0, 0.0, 20.0, 1.0));
      this.field10462 = this.addSetting(new LegacyNumberSetting("Длина", "Длина линий креста.", 5.0, 1.0, 20.0, 1.0));
      this.field10463 = this.addSetting(new LegacyNumberSetting("Толщина", "Толщина линий.", 2.0, 1.0, 5.0, 1.0));
      this.field10464 = this.addSetting(new BooleanSetting("Кулдаун", "Показывать перезарядку удара: крест плавно расходится и сходится.", true));
      this.field10465 = this.addSetting(new LegacyNumberSetting("Сила кулдауна", "Насколько крест расходится во время перезарядки.", 8.0, 1.0, 20.0, 1.0));
      this.field10465.withVisibility(() -> this.field10464.isChecked());
      this.field10466 = this.addSetting(new BooleanSetting("Обводка", "Тёмная обводка вокруг линий.", true));
      this.field10467 = this.addSetting(new BooleanSetting("T-режим", "Не рисовать верхнюю линию.", false));
      this.field10468 = this.addSetting(new BooleanSetting("Точка в центре", "Точка в центре прицела.", false));
      this.field10469 = this.addSetting(new LegacyNumberSetting("Размер точки", "Размер центральной точки.", 2.0, 1.0, 10.0, 0.5));
      this.field10469.withVisibility(() -> this.field10468.isChecked());
   }

   public static CrosshairModule method3070() {
      if (crosshairModule == null) {
         crosshairModule = new CrosshairModule();
      }

      return crosshairModule;
   }

   private void method3071(float local4904_1, float local4904_2, float local4904_3, float local4904_4, int local4904_5) {
      if (this.field10466.isChecked()) {
         Render2D.method4727(local4904_1 - 0.5F, local4904_2 - 0.5F, local4904_3 + 1.0F, local4904_4 + 1.0F, -1275068416);
      }

      Render2D.method4727(local4904_1, local4904_2, local4904_3, local4904_4, local4904_5);
   }
}
