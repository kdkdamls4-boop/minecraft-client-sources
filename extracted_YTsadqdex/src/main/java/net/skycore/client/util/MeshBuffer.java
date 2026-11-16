package net.skycore.client.util;

import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexFormat;

public final class MeshBuffer implements AutoCloseable {
   private final MeshData mesh;
   private final ByteBufferBuilder allocator;

   private MeshBuffer(ByteBufferBuilder allocator2, MeshData mesh2) {
      this.allocator = allocator2;
      this.mesh = mesh2;
   }

   public MeshData mesh() {
      return this.mesh;
   }

   public static MeshBuffer build(int value, PrimitiveTopology primitiveTopology, VertexFormat vertexFormat, MeshBuffer.Writer writer) {
      ByteBufferBuilder byteBufferBuilder = ByteBufferBuilder.exactlySized(Math.max(1, value));

      try {
         BufferBuilder bufferBuilder = new BufferBuilder(byteBufferBuilder, primitiveTopology, vertexFormat);
         writer.write(bufferBuilder);
         MeshData meshData = bufferBuilder.build();
         if (meshData == null) {
            byteBufferBuilder.close();
            return null;
         } else {
            return new MeshBuffer(byteBufferBuilder, meshData);
         }
      } catch (Throwable failure) {
         byteBufferBuilder.close();
         throw failure;
      }
   }

   // $VF: Could not verify finally blocks. A semaphore variable has been added to preserve control flow.
   // Please report this to the Vineflower issue tracker, at https://github.com/Vineflower/vineflower/issues with a copy of the class file (if you have the rights to distribute it!)
   @Override
   public void close() {
      boolean condition = false /* VF: Semaphore variable */;

      try {
         condition = true;
         this.mesh.close();
         condition = false;
      } finally {
         if (condition) {
            this.allocator.close();
         }
      }

      this.allocator.close();
   }

   @FunctionalInterface
   public interface Writer {
      public void write(BufferBuilder bufferBuilder);
   }
}
