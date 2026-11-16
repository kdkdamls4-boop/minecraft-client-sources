package net.skycore.client.module.player;

import it.unimi.dsi.fastutil.objects.Object2IntMap.Entry;
import java.util.Optional;
import java.util.function.Function;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.skycore.client.SkycoreClient;
import net.skycore.client.event.PlayerTickEndEvent;
import net.skycore.client.mixin.HudSetOverlayMessageTimeAccessor;
import net.skycore.client.module.ClientModule;
import net.skycore.client.module.ModuleCategory;
import net.skycore.client.util.SpoofElytraAsChestplateHelper;

public final class AutoToolModule extends ClientModule {
   private boolean swapping;
   private int oldSlot = -1;
   public static final AutoToolModule INSTANCE = new AutoToolModule();
   private int suppressTicks;
   private int postRestoreTicks;
   private int visualSlot = -1;

   private void restoreSlot(Minecraft minecraft) {
      int oldSlot2 = this.oldSlot;
      this.swapToSlot(oldSlot2);
      if (oldSlot2 >= 0 && oldSlot2 < 9) {
         this.visualSlot = oldSlot2;
         this.postRestoreTicks = 10;
         SpoofElytraAsChestplateHelper.hold(oldSlot2, 10);
      } else {
         this.visualSlot = -1;
      }

      this.suppressTicks = 6;
      suppressHeldItemTooltip(minecraft);
      this.oldSlot = -1;
      this.swapping = false;
   }

   @Override
   protected void onDisable() {
      this.oldSlot = -1;
      this.swapping = false;
      this.visualSlot = -1;
      this.postRestoreTicks = 0;
      this.suppressTicks = Math.max(this.suppressTicks, 4);
      Minecraft instance = Minecraft.getInstance();
      suppressHeldItemTooltip(instance);
      super.onDisable();
   }

   private static int getEfficiencyLevel(ItemStack itemStack) {
      ItemEnchantments enchantmentsForCrafting = EnchantmentHelper.getEnchantmentsForCrafting(itemStack);

      for (Entry entry : enchantmentsForCrafting.entrySet()) {
         var key = ((Holder<net.minecraft.world.item.enchantment.Enchantment>)entry.getKey()).unwrapKey();
         Function<net.minecraft.resources.ResourceKey, java.lang.String> path = resourceKey -> {
            return resourceKey.identifier().getPath();
         };
         String text = key.<String>map(path).orElse("");
         if ("efficiency".equals(text)) {
            return entry.getIntValue();
         }
      }

      return 0;
   }

   public static int getVisualSlot() {
      if (!INSTANCE.isEnabled()) {
         return -1;
      }

      return INSTANCE.visualSlot;
   }

   private int findEfficiencyShearsSlot(int index) {
      Minecraft instance = Minecraft.getInstance();
      if (instance.player == null) {
         return -1;
      }

      int index2 = -1;
      int index3 = index - 1;

      for (int value = 0; value < 9; value++) {
         ItemStack item = instance.player.getInventory().getItem(value);
         if (item.is(Items.SHEARS)) {
            int efficiencyLevel = getEfficiencyLevel(item);
            if (efficiencyLevel > index3) {
               index3 = efficiencyLevel;
               index2 = value;
            }
         }
      }

      return index2;
   }

   private AutoToolModule() {
      super("AutoTool", "Берёт из хотбара самый быстрый предмет для ломания", ModuleCategory.PLAYER);
      SkycoreClient.events().subscribe(PlayerTickEndEvent.class, this::onPreTick);
   }

   private void onPreTick(PlayerTickEndEvent event) {
      Minecraft instance = Minecraft.getInstance();
      if (this.isEnabled() && instance.player != null && instance.level != null) {
         if (this.suppressTicks > 0) {
            this.suppressTicks--;
            suppressHeldItemTooltip(instance);
         }

         if (this.postRestoreTicks > 0) {
            this.postRestoreTicks--;
            suppressHeldItemTooltip(instance);
            if (this.postRestoreTicks == 0) {
               this.visualSlot = -1;
            }
         }

         if (instance.hitResult instanceof BlockHitResult blockHitResult && instance.options.keyAttack.isDown()) {
            BlockState blockState = instance.level.getBlockState(blockHitResult.getBlockPos());
            int bestSlot = this.findBestSlot(blockState);
            if (bestSlot != -1) {
               this.swapping = true;
               if (this.oldSlot == -1) {
                  this.oldSlot = instance.player.getInventory().getSelectedSlot();
               }

               if (bestSlot != this.oldSlot) {
                  int value = this.visualSlot != this.oldSlot ? 1 : 0;
                  this.visualSlot = this.oldSlot;
                  if (value != 0) {
                     SpoofElytraAsChestplateHelper.hold(this.oldSlot, 8);
                  } else {
                     SpoofElytraAsChestplateHelper.refresh(8);
                  }
               } else {
                  this.visualSlot = -1;
               }

               this.suppressTicks = 4;
               this.postRestoreTicks = 0;
               suppressHeldItemTooltip(instance);
               this.swapToSlot(bestSlot);
            }
         } else {
            if (this.swapping) {
               this.restoreSlot(instance);
            }
         }
      }
   }

   private int findBestSlot(BlockState blockState) {
      Minecraft instance = Minecraft.getInstance();
      if (instance.player == null) {
         return -1;
      }

      if (blockState.is(Blocks.COBWEB)) {
         int efficiencyShearsSlot = this.findEfficiencyShearsSlot(5);
         if (efficiencyShearsSlot != -1) {
            return efficiencyShearsSlot;
         }
      }

      int result = -1;
      float value = 1.0F;

      for (int value2 = 0; value2 < 9; value2++) {
         ItemStack item = instance.player.getInventory().getItem(value2);
         if (!item.isEmpty()) {
            float destroySpeed = item.getDestroySpeed(blockState);
            if (destroySpeed > value) {
               value = destroySpeed;
               result = value2;
            }
         }
      }

      return result;
   }

   public static boolean shouldSuppressHeldItemTooltip() {
      if (INSTANCE.isEnabled()) {
         if (INSTANCE.swapping) {
            return true;
         }

         if (INSTANCE.suppressTicks > 0 || INSTANCE.visualSlot != -1 || INSTANCE.postRestoreTicks > 0) {
            return true;
         }
      }

      return false;
   }

   private static void suppressHeldItemTooltip(Minecraft minecraft) {
      if (minecraft.gui != null) {
         HudSetOverlayMessageTimeAccessor hudSetOverlayMessageTimeAccessor = (HudSetOverlayMessageTimeAccessor)minecraft.gui.hud;
         hudSetOverlayMessageTimeAccessor.skycore$setOverlayMessageTime(0);
         hudSetOverlayMessageTimeAccessor.skycore$setToolHighlightTimer(0);
      }
   }

   private void swapToSlot(int slot) {
      Minecraft instance = Minecraft.getInstance();
      if (slot >= 0 && slot <= 8 && instance.player != null) {
         instance.player.getInventory().setSelectedSlot(slot);
      }
   }
}
