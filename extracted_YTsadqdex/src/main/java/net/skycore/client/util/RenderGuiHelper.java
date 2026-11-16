package net.skycore.client.util;

import net.skycore.client.setting.ActionSetting;
import net.skycore.client.setting.ColorSetting;
import net.skycore.client.setting.KeyBindSetting;
import net.skycore.client.setting.NumberSetting;
import net.skycore.client.setting.StringSetting;
import net.skycore.client.setting.ModeSetting;
import net.skycore.client.setting.MultiSelectSetting;
import net.skycore.client.setting.BooleanSetting;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.skycore.client.config.ConfigManager;
import net.skycore.client.module.ClientModule;
import net.skycore.client.module.KeyBinds;
import net.skycore.client.module.ModuleCategory;
import net.skycore.client.module.ModuleManager;
import net.skycore.client.module.render.ClickGUIModule;
import net.skycore.client.module.render.CosmeticsModule;
import net.skycore.client.setting.Setting;
import net.skycore.client.theme.ThemeManager;

public final class RenderGuiHelper extends Screen implements StringBufferHelper {
   private final ClickGUIModule clickGui;
   private long lastAnimationFrame;
   private long pickerAnimationAt;
   private long closingAt;
   private float targetScroll;
   private RenderGuiHelper.PickerTarget pickerTarget;
   private int cosmeticPreviewBottom;
   private float navAnimatedY;
   private String cosmeticNav;
   private float cosmeticSwordTo;
   private int cosmeticPreviewTop;
   private final Map<Object, RenderGuiHelper.MotionState> motionStates;
   private static final Map<RenderGuiHelper.View, Float> rememberedScroll = new EnumMap<>(RenderGuiHelper.View.class);
   private float pickerValue;
   private float cosmeticSwordFrom;
   private TargetHelper activeSliderRegion;
   private int cosmeticPreviewLeft;
   private static final long PICKER_CLOSE_MS = 160L;
   private boolean pickerClosing;
   private static final Identifier LANGUAGE_ICON = Identifier.parse("skycore:textures/gui/language.png");
   private float navAnimatedH;
   private static final int DANGER = -1604983;
   private float closingGuiFrom;
   private boolean closing;
   private static final long MODULE_STAGGER_MS = 72L;
   private String configNameBuffer;
   private final Map<Setting<?>, DescriptiveHeightHelper> settingElements = new IdentityHashMap<>();
   private static final int MODULE_STAGGER_LIMIT = 7;
   private final Map<String, Long> configDeleteAt;
   private float maxScroll;
   private static final long OPEN_MS = 300L;
   private final Map<Object, Long> pulses;
   private boolean navDragging;
   private float navAnimatedX;
   private boolean configNameFocused;
   private List<ConfigManager.ProfileInfo> cachedConfigProfiles;
   private long cosmeticTabChangedAt;
   private RenderGuiHelper.View navPressedView;
   private RenderGuiHelper.DockSide navPreview;
   private Setting openDropdown;
   private StringSetting editingString;
   private long viewChangedAt;
   private String pickerHex;
   private final ModuleManager modules;
   private double navPressX;
   private float pickerX;
   private float scroll;
   private static final String SKYCORE_LOGO = "a";
   private KeyBindSetting bindingSetting;
   private static final long PICKER_OPEN_MS = 180L;
   private final Map<String, RenderGuiHelper.MotionState> namedStates;
   private final Map<ClientModule, Integer> moduleColumns;
   private int animationFrame;
   private boolean cosmeticPreviewEntity;
   private String search;
   private static final long CLOSE_MS = 240L;
   private float animatedScale;
   private GuiActionType pickerDrag;
   private final long openedAt;
   private float animationDelta;
   private final List<TargetHelper> regions = new ArrayList<>();
   private int cosmeticPreviewRight;
   private NumberSetting activeSlider;
   private final EntriesHelper cosmetica;
   private boolean navPressActive;
   private static final Identifier AVATAR = Identifier.parse("skycore:textures/main/avatar.png");
   private static final long CONFIG_CACHE_MS = 750L;
   private final Map<String, Long> configAppearAt;
   private boolean searchFocused;
   private double navPressY;
   private RenderGuiHelper.DockSide categoryDock;
   private RenderGuiHelper.View selectedView;
   private static final long MODULE_FLY_MS = 540L;
   private float pickerSaturation;
   private float navVerticalBlend;
   private float pickerHue;
   private boolean pickerHexFocused;
   private float pickerAlpha;
   private float navAnimatedW;
   private ClientModule bindingModule;
   private float pickerY;
   private String moduleColumnLayoutKey;
   private String stringBuffer;
   private static final int FAVORITE = -473745;
   private final ConfigManager config;
   private final ThemeManager themes;
   private static RenderGuiHelper.View rememberedView = RenderGuiHelper.View.COMBAT;
   private long configProfilesCachedAt;

   private void renderGui(float coordinate, float color) {
      if (!(color <= 0.001F)) {
         float clampedValue = clamp(color, 0.0F, 1.0F);
         float computedValue = coordinate * (0.92F + 0.08F * clampedValue);
         float width = 992.0F * computedValue;
         float height = 704.0F * computedValue;
         float computedValue2 = (this.width - width) * 0.5F;
         float computedValue3 = (this.height - height) * 0.5F + (1.0F - clampedValue) * 22.0F * coordinate;
         boolean is = this.clickGui.style().is("Transparent");
         BeginFrameHelper.RectBuilder rectBuilder = BeginFrameHelper.rect(computedValue2, computedValue3, width, height).color(alpha(this.guiBaseColor(is), color)).radius(24.0F * computedValue);
         if (is) {
            rectBuilder.blur(36.0F * computedValue, color * color);
         }

         rectBuilder.draw();
         float computedValue4 = computedValue2 + 8.0F * computedValue;
         float computedValue5 = computedValue3 + 8.0F * computedValue;
         float value = computedValue4;
         float computedValue6 = computedValue5 + 46.0F * computedValue;
         this.renderHeader(computedValue4, computedValue5, computedValue, color);
         BeginFrameHelper.pushScissor(computedValue4, computedValue6, 976.0F * computedValue, 642.0F * computedValue);

         try {
            if (this.selectedView == RenderGuiHelper.View.CONFIGS) {
               this.renderConfigs(value, computedValue6, computedValue, color);
            } else if (this.selectedView == RenderGuiHelper.View.COSMETICS) {
               this.renderCosmetics(value, computedValue6, computedValue, color);
            } else if (this.selectedView != RenderGuiHelper.View.AUTOBUY) {
               this.renderModules(value, computedValue6, computedValue, color);
            } else {
               this.maxScroll = 0.0F;
               this.targetScroll = 0.0F;
            }
         } finally {
            BeginFrameHelper.popScissor();
         }

         this.renderScrollBar(computedValue4, computedValue6, computedValue, color);
         this.renderCategoryNavigation(computedValue2, computedValue3, width, height, computedValue, color);
      }
   }

   private void renderModules(float index, float index2, float height, float index3) {
      ArrayList<ClientModule> clientModules = new ArrayList();
      if (!this.search.isBlank()) {
         for (ClientModule clientModule : this.modules.getModules()) {
            if (!(clientModule instanceof CosmeticsModule) && this.matchesSearch(clientModule)) {
               clientModules.add(clientModule);
            }
         }
      } else if (this.selectedView.category != null) {
         clientModules.addAll(this.modules.getByCategory(this.selectedView.category));
         clientModules.removeIf(clientModule2 -> {
            return clientModule2 instanceof CosmeticsModule;
         });
      }

      float index4 = this.viewTransition();
      float index5 = (1.0F - index4) * 12.0F * height;
      float index6 = 316.66666F * height;
      float index7 = 8.0F * height;
      this.ensureStableModuleColumns(clientModules);
      float[] floatValues = new float[]{index2 - this.scroll, index2 - this.scroll, index2 - this.scroll};
      int[] intValues = new int[3];
      long timestamp = elapsedMs(this.openedAt);

      for (ClientModule clientModule3 : clientModules) {
         int orDefault = this.moduleColumns.getOrDefault(clientModule3, 0);
         float computedValue = index + orDefault * (index6 + index7);
         float value = floatValues[orDefault];
         float computedValue2 = this.moduleRawHeight(clientModule3) * height;
         if (value + computedValue2 >= index2 && value <= index2 + 642.0F * height) {
            Object value2 = intValues[orDefault]++;
            float value3 = this.closing ? 1.0F : moduleFlyProgress(timestamp, (int)value2);
            if (value3 > 0.001F) {
               float computedValue3 = 1.0F - value3;
               float computedValue4 = orDefault == 0 ? -88.0F : (orDefault == 2 ? 88.0F : 0.0F);
               float computedValue5 = computedValue + computedValue4 * computedValue3 * height;
               float computedValue6 = value + index5 + (135.0F + Math.min((int)value2, 7) * 19.0F) * computedValue3 * height;
               this.renderModule(clientModule3, computedValue5, computedValue6, index6, computedValue2, height, index3 * index4 * value3);
            }
         }

         floatValues[orDefault] += computedValue2 + 8.0F * height;
      }

      float index8 = Math.max(floatValues[0], Math.max(floatValues[1], floatValues[2])) + this.scroll;
      float maximum = Math.max(0.0F, index8 - index2 - 8.0F * height);
      float index9 = 642.0F * height;
      this.maxScroll = Math.max(0.0F, maximum - index9);
      this.targetScroll = clamp(this.targetScroll, 0.0F, this.maxScroll);
   }

   private float moduleRawHeight(ClientModule clientModule, boolean condition) {
      var settings = this.visibleSettings(clientModule);
      int value = this.isInterface(clientModule) ? 6 : 0;
      int computedValue = settings.size() + value;
      float result = this.moduleHeaderRawHeight(clientModule);
      if (computedValue == 0) {
         return result;
      }

      float height = 32.0F;

      for (Setting setting : settings) {
         height += condition ? this.settingRawHeight(setting) : this.elementFor(setting).rawHeight();
      }

      height += value * 28.0F;
      height += Math.max(0, computedValue - 1) * 12.0F;
      return result + 1.0F + height;
   }

   private int togglerOffColor() {
      return this.fieldColor();
   }

   private static boolean isFixedHeaderRegion(GuiActionType guiActionType) {
      if (guiActionType != GuiActionType.VIEW) {
         if (guiActionType != GuiActionType.LANGUAGE) {
            if (guiActionType != GuiActionType.SEARCH) {
               if (guiActionType != GuiActionType.COSMETIC_NAV && guiActionType != GuiActionType.CONFIG_NAME) {
                  if (guiActionType != GuiActionType.CONFIG_SAVE) {
                     return false;
                  }
               }
            }
         }
      }

      return true;
   }

   @Override
   public int elevatedColor() {
      int value = 0;
      value = (byte)0;
      int alpha = shiftRgb(this.guiSolidBase(), 5);
      if (this.transparentStyle()) {
         value = withAlphaByte(alpha, this.guiAlpha(112));
      } else {
         value = alpha;
      }

      return value;
   }

   private void closePicker() {
      if (this.pickerTarget != null) {
         if (!this.pickerClosing) {
            this.pickerClosing = true;
            this.pickerAnimationAt = System.nanoTime();
            this.pickerHexFocused = false;
            this.pickerDrag = null;
         }
      }
   }

   private void ensureStableModuleColumns(List<ClientModule> clientModules) {
      StringBuilder stringBuilder = new StringBuilder(this.selectedView.name())
         .append('|')
         .append(this.search == null ? "" : this.search.toLowerCase(Locale.ROOT))
         .append('|')
         .append(clientModules.size());

      for (ClientModule clientModule : clientModules) {
         stringBuilder.append(':').append(System.identityHashCode(clientModule));
      }

      String text = stringBuilder.toString();
      if (!text.equals(this.moduleColumnLayoutKey) || this.moduleColumns.size() != clientModules.size()) {
         this.moduleColumns.clear();
         float[] floatValues = new float[]{0.0F, 0.0F, 0.0F};

         for (ClientModule clientModule2 : clientModules) {
            int value = minIndex(floatValues);
            this.moduleColumns.put(clientModule2, value);
            floatValues[value] += this.moduleBaseRawHeight(clientModule2) + 8.0F;
         }

         this.moduleColumnLayoutKey = text;
      }
   }

   @Override
   public float pulse(Object value) {
      Long element = this.pulses.get(value);
      if (element == null) {
         return 0.0F;
      } else {
         long timestamp = element;
         float computedValue = clamp((float)elapsedMs(timestamp) / 220.0F, 0.0F, 1.0F);
         if (computedValue >= 1.0F) {
            this.pulses.remove(value);
            return 0.0F;
         } else {
            return (float)Math.sin(Math.PI * computedValue);
         }
      }
   }

   private float tabGroupWidth(RenderGuiHelper.View[] viewValues, float width) {
      float width2 = 0.0F;

      for (RenderGuiHelper.View view : viewValues) {
         width2 += this.tabWidth(view, width);
      }

      return width2;
   }

   private float moduleRawHeight(ClientModule clientModule) {
      return this.moduleRawHeight(clientModule, true);
   }

   private int guiBaseColor(boolean condition) {
      int value = 0;
      value = (byte)0;
      if (condition) {
         value = this.themes.backgroundGui();
      } else {
         value = this.guiSolidBase();
         value = withAlphaByte(value, 255);
      }

      return value;
   }

   @Override
   public float motionNamed(String text, float delta) {
      RenderGuiHelper.MotionState motionState = this.namedStates.computeIfAbsent(text, text2 -> {
         return new RenderGuiHelper.MotionState(delta);
      });
      motionState.update(delta, this.animationDelta, this.animationFrame);
      return motionState.value;
   }

   private void renderCosmeticCard(
      float coordinate,
      float coordinate2,
      float coordinate3,
      float coordinate4,
      float coordinate5,
      float coordinate6,
      float coordinate7,
      float coordinate8,
      String text,
      String text2,
      String text3,
      Identifier texture,
      boolean condition,
      float coordinate9,
      float color
   ) {
      BeginFrameHelper.rect(coordinate, coordinate2, coordinate3, coordinate4).color(alpha(this.surfaceColor(), color)).radius(16.0F * coordinate9).draw();
      float computedValue = coordinate + 16.0F * coordinate9;
      float computedValue2 = coordinate2 + 16.0F * coordinate9;
      if (text2 != null) {
         CosmeticsModule cosmeticsModule = this.modules.get(CosmeticsModule.class).orElse(null);
         if (cosmeticsModule != null) {
            cosmeticsModule.renderer().renderGuiPreview(text2, computedValue, computedValue2, coordinate5, color * 0.88F);
         }
      } else {
         this.renderCosmeticThumb(computedValue, computedValue2, coordinate5, texture, color * 0.78F);
      }

      BeginFrameHelper.rect(coordinate + 1.0F * coordinate9, coordinate2 + coordinate7, coordinate3 - 2.0F * coordinate9, 1.0F * coordinate9).color(alpha(this.elevatedColor(), color)).draw();
      int value = condition ? this.primaryColor() : this.secondaryColor();
      this.text(coordinate + 16.0F * coordinate9, coordinate2 + coordinate8 + 15.0F * coordinate9, 12.0F, this.ellipsize(text3, 118.0F * coordinate9, 12.0F * coordinate9, 500), value, 500, coordinate9, color);
      float computedValue3 = 20.0F * coordinate9;
      float computedValue4 = 12.0F * coordinate9;
      float computedValue5 = coordinate + coordinate3 - 16.0F * coordinate9 - computedValue3;
      float computedValue6 = coordinate2 + coordinate8 + 14.0F * coordinate9;
      float color2 = this.cosmeticToggleProgress(text, condition);
      BeginFrameHelper.rect(computedValue5, computedValue6, computedValue3, computedValue4)
         .color(alpha(mixColor(this.fieldColor(), this.accentColor(), color2), color))
         .radius(512.0F * coordinate9)
         .draw();
      float computedValue7 = 8.0F * coordinate9;
      float computedValue8 = computedValue5 + (2.0F + color2 * 8.0F) * coordinate9;
      BeginFrameHelper.rect(computedValue8, computedValue6 + 2.0F * coordinate9, computedValue7, computedValue7)
         .color(alpha(mixColor(this.quaternaryColor(), this.primaryColor(), color2), color))
         .radius(512.0F * coordinate9)
         .draw();
   }

   @Override
   public int fieldColor() {
      int value = 1;
      int value2 = 0;
      int alpha = shiftRgb(this.guiSolidBase(), 10);
      if (this.transparentStyle()) {
         value = this.guiAlpha(98);
         value2 = withAlphaByte(alpha, value);
      } else {
         value2 = alpha;
      }

      return value2;
   }

   private int scrollTrackColor() {
      int alpha = -1;
      int value = 0;
      alpha = (byte)-1;
      value = (byte)0;
      if (this.transparentStyle()) {
         value = this.guiSolidBase();
         value = shiftRgb(value, 3);
         alpha = this.guiAlpha(56);
         value = withAlphaByte(value, alpha);
      } else {
         value = this.surfaceColor();
      }

      return value;
   }

   private TargetHelper findRegion(double index, double index2) {
      for (int index3 = this.regions.size() - 1; index3 >= 0; index3--) {
         TargetHelper targetHelper = this.regions.get(index3);
         if (isFixedHeaderRegion(targetHelper.kind()) && contains(index, index2, targetHelper.x(), targetHelper.y(), targetHelper.w(), targetHelper.h())) {
            return targetHelper;
         }
      }

      for (int index4 = this.regions.size() - 1; index4 >= 0; index4--) {
         TargetHelper targetHelper2 = this.regions.get(index4);
         if (contains(index, index2, targetHelper2.x(), targetHelper2.y(), targetHelper2.w(), targetHelper2.h())) {
            return targetHelper2;
         }
      }

      return null;
   }

   private void openPicker(RenderGuiHelper.PickerTarget pickerTarget2, float pickerX2, float pickerY2) {
      this.pickerTarget = pickerTarget2;
      this.pickerClosing = false;
      this.pickerAnimationAt = System.nanoTime();
      this.pickerX = pickerX2;
      this.pickerY = pickerY2;
      int color = pickerTarget2.get();
      float[] floatValues = Color.RGBtoHSB(color >> 16 & 0xFF, color >> 8 & 0xFF, color & 0xFF, (float[])null);
      this.pickerHue = floatValues[0];
      this.pickerSaturation = floatValues[1];
      this.pickerValue = floatValues[2];
      this.pickerAlpha = pickerTarget2.hasAlpha() ? (color >>> 24 & 0xFF) / 255.0F : 1.0F;
      this.pickerHexFocused = false;
      this.pickerHex = this.hexForColor(color);
   }

   @Override
   public int secondaryColor() {
      return this.themes.textSecondary();
   }

   private void finishBinding(int value) {
      if (this.bindingModule != null) {
         this.bindingModule.setKeyBind(value);
         this.bindingModule = null;
         this.config.save();
      }

      if (this.bindingSetting != null) {
         this.bindingSetting.set(value);
         this.bindingSetting = null;
         this.config.save();
      }
   }

   @Override
   public boolean keyPressed(KeyEvent keyEvent) {
      int value = keyEvent.key();
      if (this.bindingModule == null && this.bindingSetting == null) {
         if (this.pickerHexFocused) {
            if (value == 256) {
               this.pickerHexFocused = false;
               return true;
            }

            if (value == 257) {
               this.commitPickerHex();
               return true;
            }

            if (value == 259 && !this.pickerHex.isEmpty()) {
               this.pickerHex = this.pickerHex.substring(0, this.pickerHex.length() - 1);
               return true;
            }
         }

         if (this.editingString != null) {
            if (value == 256) {
               this.editingString = null;
               this.stringBuffer = "";
               return true;
            }

            if (value == 257) {
               this.commitStringEdit();
               return true;
            }

            if (value == 259 && !this.stringBuffer.isEmpty()) {
               this.stringBuffer = this.stringBuffer.substring(0, this.stringBuffer.length() - 1);
               return true;
            }
         }

         if (this.configNameFocused) {
            if (value == 259 && !this.configNameBuffer.isEmpty()) {
               this.configNameBuffer = this.configNameBuffer.substring(0, this.configNameBuffer.length() - 1);
               return true;
            }

            if (value == 257) {
               if (!this.configNameBuffer.isBlank()) {
                  String text = this.configNameBuffer.trim().toLowerCase(Locale.ROOT);
                  if (this.config.saveProfile(this.configNameBuffer)) {
                     this.configAppearAt.put(text, System.nanoTime());
                     this.invalidateConfigProfiles();
                     this.configNameBuffer = "";
                     this.configNameFocused = false;
                  }
               }

               return true;
            }

            if (value == 256) {
               this.configNameFocused = false;
               return true;
            }
         }

         if (this.searchFocused) {
            if (value == 259 && !this.search.isEmpty()) {
               this.search = this.search.substring(0, this.search.length() - 1);
               this.targetScroll = 0.0F;
               return true;
            }

            if (value == 257) {
               this.searchFocused = false;
               return true;
            }

            if (value == 256 && !this.search.isEmpty()) {
               this.search = "";
               this.targetScroll = 0.0F;
               return true;
            }
         }

         if (value != 256 && value != this.clickGui.getKeyBind()) {
            return super.keyPressed(keyEvent);
         } else if (this.pickerTarget != null) {
            this.closePicker();
            return true;
         } else if (this.openDropdown != null) {
            this.openDropdown = null;
            return true;
         } else {
            this.startClose();
            return true;
         }
      } else {
         this.finishBinding(value != 256 && value != 261 ? value : -1);
         return true;
      }
   }

   @Override
   public int quaternaryColor() {
      int value = 0;
      value = (byte)0;
      value = this.guiSolidBase();
      return mixColor(value, this.themes.textTertiary(), 0.52F);
   }

   private int separatorColor() {
      int alpha = -1;
      int value = 0;
      alpha = (byte)1;
      value = (byte)0;
      if (this.transparentStyle()) {
         value = this.guiSolidBase();
         value = shiftRgb(value, 10);
         alpha = this.guiAlpha(56);
         value = withAlphaByte(value, alpha);
      } else {
         value = this.fieldColor();
      }

      return value;
   }

   @Override
   public boolean dropdownOpen(Setting setting) {
      return this.openDropdown == setting;
   }

   private void renderChecker(float coordinate, float coordinate2, float coordinate3, float coordinate4, float coordinate5, float alpha) {
      float maximum = Math.max(2.0F, 3.0F * coordinate5);
      int index = Math.max(1, (int)Math.ceil(coordinate3 / maximum));
      int index2 = Math.max(1, (int)Math.ceil(coordinate4 / maximum));

      for (int value = 0; value < index2; value++) {
         float index3 = coordinate2 + value * maximum;
         float minimum = Math.min(maximum, coordinate2 + coordinate4 - index3);

         for (int value2 = 0; value2 < index; value2++) {
            float computedValue = coordinate + value2 * maximum;
            float minimum2 = Math.min(maximum, coordinate + coordinate3 - computedValue);
            int alpha2 = (value + value2 & 1) == 0 ? -11184811 : -5592406;
            if (minimum2 > 0.0F && minimum > 0.0F) {
               BeginFrameHelper.rect(computedValue, index3, minimum2, minimum).color(alpha(alpha2, alpha)).draw();
            }
         }
      }
   }

   private float pickerAnimation() {
      if (this.pickerTarget == null) {
         return 0.0F;
      }

      long timestamp = elapsedMs(this.pickerAnimationAt);
      if (this.pickerClosing) {
         float computedValue = (float)timestamp / 160.0F;
         float clampedValue = clamp(computedValue, 0.0F, 1.0F);
         if (clampedValue >= 1.0F) {
            this.pickerTarget = null;
            this.pickerClosing = false;
            return 0.0F;
         } else {
            return 1.0F - smoothstep(clampedValue);
         }
      } else {
         return smoothstep(clamp((float)timestamp / 180.0F, 0.0F, 1.0F));
      }
   }

   private float currentScale() {
      float result;
      if (this.animatedScale > 0.0F) {
         result = this.animatedScale;
      } else {
         result = this.targetScale();
      }

      return result;
   }

   private static int alpha(int color, float color2) {
      return MixColorHelper.alpha(color, color2);
   }

   @Override
   public int accentColor() {
      return this.themes.accent();
   }

   private void drawDescription(String text, float coordinate, float coordinate2, float width, float coordinate3, float coordinate4, int coordinate5) {
      if (text != null && !text.isBlank()) {
         String[] stringValues = text.trim().split("\\s+");
         StringBuilder stringBuilder = new StringBuilder();
         StringBuilder stringBuilder2 = new StringBuilder();

         for (String text2 : stringValues) {
            String text3 = stringBuilder.length() == 0 ? text2 : stringBuilder + " " + text2;
            if (stringBuilder2.length() == 0 && InterBoldHelper.forWeight(400).getWidth(text3, 11.0F * coordinate3) <= width) {
               if (stringBuilder.length() > 0) {
                  stringBuilder.append(' ');
               }

               stringBuilder.append(text2);
            } else {
               if (stringBuilder2.length() > 0) {
                  stringBuilder2.append(' ');
               }

               stringBuilder2.append(text2);
            }
         }

         this.text(coordinate, coordinate2, 11.0F, stringBuilder.toString(), coordinate5, 400, coordinate3, coordinate4);
         if (stringBuilder2.length() > 0) {
            String text4 = stringBuilder2.toString();

            while (InterBoldHelper.forWeight(400).getWidth(text4, 11.0F * coordinate3) > width && text4.length() > 3) {
               text4 = text4.substring(0, text4.length() - 2);
            }

            if (!text4.equals(stringBuilder2.toString())) {
               text4 = text4 + "…";
            }

            this.text(coordinate, coordinate2 + 14.0F * coordinate3, 11.0F, text4, coordinate5, 400, coordinate3, coordinate4);
         }
      }
   }

   private void renderVerticalTabGroup(RenderGuiHelper.View[] viewValues, float index, float index2, float color, float color2, boolean condition) {
      float color3 = 38.0F * color;

      for (int value = 0; value < viewValues.length; value++) {
         RenderGuiHelper.View view = viewValues[value];
         float color4 = index2 + value * color3;
         float color5 = this.motionNamed("view:" + view.name(), view == this.selectedView ? 1.0F : 0.0F);
         if (color5 > 0.001F) {
            BeginFrameHelper.rect(index, color4, color3, color3).color(alpha(this.elevatedColor(), color2 * color5)).radius(16.0F * color).draw();
         }

         this.csIconCentered(
            index + (color3 - 11.0F * color) * 0.5F,
            color4,
            11.0F * color,
            color3,
            11.0F,
            view.icon,
            mixColor(this.tertiaryColor(), this.accentColor(), color5),
            color,
            color2
         );
         if (condition) {
            this.regions.add(new TargetHelper(GuiActionType.VIEW, view, null, index, color4, color3, color3, null));
         }
      }
   }

   private static int minIndex(float[] floatValues) {
      int value = 0;

      for (int value2 = 1; value2 < floatValues.length; value2++) {
         if (floatValues[value2] < floatValues[value]) {
            value = value2;
         }
      }

      return value;
   }

   private float globalAnimation() {
      if (this.closing) {
         float computedValue = clamp((float)elapsedMs(this.closingAt) / 240.0F, 0.0F, 1.0F);
         return this.closingGuiFrom * (1.0F - this.ease(computedValue));
      } else {
         float intermediate = (float)elapsedMs(this.openedAt) / 300.0F;
         intermediate = clamp(intermediate, 0.0F, 1.0F);
         return this.ease(intermediate);
      }
   }

   private void renderHueTrack(float coordinate, float coordinate2, float coordinate3, float coordinate4, float alpha) {
      int color = -1;
      int[] intValues = new int[]{-65536, -256, -16711936, -16711681, -16776961, -65281, -65536};
      float index = coordinate3 / 6.0F;

      for (int value = 0; value < 6; value++) {
         BeginFrameHelper.RectBuilder rectBuilder = BeginFrameHelper.rect(coordinate + value * index, coordinate2, index + 0.5F, coordinate4);
         color = alpha(intValues[value], alpha);
         int color2 = intValues[value + 1];
         color2 = alpha(color2, alpha);
         int color3 = alpha(intValues[value + 1], alpha);
         int alpha2 = intValues[value];
         BeginFrameHelper.RectBuilder rectBuilder2 = rectBuilder.color(color, color2, color3, alpha(alpha2, alpha))
            .radius(value == 0 ? coordinate4 * 0.5F : 0.0F, value == 5 ? coordinate4 * 0.5F : 0.0F, value == 5 ? coordinate4 * 0.5F : 0.0F, value == 0 ? coordinate4 * 0.5F : 0.0F);
         rectBuilder2.draw();
      }
   }

   private float targetScale() {
      int value = 1;
      int value2 = 0;
      value2 = this.width;
      value = this.height;
      NumberSetting numberSetting = this.clickGui.renderFactor();
      return FitScaleHelper.fitScale(value2, value, numberSetting.get().floatValue());
   }

   private void renderCosmeticHeaderTabs(float coordinate, float color, float color2, float color3) {
      float color4 = 38.0F * color2;
      String[] stringValues = new String[]{"accessories", "swords"};
      String[] stringValues2 = new String[]{"Cosmetica", "Swords"};
      String[] stringValues3 = new String[]{"\ue001", "\ue010"};
      float[] floatValues = new float[stringValues.length];
      float[] floatValues2 = new float[stringValues.length];
      float value = 0.0F;
      float value2 = this.cosmeticSwordBlend();

      for (int value3 = 0; value3 < stringValues.length; value3++) {
         floatValues[value3] = value3 == 0 ? 1.0F - value2 : value2;
         float width = InterBoldHelper.forWeight(500).getWidth(stringValues2[value3], 11.0F * color2);
         floatValues2[value3] = 38.0F * color2 + (13.0F * color2 + width) * floatValues[value3];
         value += floatValues2[value3];
      }

      BeginFrameHelper.rect(coordinate, color, value, color4).color(alpha(this.surfaceColor(), color3)).radius(16.0F * color2).draw();
      float value4 = coordinate;

      for (int value5 = 0; value5 < stringValues.length; value5++) {
         float color5 = floatValues[value5];
         float value6 = floatValues2[value5];
         if (color5 > 0.001F) {
            BeginFrameHelper.rect(value4, color, value6, color4).color(alpha(this.elevatedColor(), color3 * color5)).radius(16.0F * color2).draw();
         }

         float computedValue = value4 + 13.5F * color2;
         float computedValue2 = value4 + 16.0F * color2;
         float color6 = computedValue + (computedValue2 - computedValue) * color5;
         this.csIconCentered(color6, color, 11.0F * color2, color4, 11.0F, stringValues3[value5], mixColor(this.tertiaryColor(), this.accentColor(), color5), color2, color3);
         if (color5 > 0.001F) {
            this.text(value4 + 35.0F * color2, color + 12.0F * color2, 11.0F, stringValues2[value5], this.primaryColor(), 500, color2, color3 * color5);
         }

         this.regions.add(new TargetHelper(GuiActionType.COSMETIC_NAV, stringValues[value5], null, value4, color, value6, color4, null));
         value4 += value6;
      }
   }

   private static boolean contains(double value, double value2, float value3, float value4, float value5, float value6) {
      return value >= value3 && value2 >= value4 && value <= value3 + value5 && value2 <= value4 + value6;
   }

   private float cosmeticSwordBlend() {
      if (this.cosmeticTabChangedAt == 0L) {
         return this.cosmeticSwordTo;
      }

      long nowNanos = System.nanoTime();
      float computedValue = clamp((float)(nowNanos - this.cosmeticTabChangedAt) / 2.8E8F, 0.0F, 1.0F);
      float computedValue2 = computedValue * computedValue * (3.0F - 2.0F * computedValue);
      if (computedValue >= 1.0F) {
         this.cosmeticTabChangedAt = 0L;
      }

      return this.cosmeticSwordFrom + (this.cosmeticSwordTo - this.cosmeticSwordFrom) * computedValue2;
   }

   @Override
   public int surfaceColor() {
      int value = 1;
      int value2 = 0;
      value2 = this.guiSolidBase();
      int alpha = shiftRgb(value2, 3);
      if (this.transparentStyle()) {
         value = this.guiAlpha(80);
         value2 = withAlphaByte(alpha, value);
      } else {
         value2 = alpha;
      }

      return value2;
   }

   @Override
   public String localize(String text) {
      return SelectAndSaveHelper.text(text);
   }

   private TargetHelper findRegion(GuiActionType guiActionType) {

      for (int index = this.regions.size() - 1; index >= 0; index--) {
         TargetHelper targetHelper = this.regions.get(index);
         if (targetHelper.kind() == guiActionType) {
            return targetHelper;
         }
      }

      return null;
   }

   private void renderThemeColorRow(int slot, String text, float color, float color2, float coordinate, float color3, float color4, float color5) {
      this.text(color, color2, 11.0F, this.localize(text), mixColor(this.secondaryColor(), this.primaryColor(), color5), 500, color3, color4);
      this.text(color, color2 + 16.0F * color3, 10.0F, this.localize("Interface color"), this.secondaryColor(), 400, color3, color4);
      float computedValue = 87.0F * color3;
      float computedValue2 = 22.0F * color3;
      float computedValue3 = color + coordinate - computedValue;
      float computedValue4 = color2 - 4.0F * color3;
      float value = this.pulse(slot);
      BeginFrameHelper.rect(computedValue3, computedValue4, computedValue, computedValue2)
         .color(alpha(mixColor(this.fieldColor(), this.accentColor(), value * 0.16F), color4))
         .radius(8.0F * color3)
         .draw();
      int alpha = this.themes.slot(slot);
      String text2 = String.format(Locale.ROOT, "%06X", alpha & 16777215);
      this.text(computedValue3 + 6.0F * color3, computedValue4 + 4.0F * color3, 11.0F, "#", this.secondaryColor(), 400, color3, color4);
      this.text(computedValue3 + 14.0F * color3, computedValue4 + 4.0F * color3, 11.0F, text2, this.primaryColor(), 500, color3, color4);
      float computedValue5 = (12.0F + value) * color3;
      BeginFrameHelper.rect(computedValue3 + computedValue - (18.0F + value * 0.5F) * color3, computedValue4 + (5.0F - value * 0.5F) * color3, computedValue5, computedValue5)
         .color(alpha(alpha, color4))
         .radius(512.0F * color3)
         .draw();
      this.regions.add(new TargetHelper(GuiActionType.THEME_SLOT, slot, (String)null, computedValue3, computedValue4, computedValue, computedValue2, null));
   }

   private static int shiftRgb(int value, int value2) {
      int value3 = 1;
      value3 = (byte)1;
      value3 = (byte)1;
      value3 = Math.min(255, (value >>> 16 & 0xFF) + value2);
      int maximum = Math.max(0, value3);
      int intermediate = (value >>> 8 & 0xFF) + value2;
      value3 = Math.min(255, intermediate);
      int maximum2 = Math.max(0, value3);
      int computedValue = Math.max(0, Math.min(255, (value & 0xFF) + value2));
      return value & 0xFF000000 | maximum << 16 | maximum2 << 8 | computedValue;
   }

   private void renderConfigCard(ConfigManager.ProfileInfo profileInfo, float coordinate, float coordinate2, float coordinate3, float coordinate4, float color, float color2, float color3) {
      String text = profileInfo.name();
      int value = this.config.activeProfile() != null && this.config.activeProfile().equalsIgnoreCase(text) ? 1 : 0;
      boolean favorite = profileInfo.favorite();
      float color4 = this.motionNamed("config-active:" + text, value != 0 ? 1.0F : 0.0F);
      float color5 = this.motionNamed("config-favorite:" + text, favorite ? 1.0F : 0.0F);
      int alpha = mixColor(this.moduleDisabledColor(), this.moduleEnabledColor(), color4);
      BeginFrameHelper.rect(coordinate, coordinate2, coordinate3, coordinate4).color(alpha(alpha, color2)).radius(16.0F * color).draw();
      int value2 = mixColor(this.secondaryColor(), this.primaryColor(), color4);
      int value3 = mixColor(this.tertiaryColor(), this.secondaryColor(), color4);
      this.text(coordinate + 16.0F * color, coordinate2 + 16.0F * color, 12.0F, text, value2, 500, color, color2);
      this.text(coordinate + 16.0F * color, coordinate2 + 38.0F * color, 11.0F, "by " + profileInfo.author(), value3, 400, color, color2);
      float computedValue = 20.0F * color;
      float computedValue2 = 12.0F * color;
      float computedValue3 = coordinate + coordinate3 - 16.0F * color - computedValue;
      float computedValue4 = coordinate2 + 17.0F * color;
      BeginFrameHelper.rect(computedValue3, computedValue4, computedValue, computedValue2)
         .color(alpha(mixColor(this.togglerOffColor(), this.accentColor(), color4), color2))
         .radius(512.0F * color)
         .draw();
      float computedValue5 = 8.0F * color;
      BeginFrameHelper.rect(computedValue3 + 2.0F * color + 8.0F * color * color4, computedValue4 + 2.0F * color, computedValue5, computedValue5)
         .color(alpha(mixColor(this.quaternaryColor(), this.primaryColor(), color4), color2))
         .radius(512.0F * color)
         .draw();
      float color6 = computedValue3 - 8.0F * color - 12.0F * color;
      float color7 = color6 - 4.0F * color - 12.0F * color;
      this.csIconCentered(
         color7,
         coordinate2 + 13.0F * color,
         12.0F * color,
         20.0F * color,
         10.0F + 2.0F * color5,
         "\ue00f",
         mixColor(this.tertiaryColor(), -473745, color5),
         color,
         color2 * (1.0F - color5 * 0.45F)
      );
      if (color5 > 0.01F) {
         this.csIconCentered(color7, coordinate2 + 13.0F * color, 12.0F * color, 20.0F * color, 9.0F + 3.0F * color5, "\ue008", -473745, color, color2 * color5);
      }

      this.csIconCentered(
         color6,
         coordinate2 + (13.0F + color3 * 2.0F) * color,
         12.0F * color,
         20.0F * color,
         12.0F - color3 * 2.0F,
         "\ue011",
         mixColor(color4 > 0.5F ? -1604983 : this.quaternaryColor(), -1604983, color3),
         color,
         color2
      );
      if (color3 < 0.01F) {
         this.regions.add(new TargetHelper(GuiActionType.CONFIG_LOAD, text, (String)null, coordinate, coordinate2, coordinate3, coordinate4, null));
         this.regions
            .add(new TargetHelper(GuiActionType.CONFIG_FAVORITE, text, (String)null, color7, coordinate2 + 13.0F * color, 12.0F * color, 20.0F * color, null));
         this.regions.add(new TargetHelper(GuiActionType.CONFIG_DELETE, text, (String)null, color6, coordinate2 + 13.0F * color, 12.0F * color, 20.0F * color, null));
         this.regions.add(new TargetHelper(GuiActionType.CONFIG_LOAD, text, (String)null, computedValue3, computedValue4 - 4.0F * color, computedValue, 20.0F * color, null));
      }
   }

   private void updateRenderScale() {
      float value = this.targetScale();
      if (!(this.animatedScale <= 0.0F)) {
         if (Float.isFinite(this.animatedScale)) {
            double animationDelta2 = this.animationDelta;
            float computedValue = 1.0F - (float)Math.pow(8.0E-4F, animationDelta2);
            float computedValue2 = this.animatedScale + (value - this.animatedScale) * computedValue;
            this.animatedScale = computedValue2;
            if (Math.abs(value - this.animatedScale) < 1.0E-4F) {
               this.animatedScale = value;
            }

            return;
         }
      }

      this.animatedScale = value;
   }

   private void renderHeader(float coordinate, float coordinate2, float coordinate3, float color) {
      float width = 18.0F * coordinate3;
      float width2 = InterBoldHelper.skycore().getWidth("a", width);
      float value = InterBoldHelper.skycore().textHeight(width);
      float computedValue = coordinate + (976.0F * coordinate3 - width2) * 0.5F;
      BeginFrameHelper.text(computedValue, coordinate2 + (38.0F * coordinate3 - value) * 0.5F, width, "a").font(InterBoldHelper.skycore()).color(alpha(this.accentColor(), color)).draw();
      float computedValue2 = coordinate + 976.0F * coordinate3;
      float computedValue3 = 38.0F * coordinate3;
      computedValue2 -= computedValue3;
      BeginFrameHelper.rect(computedValue2, coordinate2, computedValue3, computedValue3).color(alpha(this.surfaceColor(), color)).radius(16.0F * coordinate3).draw();
      float computedValue4 = 30.0F * coordinate3;
      BeginFrameHelper.texture(computedValue2 + 4.0F * coordinate3, coordinate2 + 4.0F * coordinate3, computedValue4, computedValue4, AVATAR).color(alpha(-1, color)).radius(12.0F * coordinate3).draw();
      computedValue2 -= 8.0F * coordinate3;
      float computedValue5 = 192.0F * coordinate3;
      computedValue2 -= computedValue5;
      this.renderSearch(computedValue2, coordinate2, computedValue5, 38.0F * coordinate3, coordinate3, color);
      computedValue2 -= 8.0F * coordinate3 + 38.0F * coordinate3;
      this.renderLanguageButton(computedValue2, coordinate2, coordinate3, color);
      if (this.selectedView == RenderGuiHelper.View.COSMETICS) {
         this.renderCosmeticHeaderTabs(coordinate, coordinate2, coordinate3, color);
      }

      if (this.selectedView == RenderGuiHelper.View.CONFIGS) {
         float computedValue6 = 160.0F * coordinate3;
         this.renderConfigInput(coordinate, coordinate2, computedValue6, 38.0F * coordinate3, coordinate3, color);
      }
   }

   private static long elapsedMs(long timestamp) {
      return (System.nanoTime() - timestamp) / 1000000L;
   }

   private DescriptiveHeightHelper elementFor(Setting<?> setting) {
      return this.settingElements.computeIfAbsent(setting, SettingWidgetFactory::create);
   }

   @Override
   public int tertiaryColor() {
      return this.themes.textTertiary();
   }

   private static float moduleFlyProgress(long timestamp, int value) {
      long timestamp2 = Math.min(value, 7) * 72L;
      float computedValue = (float)(timestamp - timestamp2) / 540.0F;
      float clampedValue = clamp(computedValue, 0.0F, 1.0F);
      float computedValue2 = 1.0F - clampedValue;
      float computedValue3 = computedValue2 * computedValue2;
      return 1.0F - computedValue3 * computedValue3;
   }

   @Override
   public boolean transparentStyle() {
      return this.clickGui.style().is("Transparent");
   }

   @Override
   public float motion(Object value, float delta) {
      var motionStates2 = this.motionStates;
      Function<java.lang.Object, net.skycore.client.util.RenderGuiHelper.MotionState> function = value2 -> {
         return new RenderGuiHelper.MotionState(delta);
      };
      RenderGuiHelper.MotionState motionState = (RenderGuiHelper.MotionState)motionStates2.computeIfAbsent(value, function);
      motionState.update(delta, this.animationDelta, this.animationFrame);
      return motionState.value;
   }

   private void renderFrame(GuiGraphicsExtractor guiGraphicsExtractor, DeltaTracker deltaTracker) {
      this.updateAnimationClock();
      if (!this.closing || elapsedMs(this.closingAt) < 240L) {
         this.updateRenderScale();
         this.updateScrollMotion();
         this.animationFrame++;
         CurrentGuiGraphicsExtractorHelper.enter2D(guiGraphicsExtractor, deltaTracker);
         BeginFrameHelper.beginFrame();

         try {
            this.regions.clear();
            float value = this.globalAnimation();
            int computedValue = Math.round(38.25F * clamp(value, 0.0F, 1.0F));
            if (computedValue > 0) {
               BeginFrameHelper.rect(0.0F, 0.0F, this.width, this.height).color(computedValue << 24).draw();
            }

            this.renderGui(this.currentScale(), value);
            if (this.pickerTarget != null) {
               float color = Math.min(this.pickerAnimation(), value);
               if (color > 0.001F) {
                  this.renderColorPicker(this.currentScale(), color);
               }
            }

            BeginFrameHelper.flush();
         } finally {
            CurrentGuiGraphicsExtractorHelper.exit2D();
         }
      } else if (this.minecraft != null && this.minecraft.gui.screen() == this) {
         this.minecraft.setScreenAndShow((Screen)null);
      }
   }

   private int moduleDisabledColor() {
      return this.surfaceColor();
   }

   private float moduleHeaderRawHeight(ClientModule clientModule) {
      String description = this.localize(clientModule.getDescription());
      if (description != null) {
         if (!description.isBlank()) {
            int value = this.descriptionLineCount(description, 284.67F, 11.0F, 2);
            return 54.0F + value * 14.0F;
         }
      }

      return 46.0F;
   }

   @Override
   public String stringBuffer() {
      return this.stringBuffer;
   }

   private float tabWidth(RenderGuiHelper.View view, float value) {
      String text = view.name();
      float computedValue = this.motionNamed("view:" + text, view == this.selectedView ? 1.0F : 0.0F);
      TextHeightHelper textHeightHelper = InterBoldHelper.forWeight(500);
      float width = 11.0F * value;
      float width2 = textHeightHelper.getWidth(view.label, width);
      float computedValue2 = 51.0F * value + width2;
      return 38.0F * value + (computedValue2 - 38.0F * value) * computedValue;
   }

   @Override
   public boolean mouseDragged(MouseButtonEvent mouseButtonEvent, double value, double value2) {
      if (this.navPressActive) {
         double x = mouseButtonEvent.x() - this.navPressX;
         double y = mouseButtonEvent.y() - this.navPressY;
         if (!this.navDragging && x * x + y * y > 64.0) {
            this.navDragging = true;
         }

         if (this.navDragging) {
            this.navPreview = this.snapCandidate(mouseButtonEvent.x(), mouseButtonEvent.y());
         }

         return true;
      } else {
         if (this.activeSlider != null && this.activeSliderRegion != null) {
            this.updateSlider(this.activeSliderRegion, mouseButtonEvent.x());
            return true;
         }

         if (this.pickerDrag != null) {
            TargetHelper region = this.findRegion(this.pickerDrag);
            if (region != null) {
               this.updatePicker(region, mouseButtonEvent.x(), mouseButtonEvent.y());
               return true;
            }
         }

         return false;
      }
   }

   private String hexForColor(int value) {
      if (this.pickerTarget != null) {
         if (this.pickerTarget.hasAlpha()) {
            return String.format(Locale.ROOT, "%08X", value);
         }
      }

      Locale rOOT = Locale.ROOT;
      Object[] objectValues = new Object[1];
      int intermediate = value & 16777215;
      objectValues[0] = intermediate;
      return String.format(rOOT, "%06X", objectValues);
   }

   private List<ConfigManager.ProfileInfo> configProfiles() {
      if (this.configProfilesCachedAt == 0L || elapsedMs(this.configProfilesCachedAt) >= 750L) {
         var profileInfos = this.config.listProfileInfos();
         var profileInfos2 = List.copyOf(profileInfos);
         this.cachedConfigProfiles = profileInfos2;
         this.configProfilesCachedAt = System.nanoTime();
      }

      return this.cachedConfigProfiles;
   }

   private int guiAlpha(int value) {
      int value2 = this.themes.backgroundGui() >>> 24 & 0xFF;
      float computedValue = value * (value2 / 255.0F);
      return Math.round(computedValue);
   }

   @Override
   public void iconText(float value, float value2, float value3, String text, int alpha, float value4, float alpha2) {
      int value5 = 1;
      BeginFrameHelper.TextBuilder textBuilder = BeginFrameHelper.text(value, value2 - 5.5F * value4, value3 * value4, text);
      BeginFrameHelper.TextBuilder textBuilder2 = textBuilder.font(InterBoldHelper.clickGuiIcons());
      value5 = alpha(alpha, alpha2);
      BeginFrameHelper.TextBuilder textBuilder3 = textBuilder2.color(value5);
      textBuilder3.draw();
   }

   @Override
   public int primaryColor() {
      return this.themes.textPrimary();
   }

   private void invalidateConfigProfiles() {
      this.configProfilesCachedAt = 0L;
   }

   private void startClose() {
      if (!this.closing) {
         this.navPressActive = false;
         this.navDragging = false;
         this.navPreview = null;
         this.navPressedView = null;
         rememberedView = this.selectedView;
         rememberedScroll.put(this.selectedView, this.targetScroll);
         this.commitStringEdit();
         this.searchFocused = false;
         this.configNameFocused = false;
         this.openDropdown = null;
         this.closingGuiFrom = this.globalAnimation();
         this.closing = true;
         this.closingAt = System.nanoTime();
         this.config.save();
      }
   }

   @Override
   public boolean mouseClicked(MouseButtonEvent mouseButtonEvent, boolean condition) {
      double x = mouseButtonEvent.x();
      double y = mouseButtonEvent.y();
      int value = mouseButtonEvent.button();
      if (this.bindingModule == null && this.bindingSetting == null) {
         TargetHelper region = this.findRegion(x, y);
         if (this.pickerTarget != null && !this.pickerClosing && (region == null || !region.kind().isPicker())) {
            this.closePicker();
         }

         if (region == null) {
            this.searchFocused = false;
            this.configNameFocused = false;
            this.openDropdown = null;
            this.commitStringEdit();
            return false;
         }

         switch (region.kind()) {
            case LANGUAGE:
               if (value == 0) {
                  SelectAndSaveHelper.toggle();
                  this.moduleColumnLayoutKey = "";
                  this.config.save();
               }

               return true;
            case VIEW:
            case NAV_DOCK:
               if (value == 0 && !this.closing) {
                  this.navPressActive = true;
                  this.navDragging = false;
                  this.navPreview = null;
                  this.navPressedView = region.kind() == GuiActionType.VIEW ? (RenderGuiHelper.View)region.target() : null;
                  this.navPressX = x;
                  this.navPressY = y;
               }

               return true;
            case MODULE:
            case EXPAND:
               ClientModule clientModule = (ClientModule)region.target();
               if (value == 2) {
                  this.bindingModule = clientModule;
               } else if (value == 0) {
                  clientModule.toggle();
                  this.config.save();
               }

               return true;
            case BUTTON:
               if (value == 0) {
                  ActionSetting actionSetting = (ActionSetting)region.target();
                  this.triggerPulse(actionSetting);
                  actionSetting.press();
                  this.config.save();
               }

               return true;
            case BOOLEAN:
               if (value == 0) {
                  BooleanSetting booleanSetting = (BooleanSetting)region.target();
                  this.triggerPulse(booleanSetting);
                  booleanSetting.toggle();
                  this.config.save();
               }

               return true;
            case NUMBER:
               if (value == 0) {
                  this.activeSlider = (NumberSetting)region.target();
                  this.triggerPulse(this.activeSlider);
                  this.activeSliderRegion = region;
                  this.updateSlider(region, x);
               }

               return true;
            case MODE:
               if (value == 0) {
                  ModeSetting modeSetting = (ModeSetting)region.target();
                  this.triggerPulse(modeSetting);
                  if ("__toggle__".equals(region.value())) {
                     this.toggleDropdown(modeSetting);
                  } else {
                     modeSetting.set(region.value());
                     this.openDropdown = null;
                     this.config.save();
                  }
               }

               return true;
            case MULTI:
               if (value == 0) {
                  MultiSelectSetting multiSelectSetting = (MultiSelectSetting)region.target();
                  this.triggerPulse(multiSelectSetting);
                  if ("__toggle__".equals(region.value())) {
                     this.toggleDropdown(multiSelectSetting);
                  } else {
                     multiSelectSetting.toggle(region.value());
                     this.config.save();
                  }
               }

               return true;
            case STRING:
               if (value == 0) {
                  this.editingString = (StringSetting)region.target();
                  this.triggerPulse(this.editingString);
                  this.stringBuffer = this.editingString.get();
                  this.searchFocused = false;
                  this.configNameFocused = false;
               }

               return true;
            case COLOR:
               if (value == 0) {
                  ColorSetting colorSetting = (ColorSetting)region.target();
                  this.triggerPulse(colorSetting);
                  this.openPicker(new RenderGuiHelper.SettingPickerTarget(colorSetting), region.x() + region.w() + 4.0F, region.y());
               }

               return true;
            case KEYBIND:
               if (value == 0) {
                  if (region.target() instanceof ClientModule clientModule2) {
                     this.bindingModule = clientModule2;
                  } else {
                     this.bindingSetting = (KeyBindSetting)region.target();
                     this.triggerPulse(this.bindingSetting);
                  }
               }

               return true;
            case THEME_SLOT:
            case THEME_ACCENT:
               if (value == 0) {
                  int value2;
                  if (region.target() instanceof Integer value3) {
                     value2 = value3;
                  } else {
                     value2 = 1;
                  }

                  int slot = value2;
                  this.triggerPulse(slot);
                  this.openPicker(new RenderGuiHelper.ThemeSlotPickerTarget(slot), region.x() + region.w() + 4.0F, region.y());
               }

               return true;
            case COSMETIC:
               if (value == 0) {
                  Object value4 = region.target();
                  this.triggerPulse(value4);
                  if (value4 instanceof EntriesHelper.Entry entry) {
                     this.cosmetica.select(entry);
                  } else if (value4 instanceof EntriesHelper.SwordEntry swordEntry) {
                     this.cosmetica.selectSword(swordEntry);
                  }

                  this.config.save();
               }

               return true;
            case COSMETIC_NAV:
               if (value == 0 && region.target() instanceof String text) {
                  if (!text.equals(this.cosmeticNav)) {
                     this.cosmeticSwordFrom = this.cosmeticSwordBlend();
                     this.cosmeticSwordTo = "swords".equals(text) ? 1.0F : 0.0F;
                     this.cosmeticTabChangedAt = System.nanoTime();
                     this.cosmeticNav = text;
                     this.config.setCosmeticSubcategory(text);
                     this.config.save();
                  }

                  this.scroll = 0.0F;
                  this.targetScroll = 0.0F;
                  this.maxScroll = 0.0F;
               }

               return true;
            case COSMETIC_ADVANCED:
            case COSMETIC_REFRESH:
               return true;
            case SEARCH:
               if (value == 0) {
                  this.searchFocused = true;
                  this.configNameFocused = false;
                  this.commitStringEdit();
               }

               return true;
            case CONFIG_NAME:
               if (value == 0) {
                  this.configNameFocused = true;
                  this.searchFocused = false;
                  this.commitStringEdit();
               }

               return true;
            case CONFIG_SAVE:
               if (value == 0 && !this.configNameBuffer.isBlank()) {
                  String text2 = this.configNameBuffer.trim().toLowerCase(Locale.ROOT);
                  if (this.config.saveProfile(this.configNameBuffer)) {
                     this.configAppearAt.put(text2, System.nanoTime());
                     this.invalidateConfigProfiles();
                     this.configNameBuffer = "";
                     this.configNameFocused = false;
                  }
               }

               return true;
            case CONFIG_LOAD:
               if (value == 0) {
                  this.config.loadProfile((String)region.target());
               }

               return true;
            case CONFIG_FAVORITE:
               if (value == 0) {
                  String text3 = (String)region.target();
                  this.config.toggleProfileFavorite(text3);
                  this.invalidateConfigProfiles();
               }

               return true;
            case CONFIG_DELETE:
               if (value == 0) {
                  String text4 = (String)region.target();
                  this.configDeleteAt.putIfAbsent(text4, System.nanoTime());
               }

               return true;
            case PICKER_SV:
            case PICKER_HUE:
            case PICKER_ALPHA:
               if (value == 0) {
                  this.pickerDrag = region.kind();
                  this.updatePicker(region, x, y);
               }

               return true;
            case PICKER_HEX:
               if (value == 0) {
                  this.pickerHexFocused = true;
                  this.pickerHex = this.hexForColor(this.pickerTarget.get());
               }

               return true;
            default:
               return true;
         }
      } else {
         this.finishBinding(1000 + value);
         return true;
      }
   }

   @Override
   public boolean isPauseScreen() {
      return false;
   }

   private void updateScrollMotion() {
      float computedValue = 1.0F - (float)Math.pow(8.0E-4F, this.animationDelta);
      this.scroll = this.scroll + (this.targetScroll - this.scroll) * computedValue;
      float computedValue2 = this.targetScroll - this.scroll;
      if (Math.abs(computedValue2) < 0.02F) {
         this.scroll = this.targetScroll;
      }
   }

   private int moduleEnabledColor() {
      return this.elevatedColor();
   }

   @Override
   protected void init() {
      this.animatedScale = this.targetScale();
      Float orDefault = rememberedScroll.getOrDefault(this.selectedView, 0.0F);
      this.scroll = orDefault;
      this.targetScroll = this.scroll;
      this.maxScroll = 0.0F;
   }

   @Override
   public void onClose() {
      rememberedView = this.selectedView;
      this.startClose();
   }

   private void renderTabGroup(RenderGuiHelper.View[] viewValues, float index, float color, float color2, float color3, boolean condition) {
      float index2 = index;

      for (RenderGuiHelper.View view : viewValues) {
         String text = view.name();
         float color4 = this.motionNamed("view:" + text, view == this.selectedView ? 1.0F : 0.0F);
         float value = this.tabWidth(view, color2);
         if (color4 > 0.001F) {
            BeginFrameHelper.rect(index2, color, value, 38.0F * color2).color(alpha(this.elevatedColor(), color3 * color4)).radius(16.0F * color2).draw();
         }

         float computedValue = index2 + (38.0F * color2 - 11.0F * color2) * 0.5F;
         float computedValue2 = index2 + 16.0F * color2;
         float color5 = computedValue + (computedValue2 - computedValue) * color4;
         this.csIconCentered(color5, color, 11.0F * color2, 38.0F * color2, 11.0F, view.icon, mixColor(this.tertiaryColor(), this.accentColor(), color4), color2, color3);
         if (color4 > 0.001F) {
            this.text(index2 + 35.0F * color2, color + 12.0F * color2, 11.0F, view.label, this.primaryColor(), 500, color2, color3 * color4);
         }

         if (condition) {
            this.regions.add(new TargetHelper(GuiActionType.VIEW, view, (String)null, index2, color, value, 38.0F * color2, null));
         }

         index2 += value;
      }
   }

   private RenderGuiHelper.DockSide snapCandidate(double width, double height) {
      if (this.width > 0 && this.height > 0) {
         double width2 = width - this.width * 0.5;
         double computedValue = width2 / this.width;
         double height2 = height - this.height * 0.5;
         double computedValue2 = height2 / this.height;
         double computedValue3 = Math.abs(computedValue);
         if (computedValue3 >= Math.abs(computedValue2)) {
            RenderGuiHelper.DockSide dockSide;
            if (computedValue < 0.0) {
               dockSide = RenderGuiHelper.DockSide.LEFT;
            } else {
               dockSide = RenderGuiHelper.DockSide.RIGHT;
            }

            return dockSide;
         } else {
            RenderGuiHelper.DockSide dockSide2;
            if (computedValue2 < 0.0) {
               dockSide2 = RenderGuiHelper.DockSide.TOP;
            } else {
               dockSide2 = RenderGuiHelper.DockSide.BOTTOM;
            }

            return dockSide2;
         }
      } else {
         return this.categoryDock;
      }
   }

   private void renderCategoryNavigation(float coordinate, float coordinate2, float coordinate3, float coordinate4, float width, float color) {
      RenderGuiHelper.View[] viewValues = new RenderGuiHelper.View[]{
         RenderGuiHelper.View.COMBAT, RenderGuiHelper.View.MOVEMENT, RenderGuiHelper.View.RENDER, RenderGuiHelper.View.PLAYER, RenderGuiHelper.View.MISC
      };
      RenderGuiHelper.View[] viewValues2 = new RenderGuiHelper.View[]{RenderGuiHelper.View.AUTOBUY, RenderGuiHelper.View.CONFIGS, RenderGuiHelper.View.COSMETICS};
      RenderGuiHelper.DockSide dockSide = this.navDragging && this.navPreview != null ? this.navPreview : this.categoryDock;
      int value = dockSide != RenderGuiHelper.DockSide.LEFT && dockSide != RenderGuiHelper.DockSide.RIGHT ? 0 : 1;
      float computedValue = 5.0F * width;
      float computedValue2 = 8.0F * width;
      float computedValue3 = 38.0F * width;
      float value2 = this.tabGroupWidth(viewValues, width);
      float value3 = this.tabGroupWidth(viewValues2, width);
      float computedValue4 = viewValues.length * computedValue3;
      float computedValue5 = viewValues2.length * computedValue3;
      float computedValue6 = value2 + computedValue2 + value3 + computedValue * 2.0F;
      float computedValue7 = computedValue3 + computedValue * 2.0F;
      float computedValue8 = computedValue3 + computedValue * 2.0F;
      float computedValue9 = computedValue4 + computedValue2 + computedValue5 + computedValue * 2.0F;
      float computedValue10 = 12.0F * width;
      float width2 = value != 0 ? computedValue8 : computedValue6;
      float height = value != 0 ? computedValue9 : computedValue7;
      float width3;
      float height2;
      switch (dockSide) {
         case TOP:
            width3 = coordinate + (coordinate3 - width2) * 0.5F;
            height2 = coordinate2 - computedValue10 - height;
            break;
         case BOTTOM:
         default:
            width3 = coordinate + (coordinate3 - width2) * 0.5F;
            height2 = coordinate2 + coordinate4 + computedValue10;
            break;
         case LEFT:
            width3 = coordinate - computedValue10 - width2;
            height2 = coordinate2 + (coordinate4 - height) * 0.5F;
            break;
         case RIGHT:
            width3 = coordinate + coordinate3 + computedValue10;
            height2 = coordinate2 + (coordinate4 - height) * 0.5F;
      }

      width3 = clamp(width3, 3.0F, Math.max(3.0F, this.width - width2 - 3.0F));
      height2 = clamp(height2, 3.0F, Math.max(3.0F, this.height - height - 3.0F));
      float computedValue11 = 1.0F - (float)Math.exp(-11.0F * this.animationDelta);
      if (!Float.isFinite(this.navAnimatedX)) {
         this.navAnimatedX = width3;
         this.navAnimatedY = height2;
         this.navAnimatedW = width2;
         this.navAnimatedH = height;
         this.navVerticalBlend = value != 0 ? 1.0F : 0.0F;
      } else {
         this.navAnimatedX = this.navAnimatedX + (width3 - this.navAnimatedX) * computedValue11;
         this.navAnimatedY = this.navAnimatedY + (height2 - this.navAnimatedY) * computedValue11;
         this.navAnimatedW = this.navAnimatedW + (width2 - this.navAnimatedW) * computedValue11;
         this.navAnimatedH = this.navAnimatedH + (height - this.navAnimatedH) * computedValue11;
         float value4 = value != 0 ? 1.0F : 0.0F;
         this.navVerticalBlend = this.navVerticalBlend + (value4 - this.navVerticalBlend) * computedValue11;
      }

      float navAnimatedX2 = this.navAnimatedX;
      float navAnimatedY2 = this.navAnimatedY;
      float navAnimatedW2 = this.navAnimatedW;
      float navAnimatedH2 = this.navAnimatedH;
      boolean transparentStyle = this.transparentStyle();
      BeginFrameHelper.RectBuilder rectBuilder = BeginFrameHelper.rect(navAnimatedX2, navAnimatedY2, navAnimatedW2, navAnimatedH2).color(alpha(this.guiBaseColor(transparentStyle), color)).radius(24.0F * width);
      if (transparentStyle) {
         rectBuilder.blur(36.0F * width, color * color);
      }

      rectBuilder.draw();
      this.regions.add(new TargetHelper(GuiActionType.NAV_DOCK, this, null, navAnimatedX2, navAnimatedY2, navAnimatedW2, navAnimatedH2, null));
      float color2 = color * (1.0F - smoothstep(this.navVerticalBlend));
      float color3 = color * smoothstep(this.navVerticalBlend);
      if (color2 > 0.002F) {
         float computedValue12 = computedValue6 - computedValue * 2.0F;
         float computedValue13 = navAnimatedX2 + (navAnimatedW2 - computedValue12) * 0.5F;
         float computedValue14 = navAnimatedY2 + (navAnimatedH2 - computedValue3) * 0.5F;
         BeginFrameHelper.rect(computedValue13, computedValue14, value2, computedValue3).color(alpha(this.surfaceColor(), color2)).radius(16.0F * width).draw();
         this.renderTabGroup(viewValues, computedValue13, computedValue14, width, color2, this.navVerticalBlend < 0.5F);
         computedValue13 += value2 + computedValue2;
         BeginFrameHelper.rect(computedValue13, computedValue14, value3, computedValue3).color(alpha(this.surfaceColor(), color2)).radius(16.0F * width).draw();
         this.renderTabGroup(viewValues2, computedValue13, computedValue14, width, color2, this.navVerticalBlend < 0.5F);
      }

      if (color3 > 0.002F) {
         float computedValue15 = computedValue9 - computedValue * 2.0F;
         float computedValue16 = navAnimatedX2 + (navAnimatedW2 - computedValue3) * 0.5F;
         float computedValue17 = navAnimatedY2 + (navAnimatedH2 - computedValue15) * 0.5F;
         BeginFrameHelper.rect(computedValue16, computedValue17, computedValue3, computedValue4).color(alpha(this.surfaceColor(), color3)).radius(16.0F * width).draw();
         this.renderVerticalTabGroup(viewValues, computedValue16, computedValue17, width, color3, this.navVerticalBlend >= 0.5F);
         computedValue17 += computedValue4 + computedValue2;
         BeginFrameHelper.rect(computedValue16, computedValue17, computedValue3, computedValue5).color(alpha(this.surfaceColor(), color3)).radius(16.0F * width).draw();
         this.renderVerticalTabGroup(viewValues2, computedValue16, computedValue17, width, color3, this.navVerticalBlend >= 0.5F);
      }
   }

   private void renderTabGroup(RenderGuiHelper.View[] viewValues, float coordinate, float coordinate2, float coordinate3, float coordinate4) {
      this.renderTabGroup(viewValues, coordinate, coordinate2, coordinate3, coordinate4, true);
   }

   public static boolean stabilizeOpeningBlur() {
      Minecraft instance = Minecraft.getInstance();
      if (instance != null) {
         if (instance.gui.screen() instanceof RenderGuiHelper renderGuiHelper) {
            if (!renderGuiHelper.closing) {
               return elapsedMs(renderGuiHelper.openedAt) < 760L;
            }
         }
      }

      return false;
   }

   @Override
   public void extractBackground(GuiGraphicsExtractor guiGraphicsExtractor, int value, int value2, float value3) {
   }

   private void commitStringEdit() {
      if (this.editingString != null) {
         this.editingString.set(this.stringBuffer);
         this.editingString = null;
         this.stringBuffer = "";
         this.config.save();
      }
   }

   @Override
   public void addHit(GuiActionType guiActionType, Object value, String text, float value2, float value3, float value4, float value5, Object value6) {
      this.regions.add(new TargetHelper(guiActionType, value, text, value2, value3, value4, value5, null));
   }

   private void renderConfigs(float coordinate, float coordinate2, float coordinate3, float coordinate4) {
      this.configDeleteAt.entrySet().removeIf(entry -> {
         Long value = entry.getValue();
         long timestamp = value;
         if (elapsedMs(timestamp) < 180L) {
            return false;
         }

         this.config.deleteProfile(entry.getKey());
         this.configAppearAt.remove(entry.getKey());
         this.invalidateConfigProfiles();
         return true;
      });
      ArrayList<ConfigManager.ProfileInfo> profileInfos = new ArrayList<>(this.configProfiles());
      String text = this.search.trim().toLowerCase(Locale.ROOT);
      if (!text.isEmpty()) {
         profileInfos.removeIf(profileInfo -> {
            String text2 = profileInfo.name();
            if (!text2.toLowerCase(Locale.ROOT).contains(text)) {
               String text3 = profileInfo.author();
               String text4 = text3.toLowerCase(Locale.ROOT);
               if (!text4.contains(text)) {
                  return true;
               }
            }

            return false;
         });
      }

      profileInfos.sort((profileInfo2, profileInfo3) -> {
         boolean favorite = profileInfo2.favorite();
         boolean favorite2 = profileInfo3.favorite();
         if (favorite != favorite2) {
            return favorite ? -1 : 1;
         }

         Comparator<String> cASE_INSENSITIVE_ORDER = String.CASE_INSENSITIVE_ORDER;
         String text5 = profileInfo2.name();
         String text6 = profileInfo3.name();
         return cASE_INSENSITIVE_ORDER.compare(text5, text6);
      });
      float value2 = this.viewTransition();
      float computedValue = (1.0F - value2) * 12.0F * coordinate3;
      float computedValue2 = 235.5F * coordinate3;
      float computedValue3 = 8.0F * coordinate3;
      float[] floatValues = new float[]{coordinate2 - this.scroll, coordinate2 - this.scroll, coordinate2 - this.scroll, coordinate2 - this.scroll};

      for (ConfigManager.ProfileInfo profileInfo4 : profileInfos) {
         String text7 = profileInfo4.name();
         int value3 = minIndex(floatValues);
         float computedValue4 = coordinate + value3 * (computedValue2 + computedValue3);
         float value4 = floatValues[value3];
         Long element = this.configDeleteAt.get(text7);
         float computedValue5 = element == null ? 0.0F : smoothstep(clamp((float)elapsedMs(element) / 180.0F, 0.0F, 1.0F));
         Long element2 = this.configAppearAt.get(text7);
         float value5 = 1.0F;
         if (element2 != null) {
            value5 = smoothstep(clamp((float)elapsedMs(element2) / 220.0F, 0.0F, 1.0F));
            if (value5 >= 0.999F) {
               this.configAppearAt.remove(text7);
            }
         }

         float maximum = Math.max(0.0F, 1.0F - computedValue5);
         float computedValue6 = 68.0F * coordinate3 * maximum;
         if (value4 + Math.max(computedValue6, 1.0F) >= coordinate2 && value4 <= coordinate2 + 642.0F * coordinate3) {
            this.renderConfigCard(profileInfo4, computedValue4, value4 + computedValue, computedValue2, Math.max(computedValue6, 1.0F), coordinate3, coordinate4 * value5 * maximum * value2, computedValue5);
         }

         floatValues[value3] += computedValue6 + 8.0F * coordinate3 * maximum;
      }

      float computedValue7 = Math.max(Math.max(floatValues[0], floatValues[1]), Math.max(floatValues[2], floatValues[3])) + this.scroll;
      float maximum2 = Math.max(0.0F, computedValue7 - coordinate2 - 8.0F * coordinate3);
      float computedValue8 = 642.0F * coordinate3;
      this.maxScroll = Math.max(0.0F, maximum2 - computedValue8);
      this.targetScroll = clamp(this.targetScroll, 0.0F, this.maxScroll);
   }

   private int scrollBarColor() {
      int value = 0;
      if (this.transparentStyle()) {
         value = this.guiSolidBase();
         value = withAlphaByte(shiftRgb(value, 5), 88);
      } else {
         value = this.elevatedColor();
      }

      return value;
   }

   private float cosmeticToggleProgress(String text, boolean condition) {
      String text2 = "cosmetic-toggle:" + text;
      float intermediate = condition ? 1.0F : 0.0F;
      return this.motionNamed(text2, intermediate);
   }

   private void updateAnimationClock() {
      long nowNanos = System.nanoTime();
      this.animationDelta = clamp((float)(nowNanos - this.lastAnimationFrame) / 1.0E9F, 0.004166667F, 0.05F);
      this.lastAnimationFrame = nowNanos;
   }

   private boolean isInterface(ClientModule clientModule) {
      if (clientModule != null) {
         String name = clientModule.getName();
         if ("Interface".equalsIgnoreCase(name)) {
            return true;
         }
      }

      return false;
   }

   @Override
   public boolean shouldCloseOnEsc() {
      return false;
   }

   private static int mixColor(int color, int color2, float color3) {
      return MixColorHelper.mixColor(color, color2, color3);
   }

   private void selectView(RenderGuiHelper.View selectedView2) {
      if (selectedView2 != this.selectedView) {
         rememberedScroll.put(this.selectedView, this.targetScroll);
         this.selectedView = selectedView2;
         rememberedView = selectedView2;
         this.viewChangedAt = System.nanoTime();
      }

      this.targetScroll = 0.0F;
      this.scroll = 0.0F;
      this.openDropdown = null;
      this.searchFocused = false;
   }

   private int descriptionLineCount(String text, float width, float width2, int value) {
      if (text != null && !text.isBlank()) {
         String[] stringValues = text.trim().split("\\s+");
         int value2 = 1;
         StringBuilder stringBuilder = new StringBuilder();

         for (String text2 : stringValues) {
            String text3 = stringBuilder.length() == 0 ? text2 : stringBuilder + " " + text2;
            if (!(InterBoldHelper.forWeight(400).getWidth(text3, width2) <= width) && stringBuilder.length() != 0) {
               if (value2 < value) {
                  value2++;
                  stringBuilder.setLength(0);
                  stringBuilder.append(text2);
               }
            } else {
               if (stringBuilder.length() > 0) {
                  stringBuilder.append(' ');
               }

               stringBuilder.append(text2);
            }
         }

         return Math.min(value2, value);
      } else {
         return 0;
      }
   }

   private static float clamp(float value, float value2, float value3) {
      return Math.max(value2, Math.min(value3, value));
   }

   private void renderLanguageButton(float coordinate, float coordinate2, float coordinate3, float color) {
      int color2 = -1;
      color2 = (byte)1;
      float computedValue = 38.0F * coordinate3;
      float color3 = this.motionNamed("language:russian", SelectAndSaveHelper.isRussian() ? 1.0F : 0.0F);
      BeginFrameHelper.RectBuilder rectBuilder = BeginFrameHelper.rect(coordinate, coordinate2, computedValue, computedValue);
      color2 = this.surfaceColor();
      rectBuilder.color(alpha(mixColor(color2, this.elevatedColor(), color3), color)).radius(16.0F * coordinate3).draw();
      float computedValue2 = 12.0F * coordinate3;
      float computedValue3 = coordinate + (computedValue - computedValue2) * 0.5F;
      float computedValue4 = coordinate2 + (computedValue - computedValue2) * 0.5F;
      BeginFrameHelper.TextureBuilder textureBuilder = BeginFrameHelper.texture(computedValue3, computedValue4, computedValue2, computedValue2, LANGUAGE_ICON);
      color2 = mixColor(this.secondaryColor(), this.accentColor(), color3);
      BeginFrameHelper.TextureBuilder textureBuilder2 = textureBuilder.color(alpha(color2, color));
      textureBuilder2.draw();
      this.regions.add(new TargetHelper(GuiActionType.LANGUAGE, this, null, coordinate, coordinate2, computedValue, computedValue, null));
   }

   private void csIconCentered(float value, float value2, float value3, float value4, float value5, String text, int alpha, float value6, float alpha2) {
      int color = -1;
      color = (byte)-1;
      color = (byte)-1;
      float width = value5 * value6;
      TextHeightHelper textHeightHelper = InterBoldHelper.csGuiIcons();
      float width2 = textHeightHelper.getWidth(text, width);
      color = (byte)1;
      TextHeightHelper textHeightHelper2 = InterBoldHelper.csGuiIcons();
      float value7 = textHeightHelper2.textHeight(width);
      BeginFrameHelper.TextBuilder textBuilder = BeginFrameHelper.text(value + (value3 - width2) * 0.5F, value2 + (value4 - value7) * 0.5F, width, text);
      TextHeightHelper textHeightHelper3 = InterBoldHelper.csGuiIcons();
      BeginFrameHelper.TextBuilder textBuilder2 = textBuilder.font(textHeightHelper3);
      color = alpha(alpha, alpha2);
      BeginFrameHelper.TextBuilder textBuilder3 = textBuilder2.color(color);
      textBuilder3.draw();
   }

   @Override
   public boolean mouseScrolled(double value, double value2, double value3, double value4) {
      float value5 = this.currentScale();
      float width = 992.0F * value5;
      float height = 704.0F * value5;
      float computedValue = (this.width - width) * 0.5F;
      float computedValue2 = (this.height - height) * 0.5F;
      float computedValue3 = computedValue + 8.0F * value5;
      float computedValue4 = computedValue2 + 54.0F * value5;
      float intermediate = 976.0F * value5;
      float intermediate2 = 642.0F * value5;
      if (contains(value, value2, computedValue3, computedValue4, intermediate, intermediate2)) {
         this.targetScroll = clamp(this.targetScroll - (float)value4 * 52.0F * value5, 0.0F, this.maxScroll);
         return true;
      } else {
         return false;
      }
   }

   private void renderSearch(float coordinate, float coordinate2, float coordinate3, float coordinate4, float coordinate5, float color) {
      int color2 = -1;
      float intermediate = this.searchFocused ? 1.0F : 0.0F;
      float color3 = this.motionNamed("search:focus", intermediate);
      BeginFrameHelper.RectBuilder rectBuilder = BeginFrameHelper.rect(coordinate, coordinate2, coordinate3, coordinate4);
      color2 = this.surfaceColor();
      int color4 = this.elevatedColor();
      int color5 = -1;
      color2 = alpha(mixColor(color2, color4, color3), color);
      rectBuilder.color(color2).radius(16.0F * coordinate5).draw();
      String text;
      if (this.search.isEmpty()) {
         text = "Search...";
      } else {
         String text2 = this.searchFocused ? "_" : "";
         text = this.search + text2;
      }

      String text3 = text;
      float computedValue = coordinate + (16.0F + 2.0F * color3) * coordinate5;
      intermediate = coordinate2 + 12.0F * coordinate5;
      if (this.search.isEmpty()) {
         color5 = this.secondaryColor();
      } else {
         color5 = this.primaryColor();
      }

      this.text(computedValue, intermediate, 11.0F, text3, color5, 500, coordinate5, color);
      float computedValue2 = coordinate + coordinate3 - (27.0F + color3) * coordinate5;
      float computedValue3 = 11.0F * coordinate5;
      int color6 = this.tertiaryColor();
      int color7 = this.accentColor();
      color6 = mixColor(color6, color7, color3);
      this.csIconCentered(computedValue2, coordinate2, computedValue3, coordinate4, 11.0F, "\ue00e", color6, coordinate5, color);
      String text4 = (String)null;
      this.regions.add(new TargetHelper(GuiActionType.SEARCH, this, text4, coordinate, coordinate2, coordinate3, coordinate4, null));
   }

   @Override
   public int fieldActiveColor() {
      int value = 1;
      int value2 = 0;
      value = (byte)1;
      value2 = (byte)0;
      value2 = this.guiSolidBase();
      int alpha = shiftRgb(value2, 14);
      if (this.transparentStyle()) {
         value = this.guiAlpha(114);
         value2 = withAlphaByte(alpha, value);
      } else {
         value2 = alpha;
      }

      return value2;
   }

   private int currentPickerRgb() {
      return Color.HSBtoRGB(this.pickerHue, this.pickerSaturation, this.pickerValue);
   }

   private void applyPickerColor() {
      int value = 0;
      value = (byte)0;
      if (this.pickerTarget != null) {
         int color = -1;
         int value2 = Color.HSBtoRGB(this.pickerHue, this.pickerSaturation, this.pickerValue) & 16777215;
         color = (byte)-1;
         if (this.pickerTarget.hasAlpha()) {
            color = (byte)1;
            value = Math.round(this.pickerAlpha * 255.0F);
         } else {
            value = 255;
         }

         int value3 = value;
         color = (byte)1;
         this.pickerTarget.set(value3 << 24 | value2);
         color = this.pickerTarget.get();
         this.pickerHex = this.hexForColor(color);
      }
   }

   private void renderModule(ClientModule clientModule, float coordinate, float coordinate2, float coordinate3, float coordinate4, float color, float color2) {
      float color3 = this.motion(clientModule, clientModule.isEnabled() ? 1.0F : 0.0F);
      int alpha = mixColor(this.moduleDisabledColor(), this.moduleEnabledColor(), color3);
      float color4 = 16.0F * color;
      BeginFrameHelper.rect(coordinate, coordinate2, coordinate3, coordinate4).color(alpha(alpha, color2)).radius(color4).draw();
      int value = mixColor(this.secondaryColor(), this.primaryColor(), color3);
      this.text(coordinate + 16.0F * color, coordinate2 + 16.0F * color, 12.0F, clientModule.getName(), value, 500, color, color2);
      float computedValue = 20.0F * color;
      float computedValue2 = 12.0F * color;
      float computedValue3 = coordinate + coordinate3 - 16.0F * color - computedValue;
      float computedValue4 = coordinate2 + 17.0F * color;
      String keyBind = this.bindingModule == clientModule ? "..." : KeyBinds.name(clientModule.getKeyBind());
      int keyBind2 = clientModule.getKeyBind() != -1 ? 1 : 0;
      if (keyBind2 == 0 && this.bindingModule != clientModule) {
         keyBind = "n/a";
      }

      float width = InterBoldHelper.forWeight(500).getWidth(keyBind, 10.0F * color);
      float computedValue5 = keyBind2 != 0 ? 14.0F * color : 0.0F;
      float maximum = Math.max(27.0F * color, width + 12.0F * color + computedValue5);
      float color5 = 20.0F * color;
      float computedValue6 = computedValue3 - 8.0F * color - maximum;
      float color6 = coordinate2 + 13.0F * color;
      String name = clientModule.getName();
      float color7 = this.motionNamed("module-bind:" + name, this.bindingModule == clientModule ? 1.0F : 0.0F);
      int color8 = keyBind2 != 0 ? this.trackColor() : this.surfaceColor();
      int color9 = keyBind2 != 0 ? this.accentColor() : this.fieldColor();
      int color10 = mixColor(color8, color9, color3);
      if (color7 > 0.0F) {
         color10 = mixColor(color10, keyBind2 != 0 ? this.accentColor() : this.trackColor(), color7);
      }

      BeginFrameHelper.rect(computedValue6, color6, maximum, color5).color(alpha(color10, color2)).radius(8.0F * color).draw();
      float computedValue7 = width + computedValue5;
      float computedValue8 = computedValue6 + (maximum - computedValue7) * 0.5F;
      int color11 = keyBind2 != 0 ? this.secondaryColor() : this.tertiaryColor();
      int color12 = keyBind2 != 0 ? this.primaryColor() : this.secondaryColor();
      int color13 = mixColor(color11, color12, color3);
      if (color7 > 0.0F) {
         color13 = mixColor(color13, keyBind2 != 0 ? this.primaryColor() : this.secondaryColor(), color7);
      }

      this.text(computedValue8, color6 + 4.0F * color, 10.0F, keyBind, color13, 500, color, color2);
      if (keyBind2 != 0) {
         this.csIconCentered(
            computedValue8 + width + 4.0F * color,
            color6,
            10.0F * color,
            color5,
            10.0F,
            "\ue004",
            mixColor(this.tertiaryColor(), this.primaryColor(), Math.max(color3, color7)),
            color,
            color2
         );
      }

      BeginFrameHelper.rect(computedValue3, computedValue4, computedValue, computedValue2)
         .color(alpha(mixColor(this.togglerOffColor(), this.accentColor(), color3), color2))
         .radius(512.0F * color)
         .draw();
      float computedValue9 = 8.0F * color;
      float computedValue10 = computedValue3 + 2.0F * color + (computedValue - 4.0F * color - computedValue9) * color3;
      BeginFrameHelper.rect(computedValue10, computedValue4 + 2.0F * color, computedValue9, computedValue9)
         .color(alpha(mixColor(this.quaternaryColor(), this.primaryColor(), color3), color2))
         .radius(512.0F * color)
         .draw();
      this.drawDescription(
         this.localize(clientModule.getDescription()),
         coordinate + 16.0F * color,
         coordinate2 + 38.0F * color,
         coordinate3 - 32.0F * color,
         color,
         color2,
         mixColor(this.tertiaryColor(), this.secondaryColor(), color3)
      );
      float value2 = this.moduleHeaderRawHeight(clientModule);
      this.regions.add(new TargetHelper(GuiActionType.MODULE, clientModule, (String)null, coordinate, coordinate2, coordinate3, value2 * color, null));
      this.regions.add(new TargetHelper(GuiActionType.KEYBIND, clientModule, (String)null, computedValue6, color6, maximum, color5, null));
      this.regions.add(new TargetHelper(GuiActionType.MODULE, clientModule, (String)null, computedValue3, computedValue4 - 4.0F * color, computedValue, 20.0F * color, null));
      var settings = this.visibleSettings(clientModule);
      int value3 = this.isInterface(clientModule) ? 6 : 0;
      if (!settings.isEmpty() || value3 != 0) {
         float computedValue11 = coordinate2 + value2 * color;
         BeginFrameHelper.rect(coordinate + color, computedValue11, coordinate3 - 2.0F * color, Math.max(0.5F, color)).color(alpha(this.fieldColor(), color2)).draw();
         float color14 = coordinate + 16.0F * color;
         float color15 = coordinate3 - 32.0F * color;
         float color16 = computedValue11 + 17.0F * color;

         for (int value4 = 0; value4 < settings.size(); value4++) {
            Setting setting = (Setting)settings.get(value4);
            float computedValue12 = this.settingRawHeight(setting) * color;
            BeginFrameHelper.pushScissor(color14, color16 - 8.0F * color, color15, computedValue12 + 8.0F * color);

            try {
               this.elementFor(setting).render(this, color14, color16, color15, color, color2, null, color3);
            } finally {
               BeginFrameHelper.popScissor();
            }

            color16 += computedValue12;
            int computedValue13 = value4 >= settings.size() - 1 && value3 <= 0 ? 0 : 1;
            if (computedValue13 != 0) {
               color16 += 12.0F * color;
            }
         }

         if (value3 > 0) {
            String[] stringValues = new String[]{"Accent", "Text", "Secondary", "Tertiary", "HUD Background", "GUI Background"};

            for (int value5 = 1; value5 <= 6; value5++) {
               this.renderThemeColorRow(value5, stringValues[value5 - 1], color14, color16, color15, color, color2, color3);
               color16 += 28.0F * color;
               if (value5 < 6) {
                  color16 += 12.0F * color;
               }
            }
         }
      }
   }

   private static float smoothstep(float progress) {
      progress = clamp(progress, 0.0F, 1.0F);
      return progress * progress * (3.0F - 2.0F * progress);
   }

   private void renderCosmeticThumb(float coordinate, float coordinate2, float coordinate3, Identifier texture, float alpha) {
      GuiGraphicsExtractor guiGraphicsExtractor = CurrentGuiGraphicsExtractorHelper.currentGuiGraphicsExtractor();
      if (guiGraphicsExtractor != null) {
         float computedValue = coordinate + coordinate3 * 0.5F;
         float computedValue2 = coordinate2 + coordinate3 * 0.5F;
         float computedValue3 = coordinate3 * 0.88F;
         guiGraphicsExtractor.pose().pushMatrix();

         try {
            guiGraphicsExtractor.pose().translate(computedValue, computedValue2);
            guiGraphicsExtractor.pose().rotate((float)Math.toRadians(32.0));
            BeginFrameHelper.texture(-computedValue3 * 0.5F, -computedValue3 * 0.5F, computedValue3, computedValue3, texture).color(alpha(-1, alpha)).draw();
            BeginFrameHelper.flush();
         } finally {
            guiGraphicsExtractor.pose().popMatrix();
         }
      }
   }

   private boolean matchesSearch(ClientModule clientModule) {
      if (this.search != null) {
         if (!this.search.isBlank()) {
            String text = this.search.toLowerCase(Locale.ROOT);
            String name = clientModule.getName().toLowerCase(Locale.ROOT);
            if (!name.contains(text)) {
               name = clientModule.getDescription();
               if (!name.toLowerCase(Locale.ROOT).contains(text)) {
                  String description = clientModule.getDescription();
                  name = this.localize(description);
                  if (!name.toLowerCase(Locale.ROOT).contains(text)) {
                     name = clientModule.getCategory().displayName();
                     if (!name.toLowerCase(Locale.ROOT).contains(text)) {
                        ModuleCategory category = clientModule.getCategory();
                        description = category.displayName();
                        name = this.localize(description);
                        if (!name.toLowerCase(Locale.ROOT).contains(text)) {
                           return false;
                        }
                     }
                  }
               }
            }

            return true;
         }
      }

      return true;
   }

   @Override
   public void toggleDropdown(Setting setting) {
      this.openDropdown = this.openDropdown == setting ? null : setting;
   }

   private void renderScrollBar(float coordinate, float coordinate2, float coordinate3, float color) {
      float computedValue = coordinate + 974.0F * coordinate3;
      float computedValue2 = 634.0F * coordinate3;
      BeginFrameHelper.RectBuilder rectBuilder = BeginFrameHelper.rect(computedValue, coordinate2, 2.0F * coordinate3, computedValue2);
      int color2 = this.surfaceColor();
      rectBuilder.color(alpha(color2, color)).radius(512.0F * coordinate3).draw();
      if (!(this.maxScroll <= 0.5F)) {
         float computedValue3 = 642.0F * coordinate3;
         float computedValue4 = computedValue3 + this.maxScroll;
         float computedValue5 = Math.max(28.0F * coordinate3, computedValue2 * (computedValue3 / computedValue4));
         float computedValue6 = this.scroll / this.maxScroll;
         float clampedValue = clamp(computedValue6, 0.0F, 1.0F);
         float computedValue7 = coordinate2 + (computedValue2 - computedValue5) * clampedValue;
         float computedValue8 = 2.0F * coordinate3;
         rectBuilder = BeginFrameHelper.rect(computedValue, computedValue7, computedValue8, computedValue5);
         color2 = alpha(this.elevatedColor(), color);
         rectBuilder = rectBuilder.color(color2);
         float radius = 512.0F * coordinate3;
         rectBuilder = rectBuilder.radius(radius);
         rectBuilder.draw();
      }
   }

   private String ellipsize(String text, float width, float width2, int value) {
      if (text == null) {
         return "";
      }

      if (InterBoldHelper.forWeight(value).getWidth(text, width2) <= width) {
         return text;
      }

      String text2 = "...";
      int length = text.length();

      while (length > 0 && InterBoldHelper.forWeight(value).getWidth(text.substring(0, length) + text2, width2) > width) {
         length--;
      }

      String text3;
      if (length <= 0) {
         text3 = text2;
      } else {
         text3 = text.substring(0, length) + text2;
      }

      return text3;
   }

   @Override
   public StringSetting editingString() {
      return this.editingString;
   }

   private float moduleBaseRawHeight(ClientModule clientModule) {
      return this.moduleRawHeight(clientModule, false);
   }

   private float viewTransition() {
      if (this.viewChangedAt == 0L) {
         return 1.0F;
      }

      float computedValue = (float)elapsedMs(this.viewChangedAt) / 250.0F;
      float clampedValue = clamp(computedValue, 0.0F, 1.0F);
      if (clampedValue >= 1.0F) {
         this.viewChangedAt = 0L;
      }

      return smoothstep(clampedValue);
   }

   @Override
   public boolean mouseReleased(MouseButtonEvent progress) {
      if (this.navPressActive && progress.button() == 0) {
         RenderGuiHelper.DockSide dockSide = this.navDragging ? this.snapCandidate(progress.x(), progress.y()) : null;
         if (this.navDragging) {
            if (dockSide != null && dockSide != this.categoryDock) {
               this.categoryDock = dockSide;
               this.config.setCategoryDock(dockSide.name());
               this.config.save();
            }
         } else if (this.navPressedView != null) {
            this.selectView(this.navPressedView);
         }

         this.navPressActive = false;
         this.navDragging = false;
         this.navPressedView = null;
         this.navPreview = null;
         return true;
      } else {
         if (this.activeSlider != null) {
            this.config.save();
            this.activeSlider = null;
            this.activeSliderRegion = null;
         }

         if (this.pickerDrag != null) {
            this.config.save();
            this.pickerDrag = null;
         }

         return true;
      }
   }

   @Override
   public void tick() {
   }

   private float ease(float progress) {
      progress = clamp(progress, 0.0F, 1.0F);
      return 1.0F - (1.0F - progress) * (1.0F - progress);
   }

   private void renderColorPicker(float color, float color2) {
      RenderGuiHelper.PickerTarget pickerTarget2 = this.pickerTarget;
      if (pickerTarget2 != null) {
         float width = 192.0F * color;
         float height = 208.0F * color;
         this.pickerX = clamp(this.pickerX, 4.0F, Math.max(4.0F, this.width - width - 4.0F));
         this.pickerY = clamp(this.pickerY, 4.0F, Math.max(4.0F, this.height - height - 4.0F));
         BeginFrameHelper.rect(this.pickerX, this.pickerY, width, height)
            .color(alpha(this.guiBaseColor(true), color2))
            .radius(16.0F * color)
            .blur(36.0F * color, color2)
            .draw();
         float alpha = this.pickerX + 16.0F * color;
         float computedValue = this.pickerY + 16.0F * color;
         float alpha2 = 160.0F * color;
         float computedValue2 = 96.0F * color;
         int alpha3 = 0xFF000000 | Color.HSBtoRGB(this.pickerHue, 1.0F, 1.0F) & 16777215;
         BeginFrameHelper.rect(alpha, computedValue, alpha2, computedValue2)
            .color(alpha(-1, color2), alpha(alpha3, color2), alpha(alpha3, color2), alpha(-1, color2))
            .radius(8.0F * color)
            .draw();
         BeginFrameHelper.rect(alpha, computedValue, alpha2, computedValue2).color(0, 0, alpha(-16777216, color2), alpha(-16777216, color2)).radius(8.0F * color).draw();
         float computedValue3 = 6.0F * color;
         float computedValue4 = alpha + this.pickerSaturation * alpha2 - computedValue3 * 0.5F;
         float computedValue5 = computedValue + (1.0F - this.pickerValue) * computedValue2 - computedValue3 * 0.5F;
         BeginFrameHelper.rect(computedValue4, computedValue5, computedValue3, computedValue3).color(16777215).border(Math.max(0.5F, color), alpha(-1, color2)).radius(512.0F * color).draw();
         this.regions.add(new TargetHelper(GuiActionType.PICKER_SV, pickerTarget2, (String)null, alpha, computedValue, alpha2, computedValue2, null));
         float computedValue6 = this.pickerY + 124.0F * color;
         this.renderHueTrack(alpha, computedValue6, alpha2, 6.0F * color, color2);
         float computedValue7 = alpha + this.pickerHue * alpha2 - 2.0F * color;
         BeginFrameHelper.rect(computedValue7, computedValue6 - 2.0F * color, 4.0F * color, 10.0F * color).color(alpha(-1, color2)).radius(2.0F * color).draw();
         this.regions.add(new TargetHelper(GuiActionType.PICKER_HUE, pickerTarget2, (String)null, alpha, computedValue6 - 4.0F * color, alpha2, 14.0F * color, null));
         float computedValue8 = this.pickerY + 144.0F * color;
         this.renderChecker(alpha, computedValue8, alpha2, 6.0F * color, color, color2);
         int alpha4 = this.currentPickerRgb() & 16777215;
         BeginFrameHelper.rect(alpha, computedValue8, alpha2, 6.0F * color)
            .color(alpha(alpha4, color2), alpha(0xFF000000 | alpha4, color2), alpha(0xFF000000 | alpha4, color2), alpha(alpha4, color2))
            .radius(3.0F * color)
            .draw();
         float clampedValue = clamp(alpha + this.pickerAlpha * alpha2 - 2.0F * color, alpha - 2.0F * color, alpha + alpha2 - 2.0F * color);
         BeginFrameHelper.rect(clampedValue, computedValue8 - 2.0F * color, 4.0F * color, 10.0F * color).color(alpha(-1, color2)).radius(2.0F * color).draw();
         this.regions.add(new TargetHelper(GuiActionType.PICKER_ALPHA, pickerTarget2, (String)null, alpha, computedValue8 - 4.0F * color, alpha2, 14.0F * color, null));
         float computedValue9 = this.pickerY + 164.0F * color;
         BeginFrameHelper.rect(alpha, computedValue9, alpha2, 28.0F * color).color(alpha(this.fieldColor(), color2)).radius(12.0F * color).draw();
         String text = this.pickerHexFocused ? this.pickerHex : this.hexForColor(pickerTarget2.get());
         this.text(alpha + 12.0F * color, computedValue9 + 8.0F * color, 10.0F, text, this.primaryColor(), 500, color, color2);
         float computedValue10 = 12.0F * color;
         BeginFrameHelper.rect(alpha + alpha2 - 24.0F * color, computedValue9 + 8.0F * color, computedValue10, computedValue10).color(alpha(pickerTarget2.get(), color2)).radius(512.0F * color).draw();
         this.regions.add(new TargetHelper(GuiActionType.PICKER_HEX, pickerTarget2, (String)null, alpha, computedValue9, alpha2, 28.0F * color, null));
      }
   }

   @Override
   public void extractRenderState(GuiGraphicsExtractor guiGraphicsExtractor, int coordinate, int coordinate2, float coordinate3) {
      int value = 1;
      this.cosmeticPreviewEntity = false;
      this.renderFrame(guiGraphicsExtractor, this.minecraft.getDeltaTracker());
      if (this.cosmeticPreviewEntity) {
         if (this.minecraft != null) {
            if (this.minecraft.player != null) {
               value = this.cosmeticPreviewBottom - this.cosmeticPreviewTop;
               int maximum = Math.max(1, value);
               value = Math.min(58, maximum / 5);
               int maximum2 = Math.max(22, value);
               float intermediate = coordinate;
               float intermediate2 = coordinate2;
               InventoryScreen.extractEntityInInventoryFollowsMouse(
                  guiGraphicsExtractor,
                  this.cosmeticPreviewLeft,
                  this.cosmeticPreviewTop,
                  this.cosmeticPreviewRight,
                  this.cosmeticPreviewBottom,
                  maximum2,
                  0.0F,
                  intermediate,
                  intermediate2,
                  this.minecraft.player
               );
            }
         }
      }
   }

   private int guiSolidBase() {
      return 0xFF000000 | this.themes.backgroundGui() & 16777215;
   }

   private void updatePicker(TargetHelper targetHelper, double value, double value2) {
      if (this.pickerTarget != null) {
         if (targetHelper.kind() == GuiActionType.PICKER_SV) {
            this.pickerSaturation = clamp((float)((value - targetHelper.x()) / targetHelper.w()), 0.0F, 1.0F);
            this.pickerValue = 1.0F - clamp((float)((value2 - targetHelper.y()) / targetHelper.h()), 0.0F, 1.0F);
         } else if (targetHelper.kind() == GuiActionType.PICKER_HUE) {
            this.pickerHue = clamp((float)((value - targetHelper.x()) / targetHelper.w()), 0.0F, 1.0F);
         } else if (targetHelper.kind() == GuiActionType.PICKER_ALPHA) {
            this.pickerAlpha = clamp((float)((value - targetHelper.x()) / targetHelper.w()), 0.0F, 1.0F);
         }

         this.applyPickerColor();
      }
   }

   @Override
   public void text(float value, float value2, float value3, String text, int alpha, int value4, float value5, float alpha2) {
      float intermediate = value3 * value5;
      BeginFrameHelper.TextBuilder textBuilder = BeginFrameHelper.text(value, value2, intermediate, text);
      TextHeightHelper textHeightHelper = InterBoldHelper.forWeight(value4);
      textBuilder.font(textHeightHelper).color(alpha(alpha, alpha2)).draw();
   }

   @Override
   public int trackColor() {
      int value = 1;
      int value2 = 0;
      value = (byte)-1;
      value2 = (byte)0;
      value2 = this.guiSolidBase();
      int alpha = shiftRgb(value2, 14);
      if (this.transparentStyle()) {
         value = this.guiAlpha(130);
         value2 = withAlphaByte(alpha, value);
      } else {
         value2 = alpha;
      }

      return value2;
   }

   private float settingRawHeight(Setting<?> tingRawHeight) {
      DescriptiveHeightHelper descriptiveHeightHelper = this.elementFor(tingRawHeight);
      float result = descriptiveHeightHelper.rawHeight();
      if (tingRawHeight instanceof ModeSetting modeSetting) {
         float value = this.motion(tingRawHeight, this.openDropdown == tingRawHeight ? 1.0F : 0.0F);
         return result + (4.0F + modeSetting.getModes().size() * 24.0F) * value;
      } else if (tingRawHeight instanceof MultiSelectSetting multiSelectSetting) {
         float value2 = this.openDropdown == tingRawHeight ? 1.0F : 0.0F;
         float value3 = this.motion(tingRawHeight, value2);
         var modes = multiSelectSetting.getModes();
         return result + (4.0F + modes.size() * 24.0F) * value3;
      } else {
         return result;
      }
   }

   private void updateSlider(TargetHelper targetHelper, double value) {
      NumberSetting numberSetting = (NumberSetting)targetHelper.target();
      double x = value - targetHelper.x();
      float value2 = targetHelper.w();
      double maximum = x / Math.max(1.0F, value2);
      numberSetting.setRatio(maximum);
   }

   @Override
   public boolean charTyped(CharacterEvent characterEvent) {
      if (!characterEvent.isAllowedChatCharacter()) {
         return false;
      }

      String text = characterEvent.codepointAsString();
      if (this.pickerHexFocused) {
         for (int value = 0; value < text.length(); value++) {
            char character = text.charAt(value);
            if (Character.digit(character, 16) >= 0 && this.pickerHex.length() < 8) {
               String pickerHex2 = this.pickerHex;
               this.pickerHex = pickerHex2 + Character.toUpperCase(character);
            }
         }

         return true;
      } else if (this.editingString != null) {
         if (this.stringBuffer.length() < this.editingString.getMaxLength()) {
            this.stringBuffer = this.stringBuffer + text;
         }

         return true;
      } else if (this.configNameFocused && this.configNameBuffer.length() < 32) {
         this.configNameBuffer = this.configNameBuffer + text;
         return true;
      } else if (this.searchFocused && this.search.length() < 64) {
         this.search = this.search + text;
         this.targetScroll = 0.0F;
         return true;
      } else {
         return false;
      }
   }

   public RenderGuiHelper(ModuleManager modules2, ThemeManager themes2, ConfigManager config2, ClickGUIModule clickGui2) {
      super(Component.literal("Skycore ClickGUI"));
      this.motionStates = new IdentityHashMap<>();
      this.namedStates = new HashMap<>();
      this.pulses = new IdentityHashMap<>();
      this.configDeleteAt = new HashMap<>();
      this.configAppearAt = new HashMap<>();
      this.cachedConfigProfiles = List.of();
      this.moduleColumns = new IdentityHashMap<>();
      this.moduleColumnLayoutKey = "";
      this.navAnimatedX = Float.NaN;
      this.navAnimatedY = Float.NaN;
      this.navAnimatedW = Float.NaN;
      this.navAnimatedH = Float.NaN;
      this.cosmeticNav = "accessories";
      this.selectedView = rememberedView;
      this.stringBuffer = "";
      this.search = "";
      this.configNameBuffer = "";
      this.pickerHex = "";
      this.openedAt = System.nanoTime();
      this.lastAnimationFrame = System.nanoTime();
      this.animationDelta = 0.016666668F;
      this.closingGuiFrom = 1.0F;
      this.animatedScale = -1.0F;
      this.modules = modules2;
      this.cosmetica = new EntriesHelper(modules2);
      this.themes = themes2;
      this.config = config2;
      this.cosmeticNav = config2.cosmeticSubcategory();
      this.categoryDock = RenderGuiHelper.DockSide.valueOf(config2.categoryDock());
      this.cosmeticSwordFrom = this.cosmeticSwordTo = "swords".equals(this.cosmeticNav) ? 1.0F : 0.0F;
      this.clickGui = clickGui2;
   }

   private void renderCosmetics(float coordinate, float coordinate2, float coordinate3, float coordinate4) {
      float value = this.viewTransition();
      float computedValue = (1.0F - value) * 12.0F * coordinate3;
      float computedValue2 = 186.8F * coordinate3;
      float computedValue3 = 233.8F * coordinate3;
      float computedValue4 = 8.0F * coordinate3;
      float computedValue5 = 154.8F * coordinate3;
      float computedValue6 = 186.8F * coordinate3;
      float computedValue7 = 186.8F * coordinate3;
      float computedValue8 = 187.8F * coordinate3;
      float computedValue9 = coordinate2 - this.scroll + computedValue;
      boolean condition = "swords".equals(this.cosmeticNav);
      int value2;
      if (condition) {
         var swordEntrys = this.cosmetica.swords(this.search);
         value2 = swordEntrys.size();

         for (int value3 = 0; value3 < swordEntrys.size(); value3++) {
            EntriesHelper.SwordEntry swordEntry = (EntriesHelper.SwordEntry)swordEntrys.get(value3);
            int computedValue10 = value3 / 5;
            int value4 = value3 % 5;
            float computedValue11 = coordinate + value4 * (computedValue2 + computedValue4);
            float computedValue12 = computedValue9 + computedValue10 * (computedValue3 + computedValue4);
            if (!(computedValue12 + computedValue3 < coordinate2) && !(computedValue12 > coordinate2 + 642.0F * coordinate3)) {
               boolean swordSelected = this.cosmetica.isSwordSelected(swordEntry);
               this.renderCosmeticCard(
                  computedValue11, computedValue12, computedValue2, computedValue3, computedValue5, computedValue6, computedValue7, computedValue8, "sword:" + swordEntry.name(), null, swordEntry.name(), swordEntry.texture(), swordSelected, coordinate3, coordinate4 * value
               );
               this.regions.add(new TargetHelper(GuiActionType.COSMETIC, swordEntry, null, computedValue11, computedValue12, computedValue2, computedValue3, null));
            }
         }
      } else {
         var entrys = this.cosmetica.accessories(this.search);
         value2 = entrys.size();

         for (int value5 = 0; value5 < entrys.size(); value5++) {
            EntriesHelper.Entry entry = (EntriesHelper.Entry)entrys.get(value5);
            int computedValue13 = value5 / 5;
            int value6 = value5 % 5;
            float computedValue14 = coordinate + value6 * (computedValue2 + computedValue4);
            float computedValue15 = computedValue9 + computedValue13 * (computedValue3 + computedValue4);
            if (!(computedValue15 + computedValue3 < coordinate2) && !(computedValue15 > coordinate2 + 642.0F * coordinate3)) {
               boolean selected = this.cosmetica.isSelected(entry);
               this.renderCosmeticCard(
                  computedValue14,
                  computedValue15,
                  computedValue2,
                  computedValue3,
                  computedValue5,
                  computedValue6,
                  computedValue7,
                  computedValue8,
                  "accessory:" + entry.id(),
                  entry.id(),
                  entry.name(),
                  entry.texture(),
                  selected,
                  coordinate3,
                  coordinate4 * value
               );
               this.regions.add(new TargetHelper(GuiActionType.COSMETIC, entry, null, computedValue14, computedValue15, computedValue2, computedValue3, null));
            }
         }
      }

      int computedValue16 = (value2 + 5 - 1) / 5;
      float computedValue17 = computedValue16 <= 0 ? 0.0F : computedValue16 * computedValue3 + Math.max(0, computedValue16 - 1) * computedValue4;
      this.maxScroll = Math.max(0.0F, computedValue17 - 642.0F * coordinate3);
      this.targetScroll = clamp(this.targetScroll, 0.0F, this.maxScroll);
   }

   private static int withAlphaByte(int value, int value2) {
      int value3 = 1;
      value3 = (byte)-1;
      value3 = Math.min(255, value2);
      return Math.max(0, value3) << 24 | value & 16777215;
   }

   @Override
   public KeyBindSetting bindingSetting() {
      return this.bindingSetting;
   }

   private void renderConfigInput(float coordinate, float color, float coordinate2, float color2, float color3, float color4) {
      float color5 = this.motionNamed("config-input:focus", this.configNameFocused ? 1.0F : 0.0F);
      BeginFrameHelper.rect(coordinate, color, coordinate2, color2).color(alpha(mixColor(this.surfaceColor(), this.elevatedColor(), color5), color4)).radius(16.0F * color3).draw();
      this.csIconCentered(
         coordinate + (16.0F + color5) * color3, color, 11.0F * color3, color2, 11.0F, "\ue00b", mixColor(this.tertiaryColor(), this.accentColor(), color5), color3, color4
      );
      String text = this.configNameBuffer.isEmpty() ? "New config..." : this.configNameBuffer + (this.configNameFocused ? "_" : "");
      this.text(
         coordinate + (35.0F + color5) * color3,
         color + 12.0F * color3,
         11.0F,
         text,
         this.configNameBuffer.isEmpty() ? this.secondaryColor() : this.primaryColor(),
         500,
         color3,
         color4
      );
      this.regions.add(new TargetHelper(GuiActionType.CONFIG_NAME, this, (String)null, coordinate, color, coordinate2, color2, null));
      this.regions.add(new TargetHelper(GuiActionType.CONFIG_SAVE, this, (String)null, coordinate, color, 32.0F * color3, color2, null));
   }

   private List<Setting> visibleSettings(ClientModule clientModule) {
      ArrayList elements = new ArrayList();
      var settings = clientModule.getSettings();

      for (Setting setting : settings) {
         if (setting.isVisible()) {
            elements.add(setting);
         }
      }

      return elements;
   }

   private void commitPickerHex() {
      if (this.pickerTarget != null && !this.pickerHex.isBlank()) {
         try {
            String text = this.pickerHex.replace("#", "").trim();
            long timestamp = Long.parseLong(text, 16);
            int value;
            if (text.length() <= 6) {
               value = 0xFF000000 | (int)timestamp;
            } else {
               value = (int)timestamp;
            }

            if (!this.pickerTarget.hasAlpha()) {
               value |= -16777216;
            }

            this.pickerTarget.set(value);
            int value2 = this.pickerTarget.get();
            float[] floatValues = Color.RGBtoHSB(value2 >> 16 & 0xFF, value2 >> 8 & 0xFF, value2 & 0xFF, (float[])null);
            this.pickerHue = floatValues[0];
            this.pickerSaturation = floatValues[1];
            this.pickerValue = floatValues[2];
            this.pickerAlpha = this.pickerTarget.hasAlpha() ? (value2 >>> 24 & 0xFF) / 255.0F : 1.0F;
            this.config.save();
         } catch (Exception failure) {
         }

         this.pickerHexFocused = false;
      } else {
         this.pickerHexFocused = false;
      }
   }

   @Override
   public void triggerPulse(Object value) {
      if (value != null) {
         var pulses2 = this.pulses;
         long nowNanos = System.nanoTime();
         Long element = nowNanos;
         pulses2.put(value, element);
      }
   }

   enum DockSide {
      TOP,
      BOTTOM,
      LEFT,
      RIGHT;
   }

   static final class MotionState {
      private int frame = -1;
      private float value;

      private MotionState(float value2) {
         this.value = value2;
      }

      private void update(float value2, float value3, int frame2) {
         if (this.frame != frame2) {
            this.frame = frame2;
            float computedValue = 1.0F - (float)Math.exp(-14.0F * value3);
            float computedValue2 = this.value + (value2 - this.value) * computedValue;
            this.value = computedValue2;
            if (Math.abs(value2 - this.value) < 5.0E-4F) {
               this.value = value2;
            }
         }
      }
   }

   interface PickerTarget {
      public void set(int value);

      public int get();

      public boolean hasAlpha();
   }

   final class SettingPickerTarget implements RenderGuiHelper.PickerTarget {
      private final ColorSetting setting;

      private SettingPickerTarget(ColorSetting setting2) {
         this.setting = setting2;
      }

      @Override
      public boolean hasAlpha() {
         return this.setting.hasAlpha();
      }

      @Override
      public int get() {
         return this.setting.get();
      }

      @Override
      public void set(int value) {
         this.setting.set(value);
      }
   }

   final class ThemeSlotPickerTarget implements RenderGuiHelper.PickerTarget {
      private final int slot;

      @Override
      public void set(int value) {
         RenderGuiHelper.this.themes.setSlot(this.slot, value);
      }

      @Override
      public boolean hasAlpha() {
         return this.slot != 1;
      }

      private ThemeSlotPickerTarget(final int slot2) {
         this.slot = slot2;
      }

      @Override
      public int get() {
         return RenderGuiHelper.this.themes.slot(this.slot);
      }
   }

   enum View {
      COMBAT("Combat", "\ue005", ModuleCategory.COMBAT),
      MOVEMENT("Movement", "\ue00a", ModuleCategory.MOVEMENT),
      RENDER("Render", "\ue00d", ModuleCategory.RENDER),
      PLAYER("Player", "\ue00c", ModuleCategory.PLAYER),
      MISC("Misc", "\ue009", ModuleCategory.MISC),
      AUTOBUY("Autobuy", "\ue002", (ModuleCategory)null),
      CONFIGS("Configs", "\ue006", (ModuleCategory)null),
      COSMETICS("Cosmetics", "\ue007", (ModuleCategory)null);

      private final ModuleCategory category;
      private final String icon;
      private final String label;

      private static RenderGuiHelper.View[] $values() {
         RenderGuiHelper.View[] viewValues = new RenderGuiHelper.View[8];
         viewValues[0] = COMBAT;
         viewValues[1] = MOVEMENT;
         viewValues[2] = RENDER;
         viewValues[3] = PLAYER;
         viewValues[4] = MISC;
         viewValues[5] = AUTOBUY;
         viewValues[6] = CONFIGS;
         viewValues[7] = COSMETICS;
         return viewValues;
      }

      View(String label2, String icon2, ModuleCategory category2) {
         this.label = label2;
         this.icon = icon2;
         this.category = category2;
      }
   }
}
