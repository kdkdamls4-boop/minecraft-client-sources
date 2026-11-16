package deluxe.client.ui.hud;

import deluxe.client.render.FontText;
import deluxe.client.util.GuiScale;
import deluxe.client.render.ThemeColors;
import deluxe.client.ui.UiTheme;

import deluxe.client.util.Animation;

import deluxe.mixin.BossBarHudMixin;
import deluxe.client.util.Easings;
import deluxe.client.render.FontFace;
import deluxe.client.module.render.InterfaceModule;
import ru.dreamix.protection.api.impl.profile.ProfileAPI;
import deluxe.client.render.Render2D;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.client.MinecraftClient;

public final class WatermarkHud {
   private static final DateTimeFormatter field08529 = DateTimeFormatter.ofPattern("HH:mm");
   private static final List<WatermarkHud.Nested0435> field08530 = new ArrayList<>(
      List.of(
         new WatermarkHud.Nested0435("logo", null),
         new WatermarkHud.Nested0435("avatar", "Аватарка"),
         new WatermarkHud.Nested0435("user", "Логин"),
         new WatermarkHud.Nested0435("fps", "ФПС"),
         new WatermarkHud.Nested0435("ping", "Пинг"),
         new WatermarkHud.Nested0435("coords", "Координаты"),
         new WatermarkHud.Nested0435("time", "Время")
      )
   );
   private static final Animation field08531 = new Animation();
   private static final Animation field08532 = new Animation();
   private static final Animation field08533 = new Animation();
   private static float field08534;
   private static float field08535;
   private static float field08536;
   private static float field08537;
   private static WatermarkHud.Nested0435 field08538;
   private static float field08539;

   public static boolean method2500(double local3929_0, double local3929_2, int local3929_4) {
      if (local3929_4 == 0 && HudRenderContext.method4343()) {
         float[] floatArray = HudManager.method3420(local3929_0, local3929_2);
         WatermarkHud.Nested0435 local3929_6 = method2505(floatArray[0], floatArray[1]);
         if (local3929_6 == null) {
            return false;
         }

         field08538 = local3929_6;
         field08539 = floatArray[0] - local3929_6.field08620.floatValue();
         if (local3929_6.field08616.equals("logo")) {
            local3929_6.field08624 = System.currentTimeMillis();
         }

         return true;
      } else {
         return false;
      }
   }

   public static void method2501(MinecraftClient minecraftClient, float local3930_1, float local3930_2) {
      field08531.update();
      field08531.animateTo(1.0, 0.3, Easings.EASE_OUT_QUART);
      field08532.update();
      field08532.animateTo(minecraftClient.getCurrentFps(), 0.15, Easings.EASE_OUT_QUART);
      String name = ProfileAPI.get().getModel().getName();
      boolean local3930_4 = InterfaceModule.method2247().method2251() && HudManager.method3387() > 0.0F;
      if (field08538 == null) {
         String local3930_5 = "," + InterfaceModule.method2247().textSetting.getValue() + ",";
         field08530.sort(
            Comparator.comparingInt(
               local3932_1 -> local3930_5.contains("," + local3932_1.field08616 + ",")
                  ? local3930_5.indexOf("," + local3932_1.field08616 + ",")
                  : Integer.MAX_VALUE
            )
         );
      }

      int local3930_5 = minecraftClient.player != null
            && minecraftClient.getNetworkHandler() != null
            && minecraftClient.getNetworkHandler().getPlayerListEntry(minecraftClient.player.getUuid()) != null
         ? minecraftClient.getNetworkHandler().getPlayerListEntry(minecraftClient.player.getUuid()).getLatency()
         : 0;

      for (WatermarkHud.Nested0435 local3930_7 : field08530) {
         local3930_7.field08618 = local3930_7.field08617 == null || InterfaceModule.method2247().field07920.matchesOption(local3930_7.field08617);
         switch (local3930_7.field08616) {
            case "logo":
               local3930_7.method2509(null, null, 10.0F);
               break;
            case "avatar":
               local3930_7.method2509(null, null, 12.0F);
               break;
            case "user":
               local3930_7.method2509(name, null, 7.0F);
               break;
            case "fps":
               local3930_7.method2509(String.valueOf((int)field08532.floatValue()), "FPS", 7.0F);
               break;
            case "ping":
               local3930_7.method2509(String.valueOf(local3930_5), "ms", 7.0F);
               break;
            case "coords":
               local3930_7.method2509(
                  minecraftClient.player == null
                     ? "0 0 0"
                     : minecraftClient.player.getBlockX() + " " + minecraftClient.player.getBlockY() + " " + minecraftClient.player.getBlockZ(),
                  null,
                  7.0F
               );
               break;
            case "time":
               local3930_7.method2509(LocalTime.now().format(field08529), null, 7.0F);
         }

         local3930_7.field08621.update();
         local3930_7.field08621
            .animateTo(
               local3930_7.field08616.equals("fps")
                  ? method2507(field08532.floatValue(), 60.0F, 25.0F)
                  : (local3930_7.field08616.equals("ping") ? method2507(local3930_5, 120.0F, 300.0F) : 0.0),
               0.5,
               Easings.EASE_OUT_QUART
            );
         local3930_7.field08619.update();
         local3930_7.field08619.animateTo(local3930_7.method2513(), 0.05, Easings.EASE_OUT_EXPO);
      }

      float local3930_6 = 0.0F;

      for (WatermarkHud.Nested0435 local3930_8 : field08530) {
         if (!(local3930_8.method2510() < 1.0F)) {
            if (local3930_6 > 0.0F) {
               local3930_6 += 4.0F;
            }

            local3930_6 += local3930_8.method2510();
         }
      }

      field08537 = Math.max(local3930_6 + 8.0F, 1.0F);
      float local3930_7 = GuiScale.method0797() / GuiScale.method0794();
      if (local3930_4) {
         local3930_1 = HudManager.method3387() / 2.0F - field08537 / 2.0F;
         local3930_2 = 5.0F;
      } else {
         float[] floatArray = HudManager.method3390("watermark", local3930_1, local3930_2);
         local3930_1 = floatArray[0];
         local3930_2 = floatArray[1];
      }

      int local3930_8 = minecraftClient.inGameHud != null ? ((BossBarHudMixin)minecraftClient.inGameHud.getBossBarHud()).getEvents().size() : 0;
      float targetValue = 0.0F;
      if (local3930_8 > 0) {
         float local3930_10 = (12.0F + (local3930_8 - 1) * 19.0F + 8.0F + 1.5F) * local3930_7;
         float local3930_11 = HudManager.method3387() / 2.0F;
         if (local3930_1 < local3930_11 + 91.0F * local3930_7 && local3930_1 + field08537 > local3930_11 - 91.0F * local3930_7 && local3930_2 < local3930_10) {
            targetValue = local3930_10 - local3930_2;
         }
      }

      field08533.update();
      field08533.animateTo(targetValue, 0.2, Easings.EASE_OUT_QUAD);
      local3930_2 += field08533.floatValue();
      field08535 = local3930_1;
      field08536 = local3930_2;
      HudManager.method3418("watermark", local3930_1, local3930_2, field08537, 15.0F);
      field08534 = HudManager.method3389() > 0.0F ? (local3930_2 + 15.0F) / HudManager.method3389() : 0.0F;
      float local3930_10 = field08531.floatValue();
      if (!(local3930_10 <= 0.001F)) {
         boolean local3930_11 = HudRenderContext.method4343();
         float local3930_12 = (float)(minecraftClient.mouse.getScaledX(minecraftClient.getWindow()) * local3930_7);
         float local3930_13 = (float)(minecraftClient.mouse.getScaledY(minecraftClient.getWindow()) * local3930_7);
         if (field08538 != null) {
            method2508(local3930_12);
         }

         float local3930_14 = local3930_1 + 4.0F;

         for (WatermarkHud.Nested0435 local3930_16 : field08530) {
            local3930_16.field08620.update();
            float value = local3930_16 == field08538
               ? Math.max(local3930_1 + 4.0F, Math.min(local3930_12 - field08539, local3930_1 + field08537 - 4.0F - local3930_16.method2510()))
               : local3930_14;
            if (!local3930_16.field08623) {
               local3930_16.field08623 = true;
               local3930_16.field08620.resetTo(value);
            } else if (local3930_16 == field08538) {
               local3930_16.field08620.resetTo(value);
            } else {
               local3930_16.field08620.animateTo(value, 0.15, Easings.EASE_OUT_EXPO);
            }

            if (local3930_16.method2510() >= 1.0F) {
               local3930_14 += local3930_16.method2510() + 4.0F;
            }
         }

         float local3930_15 = Float.MAX_VALUE;
         float local3930_16 = -Float.MAX_VALUE;

         for (WatermarkHud.Nested0435 local3930_18 : field08530) {
            if (!(local3930_18.method2510() < 1.0F)) {
               local3930_15 = Math.min(local3930_15, local3930_18.field08620.floatValue());
               local3930_16 = Math.max(local3930_16, local3930_18.field08620.floatValue() + local3930_18.method2510());
            }
         }

         float local3930_17 = local3930_15 == Float.MAX_VALUE ? local3930_1 : local3930_15 - 4.0F;
         float local3930_18 = local3930_15 == Float.MAX_VALUE ? field08537 : local3930_16 - local3930_15 + 8.0F;
         HudStyle.method4817(local3930_17, local3930_2, local3930_18, 15.0F, local3930_10, 6.75F);
         WatermarkHud.Nested0435 local3930_19 = local3930_11 ? (field08538 != null ? field08538 : method2505(local3930_12, local3930_13)) : null;
         float local3930_20 = local3930_2 + 7.5F;

         for (WatermarkHud.Nested0435 local3930_22 : field08530) {
            local3930_22.field08622.update();
            local3930_22.field08622.animateTo(local3930_22 == local3930_19 ? 1.0 : 0.0, 0.2, Easings.EASE_OUT_QUART);
            if (local3930_22 != field08538 && local3930_22.field08618 && local3930_22.method2510() >= 1.0F) {
               local3930_22.method2511(local3930_2, local3930_20, local3930_10);
            }
         }

         if (field08538 != null && field08538.field08618 && field08538.method2510() >= 1.0F) {
            field08538.method2511(local3930_2, local3930_20, local3930_10);
         }
      }
   }

   private static int method2502(int local3931_0, float local3931_1) {
      int local3931_2 = UiTheme.method2881(local3931_0, -42406, local3931_1);
      return UiTheme.method2881(local3931_2, -55256, ThemeColors.method3965((local3931_1 - 0.7F) / 0.3F));
   }

   public static float method2504() {
      return field08534;
   }

   private static WatermarkHud.Nested0435 method2505(float local3934_0, float local3934_1) {
      if (!(local3934_1 < field08536 + 3.5F) && !(local3934_1 > field08536 + 15.0F - 3.5F)) {
         for (WatermarkHud.Nested0435 local3934_3 : field08530) {
            float local3934_4 = local3934_3.method2510();
            float local3934_5 = Math.min(local3934_4 * 0.25F, 3.0F);
            if (local3934_4 >= 1.0F
               && local3934_0 >= local3934_3.field08620.floatValue() + local3934_5
               && local3934_0 <= local3934_3.field08620.floatValue() + local3934_4 - local3934_5) {
               return local3934_3;
            }
         }

         return null;
      } else {
         return null;
      }
   }

   public static void method2506() {
      if (field08538 != null) {
         field08538 = null;
         StringBuilder stringBuilder = new StringBuilder();

         for (WatermarkHud.Nested0435 local3936_2 : field08530) {
            stringBuilder.append(stringBuilder.isEmpty() ? "" : ",").append(local3936_2.field08616);
         }

         InterfaceModule.method2247().textSetting.setValue(stringBuilder.toString());
      }
   }

   private static float method2507(float local3937_0, float local3937_1, float local3937_2) {
      return local3937_2 > local3937_1
         ? ThemeColors.method3965((local3937_0 - local3937_1) / (local3937_2 - local3937_1))
         : ThemeColors.method3965((local3937_1 - local3937_0) / (local3937_1 - local3937_2));
   }

   private static void method2508(float local3938_0) {
      float local3938_1 = local3938_0 - field08539 + field08538.method2510() / 2.0F;
      field08530.remove(field08538);
      float local3938_2 = field08535 + 4.0F;
      int local3938_3 = 0;

      for (int index = 0; index < field08530.size(); index++) {
         float local3938_5 = field08530.get(index).method2510();
         if (!(local3938_5 < 1.0F)) {
            if (local3938_1 > local3938_2 + local3938_5 / 2.0F) {
               local3938_3 = index + 1;
            }

            local3938_2 += local3938_5 + 4.0F;
         }
      }

      field08530.add(local3938_3, field08538);
   }

   private static final class Nested0435 {
      final String field08616;
      final String field08617;
      boolean field08618 = true;
      final Animation field08619 = new Animation();
      final Animation field08620 = new Animation();
      final Animation field08621 = new Animation();
      final Animation field08622 = new Animation();
      boolean field08623;
      long field08624;
      String field08625;
      String field08626;
      float field08627;

      void method2509(String local3939_1, String local3939_2, float local3939_3) {
         this.field08625 = local3939_1;
         this.field08626 = local3939_2;
         this.field08627 = local3939_3;
      }

      Nested0435(String local3940_1, String local3940_2) {
         this.field08616 = local3940_1;
         this.field08617 = local3940_2;
      }

      float method2510() {
         return Math.round(this.field08619.floatValue());
      }

      void method2511(float local3942_1, float local3942_2, float local3942_3) {
         float local3942_4 = Math.round((this.field08620.floatValue() + this.method2512()) * 2.0F) / 2.0F;
         float local3942_5 = this.field08622.floatValue();
         if (local3942_5 > 0.001F) {
            Render2D.method4715(
               local3942_4 - 3.0F,
               local3942_1 + 1.0F,
               this.method2510() + 6.0F,
               13.0F,
               5.75F,
               UiTheme.method2877(-1, local3942_5 * 0.12F * local3942_3)
            );
         }

         float local3942_6 = this.field08621.floatValue();
         int local3942_7 = UiTheme.method2877(WatermarkHud.method2502(ThemeColors.field13669, local3942_6), local3942_3);
         int local3942_8 = UiTheme.method2877(WatermarkHud.method2502(HudStyle.method4820(1.0F), local3942_6), local3942_3);
         float local3942_9 = local3942_2 - this.field08627 / 2.0F;
         switch (this.field08616) {
            case "logo":
               ThemeColors.method3961("b", local3942_4 + this.field08627 / 2.0F, local3942_2, this.field08627, local3942_7);
               break;
            case "avatar":
               Render2D.method4777(
                  "assets/deluxe/textures/images/avatar.png",
                  local3942_4,
                  local3942_9,
                  this.field08627,
                  this.field08627,
                  this.field08627 / 2.0F,
                  UiTheme.method2877(-1, local3942_3)
               );
               break;
            case "user":
               HudIcons.USER.method4010(local3942_4, local3942_9, this.field08627, local3942_7);
               break;
            case "fps":
               HudIcons.FPS.method4010(local3942_4, local3942_9, this.field08627, local3942_7);
               break;
            case "ping":
               HudIcons.PING.method4010(local3942_4, local3942_9, this.field08627, local3942_7);
               break;
            case "coords":
               HudIcons.COORDS.method4010(local3942_4, local3942_9, this.field08627, local3942_7);
               break;
            default:
               HudIcons.TIME.method4010(local3942_4, local3942_9, this.field08627, local3942_7);
         }

         if (this.field08625 != null && !this.field08625.isEmpty()) {
            float local3942_10 = Math.round((local3942_4 + this.field08627 + 2.5F) * 2.0F) / 2.0F;
            FontText.method1397(FontFace.SEMI, this.field08625, local3942_10, local3942_2 - 3.5F, 6.0F, local3942_8);
            if (this.field08626 != null && !this.field08626.isEmpty()) {
               float local3942_11 = Math.round((local3942_10 + FontText.method1396(FontFace.SEMI, this.field08625, 6.0F) + 2.0F) * 2.0F)
                  / 2.0F;
               FontText.method1397(FontFace.SEMI, this.field08626, local3942_11, local3942_2 - 3.5F, 6.0F, local3942_8);
            }
         }
      }

      float method2512() {
         if (this.field08624 == 0L) {
            return 0.0F;
         } else {
            long local3943_1 = System.currentTimeMillis() - this.field08624;
            if (local3943_1 >= 360L) {
               this.field08624 = 0L;
               return 0.0F;
            } else {
               return (float)Math.sin((float)local3943_1 / 1000.0F * 70.0F) * 2.5F * (1.0F - (float)local3943_1 / 360.0F);
            }
         }
      }

      float method2513() {
         if (!this.field08618) {
            return 0.0F;
         }

         if (this.field08625 != null && !this.field08625.isEmpty()) {
            float local3944_1 = this.field08627 + 2.5F + FontText.method1396(FontFace.SEMI, this.field08625, 6.0F);
            if (this.field08626 != null && !this.field08626.isEmpty()) {
               local3944_1 += 2.0F + FontText.method1396(FontFace.SEMI, this.field08626, 6.0F);
            }

            return local3944_1;
         } else {
            return this.field08627;
         }
      }
   }
}
