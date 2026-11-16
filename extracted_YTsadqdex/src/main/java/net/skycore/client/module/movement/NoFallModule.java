package net.skycore.client.module.movement;

import net.skycore.client.module.ClientModule;
import net.skycore.client.module.ModuleCategory;

public final class NoFallModule extends ClientModule {
   public static final NoFallModule INSTANCE = new NoFallModule();

   public void newTick() {
   }

   @Override
   protected void onTick() {
      this.newTick();
   }

   private NoFallModule() {
      super("No Fall", "Prevents fall damage", ModuleCategory.MOVEMENT);
   }
}
