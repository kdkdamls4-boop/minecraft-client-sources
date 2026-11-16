package net.skycore.client.util;

import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;

public final class EnsureHelper {
   private static int vertexCount;
   private static GpuBuffer buffer;

   private static void ensure() {
      if (buffer == null) {
         try (ByteBufferBuilder vertexSize = ByteBufferBuilder.exactlySized(6 * DefaultVertexFormat.POSITION_TEX.getVertexSize())) {
            BufferBuilder bufferBuilder = new BufferBuilder(vertexSize, PrimitiveTopology.TRIANGLES, DefaultVertexFormat.POSITION_TEX);
            bufferBuilder.addVertex(-1.0F, -1.0F, 0.0F).setUv(0.0F, 0.0F);
            bufferBuilder.addVertex(1.0F, -1.0F, 0.0F).setUv(1.0F, 0.0F);
            bufferBuilder.addVertex(1.0F, 1.0F, 0.0F).setUv(1.0F, 1.0F);
            bufferBuilder.addVertex(1.0F, 1.0F, 0.0F).setUv(1.0F, 1.0F);
            bufferBuilder.addVertex(-1.0F, 1.0F, 0.0F).setUv(0.0F, 1.0F);
            bufferBuilder.addVertex(-1.0F, -1.0F, 0.0F).setUv(0.0F, 0.0F);

            try (MeshData meshData = bufferBuilder.buildOrThrow()) {
               vertexCount = meshData.drawState().vertexCount();
               buffer = RenderSystem.getDevice().createBuffer(() -> "Skycore fullscreen quad", 32, meshData.vertexBuffer());
            }
         }
      }
   }

   private EnsureHelper() {
   }

   public static int vertexCount() {
      ensure();
      return vertexCount;
   }

   public static GpuBuffer buffer() {
      ensure();
      return buffer;
   }
}
