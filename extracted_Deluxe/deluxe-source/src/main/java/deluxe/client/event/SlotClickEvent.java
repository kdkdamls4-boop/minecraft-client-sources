package deluxe.client.event;

import net.minecraft.screen.slot.SlotActionType;

public class SlotClickEvent extends CancellableEvent {
   private int field10173;
   private int field10174;
   private int field10175;
   private SlotActionType field10176;

   public int method2981() {
      return this.field10175;
   }

   public SlotClickEvent(int local4734_1, int local4734_2, int local4734_3, SlotActionType slotActionType) {
      this.field10173 = local4734_1;
      this.field10174 = local4734_2;
      this.field10175 = local4734_3;
      this.field10176 = slotActionType;
   }

   public void method2982(int local4735_1) {
      this.field10173 = local4735_1;
   }

   public int method2983() {
      return this.field10174;
   }

   public void method2984(int local4737_1) {
      this.field10174 = local4737_1;
   }

   public int method2985() {
      return this.field10173;
   }

   public void method2986(SlotActionType slotActionType) {
      this.field10176 = slotActionType;
   }

   public void method2987(int local4740_1) {
      this.field10175 = local4740_1;
   }

   public SlotActionType method2988() {
      return this.field10176;
   }
}
