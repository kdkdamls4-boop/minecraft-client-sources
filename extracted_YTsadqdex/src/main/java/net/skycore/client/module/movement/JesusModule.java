package net.skycore.client.module.movement;

import net.minecraft.client.Minecraft;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.skycore.client.module.ClientModule;
import net.skycore.client.module.ModuleCategory;
import net.skycore.client.util.SliderSetting;
import net.skycore.client.setting.ModeSetting;

public final class JesusModule extends ClientModule {
   private final SliderSetting speed;
   private final ModeSetting mode = this.add(new ModeSetting("Mode", "Custom", "Custom", "RW Jumps"));
   public static final JesusModule INSTANCE = new JesusModule();

   private JesusModule() {
      super("Jesus", "Allows movement across water", ModuleCategory.MOVEMENT);
      this.speed = this.add(new SliderSetting("Speed", 0.2, 0.05, 1.05, 0.01));
      this.speed.visibleWhen(() -> {
         return this.mode.is("Custom");
      });
   }

   @Override
   protected void onTick() {
      Minecraft instance = Minecraft.getInstance();
      if (instance.player != null && (instance.player.isInWater() || instance.player.isUnderWater())) {
         if (this.mode.is("RW Jumps")) {
            if (instance.options.keyJump.isDown()) {
               instance.player.setDeltaMovement(instance.player.getDeltaMovement().multiply(1.0, 0.0, 1.0).add(0.0, 0.13, 0.0));
            }
         } else {
            Vec2 moveVector = instance.player.input.getMoveVector();
            if (moveVector.lengthSquared() == 0.0F) {
               instance.player.setDeltaMovement(0.0, instance.player.getDeltaMovement().y, 0.0);
            } else {
               double computedValue = this.speed.get() * (instance.player.hasEffect(MobEffects.DOLPHINS_GRACE) ? 1.15 : 1.0);
               double yRot = Math.toRadians(instance.player.getYRot());
               double x = (-Math.sin(yRot) * moveVector.y + Math.cos(yRot) * moveVector.x) * computedValue;
               double x2 = (Math.cos(yRot) * moveVector.y + Math.sin(yRot) * moveVector.x) * computedValue;
               instance.player.setDeltaMovement(new Vec3(x, instance.options.keyJump.isDown() ? 0.1 : 0.02, x2));
            }
         }
      }
   }
}
