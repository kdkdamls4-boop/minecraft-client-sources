package net.skycore.client.module.movement;

import net.minecraft.client.Minecraft;
import net.skycore.client.SkycoreClient;
import net.skycore.client.module.ClientModule;
import net.skycore.client.module.ModuleCategory;
import net.skycore.client.module.ModuleManager;
import net.skycore.client.module.combat.AttackAuraModule;

public final class AutoJumpModule extends ClientModule {
   public static final AutoJumpModule INSTANCE = new AutoJumpModule();

   @Override
   protected void onTick() {
      Minecraft instance = Minecraft.getInstance();
      ModuleManager moduleManager = SkycoreClient.modules();
      AttackAuraModule attackAuraModule = moduleManager.get(AttackAuraModule.class).orElse(null);
      if (instance.player != null && attackAuraModule != null) {
         if (attackAuraModule.isEnabled()) {
            if (attackAuraModule.currentTarget() != null) {
               if (instance.player.onGround()) {
                  instance.player.jumpFromGround();
               }
            }
         }
      }
   }

   private AutoJumpModule() {
      super("Auto Jump", "Jumps when Aura acquires a target", ModuleCategory.MOVEMENT);
   }
}
