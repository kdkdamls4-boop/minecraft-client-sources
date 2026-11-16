package net.skycore.client.module.player;

import net.minecraft.client.Minecraft;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemCooldowns;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.skycore.client.SkycoreClient;
import net.skycore.client.event.KeyEvent;
import net.skycore.client.event.MouseButtonEvent;
import net.skycore.client.module.ClientModule;
import net.skycore.client.module.ModuleCategory;
import net.skycore.client.setting.KeyBindSetting;
import net.skycore.client.util.ScheduleHelper;
import net.skycore.client.setting.BooleanSetting;
import net.skycore.client.util.UseItemHelper;

public final class ClickPearlModule extends ClientModule {
   public static final ClickPearlModule INSTANCE = new ClickPearlModule();
   private boolean pearlRequested;
   public final BooleanSetting legit;
   public final KeyBindSetting clickKey = this.add(new KeyBindSetting("Throw key", 1002));

   @Override
   protected void onDisable() {
      this.pearlRequested = false;
   }

   private void onKey(KeyEvent keyEvent) {
      int value = 0;
      value = (byte)0;
      if (this.isEnabled() && keyEvent.pressed()) {
         Minecraft minecraft = keyEvent.client();
         if (minecraft.gui.screen() == null) {
            net.minecraft.client.input.KeyEvent keyEvent2 = keyEvent.key();
            value = keyEvent2.key();
            if (value == this.clickKey.get()) {
               this.requestPearl();
            }

            return;
         }
      }
   }

   private void requestPearl() {
      Minecraft instance = Minecraft.getInstance();
      LocalPlayer player = instance.player;
      if (player != null && instance.level != null) {
         if (!this.pearlRequested) {
            ItemCooldowns cooldowns = player.getCooldowns();
            ItemStack defaultInstance = Items.ENDER_PEARL.getDefaultInstance();
            if (cooldowns.isOnCooldown(defaultInstance)) {
               return;
            }

            if (UseItemHelper.getItemSlot(Items.ENDER_PEARL) == -1) {
               return;
            }

            this.pearlRequested = true;
            ScheduleHelper.schedule(() -> {
               if (this.isEnabled()) {
                  if (this.pearlRequested) {
                     UseItemHelper.inventorySwapClick(Items.ENDER_PEARL);
                     this.pearlRequested = false;
                     return;
                  }
               }
            });
            return;
         }
      }
   }

   private ClickPearlModule() {
      super("Click Pearl", "Throws an ender pearl on keybind", ModuleCategory.PLAYER);
      this.legit = this.add(new BooleanSetting("Legit", false));
      SkycoreClient.events().subscribe(KeyEvent.class, this::onKey);
      SkycoreClient.events().subscribe(MouseButtonEvent.class, this::onMouse);
   }

   public boolean isBusy() {
      if (this.isEnabled()) {
         if (this.pearlRequested) {
            return true;
         }
      }

      return false;
   }

   private void onMouse(MouseButtonEvent mouseButtonEvent) {
      if (this.isEnabled() && mouseButtonEvent.pressed()) {
         if (mouseButtonEvent.client().gui.screen() == null) {
            MouseButtonInfo mouseButtonInfo = mouseButtonEvent.button();
            int computedValue = 1000 + mouseButtonInfo.button();
            Integer value = this.clickKey.get();
            if (computedValue == value) {
               this.requestPearl();
            }

            return;
         }
      }
   }

   public void cancelPendingAction() {
      this.pearlRequested = false;
   }
}
