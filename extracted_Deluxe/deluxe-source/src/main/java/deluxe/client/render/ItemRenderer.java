package deluxe.client.render;

import deluxe.client.DeluxeClient;
import deluxe.mixin.DrawContextMixin;
import deluxe.mixin.ItemRenderStateMixin;
import deluxe.mixin.ItemRenderState_LayerRenderStateMixin;
import deluxe.client.util.GuiScale;
import deluxe.client.render.GuiLayerSerialAccess;
import deluxe.client.render.ItemDrawData;
import deluxe.client.render.CachedItemGeometry;
import deluxe.client.render.ScissorStack;
import deluxe.client.render.ItemGeometryQuad;
import deluxe.client.util.TextureBinding;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.UniformType;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.render.state.GuiRenderState;
import net.minecraft.client.gui.render.state.ItemGuiElementRenderState;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.client.render.item.KeyedItemRenderState;
import net.minecraft.client.render.item.ItemRenderState.Glint;
import net.minecraft.client.render.item.ItemRenderState.LayerRenderState;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.render.model.json.Transformation;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.client.texture.TextureSetup;
import net.minecraft.client.util.math.Vector2f;
import net.minecraft.component.ComponentMap;
import net.minecraft.item.Item;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;
import org.joml.Matrix3x2f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.lwjgl.system.MemoryUtil;

public final class ItemRenderer implements AutoCloseable {
   private static volatile ItemRenderer itemRenderer;
   private static final VertexFormat field05254 = VertexFormat.builder()
      .add("Position", VertexFormatElement.POSITION)
      .add("UV0", VertexFormatElement.UV0)
      .add("Color", VertexFormatElement.COLOR)
      .add("LineWidth", VertexFormatElement.LINE_WIDTH)
      .build();
   public static final RenderPipeline field05255 = RenderPipeline.builder()
      .withLocation(method1653("pipeline/item"))
      .withVertexShader(method1653("core/item"))
      .withFragmentShader(method1653("core/item"))
      .withSampler("Sampler0")
      .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
      .withUniform("Projection", UniformType.UNIFORM_BUFFER)
      .withUniform("ItemParamsArray", UniformType.UNIFORM_BUFFER)
      .withBlend(BlendFunction.TRANSLUCENT)
      .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
      .withCull(false)
      .withVertexFormat(field05254, DrawMode.QUADS)
      .build();
   private final Map<Identifier, ItemRenderer.Nested0402> field05256 = new HashMap<>();
   private final Map<ItemRenderer.Nested0396, CachedItemGeometry> field05257 = new LinkedHashMap<ItemRenderer.Nested0396, CachedItemGeometry>(
      256, 0.75F, true
   ) {
      @Override
      protected boolean removeEldestEntry(Entry<ItemRenderer.Nested0396, CachedItemGeometry> local2517_1) {
         return this.size() > 512;
      }
   };
   private final Map<ItemRenderer.Nested0404, CachedItemGeometry> field05258 = new LinkedHashMap<ItemRenderer.Nested0404, CachedItemGeometry>(
      256, 0.75F, true
   ) {
      @Override
      protected boolean removeEldestEntry(Entry<ItemRenderer.Nested0404, CachedItemGeometry> local2543_1) {
         return this.size() > 512;
      }
   };
   private ByteBuffer field05259;
   private final Map<ItemRenderer.Nested0397, GuiItemRenderState> field05260 = new LinkedHashMap<>(32);
   private final Map<ItemGuiElementRenderState, Integer> field05261 = new IdentityHashMap<>();
   private Integer field05262;
   private final float[] field05263 = new float[4096];
   private int field05264;
   private DrawContext field05265;
   private GpuBuffer field05266;
   private boolean field05267;

   private static Identifier method1653(String local2409_0) {
      return Identifier.of("deluxe", local2409_0);
   }

   private CachedItemGeometry method1654(MinecraftClient minecraftClient, ItemDrawData itemDrawData) {
      ItemRenderer.Nested0396 local2410_3 = ItemRenderer.Nested0396.of(itemDrawData.stack(), itemDrawData.seed());
      CachedItemGeometry cachedItemGeometry = this.field05257.get(local2410_3);
      if (cachedItemGeometry != null) {
         return cachedItemGeometry;
      }

      KeyedItemRenderState itemRenderState = new KeyedItemRenderState();
      minecraftClient.getItemModelManager()
         .clearAndUpdate(itemRenderState, itemDrawData.stack(), ItemDisplayContext.GUI, minecraftClient.world, minecraftClient.player, itemDrawData.seed());
      ItemRenderer.Nested0404 local2410_6 = new ItemRenderer.Nested0404(itemRenderState.getModelKey(), itemDrawData.seed());
      CachedItemGeometry cachedItemGeometry2 = this.field05258.get(local2410_6);
      if (cachedItemGeometry2 == null) {
         cachedItemGeometry2 = this.method1681(itemRenderState);
         this.field05258.put(local2410_6, cachedItemGeometry2);
      }

      if (!itemRenderState.isAnimated()) {
         this.field05257.put(local2410_3, cachedItemGeometry2);
      }

      return cachedItemGeometry2;
   }

   private boolean method1655(LayerRenderState layerRenderState) {
      return layerRenderState != null && ((ItemRenderState_LayerRenderStateMixin)layerRenderState).getSpecialRenderer() != null;
   }

   public void method1656(ItemDrawData itemDrawData) {
      this.method1688(this.field05265, itemDrawData);
   }

   public void method1657(ItemGuiElementRenderState itemGuiElementRenderState) {
      if (this.field05262 != null) {
         this.field05261.put(itemGuiElementRenderState, this.field05262);
      }
   }

   private static int method1658(int local2415_0, float local2415_1) {
      local2415_0 = method1684(local2415_0);
      int local2415_2 = local2415_0 >>> 24 & 0xFF;
      int local2415_3 = method1674(Math.round((local2415_0 >>> 16 & 0xFF) * local2415_1), 0, 255);
      int local2415_4 = method1674(Math.round((local2415_0 >>> 8 & 0xFF) * local2415_1), 0, 255);
      int local2415_5 = method1674(Math.round((local2415_0 & 0xFF) * local2415_1), 0, 255);
      return local2415_2 << 24 | local2415_3 << 16 | local2415_4 << 8 | local2415_5;
   }

   private ByteBuffer method1659(boolean local2416_1) {
      if (this.field05259 == null) {
         this.field05259 = MemoryUtil.memCalloc(65536);
      }

      ByteBuffer byteBuffer = this.field05259;
      byteBuffer.clear();
      float local2416_3 = (float)(System.nanoTime() % 30000000000L) / 1.0E9F;

      for (int index = 0; index < this.field05264; index++) {
         int local2416_5 = index * 4 * 4;
         byteBuffer.putFloat(local2416_5, this.field05263[index]);
         byteBuffer.putFloat(local2416_5 + 4, local2416_3);
         byteBuffer.putFloat(local2416_5 + 8, 0.0F);
         byteBuffer.putFloat(local2416_5 + 12, 0.0F);
      }

      int local2416_4 = this.field05264 * 4 * 4;
      if (local2416_1) {
         if (local2416_4 < 65536) {
            MemoryUtil.memSet(MemoryUtil.memAddress(byteBuffer) + local2416_4, 0, 65536 - local2416_4);
         }

         byteBuffer.position(0).limit(65536);
      } else {
         byteBuffer.position(0).limit(Math.max(16, local2416_4));
      }

      return byteBuffer;
   }

   private GpuBuffer method1660() {
      if (this.field05266 != null && !this.field05266.isClosed() && this.field05266.size() >= 65536L) {
         return this.field05266;
      }

      this.method1675();

      try {
         this.field05266 = RenderSystem.getDevice().createBuffer(() -> "deluxe_item_params", 136, 65536L);
         return this.field05266;
      } catch (RuntimeException exception) {
         return null;
      }
   }

   public boolean method1661(RenderPipeline renderPipeline) {
      return renderPipeline == field05255;
   }

   private void method1662(
      GuiRenderState guiRenderState,
      int local2419_2,
      Matrix3x2f matrix3x2f,
      ItemRenderer.Nested0398 local2419_4,
      TextureBinding textureBinding,
      ItemGeometryQuad itemGeometryQuad,
      float local2419_7,
      float local2419_8,
      float local2419_9,
      int local2419_10,
      float local2419_11
   ) {
      ItemRenderer.Nested0397 local2419_12 = new ItemRenderer.Nested0397(
         guiRenderState, local2419_2, textureBinding.id(), local2419_4, ScissorStack.method3969()
      );
      GuiItemRenderState itemRenderState = this.field05260.get(local2419_12);
      if (itemRenderState == null) {
         itemRenderState = new GuiItemRenderState(matrix3x2f, textureBinding, ScissorStack.method3969());
         itemRenderState.add(itemGeometryQuad, local2419_7, local2419_8, local2419_9, local2419_10, local2419_11);
         this.field05260.put(local2419_12, itemRenderState);
         guiRenderState.addSimpleElement(itemRenderState);
      } else {
         itemRenderState.add(itemGeometryQuad, local2419_7, local2419_8, local2419_9, local2419_10, local2419_11);
      }
   }

   private static int method1663(BakedQuad bakedQuad, int[] intArray) {
      int local2420_2 = -1;
      if (bakedQuad.hasTint()) {
         int local2420_3 = bakedQuad.tintIndex();
         if (local2420_3 >= 0 && local2420_3 < intArray.length) {
            local2420_2 = method1684(intArray[local2420_3]);
         }
      }

      return local2420_2;
   }

   private static float method1665(
      float local2422_0, float local2422_1, float local2422_2, float local2422_3, float local2422_4, float local2422_5, float local2422_6, float local2422_7
   ) {
      return Math.abs(
            method1687(local2422_0, local2422_1, local2422_2, local2422_3)
               + method1687(local2422_2, local2422_3, local2422_4, local2422_5)
               + method1687(local2422_4, local2422_5, local2422_6, local2422_7)
               + method1687(local2422_6, local2422_7, local2422_0, local2422_1)
         )
         * 0.5F;
   }

   private void method1666(DrawContext drawContext, ItemDrawData itemDrawData) {
      float local2423_3 = itemDrawData.size() / 16.0F;
      if (!(local2423_3 <= 0.0F)) {
         drawContext.getMatrices().pushMatrix();

         try {
            GuiScale.method0801(drawContext.getMatrices());
            drawContext.getMatrices().translate(itemDrawData.x(), itemDrawData.y());
            drawContext.getMatrices().scale(local2423_3);
            this.field05262 = method1683(itemDrawData.options().color(), itemDrawData.options().alpha());
            drawContext.drawItem(itemDrawData.stack(), 0, 0, itemDrawData.seed());
         } finally {
            this.field05262 = null;
            drawContext.getMatrices().popMatrix();
         }
      }
   }

   public void method1667(DrawContext drawContext) {
      if (this.field05265 != drawContext) {
         this.field05260.clear();
      }

      this.field05265 = drawContext;
   }

   private void method1668(List<ItemRenderer.Nested0399> local2425_1, ItemRenderer.Nested0400 local2425_2, LayerRenderState layerRenderState) {
      if (layerRenderState != null) {
         List<BakedQuad> currentQuads = layerRenderState.getQuads();
         if (!currentQuads.isEmpty()) {
            ItemRenderState_LayerRenderStateMixin itemRenderState_LayerRenderStateMixin = (ItemRenderState_LayerRenderStateMixin)layerRenderState;
            Glint foilType = itemRenderState_LayerRenderStateMixin.getFoilType();
            boolean local2425_7 = foilType != Glint.NONE;
            int[] tintLayers = itemRenderState_LayerRenderStateMixin.getTintLayers();
            Matrix4f matrix4f = this.method1669(itemRenderState_LayerRenderStateMixin);
            boolean usesBlockLight = itemRenderState_LayerRenderStateMixin.getUsesBlockLight();
            if (usesBlockLight) {
               local2425_2.method1700();
            }

            for (BakedQuad bakedQuad : currentQuads) {
               ItemRenderer.Nested0399 local2425_13 = this.method1671(
                  bakedQuad, matrix4f, method1663(bakedQuad, tintLayers), local2425_7, usesBlockLight
               );
               if (!(local2425_13.area() < 2.0E-6F)) {
                  local2425_2.method1702(local2425_13.x0(), local2425_13.y0(), local2425_13.method1696());
                  local2425_2.method1702(local2425_13.x1(), local2425_13.y1(), local2425_13.method1697());
                  local2425_2.method1702(local2425_13.method0508(), local2425_13.method0510(), local2425_13.method1699());
                  local2425_2.method1702(local2425_13.method1348(), local2425_13.method1346(), local2425_13.method1695());
                  local2425_1.add(local2425_13);
               }
            }
         }
      }
   }

   @Override
   public void close() {
      this.field05260.clear();
      this.field05261.clear();
      this.field05264 = 0;
      this.field05257.clear();
      this.field05258.clear();
      this.field05256.clear();
      this.field05265 = null;
      this.method1675();
      if (this.field05259 != null) {
         MemoryUtil.memFree(this.field05259);
         this.field05259 = null;
      }
   }

   private Matrix4f method1669(ItemRenderState_LayerRenderStateMixin itemRenderState_LayerRenderStateMixin) {
      net.minecraft.client.util.math.MatrixStack.Entry entry = new net.minecraft.client.util.math.MatrixStack.Entry();
      Transformation itemTransform = itemRenderState_LayerRenderStateMixin.getItemTransform();
      if (itemTransform == null) {
         itemTransform = Transformation.IDENTITY;
      }

      itemTransform.apply(false, entry);
      return new Matrix4f(entry.getPositionMatrix());
   }

   private ItemGeometryQuad method1670(ItemRenderer.Nested0399 local2428_1, ItemRenderer.Nested0403 local2428_2) {
      return new ItemGeometryQuad(
         local2428_1.atlas(),
         local2428_2.x(local2428_1.x0()),
         local2428_2.y(local2428_1.y0()),
         local2428_1.u0(),
         local2428_1.v0(),
         local2428_2.x(local2428_1.x1()),
         local2428_2.y(local2428_1.y1()),
         local2428_1.u1(),
         local2428_1.v1(),
         local2428_2.x(local2428_1.method0508()),
         local2428_2.y(local2428_1.method0510()),
         local2428_1.method0509(),
         local2428_1.method0514(),
         local2428_2.x(local2428_1.method1348()),
         local2428_2.y(local2428_1.method1346()),
         local2428_1.method1698(),
         local2428_1.method1694(),
         local2428_1.tint(),
         local2428_1.foil()
      );
   }

   private ItemRenderer.Nested0399 method1671(BakedQuad bakedQuad, Matrix4f matrix4f, int local2429_3, boolean local2429_4, boolean local2429_5) {
      Vector3fc vector3fc = bakedQuad.position0();
      Vector3fc vector3fc2 = bakedQuad.position1();
      Vector3fc vector3fc3 = bakedQuad.position2();
      Vector3fc vector3fc4 = bakedQuad.position3();
      Vector3f vector3f = method1689(vector3fc, matrix4f);
      Vector3f vector3f2 = method1689(vector3fc2, matrix4f);
      Vector3f vector3f3 = method1689(vector3fc3, matrix4f);
      Vector3f vector3f4 = method1689(vector3fc4, matrix4f);
      long local2429_14 = bakedQuad.packedUV0();
      long local2429_16 = bakedQuad.packedUV1();
      long local2429_18 = bakedQuad.packedUV2();
      long local2429_20 = bakedQuad.packedUV3();
      float local2429_22 = (vector3f.z + vector3f2.z + vector3f3.z + vector3f4.z) * 0.25F;
      int local2429_23 = local2429_5 ? method1658(local2429_3, method1680(bakedQuad.face())) : local2429_3;
      return new ItemRenderer.Nested0399(
         bakedQuad.sprite().getAtlasId(),
         vector3f.x,
         vector3f.y,
         Vector2f.getX(local2429_14),
         Vector2f.getY(local2429_14),
         vector3f2.x,
         vector3f2.y,
         Vector2f.getX(local2429_16),
         Vector2f.getY(local2429_16),
         vector3f3.x,
         vector3f3.y,
         Vector2f.getX(local2429_18),
         Vector2f.getY(local2429_18),
         vector3f4.x,
         vector3f4.y,
         Vector2f.getX(local2429_20),
         Vector2f.getY(local2429_20),
         local2429_23,
         local2429_4,
         local2429_22,
         method1665(vector3f.x, vector3f.y, vector3f2.x, vector3f2.y, vector3f3.x, vector3f3.y, vector3f4.x, vector3f4.y),
         vector3f.z,
         vector3f2.z,
         vector3f3.z,
         vector3f4.z
      );
   }

   public void method1672() {
      this.field05261.clear();
   }

   public static ItemRenderer method1673() {
      ItemRenderer currentItemRenderer = itemRenderer;
      if (currentItemRenderer == null) {
         synchronized (ItemRenderer.class) {
            currentItemRenderer = itemRenderer;
            if (currentItemRenderer == null) {
               currentItemRenderer = new ItemRenderer();
               itemRenderer = currentItemRenderer;
            }
         }
      }

      return currentItemRenderer;
   }

   private static int method1674(int local2432_0, int local2432_1, int local2432_2) {
      return Math.max(local2432_1, Math.min(local2432_2, local2432_0));
   }

   private void method1675() {
      if (this.field05266 != null) {
         this.field05266.close();
         this.field05266 = null;
      }
   }

   public static void method1676() {
      ItemRenderer currentItemRenderer = itemRenderer;
      if (currentItemRenderer != null) {
         currentItemRenderer.close();
         itemRenderer = null;
      }
   }

   public void method1677() {
      this.field05264 = 0;
      this.field05267 = false;
   }

   private static int method1678(int local2436_0, int local2436_1, float local2436_2) {
      local2436_0 = method1684(local2436_0);
      local2436_1 = method1684(local2436_1);
      int local2436_3 = Math.round((local2436_0 >>> 24 & 0xFF) * ((local2436_1 >>> 24 & 0xFF) / 255.0F) * local2436_2);
      int local2436_4 = (local2436_0 >>> 16 & 0xFF) * (local2436_1 >>> 16 & 0xFF) / 255;
      int local2436_5 = (local2436_0 >>> 8 & 0xFF) * (local2436_1 >>> 8 & 0xFF) / 255;
      int local2436_6 = (local2436_0 & 0xFF) * (local2436_1 & 0xFF) / 255;
      return method1674(local2436_3, 0, 255) << 24 | local2436_4 << 16 | local2436_5 << 8 | local2436_6;
   }

   public void method1679() {
      this.field05265 = null;
      this.field05260.clear();
   }

   private static float method1680(Direction direction) {
      if (direction == null) {
         return 0.84F;
      }

      return switch (direction) {
         case UP -> 0.98F;
         case DOWN -> 0.72F;
         case NORTH -> 0.9F;
         case SOUTH -> 0.84F;
         case EAST, WEST -> 0.78F;
      };
   }

   private CachedItemGeometry method1681(ItemRenderState itemRenderState) {
      ItemRenderStateMixin itemRenderStateMixin = (ItemRenderStateMixin)itemRenderState;
      LayerRenderState[] layers = itemRenderStateMixin.getLayers();
      int local2439_4 = Math.min(itemRenderStateMixin.getActiveLayerCount(), layers.length);

      for (int index = 0; index < local2439_4; index++) {
         if (this.method1655(layers[index])) {
            return new CachedItemGeometry(List.of(), itemRenderState.isAnimated(), true);
         }
      }

      List<ItemRenderer.Nested0399> local2439_5 = new ArrayList<>(Math.max(8, local2439_4 * 8));
      ItemRenderer.Nested0400 local2439_6 = new ItemRenderer.Nested0400();

      for (int index2 = 0; index2 < local2439_4; index2++) {
         this.method1668(local2439_5, local2439_6, layers[index2]);
      }

      local2439_5.sort(Comparator.comparingDouble(ItemRenderer.Nested0399::depth));
      ItemRenderer.Nested0403 local2439_7 = ItemRenderer.Nested0403.of(local2439_6);
      List<ItemGeometryQuad> local2439_8 = new ArrayList<>(local2439_5.size());

      for (ItemRenderer.Nested0399 local2439_10 : local2439_5) {
         local2439_8.add(this.method1670(local2439_10, local2439_7));
      }

      return new CachedItemGeometry(List.copyOf(local2439_8), itemRenderState.isAnimated(), false);
   }

   private GpuBuffer method1682() {
      if (!this.field05267 && this.field05266 != null) {
         return this.field05266;
      }

      this.method1686();
      if (!this.field05267 && this.field05266 != null) {
         return this.field05266;
      }

      this.method1675();
      ByteBuffer byteBuffer = this.method1659(true);
      this.field05266 = RenderSystem.getDevice().createBuffer(() -> "deluxe_item_params", 128, byteBuffer);
      this.field05267 = false;
      return this.field05266;
   }

   private static int method1683(int local2441_0, float local2441_1) {
      local2441_0 = method1684(local2441_0);
      float local2441_2 = (local2441_0 >>> 24 & 0xFF) / 255.0F * local2441_1;
      return Math.round(255.0F * local2441_2) << 24
         | Math.round((local2441_0 >>> 16 & 0xFF) * local2441_2) << 16
         | Math.round((local2441_0 >>> 8 & 0xFF) * local2441_2) << 8
         | Math.round((local2441_0 & 0xFF) * local2441_2);
   }

   private static int method1684(int local2442_0) {
      return (local2442_0 & 0xFF000000) == 0 && (local2442_0 & 16777215) != 0 ? local2442_0 | 0xFF000000 : local2442_0;
   }

   int method1685(float local2443_1) {
      int local2443_2 = this.field05264;
      if (local2443_2 == 4096) {
         return -1;
      }

      this.field05263[local2443_2] = local2443_1;
      this.field05264++;
      this.field05267 = true;
      return local2443_2;
   }

   public void method1686() {
      if (this.field05264 != 0 && this.field05267) {
         GpuBuffer gpuBuffer = this.method1660();
         if (gpuBuffer != null) {
            try {
               ByteBuffer byteBuffer = this.method1659(false);
               RenderSystem.getDevice().createCommandEncoder().writeToBuffer(gpuBuffer.slice(0L, byteBuffer.remaining()), byteBuffer);
               this.field05267 = false;
            } catch (RuntimeException exception) {
               this.field05267 = true;
            }
         }
      }
   }

   private static float method1687(float local2445_0, float local2445_1, float local2445_2, float local2445_3) {
      return local2445_0 * local2445_3 - local2445_2 * local2445_1;
   }

   private void method1688(DrawContext drawContext, ItemDrawData itemDrawData) {
      if (drawContext != null && itemDrawData != null && itemDrawData.visible()) {
         MinecraftClient minecraftClient = MinecraftClient.getInstance();
         if (minecraftClient != null && minecraftClient.getItemModelManager() != null) {
            try {
               GuiRenderState guiRenderState = ((DrawContextMixin)drawContext).getGuiRenderState();
               int currentLayerSerial = ((GuiLayerSerialAccess)guiRenderState).getLayerSerial();
               Matrix3x2f matrix3x2f = GuiScale.method0795(drawContext);
               ItemRenderer.Nested0398 local2446_7 = ItemRenderer.Nested0398.of(matrix3x2f);
               CachedItemGeometry cachedItemGeometry = this.method1654(minecraftClient, itemDrawData);
               if (cachedItemGeometry.specialRenderer()) {
                  this.method1666(drawContext, itemDrawData);
                  return;
               }

               for (ItemGeometryQuad itemGeometryQuad : cachedItemGeometry.quads()) {
                  TextureBinding textureBinding = this.method1692(itemGeometryQuad.atlas());
                  if (textureBinding != null) {
                     float local2446_12 = itemDrawData.options().glintMode().shouldRender(itemDrawData.stack(), itemGeometryQuad.foil())
                        ? itemDrawData.options().glintStrength()
                        : 0.0F;
                     int local2446_13 = method1678(itemDrawData.options().color(), itemGeometryQuad.tint(), itemDrawData.options().alpha());
                     this.method1662(
                        guiRenderState,
                        currentLayerSerial,
                        matrix3x2f,
                        local2446_7,
                        textureBinding,
                        itemGeometryQuad,
                        itemDrawData.x(),
                        itemDrawData.y(),
                        itemDrawData.size(),
                        local2446_13,
                        local2446_12
                     );
                  }
               }
            } catch (RuntimeException exception) {
               DeluxeClient.logger().warn("[RenderItem] Failed to submit item: {}", itemDrawData.stack().getName().getString(), exception);
            }
         }
      }
   }

   private static Vector3f method1689(Vector3fc vector3fc, Matrix4f matrix4f) {
      return new Vector3f(vector3fc).mulPosition(matrix4f);
   }

   private ItemRenderer() {
   }

   public int method1690(ItemGuiElementRenderState itemGuiElementRenderState) {
      return this.field05261.getOrDefault(itemGuiElementRenderState, -1);
   }

   public void method1691(RenderPass renderPass) {
      if (renderPass != null && this.field05264 != 0) {
         GpuBuffer gpuBuffer = this.method1682();
         if (gpuBuffer != null) {
            renderPass.setUniform("ItemParamsArray", gpuBuffer);
         }
      }
   }

   private TextureBinding method1692(Identifier identifier) {
      if (identifier == null) {
         return null;
      }

      MinecraftClient minecraftClient = MinecraftClient.getInstance();
      if (minecraftClient == null) {
         return null;
      }

      TextureManager textureManager = minecraftClient.getTextureManager();
      if (textureManager == null) {
         return null;
      }

      AbstractTexture currentTexture = textureManager.getTexture(identifier);
      if (currentTexture != null && currentTexture.getGlTextureView() != null) {
         ItemRenderer.Nested0402 local2451_5 = this.field05256.get(identifier);
         if (local2451_5 != null && local2451_5.texture() == currentTexture) {
            return local2451_5.value();
         }

         TextureSetup textureSetup = TextureSetup.of(currentTexture.getGlTextureView(), RenderSystem.getSamplerCache().get(FilterMode.NEAREST));
         TextureBinding textureBinding = new TextureBinding(
            identifier, textureSetup, TextureSetup.of(currentTexture.getGlTextureView(), RenderSystem.getSamplerCache().get(FilterMode.LINEAR))
         );
         this.field05256.put(identifier, new ItemRenderer.Nested0402(currentTexture, textureBinding));
         return textureBinding;
      } else {
         return null;
      }
   }

   public void method1693() {
      this.field05257.clear();
      this.field05258.clear();
      this.field05256.clear();
   }

   private record Nested0396(Item item, ComponentMap components, int count, int seed) {
      static ItemRenderer.Nested0396 of(ItemStack itemStack, int local2460_1) {
         return new ItemRenderer.Nested0396(itemStack.getItem(), itemStack.getImmutableComponents(), itemStack.getCount(), local2460_1);
      }
   }

   private record Nested0397(GuiRenderState state, int layerSerial, Identifier texture, ItemRenderer.Nested0398 pose, ScreenRect scissor) {
   }

   private record Nested0398(float m00, float m01, float m10, float m11, float m20, float m21) {
      static ItemRenderer.Nested0398 of(Matrix3x2f matrix3x2f) {
         return new ItemRenderer.Nested0398(
            matrix3x2f.m00(), matrix3x2f.m01(), matrix3x2f.m10(), matrix3x2f.m11(), matrix3x2f.m20(), matrix3x2f.m21()
         );
      }
   }

   private record Nested0399(
      Identifier atlas,
      float x0,
      float y0,
      float u0,
      float v0,
      float x1,
      float y1,
      float u1,
      float v1,
      float method0508,
      float method0510,
      float method0509,
      float method0514,
      float method1348,
      float method1346,
      float method1698,
      float method1694,
      int tint,
      boolean foil,
      float depth,
      float area,
      float method1696,
      float method1697,
      float method1699,
      float method1695
   ) {
   }

   private static final class Nested0400 {
      private float field05321 = Float.MAX_VALUE;
      private float field05322 = Float.MAX_VALUE;
      private float field05323 = Float.MAX_VALUE;
      private float field05324 = -Float.MAX_VALUE;
      private float field05325 = -Float.MAX_VALUE;
      private float field05326 = -Float.MAX_VALUE;
      private boolean field05327;

      private void method1700() {
         this.field05327 = true;
      }

      private boolean method1701() {
         return this.field05327 || this.field05326 - this.field05323 > 0.02F;
      }

      private void method1702(float local2515_1, float local2515_2, float local2515_3) {
         this.field05321 = Math.min(this.field05321, local2515_1);
         this.field05322 = Math.min(this.field05322, local2515_2);
         this.field05323 = Math.min(this.field05323, local2515_3);
         this.field05324 = Math.max(this.field05324, local2515_1);
         this.field05325 = Math.max(this.field05325, local2515_2);
         this.field05326 = Math.max(this.field05326, local2515_3);
      }

      private boolean method1703() {
         return this.field05321 <= this.field05324 && this.field05322 <= this.field05325;
      }
   }

   private record Nested0402(AbstractTexture texture, TextureBinding value) {
   }

   private record Nested0403(float minX, float maxY, float scale, float offsetX, float offsetY) {
      private float x(float local2526_1) {
         return this.offsetX + (local2526_1 - this.minX) * this.scale;
      }

      private float y(float local2528_1) {
         return this.offsetY + (this.maxY - local2528_1) * this.scale;
      }

      private static ItemRenderer.Nested0403 of(ItemRenderer.Nested0400 local2536_0) {
         if (!local2536_0.method1703()) {
            return new ItemRenderer.Nested0403(0.0F, 1.0F, 1.0F, 0.0F, 0.0F);
         } else {
            float local2536_1 = Math.max(1.0E-4F, local2536_0.field05324 - local2536_0.field05321);
            float local2536_2 = Math.max(1.0E-4F, local2536_0.field05325 - local2536_0.field05322);
            float local2536_3 = Math.max(local2536_1, local2536_2);
            if (local2536_0.method1701()) {
               float local2536_4 = Math.min(1.0F / local2536_3, 1.08F);
               return new ItemRenderer.Nested0403(
                  (local2536_0.field05321 + local2536_0.field05324) * 0.5F, (local2536_0.field05322 + local2536_0.field05325) * 0.5F, local2536_4, 0.5F, 0.5F
               );
            } else {
               float local2536_4 = 1.0F / local2536_3;
               return new ItemRenderer.Nested0403(
                  local2536_0.field05321,
                  local2536_0.field05325,
                  local2536_4,
                  (1.0F - local2536_1 * local2536_4) * 0.5F,
                  (1.0F - local2536_2 * local2536_4) * 0.5F
               );
            }
         }
      }
   }

   private record Nested0404(Object identity, int seed) {
   }
}
