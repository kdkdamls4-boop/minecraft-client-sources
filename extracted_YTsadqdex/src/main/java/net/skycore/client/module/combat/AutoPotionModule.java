package net.skycore.client.module.combat;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.skycore.client.SkycoreClient;
import net.skycore.client.event.PlayerTickEndEvent;
import net.skycore.client.module.ClientModule;
import net.skycore.client.module.ModuleCategory;
import net.skycore.client.setting.NumberSetting;
import net.skycore.client.setting.BooleanSetting;
import net.skycore.client.util.UseItemHelper;

public final class AutoPotionModule extends ClientModule {
   private final NumberSetting healHealth;
   private final BooleanSetting healing;
   private int cooldownTicks;
   private final BooleanSetting speed;
   private final BooleanSetting strength = this.add(new BooleanSetting("Strength", true));
   public static final AutoPotionModule INSTANCE = new AutoPotionModule();
   private int busyTicks;
   private final BooleanSetting fireResistance;

   private boolean isSplashWithEffect(ItemStack itemStack, Holder<MobEffect> holder) {
      if (itemStack != null && itemStack.is(Items.SPLASH_POTION)) {
         PotionContents potionContents = itemStack.get(DataComponents.POTION_CONTENTS);
         if (potionContents == null) {
            return false;
         }

         var allEffects = potionContents.getAllEffects();

         for (MobEffectInstance mobEffectInstance : allEffects) {
            if (mobEffectInstance.is(holder)) {
               return true;
            }
         }

         return false;
      } else {
         return false;
      }
   }

   private List<Holder<MobEffect>> collectNeededEffects(LocalPlayer player) {
      ArrayList elements = new ArrayList();
      Boolean get = this.strength.get();
      if (get) {
         if (!player.hasEffect(MobEffects.STRENGTH)) {
            elements.add(MobEffects.STRENGTH);
         }
      }

      if (this.speed.get()) {
         if (!player.hasEffect(MobEffects.SPEED)) {
            elements.add(MobEffects.SPEED);
         }
      }

      Boolean get2 = this.fireResistance.get();
      if (get2 && !player.hasEffect(MobEffects.FIRE_RESISTANCE)) {
         elements.add(MobEffects.FIRE_RESISTANCE);
      }

      if (this.healing.get()) {
         float health = player.getHealth();
         double absorptionAmount = health + player.getAbsorptionAmount();
         if (absorptionAmount <= this.healHealth.get()) {
            elements.add(MobEffects.INSTANT_HEALTH);
         }
      }

      return elements;
   }

   private AutoPotionModule() {
      super("Auto Potion", "Automatically throws splash potions", ModuleCategory.COMBAT);
      this.speed = this.add(new BooleanSetting("Speed", true));
      this.fireResistance = this.add(new BooleanSetting("Fire Resistance", true));
      this.healing = this.add(new BooleanSetting("Healing", true));
      this.healHealth = this.add(new NumberSetting("Heal Health", 14.0, 1.0, 20.0, 0.5));
      this.healHealth.visibleWhen(this.healing::get);
      SkycoreClient.events().subscribe(PlayerTickEndEvent.class, this::onUpdate);
   }

   private boolean findAndThrowPotion(Holder<MobEffect> holder) {
      LocalPlayer instance = Minecraft.getInstance().player;
      if (instance == null) {
         return false;
      }

      for (int value = 0; value < 36; value++) {
         Inventory inventory = instance.getInventory();
         ItemStack item = inventory.getItem(value);
         if (this.isSplashWithEffect(item, holder)) {
            return this.throwPotion(value);
         }
      }

      return false;
   }

   private boolean throwPotion(int slot) {
      LocalPlayer instance = Minecraft.getInstance().player;
      if (instance != null && instance.connection != null) {
         int selectedSlot = instance.getInventory().getSelectedSlot();
         if (slot < 9) {
            UseItemHelper.switchToHotbarSlot(slot);
            instance.connection.send(new ServerboundUseItemPacket(InteractionHand.MAIN_HAND, 0, instance.getYRot(), 90.0F));
            UseItemHelper.switchToHotbarSlot(selectedSlot);
            return true;
         } else {
            int slot2 = selectedSlot;
            int slot3 = UseItemHelper.toMenuSlot(slot);
            UseItemHelper.clickSlotId(slot3, slot2, ContainerInput.SWAP, false);
            instance.connection.send(new ServerboundSetCarriedItemPacket(slot2));
            instance.connection.send(new ServerboundUseItemPacket(InteractionHand.MAIN_HAND, 0, instance.getYRot(), 90.0F));
            UseItemHelper.clickSlotId(slot3, slot2, ContainerInput.SWAP, false);
            return true;
         }
      } else {
         return false;
      }
   }

   @Override
   protected void onDisable() {
      this.cooldownTicks = 0;
      this.busyTicks = 0;
   }

   private void onUpdate(PlayerTickEndEvent playerTickEndEvent) {
      if (this.isEnabled() && (this.busyTicks <= 0 || --this.busyTicks < 0)) {
         Minecraft instance = Minecraft.getInstance();
         LocalPlayer player = playerTickEndEvent.player();
         if (player != null
            && instance.level != null
            && instance.gameMode != null
            && instance.gui.screen() == null
            && !player.isSpectator()
            && player.connection != null
            && this.cooldownTicks <= 0) {
            if (!SkycoreClient.modules().get(AutoTotemModule.class).map(AutoTotemModule::needsTotem).orElse(false)) {
               var neededEffects = this.collectNeededEffects(player);
               if (!neededEffects.isEmpty()) {
                  for (Holder holder : neededEffects) {
                     if (this.findAndThrowPotion(holder)) {
                        this.cooldownTicks = 15;
                        this.busyTicks = 4;
                        return;
                     }
                  }
               }
            }
         } else {
            if (this.cooldownTicks > 0) {
               this.cooldownTicks--;
            }
         }
      }
   }
}
