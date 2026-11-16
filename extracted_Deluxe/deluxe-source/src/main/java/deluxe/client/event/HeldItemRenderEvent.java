package deluxe.client.event;

import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;

public final class HeldItemRenderEvent extends TickEvent {
   private final MatrixStack field01603;
   private final ItemStack field01604;
   private final Hand field01605;
   private final Arm field01606;
   private final float field01607;

   public float method0876() {
      return this.field01607;
   }

   public Arm method0877() {
      return this.field01606;
   }

   public MatrixStack method0878() {
      return this.field01603;
   }

   public Hand method0879() {
      return this.field01605;
   }

   public ItemStack method0880() {
      return this.field01604;
   }

   public HeldItemRenderEvent(MatrixStack matrixStack, ItemStack itemStack, Hand hand, Arm arm, float local1180_5) {
      this.field01603 = matrixStack;
      this.field01604 = itemStack;
      this.field01605 = hand;
      this.field01606 = arm;
      this.field01607 = local1180_5;
   }
}
