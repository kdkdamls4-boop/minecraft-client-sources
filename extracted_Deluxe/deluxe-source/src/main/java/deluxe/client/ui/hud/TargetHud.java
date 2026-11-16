package deluxe.client.ui.hud;

import deluxe.client.render.CornerRadii;
import deluxe.client.util.EntityUtils;
import deluxe.client.render.FontText;
import deluxe.client.util.GlintMode;
import deluxe.client.render.ItemRenderCache;
import deluxe.client.render.ShapeDrawing;
import deluxe.client.render.ThemeColors;
import deluxe.client.ui.UiTheme;

import deluxe.client.util.Animation;

import deluxe.client.module.combat.AttackAuraModule;
import deluxe.client.util.Colors;
import deluxe.client.util.Easings;
import deluxe.client.render.FontFace;
import deluxe.client.module.render.InterfaceModule;
import deluxe.client.util.ItemDrawOptions;
import deluxe.client.render.Render2D;
import deluxe.client.module.player.ServerHelperModule;
import deluxe.client.module.misc.YTMODEModule;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.EntityRenderManager;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

public final class TargetHud {
   private static final Animation field00111 = new Animation();
   private static final Animation field00112 = new Animation();
   private static final Animation field00113 = new Animation();
   private static final Animation field00114 = new Animation();
   private static final Animation field00115 = new Animation();
   private static final Map<EntityType<?>, Identifier> field00116 = new HashMap<>();
   private static final Map<Identifier, float[]> field00117 = new HashMap<>();
   private static final ArrayList<TargetHud.Nested0350> field00118 = new ArrayList<>();
   private static LivingEntity field00119;
   private static LivingEntity field00120;
   private static LivingEntity field00121;
   private static long field00122;
   private static int field00123;

   private static int method0483(byte[] byteArray, int local583_1) {
      return (byteArray[local583_1] & 0xFF) << 24
         | (byteArray[local583_1 + 1] & 0xFF) << 16
         | (byteArray[local583_1 + 2] & 0xFF) << 8
         | byteArray[local583_1 + 3] & 0xFF;
   }

   private static boolean method0484(byte[] byteArray) {
      return byteArray.length == 24
         && (byteArray[0] & 255) == 137
         && byteArray[1] == 80
         && byteArray[2] == 78
         && byteArray[3] == 71
         && byteArray[12] == 73
         && byteArray[13] == 72
         && byteArray[14] == 68
         && byteArray[15] == 82;
   }

   private static float method0485(float local585_0) {
      return Math.round(local585_0 * 2.0F) / 2.0F;
   }

   public static void method0486(MinecraftClient minecraftClient, float local586_1, float local586_2, float local586_3) {
      long local586_4 = System.currentTimeMillis();
      LivingEntity livingEntity = method0499(minecraftClient);
      if (livingEntity != null) {
         field00119 = livingEntity;
         field00122 = local586_4;
      }

      boolean local586_7 = field00119 == null || !field00119.isAlive() || field00119.isRemoved() || local586_4 - field00122 > 800L;
      field00111.update();
      field00111.animateTo(local586_7 ? 0.0 : 1.0, 0.18, Easings.EASE_OUT_QUART);
      float local586_8 = field00111.floatValue();
      if (!(local586_8 <= 0.01F) && field00119 != null) {
         if (field00119 != field00120) {
            field00121 = field00120;
            field00120 = field00119;
            field00115.resetTo(0.0);
            field00115.animateTo(1.0, 0.32, Easings.EASE_OUT_EXPO);
         }

         field00115.update();
         float local586_9 = YTMODEModule.method1426().isEnabled() && field00119.isInvisible()
            ? 1000.0F
            : (field00119 instanceof PlayerEntity ? EntityUtils.method2790(field00119) : field00119.getHealth());
         float local586_10 = Math.max(1.0F, Math.max(field00119.getMaxHealth(), local586_9));
         float local586_11 = MathHelper.clamp(local586_9, 0.0F, local586_10);
         float local586_12 = MathHelper.clamp(field00119.getAbsorptionAmount(), 0.0F, local586_10);
         boolean local586_13 = local586_12 > 0.01F;
         float targetValue = local586_11 / local586_10;
         float targetValue2 = local586_13 ? local586_12 / local586_10 : 0.0F;
         field00112.update();
         field00112.animateTo(targetValue, 0.5, Easings.EASE_OUT_EXPO);
         if (local586_13) {
            field00113.update();
            field00113.animateTo(targetValue2, 0.5, Easings.EASE_OUT_EXPO);
         } else {
            field00113.resetTo(0.0);
         }

         float[] floatArray = HudManager.method3390("target", local586_1 / 2.0F - 46.0F, local586_2 / 2.0F + 24.0F);
         HudManager.method3418("target", floatArray[0], floatArray[1], 92.0F, 30.0F);
         HudRenderContext.method4341(floatArray[0] + 46.0F, floatArray[1] + 15.0F, local586_8);
         float local586_17 = floatArray[0];
         float local586_18 = floatArray[1];
         float local586_19 = 1.0F - (float)Easings.EASE_OUT_EXPO.apply(local586_8);
         HudStyle.method4817(local586_17, local586_18, 92.0F, 30.0F, local586_8, 7.0F);
         float local586_20 = field00115.floatValue();
         float local586_21 = local586_17 + 4.0F - local586_19 * 16.0F;
         float local586_22 = local586_18 + 4.0F;
         ShapeDrawing.method3654(
            method0485(local586_21), method0485(local586_22), 22.0F, 22.0F, 2.0F, -16777216, local586_8 * 0.2F, CornerRadii.uniform(4.0F)
         );
         if (field00121 != null && local586_20 < 0.999F) {
            method0493(minecraftClient, field00121, local586_21 - 5.0F * local586_20, local586_22, 22.0F, local586_8 * (1.0F - local586_20));
         }

         method0493(
            minecraftClient, field00119, local586_21 + 5.0F * (1.0F - local586_20), local586_22, 22.0F, local586_8 * (field00121 != null ? local586_20 : 1.0F)
         );
         if (InterfaceModule.method2247().field07923.isChecked() && field00119.hurtTime > field00123) {
            method0496(local586_21 + 11.0F, local586_22 + 11.0F, local586_4);
         }

         field00123 = field00119.hurtTime;
         field00114.update();
         field00114.animateTo(method0498(field00119) ? 1.0 : 0.0, 0.35, Easings.EASE_OUT_QUAD);
         float local586_23 = local586_18 + 7.0F - local586_19 * 6.0F;
         float local586_24 = local586_18 + 18.0F + local586_19 * 6.0F;
         float local586_25 = local586_17 + 30.0F;
         float local586_26 = local586_17 + 88.0F;
         float local586_27 = local586_26 - local586_25;
         String local586_28 = method0492(local586_11);
         float local586_29 = FontText.method1396(FontFace.SEMI_BOLD, local586_10 >= 100.0F ? "000.0" : "00.0", 6.0F);
         float local586_30 = Math.max(12.0F, local586_27 - local586_29 - 3.0F);
         method0494(field00119, local586_25, local586_23, local586_27, local586_8);
         float local586_31 = field00114.floatValue();
         if (local586_31 > 0.01F) {
            method0489(
               field00119,
               local586_17 + 4.0F,
               local586_18 - 10.0F + (1.0F - local586_31) * 4.0F - local586_19 * 6.0F,
               local586_26 - local586_17 - 4.0F,
               local586_8 * local586_31
            );
         }

         method0487(local586_25, local586_24, local586_30, local586_8);
         FontText.method1397(
            FontFace.SEMI_BOLD,
            local586_28,
            local586_25 + local586_30 + 3.0F,
            local586_24 - 1.5F,
            6.0F,
            UiTheme.method2877(ThemeColors.field13669, local586_8)
         );
         method0497(local586_4, local586_8);
         HudRenderContext.method4337();
      } else {
         if (local586_7 && local586_8 <= 0.01F) {
            field00119 = null;
         }

         field00120 = null;
         field00121 = null;
         field00115.resetTo(1.0);
      }
   }

   private static void method0487(float local587_0, float local587_1, float local587_2, float local587_3) {
      CornerRadii cornerRadii = CornerRadii.uniform(2.5F);
      int local587_5 = ThemeColors.field13669;
      int local587_6 = Colors.method3723(local587_5, -16777216, 0.35F);
      float local587_7 = local587_3 * 0.15F;
      int local587_8 = Colors.method3727(local587_5, local587_7);
      int local587_9 = Colors.method3727(local587_6, local587_7);
      ShapeDrawing.method3655(local587_0, local587_1, local587_2, 5.0F, local587_8, local587_8, local587_9, local587_9, 1.0F, cornerRadii);
      int local587_10 = Colors.method3727(local587_5, local587_3);
      int local587_11 = Colors.method3727(local587_6, local587_3);
      float local587_12 = local587_2 * MathHelper.clamp(field00112.floatValue(), 0.0F, 1.0F);
      if (local587_12 > 0.5F) {
         ShapeDrawing.method3655(local587_0, local587_1, local587_12, 5.0F, local587_10, local587_10, local587_11, local587_11, 1.0F, cornerRadii);
      }

      if (!ServerHelperModule.method0707().modeSetting.isSelected("FunTime")) {
         float local587_13 = local587_3 * 0.9F;
         int local587_14 = Colors.method3727(-7717102, local587_13);
         int local587_15 = Colors.method3727(-1013958, local587_13);
         float local587_16 = Math.min(local587_2, local587_2 * MathHelper.clamp(field00113.floatValue(), 0.0F, 1.0F));
         if (local587_16 > 0.5F) {
            ShapeDrawing.method3655(local587_0, local587_1, local587_16, 5.0F, local587_14, local587_14, local587_15, local587_15, 1.0F, cornerRadii);
         }
      }
   }

   private static void method0489(LivingEntity livingEntity, float local589_1, float local589_2, float local589_3, float local589_4) {
      float local589_5 = method0485(local589_1);
      float local589_6 = method0485(local589_2);
      float local589_7 = local589_5 + local589_3;
      float local589_8 = local589_5;

      for (EquipmentSlot equipmentSlot : new EquipmentSlot[]{
         EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND, EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
      }) {
         ItemStack equippedStack = livingEntity.getEquippedStack(equipmentSlot);
         if (!equippedStack.isEmpty()) {
            if (local589_8 + 8.0F > local589_7) {
               break;
            }

            ItemRenderCache.method2336(equippedStack, local589_8, local589_6, 8.0F, ItemDrawOptions.decorated(local589_4).glint(GlintMode.OFF));
            local589_8 += 9.0F;
         }
      }
   }

   private static Identifier method0490(MinecraftClient minecraftClient, LivingEntity livingEntity) {
      try {
         EntityRenderManager entityRenderDispatcher = minecraftClient.getEntityRenderDispatcher();
         if (entityRenderDispatcher.getRenderer(livingEntity) instanceof LivingEntityRenderer livingEntityRenderer
            && entityRenderDispatcher.getAndUpdateRenderState(livingEntity, 1.0F) instanceof LivingEntityRenderState livingEntityRenderState) {
            Identifier texture = livingEntityRenderer.getTexture(livingEntityRenderState);
            return texture != null && minecraftClient.getResourceManager().getResource(texture).isPresent() ? texture : null;
         }
      } catch (Throwable throwable) {
      }

      return null;
   }

   private static Identifier method0491(MinecraftClient minecraftClient, LivingEntity livingEntity) {
      if (livingEntity instanceof AbstractClientPlayerEntity abstractClientPlayerEntity) {
         return abstractClientPlayerEntity.getSkin().body().texturePath();
      } else {
         EntityType<?> type = livingEntity.getType();
         if (field00116.containsKey(type)) {
            return field00116.get(type);
         }

         ResourceManager resourceManager = minecraftClient.getResourceManager();
         String path = EntityType.getId(type).getPath();
         Identifier identifier = Identifier.ofVanilla("textures/entity/" + path + ".png");
         if (resourceManager.getResource(identifier).isEmpty()) {
            identifier = Identifier.ofVanilla("textures/entity/" + path + "/" + path + ".png");
         }

         if (resourceManager.getResource(identifier).isEmpty()) {
            identifier = method0490(minecraftClient, livingEntity);
         }

         field00116.put(type, identifier);
         return identifier;
      }
   }

   private static String method0492(float local593_0) {
      int local593_1 = (int)local593_0;
      int local593_2 = Math.round((local593_0 - local593_1) * 10.0F);
      if (local593_2 >= 10) {
         return Integer.toString(local593_1 + 1);
      } else {
         return local593_2 <= 0 ? Integer.toString(local593_1) : local593_1 + "." + local593_2;
      }
   }

   private static void method0493(MinecraftClient minecraftClient, LivingEntity livingEntity, float local594_2, float local594_3, float local594_4, float local594_5) {
      if (!(local594_5 <= 0.004F)) {
         float local594_6 = method0485(local594_2);
         float local594_7 = method0485(local594_3);
         CornerRadii cornerRadii = CornerRadii.uniform(4.0F);
         Identifier identifier = method0491(minecraftClient, livingEntity);
         if (identifier == null) {
            float local594_10 = local594_4 * 0.25F;
            ShapeDrawing.method3663(
               local594_6, local594_7, local594_4, local594_4, Colors.method3723(-15790575, -1, 0.15F), local594_5 * 0.6F, cornerRadii
            );
            HudIcons.USER
               .method4010(
                  local594_6 + local594_10,
                  local594_7 + local594_10,
                  local594_4 * 0.5F,
                  UiTheme.method2877(HudStyle.method4815(1.0F), local594_5)
               );
         } else {
            float[] floatArray = method0495(minecraftClient, identifier);
            String local594_11 = identifier.toString();
            int local594_12 = UiTheme.method2877(-1, local594_5);
            Render2D.method4677(
               local594_11,
               local594_6,
               local594_7,
               local594_4,
               local594_4,
               4.0F,
               0.25F,
               floatArray[0],
               floatArray[1],
               floatArray[2],
               floatArray[3],
               local594_12
            );
            if (floatArray[4] > 0.5F) {
               Render2D.method4677(
                  local594_11,
                  local594_6,
                  local594_7,
                  local594_4,
                  local594_4,
                  4.0F,
                  0.25F,
                  floatArray[5],
                  floatArray[6],
                  floatArray[7],
                  floatArray[8],
                  local594_12
               );
            }

            float local594_13 = livingEntity.maxHurtTime > 0 ? (float)livingEntity.hurtTime / livingEntity.maxHurtTime : 0.0F;
            if (local594_13 > 0.0F) {
               ShapeDrawing.method3663(local594_6, local594_7, local594_4, local594_4, -65536, local594_5 * local594_13 * 0.55F, cornerRadii);
            }
         }
      }
   }

   private static void method0494(LivingEntity livingEntity, float local595_1, float local595_2, float local595_3, float local595_4) {
      String string = livingEntity.getName().getString();
      float local595_6 = local595_1 + Math.max(0.0F, local595_3);
      FontText.method1403(
         FontFace.SEMI_BOLD,
         string,
         local595_1,
         local595_2,
         6.0F,
         UiTheme.method2877(HudStyle.method4820(1.0F), local595_4),
         local595_1,
         local595_6,
         4.0F,
         false,
         true
      );
   }

   private static float[] method0495(MinecraftClient minecraftClient, Identifier identifier) {
      float[] floatArray = field00117.get(identifier);
      if (floatArray != null) {
         return floatArray;
      }

      int local596_3 = 64;
      int local596_4 = 64;
      Optional<Resource> resource = minecraftClient.getResourceManager().getResource(identifier);
      if (resource.isPresent()) {
         boolean local596_6 = false;

         try (InputStream inputStream = resource.get().getInputStream()) {
            byte[] byteArray = inputStream.readNBytes(24);
            if (method0484(byteArray)) {
               local596_3 = Math.max(1, method0483(byteArray, 16));
               local596_4 = Math.max(1, method0483(byteArray, 20));
               local596_6 = true;
            }
         } catch (Exception exception) {
         }

         if (!local596_6) {
            try (
               InputStream inputStream2 = resource.get().getInputStream();
               NativeImage nativeImage = NativeImage.read(inputStream2);
            ) {
               local596_3 = Math.max(1, nativeImage.getWidth());
               local596_4 = Math.max(1, nativeImage.getHeight());
            } catch (Exception exception2) {
            }
         }
      }

      float local596_6 = local596_3 == 64 && local596_4 == 64 ? 1.0F : 0.0F;
      float[] floatArray2 = new float[]{
         8.0F / local596_3,
         8.0F / local596_4,
         16.0F / local596_3,
         16.0F / local596_4,
         local596_6,
         40.0F / local596_3,
         8.0F / local596_4,
         48.0F / local596_3,
         16.0F / local596_4
      };
      field00117.put(identifier, floatArray2);
      return floatArray2;
   }

   private static void method0496(float local597_0, float local597_1, long local597_2) {
      field00118.replaceAll(
         local582_2 -> local582_2.fadeFromMs() > 0L
            ? local582_2
            : new TargetHud.Nested0350(
               local582_2.startX(), local582_2.startY(), local582_2.offsetX(), local582_2.offsetY(), local582_2.size(), local582_2.bornMs(), local597_2
            )
      );
      ThreadLocalRandom threadLocalRandom = ThreadLocalRandom.current();
      int local597_5 = 10 + threadLocalRandom.nextInt(4);

      for (int index = 0; index < local597_5; index++) {
         double local597_7 = (Math.PI * 2) * (index + threadLocalRandom.nextDouble(0.7)) / local597_5;
         float local597_9 = 20.0F + threadLocalRandom.nextFloat() * 14.0F;
         float local597_10 = 5.0F + threadLocalRandom.nextFloat() * 4.0F;
         field00118.add(
            new TargetHud.Nested0350(
               local597_0, local597_1, (float)Math.cos(local597_7) * local597_9, (float)Math.sin(local597_7) * local597_9, local597_10, local597_2, 0L
            )
         );
      }
   }

   private TargetHud() {
   }

   private static void method0497(long local599_0, float local599_2) {
      field00118.removeIf(
         local588_2 -> local599_0 - local588_2.bornMs() >= 1000L || local588_2.fadeFromMs() > 0L && local599_0 - local588_2.fadeFromMs() >= 200L
      );
      if (!field00118.isEmpty()) {
         if (HudRenderContext.method4340() != null) {
            HudRenderContext.method4340().createNewRootLayer();
         }

         for (TargetHud.Nested0350 local599_4 : field00118) {
            float local599_5 = (float)(local599_0 - local599_4.bornMs()) / 1000.0F;
            float local599_6 = 1.0F - local599_5;
            float local599_7 = 1.0F - local599_6 * local599_6 * local599_6;
            float local599_8 = Math.min(1.0F, local599_5 / 0.15F) * local599_6 * local599_2;
            float local599_9 = local599_4.startX() + local599_4.offsetX() * local599_7;
            float local599_10 = local599_4.startY() + local599_4.offsetY() * local599_7;
            if (local599_4.fadeFromMs() > 0L) {
               local599_8 *= Math.max(0.0F, 1.0F - (float)(local599_0 - local599_4.fadeFromMs()) / 200.0F);
            }

            float local599_11 = local599_4.size() * (1.0F - local599_5 * 0.4F);
            float local599_12 = local599_11 / 2.0F;
            Render2D.method4673(
               "assets/deluxe/textures/particles/glow.png",
               local599_9 - local599_12,
               local599_10 - local599_12,
               local599_11,
               0.0F,
               0.0F,
               local599_9,
               local599_10,
               UiTheme.method2877(ThemeColors.field13669, local599_8)
            );
         }
      }
   }

   private static boolean method0498(LivingEntity livingEntity) {
      if (livingEntity.getMainHandStack().isEmpty() && livingEntity.getOffHandStack().isEmpty()) {
         for (EquipmentSlot equipmentSlot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            if (!livingEntity.getEquippedStack(equipmentSlot).isEmpty()) {
               return true;
            }
         }

         return false;
      } else {
         return true;
      }
   }

   private static LivingEntity method0499(MinecraftClient minecraftClient) {
      if (minecraftClient.player == null) {
         return null;
      } else if (HudRenderContext.method4343()) {
         return minecraftClient.player;
      } else {
         AttackAuraModule attackAuraModule = AttackAuraModule.method1029();
         if (attackAuraModule.isEnabled() && attackAuraModule.method1005() instanceof LivingEntity livingEntity && livingEntity.isAlive()) {
            return livingEntity;
         } else {
            return minecraftClient.targetedEntity instanceof LivingEntity livingEntity2 && livingEntity2 != minecraftClient.player && livingEntity2.isAlive() ? livingEntity2 : null;
         }
      }
   }

   public static float method0500() {
      return field00111.floatValue();
   }

   private record Nested0350(float startX, float startY, float offsetX, float offsetY, float size, long bornMs, long fadeFromMs) {
   }
}
