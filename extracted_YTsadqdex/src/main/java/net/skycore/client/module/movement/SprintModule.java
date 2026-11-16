package net.skycore.client.module.movement;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.skycore.client.SkycoreClient;
import net.skycore.client.combat.ActionStop;
import net.skycore.client.event.PlayerTickEndEvent;
import net.skycore.client.module.ClientModule;
import net.skycore.client.module.ModuleCategory;
import net.skycore.client.module.combat.AttackAuraModule;
import net.skycore.client.util.SprintUseItemPolicy;

public final class SprintModule extends ClientModule {
   public static final SprintModule INSTANCE = new SprintModule();

   public boolean canSprint() {
      Minecraft instance = Minecraft.getInstance();
      LocalPlayer player = instance.player;
      if (player != null && !ActionStop.isStop()) {
         AttackAuraModule attackAuraModule = SkycoreClient.modules().get(AttackAuraModule.class).orElse(null);
         int value = attackAuraModule != null && attackAuraModule.isEnabled() && attackAuraModule.currentTarget() != null ? 1 : 0;
         int value2 = !player.isInWater() && !player.isSuppressingSlidingDownLadder() && !player.isInLava() ? 0 : 1;
         return SprintUseItemPolicy.allows((value != 0), player.onGround(), player.getAbilities().flying, (value2 != 0));
      } else {
         return false;
      }
   }

   public void preTick() {
      if (this.isEnabled()) {
         Minecraft instance = Minecraft.getInstance();
         if (instance.player != null) {
            if (instance.level != null) {
               if (instance.options != null) {
                  if (this.canSprint()) {
                     instance.options.keySprint.setDown(true);
                  } else {
                     instance.options.keySprint.setDown(false);
                     instance.player.setSprinting(false);
                  }

                  return;
               }
            }
         }
      }
   }

   private SprintModule() {
      super("Sprint", "Automatically enables sprint", ModuleCategory.MOVEMENT);
      SkycoreClient.events().subscribe(PlayerTickEndEvent.class, playerTickEndEvent -> {
         this.preTick();
      });
   }
}
