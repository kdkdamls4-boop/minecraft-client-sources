package net.skycore.client.hud;

import com.mojang.blaze3d.platform.Window;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.skycore.client.SkycoreClient;
import net.skycore.client.config.ConfigManager;
import net.skycore.client.module.ClientModule;
import net.skycore.client.module.ModuleManager;
import net.skycore.client.module.render.ClickGUIModule;
import net.skycore.client.module.render.InterfaceModule;
import net.skycore.client.util.BeginFrameHelper;
import net.skycore.client.util.CurrentGuiGraphicsExtractorHelper;

public final class HudRenderer {
   private final ConfigManager config;
   private final Set<String> appliedPositions;
   private final ModuleManager modules;
   private int lastScreenHeight;
   private final DraggableManager draggables = new DraggableManager();
   private long lastFrameAt = System.nanoTime();
   private final InterfaceModule interfaceModule;
   private int lastScreenWidth = -1;
   private final ClickGUIModule clickGuiModule;

   public HudRenderer(ModuleManager interfaceModule2, ConfigManager config2) {
      this.lastScreenHeight = -1;
      this.appliedPositions = new HashSet<>();
      this.interfaceModule = interfaceModule2.get(InterfaceModule.class).orElseThrow();
      this.clickGuiModule = interfaceModule2.get(ClickGUIModule.class).orElseThrow();
      this.modules = interfaceModule2;
      this.config = config2;
      this.draggables.register(this.interfaceModule.watermark().draggable());
      this.draggables.register(this.interfaceModule.information().draggable());
      this.draggables.register(this.interfaceModule.hotkeys().draggable());
      this.draggables.register(this.interfaceModule.binds().draggable());
      this.draggables.register(this.interfaceModule.hotbar().draggable());
      this.draggables.register(this.interfaceModule.potions().draggable());
      this.draggables.register(this.interfaceModule.cooldowns().draggable());
      this.draggables.register(this.interfaceModule.armorHud().draggable());
      this.draggables.register(this.interfaceModule.targetHud().draggable());
      this.draggables.register(this.interfaceModule.notifications().draggable());
      ClientModule.addToggleListener((clientModule, condition) -> {
         this.interfaceModule.notifications().onModuleToggle(clientModule, condition);
      });
   }

   public void applySavedPositions() {
      Minecraft instance = Minecraft.getInstance();
      if (instance.getWindow() != null) {
         int guiScaledWidth = instance.getWindow().getGuiScaledWidth();
         int guiScaledHeight = instance.getWindow().getGuiScaledHeight();

         for (Draggable draggable : this.draggables.all()) {
            if (!this.appliedPositions.contains(draggable.id()) && !(draggable.width() <= 0.0F) && !(draggable.height() <= 0.0F)) {
               ConfigManager.DraggablePosition draggablePosition = this.config.getDraggablePosition(draggable.id());
               float x = draggablePosition == null ? draggable.defaultNormalizedX() : draggablePosition.x();
               float y = draggablePosition == null ? draggable.defaultNormalizedY() : draggablePosition.y();
               if (draggable.axisLock() == Draggable.AxisLock.VERTICAL) {
                  x = 0.5F;
               } else if (draggable.axisLock() == Draggable.AxisLock.HORIZONTAL) {
                  y = 0.5F;
               }

               float computedValue = Math.max(0.0F, guiScaledWidth - draggable.width());
               float computedValue2 = Math.max(0.0F, guiScaledHeight - draggable.height());
               draggable.setPosition(clamp(x, 0.0F, 1.0F) * computedValue, clamp(y, 0.0F, 1.0F) * computedValue2);
               this.appliedPositions.add(draggable.id());
            }
         }

         this.lastScreenWidth = guiScaledWidth;
         this.lastScreenHeight = guiScaledHeight;
      }
   }

   private float interfaceScale(Minecraft minecraft) {
      Window window = minecraft.getWindow();
      float guiScale = Math.max(0.6666667F, window.getGuiScale() / 3.0F);
      Double value = this.clickGuiModule.renderFactor().get();
      float value2 = value.floatValue();
      return guiScale * value2 / 0.5F;
   }

   public void render(GuiRenderState guiRenderState, DeltaTracker deltaTracker) {
      Minecraft instance = Minecraft.getInstance();
      if (instance.level != null && instance.player != null) {
         int guiScaledWidth = instance.getWindow().getGuiScaledWidth();
         int guiScaledHeight = instance.getWindow().getGuiScaledHeight();
         this.reprojectIfNeeded(guiScaledWidth, guiScaledHeight);
         long nowNanos = System.nanoTime();
         float computedValue = clamp((float)(nowNanos - this.lastFrameAt) / 1.0E9F, 0.0F, 0.05F);
         this.lastFrameAt = nowNanos;
         if (this.draggables.active() != null) {
            if (instance.gui.screen() instanceof ChatScreen) {
               this.draggables.dragTo(this.mouseX(instance), this.mouseY(instance), guiScaledWidth, guiScaledHeight);
            } else {
               Draggable draggable = this.draggables.endDrag();
               if (draggable != null) {
                  this.savePosition(draggable, guiScaledWidth, guiScaledHeight);
               }
            }
         }

         this.draggables.updateMotion(computedValue, guiScaledWidth, guiScaledHeight);
         boolean watermarkEnabled = this.interfaceModule.isWatermarkEnabled();
         boolean informationEnabled = this.interfaceModule.isInformationEnabled();
         boolean hotkeysEnabled = this.interfaceModule.isHotkeysEnabled();
         boolean bindsEnabled = this.interfaceModule.isBindsEnabled();
         boolean hotbarEnabled = this.interfaceModule.isHotbarEnabled();
         boolean potionsEnabled = this.interfaceModule.isPotionsEnabled();
         boolean cooldownsEnabled = this.interfaceModule.isCooldownsEnabled();
         boolean armorHudEnabled = this.interfaceModule.isArmorHudEnabled();
         boolean targetHudEnabled = this.interfaceModule.isTargetHudEnabled();
         boolean notificationsEnabled = this.interfaceModule.isNotificationsEnabled();
         if (notificationsEnabled) {
            this.interfaceModule.notifications().tick(instance.player);
         } else {
            this.interfaceModule.notifications().tick(null);
         }

         if (watermarkEnabled || informationEnabled || hotkeysEnabled || bindsEnabled || hotbarEnabled || potionsEnabled || cooldownsEnabled || armorHudEnabled || targetHudEnabled || notificationsEnabled) {
            boolean condition = instance.gui.screen() instanceof ChatScreen;
            GuiGraphicsExtractor guiGraphicsExtractor = new GuiGraphicsExtractor(instance, guiRenderState, (int)Math.round(this.mouseX(instance)), (int)Math.round(this.mouseY(instance)));
            CurrentGuiGraphicsExtractorHelper.enter2D(guiGraphicsExtractor, deltaTracker);
            BeginFrameHelper.beginFrame();

            try {
               BeginFrameHelper.setHudGlowColor(0);
               int color = this.interfaceModule.glowEnabled() ? SkycoreClient.themes().accent() : 0;
               BeginFrameHelper.setHudIconGlowColor(color);
               float value = this.interfaceScale(instance);
               if (watermarkEnabled) {
                  this.interfaceModule.watermark().render(value, 1.0F);
               }

               if (informationEnabled) {
                  this.interfaceModule.information().render(value, 1.0F, condition);
               }

               if (hotkeysEnabled) {
                  this.interfaceModule.hotkeys().render(value, 1.0F, this.modules.getModules(), condition);
               }

               if (bindsEnabled) {
                  this.interfaceModule.binds().render(value, 1.0F, this.modules.getModules(), condition);
               }

               if (hotbarEnabled) {
                  BeginFrameHelper.setHudIconGlowColor(0);
                  this.interfaceModule.hotbar().render(value, 1.0F);
                  BeginFrameHelper.setHudIconGlowColor(color);
               }

               if (potionsEnabled) {
                  this.interfaceModule.potions().render(value, 1.0F, condition);
               }

               if (cooldownsEnabled) {
                  this.interfaceModule.cooldowns().render(value, 1.0F, condition);
               }

               if (armorHudEnabled) {
                  this.interfaceModule.armorHud().render(value, 1.0F, condition);
               }

               if (targetHudEnabled) {
                  this.interfaceModule.targetHud().render(value, condition);
               }

               if (notificationsEnabled) {
                  this.interfaceModule.notifications().render(value, condition, guiScaledWidth);
               }

               BeginFrameHelper.flush();
               this.applySavedPositions();
            } finally {
               BeginFrameHelper.setHudGlowColor(0);
               BeginFrameHelper.setHudIconGlowColor(0);
               CurrentGuiGraphicsExtractorHelper.exit2D();
            }
         }
      }
   }

   private List<Draggable> activeDraggables() {
      ArrayList elements = new ArrayList();
      if (this.interfaceModule.isWatermarkEnabled()) {
         elements.add(this.interfaceModule.watermark().draggable());
      }

      if (this.interfaceModule.isInformationEnabled()) {
         elements.add(this.interfaceModule.information().draggable());
      }

      if (this.interfaceModule.isHotkeysEnabled()) {
         elements.add(this.interfaceModule.hotkeys().draggable());
      }

      if (this.interfaceModule.isBindsEnabled()) {
         elements.add(this.interfaceModule.binds().draggable());
      }

      if (this.interfaceModule.isHotbarEnabled()) {
         elements.add(this.interfaceModule.hotbar().draggable());
      }

      if (this.interfaceModule.isPotionsEnabled()) {
         elements.add(this.interfaceModule.potions().draggable());
      }

      if (this.interfaceModule.isCooldownsEnabled()) {
         elements.add(this.interfaceModule.cooldowns().draggable());
      }

      if (this.interfaceModule.isArmorHudEnabled()) {
         elements.add(this.interfaceModule.armorHud().draggable());
      }

      if (this.interfaceModule.isTargetHudEnabled()) {
         elements.add(this.interfaceModule.targetHud().draggable());
      }

      if (this.interfaceModule.isNotificationsEnabled()) {
         elements.add(this.interfaceModule.notifications().draggable());
      }

      return elements;
   }

   private void reprojectIfNeeded(int lastScreenWidth2, int lastScreenHeight2) {
      if (this.lastScreenWidth < 0 || this.lastScreenHeight < 0) {
         this.lastScreenWidth = lastScreenWidth2;
         this.lastScreenHeight = lastScreenHeight2;
      } else if (lastScreenWidth2 != this.lastScreenWidth || lastScreenHeight2 != this.lastScreenHeight) {
         for (Draggable draggable : this.draggables.all()) {
            float computedValue = Math.max(1.0F, this.lastScreenWidth - draggable.width());
            float computedValue2 = Math.max(1.0F, this.lastScreenHeight - draggable.height());
            float computedValue3 = clamp(draggable.targetX() / computedValue, 0.0F, 1.0F);
            float computedValue4 = clamp(draggable.targetY() / computedValue2, 0.0F, 1.0F);
            float computedValue5 = Math.max(0.0F, lastScreenWidth2 - draggable.width());
            float computedValue6 = Math.max(0.0F, lastScreenHeight2 - draggable.height());
            draggable.setPosition(computedValue3 * computedValue5, computedValue4 * computedValue6);
         }

         this.lastScreenWidth = lastScreenWidth2;
         this.lastScreenHeight = lastScreenHeight2;
      }
   }

   public DraggableManager draggables() {
      return this.draggables;
   }

   private double mouseX(Minecraft minecraft) {
      return minecraft.mouseHandler.getScaledXPos(minecraft.getWindow());
   }

   public void onMouseButton(MouseButtonInfo mouseButtonInfo, int value) {
      Minecraft instance = Minecraft.getInstance();
      if (instance.gui.screen() instanceof ChatScreen && mouseButtonInfo.button() == 0) {
         int guiScaledWidth = instance.getWindow().getGuiScaledWidth();
         int guiScaledHeight = instance.getWindow().getGuiScaledHeight();
         double value2 = this.mouseX(instance);
         double value3 = this.mouseY(instance);
         var draggables2 = this.activeDraggables();
         if (value == 1) {
            this.draggables.beginDrag(value2, value3, draggables2);
         } else if (value == 0) {
            Draggable draggable = this.draggables.endDrag();
            if (draggable != null) {
               this.savePosition(draggable, guiScaledWidth, guiScaledHeight);
            }
         }
      }
   }

   private void savePosition(Draggable draggable, int value, int value2) {
      float value3 = draggable.normalizedTargetX(value);
      float value4 = draggable.normalizedTargetY(value2);
      if (draggable.axisLock() == Draggable.AxisLock.VERTICAL) {
         value3 = 0.5F;
      } else {
         Draggable.AxisLock axisLock = draggable.axisLock();
         if (axisLock == Draggable.AxisLock.HORIZONTAL) {
            value4 = 0.5F;
         }
      }

      ConfigManager config2 = this.config;
      String text = draggable.id();
      config2.setDraggablePosition(text, value3, value4);
      this.config.save();
   }

   private double mouseY(Minecraft minecraft) {
      return minecraft.mouseHandler.getScaledYPos(minecraft.getWindow());
   }

   private static float clamp(float value, float value2, float value3) {
      return Math.max(value2, Math.min(value3, value));
   }
}
