package deluxe.client.event;

import net.minecraft.entity.Entity;

public class EntityYawEvent extends TickEvent {
   private Entity field10855;
   private float field10856;

   public Entity method3191() {
      return this.field10855;
   }

   public float method3192() {
      return this.field10856;
   }

   public EntityYawEvent(Entity entity, float local5162_2) {
      this.field10855 = entity;
      this.field10856 = local5162_2;
   }

   public void method3193(Entity entity) {
      this.field10855 = entity;
   }

   public void method3194(float local5164_1) {
      this.field10856 = local5164_1;
   }
}
