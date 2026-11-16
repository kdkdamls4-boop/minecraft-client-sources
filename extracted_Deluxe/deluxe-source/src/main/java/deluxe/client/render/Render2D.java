package deluxe.client.render;

import deluxe.client.DeluxeClient;
import deluxe.mixin.DrawContextMixin;
import deluxe.client.util.FormattedText;
import deluxe.client.module.misc.NameProtectModule;
import deluxe.client.render.RasterTextDrawData;
import deluxe.client.render.Outline360DrawData;
import deluxe.client.util.GuiScale;
import deluxe.client.render.GuiLayerSerialAccess;
import deluxe.client.render.ArcColorSpan;
import deluxe.client.render.WarpTextDrawData;
import deluxe.client.render.BlobDrawData;
import deluxe.client.render.RippleDrawData;
import deluxe.client.render.LineDrawData;
import deluxe.client.render.PickerDrawData;
import deluxe.client.render.CircleDrawData;
import deluxe.client.render.ThemePaletteBuffer;
import deluxe.client.render.ItemRenderCache;
import deluxe.client.render.ImageDrawData;
import deluxe.client.render.CornerBracketDrawData;
import deluxe.client.render.SoftGlowDrawData;
import deluxe.client.render.ArcDividerDrawData;
import deluxe.client.render.MenuGridDrawData;
import deluxe.client.render.HalfIconDrawData;
import deluxe.client.render.DitherDrawData;
import deluxe.client.render.GlassDrawData;
import deluxe.client.render.ShapeDrawData;
import deluxe.client.render.BlurDrawBuilder;
import deluxe.client.render.RectDrawData;
import deluxe.client.render.GlassOutlineDrawData;
import deluxe.client.render.ScissorStack;
import deluxe.client.render.BlurDrawData;
import deluxe.client.render.MsdfTextDrawData;
import deluxe.client.render.GlowCompositeDrawData;
import deluxe.client.render.ShimmerDrawData;
import deluxe.client.render.OutlineDrawData;
import deluxe.client.render.RoundedClipStack;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.resource.ResourceType;
import net.minecraft.resource.SynchronousResourceReloader;
import net.minecraft.util.Identifier;

public final class Render2D {
   private static boolean field16024;
   private static boolean field16025;
   private static String field16027 = "∅∆∏∑√∞∫≈≠₡₣₤₦₩₫₭₮₱₲₴₵₸₹₺₼₽ƏƒƠƷǅǎǓǤǯǺȞȫȷʒѢѫѲҊҝҠӿḈḝṚẅẎẓẞΔΩμπ№™℮‰†‡ĐđĦħĿŀŁłŊŋŒœŦŧĲĳĸſǆǇǈǉǊǋǌǪǫǮǻǼǽǾǿȀȁȂȃȄȅȆȇȈȉȊȋȌȍȎȏȐȑȒȓȘșȚțȟȪȬȭȰȱȲȳəʹʻʾˆˇ˘˙˚˛˜˝ЀЂЃЄЅІЇЈЉЊЋЌЍЎЏѣѳѴѵҋҌҍҎҏҐґҒғҔҕҖҗҘҙҚқҜҡҢңҤҥҦҧҨҩҪҫҬҭҮүҰұҲҳҴҵҶҷҸҹҺһҼҽҾҿӀӁӂӃӄӆӇӈӉӊӋӌӍӎӏӔӕӘәӚӛӜӝӞӟӠӡӢӣӤӥӦӧӨөӪӫӬӭӮӯӰӱӲӳӴӵӶӷӸӹӺӻӼӽӾԯḉḌḍḎḏḔḕḖḗḜḠḡḤḥḪḫḮḯḶḷḺḻṂṃṄṅṆṇṈṉṌṍṎṏṐṑṒṓṛṞṟṠṡṢṣṤṥṦṧṨṩṬṭṮṯṸṹṺṻẀẁẂẃẄẏẒẗẠạẢảẤấẦầẨẩẪẫẬậẮắẰằẲẳẴẵẶặẸẹẺẻẼẽẾếỀềỂểỄễỆệỈỉỊịỌọỎỏỐốỒồỔổỖỗỘộỚớỜờỞởỠỡỢợỤụỦủỨứỪừỬửỮữỰựỲỳỴỵỶỷỸỹ⁰⁴⁵⁶⁷⁸⁹₀₁₂₃₄₅₆₇₈₉€₧ℓ′″⁄⁒•…アイウエオカキクケコサシスセソタチツテトナニヌネノハヒフヘホマミムメモヤユヨラリルレロワヲン天地火水木金土日月山川風雨雷電光闇魔剣龍神鬼王力心目手口人大小中上下左右前後東西南北春夏秋冬星空海島城門道車馬鳥魚虎狼熊蛇竜獣霊魂死生愛憎戦争平和";
   private static final HashMap<String, String> field16028 = new HashMap<>();
   private static final ArrayDeque<Float> field16029 = new ArrayDeque<>();
   private static float field16030 = 1.0F;

   public static void method4639(String local7820_0, String local7820_1, float local7820_2, float local7820_3, float local7820_4, int local7820_5) {
      method4773();
      if (local7820_1 != null && local7820_1.indexOf(167) >= 0) {
         StringBuilder stringBuilder = new StringBuilder();

         for (FormattedText.Nested0521 local7820_8 : FormattedText.method4200(local7820_1, local7820_5)) {
            String local7820_9 = local7820_8.text().indexOf(167) >= 0 ? FormattedText.method4203(local7820_8.text()) : local7820_8.text();
            if (!local7820_9.isEmpty()) {
               if (local7820_8.obfuscated()) {
                  method4718(
                     local7820_0,
                     local7820_9,
                     local7820_2 + method4738(local7820_0, stringBuilder.toString(), local7820_4),
                     local7820_3,
                     local7820_4,
                     local7820_8.color() & 16777215 | local7820_5 & 0xFF000000
                  );
               } else {
                  method4646(
                     local7820_0,
                     local7820_9,
                     local7820_2 + method4738(local7820_0, stringBuilder.toString(), local7820_4),
                     local7820_3,
                     local7820_4,
                     local7820_8.color() & 16777215 | local7820_5 & 0xFF000000
                  );
               }

               stringBuilder.append(local7820_9);
            }
         }
      } else {
         method4646(local7820_0, local7820_1, local7820_2, local7820_3, local7820_4, local7820_5);
      }
   }

   private static WarpTextRenderer method4640() {
      return WarpTextRenderer.method0813();
   }

   private static RectHalfIconRenderer method4641() {
      return RectHalfIconRenderer.method3933();
   }

   public static void method4642(
      String local7823_0,
      String local7823_1,
      float local7823_2,
      float local7823_3,
      float local7823_4,
      int local7823_5,
      float local7823_6,
      float local7823_7,
      float local7823_8
   ) {
      method4773();
      local7823_1 = NameProtectModule.method2868().method2866(local7823_1);
      String local7823_9 = method4760(local7823_0);
      if (local7823_9 != null) {
         method4783()
            .method2367(
               new MsdfTextDrawData(
                  local7823_9, local7823_1, local7823_2, local7823_3, local7823_4, method4756(local7823_5), local7823_6, local7823_7, local7823_8
               )
            );
      } else {
         method4651()
            .method3012(
               new RasterTextDrawData(
                  local7823_0,
                  local7823_1,
                  local7823_2,
                  local7823_3,
                  local7823_4,
                  local7823_5,
                  local7823_5,
                  local7823_5,
                  local7823_5,
                  local7823_6,
                  local7823_7,
                  local7823_8,
                  false,
                  false,
                  0.0F,
                  0.0F,
                  0.0F,
                  0.0F,
                  0.0F,
                  true
               )
            );
      }
   }

   public static void method4643(
      float local7824_0,
      float local7824_1,
      float local7824_2,
      float local7824_3,
      float local7824_4,
      float local7824_5,
      float local7824_6,
      float local7824_7,
      float local7824_8,
      int local7824_9,
      List<ArcColorSpan> local7824_10
   ) {
      method4773();
      local7824_9 = method4724(local7824_9);
      local7824_10 = method4735(local7824_10);
      method4763()
         .method2735(
            new Outline360DrawData(
               local7824_0, local7824_1, local7824_2, local7824_3, local7824_4, local7824_5, local7824_6, local7824_7, local7824_8, local7824_9, local7824_10
            )
         );
   }

   private static ImageRenderer method4644() {
      return ImageRenderer.method3174();
   }

   public static void method4645(
      float local7826_0, float local7826_1, float local7826_2, float local7826_3, float local7826_4, int local7826_5, float local7826_6, float local7826_7
   ) {
      method4722(new SoftGlowDrawData(local7826_0, local7826_1, local7826_2, local7826_3, local7826_4, local7826_5, local7826_6).withIntensity(local7826_7));
   }

   private static void method4646(String local7827_0, String local7827_1, float local7827_2, float local7827_3, float local7827_4, int local7827_5) {
      if (local7827_1 != null && !NameProtectModule.method2868().method2866(local7827_1).equals(local7827_1)) {
         String local7827_6 = NameProtectModule.method2868().method2866(local7827_1);
         String local7827_7 = NameProtectModule.method2868().numericTextSetting.getText();
         float local7827_8 = local7827_2;
         int local7827_9 = 0;

         while (local7827_9 < local7827_6.length()) {
            int local7827_10 = local7827_6.indexOf(local7827_7, local7827_9);
            if (local7827_10 < 0) {
               method4740(local7827_0, local7827_6.substring(local7827_9), local7827_8, local7827_3, local7827_4, local7827_5);
               return;
            }

            if (local7827_10 > local7827_9) {
               method4740(local7827_0, local7827_6.substring(local7827_9, local7827_10), local7827_8, local7827_3, local7827_4, local7827_5);
               local7827_8 += method4738(local7827_0, local7827_6.substring(local7827_9, local7827_10), local7827_4);
            }

            for (int index = 0; index < local7827_7.length(); index++) {
               method4740(
                  local7827_0,
                  String.valueOf(local7827_7.charAt(index)),
                  local7827_8,
                  local7827_3,
                  local7827_4,
                  NameProtectModule.method2868().method2870(index, local7827_5)
               );
               local7827_8 += method4738(local7827_0, String.valueOf(local7827_7.charAt(index)), local7827_4);
            }

            local7827_9 = local7827_10 + local7827_7.length();
         }
      } else {
         method4740(local7827_0, local7827_1, local7827_2, local7827_3, local7827_4, local7827_5);
      }
   }

   public static void method4647(String local7828_0, float local7828_1, float local7828_2, float local7828_3, float local7828_4, int... intArray) {
      int[] intArray2 = new int[intArray.length];

      for (int index = 0; index < intArray.length; index++) {
         intArray2[index] = method4724(intArray[index]);
      }

      method4644().method3184(new ImageDrawData(local7828_0, local7828_1, local7828_2, local7828_3, local7828_4).withColors(intArray2));
   }

   private static GlowCompositeRenderer method4648() {
      return GlowCompositeRenderer.method1853();
   }

   public static void method4649(int local7830_0) {
      BatchedBlurRenderer.method4508(local7830_0);
   }

   public static void method4650() {
      field16030 = field16029.isEmpty() ? 1.0F : field16029.pop();
   }

   private static TextAtlasRenderer method4651() {
      return TextAtlasRenderer.method3016();
   }

   public static void method4652() {
      ImageRenderer.method3174().method3179();
      MsdfTextRenderer.method2389().method2387();
      EffectIconRenderer.method1366().method1365();
      FontRegistry.clearCache();
      DeluxeClient.logger().info("[Render2D] textures reloaded");
   }

   private static CornerBracketRenderer method4653() {
      return CornerBracketRenderer.method4283();
   }

   public static void method4654(
      String local7835_0,
      String local7835_1,
      float local7835_2,
      float local7835_3,
      float local7835_4,
      int local7835_5,
      int local7835_6,
      int local7835_7,
      int local7835_8,
      float local7835_9,
      float local7835_10,
      float local7835_11
   ) {
      method4773();
      local7835_1 = NameProtectModule.method2868().method2866(local7835_1);
      String local7835_12 = method4760(local7835_0);
      if (local7835_12 != null) {
         method4783()
            .method2367(
               new MsdfTextDrawData(
                  local7835_12,
                  local7835_1,
                  local7835_2,
                  local7835_3,
                  local7835_4,
                  method4756(local7835_5),
                  method4756(local7835_6),
                  method4756(local7835_7),
                  method4756(local7835_8),
                  local7835_9,
                  local7835_10,
                  local7835_11
               )
            );
      } else {
         method4651()
            .method3012(
               new RasterTextDrawData(
                  local7835_0,
                  local7835_1,
                  local7835_2,
                  local7835_3,
                  local7835_4,
                  local7835_5,
                  local7835_6,
                  local7835_7,
                  local7835_8,
                  local7835_9,
                  local7835_10,
                  local7835_11,
                  false,
                  false,
                  0.0F,
                  0.0F,
                  0.0F,
                  0.0F,
                  0.0F,
                  true
               )
            );
      }
   }

   public static float[] method4655(String local7836_0, String local7836_1, float local7836_2) {
      return method4783().method2369(local7836_0, local7836_1, local7836_2);
   }

   private static OutlineDefaultRenderer method4656() {
      return OutlineDefaultRenderer.method1479();
   }

   public static void method4657(
      float local7839_0,
      float local7839_1,
      float local7839_2,
      float local7839_3,
      float local7839_4,
      float local7839_5,
      float local7839_6,
      float local7839_7,
      int local7839_8,
      int local7839_9
   ) {
      method4685(local7839_0, local7839_1, local7839_2, local7839_3, local7839_4, 0.5F, local7839_5, local7839_6, local7839_7, 25.0F, local7839_8, local7839_9);
   }

   private Render2D() {
   }

   public static void method4658(
      String local7841_0,
      float local7841_1,
      float local7841_2,
      float local7841_3,
      float local7841_4,
      float local7841_5,
      float local7841_6,
      float local7841_7,
      int local7841_8
   ) {
      method4644()
         .method3184(
            new ImageDrawData(local7841_0, local7841_1, local7841_2, local7841_3, local7841_4, method4724(local7841_8))
               .withRotation(local7841_5, local7841_6, local7841_7)
         );
   }

   public static void method4659(
      float local7842_0,
      float local7842_1,
      float local7842_2,
      float local7842_3,
      float local7842_4,
      int local7842_5,
      float local7842_6,
      float local7842_7,
      int local7842_8
   ) {
      float local7842_9 = local7842_2 - local7842_0;
      float local7842_10 = local7842_3 - local7842_1;
      float local7842_11 = (float)Math.sqrt(local7842_9 * local7842_9 + local7842_10 * local7842_10);
      if (!(local7842_11 < 0.01F) && local7842_5 >>> 24 != 0 && !(local7842_4 <= 0.0F)) {
         float local7842_12 = local7842_9 / local7842_11;
         float local7842_13 = local7842_10 / local7842_11;
         float local7842_14 = Math.max(0.5F, local7842_6 + local7842_7);
         int local7842_15 = (int)Math.ceil(local7842_11 / local7842_14);
         if (local7842_8 > 0 && local7842_15 > local7842_8) {
            local7842_15 = local7842_8;
         }

         for (int index = 0; index < local7842_15; index++) {
            float local7842_17 = index * local7842_14;
            float local7842_18 = Math.min(local7842_17 + local7842_6, local7842_11);
            if (!(local7842_18 <= local7842_17)) {
               method4679(
                  local7842_0 + local7842_12 * local7842_17,
                  local7842_1 + local7842_13 * local7842_17,
                  local7842_0 + local7842_12 * local7842_18,
                  local7842_1 + local7842_13 * local7842_18,
                  local7842_4,
                  local7842_5
               );
            }
         }
      }
   }

   public static void method4660(
      String local7843_0,
      String local7843_1,
      float local7843_2,
      float local7843_3,
      float local7843_4,
      int local7843_5,
      float local7843_6,
      float local7843_7,
      float local7843_8,
      float local7843_9
   ) {
      method4773();
      method4783()
         .method2384(
            new MsdfTextDrawData(local7843_0, local7843_1, local7843_2, local7843_3, local7843_4, local7843_5),
            local7843_6,
            local7843_7,
            local7843_8,
            local7843_9
         );
   }

   public static void method4661(DrawContext drawContext) {
      ScissorStack.method3972();
      if (drawContext != null) {
         drawContext.disableScissor();
      }
   }

   public static void method4662(
      float local7845_0, float local7845_1, float local7845_2, float local7845_3, float local7845_4, int local7845_5, float local7845_6
   ) {
      method4722(new SoftGlowDrawData(local7845_0, local7845_1, local7845_2, local7845_3, local7845_4, local7845_5, local7845_6));
   }

   private static DitherRenderer method4663() {
      return DitherRenderer.method3358();
   }

   public static void method4664(float local7847_0, float local7847_1, float local7847_2, float local7847_3, int local7847_4) {
      method4773();
      method4745().method4303(new CircleDrawData(local7847_0, local7847_1, local7847_2, method4724(local7847_4)).withThickness(local7847_3));
   }

   public static void method4665(float local7848_0, float local7848_1, float local7848_2, float local7848_3, float local7848_4, int local7848_5) {
      method4773();
      method4709()
         .method4473(new BlurDrawData(local7848_0, local7848_1, local7848_2, local7848_3, local7848_4, 1.0F, 36.0F).withColor(method4724(local7848_5)));
   }

   public static void method4666(
      float local7849_0,
      float local7849_1,
      float local7849_2,
      float local7849_3,
      float[] floatArray,
      int local7849_5,
      float local7849_6,
      float local7849_7,
      int local7849_8,
      float local7849_9,
      boolean local7849_10,
      float local7849_11,
      float local7849_12,
      float local7849_13,
      float local7849_14
   ) {
      method4773();
      method4697()
         .method2704(
            new GlassDrawData(
               local7849_0,
               local7849_1,
               local7849_2,
               local7849_3,
               floatArray,
               local7849_5,
               local7849_6,
               local7849_7,
               local7849_8,
               local7849_9,
               local7849_10,
               local7849_11,
               local7849_12,
               local7849_13,
               local7849_14
            )
         );
   }

   public static void method4667(
      float local7850_0,
      float local7850_1,
      float local7850_2,
      float local7850_3,
      float local7850_4,
      float local7850_5,
      float local7850_6,
      float local7850_7,
      int local7850_8,
      int local7850_9,
      String local7850_10,
      String local7850_11,
      float local7850_12,
      float local7850_13,
      float local7850_14,
      float local7850_15,
      float local7850_16,
      float local7850_17,
      float local7850_18,
      float local7850_19,
      long local7850_20
   ) {
      method4688(
         new HalfIconDrawData(
            local7850_0,
            local7850_1,
            local7850_2,
            local7850_3,
            local7850_4,
            local7850_5,
            local7850_6,
            local7850_7,
            local7850_8,
            local7850_8,
            local7850_8,
            local7850_8,
            0.0F,
            local7850_10,
            local7850_11,
            local7850_12,
            local7850_9,
            local7850_13,
            local7850_14,
            local7850_15,
            local7850_16,
            local7850_17,
            local7850_18,
            local7850_19,
            local7850_20
         )
      );
   }

   public static void method4668(GlowCompositeDrawData glowCompositeDrawData) {
      if (glowCompositeDrawData != null) {
         method4648().method1859(glowCompositeDrawData);
      }
   }

   public static void method4669(
      String local7852_0,
      String local7852_1,
      float local7852_2,
      float local7852_3,
      float local7852_4,
      int local7852_5,
      int local7852_6,
      int local7852_7,
      int local7852_8
   ) {
      method4773();
      local7852_1 = NameProtectModule.method2868().method2866(local7852_1);
      String local7852_9 = method4760(local7852_0);
      if (local7852_9 != null) {
         method4783()
            .method2367(
               new MsdfTextDrawData(
                  local7852_9,
                  local7852_1,
                  local7852_2,
                  local7852_3,
                  local7852_4,
                  method4756(local7852_5),
                  method4756(local7852_6),
                  method4756(local7852_7),
                  method4756(local7852_8)
               )
            );
      } else {
         method4651()
            .method3012(
               new RasterTextDrawData(local7852_0, local7852_1, local7852_2, local7852_3, local7852_4, local7852_5, local7852_6, local7852_7, local7852_8)
            );
      }
   }

   private static SoftGlowRenderer method4670() {
      return SoftGlowRenderer.method2483();
   }

   public static void method4671(
      float local7854_0,
      float local7854_1,
      float local7854_2,
      float local7854_3,
      float local7854_4,
      float local7854_5,
      float local7854_6,
      float local7854_7,
      float local7854_8,
      int local7854_9
   ) {
      method4773();
      method4656()
         .method1466(
            new OutlineDrawData(
               local7854_0, local7854_1, local7854_2, local7854_3, local7854_4, local7854_5, local7854_6, local7854_7, local7854_8, method4724(local7854_9)
            )
         );
   }

   public static void method4672(ShapeDrawData shapeDrawData) {
      method4773();
      method4693().method3918(shapeDrawData);
   }

   public static void method4673(
      String local7856_0,
      float local7856_1,
      float local7856_2,
      float local7856_3,
      float local7856_4,
      float local7856_5,
      float local7856_6,
      float local7856_7,
      int local7856_8
   ) {
      method4644()
         .method3184(
            new ImageDrawData(local7856_0, local7856_1, local7856_2, local7856_3, local7856_4, method4724(local7856_8))
               .withRotation(local7856_5, local7856_6, local7856_7)
               .withGlow()
         );
   }

   public static void method4674(
      String local7857_0,
      String local7857_1,
      float local7857_2,
      float local7857_3,
      float local7857_4,
      float local7857_5,
      float local7857_6,
      float local7857_7,
      float local7857_8
   ) {
      method4773();
      method4783()
         .method2384(
            new MsdfTextDrawData(local7857_0, local7857_1, local7857_2, local7857_3, local7857_4, -1), local7857_5, local7857_6, local7857_7, local7857_8
         );
   }

   public static void method4675(GlassOutlineDrawData glassOutlineDrawData) {
      method4773();
      method4684().method2428(glassOutlineDrawData);
   }

   public static void method4676(float local7859_0, float local7859_1, float local7859_2, float local7859_3, float local7859_4, float local7859_5) {
      method4731(local7859_0, local7859_1, local7859_2, local7859_3, local7859_4, local7859_5, 1.0F, -1);
   }

   public static void method4677(
      String local7860_0,
      float local7860_1,
      float local7860_2,
      float local7860_3,
      float local7860_4,
      float local7860_5,
      float local7860_6,
      float local7860_7,
      float local7860_8,
      float local7860_9,
      float local7860_10,
      int local7860_11
   ) {
      if (!(local7860_3 <= 0.0F) && !(local7860_4 <= 0.0F)) {
         method4644()
            .method3184(
               new ImageDrawData(local7860_0, local7860_1, local7860_2, local7860_3, local7860_4, local7860_5, method4724(local7860_11))
                  .withSmoothness(local7860_6)
                  .withUv(local7860_7, local7860_8, local7860_9, local7860_10)
                  .withNearest()
            );
      }
   }

   public static void method4678(DrawContext drawContext, float local7861_1, float local7861_2, float local7861_3, float local7861_4) {
      if (drawContext == null) {
         ScissorStack.method3968(local7861_1, local7861_2, local7861_3, local7861_4);
      } else {
         ScissorStack.method3967(GuiScale.method0795(drawContext), local7861_1, local7861_2, local7861_3, local7861_4);
         drawContext.enableScissor(
            GuiScale.method0798(local7861_1),
            GuiScale.method0798(local7861_2),
            GuiScale.method0798(local7861_1 + local7861_3),
            GuiScale.method0798(local7861_2 + local7861_4)
         );
      }
   }

   public static void method4679(float local7862_0, float local7862_1, float local7862_2, float local7862_3, float local7862_4, int local7862_5) {
      method4770().method1275(new LineDrawData(local7862_0, local7862_1, local7862_2, local7862_3, local7862_4, local7862_5));
   }

   public static void method4680(
      float local7863_0,
      float local7863_1,
      float local7863_2,
      float local7863_3,
      float local7863_4,
      float local7863_5,
      float local7863_6,
      float local7863_7,
      int local7863_8
   ) {
      method4698(
         local7863_0,
         local7863_1,
         local7863_2,
         local7863_3,
         new float[]{local7863_6 * local7863_4 / 2.0F, local7863_6 * local7863_4 / 2.0F, local7863_6 * local7863_4 / 2.0F, local7863_6 * local7863_4 / 2.0F},
         local7863_7,
         local7863_8,
         (local7863_8 >>> 24 & 0xFF) / 255.0F,
         local7863_3 == 240.0F ? 100.0F : 50.0F,
         local7863_8 | 0xFF000000,
         1.0F,
         true,
         0.0F,
         local7863_5,
         local7863_4,
         0.0F
      );
   }

   public static void method4681(RegistryEntry<StatusEffect> local7864_0, float local7864_1, float local7864_2, float local7864_3, int local7864_4) {
      method4766().method1368(new EffectIconDraw(local7864_0, local7864_1, local7864_2, local7864_3, method4724(local7864_4)));
   }

   public static void method4682(float local7865_0, float local7865_1, float local7865_2, float local7865_3, int local7865_4) {
      method4773();
      method4745().method4303(new CircleDrawData(local7865_0, local7865_1, local7865_2, method4724(local7865_4)).withSmoothness(local7865_3));
   }

   public static void method4683(StatusEffectInstance statusEffectInstance, float local7866_1, float local7866_2, float local7866_3, int local7866_4) {
      if (statusEffectInstance != null && statusEffectInstance.shouldShowIcon()) {
         method4766().method1368(new EffectIconDraw(statusEffectInstance, local7866_1, local7866_2, local7866_3, method4724(local7866_4)));
      }
   }

   private static OutlineGlassRenderer method4684() {
      return OutlineGlassRenderer.method2432();
   }

   public static void method4685(
      float local7868_0,
      float local7868_1,
      float local7868_2,
      float local7868_3,
      float local7868_4,
      float local7868_5,
      float local7868_6,
      float local7868_7,
      float local7868_8,
      float local7868_9,
      int local7868_10,
      int local7868_11
   ) {
      method4750()
         .method4351(
            new RippleDrawData(
               local7868_0,
               local7868_1,
               local7868_2,
               local7868_3,
               local7868_4,
               local7868_4,
               local7868_4,
               local7868_4,
               local7868_5,
               local7868_6,
               local7868_7,
               local7868_8,
               local7868_9,
               local7868_10,
               local7868_11,
               false
            )
         );
   }

   public static void method4686(
      DrawContext drawContext, String local7869_1, String local7869_2, float local7869_3, float local7869_4, float local7869_5, int local7869_6
   ) {
      method4713(drawContext);
      method4639(local7869_1, local7869_2, local7869_3, local7869_4, local7869_5, local7869_6);
      method4754();
   }

   public static void method4687(
      float local7870_0,
      float local7870_1,
      float local7870_2,
      float local7870_3,
      float local7870_4,
      float local7870_5,
      float local7870_6,
      float local7870_7,
      int local7870_8,
      float local7870_9,
      float local7870_10,
      int local7870_11,
      float local7870_12,
      boolean local7870_13,
      float local7870_14,
      float local7870_15,
      float local7870_16,
      float local7870_17
   ) {
      method4773();
      method4697()
         .method2704(
            new GlassDrawData(
               local7870_0,
               local7870_1,
               local7870_2,
               local7870_3,
               local7870_4,
               local7870_5,
               local7870_6,
               local7870_7,
               local7870_8,
               local7870_9,
               local7870_10,
               local7870_11,
               local7870_12,
               local7870_13,
               local7870_14,
               local7870_15,
               local7870_16,
               local7870_17
            )
         );
   }

   public static void method4688(HalfIconDrawData halfIconDrawData) {
      method4773();
      if (halfIconDrawData != null && halfIconDrawData.backgroundVisible()) {
         method4696(
            halfIconDrawData.x(),
            halfIconDrawData.y(),
            halfIconDrawData.width(),
            halfIconDrawData.height(),
            halfIconDrawData.radiusTopLeft(),
            halfIconDrawData.radiusTopRight(),
            halfIconDrawData.radiusBottomRight(),
            halfIconDrawData.radiusBottomLeft(),
            halfIconDrawData.colorTopLeft(),
            halfIconDrawData.colorTopRight(),
            halfIconDrawData.colorBottomRight(),
            halfIconDrawData.colorBottomLeft()
         );
      }

      if (halfIconDrawData != null && halfIconDrawData.iconsVisible()) {
         method4641().method3940(halfIconDrawData);
      }
   }

   public static void method4689(
      float local7872_0, float local7872_1, float local7872_2, float local7872_3, float local7872_4, int local7872_5, float local7872_6, float local7872_7
   ) {
      method4722(
         new SoftGlowDrawData(local7872_0, local7872_1, local7872_2, local7872_3, local7872_4, local7872_5, local7872_6).withIntensity(local7872_7).asRing()
      );
   }

   public static void method4690(OutlineDrawData outlineDrawData) {
      method4773();
      method4656().method1466(outlineDrawData);
   }

   public static void method4691(
      float local7874_0,
      float local7874_1,
      float local7874_2,
      float local7874_3,
      float local7874_4,
      int local7874_5,
      int local7874_6,
      int local7874_7,
      int local7874_8
   ) {
      method4696(
         local7874_0,
         local7874_1,
         local7874_2,
         local7874_3,
         local7874_4,
         local7874_4,
         local7874_4,
         local7874_4,
         local7874_5,
         local7874_6,
         local7874_7,
         local7874_8
      );
   }

   public static void method4692(DrawContext drawContext) {
      if (drawContext != null) {
         ((GuiLayerSerialAccess)((DrawContextMixin)drawContext).getGuiRenderState()).bumpLayerSerial();
      }
   }

   private static ShapeRenderer method4693() {
      return ShapeRenderer.method3903();
   }

   public static void method4694(
      String local7877_0,
      String local7877_1,
      float local7877_2,
      float local7877_3,
      float local7877_4,
      int local7877_5,
      float local7877_6,
      float local7877_7,
      float local7877_8,
      float local7877_9,
      float local7877_10
   ) {
      method4773();
      method4783()
         .method2367(
            new MsdfTextDrawData(local7877_0, local7877_1, local7877_2, local7877_3, local7877_4, method4756(local7877_5))
               .withHorizontalFade(local7877_6, local7877_7, local7877_8, local7877_9, local7877_10)
         );
   }

   public static void method4695(
      String local7878_0,
      float local7878_1,
      float local7878_2,
      float local7878_3,
      float local7878_4,
      float local7878_5,
      float local7878_6,
      float local7878_7,
      float local7878_8,
      float local7878_9,
      int local7878_10
   ) {
      if (!(local7878_3 <= 0.0F) && !(local7878_4 <= 0.0F)) {
         method4644()
            .method3184(
               new ImageDrawData(local7878_0, local7878_1, local7878_2, local7878_3, local7878_4, local7878_5, method4724(local7878_10))
                  .withUv(local7878_6, local7878_7, local7878_8, local7878_9)
            );
      }
   }

   public static void method4696(
      float local7879_0,
      float local7879_1,
      float local7879_2,
      float local7879_3,
      float local7879_4,
      float local7879_5,
      float local7879_6,
      float local7879_7,
      int local7879_8,
      int local7879_9,
      int local7879_10,
      int local7879_11
   ) {
      method4773();
      method4739()
         .method4249(
            new RectDrawData(
               local7879_0,
               local7879_1,
               local7879_2,
               local7879_3,
               local7879_4,
               local7879_5,
               local7879_6,
               local7879_7,
               method4724(local7879_8),
               method4724(local7879_9),
               method4724(local7879_10),
               method4724(local7879_11),
               0.5F
            )
         );
   }

   private static GlassRenderer method4697() {
      return GlassRenderer.method2703();
   }

   public static void method4698(
      float local7881_0,
      float local7881_1,
      float local7881_2,
      float local7881_3,
      float[] floatArray,
      float local7881_5,
      int local7881_6,
      float local7881_7,
      float local7881_8,
      int local7881_9,
      float local7881_10,
      boolean local7881_11,
      float local7881_12,
      float local7881_13,
      float local7881_14,
      float local7881_15
   ) {
      method4773();
      method4684()
         .method2428(
            new GlassOutlineDrawData(
               local7881_0,
               local7881_1,
               local7881_2,
               local7881_3,
               floatArray,
               local7881_5,
               local7881_6,
               local7881_7,
               local7881_8,
               local7881_9,
               local7881_10,
               local7881_11,
               local7881_12,
               local7881_13,
               local7881_14,
               local7881_15
            )
         );
   }

   public static void method4699(Outline360DrawData outline360DrawData) {
      method4773();
      method4763().method2735(outline360DrawData);
   }

   public static ArcColorSpan method4700(float local7883_0, float local7883_1, int local7883_2) {
      return ArcColorSpan.of(local7883_0, local7883_1, local7883_2);
   }

   public static void method4701(
      String local7884_0,
      String local7884_1,
      float local7884_2,
      float local7884_3,
      float local7884_4,
      float local7884_5,
      float local7884_6,
      float local7884_7,
      float local7884_8,
      float local7884_9,
      int local7884_10
   ) {
      FontAtlas fontAtlas = FontRegistry.getAtlas(local7884_0);
      if (fontAtlas != null) {
         float[] floatArray = fontAtlas.method1712(local7884_1, local7884_4);

         for (int index = 0; index < floatArray.length; index += 8) {
            floatArray[index] += local7884_5;
            floatArray[index + 1] = floatArray[index + 1] + local7884_5;
            floatArray[index + 2] = floatArray[index + 2] + local7884_5;
            floatArray[index + 3] = floatArray[index + 3] + local7884_5;
         }

         method4640()
            .method0808(
               new WarpTextDrawData(
                  local7884_2 - local7884_5,
                  local7884_3 - local7884_5,
                  fontAtlas.method1713(local7884_1, local7884_4) + local7884_5 * 2.0F,
                  fontAtlas.method1708(local7884_4) + local7884_5 * 2.0F,
                  floatArray,
                  local7884_6 - local7884_2 + local7884_5,
                  local7884_7 - local7884_3 + local7884_5,
                  local7884_8,
                  local7884_9,
                  fontAtlas.method1716(),
                  fontAtlas.method1715(),
                  method4724(local7884_10)
               )
            );
      }
   }

   public static void method4702(float local7885_0, float local7885_1, float local7885_2, float local7885_3, float local7885_4) {
      method4773();
      method4709().method4473(new BlurDrawData(local7885_0, local7885_1, local7885_2, local7885_3, local7885_4, 1.0F, 36.0F).withColor(method4724(-1)));
   }

   private static ArcDividerRenderer method4703() {
      return ArcDividerRenderer.method3734();
   }

   public static void method4704(
      String local7887_0,
      String local7887_1,
      float local7887_2,
      float local7887_3,
      float local7887_4,
      int local7887_5,
      float local7887_6,
      float local7887_7,
      float local7887_8
   ) {
      method4773();
      method4783()
         .method2367(
            new MsdfTextDrawData(
               local7887_0, local7887_1, local7887_2, local7887_3, local7887_4, method4756(local7887_5), local7887_6, local7887_7, local7887_8
            )
         );
   }

   public static void method4705(float local7888_0) {
      field16029.push(field16030);
      field16030 = field16030 * Math.max(0.0F, Math.min(1.0F, local7888_0));
   }

   private static void method4706() {
      if (!field16024) {
         Identifier identifier = Identifier.of("deluxe", "render2d_msdf_cache");
         ResourceLoader.get(ResourceType.CLIENT_RESOURCES)
            .registerReloader(identifier, (SynchronousResourceReloader)resourceManager -> MsdfTextRenderer.method2378());
         field16024 = true;
      }
   }

   public static void method4708(
      float local7891_0,
      float local7891_1,
      float local7891_2,
      float local7891_3,
      float local7891_4,
      float local7891_5,
      int local7891_6,
      int local7891_7,
      int local7891_8,
      int local7891_9
   ) {
      method4781(
         local7891_0,
         local7891_1,
         local7891_2,
         local7891_3,
         local7891_4,
         local7891_4,
         local7891_4,
         local7891_4,
         local7891_5,
         local7891_6,
         local7891_7,
         local7891_8,
         local7891_9
      );
   }

   private static BatchedBlurRenderer method4709() {
      return BatchedBlurRenderer.method4504();
   }

   public static void method4710(
      float local7893_0, float local7893_1, float local7893_2, float local7893_3, float local7893_4, float local7893_5, float local7893_6, int local7893_7
   ) {
      method4747().method3142(new ArcDividerDrawData(local7893_0, local7893_1, local7893_2, local7893_3, local7893_4, local7893_5, local7893_6, local7893_7));
   }

   public static void method4711(
      float local7894_0,
      float local7894_1,
      float local7894_2,
      float local7894_3,
      float local7894_4,
      int local7894_5,
      float local7894_6,
      float local7894_7,
      int local7894_8,
      float local7894_9,
      float local7894_10
   ) {
      float local7894_11 = local7894_2 - local7894_0;
      float local7894_12 = local7894_3 - local7894_1;
      float local7894_13 = (float)Math.sqrt(local7894_11 * local7894_11 + local7894_12 * local7894_12);
      if (!(local7894_13 < 0.01F) && local7894_5 >>> 24 != 0 && !(local7894_4 <= 0.0F)) {
         float local7894_14 = local7894_11 / local7894_13;
         float local7894_15 = local7894_12 / local7894_13;
         float local7894_16 = Math.max(0.5F, local7894_6 + local7894_7);
         int local7894_17 = (int)Math.ceil(local7894_13 / local7894_16);
         if (local7894_8 > 0 && local7894_17 > local7894_8) {
            local7894_17 = local7894_8;
         }

         for (int index = 0; index < local7894_17; index++) {
            float local7894_19 = index * local7894_16;
            float local7894_20 = Math.min(local7894_19 + local7894_6, local7894_13);
            if (!(local7894_20 <= local7894_19)) {
               float local7894_21 = method4771(local7894_19 / local7894_13, local7894_9, local7894_10);
               float local7894_22 = method4771(local7894_20 / local7894_13, local7894_9, local7894_10);
               method4761(
                  local7894_0 + local7894_14 * local7894_19,
                  local7894_1 + local7894_15 * local7894_19,
                  local7894_0 + local7894_14 * local7894_20,
                  local7894_1 + local7894_15 * local7894_20,
                  local7894_4,
                  local7894_5,
                  local7894_21,
                  local7894_22
               );
            }
         }
      }
   }

   public static void method4712(
      float local7895_0,
      float local7895_1,
      float local7895_2,
      float local7895_3,
      float local7895_4,
      float local7895_5,
      float local7895_6,
      int local7895_7,
      int local7895_8,
      int local7895_9,
      int local7895_10
   ) {
      method4773();
      method4709()
         .method4522(
            new BlurDrawData(local7895_0, local7895_1, local7895_2, local7895_3, local7895_4, local7895_6, local7895_5)
               .withColors(local7895_7, local7895_8, local7895_9, local7895_10)
         );
   }

   public static void method4713(DrawContext drawContext) {
      if (!field16025) {
         field16025 = true;
         field16030 = 1.0F;
         field16029.clear();
         ScissorStack.method3971();
         RoundedClipStack.method4829();
         method4709().method4491(drawContext);
         method4697().method2698(drawContext);
         method4693().method3916(drawContext);
         method4684().method2433(drawContext);
         method4745().method4297(drawContext);
         method4653().method4277(drawContext);
         method4774().method3054(drawContext);
         method4703().method3750(drawContext);
         method4752().method0929(drawContext);
         method4739().method4242(drawContext);
         method4641().method3945(drawContext);
         method4656().method1478(drawContext);
         method4763().method2721(drawContext);
         method4651().method3014(drawContext);
         method4783().method2386(drawContext);
         method4747().method3143(drawContext);
         method4663().method3343(drawContext);
         method4784().method2398(drawContext);
         method4640().method0802(drawContext);
         method4644().method3182(drawContext);
         method4770().method1274(drawContext);
         method4766().method1364(drawContext);
         method4750().method4360(drawContext);
         method4648().method1816(drawContext);
         method4670().method2488(drawContext);
         ItemRenderCache.method2339(drawContext);
      }
   }

   public static void method4714() {
      BatchedBlurRenderer.method4519();
      GlassRenderer.method2711();
      ShapeRenderer.method3905();
      OutlineGlassRenderer.method2434();
      CircleRenderer.method4299();
      CornerBracketRenderer.method4267();
      BlobRenderer.method3057();
      ArcDividerRenderer.method3749();
      PickerRenderer.method0927();
      RectDefaultRenderer.method4246();
      RectHalfIconRenderer.method3941();
      OutlineDefaultRenderer.method1472();
      Outline360Renderer.method2716();
      TextAtlasRenderer.method3017();
      MsdfTextRenderer.method2363();
      ShimmerRenderer.method3151();
      DitherRenderer.method3342();
      MenuGridRenderer.method2401();
      WarpTextRenderer.method0810();
      ImageRenderer.method3167();
      LineRenderer.method1278();
      EffectIconRenderer.method1372();
      RippleRenderer.method4354();
      GlowCompositeRenderer.method1842();
      SoftGlowRenderer.method2481();
      ThemePaletteBuffer.method2153();
      DeluxeClient.logger().info("[Render2D] Render2D closed");
   }

   public static void method4715(float local7898_0, float local7898_1, float local7898_2, float local7898_3, float local7898_4, int local7898_5) {
      method4773();
      method4739().method4249(new RectDrawData(local7898_0, local7898_1, local7898_2, local7898_3, local7898_4, method4724(local7898_5)));
   }

   public static void method4716(
      float local7899_0,
      float local7899_1,
      float local7899_2,
      float local7899_3,
      float local7899_4,
      float local7899_5,
      float local7899_6,
      float local7899_7,
      float local7899_8,
      int local7899_9,
      ArcColorSpan... recoveredClass0065Array
   ) {
      method4773();
      method4643(
         local7899_0,
         local7899_1,
         local7899_2,
         local7899_3,
         local7899_4,
         local7899_5,
         local7899_6,
         local7899_7,
         local7899_8,
         local7899_9,
         Arrays.asList(recoveredClass0065Array)
      );
   }

   public static void method4717(
      String local7900_0,
      float local7900_1,
      float local7900_2,
      float local7900_3,
      float local7900_4,
      float local7900_5,
      float local7900_6,
      float local7900_7,
      int local7900_8
   ) {
      int local7900_9 = method4724(local7900_8);
      method4644()
         .method3184(
            new ImageDrawData(
               local7900_0,
               local7900_1,
               local7900_2,
               local7900_3,
               local7900_4,
               local7900_5,
               local7900_6,
               local7900_7,
               local7900_9,
               local7900_9,
               local7900_9,
               local7900_9
            )
         );
   }

   public static void method4718(String local7901_0, String local7901_1, float local7901_2, float local7901_3, float local7901_4, int local7901_5) {
      String local7901_6 = method4775(local7901_0);
      method4783().method2385(true);
      float local7901_7 = local7901_2;
      int local7901_8 = 0;

      while (local7901_8 < local7901_1.length()) {
         int local7901_9 = local7901_1.codePointAt(local7901_8);
         local7901_8 += Character.charCount(local7901_9);
         if (!Character.isWhitespace(local7901_9)) {
            String local7901_10 = String.valueOf(
               local7901_6.charAt((int)(((System.currentTimeMillis() / 10L + local7901_8 * 1337L) * -7046029254386353131L >>> 10) % local7901_6.length()))
            );
            method4639(
               local7901_0,
               local7901_10,
               local7901_7
                  + (method4738(local7901_0, new String(Character.toChars(local7901_9)), local7901_4) - method4738(local7901_0, local7901_10, local7901_4))
                     / 2.0F,
               local7901_3,
               local7901_4,
               local7901_5
            );
         }

         local7901_7 += method4738(local7901_0, new String(Character.toChars(local7901_9)), local7901_4);
      }

      method4783().method2385(false);
   }

   public static void method4719(float local7902_0, float local7902_1, float local7902_2, int local7902_3) {
      method4773();
      method4745().method4303(new CircleDrawData(local7902_0, local7902_1, local7902_2, method4724(local7902_3)));
   }

   public static void method4720(float local7903_0, float local7903_1, float local7903_2, float local7903_3, float local7903_4) {
      method4773();
      method4752().method0920(PickerDrawData.hue(local7903_0, local7903_1, local7903_2, local7903_3, local7903_4 * field16030));
   }

   public static void method4721(
      float local7904_0,
      float local7904_1,
      float local7904_2,
      float local7904_3,
      float local7904_4,
      int local7904_5,
      int local7904_6,
      String local7904_7,
      String local7904_8,
      float local7904_9,
      float local7904_10,
      float local7904_11,
      float local7904_12
   ) {
      method4688(
         new HalfIconDrawData(
            local7904_0,
            local7904_1,
            local7904_2,
            local7904_3,
            local7904_4,
            local7904_5,
            local7904_7,
            local7904_8,
            local7904_9,
            local7904_6,
            local7904_10,
            local7904_11,
            local7904_12
         )
      );
   }

   public static void method4722(SoftGlowDrawData softGlowDrawData) {
      if (softGlowDrawData != null) {
         method4773();
         method4670().method2495(softGlowDrawData);
      }
   }

   public static void method4723(
      float local7906_0,
      float local7906_1,
      float local7906_2,
      float local7906_3,
      float local7906_4,
      float local7906_5,
      int local7906_6,
      List<ArcColorSpan> local7906_7
   ) {
      method4773();
      method4763()
         .method2735(
            new Outline360DrawData(
               local7906_0, local7906_1, local7906_2, local7906_3, local7906_4, local7906_5, method4724(local7906_6), method4735(local7906_7)
            )
         );
   }

   public static int method4724(int local7907_0) {
      if (field16030 >= 0.999F) {
         return local7907_0;
      }

      int local7907_1 = Math.round((local7907_0 >>> 24 & 0xFF) * field16030);
      return local7907_1 << 24 | local7907_0 & 16777215;
   }

   public static void method4725(
      float local7908_0, float local7908_1, float local7908_2, float local7908_3, float local7908_4, float local7908_5, float local7908_6, int local7908_7
   ) {
      method4773();
      method4709()
         .method4506(
            new BlurDrawData(local7908_0, local7908_1, local7908_2, local7908_3, local7908_4, local7908_6, local7908_5)
               .withColor(method4724(local7908_7))
         );
   }

   public static void method4726(GlassDrawData glassDrawData) {
      method4773();
      method4697().method2704(glassDrawData);
   }

   public static void method4727(float local7910_0, float local7910_1, float local7910_2, float local7910_3, int local7910_4) {
      method4715(local7910_0, local7910_1, local7910_2, local7910_3, 0.0F, local7910_4);
   }

   public static void method4728() {
      method4706();
      DeluxeClient.logger().info("[Render2D] Render2D initialized");
   }

   public static void method4729(String local7912_0, float local7912_1, float local7912_2, float local7912_3, float local7912_4) {
      method4644().method3184(new ImageDrawData(local7912_0, local7912_1, local7912_2, local7912_3, local7912_4));
   }

   public static void method4730(
      DrawContext drawContext, float local7913_1, float local7913_2, float local7913_3, float local7913_4, float local7913_5, int local7913_6
   ) {
      method4713(drawContext);
      method4715(local7913_1, local7913_2, local7913_3, local7913_4, local7913_5, local7913_6);
      method4754();
   }

   public static void method4731(
      float local7914_0, float local7914_1, float local7914_2, float local7914_3, float local7914_4, float local7914_5, float local7914_6, int local7914_7
   ) {
      method4773();
      method4709()
         .method4522(new BlurDrawData(local7914_0, local7914_1, local7914_2, local7914_3, local7914_4, local7914_6, local7914_5).withColor(local7914_7));
   }

   public static void method4732(RasterTextDrawData rasterTextDrawData) {
      method4773();
      if (rasterTextDrawData != null && !rasterTextDrawData.hasHorizontalFade() && Math.abs(rasterTextDrawData.rotationDegrees()) < 0.001F) {
         String local7915_1 = method4760(rasterTextDrawData.fontName());
         if (local7915_1 != null) {
            method4783()
               .method2367(
                  new MsdfTextDrawData(
                     local7915_1,
                     rasterTextDrawData.text(),
                     rasterTextDrawData.x(),
                     rasterTextDrawData.y(),
                     rasterTextDrawData.size(),
                     method4756(rasterTextDrawData.colorTopLeft()),
                     method4756(rasterTextDrawData.colorTopRight()),
                     method4756(rasterTextDrawData.colorBottomRight()),
                     method4756(rasterTextDrawData.colorBottomLeft())
                  )
               );
            return;
         }
      }

      method4651().method3012(rasterTextDrawData);
   }

   public static void method4733(MsdfTextDrawData msdfTextDrawData) {
      method4773();
      if (msdfTextDrawData != null) {
         method4783()
            .method2367(
               new MsdfTextDrawData(
                     msdfTextDrawData.fontName(),
                     msdfTextDrawData.text(),
                     msdfTextDrawData.x(),
                     msdfTextDrawData.y(),
                     msdfTextDrawData.size(),
                     method4756(msdfTextDrawData.colorTopLeft()),
                     method4756(msdfTextDrawData.colorTopRight()),
                     method4756(msdfTextDrawData.colorBottomRight()),
                     method4756(msdfTextDrawData.colorBottomLeft()),
                     msdfTextDrawData.rotationDegrees(),
                     msdfTextDrawData.rotationOriginX(),
                     msdfTextDrawData.rotationOriginY()
                  )
                  .withHorizontalFade(
                     msdfTextDrawData.fadeLeftX(),
                     msdfTextDrawData.fadeRightX(),
                     msdfTextDrawData.fadeWidth(),
                     msdfTextDrawData.fadeLeftStrength(),
                     msdfTextDrawData.fadeRightStrength()
                  )
            );
      }
   }

   public static void method4734(
      DrawContext drawContext, float local7917_1, float local7917_2, float local7917_3, float local7917_4, float local7917_5, float local7917_6
   ) {
      method4713(drawContext);
      method4676(local7917_1, local7917_2, local7917_3, local7917_4, local7917_5, local7917_6);
      method4754();
   }

   private static List<ArcColorSpan> method4735(List<ArcColorSpan> local7918_0) {
      if (!(field16030 >= 0.999F) && local7918_0 != null && !local7918_0.isEmpty()) {
         List<ArcColorSpan> local7918_1 = new ArrayList<>(local7918_0.size());

         for (ArcColorSpan arcColorSpan : local7918_0) {
            local7918_1.add(new ArcColorSpan(arcColorSpan.startDegrees(), arcColorSpan.endDegrees(), method4724(arcColorSpan.color())));
         }

         return local7918_1;
      } else {
         return local7918_0;
      }
   }

   public static boolean method4736(String local7919_0) {
      return method4644().method3169(local7919_0);
   }

   public static void method4737(
      String local7920_0,
      String local7920_1,
      float local7920_2,
      float local7920_3,
      float local7920_4,
      int local7920_5,
      float local7920_6,
      float local7920_7,
      float local7920_8,
      float local7920_9,
      float local7920_10
   ) {
      method4773();
      local7920_1 = NameProtectModule.method2868().method2866(local7920_1);
      if (local7920_1 != null && local7920_1.indexOf(167) >= 0) {
         StringBuilder stringBuilder = new StringBuilder();

         for (FormattedText.Nested0521 local7920_13 : FormattedText.method4200(local7920_1, local7920_5)) {
            String local7920_14 = local7920_13.text().indexOf(167) >= 0 ? FormattedText.method4203(local7920_13.text()) : local7920_13.text();
            if (!local7920_14.isEmpty()) {
               if (local7920_13.obfuscated()) {
                  method4718(
                     local7920_0,
                     local7920_14,
                     local7920_2 + method4738(local7920_0, stringBuilder.toString(), local7920_4),
                     local7920_3,
                     local7920_4,
                     local7920_13.color() & 16777215 | local7920_5 & 0xFF000000
                  );
               } else {
                  method4737(
                     local7920_0,
                     local7920_14,
                     local7920_2 + method4738(local7920_0, stringBuilder.toString(), local7920_4),
                     local7920_3,
                     local7920_4,
                     local7920_13.color() & 16777215 | local7920_5 & 0xFF000000,
                     local7920_6,
                     local7920_7,
                     local7920_8,
                     local7920_9,
                     local7920_10
                  );
               }

               stringBuilder.append(local7920_14);
            }
         }
      } else {
         String local7920_11 = method4760(local7920_0);
         if (local7920_11 != null) {
            method4783()
               .method2367(
                  new MsdfTextDrawData(local7920_11, local7920_1, local7920_2, local7920_3, local7920_4, method4756(local7920_5))
                     .withHorizontalFade(local7920_6, local7920_7, local7920_8, local7920_9, local7920_10)
               );
         } else {
            method4651()
               .method3012(
                  new RasterTextDrawData(local7920_0, local7920_1, local7920_2, local7920_3, local7920_4, local7920_5)
                     .withHorizontalFade(local7920_6, local7920_7, local7920_8, local7920_9, local7920_10)
               );
         }
      }
   }

   public static float method4738(String local7921_0, String local7921_1, float local7921_2) {
      if (local7921_1 != null && local7921_1.indexOf(167) >= 0) {
         local7921_1 = FormattedText.method4203(local7921_1);
      }

      local7921_1 = NameProtectModule.method2868().method2866(local7921_1);
      String local7921_3 = method4760(local7921_0);
      return local7921_3 != null
         ? method4783().method2359(local7921_3, local7921_1, local7921_2)
         : method4651().method3006(local7921_0, local7921_1, local7921_2);
   }

   private static RectDefaultRenderer method4739() {
      return RectDefaultRenderer.method4243();
   }

   private static void method4740(String local7923_0, String local7923_1, float local7923_2, float local7923_3, float local7923_4, int local7923_5) {
      String local7923_6 = method4760(local7923_0);
      if (local7923_6 != null) {
         method4783().method2367(new MsdfTextDrawData(local7923_6, local7923_1, local7923_2, local7923_3, local7923_4, method4756(local7923_5)));
      } else {
         method4651().method3012(new RasterTextDrawData(local7923_0, local7923_1, local7923_2, local7923_3, local7923_4, local7923_5));
      }
   }

   public static void method4741(RegistryEntry<StatusEffect> local7924_0, float local7924_1, float local7924_2, float local7924_3) {
      method4766().method1368(new EffectIconDraw(local7924_0, local7924_1, local7924_2, local7924_3, method4724(-1)));
   }

   public static void method4742(
      float local7925_0, float local7925_1, float local7925_2, float local7925_3, float local7925_4, int local7925_5, int local7925_6
   ) {
      method4773();
      method4653().method4268(new CornerBracketDrawData(local7925_0, local7925_1, local7925_2, local7925_3, local7925_4, local7925_5, method4724(local7925_6)));
   }

   public static void method4743(StatusEffectInstance statusEffectInstance, float local7926_1, float local7926_2, float local7926_3) {
      if (statusEffectInstance != null && statusEffectInstance.shouldShowIcon()) {
         method4766().method1368(new EffectIconDraw(statusEffectInstance, local7926_1, local7926_2, local7926_3, method4724(-1)));
      }
   }

   public static void method4744(RectDrawData rectDrawData) {
      method4739().method4249(rectDrawData);
   }

   private static CircleRenderer method4745() {
      return CircleRenderer.method4292();
   }

   public static void method4746(String local7929_0, String local7929_1, float local7929_2, float local7929_3, float local7929_4, int local7929_5) {
      method4773();
      method4783().method2367(new MsdfTextDrawData(local7929_0, local7929_1, local7929_2, local7929_3, local7929_4, method4756(local7929_5)));
   }

   private static ShimmerRenderer method4747() {
      return ShimmerRenderer.method3145();
   }

   public static void method4748(
      float local7931_0, float local7931_1, float local7931_2, float local7931_3, float local7931_4, float local7931_5, float local7931_6, float local7931_7
   ) {
      method4773();
      method4709()
         .method4473(
            new BlurDrawData(
               local7931_0, local7931_1, local7931_2, local7931_3, local7931_4, local7931_5, local7931_6, local7931_7, 1.0F, 36.0F, method4724(-1)
            )
         );
   }

   public static void method4749(
      float local7932_0,
      float local7932_1,
      float local7932_2,
      float local7932_3,
      float local7932_4,
      float local7932_5,
      int local7932_6,
      ArcColorSpan... recoveredClass0065Array
   ) {
      method4773();
      method4723(local7932_0, local7932_1, local7932_2, local7932_3, local7932_4, local7932_5, local7932_6, Arrays.asList(recoveredClass0065Array));
   }

   private static RippleRenderer method4750() {
      return RippleRenderer.method4355();
   }

   public static void method4751(
      String local7934_0,
      String local7934_1,
      float local7934_2,
      float local7934_3,
      float local7934_4,
      int local7934_5,
      float local7934_6,
      float local7934_7,
      float local7934_8,
      boolean local7934_9,
      boolean local7934_10
   ) {
      method4737(
         local7934_0,
         local7934_1,
         local7934_2,
         local7934_3,
         local7934_4,
         local7934_5,
         local7934_6,
         local7934_7,
         local7934_8,
         local7934_9 ? 1.0F : 0.0F,
         local7934_10 ? 1.0F : 0.0F
      );
   }

   private static PickerRenderer method4752() {
      return PickerRenderer.method0930();
   }

   public static void method4753(float local7936_0, float local7936_1, float local7936_2, float local7936_3, int local7936_4, float local7936_5) {
      method4773();
      method4752().method0920(PickerDrawData.alpha(local7936_0, local7936_1, local7936_2, local7936_3, local7936_4, local7936_5 * field16030));
   }

   public static void method4754() {
      if (field16025) {
         field16025 = false;
         method4709().method4527();
         method4697().method2714();
         method4693().method3917();
         method4684().method2419();
         method4745().method4294();
         method4653().method4269();
         method4774().method3050();
         method4703().method3746();
         method4752().method0932();
         method4739().method4241();
         method4641().method3946();
         method4656().method1483();
         method4763().method2737();
         method4651().method3011();
         method4783().method2360();
         method4747().method3146();
         method4663().method3353();
         method4784().method2395();
         method4640().method0815();
         method4644().method3183();
         method4770().method1271();
         method4766().method1367();
         method4750().method4361();
         method4648().method1849();
         method4670().method2493();
         ItemRenderCache.method2348();
      }
   }

   public static void method4755(
      float local7938_0,
      float local7938_1,
      float local7938_2,
      float local7938_3,
      float local7938_4,
      float local7938_5,
      float local7938_6,
      float local7938_7,
      float local7938_8,
      int local7938_9
   ) {
      method4784()
         .method2400(
            new MenuGridDrawData(
               local7938_0, local7938_1, local7938_2, local7938_3, local7938_4, local7938_5, local7938_6, local7938_7, local7938_8, local7938_9
            )
         );
   }

   private static int method4756(int local7939_0) {
      return method4724(local7939_0);
   }

   public static void method4757(BlurDrawData blurDrawData) {
      method4773();
      method4709().method4522(blurDrawData);
   }

   public static void method4758(
      float local7941_0,
      float local7941_1,
      float local7941_2,
      float local7941_3,
      float local7941_4,
      float local7941_5,
      float local7941_6,
      float local7941_7,
      float local7941_8,
      float local7941_9,
      float local7941_10,
      float local7941_11,
      float local7941_12,
      int local7941_13
   ) {
      method4773();
      method4774()
         .method3053(
            new BlobDrawData(
               local7941_0,
               local7941_1,
               local7941_2,
               local7941_3,
               local7941_4,
               local7941_5,
               local7941_6,
               local7941_7,
               local7941_8,
               0.25F,
               local7941_9,
               local7941_10,
               local7941_11,
               local7941_12,
               method4724(local7941_13)
            )
         );
   }

   public static void method4759(
      float local7942_0,
      float local7942_1,
      float local7942_2,
      float local7942_3,
      float local7942_4,
      float local7942_5,
      float local7942_6,
      float local7942_7,
      int local7942_8,
      int local7942_9
   ) {
      method4750()
         .method4351(
            new RippleDrawData(
               local7942_0,
               local7942_1,
               local7942_2,
               local7942_3,
               local7942_4,
               local7942_4,
               local7942_4,
               local7942_4,
               0.5F,
               local7942_5,
               local7942_6,
               local7942_7,
               25.0F,
               local7942_8,
               local7942_9,
               true
            )
         );
   }

   private static String method4760(String local7943_0) {
      if (local7943_0 != null && !local7943_0.isBlank()) {
         String local7943_1 = local7943_0.trim().toLowerCase(Locale.ROOT).replace('-', '_').replace(' ', '_');

         return switch (local7943_1) {
            case "montserrat", "montserrat_regular" -> "montserrat-regular";
            case "montserrat_medium" -> "montserrat-medium";
            case "montserrat_semibold", "montserrat_semi_bold" -> "montserrat-semibold";
            case "montserrat_bold" -> "montserrat-bold";
            case "montserrat_extrabold", "montserrat_extra_bold" -> "montserrat-extrabold";
            case "montserrat_black" -> "montserrat-black";
            case "montserrat_light" -> "montserrat-light";
            case "montserrat_extralight", "montserrat_extra_light" -> "montserrat-extralight";
            case "montserrat_thin" -> "montserrat-thin";
            case "sf", "sf_regular" -> "sf-regular";
            case "sf_medium" -> "sf-medium";
            case "sf_bold" -> "sf-bold";
            case "kimiko" -> "kimiko";
            case "event_icons", "events_icons", "eventicons" -> "event-icons";
            case "icons", "icon" -> "icons";
            case "test", "semibold" -> "test";
            case "thin" -> "thin";
            case "mini", "light" -> "mini";
            case "semi", "medium" -> "semi";
            case "semi_bold", "semi-bold" -> "semi_bold";
            case "bold" -> "bold";
            default -> null;
         };
      } else {
         return null;
      }
   }

   public static void method4761(
      float local7944_0, float local7944_1, float local7944_2, float local7944_3, float local7944_4, int local7944_5, float local7944_6, float local7944_7
   ) {
      method4770().method1275(new LineDrawData(local7944_0, local7944_1, local7944_2, local7944_3, local7944_4, local7944_5, local7944_6, local7944_7));
   }

   public static float method4762(String local7945_0, String local7945_1, float local7945_2) {
      return method4783().method2359(local7945_0, local7945_1, local7945_2);
   }

   private static Outline360Renderer method4763() {
      return Outline360Renderer.method2731();
   }

   public static void method4764(
      float local7947_0, float local7947_1, float local7947_2, float local7947_3, float local7947_4, float local7947_5, float local7947_6, int local7947_7
   ) {
      method4663().method3348(new DitherDrawData(local7947_0, local7947_1, local7947_2, local7947_3, local7947_4, local7947_5, local7947_6, local7947_7));
   }

   public static BlurDrawBuilder method4765() {
      return new BlurDrawBuilder();
   }

   private static EffectIconRenderer method4766() {
      return EffectIconRenderer.method1366();
   }

   public static void method4767(
      float local7950_0,
      float local7950_1,
      float local7950_2,
      float local7950_3,
      float local7950_4,
      float local7950_5,
      float local7950_6,
      float local7950_7,
      int local7950_8
   ) {
      method4773();
      method4739()
         .method4249(
            new RectDrawData(
               local7950_0, local7950_1, local7950_2, local7950_3, local7950_4, local7950_5, local7950_6, local7950_7, method4724(local7950_8)
            )
         );
   }

   public static void method4768(
      float local7951_0,
      float local7951_1,
      float local7951_2,
      float local7951_3,
      float local7951_4,
      int local7951_5,
      float local7951_6,
      float local7951_7,
      int local7951_8,
      float local7951_9,
      boolean local7951_10,
      float local7951_11,
      float local7951_12,
      float local7951_13,
      float local7951_14
   ) {
      method4687(
         local7951_0,
         local7951_1,
         local7951_2,
         local7951_3,
         local7951_4,
         local7951_4,
         local7951_4,
         local7951_4,
         local7951_5,
         local7951_6,
         local7951_7,
         local7951_8,
         local7951_9,
         local7951_10,
         local7951_11,
         local7951_12,
         local7951_13,
         local7951_14
      );
   }

   public static void method4769(
      float local7952_0, float local7952_1, float local7952_2, float local7952_3, float local7952_4, float local7952_5, int local7952_6
   ) {
      method4773();
      method4656().method1466(new OutlineDrawData(local7952_0, local7952_1, local7952_2, local7952_3, local7952_4, local7952_5, method4724(local7952_6)));
   }

   private static LineRenderer method4770() {
      return LineRenderer.method1280();
   }

   private static float method4771(float local7954_0, float local7954_1, float local7954_2) {
      local7954_0 = Math.max(0.0F, Math.min(1.0F, local7954_0));
      float local7954_3 = local7954_0 < 0.5F ? local7954_1 : local7954_2;
      float local7954_4 = 1.0F - Math.abs(local7954_0 - 0.5F) * 2.0F;
      return local7954_3 + (1.0F - local7954_3) * local7954_4;
   }

   public static void method4772(float local7955_0, float local7955_1, float local7955_2, float local7955_3, float local7955_4, int local7955_5) {
      method4773();
      method4703().method3737(new ShimmerDrawData(local7955_0, local7955_1, local7955_2, local7955_3, local7955_4, 0.85F, local7955_5));
   }

   private static void method4773() {
      ImageRenderer.method3174().method3181();
      EffectIconRenderer.method1366().method1370();
   }

   private static BlobRenderer method4774() {
      return BlobRenderer.method3064();
   }

   private static String method4775(String local7958_0) {
      String local7958_1 = method4760(local7958_0);
      if (local7958_1 == null) {
         return field16027;
      }

      String local7958_2 = field16028.get(local7958_1);
      if (local7958_2 != null) {
         return local7958_2;
      }

      StringBuilder stringBuilder = new StringBuilder(field16027.length());

      for (int index = 0; index < field16027.length(); index++) {
         if (method4783().method2388(local7958_1, field16027.charAt(index))) {
            stringBuilder.append(field16027.charAt(index));
         }
      }

      local7958_2 = stringBuilder.length() < 16 ? field16027 : stringBuilder.toString();
      field16028.put(local7958_1, local7958_2);
      return local7958_2;
   }

   public static void method4776(
      String local7959_0,
      String local7959_1,
      float local7959_2,
      float local7959_3,
      float local7959_4,
      int local7959_5,
      int local7959_6,
      int local7959_7,
      int local7959_8
   ) {
      method4773();
      method4783()
         .method2367(
            new MsdfTextDrawData(
               local7959_0,
               local7959_1,
               local7959_2,
               local7959_3,
               local7959_4,
               method4756(local7959_5),
               method4756(local7959_6),
               method4756(local7959_7),
               method4756(local7959_8)
            )
         );
   }

   public static void method4777(
      String local7960_0, float local7960_1, float local7960_2, float local7960_3, float local7960_4, float local7960_5, int local7960_6
   ) {
      method4644().method3184(new ImageDrawData(local7960_0, local7960_1, local7960_2, local7960_3, local7960_4, local7960_5, method4724(local7960_6)));
   }

   public static void method4778(
      DrawContext drawContext,
      float local7961_1,
      float local7961_2,
      float local7961_3,
      float local7961_4,
      float local7961_5,
      float local7961_6,
      int local7961_7
   ) {
      method4713(drawContext);
      method4769(local7961_1, local7961_2, local7961_3, local7961_4, local7961_5, local7961_6, local7961_7);
      method4754();
   }

   public static void method4779(float local7962_0, float local7962_1, float local7962_2, float local7962_3, float local7962_4) {
      method4731(local7962_0, local7962_1, local7962_2, local7962_3, local7962_4, 16.0F, 1.0F, -1);
   }

   public static void method4780(float local7963_0, float local7963_1, int local7963_2, float local7963_3, float local7963_4) {
      method4722(new SoftGlowDrawData(local7963_0, local7963_1, 0.0F, 0.0F, 0.0F, local7963_2, local7963_3).withIntensity(local7963_4));
   }

   public static void method4781(
      float local7964_0,
      float local7964_1,
      float local7964_2,
      float local7964_3,
      float local7964_4,
      float local7964_5,
      float local7964_6,
      float local7964_7,
      float local7964_8,
      int local7964_9,
      int local7964_10,
      int local7964_11,
      int local7964_12
   ) {
      method4773();
      method4656()
         .method1466(
            new OutlineDrawData(
               local7964_0,
               local7964_1,
               local7964_2,
               local7964_3,
               local7964_4,
               local7964_5,
               local7964_6,
               local7964_7,
               local7964_8,
               method4724(local7964_9),
               method4724(local7964_10),
               method4724(local7964_11),
               method4724(local7964_12),
               0.5F,
               null
            )
         );
   }

   public static void method4782(
      float local7965_0, float local7965_1, float local7965_2, float local7965_3, float local7965_4, float local7965_5, float local7965_6, int local7965_7
   ) {
      method4666(
         local7965_0,
         local7965_1,
         local7965_2,
         local7965_3,
         new float[]{local7965_6 * local7965_4 / 2.0F, local7965_6 * local7965_4 / 2.0F, local7965_6 * local7965_4 / 2.0F, local7965_6 * local7965_4 / 2.0F},
         local7965_7,
         (local7965_7 >>> 24 & 0xFF) / 255.0F,
         local7965_3 == 240.0F ? 100.0F : 50.0F,
         local7965_7 | 0xFF000000,
         1.0F,
         true,
         0.0F,
         local7965_5,
         local7965_4,
         0.0F
      );
   }

   private static MsdfTextRenderer method4783() {
      return MsdfTextRenderer.method2389();
   }

   private static MenuGridRenderer method4784() {
      return MenuGridRenderer.method2408();
   }
}
