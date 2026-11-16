package deluxe.client.ui.hud;

import deluxe.client.render.FontText;
import deluxe.client.util.GuiScale;
import deluxe.client.util.ItemCooldownAccess;
import deluxe.client.render.ItemRenderCache;
import deluxe.client.ui.UiTheme;

import deluxe.client.util.Animation;

import deluxe.client.util.Easings;
import deluxe.client.render.FontFace;
import deluxe.client.module.render.InterfaceModule;
import deluxe.client.util.ItemDrawOptions;
import deluxe.client.render.Render2D;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Map.Entry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;

public final class CooldownsHud {
   private static final Item[] field15369 = new Item[]{
      Items.ENDER_EYE,
      Items.ENDER_PEARL,
      Items.SUGAR,
      Items.MACE,
      Items.ENCHANTED_GOLDEN_APPLE,
      Items.TRIDENT,
      Items.CROSSBOW,
      Items.DRIED_KELP,
      Items.NETHERITE_SCRAP
   };
   private static final CooldownsHud.Nested0531 field15370 = new CooldownsHud.Nested0531(Items.ENDER_EYE);
   private static long field15371;
   private static int field15372;
   private static final Map<Item, CooldownsHud.Nested0531> field15373 = new LinkedHashMap<>();
   private static final Map<Item, CooldownsHud.Nested0532> field15374 = new LinkedHashMap<>();
   private static final List<CooldownsHud.Nested0531> field15375 = new ArrayList<>();
   private static final Animation field15376 = new Animation();
   private static final Animation field15377 = new Animation();
   private static final Animation field15378 = new Animation();
   private static final Animation field15379 = new Animation();
   private static float field15380;
   private static float field15381;

   static float method4544(Item item) {
      CooldownsHud.Nested0532 local7670_1 = field15374.get(item);
      return local7670_1 == null ? 0.0F : Math.max(0.0F, local7670_1.field15464 - (float)(System.currentTimeMillis() - local7670_1.field15463) / 1000.0F);
   }

   private static TextColor method4546(ItemStack itemStack) {
      TextColor[] textColorArray = new TextColor[]{null};
      itemStack.getName().visit((style, local7674_2) -> {
         if (textColorArray[0] == null && style.getColor() != null && !local7674_2.isBlank()) {
            textColorArray[0] = style.getColor();
         }

         return Optional.empty();
      }, Style.EMPTY);
      return textColorArray[0] != null ? textColorArray[0] : TextColor.fromFormatting(itemStack.getRarity().getFormatting());
   }

   private static void method4547(CooldownsHud.Nested0531 local7673_0, int local7673_1) {
      if (local7673_1 != local7673_0.field15459) {
         local7673_0.field15459 = local7673_1;
         int local7673_2 = (local7673_1 + 9) / 10;
         int local7673_3 = local7673_2 / 60;
         int local7673_4 = local7673_2 % 60;
         local7673_0.field15456 = local7673_3 > 0 ? local7673_3 + "м " + local7673_4 + "с" : local7673_4 + "с";
         local7673_0.field15457 = FontText.method1396(FontFace.SEMI_BOLD, local7673_0.field15456, 6.0F);
      }
   }

   private static ItemStack method4549(MinecraftClient minecraftClient, Item item, CooldownsHud.Nested0532 local7675_2) {
      int changeCount = minecraftClient.player.getInventory().getChangeCount();
      long local7675_4 = System.currentTimeMillis();
      if (local7675_2.field15465 == null
         || local7675_2.field15466 != minecraftClient.player
         || local7675_2.field15467 != changeCount
         || local7675_4 - local7675_2.field15468 >= 1000L) {
         local7675_2.field15465 = method4555(minecraftClient, item, local7675_2.field15462);
         local7675_2.field15466 = minecraftClient.player;
         local7675_2.field15467 = changeCount;
         local7675_2.field15468 = local7675_4;
      }

      return local7675_2.field15465;
   }

   private CooldownsHud() {
   }

   public static void method4553(String local7680_0, Item item, float local7680_2) {
      CooldownsHud.Nested0532 local7680_3 = field15374.get(item);
      if (local7680_3 == null || !((float)(System.currentTimeMillis() - local7680_3.field15463) < local7680_3.field15464 * 1000.0F)) {
         CooldownsHud.Nested0532 local7680_4 = new CooldownsHud.Nested0532();
         local7680_4.field15462 = local7680_0;
         local7680_4.field15463 = System.currentTimeMillis();
         local7680_4.field15464 = local7680_2;
         field15374.put(item, local7680_4);
      }
   }

   private static void method4554(MinecraftClient minecraftClient) {
      field15375.clear();
      field15373.values().forEach(local7692_0 -> local7692_0.visible = false);
      if (minecraftClient.player != null) {
         for (int index = 0; index < minecraftClient.player.getInventory().size(); index++) {
            method4560(minecraftClient, minecraftClient.player.getInventory().getStack(index));
         }

         method4560(minecraftClient, minecraftClient.player.getOffHandStack());
         field15374.values().removeIf(local7687_0 -> (float)(System.currentTimeMillis() - local7687_0.field15463) >= local7687_0.field15464 * 1000.0F);

         for (Entry<Item, CooldownsHud.Nested0532> local7681_2 : field15374.entrySet()) {
            CooldownsHud.Nested0532 value = local7681_2.getValue();
            float local7681_4 = Math.max(0.0F, value.field15464 - (float)(System.currentTimeMillis() - value.field15463) / 1000.0F);
            CooldownsHud.Nested0531 local7681_5 = field15373.computeIfAbsent(local7681_2.getKey(), CooldownsHud.Nested0531::new);
            local7681_5.field15461 = method4549(minecraftClient, local7681_2.getKey(), value);
            local7681_5.field15455 = value.field15462;
            local7681_5.field15460 = true;
            method4547(local7681_5, Math.round(local7681_4 * 10.0F));
            local7681_5.visible = true;
         }
      }

      for (CooldownsHud.Nested0531 local7681_2 : field15373.values()) {
         if (local7681_2.visible || local7681_2.visibilityAnimation.floatValue() > 0.001) {
            field15375.add(local7681_2);
         }
      }

      field15373.entrySet()
         .removeIf(
            local7689_0 -> !local7689_0.getValue().visible
               && local7689_0.getValue().visibilityAnimation.floatValue() <= 0.001
               && local7689_0.getValue().visibilityAnimation.isFinished()
         );
      field15370.visible = field15375.isEmpty() && HudRenderContext.method4342();
      if (field15370.visible) {
         long local7681_1 = System.currentTimeMillis();
         float local7681_3 = (float)(10000L - (local7681_1 - field15371)) / 1000.0F;
         if (local7681_3 <= 0.0F) {
            field15371 = local7681_1;
            field15372++;
            local7681_3 = 10.0F;
         }

         Item item = field15369[field15372 % field15369.length];
         if (field15370.field15461 == null || !field15370.field15461.isOf(item)) {
            field15370.field15461 = new ItemStack(item);
         }

         field15370.field15455 = "Example Cooldown";
         field15370.field15460 = true;
         method4547(field15370, Math.round(Math.max(1.0F, local7681_3) * 10.0F));
      } else {
         field15371 = 0L;
      }

      if (field15370.visible || field15370.visibilityAnimation.floatValue() > 0.001) {
         field15375.add(field15370);
      }

      for (CooldownsHud.Nested0531 local7681_2 : field15375) {
         local7681_2.field15458 = method4556(local7681_2);
      }

      field15375.sort(Comparator.<CooldownsHud.Nested0531>comparingDouble(local7676_0 -> local7676_0.field15458).reversed());

      for (CooldownsHud.Nested0531 local7681_2 : field15375) {
         local7681_2.visibilityAnimation.update();
         local7681_2.visibilityAnimation.animateTo(local7681_2.visible ? 1.0 : 0.0, 0.6, Easings.EASE_OUT_QUART);
      }
   }

   static {
      field15377.resetTo(1.0);
   }

   private static ItemStack method4555(MinecraftClient minecraftClient, Item item, String local7683_2) {
      ItemStack itemStack = ItemStack.EMPTY;
      String[] stringArray = local7683_2.toLowerCase(Locale.ROOT).split("/");

      for (int index = 0; index < minecraftClient.player.getInventory().size() + 1; index++) {
         ItemStack itemStack2 = index < minecraftClient.player.getInventory().size()
            ? minecraftClient.player.getInventory().getStack(index)
            : minecraftClient.player.getOffHandStack();
         if (!itemStack2.isEmpty() && itemStack2.isOf(item)) {
            if (itemStack.isEmpty()) {
               itemStack = itemStack2;
            }

            String local7683_7 = Formatting.strip(itemStack2.getName().getString());
            if (local7683_7 != null) {
               local7683_7 = local7683_7.toLowerCase(Locale.ROOT);

               for (String local7683_11 : stringArray) {
                  if (!local7683_11.isBlank() && local7683_7.contains(local7683_11.trim())) {
                     return itemStack2.copy();
                  }
               }
            }
         }
      }

      return itemStack.isEmpty() ? new ItemStack(item) : itemStack.copy();
   }

   private static float method4556(CooldownsHud.Nested0531 local7684_0) {
      float[] floatArray = new float[]{0.0F};
      method4563(local7684_0).visit((style, local7677_2) -> {
         floatArray[0] += FontText.method1396(FontFace.SEMI_BOLD, local7677_2, 6.0F);
         return Optional.empty();
      }, Style.EMPTY);
      return floatArray[0];
   }

   public static float method4558(MinecraftClient minecraftClient, float local7686_1, float local7686_2) {
      method4554(minecraftClient);
      float local7686_3 = HudStyle.method4823("Cooldowns");
      float local7686_4 = 0.0F;
      float local7686_5 = 0.0F;
      float local7686_6 = 13.0F;

      for (CooldownsHud.Nested0531 local7686_8 : field15375) {
         local7686_4 = Math.max(local7686_4, HudStyle.method4809(8.0F, local7686_8.field15457));
         local7686_5 = Math.max(local7686_5, HudStyle.method4822(local7686_8.field15458));
         local7686_6 += 14.0F * local7686_8.visibilityAnimation.floatValue();
      }

      float local7686_7 = local7686_4 + (local7686_4 > 0.0F && local7686_5 > 0.0F ? 2.0F : 0.0F) + local7686_5;
      float local7686_8 = Math.max(local7686_3, Math.max(local7686_7, 1.0F));
      boolean local7686_9 = !field15375.isEmpty() || HudRenderContext.method4342();
      field15376.update();
      field15376.animateTo(local7686_9 ? 1.0 : 0.0, 0.4, Easings.EASE_OUT_QUART);
      field15377.update();
      if (!field15375.isEmpty()) {
         field15377.animateTo(Math.max(local7686_8, 1.0F), 0.6, Easings.EASE_OUT_QUART);
      }

      field15378.update();
      if (!field15375.isEmpty()) {
         field15378.animateTo(Math.max(local7686_8, 1.0F), 0.6, Easings.EASE_OUT_QUART);
      }

      field15379.update();
      if (!field15375.isEmpty()) {
         field15379.animateTo(Math.max(local7686_4, 0.0F), 0.6, Easings.EASE_OUT_QUART);
      }

      field15380 = field15377.floatValue();
      field15381 = Math.max(local7686_6, 1.0F);
      float local7686_10 = field15376.floatValue();
      if (local7686_10 <= 0.001F) {
         return 0.0F;
      }

      float[] floatArray = HudManager.method3390("cooldowns", local7686_1 - field15380, local7686_2);
      float local7686_12 = floatArray[0];
      float local7686_13 = floatArray[1];
      HudManager.method3418("cooldowns", local7686_12, local7686_13, field15380, field15381);
      HudRenderContext.method4341(local7686_12 + field15380 / 2.0F, local7686_13 + field15381 / 2.0F, local7686_10);
      HudStyle.method4824(
         local7686_12, local7686_13, Math.max(field15378.floatValue(), 1.0F), local7686_10, HudIcons.COOLDOWNS, "Cooldowns"
      );
      float local7686_14 = local7686_13 + 15.0F;

      for (CooldownsHud.Nested0531 local7686_16 : field15375) {
         method4565(local7686_12, local7686_14, local7686_10, local7686_16);
         local7686_14 += 13.0F * local7686_16.visibilityAnimation.floatValue();
      }

      HudRenderContext.method4337();
      return field15381 * local7686_10 + 8.0F * local7686_10;
   }

   private static void method4560(MinecraftClient minecraftClient, ItemStack itemStack) {
      if (!itemStack.isEmpty() && !itemStack.isOf(Items.BOW) && !itemStack.isOf(Items.CROSSBOW) && !itemStack.isOf(Items.TRIDENT)) {
         if (!(minecraftClient.player.getItemCooldownManager().getCooldownProgress(itemStack, 0.0F) <= 0.001F)) {
            CooldownsHud.Nested0531 local7688_2 = field15373.computeIfAbsent(itemStack.getItem(), CooldownsHud.Nested0531::new);
            if (!local7688_2.visible) {
               int local7688_3 = ((ItemCooldownAccess)minecraftClient.player.getItemCooldownManager()).remainingCooldownTicks(itemStack);
               method4547(local7688_2, Math.max(0, Math.round(local7688_3 / 2.0F)));
               if (local7688_2.field15461 == null || local7688_2.field15461.getItem() != itemStack.getItem()) {
                  local7688_2.field15461 = itemStack.copy();
                  local7688_2.field15455 = itemStack.getName().getString();
               }

               local7688_2.field15460 = false;
               local7688_2.visible = true;
            }
         }
      }
   }

   public static void method4562(MinecraftClient minecraftClient, float local7690_1, float local7690_2) {
      if (minecraftClient.player != null) {
         float local7690_3 = GuiScale.method0797() / GuiScale.method0794();

         for (int index = 0; index < 9; index++) {
            ItemStack stack = minecraftClient.player.getInventory().getStack(index);
            if (!stack.isEmpty() && !stack.isOf(Items.BOW) && !stack.isOf(Items.CROSSBOW) && !stack.isOf(Items.TRIDENT)) {
               float local7690_6 = Math.max(
                  ((ItemCooldownAccess)minecraftClient.player.getItemCooldownManager()).remainingCooldownTicks(stack) / 20.0F,
                  method4544(stack.getItem())
               );
               if (!(local7690_6 <= 0.05F)) {
                  float local7690_7 = local7690_1 / 2.0F - 91.0F + 3.0F + index * 20.0F;
                  Render2D.method4715(
                     local7690_7 * local7690_3,
                     (local7690_2 - 19.0F) * local7690_3,
                     16.0F * local7690_3,
                     16.0F * local7690_3,
                     2.0F * local7690_3,
                     Math.round(InterfaceModule.method2247().field07918.floatValue() * 2.55F) << 24
                  );
                  String local7690_8 = String.valueOf((int)Math.ceil(local7690_6));
                  float local7690_9 = 5.0F * local7690_3;
                  float local7690_10 = 0.7F * local7690_3;
                  float local7690_11 = (local7690_7 + 1.0F) * local7690_3;
                  float local7690_12 = (local7690_2 - 19.0F + 2.5F) * local7690_3 - local7690_9 / 2.0F;
                  int local7690_13 = local7690_6 > 30.0F ? -50384 : (local7690_6 <= 5.0F ? -12517568 : (local7690_6 < 15.0F ? -20440 : -34264));
                  FontText.method1397(FontFace.BOLD, local7690_8, local7690_11 - local7690_10, local7690_12, local7690_9, -16777216);
                  FontText.method1397(FontFace.BOLD, local7690_8, local7690_11 + local7690_10, local7690_12, local7690_9, -16777216);
                  FontText.method1397(FontFace.BOLD, local7690_8, local7690_11, local7690_12 - local7690_10, local7690_9, -16777216);
                  FontText.method1397(FontFace.BOLD, local7690_8, local7690_11, local7690_12 + local7690_10, local7690_9, -16777216);
                  FontText.method1397(FontFace.BOLD, local7690_8, local7690_11, local7690_12, local7690_9, local7690_13);
               }
            }
         }
      }
   }

   private static Text method4563(CooldownsHud.Nested0531 local7691_0) {
      if (local7691_0.field15461 == null || local7691_0.field15461.isEmpty()) {
         return Text.literal(local7691_0.field15455);
      } else {
         return local7691_0.field15460
            ? Text.literal(local7691_0.field15455).styled(style -> style.withColor(method4546(local7691_0.field15461)))
            : local7691_0.field15461
               .getName()
               .copy()
               .styled(style2 -> style2.getColor() == null ? style2.withColor(local7691_0.field15461.getRarity().getFormatting()) : style2);
      }
   }

   private static void method4565(float local7693_0, float local7693_1, float local7693_2, CooldownsHud.Nested0531 local7693_3) {
      float local7693_4 = local7693_3.visibilityAnimation.floatValue() * local7693_2;
      if (!(local7693_4 <= 0.001F)) {
         float local7693_5 = HudStyle.method4814(local7693_4);
         float local7693_6 = local7693_3.field15457;
         float local7693_7 = HudStyle.method4809(8.0F, local7693_6);
         float local7693_8 = HudStyle.method4822(local7693_3.field15458);
         float local7693_9 = local7693_0 - local7693_5;
         float local7693_10 = local7693_0 + field15379.floatValue() + 2.0F - local7693_5;
         float local7693_11 = HudStyle.method4816(local7693_1);
         HudStyle.method4821(local7693_9, local7693_1, local7693_7, 11.0F, local7693_4);
         float local7693_12 = local7693_9 + 2.0F + 8.0F + 1.0F;
         float local7693_13 = local7693_12 + 0.25F;
         HudStyle.method4810(local7693_12, local7693_1 + 5.5F, local7693_4, 3.0F);
         if (local7693_3.field15461 != null && !local7693_3.field15461.isEmpty()) {
            float local7693_14 = Math.round((local7693_9 + (local7693_13 - local7693_9 - 8.0F) / 2.0F) * 2.0F) / 2.0F;
            float local7693_15 = Math.round((local7693_1 + 5.5F - 4.0F) * 2.0F) / 2.0F;
            ItemRenderCache.method2336(local7693_3.field15461, local7693_14, local7693_15, 8.0F, ItemDrawOptions.noDecorations(local7693_4));
         }

         FontText.method1397(
            FontFace.SEMI_BOLD,
            local7693_3.field15456,
            local7693_13 + (local7693_9 + local7693_7 - local7693_13 - local7693_6) / 2.0F,
            local7693_11,
            6.0F,
            UiTheme.method2877(HudStyle.method4820(1.0F), local7693_4)
         );
         HudStyle.method4821(local7693_10, local7693_1, local7693_8, 11.0F, local7693_4);
         int local7693_14 = UiTheme.method2877(HudStyle.method4820(1.0F), local7693_4);
         float[] floatArray = new float[]{local7693_10 + 4.0F};
         method4563(local7693_3).visit((style, local7678_4) -> {
            if (!local7678_4.isEmpty()) {
               int local7678_5 = style.getColor() != null ? style.getColor().getRgb() & 16777215 | local7693_14 & 0xFF000000 : local7693_14;
               FontText.method1397(FontFace.SEMI_BOLD, local7678_4, floatArray[0], local7693_11, 6.0F, local7678_5);
               floatArray[0] += FontText.method1396(FontFace.SEMI_BOLD, local7678_4, 6.0F);
            }

            return Optional.empty();
         }, Style.EMPTY);
      }
   }

   private static final class Nested0531 extends DraggableHudElement {
      String field15455 = "";
      String field15456 = "";
      float field15457;
      float field15458;
      int field15459 = -1;
      boolean field15460;
      ItemStack field15461;

      Nested0531(Item item) {
      }
   }

   private static final class Nested0532 {
      String field15462 = "";
      long field15463;
      float field15464;
      ItemStack field15465;
      Object field15466;
      int field15467;
      long field15468;
   }
}
