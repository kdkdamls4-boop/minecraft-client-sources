package deluxe.client.ui.hud;

import deluxe.client.render.FontText;
import deluxe.client.util.ServerItemType;
import deluxe.client.render.ThemeColors;
import deluxe.client.ui.UiTheme;

import deluxe.client.util.Animation;

import deluxe.client.util.Easings;
import deluxe.client.render.FontFace;
import deluxe.client.module.render.InterfaceModule;
import deluxe.client.render.Render2D;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

public final class PotionsHud {
   private static final Set<RegistryEntry<StatusEffect>> field10857 = Set.of(
      StatusEffects.STRENGTH,
      StatusEffects.SPEED,
      StatusEffects.FIRE_RESISTANCE,
      StatusEffects.RESISTANCE,
      StatusEffects.ABSORPTION,
      StatusEffects.REGENERATION
   );
   private static final List<RegistryEntry<StatusEffect>> field10858 = List.of(
      StatusEffects.SPEED,
      StatusEffects.STRENGTH,
      StatusEffects.REGENERATION,
      StatusEffects.FIRE_RESISTANCE,
      StatusEffects.WATER_BREATHING,
      StatusEffects.POISON
   );
   private static final PotionsHud.Nested0477 field10859 = new PotionsHud.Nested0477("Example Potion", 0);
   private static long field10860;
   private static int field10861;
   private static final Map<ServerItemType, PotionsHud.Nested0477> field10862 = new EnumMap<>(ServerItemType.class);
   private static final Map<ServerItemType, Long> field10863 = new EnumMap<>(ServerItemType.class);
   private static final Map<RegistryEntry<StatusEffect>, PotionsHud.Nested0477> field10864 = new IdentityHashMap<>();
   private static final List<PotionsHud.Nested0477> field10865 = new ArrayList<>();
   private static final Animation field10866 = new Animation();
   private static final Animation field10867 = new Animation();
   private static final Animation field10868 = new Animation();
   private static final Animation field10869 = new Animation();
   private static float field10870;
   private static float field10871;

   private static float method3195(PotionsHud.Nested0477 local5166_0) {
      return FontText.method1396(FontFace.SEMI_BOLD, local5166_0.field10921, 6.0F)
         + FontText.method1396(FontFace.SEMI_BOLD, local5166_0.field10922, 6.0F);
   }

   private static void method3198(MinecraftClient minecraftClient) {
      field10865.clear();
      field10864.values().forEach(local5171_0 -> local5171_0.visible = false);
      if (minecraftClient.player != null) {
         for (StatusEffectInstance statusEffectInstance : minecraftClient.player.getStatusEffects()) {
            PotionsHud.Nested0477 local5169_3 = field10864.computeIfAbsent(statusEffectInstance.getEffectType(), PotionsHud.Nested0477::new);
            int currentAmplifier = statusEffectInstance.getAmplifier();
            if (currentAmplifier != local5169_3.field10926) {
               local5169_3.field10926 = currentAmplifier;
               local5169_3.field10922 = currentAmplifier > 0 ? " " + (currentAmplifier + 1) : "";
            }

            local5169_3.field10924 = statusEffectInstance;
            local5169_3.field10923 = method3202(statusEffectInstance);
            local5169_3.visible = true;
         }
      }

      method3210(minecraftClient);

      for (PotionsHud.Nested0477 local5169_2 : field10864.values()) {
         if (local5169_2.visible || local5169_2.visibilityAnimation.floatValue() > 0.001) {
            field10865.add(local5169_2);
         }
      }

      for (PotionsHud.Nested0477 local5169_2 : field10862.values()) {
         if (local5169_2.visible || local5169_2.visibilityAnimation.floatValue() > 0.001) {
            field10865.add(local5169_2);
         }
      }

      field10864.entrySet()
         .removeIf(
            local5172_0 -> !local5172_0.getValue().visible
               && local5172_0.getValue().visibilityAnimation.floatValue() <= 0.001
               && local5172_0.getValue().visibilityAnimation.isFinished()
         );
      field10862.entrySet()
         .removeIf(
            local5181_0 -> !local5181_0.getValue().visible
               && local5181_0.getValue().visibilityAnimation.floatValue() <= 0.001
               && local5181_0.getValue().visibilityAnimation.isFinished()
         );
      field10859.visible = field10865.isEmpty() && HudRenderContext.method4342();
      if (field10859.visible) {
         long local5169_1 = System.currentTimeMillis();
         float local5169_3 = (float)(10000L - (local5169_1 - field10860)) / 1000.0F;
         if (local5169_3 <= 0.0F) {
            field10860 = local5169_1;
            field10861++;
            local5169_3 = 10.0F;
         }

         field10859.field10924 = new StatusEffectInstance(field10858.get(field10861 % field10858.size()), (int)Math.ceil(Math.max(1.0F, local5169_3)) * 20, 0);
         field10859.field10923 = method3202(field10859.field10924);
      } else {
         field10860 = 0L;
      }

      if (field10859.visible || field10859.visibilityAnimation.floatValue() > 0.001) {
         field10865.add(field10859);
      }

      field10865.sort(
         Comparator.<PotionsHud.Nested0477, Integer>comparing(local5170_0 -> local5170_0.field10925)
            .thenComparing(Comparator.<PotionsHud.Nested0477>comparingDouble(local5180_0 -> method3195(local5180_0)).reversed())
      );

      for (PotionsHud.Nested0477 local5169_2 : field10865) {
         local5169_2.visibilityAnimation.update();
         local5169_2.visibilityAnimation.animateTo(local5169_2.visible ? 1.0 : 0.0, 0.6, Easings.EASE_OUT_QUART);
      }
   }

   static {
      field10867.resetTo(1.0);
   }

   private static String method3202(StatusEffectInstance statusEffectInstance) {
      if (statusEffectInstance.isInfinite()) {
         return "INF";
      }

      int local5174_1 = statusEffectInstance.getDuration() / 20;
      int local5174_2 = local5174_1 / 60;
      int local5174_3 = local5174_1 % 60;
      return local5174_2 > 0 ? local5174_2 + "м " + local5174_3 + "с" : local5174_3 + "с";
   }

   public static float method3203(MinecraftClient minecraftClient, float local5175_1, float local5175_2) {
      method3198(minecraftClient);
      float local5175_3 = HudStyle.method4823("Potions");
      float local5175_4 = 0.0F;
      float local5175_5 = 0.0F;
      float local5175_6 = 13.0F;

      for (PotionsHud.Nested0477 local5175_8 : field10865) {
         local5175_4 = Math.max(local5175_4, HudStyle.method4809(8.0F, method3209(local5175_8)));
         local5175_5 = Math.max(local5175_5, HudStyle.method4822(method3195(local5175_8)));
         local5175_6 += 14.0F * local5175_8.visibilityAnimation.floatValue();
      }

      float local5175_7 = local5175_4 + (local5175_4 > 0.0F && local5175_5 > 0.0F ? 2.0F : 0.0F) + local5175_5;
      float local5175_8 = Math.max(local5175_3, Math.max(local5175_7, 1.0F));
      boolean local5175_9 = !field10865.isEmpty() || HudRenderContext.method4342();
      field10866.update();
      field10866.animateTo(local5175_9 ? 1.0 : 0.0, 0.4, Easings.EASE_OUT_QUART);
      field10867.update();
      if (!field10865.isEmpty()) {
         field10867.animateTo(Math.max(local5175_8, 1.0F), 0.6, Easings.EASE_OUT_QUART);
      }

      field10868.update();
      if (!field10865.isEmpty()) {
         field10868.animateTo(Math.max(local5175_8, 1.0F), 0.6, Easings.EASE_OUT_QUART);
      }

      field10869.update();
      if (!field10865.isEmpty()) {
         field10869.animateTo(Math.max(local5175_4, 0.0F), 0.6, Easings.EASE_OUT_QUART);
      }

      field10870 = field10867.floatValue();
      field10871 = Math.max(local5175_6, 1.0F);
      float local5175_10 = field10866.floatValue();
      if (local5175_10 <= 0.001F) {
         return 0.0F;
      }

      float[] floatArray = HudManager.method3390("potions", local5175_1 - field10870, local5175_2);
      float local5175_12 = floatArray[0];
      float local5175_13 = floatArray[1];
      HudManager.method3418("potions", local5175_12, local5175_13, field10870, field10871);
      HudRenderContext.method4341(local5175_12 + field10870 / 2.0F, local5175_13 + field10871 / 2.0F, local5175_10);
      HudStyle.method4824(local5175_12, local5175_13, Math.max(field10868.floatValue(), 1.0F), local5175_10, HudIcons.POTIONS, "Potions");
      float local5175_14 = local5175_13 + 15.0F;

      for (PotionsHud.Nested0477 local5175_16 : field10865) {
         method3211(local5175_12, local5175_14, local5175_10, local5175_16);
         local5175_14 += 13.0F * local5175_16.visibilityAnimation.floatValue();
      }

      HudRenderContext.method4337();
      return field10871 * local5175_10 + 8.0F * local5175_10;
   }

   public static void method3205(ServerItemType serverItemType, BlockPos blockPos) {
      MinecraftClient minecraftClient = MinecraftClient.getInstance();
      if (minecraftClient.player != null && !(minecraftClient.player.getBoundingBox().squaredMagnitude(blockPos.toCenterPos()) > 16.0)) {
         field10863.put(serverItemType, System.currentTimeMillis());
      }
   }

   static int method3206(RegistryEntry<StatusEffect> local5178_0) {
      if (field10857.contains(local5178_0)) {
         return 0;
      } else {
         StatusEffectCategory category = local5178_0.value().getCategory();
         if (category == StatusEffectCategory.BENEFICIAL) {
            return 1;
         } else {
            return category == StatusEffectCategory.NEUTRAL ? 2 : 3;
         }
      }
   }

   private PotionsHud() {
   }

   private static float method3209(PotionsHud.Nested0477 local5182_0) {
      return FontText.method1396(FontFace.SEMI_BOLD, local5182_0.field10923, 6.0F);
   }

   private static void method3210(MinecraftClient minecraftClient) {
      Map<ServerItemType, Boolean> local5183_1 = new EnumMap<>(ServerItemType.class);
      field10862.forEach((serverItemType, local5167_2) -> local5183_1.put(serverItemType, local5167_2.visible));
      field10862.values().forEach(local5176_0 -> local5176_0.visible = false);
      if (minecraftClient.player != null && InterfaceModule.method2247().field07922.isChecked() && ServerItemType.method2469()) {
         for (ServerItemType serverItemType2 : ServerItemType.values()) {
            StatusEffectInstance statusEffectInstance = null;

            for (ServerItemType.Nested0434 local5183_8 : serverItemType2.method2461()) {
               StatusEffectInstance statusEffectInstance2 = minecraftClient.player.getStatusEffect(local5183_8.effect());
               if (statusEffectInstance2 == null || !ServerItemType.method2467(statusEffectInstance2.getAmplifier(), local5183_8.amplifier())) {
                  statusEffectInstance = null;
                  break;
               }

               if (statusEffectInstance == null || statusEffectInstance2.getDuration() > statusEffectInstance.getDuration()) {
                  statusEffectInstance = statusEffectInstance2;
               }
            }

            boolean local5183_7 = local5183_1.getOrDefault(serverItemType2, false) || System.currentTimeMillis() - field10863.getOrDefault(serverItemType2, 0L) < 3000L;
            if (statusEffectInstance != null && local5183_7) {
               PotionsHud.Nested0477 local5183_8 = field10862.computeIfAbsent(serverItemType2, serverItemType3 -> {
                  PotionsHud.Nested0477 local5168_1 = new PotionsHud.Nested0477(serverItemType3.method2463(), 0);
                  local5168_1.field10927 = serverItemType3.method2462();
                  return local5168_1;
               });

               for (ServerItemType.Nested0434 local5183_10 : serverItemType2.method2461()) {
                  PotionsHud.Nested0477 local5183_11 = field10864.get(local5183_10.effect());
                  if (local5183_11 != null) {
                     local5183_11.visible = false;
                  }
               }

               local5183_8.field10924 = statusEffectInstance;
               local5183_8.field10923 = method3202(statusEffectInstance);
               local5183_8.visible = true;
            }
         }
      }
   }

   private static void method3211(float local5184_0, float local5184_1, float local5184_2, PotionsHud.Nested0477 local5184_3) {
      float local5184_4 = local5184_3.visibilityAnimation.floatValue() * local5184_2;
      if (!(local5184_4 <= 0.001F)) {
         float local5184_5 = HudStyle.method4814(local5184_4);
         float local5184_6 = method3209(local5184_3);
         float local5184_7 = HudStyle.method4809(8.0F, local5184_6);
         float local5184_8 = HudStyle.method4822(method3195(local5184_3));
         float local5184_9 = local5184_0 - local5184_5;
         float local5184_10 = local5184_0 + field10869.floatValue() + 2.0F - local5184_5;
         float local5184_11 = HudStyle.method4816(local5184_1);
         HudStyle.method4821(local5184_9, local5184_1, local5184_7, 11.0F, local5184_4);
         float local5184_12 = local5184_9 + 2.0F + 8.0F + 1.0F;
         float local5184_13 = local5184_12 + 0.25F;
         HudStyle.method4810(local5184_12, local5184_1 + 5.5F, local5184_4, 3.0F);
         if (local5184_3.field10924 != null) {
            Render2D.method4705(local5184_4);
            Render2D.method4741(
               local5184_3.field10924.getEffectType(), local5184_9 + (local5184_13 - local5184_9 - 8.0F) / 2.0F, local5184_1 + 5.5F - 4.0F, 8.0F
            );
            Render2D.method4650();
         }

         FontText.method1397(
            FontFace.SEMI_BOLD,
            local5184_3.field10923,
            local5184_13 + (local5184_9 + local5184_7 - local5184_13 - local5184_6) / 2.0F,
            local5184_11,
            6.0F,
            UiTheme.method2877(HudStyle.method4820(1.0F), local5184_4)
         );
         HudStyle.method4821(local5184_10, local5184_1, local5184_8, 11.0F, local5184_4);
         boolean local5184_14 = local5184_3.field10924 != null && local5184_3.field10924.getEffectType().value().getCategory() == StatusEffectCategory.HARMFUL;
         int local5184_15 = UiTheme.method2877(local5184_14 ? -44976 : -1, local5184_4);
         if (local5184_3.field10927 != null) {
            FontText.method1400(
               FontFace.SEMI_BOLD, local5184_3.field10927, local5184_10 + 4.0F, local5184_11, 6.0F, UiTheme.method2877(-1, local5184_4)
            );
         } else {
            FontText.method1397(FontFace.SEMI_BOLD, local5184_3.field10921, local5184_10 + 4.0F, local5184_11, 6.0F, local5184_15);
         }

         if (!local5184_3.field10922.isEmpty()) {
            FontText.method1397(
               FontFace.SEMI_BOLD,
               local5184_3.field10922,
               local5184_10 + 4.0F + FontText.method1396(FontFace.SEMI_BOLD, local5184_3.field10921, 6.0F),
               local5184_11,
               6.0F,
               UiTheme.method2877(ThemeColors.field13669, local5184_4)
            );
         }
      }
   }

   private static final class Nested0477 extends DraggableHudElement {
      final String field10921;
      String field10922 = "";
      String field10923 = "";
      StatusEffectInstance field10924;
      final int field10925;
      int field10926 = -1;
      Text field10927;

      Nested0477(String local5185_1, int local5185_2) {
         this.field10921 = local5185_1;
         this.field10925 = local5185_2;
      }

      Nested0477(RegistryEntry<StatusEffect> local5186_1) {
         this.field10921 = local5186_1.value().getName().getString();
         this.field10925 = PotionsHud.method3206(local5186_1);
      }
   }
}
