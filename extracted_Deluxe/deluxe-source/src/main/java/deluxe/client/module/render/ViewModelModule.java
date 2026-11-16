package deluxe.client.module.render;

import deluxe.client.module.combat.AttackAuraModule;
import deluxe.client.setting.BooleanSetting;
import deluxe.client.event.EventListener;
import deluxe.client.event.HeldItemRenderEvent;
import deluxe.client.setting.LegacyNumberSetting;
import deluxe.client.module.Module;
import deluxe.client.module.ModuleCategory;
import deluxe.client.module.ModuleInfo;
import deluxe.client.setting.SectionSetting;

import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.util.Arm;

@ModuleInfo(name = "ViewModel", description = "Позиция рук в кадре и поведение вьюмодели.", category = ModuleCategory.RENDER)
public final class ViewModelModule extends Module {
   private static ViewModelModule viewModelModule;
   public LegacyNumberSetting field10261;
   public LegacyNumberSetting field10262;
   public LegacyNumberSetting field10263;
   public LegacyNumberSetting field10264;
   public LegacyNumberSetting field10265;
   public LegacyNumberSetting field10266;
   public BooleanSetting field10267;
   public BooleanSetting field10268;
   public BooleanSetting field10269;
   private final EventListener<HeldItemRenderEvent> field10270 = heldItemRenderEvent -> {
      if (this.method3024() && !heldItemRenderEvent.method0880().isEmpty() && !heldItemRenderEvent.method0880().contains(DataComponentTypes.MAP_ID)) {
         this.method3026(heldItemRenderEvent.method0878(), heldItemRenderEvent.method0877());
      }
   };

   private ViewModelModule() {
      this.addSetting(new SectionSetting("Правая рука"));
      this.field10261 = this.addSetting(new LegacyNumberSetting("Правая X", "Смещение правой руки по X.", 0.0, -1.0, 1.0, 0.05));
      this.field10262 = this.addSetting(new LegacyNumberSetting("Правая Y", "Смещение правой руки по Y.", 0.0, -1.0, 1.0, 0.05));
      this.field10263 = this.addSetting(new LegacyNumberSetting("Правая Z", "Смещение правой руки по Z.", 0.0, -1.0, 1.0, 0.05));
      this.addSetting(new SectionSetting("Левая рука"));
      this.field10264 = this.addSetting(new LegacyNumberSetting("Левая X", "Смещение левой руки по X.", 0.0, -1.0, 1.0, 0.05));
      this.field10265 = this.addSetting(new LegacyNumberSetting("Левая Y", "Смещение левой руки по Y.", 0.0, -1.0, 1.0, 0.05));
      this.field10266 = this.addSetting(new LegacyNumberSetting("Левая Z", "Смещение левой руки по Z.", 0.0, -1.0, 1.0, 0.05));
      this.field10267 = this.addSetting(new BooleanSetting("Убрать анимацию юза", "Убирает тряску предмета при еде и питье.", false));
      this.field10268 = this.addSetting(new BooleanSetting("Не двигать руки", "Отключает отставание рук при повороте камеры.", false));
      this.field10269 = this.addSetting(new BooleanSetting("Только с киллаурой", "Смещения рук работают только при включённой AttackAura.", false));
   }

   public boolean method3024() {
      return !this.field10269.isChecked() || AttackAuraModule.method1029().isEnabled();
   }

   public void method3026(MatrixStack matrixStack, Arm arm) {
      if (arm == Arm.RIGHT) {
         matrixStack.translate(this.field10261.getValue(), this.field10262.getValue(), this.field10263.getValue());
      } else {
         matrixStack.translate(-this.field10264.getValue(), this.field10265.getValue(), this.field10266.getValue());
      }
   }

   public static ViewModelModule method3027() {
      if (viewModelModule == null) {
         viewModelModule = new ViewModelModule();
      }

      return viewModelModule;
   }

   public boolean method3028() {
      return this.isEnabled() && this.field10267.isChecked();
   }

   public boolean method3029() {
      return this.isEnabled() && this.field10268.isChecked();
   }
}
