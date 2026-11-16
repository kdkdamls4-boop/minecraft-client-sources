package deluxe.client.module.movement;

import deluxe.client.event.EventListener;
import deluxe.client.setting.ModeSetting;
import deluxe.client.module.Module;
import deluxe.client.module.ModuleCategory;
import deluxe.client.module.ModuleInfo;
import deluxe.client.event.PlayerSlowdownEvent;
import deluxe.client.event.PlayerTickEvent;

import net.minecraft.item.CrossbowItem;

@ModuleInfo(name = "NoSlow", description = "Убирает замедление при использовании предметов.", category = ModuleCategory.MOVEMENT)
public final class NoSlowModule extends Module {
   private static NoSlowModule noSlowModule;
   private final ModeSetting modeSetting = this.addSetting(
      new ModeSetting("Режим", "Способ обхода замедления.", "Grim Ticks", "Vanilla", "Grim Ticks", "Арбалет")
   );
   private int field13876;
   private final EventListener<PlayerTickEvent> field13877 = playerTickEvent -> {
      if (mc.player != null) {
         if (this.modeSetting.isSelected("Grim Ticks")) {
            if (mc.player.isUsingItem()) {
               this.field13876++;
            } else {
               this.field13876 = 0;
            }
         }
      }
   };
   private final EventListener<PlayerSlowdownEvent> field13878 = playerSlowdownEvent -> {
      if (mc.player != null) {
         mc.player.setSprinting(mc.player.input.hasForwardMovement());
         if (this.modeSetting.isSelected("Grim Ticks")) {
            if (this.field13876 > 1) {
               playerSlowdownEvent.method4423();
               this.field13876 = 0;
            }
         } else if (this.modeSetting.isSelected("Арбалет")) {
            if (mc.player.getMainHandStack().getItem() instanceof CrossbowItem) {
               playerSlowdownEvent.method4423();
            }
         } else if (this.modeSetting.isSelected("Vanilla")) {
            playerSlowdownEvent.method4423();
         }
      }
   };

   private NoSlowModule() {
   }

   public static NoSlowModule method4030() {
      if (noSlowModule == null) {
         noSlowModule = new NoSlowModule();
      }

      return noSlowModule;
   }
}
