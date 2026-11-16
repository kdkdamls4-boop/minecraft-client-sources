package net.skycore.client.module.misc;

import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.Items;
import net.skycore.client.module.ClientModule;
import net.skycore.client.module.ModuleCategory;
import net.skycore.client.util.SliderSetting;

public final class AutoJoinerModule extends ClientModule {
   private int wait;
   public static final AutoJoinerModule INSTANCE = new AutoJoinerModule();
   private final SliderSetting grief = this.add(new SliderSetting("Grief", 5.0, 1.0, 76.0, 1.0));

   @Override
   protected void onTick() {
      Minecraft instance = Minecraft.getInstance();
      if (instance.player != null && instance.gameMode != null && this.wait-- <= 0) {
         AbstractContainerMenu abstractContainerMenu = instance.player.containerMenu;
         if (abstractContainerMenu != instance.player.inventoryMenu) {
            String text = "ГРИФ #" + this.grief.get().intValue();

            for (int value = 0; value < abstractContainerMenu.slots.size(); value++) {
               if (abstractContainerMenu.getSlot(value).getItem().getHoverName().getString().toUpperCase().contains(text)) {
                  instance.gameMode.handleContainerInput(abstractContainerMenu.containerId, value, 0, ContainerInput.PICKUP, instance.player);
                  this.wait = 20;
                  return;
               }
            }
         } else {
            for (int value2 = 0; value2 < 9; value2++) {
               if (instance.player.getInventory().getItem(value2).is(Items.COMPASS)) {
                  instance.player.getInventory().setSelectedSlot(value2);
                  instance.gameMode.useItem(instance.player, InteractionHand.MAIN_HAND);
                  this.wait = 20;
                  return;
               }
            }
         }
      }
   }

   @Override
   protected void onEnable() {
      this.wait = 0;
   }

   private AutoJoinerModule() {
      super("Auto Joiner", "Automatically joins a selected grief server", ModuleCategory.MISC);
   }
}
