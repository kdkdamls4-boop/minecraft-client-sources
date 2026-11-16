package net.skycore.client.module.render;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import net.skycore.client.SkycoreClient;
import net.skycore.client.config.FriendManager;
import net.skycore.client.module.ClientModule;
import net.skycore.client.module.ModuleCategory;
import net.skycore.client.util.BeginFrameHelper;
import net.skycore.client.setting.ColorSetting;
import net.skycore.client.setting.NumberSetting;
import net.skycore.client.setting.ModeSetting;
import net.skycore.client.setting.MultiSelectSetting;
import net.skycore.client.util.CurrentGuiGraphicsExtractorHelper;
import net.skycore.client.setting.BooleanSetting;
import net.skycore.client.util.WriteHexByteHelper;

public final class ArrowsModule extends ClientModule {
   private static final float FADE_IN_SPEED = 7.0F;
   private final MultiSelectSetting targets = this.add(
      new MultiSelectSetting("Targets", List.of("Players", "Friends", "Naked", "Items"), Set.of("Players", "Friends"))
   );
   public static final Identifier ARROW_TEXTURE = Identifier.parse("skycore:textures/nametag/arrow.png");
   private final ModeSetting colorMode = this.add(new ModeSetting("Color", "Sync", "Sync", "Custom"));
   private long lastFrameNanos;
   private static final float FOLLOW_SPEED = 8.0F;
   private final BooleanSetting onlyIfNotVisible;
   private final Map<Entity, ArrowsModule.ArrowState> arrowStates;
   private static final float ARROW_SIZE = 18.0F;
   private final ColorSetting customColor = this.add(new ColorSetting("Custom Color", -1, false));
   private static final int FRIEND_COLOR = -11141291;
   private static final float FADE_OUT_SPEED = 5.0F;
   public static final ArrowsModule INSTANCE = new ArrowsModule();
   private Object lastLevel;
   private final NumberSetting radius;

   private boolean isEntityVisible(Minecraft minecraft, Entity entity) {
      Vec3 eyePosition = minecraft.player.getEyePosition();
      Vec3 position = entity.position();
      Vec3 bbHeight = position.add(0.0, entity.getBbHeight() * 0.5, 0.0);
      BlockHitResult blockHitResult = minecraft.level.clip(new ClipContext(eyePosition, bbHeight, Block.COLLIDER, Fluid.NONE, minecraft.player));
      return blockHitResult.getType() == Type.MISS;
   }

   public ArrowsModule() {
      super("Arrows", "Draws directional arrows for selected targets", ModuleCategory.RENDER);
      this.onlyIfNotVisible = this.add(new BooleanSetting("Only If Not Visible", false));
      this.radius = this.add(new NumberSetting("Radius", 40.0, 30.0, 80.0, 5.0));
      this.arrowStates = new IdentityHashMap<>();
      this.customColor.visibleWhen(() -> {
         return this.colorMode.is("Custom");
      });
   }

   @Override
   protected void onDisable() {
      this.arrowStates.clear();
      this.lastFrameNanos = 0L;
      this.lastLevel = null;
   }

   public void render(GuiRenderState guiRenderState, DeltaTracker deltaTracker) {
      if (this.isEnabled()) {
         Minecraft instance = Minecraft.getInstance();
         if (instance.player != null && instance.level != null && instance.getWindow() != null && instance.gameRenderer != null) {
            GuiGraphicsExtractor guiGraphicsExtractor = new GuiGraphicsExtractor(
               instance,
               guiRenderState,
               (int)Math.round(instance.mouseHandler.getScaledXPos(instance.getWindow())),
               (int)Math.round(instance.mouseHandler.getScaledYPos(instance.getWindow()))
            );
            long nowNanos = System.nanoTime();
            float computedValue = this.lastFrameNanos == 0L ? 0.016666668F : Math.min(0.1F, (float)(nowNanos - this.lastFrameNanos) / 1.0E9F);
            this.lastFrameNanos = nowNanos;
            float computedValue2 = 1.0F - (float)Math.exp(-8.0F * computedValue);
            if (this.lastLevel != instance.level) {
               this.arrowStates.clear();
               this.lastLevel = instance.level;
            }

            CurrentGuiGraphicsExtractorHelper.enter2D(guiGraphicsExtractor, deltaTracker);
            BeginFrameHelper.beginFrame();

            try {
               Vec3 position = instance.gameRenderer.gameRenderState().levelRenderState.cameraRenderState.pos;
               double yRot = Math.toRadians(instance.player.getYRot());
               double cosine = Math.cos(yRot);
               double sine = Math.sin(yRot);
               float window = instance.getWindow().getGuiScaledWidth() * 0.5F;
               float window2 = instance.getWindow().getGuiScaledHeight() * 0.5F;
               float value = this.radius.get().floatValue();
               int value2 = this.colorMode.is("Custom") ? this.customColor.get() : SkycoreClient.themes().accent();
               var entries = Collections.newSetFromMap(new IdentityHashMap());

               for (Entity entity : instance.level.entitiesForRendering()) {
                  boolean validTarget = this.isValidTarget(instance, entity);
                  ArrowsModule.ArrowState arrowState = this.arrowStates.get(entity);
                  if (validTarget || arrowState != null) {
                     if (arrowState == null) {
                        arrowState = new ArrowsModule.ArrowState();
                        this.arrowStates.put(entity, arrowState);
                     }

                     arrowState.visible = validTarget;
                     entries.add(entity);
                  }
               }

               for (Entry entry : this.arrowStates.entrySet()) {
                  Entity key = (Entity)entry.getKey();
                  ArrowsModule.ArrowState value3 = (ArrowsModule.ArrowState)entry.getValue();
                  if (!entries.contains(key)) {
                     value3.visible = false;
                  }

                  double computedValue3 = key instanceof ItemEntity ? Math.max(value - 20.0F, 0.0F) : value;
                  Vec3 position2 = key.position();
                  double x = position2.x - position.x;
                  double z = position2.z - position.z;
                  double computedValue4 = x * 0.01;
                  double computedValue5 = z * 0.01;
                  double computedValue6 = -(computedValue5 * cosine - computedValue4 * sine);
                  double computedValue7 = -(computedValue4 * cosine + computedValue5 * sine);
                  if (Math.sqrt(computedValue7 * computedValue7 + computedValue6 * computedValue6) >= computedValue3) {
                     value3.visible = false;
                  }

                  float value4 = value3.visible ? 7.0F : 5.0F;
                  float alpha = 1.0F - (float)Math.exp(-value4 * computedValue);
                  value3.alpha = value3.alpha + ((value3.visible ? 1.0F : 0.0F) - value3.alpha) * alpha;
                  if (value3.visible || !(value3.alpha <= 0.01F)) {
                     float angleRadians = (float)Math.atan2(computedValue6, computedValue7);
                     float angle = value3.hasAngle ? value3.angle : angleRadians;
                     float computedValue8 = (float)Math.atan2(Math.sin(angleRadians - angle), Math.cos(angleRadians - angle));
                     angle += computedValue8 * computedValue2;
                     value3.angle = angle;
                     value3.hasAngle = true;
                     float computedValue9 = window + (float)(computedValue3 * Math.cos(angle));
                     float computedValue10 = window2 + (float)(computedValue3 * Math.sin(angle));
                     guiGraphicsExtractor.pose().pushMatrix();
                     guiGraphicsExtractor.pose().translate(computedValue9, computedValue10);
                     guiGraphicsExtractor.pose().rotate(angle);
                     int gameProfile = key instanceof Player player && FriendManager.INSTANCE.isFriend(player.getGameProfile().name()) ? -11141291 : value2;
                     gameProfile = WriteHexByteHelper.multiplyAlpha(gameProfile, value3.alpha);
                     BeginFrameHelper.texture(-9.0F, -9.0F, 18.0F, 18.0F, ARROW_TEXTURE).color(gameProfile).draw();
                     BeginFrameHelper.flush();
                     guiGraphicsExtractor.pose().popMatrix();
                  }
               }

               this.arrowStates.entrySet().removeIf(entry2 -> {
                  if (!entry2.getValue().visible) {
                     if (entry2.getValue().alpha <= 0.01F) {
                        return true;
                     }
                  }

                  return false;
               });
               BeginFrameHelper.flush();
            } finally {
               CurrentGuiGraphicsExtractorHelper.exit2D();
            }
         }
      }
   }

   private boolean isValidTarget(Minecraft minecraft, Entity entity) {
      if (entity != null && entity != minecraft.player && !entity.isRemoved() && entity.isAlive()) {
         boolean condition;
         if (entity instanceof Player player) {
            boolean gameProfile = FriendManager.INSTANCE.isFriend(player.getGameProfile().name());
            int armorValue = player.getArmorValue() == 0 ? 1 : 0;
            if (gameProfile) {
               condition = this.targets.isSelected("Friends");
            } else if (armorValue != 0) {
               condition = this.targets.isSelected("Naked");
            } else {
               condition = this.targets.isSelected("Players");
            }
         } else {
            if (!(entity instanceof ItemEntity)) {
               return false;
            }

            condition = this.targets.isSelected("Items");
         }

         return !condition ? false : !this.onlyIfNotVisible.get() || !this.isEntityVisible(minecraft, entity);
      } else {
         return false;
      }
   }

   static final class ArrowState {
      private float angle;
      private float alpha;
      private boolean hasAngle;
      private boolean visible;

      private ArrowState() {
      }
   }
}
