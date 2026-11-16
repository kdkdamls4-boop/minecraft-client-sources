package net.skycore.client.util;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.FormattedCharSink;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.scores.PlayerTeam;

public final class ContainsRankSymbolHelper {
   private static boolean containsRankSymbol(Component text) {
      if (text == null) {
         return false;
      }

      boolean[] booleanValues = new boolean[]{false};
      FormattedCharSequence visualOrderText = text.getVisualOrderText();
      FormattedCharSink formattedCharSink = (value, style, value2) -> {
         if (ReplaceLiteralHelper.isRankCodePoint(value2)) {
            booleanValues[0] = true;
            return false;
         } else {
            String text2 = ReplaceLiteralHelper.replaceCodePoint(value2);
            if (text2 != null && text2.length() > 1) {
               booleanValues[0] = true;
               return false;
            } else {
               return true;
            }
         }
      };
      visualOrderText.accept(formattedCharSink);
      return booleanValues[0];
   }

   public static String resolvePlainName(LivingEntity entity) {
      Component text = resolveEntityDisplayName(entity);
      return ReplaceLiteralHelper.replaceSymbols(text.getString());
   }

   private ContainsRankSymbolHelper() {
   }

   public static Component resolveEntityDisplayName(LivingEntity entity) {
      Component text = resolveRawDisplayName(entity);
      text = ReplaceLiteralHelper.sanitizeDisplayText(text);
      if (entity instanceof Player player) {
         GameProfile gameProfile = player.getGameProfile();
         text = ReplaceLiteralHelper.truncateAfterPlayerName(text, gameProfile.name());
      }

      return ReplaceLiteralHelper.replaceSymbols(text);
   }

   private static Component resolveRawDisplayName(LivingEntity entity) {
      if (entity instanceof Player player) {
         Minecraft instance = Minecraft.getInstance();
         PlayerTeam team = player.getTeam();
         MutableComponent name = team != null ? PlayerTeam.formatNameForTeam(team, player.getName()) : null;
         if (instance.getConnection() != null) {
            PlayerInfo connection = instance.getConnection().getPlayerInfo(player.getUUID());
            if (connection == null) {
               connection = instance.getConnection().getPlayerInfo(player.getGameProfile().name());
            }

            if (connection != null && connection.getTabListDisplayName() != null) {
               Component tabListDisplayName = connection.getTabListDisplayName();
               if (name != null && !containsRankSymbol(tabListDisplayName) && containsRankSymbol(name)) {
                  return name;
               }

               return tabListDisplayName;
            }
         }

         if (name != null) {
            return name;
         }
      }

      return entity.getDisplayName();
   }
}
