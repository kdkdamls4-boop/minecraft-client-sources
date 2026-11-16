package deluxe.client.module.combat;

import deluxe.client.setting.BooleanSetting;
import deluxe.client.event.EventListener;
import deluxe.client.event.EventPriority;
import deluxe.client.setting.LegacyNumberSetting;
import deluxe.client.setting.ModeSetting;
import deluxe.client.module.Module;
import deluxe.client.module.ModuleCategory;
import deluxe.client.module.ModuleInfo;
import deluxe.client.event.MovementInputEvent;
import deluxe.client.setting.MultiSelectSetting;
import deluxe.client.event.PacketEvent;

import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket;
import net.minecraft.sound.SoundEvents;

@ModuleInfo(name = "Velocity", description = "Убирает откидывание от ударов и взрывов.", category = ModuleCategory.COMBAT)
public final class VelocityModule extends Module {
   private static VelocityModule velocityModule;
   public MultiSelectSetting multiSelectSetting;
   public ModeSetting modeSetting;
   public BooleanSetting booleanSetting;
   public LegacyNumberSetting legacyNumberSetting;
   private boolean field04742;
   private int field04743;
   private final EventListener<PacketEvent> field04744 = packetEvent -> {
      if (mc.player != null && packetEvent.isIncoming()) {
         if (packetEvent.getPacket() instanceof ExplosionS2CPacket explosionS2CPacket) {
            boolean local2249_3 = explosionS2CPacket.explosionSound().value() == SoundEvents.ENTITY_WIND_CHARGE_WIND_BURST.value();
            if (local2249_3 ? this.multiSelectSetting.matchesOption("Заряд ветра") : this.multiSelectSetting.matchesOption("Взрывы")) {
               packetEvent.method4423();
            }
         } else if (this.multiSelectSetting.matchesOption("Удары")) {
            if (packetEvent.getPacket() instanceof EntityVelocityUpdateS2CPacket entityVelocityUpdateS2CPacket && entityVelocityUpdateS2CPacket.getEntityId() == mc.player.getId()) {
               if (this.modeSetting.isSelected("Обычный")) {
                  packetEvent.method4423();
               } else {
                  this.field04742 = entityVelocityUpdateS2CPacket.getVelocity().x == 0.0 && entityVelocityUpdateS2CPacket.getVelocity().z == 0.0 && entityVelocityUpdateS2CPacket.getVelocity().y < 0.0;
               }
            }
         }
      }
   };
   @EventPriority(100)
   private final EventListener<MovementInputEvent> field04745 = movementInputEvent -> {
      if (mc.player != null
         && this.multiSelectSetting.matchesOption("Удары")
         && this.modeSetting.isSelected("JumpReset")
         && AttackAuraModule.method1029().isEnabled()) {
         if (!mc.player.isTouchingWater() && !mc.player.isSubmergedInWater() && !mc.player.isInLava()) {
            if (mc.player.hurtTime != 9
               || !mc.player.isOnGround()
               || !mc.player.isSprinting()
               || this.field04742
               || this.booleanSetting.isChecked() && this.field04743 < (int)this.legacyNumberSetting.getValue()) {
               this.field04743++;
            } else {
               movementInputEvent.method4442(true);
               this.field04743 = 0;
            }
         }
      }
   };

   @Override
   protected void onDisable() {
      this.field04742 = false;
      this.field04743 = 0;
   }

   private VelocityModule() {
      this.multiSelectSetting = this.addSetting(
         new MultiSelectSetting("Отменять", "От чего убирать откидывание.", new String[]{"Удары", "Взрывы", "Заряд ветра"}, "Удары")
      );
      this.modeSetting = this.addSetting(new ModeSetting("Тип", "Как гасить откидывание от ударов.", "Обычный", "Обычный", "JumpReset"));
      this.modeSetting.withVisibility(() -> this.multiSelectSetting.matchesOption("Удары"));
      this.booleanSetting = this.addSetting(new BooleanSetting("Прыгать с задержкой тиков", "Прыгать только после паузы между джамп-ресетами.", true));
      this.legacyNumberSetting = this.addSetting(new LegacyNumberSetting("Тиков между прыжками", "Сколько тиков ждать между джамп-ресетами.", 2.0, 0.0, 20.0, 1.0));
      this.booleanSetting.withVisibility(() -> this.multiSelectSetting.matchesOption("Удары") && this.modeSetting.isSelected("JumpReset"));
      this.legacyNumberSetting.withVisibility(() -> this.multiSelectSetting.matchesOption("Удары") && this.modeSetting.isSelected("JumpReset") && this.booleanSetting.isChecked());
   }

   public static VelocityModule method1538() {
      if (velocityModule == null) {
         velocityModule = new VelocityModule();
      }

      return velocityModule;
   }
}
