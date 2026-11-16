package net.skycore.client.mixin;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import net.minecraft.server.packs.AbstractPackResources;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.resources.ResourceMetadata;
import net.skycore.client.util.PackMetadataNormalizer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(AbstractPackResources.class)
public abstract class AbstractPackResourcesReadLegacyServerMetadataMixin {
   @Redirect(
      method = "loadMetadata",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/server/packs/resources/ResourceMetadata;fromJsonStream(Ljava/io/InputStream;)Lnet/minecraft/server/packs/resources/ResourceMetadata;"
      )
   )
   private static ResourceMetadata skycore$readLegacyServerMetadata(InputStream inputStream, PackResources packResources) throws IOException {
      if (!packResources.location().id().startsWith("server/")) {
         return ResourceMetadata.fromJsonStream(inputStream);
      }

      byte[] byteValues = inputStream.readAllBytes();
      JsonObject asJsonObject = JsonParser.parseString(new String(byteValues, StandardCharsets.UTF_8)).getAsJsonObject();
      PackMetadataNormalizer.normalize(asJsonObject);
      return ResourceMetadata.fromJsonStream(new ByteArrayInputStream(asJsonObject.toString().getBytes(StandardCharsets.UTF_8)));
   }
}
