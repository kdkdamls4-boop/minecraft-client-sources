package deluxe.client.cosmetic;

import deluxe.client.module.render.TagsModule;

import java.util.ArrayList;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public final class BedrockVertexCollector {
   public static final int field08013 = 5;
   private static float[] field08014 = new float[7680];
   private static int field08015;
   private static final ArrayList<Identifier> field08016 = new ArrayList<>(4);
   private static final ArrayList<int[]> field08017 = new ArrayList<>(4);
   private static final Vector3f field08018 = new Vector3f();

   public static Identifier method2286(int local3569_0) {
      return field08016.get(local3569_0);
   }

   public static void method2287() {
      field08015 = 0;
      field08016.clear();
      field08017.clear();
   }

   public static void method2288(BedrockCube bedrockCube, Matrix4f matrix4f) {
      if (!field08017.isEmpty()) {
         for (BedrockQuad bedrockQuad : bedrockCube.recoveredClass0278Array) {
            if (bedrockQuad != null) {
               if (field08015 + 20 > field08014.length) {
                  float[] floatArray = new float[Math.max(field08014.length * 2, field08015 + 20)];
                  System.arraycopy(field08014, 0, floatArray, 0, field08015);
                  field08014 = floatArray;
               }

               for (BedrockVertex bedrockVertex : bedrockQuad.recoveredClass0057Array) {
                  matrix4f.transformPosition(
                     field08018.set(bedrockVertex.modelVec3.method4368(), bedrockVertex.modelVec3.method4374(), bedrockVertex.modelVec3.method4372())
                  );
                  field08014[field08015++] = field08018.x;
                  field08014[field08015++] = field08018.y;
                  field08014[field08015++] = field08018.z;
                  field08014[field08015++] = bedrockVertex.field03510;
                  field08014[field08015++] = bedrockVertex.field03511;
               }

               field08017.get(field08017.size() - 1)[1]++;
            }
         }
      }
   }

   private BedrockVertexCollector() {
   }

   public static int method2289(int local3573_0) {
      return field08017.get(local3573_0)[0];
   }

   public static float method2290(int local3575_0) {
      return field08014[local3575_0];
   }

   public static void method2291(Identifier identifier) {
      field08016.add(identifier);
      field08017.add(new int[]{field08015 / 20, 0});
   }

   public static int method2292(int local3577_0) {
      return field08017.get(local3577_0)[1];
   }

   public static int method2293() {
      return field08016.size();
   }

   public static boolean method2294() {
      return TagsModule.method4106().isEnabled()
         && TagsModule.method4106().field13990.isChecked()
         && MinecraftClient.getInstance().player != null
         && TagsModule.method4106().method4105(MinecraftClient.getInstance().player);
   }

   public static int method2295() {
      return field08015 / 20;
   }
}
