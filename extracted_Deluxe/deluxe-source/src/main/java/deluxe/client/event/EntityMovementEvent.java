package deluxe.client.event;

import net.minecraft.util.math.Vec3d;

public final class EntityMovementEvent extends CancellableEvent {
   private Vec3d field15024;

   public EntityMovementEvent(Vec3d vec3d) {
      this.field15024 = vec3d;
   }

   public Vec3d method4394() {
      return this.field15024;
   }

   public void method4395(Vec3d vec3d) {
      this.field15024 = vec3d;
   }
}
