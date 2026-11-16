package deluxe.client.render;

import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;

public final class DeluxeVertexFormats {
   public static final VertexFormat field13710 = VertexFormat.builder()
      .add("Position", VertexFormatElement.POSITION)
      .add("Color", VertexFormatElement.COLOR)
      .add("UV0", VertexFormatElement.UV0)
      .add("UV1", VertexFormatElement.UV1)
      .add("UV2", VertexFormatElement.UV2)
      .add("Normal", VertexFormatElement.NORMAL)
      .add("LineWidth", VertexFormatElement.LINE_WIDTH)
      .padding(1)
      .build();
   public static final VertexFormat field13711 = VertexFormat.builder()
      .add("Position", VertexFormatElement.POSITION)
      .add("UV0", VertexFormatElement.UV0)
      .add("LineWidth", VertexFormatElement.LINE_WIDTH)
      .build();

   private DeluxeVertexFormats() {
   }
}
