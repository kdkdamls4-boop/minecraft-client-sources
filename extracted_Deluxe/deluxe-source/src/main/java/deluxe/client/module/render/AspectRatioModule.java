package deluxe.client.module.render;

import deluxe.client.event.CameraFovEvent;
import deluxe.client.event.EventListener;
import deluxe.client.setting.ModeSetting;
import deluxe.client.module.Module;
import deluxe.client.module.ModuleCategory;
import deluxe.client.module.ModuleInfo;
import deluxe.client.setting.NumberSetting;

@ModuleInfo(name = "AspectRatio", description = "Меняет соотношение сторон картинки мира с плавной анимацией перехода.", category = ModuleCategory.RENDER)
public final class AspectRatioModule extends Module {
   private static AspectRatioModule aspectRatioModule;
   public ModeSetting modeSetting;
   public NumberSetting numberSetting;
   private float field02256;
   private long field02257;
   private final EventListener<CameraFovEvent> field02258 = cameraFovEvent -> {
      if (mc.getWindow() != null) {
         float local1414_2 = (float)mc.getWindow().getFramebufferWidth() / Math.max(1, mc.getWindow().getFramebufferHeight());

         float local1414_3 = switch (this.modeSetting.getSelectedOption()) {
            case "21:9" -> 2.3333333F;
            case "16:9" -> 1.7777778F;
            case "16:10" -> 1.6F;
            case "5:4" -> 1.25F;
            case "1:1" -> 1.0F;
            case "9:16" -> 0.5625F;
            case "Кастом" -> this.numberSetting.floatValue();
            default -> 1.3333334F;
         };
         long local1414_4 = System.currentTimeMillis();
         if (this.field02256 == 0.0F) {
            this.field02256 = local1414_2;
         }

         this.field02256 = this.field02256 + (local1414_3 - this.field02256) * (1.0F - (float)Math.exp(-Math.min(100L, local1414_4 - this.field02257) / 160.0));
         this.field02257 = local1414_4;
         cameraFovEvent.method3579(this.field02256);
         cameraFovEvent.method4424(true);
      }
   };

   @Override
   protected void onEnable() {
      this.field02256 = 0.0F;
      this.field02257 = System.currentTimeMillis();
   }

   public int method1040(int local1415_1) {
      return this.isEnabled() && !(this.field02256 <= 0.0F) && mc.getWindow() != null
         ? Math.max(1, Math.round(mc.getWindow().getFramebufferHeight() * this.field02256))
         : local1415_1;
   }

   private AspectRatioModule() {
      this.modeSetting = this.addSetting(
         new ModeSetting("Соотношение", "Готовые пресеты соотношения сторон.", "4:3", "21:9", "16:9", "16:10", "4:3", "5:4", "1:1", "9:16", "Кастом")
      );
      this.numberSetting = this.addSetting(
         new NumberSetting("Своё соотношение", "Ширина к высоте для пресета Кастом.")
            .withRange(0.4F, 3.2F)
            .withStep(0.01F)
            .setValue(1.78F)
            .withSuffix(":1")
      );
   }

   public static AspectRatioModule method1041() {
      if (aspectRatioModule == null) {
         aspectRatioModule = new AspectRatioModule();
      }

      return aspectRatioModule;
   }
}
