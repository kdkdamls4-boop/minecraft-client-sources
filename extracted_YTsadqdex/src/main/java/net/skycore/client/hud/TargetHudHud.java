package net.skycore.client.hud;

import java.util.function.Function;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.ModelPart.Cube;
import net.minecraft.client.model.geom.ModelPart.Polygon;
import net.minecraft.client.model.geom.ModelPart.Vertex;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.skycore.client.SkycoreClient;
import net.skycore.client.mixin.ModelPartGetCubesAccessor;
import net.skycore.client.module.combat.AttackAuraModule;
import net.skycore.client.theme.ThemeManager;
import net.skycore.client.util.BeginFrameHelper;
import net.skycore.client.util.CurrentGuiGraphicsExtractorHelper;
import net.skycore.client.util.DrawDigitHelper;
import net.skycore.client.util.InterBoldHelper;
import net.skycore.client.util.ScoreboardModeHelper;
import net.skycore.client.util.TextHeightHelper;
import org.joml.Matrix3x2fStack;

public final class TargetHudHud {
   private long healthTransitionAt;
   private static final float EQUIPMENT_ITEM_SIZE = 9.0F;
   private static final float HP_PILL_PAD_X = 4.0F;
   private static final float BODY_RADIUS = 11.0F;
   private static final float PAD_X = 6.0F;
   private LivingEntity animatedTarget;
   private static final float HEALTH_RESPONSE_MIN = 4.0F;
   private int healthValue;
   private static final float HP_PILL_RADIUS = 5.5F;
   private float displayedHealth;
   private static final long HP_VALUE_TRANSITION_MS = 220L;
   private float alpha;
   private static final float HP_TEXT_SIZE = 7.5F;
   private LivingEntity visibleTarget;
   private static final float WIDTH = 128.0F;
   private static final float HP_RIGHT_PAD = 10.0F;
   private static final float EQUIPMENT_GROUP_GAP = 5.0F;
   private static final float HEALTH_RESPONSE_PER_HP = 2.35F;
   private static final float HEAD_SIZE = 31.0F;
   private static final float EQUIPMENT_GAP_Y = 4.0F;
   private static final float HEALTH_RESPONSE_MAX = 30.0F;
   private static final float ALPHA_SPEED = 5.5F;
   private static final String NO_ARMOR_GLYPH = "k";
   private static final float NAME_SIZE = 10.0F;
   private static final float HANDS_WIDTH = 27.0F;
   private long lastFrameAt;
   private final Draggable draggable = new Draggable("target_hud", 0.43F, 0.62F);
   private static final float BODY_HEIGHT = 43.0F;
   private static final float HP_PILL_HEIGHT = 11.0F;
   private int previousHealthValue;
   private static final float EQUIPMENT_ITEM_GAP = 3.0F;
   private static final float INFO_GAP = 7.0F;
   private static final float BAR_HEIGHT = 2.0F;
   private static final float HP_PILL_MIN_WIDTH = 24.0F;
   private static final float EQUIPMENT_RADIUS = 100.0F;
   private static final float ARMOR_WIDTH = 51.0F;
   private static final float EQUIPMENT_HEIGHT = 15.0F;
   private static final float HEAD_RADIUS_FACTOR = 0.235F;

   private static void drawBlock(float coordinate, float coordinate2, float coordinate3, float coordinate4, float color, float radius, float alpha2) {
      if (HudStyle.transparent()) {
         BeginFrameHelper.RectBuilder rectBuilder = BeginFrameHelper.rect(coordinate, coordinate2, coordinate3, coordinate4).color(HudStyle.background(alpha2));
         rectBuilder = rectBuilder.radius(color).hudGlow(false);
         rectBuilder = rectBuilder.blur(HudStyle.blurRadius(radius), alpha2);
         rectBuilder.draw();
      } else {
         BeginFrameHelper.RectBuilder rectBuilder2 = BeginFrameHelper.rect(coordinate, coordinate2, coordinate3, coordinate4).color(withAlpha(-15856114, alpha2)).radius(color);
         rectBuilder2.hudGlow(false).draw();
      }
   }

   private LivingEntity resolveTarget(Minecraft minecraft, boolean condition) {
      if (condition) {
         return minecraft.player;
      } else {
         AttackAuraModule attackAuraModule = SkycoreClient.modules().get(AttackAuraModule.class).orElse(null);
         LivingEntity entity = attackAuraModule == null ? null : attackAuraModule.currentTarget();
         return entity != null && entity.isAlive() ? entity : null;
      }
   }

   private void drawEquipmentSlot(GuiGraphicsExtractor guiGraphicsExtractor, ItemStack itemStack, float coordinate, float coordinate2, float width) {
      int color = -1;
      if (!itemStack.isEmpty()) {
         drawEquipmentItem(guiGraphicsExtractor, itemStack, coordinate, coordinate2, width);
      } else {
         color = (byte)-1;
         float width2 = InterBoldHelper.skycoreIcons().getWidth("k", width);
         BeginFrameHelper.TextBuilder textBuilder = BeginFrameHelper.text(coordinate + (width - width2) * 0.5F, coordinate2 + 0.3F, width - 1.0F, "k");
         TextHeightHelper textHeightHelper = InterBoldHelper.skycoreIcons();
         BeginFrameHelper.TextBuilder textBuilder2 = textBuilder.font(textHeightHelper);
         float alpha2 = 0.1F * this.alpha;
         color = withAlpha(-1, alpha2);
         textBuilder2.color(color).draw();
      }
   }

   public void render(float alpha2, boolean condition) {
      Minecraft instance = Minecraft.getInstance();
      LivingEntity entity = this.resolveTarget(instance, condition);
      long nowNanos = System.nanoTime();
      float computedValue = this.lastFrameAt == 0L ? 0.016F : Math.min(0.05F, (float)(nowNanos - this.lastFrameAt) / 1.0E9F);
      this.lastFrameAt = nowNanos;
      if (entity != null) {
         this.visibleTarget = entity;
         if (this.animatedTarget != entity) {
            this.animatedTarget = entity;
            this.displayedHealth = Math.max(0.0F, ScoreboardModeHelper.getDisplayHealth(entity));
            this.healthValue = (int)Math.ceil(ScoreboardModeHelper.getLabelHealth(entity));
            this.previousHealthValue = this.healthValue;
            this.healthTransitionAt = nowNanos;
         }
      }

      this.alpha = moveTowards(this.alpha, entity == null ? 0.0F : 1.0F, 5.5F * computedValue);
      if (this.visibleTarget != null && !(this.alpha <= 0.001F)) {
         LivingEntity entity2 = entity != null ? entity : this.visibleTarget;
         float displayHealth = Math.max(0.0F, ScoreboardModeHelper.getDisplayHealth(entity2));
         float computedValue2 = Math.abs(displayHealth - this.displayedHealth);
         float clampedValue = clamp(4.0F + computedValue2 * 2.35F, 4.0F, 30.0F);
         float computedValue3 = 1.0F - (float)Math.exp(-clampedValue * computedValue);
         this.displayedHealth = this.displayedHealth + (displayHealth - this.displayedHealth) * computedValue3;
         if (Math.abs(displayHealth - this.displayedHealth) < 0.01F) {
            this.displayedHealth = displayHealth;
         }

         int labelHealth = (int)Math.ceil(ScoreboardModeHelper.getLabelHealth(entity2));
         if (labelHealth != this.healthValue) {
            this.previousHealthValue = this.healthValue;
            this.healthValue = labelHealth;
            this.healthTransitionAt = nowNanos;
         }

         float alpha3 = 128.0F * alpha2;
         float alpha4 = 43.0F * alpha2;
         float computedValue4 = 4.0F * alpha2;
         float computedValue5 = 15.0F * alpha2;
         float computedValue6 = alpha4 + computedValue4 + computedValue5;
         this.draggable.setBounds(alpha3, computedValue6);
         float alpha5 = this.draggable.x();
         float alpha6 = this.draggable.y();
         ThemeManager themeManager = SkycoreClient.themes();
         int computedValue7 = withAlpha(themeManager.textPrimary(), 0.75F * this.alpha);
         int computedValue8 = withAlpha(-1, 0.5F * this.alpha);
         int color = withAlpha(-1, 0.25F * this.alpha);
         int color2 = withAlpha(-1, 0.03F * this.alpha);
         int color3 = withAlpha(themeManager.accent(), this.alpha);
         drawBlock(alpha5, alpha6, alpha3, alpha4, 11.0F * alpha2 + 1.0F, alpha2, this.alpha);
         float alpha7 = 31.0F * alpha2;
         float alpha8 = alpha5 + 6.0F * alpha2;
         float alpha9 = alpha6 + (alpha4 - alpha7) * 0.5F;
         drawHead(entity2, alpha8, alpha9, alpha7, this.alpha, 0.235F);
         float computedValue9 = alpha8 + alpha7 + 7.0F * alpha2;
         float computedValue10 = alpha5 + alpha3 - 10.0F * alpha2;
         String text = Integer.toString(this.healthValue);
         String text2 = Integer.toString(this.previousHealthValue);
         String text3 = "hp";
         float width = 7.5F * alpha2;
         float value = DrawDigitHelper.slotWidth(text2, text, width, 600, this.healthTransitionAt, nowNanos, 220L, false);
         float width2 = InterBoldHelper.forWeight(600).getWidth(text3, width);
         float computedValue11 = value + width2;
         float maximum = Math.max(24.0F * alpha2, computedValue11 + 8.0F * alpha2);
         float computedValue12 = 11.0F * alpha2;
         float computedValue13 = computedValue10 - maximum;
         float computedValue14 = alpha6 + 12.0F * alpha2;
         float computedValue15 = alpha6 + 13.0F * alpha2 - 1.0F;
         float computedValue16 = computedValue13 - 5.0F * alpha2;
         float maximum2 = Math.max(0.0F, computedValue16 - computedValue9);
         BeginFrameHelper.pushScissor(computedValue9, computedValue15 - 2.0F * alpha2, maximum2, 13.0F * alpha2);

         try {
            drawFadingName(computedValue9, computedValue15, 10.0F * alpha2, entity2.getName().getString(), maximum2, 9.0F * alpha2, computedValue7);
         } finally {
            BeginFrameHelper.popScissor();
         }

         BeginFrameHelper.rect(computedValue13, computedValue14, maximum, computedValue12).color(withAlpha(-1, 0.02F * this.alpha)).radius(5.5F * alpha2).hudGlow(false).draw();
         float computedValue17 = computedValue13 + (maximum - computedValue11) * 0.5F;
         float computedValue18 = computedValue14 + 2.1F * alpha2 - 1.0F;
         DrawDigitHelper.renderRight(computedValue17 + value, computedValue18, width, text2, text, this.healthTransitionAt, nowNanos, 220L, computedValue8, 1.0F, 600, width * 0.85F, false);
         BeginFrameHelper.text(computedValue17 + value, computedValue18, width, text3).weight(600).color(color).draw();
         float value2 = computedValue9;
         float maximum3 = Math.max(0.0F, computedValue10 - computedValue9);
         float computedValue19 = alpha6 + 29.0F * alpha2;
         float computedValue20 = 2.0F * alpha2;
         BeginFrameHelper.rect(value2, computedValue19, maximum3, computedValue20).color(color2).radius(computedValue20 * 0.5F).hudGlow(false).draw();
         float maxHealth = Math.max(1.0F, entity2.getMaxHealth());
         float clampedValue2 = clamp(this.displayedHealth / maxHealth, 0.0F, 1.0F);
         float computedValue21 = maximum3 * clampedValue2;
         if (computedValue21 > 0.0F) {
            BeginFrameHelper.rect(value2, computedValue19, computedValue21, computedValue20).color(color3).radius(computedValue20 * 0.5F).hudGlow(false).draw();
         }

         this.renderEquipment(entity2, alpha5, alpha6 + alpha4 + computedValue4, alpha3, alpha2);
      } else {
         if (entity == null) {
            this.visibleTarget = null;
            this.animatedTarget = null;
         }
      }
   }

   private static float clamp(float value, float value2, float value3) {
      return Math.max(value2, Math.min(value3, value));
   }

   private static float moveTowards(float value, float value2, float value3) {
      return value < value2 ? Math.min(value2, value + value3) : Math.max(value2, value - value3);
   }

   private static int withAlpha(int value, float value2) {
      float value3 = value >>> 24 & 0xFF;
      int computedValue = Math.round(value3 * clamp(value2, 0.0F, 1.0F));
      return value & 16777215 | computedValue << 24;
   }

   private static void drawFadingName(float coordinate, float coordinate2, float width, String text, float width2, float coordinate3, int color) {
      TextHeightHelper textHeightHelper = InterBoldHelper.forWeight(500);
      if (textHeightHelper.getWidth(text, width) <= width2) {
         BeginFrameHelper.text(coordinate, coordinate2, width, text).weight(500).color(color).draw();
      } else {
         float computedValue = coordinate + width2;
         float maximum = Math.max(coordinate, computedValue - coordinate3);
         float computedValue2 = coordinate2 - width * 0.35F;
         float computedValue3 = width * 1.8F;
         float maximum2 = Math.max(0.0F, maximum - coordinate);
         if (maximum2 > 0.0F) {
            BeginFrameHelper.pushScissor(coordinate, computedValue2, maximum2, computedValue3);

            try {
               BeginFrameHelper.text(coordinate, coordinate2, width, text).weight(500).color(color).draw();
            } finally {
               BeginFrameHelper.popScissor();
            }
         }

         float value = 1.0F;

         for (float index = maximum; index < computedValue; index += value) {
            float minimum = Math.min(value, computedValue - index);
            float computedValue4 = clamp((index + minimum * 0.5F - maximum) / Math.max(1.0F, computedValue - maximum), 0.0F, 1.0F);
            float computedValue5 = computedValue4 * computedValue4 * (3.0F - 2.0F * computedValue4);
            float alpha2 = 1.0F - computedValue5;
            if (!(alpha2 <= 0.001F)) {
               BeginFrameHelper.pushScissor(index, computedValue2, minimum, computedValue3);

               try {
                  BeginFrameHelper.text(coordinate, coordinate2, width, text).weight(500).color(withAlpha(color, alpha2)).draw();
               } finally {
                  BeginFrameHelper.popScissor();
               }
            }
         }
      }
   }

   private static void drawHead(LivingEntity entity, float coordinate, float coordinate2, float coordinate3, float alpha2, float coordinate4) {
      float color = coordinate3 * coordinate4;
      if (entity instanceof AbstractClientPlayer player) {
         Identifier skin = player.getSkin().body().texturePath();
         int color2 = withAlpha(-1, alpha2);
         BeginFrameHelper.texture(coordinate, coordinate2, coordinate3, coordinate3, skin).managed().uv(0.125F, 0.125F, 0.25F, 0.25F).radius(color).color(color2).draw();
         BeginFrameHelper.texture(coordinate, coordinate2, coordinate3, coordinate3, skin).managed().uv(0.625F, 0.125F, 0.75F, 0.25F).radius(color).color(color2).draw();
      } else {
         TargetHudHud.FaceIcon faceIcon = resolveFaceIcon(entity);
         if (faceIcon != null) {
            BeginFrameHelper.texture(coordinate, coordinate2, coordinate3, coordinate3, faceIcon.texture())
               .managed()
               .uv(faceIcon.u0(), faceIcon.v0(), faceIcon.u1(), faceIcon.v1())
               .radius(color)
               .color(withAlpha(-1, alpha2))
               .draw();
         } else {
            BeginFrameHelper.rect(coordinate, coordinate2, coordinate3, coordinate3).color(withAlpha(SkycoreClient.themes().accent(), alpha2)).radius(color).hudGlow(false).draw();
         }
      }
   }

   private static TargetHudHud.FaceIcon findLargestFrontFace(Identifier texture, ModelPart modelPart) {
      TargetHudHud.FaceIcon faceIcon = null;
      float index = 0.0F;

      for (ModelPart modelPart2 : modelPart.getAllParts()) {
         for (Cube cube : ((ModelPartGetCubesAccessor)(Object)modelPart2).skycore$getCubes()) {
            for (Polygon polygon : cube.polygons) {
               if (!(polygon.normal().z() > -0.9F)) {
                  float pOSITIVE_INFINITY = Float.POSITIVE_INFINITY;
                  float pOSITIVE_INFINITY2 = Float.POSITIVE_INFINITY;
                  float nEGATIVE_INFINITY = Float.NEGATIVE_INFINITY;
                  float nEGATIVE_INFINITY2 = Float.NEGATIVE_INFINITY;

                  for (Vertex vertex : polygon.vertices()) {
                     pOSITIVE_INFINITY = Math.min(pOSITIVE_INFINITY, vertex.u());
                     pOSITIVE_INFINITY2 = Math.min(pOSITIVE_INFINITY2, vertex.v());
                     nEGATIVE_INFINITY = Math.max(nEGATIVE_INFINITY, vertex.u());
                     nEGATIVE_INFINITY2 = Math.max(nEGATIVE_INFINITY2, vertex.v());
                  }

                  float index2 = (nEGATIVE_INFINITY - pOSITIVE_INFINITY) * (nEGATIVE_INFINITY2 - pOSITIVE_INFINITY2);
                  if (index2 > index) {
                     index = index2;
                     faceIcon = new TargetHudHud.FaceIcon(texture, pOSITIVE_INFINITY, pOSITIVE_INFINITY2, nEGATIVE_INFINITY, nEGATIVE_INFINITY2);
                  }
               }
            }
         }
      }

      return faceIcon;
   }

   private static TargetHudHud.FaceIcon resolveFaceIcon(LivingEntity entity) {
      EntityRenderer renderer = Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(entity);
      EntityRenderState entityRenderState = renderer.createRenderState(entity, 1.0F);
      if (renderer instanceof LivingEntityRenderer livingEntityRenderer && entityRenderState instanceof LivingEntityRenderState livingEntityRenderState) {
         Identifier textureLocation = livingEntityRenderer.getTextureLocation(livingEntityRenderState);
         EntityModel model = livingEntityRenderer.getModel();
         ModelPart modelPart = model.root();
         Function function = modelPart.createPartLookup();
         ModelPart modelPart2 = (ModelPart)function.apply("head");
         if (modelPart2 == null) {
            modelPart2 = (ModelPart)function.apply("body");
         }

         if (modelPart2 == null) {
            modelPart2 = modelPart;
         }

         TargetHudHud.FaceIcon largestFrontFace = findLargestFrontFace(textureLocation, modelPart2);
         return largestFrontFace == null && modelPart2 != modelPart ? findLargestFrontFace(textureLocation, modelPart) : largestFrontFace;
      } else {
         return null;
      }
   }

   private void renderEquipment(LivingEntity entity, float coordinate, float alpha2, float coordinate2, float alpha3) {
      float alpha4 = 27.0F * alpha3;
      float alpha5 = 51.0F * alpha3;
      float computedValue = 5.0F * alpha3;
      float computedValue2 = alpha4 + computedValue + alpha5;
      float computedValue3 = coordinate + (coordinate2 - computedValue2) * 0.5F;
      float alpha6 = 15.0F * alpha3;
      float alpha7 = computedValue3;
      float alpha8 = alpha7 + alpha4 + computedValue;
      drawBlock(alpha7, alpha2, alpha4, alpha6, 100.0F * alpha3, alpha3, this.alpha);
      drawBlock(alpha8, alpha2, alpha5, alpha6, 100.0F * alpha3, alpha3, this.alpha);
      GuiGraphicsExtractor guiGraphicsExtractor = CurrentGuiGraphicsExtractorHelper.currentGuiGraphicsExtractor();
      if (guiGraphicsExtractor != null) {
         BeginFrameHelper.flush();
         float slot = 9.0F * alpha3;
         float slot2 = alpha2 + (alpha6 - slot) * 0.5F;
         ItemStack mainHandItem = entity.getMainHandItem();
         ItemStack offhandItem = entity.getOffhandItem();
         ItemStack[] itemStackValues = new ItemStack[]{
            entity.getItemBySlot(EquipmentSlot.HEAD),
            entity.getItemBySlot(EquipmentSlot.CHEST),
            entity.getItemBySlot(EquipmentSlot.LEGS),
            entity.getItemBySlot(EquipmentSlot.FEET)
         };
         float computedValue4 = 3.0F * alpha3;
         float computedValue5 = slot * 2.0F + computedValue4;
         float slot3 = alpha7 + (alpha4 - computedValue5) * 0.5F;
         this.drawEquipmentSlot(guiGraphicsExtractor, mainHandItem, slot3, slot2, slot);
         this.drawEquipmentSlot(guiGraphicsExtractor, offhandItem, slot3 + slot + computedValue4, slot2, slot);
         float computedValue6 = 3.0F * alpha3;
         float computedValue7 = slot * 4.0F + computedValue6 * 3.0F;
         float slot4 = alpha8 + (alpha5 - computedValue7) * 0.5F;

         for (ItemStack itemStack : itemStackValues) {
            this.drawEquipmentSlot(guiGraphicsExtractor, itemStack, slot4, slot2, slot);
            slot4 += slot + computedValue6;
         }
      }
   }

   public Draggable draggable() {
      return this.draggable;
   }

   private static void drawEquipmentItem(GuiGraphicsExtractor guiGraphicsExtractor, ItemStack itemStack, float coordinate, float coordinate2, float coordinate3) {
      Minecraft instance = Minecraft.getInstance();
      Matrix3x2fStack matrix3x2fStack = guiGraphicsExtractor.pose();
      float guiScale = instance.getWindow().getGuiScale();
      float computedValue = coordinate3 / 16.0F;
      float computedValue2 = Math.round(coordinate * guiScale) / guiScale;
      float computedValue3 = coordinate2 * guiScale;
      float computedValue4 = Math.round(computedValue3) / guiScale;
      matrix3x2fStack.pushMatrix();
      matrix3x2fStack.translate(computedValue2, computedValue4);
      matrix3x2fStack.scale(computedValue);
      guiGraphicsExtractor.item(itemStack, 0, 0);
      matrix3x2fStack.popMatrix();
   }

   public record FaceIcon(Identifier texture, float u0, float v0, float u1, float v1) {
      public float v0() {
         return this.v0;
      }

      public float v1() {
         return this.v1;
      }

      public Identifier texture() {
         return this.texture;
      }

      public float u0() {
         return this.u0;
      }

      public float u1() {
         return this.u1;
      }
   }
}
