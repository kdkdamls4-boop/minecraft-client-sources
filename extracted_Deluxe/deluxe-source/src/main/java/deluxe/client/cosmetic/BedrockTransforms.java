package deluxe.client.cosmetic;

import net.minecraft.client.util.math.MatrixStack;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class BedrockTransforms {
   private static final Vector3f field15089 = new Vector3f(1.0F, 0.0F, 0.0F);
   private static final Vector3f field15090 = new Vector3f(0.0F, 1.0F, 0.0F);
   private static final Vector3f field15091 = new Vector3f(0.0F, 0.0F, 1.0F);

   public static void method4425(BedrockBone bedrockBone, MatrixStack matrixStack) {
      matrixStack.translate(-bedrockBone.method0859() / 16.0F, bedrockBone.method0851() / 16.0F, bedrockBone.method0870() / 16.0F);
   }

   public static void method4426(BedrockBone bedrockBone, MatrixStack matrixStack) {
      matrixStack.translate(-bedrockBone.method0853() / 16.0F, -bedrockBone.method0860() / 16.0F, -bedrockBone.method0875() / 16.0F);
   }

   public static void method4427(BedrockCube bedrockCube, MatrixStack matrixStack) {
      if (bedrockCube.field09833.method4372() != 0.0F) {
         matrixStack.multiply(method4433(field15091, bedrockCube.field09833.method4372()));
      }

      if (bedrockCube.field09833.method4374() != 0.0F) {
         matrixStack.multiply(method4433(field15090, bedrockCube.field09833.method4374()));
      }

      if (bedrockCube.field09833.method4368() != 0.0F) {
         matrixStack.multiply(method4433(field15089, bedrockCube.field09833.method4368()));
      }
   }

   public static void method4428(BedrockBone bedrockBone, MatrixStack matrixStack) {
      matrixStack.scale(bedrockBone.method0872(), bedrockBone.method0854(), bedrockBone.method0868());
   }

   public static void method4429(BedrockCube bedrockCube, MatrixStack matrixStack) {
      matrixStack.translate(
         -bedrockCube.field09832.method4368() / 16.0F, -bedrockCube.field09832.method4374() / 16.0F, -bedrockCube.field09832.method4372() / 16.0F
      );
   }

   public static void method4430(BedrockBone bedrockBone, MatrixStack matrixStack) {
      if (bedrockBone.method0858() != 0.0F) {
         matrixStack.multiply(method4433(field15091, bedrockBone.method0858()));
      }

      if (bedrockBone.method0867() != 0.0F) {
         matrixStack.multiply(method4433(field15090, bedrockBone.method0867()));
      }

      if (bedrockBone.method0865() != 0.0F) {
         matrixStack.multiply(method4433(field15089, bedrockBone.method0865()));
      }
   }

   public static void method4431(BedrockCube bedrockCube, MatrixStack matrixStack) {
      matrixStack.translate(
         bedrockCube.field09832.method4368() / 16.0F, bedrockCube.field09832.method4374() / 16.0F, bedrockCube.field09832.method4372() / 16.0F
      );
   }

   public static void method4432(BedrockBone bedrockBone, MatrixStack matrixStack) {
      matrixStack.translate(bedrockBone.method0853() / 16.0F, bedrockBone.method0860() / 16.0F, bedrockBone.method0875() / 16.0F);
   }

   public static Quaternionf method4433(Vector3f vector3f, float local7452_1) {
      float local7452_2 = (float)Math.sin(local7452_1 / 2.0F);
      float local7452_3 = vector3f.x() * local7452_2;
      float local7452_4 = vector3f.y() * local7452_2;
      float local7452_5 = vector3f.z() * local7452_2;
      float local7452_6 = (float)Math.cos(local7452_1 / 2.0F);
      return new Quaternionf(local7452_3, local7452_4, local7452_5, local7452_6);
   }
}
