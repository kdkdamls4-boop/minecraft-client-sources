package net.skycore.client.util;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.MeshData;
import java.nio.ByteBuffer;

public final class UploadHelper implements AutoCloseable {
   private GpuBuffer buffer;
   private int capacity;
   private final String label;

   public GpuBuffer upload(MeshData meshData) {
      ByteBuffer byteBuffer;
      int value;
      bufferCapacityCheck: {
         byteBuffer = meshData.vertexBuffer();
         value = byteBuffer.remaining();
         int intermediate = -1;
         if (this.buffer != null) {
            intermediate = (byte)-1;
            if (this.capacity >= value) {
               break bufferCapacityCheck;
            }
         }

         if (this.buffer != null) {
            this.buffer.close();
         }

         if (this.capacity == 0) {
            intermediate = value;
         } else {
            intermediate = this.capacity * 2;
         }

         int maximum = Math.max(value, intermediate);
         this.capacity = maximum;
         this.buffer = RenderSystem.getDevice().createBuffer(() -> {
            return this.label;
         }, 40, this.capacity);
      }

      GpuDevice device = RenderSystem.getDevice();
      device.createCommandEncoder().writeToBuffer(this.buffer.slice(0L, value), byteBuffer);
      return this.buffer;
   }

   @Override
   public void close() {
      if (this.buffer != null) {
         this.buffer.close();
         this.buffer = null;
         this.capacity = 0;
      }
   }

   public UploadHelper(String label2) {
      this.label = label2;
   }

   public GpuBuffer buffer() {
      return this.buffer;
   }
}
