package net.skycore.client.event;

import net.minecraft.client.Minecraft;
import net.minecraft.client.input.MouseButtonInfo;

public record MouseButtonEvent(Minecraft client, MouseButtonInfo button, int action) {
   public int action() {
      return this.action;
   }

   public Minecraft client() {
      return this.client;
   }

   public boolean pressed() {
      return this.action == 1;
   }

   public boolean released() {
      return this.action == 0;
   }

   public MouseButtonInfo button() {
      return this.button;
   }
}
