package net.skycore.client.module.render;

import com.mojang.blaze3d.platform.Window;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ambient.AmbientCreature;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.skycore.client.SkycoreClient;
import net.skycore.client.config.FriendManager;
import net.skycore.client.module.ClientModule;
import net.skycore.client.module.ModuleCategory;
import net.skycore.client.module.ModuleManager;
import net.skycore.client.module.misc.StreamerModeModule;
import net.skycore.client.util.BeginFrameHelper;
import net.skycore.client.util.CaptureLevelProjectionHelper;
import net.skycore.client.setting.ColorSetting;
import net.skycore.client.setting.NumberSetting;
import net.skycore.client.setting.ModeSetting;
import net.skycore.client.setting.MultiSelectSetting;
import net.skycore.client.util.ContainsRankSymbolHelper;
import net.skycore.client.util.CurrentGuiGraphicsExtractorHelper;
import net.skycore.client.util.IconStackHelper;
import net.skycore.client.util.InterBoldHelper;
import net.skycore.client.util.NeedsFallbackHelper;
import net.skycore.client.util.ReplaceLiteralHelper;
import net.skycore.client.util.ScoreboardModeHelper;
import net.skycore.client.util.TextHeightHelper;
import net.skycore.client.setting.BooleanSetting;
import net.skycore.client.util.WriteHexByteHelper;
import org.joml.Matrix3x2fStack;

public final class NameTagsModule extends ClientModule {
   private final BooleanSetting customBoxColor;
   private final NumberSetting scale;
   private static final float TEXT_SIZE = 9.0F;
   private final Map<Entity, NameTagsModule.CachedName> nameCache = new WeakHashMap<>();
   private static final float PADDING_X = 3.9F;
   private static final float LETTER_SPACING_EM = 0.01F;
   private static final long NAME_REFRESH_NANOS = 50000000L;
   private static final EquipmentSlot[] EQUIPMENT_ORDER = new EquipmentSlot[]{
      EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET, EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND
   };
   private final NumberSetting distance;
   private long cachedFriendRevision = -1L;
   private static final float GAP = 3.0F;
   private final ModeSetting itemBoxMode;
   private long frameTime;
   private final ColorSetting boxBottomColor;
   private static final float ITEM_SIZE = 12.0F;
   private final BooleanSetting equipmentGlint;
   private boolean cachedStreamerMode;
   private final ColorSetting boxTopColor;
   private static final float HEAD_OFFSET = 0.12F;
   private static final int HEALTH_VALUE_COLOR = WriteHexByteHelper.rgb(255, 85, 85);
   private static final float SCALE = 0.9F;
   private final ModeSetting boxMode;
   private boolean cachedPrivilege;
   private final BooleanSetting equipment;
   private final MultiSelectSetting targets;
   private static final int TAG_BACKGROUND = 1342177280;
   private static final int HEALTH_BRACKET_COLOR = WriteHexByteHelper.rgb(170, 170, 170);
   private static final float TAG_HEIGHT = 25.0F;
   private final BooleanSetting health;
   private final BooleanSetting privilege;
   private final IconStackHelper equipmentCache;

   private void drawEntityBox(Entity entity, float coordinate, ModeSetting modeSetting) {
      if (!modeSetting.is("Off")) {
         Minecraft instance = Minecraft.getInstance();
         int value = SkycoreClient.themes().accent();
         int computedValue = this.customBoxColor.get() ? this.boxTopColor.get() : WriteHexByteHelper.fade(Integer.MAX_VALUE, 80, value, -1);
         int computedValue2 = this.customBoxColor.get() ? this.boxBottomColor.get() : WriteHexByteHelper.fade(Integer.MAX_VALUE, 260, value, -1);
         Vec3 position = CaptureLevelProjectionHelper.interpolatedPosition(entity, coordinate);
         AABB boundingBox = entity.getBoundingBox().move(position.subtract(entity.position()));
         float pOSITIVE_INFINITY = Float.POSITIVE_INFINITY;
         float nEGATIVE_INFINITY = Float.NEGATIVE_INFINITY;
         float pOSITIVE_INFINITY2 = Float.POSITIVE_INFINITY;
         float nEGATIVE_INFINITY2 = Float.NEGATIVE_INFINITY;

         for (int value2 = 0; value2 < 8; value2++) {
            CaptureLevelProjectionHelper.ScreenPoint screenPoint = CaptureLevelProjectionHelper.projectToScreen(
               instance, new Vec3((value2 & 1) == 0 ? boundingBox.minX : boundingBox.maxX, (value2 & 2) == 0 ? boundingBox.minY : boundingBox.maxY, (value2 & 4) == 0 ? boundingBox.minZ : boundingBox.maxZ)
            );
            if (screenPoint == null) {
               return;
            }

            pOSITIVE_INFINITY = Math.min(pOSITIVE_INFINITY, screenPoint.x());
            nEGATIVE_INFINITY = Math.max(nEGATIVE_INFINITY, screenPoint.x());
            pOSITIVE_INFINITY2 = Math.min(pOSITIVE_INFINITY2, screenPoint.y());
            nEGATIVE_INFINITY2 = Math.max(nEGATIVE_INFINITY2, screenPoint.y());
         }

         if (!(nEGATIVE_INFINITY - pOSITIVE_INFINITY < 2.0F) && !(nEGATIVE_INFINITY2 - pOSITIVE_INFINITY2 < 2.0F)) {
            boolean is = modeSetting.is("Углы");
            box(pOSITIVE_INFINITY, pOSITIVE_INFINITY2, nEGATIVE_INFINITY, nEGATIVE_INFINITY2, -16777216, -16777216, 0.5F, is);
            box(pOSITIVE_INFINITY, pOSITIVE_INFINITY2, nEGATIVE_INFINITY, nEGATIVE_INFINITY2, computedValue, computedValue2, 0.0F, is);
         }
      }
   }

   private boolean hasLivingTargets() {
      if (!this.targets.isSelected("Players")) {
         if (!this.targets.isSelected("Naked")) {
            if (!this.targets.isSelected("Mobs")) {
               if (!this.targets.isSelected("Animals")) {
                  return false;
               }
            }
         }
      }

      return true;
   }

   private static int itemNameColor(ItemStack itemStack) {
      Style style = itemStack.getStyledHoverName().getStyle();
      TextColor color = style.getColor();
      if (color == null) {
         return SkycoreClient.themes().textPrimary();
      }

      int value = color.getValue();
      return WriteHexByteHelper.rgba(value >> 16 & 0xFF, value >> 8 & 0xFF, value & 0xFF, 255);
   }

   private static boolean containsRank(Component text) {
      boolean[] booleanValues = new boolean[]{false};
      text.getVisualOrderText().accept((value, style, value2) -> {
         if (isRankGlyph(value2, ReplaceLiteralHelper.replaceCodePoint(value2))) {
            booleanValues[0] = true;
            return false;
         } else {
            return true;
         }
      });
      return booleanValues[0];
   }

   private boolean hasVisibleBoxes() {
      if (this.hasLivingTargets()) {
         if (!this.boxMode.is("Off")) {
            return true;
         }
      }

      if (this.targets.isSelected("Items")) {
         if (!this.itemBoxMode.is("Off")) {
            return true;
         }
      }

      return false;
   }

   private static boolean outsideScreen(Minecraft minecraft, float width, float height, float value, float value2) {
      if (!(width + value < -4.0F) && !(height + value2 < -4.0F)) {
         Window window = minecraft.getWindow();
         if (!(width > window.getGuiScaledWidth() + 4.0F)) {
            Window window2 = minecraft.getWindow();
            if (!(height > window2.getGuiScaledHeight() + 4.0F)) {
               return false;
            }
         }
      }

      return true;
   }

   private static int resolveStyleColor(Style style, int result) {
      TextColor color = style.getColor();
      if (color == null) {
         return result;
      }

      int value = result >> 24 & 0xFF;
      if (value == 0) {
         value = 255;
      }

      int value2 = color.getValue();
      return value << 24 | value2 & 16777215;
   }

   public NameTagsModule() {
      super("Name Tags", "Draws nametags and boxes for selected entities", ModuleCategory.RENDER);
      this.scale = this.add(new NumberSetting("Scale", "", 1.0, 0.7, 1.3, 0.05, "x"));
      this.targets = this.add(new MultiSelectSetting("Targets", List.of("Items", "Naked", "Mobs", "Animals", "Players"), Set.of("Naked", "Players")));
      this.boxMode = this.add(new ModeSetting("Box", "Углы", "Off", "Углы", "Квадрат"));
      this.itemBoxMode = this.add(new ModeSetting("Item Box", "Квадрат", "Off", "Углы", "Квадрат"));
      this.customBoxColor = this.add(new BooleanSetting("Custom Box Color", false));
      this.boxTopColor = this.add(new ColorSetting("Top Box Color", -1, false));
      this.boxBottomColor = this.add(new ColorSetting("Bottom Box Color", -1, false));
      this.privilege = this.add(new BooleanSetting("Privilege", true));
      this.health = this.add(new BooleanSetting("Health", true));
      this.equipment = this.add(new BooleanSetting("Equipment", true));
      this.equipmentGlint = this.add(new BooleanSetting("Equipment Glint", false));
      this.equipmentCache = new IconStackHelper();
      this.distance = this.add(new NumberSetting("Distance", "", 64.0, 16.0, 128.0, 8.0, " blocks"));
      this.itemBoxMode.visibleWhen(() -> {
         return this.targets.isSelected("Items");
      });
      this.boxMode.visibleWhen(this::hasLivingTargets);
      this.customBoxColor.visibleWhen(this::hasVisibleBoxes);
      this.boxTopColor.visibleWhen(() -> {
         if (this.hasVisibleBoxes()) {
            if (this.customBoxColor.get()) {
               return true;
            }
         }

         return false;
      });
      this.boxBottomColor.visibleWhen(() -> {
         return this.hasVisibleBoxes() && this.customBoxColor.get();
      });
      this.privilege.visibleWhen(() -> {
         if (!this.targets.isSelected("Players")) {
            if (!this.targets.isSelected("Naked")) {
               return false;
            }
         }

         return true;
      });
      this.health.visibleWhen(this::hasLivingTargets);
      this.equipment.visibleWhen(this::hasLivingTargets);
      this.equipmentGlint.visibleWhen(() -> {
         if (this.hasLivingTargets()) {
            Boolean get = this.equipment.get();
            if (get) {
               return true;
            }
         }

         return false;
      });
   }

   private Component applyNameProtect(LivingEntity entity, Component text) {
      if (entity instanceof Player player && StreamerModeModule.INSTANCE.isEnabled()) {
         Minecraft instance = Minecraft.getInstance();
         String gameProfile = player.getGameProfile().name();
         if (player == instance.player) {
            return Component.literal(StreamerModeModule.getCustomName());
         }

         if (!FriendManager.INSTANCE.isFriend(gameProfile)) {
            return text;
         }

         String display = SkycoreClient.customNames().getDisplay(gameProfile);
         if (display != null && !display.isEmpty()) {
            return Component.empty().append(ReplaceLiteralHelper.replaceLiteral(text, gameProfile, display));
         }

         String customName = StreamerModeModule.getCustomName(gameProfile);
         return !gameProfile.equals(customName) ? Component.empty().append(ReplaceLiteralHelper.replaceLiteral(text, gameProfile, customName)) : Component.empty().append(text);
      } else {
         return text;
      }
   }

   private static boolean shouldSkipLeading(int value, String text, boolean condition) {
      if (text != null && text.isEmpty()) {
         return true;
      } else if (Character.isWhitespace(value)) {
         return true;
      } else if (condition) {
         return !isRankGlyph(value, text);
      } else {
         return false;
      }
   }

   private void drawLivingTag(GuiGraphicsExtractor guiGraphicsExtractor, LivingEntity entity, float coordinate, float coordinate2) {
      Minecraft instance = Minecraft.getInstance();
      Vec3 bbHeight = CaptureLevelProjectionHelper.interpolatedPosition(entity, coordinate).add(0.0, entity.getBbHeight() + 0.12F, 0.0);
      CaptureLevelProjectionHelper.ScreenPoint screenPoint = CaptureLevelProjectionHelper.projectToScreen(instance, bbHeight);
      if (screenPoint != null) {
         TextHeightHelper textHeightHelper = InterBoldHelper.interMedium();
         float height = 9.0F * coordinate2 + 2.0F;
         float computedValue = height * 0.01F;
         float computedValue2 = 12.0F * coordinate2;
         float computedValue3 = 3.0F * coordinate2;
         float width = 3.9F * coordinate2;
         NeedsFallbackHelper needsFallbackHelper = this.cachedName(entity, textHeightHelper, height, SkycoreClient.themes().textPrimary());
         String labelHealth = this.health.get() ? Integer.toString((int)Math.ceil(ScoreboardModeHelper.getLabelHealth(entity))) : null;
         float value = labelHealth == null ? 0.0F : measure(textHeightHelper, "[", height, computedValue);
         float value2 = labelHealth == null ? 0.0F : measure(textHeightHelper, labelHealth, height, computedValue);
         float computedValue4 = width + needsFallbackHelper.width() + width;
         if (labelHealth != null) {
            computedValue4 += computedValue3 + value + value2 + measure(textHeightHelper, "]", height, computedValue);
         }

         float computedValue5 = 25.0F * coordinate2;
         float x = screenPoint.x() - computedValue4 * 0.5F;
         float y = screenPoint.y() - computedValue5;
         float computedValue6 = 1.5F * coordinate2;
         float computedValue7 = this.equipment.get() ? EQUIPMENT_ORDER.length * computedValue2 + (EQUIPMENT_ORDER.length - 1) * computedValue6 : 0.0F;
         float maximum = Math.max(computedValue4, computedValue7);
         float computedValue8 = this.equipment.get() ? y - computedValue2 - 2.0F * coordinate2 : y;
         if (!outsideScreen(instance, screenPoint.x() - maximum * 0.5F, computedValue8, maximum, screenPoint.y() - computedValue8)) {
            float height2 = y + computedValue5 * 0.5F;
            float color = height2 - textHeightHelper.textHeight(height) * 0.5F;
            BeginFrameHelper.rect(x, y, computedValue4, computedValue5)
               .color(entity instanceof Player player && FriendManager.INSTANCE.isFriend(player.getGameProfile().name()) ? -2144102093 : 1342177280)
               .draw();
            float color2 = x + width;
            BeginFrameHelper.coloredText(color2, color, needsFallbackHelper);
            color2 += needsFallbackHelper.width();
            if (labelHealth != null) {
               color2 += computedValue3;
               drawText(color2, color, height, computedValue, "[", HEALTH_BRACKET_COLOR);
               color2 += value;
               drawText(color2, color, height, computedValue, labelHealth, HEALTH_VALUE_COLOR);
               color2 += value2;
               drawText(color2, color, height, computedValue, "]", HEALTH_BRACKET_COLOR);
            }

            int value3 = this.equipment.get() ? equipmentCount(entity) : 0;
            if (value3 > 0) {
               float computedValue9 = value3 * computedValue2 + (value3 - 1) * computedValue6;
               float x2 = screenPoint.x() - computedValue9 * 0.5F;
               float computedValue10 = y - computedValue2 - 2.0F * coordinate2;
               this.drawItems(guiGraphicsExtractor, entity, x2, computedValue10, computedValue2, computedValue6);
            }
         }
      }
   }

   private void drawItems(GuiGraphicsExtractor guiGraphicsExtractor, LivingEntity entity, float coordinate, float coordinate2, float coordinate3, float coordinate4) {
      Minecraft instance = Minecraft.getInstance();
      BeginFrameHelper.flush();
      Matrix3x2fStack matrix3x2fStack = guiGraphicsExtractor.pose();
      float guiScale = instance.getWindow().getGuiScale();
      float computedValue = coordinate3 / 16.0F;
      float computedValue2 = Math.round(coordinate2 * guiScale) / guiScale;
      float value = coordinate;

      for (EquipmentSlot equipmentSlot : EQUIPMENT_ORDER) {
         ItemStack itemBySlot = entity.getItemBySlot(equipmentSlot);
         if (!itemBySlot.isEmpty()) {
            float computedValue3 = Math.round(value * guiScale) / guiScale;
            matrix3x2fStack.pushMatrix();
            matrix3x2fStack.translate(computedValue3, computedValue2);
            matrix3x2fStack.scale(computedValue);
            this.equipmentCache.draw(guiGraphicsExtractor, itemBySlot, this.equipmentGlint.get());
            matrix3x2fStack.popMatrix();
            if (itemBySlot.isBarVisible()) {
               float computedValue4 = computedValue3 + coordinate3 * 0.125F;
               float computedValue5 = computedValue2 + coordinate3 * 0.8125F;
               float width = coordinate3 * 0.8125F;
               float maximum = Math.max(1.0F, coordinate3 * 0.125F);
               float barWidth = width * Math.max(0, Math.min(13, itemBySlot.getBarWidth())) / 13.0F;
               BeginFrameHelper.rect(computedValue4, computedValue5, width, maximum).color(-16777216).draw();
               BeginFrameHelper.rect(computedValue4, computedValue5, barWidth, Math.max(0.75F, maximum * 0.5F)).color(0xFF000000 | itemBySlot.getBarColor()).draw();
            }

            value += coordinate3 + coordinate4;
         }
      }
   }

   public void render(GuiRenderState guiRenderState, DeltaTracker deltaTracker) {
      if (this.isEnabled()) {
         Minecraft instance = Minecraft.getInstance();
         if (instance.level != null && instance.player != null && instance.getWindow() != null) {
            this.frameTime = System.nanoTime();
            if (this.cachedPrivilege != this.privilege.get()
               || this.cachedStreamerMode != StreamerModeModule.INSTANCE.isEnabled()
               || this.cachedFriendRevision != FriendManager.INSTANCE.revision()) {
               this.nameCache.clear();
               this.cachedPrivilege = this.privilege.get();
               this.cachedStreamerMode = StreamerModeModule.INSTANCE.isEnabled();
               this.cachedFriendRevision = FriendManager.INSTANCE.revision();
            }

            GuiGraphicsExtractor guiGraphicsExtractor = new GuiGraphicsExtractor(
               instance,
               guiRenderState,
               (int)Math.round(instance.mouseHandler.getScaledXPos(instance.getWindow())),
               (int)Math.round(instance.mouseHandler.getScaledYPos(instance.getWindow()))
            );
            CurrentGuiGraphicsExtractorHelper.enter2D(guiGraphicsExtractor, deltaTracker);
            BeginFrameHelper.beginFrame();

            try {
               float gameTimeDeltaPartialTick = deltaTracker.getGameTimeDeltaPartialTick(false);
               float window = (float)(0.9F * this.scale.get() / instance.getWindow().getGuiScale());
               double distance2 = this.distance.get() * this.distance.get();

               for (Entity entity : instance.level.entitiesForRendering()) {
                  if (!entity.isRemoved() && !(instance.player.distanceToSqr(entity) > distance2)) {
                     if (entity instanceof ItemEntity itemEntity) {
                        if (this.targets.isSelected("Items")) {
                           this.drawEntityBox(itemEntity, gameTimeDeltaPartialTick, this.itemBoxMode);
                           this.drawItemTag(itemEntity, gameTimeDeltaPartialTick, window);
                        }
                     } else if (entity instanceof LivingEntity entity2
                        && entity2.isAlive()
                        && !(entity2 instanceof ArmorStand)
                        && this.isLivingTargetSelected(entity2)
                        && (entity2 != instance.player || !instance.options.getCameraType().isFirstPerson())) {
                        this.drawEntityBox(entity2, gameTimeDeltaPartialTick, this.boxMode);
                        this.drawLivingTag(guiGraphicsExtractor, entity2, gameTimeDeltaPartialTick, window);
                     }
                  }
               }

               BeginFrameHelper.flush();
            } finally {
               CurrentGuiGraphicsExtractorHelper.exit2D();
            }
         } else {
            this.nameCache.clear();
            this.equipmentCache.clear();
         }
      }
   }

   private NeedsFallbackHelper cachedName(Entity entity, TextHeightHelper textHeightHelper, float value, int color) {
      NameTagsModule.CachedName cachedName = this.nameCache.get(entity);
      if (cachedName != null && cachedName.layout.font() == textHeightHelper && cachedName.layout.size() == value && cachedName.color == color && this.frameTime - cachedName.checkedAt < 50000000L) {
         return cachedName.layout;
      }

      Component text;
      if (entity instanceof LivingEntity entity2) {
         text = this.buildNameComponent(entity2);
      } else {
         ItemStack item = ((ItemEntity)entity).getItem();
         text = ReplaceLiteralHelper.replaceSymbols(Component.empty().append(item.getHoverName()).withStyle(item.getRarity().color()));
      }

      if (cachedName != null && cachedName.layout.font() == textHeightHelper && cachedName.layout.size() == value && cachedName.color == color && cachedName.text.equals(text)) {
         cachedName.checkedAt = this.frameTime;
         return cachedName.layout;
      }

      NeedsFallbackHelper needsFallbackHelper = layoutComponent(textHeightHelper, text, value, color);
      if (cachedName == null && this.nameCache.size() >= 1024) {
         Iterator iterator = this.nameCache.keySet().iterator();
         if (iterator.hasNext()) {
            iterator.next();
            iterator.remove();
         }
      }

      this.nameCache.put(entity, new NameTagsModule.CachedName(text.copy(), needsFallbackHelper, color, this.frameTime));
      return needsFallbackHelper;
   }

   private Component buildNameComponent(LivingEntity entity) {
      Component text = ContainsRankSymbolHelper.resolveEntityDisplayName(entity);
      if (!this.privilege.get()) {
         text = ReplaceLiteralHelper.stripRankSymbols(text);
      }

      return this.applyNameProtect(entity, text);
   }

   private boolean isLivingTargetSelected(LivingEntity entity) {
      if (entity instanceof Player player) {
         return this.isPlayerTargetSelected(player);
      } else if (!(entity instanceof Animal) && !(entity instanceof AmbientCreature)) {
         return this.targets.isSelected("Mobs");
      } else {
         return this.targets.isSelected("Animals");
      }
   }

   private static float measure(TextHeightHelper textHeightHelper, String text, float width, float width2) {
      TextHeightHelper.Paragraph paragraph = textHeightHelper.shape(text);
      return paragraph.maxWidth(width, width2);
   }

   private static NeedsFallbackHelper layoutComponent(TextHeightHelper textHeightHelper, Component text, float value, int color) {
      NeedsFallbackHelper.Builder builder = new NeedsFallbackHelper.Builder(textHeightHelper, value);
      boolean containsRank = containsRank(text);
      boolean[] booleanValues = new boolean[]{false};
      float computedValue = (color >>> 24) / 255.0F;
      float color2 = computedValue <= 0.0F ? 1.0F : computedValue;
      FormattedCharSequence visualOrderText = text.getVisualOrderText();
      visualOrderText.accept((value2, style, color3) -> {
         if (!ReplaceLiteralHelper.isDisplayChar(color3)) {
            return true;
         }

         String text2 = ReplaceLiteralHelper.replaceCodePoint(color3);
         if (!booleanValues[0]) {
            if (shouldSkipLeading(color3, text2, containsRank)) {
               return true;
            }

            booleanValues[0] = true;
         }

         int color4 = resolveStyleColor(style, color);
         if (text2 == null) {
            builder.append(color3, color4);
         } else {
            for (int value3 = 0; value3 < text2.length(); value3++) {
               builder.append(text2.charAt(value3), ReplaceLiteralHelper.getGradientColorForReplacement(color3, value3, text2.length(), color2, color4));
            }
         }

         return true;
      });
      return builder.build();
   }

   private boolean isPlayerTargetSelected(Player player) {
      boolean condition;
      if (player.getArmorValue() == 0) {
         condition = this.targets.isSelected("Naked");
      } else {
         condition = this.targets.isSelected("Players");
      }

      return condition;
   }

   private static void strip(float value, float value2, float value3, float value4, int color, int color2, float value5) {
      BeginFrameHelper.rect(value - value5, value2 - value5, value3 + value5 * 2.0F, value4 + value5 * 2.0F).color(color, color, color2, color2).draw();
   }

   @Override
   protected void onDisable() {
      this.nameCache.clear();
      this.equipmentCache.clear();
   }

   public static boolean shouldHideVanillaTag(Player player) {
      ModuleManager moduleManager = SkycoreClient.modules();
      NameTagsModule nameTagsModule = moduleManager.get(NameTagsModule.class).orElse(null);
      return nameTagsModule != null && nameTagsModule.isEnabled() && nameTagsModule.isPlayerTargetSelected(player);
   }

   private static int gradient(int value, int value2, float value3) {
      return (int)((value >>> 24) + ((value2 >>> 24) - (value >>> 24)) * value3) << 24
         | (int)((value >> 16 & 0xFF) + ((value2 >> 16 & 0xFF) - (value >> 16 & 0xFF)) * value3) << 16
         | (int)((value >> 8 & 0xFF) + ((value2 >> 8 & 0xFF) - (value >> 8 & 0xFF)) * value3) << 8
         | (int)((value & 0xFF) + ((value2 & 0xFF) - (value & 0xFF)) * value3);
   }

   private static void drawText(float coordinate, float coordinate2, float coordinate3, float coordinate4, String text, int color) {
      BeginFrameHelper.TextBuilder textBuilder = BeginFrameHelper.text(coordinate, coordinate2, coordinate3, text);
      BeginFrameHelper.TextBuilder textBuilder2 = textBuilder.weight(500).letterSpacing(coordinate4);
      textBuilder2.color(color).draw();
   }

   private static void box(float value, float value2, float value3, float value4, int value5, int value6, float value7, boolean condition) {
      float computedValue = value3 - value;
      float computedValue2 = value4 - value2;
      float value8 = 0.5F;
      if (!condition) {
         strip(value, value2, computedValue, value8, value5, value5, value7);
         strip(value, value4 - value8, computedValue, value8, value6, value6, value7);
         strip(value, value2, value8, computedValue2, value5, value6, value7);
         strip(value3 - value8, value2, value8, computedValue2, value5, value6, value7);
      } else {
         float computedValue3 = computedValue * 0.25F;
         float computedValue4 = computedValue2 * 0.25F;
         int value9 = gradient(value5, value6, 0.25F);
         int value10 = gradient(value5, value6, 0.75F);
         strip(value, value2, computedValue3, value8, value5, value5, value7);
         strip(value3 - computedValue3, value2, computedValue3, value8, value5, value5, value7);
         strip(value, value2, value8, computedValue4, value5, value9, value7);
         strip(value3 - value8, value2, value8, computedValue4, value5, value9, value7);
         strip(value, value4 - value8, computedValue3, value8, value6, value6, value7);
         strip(value3 - computedValue3, value4 - value8, computedValue3, value8, value6, value6, value7);
         strip(value, value4 - computedValue4, value8, computedValue4, value10, value6, value7);
         strip(value3 - value8, value4 - computedValue4, value8, computedValue4, value10, value6, value7);
      }
   }

   private static int equipmentCount(LivingEntity entity) {
      int value = 0;

      for (EquipmentSlot equipmentSlot : EQUIPMENT_ORDER) {
         ItemStack itemBySlot = entity.getItemBySlot(equipmentSlot);
         if (!itemBySlot.isEmpty()) {
            value++;
         }
      }

      return value;
   }

   private void drawItemTag(ItemEntity itemEntity, float coordinate, float coordinate2) {
      ItemStack item = itemEntity.getItem();
      if (!item.isEmpty()) {
         Minecraft instance = Minecraft.getInstance();
         Vec3 bbHeight = CaptureLevelProjectionHelper.interpolatedPosition(itemEntity, coordinate).add(0.0, itemEntity.getBbHeight() + 0.12, 0.0);
         CaptureLevelProjectionHelper.ScreenPoint screenPoint = CaptureLevelProjectionHelper.projectToScreen(instance, bbHeight);
         if (screenPoint != null) {
            TextHeightHelper textHeightHelper = InterBoldHelper.interMedium();
            float color = 9.0F * coordinate2 + 2.0F;
            float computedValue = color * 0.01F;
            float computedValue2 = 3.0F * coordinate2;
            float width = 3.9F * coordinate2;
            NeedsFallbackHelper needsFallbackHelper = this.cachedName(itemEntity, textHeightHelper, color, itemNameColor(item));
            int count = item.getCount() > 1 ? 1 : 0;
            String count2 = count != 0 ? "x" + item.getCount() : "";
            float computedValue3 = width + needsFallbackHelper.width() + (count != 0 ? computedValue2 + measure(textHeightHelper, count2, color, computedValue) : 0.0F) + width;
            float computedValue4 = 25.0F * coordinate2;
            float x = screenPoint.x() - computedValue3 * 0.5F;
            float y = screenPoint.y() - computedValue4;
            if (!outsideScreen(instance, x, y, computedValue3, computedValue4)) {
               float height = y + computedValue4 * 0.5F;
               float color2 = height - textHeightHelper.textHeight(color) * 0.5F;
               BeginFrameHelper.rect(x, y, computedValue3, computedValue4).color(1342177280).draw();
               float color3 = x + width;
               BeginFrameHelper.coloredText(color3, color2, needsFallbackHelper);
               color3 += needsFallbackHelper.width();
               if (count != 0) {
                  color3 += computedValue2;
                  drawText(color3, color2, color, computedValue, count2, SkycoreClient.themes().textSecondary());
               }
            }
         }
      }
   }

   private static boolean isRankGlyph(int value, String text) {
      if (!ReplaceLiteralHelper.isRankCodePoint(value)) {
         if (text != null) {
            if (text.length() > 1) {
               return true;
            }
         }

         return false;
      } else {
         return true;
      }
   }

   static final class CachedName {
      private final int color;
      private final NeedsFallbackHelper layout;
      private final Component text;
      private long checkedAt;

      private CachedName(Component text2, NeedsFallbackHelper layout2, int color2, long checkedAt2) {
         this.text = text2;
         this.layout = layout2;
         this.color = color2;
         this.checkedAt = checkedAt2;
      }
   }
}
