package net.skycore.client.module.render;

import java.util.Optional;
import net.skycore.client.SkycoreClient;
import net.skycore.client.module.ClientModule;
import net.skycore.client.module.ModuleCategory;
import net.skycore.client.setting.NumberSetting;
import net.skycore.client.util.MotionBlurRenderer;

public final class MotionBlurModule extends ClientModule {
   private final NumberSetting strength = this.add(new NumberSetting("Strength", "", 50.0, 1.0, 100.0, 1.0, ""));
   private final MotionBlurRenderer renderer = new MotionBlurRenderer(this);

   @Override
   protected void onEnable() {
      this.renderer.resetHistory();
   }

   @Override
   protected void onDisable() {
      this.renderer.release();
   }

   public MotionBlurRenderer renderer() {
      return this.renderer;
   }

   public Number strength() {
      return this.strength.get();
   }

   public static MotionBlurModule enabled() {
      var optional = SkycoreClient.modules().get(MotionBlurModule.class);
      return optional.filter(ClientModule::isEnabled).orElse(null);
   }

   public MotionBlurModule() {
      super("Motion Blur", "Screen motion trail from previous frames", ModuleCategory.RENDER);
   }
}
