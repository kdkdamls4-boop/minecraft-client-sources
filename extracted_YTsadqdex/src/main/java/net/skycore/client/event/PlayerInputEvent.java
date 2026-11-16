package net.skycore.client.event;

import net.minecraft.client.player.KeyboardInput;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec2;

public final class PlayerInputEvent {
   private Input keyPresses;
   private final KeyboardInput keyboardInput;
   private Vec2 moveVector;

   public KeyboardInput keyboardInput() {
      return this.keyboardInput;
   }

   public void setSprint(boolean sprint) {
      Input keyPresses2 = this.keyPresses;
      boolean forward = keyPresses2.forward();
      boolean backward = keyPresses2.backward();
      boolean left = keyPresses2.left();
      boolean right = keyPresses2.right();
      boolean jump = keyPresses2.jump();
      boolean shift = keyPresses2.shift();
      Input input = new Input((boolean)forward, backward, left, right, jump, shift, sprint);
      forward = true;
      this.keyPresses = input;
   }

   public void setJump(boolean jump) {
      Input keyPresses2 = this.keyPresses;
      boolean forward = keyPresses2.forward();
      boolean backward = keyPresses2.backward();
      boolean left = keyPresses2.left();
      boolean right = keyPresses2.right();
      boolean shift = keyPresses2.shift();
      boolean sprint = keyPresses2.sprint();
      Input input = new Input((boolean)forward, backward, left, right, jump, shift, sprint);
      forward = true;
      this.keyPresses = input;
   }

   public void setMoveVector(Vec2 moveVector2) {
      this.moveVector = moveVector2 == null ? Vec2.ZERO : moveVector2;
   }

   public PlayerInputEvent(KeyboardInput keyboardInput2, Input keyPresses2, Vec2 moveVector2) {
      this.keyboardInput = keyboardInput2;
      this.keyPresses = keyPresses2 == null ? Input.EMPTY : keyPresses2;
      this.moveVector = moveVector2 == null ? Vec2.ZERO : moveVector2;
   }

   public void setShift(boolean shift) {
      Input keyPresses2 = this.keyPresses;
      boolean forward = keyPresses2.forward();
      boolean backward = keyPresses2.backward();
      boolean left = keyPresses2.left();
      boolean right = keyPresses2.right();
      boolean jump = keyPresses2.jump();
      boolean sprint = keyPresses2.sprint();
      this.keyPresses = new Input(forward, backward, left, right, jump, shift, sprint);
   }

   public void setKeyPresses(Input keyPresses2) {
      Input input = keyPresses2 == null ? Input.EMPTY : keyPresses2;
      this.keyPresses = input;
   }

   public Input keyPresses() {
      return this.keyPresses;
   }

   public void clearMovement(boolean condition, boolean condition2) {
      Input keyPresses2 = this.keyPresses;
      this.keyPresses = new Input(false, false, false, false, condition && keyPresses2.jump(), condition2 && keyPresses2.shift(), false);
      this.moveVector = Vec2.ZERO;
   }

   public Vec2 moveVector() {
      return this.moveVector;
   }
}
