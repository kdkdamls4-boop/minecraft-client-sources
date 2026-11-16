package deluxe.client.module.render;

import deluxe.client.setting.BooleanSetting;
import deluxe.client.util.Colors;
import deluxe.client.DeluxeClient;
import deluxe.client.util.DepthSnapshot;
import deluxe.client.event.EventListener;
import deluxe.client.setting.LegacyNumberSetting;
import deluxe.client.MinecraftAccess;
import deluxe.client.setting.ModeSetting;
import deluxe.client.module.Module;
import deluxe.client.module.ModuleCategory;
import deluxe.client.module.ModuleInfo;
import deluxe.client.setting.MultiSelectSetting;
import deluxe.client.event.PacketEvent;
import deluxe.client.event.PlayerTickEvent;
import deluxe.client.render.WorldProjection;
import deluxe.client.render.ThemePaletteBuffer;
import deluxe.client.setting.ColorSetting;
import deluxe.client.setting.SectionSetting;
import deluxe.client.event.WorldRenderEvent;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.client.util.BufferAllocator.CloseableBuffer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;

@ModuleInfo(name = "Particles", description = "Частицы вокруг игрока: метеоры, фигуры и хит эффекты.", category = ModuleCategory.RENDER)
public final class ParticlesModule extends Module {
   private static ParticlesModule particlesModule;
   private static final Vector4f field00334 = new Vector4f(1.0F, 1.0F, 1.0F, 1.0F);
   private static final Vector3f field00335 = new Vector3f();
   private static final Matrix4f field00336 = new Matrix4f();
   private static final RenderPipeline field00337 = RenderPipeline.builder(RenderPipelines.TRANSFORMS_AND_PROJECTION_SNIPPET)
      .withLocation(DeluxeClient.id("pipeline/particles_glow"))
      .withVertexShader(Identifier.ofVanilla("core/position_tex_color"))
      .withFragmentShader(Identifier.ofVanilla("core/position_tex_color"))
      .withVertexFormat(VertexFormats.POSITION_TEXTURE_COLOR, DrawMode.QUADS)
      .withBlend(BlendFunction.LIGHTNING)
      .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
      .withDepthWrite(false)
      .withCull(false)
      .withSampler("Sampler0")
      .build();
   private static final RenderPipeline field00338 = RenderPipeline.builder(RenderPipelines.TRANSFORMS_AND_PROJECTION_SNIPPET)
      .withLocation(DeluxeClient.id("pipeline/particles_wires"))
      .withVertexShader(Identifier.ofVanilla("core/position_color"))
      .withFragmentShader(Identifier.ofVanilla("core/position_color"))
      .withVertexFormat(VertexFormats.POSITION_COLOR, DrawMode.QUADS)
      .withBlend(BlendFunction.LIGHTNING)
      .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
      .withDepthWrite(false)
      .withCull(false)
      .build();
   private static final int[] field00339 = new int[]{0, 1, 2, 3, 4, 5, 6, 7, 0, 2, 1, 3, 4, 6, 5, 7, 0, 4, 1, 5, 2, 6, 3, 7};
   private static final int[] field00340 = new int[]{0, 1, 1, 2, 2, 3, 3, 0, 0, 4, 1, 4, 2, 4, 3, 4};
   private static final int[] field00341 = new int[]{0, 2, 0, 3, 0, 4, 0, 5, 1, 2, 1, 3, 1, 4, 1, 5, 2, 5, 5, 3, 3, 4, 4, 2};
   private static final int[] field00342 = new int[]{0, 1, 0, 2, 0, 3, 1, 2, 2, 3, 3, 1};
   public MultiSelectSetting field00343;
   public LegacyNumberSetting field00344;
   public LegacyNumberSetting field00345;
   public LegacyNumberSetting field00346;
   public LegacyNumberSetting field00347;
   public LegacyNumberSetting field00348;
   public LegacyNumberSetting field00349;
   public LegacyNumberSetting field00350;
   public ModeSetting modeSetting;
   public MultiSelectSetting field00352;
   public LegacyNumberSetting field00353;
   public LegacyNumberSetting field00354;
   public LegacyNumberSetting field00355;
   public LegacyNumberSetting field00356;
   public LegacyNumberSetting field00357;
   public BooleanSetting field00358;
   public MultiSelectSetting field00359;
   public LegacyNumberSetting field00360;
   public LegacyNumberSetting field00361;
   public BooleanSetting field00362;
   public BooleanSetting field00363;
   public ColorSetting colorSetting;
   private final ArrayList<ParticlesModule.Nested0353> field00365 = new ArrayList<>();
   private final ArrayList<ParticlesModule.Nested0353> field00366 = new ArrayList<>();
   private final ArrayList<ParticlesModule.Nested0353> field00367 = new ArrayList<>();
   private final ArrayList<ParticlesModule.Nested0353> field00368 = new ArrayList<>();
   private final ArrayList<ParticlesModule.Nested0353> field00369 = new ArrayList<>();
   private final ArrayList<ParticlesModule.Nested0353> field00370 = new ArrayList<>();
   private final ArrayList<ParticlesModule.Nested0353> field00371 = new ArrayList<>();
   private final ArrayList<ParticlesModule.Nested0356> field00372 = new ArrayList<>();
   private final ArrayList<ParticlesModule.Nested0356> field00373 = new ArrayList<>();
   private final ConcurrentLinkedQueue<ParticlesModule.Nested0357> field00374 = new ConcurrentLinkedQueue<>();
   private long field00375;
   private final HashMap<String, GpuBuffer> field00376 = new HashMap<>();
   private GpuBuffer field00377;
   private final Matrix3f field00378 = new Matrix3f();
   private final Vector3f[] field00379 = new Vector3f[8];
   private final Vector3f field00380 = new Vector3f();
   private final Vector3f field00381 = new Vector3f();
   private float[] field00382;
   private float[] field00383;
   private float[] field00384;
   private float[] field00385;
   private float[] field00386;
   private boolean field00387;
   private boolean field00388;
   private boolean field00389;
   private boolean field00390;
   private volatile boolean field00391;
   private BufferAllocator field00392;
   private final int[] field00393;
   private final ArrayList<ParticlesModule.Nested0356> field00394;
   private final EventListener<PacketEvent> field00395;
   private final EventListener<PlayerTickEvent> field00396;
   private final EventListener<WorldRenderEvent> field00397;
   private static final String[] field00398 = new String[]{"Светлячок", "Звезда", "Сердце", "Снежинка", "Доллар"};

   private boolean method0559() {
      return mc.player != null
         && mc.player.fallDistance > 0.0
         && !mc.player.isOnGround()
         && !mc.player.isClimbing()
         && !mc.player.isTouchingWater()
         && !mc.player.hasVehicle()
         && !mc.player.isSprinting();
   }

   private void method0560(
      BufferBuilder bufferBuilder,
      Quaternionf quaternionf,
      Vector3f vector3f,
      double local717_4,
      double local717_6,
      double local717_8,
      float local717_10,
      int local717_11
   ) {
      double local717_12 = local717_4 - mc.gameRenderer.getCamera().getCameraPos().x;
      double local717_14 = local717_6 - mc.gameRenderer.getCamera().getCameraPos().y;
      double local717_16 = local717_8 - mc.gameRenderer.getCamera().getCameraPos().z;
      quaternionf.transform(vector3f.set(-local717_10, -local717_10, 0.0F));
      bufferBuilder.vertex((float)(local717_12 + vector3f.x), (float)(local717_14 + vector3f.y), (float)(local717_16 + vector3f.z))
         .texture(0.0F, 0.0F)
         .color(local717_11);
      quaternionf.transform(vector3f.set(-local717_10, local717_10, 0.0F));
      bufferBuilder.vertex((float)(local717_12 + vector3f.x), (float)(local717_14 + vector3f.y), (float)(local717_16 + vector3f.z))
         .texture(0.0F, 1.0F)
         .color(local717_11);
      quaternionf.transform(vector3f.set(local717_10, local717_10, 0.0F));
      bufferBuilder.vertex((float)(local717_12 + vector3f.x), (float)(local717_14 + vector3f.y), (float)(local717_16 + vector3f.z))
         .texture(1.0F, 1.0F)
         .color(local717_11);
      quaternionf.transform(vector3f.set(local717_10, -local717_10, 0.0F));
      bufferBuilder.vertex((float)(local717_12 + vector3f.x), (float)(local717_14 + vector3f.y), (float)(local717_16 + vector3f.z))
         .texture(1.0F, 0.0F)
         .color(local717_11);
   }

   private static int method0562(String local719_0) {
      for (int index = 0; index < field00398.length; index++) {
         if (field00398[index].equals(local719_0)) {
            return index;
         }
      }

      return -1;
   }

   private static void method0564(
      JsonArray jsonArray, int local721_1, float local721_2, float local721_3, float local721_4, float[][] floatArrayArray, boolean[] booleanArray
   ) {
      if (local721_1 >= 0 && local721_1 < jsonArray.size() && !booleanArray[local721_1]) {
         booleanArray[local721_1] = true;
         JsonObject asJsonObject = jsonArray.get(local721_1).getAsJsonObject();
         float local721_8 = local721_2;
         float local721_9 = local721_3;
         float local721_10 = local721_4;
         if (asJsonObject.has("translation")) {
            JsonArray asJsonArray = asJsonObject.getAsJsonArray("translation");
            local721_8 = local721_2 + asJsonArray.get(0).getAsFloat();
            local721_9 = local721_3 + asJsonArray.get(1).getAsFloat();
            local721_10 = local721_4 + asJsonArray.get(2).getAsFloat();
         }

         floatArrayArray[local721_1][0] = local721_8;
         floatArrayArray[local721_1][1] = local721_9;
         floatArrayArray[local721_1][2] = local721_10;
         if (asJsonObject.has("children")) {
            JsonArray asJsonArray2 = asJsonObject.getAsJsonArray("children");

            for (int index = 0; index < asJsonArray2.size(); index++) {
               method0564(jsonArray, asJsonArray2.get(index).getAsInt(), local721_8, local721_9, local721_10, floatArrayArray, booleanArray);
            }
         }
      }
   }

   private void method0566(long local723_1) {
      if (this.field00343.isSelected("Кубы")
         || this.field00343.isSelected("Пирамиды")
         || this.field00343.isSelected("Тотем")
         || this.field00343.isSelected("Полумесяц")
         || this.field00343.isSelected("Сердце")
         || this.field00343.isSelected("Звёзды")
         || this.field00343.isSelected("Делюкс")) {
         Vec3d vec3d = mc.player.getEntityPos();
         double local723_4 = Math.min(this.field00347.getValue(), this.field00348.getValue());
         double local723_6 = Math.max(this.field00348.getValue(), local723_4 + 0.1);
         int local723_8 = Math.max(1, (int)this.field00344.getValue());
         if (this.field00343.isSelected("Кубы")) {
            this.method0615(this.field00365, vec3d, local723_4, local723_6, local723_1, local723_8, 6.0F, 6.0F, 6.0F);
         } else {
            this.field00365.clear();
         }

         if (this.field00343.isSelected("Пирамиды")) {
            this.method0615(this.field00366, vec3d, local723_4, local723_6, local723_1, local723_8, 7.0F, 7.0F, 7.0F);
         } else {
            this.field00366.clear();
         }

         if (this.field00343.isSelected("Тотем")) {
            this.method0615(this.field00367, vec3d, local723_4, local723_6, local723_1, local723_8, 5.0F, 8.0F, 5.0F);
         } else {
            this.field00367.clear();
         }

         if (this.field00343.isSelected("Полумесяц")) {
            this.method0615(this.field00368, vec3d, local723_4, local723_6, local723_1, local723_8, 6.0F, 6.0F, 6.0F);
         } else {
            this.field00368.clear();
         }

         if (this.field00343.isSelected("Сердце")) {
            this.method0615(this.field00369, vec3d, local723_4, local723_6, local723_1, local723_8, 6.0F, 6.0F, 6.0F);
         } else {
            this.field00369.clear();
         }

         if (this.field00343.isSelected("Звёзды")) {
            this.method0615(this.field00370, vec3d, local723_4, local723_6, local723_1, local723_8, 6.0F, 6.0F, 6.0F);
         } else {
            this.field00370.clear();
         }

         if (this.field00343.isSelected("Делюкс")) {
            this.method0615(this.field00371, vec3d, local723_4, local723_6, local723_1, local723_8, 6.0F, 6.0F, 6.0F);
         } else {
            this.field00371.clear();
         }
      }
   }

   private ParticlesModule() {
      this.field00393 = new int[field00398.length];
      this.field00394 = new ArrayList<>();
      this.field00395 = packetEvent -> {
         if (packetEvent.isOutgoing() && packetEvent.getPacket() instanceof HandSwingC2SPacket && this.field00358.isChecked()) {
            if (mc.crosshairTarget instanceof EntityHitResult entityHitResult
               && entityHitResult.getEntity() instanceof LivingEntity livingEntity
               && entityHitResult.getEntity() != mc.player) {
               if (!this.field00362.isChecked() || this.method0559()) {
                  this.field00374.add(new ParticlesModule.Nested0357(livingEntity.getEntityPos(), livingEntity.getHeight()));
               }
            }
         }
      };
      this.field00396 = playerTickEvent -> {
         if (mc.player != null && mc.world != null) {
            long local757_2 = System.currentTimeMillis();
            this.method0628(local757_2);
            this.field00372
               .removeIf(local724_2 -> local724_2.method0634(local757_2) || WorldProjection.method1125(local724_2.field00847, 1.0F, 1.0F) == null);
            if (this.field00343.isSelected("Мир")) {
               this.field00373
                  .removeIf(
                     local733_3 -> local733_3.method0634(local757_2)
                        || local733_3.field00847.distanceTo(mc.player.getEntityPos()) > this.field00356.getValue() + 10.0
                        || WorldProjection.method1125(local733_3.field00847, 1.0F, 1.0F) == null
                  );

               for (int index = 0;
                  index < (int)this.field00353.getValue() && this.field00373.size() < (int)this.field00353.getValue();
                  index++
               ) {
                  this.method0617(local757_2);
               }
            } else {
               this.field00373.clear();
            }

            for (ParticlesModule.Nested0356 local757_5 : this.field00372) {
               local757_5.method0632();
            }

            for (ParticlesModule.Nested0356 local757_5 : this.field00373) {
               local757_5.method0632();
            }

            this.method0587();
            this.method0612(this.field00372);
            if (this.field00343.isSelected("Мир")) {
               this.method0612(this.field00373);
            }
         }
      };
      this.field00397 = worldRenderEvent -> {
         if (mc.player != null && mc.world != null) {
            long local778_2 = System.currentTimeMillis();
            this.method0611(local778_2);
            this.method0566(local778_2);
            this.method0589(worldRenderEvent.method0647(), worldRenderEvent.method0646(), local778_2);
            this.method0621(this.field00372, this.field00359, this.field00361.getValue(), worldRenderEvent.method0647(), worldRenderEvent.method0646(), local778_2);
            if (this.field00343.isSelected("Мир")) {
               this.method0621(this.field00373, this.field00352, this.field00354.getValue(), worldRenderEvent.method0647(), worldRenderEvent.method0646(), local778_2);
            }

            this.method0579(worldRenderEvent.method0647(), local778_2);
         }
      };
      this.field00343 = this.addSetting(
         new MultiSelectSetting(
            "Эффекты", "Какие частицы живут вокруг игрока.", new String[]{"Мир", "Кубы", "Пирамиды", "Тотем", "Полумесяц", "Сердце", "Звёзды", "Делюкс"}, "Мир"
         )
      );
      this.field00344 = this.addSetting(new LegacyNumberSetting("Количество", "Сколько частиц каждой фигуры держать вокруг.", 14.0, 1.0, 80.0, 1.0));
      this.field00345 = this.addSetting(new LegacyNumberSetting("Жизнь", "Время жизни частицы в миллисекундах.", 1600.0, 500.0, 5000.0, 100.0));
      this.field00346 = this.addSetting(new LegacyNumberSetting("Размер", "Множитель размера фигур.", 0.45, 0.1, 1.5, 0.05));
      this.field00347 = this.addSetting(new LegacyNumberSetting("Спавн от", "Ближе этой дистанции фигуры не спавнятся.", 4.0, 0.5, 80.0, 0.5));
      this.field00348 = this.addSetting(new LegacyNumberSetting("Спавн до", "Дальше этой дистанции фигуры не спавнятся.", 10.0, 1.0, 80.0, 0.5));
      this.field00349 = this.addSetting(new LegacyNumberSetting("Высота фигур", "Вертикальный разброс спавна фигур вверх и вниз.", 6.0, 0.5, 40.0, 0.5));
      this.field00350 = this.addSetting(new LegacyNumberSetting("Свечение", "Сила свечения фигур и картинок.", 1.0, 0.0, 3.0, 0.1));
      this.addSetting(new SectionSetting("Мир"));
      this.modeSetting = this.addSetting(new ModeSetting("Спавн мира", "Где живут мировые частицы.", "Небо", "Небо", "Земля"));
      this.field00352 = this.addSetting(
         new MultiSelectSetting(
            "Картинки мира", "Какие картинки летают в мире.", new String[]{"Светлячок", "Звезда", "Сердце", "Снежинка", "Доллар"}, "Светлячок"
         )
      );
      this.field00353 = this.addSetting(new LegacyNumberSetting("Кол-во в мире", "Сколько частиц держать в мире.", 100.0, 10.0, 300.0, 10.0));
      this.field00354 = this.addSetting(new LegacyNumberSetting("Размер в мире", "Размер мировых картинок.", 0.1, 0.03, 0.5, 0.005));
      this.field00355 = this.addSetting(new LegacyNumberSetting("Дистанция от", "Ближе этой дистанции мировые частицы не спавнятся.", 2.0, 1.0, 100.0, 1.0));
      this.field00356 = this.addSetting(new LegacyNumberSetting("Дистанция до", "Дальше этой дистанции мировые частицы не спавнятся.", 16.0, 5.0, 120.0, 1.0));
      this.field00357 = this.addSetting(new LegacyNumberSetting("Высота мира", "Насколько высоко над игроком спавнятся небесные частицы.", 8.0, 1.5, 40.0, 0.5));
      this.addSetting(new SectionSetting("Хит партиклы"));
      this.field00358 = this.addSetting(new BooleanSetting("Хит партиклы", "Разлетающиеся картинки при ударе по цели.", false));
      this.field00359 = this.addSetting(
         new MultiSelectSetting(
            "Картинки", "Какие картинки вылетают из цели.", new String[]{"Светлячок", "Звезда", "Сердце", "Снежинка", "Доллар"}, "Светлячок"
         )
      );
      this.field00360 = this.addSetting(new LegacyNumberSetting("Кол-во", "Сколько картинок спавнить на удар.", 12.0, 1.0, 50.0, 1.0));
      this.field00361 = this.addSetting(new LegacyNumberSetting("Размер удара", "Размер картинок при ударе.", 0.1, 0.03, 0.5, 0.005));
      this.field00362 = this.addSetting(new BooleanSetting("Только криты", "Спавнить хит частицы только на критах.", false));
      this.addSetting(new SectionSetting("Цвет"));
      this.field00363 = this.addSetting(new BooleanSetting("Свой цвет", "Красить частицы своим цветом вместо темы клиента.", false));
      this.colorSetting = this.addSetting(new ColorSetting("Цвет", "Кастомный цвет частиц."));
      this.field00350.withVisibility(() -> this.field00343.getSelectedCount() > 0 || this.field00358.isChecked());
      this.modeSetting.withVisibility(() -> this.field00343.isSelected("Мир"));
      this.field00352.withVisibility(() -> this.field00343.isSelected("Мир"));
      this.field00353.withVisibility(() -> this.field00343.isSelected("Мир"));
      this.field00354.withVisibility(() -> this.field00343.isSelected("Мир"));
      this.field00355.withVisibility(() -> this.field00343.isSelected("Мир"));
      this.field00356.withVisibility(() -> this.field00343.isSelected("Мир"));
      this.field00357.withVisibility(() -> this.field00343.isSelected("Мир") && this.modeSetting.isSelected("Небо"));
      this.field00359.withVisibility(() -> this.field00358.isChecked());
      this.field00360.withVisibility(() -> this.field00358.isChecked());
      this.field00361.withVisibility(() -> this.field00358.isChecked());
      this.field00362.withVisibility(() -> this.field00358.isChecked());
      this.colorSetting.withVisibility(() -> this.field00363.isChecked());

      for (int index2 = 0; index2 < 8; index2++) {
         this.field00379[index2] = new Vector3f();
      }
   }

   @Override
   protected void onEnable() {
      this.field00365.clear();
      this.field00366.clear();
      this.field00367.clear();
      this.field00368.clear();
      this.field00369.clear();
      this.field00370.clear();
      this.field00371.clear();
      this.field00372.clear();
      this.field00373.clear();
      this.field00374.clear();
      this.field00375 = System.currentTimeMillis();
      if (!this.field00391) {
         this.field00391 = true;
         Thread thread = new Thread(() -> {
            this.method0576();
            this.method0605();
            this.method0620();
            this.method0578();
            this.method0586();
         }, "deluxe-particles-models");
         thread.setDaemon(true);
         thread.start();
      }
   }

   private ParticlesModule.Nested0353 method0569(
      Vec3d vec3d, double local727_2, double local727_4, long local727_6, float local727_8, float local727_9, float local727_10
   ) {
      ThreadLocalRandom threadLocalRandom = ThreadLocalRandom.current();
      double local727_12 = threadLocalRandom.nextDouble() * Math.PI * 2.0;
      double local727_14 = local727_2 + threadLocalRandom.nextDouble() * (local727_4 - local727_2);
      double local727_16 = threadLocalRandom.nextDouble() * Math.PI * 2.0;
      double local727_18 = Math.acos(2.0 * threadLocalRandom.nextDouble() - 1.0);
      double local727_20 = 1.6 + threadLocalRandom.nextDouble() * 1.8;
      return new ParticlesModule.Nested0353(
         vec3d.add(
            Math.cos(local727_12) * local727_14,
            1.0 + threadLocalRandom.nextDouble(-this.field00349.getValue(), this.field00349.getValue()),
            Math.sin(local727_12) * local727_14
         ),
         local727_12,
         local727_6,
         this.method0609(this.field00345.getValue()),
         threadLocalRandom.nextInt(180),
         (threadLocalRandom.nextFloat() - 0.5F) * local727_8,
         (threadLocalRandom.nextFloat() - 0.5F) * local727_9,
         (threadLocalRandom.nextFloat() - 0.5F) * local727_10,
         Math.sin(local727_18) * Math.cos(local727_16) * local727_20,
         Math.cos(local727_18) * local727_20,
         Math.sin(local727_18) * Math.sin(local727_16) * local727_20
      );
   }

   private int method0570(BufferBuilder bufferBuilder, long local728_2) {
      float local728_4 = this.field00346.getValue() * 0.75F;
      int local728_5 = 0;

      for (ParticlesModule.Nested0353 local728_7 : this.field00366) {
         float local728_8 = (float)(local728_2 - local728_7.field00808) / (float)local728_7.field00809;
         if (!(local728_8 >= 1.0F)) {
            Vec3d vec3d = local728_7.method0629(local728_8);
            if (WorldProjection.method1124(vec3d.x, vec3d.y, vec3d.z, 2.0F)) {
               float local728_10 = local728_4 * (0.65F + local728_8 * 0.55F) * 0.5F;
               this.field00378
                  .identity()
                  .rotateY(local728_7.field00812 * local728_8)
                  .rotateX(local728_7.field00811 * local728_8)
                  .rotateZ(local728_7.field00813 * local728_8);
               this.field00378.transform(this.field00379[0].set(-local728_10, -local728_10, -local728_10));
               this.field00378.transform(this.field00379[1].set(local728_10, -local728_10, -local728_10));
               this.field00378.transform(this.field00379[2].set(local728_10, -local728_10, local728_10));
               this.field00378.transform(this.field00379[3].set(-local728_10, -local728_10, local728_10));
               this.field00378.transform(this.field00379[4].set(0.0F, local728_10, 0.0F));
               int local728_11 = Colors.method3727(this.method0625(local728_7.field00810), this.method0600(local728_8));

               for (int index = 0; index < field00340.length; index += 2) {
                  local728_5 += this.method0606(
                     bufferBuilder,
                     vec3d.x + this.field00379[field00340[index]].x,
                     vec3d.y + this.field00379[field00340[index]].y,
                     vec3d.z + this.field00379[field00340[index]].z,
                     vec3d.x + this.field00379[field00340[index + 1]].x,
                     vec3d.y + this.field00379[field00340[index + 1]].y,
                     vec3d.z + this.field00379[field00340[index + 1]].z,
                     local728_11
                  );
               }
            }
         }
      }

      return local728_5;
   }

   private static void method0571(
      HashMap<String, ParticlesModule.Nested0354> local729_0,
      float local729_1,
      float local729_2,
      float local729_3,
      float local729_4,
      float local729_5,
      float local729_6,
      float local729_7,
      float local729_8,
      float local729_9
   ) {
      int local729_10 = Math.round(local729_1 * 10000.0F);
      int local729_11 = Math.round(local729_2 * 10000.0F);
      int local729_12 = Math.round(local729_3 * 10000.0F);
      int local729_13 = Math.round(local729_4 * 10000.0F);
      int local729_14 = Math.round(local729_5 * 10000.0F);
      int local729_15 = Math.round(local729_6 * 10000.0F);
      boolean local729_16 = local729_10 != local729_13
         ? local729_10 > local729_13
         : (local729_11 != local729_14 ? local729_11 > local729_14 : local729_12 > local729_15);
      String local729_17 = local729_16
         ? local729_13 + "," + local729_14 + "," + local729_15 + "|" + local729_10 + "," + local729_11 + "," + local729_12
         : local729_10 + "," + local729_11 + "," + local729_12 + "|" + local729_13 + "," + local729_14 + "," + local729_15;
      ParticlesModule.Nested0354 local729_18 = local729_0.get(local729_17);
      if (local729_18 == null) {
         local729_0.put(
            local729_17,
            new ParticlesModule.Nested0354(local729_1, local729_2, local729_3, local729_4, local729_5, local729_6, local729_7, local729_8, local729_9)
         );
      } else {
         local729_18.field00836++;
         if (Math.abs(local729_18.field00833 * local729_7 + local729_18.field00834 * local729_8 + local729_18.field00835 * local729_9) < 0.985F) {
            local729_18.field00837 = false;
         }
      }
   }

   private void method0572(MatrixStack matrixStack, BufferBuilder bufferBuilder, int local730_3) {
      BuiltBuffer builtBuffer = bufferBuilder.endNullable();
      if (builtBuffer != null) {
         try (builtBuffer) {
            if (local730_3 != 0 && mc.getFramebuffer() != null && mc.getFramebuffer().getColorAttachmentView() != null) {
               if (this.field00377 != null && !this.field00377.isClosed() && this.field00377.size() >= builtBuffer.getBuffer().remaining()) {
                  RenderSystem.getDevice()
                     .createCommandEncoder()
                     .writeToBuffer(this.field00377.slice(0L, builtBuffer.getBuffer().remaining()), builtBuffer.getBuffer());
               } else {
                  if (this.field00377 != null && !this.field00377.isClosed()) {
                     this.field00377.close();
                  }

                  this.field00377 = RenderSystem.getDevice().createBuffer(() -> "deluxe_particles_wires", 40, builtBuffer.getBuffer());
               }

               GpuBufferSlice gpuBufferSlice = RenderSystem.getDynamicUniforms().write(matrixStack.peek().getPositionMatrix(), field00334, field00335, field00336);

               try (RenderPass renderPass = RenderSystem.getDevice()
                     .createCommandEncoder()
                     .createRenderPass(
                        () -> "deluxe_particles_wires",
                        mc.getFramebuffer().getColorAttachmentView(),
                        OptionalInt.empty(),
                        DepthSnapshot.method4228(),
                        OptionalDouble.empty()
                     )) {
                  renderPass.setPipeline(field00338);
                  renderPass.setUniform("DynamicTransforms", gpuBufferSlice);
                  renderPass.setUniform("Projection", RenderSystem.getProjectionMatrixBuffer());
                  renderPass.setVertexBuffer(0, this.field00377);
                  renderPass.setIndexBuffer(
                     RenderSystem.getSequentialBuffer(DrawMode.QUADS).getIndexBuffer(local730_3 * 6),
                     RenderSystem.getSequentialBuffer(DrawMode.QUADS).getIndexType()
                  );
                  renderPass.drawIndexed(0, 0, local730_3 * 6, 1);
                  return;
               }
            }
         }
      }
   }

   private synchronized float[] method0576() {
      if (this.field00387) {
         return this.field00382;
      }

      this.field00387 = true;
      this.field00382 = this.method0622("/assets/deluxe/models/particles/totem_undying.obj", -0.75F, false);
      return this.field00382;
   }

   private synchronized float[] method0578() {
      if (this.field00390) {
         return this.field00386;
      }

      this.field00390 = true;
      InputStream resourceAsStream = ParticlesModule.class.getResourceAsStream("/assets/deluxe/models/particles/deluxe.obj");
      if (resourceAsStream == null) {
         return null;
      }

      try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(resourceAsStream, StandardCharsets.UTF_8))) {
         ArrayList<Vector3f> local736_3 = new ArrayList<>();
         HashMap<String, ParticlesModule.Nested0354> local736_4 = new HashMap<>();
         float local736_5 = Float.POSITIVE_INFINITY;
         float local736_6 = Float.POSITIVE_INFINITY;
         float local736_7 = Float.POSITIVE_INFINITY;
         float local736_8 = Float.NEGATIVE_INFINITY;
         float local736_9 = Float.NEGATIVE_INFINITY;
         float local736_10 = Float.NEGATIVE_INFINITY;

         String local736_11;
         while ((local736_11 = bufferedReader.readLine()) != null) {
            local736_11 = local736_11.trim();
            if (local736_11.startsWith("v ")) {
               String[] stringArray = local736_11.split("\\s+");
               Vector3f vector3f = new Vector3f(Float.parseFloat(stringArray[1]), -Float.parseFloat(stringArray[2]), Float.parseFloat(stringArray[3]));
               local736_3.add(vector3f);
               local736_5 = Math.min(local736_5, vector3f.x);
               local736_8 = Math.max(local736_8, vector3f.x);
               local736_6 = Math.min(local736_6, vector3f.y);
               local736_9 = Math.max(local736_9, vector3f.y);
               local736_7 = Math.min(local736_7, vector3f.z);
               local736_10 = Math.max(local736_10, vector3f.z);
            } else if (local736_11.startsWith("f ")) {
               String[] stringArray2 = local736_11.split("\\s+");
               Vector3f vector3f2 = local736_3.get(Integer.parseInt(stringArray2[1].split("/")[0]) - 1);
               Vector3f vector3f3 = local736_3.get(Integer.parseInt(stringArray2[2].split("/")[0]) - 1);
               Vector3f vector3f4 = local736_3.get(Integer.parseInt(stringArray2[3].split("/")[0]) - 1);
               float local736_16 = (vector3f3.y - vector3f2.y) * (vector3f4.z - vector3f2.z)
                  - (vector3f3.z - vector3f2.z) * (vector3f4.y - vector3f2.y);
               float local736_17 = (vector3f3.z - vector3f2.z) * (vector3f4.x - vector3f2.x)
                  - (vector3f3.x - vector3f2.x) * (vector3f4.z - vector3f2.z);
               float local736_18 = (vector3f3.x - vector3f2.x) * (vector3f4.y - vector3f2.y)
                  - (vector3f3.y - vector3f2.y) * (vector3f4.x - vector3f2.x);
               float local736_19 = (float)Math.sqrt(local736_16 * local736_16 + local736_17 * local736_17 + local736_18 * local736_18);
               if (!(local736_19 < 1.0E-8F)) {
                  method0571(
                     local736_4,
                     vector3f2.x,
                     vector3f2.y,
                     vector3f2.z,
                     vector3f3.x,
                     vector3f3.y,
                     vector3f3.z,
                     local736_16 / local736_19,
                     local736_17 / local736_19,
                     local736_18 / local736_19
                  );
                  method0571(
                     local736_4,
                     vector3f3.x,
                     vector3f3.y,
                     vector3f3.z,
                     vector3f4.x,
                     vector3f4.y,
                     vector3f4.z,
                     local736_16 / local736_19,
                     local736_17 / local736_19,
                     local736_18 / local736_19
                  );
                  method0571(
                     local736_4,
                     vector3f4.x,
                     vector3f4.y,
                     vector3f4.z,
                     vector3f2.x,
                     vector3f2.y,
                     vector3f2.z,
                     local736_16 / local736_19,
                     local736_17 / local736_19,
                     local736_18 / local736_19
                  );
               }
            }
         }

         float local736_12 = (local736_5 + local736_8) * 0.5F;
         float local736_13 = (local736_6 + local736_9) * 0.5F;
         float local736_14 = (local736_7 + local736_10) * 0.5F;
         float local736_15 = Math.max(local736_8 - local736_5, Math.max(local736_9 - local736_6, local736_10 - local736_7)) > 1.0E-5F
            ? 1.0F / Math.max(local736_8 - local736_5, Math.max(local736_9 - local736_6, local736_10 - local736_7))
            : 1.0F;
         ArrayList<Float> local736_16 = new ArrayList<>(local736_4.size() * 6);

         for (ParticlesModule.Nested0354 local736_18 : local736_4.values()) {
            if (local736_18.field00836 == 1 || !local736_18.field00837) {
               local736_16.add((local736_18.field00827 - local736_12) * local736_15);
               local736_16.add((local736_18.field00828 - local736_13) * local736_15);
               local736_16.add((local736_18.field00829 - local736_14) * local736_15);
               local736_16.add((local736_18.field00830 - local736_12) * local736_15);
               local736_16.add((local736_18.field00831 - local736_13) * local736_15);
               local736_16.add((local736_18.field00832 - local736_14) * local736_15);
            }
         }

         this.field00386 = new float[local736_16.size()];

         for (int index = 0; index < local736_16.size(); index++) {
            this.field00386[index] = local736_16.get(index);
         }
      } catch (Exception exception) {
         this.field00386 = null;
      }

      return this.field00386;
   }

   private void method0579(MatrixStack matrixStack, long local737_2) {
      boolean local737_4 = this.field00343.isSelected("Кубы") && !this.field00365.isEmpty()
         || this.field00343.isSelected("Пирамиды") && !this.field00366.isEmpty()
         || this.field00343.isSelected("Тотем") && !this.field00367.isEmpty()
         || this.field00343.isSelected("Полумесяц") && !this.field00368.isEmpty()
         || this.field00343.isSelected("Сердце") && !this.field00369.isEmpty()
         || this.field00343.isSelected("Звёзды") && !this.field00370.isEmpty()
         || this.field00343.isSelected("Делюкс") && !this.field00371.isEmpty();
      if (local737_4) {
         BufferBuilder bufferBuilder = new BufferBuilder(this.method0583(), DrawMode.QUADS, VertexFormats.POSITION_COLOR);
         int local737_6 = 0;
         if (this.field00343.isSelected("Кубы")) {
            local737_6 += this.method0618(bufferBuilder, local737_2);
         }

         if (this.field00343.isSelected("Пирамиды")) {
            local737_6 += this.method0570(bufferBuilder, local737_2);
         }

         if (this.field00343.isSelected("Тотем")) {
            local737_6 += this.method0613(this.field00367, this.method0576(), 0.78F, 0.75F, 0.35F, true, bufferBuilder, local737_2);
         }

         if (this.field00343.isSelected("Полумесяц")) {
            local737_6 += this.method0613(this.field00368, this.method0586(), 0.72F, 0.7F, 0.45F, false, bufferBuilder, local737_2);
         }

         if (this.field00343.isSelected("Сердце")) {
            local737_6 += this.method0613(this.field00369, this.method0620(), 0.7F, 0.7F, 0.45F, false, bufferBuilder, local737_2);
         }

         if (this.field00343.isSelected("Звёзды")) {
            local737_6 += this.method0613(this.field00370, this.method0605(), 0.9F, 0.7F, 0.45F, false, bufferBuilder, local737_2);
         }

         if (this.field00343.isSelected("Делюкс")) {
            local737_6 += this.method0613(this.field00371, this.method0578(), 0.9F, 0.7F, 0.45F, false, bufferBuilder, local737_2);
         }

         this.method0572(matrixStack, bufferBuilder, local737_6);
      }
   }

   private void method0581(String local739_1, MatrixStack matrixStack, BufferBuilder bufferBuilder, int local739_4) {
      BuiltBuffer builtBuffer = bufferBuilder.endNullable();
      if (builtBuffer != null) {
         try (builtBuffer) {
            if (local739_4 != 0
               && mc.getTextureManager().getTexture(Identifier.of("deluxe", local739_1)).getGlTextureView() != null
               && mc.getFramebuffer() != null
               && mc.getFramebuffer().getColorAttachmentView() != null) {
               GpuBuffer gpuBuffer = this.field00376.get(local739_1);
               if (gpuBuffer != null && !gpuBuffer.isClosed() && gpuBuffer.size() >= builtBuffer.getBuffer().remaining()) {
                  RenderSystem.getDevice()
                     .createCommandEncoder()
                     .writeToBuffer(gpuBuffer.slice(0L, builtBuffer.getBuffer().remaining()), builtBuffer.getBuffer());
               } else {
                  if (gpuBuffer != null && !gpuBuffer.isClosed()) {
                     gpuBuffer.close();
                  }

                  gpuBuffer = RenderSystem.getDevice().createBuffer(() -> "deluxe_particles_" + local739_1, 40, builtBuffer.getBuffer());
                  this.field00376.put(local739_1, gpuBuffer);
               }

               GpuBufferSlice gpuBufferSlice = RenderSystem.getDynamicUniforms().write(matrixStack.peek().getPositionMatrix(), field00334, field00335, field00336);

               try (RenderPass renderPass = RenderSystem.getDevice()
                     .createCommandEncoder()
                     .createRenderPass(
                        () -> "deluxe_particles",
                        mc.getFramebuffer().getColorAttachmentView(),
                        OptionalInt.empty(),
                        DepthSnapshot.method4228(),
                        OptionalDouble.empty()
                     )) {
                  renderPass.setPipeline(field00337);
                  renderPass.bindTexture(
                     "Sampler0",
                     mc.getTextureManager().getTexture(Identifier.of("deluxe", local739_1)).getGlTextureView(),
                     RenderSystem.getSamplerCache().get(FilterMode.LINEAR)
                  );
                  renderPass.setUniform("DynamicTransforms", gpuBufferSlice);
                  renderPass.setUniform("Projection", RenderSystem.getProjectionMatrixBuffer());
                  renderPass.setVertexBuffer(0, gpuBuffer);
                  renderPass.setIndexBuffer(
                     RenderSystem.getSequentialBuffer(DrawMode.QUADS).getIndexBuffer(local739_4 * 6),
                     RenderSystem.getSequentialBuffer(DrawMode.QUADS).getIndexType()
                  );
                  renderPass.drawIndexed(0, 0, local739_4 * 6, 1);
                  return;
               }
            }
         }
      }
   }

   private BufferAllocator method0583() {
      if (this.field00392 == null) {
         this.field00392 = new BufferAllocator(65536);
      }

      CloseableBuffer allocated = this.field00392.getAllocated();
      if (allocated != null) {
         allocated.close();
      }

      return this.field00392;
   }

   private synchronized float[] method0586() {
      if (this.field00385 != null) {
         return this.field00385;
      }

      float local744_1 = 0.5F;
      float local744_2 = 0.42F;
      float local744_3 = 0.3F;
      float local744_4 = 0.13F;
      float local744_5 = 0.022F;
      float local744_6 = (local744_1 * local744_1 - local744_2 * local744_2 + local744_3 * local744_3) / (2.0F * local744_3);
      float local744_7 = (float)Math.sqrt(Math.max(0.0F, local744_1 * local744_1 - local744_6 * local744_6));
      float local744_8 = (float)Math.atan2(local744_7, local744_6);
      float local744_9 = (float)Math.atan2(local744_7, local744_6 - local744_3);
      int local744_10 = 18;
      int local744_11 = 3;
      float local744_12 = (local744_1 - local744_6) * 0.5F;
      float local744_13 = (float) (Math.PI * 2) - 2.0F * local744_8;
      float local744_14 = (float) (Math.PI * 2) - 2.0F * local744_9;
      float[] floatArray = new float[local744_10 + 1];
      float[] floatArray2 = new float[local744_10 + 1];
      float[] floatArray3 = new float[local744_10 + 1];
      float[] floatArray4 = new float[local744_10 + 1];
      float[] floatArray5 = new float[local744_10 + 1];
      float[] floatArray6 = new float[local744_10 + 1];

      for (int index = 0; index <= local744_10; index++) {
         float local744_22 = local744_8 + local744_13 / local744_10 * index;
         floatArray[index] = (float)Math.cos(local744_22) * local744_1 + local744_12;
         floatArray2[index] = (float)Math.sin(local744_22) * local744_1;
         floatArray3[index] = local744_5 + (local744_4 - local744_5) * (float)Math.sin((local744_22 - local744_8) / local744_13 * Math.PI);
         float local744_23 = local744_9 + local744_14 / local744_10 * index;
         floatArray4[index] = (float)Math.cos(local744_23) * local744_2 + local744_3 + local744_12;
         floatArray5[index] = (float)Math.sin(local744_23) * local744_2;
         floatArray6[index] = local744_5 + (local744_4 - local744_5) * (float)Math.sin((local744_23 - local744_9) / local744_14 * Math.PI);
      }

      ArrayList<Float> local744_21 = new ArrayList<>(local744_10 * 28 + 64);

      for (int index2 = 0; index2 < local744_10; index2++) {
         local744_21.add(floatArray[index2]);
         local744_21.add(floatArray2[index2]);
         local744_21.add(floatArray3[index2]);
         local744_21.add(floatArray[index2 + 1]);
         local744_21.add(floatArray2[index2 + 1]);
         local744_21.add(floatArray3[index2 + 1]);
         local744_21.add(floatArray[index2]);
         local744_21.add(floatArray2[index2]);
         local744_21.add(-floatArray3[index2]);
         local744_21.add(floatArray[index2 + 1]);
         local744_21.add(floatArray2[index2 + 1]);
         local744_21.add(-floatArray3[index2 + 1]);
      }

      for (int index3 = 0; index3 < local744_10; index3++) {
         local744_21.add(floatArray4[index3]);
         local744_21.add(floatArray5[index3]);
         local744_21.add(floatArray6[index3]);
         local744_21.add(floatArray4[index3 + 1]);
         local744_21.add(floatArray5[index3 + 1]);
         local744_21.add(floatArray6[index3 + 1]);
         local744_21.add(floatArray4[index3]);
         local744_21.add(floatArray5[index3]);
         local744_21.add(-floatArray6[index3]);
         local744_21.add(floatArray4[index3 + 1]);
         local744_21.add(floatArray5[index3 + 1]);
         local744_21.add(-floatArray6[index3 + 1]);
      }

      for (int index4 = 0; index4 <= local744_10; index4 += local744_11) {
         local744_21.add(floatArray[index4]);
         local744_21.add(floatArray2[index4]);
         local744_21.add(floatArray3[index4]);
         local744_21.add(floatArray[index4]);
         local744_21.add(floatArray2[index4]);
         local744_21.add(-floatArray3[index4]);
      }

      for (int index5 = local744_11; index5 < local744_10; index5 += local744_11) {
         local744_21.add(floatArray4[index5]);
         local744_21.add(floatArray5[index5]);
         local744_21.add(floatArray6[index5]);
         local744_21.add(floatArray4[index5]);
         local744_21.add(floatArray5[index5]);
         local744_21.add(-floatArray6[index5]);
      }

      this.field00385 = new float[local744_21.size()];

      for (int index6 = 0; index6 < local744_21.size(); index6++) {
         this.field00385[index6] = local744_21.get(index6);
      }

      return this.field00385;
   }

   private void method0587() {
      ArrayList<ParticlesModule.Nested0356> local745_1 = this.field00394;
      local745_1.clear();
      local745_1.addAll(this.field00372);
      if (this.field00343.isSelected("Мир")) {
         local745_1.addAll(this.field00373);
      }

      int local745_2 = 0;

      for (int index = local745_1.size(); local745_2 < index; local745_2++) {
         ParticlesModule.Nested0356 local745_4 = local745_1.get(local745_2);

         for (int index2 = local745_2 + 1; index2 < index; index2++) {
            ParticlesModule.Nested0356 local745_6 = local745_1.get(index2);
            double local745_7 = local745_4.field00856 + local745_6.field00856;
            double local745_9 = local745_6.field00847.x - local745_4.field00847.x;
            if (!(local745_9 >= local745_7) && !(-local745_9 >= local745_7)) {
               double local745_11 = local745_6.field00847.y - local745_4.field00847.y;
               if (!(local745_11 >= local745_7) && !(-local745_11 >= local745_7)) {
                  double local745_13 = local745_6.field00847.z - local745_4.field00847.z;
                  if (!(local745_13 >= local745_7) && !(-local745_13 >= local745_7)) {
                     double local745_15 = local745_9 * local745_9 + local745_11 * local745_11 + local745_13 * local745_13;
                     if (!(local745_15 >= local745_7 * local745_7) && !(local745_15 < 1.0E-8)) {
                        Vec3d vec3d = new Vec3d(local745_9, local745_11, local745_13);
                        double local745_18 = Math.sqrt(local745_15);
                        Vec3d vec3d2 = vec3d.multiply(1.0 / local745_18);
                        local745_4.field00847 = local745_4.field00847.subtract(vec3d2.multiply((local745_7 - local745_18) * 0.5));
                        local745_6.field00847 = local745_6.field00847.add(vec3d2.multiply((local745_7 - local745_18) * 0.5));
                        double local745_21 = local745_6.field00849.subtract(local745_4.field00849).dotProduct(vec3d2);
                        if (local745_21 < 0.0) {
                           local745_4.field00849 = local745_4.field00849.subtract(vec3d2.multiply(-local745_21 * 0.45));
                           local745_6.field00849 = local745_6.field00849.add(vec3d2.multiply(-local745_21 * 0.45));
                        }
                     }
                  }
               }
            }
         }
      }

      local745_1.clear();
   }

   private void method0589(MatrixStack matrixStack, float local747_2, long local747_3) {
      boolean local747_5 = this.field00350.getValue() > 0.0F && this.field00358.isChecked() && !this.field00372.isEmpty();
      boolean local747_6 = this.field00350.getValue() > 0.0F && this.field00343.isSelected("Мир") && !this.field00373.isEmpty();
      boolean local747_7 = this.field00350.getValue() > 0.0F
         && (
            this.field00343.isSelected("Кубы") && !this.field00365.isEmpty()
               || this.field00343.isSelected("Пирамиды") && !this.field00366.isEmpty()
               || this.field00343.isSelected("Тотем") && !this.field00367.isEmpty()
               || this.field00343.isSelected("Полумесяц") && !this.field00368.isEmpty()
               || this.field00343.isSelected("Сердце") && !this.field00369.isEmpty()
               || this.field00343.isSelected("Звёзды") && !this.field00370.isEmpty()
         );
      if (local747_6 || local747_7 || local747_5) {
         Quaternionf quaternionf = new Quaternionf(mc.gameRenderer.getCamera().getRotation()).rotateY((float) Math.PI);
         Vector3f vector3f = new Vector3f();
         BufferBuilder bufferBuilder = new BufferBuilder(this.method0583(), DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
         int local747_11 = 0;
         if (local747_7) {
            if (this.field00343.isSelected("Кубы")) {
               local747_11 += this.method0592(this.field00365, this.field00346.getValue() * 1.7F, false, bufferBuilder, quaternionf, vector3f, local747_3);
            }

            if (this.field00343.isSelected("Пирамиды")) {
               local747_11 += this.method0592(this.field00366, this.field00346.getValue() * 1.7F, false, bufferBuilder, quaternionf, vector3f, local747_3);
            }

            if (this.field00343.isSelected("Тотем")) {
               local747_11 += this.method0592(this.field00367, this.field00346.getValue() * 2.0F, true, bufferBuilder, quaternionf, vector3f, local747_3);
            }

            if (this.field00343.isSelected("Полумесяц")) {
               local747_11 += this.method0592(this.field00368, this.field00346.getValue() * 1.8F, false, bufferBuilder, quaternionf, vector3f, local747_3);
            }

            if (this.field00343.isSelected("Сердце")) {
               local747_11 += this.method0592(this.field00369, this.field00346.getValue() * 1.8F, false, bufferBuilder, quaternionf, vector3f, local747_3);
            }

            if (this.field00343.isSelected("Звёзды")) {
               local747_11 += this.method0592(this.field00370, this.field00346.getValue() * 1.8F, false, bufferBuilder, quaternionf, vector3f, local747_3);
            }
         }

         if (local747_5) {
            local747_11 += this.method0598(this.field00372, this.field00361.getValue() / 1.5F, local747_2, bufferBuilder, quaternionf, vector3f, local747_3);
         }

         if (local747_6) {
            local747_11 += this.method0598(this.field00373, this.field00354.getValue(), local747_2, bufferBuilder, quaternionf, vector3f, local747_3);
         }

         this.method0581("textures/particles/glowboost.png", matrixStack, bufferBuilder, local747_11);
      }
   }

   private int method0592(
      ArrayList<ParticlesModule.Nested0353> local750_1,
      float local750_2,
      boolean local750_3,
      BufferBuilder bufferBuilder,
      Quaternionf quaternionf,
      Vector3f vector3f,
      long local750_7
   ) {
      int local750_9 = 0;

      for (ParticlesModule.Nested0353 local750_11 : local750_1) {
         float local750_12 = (float)(local750_7 - local750_11.field00808) / (float)local750_11.field00809;
         if (!(local750_12 >= 1.0F)) {
            Vec3d vec3d = local750_3 ? local750_11.method0630(local750_12) : local750_11.method0629(local750_12);
            if (WorldProjection.method1124(vec3d.x, vec3d.y, vec3d.z, 2.0F)) {
               int local750_14 = this.method0625(local750_11.field00810);
               float local750_15 = 0.85F + 0.15F * MathHelper.sin((float)local750_11.field00807 * 3.0F + local750_12 * 6.0F);
               this.method0560(
                  bufferBuilder,
                  quaternionf,
                  vector3f,
                  vec3d.x,
                  vec3d.y,
                  vec3d.z,
                  local750_2 * (0.95F + local750_12 * 0.25F) * local750_15,
                  Colors.method3727(local750_14, this.method0600(local750_12) * 0.22F * this.field00350.getValue())
               );
               this.method0560(
                  bufferBuilder,
                  quaternionf,
                  vector3f,
                  vec3d.x,
                  vec3d.y,
                  vec3d.z,
                  local750_2 * 0.55F * (0.9F + local750_12 * 0.2F) * local750_15,
                  Colors.method3727(local750_14, this.method0600(local750_12) * 0.55F * Math.min(1.0F, this.field00350.getValue()))
               );
               local750_9 += 2;
            }
         }
      }

      return local750_9;
   }

   private boolean method0593(Vec3d vec3d) {
      if (mc.getCameraEntity() == null) {
         return false;
      } else {
         Vec3d cameraPos = mc.gameRenderer.getCamera().getCameraPos();
         HitResult hitResult = mc.world
            .raycast(new RaycastContext(cameraPos, vec3d, ShapeType.COLLIDER, FluidHandling.NONE, mc.getCameraEntity()));
         if (hitResult.getType() == Type.BLOCK && hitResult.getPos().squaredDistanceTo(cameraPos) < vec3d.squaredDistanceTo(cameraPos) - 0.001) {
            return false;
         } else {
            return mc.player.getBoundingBox().expand(0.15).raycast(cameraPos, vec3d).isPresent()
               ? false
               : mc.world
                  .getOtherEntities(
                     mc.getCameraEntity(),
                     mc.getCameraEntity().getBoundingBox().stretch(vec3d.subtract(cameraPos)).expand(0.3),
                     entity -> entity.getBoundingBox().expand(0.1).raycast(cameraPos, vec3d).isPresent()
                  )
                  .isEmpty();
         }
      }
   }

   public static ParticlesModule method0596() {
      if (particlesModule == null) {
         particlesModule = new ParticlesModule();
      }

      return particlesModule;
   }

   private int method0598(
      ArrayList<ParticlesModule.Nested0356> local756_1,
      float local756_2,
      float local756_3,
      BufferBuilder bufferBuilder,
      Quaternionf quaternionf,
      Vector3f vector3f,
      long local756_7
   ) {
      int local756_9 = 0;

      for (ParticlesModule.Nested0356 local756_11 : local756_1) {
         float local756_12 = local756_11.method0633(local756_7);
         if (!(local756_12 <= 0.0F)) {
            Vec3d vec3d = local756_11.field00848.lerp(local756_11.field00847, local756_3);
            if (WorldProjection.method1124(vec3d.x, vec3d.y, vec3d.z, 2.0F)) {
               int local756_14 = this.method0625(local756_11.field00853);
               float local756_15 = 0.85F + 0.15F * MathHelper.sin(local756_11.field00853 + local756_11.method0631(local756_7) * 6.0F);
               this.method0560(
                  bufferBuilder,
                  quaternionf,
                  vector3f,
                  vec3d.x,
                  vec3d.y,
                  vec3d.z,
                  local756_2 * 1.9F * local756_15,
                  Colors.method3727(local756_14, local756_12 * 0.22F * this.field00350.getValue())
               );
               this.method0560(
                  bufferBuilder,
                  quaternionf,
                  vector3f,
                  vec3d.x,
                  vec3d.y,
                  vec3d.z,
                  local756_2 * 1.1F * local756_15,
                  Colors.method3727(local756_14, local756_12 * 0.55F * Math.min(1.0F, this.field00350.getValue()))
               );
               local756_9 += 2;
            }
         }
      }

      return local756_9;
   }

   private float method0600(float local758_1) {
      if (local758_1 < 0.15F) {
         return local758_1 / 0.15F;
      } else {
         return local758_1 > 0.75F ? Math.max(0.0F, (1.0F - local758_1) / 0.25F) : 1.0F;
      }
   }

   @Override
   protected void onDisable() {
      this.field00365.clear();
      this.field00366.clear();
      this.field00367.clear();
      this.field00368.clear();
      this.field00369.clear();
      this.field00370.clear();
      this.field00371.clear();
      this.field00372.clear();
      this.field00373.clear();
      this.field00374.clear();

      for (GpuBuffer gpuBuffer : this.field00376.values()) {
         if (!gpuBuffer.isClosed()) {
            gpuBuffer.close();
         }
      }

      this.field00376.clear();
      if (this.field00377 != null && !this.field00377.isClosed()) {
         this.field00377.close();
      }

      this.field00377 = null;
      if (this.field00392 != null) {
         this.field00392.close();
         this.field00392 = null;
      }

      this.field00394.clear();
   }

   private synchronized float[] method0605() {
      if (this.field00388) {
         return this.field00383;
      }

      this.field00388 = true;
      this.field00383 = this.method0622("/assets/deluxe/models/particles/fire_nether_star.obj", 0.0F, true);
      return this.field00383;
   }

   private int method0606(
      BufferBuilder bufferBuilder,
      double local764_2,
      double local764_4,
      double local764_6,
      double local764_8,
      double local764_10,
      double local764_12,
      int local764_14
   ) {
      float local764_15 = (float)(local764_2 - mc.gameRenderer.getCamera().getCameraPos().x);
      float local764_16 = (float)(local764_4 - mc.gameRenderer.getCamera().getCameraPos().y);
      float local764_17 = (float)(local764_6 - mc.gameRenderer.getCamera().getCameraPos().z);
      float local764_18 = (float)(local764_8 - mc.gameRenderer.getCamera().getCameraPos().x);
      float local764_19 = (float)(local764_10 - mc.gameRenderer.getCamera().getCameraPos().y);
      float local764_20 = (float)(local764_12 - mc.gameRenderer.getCamera().getCameraPos().z);
      float local764_21 = local764_18 - local764_15;
      float local764_22 = local764_19 - local764_16;
      float local764_23 = local764_20 - local764_17;
      float local764_24 = (local764_15 + local764_18) * 0.5F;
      float local764_25 = (local764_16 + local764_19) * 0.5F;
      float local764_26 = (local764_17 + local764_20) * 0.5F;
      float local764_27 = local764_22 * local764_26 - local764_23 * local764_25;
      float local764_28 = local764_23 * local764_24 - local764_21 * local764_26;
      float local764_29 = local764_21 * local764_25 - local764_22 * local764_24;
      float local764_30 = MathHelper.sqrt(local764_27 * local764_27 + local764_28 * local764_28 + local764_29 * local764_29);
      if (local764_30 < 1.0E-6F) {
         return 0;
      }

      int local764_31 = 0;
      if (this.field00350.getValue() > 0.05F) {
         float local764_32 = 0.0256F / local764_30;
         int local764_33 = Colors.method3727(local764_14, 0.22F + 0.1F * this.field00350.getValue());
         bufferBuilder.vertex(local764_15 - local764_27 * local764_32, local764_16 - local764_28 * local764_32, local764_17 - local764_29 * local764_32)
            .color(local764_33);
         bufferBuilder.vertex(local764_15 + local764_27 * local764_32, local764_16 + local764_28 * local764_32, local764_17 + local764_29 * local764_32)
            .color(local764_33);
         bufferBuilder.vertex(local764_18 + local764_27 * local764_32, local764_19 + local764_28 * local764_32, local764_20 + local764_29 * local764_32)
            .color(local764_33);
         bufferBuilder.vertex(local764_18 - local764_27 * local764_32, local764_19 - local764_28 * local764_32, local764_20 - local764_29 * local764_32)
            .color(local764_33);
         local764_31++;
      }

      float local764_32 = 0.008F / local764_30;
      bufferBuilder.vertex(local764_15 - local764_27 * local764_32, local764_16 - local764_28 * local764_32, local764_17 - local764_29 * local764_32)
         .color(local764_14);
      bufferBuilder.vertex(local764_15 + local764_27 * local764_32, local764_16 + local764_28 * local764_32, local764_17 + local764_29 * local764_32)
         .color(local764_14);
      bufferBuilder.vertex(local764_18 + local764_27 * local764_32, local764_19 + local764_28 * local764_32, local764_20 + local764_29 * local764_32)
         .color(local764_14);
      bufferBuilder.vertex(local764_18 - local764_27 * local764_32, local764_19 - local764_28 * local764_32, local764_20 - local764_29 * local764_32)
         .color(local764_14);
      return local764_31 + 1;
   }

   private long method0609(float local767_1) {
      return Math.max(50L, Math.round(local767_1 * (0.75F + ThreadLocalRandom.current().nextFloat() * 0.5F)));
   }

   private void method0611(long local769_1) {
      this.field00365.removeIf(local772_2 -> local769_1 - local772_2.field00808 > local772_2.field00809);
      this.field00366.removeIf(local774_2 -> local769_1 - local774_2.field00808 > local774_2.field00809);
      this.field00367.removeIf(local740_2 -> local769_1 - local740_2.field00808 > local740_2.field00809);
      this.field00368.removeIf(local732_2 -> local769_1 - local732_2.field00808 > local732_2.field00809);
      this.field00369.removeIf(local749_2 -> local769_1 - local749_2.field00808 > local749_2.field00809);
      this.field00370.removeIf(local759_2 -> local769_1 - local759_2.field00808 > local759_2.field00809);
      this.field00371.removeIf(local752_2 -> local769_1 - local752_2.field00808 > local752_2.field00809);
   }

   private void method0612(ArrayList<ParticlesModule.Nested0356> local770_1) {
      Vec3d vec3d = mc.player.getEntityPos().subtract(mc.player.lastRenderX, mc.player.lastRenderY, mc.player.lastRenderZ);

      for (ParticlesModule.Nested0356 local770_4 : local770_1) {
         double local770_5 = MathHelper.clamp(local770_4.field00847.x, mc.player.getBoundingBox().minX, mc.player.getBoundingBox().maxX);
         double local770_7 = MathHelper.clamp(local770_4.field00847.y, mc.player.getBoundingBox().minY, mc.player.getBoundingBox().maxY);
         double local770_9 = MathHelper.clamp(local770_4.field00847.z, mc.player.getBoundingBox().minZ, mc.player.getBoundingBox().maxZ);
         Vec3d vec3d2 = local770_4.field00847.subtract(local770_5, local770_7, local770_9);
         double local770_12 = vec3d2.lengthSquared();
         if (!(local770_12 >= local770_4.field00856 * local770_4.field00856)) {
            double local770_14 = Math.sqrt(Math.max(local770_12, 1.0E-8));
            Vec3d vec3d3 = local770_14 < 1.0E-4
               ? local770_4.field00847.subtract(mc.player.getEntityPos())
               : vec3d2.multiply(1.0 / local770_14);
            vec3d3 = vec3d3.lengthSquared() < 1.0E-8 ? new Vec3d(0.0, 1.0, 0.0) : vec3d3.normalize();
            local770_4.field00847 = local770_4.field00847.add(vec3d3.multiply(local770_4.field00856 - local770_14 + 0.01));
            local770_4.field00849 = local770_4.field00849
               .add(vec3d3.multiply(0.035 + Math.min(vec3d.length() * 0.8, 0.25)))
               .add(vec3d.x * 0.4, Math.max(vec3d.y * 0.2, 0.0), vec3d.z * 0.4);
         }
      }
   }

   private int method0613(
      ArrayList<ParticlesModule.Nested0353> local771_1,
      float[] floatArray,
      float local771_3,
      float local771_4,
      float local771_5,
      boolean local771_6,
      BufferBuilder bufferBuilder,
      long local771_8
   ) {
      if (floatArray != null && floatArray.length != 0) {
         float local771_10 = this.field00346.getValue() * local771_3;
         int local771_11 = 0;

         for (ParticlesModule.Nested0353 local771_13 : local771_1) {
            float local771_14 = (float)(local771_8 - local771_13.field00808) / (float)local771_13.field00809;
            if (!(local771_14 >= 1.0F)) {
               Vec3d vec3d = local771_6 ? local771_13.method0630(local771_14) : local771_13.method0629(local771_14);
               if (WorldProjection.method1124(vec3d.x, vec3d.y, vec3d.z, 2.0F)) {
                  float local771_16 = local771_10 * (local771_4 + local771_14 * local771_5);
                  this.field00378
                     .identity()
                     .rotateY(local771_13.field00812 * local771_14)
                     .rotateX(local771_13.field00811 * local771_14)
                     .rotateZ(local771_13.field00813 * local771_14);
                  int local771_17 = Colors.method3727(this.method0625(local771_13.field00810), this.method0600(local771_14));

                  for (int index = 0; index < floatArray.length; index += 6) {
                     this.field00378
                        .transform(
                           this.field00380
                              .set(floatArray[index] * local771_16, floatArray[index + 1] * local771_16, floatArray[index + 2] * local771_16)
                        );
                     this.field00378
                        .transform(
                           this.field00381
                              .set(
                                 floatArray[index + 3] * local771_16,
                                 floatArray[index + 4] * local771_16,
                                 floatArray[index + 5] * local771_16
                              )
                        );
                     local771_11 += this.method0606(
                        bufferBuilder,
                        vec3d.x + this.field00380.x,
                        vec3d.y + this.field00380.y,
                        vec3d.z + this.field00380.z,
                        vec3d.x + this.field00381.x,
                        vec3d.y + this.field00381.y,
                        vec3d.z + this.field00381.z,
                        local771_17
                     );
                  }
               }
            }
         }

         return local771_11;
      } else {
         return 0;
      }
   }

   private void method0615(
      ArrayList<ParticlesModule.Nested0353> local773_1,
      Vec3d vec3d,
      double local773_3,
      double local773_5,
      long local773_7,
      int local773_9,
      float local773_10,
      float local773_11,
      float local773_12
   ) {
      while (local773_1.size() > local773_9) {
         local773_1.remove(0);
      }

      while (local773_1.size() < local773_9) {
         local773_1.add(this.method0569(vec3d, local773_3, local773_5, local773_7, local773_10, local773_11, local773_12));
      }
   }

   private void method0617(long local775_1) {
      ThreadLocalRandom threadLocalRandom = ThreadLocalRandom.current();
      double local775_4 = Math.min(this.field00355.getValue(), this.field00356.getValue());
      double local775_6 = local775_4 + threadLocalRandom.nextDouble() * Math.max(0.1, this.field00356.getValue() - local775_4);
      double local775_8 = Math.toRadians(mc.player.getYaw() + threadLocalRandom.nextDouble(-80.0, 80.0));
      double local775_10 = mc.player.getX() - Math.sin(local775_8) * local775_6;
      double local775_12 = mc.player.getZ() + Math.cos(local775_8) * local775_6;
      if (this.modeSetting.isSelected("Земля")) {
         BlockPos blockPos = mc.world
            .getTopPosition(net.minecraft.world.Heightmap.Type.MOTION_BLOCKING, BlockPos.ofFloored(local775_10, mc.player.getY(), local775_12));
         Vec3d vec3d = new Vec3d(
            local775_10 + threadLocalRandom.nextDouble(), blockPos.getY() + threadLocalRandom.nextDouble(0.02, 0.12), local775_12 + threadLocalRandom.nextDouble()
         );
         if (WorldProjection.method1125(vec3d, 1.0F, 1.0F) != null) {
            this.field00373
               .add(
                  new ParticlesModule.Nested0356(
                     vec3d,
                     new Vec3d(threadLocalRandom.nextDouble(-0.02, 0.02), threadLocalRandom.nextDouble(0.02, 0.05), threadLocalRandom.nextDouble(-0.02, 0.02)),
                     local775_1,
                     2400L,
                     true,
                     threadLocalRandom.nextInt(360),
                     this.field00352.getSelectedCount() == 0 ? "Светлячок" : this.field00352.getSelectedOptions().get(threadLocalRandom.nextInt(this.field00352.getSelectedCount())),
                     this.field00354.getValue() * 0.42F
                  )
               );
         }
      } else {
         Vec3d vec3d2 = new Vec3d(
            local775_10, mc.player.getY() + threadLocalRandom.nextDouble(1.0, Math.max(1.5, this.field00357.getValue())), local775_12
         );
         if (WorldProjection.method1125(vec3d2, 1.0F, 1.0F) != null) {
            this.field00373
               .add(
                  new ParticlesModule.Nested0356(
                     vec3d2,
                     Vec3d.ZERO,
                     local775_1,
                     5000L,
                     false,
                     threadLocalRandom.nextInt(360),
                     this.field00352.getSelectedCount() == 0 ? "Светлячок" : this.field00352.getSelectedOptions().get(threadLocalRandom.nextInt(this.field00352.getSelectedCount())),
                     this.field00354.getValue() * 0.42F
                  )
               );
         }
      }
   }

   private int method0618(BufferBuilder bufferBuilder, long local777_2) {
      float local777_4 = this.field00346.getValue() * 0.6F;
      int local777_5 = 0;

      for (ParticlesModule.Nested0353 local777_7 : this.field00365) {
         float local777_8 = (float)(local777_2 - local777_7.field00808) / (float)local777_7.field00809;
         if (!(local777_8 >= 1.0F)) {
            Vec3d vec3d = local777_7.method0629(local777_8);
            if (WorldProjection.method1124(vec3d.x, vec3d.y, vec3d.z, 2.0F)) {
               float local777_10 = local777_4 * (0.6F + local777_8 * 0.6F) * 0.5F;
               this.field00378
                  .identity()
                  .rotateY(local777_7.field00812 * local777_8)
                  .rotateX(local777_7.field00811 * local777_8)
                  .rotateZ(local777_7.field00813 * local777_8);

               for (int index = 0; index < 8; index++) {
                  this.field00378
                     .transform(
                        this.field00379[index]
                           .set(
                              (index & 1) == 0 ? -local777_10 : local777_10,
                              (index & 2) == 0 ? -local777_10 : local777_10,
                              (index & 4) == 0 ? -local777_10 : local777_10
                           )
                     );
               }

               int local777_11 = Colors.method3727(this.method0625(local777_7.field00810), this.method0600(local777_8));

               for (int index2 = 0; index2 < field00339.length; index2 += 2) {
                  local777_5 += this.method0606(
                     bufferBuilder,
                     vec3d.x + this.field00379[field00339[index2]].x,
                     vec3d.y + this.field00379[field00339[index2]].y,
                     vec3d.z + this.field00379[field00339[index2]].z,
                     vec3d.x + this.field00379[field00339[index2 + 1]].x,
                     vec3d.y + this.field00379[field00339[index2 + 1]].y,
                     vec3d.z + this.field00379[field00339[index2 + 1]].z,
                     local777_11
                  );
               }
            }
         }
      }

      return local777_5;
   }

   private synchronized float[] method0620() {
      if (this.field00389) {
         return this.field00384;
      }

      this.field00389 = true;
      InputStream resourceAsStream = ParticlesModule.class.getResourceAsStream("/assets/deluxe/models/particles/heart.gltf");
      if (resourceAsStream == null) {
         return null;
      }

      try {
         JsonObject asJsonObject = JsonParser.parseString(new String(resourceAsStream.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
         String asString = asJsonObject.getAsJsonArray("buffers").get(0).getAsJsonObject().get("uri").getAsString();
         ByteBuffer byteBuffer = ByteBuffer.wrap(Base64.getDecoder().decode(asString.substring(asString.indexOf(44) + 1))).order(ByteOrder.LITTLE_ENDIAN);
         JsonArray asJsonArray = asJsonObject.getAsJsonArray("bufferViews");
         int[] intArray = new int[asJsonArray.size()];

         for (int index = 0; index < asJsonArray.size(); index++) {
            intArray[index] = asJsonArray.get(index).getAsJsonObject().has("byteOffset")
               ? asJsonArray.get(index).getAsJsonObject().get("byteOffset").getAsInt()
               : 0;
         }

         JsonArray asJsonArray2 = asJsonObject.getAsJsonArray("accessors");
         int[] intArray2 = new int[asJsonArray2.size()];
         int[] intArray3 = new int[asJsonArray2.size()];
         int[] intArray4 = new int[asJsonArray2.size()];
         int[] intArray5 = new int[asJsonArray2.size()];

         for (int index2 = 0; index2 < asJsonArray2.size(); index2++) {
            JsonObject asJsonObject2 = asJsonArray2.get(index2).getAsJsonObject();
            intArray2[index2] = asJsonObject2.get("bufferView").getAsInt();
            intArray3[index2] = asJsonObject2.get("componentType").getAsInt();
            intArray4[index2] = asJsonObject2.get("count").getAsInt();
            intArray5[index2] = asJsonObject2.has("byteOffset") ? asJsonObject2.get("byteOffset").getAsInt() : 0;
         }

         JsonArray asJsonArray3 = asJsonObject.getAsJsonArray("nodes");
         float[][] floatArrayArray = new float[asJsonArray3.size()][3];
         boolean[] booleanArray = new boolean[asJsonArray3.size()];
         JsonArray asJsonArray4 = asJsonObject.getAsJsonArray("scenes")
            .get(asJsonObject.has("scene") ? asJsonObject.get("scene").getAsInt() : 0)
            .getAsJsonObject()
            .getAsJsonArray("nodes");

         for (int index3 = 0; index3 < asJsonArray4.size(); index3++) {
            method0564(asJsonArray3, asJsonArray4.get(index3).getAsInt(), 0.0F, 0.0F, 0.0F, floatArrayArray, booleanArray);
         }

         JsonArray asJsonArray5 = asJsonObject.getAsJsonArray("meshes");
         int[] intArray6 = new int[asJsonArray5.size()];
         int[] intArray7 = new int[asJsonArray5.size()];

         for (int index4 = 0; index4 < asJsonArray5.size(); index4++) {
            JsonObject asJsonObject3 = asJsonArray5.get(index4).getAsJsonObject().getAsJsonArray("primitives").get(0).getAsJsonObject();
            intArray6[index4] = asJsonObject3.getAsJsonObject("attributes").get("POSITION").getAsInt();
            intArray7[index4] = asJsonObject3.get("indices").getAsInt();
         }

         HashMap<String, ParticlesModule.Nested0354> local779_19 = new HashMap<>();
         float local779_20 = Float.POSITIVE_INFINITY;
         float local779_21 = Float.POSITIVE_INFINITY;
         float local779_22 = Float.POSITIVE_INFINITY;
         float local779_23 = Float.NEGATIVE_INFINITY;
         float local779_24 = Float.NEGATIVE_INFINITY;
         float local779_25 = Float.NEGATIVE_INFINITY;

         for (int index5 = 0; index5 < asJsonArray3.size(); index5++) {
            JsonObject asJsonObject4 = asJsonArray3.get(index5).getAsJsonObject();
            if (asJsonObject4.has("mesh")) {
               int asInt = asJsonObject4.get("mesh").getAsInt();
               int local779_29 = intArray[intArray2[intArray6[asInt]]] + intArray5[intArray6[asInt]];
               float[] floatArray = new float[intArray4[intArray6[asInt]] * 3];

               for (int index6 = 0; index6 < intArray4[intArray6[asInt]]; index6++) {
                  floatArray[index6 * 3] = byteBuffer.getFloat(local779_29 + index6 * 12) + floatArrayArray[index5][0];
                  floatArray[index6 * 3 + 1] = byteBuffer.getFloat(local779_29 + index6 * 12 + 4) + floatArrayArray[index5][1];
                  floatArray[index6 * 3 + 2] = byteBuffer.getFloat(local779_29 + index6 * 12 + 8) + floatArrayArray[index5][2];
                  local779_20 = Math.min(local779_20, floatArray[index6 * 3]);
                  local779_23 = Math.max(local779_23, floatArray[index6 * 3]);
                  local779_21 = Math.min(local779_21, floatArray[index6 * 3 + 1]);
                  local779_24 = Math.max(local779_24, floatArray[index6 * 3 + 1]);
                  local779_22 = Math.min(local779_22, floatArray[index6 * 3 + 2]);
                  local779_25 = Math.max(local779_25, floatArray[index6 * 3 + 2]);
               }

               int local779_31 = intArray[intArray2[intArray7[asInt]]] + intArray5[intArray7[asInt]];
               int[] intArray8 = new int[intArray4[intArray7[asInt]]];

               for (int index7 = 0; index7 < intArray8.length; index7++) {
                  if (intArray3[intArray7[asInt]] == 5123) {
                     intArray8[index7] = byteBuffer.getShort(local779_31 + index7 * 2) & '\uffff';
                  } else if (intArray3[intArray7[asInt]] == 5125) {
                     intArray8[index7] = byteBuffer.getInt(local779_31 + index7 * 4);
                  } else {
                     intArray8[index7] = byteBuffer.get(local779_31 + index7) & 255;
                  }
               }

               for (int index8 = 0; index8 + 2 < intArray8.length; index8 += 3) {
                  float local779_34 = floatArray[intArray8[index8] * 3];
                  float local779_35 = floatArray[intArray8[index8] * 3 + 1];
                  float local779_36 = floatArray[intArray8[index8] * 3 + 2];
                  float local779_37 = floatArray[intArray8[index8 + 1] * 3];
                  float local779_38 = floatArray[intArray8[index8 + 1] * 3 + 1];
                  float local779_39 = floatArray[intArray8[index8 + 1] * 3 + 2];
                  float local779_40 = floatArray[intArray8[index8 + 2] * 3];
                  float local779_41 = floatArray[intArray8[index8 + 2] * 3 + 1];
                  float local779_42 = floatArray[intArray8[index8 + 2] * 3 + 2];
                  float local779_43 = (local779_38 - local779_35) * (local779_42 - local779_36) - (local779_39 - local779_36) * (local779_41 - local779_35);
                  float local779_44 = (local779_39 - local779_36) * (local779_40 - local779_34) - (local779_37 - local779_34) * (local779_42 - local779_36);
                  float local779_45 = (local779_37 - local779_34) * (local779_41 - local779_35) - (local779_38 - local779_35) * (local779_40 - local779_34);
                  float local779_46 = (float)Math.sqrt(local779_43 * local779_43 + local779_44 * local779_44 + local779_45 * local779_45);
                  if (!(local779_46 < 1.0E-8F)) {
                     method0571(
                        local779_19,
                        local779_34,
                        local779_35,
                        local779_36,
                        local779_37,
                        local779_38,
                        local779_39,
                        local779_43 / local779_46,
                        local779_44 / local779_46,
                        local779_45 / local779_46
                     );
                     method0571(
                        local779_19,
                        local779_37,
                        local779_38,
                        local779_39,
                        local779_40,
                        local779_41,
                        local779_42,
                        local779_43 / local779_46,
                        local779_44 / local779_46,
                        local779_45 / local779_46
                     );
                     method0571(
                        local779_19,
                        local779_40,
                        local779_41,
                        local779_42,
                        local779_34,
                        local779_35,
                        local779_36,
                        local779_43 / local779_46,
                        local779_44 / local779_46,
                        local779_45 / local779_46
                     );
                  }
               }
            }
         }

         float local779_26 = (local779_20 + local779_23) * 0.5F;
         float local779_27 = (local779_21 + local779_24) * 0.5F;
         float local779_28 = (local779_22 + local779_25) * 0.5F;
         float local779_29 = Math.max(local779_23 - local779_20, Math.max(local779_24 - local779_21, local779_25 - local779_22)) > 1.0E-5F
            ? 1.2F / Math.max(local779_23 - local779_20, Math.max(local779_24 - local779_21, local779_25 - local779_22))
            : 1.0F;
         ArrayList<Float> local779_30 = new ArrayList<>(local779_19.size() * 6);

         for (ParticlesModule.Nested0354 local779_32 : local779_19.values()) {
            if (local779_32.field00836 == 1 || !local779_32.field00837) {
               local779_30.add((local779_32.field00827 - local779_26) * local779_29);
               local779_30.add((local779_32.field00828 - local779_27) * local779_29);
               local779_30.add((local779_32.field00829 - local779_28) * local779_29);
               local779_30.add((local779_32.field00830 - local779_26) * local779_29);
               local779_30.add((local779_32.field00831 - local779_27) * local779_29);
               local779_30.add((local779_32.field00832 - local779_28) * local779_29);
            }
         }

         this.field00384 = new float[local779_30.size()];

         for (int index9 = 0; index9 < local779_30.size(); index9++) {
            this.field00384[index9] = local779_30.get(index9);
         }
      } catch (Exception exception) {
         this.field00384 = null;
      }

      return this.field00384;
   }

   private void method0621(
      ArrayList<ParticlesModule.Nested0356> local780_1,
      MultiSelectSetting multiSelectSetting,
      float local780_3,
      MatrixStack matrixStack,
      float local780_5,
      long local780_6
   ) {
      if (!local780_1.isEmpty()) {
         Quaternionf quaternionf = new Quaternionf(mc.gameRenderer.getCamera().getRotation()).rotateY((float) Math.PI);
         Vector3f vector3f = new Vector3f();
         Arrays.fill(this.field00393, 0);
         int local780_10 = 0;
         int local780_11 = 0;

         for (int index = local780_1.size(); local780_11 < index; local780_11++) {
            int local780_13 = local780_1.get(local780_11).field00855;
            if (local780_13 >= 0) {
               this.field00393[local780_13]++;
            } else {
               local780_10++;
            }
         }

         for (String local780_12 : multiSelectSetting.getOptions()) {
            int local780_13 = method0562(local780_12);
            if (local780_13 >= 0 ? this.field00393[local780_13] != 0 : local780_10 != 0) {
               BufferBuilder bufferBuilder = new BufferBuilder(this.method0583(), DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
               int local780_15 = 0;
               int local780_16 = 0;

               for (int index2 = local780_1.size(); local780_16 < index2; local780_16++) {
                  ParticlesModule.Nested0356 local780_18 = local780_1.get(local780_16);
                  if (local780_13 >= 0 ? local780_18.field00855 == local780_13 : local780_18.field00854.equals(local780_12)) {
                     float local780_19 = local780_18.method0633(local780_6);
                     if (!(local780_19 <= 0.0F)) {
                        Vec3d vec3d = local780_18.field00848.lerp(local780_18.field00847, local780_5);
                        if (WorldProjection.method1124(vec3d.x, vec3d.y, vec3d.z, 2.0F)) {
                           this.method0560(
                              bufferBuilder,
                              quaternionf,
                              vector3f,
                              vec3d.x,
                              vec3d.y,
                              vec3d.z,
                              local780_3 / 2.0F,
                              Colors.method3727(this.method0625(local780_18.field00853), local780_19)
                           );
                           local780_15++;
                        }
                     }
                  }
               }

               this.method0581(this.method0623(local780_12), matrixStack, bufferBuilder, local780_15);
            }
         }
      }
   }

   private float[] method0622(String local781_1, float local781_2, boolean local781_3) {
      InputStream resourceAsStream = ParticlesModule.class.getResourceAsStream(local781_1);
      if (resourceAsStream == null) {
         return null;
      }

      try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(resourceAsStream, StandardCharsets.UTF_8))) {
         ArrayList<Vector3f> local781_6 = new ArrayList<>();
         HashMap<Long, ParticlesModule.Nested0355> local781_7 = new HashMap<>();
         float local781_8 = Float.POSITIVE_INFINITY;
         float local781_9 = Float.POSITIVE_INFINITY;
         float local781_10 = Float.POSITIVE_INFINITY;
         float local781_11 = Float.NEGATIVE_INFINITY;
         float local781_12 = Float.NEGATIVE_INFINITY;
         float local781_13 = Float.NEGATIVE_INFINITY;

         String local781_14;
         while ((local781_14 = bufferedReader.readLine()) != null) {
            local781_14 = local781_14.trim();
            if (local781_14.startsWith("v ")) {
               String[] stringArray = local781_14.split("\\s+");
               Vector3f vector3f = new Vector3f(
                  Float.parseFloat(stringArray[1]), Float.parseFloat(stringArray[2]) + local781_2, Float.parseFloat(stringArray[3])
               );
               local781_6.add(vector3f);
               local781_8 = Math.min(local781_8, vector3f.x);
               local781_11 = Math.max(local781_11, vector3f.x);
               local781_9 = Math.min(local781_9, vector3f.y);
               local781_12 = Math.max(local781_12, vector3f.y);
               local781_10 = Math.min(local781_10, vector3f.z);
               local781_13 = Math.max(local781_13, vector3f.z);
            } else if (local781_14.startsWith("f ")) {
               String[] stringArray2 = local781_14.split("\\s+");

               for (int index = 1; index < stringArray2.length; index++) {
                  int local781_17 = Integer.parseInt(stringArray2[index].split("/")[0]) - 1;
                  int local781_18 = Integer.parseInt(stringArray2[index == stringArray2.length - 1 ? 1 : index + 1].split("/")[0]) - 1;
                  if (local781_17 != local781_18) {
                     String[] stringArray3 = stringArray2[index].split("/");
                     int local781_20 = stringArray3.length > 2 && !stringArray3[2].isEmpty() ? Integer.parseInt(stringArray3[2]) - 1 : -1;
                     long local781_21 = (long)Math.min(local781_17, local781_18) << 32 | Math.max(local781_17, local781_18) & 4294967295L;
                     ParticlesModule.Nested0355 local781_23 = local781_7.get(local781_21);
                     if (local781_23 == null) {
                        local781_7.put(local781_21, new ParticlesModule.Nested0355(local781_6.get(local781_17), local781_6.get(local781_18), local781_20));
                     } else {
                        local781_23.field00845++;
                        if (local781_23.field00844 != local781_20) {
                           local781_23.field00846 = false;
                        }
                     }
                  }
               }
            }
         }

         float local781_15 = local781_3 ? (local781_8 + local781_11) * 0.5F : 0.0F;
         float local781_16 = local781_3 ? (local781_9 + local781_12) * 0.5F : 0.0F;
         float local781_17 = local781_3 ? (local781_10 + local781_13) * 0.5F : 0.0F;
         float local781_18 = local781_3 && Math.max(local781_11 - local781_8, Math.max(local781_12 - local781_9, local781_13 - local781_10)) > 1.0E-5F
            ? 1.0F / Math.max(local781_11 - local781_8, Math.max(local781_12 - local781_9, local781_13 - local781_10))
            : 1.0F;
         ArrayList<Float> local781_19 = new ArrayList<>();

         for (ParticlesModule.Nested0355 local781_21 : local781_7.values()) {
            if (local781_21.field00845 == 1 || !local781_21.field00846) {
               local781_19.add((local781_21.field00838 - local781_15) * local781_18);
               local781_19.add((local781_21.field00839 - local781_16) * local781_18);
               local781_19.add((local781_21.field00840 - local781_17) * local781_18);
               local781_19.add((local781_21.field00841 - local781_15) * local781_18);
               local781_19.add((local781_21.field00842 - local781_16) * local781_18);
               local781_19.add((local781_21.field00843 - local781_17) * local781_18);
            }
         }

         float[] floatArray = new float[local781_19.size()];

         for (int index2 = 0; index2 < local781_19.size(); index2++) {
            floatArray[index2] = local781_19.get(index2);
         }

         return floatArray;
      } catch (Exception exception) {
         return null;
      }
   }

   private String method0623(String local782_1) {
      return switch (local782_1) {
         case "Звезда" -> "textures/particles/star.png";
         case "Сердце" -> "textures/particles/heart.png";
         case "Снежинка" -> "textures/particles/snowflake.png";
         case "Доллар" -> "textures/particles/dollar.png";
         default -> "textures/particles/firefly.png";
      };
   }

   private int method0625(int local784_1) {
      return this.field00363.isChecked()
         ? this.colorSetting.getValue()
         : 0xFF000000 | ThemePaletteBuffer.method2155((float)(System.currentTimeMillis() % 3600L) / 3600.0F + local784_1 / 360.0F);
   }

   private void method0628(long local787_1) {
      ParticlesModule.Nested0357 local787_3;
      while ((local787_3 = this.field00374.poll()) != null) {
         if (this.field00358.isChecked() && this.field00359.getSelectedCount() != 0) {
            for (int index = 0; index < Math.max(1, (int)this.field00360.getValue()); index++) {
               ThreadLocalRandom threadLocalRandom = ThreadLocalRandom.current();
               if (this.field00372.size() < 200) {
                  this.field00372
                     .add(
                        new ParticlesModule.Nested0356(
                           new Vec3d(
                              local787_3.feetPos().x, local787_3.feetPos().y + threadLocalRandom.nextDouble() * local787_3.heightBlocks(), local787_3.feetPos().z
                           ),
                           new Vec3d(threadLocalRandom.nextDouble(-2.0, 2.0) * 0.08, threadLocalRandom.nextDouble(0.05, 0.2) * 0.08, threadLocalRandom.nextDouble(-2.0, 2.0) * 0.08),
                           local787_1,
                           2200L,
                           false,
                           threadLocalRandom.nextInt(360),
                           this.field00359.getSelectedOptions().get(threadLocalRandom.nextInt(this.field00359.getSelectedCount())),
                           this.field00361.getValue() * 0.42F
                        )
                     );
               }
            }
         }
      }
   }

   private static final class Nested0353 {
      final Vec3d field00806;
      final double field00807;
      final long field00808;
      final long field00809;
      final int field00810;
      final float field00811;
      final float field00812;
      final float field00813;
      final double field00814;
      final double field00815;
      final double field00816;

      Vec3d method0629(float local788_1) {
         return new Vec3d(
            this.field00806.x + this.field00814 * local788_1,
            this.field00806.y + this.field00815 * local788_1,
            this.field00806.z + this.field00816 * local788_1
         );
      }

      Vec3d method0630(float local789_1) {
         return new Vec3d(
            this.field00806.x + MathHelper.sin((float)this.field00807 * 7.0F + local789_1 * 4.0F) * 0.6F,
            this.field00806.y + MathHelper.sin((float)this.field00807 * 5.0F + local789_1 * 3.0F) * 0.4F + local789_1 * 0.6F,
            this.field00806.z + MathHelper.cos((float)this.field00807 * 9.0F + local789_1 * 5.0F) * 0.6F
         );
      }

      Nested0353(
         Vec3d vec3d,
         double local790_2,
         long local790_4,
         long local790_6,
         int local790_8,
         float local790_9,
         float local790_10,
         float local790_11,
         double local790_12,
         double local790_14,
         double local790_16
      ) {
         this.field00806 = vec3d;
         this.field00807 = local790_2;
         this.field00808 = local790_4;
         this.field00809 = local790_6;
         this.field00810 = local790_8;
         this.field00811 = local790_9;
         this.field00812 = local790_10;
         this.field00813 = local790_11;
         this.field00814 = local790_12;
         this.field00815 = local790_14;
         this.field00816 = local790_16;
      }
   }

   private static final class Nested0354 {
      final float field00827;
      final float field00828;
      final float field00829;
      final float field00830;
      final float field00831;
      final float field00832;
      final float field00833;
      final float field00834;
      final float field00835;
      int field00836 = 1;
      boolean field00837 = true;

      Nested0354(
         float local791_1,
         float local791_2,
         float local791_3,
         float local791_4,
         float local791_5,
         float local791_6,
         float local791_7,
         float local791_8,
         float local791_9
      ) {
         this.field00827 = local791_1;
         this.field00828 = local791_2;
         this.field00829 = local791_3;
         this.field00830 = local791_4;
         this.field00831 = local791_5;
         this.field00832 = local791_6;
         this.field00833 = local791_7;
         this.field00834 = local791_8;
         this.field00835 = local791_9;
      }
   }

   private static final class Nested0355 {
      final float field00838;
      final float field00839;
      final float field00840;
      final float field00841;
      final float field00842;
      final float field00843;
      final int field00844;
      int field00845 = 1;
      boolean field00846 = true;

      Nested0355(Vector3f vector3f, Vector3f vector3f2, int local792_3) {
         this.field00838 = vector3f.x;
         this.field00839 = vector3f.y;
         this.field00840 = vector3f.z;
         this.field00841 = vector3f2.x;
         this.field00842 = vector3f2.y;
         this.field00843 = vector3f2.z;
         this.field00844 = local792_3;
      }
   }

   private static final class Nested0356 {
      Vec3d field00847;
      Vec3d field00848;
      Vec3d field00849;
      final long field00850;
      final long field00851;
      final boolean field00852;
      final int field00853;
      final String field00854;
      final int field00855;
      final float field00856;

      Nested0356(Vec3d vec3d, Vec3d vec3d2, long local793_3, long local793_5, boolean local793_7, int local793_8, String local793_9, float local793_10) {
         this.field00847 = vec3d;
         this.field00848 = vec3d;
         this.field00849 = vec3d2;
         this.field00850 = local793_3;
         this.field00851 = local793_5;
         this.field00852 = local793_7;
         this.field00853 = local793_8;
         this.field00854 = local793_9;
         this.field00855 = ParticlesModule.method0562(local793_9);
         this.field00856 = local793_10;
      }

      float method0631(long local794_1) {
         return MathHelper.clamp((float)(local794_1 - this.field00850) / (float)this.field00851, 0.0F, 1.0F);
      }

      void method0632() {
         this.field00848 = this.field00847;
         if (this.field00852) {
            this.field00849 = this.field00849.add(0.0, -0.0045, 0.0);
         }

         double local795_1 = this.field00847.x + this.field00849.x;
         double local795_3 = this.field00847.y + this.field00849.y;
         double local795_5 = this.field00847.z + this.field00849.z;
         if (!MinecraftAccess.mc
            .world
            .getBlockState(BlockPos.ofFloored(this.field00847.x, this.field00847.y, local795_5))
            .getCollisionShape(MinecraftAccess.mc.world, BlockPos.ofFloored(this.field00847.x, this.field00847.y, local795_5))
            .isEmpty()) {
            this.field00849 = new Vec3d(this.field00849.x, this.field00849.y, this.field00849.z * -0.8);
         }

         if (!MinecraftAccess.mc
            .world
            .getBlockState(BlockPos.ofFloored(this.field00847.x, local795_3, this.field00847.z))
            .getCollisionShape(MinecraftAccess.mc.world, BlockPos.ofFloored(this.field00847.x, local795_3, this.field00847.z))
            .isEmpty()) {
            if (this.field00852 && this.field00849.y < 0.0) {
               this.field00849 = this.field00849
                  .lerp(new Vec3d(this.field00849.x * 0.82, ThreadLocalRandom.current().nextDouble(0.012, 0.028), this.field00849.z * 0.82), 0.42);
            } else {
               this.field00849 = new Vec3d(this.field00849.x * 0.999, this.field00849.y * -0.7, this.field00849.z * 0.999);
            }
         }

         if (!MinecraftAccess.mc
            .world
            .getBlockState(BlockPos.ofFloored(local795_1, this.field00847.y, this.field00847.z))
            .getCollisionShape(MinecraftAccess.mc.world, BlockPos.ofFloored(local795_1, this.field00847.y, this.field00847.z))
            .isEmpty()) {
            this.field00849 = new Vec3d(this.field00849.x * -0.8, this.field00849.y, this.field00849.z);
         }

         if (this.field00852) {
            this.field00849 = this.field00849.multiply(0.996);
         } else {
            this.field00849 = new Vec3d(
               MathHelper.clamp(this.field00849.x + (ThreadLocalRandom.current().nextDouble() - 0.5) * 0.005, -0.5, 0.5),
               MathHelper.clamp(this.field00849.y + (ThreadLocalRandom.current().nextDouble() - 0.5) * 0.005, -0.5, 0.5),
               MathHelper.clamp(this.field00849.z + (ThreadLocalRandom.current().nextDouble() - 0.5) * 0.005, -0.5, 0.5)
            );
         }

         this.field00847 = this.field00847.add(this.field00849);
         if (!this.field00852) {
            this.field00849 = new Vec3d(this.field00849.x / 0.999999, this.field00849.y - 5.0E-5, this.field00849.z / 0.999999);
         }
      }

      float method0633(long local796_1) {
         long local796_3 = local796_1 - this.field00850;
         if (local796_3 >= this.field00851) {
            return 0.0F;
         } else if (local796_3 < 250L) {
            return (float)local796_3 / 250.0F;
         } else {
            return local796_3 >= this.field00851 - 250L ? (float)(this.field00851 - local796_3) / 250.0F : 1.0F;
         }
      }

      boolean method0634(long local797_1) {
         return local797_1 - this.field00850 >= this.field00851;
      }
   }

   private record Nested0357(Vec3d feetPos, float heightBlocks) {
   }
}
