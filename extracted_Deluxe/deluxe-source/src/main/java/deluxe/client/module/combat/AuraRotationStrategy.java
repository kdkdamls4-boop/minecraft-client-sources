package deluxe.client.module.combat;

import net.minecraft.entity.LivingEntity;

public interface AuraRotationStrategy {
   default boolean method0844(LivingEntity livingEntity, float local1138_2) {
      return false;
   }

   void method0677(LivingEntity livingEntity, float var2);

   void method0676(LivingEntity livingEntity, boolean var2, float var3);

   void l_();
}
