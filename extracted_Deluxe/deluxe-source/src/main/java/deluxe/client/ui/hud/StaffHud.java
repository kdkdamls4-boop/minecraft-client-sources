package deluxe.client.ui.hud;

import deluxe.client.render.FontText;
import deluxe.client.render.ThemeColors;
import deluxe.client.ui.UiTheme;

import deluxe.client.util.Animation;

import deluxe.client.util.Easings;
import deluxe.client.render.FontFace;
import deluxe.client.render.Render2D;
import deluxe.client.storage.StaffStore;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.scoreboard.Team;
import net.minecraft.text.Text;
import net.minecraft.world.GameMode;

public final class StaffHud {
   public static final Pattern field07789 = Pattern.compile("^\\w{3,16}$");
   public static final Pattern field07790 = Pattern.compile(
      ".*(mod|der|adm|help|wne|мод|хелп|помо|адм|владе|отри|таф|taf|curat|курато|dev|раз|supp|сапп|yt|ютуб).*"
   );
   private static final String[][] field07791 = new String[][]{
      {"karandash", "Helper", "26:08", "ι"}, {"smdxyxz", "Tester", "08:56", "κ"}, {"Кирейко", "Moder", "10:34", "ι"}
   };
   private static final Map<String, StaffHud.Nested0425> field07792 = new LinkedHashMap<>();
   private static final HashMap<String, Long> field07793 = new HashMap<>();
   private static final List<StaffHud.Nested0425> field07794 = new ArrayList<>();
   private static final Animation field07795 = new Animation();
   private static final Animation field07796 = new Animation();
   private static final Animation field07797 = new Animation();
   private static final Animation field07798 = new Animation();
   private static float field07799;
   private static float field07800;
   private static long field07801;
   private static final IdentityHashMap<PlayerListEntry, StaffHud.Nested0424> field07802 = new IdentityHashMap<>();
   private static final HashSet<String> field07803 = new HashSet<>();
   private static Object field07804;
   private static long field07805;

   private static float method2227(StaffHud.Nested0425 local3482_0) {
      return local3482_0.field07885.isEmpty() ? 0.0F : FontText.method1396(FontFace.SEMI, local3482_0.field07885, 6.0F) + 3.0F;
   }

   private StaffHud() {
   }

   private static String method2229(PlayerListEntry playerListEntry, String local3485_1, Team newTeam) {
      if (!StaffStore.method4328(local3485_1) && !field07789.matcher(local3485_1).matches()) {
         return null;
      }

      String local3485_3 = newTeam == null ? "" : newTeam.getPrefix().getString().trim();
      String local3485_4 = !local3485_3.isEmpty()
         ? local3485_3
         : (playerListEntry.getDisplayName() == null ? "" : playerListEntry.getDisplayName().getString().replace(local3485_1, "").trim());
      return StaffStore.method4328(local3485_1)
            || StaffStore.method4325(local3485_4)
            || !local3485_4.isEmpty() && field07790.matcher(local3485_4.toLowerCase(Locale.ROOT)).matches()
         ? local3485_4
         : null;
   }

   static {
      field07796.resetTo(1.0);
   }

   private static void method2230(
      MinecraftClient minecraftClient, float local3487_1, float local3487_2, float local3487_3, StaffHud.Nested0425 local3487_4
   ) {
      float local3487_5 = local3487_4.visibilityAnimation.floatValue() * local3487_3;
      if (!(local3487_5 <= 0.001F)) {
         float local3487_6 = HudStyle.method4814(local3487_5);
         float local3487_7 = method2235(local3487_4);
         float local3487_8 = HudStyle.method4809(6.0F, local3487_7);
         float local3487_9 = method2231(local3487_4);
         float local3487_10 = local3487_1 - local3487_6;
         float local3487_11 = local3487_1 + field07798.floatValue() + 2.0F - local3487_6;
         local3487_9 = Math.max(local3487_9, field07797.floatValue() - field07798.floatValue() - 2.0F);
         float local3487_12 = HudStyle.method4816(local3487_2);
         HudStyle.method4821(local3487_11, local3487_2, local3487_9, 11.0F, local3487_5);
         String local3487_13 = method2238(minecraftClient, local3487_4.field07884);
         float local3487_14 = local3487_2 + 5.5F - 4.0F;
         int local3487_15 = UiTheme.method2877(-1, local3487_5);
         Render2D.method4677(local3487_13, local3487_11 + 4.0F, local3487_14, 8.0F, 8.0F, 3.5F, 0.25F, 0.125F, 0.125F, 0.25F, 0.25F, local3487_15);
         Render2D.method4677(local3487_13, local3487_11 + 4.0F, local3487_14, 8.0F, 8.0F, 3.5F, 0.25F, 0.625F, 0.125F, 0.75F, 0.25F, local3487_15);
         float local3487_16 = local3487_11 + 4.0F + 8.0F + 3.0F;
         if (local3487_4.field07886 != null) {
            FontText.method1400(
               FontFace.SEMI,
               local3487_4.field07886,
               local3487_16,
               local3487_12,
               6.0F,
               UiTheme.method2877(HudStyle.method4820(1.0F), local3487_5)
            );
         } else if (!local3487_4.field07885.isEmpty()) {
            FontText.method1397(
               FontFace.SEMI,
               local3487_4.field07885,
               local3487_16,
               local3487_12,
               6.0F,
               UiTheme.method2877(HudStyle.method4815(1.0F), local3487_5)
            );
         }

         FontText.method1397(
            FontFace.SEMI,
            local3487_4.field07884,
            local3487_16 + method2227(local3487_4),
            local3487_12,
            6.0F,
            UiTheme.method2877(HudStyle.method4820(1.0F), local3487_5)
         );
         HudStyle.method4821(local3487_10, local3487_2, local3487_8, 11.0F, local3487_5);
         float local3487_17 = local3487_10 + 2.0F + 6.0F + 1.0F;
         float local3487_18 = local3487_17 + 0.25F;
         HudStyle.method4810(local3487_17, local3487_2 + 5.5F, local3487_5, 3.0F);
         ThemeColors.method3961(
            local3487_4.field07888,
            local3487_10 + (local3487_18 - local3487_10 - 6.0F) / 2.0F + 3.0F,
            local3487_2 + 5.5F,
            6.0F,
            UiTheme.method2877(HudStyle.method4815(1.0F), local3487_5)
         );
         FontText.method1397(
            FontFace.SEMI,
            local3487_4.field07887,
            local3487_18 + (local3487_10 + local3487_8 - local3487_18 - local3487_7) / 2.0F,
            local3487_12,
            6.0F,
            UiTheme.method2877(HudStyle.method4820(1.0F), local3487_5)
         );
      }
   }

   private static float method2231(StaffHud.Nested0425 local3488_0) {
      return 15.0F + method2227(local3488_0) + FontText.method1396(FontFace.SEMI, local3488_0.field07884, 6.0F) + 4.0F;
   }

   private static void method2233(MinecraftClient minecraftClient) {
      field07794.clear();
      long local3490_1 = System.currentTimeMillis();
      if (local3490_1 - field07801 >= 250L) {
         method2236(minecraftClient);
         field07801 = local3490_1;
      }

      boolean local3490_3 = field07792.values().stream().anyMatch(local3484_0 -> local3484_0.visible && !local3484_0.field07889);
      if (!local3490_3 && HudRenderContext.method4342()) {
         for (String[] stringArray : field07791) {
            StaffHud.Nested0425 local3490_8 = field07792.computeIfAbsent(stringArray[0], StaffHud.Nested0425::new);
            local3490_8.field07885 = stringArray[1];
            local3490_8.field07886 = null;
            local3490_8.field07887 = stringArray[2];
            local3490_8.field07888 = stringArray[3];
            local3490_8.field07889 = true;
            local3490_8.visible = true;
         }
      }

      for (StaffHud.Nested0425 local3490_5 : field07792.values()) {
         if (local3490_5.visible || local3490_5.visibilityAnimation.floatValue() > 0.001) {
            field07794.add(local3490_5);
         }
      }

      field07792.entrySet()
         .removeIf(
            local3489_0 -> !local3489_0.getValue().visible
               && local3489_0.getValue().visibilityAnimation.floatValue() <= 0.001
               && local3489_0.getValue().visibilityAnimation.isFinished()
         );

      for (StaffHud.Nested0425 local3490_5 : field07794) {
         local3490_5.visibilityAnimation.update();
         local3490_5.visibilityAnimation.animateTo(local3490_5.visible ? 1.0 : 0.0, 0.6, Easings.EASE_OUT_QUART);
      }
   }

   public static float method2234(float local3491_0, float local3491_1) {
      MinecraftClient minecraftClient = MinecraftClient.getInstance();
      method2233(minecraftClient);
      float local3491_3 = HudStyle.method4823("Staffs");
      float local3491_4 = 0.0F;
      float local3491_5 = 0.0F;
      float local3491_6 = 13.0F;

      for (StaffHud.Nested0425 local3491_8 : field07794) {
         local3491_4 = Math.max(local3491_4, HudStyle.method4809(6.0F, method2235(local3491_8)));
         local3491_5 = Math.max(local3491_5, method2231(local3491_8));
         local3491_6 += 14.0F * local3491_8.visibilityAnimation.floatValue();
      }

      float local3491_7 = local3491_4 + (local3491_4 > 0.0F && local3491_5 > 0.0F ? 2.0F : 0.0F) + local3491_5;
      float local3491_8 = Math.max(local3491_3, Math.max(local3491_7, 1.0F));
      boolean local3491_9 = !field07794.isEmpty() || HudRenderContext.method4342();
      field07795.update();
      field07795.animateTo(local3491_9 ? 1.0 : 0.0, 0.4, Easings.EASE_OUT_QUART);
      field07796.update();
      if (!field07794.isEmpty()) {
         field07796.animateTo(Math.max(local3491_8, 1.0F), 0.6, Easings.EASE_OUT_QUART);
      }

      field07797.update();
      if (!field07794.isEmpty()) {
         field07797.animateTo(Math.max(local3491_8, 1.0F), 0.6, Easings.EASE_OUT_QUART);
      }

      field07798.update();
      if (!field07794.isEmpty()) {
         field07798.animateTo(Math.max(local3491_4, 0.0F), 0.6, Easings.EASE_OUT_QUART);
      }

      field07799 = field07796.floatValue();
      field07800 = Math.max(local3491_6, 1.0F);
      float local3491_10 = field07795.floatValue();
      if (local3491_10 <= 0.001F) {
         return 0.0F;
      }

      float[] floatArray = HudManager.method3390("staffs", local3491_0 - field07799, local3491_1);
      float local3491_12 = floatArray[0];
      float local3491_13 = floatArray[1];
      HudManager.method3418("staffs", local3491_12, local3491_13, field07799, field07800);
      HudRenderContext.method4341(local3491_12 + field07799 / 2.0F, local3491_13 + field07800 / 2.0F, local3491_10);
      HudStyle.method4824(local3491_12, local3491_13, Math.max(field07797.floatValue(), 1.0F), local3491_10, HudIcons.STAFF, "Staffs");
      float local3491_14 = local3491_13 + 15.0F;

      for (StaffHud.Nested0425 local3491_16 : field07794) {
         method2230(minecraftClient, local3491_12, local3491_14, local3491_10, local3491_16);
         local3491_14 += 13.0F * local3491_16.visibilityAnimation.floatValue();
      }

      HudRenderContext.method4337();
      return field07800 * local3491_10;
   }

   private static float method2235(StaffHud.Nested0425 local3492_0) {
      return FontText.method1396(FontFace.SEMI, local3492_0.field07887, 6.0F);
   }

   private static void method2236(MinecraftClient minecraftClient) {
      for (StaffHud.Nested0425 local3493_2 : field07792.values()) {
         local3493_2.visible = false;
      }

      if (minecraftClient.getNetworkHandler() != null && minecraftClient.world != null) {
         long local3493_1 = System.currentTimeMillis();
         if (minecraftClient.world != field07804 || local3493_1 - field07805 >= 5000L) {
            field07802.clear();
            field07804 = minecraftClient.world;
            field07805 = local3493_1;
         }

         field07803.clear();
         Collection<PlayerListEntry> playerList = minecraftClient.getNetworkHandler().getPlayerList();
         if (field07802.size() > playerList.size() * 2 + 16) {
            field07802.clear();
         }

         for (PlayerListEntry playerListEntry : playerList) {
            String local3493_6 = playerListEntry.getProfile().name();
            if (local3493_6 != null && !local3493_6.isEmpty()) {
               Team scoreHolderTeam = minecraftClient.world.getScoreboard().getScoreHolderTeam(local3493_6);
               Object local3493_8 = scoreHolderTeam == null ? null : scoreHolderTeam.getPrefix();
               Object displayName = playerListEntry.getDisplayName();
               StaffHud.Nested0424 local3493_10 = field07802.get(playerListEntry);
               if (local3493_10 == null
                  || local3493_10.team != scoreHolderTeam
                  || local3493_10.prefix != local3493_8
                  || local3493_10.display != displayName
                  || !local3493_6.equals(local3493_10.name)) {
                  local3493_10 = new StaffHud.Nested0424(
                     local3493_6, scoreHolderTeam, local3493_8, displayName, method2229(playerListEntry, local3493_6, scoreHolderTeam)
                  );
                  field07802.put(playerListEntry, local3493_10);
               }

               String local3493_11 = local3493_10.rankText;
               if (local3493_11 != null) {
                  field07803.add(local3493_6);
                  long local3493_12 = field07793.computeIfAbsent(local3493_6, local3494_0 -> System.currentTimeMillis());
                  StaffHud.Nested0425 local3493_14 = field07792.computeIfAbsent(local3493_6, StaffHud.Nested0425::new);
                  local3493_14.field07885 = local3493_11.isEmpty() ? "Staff" : local3493_11;
                  local3493_14.field07886 = scoreHolderTeam != null && !scoreHolderTeam.getPrefix().getString().trim().isEmpty() ? scoreHolderTeam.getPrefix() : null;
                  local3493_14.field07887 = String.format(
                     "%02d:%02d", (System.currentTimeMillis() - local3493_12) / 60000L, (System.currentTimeMillis() - local3493_12) / 1000L % 60L
                  );
                  local3493_14.field07888 = playerListEntry.getGameMode() == GameMode.SPECTATOR ? "ι" : "κ";
                  local3493_14.field07889 = false;
                  local3493_14.visible = true;
               }
            }
         }

         field07793.keySet().retainAll(field07803);
      } else {
         field07793.clear();
      }
   }

   private static String method2238(MinecraftClient minecraftClient, String local3495_1) {
      if (minecraftClient.getNetworkHandler() != null) {
         PlayerListEntry playerListEntry = minecraftClient.getNetworkHandler().getPlayerListEntry(local3495_1);
         if (playerListEntry != null) {
            return playerListEntry.getSkinTextures().body().texturePath().toString();
         }
      }

      UUID uUID = UUID.nameUUIDFromBytes(("OfflinePlayer:" + local3495_1).getBytes(StandardCharsets.UTF_8));
      return DefaultSkinHelper.getSkinTextures(uUID).body().texturePath().toString();
   }

   private record Nested0424(String name, Object team, Object prefix, Object display, String rankText) {
   }

   private static final class Nested0425 extends DraggableHudElement {
      final String field07884;
      String field07885 = "";
      Text field07886;
      String field07887 = "";
      String field07888 = "κ";
      boolean field07889;

      Nested0425(String local3505_1) {
         this.field07884 = local3505_1;
      }
   }
}
