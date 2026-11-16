package deluxe.client.module.movement;

import deluxe.client.setting.BooleanSetting;
import deluxe.client.event.EventListener;
import deluxe.client.module.Module;
import deluxe.client.module.ModuleCategory;
import deluxe.client.module.ModuleInfo;
import deluxe.client.event.PlayerTickEvent;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.UseEffectsComponent;

@ModuleInfo(name = "AutoSprint", description = "Автоспринт: не теряет скорость при ударе и умеет игнорить слепоту.", category = ModuleCategory.MOVEMENT)
public final class AutoSprintModule extends Module {
   private static AutoSprintModule autoSprintModule;
   public BooleanSetting field11792;
   public BooleanSetting field11793;
   private final EventListener<PlayerTickEvent> field11794 = playerTickEvent -> {
      if (mc.player != null) {
         mc.player
            .setSprinting(
               mc.player.input.hasForwardMovement()
                  && !mc.player.hasBlindnessEffect()
                  && (
                     mc.player.hasVehicle()
                        ? mc.player.getVehicle().canSprintAsVehicle() && mc.player.getVehicle().isLogicalSideForUpdatingMovement()
                        : mc.player.getHungerManager().canSprint() || mc.player.getAbilities().allowFlying
                  )
                  && (mc.player.getAbilities().flying || !mc.player.isPartlyTouchingWater())
                  && (
                     !mc.player.isUsingItem()
                        || mc.player.getActiveItem().getOrDefault(DataComponentTypes.USE_EFFECTS, UseEffectsComponent.DEFAULT).canSprint()
                  )
                  && (!mc.player.isGliding() || mc.player.isSubmergedInWater())
                  && (!mc.player.shouldSlowDown() || mc.player.isSubmergedInWater())
            );
      }
   };

   private AutoSprintModule() {
      this.field11792 = this.addSetting(new BooleanSetting("Кип спринт", "Не сбрасывать скорость спринта при ударе.", false));
      this.field11793 = this.addSetting(new BooleanSetting("Игнор слепоты", "Спринтить даже под эффектом слепоты.", false));
   }

   public static AutoSprintModule method3360() {
      if (autoSprintModule == null) {
         autoSprintModule = new AutoSprintModule();
      }

      return autoSprintModule;
   }

   public boolean method3361() {
      return this.isEnabled() && this.field11792.isChecked();
   }

   public boolean method3362() {
      return this.isEnabled() && this.field11793.isChecked();
   }
}
