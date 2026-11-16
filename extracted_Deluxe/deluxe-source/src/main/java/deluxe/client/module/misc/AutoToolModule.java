package deluxe.client.module.misc;

import deluxe.client.event.EventListener;
import deluxe.client.module.Module;
import deluxe.client.module.ModuleCategory;
import deluxe.client.module.ModuleInfo;
import deluxe.client.event.PlayerTickEvent;
import deluxe.client.util.InventoryUtils;
import deluxe.client.util.InventoryActionQueue;

import net.minecraft.block.BlockState;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult.Type;

@ModuleInfo(name = "AutoTool", description = "Сам берёт лучший инструмент из хотбара или инвентаря при копании блока.", category = ModuleCategory.MISC)
public final class AutoToolModule extends Module {
   private static AutoToolModule autoToolModule;
   private int field10226 = -1;
   private int field10227 = -1;
   private final EventListener<PlayerTickEvent> field10228 = playerTickEvent -> {
      if (mc.player != null && mc.world != null && mc.interactionManager != null && !mc.player.isCreative()) {
         if (!InventoryActionQueue.method2656().method2665()) {
            if (mc.options.attackKey.isPressed()
               && mc.crosshairTarget != null
               && mc.crosshairTarget.getType() == Type.BLOCK
               && mc.crosshairTarget instanceof BlockHitResult blockHitResult) {
               BlockState blockState = mc.world.getBlockState(blockHitResult.getBlockPos());
               if (!blockState.isAir()) {
                  int local4769_4 = -1;
                  float miningSpeedMultiplier = mc.player.getInventory().getStack(InventoryUtils.method0554()).getMiningSpeedMultiplier(blockState);

                  for (int index = 0; index < 36; index++) {
                     float miningSpeedMultiplier2 = mc.player.getInventory().getStack(index).getMiningSpeedMultiplier(blockState);
                     if (miningSpeedMultiplier2 > miningSpeedMultiplier + 0.01F) {
                        miningSpeedMultiplier = miningSpeedMultiplier2;
                        local4769_4 = index;
                     }
                  }

                  if (local4769_4 != -1 && local4769_4 != InventoryUtils.method0554()) {
                     if (this.field10226 == -1) {
                        this.field10226 = InventoryUtils.method0554();
                     }

                     if (local4769_4 < 9) {
                        InventoryUtils.method0553(local4769_4, false);
                     } else if (this.field10227 == -1) {
                        this.field10227 = local4769_4;
                        InventoryActionQueue.method2656().method2662(local4769_4, this.field10226, 1);
                     }
                  }
               }
            } else if (this.field10226 != -1 && !mc.options.attackKey.isPressed()) {
               if (this.field10227 == -1) {
                  InventoryUtils.method0553(this.field10226, false);
               } else {
                  InventoryActionQueue.method2656().method2662(this.field10227, this.field10226, 1);
               }

               this.field10226 = -1;
               this.field10227 = -1;
            }
         }
      }
   };

   public static AutoToolModule method3003() {
      if (autoToolModule == null) {
         autoToolModule = new AutoToolModule();
      }

      return autoToolModule;
   }

   @Override
   protected void onEnable() {
      this.field10226 = -1;
      this.field10227 = -1;
   }
}
