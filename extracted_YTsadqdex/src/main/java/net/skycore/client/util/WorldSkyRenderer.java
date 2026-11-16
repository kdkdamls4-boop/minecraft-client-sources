package net.skycore.client.util;

import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.pipeline.BindGroupLayout;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.RenderPipeline.Builder;
import com.mojang.blaze3d.platform.BlendFactor;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.SimpleTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.level.Level;
import net.skycore.client.SkycoreClient;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryStack;

public final class WorldSkyRenderer implements AutoCloseable {
   private long nextRefresh;
   private static final BindGroupLayout LAYOUT = BindGroupLayout.builder()
      .withSampler("DepthSampler")
      .withSampler("SkySampler")
      .withUniform("CustomSkyUniforms", UniformType.UNIFORM_BUFFER)
      .build();
   private final Set<Identifier> registeredTextures;
   private static final RenderPipeline REPLACE_PIPELINE = pipeline("replace", null);
   private static final BlendFunction ADD = new BlendFunction(BlendFactor.SRC_ALPHA, BlendFactor.ONE);
   private static final RenderPipeline ADD_PIPELINE = pipeline("add", WorldSkyRenderer.ADD);
   private static final RenderPipeline ALPHA_PIPELINE = pipeline("alpha", BlendFunction.TRANSLUCENT);
   private List<WorldSkyRenderer.Layer> layers;
   private final GpuBuffer uniforms = RenderSystem.getDevice().createBuffer(() -> {
      byte flag = 0;
      flag = 0;
      return "Skycore OptiFine custom sky uniforms";
   }, 136, 96L);
   private static final int UNIFORM_SIZE = 96;
   private String loadedWorld;
   public static final WorldSkyRenderer INSTANCE = new WorldSkyRenderer();
   private static final long REFRESH_NANOS = 2000000000L;

   private static String fileName(Identifier texture) {
      String path = texture.getPath();
      int value = path.lastIndexOf(47);
      return texture.getPath().substring(value + 1);
   }

   public static RenderPipeline[] pipelines() {
      RenderPipeline[] renderPipelineValues = new RenderPipeline[3];
      renderPipelineValues[0] = ALPHA_PIPELINE;
      renderPipelineValues[1] = ADD_PIPELINE;
      renderPipelineValues[2] = REPLACE_PIPELINE;
      return renderPipelineValues;
   }

   public WorldSkyRenderer() {
      this.layers = List.of();
      this.registeredTextures = new HashSet<>();
      this.loadedWorld = "";
   }

   private void writeUniforms(Matrix4f matrix, float value, float value2, float value3, float value4, float value5) {
      float squareRoot = (float)Math.sqrt(value3 * value3 + value4 * value4 + value5 * value5);
      if (squareRoot < 1.0E-5F) {
         value3 = 0.0F;
         value4 = 0.0F;
         value5 = 1.0F;
         squareRoot = 1.0F;
      }

      try (MemoryStack memoryStack = MemoryStack.stackPush()) {
         ByteBuffer device = Std140Builder.onStack(memoryStack, 96)
            .putMat4f(matrix)
            .putVec4(value, value2, RenderSystem.getDevice().getDeviceInfo().isZZeroToOne() ? 1.0F : 0.0F, 0.0F)
            .putVec4(value3 / squareRoot, value4 / squareRoot, value5 / squareRoot, 0.0F)
            .get();
         RenderSystem.getDevice().createCommandEncoder().writeToBuffer(this.uniforms.slice(), device);
      }
   }

   @Override
   public void close() {
      this.uniforms.close();
   }

   private void reloadLayers(Minecraft minecraft, String loadedWorld2) {
      this.loadedWorld = loadedWorld2;
      ResourceManager resourceManager = minecraft.getResourceManager();
      String text = "optifine/sky/" + loadedWorld2;
      String text2 = text + "/";
      var path = resourceManager.listResources(text, texture -> {
         String path2 = texture.getPath();
         if (path2.startsWith(text2)) {
            String path3 = texture.getPath();
            if (path3.endsWith(".properties")) {
               String text3 = fileName(texture);
               if (text3.startsWith("sky")) {
                  return true;
               }
            }
         }

         return false;
      });
      var identifiers = path.keySet().stream().sorted(Comparator.comparingInt(WorldSkyRenderer::layerNumber)).toList();
      ArrayList elements = new ArrayList();

      for (Identifier texture2 : identifiers) {
         try {
            Properties properties = new Properties();

            try (BufferedReader bufferedReader = ((Resource)path.get(texture2)).openAsReader()) {
               properties.load(bufferedReader);
            }

            Identifier property = resolveTexture(texture2, properties.getProperty("source"));
            if (resourceManager.getResource(property).isEmpty()) {
               SkycoreClient.LOGGER.warn("Custom sky texture is missing: {}", property);
            } else {
               if (this.registeredTextures.add(property)) {
                  minecraft.getTextureManager().registerAndLoad(property, new SimpleTexture(property));
               }

               elements.add(WorldSkyRenderer.Layer.parse(properties, property));
            }
         } catch (IOException | RuntimeException failure) {
            SkycoreClient.LOGGER.warn("Could not load custom sky layer {}", texture2, failure);
         }
      }

      this.layers = List.copyOf(elements);
   }

   public void render(CameraRenderState cameraRenderState) {
      Minecraft instance = Minecraft.getInstance();
      if (instance.level != null && cameraRenderState != null && cameraRenderState.initialized) {
         String text = worldFolder(instance.level);
         long nowNanos = System.nanoTime();
         if (!text.equals(this.loadedWorld) || nowNanos >= this.nextRefresh) {
            this.reloadLayers(instance, text);
            this.nextRefresh = nowNanos + 2000000000L;
         }

         if (!this.layers.isEmpty()) {
            RenderTarget renderTarget = instance.gameRenderer.mainRenderTarget();
            Matrix4f matrix = CaptureLevelProjectionHelper.levelProjectionCopy();
            if (renderTarget != null && renderTarget.getColorTextureView() != null && renderTarget.getDepthTextureView() != null && matrix != null) {
               Matrix4f matrix2 = matrix.mul(cameraRenderState.viewRotationMatrix).invert();
               long overworldClockTime = Math.floorMod(instance.level.getOverworldClockTime(), 24000L);
               float index = (float)overworldClockTime / 24000.0F;

               for (WorldSkyRenderer.Layer layer : this.layers) {
                  float value = layer.brightness((int)overworldClockTime);
                  value *= weatherBrightness(instance, layer.weather);
                  if (!(value <= 1.0E-4F)) {
                     float computedValue = layer.rotate ? index * (float) (Math.PI * 2) * layer.speed : 0.0F;
                     this.writeUniforms(matrix2, value, computedValue, layer.axisX, layer.axisY, layer.axisZ);
                     AbstractTexture texture = instance.getTextureManager().getTexture(layer.texture);

                     try (RenderPass device = RenderSystem.getDevice().createCommandEncoder().createRenderPass(() -> {
                           return "Skycore OptiFine custom sky";
                        }, renderTarget.getColorTextureView(), Optional.empty())) {
                        device.setPipeline(layer.pipeline());
                        RenderSystem.bindDefaultUniforms(device);
                        device.setUniform("CustomSkyUniforms", this.uniforms);
                        device.bindTexture("DepthSampler", renderTarget.getDepthTextureView(), RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST));
                        device.bindTexture("SkySampler", texture.getTextureView(), RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
                        device.setVertexBuffer(0, EnsureHelper.buffer().slice());
                        device.draw(EnsureHelper.vertexCount(), 1, 0, 0);
                     }
                  }
               }
            }
         }
      }
   }

   private static String worldFolder(Level level) {
      if (level.dimension() == Level.END) {
         return "world1";
      }

      ResourceKey resourceKey = level.dimension();
      return resourceKey == Level.NETHER ? "world-1" : "world0";
   }

   private static float weatherBrightness(Minecraft minecraft, String text) {
      boolean thundering = minecraft.level.isThundering();
      boolean raining = minecraft.level.isRaining();
      if (thundering) {
         return text.contains("thunder") ? 1.0F : 0.0F;
      } else if (raining) {
         return text.contains("rain") ? 1.0F : 0.0F;
      } else {
         return text.contains("clear") ? 1.0F : 0.0F;
      }
   }

   private static Identifier resolveTexture(Identifier texture, String text) {
      String path = texture.getPath().substring(0, texture.getPath().length() - ".properties".length()) + ".png";
      if (text != null && !text.isBlank()) {
         String text2 = text.trim().replace('\\', '/');
         if (text2.contains(":")) {
            return Identifier.parse(text2);
         } else if (text2.startsWith("~/")) {
            return Identifier.withDefaultNamespace("optifine/" + text2.substring(2));
         } else if (text2.startsWith("./")) {
            int path2 = texture.getPath().lastIndexOf(47);
            return Identifier.fromNamespaceAndPath(texture.getNamespace(), texture.getPath().substring(0, path2 + 1) + text2.substring(2));
         } else {
            return Identifier.withDefaultNamespace(text2.startsWith("/") ? text2.substring(1) : text2);
         }
      } else {
         return Identifier.fromNamespaceAndPath(texture.getNamespace(), path);
      }
   }

   private static int layerNumber(Identifier texture) {
      String text = fileName(texture);

      try {
         int length = text.length() - ".properties".length();
         String text2 = text.substring(3, length);
         return Integer.parseInt(text2);
      } catch (RuntimeException failure) {
         return Integer.MAX_VALUE;
      }
   }

   private static RenderPipeline pipeline(String text, BlendFunction blendFunction) {
      ColorTargetState colorTargetState;
      if (blendFunction == null) {
         colorTargetState = ColorTargetState.DEFAULT;
      } else {
         colorTargetState = new ColorTargetState(blendFunction);
      }

      ColorTargetState colorTargetState2 = colorTargetState;
      Builder builder = RenderPipeline.builder();
      String text2 = "skycore:pipeline/world/optifine_custom_sky_" + text;
      Identifier texture = Identifier.parse(text2);
      Builder builder2 = builder.withLocation(texture);
      Identifier texture2 = Identifier.parse("skycore:post/blurs/kawase_common");
      Builder builder3 = builder2.withVertexShader(texture2);
      Identifier texture3 = Identifier.parse("skycore:world/sky/optifine_custom_sky");
      Builder builder4 = builder3.withFragmentShader(texture3);
      Builder builder5 = builder4.withBindGroupLayout(LAYOUT).withColorTargetState(colorTargetState2).withDepthStencilState(Optional.empty());
      Builder builder6 = builder5.withVertexBinding(0, DefaultVertexFormat.POSITION_TEX);
      Builder builder7 = builder6.withPrimitiveTopology(PrimitiveTopology.TRIANGLES);
      Builder builder8 = builder7.withCull(false);
      return builder8.build();
   }

   public record Layer(
      Identifier texture,
      String blend,
      boolean rotate,
      float speed,
      float axisX,
      float axisY,
      float axisZ,
      int startFadeIn,
      int endFadeIn,
      int startFadeOut,
      int endFadeOut,
      String weather
   ) {
      private static boolean between(int value, int value2, int value3) {
         return value2 <= value3 ? value >= value2 && value <= value3 : value >= value2 || value <= value3;
      }

      private static float parseFloat(String text, float result) {

         try {
            return Float.parseFloat(text);
         } catch (RuntimeException failure) {
            return result;
         }
      }

      public float brightness(int value) {
         if (this.startFadeIn >= 0) {
            if (this.endFadeIn >= 0 && this.startFadeOut >= 0) {
               if (this.endFadeOut >= 0) {
                  if (between(value, this.startFadeIn, this.endFadeIn)) {
                     return ratio(value, this.startFadeIn, this.endFadeIn);
                  }

                  if (between(value, this.endFadeIn, this.startFadeOut)) {
                     return 1.0F;
                  }

                  if (between(value, this.startFadeOut, this.endFadeOut)) {
                     return 1.0F - ratio(value, this.startFadeOut, this.endFadeOut);
                  }

                  return 0.0F;
               }
            }
         }

         return 1.0F;
      }

      public Identifier texture() {
         return this.texture;
      }

      public int endFadeIn() {
         return this.endFadeIn;
      }

      public boolean rotate() {
         return this.rotate;
      }

      public float axisZ() {
         return this.axisZ;
      }

      public float axisX() {
         return this.axisX;
      }

      public float axisY() {
         return this.axisY;
      }

      private static float ratio(int value, int value2, int value3) {
         int value4 = 0;
         value4 = (byte)0;
         int computedValue = normalize(value3 - value2);
         float result;
         if (computedValue == 0) {
            result = 1.0F;
         } else {
            value4 = value - value2;
            result = Math.clamp((float)normalize(value4) / computedValue, 0.0F, 1.0F);
         }

         return result;
      }

      private static int parseTime(String text) {
         int value = 0;
         if (text == null) {
            return -1;
         }

         String text2 = text.trim();
         String[] stringValues = text2.split(":", 2);
         if (stringValues.length != 2) {
            return -1;
         }

         try {
            int value2 = Integer.parseInt(stringValues[0]);
            String text3 = stringValues[1];
            int value3 = Integer.parseInt(text3);
            if (value2 >= 0 && value2 <= 23 && value3 >= 0 && value3 <= 59) {
               value = (value2 - 6) * 1000;
               float computedValue = value3 / 60.0F * 1000.0F;
               value += Math.round(computedValue);
               return normalize(value);
            } else {
               return -1;
            }
         } catch (NumberFormatException failure) {
            return -1;
         }
      }

      public String weather() {
         return this.weather;
      }

      public int endFadeOut() {
         return this.endFadeOut;
      }

      public int startFadeIn() {
         return this.startFadeIn;
      }

      private static float[] parseAxis(String text) {
         String[] stringValues = text.trim().split("\\s+");
         if (stringValues.length != 3) {
            return new float[]{0.0F, 0.0F, 1.0F};
         }

         try {
            float[] floatValues = new float[3];
            String text2 = stringValues[0];
            floatValues[0] = Float.parseFloat(text2);
            text2 = stringValues[1];
            floatValues[1] = Float.parseFloat(text2);
            text2 = stringValues[2];
            floatValues[2] = Float.parseFloat(text2);
            return floatValues;
         } catch (NumberFormatException failure) {
            return new float[]{0.0F, 0.0F, 1.0F};
         }
      }

      public String blend() {
         return this.blend;
      }

      private static int normalize(int value) {
         return Math.floorMod(value, 24000);
      }

      public int startFadeOut() {
         return this.startFadeOut;
      }

      public RenderPipeline pipeline() {
         return switch (this.blend) {
            case "replace" -> REPLACE_PIPELINE;
            case "alpha" -> ALPHA_PIPELINE;
            default -> ADD_PIPELINE;
         };
      }

      public static WorldSkyRenderer.Layer parse(Properties properties, Identifier texture2) {
         String property = properties.getProperty("startFadeIn");
         int value = parseTime(property);
         String property2 = properties.getProperty("endFadeIn");
         int value2 = parseTime(property2);
         String property3 = properties.getProperty("endFadeOut");
         int value3 = parseTime(property3);
         String property4 = properties.getProperty("startFadeOut");
         int value4 = parseTime(property4);
         if (value >= 0 && value2 >= 0 && value3 >= 0 && value4 < 0) {
            value4 = normalize(value3 - normalize(value2 - value));
         }

         float[] property5 = parseAxis(properties.getProperty("axis", "0 0 1"));
         String property6 = properties.getProperty("blend", "add");
         String text = property6.toLowerCase(Locale.ROOT);
         String property7 = properties.getProperty("rotate", "true");
         boolean parseBoolean = Boolean.parseBoolean(property7);
         String property8 = properties.getProperty("speed");
         float value5 = parseFloat(property8, 1.0F);
         float value6 = property5[0];
         float value7 = property5[1];
         float value8 = property5[2];
         return new WorldSkyRenderer.Layer(
            texture2, text, parseBoolean, value5, value6, value7, value8, value, value2, value4, value3, properties.getProperty("weather", "clear").toLowerCase(Locale.ROOT)
         );
      }

      public float speed() {
         return this.speed;
      }
   }
}
