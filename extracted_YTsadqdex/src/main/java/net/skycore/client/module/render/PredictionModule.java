package net.skycore.client.module.render;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.entity.projectile.arrow.ThrownTrident;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEnderpearl;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import net.skycore.client.SkycoreClient;
import net.skycore.client.mixin.AbstractArrowIsInGroundAccessor;
import net.skycore.client.module.ClientModule;
import net.skycore.client.module.ModuleCategory;
import net.skycore.client.util.AddQuadHelper;
import net.skycore.client.util.BeginFrameHelper;
import net.skycore.client.util.CaptureLevelProjectionHelper;
import net.skycore.client.setting.MultiSelectSetting;
import net.skycore.client.util.CurrentGuiGraphicsExtractorHelper;
import net.skycore.client.util.IconStackHelper;
import net.skycore.client.util.InterBoldHelper;
import net.skycore.client.util.TextHeightHelper;
import org.joml.Matrix3x2fStack;

public final class PredictionModule extends ClientModule {
   private static final String PEARL = "Эндер Пёрл";
   private static final String TRIDENT = "Трезубец";
   private static final float LETTER_SPACING_EM = 0.01F;
   private static final int TAG_BACKGROUND = 1342177280;
   private static final float ITEM_SIZE = 12.0F;
   private static final DecimalFormat TIME_FORMAT = new DecimalFormat("0.0", new DecimalFormatSymbols(Locale.US));
   private static final float TAG_HEIGHT = 25.0F;
   private static final int MAX_SIM_TICKS = 300;
   private static final float TEXT_SIZE = 9.0F;
   private static final float PADDING_X = 3.9F;
   public static final PredictionModule INSTANCE = new PredictionModule();
   private static final float GAP = 3.0F;
   private static final String ARROW = "Стрела";
   private final MultiSelectSetting projectiles = this.add(
      new MultiSelectSetting("Снаряды", List.of("Эндер Пёрл", "Стрела", "Трезубец"), Set.of("Эндер Пёрл", "Стрела", "Трезубец"))
   );
   private static final float SCALE = 0.9F;
   private final IconStackHelper itemCache = new IconStackHelper();

   private PredictionModule.PredictionResult simulate(Minecraft minecraft, Entity entity) {
      Vec3 position = entity.position();
      Vec3 deltaMovement = entity.getDeltaMovement();
      Vec3 position2 = position;
      int value = 0;

      for (int value2 = 0; value2 <= 300; value2++) {
         value++;
         Vec3 position3 = position;
         position2 = position;
         position = position.add(deltaMovement);
         deltaMovement = this.updatePearlMotion(minecraft, entity, deltaMovement, position);
         if (this.shouldEntityHit(minecraft, entity, position, position3) || position.y <= 0.0) {
            break;
         }
      }

      return new PredictionModule.PredictionResult(position2, value);
   }

   private PredictionModule() {
      super("Prediction", "Предугадывает куда и за сколько времени упадёт предмет", ModuleCategory.RENDER);
   }

   private boolean noMove(Entity entity) {
      if (entity.yo == entity.getY()) {
         if (entity.xo == entity.getX()) {
            if (entity.zo == entity.getZ()) {
               return false;
            }
         }
      }

      return true;
   }

   private PredictionModule.PredictionResult simulateHeldProjectile(Minecraft minecraft, double angle, double value, double value2) {
      Vec3 eyePosition = minecraft.player.getEyePosition().add(0.0, -0.1, 0.0);
      Vec3 knownMovement = minecraft.player.getKnownMovement();
      Vec3 lookAngle = minecraft.player.getLookAngle().normalize().scale(angle).add(knownMovement.x, minecraft.player.onGround() ? 0.0 : knownMovement.y, knownMovement.z);
      Vec3 position = eyePosition;
      int value3 = 0;

      for (int value4 = 0; value4 <= 300; value4++) {
         value3++;
         Vec3 position2 = eyePosition;
         Vec3 position3 = eyePosition.add(lookAngle);
         BlockHitResult blockHitResult = minecraft.level.clip(new ClipContext(position2, position3, Block.COLLIDER, Fluid.NONE, minecraft.player));
         if (blockHitResult.getType() == Type.BLOCK) {
            return new PredictionModule.PredictionResult(blockHitResult.getLocation(), value3);
         }

         eyePosition = position3;
         position = eyePosition;
         boolean is = minecraft.level.getBlockState(BlockPos.containing(eyePosition)).is(Blocks.WATER);
         lookAngle = lookAngle.scale(is ? value2 : 0.99).add(0.0, -value, 0.0);
         if (eyePosition.y <= 0.0) {
            break;
         }
      }

      return new PredictionModule.PredictionResult(position, value3);
   }

   @Override
   protected void onDisable() {
      this.itemCache.clear();
   }

   public void renderWorld() {
      if (this.isEnabled()) {
         Minecraft instance = Minecraft.getInstance();
         if (instance.level != null && instance.player != null && instance.gameRenderer != null) {
            int index = SkycoreClient.themes().accent();
            AddQuadHelper.beginFrame();

            for (Entity entity : instance.level.entitiesForRendering()) {
               if (this.validEntity(entity)
                  && this.noMove(entity)
                  && !(entity instanceof AbstractArrow abstractArrow && ((AbstractArrowIsInGroundAccessor)abstractArrow).skycore$isInGround())) {
                  this.renderLine(instance, entity, index);
               }
            }

            AddQuadHelper.endFrame();
         }
      }
   }

   private Vec3 updatePearlMotion(Minecraft minecraft, Entity entity, Vec3 position, Vec3 position2) {
      int blockState = !entity.isInWater() && !minecraft.level.getBlockState(BlockPos.containing(position2)).is(Blocks.WATER) ? 0 : 1;
      Vec3 position3;
      if (blockState != 0 && !(entity instanceof ThrownTrident)) {
         float value = entity instanceof ThrownEnderpearl ? 0.8F : 0.6F;
         position3 = position.scale(value);
      } else {
         position3 = position.scale(0.99);
      }

      if (!entity.isNoGravity()) {
         position3 = position3.add(0.0, entity instanceof ThrownEnderpearl ? -0.03 : -0.05, 0.0);
      }

      return position3;
   }

   private static boolean containsItem(Item item, Item... itemValues) {

      for (Item item2 : itemValues) {
         if (item == item2) {
            return true;
         }
      }

      return false;
   }

   private void renderLine(Minecraft minecraft, Entity entity, int index) {
      Vec3 position = entity.position();
      Vec3 deltaMovement = entity.getDeltaMovement();

      for (int value = 0; value <= 300; value++) {
         Vec3 position2 = position;
         position = position.add(deltaMovement);
         deltaMovement = this.updatePearlMotion(minecraft, entity, deltaMovement, position2);
         if (this.shouldEntityHit(minecraft, entity, position, position2) || position.y <= 0.0) {
            break;
         }

         AddQuadHelper.addScreenSpaceLine(position2, position, index);
      }
   }

   private ItemStack heldItem(Minecraft minecraft, Item... itemValues) {
      ItemStack mainHandItem = minecraft.player.getMainHandItem();
      if (!mainHandItem.isEmpty()) {
         Item item = mainHandItem.getItem();
         if (containsItem(item, itemValues)) {
            return mainHandItem;
         }
      }

      ItemStack offhandItem = minecraft.player.getOffhandItem();
      if (!offhandItem.isEmpty()) {
         Item item2 = offhandItem.getItem();
         if (containsItem(item2, itemValues)) {
            return offhandItem;
         }
      }

      return ItemStack.EMPTY;
   }

   private boolean validEntity(Entity entity) {
      if (entity instanceof ThrownEnderpearl) {
         return this.projectiles.isSelected("Эндер Пёрл");
      } else if (entity instanceof Arrow) {
         return this.projectiles.isSelected("Стрела");
      } else if (entity instanceof ThrownTrident) {
         return this.projectiles.isSelected("Трезубец");
      } else {
         return false;
      }
   }

   private void drawPreviewTag(
      GuiGraphicsExtractor guiGraphicsExtractor,
      Minecraft minecraft,
      ItemStack itemStack,
      PredictionModule.PredictionResult predictionResult,
      TextHeightHelper textHeightHelper,
      float width,
      float width2,
      float coordinate,
      float coordinate2,
      float coordinate3,
      float coordinate4,
      float coordinate5
   ) {
      CaptureLevelProjectionHelper.ScreenPoint screenPoint = CaptureLevelProjectionHelper.projectToScreen(minecraft, predictionResult.landing);
      if (screenPoint != null) {
         String string = itemStack.getHoverName().getString();
         String text = TIME_FORMAT.format(predictionResult.ticks * 0.05F) + " сек.";
         float value = textHeightHelper.shape(string).maxWidth(width, width2);
         float value2 = textHeightHelper.shape(text).maxWidth(width, width2);
         float computedValue = coordinate4 + coordinate2 + coordinate3 + value + coordinate3 + value2 + coordinate4;
         float x = screenPoint.x() - computedValue * 0.5F;
         float y = screenPoint.y() - coordinate5;
         float computedValue2 = y + coordinate5 * 0.5F;
         float computedValue3 = computedValue2 - coordinate * 0.5F;
         BeginFrameHelper.rect(x, y, computedValue, coordinate5).color(1342177280).draw();
         float computedValue4 = x + coordinate4;
         this.drawItem(guiGraphicsExtractor, itemStack, computedValue4, computedValue2 - coordinate2 * 0.5F, coordinate2);
         computedValue4 += coordinate2 + coordinate3;
         BeginFrameHelper.text(computedValue4, computedValue3, width, string).font(textHeightHelper).weight(500).letterSpacing(width2).color(SkycoreClient.themes().textPrimary()).draw();
         computedValue4 += value + coordinate3;
         BeginFrameHelper.text(computedValue4, computedValue3, width, text).font(textHeightHelper).weight(500).letterSpacing(width2).color(SkycoreClient.themes().textSecondary()).draw();
      }
   }

   private static Item itemOf(Entity entity) {
      if (entity instanceof ThrownEnderpearl) {
         return Items.ENDER_PEARL;
      } else {
         return entity instanceof Arrow ? Items.ARROW : Items.TRIDENT;
      }
   }

   private void drawItem(GuiGraphicsExtractor guiGraphicsExtractor, ItemStack itemStack, float coordinate, float coordinate2, float coordinate3) {
      Minecraft instance = Minecraft.getInstance();
      BeginFrameHelper.flush();
      Matrix3x2fStack matrix3x2fStack = guiGraphicsExtractor.pose();
      float guiScale = instance.getWindow().getGuiScale();
      float computedValue = coordinate3 / 16.0F;
      float intermediate = coordinate * guiScale;
      float computedValue2 = Math.round(intermediate) / guiScale;
      intermediate = coordinate2 * guiScale;
      float computedValue3 = Math.round(intermediate) / guiScale;
      matrix3x2fStack.pushMatrix();
      matrix3x2fStack.translate(computedValue2, computedValue3);
      matrix3x2fStack.scale(computedValue);
      this.itemCache.draw(guiGraphicsExtractor, itemStack, true);
      matrix3x2fStack.popMatrix();
   }

   private boolean shouldEntityHit(Minecraft minecraft, Entity entity, Vec3 position, Vec3 position2) {
      BlockHitResult blockHitResult = minecraft.level.clip(new ClipContext(position2, position, Block.COLLIDER, Fluid.NONE, entity));
      Type type = blockHitResult.getType();
      return type == Type.BLOCK;
   }

   public void render(GuiRenderState guiRenderState, DeltaTracker deltaTracker) {
      if (this.isEnabled()) {
         Minecraft instance = Minecraft.getInstance();
         if (instance.level != null && instance.player != null && instance.getWindow() != null) {
            GuiGraphicsExtractor guiGraphicsExtractor = new GuiGraphicsExtractor(
               instance,
               guiRenderState,
               (int)Math.round(instance.mouseHandler.getScaledXPos(instance.getWindow())),
               (int)Math.round(instance.mouseHandler.getScaledYPos(instance.getWindow()))
            );
            CurrentGuiGraphicsExtractorHelper.enter2D(guiGraphicsExtractor, deltaTracker);
            BeginFrameHelper.beginFrame();

            try {
               TextHeightHelper textHeightHelper = InterBoldHelper.interMedium();
               float guiScale = 0.9F / instance.getWindow().getGuiScale();
               float width = 9.0F * guiScale + 2.0F;
               float width2 = width * 0.01F;
               float value = textHeightHelper.textHeight(width);
               float computedValue = 12.0F * guiScale;
               float computedValue2 = 3.0F * guiScale;
               float computedValue3 = 3.9F * guiScale;
               float computedValue4 = 25.0F * guiScale;

               for (Entity entity : instance.level.entitiesForRendering()) {
                  if (this.validEntity(entity)
                     && this.noMove(entity)
                     && !(entity instanceof AbstractArrow abstractArrow && ((AbstractArrowIsInGroundAccessor)abstractArrow).skycore$isInGround())) {
                     PredictionModule.PredictionResult predictionResult = this.simulate(instance, entity);
                     CaptureLevelProjectionHelper.ScreenPoint screenPoint = CaptureLevelProjectionHelper.projectToScreen(instance, predictionResult.landing);
                     if (screenPoint != null) {
                        ItemStack itemStack = new ItemStack(itemOf(entity));
                        String string = itemStack.getHoverName().getString();
                        String text = TIME_FORMAT.format(predictionResult.ticks * 0.05F) + " сек.";
                        float value2 = textHeightHelper.shape(string).maxWidth(width, width2);
                        float value3 = textHeightHelper.shape(text).maxWidth(width, width2);
                        float computedValue5 = computedValue3 + computedValue + computedValue2 + value2 + computedValue2 + value3 + computedValue3;
                        float x = screenPoint.x() - computedValue5 * 0.5F;
                        float y = screenPoint.y() - computedValue4;
                        float computedValue6 = y + computedValue4 * 0.5F;
                        float computedValue7 = computedValue6 - value * 0.5F;
                        BeginFrameHelper.rect(x, y, computedValue5, computedValue4).color(1342177280).draw();
                        float computedValue8 = x + computedValue3;
                        this.drawItem(guiGraphicsExtractor, itemStack, computedValue8, computedValue6 - computedValue * 0.5F, computedValue);
                        computedValue8 += computedValue + computedValue2;
                        BeginFrameHelper.text(computedValue8, computedValue7, width, string)
                           .font(textHeightHelper)
                           .weight(500)
                           .letterSpacing(width2)
                           .color(SkycoreClient.themes().textPrimary())
                           .draw();
                        computedValue8 += value2 + computedValue2;
                        BeginFrameHelper.text(computedValue8, computedValue7, width, text)
                           .font(textHeightHelper)
                           .weight(500)
                           .letterSpacing(width2)
                           .color(SkycoreClient.themes().textSecondary())
                           .draw();
                     }
                  }
               }

               if (this.projectiles.isSelected("Эндер Пёрл")) {
                  ItemStack itemStack2 = this.heldItem(instance, Items.ENDER_PEARL);
                  if (!itemStack2.isEmpty()) {
                     this.drawPreviewTag(
                        guiGraphicsExtractor, instance, itemStack2, this.simulateHeldProjectile(instance, 1.5, 0.03, 0.8), textHeightHelper, width, width2, value, computedValue, computedValue2, computedValue3, computedValue4
                     );
                  }
               }

               if (this.projectiles.isSelected("Трезубец")) {
                  ItemStack itemStack3 = this.heldItem(instance, Items.TRIDENT);
                  if (!itemStack3.isEmpty()) {
                     this.drawPreviewTag(
                        guiGraphicsExtractor, instance, itemStack3, this.simulateHeldProjectile(instance, 2.5, 0.05, 0.99), textHeightHelper, width, width2, value, computedValue, computedValue2, computedValue3, computedValue4
                     );
                  }
               }

               if (this.projectiles.isSelected("Стрела")) {
                  ItemStack itemStack4 = this.heldItem(instance, Items.BOW, Items.CROSSBOW);
                  if (!itemStack4.isEmpty()) {
                     double item = itemStack4.getItem() == Items.CROSSBOW ? 3.15 : 3.0;
                     this.drawPreviewTag(
                        guiGraphicsExtractor,
                        instance,
                        new ItemStack(Items.ARROW),
                        this.simulateHeldProjectile(instance, item, 0.05, 0.6),
                        textHeightHelper,
                        width,
                        width2,
                        value,
                        computedValue,
                        computedValue2,
                        computedValue3,
                        computedValue4
                     );
                  }
               }

               BeginFrameHelper.flush();
            } finally {
               CurrentGuiGraphicsExtractorHelper.exit2D();
            }
         }
      }
   }

   public record PredictionResult(Vec3 landing, int ticks) {
      public Vec3 landing() {
         return this.landing;
      }

      public int ticks() {
         return this.ticks;
      }
   }
}
