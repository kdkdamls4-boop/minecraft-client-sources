package deluxe.client.cosmetic;

public class BedrockVertex {
   public ModelVec3 modelVec3;
   public float field03510;
   public float field03511;

   public BedrockVertex(ModelVec3 initialModelVec3, float local1757_2, float local1757_3) {
      this.modelVec3 = initialModelVec3;
      this.field03510 = local1757_2;
      this.field03511 = local1757_3;
   }

   public BedrockVertex(float local1758_1, float local1758_2, float local1758_3, float local1758_4, float local1758_5) {
      this.modelVec3 = new ModelVec3(local1758_1, local1758_2, local1758_3);
      this.field03510 = local1758_4;
      this.field03511 = local1758_5;
   }
}
