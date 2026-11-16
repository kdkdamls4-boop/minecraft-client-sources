package deluxe.client.util;

import deluxe.client.render.FontText;
import deluxe.client.ui.hud.HudIcons;
import deluxe.client.ui.hud.HudManager;
import deluxe.client.ui.hud.HudRenderContext;
import deluxe.client.ui.hud.HudStyle;
import deluxe.client.render.ItemRenderCache;
import deluxe.client.ui.UiTheme;

import deluxe.client.util.Animation;

import deluxe.client.module.combat.AttackAuraModule;
import deluxe.mixin.BossBarHudMixin;
import deluxe.client.util.Easings;
import deluxe.client.render.FontFace;
import deluxe.client.storage.FriendStore;
import deluxe.client.util.ItemDrawOptions;
import deluxe.client.MinecraftAccess;
import deluxe.client.render.Render2D;
import deluxe.client.RotationAngles;
import deluxe.client.module.player.ServerHelperModule;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.UUID;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.projectile.thrown.ThrownItemEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public final class TargetHudStyle implements MinecraftAccess {
   private static final EquipmentSlot[] field13309 = new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
   private static final HashMap<String, Item> field13310 = new HashMap<>();
   private static final HashMap<String, Item> field13311 = new HashMap<>();
   private static final HashMap<Item, Integer> field13312 = new HashMap<>();
   private static final HashMap<Item, Long> field13313 = new HashMap<>();
   private static final HashMap<UUID, TargetHudStyle.Nested0509> field13314 = new HashMap<>();
   private static final HashSet<Integer> field13315 = new HashSet<>();
   private static final Animation field13316 = new Animation();
   private static final Animation field13317 = new Animation();
   private static final Animation field13318 = new Animation();
   private static final Animation field13319 = new Animation();
   private static final Animation field13320 = new Animation();
   private static final Animation field13321 = new Animation();
   private static final Animation field13322 = new Animation();
   private static UUID field13323;
   private static long field13324;
   private static boolean field13325;
   private static long field13326;
   private static ClientWorld field13327;
   private static TargetHudStyle.Nested0509 field13328;

   private static void method3840(MinecraftClient minecraftClient) {
      if (minecraftClient.player != null && minecraftClient.world != null) {
         if (minecraftClient.world != field13327) {
            field13327 = minecraftClient.world;
            field13323 = null;
            field13325 = false;
            field13314.clear();
         }

         long local6320_1 = System.currentTimeMillis();
         if (field13323 != null && minecraftClient.inGameHud != null) {
            if (!((BossBarHudMixin)minecraftClient.inGameHud.getBossBarHud()).getEvents().isEmpty()) {
               field13325 = true;
               field13326 = 0L;
            } else if (field13325 && field13326 == 0L) {
               field13326 = local6320_1;
            } else if (field13325 && local6320_1 - field13326 > 2000L) {
               field13323 = null;
               field13325 = false;
               field13326 = 0L;
               field13314.clear();
            }
         }

         PlayerEntity playerEntity = AttackAuraModule.method1029().isEnabled()
               && AttackAuraModule.method1029().method1005() instanceof PlayerEntity playerEntity2
            ? playerEntity2
            : null;
         if (playerEntity != null && !FriendStore.method3431(playerEntity.getGameProfile().name())) {
            field13324 = local6320_1;
            if (!playerEntity.getUuid().equals(field13323)) {
               field13323 = playerEntity.getUuid();
               field13325 = false;
               field13326 = 0L;
            }
         }

         boolean local6320_4 = field13323 != null && field13323.equals(minecraftClient.player.getUuid());
         if (playerEntity != null || !HudRenderContext.method4342() || field13323 != null && !local6320_4) {
            if (field13323 != null
               && (!local6320_4 || HudRenderContext.method4342())
               && (playerEntity != null || local6320_1 - field13324 <= 3000L || field13325 || HudRenderContext.method4342())) {
               PlayerEntity playerEntity3 = null;

               for (PlayerEntity playerEntity4 : minecraftClient.world.getPlayers()) {
                  if (playerEntity4.getUuid().equals(field13323)) {
                     playerEntity3 = playerEntity4;
                  }
               }

               if (playerEntity3 != null && EntityUtils.method2790(playerEntity3) <= 0.0F) {
                  field13314.clear();
                  field13323 = null;
               } else if (playerEntity3 != null || field13314.containsKey(field13323)) {
                  if (playerEntity3 != null) {
                     field13314.computeIfAbsent(field13323, TargetHudStyle.Nested0509::new).method3859(minecraftClient, playerEntity3, local6320_1);
                  }

                  field13314.keySet().removeIf(uUID -> !uUID.equals(field13323));
                  method3843(minecraftClient, local6320_1);
               }
            } else {
               field13314.clear();
               field13323 = null;
            }
         } else {
            field13323 = minecraftClient.player.getUuid();
            field13314.computeIfAbsent(field13323, TargetHudStyle.Nested0509::new).method3859(minecraftClient, minecraftClient.player, local6320_1);
            field13314.get(field13323).field13418 = "Example Player";
         }
      }
   }

   private static boolean method3841(ItemStack itemStack) {
      return method3854(itemStack) && method3849(itemStack) <= 0
         || itemStack.isOf(Items.ENCHANTED_GOLDEN_APPLE)
         || itemStack.isOf(Items.GOLDEN_APPLE)
         || itemStack.isOf(Items.GOLDEN_CARROT);
   }

   public static void method3842(Item item) {
      field13313.put(item, System.currentTimeMillis());
   }

   private static void method3843(MinecraftClient minecraftClient, long local6323_1) {
      TargetHudStyle.Nested0509 local6323_3 = field13323 == null ? null : field13314.get(field13323);
      if (local6323_3 != null) {
         for (Entity entity : minecraftClient.world.getEntities()) {
            if (entity instanceof ProjectileEntity projectileEntity && !field13315.contains(entity.getId()) && projectileEntity.getOwner() != null) {
               field13315.add(entity.getId());
               if (projectileEntity.getOwner().getUuid().equals(field13323)) {
                  if (entity instanceof ThrownItemEntity thrownItemEntity && !thrownItemEntity.getStack().isEmpty()) {
                     local6323_3.method3861(thrownItemEntity.getStack().getItem(), local6323_1);
                  } else if (entity.getType() == EntityType.WIND_CHARGE) {
                     local6323_3.method3861(Items.WIND_CHARGE, local6323_1);
                  } else if (entity.getType() == EntityType.SMALL_FIREBALL) {
                     local6323_3.method3861(Items.FIRE_CHARGE, local6323_1);
                  }
               }
            }
         }

         if (field13315.size() > 256) {
            field13315.clear();
         }
      }
   }

   static {
      field13312.put(Items.DRIED_KELP, 10);
      field13312.put(Items.NETHERITE_SCRAP, 10);
      field13312.put(Items.SNOWBALL, 20);
      field13312.put(Items.ENDER_EYE, 60);
      field13312.put(Items.FIRE_CHARGE, 60);
      field13312.put(Items.SUGAR, 60);
      field13312.put(Items.ENCHANTED_GOLDEN_APPLE, 150);
      field13312.put(Items.GOLDEN_APPLE, 30);
      field13312.put(Items.GOLDEN_CARROT, 30);
      field13311.put("minecraft:entity.illusioner.mirror_move", Items.ENDER_EYE);
      field13310.put("minecraft:entity.blaze.shoot", Items.FIRE_CHARGE);
      field13310.put("minecraft:entity.wind_charge.throw", Items.WIND_CHARGE);
   }

   public static void method3844(boolean local6325_0, boolean local6325_1) {
      TargetHudStyle.Nested0509 local6325_2 = field13323 == null ? null : field13314.get(field13323);
      if (local6325_2 != null) {
         Item item = local6325_0 ? Items.NETHERITE_SCRAP : Items.DRIED_KELP;
         if (System.currentTimeMillis() - field13313.getOrDefault(item, 0L) >= 2500L) {
            local6325_2.method3861(item, System.currentTimeMillis());
            TargetHudStyle.Nested0508 local6325_4 = local6325_2.field13416.get(item);
            if (local6325_4 != null) {
               local6325_4.field13413 = local6325_1 ? 15 : 10;
            }
         }
      }
   }

   public static void method3845(PlayerEntity playerEntity) {
      field13323 = playerEntity.getUuid();
   }

   private static float method3846(MinecraftClient minecraftClient, TargetHudStyle.Nested0509 local6327_1) {
      float local6327_2 = 0.0F;
      if (minecraftClient.player != null && local6327_1.field13421 != null) {
         double local6327_3 = local6327_1.field13421.x - minecraftClient.player.getX();
         double local6327_5 = local6327_1.field13421.z - minecraftClient.player.getZ();
         if (local6327_3 * local6327_3 + local6327_5 * local6327_5 >= 0.01) {
            local6327_2 = (float)Math.toDegrees(Math.atan2(local6327_5, local6327_3)) - 90.0F - RotationAngles.method4366();
         }
      }

      field13322.update();
      field13322.animateTo(field13322.floatValue() + MathHelper.wrapDegrees(local6327_2 - field13322.floatValue()), 0.25, Easings.EASE_OUT_QUART);
      return field13322.floatValue();
   }

   private static void method3847(TargetHudStyle.Nested0509 local6328_0, float local6328_1, float local6328_2, float local6328_3) {
      if (!(local6328_3 <= 0.001F) && !local6328_0.field13416.isEmpty()) {
         float local6328_4 = HudStyle.method4814(local6328_3);
         ArrayList<TargetHudStyle.Nested0508> local6328_5 = new ArrayList<>(local6328_0.field13416.values());

         for (int index = 0; index < local6328_5.size(); index++) {
            TargetHudStyle.Nested0508 local6328_7 = local6328_5.get(local6328_5.size() - 1 - index);
            float local6328_8 = local6328_1 + index * 15.0F - local6328_4;
            HudStyle.method4819(local6328_8, local6328_2, 13.0F, 13.0F, local6328_3, 3.0F);
            ItemRenderCache.method2336(
               local6328_7.field13409,
               Math.round((local6328_8 + 2.1F) * 2.0F) / 2.0F,
               Math.round((local6328_2 + 2.1F) * 2.0F) / 2.0F,
               8.8F,
               ItemDrawOptions.noDecorations(local6328_3).glint(GlintMode.OFF)
            );
            if (local6328_7.field13409.getCount() > 1) {
               FontText.method1397(
                  FontFace.SEMI_BOLD,
                  String.valueOf(local6328_7.field13409.getCount()),
                  local6328_8
                     + 13.0F
                     - 1.5F
                     - FontText.method1396(FontFace.SEMI_BOLD, String.valueOf(local6328_7.field13409.getCount()), 5.0F),
                  local6328_2 + 7.0F,
                  5.0F,
                  UiTheme.method2877(HudStyle.method4820(1.0F), local6328_3)
               );
            }

            int local6328_9 = local6328_7.method3858(System.currentTimeMillis());
            if (local6328_9 > 0) {
               String local6328_10 = local6328_9 > 99 ? "99" : String.valueOf(local6328_9);
               float local6328_11 = FontText.method1396(FontFace.SEMI_BOLD, local6328_10, 6.5F);
               FontText.method1397(
                  FontFace.SEMI_BOLD,
                  local6328_10,
                  local6328_8 + (13.0F - local6328_11) / 2.0F,
                  local6328_2 + 11.0F,
                  6.5F,
                  UiTheme.method2877(-50384, local6328_3 * 0.9F)
               );
            }
         }
      }
   }

   public static void method3848(PlayerEntity playerEntity, double local6329_1, double local6329_3, double local6329_5, String local6329_7) {
      if (field13323 != null && field13314.get(field13323) != null) {
         PlayerEntity playerEntity2 = null;

         for (PlayerEntity playerEntity3 : playerEntity.getEntityWorld().getPlayers()) {
            if (playerEntity3.getUuid().equals(field13323)) {
               playerEntity2 = playerEntity3;
            }
         }

         if (playerEntity2 != null) {
            Item item = field13311.get(local6329_7);
            if (item == null) {
               Item item2 = field13310.get(local6329_7);
               if (item2 != null) {
                  if (System.currentTimeMillis() - field13313.getOrDefault(item2, 0L) >= 2500L) {
                     if (!(playerEntity2.squaredDistanceTo(local6329_1, local6329_3, local6329_5) > 36.0)
                        && !(
                           playerEntity2.squaredDistanceTo(local6329_1, local6329_3, local6329_5)
                              > playerEntity.squaredDistanceTo(local6329_1, local6329_3, local6329_5)
                        )) {
                        field13314.get(field13323).method3861(item2, System.currentTimeMillis());
                     }
                  }
               }
            } else if (System.currentTimeMillis() - field13313.getOrDefault(item, 0L) >= 2500L) {
               if (!(playerEntity.squaredDistanceTo(local6329_1, local6329_3, local6329_5) > 36.0)
                  && !(
                     playerEntity.squaredDistanceTo(local6329_1, local6329_3, local6329_5)
                        > playerEntity2.squaredDistanceTo(local6329_1, local6329_3, local6329_5)
                  )) {
                  field13314.get(field13323).method3861(item, System.currentTimeMillis());
               }
            }
         }
      }
   }

   private static int method3849(ItemStack itemStack) {
      for (ServerHelperModule.Nested0360 local6330_2 : ServerHelperModule.method0707().method0690()) {
         if (local6330_2.getKeybindSetting().isVisible() && local6330_2.method0712(itemStack)) {
            return 3;
         }
      }

      return 0;
   }

   public static void method3850(MinecraftClient minecraftClient, float local6331_1, float local6331_2) {
      method3840(minecraftClient);
      TargetHudStyle.Nested0509 local6331_3 = field13323 == null ? null : field13314.get(field13323);
      if (local6331_3 != null) {
         field13328 = local6331_3;
      }

      field13317.update();
      field13317.animateTo(local6331_3 != null ? 1.0 : 0.0, 0.6, Easings.EASE_OUT_QUART);
      field13321.update();
      field13321.animateTo(local6331_3 != null && !local6331_3.field13416.isEmpty() ? 1.0 : 0.0, 0.6, Easings.EASE_OUT_QUART);
      field13316.update();
      field13316.animateTo(
         local6331_3 == null && !(field13317.floatValue() > 0.001F) && !(field13321.floatValue() > 0.001F) ? 0.0 : 1.0, 0.4, Easings.EASE_OUT_QUART
      );
      if (field13328 != null) {
         TargetHudStyle.Nested0509 local6331_4 = field13328;
         float local6331_5 = method3857(local6331_4);
         float targetValue = method3855(local6331_4);
         float local6331_7 = Math.max(local6331_5 + 2.0F + targetValue, local6331_4.field13416.size() * 15.0F - 2.0F);
         float targetValue2 = Math.max(Math.max(HudStyle.method4823("Target"), local6331_7), 1.0F);
         field13318.update();
         field13318.animateTo(targetValue2, 0.6, Easings.EASE_OUT_QUART);
         field13319.update();
         field13319.animateTo(targetValue2, 0.6, Easings.EASE_OUT_QUART);
         field13320.update();
         field13320.animateTo(targetValue, 0.6, Easings.EASE_OUT_QUART);
         float local6331_9 = field13318.floatValue();
         float local6331_10 = Math.max(13.0F + 14.0F * field13317.floatValue() + 17.0F * field13321.floatValue(), 1.0F);
         float local6331_11 = field13316.floatValue();
         if (!(local6331_11 <= 0.001F)) {
            float[] floatArray = HudManager.method3390("Target", local6331_1, local6331_2);
            HudManager.method3418("Target", floatArray[0], floatArray[1], local6331_9, local6331_10);
            HudRenderContext.method4341(floatArray[0] + local6331_9 / 2.0F, floatArray[1] + local6331_10 / 2.0F, local6331_11);
            HudStyle.method4824(
               floatArray[0], floatArray[1], Math.max(field13319.floatValue(), 1.0F), local6331_11, HudIcons.ENVIRONMENT, "Target"
            );
            method3856(minecraftClient, local6331_4, floatArray[0], floatArray[1] + 15.0F, local6331_5, targetValue, field13317.floatValue() * local6331_11);
            method3847(local6331_4, floatArray[0], floatArray[1] + 15.0F + 13.0F * field13317.floatValue() - 1.0F, field13321.floatValue() * local6331_11);
            HudRenderContext.method4337();
         }
      }
   }

   private static int method3852(TargetHudStyle.Nested0509 local6333_0) {
      int local6333_1 = 0;

      for (int index = 0; index < 4; index++) {
         if (!local6333_0.field13417[index].isEmpty()) {
            local6333_1++;
         }
      }

      return local6333_1;
   }

   private static int method3853(ItemStack itemStack) {
      return method3854(itemStack) ? 15 : field13312.getOrDefault(itemStack.getItem(), 0);
   }

   private static boolean method3854(ItemStack itemStack) {
      for (StatusEffectInstance statusEffectInstance : itemStack.getOrDefault(DataComponentTypes.POTION_CONTENTS, PotionContentsComponent.DEFAULT).getEffects()) {
         if (statusEffectInstance.getEffectType().getIdAsString().equals("minecraft:instant_health")) {
            return true;
         }
      }

      return false;
   }

   private static float method3855(TargetHudStyle.Nested0509 local6336_0) {
      return HudStyle.method4822(FontText.method1396(FontFace.SEMI, (int)local6336_0.field13420 + "HP", 6.0F));
   }

   private static void method3856(
      MinecraftClient minecraftClient,
      TargetHudStyle.Nested0509 local6337_1,
      float local6337_2,
      float local6337_3,
      float local6337_4,
      float local6337_5,
      float local6337_6
   ) {
      if (!(local6337_6 <= 0.001F)) {
         float local6337_7 = HudStyle.method4814(local6337_6);
         float local6337_8 = local6337_2 - local6337_7;
         float local6337_9 = local6337_2 + field13320.floatValue() + 2.0F - local6337_7;
         local6337_4 = Math.max(local6337_4, field13319.floatValue() - field13320.floatValue() - 2.0F);
         float local6337_10 = HudStyle.method4816(local6337_3);
         float local6337_11 = Math.round((local6337_3 + 1.5F) * 2.0F) / 2.0F;
         HudStyle.method4821(local6337_9, local6337_3, local6337_4, 11.0F, local6337_6);
         if (local6337_1.field13419 != null) {
            Render2D.method4677(
               local6337_1.field13419.toString(),
               Math.round((local6337_9 + 2.0F) * 2.0F) / 2.0F,
               local6337_11,
               8.0F,
               8.0F,
               3.0F,
               0.25F,
               0.125F,
               0.125F,
               0.25F,
               0.25F,
               UiTheme.method2877(-1, local6337_6)
            );
         }

         FontText.method1397(
            FontFace.SEMI,
            local6337_1.field13418,
            local6337_9 + 12.5F,
            local6337_10,
            6.0F,
            UiTheme.method2877(HudStyle.method4820(1.0F), local6337_6)
         );
         float local6337_12 = local6337_9 + 12.5F + FontText.method1396(FontFace.SEMI, local6337_1.field13418, 6.0F) + 3.0F;

         for (int index = 0; index < 4; index++) {
            if (!local6337_1.field13417[index].isEmpty()) {
               ItemRenderCache.method2336(
                  local6337_1.field13417[index],
                  Math.round((local6337_12 - 0.5F) * 2.0F) / 2.0F,
                  local6337_11,
                  8.0F,
                  ItemDrawOptions.noDecorations(local6337_6).glint(GlintMode.OFF)
               );
               local6337_12 += 7.0F;
            }
         }

         HudStyle.method4821(local6337_8, local6337_3, local6337_5, 11.0F, local6337_6);
         FontText.method1397(
            FontFace.SEMI,
            (int)local6337_1.field13420 + "HP",
            local6337_8 + 4.0F,
            local6337_10,
            6.0F,
            UiTheme.method2877(HudStyle.method4820(1.0F), local6337_6)
         );
      }
   }

   private TargetHudStyle() {
   }

   private static float method3857(TargetHudStyle.Nested0509 local6339_0) {
      int local6339_1 = method3852(local6339_0);
      return 12.5F
         + FontText.method1396(FontFace.SEMI, local6339_0.field13418, 6.0F)
         + (local6339_1 > 0 ? 3.0F + local6339_1 * 7.0F - 1.0F : 0.0F)
         + 4.0F;
   }

   private static final class Nested0508 {
      final ItemStack field13409;
      long field13410;
      long field13411;
      int field13412;
      int field13413;

      int method3858(long local6340_1) {
         int local6340_3 = this.field13413 > 0 ? this.field13413 : TargetHudStyle.method3853(this.field13409);
         return local6340_3 != 0 && this.field13410 != 0L ? Math.max((int)Math.ceil((local6340_3 * 1000L - (local6340_1 - this.field13410)) / 1000.0), 0) : 0;
      }

      Nested0508(ItemStack itemStack, int local6341_2) {
         this.field13409 = itemStack.copy();
         this.field13409.setDamage(0);
         this.field13412 = local6341_2;
      }
   }

   private static final class Nested0509 {
      final LinkedHashMap<Item, TargetHudStyle.Nested0508> field13416 = new LinkedHashMap<>();
      final ItemStack[] field13417 = new ItemStack[6];
      String field13418 = "";
      Identifier field13419;
      float field13420;
      Vec3d field13421;
      boolean field13422;
      int field13423;
      ItemStack field13424 = ItemStack.EMPTY;

      void method3859(MinecraftClient minecraftClient, PlayerEntity playerEntity, long local6342_3) {
         this.field13418 = playerEntity.getName().getString();
         if (playerEntity instanceof AbstractClientPlayerEntity abstractClientPlayerEntity) {
            this.field13419 = abstractClientPlayerEntity.getSkin().body().texturePath();
         }

         this.field13420 = EntityUtils.method2790(playerEntity);
         this.field13421 = playerEntity.getEntityPos();

         for (int index = 0; index < 4; index++) {
            this.field13417[index] = playerEntity.getEquippedStack(TargetHudStyle.field13309[index]);
         }

         for (TargetHudStyle.Nested0508 local6342_6 : this.field13416.values()) {
            if (local6342_6.field13411 == 0L
               && local6342_6.field13409.getCount() == 1
               && this.field13417[local6342_6.field13412].isOf(local6342_6.field13409.getItem())
               && (local6342_6.field13412 == 4 ? playerEntity.getMainHandStack() : playerEntity.getOffHandStack()).isEmpty()) {
               local6342_6.field13411 = local6342_3;
            }
         }

         this.field13417[4] = playerEntity.getMainHandStack();
         this.field13417[5] = playerEntity.getOffHandStack();
         if (playerEntity.isUsingItem() && !playerEntity.getActiveItem().isEmpty()) {
            if (playerEntity.getItemUseTimeLeft() <= 1 && !this.field13422) {
               this.field13422 = true;
               this.method3863(playerEntity.getActiveItem().copyWithCount(1), local6342_3);
            }
         } else {
            this.field13422 = false;
         }

         for (int index2 = 4; index2 < 6; index2++) {
            if (TargetHudStyle.method3849(this.field13417[index2]) > 0 || TargetHudStyle.method3841(this.field13417[index2])) {
               this.method3860(minecraftClient, playerEntity, this.field13417[index2], index2, local6342_3);
            }
         }

         this.field13416
            .values()
            .removeIf(local6348_2 -> local6348_2.field13411 != 0L && local6342_3 - local6348_2.field13411 > 10000L && local6348_2.method3858(local6342_3) <= 0);
      }

      Nested0509(UUID uUID) {
         Arrays.fill(this.field13417, ItemStack.EMPTY);
      }

      private void method3860(MinecraftClient minecraftClient, PlayerEntity playerEntity, ItemStack itemStack, int local6344_4, long local6344_5) {
         TargetHudStyle.Nested0508 local6344_7 = this.field13416.remove(itemStack.getItem());
         if (local6344_7 != null) {
            if (itemStack.getCount() < local6344_7.field13409.getCount()
               && TargetHudStyle.method3853(itemStack) > 0
               && minecraftClient.world
                  .getEntitiesByClass(
                     ItemEntity.class, playerEntity.getBoundingBox().expand(2.0), itemEntity -> itemEntity.getStack().isOf(itemStack.getItem())
                  )
                  .isEmpty()) {
               local6344_7.field13410 = local6344_5;
            }

            local6344_7.field13409.setCount(itemStack.getCount());
            local6344_7.field13412 = local6344_4;
            local6344_7.field13411 = 0L;
            this.field13416.put(itemStack.getItem(), local6344_7);
         } else {
            if (this.field13416.size() >= 9) {
               for (Item item : new ArrayList<>(this.field13416.keySet())) {
                  if (TargetHudStyle.method3849(this.field13416.get(item).field13409) < TargetHudStyle.method3849(itemStack)) {
                     this.field13416.remove(item);
                     break;
                  }
               }

               if (this.field13416.size() >= 9) {
                  return;
               }
            }

            this.field13416.put(itemStack.getItem(), new TargetHudStyle.Nested0508(itemStack, local6344_4));
         }
      }

      void method3861(Item item, long local6345_2) {
         this.method3863(new ItemStack(item), local6345_2);
      }

      void method3863(ItemStack itemStack, long local6347_2) {
         if (TargetHudStyle.method3854(itemStack) && TargetHudStyle.method3849(itemStack) <= 0
            || itemStack.isOf(Items.ENCHANTED_GOLDEN_APPLE)
            || itemStack.isOf(Items.GOLDEN_APPLE)
            || itemStack.isOf(Items.GOLDEN_CARROT)
            || TargetHudStyle.method3849(itemStack) > 0
               && !itemStack.isOf(Items.POTION)
               && !itemStack.isOf(Items.SPLASH_POTION)
               && !itemStack.isOf(Items.LINGERING_POTION)) {
            TargetHudStyle.Nested0508 local6347_4 = this.field13416.remove(itemStack.getItem());
            if (local6347_4 == null) {
               local6347_4 = new TargetHudStyle.Nested0508(itemStack, 4);
            } else if (TargetHudStyle.method3841(local6347_4.field13409) && local6347_4.field13409.getCount() > 1) {
               local6347_4.field13409.setCount(local6347_4.field13409.getCount() - 1);
            }

            local6347_4.field13410 = local6347_2;
            local6347_4.field13411 = local6347_2;
            if (this.field13416.size() >= 9) {
               this.field13416.remove(this.field13416.keySet().iterator().next());
            }

            this.field13416.put(itemStack.getItem(), local6347_4);
         }
      }
   }
}
