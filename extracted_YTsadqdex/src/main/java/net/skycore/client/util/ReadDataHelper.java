package net.skycore.client.util;

import com.google.gson.Gson;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2FloatOpenHashMap;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;

public final class ReadDataHelper {
   private Identifier atlasId;
   private String name;
   private Identifier dataId;
   private static final Gson GSON = new Gson();

   public ReadDataHelper atlas(String text) {
      String text2 = Objects.requireNonNull(text, "name");
      Identifier texture = Identifier.parse("skycore:fonts/" + text2 + ".png");
      return this.atlas(texture);
   }

   public ReadDataHelper data(Identifier texture) {
      Identifier texture2 = Objects.requireNonNull(texture, "id");
      this.dataId = texture2;
      return this;
   }

   public ReadDataHelper atlas(Identifier atlasId2) {
      this.atlasId = Objects.requireNonNull(atlasId2, "id");
      return this;
   }

   public ReadDataHelper data(String text) {
      String text2 = "skycore:fonts/" + Objects.requireNonNull(text, "name") + ".json";
      Identifier texture = Identifier.parse(text2);
      return this.data(texture);
   }

   private static KerningsHelper readData(Identifier texture) {

      try {
         ResourceManager resourceManager = MinecraftContext.mc.getResourceManager();

         try (BufferedReader bufferedReader = resourceManager.openAsReader(texture)) {
            KerningsHelper kerningsHelper = GSON.fromJson(bufferedReader, KerningsHelper.class);
            if (kerningsHelper == null) {
               String text = String.valueOf(texture);
               String text2 = "Empty MSDF font metadata: " + text;
               throw new IllegalStateException(text2);
            } else {
               return kerningsHelper;
            }
         }
      } catch (IOException failure) {
         String text3 = String.valueOf(texture);
         text3 = "Failed to read MSDF font metadata: " + text3;
         throw new IllegalStateException(text3, failure);
      }
   }

   private static void validate(KerningsHelper kerningsHelper, Identifier texture) {
      DistanceRangeMiddleHelper distanceRangeMiddleHelper = kerningsHelper.atlas();
      AscentHelper ascentHelper = kerningsHelper.metrics();
      if (distanceRangeMiddleHelper != null && ascentHelper != null) {
         if (kerningsHelper.glyphs() != null && !kerningsHelper.glyphs().isEmpty()) {
            String text = distanceRangeMiddleHelper.type();
            if ("msdf".equalsIgnoreCase(text) && !(distanceRangeMiddleHelper.distanceRange() <= 0.0F)) {
               if (!(distanceRangeMiddleHelper.size() <= 0.0F) && distanceRangeMiddleHelper.width() > 0 && distanceRangeMiddleHelper.height() > 0 && !(ascentHelper.lineHeight() <= 0.0F)) {
                  return;
               }
            }

            throw new IllegalStateException("Unsupported MSDF font metadata: " + texture);
         }
      }

      throw new IllegalStateException("Invalid MSDF font metadata: " + texture);
   }

   public ReadDataHelper name(String name2) {
      this.name = Objects.requireNonNull(name2, "name");
      return this;
   }

   public TextHeightHelper build() {
      if (this.atlasId != null && this.dataId != null) {
         KerningsHelper kerningsHelper = readData(this.dataId);
         validate(kerningsHelper, this.dataId);
         DistanceRangeMiddleHelper distanceRangeMiddleHelper = kerningsHelper.atlas();
         Int2ObjectOpenHashMap lookup = new Int2ObjectOpenHashMap(kerningsHelper.glyphs().size());

         for (FontGlyph advanceHelper2 : kerningsHelper.glyphs()) {
            lookup.put(advanceHelper2.unicode(), new HeightHelper(advanceHelper2, distanceRangeMiddleHelper.width(), distanceRangeMiddleHelper.height()));
         }

         Long2FloatOpenHashMap lookup2 = new Long2FloatOpenHashMap();
         lookup2.defaultReturnValue(0.0F);
         var unicode2Helpers = kerningsHelper.kernings();
         if (unicode2Helpers != null) {
            for (Unicode2Helper unicode2Helper : unicode2Helpers) {
               lookup2.put(TextHeightHelper.packKerningKey(unicode2Helper.unicode1(), unicode2Helper.unicode2()), unicode2Helper.advance());
            }
         }

         return new TextHeightHelper(this.name != null ? this.name : this.dataId.toString(), this.atlasId, distanceRangeMiddleHelper, kerningsHelper.metrics(), lookup, lookup2);
      } else {
         throw new IllegalStateException("MSDF font needs both atlas and data resources");
      }
   }
}
