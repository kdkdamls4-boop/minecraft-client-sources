package deluxe.client.module.render;

import deluxe.client.event.EventListener;
import deluxe.client.event.EventPriority;
import deluxe.client.render.FontFace;
import deluxe.client.event.HudRenderEvent;
import deluxe.client.module.Module;
import deluxe.client.module.ModuleCategory;
import deluxe.client.module.ModuleInfo;
import deluxe.client.setting.NumberSetting;
import deluxe.client.event.PlayerTickEvent;
import deluxe.client.util.GuiScale;
import deluxe.client.render.FontText;

import java.util.Locale;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

@ModuleInfo(name = "FreeCamera", description = "Отвязанная камера для полёта и обзора сквозь блоки.", category = ModuleCategory.RENDER)
public final class FreeCameraModule extends Module {
   private static FreeCameraModule freeCameraModule;
   public NumberSetting numberSetting;
   private Vec3d field01737;
   private Vec3d field01738;
   private float field01739;
   private float field01740;
   private long field01741;
   private Vec3d field01742;
   private float field01743;
   private float field01744;
   private ClientPlayerEntity field01745;
   private final EventListener<PlayerTickEvent> field01746 = playerTickEvent -> {
      if (mc.player != null) {
         if (this.field01737 == null || this.field01745 != mc.player) {
            this.field01745 = mc.player;
            this.field01737 = mc.player.getEyePos();
            this.field01738 = this.field01737;
            this.field01739 = mc.player.getYaw();
            this.field01740 = mc.player.getPitch();
         }

         this.field01738 = this.field01737;
         Vec3d vec3d = Vec3d.ZERO;
         if (mc.options.forwardKey.isPressed()) {
            vec3d = vec3d.add(Vec3d.fromPolar(0.0F, this.field01739));
         }

         if (mc.options.backKey.isPressed()) {
            vec3d = vec3d.subtract(Vec3d.fromPolar(0.0F, this.field01739));
         }

         if (mc.options.rightKey.isPressed()) {
            vec3d = vec3d.add(Vec3d.fromPolar(0.0F, this.field01739 + 90.0F));
         }

         if (mc.options.leftKey.isPressed()) {
            vec3d = vec3d.subtract(Vec3d.fromPolar(0.0F, this.field01739 + 90.0F));
         }

         if (mc.options.jumpKey.isPressed()) {
            vec3d = vec3d.add(0.0, 1.0, 0.0);
         }

         if (mc.options.sneakKey.isPressed()) {
            vec3d = vec3d.subtract(0.0, 1.0, 0.0);
         }

         if (vec3d.lengthSquared() > 0.0) {
            this.field01737 = this.field01737
               .add(vec3d.normalize().multiply(this.numberSetting.floatValue() * (mc.options.sprintKey.isPressed() ? 2.0F : 1.0F)));
         }
      }
   };
   @EventPriority(100)
   private final EventListener<HudRenderEvent> field01747 = hudRenderEvent -> {
      if (mc.player != null && this.field01737 != null) {
         FontText.method1397(
            FontFace.SEMI,
            String.format(Locale.US, "%.1f, %.1f, %.1f", this.field01737.x, this.field01737.y, this.field01737.z),
            hudRenderEvent.method4462() * GuiScale.method0797() / GuiScale.method0794() / 2.0F
               - FontText.method1396(
                     FontFace.SEMI, String.format(Locale.US, "%.1f, %.1f, %.1f", this.field01737.x, this.field01737.y, this.field01737.z), 5.5F
                  )
                  / 2.0F,
            hudRenderEvent.method4460() * GuiScale.method0797() / GuiScale.method0794() / 2.0F + 8.0F,
            5.5F,
            -419430401
         );
      }
   };

   @Override
   protected void onDisable() {
      if (this.field01737 != null) {
         this.field01741 = System.currentTimeMillis();
         this.field01742 = this.field01737;
         this.field01743 = this.field01739;
         this.field01744 = this.field01740;
      }
   }

   public Vec3d method0907(float local1210_1) {
      return this.isEnabled()
         ? this.field01738.lerp(this.field01737, local1210_1)
         : this.field01742.lerp(mc.player.getCameraPosVec(local1210_1), this.method0914());
   }

   public void method0908(double local1211_1, double local1211_3) {
      this.field01739 += (float)local1211_1 * 0.15F;
      this.field01740 = Math.max(-90.0F, Math.min(90.0F, this.field01740 + (float)local1211_3 * 0.15F));
   }

   @Override
   protected void onEnable() {
      this.field01741 = 0L;
      this.field01745 = mc.player;
      this.field01737 = mc.player == null ? null : mc.gameRenderer.getCamera().getCameraPos();
      this.field01738 = this.field01737;
      if (mc.player != null) {
         this.field01739 = mc.player.getYaw();
         this.field01740 = mc.player.getPitch();
      }
   }

   public boolean method0909() {
      return this.method0913() && this.method0907(1.0F).squaredDistanceTo(mc.player.getEyePos()) > 0.5625;
   }

   public static FreeCameraModule method0910() {
      if (freeCameraModule == null) {
         freeCameraModule = new FreeCameraModule();
      }

      return freeCameraModule;
   }

   private FreeCameraModule() {
      this.numberSetting = this.addSetting(
         new NumberSetting("Скорость", "Блоков за тик полёта камеры.").withRange(0.1F, 2.0F).withStep(0.1F).setValue(0.5F)
      );
   }

   public float method0912() {
      return this.isEnabled() ? this.field01740 : this.field01744 + (mc.player.getPitch() - this.field01744) * this.method0914();
   }

   public boolean method0913() {
      return mc.player != null && this.field01737 != null && (this.isEnabled() || System.currentTimeMillis() - this.field01741 < 350L);
   }

   private float method0914() {
      float local1219_1 = Math.min(1.0F, (float)(System.currentTimeMillis() - this.field01741) / 350.0F);
      return local1219_1 * local1219_1 * (3.0F - 2.0F * local1219_1);
   }

   public float method0915() {
      return this.isEnabled() ? this.field01739 : this.field01743 + MathHelper.wrapDegrees(mc.player.getYaw() - this.field01743) * this.method0914();
   }
}
