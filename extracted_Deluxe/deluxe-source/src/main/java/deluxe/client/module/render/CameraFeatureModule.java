package deluxe.client.module.render;

import deluxe.client.setting.BooleanSetting;
import deluxe.client.event.EventListener;
import deluxe.client.setting.KeybindSetting;
import deluxe.client.module.Module;
import deluxe.client.module.ModuleCategory;
import deluxe.client.module.ModuleInfo;
import deluxe.client.setting.NumberSetting;
import deluxe.client.event.PlayerTickEvent;
import deluxe.client.ui.AnimatedValue;
import deluxe.client.RotationManager;

import net.minecraft.client.option.Perspective;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;

@ModuleInfo(name = "CameraFeature", description = "Фрилук, зум и плавное переключение камеры на F5.", category = ModuleCategory.RENDER)
public final class CameraFeatureModule extends Module {
   private static CameraFeatureModule cameraFeatureModule;
   public KeybindSetting field01390;
   public KeybindSetting field01391;
   public NumberSetting numberSetting;
   public BooleanSetting booleanSetting;
   private boolean field01394;
   private boolean field01395;
   private float field01396;
   private float field01397;
   private final AnimatedValue field01398 = new AnimatedValue(280L);
   private final AnimatedValue field01399 = new AnimatedValue(240L);
   private final EventListener<PlayerTickEvent> field01400 = playerTickEvent -> {
      if (mc.player != null && mc.options != null) {
         boolean local1061_2 = this.field01390.isBound() && this.field01390.getBinding().method2749(mc.getWindow().getHandle());
         if (local1061_2 && !this.field01394) {
            this.field01396 = mc.player.getYaw();
            this.field01397 = mc.player.getPitch();
         }

         this.field01394 = local1061_2;
         this.field01395 = this.field01391.isBound() && this.field01391.getBinding().method2749(mc.getWindow().getHandle());
         this.field01398.method3869(this.field01394 ? 1.0 : 0.0);
         this.field01399.method3869(this.booleanSetting.isChecked() && mc.options.getPerspective() == Perspective.THIRD_PERSON_BACK ? 1.0 : 0.0);
      }
   };

   public void method0779(double local1046_1, double local1046_3) {
      this.field01396 += (float)local1046_1 * 0.15F;
      this.field01397 = Math.max(-90.0F, Math.min(90.0F, this.field01397 + (float)local1046_3 * 0.15F));
   }

   @Override
   protected void onEnable() {
      this.field01394 = false;
      this.field01395 = false;
      this.field01398.method3870(0.0);
      this.field01399.method3870(mc.options != null && mc.options.getPerspective() == Perspective.THIRD_PERSON_BACK ? 1.0 : 0.0);
   }

   public float method0780() {
      return this.isEnabled() ? (float)this.field01398.method3871() : 0.0F;
   }

   public boolean method0781() {
      return this.isEnabled()
         && mc.player != null
         && (
            this.method0788()
               || this.booleanSetting.isChecked()
                  && mc.options != null
                  && mc.options.getPerspective() == Perspective.THIRD_PERSON_BACK
                  && this.field01399.method3871() > 0.001
                  && this.field01399.method3871() < 0.999
         );
   }

   public static CameraFeatureModule method0782() {
      if (cameraFeatureModule == null) {
         cameraFeatureModule = new CameraFeatureModule();
      }

      return cameraFeatureModule;
   }

   public float method0783() {
      return this.method0788()
         ? this.field01397
            + ((RotationManager.method0658().method0667() ? RotationManager.method0658().method0655() : mc.player.getPitch()) - this.field01397)
               * (1.0F - (float)this.field01398.method3871())
         : (RotationManager.method0658().method0667() ? RotationManager.method0658().method0655() : mc.player.getPitch());
   }

   private float method0784() {
      return this.method0788() ? (float)this.field01398.method3871() : (float)this.field01399.method3871();
   }

   public float method0785() {
      return this.isEnabled() && this.field01395 ? 1.0F / this.numberSetting.floatValue() : -1.0F;
   }

   private double method0786(Vec3d vec3d, Vec3d vec3d2, double local1054_3) {
      if (mc.world == null) {
         return local1054_3;
      }

      double local1054_5 = local1054_3;

      for (int index = 0; index < 8; index++) {
         Vec3d vec3d3 = vec3d.add(
            (index & 1) == 0 ? -0.1 : 0.1, (index >> 1 & 1) == 0 ? -0.1 : 0.1, (index >> 2 & 1) == 0 ? -0.1 : 0.1
         );
         HitResult hitResult = mc.world
            .raycast(
               new RaycastContext(vec3d3, vec3d3.add(vec3d2.multiply(local1054_3)), ShapeType.VISUAL, FluidHandling.NONE, mc.player)
            );
         if (hitResult.getType() != Type.MISS) {
            local1054_5 = Math.min(local1054_5, hitResult.getPos().distanceTo(vec3d));
         }
      }

      return local1054_5;
   }

   private CameraFeatureModule() {
      this.field01390 = this.addSetting(new KeybindSetting("Обзор", "Свободный обзор не позволяющий сменить траекторию вашего игрока."));
      this.field01391 = this.addSetting(new KeybindSetting("Зум", "Зум который был вырезан в 1.16."));
      this.numberSetting = this.addSetting(
         new NumberSetting("Сила зума", "Во сколько раз приближать.").withRange(2.0F, 10.0F).withStep(0.5F).setValue(4.0F)
      );
      this.booleanSetting = this.addSetting(new BooleanSetting("Плавная смена вида", "Анимация переключения камеры по F5.", true));
   }

   @Override
   protected void onDisable() {
      this.field01394 = false;
      this.field01395 = false;
   }

   public float method0787() {
      return this.method0788()
         ? MathHelper.lerpAngleDegrees(
            1.0F - (float)this.field01398.method3871(),
            this.field01396,
            RotationManager.method0658().method0667() ? RotationManager.method0658().method0666() : mc.player.getYaw()
         )
         : (RotationManager.method0658().method0667() ? RotationManager.method0658().method0666() : mc.player.getYaw());
   }

   private boolean method0788() {
      return this.field01394 || this.field01398.method3871() > 0.001;
   }

   public boolean method0789(double local1059_1, double local1059_3) {
      if (this.isEnabled()
         && mc.player != null
         && this.field01390.isBound()
         && this.field01390.getBinding().method2749(mc.getWindow().getHandle())) {
         if (!this.field01394) {
            this.field01396 = mc.player.getYaw();
            this.field01397 = mc.player.getPitch();
            this.field01394 = true;
            this.field01398.method3869(1.0);
         }

         this.method0779(local1059_1, local1059_3);
         return true;
      } else {
         return false;
      }
   }

   public Vec3d method0790(float local1060_1) {
      Vec3d vec3d = Vec3d.fromPolar(this.method0783(), this.method0787()).multiply(-1.0);
      return mc.player
         .getCameraPosVec(local1060_1)
         .add(
            vec3d.multiply(
               this.method0786(
                  mc.player.getCameraPosVec(local1060_1),
                  vec3d,
                  4.0
                     * (
                        this.method0788()
                           ? Math.max(
                              this.field01398.method3871(),
                              mc.options != null && mc.options.getPerspective() != Perspective.FIRST_PERSON ? 1.0 : 0.0
                           )
                           : this.field01399.method3871()
                     )
               )
            )
         );
   }

   public boolean method0792() {
      return this.method0781() && this.method0784() > 0.15F;
   }

   public boolean method0793() {
      return this.isEnabled() && this.field01394;
   }
}
