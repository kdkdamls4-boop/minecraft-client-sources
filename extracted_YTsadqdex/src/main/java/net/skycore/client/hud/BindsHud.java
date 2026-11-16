package net.skycore.client.hud;

import com.mojang.blaze3d.platform.Window;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.skycore.client.module.ClientModule;
import net.skycore.client.module.KeyBinds;
import net.skycore.client.setting.Setting;
import net.skycore.client.util.BeginFrameHelper;
import net.skycore.client.util.CaptureHelper;
import net.skycore.client.setting.KeyBindSetting;
import net.skycore.client.setting.ModeSetting;
import net.skycore.client.util.CurrentGuiGraphicsExtractorHelper;
import net.skycore.client.util.InterBoldHelper;
import net.skycore.client.util.TextHeightHelper;
import org.joml.Matrix3x2fStack;

public final class BindsHud {
   private static final float KEY_CHIP_MIN_WIDTH = 22.0F;
   private static final float KEY_ICON_SIZE = 7.0F;
   private static final float HEIGHT = 24.0F;
   private static final float KEY_RIGHT_PAD = 4.0F;
   private static final float KEY_CHIP_HEIGHT = 10.5F;
   private final Draggable draggable = new Draggable("binds", 0.0065F, 0.145F);
   private static final float KEY_TEXT_SIZE = 7.5F;
   private static final float SEPARATOR_HEIGHT = 10.0F;
   private static final float PAD_LEFT = 6.0F;
   private final Map<KeyBindSetting, BindsHud.CardState> cards = new IdentityHashMap<>();
   private static final float KEY_LEFT_PAD = 4.0F;
   private long lastFrameAt = System.nanoTime();
   private static final float RADIUS = 10.5F;
   private static final float ITEM_SIZE = 12.5F;
   private static final float KEY_CHIP_RADIUS = 5.25F;
   private static final float ITEM_SEPARATOR_GAP = 6.0F;
   private static final float MOTION_BASE = 4.0E-6F;
   private static final float CARD_GAP = 7.0F;
   private static final float SEPARATOR_KEY_GAP = 6.0F;
   private static final float VISIBILITY_BASE = 1.0E-5F;
   private static final float PAD_RIGHT = 6.0F;
   private float visibilityProgress;
   private static final float SEPARATOR_WIDTH = 1.5F;
   private static final float KEY_ICON_GAP = 3.0F;

   private List<BindsHud.BoundItem> collect(List<ClientModule> clientModules, LocalPlayer player) {
      ArrayList elements = new ArrayList();

      for (ClientModule clientModule : clientModules) {
         if (clientModule.isEnabled()) {
            for (Setting setting : clientModule.getSettings()) {
               if (setting instanceof KeyBindSetting keyBindSetting && keyBindSetting.get() != -1 && keyBindSetting.isVisible()) {
                  ItemStack itemStack = resolveItem(clientModule, keyBindSetting, player);
                  if (itemStack != null && !itemStack.isEmpty()) {
                     elements.add(new BindsHud.BoundItem(keyBindSetting, itemStack, KeyBinds.name(keyBindSetting.get())));
                  }
               }
            }
         }
      }

      return elements;
   }

   private static void drawItem(GuiGraphicsExtractor guiGraphicsExtractor, ItemStack itemStack, float coordinate, float coordinate2, float coordinate3) {
      Minecraft instance = Minecraft.getInstance();
      Matrix3x2fStack matrix3x2fStack = guiGraphicsExtractor.pose();
      Window window = instance.getWindow();
      float guiScale = window.getGuiScale();
      float computedValue = coordinate3 / 16.0F;
      float computedValue2 = Math.round(coordinate * guiScale) / guiScale;
      float computedValue3 = Math.round(coordinate2 * guiScale) / guiScale;
      matrix3x2fStack.pushMatrix();
      matrix3x2fStack.translate(computedValue2, computedValue3);
      matrix3x2fStack.scale(computedValue);
      guiGraphicsExtractor.item(itemStack, 0, 0);
      matrix3x2fStack.popMatrix();
   }

   private void syncCards(List<BindsHud.BoundItem> boundItems, float progress) {
      for (BindsHud.CardState cardState : this.cards.values()) {
         cardState.target = false;
      }

      int value = 0;

      for (BindsHud.BoundItem boundItem : boundItems) {
         BindsHud.CardState cardState2 = this.cards.get(boundItem.setting);
         if (cardState2 == null) {
            cardState2 = new BindsHud.CardState(value);
            this.cards.put(boundItem.setting, cardState2);
         }

         cardState2.target = true;
         cardState2.targetIndex = value++;
         cardState2.stack = boundItem.stack;
         cardState2.key = boundItem.key;
      }

      Iterator iterator = this.cards.entrySet().iterator();

      while (iterator.hasNext()) {
         BindsHud.CardState value2 = (BindsHud.CardState)((Entry)iterator.next()).getValue();
         value2.progress = approach(value2.progress, value2.target ? 1.0F : 0.0F, progress, 4.0E-6F);
         value2.visualIndex = approach(value2.visualIndex, value2.targetIndex, progress, 3.0E-5F);
         if (!value2.target && value2.progress < 0.005F) {
            iterator.remove();
         }
      }
   }

   private static int shade(int value, float value2) {
      value2 = clamp(value2, 0.0F, 1.0F);
      int value3 = value >>> 24 & 0xFF;
      int computedValue = Math.round((value >>> 16 & 0xFF) * value2);
      float computedValue2 = (value >>> 8 & 0xFF) * value2;
      int roundedValue = Math.round(computedValue2);
      float computedValue3 = (value & 0xFF) * value2;
      int roundedValue2 = Math.round(computedValue3);
      return value3 << 24 | computedValue << 16 | roundedValue << 8 | roundedValue2;
   }

   public void render(float width, float coordinate, List<ClientModule> clientModules, boolean condition) {
      float progress = this.deltaSeconds();
      LocalPlayer instance = Minecraft.getInstance().player;
      var boundItems = this.collect(clientModules, instance);
      this.syncCards(boundItems, progress);
      int value = !condition && boundItems.isEmpty() ? 0 : 1;
      this.visibilityProgress = approach(this.visibilityProgress, value != 0 ? 1.0F : 0.0F, progress, 1.0E-5F);
      float value2 = smoothstep(this.visibilityProgress);
      if (value == 0 && value2 <= 0.001F) {
         this.draggable.setBounds(0.0F, 0.0F);
      } else {
         var boundItems2 = this.visibleItems(boundItems);
         if (boundItems2.isEmpty() && condition) {
            boundItems2 = List.of(new BindsHud.BoundItem(null, Items.ENDER_PEARL.getDefaultInstance(), KeyBinds.name(82)));
         }

         if (boundItems2.isEmpty()) {
            this.draggable.setBounds(0.0F, 0.0F);
         } else {
            float x = this.draggable.x();
            float y = this.draggable.y();
            float index = 24.0F * width;
            float index2 = 7.0F * width;
            float width2 = 0.0F;

            for (int value3 = 0; value3 < boundItems2.size(); value3++) {
               width2 += this.cardWidth((BindsHud.BoundItem)boundItems2.get(value3), width);
               if (value3 + 1 < boundItems2.size()) {
                  width2 += index2;
               }
            }

            this.draggable.setBounds(width2, index);
            float index3 = x;

            for (BindsHud.BoundItem boundItem : boundItems2) {
               BindsHud.CardState cardState = boundItem.setting == null ? null : this.cards.get(boundItem.setting);
               float value4 = cardState == null ? 1.0F : smoothstep(cardState.progress);
               float computedValue = value4 * value2;
               float computedValue2 = (1.0F - value4) * 3.0F * width;
               float value5 = this.cardWidth(boundItem, width);
               this.drawCard(boundItem, index3, y + computedValue2, value5, index, width, coordinate, computedValue);
               index3 += value5 + index2;
            }
         }
      }
   }

   private static float smoothstep(float progress) {
      progress = clamp(progress, 0.0F, 1.0F);
      return progress * progress * (3.0F - 2.0F * progress);
   }

   private static String normalize(String text) {
      String text2;
      if (text == null) {
         text2 = "";
      } else {
         String text3 = text.toLowerCase();
         text2 = text3.replace(" key", "").replace(" bind", "").replace(" ", "").replace("_", "").replace("-", "");
      }

      return text2;
   }

   private float cardWidth(BindsHud.BoundItem boundItem, float value) {
      float width = 7.5F * value;
      TextHeightHelper textHeightHelper = InterBoldHelper.forWeight(600);
      float width2 = textHeightHelper.getWidth(boundItem.key, width);
      TextHeightHelper textHeightHelper2 = InterBoldHelper.csGuiIcons();
      float width3 = 7.0F * value;
      float width4 = textHeightHelper2.getWidth("\ue004", width3);
      float maximum = Math.max(22.0F * value, 4.0F * value + width2 + 3.0F * value + width4 + 4.0F * value);
      return 6.0F * value + 12.5F * value + 6.0F * value + 1.5F * value + 6.0F * value + maximum + 6.0F * value;
   }

   private void drawCard(BindsHud.BoundItem boundItem, float coordinate, float coordinate2, float coordinate3, float coordinate4, float coordinate5, float alpha, float alpha2) {
      drawBlock(coordinate, coordinate2, coordinate3, coordinate4, 10.5F * coordinate5, coordinate5, alpha, alpha2);
      float computedValue = 12.5F * coordinate5;
      float computedValue2 = coordinate + 6.0F * coordinate5;
      float computedValue3 = coordinate2 + (coordinate4 - computedValue) * 0.5F;
      GuiGraphicsExtractor guiGraphicsExtractor = CurrentGuiGraphicsExtractorHelper.currentGuiGraphicsExtractor();
      if (guiGraphicsExtractor != null && !boundItem.stack.isEmpty()) {
         BeginFrameHelper.flush();
         if (alpha2 >= 0.999F) {
            drawItem(guiGraphicsExtractor, boundItem.stack, computedValue2, computedValue3, computedValue);
         } else {
            CaptureHelper.withAlpha(alpha2, () -> {
               drawItem(guiGraphicsExtractor, boundItem.stack, computedValue2, computedValue3, computedValue);
            });
         }
      }

      float computedValue4 = computedValue2 + computedValue + 6.0F * coordinate5;
      float computedValue5 = 10.0F * coordinate5;
      BeginFrameHelper.rect(computedValue4, coordinate2 + (coordinate4 - computedValue5) * 0.5F, 1.5F * coordinate5, computedValue5)
         .color(shade(alpha(-1, 0.03F * alpha2), alpha))
         .radius(1.5F * coordinate5)
         .hudGlow(false)
         .draw();
      String text = "\ue004";
      float width = 7.5F * coordinate5;
      float width2 = InterBoldHelper.forWeight(600).getWidth(boundItem.key, width);
      float width3 = 7.0F * coordinate5;
      float width4 = InterBoldHelper.csGuiIcons().getWidth(text, width3);
      float maximum = Math.max(22.0F * coordinate5, 4.0F * coordinate5 + width2 + 3.0F * coordinate5 + width4 + 4.0F * coordinate5);
      float computedValue6 = 10.5F * coordinate5;
      float computedValue7 = computedValue4 + 1.5F * coordinate5 + 6.0F * coordinate5;
      float computedValue8 = coordinate2 + (coordinate4 - computedValue6) * 0.5F;
      BeginFrameHelper.rect(computedValue7, computedValue8, maximum, computedValue6).color(shade(alpha(-1, 0.02F * alpha2), alpha)).radius(5.25F * coordinate5).hudGlow(false).draw();
      BeginFrameHelper.text(computedValue7 + 4.0F * coordinate5, computedValue8 + 2.0F * coordinate5 - 0.6F, width, boundItem.key).weight(600).color(shade(alpha(-1, 0.5F * alpha2), alpha)).draw();
      BeginFrameHelper.text(computedValue7 + maximum - 4.0F * coordinate5 - width4, computedValue8 + 1.8F * coordinate5, width3, text)
         .font(InterBoldHelper.csGuiIcons())
         .color(shade(alpha(-1, 0.25F * alpha2), alpha))
         .draw();
   }

   private float deltaSeconds() {
      long nowNanos = System.nanoTime();
      float intermediate = (float)(nowNanos - this.lastFrameAt) / 1.0E9F;
      float clampedValue = clamp(intermediate, 0.0F, 0.05F);
      this.lastFrameAt = nowNanos;
      return clampedValue;
   }

   private static ItemStack findInventoryMatch(LocalPlayer player, String text) {
      if (player != null && text != null && !text.isBlank()) {
         String text2 = normalize(text);

         for (int value = 0; value < 36; value++) {
            ItemStack item = player.getInventory().getItem(value);
            if (matches(item, text2)) {
               return item.copy();
            }
         }

         ItemStack offhandItem = player.getOffhandItem();
         return matches(offhandItem, text2) ? offhandItem.copy() : ItemStack.EMPTY;
      } else {
         return ItemStack.EMPTY;
      }
   }

   private List<BindsHud.BoundItem> visibleItems(List<BindsHud.BoundItem> boundItems) {
      ArrayList<BindsHud.BoundItem> boundItems2 = new ArrayList<>(boundItems);

      for (Entry entry : this.cards.entrySet()) {
         BindsHud.CardState value = (BindsHud.CardState)entry.getValue();
         if (!value.target && !(value.progress <= 0.005F)) {
            boolean key = boundItems2.stream().anyMatch(boundItem -> {
               return boundItem.setting == entry.getKey();
            });
            if (!key) {
               boundItems2.add(new BindsHud.BoundItem((KeyBindSetting)entry.getKey(), value.stack, value.key));
            }
         }
      }

      return boundItems2;
   }

   private static Item itemForMode(String text) {
      return switch (text == null ? "" : text.toLowerCase()) {
         case "axe" -> Items.DIAMOND_AXE;
         case "totem" -> Items.TOTEM_OF_UNDYING;
         case "elytra" -> Items.ELYTRA;
         default -> Items.DIAMOND_SWORD;
      };
   }

   private static int alpha(int value, float value2) {
      int value3 = value >>> 24 & 0xFF;
      float value4 = value3;
      float computedValue = value4 * clamp(value2, 0.0F, 1.0F);
      int roundedValue = Math.round(computedValue);
      return value & 16777215 | roundedValue << 24;
   }

   private static ItemStack resolveItem(ClientModule clientModule, KeyBindSetting keyBindSetting, LocalPlayer player) {
      String name = clientModule.getName();
      String name2 = keyBindSetting.getName();
      if ("Elytra Helper".equals(name)) {
         if ("Swap Key".equals(name2)) {
            return Items.ELYTRA.getDefaultInstance();
         }

         if ("Firework Key".equals(name2)) {
            return Items.FIREWORK_ROCKET.getDefaultInstance();
         }
      }

      if ("Click Pearl".equals(name)) {
         return Items.ENDER_PEARL.getDefaultInstance();
      }

      if ("Fast EXP".equals(name)) {
         return Items.EXPERIENCE_BOTTLE.getDefaultInstance();
      }

      if ("Arrow Swap".equals(name)) {
         return Items.ARROW.getDefaultInstance();
      }

      if ("ReallyWorld Helper".equals(name)) {
         if ("Anti flight key".equals(name2)) {
            return Items.FIREWORK_STAR.getDefaultInstance();
         }

         if ("Trap key".equals(name2)) {
            return Items.HEART_OF_THE_SEA.getDefaultInstance();
         }
      }

      if ("FT Helper".equals(name)) {
         if ("Open Shulker".equals(name2)) {
            return Items.SHULKER_BOX.getDefaultInstance();
         }

         if ("Wind Charge".equals(name2)) {
            return Items.WIND_CHARGE.getDefaultInstance();
         }

         if (name2.toLowerCase().contains("potion") || "Holy Water".equals(name2) || "Popper".equals(name2)) {
            return Items.SPLASH_POTION.getDefaultInstance();
         }
      }

      if ("Auto Swap".equals(name)) {
         if ("Helmet Swap Key".equals(name2)) {
            return Items.ELYTRA.getDefaultInstance();
         }

         if ("Swap Key".equals(name2)) {
            String text = modeValue(clientModule, "Hand Item 1");
            return itemForMode(text).getDefaultInstance();
         }
      }

      ItemStack inventoryMatch = findInventoryMatch(player, name2);
      return inventoryMatch == null ? ItemStack.EMPTY : inventoryMatch;
   }

   private static boolean matches(ItemStack itemStack, String text) {
      if (itemStack != null) {
         if (!itemStack.isEmpty()) {
            String string = itemStack.getHoverName().getString();
            String text2 = normalize(string);
            if (!text.isEmpty()) {
               if (text2.contains(text)) {
                  return true;
               }

               if (text.contains(text2)) {
                  return true;
               }
            }

            return false;
         }
      }

      return false;
   }

   private static String modeValue(ClientModule clientModule, String text) {
      var settings = clientModule.getSettings();
      Iterator iterator = settings.iterator();

      while (true) {
         if (!iterator.hasNext()) {
            return "";
         }

         Setting element = (Setting)iterator.next();
         if (element instanceof ModeSetting modeSetting) {
            if (text.equals(element.getName())) {
               return modeSetting.get();
            }
         }
      }
   }

   public Draggable draggable() {
      return this.draggable;
   }

   private static float approach(float value, float value2, float value3, float value4) {
      if (value == 0.0F && value2 > 0.0F) {
         return Math.min(value2, 0.08F);
      }

      double intermediate = value4;
      double intermediate2 = value3;
      float computedValue = 1.0F - (float)Math.pow(intermediate, intermediate2);
      float computedValue2 = value + (value2 - value) * computedValue;
      return Math.abs(value2 - computedValue2) < 0.001F ? value2 : computedValue2;
   }

   private static float clamp(float value, float value2, float value3) {
      float minimum = Math.min(value3, value);
      return Math.max(value2, minimum);
   }

   private static void drawBlock(float coordinate, float coordinate2, float coordinate3, float coordinate4, float radius, float radius2, float alpha, float alpha2) {
      int intermediate = -1;
      if (HudStyle.transparent()) {
         BeginFrameHelper.RectBuilder rectBuilder = BeginFrameHelper.rect(coordinate, coordinate2, coordinate3, coordinate4);
         intermediate = HudStyle.background(alpha2);
         BeginFrameHelper.RectBuilder rectBuilder2 = rectBuilder.color(shade(intermediate, alpha));
         BeginFrameHelper.RectBuilder rectBuilder3 = rectBuilder2.radius(radius);
         BeginFrameHelper.RectBuilder rectBuilder4 = rectBuilder3.hudGlow(false).blur(HudStyle.blurRadius(radius2), alpha2);
         rectBuilder4.draw();
      } else {
         BeginFrameHelper.RectBuilder rectBuilder5 = BeginFrameHelper.rect(coordinate, coordinate2, coordinate3, coordinate4).color(shade(alpha(-15856114, alpha2), alpha));
         BeginFrameHelper.RectBuilder rectBuilder6 = rectBuilder5.radius(radius);
         BeginFrameHelper.RectBuilder rectBuilder7 = rectBuilder6.hudGlow(false);
         rectBuilder7.draw();
      }
   }

   public record BoundItem(KeyBindSetting setting, ItemStack stack, String key) {
      public ItemStack stack() {
         return this.stack;
      }

      public String key() {
         return this.key;
      }

      public KeyBindSetting setting() {
         return this.setting;
      }
   }

   static final class CardState {
      private ItemStack stack;
      private int targetIndex;
      private String key;
      private boolean target = true;
      private float progress;
      private float visualIndex;

      private CardState(int visualIndex2) {
         this.stack = ItemStack.EMPTY;
         this.key = "";
         this.visualIndex = visualIndex2;
         this.targetIndex = visualIndex2;
      }
   }
}
