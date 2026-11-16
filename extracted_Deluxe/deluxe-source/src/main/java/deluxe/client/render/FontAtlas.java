package deluxe.client.render;

import deluxe.client.render.MsdfGlyph;

import java.util.Arrays;
import java.util.Map;
import net.minecraft.util.Identifier;

public final class FontAtlas {
   private static final MsdfGlyph field05337 = MsdfGlyph.nonDrawable(0.0F);
   private final Identifier field05338;
   private final int field05339;
   private final int field05340;
   private final float field05341;
   private final float field05342;
   private final float field05343;
   private final float field05344;
   private final Map<Integer, MsdfGlyph> field05345;
   private final Map<Long, Float> field05346;
   private final MsdfGlyph field05347;

   public float[] method1704(String local2545_1, float local2545_2) {
      if (local2545_1 != null && !local2545_1.isEmpty()) {
         float local2545_3 = this.field05343 * local2545_2;
         float local2545_4 = Float.MAX_VALUE;
         float local2545_5 = -Float.MAX_VALUE;
         float local2545_6 = Float.MAX_VALUE;
         float local2545_7 = -Float.MAX_VALUE;
         boolean local2545_8 = false;
         float local2545_9 = 0.0F;
         int local2545_10 = -1;
         int local2545_11 = 0;

         while (local2545_11 < local2545_1.length()) {
            int local2545_12 = local2545_1.codePointAt(local2545_11);
            local2545_11 += Character.charCount(local2545_12);
            if (local2545_12 == 10) {
               local2545_10 = -1;
            } else {
               MsdfGlyph msdfGlyph = this.method1714(local2545_12);
               if (local2545_10 != -1) {
                  local2545_9 += this.method1707(local2545_10, local2545_12) * local2545_2;
               }

               if (!this.method1711(local2545_12) && SmallTextAtlasBaker.method3582(local2545_12)) {
                  local2545_4 = Math.min(local2545_4, local2545_9);
                  local2545_5 = Math.max(local2545_5, local2545_9 + SmallTextAtlasBaker.method3605(local2545_12) * local2545_2);
                  local2545_6 = Math.min(local2545_6, local2545_3 - 0.74F * local2545_2);
                  local2545_7 = Math.max(local2545_7, local2545_3);
                  local2545_8 = true;
               } else if (msdfGlyph.drawable()) {
                  local2545_4 = Math.min(local2545_4, local2545_9 + msdfGlyph.planeLeft() * local2545_2);
                  local2545_5 = Math.max(local2545_5, local2545_9 + msdfGlyph.planeRight() * local2545_2);
                  local2545_6 = Math.min(local2545_6, local2545_3 - msdfGlyph.planeTop() * local2545_2);
                  local2545_7 = Math.max(local2545_7, local2545_3 - msdfGlyph.planeBottom() * local2545_2);
                  local2545_8 = true;
               }

               local2545_9 += (
                     !this.method1711(local2545_12) && SmallTextAtlasBaker.method3582(local2545_12)
                        ? SmallTextAtlasBaker.method3605(local2545_12)
                        : msdfGlyph.advance()
                  )
                  * local2545_2;
               local2545_10 = local2545_12;
            }
         }

         return local2545_8 ? new float[]{local2545_4, local2545_6, local2545_5, local2545_7} : new float[]{0.0F, 0.0F, 0.0F, 0.0F};
      } else {
         return new float[]{0.0F, 0.0F, 0.0F, 0.0F};
      }
   }

   public int method1705() {
      return this.field05339;
   }

   public int method1706() {
      return this.field05340;
   }

   float method1707(int local2548_1, int local2548_2) {
      if (this.field05346.isEmpty()) {
         return 0.0F;
      }

      Float local2548_3 = this.field05346.get((long)local2548_1 << 32 | local2548_2 & 4294967295L);
      return local2548_3 == null ? 0.0F : local2548_3;
   }

   public float method1708(float local2549_1) {
      return this.field05342 * local2549_1;
   }

   public float method1709(float local2551_1) {
      return this.field05344 * local2551_1;
   }

   public float method1710(float local2552_1) {
      return this.field05343 * local2552_1;
   }

   public boolean method1711(int local2553_1) {
      return this.field05345.containsKey(local2553_1);
   }

   public float[] method1712(String local2554_1, float local2554_2) {
      if (local2554_1 != null && !local2554_1.isEmpty()) {
         float[] floatArray = new float[local2554_1.length() * 8];
         float local2554_4 = 0.0F;
         float local2554_5 = this.method1710(local2554_2);
         int local2554_6 = 0;
         int local2554_7 = -1;
         int local2554_8 = 0;

         while (local2554_8 < local2554_1.length()) {
            int local2554_9 = local2554_1.codePointAt(local2554_8);
            local2554_8 += Character.charCount(local2554_9);
            MsdfGlyph msdfGlyph = this.method1714(local2554_9);
            if (local2554_7 != -1) {
               local2554_4 += this.method1707(local2554_7, local2554_9) * local2554_2;
            }

            if (msdfGlyph.drawable()) {
               floatArray[local2554_6] = local2554_4 + msdfGlyph.planeLeft() * local2554_2;
               floatArray[local2554_6 + 1] = local2554_5 - msdfGlyph.planeTop() * local2554_2;
               floatArray[local2554_6 + 2] = local2554_4 + msdfGlyph.planeRight() * local2554_2;
               floatArray[local2554_6 + 3] = local2554_5 - msdfGlyph.planeBottom() * local2554_2;
               floatArray[local2554_6 + 4] = msdfGlyph.u0();
               floatArray[local2554_6 + 5] = msdfGlyph.v0();
               floatArray[local2554_6 + 6] = msdfGlyph.u1();
               floatArray[local2554_6 + 7] = msdfGlyph.v1();
               local2554_6 += 8;
            }

            local2554_4 += msdfGlyph.advance() * local2554_2;
            local2554_7 = local2554_9;
         }

         return Arrays.copyOf(floatArray, local2554_6);
      } else {
         return new float[0];
      }
   }

   public float method1713(String local2555_1, float local2555_2) {
      if (local2555_1 != null && !local2555_1.isEmpty()) {
         float local2555_3 = 0.0F;
         float local2555_4 = 0.0F;
         int local2555_5 = -1;
         int local2555_6 = 0;

         while (local2555_6 < local2555_1.length()) {
            int local2555_7 = local2555_1.codePointAt(local2555_6);
            local2555_6 += Character.charCount(local2555_7);
            if (local2555_7 == 10) {
               local2555_3 = Math.max(local2555_3, local2555_4);
               local2555_4 = 0.0F;
               local2555_5 = -1;
            } else {
               MsdfGlyph msdfGlyph = this.method1714(local2555_7);
               if (local2555_5 != -1) {
                  local2555_4 += this.method1707(local2555_5, local2555_7) * local2555_2;
               }

               local2555_4 += (
                     !this.method1711(local2555_7) && SmallTextAtlasBaker.method3582(local2555_7)
                        ? SmallTextAtlasBaker.method3605(local2555_7)
                        : msdfGlyph.advance()
                  )
                  * local2555_2;
               local2555_5 = local2555_7;
            }
         }

         return Math.max(local2555_3, local2555_4);
      } else {
         return 0.0F;
      }
   }

   MsdfGlyph method1714(int local2556_1) {
      MsdfGlyph msdfGlyph = this.field05345.get(local2556_1);
      if (msdfGlyph != null) {
         return msdfGlyph;
      } else {
         return local2556_1 > 32 && !Character.isWhitespace(local2556_1) && Character.isDefined(local2556_1) ? this.field05347 : field05337;
      }
   }

   public FontAtlas(
      Identifier identifier,
      int local2557_2,
      int local2557_3,
      float local2557_4,
      float local2557_5,
      float local2557_6,
      float local2557_7,
      Map<Integer, MsdfGlyph> local2557_8,
      Map<Long, Float> local2557_9
   ) {
      this.field05338 = identifier;
      this.field05339 = local2557_2;
      this.field05340 = local2557_3;
      this.field05341 = local2557_4;
      this.field05342 = local2557_5;
      this.field05343 = local2557_6;
      this.field05344 = local2557_7;
      this.field05345 = local2557_8;
      this.field05346 = local2557_9;
      MsdfGlyph msdfGlyph = local2557_8.get(63);
      this.field05347 = msdfGlyph != null ? msdfGlyph : field05337;
   }

   public Identifier method1715() {
      return this.field05338;
   }

   public float method1716() {
      return this.field05341;
   }
}
