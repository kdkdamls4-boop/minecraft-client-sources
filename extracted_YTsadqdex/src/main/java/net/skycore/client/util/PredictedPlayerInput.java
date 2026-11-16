package net.skycore.client.util;

import net.minecraft.client.player.ClientInput;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public class PredictedPlayerInput extends ClientInput {
   public float forward;
   public Input buttons;
   public float sideways;
   public boolean forceSneaking = false;

   public static PredictedPlayerInput from(Input input) {
      return new PredictedPlayerInput(input);
   }

   public PredictedPlayerInput(Input buttons2) {
      this.buttons = buttons2;
   }

   public static PredictedPlayerInput infer(Player player) {
      Vec3 position = player.position();
      int value = 1;
      Vec3 position2 = new Vec3(player.xo, player.yo, player.zo);
      Vec3 position3 = position.subtract(position2);
      double value2 = position3.horizontalDistanceSqr();
      value = !player.onGround() ? 1 : 0;
      Input input = new Input(false, false, false, false, (value != 0), player.isShiftKeyDown(), value2 >= 0.014641);
      if (value2 > 0.0025000000000000005) {
         double yRot = VisibleHelper.relativeDegrees(position3, player.getYRot());
         double computedValue = Mth.wrapDegrees(yRot);
         input = VisibleHelper.directionalInput(input, computedValue, 20.0F);
      }

      return new PredictedPlayerInput(input);
   }

   @Override
   public void tick() {
      if (this.buttons.forward() != this.buttons.backward()) {
         this.forward = this.buttons.forward() ? 1.0F : -1.0F;
      } else {
         this.forward = 0.0F;
      }

      if (this.buttons.left() == this.buttons.right()) {
         this.sideways = 0.0F;
      } else {
         this.sideways = this.buttons.left() ? 1.0F : -1.0F;
      }

      if (this.buttons.shift()) {
         this.sideways *= 0.3F;
         this.forward *= 0.3F;
      }
   }
}
