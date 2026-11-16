package net.skycore.client.module.player;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.skycore.client.mixin.LivingEntityGetNoJumpDelayAccessor;
import net.skycore.client.mixin.MinecraftGetRightClickDelayAccessor;
import net.skycore.client.module.ClientModule;
import net.skycore.client.module.ModuleCategory;
import net.skycore.client.setting.BooleanSetting;

public final class NoDelayModule extends ClientModule {
   private final BooleanSetting fastExp = this.add(new BooleanSetting("Fast EXP", true));
   public static final NoDelayModule INSTANCE = new NoDelayModule();

   @Override
   protected void onTick() {
      Minecraft instance = Minecraft.getInstance();
      LocalPlayer player = instance.player;
      if (player != null) {
         if (instance.level != null) {
            ((LivingEntityGetNoJumpDelayAccessor)player).skycore$setNoJumpDelay(0);
            Boolean get = this.fastExp.get();
            if (get) {
               if (instance.options.keyUse.isDown()) {
                  ItemStack mainHandItem = player.getMainHandItem();
                  if (mainHandItem.is(Items.EXPERIENCE_BOTTLE) && player.connection != null) {
                     ((MinecraftGetRightClickDelayAccessor)instance).skycore$setRightClickDelay(0);
                  }
               }
            }

            return;
         }
      }
   }

   private NoDelayModule() {
      super("No Delay", "Removes jump delay (not block place)", ModuleCategory.PLAYER);
   }
}
