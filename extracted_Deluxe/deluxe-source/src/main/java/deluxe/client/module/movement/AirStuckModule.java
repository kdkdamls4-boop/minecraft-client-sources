package deluxe.client.module.movement;

import deluxe.client.event.EntityMovementEvent;
import deluxe.client.event.EventListener;
import deluxe.client.module.Module;
import deluxe.client.module.ModuleCategory;
import deluxe.client.module.ModuleInfo;
import deluxe.client.event.PacketEvent;

import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.Vec3d;

@ModuleInfo(name = "AirStuck", description = "Зависает в воздухе.", category = ModuleCategory.MOVEMENT)
public final class AirStuckModule extends Module {
   private static AirStuckModule airStuckModule;
   private Vec3d field09499 = Vec3d.ZERO;
   private boolean field09500;
   private final EventListener<EntityMovementEvent> field09501 = entityMovementEvent -> {
      if (mc.player != null) {
         if (!this.field09500) {
            boolean local4386_2 = (mc.player.getVelocity().y < -0.05 || entityMovementEvent.method4394().y < -0.05) && !mc.player.isOnGround();
            if (local4386_2 && mc.player.fallDistance > 0.0) {
               this.field09499 = mc.player.getEntityPos();
               this.field09500 = true;
            }
         }

         if (this.field09500) {
            entityMovementEvent.method4395(Vec3d.ZERO);
            mc.player.setPosition(this.field09499.x, this.field09499.y, this.field09499.z);
            mc.player.setVelocity(Vec3d.ZERO);
         }
      }
   };
   private final EventListener<PacketEvent> field09502 = packetEvent -> {
      if (this.field09500 && packetEvent.isOutgoing() && packetEvent.getPacket() instanceof PlayerMoveC2SPacket) {
         packetEvent.method4423();
      }
   };

   public static AirStuckModule method2785() {
      if (airStuckModule == null) {
         airStuckModule = new AirStuckModule();
      }

      return airStuckModule;
   }

   public boolean method2787() {
      return this.isEnabled() && this.field09500;
   }

   @Override
   protected void onDisable() {
      this.field09500 = false;
   }

   @Override
   protected void onEnable() {
      this.field09500 = false;
   }

   private AirStuckModule() {
   }
}
