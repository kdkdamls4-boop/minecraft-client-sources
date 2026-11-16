package deluxe.client.ui.hud;

import deluxe.client.ui.AnimatedValue;
import deluxe.client.render.ArcColorSpan;
import deluxe.client.storage.ConfigStore;
import deluxe.client.render.FontText;
import deluxe.client.util.GuiScale;
import deluxe.client.ui.UiTheme;

import deluxe.client.render.FontFace;
import deluxe.client.render.Render2D;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;

public final class HudManager {
   private static final Map<String, float[]> field11882 = new HashMap<>();
   private static final Map<String, float[]> field11883 = new HashMap<>();
   private static final Map<String, HudManager.Nested0487> field11884 = new LinkedHashMap<>();
   private static final Map<String, float[]> field11885 = new HashMap<>();
   private static final Map<String, float[]> field11886 = new HashMap<>();
   private static final Map<String, Long> field11887 = new HashMap<>();
   private static final Map<String, Float> field11888 = new HashMap<>();
   private static final Map<String, float[]> field11889 = new HashMap<>();
   private static final AnimatedValue field11890 = new AnimatedValue(160L).method3870(0.0);
   private static final AnimatedValue field11891 = new AnimatedValue(160L).method3870(0.0);
   private static final Map<String, AnimatedValue> field11892 = new HashMap<>();
   private static final Map<String, AnimatedValue> field11893 = new HashMap<>();
   private static final AnimatedValue field11894 = new AnimatedValue(160L).method3870(0.0);
   private static float[] field11895;
   private static float[] field11896;
   private static String field11897;
   private static final List<String> field11898 = new ArrayList<>(
      List.of("watermark", "keybinds", "serverbinds", "potions", "cooldowns", "staffs", "target", "inventory", "Target", "arrows")
   );
   private static String field11899;
   private static String field11900;
   private static float field11901;
   private static float field11902;
   private static float field11903;
   private static float field11904;
   private static float field11905;
   private static float field11906;
   private static float field11907;
   private static float field11908;
   private static float field11909;
   private static boolean field11910;
   private static Path field11911;

   public static List<String> method3381() {
      return field11898;
   }

   public static boolean method3382(String local5539_0) {
      return method3385(local5539_0)[3] > 0.5F;
   }

   public static String method3383(float local5540_0, float local5540_1) {
      List<Entry<String, HudManager.Nested0487>> local5540_2 = new ArrayList<>(field11884.entrySet());

      for (int index = local5540_2.size() - 1; index >= 0; index--) {
         HudManager.Nested0487 value = local5540_2.get(index).getValue();
         if (local5540_0 >= value.field12014
            && local5540_0 <= value.field12014 + value.field12016
            && local5540_1 >= value.field12015
            && local5540_1 <= value.field12015 + value.field12017) {
            return local5540_2.get(index).getKey();
         }
      }

      return null;
   }

   public static boolean method3384(String local5541_0) {
      return false;
   }

   private static float[] method3385(String local5542_0) {
      method3402();
      return field11883.computeIfAbsent(local5542_0, local5557_0 -> new float[]{0.9F, 1.0F, 0.0F, 0.0F});
   }

   public static void method3386(String local5543_0, boolean local5543_1) {
      method3385(local5543_0)[3] = local5543_1 ? 1.0F : 0.0F;
      if (local5543_1) {
         field11888.put(local5543_0, field11886.containsKey(local5543_0) ? field11886.get(local5543_0)[0] : field11908 / 2.0F);
      } else {
         field11888.remove(local5543_0);
      }

      method3410();
   }

   public static float method3387() {
      return field11908;
   }

   public static float[] method3388(String local5545_0) {
      HudManager.Nested0487 local5545_1 = field11884.get(local5545_0);
      return local5545_1 == null ? null : new float[]{local5545_1.field12014, local5545_1.field12015, local5545_1.field12016, local5545_1.field12017};
   }

   public static float method3389() {
      return field11909;
   }

   public static float[] method3390(String local5547_0, float local5547_1, float local5547_2) {
      method3402();
      float[] floatArray = field11882.get(local5547_0);
      float local5547_4 = floatArray != null ? floatArray[0] : local5547_1;
      float local5547_5 = floatArray != null ? floatArray[1] : local5547_2;
      if (!(field11908 <= 0.0F) && !(field11909 <= 0.0F)) {
         float[] floatArray2 = field11885.get(local5547_0);
         float local5547_7 = floatArray2 != null ? floatArray2[0] : 0.0F;
         float local5547_8 = floatArray2 != null ? floatArray2[1] : 0.0F;
         float local5547_9 = floatArray2 != null ? floatArray2[2] : 0.0F;
         float local5547_10 = floatArray2 != null ? floatArray2[3] : 0.0F;
         if (method3382(local5547_0) && floatArray2 != null && field11888.containsKey(local5547_0)) {
            local5547_4 = field11888.get(local5547_0) + local5547_9 - local5547_7 / 2.0F;
         }

         float local5547_11 = Math.max(5.0F, Math.min(Math.max(5.0F, field11908 - local5547_7 - 5.0F), local5547_4 - local5547_9));
         float local5547_12 = Math.max(5.0F, Math.min(Math.max(5.0F, field11909 - local5547_8 - 5.0F), local5547_5 - local5547_10));
         if (floatArray2 != null) {
            local5547_12 = method3397(local5547_0, local5547_11, local5547_12, local5547_7, local5547_8);
         }

         return new float[]{local5547_11 + local5547_9, local5547_12 + local5547_10};
      } else {
         return new float[]{local5547_4, local5547_5};
      }
   }

   public static void method3392(String local5549_0, boolean local5549_1) {
      method3385(local5549_0)[2] = local5549_1 ? 1.0F : 0.0F;
      method3410();
   }

   public static void method3393(String local5550_0, float local5550_1) {
      method3385(local5550_0)[0] = Math.max(0.25F, Math.min(2.0F, local5550_1));
   }

   public static float[] method3394(String local5552_0) {
      return field11886.get(local5552_0);
   }

   private static void method3395(JsonObject jsonObject) {
      for (String local5553_2 : jsonObject.keySet()) {
         JsonObject asJsonObject = jsonObject.getAsJsonObject(local5553_2);
         if (asJsonObject.has("x") && asJsonObject.has("y")) {
            field11882.put(local5553_2, new float[]{asJsonObject.get("x").getAsFloat(), asJsonObject.get("y").getAsFloat()});
         }

         if (asJsonObject.has("scale") || asJsonObject.has("auto") || asJsonObject.has("hideF1") || asJsonObject.has("pin")) {
            float[] floatArray = field11883.computeIfAbsent(local5553_2, local5548_0 -> new float[]{0.9F, 1.0F, 0.0F, 0.0F});
            if (asJsonObject.has("scale")) {
               floatArray[0] = asJsonObject.get("scale").getAsFloat();
            }

            if (asJsonObject.has("auto")) {
               floatArray[1] = asJsonObject.get("auto").getAsBoolean() ? 1.0F : 0.0F;
            }

            if (asJsonObject.has("hideF1")) {
               floatArray[2] = asJsonObject.get("hideF1").getAsBoolean() ? 1.0F : 0.0F;
            }

            if (asJsonObject.has("pin")) {
               floatArray[3] = asJsonObject.get("pin").getAsBoolean() ? 1.0F : 0.0F;
            }
         }
      }
   }

   public static void method3396(int local5554_0) {
      if (local5554_0 == 0 && field11899 != null) {
         method3410();
         field11899 = null;
      }

      if (local5554_0 == 0 && field11900 != null) {
         method3410();
         field11900 = null;
      }
   }

   private static float method3397(String local5555_0, float local5555_1, float local5555_2, float local5555_3, float local5555_4) {
      for (Entry<String, float[]> local5555_6 : field11889.entrySet()) {
         if (!local5555_6.getKey().equals(local5555_0) && (!method3382(local5555_0) || method3408(local5555_6.getKey()) < method3408(local5555_0))) {
            float[] value = local5555_6.getValue();
            if (local5555_1 < value[0] + value[2]
               && local5555_1 + local5555_3 > value[0]
               && local5555_2 < value[1] + value[3]
               && local5555_2 + local5555_4 > value[1]) {
               local5555_2 = value[1] + value[3] + 4.0F;
            }
         }
      }

      return Math.max(5.0F, Math.min(Math.max(5.0F, field11909 - local5555_4 - 5.0F), local5555_2));
   }

   private HudManager() {
   }

   public static void method3399(
      String local5558_0, float local5558_1, float local5558_2, float local5558_3, float local5558_4, float local5558_5, float local5558_6
   ) {
      float local5558_7 = method3407(local5558_0);
      float local5558_8 = local5558_1 + local5558_3 / 2.0F;
      float local5558_9 = local5558_2 + local5558_4 / 2.0F;
      HudManager.Nested0487 local5558_10 = new HudManager.Nested0487();
      local5558_10.field12014 = local5558_8 - local5558_3 * local5558_7 / 2.0F;
      local5558_10.field12015 = local5558_9 - local5558_4 * local5558_7 / 2.0F;
      local5558_10.field12016 = local5558_3 * local5558_7;
      local5558_10.field12017 = local5558_4 * local5558_7;
      local5558_10.field12018 = local5558_5;
      local5558_10.field12019 = local5558_6;
      field11884.put(local5558_0, local5558_10);
      field11885.put(
         local5558_0,
         new float[]{local5558_10.field12016, local5558_10.field12017, local5558_5 - local5558_10.field12014, local5558_6 - local5558_10.field12015}
      );
      field11886.put(local5558_0, new float[]{local5558_8, local5558_9});
      if (method3382(local5558_0)) {
         field11889.put(local5558_0, new float[]{local5558_10.field12014, local5558_10.field12015, local5558_10.field12016, local5558_10.field12017});
         field11888.putIfAbsent(local5558_0, local5558_8);
      } else {
         field11889.remove(local5558_0);
      }
   }

   public static boolean method3400() {
      return field11899 != null;
   }

   public static boolean method3401(double local5560_0, double local5560_2, int local5560_4) {
      if (local5560_4 != 0) {
         return false;
      }

      float local5560_5 = method3415();
      float local5560_6 = (float)(local5560_0 * local5560_5);
      float local5560_7 = (float)(local5560_2 * local5560_5);
      String local5560_8 = method3405(local5560_6, local5560_7);
      if (local5560_8 != null) {
         HudManager.Nested0487 local5560_9 = field11884.get(local5560_8);
         field11900 = local5560_8;
         field11903 = local5560_6;
         field11904 = local5560_7;
         field11905 = Math.max(0.25F, Math.min(2.0F, method3411(local5560_8)));
         field11906 = Math.max(1.0F, local5560_9.field12016 / method3407(local5560_8) / 2.0F);
         field11907 = Math.max(1.0F, local5560_9.field12017 / method3407(local5560_8) / 2.0F);
         if (field11898.remove(field11900)) {
            field11898.add(field11900);
         }

         return true;
      } else {
         String local5560_9 = method3383(local5560_6, local5560_7);
         if (local5560_9 == null) {
            return false;
         }

         HudManager.Nested0487 local5560_10 = field11884.get(local5560_9);
         field11899 = local5560_9;
         field11901 = local5560_6 - local5560_10.field12018;
         field11902 = local5560_7 - local5560_10.field12019;
         if (field11898.remove(field11899)) {
            field11898.add(field11899);
         }

         return true;
      }
   }

   private static void method3402() {
      if (!field11910) {
         field11910 = true;
         field11911 = FabricLoader.getInstance().getConfigDir().resolve("deluxe").resolve("draggables.json");

         try {
            if (!Files.exists(field11911)) {
               return;
            }

            method3395(JsonParser.parseString(Files.readString(field11911, StandardCharsets.UTF_8)).getAsJsonObject());
         } catch (Exception exception) {
         }
      }
   }

   public static JsonObject method3403() {
      method3402();
      JsonObject jsonObject = new JsonObject();
      Set<String> local5562_1 = new LinkedHashSet<>(field11882.keySet());
      local5562_1.addAll(field11883.keySet());

      for (String local5562_3 : local5562_1) {
         JsonObject jsonObject2 = new JsonObject();
         float[] floatArray = field11882.get(local5562_3);
         if (floatArray != null) {
            jsonObject2.addProperty("x", floatArray[0]);
            jsonObject2.addProperty("y", floatArray[1]);
         }

         float[] floatArray2 = field11883.get(local5562_3);
         if (floatArray2 != null) {
            jsonObject2.addProperty("scale", floatArray2[0]);
            jsonObject2.addProperty("auto", floatArray2[1] > 0.5F);
            jsonObject2.addProperty("hideF1", floatArray2[2] > 0.5F);
            jsonObject2.addProperty("pin", floatArray2.length > 3 && floatArray2[3] > 0.5F);
         }

         jsonObject.add(local5562_3, jsonObject2);
      }

      return jsonObject;
   }

   static void method3404(float local5563_0, float local5563_1, boolean local5563_2, float local5563_3, float local5563_4) {
      field11908 = local5563_3;
      field11909 = local5563_4;
      if (!local5563_2) {
         if (field11899 != null) {
            method3410();
            field11899 = null;
         }

         if (field11900 != null) {
            method3410();
            field11900 = null;
         }

         field11887.clear();
      } else {
         if (field11900 != null) {
            method3393(field11900, field11905 + ((local5563_0 - field11903) / field11906 + (local5563_1 - field11904) / field11907) / 2.0F);
         }

         if (field11899 != null) {
            float[] floatArray = field11885.get(field11899);
            float local5563_6 = floatArray != null ? floatArray[2] : 0.0F;
            float local5563_7 = floatArray != null ? floatArray[3] : 0.0F;
            float local5563_8 = local5563_0 - field11901;
            float local5563_9 = local5563_1 - field11902;
            if (method3417() && floatArray != null) {
               if (Math.abs(local5563_8 - local5563_6 + floatArray[0] / 2.0F - field11908 / 2.0F) < 8.0F) {
                  local5563_8 = field11908 / 2.0F - floatArray[0] / 2.0F + local5563_6;
               }

               if (Math.abs(local5563_9 - local5563_7 + floatArray[1] / 2.0F - field11909 / 2.0F) < 8.0F) {
                  local5563_9 = field11909 / 2.0F - floatArray[1] / 2.0F + local5563_7;
               }
            }

            field11882.put(field11899, new float[]{local5563_8, local5563_9});
            if (method3382(field11899) && floatArray != null) {
               field11888.put(field11899, local5563_8 - local5563_6 + floatArray[0] / 2.0F);
            }
         }
      }
   }

   private static String method3405(float local5564_0, float local5564_1) {
      List<Entry<String, HudManager.Nested0487>> local5564_2 = new ArrayList<>(field11884.entrySet());

      for (int index = local5564_2.size() - 1; index >= 0; index--) {
         HudManager.Nested0487 value = local5564_2.get(index).getValue();
         float local5564_5 = Math.min(9.0F, Math.min(value.field12016, value.field12017) / 3.0F);
         if (local5564_0 >= value.field12014 + value.field12016 - local5564_5
            && local5564_0 <= value.field12014 + value.field12016 + 4.0F
            && local5564_1 >= value.field12015 + value.field12017 - local5564_5
            && local5564_1 <= value.field12015 + value.field12017 + 4.0F) {
            return local5564_2.get(index).getKey();
         }
      }

      return null;
   }

   public static float method3407(String local5566_0) {
      float local5566_1 = Math.max(0.25F, Math.min(2.0F, method3385(local5566_0)[0]));
      AnimatedValue animatedValue = field11892.computeIfAbsent(local5566_0, local5537_1 -> new AnimatedValue(140L).method3870(local5566_1));
      animatedValue.method3869(local5566_1);
      return (float)animatedValue.method3871();
   }

   private static int method3408(String local5567_0) {
      int local5567_1 = field11898.indexOf(local5567_0);
      return local5567_1 < 0 ? Integer.MAX_VALUE : local5567_1;
   }

   public static boolean method3409(String local5568_0) {
      return method3385(local5568_0)[2] > 0.5F;
   }

   private static void method3410() {
      method3402();

      try {
         ConfigStore.writeAsync(field11911, method3403().toString());
      } catch (Exception exception) {
      }
   }

   public static float method3411(String local5570_0) {
      return method3385(local5570_0)[0];
   }

   static void method3412() {
      field11884.clear();
   }

   public static void method3413() {
      method3410();
   }

   static void method3414(float local5573_0, float local5573_1, boolean local5573_2) {
      if (local5573_2) {
         field11890.method3869(method3417() && field11899 != null ? 1.0 : 0.0);
         float local5573_3 = (float)field11890.method3871();
         if (local5573_3 > 0.01F && field11908 > 0.0F && field11909 > 0.0F) {
            Render2D.method4715(
               field11908 / 2.0F - 0.25F,
               field11909 / 2.0F * (1.0F - local5573_3),
               0.5F,
               field11909 * local5573_3,
               0.0F,
               UiTheme.method2877(UiTheme.field09680, 0.14F * local5573_3)
            );
            Render2D.method4715(
               field11908 / 2.0F * (1.0F - local5573_3),
               field11909 / 2.0F - 0.25F,
               field11908 * local5573_3,
               0.5F,
               0.0F,
               UiTheme.method2877(UiTheme.field09680, 0.14F * local5573_3)
            );
         }

         long local5573_4 = System.currentTimeMillis();

         for (Entry<String, HudManager.Nested0487> local5573_7 : field11884.entrySet()) {
            HudManager.Nested0487 value = local5573_7.getValue();
            boolean local5573_9 = local5573_7.getKey().equals(field11899)
               || local5573_0 >= value.field12014
                  && local5573_0 <= value.field12014 + value.field12016
                  && local5573_1 >= value.field12015
                  && local5573_1 <= value.field12015 + value.field12017;
            if (!local5573_9) {
               field11887.remove(local5573_7.getKey());
            } else {
               float local5573_10 = Math.min(
                  1.0F, Math.max(0.02F, (float)(local5573_4 - field11887.computeIfAbsent(local5573_7.getKey(), local5575_2 -> local5573_4)) / 50.0F)
               );
               Render2D.method4749(
                  value.field12014 - 1.5F,
                  value.field12015 - 1.5F,
                  value.field12016 + 3.0F,
                  value.field12017 + 3.0F,
                  9.5F,
                  0.5F,
                  0,
                  ArcColorSpan.of(90.0F - local5573_10 * 360.0F, 90.0F, UiTheme.method2877(UiTheme.field09680, 0.3F))
               );
            }
         }

         String local5573_6 = method3405(local5573_0, local5573_1);

         for (Entry<String, HudManager.Nested0487> local5573_8 : field11884.entrySet()) {
            HudManager.Nested0487 value2 = local5573_8.getValue();
            AnimatedValue animatedValue = field11893.computeIfAbsent(local5573_8.getKey(), local5565_0 -> new AnimatedValue(160L).method3870(0.0));
            animatedValue.method3869(
               !local5573_8.getKey().equals(field11900)
                     && (
                        !(local5573_0 >= value2.field12014 - 4.0F)
                           || !(local5573_0 <= value2.field12014 + value2.field12016 + 4.0F)
                           || !(local5573_1 >= value2.field12015 - 4.0F)
                           || !(local5573_1 <= value2.field12015 + value2.field12017 + 4.0F)
                     )
                  ? 0.0
                  : 1.0
            );
            float local5573_11 = (float)animatedValue.method3871();
            if (!(local5573_11 < 0.01F)) {
               float local5573_12 = local5573_11 * (!local5573_8.getKey().equals(field11900) && !local5573_8.getKey().equals(local5573_6) ? 0.45F : 0.9F);
               float local5573_13 = value2.field12014 + value2.field12016 + 1.5F;
               float local5573_14 = value2.field12015 + value2.field12017 + 1.5F;

               for (int index = 0; index < 3; index++) {
                  for (int index2 = 0; index2 < 3; index2++) {
                     if (index + index2 >= 2) {
                        Render2D.method4715(
                           local5573_13 - 2.6F - (2 - index) * 2.2F,
                           local5573_14 - 2.6F - (2 - index2) * 2.2F,
                           1.1F,
                           1.1F,
                           0.55F,
                           UiTheme.method2877(UiTheme.field09680, local5573_12)
                        );
                     }
                  }
               }
            }
         }

         field11894.method3869(field11900 != null && field11884.containsKey(field11900) ? 1.0 : 0.0);
         if (field11900 != null && field11884.containsKey(field11900)) {
            field11896 = method3388(field11900);
            field11897 = field11900;
         }

         float local5573_7 = (float)field11894.method3871();
         if (local5573_7 > 0.01F && field11896 != null) {
            String local5573_8 = Math.round(method3411(field11897) * 100.0F) + "%";
            float local5573_9 = FontText.method1396(FontFace.SEMI, local5573_8, 5.0F);
            float local5573_10 = field11896[0] + field11896[2] + 5.0F;
            if (local5573_10 + local5573_9 > field11908 - 2.0F) {
               local5573_10 = field11896[0] + field11896[2] - 10.0F - local5573_9;
            }

            FontText.method1397(
               FontFace.SEMI,
               local5573_8,
               local5573_10,
               field11896[1] + field11896[3] + 3.0F + (1.0F - local5573_7) * 2.0F,
               5.0F,
               UiTheme.method2877(UiTheme.field09680, 0.8F * local5573_7)
            );
         }

         field11891.method3869(field11899 != null && field11884.containsKey(field11899) ? 1.0 : 0.0);
         if (field11899 != null && field11884.containsKey(field11899)) {
            field11895 = method3388(field11899);
         }

         float local5573_8 = (float)field11891.method3871();
         if (local5573_8 > 0.01F && field11895 != null) {
            FontText.method1397(
               FontFace.SEMI,
               "Зажмите LCTRL для перемещения по сетке",
               field11895[0]
                  + field11895[2] / 2.0F
                  - FontText.method1396(FontFace.SEMI, "Зажмите LCTRL для перемещения по сетке", 5.0F) / 2.0F,
               field11895[1] + field11895[3] + 4.0F + (1.0F - local5573_8) * 3.0F,
               5.0F,
               UiTheme.method2877(UiTheme.field09680, 0.7F * local5573_8)
            );
         }
      }
   }

   private static float method3415() {
      return GuiScale.method0797() / GuiScale.method0794();
   }

   public static boolean method3417() {
      return MinecraftClient.getInstance().getWindow() != null && InputUtil.isKeyPressed(MinecraftClient.getInstance().getWindow(), 341);
   }

   public static void method3418(String local5577_0, float local5577_1, float local5577_2, float local5577_3, float local5577_4) {
      method3399(local5577_0, local5577_1, local5577_2, local5577_3, local5577_4, local5577_1, local5577_2);
   }

   public static boolean method3419(String local5578_0) {
      method3402();
      return field11882.containsKey(local5578_0);
   }

   public static float[] method3420(double local5579_0, double local5579_2) {
      float local5579_4 = method3415();
      return new float[]{(float)(local5579_0 * local5579_4), (float)(local5579_2 * local5579_4)};
   }

   public static void method3421(JsonObject jsonObject) {
      method3402();
      field11882.clear();
      field11883.clear();
      field11888.clear();
      method3395(jsonObject);
      method3410();
   }

   public static void method3422(String local5581_0, boolean local5581_1) {
      method3385(local5581_0)[1] = local5581_1 ? 1.0F : 0.0F;
      method3410();
   }

   public static void method3423() {
      if (field11899 != null) {
         method3410();
         field11899 = null;
      }

      if (field11900 != null) {
         method3410();
         field11900 = null;
      }

      field11887.clear();
   }

   private static final class Nested0487 {
      float field12014;
      float field12015;
      float field12016;
      float field12017;
      float field12018;
      float field12019;
   }
}
