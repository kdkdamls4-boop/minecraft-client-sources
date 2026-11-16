package net.skycore.client.module.player;

import net.minecraft.client.Minecraft;
import net.skycore.client.combat.MiningStrategy;
import net.skycore.client.mixin.MultiPlayerGameModeEnsureHasSentCarriedItemAccessor;
import net.skycore.client.module.ClientModule;
import net.skycore.client.module.ModuleCategory;
import net.skycore.client.util.SliderSetting;
import net.skycore.client.setting.BooleanSetting;

public final class FastBreakModule extends ClientModule {
   private final SliderSetting speed;
   public static final FastBreakModule INSTANCE = new FastBreakModule();
   private final BooleanSetting noCooldown = this.add(new BooleanSetting("No Cooldown", true));

   private FastBreakModule() {
      super("Fast Break", "Breaks blocks faster", ModuleCategory.PLAYER);
      this.speed = this.add(new SliderSetting("Speed", 1.5, 1.0, 3.0, 0.1));
   }

   @Override
   protected void onTick() {
      if (!net.skycore.client.module.combat.CrystalAuraModule.INSTANCE.isEnabled() || !MiningStrategy.INSTANCE.hasTarget()) {
         Minecraft instance = Minecraft.getInstance();
         if (instance.gameMode != null) {
            MultiPlayerGameModeEnsureHasSentCarriedItemAccessor multiPlayerGameModeEnsureHasSentCarriedItemAccessor = (MultiPlayerGameModeEnsureHasSentCarriedItemAccessor)instance.gameMode;
            if (this.noCooldown.get()) {
               multiPlayerGameModeEnsureHasSentCarriedItemAccessor.skycore$setDestroyDelay(0);
            }

            float speed2 = multiPlayerGameModeEnsureHasSentCarriedItemAccessor.skycore$getDestroyProgress();
            if (speed2 > 0.0F && speed2 < 1.0F && this.speed.get() > 1.0) {
               multiPlayerGameModeEnsureHasSentCarriedItemAccessor.skycore$setDestroyProgress(Math.min(1.0F, speed2 + (this.speed.get().floatValue() - 1.0F) * 0.1F));
            }
         }
      }
   }
}
