package deluxe.client.ui.hud;

import deluxe.client.render.FontText;
import deluxe.client.util.ItemCooldownAccess;
import deluxe.client.render.ItemRenderCache;
import deluxe.client.util.PotionPreset;
import deluxe.client.ui.UiTheme;

import deluxe.client.util.Animation;

import deluxe.client.util.Colors;
import deluxe.mixin.ContainerComponentMixin;
import deluxe.client.util.Easings;
import deluxe.client.render.FontFace;
import deluxe.client.util.ItemDrawOptions;
import deluxe.client.module.misc.PotionCombinerModule;
import deluxe.client.module.player.ServerHelperModule;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.block.Block;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

public final class ServerKeybindsHud {
   private static final Map<String, ServerKeybindsHud.Nested0514> field13735 = new HashMap<>();
   private static final List<ServerKeybindsHud.Nested0514> field13736 = new ArrayList<>();
   private static final Animation animation = new Animation();
   private static float field13738;
   private static float field13739;

   private static float method3995(ServerKeybindsHud.Nested0514 local6665_0) {
      float local6665_1 = local6665_0.animation.floatValue();
      float local6665_2 = FontText.method1396(FontFace.SEMI, local6665_0.field13789, 6.0F);
      float local6665_3 = local6665_0.field13795.isEmpty() ? 0.0F : FontText.method1396(FontFace.SEMI, local6665_0.field13795, 6.0F);
      return local6665_2 * (1.0F - local6665_1) + local6665_3 * local6665_1;
   }

   private static void method3996(MinecraftClient minecraftClient, ServerKeybindsHud.Nested0514 local6666_1, ServerHelperModule.Nested0360 local6666_2) {
      int local6666_3 = 0;
      ItemStack itemStack = ItemStack.EMPTY;

      for (int index = 0; index < minecraftClient.player.getInventory().size(); index++) {
         ItemStack stack = minecraftClient.player.getInventory().getStack(index);
         if (local6666_2.method0712(stack)) {
            local6666_3 += stack.getCount();
            if (itemStack.isEmpty()) {
               itemStack = stack;
            }
         }

         if (stack.get(DataComponentTypes.BUNDLE_CONTENTS) != null) {
            for (ItemStack itemStack2 : stack.get(DataComponentTypes.BUNDLE_CONTENTS).iterate()) {
               if (local6666_2.method0712(itemStack2)) {
                  local6666_3 += itemStack2.getCount();
                  if (itemStack.isEmpty()) {
                     itemStack = itemStack2;
                  }
               }
            }
         }

         if (Block.getBlockFromItem(stack.getItem()) instanceof ShulkerBoxBlock && stack.get(DataComponentTypes.CONTAINER) != null) {
            for (ItemStack itemStack3 : ((ContainerComponentMixin)(Object)stack.get(DataComponentTypes.CONTAINER)).getItems()) {
               if (local6666_2.method0712(itemStack3)) {
                  local6666_3 += itemStack3.getCount();
                  if (itemStack.isEmpty()) {
                     itemStack = itemStack3;
                  }
               }

               if (itemStack3.get(DataComponentTypes.BUNDLE_CONTENTS) != null) {
                  for (ItemStack itemStack4 : itemStack3.get(DataComponentTypes.BUNDLE_CONTENTS).iterate()) {
                     if (local6666_2.method0712(itemStack4)) {
                        local6666_3 += itemStack4.getCount();
                        if (itemStack.isEmpty()) {
                           itemStack = itemStack4;
                        }
                     }
                  }
               }
            }
         }
      }

      if (!itemStack.isEmpty()) {
         local6666_1.field13790 = itemStack.copy();
      }

      local6666_1.field13792 = local6666_3;
      local6666_1.field13793 = false;
      ItemStack itemStack5 = !itemStack.isEmpty()
         ? itemStack
         : (local6666_2.method0710() != null ? new ItemStack(local6666_2.method0710()) : ItemStack.EMPTY);
      if (!itemStack5.isEmpty() && minecraftClient.player.getItemCooldownManager().getCooldownProgress(itemStack5, 0.0F) > 0.001F) {
         int local6666_6 = ((ItemCooldownAccess)minecraftClient.player.getItemCooldownManager()).remainingCooldownTicks(itemStack5);
         local6666_1.field13794 = minecraftClient.player.getItemCooldownManager().getCooldownProgress(itemStack5, 0.0F);
         int local6666_7 = Math.max(0, Math.round(local6666_6 / 2.0F));
         local6666_1.field13795 = local6666_7 / 10 + "." + local6666_7 % 10 + "с";
         local6666_1.field13793 = true;
         if (itemStack.isEmpty()) {
            local6666_1.field13790 = itemStack5.copy();
         }
      }

      local6666_1.visible = local6666_3 > 0 || local6666_1.field13793;
   }

   private ServerKeybindsHud() {
   }

   private static float method4000(ServerKeybindsHud.Nested0514 local6671_0) {
      return HudStyle.method4809(8.0F + (local6671_0.field13791 == null ? 0.0F : (local6671_0.field13791.size() - 1) * 9.5F), method3995(local6671_0));
   }

   public static float method4004(MinecraftClient minecraftClient, float local6675_1, float local6675_2) {
      method4006(minecraftClient);
      float local6675_3 = 0.0F;
      float local6675_4 = 0.0F;
      int local6675_5 = 1;
      boolean local6675_6 = false;

      for (ServerKeybindsHud.Nested0514 local6675_8 : field13736) {
         float local6675_9 = method4000(local6675_8);
         if (local6675_4 > 0.0F && local6675_4 + local6675_9 > 150.0F) {
            local6675_5++;
            local6675_4 = 0.0F;
         }

         local6675_8.sortIndex = local6675_5 - 1;
         local6675_8.yOffset = local6675_4;
         local6675_4 += local6675_9 + 2.0F;
         local6675_3 = Math.max(local6675_3, local6675_4 - 2.0F);
         local6675_6 = true;
      }

      float local6675_7 = local6675_6 ? 2.0F + local6675_5 * 11.0F + (local6675_5 - 1) * 2.0F : 0.0F;
      animation.update();
      animation.animateTo(field13736.isEmpty() ? 0.0 : 1.0, 0.4, Easings.EASE_OUT_QUART);
      if (local6675_6) {
         field13738 = Math.max(local6675_3, 1.0F);
         field13739 = Math.max(local6675_7, 1.0F);
      }

      float local6675_8 = animation.floatValue();
      if (local6675_8 <= 0.001F) {
         return 0.0F;
      }

      float[] floatArray = HudManager.method3390("serverbinds", local6675_1, local6675_2);
      float local6675_10 = floatArray[0] - field13738 / 2.0F;
      float local6675_11 = floatArray[1] - field13739;
      HudManager.method3399("serverbinds", local6675_10, local6675_11, field13738, field13739, floatArray[0], floatArray[1]);
      HudRenderContext.method4341(local6675_10 + field13738 / 2.0F, local6675_11 + field13739 / 2.0F, local6675_8);

      for (ServerKeybindsHud.Nested0514 local6675_13 : field13736) {
         float local6675_14 = local6675_13.visibilityAnimation.floatValue() * local6675_8;
         if (!(local6675_14 <= 0.001F)) {
            method4005(local6675_10 + local6675_13.yOffset, local6675_11 + local6675_13.sortIndex * 13.0F, local6675_14, local6675_13);
         }
      }

      HudRenderContext.method4337();
      return field13739 * local6675_8;
   }

   private static void method4005(float local6676_0, float local6676_1, float local6676_2, ServerKeybindsHud.Nested0514 local6676_3) {
      float local6676_4 = method4000(local6676_3);
      float local6676_5 = method3995(local6676_3);
      HudStyle.method4821(local6676_0, local6676_1, local6676_4, 11.0F, local6676_2);
      float local6676_6 = local6676_0 + 2.0F + 8.0F + (local6676_3.field13791 == null ? 0.0F : (local6676_3.field13791.size() - 1) * 9.5F) + 1.0F;
      float local6676_7 = local6676_6 + 0.25F;
      HudStyle.method4810(local6676_6, local6676_1 + 5.5F, local6676_2, 3.0F);
      float local6676_8 = local6676_0
         + (local6676_7 - local6676_0 - 8.0F - (local6676_3.field13791 == null ? 0.0F : (local6676_3.field13791.size() - 1) * 9.5F)) / 2.0F;
      if (local6676_3.field13791 != null) {
         for (int index = 0; index < local6676_3.field13791.size(); index++) {
            ItemRenderCache.method2336(
               local6676_3.field13791.get(index),
               Math.round((local6676_8 + index * 9.5F) * 2.0F) / 2.0F,
               Math.round((local6676_1 + 5.5F - 4.0F) * 2.0F) / 2.0F,
               8.0F,
               ItemDrawOptions.noDecorations(local6676_2)
            );
         }
      } else if (local6676_3.field13790 != null && !local6676_3.field13790.isEmpty()) {
         float local6676_9 = Math.round(local6676_8 * 2.0F) / 2.0F;
         float local6676_10 = Math.round((local6676_1 + 5.5F - 4.0F) * 2.0F) / 2.0F;
         ItemRenderCache.method2336(local6676_3.field13790, local6676_9, local6676_10, 8.0F, ItemDrawOptions.noDecorations(local6676_2));
      }

      if (local6676_3.field13792 > 0) {
         String local6676_9 = Integer.toString(local6676_3.field13792);
         float local6676_10 = 5.0F;
         float local6676_11 = FontText.method1396(FontFace.SEMI, local6676_9, local6676_10);
         float local6676_12 = local6676_8 + 8.0F - local6676_11;
         float local6676_13 = local6676_1 + 5.5F + 4.0F - local6676_10;
         int local6676_14 = Colors.method3723(HudStyle.method4820(1.0F), -16777216, 0.1F);
         FontText.method1397(
            FontFace.SEMI, local6676_9, local6676_12, local6676_13, local6676_10, UiTheme.method2877(local6676_14, local6676_2)
         );
      }

      float local6676_9 = local6676_7 + (local6676_0 + local6676_4 - local6676_7 - local6676_5) / 2.0F;
      float local6676_10 = HudStyle.method4816(local6676_1);
      float local6676_11 = local6676_3.animation.floatValue();
      if (local6676_11 < 0.999F) {
         FontText.method1397(
            FontFace.SEMI,
            local6676_3.field13789,
            local6676_9,
            local6676_10,
            6.0F,
            UiTheme.method2877(HudStyle.method4820(1.0F), local6676_2 * (1.0F - local6676_11))
         );
      }

      if (local6676_11 > 0.001F && !local6676_3.field13795.isEmpty()) {
         int local6676_12 = Colors.method3723(-16711936, -65536, local6676_3.field13794);
         FontText.method1397(
            FontFace.SEMI,
            local6676_3.field13795,
            local6676_9,
            local6676_10,
            6.0F,
            UiTheme.method2877(local6676_12, local6676_2 * local6676_11)
         );
      }
   }

   private static void method4006(MinecraftClient minecraftClient) {
      field13736.clear();
      field13735.values().forEach(local6670_0 -> local6670_0.visible = false);
      if (minecraftClient.player != null) {
         for (ServerHelperModule.Nested0360 local6677_2 : ServerHelperModule.method0707().method0690()) {
            if (local6677_2.getKeybindSetting().isBound() && local6677_2.getKeybindSetting().isVisible()) {
               ServerKeybindsHud.Nested0514 local6677_3 = field13735.computeIfAbsent(local6677_2.getKeybindSetting().getKeyName(), ServerKeybindsHud.Nested0514::new);
               method3996(minecraftClient, local6677_3, local6677_2);
            }
         }

         for (PotionPreset.Nested0421 local6677_2 : PotionPreset.method2166().field07362) {
            if (local6677_2.field07390 && local6677_2.keybindSetting.isBound() && PotionCombinerModule.method0722().isEnabled()) {
               method4008(
                  minecraftClient,
                  field13735.computeIfAbsent(
                     "combo:" + local6677_2.field07389 + ":" + local6677_2.keybindSetting.getKeyName(),
                     local6673_1 -> new ServerKeybindsHud.Nested0514(local6677_2.keybindSetting.getKeyName())
                  ),
                  local6677_2
               );
            }
         }
      }

      boolean local6677_1 = field13735.values().stream().anyMatch(local6678_0 -> local6678_0.visible);
      if (!local6677_1 && HudRenderContext.method4342()) {
         Object[][] objectArrayArray = new Object[][]{
            {"R", Items.NETHERITE_SCRAP, 4}, {"F", Items.ENDER_EYE, 16}, {"V", Items.PHANTOM_MEMBRANE, 8}, {"X", Items.FIRE_CHARGE, 12}
         };

         for (Object[] objectArray : objectArrayArray) {
            ServerKeybindsHud.Nested0514 local6677_7 = field13735.computeIfAbsent(
               "preview:" + objectArray[0], local6672_1 -> new ServerKeybindsHud.Nested0514((String)objectArray[0])
            );
            local6677_7.field13790 = new ItemStack((Item)objectArray[1]);
            local6677_7.field13792 = (Integer)objectArray[2];
            local6677_7.field13793 = false;
            local6677_7.visible = true;
         }
      }

      for (ServerKeybindsHud.Nested0514 local6677_3 : field13735.values()) {
         if (local6677_3.visible || local6677_3.visibilityAnimation.floatValue() > 0.001) {
            field13736.add(local6677_3);
         }
      }

      field13735.entrySet()
         .removeIf(
            local6680_0 -> !local6680_0.getValue().visible
               && local6680_0.getValue().visibilityAnimation.floatValue() <= 0.001
               && local6680_0.getValue().visibilityAnimation.isFinished()
         );
      field13736.sort(
         Comparator.<ServerKeybindsHud.Nested0514>comparingInt(local6674_0 -> local6674_0.field13793 ? 1 : 0)
            .thenComparingDouble(local6667_0 -> -(8.0F + FontText.method1396(FontFace.SEMI, local6667_0.field13789, 6.0F)))
      );

      for (ServerKeybindsHud.Nested0514 local6677_3 : field13736) {
         local6677_3.visibilityAnimation.update();
         local6677_3.visibilityAnimation.animateTo(local6677_3.visible ? 1.0 : 0.0, 0.6, Easings.EASE_OUT_QUART);
         local6677_3.animation.update();
         local6677_3.animation.animateTo(local6677_3.field13793 ? 1.0 : 0.0, 0.35, Easings.EASE_OUT_QUART);
      }
   }

   private static void method4008(MinecraftClient minecraftClient, ServerKeybindsHud.Nested0514 local6679_1, PotionPreset.Nested0421 local6679_2) {
      int local6679_3 = 0;

      for (int index = 0; index < minecraftClient.player.getInventory().size(); index++) {
         ItemStack stack = minecraftClient.player.getInventory().getStack(index);
         if (!stack.isEmpty()) {
            for (ServerHelperModule.Nested0360 local6679_7 : ServerHelperModule.method0707().method0690()) {
               if (local6679_7.method0710() == null
                  && local6679_7.method0712(stack)
                  && !local6679_2.field07392.stream().noneMatch(local6669_1 -> local6669_1.field07397.equals(local6679_7.getKeybindSetting().name))) {
                  local6679_3 += stack.getCount();
                  break;
               }
            }
         }
      }

      local6679_1.field13791 = new ArrayList<>();

      for (PotionPreset.Nested0423 local6679_5 : local6679_2.field07392) {
         local6679_1.field13791.add(PotionPreset.method2168(local6679_5.field07397));
      }

      local6679_1.field13790 = local6679_1.field13791.isEmpty() ? ItemStack.EMPTY : local6679_1.field13791.get(0);
      local6679_1.field13792 = local6679_3;
      local6679_1.field13793 = false;
      local6679_1.visible = local6679_3 > 0;
   }

   private static final class Nested0514 extends DraggableHudElement {
      final String field13789;
      ItemStack field13790;
      ArrayList<ItemStack> field13791;
      int field13792;
      boolean field13793;
      float field13794;
      String field13795 = "";
      final Animation animation = new Animation();

      Nested0514(String local6682_1) {
         this.field13789 = local6682_1;
      }
   }
}
