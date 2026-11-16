package deluxe.client.cosmetic;

public class ModelVec3 {
   public static final ModelVec3 modelVec3 = new ModelVec3(0.0F, 0.0F, 0.0F);
   public float field14926;
   public float field14927;
   public float field14928;

   public void method4367(float local7372_1, float local7372_2, float local7372_3) {
      this.field14926 = local7372_1;
      this.field14927 = local7372_2;
      this.field14928 = local7372_3;
   }

   public float method4368() {
      return this.field14926;
   }

   public void method4369(float local7374_1) {
      this.field14928 = local7374_1;
   }

   public ModelVec3(float local7375_1, float local7375_2, float local7375_3) {
      this.field14926 = local7375_1;
      this.field14927 = local7375_2;
      this.field14928 = local7375_3;
   }

   public void method4370(float local7377_1) {
      this.field14926 = local7377_1;
   }

   public void method4371(float local7378_1) {
      this.field14927 = local7378_1;
   }

   public float method4372() {
      return this.field14928;
   }

   @Override
   public String toString() {
      return this.field14926 + "," + this.field14927 + "," + this.field14928;
   }

   public ModelVec3 method4373(ModelVec3 newModelVec3) {
      return new ModelVec3(
         this.field14927 * newModelVec3.field14928 - this.field14928 * newModelVec3.field14927,
         this.field14928 * newModelVec3.field14926 - this.field14926 * newModelVec3.field14928,
         this.field14926 * newModelVec3.field14927 - this.field14927 * newModelVec3.field14926
      );
   }

   public float method4374() {
      return this.field14927;
   }
}
