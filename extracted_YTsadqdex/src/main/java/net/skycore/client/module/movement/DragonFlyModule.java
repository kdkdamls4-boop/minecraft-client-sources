package net.skycore.client.module.movement;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.skycore.client.module.ClientModule;
import net.skycore.client.module.ModuleCategory;

public final class DragonFlyModule extends ClientModule {
   private float savedSpeed = 0.05F;
   public static final DragonFlyModule INSTANCE = new DragonFlyModule();

   @Override
   protected void onDisable() {
      Minecraft instance = Minecraft.getInstance();
      if (instance.player != null) {
         Abilities abilities = Minecraft.getInstance().player.getAbilities();
         abilities.setFlyingSpeed(this.savedSpeed);
      }
   }

   @Override
   protected void onEnable() {
      Minecraft instance = Minecraft.getInstance();
      if (instance.player != null) {
         Minecraft instance2 = Minecraft.getInstance();
         this.savedSpeed = instance2.player.getAbilities().getFlyingSpeed();
      }
   }

   @Override
   protected void onTick() {
      Minecraft instance = Minecraft.getInstance();
      if (instance.player != null) {
         if (!instance.player.getAbilities().flying) {
            instance.player.getAbilities().setFlyingSpeed(this.savedSpeed);
         } else {
            instance.player.getAbilities().setFlyingSpeed(0.15F);
            Vec2 moveVector = instance.player.input.getMoveVector();
            int value = !instance.options.keyJump.isDown() && !instance.options.keyShift.isDown() ? 0 : 1;
            double value2 = value != 0 ? 0.7 : 1.09;
            double yRot = Math.toRadians(instance.player.getYRot());
            double x = (-Math.sin(yRot) * moveVector.y + Math.cos(yRot) * moveVector.x) * value2;
            double x2 = (Math.cos(yRot) * moveVector.y + Math.sin(yRot) * moveVector.x) * value2;
            double computedValue = instance.options.keyShift.isDown() ? -0.74 : (instance.options.keyJump.isDown() ? 0.74 : 0.0);
            instance.player.setDeltaMovement(new Vec3(x, computedValue, x2));
         }
      }
   }

   private DragonFlyModule() {
      super("Dragon Fly", "Faster flight with DRAGON (/fly)", ModuleCategory.MOVEMENT);
   }
}
