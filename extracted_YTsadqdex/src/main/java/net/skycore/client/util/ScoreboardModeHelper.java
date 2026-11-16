package net.skycore.client.util;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.ReadOnlyScoreInfo;
import net.minecraft.world.scores.Scoreboard;
import net.skycore.client.SkycoreClient;
import net.skycore.client.module.ClientModule;
import net.skycore.client.module.ModuleManager;
import net.skycore.client.module.misc.ScoreboardHPModule;

public final class ScoreboardModeHelper {
   public static float getLabelHealth(LivingEntity entity) {
      if (scoreboardMode()) {
         return getDisplayHealth(entity);
      } else {
         return entity.getHealth() + entity.getAbsorptionAmount();
      }
   }

   public static float getScoreboardHealth(LivingEntity entity) {
      if (entity instanceof Player player) {
         Minecraft instance = Minecraft.getInstance();
         if (instance.level != null) {
            Scoreboard scoreboard = instance.level.getScoreboard();
            Objective displayObjective = scoreboard.getDisplayObjective(DisplaySlot.BELOW_NAME);
            if (displayObjective != null) {
               ReadOnlyScoreInfo playerScoreInfo = scoreboard.getPlayerScoreInfo(player, displayObjective);
               if (playerScoreInfo != null) {
                  return playerScoreInfo.value();
               }
            }

            Object2IntMap lookup = scoreboard.listPlayerScores(player);
            if (!lookup.isEmpty()) {
               return lookup.values().iterator().nextInt();
            }
         }
      }

      return entity.getHealth() + entity.getAbsorptionAmount();
   }

   public static boolean scoreboardMode() {
      ModuleManager moduleManager = SkycoreClient.modules();
      var optional = moduleManager.get(ScoreboardHPModule.class).map(ClientModule::isEnabled);
      Boolean condition = false;
      return optional.orElse(condition);
   }

   private ScoreboardModeHelper() {
   }

   public static float getDisplayHealth(LivingEntity entity) {
      if (scoreboardMode()) {
         float scoreboardHealth = getScoreboardHealth(entity);
         float health = entity.getHealth();
         float absorptionAmount = health + entity.getAbsorptionAmount();
         double value = scoreboardHealth;
         double value2 = absorptionAmount;
         float result;
         if (value == Math.floor(value2)) {
            result = entity.getHealth();
         } else {
            result = Math.max(0.0F, scoreboardHealth);
         }

         return result;
      } else {
         return entity.getHealth();
      }
   }
}
