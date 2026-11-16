package deluxe.client.cosmetic;

import deluxe.client.cosmetic.BedrockBone;
import deluxe.client.cosmetic.BedrockModel;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.Map.Entry;

public final class CosmeticAnimationState {
   final int field15525;
   final String field15526;
   final int field15527;
   public final float field15528;
   public final float field15529;
   public final float field15530;
   public final float field15531;
   public final float field15532;
   public final float field15533;
   public final float field15534;
   public final BedrockModel bedrockModel;
   public final Map<String, CosmeticAnimationState.Nested0534> field15536;
   public final Map<String, float[]> field15537 = new HashMap<>();
   private final Map<String, BedrockBone> field15538 = new HashMap<>();
   private final String[] field15539 = new String[16];
   private final boolean[] field15540 = new boolean[16];
   public int field15541;

   private String method4569(String... stringArray) {
      for (String local7724_3 : this.field15536.keySet()) {
         String local7724_4 = method4575(local7724_3);

         for (String local7724_8 : stringArray) {
            if (local7724_4.contains(local7724_8)) {
               return local7724_3;
            }
         }
      }

      return null;
   }

   private String method4570(String stringArray) {
      for (String local7725_3 : this.field15536.keySet()) {
         if (method4575(local7725_3).equals(stringArray)) {
            return local7725_3;
         }
      }

      return this.method4569(stringArray);
   }

   String method4571(boolean local7726_1, boolean local7726_2, boolean local7726_3, boolean local7726_4) {
      if (this.field15536.isEmpty()) {
         return null;
      }

      if (local7726_1) {
         String local7726_5 = this.method4569("elytra", "flying", "fly");
         if (local7726_5 != null) {
            return local7726_5;
         }
      }

      if (local7726_2) {
         if (local7726_4) {
            String local7726_5 = this.method4573("moving", "water");
            if (local7726_5 != null) {
               return local7726_5;
            }
         }

         String local7726_5 = this.method4569("swimming", "swim");
         if (local7726_5 != null) {
            return local7726_5;
         }

         local7726_5 = this.method4573("idle", "water");
         if (local7726_5 != null) {
            return local7726_5;
         }

         local7726_5 = this.method4573("main", "water");
         if (local7726_5 != null) {
            return local7726_5;
         }
      }

      if (local7726_3) {
         if (local7726_4) {
            String local7726_5 = this.method4573("moving", "sneak");
            if (local7726_5 != null) {
               return local7726_5;
            }
         }

         String local7726_5 = this.method4573("idle", "sneak");
         if (local7726_5 != null) {
            return local7726_5;
         }

         local7726_5 = this.method4573("main", "sneak");
         if (local7726_5 != null) {
            return local7726_5;
         }

         local7726_5 = this.method4569("sneak", "crouch");
         if (local7726_5 != null) {
            return local7726_5;
         }
      }

      if (local7726_4) {
         String local7726_5 = this.method4569("walking", "walk", "moving", "move", "run");
         if (local7726_5 != null && !method4575(local7726_5).contains("water") && !method4575(local7726_5).contains("sneak")) {
            return local7726_5;
         }
      }

      String local7726_5 = this.method4570("idle");
      if (local7726_5 != null && !method4575(local7726_5).contains("water") && !method4575(local7726_5).contains("sneak")) {
         return local7726_5;
      }

      String local7726_6 = this.method4570("main");
      if (local7726_6 != null && !method4575(local7726_6).contains("water") && !method4575(local7726_6).contains("sneak")) {
         return local7726_6;
      }

      for (String local7726_8 : this.field15536.keySet()) {
         String local7726_9 = method4575(local7726_8);
         if (!local7726_9.contains("gui") && !local7726_9.contains("preview")) {
            return local7726_8;
         }
      }

      return this.field15536.keySet().iterator().next();
   }

   private void method4572(BedrockBone bedrockBone) {
      float[] floatArray = this.field15537.get(bedrockBone.field01589);
      if (floatArray != null) {
         bedrockBone.method0856(floatArray[0]);
         bedrockBone.method0862(floatArray[1]);
         bedrockBone.method0863(floatArray[2]);
         bedrockBone.method0850(floatArray[3]);
         bedrockBone.method0874(floatArray[4]);
         bedrockBone.method0855(floatArray[5]);
         bedrockBone.method0864(floatArray[6]);
         bedrockBone.method0873(floatArray[7]);
         bedrockBone.method0861(floatArray[8]);
      }

      for (BedrockBone bedrockBone2 : bedrockBone.field01587) {
         this.method4572(bedrockBone2);
      }
   }

   private String method4573(String... stringArray) {
      for (String local7728_3 : this.field15536.keySet()) {
         String local7728_4 = method4575(local7728_3);
         boolean local7728_5 = true;

         for (String local7728_9 : stringArray) {
            local7728_5 &= local7728_4.contains(local7728_9);
         }

         if (local7728_5) {
            return local7728_3;
         }
      }

      return null;
   }

   public String method4574(boolean local7729_1, boolean local7729_2, boolean local7729_3, boolean local7729_4) {
      int local7729_5 = (local7729_1 ? 1 : 0) | (local7729_2 ? 2 : 0) | (local7729_3 ? 4 : 0) | (local7729_4 ? 8 : 0);
      if (!this.field15540[local7729_5]) {
         this.field15539[local7729_5] = this.method4571(local7729_1, local7729_2, local7729_3, local7729_4);
         this.field15540[local7729_5] = true;
      }

      return this.field15539[local7729_5];
   }

   private static String method4575(String local7730_0) {
      return local7730_0.toLowerCase(Locale.ROOT);
   }

   private void method4576(BedrockBone bedrockBone) {
      this.field15537
         .put(
            bedrockBone.field01589,
            new float[]{
               bedrockBone.method0865(),
               bedrockBone.method0867(),
               bedrockBone.method0858(),
               bedrockBone.method0859(),
               bedrockBone.method0851(),
               bedrockBone.method0870(),
               bedrockBone.method0872(),
               bedrockBone.method0854(),
               bedrockBone.method0868()
            }
         );
      this.field15538.putIfAbsent(bedrockBone.field01589, bedrockBone);
      this.field15541 = this.field15541 + bedrockBone.field01588.size();

      for (BedrockBone bedrockBone2 : bedrockBone.field01587) {
         this.method4576(bedrockBone2);
      }
   }

   public CosmeticAnimationState(
      int local7732_1,
      String local7732_2,
      int local7732_3,
      float local7732_4,
      float local7732_5,
      float local7732_6,
      float local7732_7,
      float local7732_8,
      float local7732_9,
      float local7732_10,
      BedrockModel initialBedrockModel,
      Map<String, CosmeticAnimationState.Nested0534> local7732_12
   ) {
      this.field15525 = local7732_1;
      this.field15526 = local7732_2;
      this.field15527 = local7732_3;
      this.field15528 = local7732_4;
      this.field15529 = local7732_5;
      this.field15530 = local7732_6;
      this.field15531 = local7732_7;
      this.field15532 = local7732_8;
      this.field15533 = local7732_9;
      this.field15534 = local7732_10;
      this.bedrockModel = initialBedrockModel;
      this.field15536 = local7732_12;

      for (BedrockBone bedrockBone : initialBedrockModel.field03648) {
         this.method4576(bedrockBone);
      }
   }

   public void method4577() {
      for (BedrockBone bedrockBone : this.bedrockModel.field03648) {
         this.method4572(bedrockBone);
      }
   }

   public BedrockBone method4578(String local7734_1) {
      return this.field15538.get(local7734_1);
   }

   public static final class Nested0533 {
      final NavigableMap<Float, float[]> field15559 = new TreeMap<>();
      float[] field15560;
      private float[] field15561;
      private float[][] field15562;

      public boolean method4579() {
         return this.field15560 == null && this.field15559.isEmpty();
      }

      float[] method4580(float local7736_1, float[] floatArray) {
         return this.method4581(local7736_1, floatArray, new float[3]);
      }

      public float[] method4581(float local7737_1, float[] floatArray, float[] floatArray2) {
         if (this.field15560 != null) {
            return this.field15560;
         }

         if (this.field15559.isEmpty()) {
            return floatArray;
         }

         if (this.field15559.size() == 1) {
            return this.field15559.firstEntry().getValue();
         }

         if (this.field15561 == null || this.field15561.length != this.field15559.size()) {
            float[] floatArray3 = new float[this.field15559.size()];
            float[][] floatArrayArray = new float[this.field15559.size()][];
            int local7737_6 = 0;

            for (Entry<Float, float[]> local7737_8 : this.field15559.entrySet()) {
               floatArray3[local7737_6] = local7737_8.getKey();
               floatArrayArray[local7737_6++] = local7737_8.getValue();
            }

            this.field15562 = floatArrayArray;
            this.field15561 = floatArray3;
         }

         float[] floatArray4 = this.field15561;
         int local7737_5 = floatArray4.length;
         if (local7737_1 <= floatArray4[0]) {
            return this.field15562[0];
         }

         if (local7737_1 >= floatArray4[local7737_5 - 1]) {
            return this.field15562[local7737_5 - 1];
         }

         int local7737_6 = 0;
         int local7737_7 = local7737_5 - 1;

         while (local7737_7 - local7737_6 > 1) {
            int local7737_8 = local7737_6 + local7737_7 >>> 1;
            if (floatArray4[local7737_8] <= local7737_1) {
               local7737_6 = local7737_8;
            } else {
               local7737_7 = local7737_8;
            }
         }

         if (floatArray4[local7737_6] == local7737_1) {
            return this.field15562[local7737_6];
         }

         float local7737_8 = (local7737_1 - floatArray4[local7737_6]) / (floatArray4[local7737_7] - floatArray4[local7737_6]);
         local7737_8 = Math.max(0.0F, Math.min(1.0F, local7737_8));
         float[] floatArray5 = this.field15562[local7737_6];
         float[] floatArray6 = this.field15562[local7737_7];
         floatArray2[0] = floatArray5[0] + local7737_8 * (floatArray6[0] - floatArray5[0]);
         floatArray2[1] = floatArray5[1] + local7737_8 * (floatArray6[1] - floatArray5[1]);
         floatArray2[2] = floatArray5[2] + local7737_8 * (floatArray6[2] - floatArray5[2]);
         return floatArray2;
      }
   }

   public static final class Nested0534 {
      final String field15563;
      public final boolean field15564;
      public final float field15565;
      public final Map<String, CosmeticAnimationState.Nested0535> field15566;

      Nested0534(String local7739_1, boolean local7739_2, float local7739_3, Map<String, CosmeticAnimationState.Nested0535> local7739_4) {
         this.field15563 = local7739_1;
         this.field15564 = local7739_2;
         this.field15565 = Math.max(0.001F, local7739_3);
         this.field15566 = local7739_4;
      }
   }

   public static final class Nested0535 {
      public final CosmeticAnimationState.Nested0533 field15568;
      public final CosmeticAnimationState.Nested0533 field15569;
      public final CosmeticAnimationState.Nested0533 field15570;

      Nested0535(CosmeticAnimationState.Nested0533 local7740_1, CosmeticAnimationState.Nested0533 local7740_2, CosmeticAnimationState.Nested0533 local7740_3) {
         this.field15568 = local7740_1;
         this.field15569 = local7740_2;
         this.field15570 = local7740_3;
      }
   }
}
