package deluxe.client.module.misc;

import deluxe.client.util.ClientChat;
import deluxe.client.event.EventListener;
import deluxe.client.util.MathUtils;
import deluxe.mixin.MinecraftClientMixin6;
import deluxe.client.module.Module;
import deluxe.client.module.ModuleCategory;
import deluxe.client.module.ModuleInfo;
import deluxe.client.event.PlayerTickEvent;
import deluxe.client.util.InventoryUtils;
import deluxe.client.RotationAngles;
import deluxe.client.RotationManager;
import deluxe.client.RotationSpeed;

import net.minecraft.block.Blocks;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;

@ModuleInfo(name = "ClanUpgrader", description = "Быстро прокачивает клан с помощью редстоуна и факела.", category = ModuleCategory.MISC)
public final class ClanUpgraderModule extends Module {
   private static ClanUpgraderModule clanUpgraderModule;
   private int field04135 = -1;
   private final EventListener<PlayerTickEvent> field04136 = playerTickEvent -> {
      if (mc.player != null && mc.world != null && mc.interactionManager != null) {
         int local1997_2 = InventoryUtils.method0539(Items.REDSTONE);
         int local1997_3 = InventoryUtils.method0539(Items.TORCH);
         int local1997_4 = local1997_2 != -1 ? local1997_2 : local1997_3;
         if (local1997_4 == -1) {
            ClientChat.method2267("Вам необходимо иметь факел или редстоун в хотбаре");
            this.toggle();
         } else {
            float local1997_5 = (float)(Math.sin(System.currentTimeMillis() / 1220.0) * (Math.abs(90.0F - mc.player.getPitch()) / 8.0F))
               + MathUtils.randomFloat(-0.1F, 0.1F);
            RotationAngles rotationAngles = new RotationAngles(
               mc.player.getYaw() + MathUtils.randomFloat(-1.0F, 1.0F), MathHelper.clamp(88.0F + local1997_5, -90.0F, 90.0F)
            );
            RotationManager.method0658().method0652(rotationAngles, new RotationSpeed(90.0F, 90.0F, 90.0F, 90.0F));
            if (this.field04135 == -1) {
               this.field04135 = InventoryUtils.method0554();
            }

            if (InventoryUtils.method0554() != local1997_4) {
               mc.player.getInventory().setSelectedSlot(local1997_4);
            }

            if (!(
               Math.hypot(
                     Math.abs(MathHelper.wrapDegrees(rotationAngles.method4362() - mc.player.getYaw())),
                     Math.abs(MathHelper.wrapDegrees(rotationAngles.method4365() - mc.player.getPitch()))
                  )
                  > 1.0
            )) {
               BlockPos blockPos = mc.player.getBlockPos();
               if (!mc.world.getBlockState(blockPos).isOf(Blocks.REDSTONE_WIRE) && !mc.world.getBlockState(blockPos).isOf(Blocks.TORCH)) {
                  ((MinecraftClientMixin6)mc).invokeStartUseItem();
               } else {
                  mc.interactionManager.attackBlock(blockPos, Direction.UP);
                  mc.player.swingHand(Hand.MAIN_HAND);
               }
            }
         }
      }
   };

   public static ClanUpgraderModule method1377() {
      if (clanUpgraderModule == null) {
         clanUpgraderModule = new ClanUpgraderModule();
      }

      return clanUpgraderModule;
   }

   private ClanUpgraderModule() {
   }

   @Override
   protected void onDisable() {
      if (mc.player != null && this.field04135 != -1) {
         mc.player.getInventory().setSelectedSlot(this.field04135);
         this.field04135 = -1;
      }
   }
}
