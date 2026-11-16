package deluxe.client.module.render;

import deluxe.client.module.combat.AttackAuraModule;
import deluxe.client.setting.BooleanSetting;
import deluxe.client.render.HandOutlineRenderer;
import deluxe.client.setting.LegacyNumberSetting;
import deluxe.client.setting.ModeSetting;
import deluxe.client.module.Module;
import deluxe.client.module.ModuleCategory;
import deluxe.client.module.ModuleInfo;
import deluxe.client.ui.UiTheme;
import deluxe.client.setting.ColorSetting;
import deluxe.client.setting.SectionSetting;

import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.util.Arm;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.joml.Quaternionf;

@ModuleInfo(name = "SwordAnimations", description = "Позволяет изменять анимацию удара.", category = ModuleCategory.RENDER)
public final class SwordAnimationsModule extends Module {
   private static SwordAnimationsModule swordAnimationsModule;
   public ModeSetting modeSetting = this.addSetting(
      new ModeSetting("Тип", "Вариант анимации удара.", "Мод 1", "Нету", "Мод 1", "Мод 2", "Мод 3", "Мод 4", "Мод 5", "Мод 6", "Мод 7", "Мод 8")
   );
   public LegacyNumberSetting field05586 = this.addSetting(new LegacyNumberSetting("Размер правой руки", "Масштаб предмета в правой руке.", 1.0, 0.5, 1.5, 0.05));
   public LegacyNumberSetting field05587 = this.addSetting(new LegacyNumberSetting("Размер левой руки", "Масштаб предмета в левой руке.", 1.0, 0.5, 1.5, 0.05));
   public LegacyNumberSetting field05588 = this.addSetting(new LegacyNumberSetting("Сила наклона", "Амплитуда наклона предмета при ударе.", 3.0, 1.0, 10.0, 1.0));
   public LegacyNumberSetting field05589 = this.addSetting(new LegacyNumberSetting("Сила", "Амплитуда провала руки вниз.", 15.0, 1.0, 15.0, 0.05));
   public LegacyNumberSetting field05590 = this.addSetting(new LegacyNumberSetting("Скорость удара", "Длительность взмаха руки в тиках.", 6.0, 3.0, 10.0, 1.0));
   public BooleanSetting field05591 = this.addSetting(
      new BooleanSetting("Только с аурой", "Анимация работает только при включённой AttackAura.", false)
   );
   public BooleanSetting field05592;
   public LegacyNumberSetting field05593;
   public LegacyNumberSetting field05594;
   public LegacyNumberSetting field05595;
   public BooleanSetting field05596;
   public ColorSetting colorSetting;

   public int method1756() {
      int local2653_1 = (int)this.field05590.getValue();
      if (mc.player.hasStatusEffect(StatusEffects.HASTE)) {
         return local2653_1 - (1 + mc.player.getStatusEffect(StatusEffects.HASTE).getAmplifier());
      } else {
         return mc.player.hasStatusEffect(StatusEffects.MINING_FATIGUE)
            ? local2653_1 + (1 + mc.player.getStatusEffect(StatusEffects.MINING_FATIGUE).getAmplifier()) * 2
            : local2653_1;
      }
   }

   @Override
   protected void onDisable() {
      HandOutlineRenderer.method0466().method0468();
   }

   private Quaternionf method1757(int local2655_1, float local2655_2) {
      return this.method1764(local2655_1).rotationDegrees(local2655_2);
   }

   public boolean method1758() {
      return !this.field05591.isChecked() || AttackAuraModule.method1029().isEnabled();
   }

   public void method1759(MatrixStack matrixStack, int local2657_2, float local2657_3) {
      float local2657_4 = this.field05588.getValue() * 10.0F;
      float local2657_5 = MathHelper.sin(local2657_3 * (float) Math.PI);
      float local2657_6 = local2657_4 / 10.0F / 10.0F * 40.0F;
      switch (this.modeSetting.getSelectedOption()) {
         case "Мод 1":
            matrixStack.translate(local2657_2 * 0.1F, 0.0F, -0.2F);
            matrixStack.translate(local2657_2 * 0.5F, -0.4F, -0.82F);
            matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(local2657_2 * 90.0F));
            matrixStack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(local2657_2 * -60.0F));
            matrixStack.multiply(this.method1764(local2657_2).rotationDegrees(local2657_2 * (-80.0F - local2657_4 * local2657_5)));
            break;
         case "Мод 2":
            matrixStack.translate(local2657_2 * 0.56F, -0.52F, -0.72F);
            matrixStack.multiply(
               RotationAxis.POSITIVE_Y.rotationDegrees(local2657_2 * (45.0F + MathHelper.sin(local2657_3 * local2657_3 * (float) Math.PI) / 4.0F * -20.0F))
            );
            matrixStack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(local2657_2 * MathHelper.sin(MathHelper.sqrt(local2657_3) * (float) Math.PI) * -20.0F));
            matrixStack.multiply(this.method1757(local2657_2, local2657_2 * local2657_5 * -local2657_4));
            matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(local2657_2 * -45.0F));
            break;
         case "Мод 3":
            matrixStack.translate(local2657_2 * 0.1F, 0.0F, -0.2F);
            matrixStack.translate(local2657_2 * 0.5F, -0.4F, -0.82F);
            matrixStack.multiply(this.method1757(local2657_2, local2657_2 * -120.0F));
            matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(local2657_2 * (-local2657_5 * local2657_4 + 30.0F)));
            matrixStack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(local2657_2 * 90.0F));
            break;
         case "Мод 4":
            matrixStack.translate(local2657_2 * 0.1F, 0.0F, -0.2F);
            matrixStack.translate(local2657_2 * 0.5F, -0.4F, -0.82F);
            matrixStack.multiply(this.method1757(local2657_2, local2657_2 * -120.0F));
            matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(local2657_2 * -local2657_5 * local2657_4));
            matrixStack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(local2657_2 * 90.0F));
            break;
         case "Мод 5":
            matrixStack.translate(local2657_2 * 0.3F, 0.0F, -0.3F);
            matrixStack.translate(local2657_2 * 0.6F, -0.6F, -0.9F);
            matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(local2657_2 * 45.0F));
            matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(local2657_2 * local2657_5 * -(local2657_6 / 2.0F)));
            matrixStack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(local2657_2 * local2657_5 * -(local2657_6 / 2.0F)));
            matrixStack.multiply(this.method1757(local2657_2, local2657_2 * local2657_5 * -local2657_6));
            matrixStack.translate(local2657_2 * -0.5F, 0.2F, 0.0F);
            matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(local2657_2 * 30.0F));
            matrixStack.multiply(this.method1757(local2657_2, local2657_2 * -80.0F));
            matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(local2657_2 * 60.0F));
            break;
         case "Мод 6":
            matrixStack.translate(local2657_2 * 0.3F, 0.0F, -0.3F);
            matrixStack.translate(local2657_2 * 0.6F, -0.6F, -0.9F);
            matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(local2657_2 * 45.0F));
            matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(local2657_2 * local2657_5 * local2657_6));
            matrixStack.multiply(this.method1757(local2657_2, local2657_2 * local2657_5 * -local2657_6));
            matrixStack.translate(local2657_2 * -0.5F, 0.2F, 0.0F);
            matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(local2657_2 * 30.0F));
            matrixStack.multiply(this.method1757(local2657_2, local2657_2 * -80.0F));
            matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(local2657_2 * 60.0F));
            break;
         case "Мод 7":
            matrixStack.translate(local2657_2 * 0.56F, -0.52F, -0.72F);
            matrixStack.translate(
               0.0F, MathHelper.lerp(local2657_5, 0.0F, -0.8F) * MathHelper.clamp((this.field05589.getValue() - 1.0F) / 14.0F, 0.0F, 1.0F), 0.0F
            );
            break;
         case "Мод 8":
            matrixStack.translate(local2657_2 * 0.72F, -0.5F, -1.0F);
            matrixStack.multiply(this.method1757(local2657_2, local2657_2 * 20.0F));
            matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(local2657_2 * -90.0F));
            matrixStack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(local2657_2 * 60.0F));
            matrixStack.multiply(this.method1757(local2657_2, local2657_2 * 30.0F));
            matrixStack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(local2657_2 * local2657_4 * 0.3F));
            matrixStack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(local2657_2 * local2657_4 * local2657_5));
      }
   }

   public float method1760(Arm arm) {
      return !this.isEnabled() ? 1.0F : (arm == Arm.RIGHT ? this.field05586 : this.field05587).getValue();
   }

   public boolean method1762() {
      return this.isEnabled() && !this.method1763();
   }

   public boolean method1763() {
      return this.modeSetting.isSelected("Нету");
   }

   private RotationAxis method1764(int local2662_1) {
      return local2662_1 == -1 ? RotationAxis.NEGATIVE_X : RotationAxis.POSITIVE_X;
   }

   public boolean method1766() {
      return this.isEnabled() && this.field05592.isChecked();
   }

   public int method1767() {
      return this.field05596.isChecked() ? this.colorSetting.getValue() : UiTheme.field09686;
   }

   public boolean method1768(Arm arm) {
      return this.isEnabled() && !this.method1763() && this.method1758() && mc.player != null && arm == mc.player.getMainArm();
   }

   public static SwordAnimationsModule method1770() {
      if (swordAnimationsModule == null) {
         swordAnimationsModule = new SwordAnimationsModule();
      }

      return swordAnimationsModule;
   }

   private SwordAnimationsModule() {
      this.addSetting(new SectionSetting("Применить шейдер"));
      this.field05592 = this.addSetting(new BooleanSetting("Применить шейдер", "Обводка и тень вокруг предмета в руке.", false));
      this.field05593 = this.addSetting(new LegacyNumberSetting("Оутлайн", "Сила обводки.", 0.8, 0.0, 1.0, 0.05));
      this.field05594 = this.addSetting(new LegacyNumberSetting("Заливка", "Заливка предмета цветом.", 0.0, 0.0, 1.0, 0.05));
      this.field05595 = this.addSetting(new LegacyNumberSetting("Подсветка", "Сила свечения вокруг предмета.", 0.55, 0.0, 3.0, 0.05));
      this.field05596 = this.addSetting(new BooleanSetting("Кастомный цвет", "Свой цвет вместо акцента темы.", false));
      this.colorSetting = this.addSetting(new ColorSetting("Цвет", "Цвет обводки и тени."));
      this.field05588.withVisibility(() -> !this.modeSetting.isSelected("Мод 7") && !this.method1763());
      this.field05590.withVisibility(() -> !this.method1763());
      this.field05589.withVisibility(() -> this.modeSetting.isSelected("Мод 7"));
      this.field05593.withVisibility(() -> this.field05592.isChecked());
      this.field05594.withVisibility(() -> this.field05592.isChecked());
      this.field05595.withVisibility(() -> this.field05592.isChecked());
      this.field05596.withVisibility(() -> this.field05592.isChecked());
      this.colorSetting.withVisibility(() -> this.field05592.isChecked() && this.field05596.isChecked());
   }
}
