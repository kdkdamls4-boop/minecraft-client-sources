package deluxe.client.module.combat;

import deluxe.client.setting.BooleanSetting;
import deluxe.client.event.EntityYawEvent;
import deluxe.client.event.EventListener;
import deluxe.client.module.Module;
import deluxe.client.module.ModuleCategory;
import deluxe.client.module.ModuleInfo;
import deluxe.client.setting.NumberSetting;

@ModuleInfo(name = "HitBoxes", description = "Раздувает хитбоксы сущностей, по которым попадают удары.", category = ModuleCategory.COMBAT)
public final class HitBoxesModule extends Module {
   private static HitBoxesModule hitBoxesModule;
   public NumberSetting numberSetting;
   public BooleanSetting booleanSetting;
   public boolean field02329;
   public final EventListener<EntityYawEvent> field02330 = entityYawEvent -> {
      if (!this.field02329 || this.booleanSetting.isChecked()) {
         if (entityYawEvent.method3192() < this.numberSetting.floatValue()) {
            entityYawEvent.method3194(this.numberSetting.floatValue());
         }
      }
   };

   private HitBoxesModule() {
      this.numberSetting = this.addSetting(
         new NumberSetting("Установите размер бокса", "На сколько блоков раздуть бокс по горизонтали.")
            .withRange(0.0F, 5.0F)
            .withStep(0.1F)
            .setValue(0.3F)
      );
      this.booleanSetting = this.addSetting(new BooleanSetting("Отображать в F3+B", "Рисовать раздутый бокс в дебаг-рендере хитбоксов.", true));
   }

   public static HitBoxesModule method1050() {
      if (hitBoxesModule == null) {
         hitBoxesModule = new HitBoxesModule();
      }

      return hitBoxesModule;
   }
}
