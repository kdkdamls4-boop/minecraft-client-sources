package net.skycore.client.hud;

import com.mojang.blaze3d.platform.Window;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import net.skycore.client.SkycoreClient;
import net.skycore.client.module.ModuleManager;
import net.skycore.client.module.render.InterfaceModule;
import net.skycore.client.theme.ThemeManager;
import net.skycore.client.util.BeginFrameHelper;
import net.skycore.client.util.CurrentGuiGraphicsExtractorHelper;
import net.skycore.client.util.InterBoldHelper;
import net.skycore.client.util.SpoofElytraAsChestplateHelper;
import org.joml.Matrix3x2fStack;

public final class HotbarHud {
   private static final float HEIGHT = 29.5F;
   private static final float INDICATOR_BOTTOM = 1.5F;
   private static final float SLOT_GAP = 3.75F;
   private static final float SLOT_SIZE = 21.0F;
   private static final float INDICATOR_WIDTH = 11.5F;
   private static final float COUNT_SIZE = 7.25F;
   private static final float MAIN_PAD_X = 5.5F;
   private float lastMainCenterX;
   private static final float SLOT_RADIUS = 5.75F;
   private static final float RADIUS = 8.0F;
   private static final float GROUP_GAP = 5.75F;
   private static final float SIZE_SCALE = 1.35F;
   private final Draggable draggable = new Draggable("hotbar", 0.5F, 0.975F).lockAxis(Draggable.AxisLock.VERTICAL);
   private float animatedSelectedSlot = -1.0F;
   private static final float INDICATOR_HEIGHT = 1.5F;
   private static final float ITEM_SIZE = 17.0F;
   private static final float OFFHAND_WIDTH = 29.5F;
   private long lastFrameAt;
   private static final float SLOT_ANIMATION_BASE = 2.0E-5F;

   private static int shade(int value, float value2) {
      value2 = clamp(value2, 0.0F, 1.0F);
      int value3 = value >>> 24 & 0xFF;
      float computedValue = (value >>> 16 & 0xFF) * value2;
      int roundedValue = Math.round(computedValue);
      float computedValue2 = (value >>> 8 & 0xFF) * value2;
      int roundedValue2 = Math.round(computedValue2);
      int computedValue3 = Math.round((value & 0xFF) * value2);
      return value3 << 24 | roundedValue << 16 | roundedValue2 << 8 | computedValue3;
   }

   private static void drawIndicator(float coordinate, float coordinate2, float coordinate3, float coordinate4, float coordinate5) {
      int value = 1;
      value = (byte)-1;
      float computedValue = 11.5F * coordinate4;
      float computedValue2 = 1.5F * coordinate4;
      float maximum = Math.max(1.0F, computedValue2);
      float computedValue3 = coordinate + (coordinate3 - computedValue) * 0.5F;
      float computedValue4 = coordinate2 + coordinate3 - 1.5F * coordinate4;
      BeginFrameHelper.RectBuilder rectBuilder = BeginFrameHelper.rect(computedValue3, computedValue4, computedValue, maximum);
      ThemeManager themeManager = SkycoreClient.themes();
      value = themeManager.accent();
      value = shade(value, coordinate5);
      BeginFrameHelper.RectBuilder rectBuilder2 = rectBuilder.color(value);
      float radius = maximum * 0.5F;
      BeginFrameHelper.RectBuilder rectBuilder3 = rectBuilder2.radius(radius).hudGlow(false);
      rectBuilder3.draw();
   }

   private float hudOffsetX(int value) {
      return Float.isNaN(this.lastMainCenterX) ? 0.0F : this.lastMainCenterX - value * 0.5F;
   }

   public static float vanillaHudOffsetX() {
      Minecraft instance = Minecraft.getInstance();
      if (instance.getWindow() == null) {
         return 0.0F;
      }

      var optional = SkycoreClient.modules().get(InterfaceModule.class).filter(InterfaceModule::isHotbarEnabled);
      Function<net.skycore.client.module.render.InterfaceModule, java.lang.Float> window = interfaceModule -> {
         HotbarHud hotbarHud = interfaceModule.hotbar();
         Window window2 = instance.getWindow();
         return hotbarHud.hudOffsetX(window2.getGuiScaledWidth());
      };
      var optional2 = optional.map(window);
      Float value = 0.0F;
      return optional2.orElse(value);
   }

   private static float approach(float value, float value2, float value3, float value4) {
      float computedValue = 1.0F - (float)Math.pow(value4, value3);
      float computedValue2 = value + (value2 - value) * computedValue;
      float computedValue3 = value2 - computedValue2;
      return Math.abs(computedValue3) < 0.001F ? value2 : computedValue2;
   }

   private static int alpha(int value, float value2) {
      int value3 = value >>> 24 & 0xFF;
      float value4 = value3;
      int computedValue = Math.round(value4 * clamp(value2, 0.0F, 1.0F));
      return value & 16777215 | computedValue << 24;
   }

   public void render(float slot, float slot2) {
      Minecraft instance = Minecraft.getInstance();
      LocalPlayer player = instance.player;
      if (player == null) {
         this.draggable.setBounds(0.0F, 0.0F);
      } else {
         slot *= 1.35F;
         float computedValue = 29.5F * slot;
         float computedValue2 = 29.5F * slot;
         float computedValue3 = 5.75F * slot;
         float slot3 = 21.0F * slot;
         float computedValue4 = 3.75F * slot;
         float computedValue5 = 5.5F * slot;
         float computedValue6 = computedValue5 * 2.0F + slot3 * 9.0F + computedValue4 * 8.0F;
         float computedValue7 = computedValue2 + computedValue3 + computedValue6;
         this.draggable.setBounds(computedValue7, computedValue);
         float x = this.draggable.x() - (computedValue2 + computedValue3) * 0.5F;
         float y = this.draggable.y();
         drawBlock(x, y, computedValue2, computedValue, 8.0F * slot, slot, slot2);
         float computedValue8 = x + computedValue2 + computedValue3;
         this.lastMainCenterX = computedValue8 + computedValue6 * 0.5F;
         drawBlock(computedValue8, y, computedValue6, computedValue, 8.0F * slot, slot, slot2);
         GuiGraphicsExtractor guiGraphicsExtractor = CurrentGuiGraphicsExtractorHelper.currentGuiGraphicsExtractor();
         if (guiGraphicsExtractor != null) {
            ItemStack offhandItem = player.getOffhandItem();
            float slot4 = x + (computedValue2 - slot3) * 0.5F;
            float slot5 = y + (computedValue - slot3) * 0.5F;
            drawSlot(slot4, slot5, slot3, slot, slot2, !offhandItem.isEmpty());
            drawStack(guiGraphicsExtractor, offhandItem, slot4, slot5, slot3, slot, slot2);
            if (!offhandItem.isEmpty()) {
               drawIndicator(slot4, slot5, slot3, slot, slot2);
            }

            int visualSlot = SpoofElytraAsChestplateHelper.getVisualSlot();
            int selectedSlot = visualSlot >= 0 && visualSlot <= 8 ? visualSlot : player.getInventory().getSelectedSlot();
            long nowNanos = System.nanoTime();
            float slot6 = clamp((float)(nowNanos - this.lastFrameAt) / 1.0E9F, 0.0F, 0.05F);
            this.lastFrameAt = nowNanos;
            if (this.animatedSelectedSlot < 0.0F) {
               this.animatedSelectedSlot = selectedSlot;
            } else {
               this.animatedSelectedSlot = approach(this.animatedSelectedSlot, selectedSlot, slot6, 2.0E-5F);
            }

            for (int value = 0; value < 9; value++) {
               float slot7 = computedValue8 + computedValue5 + value * (slot3 + computedValue4);
               int value2 = value == selectedSlot ? 1 : 0;
               drawSlot(slot7, slot5, slot3, slot, slot2, (value2 != 0));
               ItemStack item = player.getInventory().getItem(value);
               ItemStack hotbarSpoofStack = SpoofElytraAsChestplateHelper.getHotbarSpoofStack(value, item);
               drawStack(guiGraphicsExtractor, hotbarSpoofStack, slot7, slot5, slot3, slot, slot2);
            }

            float computedValue9 = computedValue8 + computedValue5 + this.animatedSelectedSlot * (slot3 + computedValue4);
            drawIndicator(computedValue9, slot5, slot3, slot, slot2);
         }
      }
   }

   private static void drawStack(GuiGraphicsExtractor guiGraphicsExtractor, ItemStack itemStack, float coordinate, float coordinate2, float coordinate3, float coordinate4, float alpha) {
      if (itemStack != null && !itemStack.isEmpty()) {
         float computedValue = 17.0F * coordinate4;
         float computedValue2 = coordinate + (coordinate3 - computedValue) * 0.5F;
         float computedValue3 = coordinate2 + (coordinate3 - computedValue) * 0.5F - 0.5F * coordinate4;
         BeginFrameHelper.flush();
         drawItem(guiGraphicsExtractor, itemStack, computedValue2, computedValue3, computedValue);
         int count = itemStack.getCount();
         if (count > 1) {
            String text = Integer.toString(count);
            float width = 7.25F * coordinate4;
            float width2 = InterBoldHelper.forWeight(600).getWidth(text, width);
            BeginFrameHelper.text(coordinate + coordinate3 - width2 - 1.0F * coordinate4, coordinate2 + coordinate3 - width - 0.3F * coordinate4, width, text)
               .weight(600)
               .color(shade(alpha(-1, 0.75F), alpha))
               .draw();
         }
      }
   }

   public Draggable draggable() {
      return this.draggable;
   }

   public static boolean shouldReplaceVanilla() {
      ModuleManager moduleManager = SkycoreClient.modules();
      var optional = moduleManager.get(InterfaceModule.class);
      Function<net.skycore.client.module.render.InterfaceModule, java.lang.Boolean> function = InterfaceModule::isHotbarEnabled;
      var optional2 = optional.map(function);
      Boolean condition = false;
      return optional2.orElse(condition);
   }

   private static float clamp(float value, float value2, float value3) {
      return Math.max(value2, Math.min(value3, value));
   }

   private float hudOffsetY(int value) {
      if (this.draggable.height() <= 0.0F) {
         return 0.0F;
      }

      float height = value;
      float computedValue = height - this.draggable.height();
      float maximum = Math.max(0.0F, computedValue);
      float computedValue2 = this.draggable.defaultNormalizedY() * maximum;
      return this.draggable.y() - computedValue2 - 17.0F;
   }

   public static float vanillaHudOffsetY() {
      Minecraft instance = Minecraft.getInstance();
      if (instance.getWindow() == null) {
         return 0.0F;
      }

      var optional = SkycoreClient.modules().get(InterfaceModule.class);
      Predicate<net.skycore.client.module.render.InterfaceModule> predicate = InterfaceModule::isHotbarEnabled;
      var optional2 = optional.filter(predicate);
      Function<net.skycore.client.module.render.InterfaceModule, java.lang.Float> window = interfaceModule -> {
         int height = -1;
         HotbarHud hotbarHud = interfaceModule.hotbar();
         height = instance.getWindow().getGuiScaledHeight();
         float result = hotbarHud.hudOffsetY(height);
         return result;
      };
      Float value = optional2.<Float>map(window).orElse(0.0F);
      return value;
   }

   private static void drawSlot(float coordinate, float coordinate2, float coordinate3, float coordinate4, float alpha, boolean condition) {
      int color = shade(alpha(-1, condition ? 0.03F : 0.01F), alpha);
      BeginFrameHelper.RectBuilder rectBuilder = BeginFrameHelper.rect(coordinate, coordinate2, coordinate3, coordinate3).color(color).radius(5.75F * coordinate4).hudGlow(false);
      rectBuilder.draw();
   }

   private static void drawBlock(float coordinate, float coordinate2, float coordinate3, float coordinate4, float color, float radius, float coordinate5) {
      if (HudStyle.transparent()) {
         BeginFrameHelper.RectBuilder rectBuilder = BeginFrameHelper.rect(coordinate, coordinate2, coordinate3, coordinate4);
         int color2 = HudStyle.background(1.0F);
         color2 = shade(color2, coordinate5);
         rectBuilder = rectBuilder.color(color2).radius(color);
         rectBuilder = rectBuilder.hudGlow(false);
         float value = HudStyle.blurRadius(radius);
         rectBuilder.blur(value, 1.0F).draw();
      } else {
         BeginFrameHelper.RectBuilder rectBuilder2 = BeginFrameHelper.rect(coordinate, coordinate2, coordinate3, coordinate4);
         int color3 = shade(-15856114, coordinate5);
         rectBuilder2.color(color3).radius(color).hudGlow(false).draw();
      }
   }

   private static void drawItem(GuiGraphicsExtractor guiGraphicsExtractor, ItemStack itemStack, float coordinate, float coordinate2, float coordinate3) {
      Minecraft instance = Minecraft.getInstance();
      Matrix3x2fStack matrix3x2fStack = guiGraphicsExtractor.pose();
      Window window = instance.getWindow();
      float guiScale = window.getGuiScale();
      float computedValue = coordinate3 / 16.0F;
      float computedValue2 = coordinate * guiScale;
      float computedValue3 = Math.round(computedValue2) / guiScale;
      float computedValue4 = coordinate2 * guiScale;
      float computedValue5 = Math.round(computedValue4) / guiScale;
      matrix3x2fStack.pushMatrix();
      matrix3x2fStack.translate(computedValue3, computedValue5);
      matrix3x2fStack.scale(computedValue);
      guiGraphicsExtractor.item(itemStack, 0, 0);
      matrix3x2fStack.popMatrix();
   }

   public HotbarHud() {
      this.lastMainCenterX = Float.NaN;
      this.lastFrameAt = System.nanoTime();
   }
}
