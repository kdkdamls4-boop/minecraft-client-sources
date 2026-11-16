package net.skycore.client.event;

import net.minecraft.client.Minecraft;

public record KeyEvent(Minecraft client, net.minecraft.client.input.KeyEvent key, int action) {
   public boolean repeated() {
      return this.action == 2;
   }

   public boolean released() {
      return this.action == 0;
   }

   public boolean pressed() {
      return this.action == 1;
   }

   public int action() {
      return this.action;
   }

   public net.minecraft.client.input.KeyEvent key() {
      return this.key;
   }

   public Minecraft client() {
      return this.client;
   }
}
