package net.skycore.client.combat;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec2;
import net.skycore.client.event.PlayerInputEvent;
import net.skycore.client.util.NeedsPacketOrderSafeHelper;

public final class InputStop {
   private boolean active;
   private boolean sawIdleInput;

   public boolean readyAtTickEnd() {
      Minecraft instance = Minecraft.getInstance();
      if (this.active && !NeedsPacketOrderSafeHelper.needsSkycoreActionStop()) {
         return true;
      } else if (this.active && this.sawIdleInput && instance.player != null && instance.player.input != null) {
         Input input = instance.player.input.keyPresses;
         return input != null
            && !instance.player.isSprinting()
            && !input.forward()
            && !input.backward()
            && !input.left()
            && !input.right()
            && !input.jump()
            && !input.shift()
            && !input.sprint();
      } else {
         return false;
      }
   }

   public void keepStopped() {
      int value = 0;
      value = (byte)0;
      value = (byte)0;
      if (this.active) {
         if (NeedsPacketOrderSafeHelper.needsSkycoreActionStop()) {
            ActionStop.stop = true;
            value = Math.max(ActionStop.stopTicks, 1);
            ActionStop.stopTicks = value;
            return;
         }
      }
   }

   public void cancel() {
      this.active = false;
      this.sawIdleInput = false;
   }

   public void begin() {
      if (!this.active) {
         this.active = true;
         this.sawIdleInput = false;
         this.keepStopped();
      }
   }

   public void onInput(PlayerInputEvent playerInputEvent) {
      if (this.active) {
         if (NeedsPacketOrderSafeHelper.needsSkycoreActionStop()) {
            playerInputEvent.setMoveVector(Vec2.ZERO);
            playerInputEvent.setKeyPresses(Input.EMPTY);
            this.sawIdleInput = true;
            this.keepStopped();
            return;
         }
      }
   }
}
