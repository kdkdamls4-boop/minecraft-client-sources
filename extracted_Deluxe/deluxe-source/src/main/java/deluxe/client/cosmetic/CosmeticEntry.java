package deluxe.client.cosmetic;

import net.minecraft.util.Identifier;

public record CosmeticEntry(
   int id,
   CosmeticCategory category,
   String name,
   boolean beta,
   String modelResource,
   String textureResource,
   String previewResource,
   CosmeticPlacement placement
) {
   public Identifier textureId() {
      return this.textureResource == null ? null : Identifier.of("deluxe", "cosmetics/" + this.textureResource);
   }

   public Identifier previewId() {
      return Identifier.of("deluxe", "cosmetics/" + this.previewResource);
   }
}
