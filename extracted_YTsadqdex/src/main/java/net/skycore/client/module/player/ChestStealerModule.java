package net.skycore.client.module.player;

import java.util.ArrayList;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.client.Minecraft;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.HopperMenu;
import net.minecraft.world.inventory.Slot;
import net.skycore.client.module.ClientModule;
import net.skycore.client.module.ModuleCategory;
import net.skycore.client.util.SliderSetting;
import net.skycore.client.setting.BooleanSetting;

public final class ChestStealerModule extends ClientModule {
   private long last;
   private final BooleanSetting random;
   private final SliderSetting delay = this.add(new SliderSetting("Delay (ms)", 100.0, 0.0, 1000.0, 1.0));
   public static final ChestStealerModule INSTANCE = new ChestStealerModule();

   private ChestStealerModule() {
      super("Chest Stealer", "Takes items from an open container", ModuleCategory.PLAYER);
      this.random = this.add(new BooleanSetting("Random Order", false));
   }

   @Override
   protected void onDisable() {
      this.last = 0L;
   }

   @Override
   protected void onTick() {
      Minecraft instance = Minecraft.getInstance();
      if (instance.player != null && instance.gameMode != null && !(System.currentTimeMillis() - this.last < this.delay.get())) {
         AbstractContainerMenu abstractContainerMenu = instance.player.containerMenu;
         int container = abstractContainerMenu instanceof ChestMenu chestMenu ? chestMenu.getContainer().getContainerSize() : (abstractContainerMenu instanceof HopperMenu ? 5 : 0);
         if (container != 0) {
            ArrayList elements = new ArrayList();

            for (int value = 0; value < container; value++) {
               if (abstractContainerMenu.getSlot(value).hasItem()) {
                  elements.add(abstractContainerMenu.getSlot(value));
               }
            }

            if (!elements.isEmpty()) {
               Slot first = this.random.get() ? (Slot)elements.get(ThreadLocalRandom.current().nextInt(elements.size())) : (Slot)elements.getFirst();
               instance.gameMode.handleContainerInput(abstractContainerMenu.containerId, first.index, 0, ContainerInput.QUICK_MOVE, instance.player);
               this.last = System.currentTimeMillis();
            }
         }
      }
   }
}
