package net.skycore.client.module.combat;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.skycore.client.command.FakeplayerCommand;
import net.skycore.client.module.ClientModule;
import net.skycore.client.module.ModuleCategory;
import net.skycore.client.setting.ModeSetting;
import net.skycore.client.util.FinishedHelper;
import net.skycore.client.setting.BooleanSetting;

public final class AntiBotModule extends ClientModule {
   private final ModeSetting mode = this.add(new ModeSetting("Bypass", "ReallyWorld", "ReallyWorld", "Matrix", "CakeWorld"));
   private static final EquipmentSlot[] ARMOR = new EquipmentSlot[]{EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD};
   public static final AntiBotModule INSTANCE = new AntiBotModule();
   private final BooleanSetting cakeAirborneOnly = this.add(new BooleanSetting("Airborne Only", true));
   private final FinishedHelper timer = new FinishedHelper();
   public static final List<Player> bots = new ArrayList<>();

   public static boolean isBot(Player player) {
      if (player != null) {
         if (!FakeplayerCommand.isFake(player)) {
            if (INSTANCE.isEnabled()) {
               if (bots.contains(player)) {
                  return true;
               }
            }

            return false;
         }
      }

      return false;
   }

   private boolean isNaked(Player player) {
      return player.getArmorValue() == 0;
   }

   @Override
   protected void onTick() {
      Minecraft instance = Minecraft.getInstance();
      LocalPlayer player = instance.player;
      if (player != null && instance.level != null) {
         if (this.timer.finished(10000L) && !bots.isEmpty()) {
            bots.clear();
            this.timer.reset();
         }

         for (Player player2 : instance.level.players()) {
            if (player2 != null && player2 != player) {
               boolean condition = false;
               if (this.mode.is("ReallyWorld")) {
                  condition = this.reallyWorldCheck(player2);
               } else if (this.mode.is("Matrix")) {
                  String string = player2.getName().getString();
                  UUID bytes = UUID.nameUUIDFromBytes(("OfflinePlayer:" + string).getBytes(StandardCharsets.UTF_8));
                  condition = !player2.getUUID().equals(bytes);
               } else if (this.mode.is("CakeWorld")) {
                  condition = this.cakeWorldCheck(player2);
               }

               if (condition) {
                  if (!bots.contains(player2)) {
                     bots.add(player2);
                  }
               } else if (this.mode.is("ReallyWorld") || this.mode.is("CakeWorld")) {
                  bots.remove(player2);
               }
            }
         }
      }
   }

   @Override
   protected void onDisable() {
      bots.clear();
   }

   private boolean isIronArmor(ItemStack itemStack) {
      if (!itemStack.is(Items.IRON_HELMET)) {
         if (!itemStack.is(Items.IRON_CHESTPLATE)) {
            if (!itemStack.is(Items.IRON_LEGGINGS)) {
               if (!itemStack.is(Items.IRON_BOOTS)) {
                  return false;
               }
            }
         }
      }

      return true;
   }

   private ItemStack getArmor(Player player, int value) {
      EquipmentSlot equipmentSlot = ARMOR[value];
      return player.getItemBySlot(equipmentSlot);
   }

   private AntiBotModule() {
      super("Anti Bot", "Filters out bots", ModuleCategory.COMBAT);
      this.cakeAirborneOnly.visibleWhen(() -> {
         return this.mode.is("CakeWorld");
      });
   }

   private boolean isInvisible(Player player) {
      if (!player.isInvisible()) {
         if (!player.hasEffect(MobEffects.INVISIBILITY)) {
            return false;
         }
      }

      return true;
   }

   private boolean isLeatherArmor(ItemStack itemStack) {
      if (!itemStack.is(Items.LEATHER_HELMET)) {
         if (!itemStack.is(Items.LEATHER_CHESTPLATE)) {
            if (!itemStack.is(Items.LEATHER_LEGGINGS)) {
               if (!itemStack.is(Items.LEATHER_BOOTS)) {
                  return false;
               }
            }
         }
      }

      return true;
   }

   private boolean cakeWorldCheck(Player player) {
      if (this.isNaked(player)) {
         if (this.isInvisible(player)) {
            if (!this.cakeAirborneOnly.get()) {
               return true;
            }

            if (!player.onGround() && !player.isInWater()) {
               if (!player.onClimbable()) {
                  if (!player.isPassenger() && !player.isFallFlying()) {
                     return true;
                  }
               }
            }

            return false;
         }
      }

      return false;
   }

   @Override
   protected void onEnable() {
      bots.clear();
   }

   private boolean reallyWorldCheck(Player player) {
      byte flag = 0;

      for (int value = 0; value < 4; value++) {
         ItemStack armor = this.getArmor(player, value);
         if (!armor.isEmpty()) {
            boolean leatherArmor = this.isLeatherArmor(armor);
            boolean ironArmor = this.isIronArmor(armor);
            if (leatherArmor || ironArmor) {
               flag = 1;
            }
         }
      }

      byte flag2 = 1;

      for (int value2 = 0; value2 < 4; value2++) {
         ItemStack armor2 = this.getArmor(player, value2);
         if (!armor2.isEmpty() && armor2.isEnchanted()) {
            flag2 = 0;
            break;
         }
      }

      byte flag3 = 1;

      for (int value3 = 0; value3 < 4; value3++) {
         ItemStack armor3 = this.getArmor(player, value3);
         if (!armor3.isEmpty() && armor3.isDamaged()) {
            flag3 = 0;
            break;
         }
      }

      byte flag4 = 1;

      for (int value4 = 0; value4 < 4; value4++) {
         ItemStack armor4 = this.getArmor(player, value4);
         if (!armor4.isEmpty() && this.isLeatherArmor(armor4) && armor4.has(DataComponents.DYED_COLOR)) {
            flag4 = 0;
            break;
         }
      }

      boolean empty = player.getOffhandItem().isEmpty();
      int foodData = player.getFoodData().getFoodLevel() == 20 ? 1 : 0;
      return flag != 0 && flag2 != 0 && flag3 != 0 && flag4 != 0 && empty && foodData != 0;
   }
}
