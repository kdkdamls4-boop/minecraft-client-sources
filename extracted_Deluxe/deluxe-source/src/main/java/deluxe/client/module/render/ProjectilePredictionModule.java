package deluxe.client.module.render;

import deluxe.client.module.combat.AttackAuraModule;
import deluxe.client.setting.BooleanSetting;
import deluxe.client.util.Colors;
import deluxe.client.DeluxeClient;
import deluxe.client.util.DepthSnapshot;
import deluxe.client.event.EventListener;
import deluxe.client.render.FontFace;
import deluxe.client.event.HudRenderEvent;
import deluxe.client.module.Module;
import deluxe.client.module.ModuleCategory;
import deluxe.client.module.ModuleInfo;
import deluxe.client.util.GuiScale;
import deluxe.client.render.WorldProjection;
import deluxe.client.render.FontText;
import deluxe.client.render.ItemRenderCache;
import deluxe.client.setting.ColorSetting;
import deluxe.client.render.ShapeDrawing;
import deluxe.client.render.ThemeColors;
import deluxe.client.render.CornerRadii;
import deluxe.client.ui.hud.HudRenderContext;
import deluxe.client.ui.hud.HudStyle;
import deluxe.client.event.WorldRenderEvent;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap.Entry;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.RenderSetup;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.VertexRendering;
import net.minecraft.client.render.VertexConsumerProvider.Immediate;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ChargedProjectilesComponent;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.projectile.TridentEntity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.entity.projectile.thrown.ThrownItemEntity;
import net.minecraft.item.BowItem;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.EggItem;
import net.minecraft.item.EnderPearlItem;
import net.minecraft.item.ExperienceBottleItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.LingeringPotionItem;
import net.minecraft.item.SnowballItem;
import net.minecraft.item.SplashPotionItem;
import net.minecraft.item.TridentItem;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

@ModuleInfo(
   name = "ProjectilePrediction",
   description = "Показывает, куда прилетит снаряд из руки: траектория, маркер приземления и радиус действия, а также траектории летящих снарядов.",
   category = ModuleCategory.RENDER
)
public final class ProjectilePredictionModule extends Module {
   private static ProjectilePredictionModule projectilePredictionModule;
   public BooleanSetting booleanSetting;
   public ColorSetting colorSetting;
   private static final RenderPipeline field05040 = RenderPipeline.builder(RenderPipelines.TRANSFORMS_AND_PROJECTION_SNIPPET)
      .withLocation(DeluxeClient.id("pipeline/projectile_prediction_ribbon"))
      .withVertexShader(Identifier.ofVanilla("core/position_color"))
      .withFragmentShader(Identifier.ofVanilla("core/position_color"))
      .withVertexFormat(VertexFormats.POSITION_COLOR, DrawMode.QUADS)
      .withBlend(BlendFunction.TRANSLUCENT)
      .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
      .withDepthWrite(false)
      .withCull(false)
      .build();
   private static final RenderLayer field05041 = RenderLayer.of(
      "projectile_prediction_ribbon", RenderSetup.builder(field05040).expectedBufferSize(32768).build()
   );
   private final float[] field05042 = new float[8];
   private final ArrayList<ProjectilePredictionModule.Nested0390> field05043 = new ArrayList<>();
   private final Int2ObjectOpenHashMap<ProjectilePredictionModule.Nested0389> field05044 = new Int2ObjectOpenHashMap();
   private final Immediate field05045 = VertexConsumerProvider.immediate(new BufferAllocator(131072));
   private ClientWorld field05046;
   private int field05047 = Integer.MIN_VALUE;
   private long field05048;
   private Vec3d field05049;
   private final EventListener<WorldRenderEvent> field05050 = worldRenderEvent -> {
      if (mc.player != null && mc.world != null) {
         ArrayList<ProjectilePredictionModule.Nested0393> local2352_2 = new ArrayList<>();

         for (Entity entity : mc.world.getEntities()) {
            if (entity instanceof LivingEntity livingEntity && livingEntity != mc.player && livingEntity.isAlive()) {
               local2352_2.add(new ProjectilePredictionModule.Nested0393(livingEntity, livingEntity.getBoundingBox().expand(0.3)));
            }
         }

         ArrayList<ProjectilePredictionModule.Nested0392> local2352_3 = new ArrayList<>();

         for (Hand hand : Hand.values()) {
            this.method1627(mc.player.getStackInHand(hand), hand, worldRenderEvent.method0646(), local2352_2, local2352_3);
         }

         if (mc.world != this.field05046 || mc.player.age != this.field05047) {
            this.method1634(local2352_2);
         }

         this.method1630(local2352_3, worldRenderEvent.method0646());

         for (int index = 0; index < local2352_3.size(); index++) {
            this.field05042[index % this.field05042.length] = this.field05042[index % this.field05042.length]
               + (
                     (local2352_3.get(index).landing().hitTarget() == null && !this.method1638(local2352_3.get(index), local2352_2) ? 0.0F : 1.0F)
                        - this.field05042[index % this.field05042.length]
                  )
                  * 0.3F;
         }

         if (!this.field05044.isEmpty()) {
            DepthSnapshot.method4230();
            Vec3d cameraPos = mc.gameRenderer.getCamera().getCameraPos();
            double local2352_5 = 2.0
               * Math.tan(Math.toRadians(mc.options.getFov().getValue().intValue()) / 2.0)
               / Math.max(1, mc.getWindow().getFramebufferHeight());
            ObjectIterator objectIterator = this.field05044.int2ObjectEntrySet().iterator();

            while (objectIterator.hasNext()) {
               Entry<ProjectilePredictionModule.Nested0389> local2352_8 = (Entry<ProjectilePredictionModule.Nested0389>)objectIterator.next();
               ProjectilePredictionModule.Nested0389 local2352_9 = (ProjectilePredictionModule.Nested0389)local2352_8.getValue();
               if (local2352_8.getIntKey() < 0 || this.method1639(local2352_9)) {
                  int local2352_10 = local2352_8.getIntKey() < 0
                     ? this.method1635(-1 - local2352_8.getIntKey())
                     : (local2352_9.field05212 ? -54742 : ThemeColors.field13669);
                  if (this.method1643(local2352_9, cameraPos, local2352_5, local2352_10)) {
                     method1644(this.field05045.getBuffer(field05041), worldRenderEvent.method0647().peek().getPositionMatrix(), local2352_9.field05215);
                  }
               }
            }

            this.field05045.draw();

            for (int index2 = 0; index2 < local2352_3.size(); index2++) {
               ProjectilePredictionModule.Nested0392 local2352_8 = local2352_3.get(index2);
               ProjectilePredictionModule.Nested0389 local2352_9 = (ProjectilePredictionModule.Nested0389)(Object)this.field05044.get(-1 - index2);
               Vec3d vec3d = local2352_9 != null ? local2352_9.field05209 : local2352_8.landing().pos();
               this.method1636(worldRenderEvent.method0647(), vec3d, this.method1635(index2));
               if (this.booleanSetting.isChecked() && local2352_8.areaRadiusBlocks() > 0.0F) {
                  this.method1641(
                     worldRenderEvent.method0647(), vec3d, local2352_8.areaRadiusBlocks(), Colors.method3727(this.method1635(index2), 0.5F)
                  );
               }
            }

            mc.getBufferBuilders().getEntityVertexConsumers().draw();
            ArrayList<LivingEntity> local2352_7 = new ArrayList<>();

            for (ProjectilePredictionModule.Nested0392 local2352_9 : local2352_3) {
               if (local2352_9.landing().hitTarget() != null && !local2352_7.contains(local2352_9.landing().hitTarget())) {
                  local2352_7.add(local2352_9.landing().hitTarget());
               }
            }

            for (ProjectilePredictionModule.Nested0390 local2352_9 : this.field05043) {
               if (local2352_9.landing().hitTarget() != null && !local2352_7.contains(local2352_9.landing().hitTarget())) {
                  local2352_7.add(local2352_9.landing().hitTarget());
               }
            }

            for (LivingEntity livingEntity2 : local2352_7) {
               if (WorldProjection.method1121(livingEntity2.getBoundingBox())) {
                  this.method1640(worldRenderEvent.method0647(), livingEntity2, worldRenderEvent.method0646());
               }
            }

            mc.getBufferBuilders().getEntityVertexConsumers().draw();
         }
      }
   };
   private String field05051 = "";
   private final EventListener<HudRenderEvent> field05052 = hudRenderEvent -> {
      if (mc.player != null && mc.world != null) {
         HudRenderContext.method4333(hudRenderEvent.method4459());
         float local2371_2 = GuiScale.method0797() / GuiScale.method0794();
         if (AttackAuraModule.method1029().isEnabled() && AttackAuraModule.method1029().method1005() != null) {
            this.field05051 = AttackAuraModule.method1029().method1005().getName().getString();
         }

         ObjectIterator objectIterator2 = this.field05044.int2ObjectEntrySet().iterator();

         while (objectIterator2.hasNext()) {
            Entry<ProjectilePredictionModule.Nested0389> local2371_4 = (Entry<ProjectilePredictionModule.Nested0389>)objectIterator2.next();
            ProjectilePredictionModule.Nested0389 local2371_5 = (ProjectilePredictionModule.Nested0389)local2371_4.getValue();
            if (!local2371_5.field05214.isEmpty()) {
               Vec3d vec3d2 = WorldProjection.method1125(local2371_5.field05209.add(0.0, 0.25, 0.0), hudRenderEvent.method4462(), hudRenderEvent.method4460());
               if (vec3d2 != null) {
                  String local2371_7 = local2371_4.getIntKey() >= 0
                        && mc.world.getEntityById(local2371_4.getIntKey()) instanceof ProjectileEntity projectileEntity
                        && projectileEntity.getOwner() != null
                        && projectileEntity.getOwner() != mc.player
                     ? projectileEntity.getOwner().getName().getString()
                     : "";
                  float local2371_8 = (float)vec3d2.x * local2371_2;
                  float local2371_9 = (float)vec3d2.y * local2371_2;
                  boolean local2371_10 = !local2371_7.isEmpty() && local2371_7.equals(this.field05051);
                  FontFace fontFace = local2371_10 ? FontFace.BOLD : FontFace.SEMI;
                  float local2371_12 = local2371_7.isEmpty() ? 0.0F : FontText.method1396(fontFace, local2371_7, 6.0F) + 3.0F;
                  float local2371_13 = FontText.method1396(FontFace.SEMI, local2371_5.field05211, 6.0F);
                  float local2371_14 = 14.0F + local2371_12 + local2371_13 + 4.0F;
                  float local2371_15 = 11.0F;
                  float local2371_16 = Math.round((local2371_8 - local2371_14 / 2.0F) * 2.0F) / 2.0F;
                  float local2371_17 = Math.round((local2371_9 - local2371_15 / 2.0F) * 2.0F) / 2.0F;
                  hudRenderEvent.method4459().createNewRootLayer();
                  HudRenderContext.method4336(
                     local2371_8,
                     local2371_9,
                     Math.max(0.6F, 1.1F - (float)mc.gameRenderer.getCamera().getCameraPos().distanceTo(local2371_5.field05209) * 0.004F)
                  );
                  ShapeDrawing.method3659(
                     ShapeDrawing.Nested0502.LOW,
                     true,
                     local2371_16,
                     local2371_17,
                     local2371_14,
                     local2371_15,
                     1.0F,
                     CornerRadii.uniform(3.5F),
                     HudStyle.method4818(),
                     0.65F,
                     Colors.method3723(-16777216, ThemeColors.field13669, 0.1F)
                  );
                  ItemRenderCache.method2342(
                     local2371_5.field05214,
                     Math.round((local2371_16 + 3.0F) * 2.0F) / 2.0F,
                     Math.round((local2371_17 + (local2371_15 - 8.0F) / 2.0F) * 2.0F) / 2.0F,
                     8.0F
                  );
                  float local2371_18 = HudStyle.method4816(local2371_17);
                  if (!local2371_7.isEmpty()) {
                     FontText.method1397(
                        fontFace, local2371_7, local2371_16 + 14.0F, local2371_18, 6.0F, local2371_10 ? -42386 : HudStyle.method4820(1.0F)
                     );
                  }

                  FontText.method1397(
                     FontFace.SEMI,
                     local2371_5.field05211,
                     local2371_16 + 14.0F + local2371_12,
                     local2371_18,
                     6.0F,
                     HudStyle.method4815(1.0F)
                  );
                  HudRenderContext.method4337();
               }
            }
         }
      }
   };
   private final ArrayList<Vec3d> field05053 = new ArrayList<>();

   public static ProjectilePredictionModule method1623() {
      if (projectilePredictionModule == null) {
         projectilePredictionModule = new ProjectilePredictionModule();
      }

      return projectilePredictionModule;
   }

   private ProjectilePredictionModule.Nested0394 method1624(
      Vec3d vec3d,
      Vec3d vec3d2,
      double local2347_3,
      double local2347_5,
      double local2347_7,
      boolean local2347_9,
      Entity entity,
      Entity entity2,
      ArrayList<ProjectilePredictionModule.Nested0393> local2347_12
   ) {
      this.field05053.clear();
      this.field05053.add(vec3d);
      Vec3d vec3d3 = vec3d;
      Vec3d vec3d4 = vec3d2;

      for (int index = 0; index < 300; index++) {
         Vec3d vec3d5 = vec3d3;
         double local2347_17 = mc.world.getBlockState(BlockPos.ofFloored(vec3d5)).getFluidState().isIn(FluidTags.WATER)
            ? local2347_7
            : local2347_5;
         if (local2347_9) {
            vec3d4 = vec3d4.subtract(0.0, local2347_3, 0.0).multiply(local2347_17);
         }

         vec3d3 = vec3d3.add(vec3d4);
         if (!local2347_9) {
            vec3d4 = vec3d4.multiply(local2347_17).subtract(0.0, local2347_3, 0.0);
         }

         BlockHitResult blockHitResult = mc.world
            .raycast(new RaycastContext(vec3d5, vec3d3, ShapeType.COLLIDER, FluidHandling.NONE, entity));
         Vec3d vec3d6 = blockHitResult.getType() != Type.MISS ? blockHitResult.getPos() : vec3d3;
         this.field05053.add(vec3d6);

         for (ProjectilePredictionModule.Nested0393 local2347_22 : local2347_12) {
            if (local2347_22.target() != entity2 && local2347_22.box().intersects(vec3d5, vec3d6)) {
               BlockHitResult blockHitResult2 = Box.raycast(List.of(local2347_22.box()), vec3d5, vec3d6, BlockPos.ORIGIN);
               ProjectilePredictionModule.Nested0394 local2347_24 = blockHitResult2 != null
                  ? new ProjectilePredictionModule.Nested0394(blockHitResult2.getPos(), local2347_22.target())
                  : new ProjectilePredictionModule.Nested0394(
                     local2347_22.box().raycast(vec3d5, vec3d6).orElse(vec3d6), local2347_22.target()
                  );
               this.field05053.set(this.field05053.size() - 1, local2347_24.pos());
               return local2347_24;
            }
         }

         if (blockHitResult.getType() != Type.MISS) {
            return new ProjectilePredictionModule.Nested0394(blockHitResult.getPos(), null);
         }

         if (vec3d3.y < -128.0) {
            break;
         }
      }

      return null;
   }

   private boolean method1625(ItemStack itemStack) {
      return itemStack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT).copyNbt().contains("don-item");
   }

   private void method1626(
      int local2349_1, ArrayList<Vec3d> local2349_2, ProjectilePredictionModule.Nested0394 local2349_3, ItemStack itemStack, float local2349_5
   ) {
      ProjectilePredictionModule.Nested0389 local2349_6 = (ProjectilePredictionModule.Nested0389)(Object)this.field05044.get(local2349_1);
      if (local2349_6 == null) {
         local2349_6 = new ProjectilePredictionModule.Nested0389();
         this.field05044.put(local2349_1, local2349_6);
      }

      local2349_6.method1648(local2349_2, local2349_3.pos(), local2349_3.hitTarget() != null, itemStack, local2349_5);
      local2349_6.field05213 = true;
   }

   private void method1627(
      ItemStack itemStack,
      Hand hand,
      float local2350_3,
      ArrayList<ProjectilePredictionModule.Nested0393> local2350_4,
      ArrayList<ProjectilePredictionModule.Nested0392> local2350_5
   ) {
      Item item = itemStack.getItem();
      Vec3d vec3d = new Vec3d(mc.player.getX(), mc.player.getEyeY() - 0.1, mc.player.getZ());
      if (item instanceof BowItem && mc.player.isUsingItem() && mc.player.getActiveItem() == itemStack) {
         float local2350_8 = BowItem.getPullProgress(mc.player.getItemUseTime());
         if (local2350_8 >= 0.1F) {
            this.method1631(
               local2350_5,
               this.method1624(
                  vec3d, this.method1628(0.0F, local2350_8 * 3.0F), 0.05, 0.99F, 0.6F, false, mc.player, mc.player, local2350_4
               ),
               0.0F,
               new ItemStack(Items.ARROW),
               hand,
               local2350_3
            );
         }
      } else if (item instanceof CrossbowItem && CrossbowItem.isCharged(itemStack)) {
         ChargedProjectilesComponent orDefault = itemStack.getOrDefault(DataComponentTypes.CHARGED_PROJECTILES, ChargedProjectilesComponent.DEFAULT);
         List<ItemStack> projectiles = orDefault.getProjectiles();
         float local2350_10 = orDefault.contains(Items.FIREWORK_ROCKET) ? 1.6F : 3.15F;
         float local2350_11 = projectiles.size() <= 1 ? 0.0F : 20.0F / (projectiles.size() - 1);
         Vec3d oppositeRotationVector = mc.player.getOppositeRotationVector(1.0F);

         for (int index = 0; index < projectiles.size(); index++) {
            float local2350_14 = (projectiles.size() - 1) % 2 * local2350_11 / 2.0F
               + (index % 2 == 0 ? 1 : -1) * ((index + 1) / 2) * local2350_11;
            Vector3f vector3f = mc.player
               .getRotationVec(1.0F)
               .toVector3f()
               .rotate(new Quaternionf().setAngleAxis(local2350_14 * (float) (Math.PI / 180.0), oppositeRotationVector.x, oppositeRotationVector.y, oppositeRotationVector.z));
            Vec3d vec3d2 = new Vec3d(vector3f.x(), vector3f.y(), vector3f.z()).normalize().multiply(local2350_10);
            if (projectiles.get(index).isOf(Items.FIREWORK_ROCKET)) {
               this.method1631(
                  local2350_5,
                  this.method1624(
                     new Vec3d(mc.player.getX(), mc.player.getEyeY() - 0.15, mc.player.getZ()),
                     vec3d2,
                     0.0,
                     1.0,
                     1.0,
                     false,
                     mc.player,
                     mc.player,
                     local2350_4
                  ),
                  0.0F,
                  projectiles.get(index),
                  hand,
                  local2350_3
               );
            } else {
               this.method1631(
                  local2350_5,
                  this.method1624(vec3d, vec3d2, 0.05, 0.99F, 0.6F, false, mc.player, mc.player, local2350_4),
                  0.0F,
                  projectiles.get(index),
                  hand,
                  local2350_3
               );
            }
         }
      } else if (!(item instanceof SnowballItem) && !(item instanceof EggItem) && !(item instanceof EnderPearlItem)) {
         if (item instanceof TridentItem
            && mc.player.isUsingItem()
            && mc.player.getActiveItem() == itemStack
            && mc.player.getItemUseTime() >= 10) {
            this.method1631(
               local2350_5,
               this.method1624(vec3d, this.method1628(0.0F, 2.5F), 0.05, 0.99F, 0.99F, false, mc.player, mc.player, local2350_4),
               0.0F,
               itemStack,
               hand,
               local2350_3
            );
         } else if (item instanceof SplashPotionItem || item instanceof LingeringPotionItem) {
            this.method1631(
               local2350_5,
               this.method1624(vec3d, this.method1628(-20.0F, 0.5F), 0.05, 0.99F, 0.8F, true, mc.player, mc.player, local2350_4),
               item instanceof LingeringPotionItem ? 4.0F : 3.0F,
               ItemStack.EMPTY,
               hand,
               local2350_3
            );
         } else if (item instanceof ExperienceBottleItem) {
            this.method1631(
               local2350_5,
               this.method1624(vec3d, this.method1628(-20.0F, 0.7F), 0.07, 0.99F, 0.8F, true, mc.player, mc.player, local2350_4),
               0.0F,
               ItemStack.EMPTY,
               hand,
               local2350_3
            );
         }
      } else {
         this.method1631(
            local2350_5,
            this.method1624(vec3d, this.method1628(0.0F, 1.5F), 0.03, 0.99F, 0.8F, true, mc.player, mc.player, local2350_4),
            item instanceof SnowballItem && this.method1625(itemStack) ? 7.0F : 0.0F,
            item instanceof EnderPearlItem ? itemStack : ItemStack.EMPTY,
            hand,
            local2350_3
         );
      }
   }

   private Vec3d method1628(float local2351_1, float local2351_2) {
      float local2351_3 = mc.player.getPitch() * (float) (Math.PI / 180.0);
      float local2351_4 = mc.player.getYaw() * (float) (Math.PI / 180.0);
      Vec3d vec3d = new Vec3d(
            -MathHelper.sin(local2351_4) * MathHelper.cos(local2351_3),
            -MathHelper.sin((mc.player.getPitch() + local2351_1) * (float) (Math.PI / 180.0)),
            MathHelper.cos(local2351_4) * MathHelper.cos(local2351_3)
         )
         .normalize()
         .multiply(local2351_2);
      Vec3d vec3d2 = mc.player.getEntityPos().subtract(mc.player.lastX, mc.player.lastY, mc.player.lastZ);
      return vec3d.add(vec3d2.x, mc.player.isOnGround() ? 0.0 : vec3d2.y, vec3d2.z);
   }

   private ProjectilePredictionModule() {
      this.booleanSetting = this.addSetting(new BooleanSetting("Радиус действия", "Рисует зону поражения зелий и особых снежков.", true));
      this.colorSetting = this.addSetting(new ColorSetting("Цвет", "Цвет маркера приземления.").withArgb(-12417035));
   }

   private void method1630(ArrayList<ProjectilePredictionModule.Nested0392> local2354_1, float local2354_2) {
      Vec3d lerpedPos = mc.player.getLerpedPos(local2354_2);
      Vec3d vec3d = this.field05049 == null ? Vec3d.ZERO : lerpedPos.subtract(this.field05049);
      this.field05049 = lerpedPos;
      if (vec3d.lengthSquared() < 64.0) {
         ObjectIterator objectIterator = this.field05044.int2ObjectEntrySet().iterator();

         while (objectIterator.hasNext()) {
            Entry<ProjectilePredictionModule.Nested0389> local2354_6 = (Entry<ProjectilePredictionModule.Nested0389>)objectIterator.next();
            if (local2354_6.getIntKey() < 0) {
               ((ProjectilePredictionModule.Nested0389)local2354_6.getValue()).method1647(vec3d);
            }
         }
      }

      float local2354_5 = this.field05048 == 0L ? 0.05F : Math.min(0.1F, (float)(System.nanoTime() - this.field05048) / 1.0E9F);
      this.field05048 = System.nanoTime();
      ObjectIterator objectIterator2 = this.field05044.values().iterator();

      while (objectIterator2.hasNext()) {
         ProjectilePredictionModule.Nested0389 local2354_7 = (ProjectilePredictionModule.Nested0389)objectIterator2.next();
         local2354_7.field05213 = false;
      }

      for (int index = 0; index < local2354_1.size(); index++) {
         this.method1626(
            -1 - index,
            local2354_1.get(index).flightPath(),
            local2354_1.get(index).landing(),
            local2354_1.get(index).icon(),
            local2354_5
         );
      }

      for (ProjectilePredictionModule.Nested0390 local2354_7 : this.field05043) {
         this.method1626(local2354_7.entityId(), local2354_7.path(), local2354_7.landing(), local2354_7.icon(), local2354_5);
      }

      this.field05044.values().removeIf(local2372_0 -> !local2372_0.field05213);
   }

   private void method1631(
      ArrayList<ProjectilePredictionModule.Nested0392> local2355_1,
      ProjectilePredictionModule.Nested0394 local2355_2,
      float local2355_3,
      ItemStack itemStack,
      Hand hand,
      float local2355_6
   ) {
      if (local2355_2 != null) {
         Vec3d vec3d = mc.player.getLerpedPos(local2355_6).subtract(mc.player.getEntityPos());
         ArrayList<Vec3d> local2355_8 = new ArrayList<>(this.field05053.size());

         for (Vec3d vec3d2 : this.field05053) {
            local2355_8.add(vec3d2.add(vec3d));
         }

         local2355_2 = new ProjectilePredictionModule.Nested0394(local2355_2.pos().add(vec3d), local2355_2.hitTarget());
         Vec3d rotationVec = mc.player.getRotationVec(local2355_6);
         Vec3d vec3d3 = Vec3d.fromPolar(0.0F, mc.player.getYaw(local2355_6) + 90.0F).multiply(hand == Hand.OFF_HAND ? -0.32 : 0.32);
         int local2355_11 = Math.min(5, local2355_8.size());

         for (int index = 0; index < local2355_11; index++) {
            float local2355_13 = (1.0F - (float)index / local2355_11) * (1.0F - (float)index / local2355_11);
            local2355_8.set(
               index,
               local2355_8.get(index)
                  .add(
                     (vec3d3.x + rotationVec.x * 0.25) * local2355_13,
                     (vec3d3.y + rotationVec.y * 0.25 - 0.18) * local2355_13,
                     (vec3d3.z + rotationVec.z * 0.25) * local2355_13
                  )
            );
         }

         local2355_1.add(new ProjectilePredictionModule.Nested0392(local2355_2, local2355_3, local2355_8, itemStack));
      }
   }

   private void method1632(VertexConsumer vertexConsumer, MatrixStack matrixStack, float local2356_3, int local2356_4, int local2356_5, float local2356_6) {
      for (int index = 0; index < local2356_4; index++) {
         float local2356_8 = (float)(index * Math.PI * 2.0 / local2356_4);
         float local2356_9 = (float)((index + 1) * Math.PI * 2.0 / local2356_4);
         float local2356_10 = MathHelper.cos(local2356_8) * local2356_3;
         float local2356_11 = MathHelper.sin(local2356_8) * local2356_3;
         float local2356_12 = MathHelper.cos(local2356_9) * local2356_3;
         float local2356_13 = MathHelper.sin(local2356_9) * local2356_3;
         float local2356_14 = (local2356_12 - local2356_10)
            / MathHelper.sqrt((local2356_12 - local2356_10) * (local2356_12 - local2356_10) + (local2356_13 - local2356_11) * (local2356_13 - local2356_11));
         float local2356_15 = (local2356_13 - local2356_11)
            / MathHelper.sqrt((local2356_12 - local2356_10) * (local2356_12 - local2356_10) + (local2356_13 - local2356_11) * (local2356_13 - local2356_11));
         vertexConsumer.vertex(matrixStack.peek(), local2356_10, 0.0F, local2356_11)
            .color(local2356_5)
            .normal(matrixStack.peek(), local2356_14, 0.0F, local2356_15)
            .lineWidth(local2356_6);
         vertexConsumer.vertex(matrixStack.peek(), local2356_12, 0.0F, local2356_13)
            .color(local2356_5)
            .normal(matrixStack.peek(), local2356_14, 0.0F, local2356_15)
            .lineWidth(local2356_6);
      }
   }

   private static int method1633(int local2357_0, float local2357_1) {
      return local2357_0 + (int)((255 - local2357_0) * 0.45F * local2357_1);
   }

   private void method1634(ArrayList<ProjectilePredictionModule.Nested0393> local2358_1) {
      this.field05043.clear();
      this.field05046 = mc.world;
      this.field05047 = mc.player.age;

      for (Entity entity : mc.world.getEntities()) {
         if ((entity instanceof PersistentProjectileEntity || entity instanceof ThrownItemEntity || entity instanceof ItemEntity)
            && !entity.isOnGround()
            && !entity.noClip
            && !(entity.getVelocity().lengthSquared() < 1.0E-4)) {
            ProjectilePredictionModule.Nested0394 local2358_4 = this.method1624(
               entity.getEntityPos(),
               entity.getVelocity(),
               entity.getFinalGravity(),
               0.99F,
               entity instanceof TridentEntity ? 0.99F : (entity instanceof PersistentProjectileEntity ? 0.6F : 0.8F),
               entity instanceof ThrownItemEntity,
               entity,
               entity instanceof ProjectileEntity projectileEntity ? projectileEntity.getOwner() : null,
               local2358_1
            );
            if (local2358_4 != null && this.field05053.size() > 2) {
               ItemStack itemStack = entity instanceof EnderPearlEntity
                  ? new ItemStack(Items.ENDER_PEARL)
                  : (
                     entity instanceof TridentEntity
                        ? new ItemStack(Items.TRIDENT)
                        : (entity instanceof PersistentProjectileEntity ? new ItemStack(Items.ARROW) : ItemStack.EMPTY)
                  );
               this.field05043.add(new ProjectilePredictionModule.Nested0390(entity.getId(), local2358_4, new ArrayList<>(this.field05053), itemStack));
            }
         }
      }
   }

   private int method1635(int local2359_1) {
      return Colors.method3723(this.colorSetting.getValue(), -54742, this.field05042[local2359_1 % this.field05042.length]);
   }

   @Override
   protected void onDisable() {
      this.field05043.clear();
      this.field05044.clear();
      this.field05046 = null;
      this.field05047 = Integer.MIN_VALUE;
      this.field05048 = 0L;
      this.field05049 = null;
   }

   private void method1636(MatrixStack matrixStack, Vec3d vec3d, int local2361_3) {
      Vec3d cameraPos = mc.gameRenderer.getCamera().getCameraPos();
      matrixStack.push();
      matrixStack.translate(vec3d.x - cameraPos.x, vec3d.y - cameraPos.y, vec3d.z - cameraPos.z);
      Matrix4f matrix4f = matrixStack.peek().getPositionMatrix();
      VertexConsumer vertexConsumer = mc.getBufferBuilders().getEntityVertexConsumers().getBuffer(RenderLayers.debugQuads());
      int local2361_7 = local2361_3 & 16777215 | 771751936;

      for (int index = 0; index < 10; index++) {
         float local2361_9 = (float)(index * Math.PI / 10.0);
         float local2361_10 = (float)((index + 1) * Math.PI / 10.0);

         for (int index2 = 0; index2 < 14; index2++) {
            float local2361_12 = (float)(index2 * Math.PI * 2.0 / 14.0);
            float local2361_13 = (float)((index2 + 1) * Math.PI * 2.0 / 14.0);
            method1642(vertexConsumer, matrix4f, local2361_9, local2361_12, local2361_7);
            method1642(vertexConsumer, matrix4f, local2361_10, local2361_12, local2361_7);
            method1642(vertexConsumer, matrix4f, local2361_10, local2361_13, local2361_7);
            method1642(vertexConsumer, matrix4f, local2361_9, local2361_13, local2361_7);
         }
      }

      VertexConsumer vertexConsumer2 = mc.getBufferBuilders().getEntityVertexConsumers().getBuffer(RenderLayers.lines());
      this.method1632(vertexConsumer2, matrixStack, 0.1F, 32, 0xFF000000 | local2361_3, 2.0F);
      matrixStack.push();
      matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90.0F));
      this.method1632(vertexConsumer2, matrixStack, 0.1F, 32, 0xFF000000 | local2361_3, 2.0F);
      matrixStack.pop();
      matrixStack.multiply(mc.gameRenderer.getCamera().getRotation());
      matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90.0F));
      this.method1632(vertexConsumer2, matrixStack, 0.1F, 32, 0xFF000000 | local2361_3, 2.0F);
      matrixStack.pop();
   }

   private static void method1637(
      VertexConsumer vertexConsumer,
      Matrix4f matrix4f,
      double local2362_2,
      double local2362_4,
      double local2362_6,
      int local2362_8,
      int local2362_9,
      int local2362_10,
      int local2362_11
   ) {
      vertexConsumer.vertex(matrix4f, (float)local2362_2, (float)local2362_4, (float)local2362_6).color(local2362_8, local2362_9, local2362_10, local2362_11);
   }

   private boolean method1638(ProjectilePredictionModule.Nested0392 local2363_1, ArrayList<ProjectilePredictionModule.Nested0393> local2363_2) {
      if (local2363_1.areaRadiusBlocks() <= 0.0F) {
         return false;
      }

      for (ProjectilePredictionModule.Nested0393 local2363_4 : local2363_2) {
         if (local2363_4.target().getBoundingBox().getCenter().squaredDistanceTo(local2363_1.landing().pos())
            <= local2363_1.areaRadiusBlocks() * local2363_1.areaRadiusBlocks()) {
            return true;
         }
      }

      return false;
   }

   private boolean method1639(ProjectilePredictionModule.Nested0389 local2364_1) {
      double local2364_2 = Double.MAX_VALUE;
      double local2364_4 = Double.MAX_VALUE;
      double local2364_6 = Double.MAX_VALUE;
      double local2364_8 = -Double.MAX_VALUE;
      double local2364_10 = -Double.MAX_VALUE;
      double local2364_12 = -Double.MAX_VALUE;

      for (Vec3d vec3d : local2364_1.field05208) {
         local2364_2 = Math.min(local2364_2, vec3d.x);
         local2364_4 = Math.min(local2364_4, vec3d.y);
         local2364_6 = Math.min(local2364_6, vec3d.z);
         local2364_8 = Math.max(local2364_8, vec3d.x);
         local2364_10 = Math.max(local2364_10, vec3d.y);
         local2364_12 = Math.max(local2364_12, vec3d.z);
      }

      return local2364_1.field05208.isEmpty()
         || WorldProjection.method1121(new Box(local2364_2, local2364_4, local2364_6, local2364_8, local2364_10, local2364_12).expand(0.5));
   }

   private void method1640(MatrixStack matrixStack, LivingEntity livingEntity, float local2365_3) {
      Box boundingBox = livingEntity.getBoundingBox();
      Vec3d vec3d = livingEntity.getLerpedPos(local2365_3)
         .subtract(livingEntity.getEntityPos())
         .subtract(mc.gameRenderer.getCamera().getCameraPos());
      matrixStack.push();
      matrixStack.translate(boundingBox.minX + vec3d.x, boundingBox.minY + vec3d.y, boundingBox.minZ + vec3d.z);
      Matrix4f positionMatrix = matrixStack.peek().getPositionMatrix();
      VertexConsumer vertexConsumer = mc.getBufferBuilders().getEntityVertexConsumers().getBuffer(RenderLayers.debugQuads());
      float[] floatArray = new float[]{0.0F, (float)boundingBox.getLengthX()};
      float[] floatArray2 = new float[]{0.0F, (float)boundingBox.getLengthY()};
      float[] floatArray3 = new float[]{0.0F, (float)boundingBox.getLengthZ()};
      int[][] intArrayArray = new int[][]{{0, 1, 3, 2}, {4, 6, 7, 5}, {0, 4, 5, 1}, {2, 3, 7, 6}, {0, 2, 6, 4}, {1, 5, 7, 3}};

      for (int[] intArray : intArrayArray) {
         for (int local2365_19 : intArray) {
            vertexConsumer.vertex(positionMatrix, floatArray[local2365_19 >> 2], floatArray2[local2365_19 >> 1 & 1], floatArray3[local2365_19 & 1])
               .color(788486766);
         }
      }

      VertexConsumer vertexConsumer2 = mc.getBufferBuilders().getEntityVertexConsumers().getBuffer(RenderLayers.lines());
      VertexRendering.drawOutline(
         matrixStack,
         vertexConsumer2,
         VoxelShapes.cuboid(new Box(0.0, 0.0, 0.0, boundingBox.getLengthX(), boundingBox.getLengthY(), boundingBox.getLengthZ())),
         0.0,
         0.0,
         0.0,
         -42386,
         2.5F
      );
      matrixStack.pop();
   }

   private void method1641(MatrixStack matrixStack, Vec3d vec3d, float local2366_3, int local2366_4) {
      matrixStack.push();
      matrixStack.translate(
         vec3d.x - mc.gameRenderer.getCamera().getCameraPos().x,
         vec3d.y - mc.gameRenderer.getCamera().getCameraPos().y + 0.01,
         vec3d.z - mc.gameRenderer.getCamera().getCameraPos().z
      );
      this.method1632(
         mc.getBufferBuilders().getEntityVertexConsumers().getBuffer(RenderLayers.lines()), matrixStack, local2366_3, 64, local2366_4, 1.5F
      );
      matrixStack.pop();
   }

   private static void method1642(VertexConsumer vertexConsumer, Matrix4f matrix4f, float local2367_2, float local2367_3, int local2367_4) {
      vertexConsumer.vertex(
            matrix4f,
            MathHelper.sin(local2367_2) * MathHelper.cos(local2367_3) * 0.1F,
            MathHelper.cos(local2367_2) * 0.1F,
            MathHelper.sin(local2367_2) * MathHelper.sin(local2367_3) * 0.1F
         )
         .color(local2367_4);
   }

   private boolean method1643(ProjectilePredictionModule.Nested0389 local2369_1, Vec3d vec3d, double local2369_3, int local2369_5) {
      int local2369_6 = 0;

      while (local2369_1.field05208.size() - local2369_6 > 2 && local2369_1.field05208.get(local2369_6).squaredDistanceTo(vec3d) < 2.25) {
         local2369_6++;
      }

      if (local2369_1.field05208.size() - local2369_6 < 2) {
         return false;
      }

      ProjectilePredictionModule.Nested0391 local2369_7 = local2369_1.field05215;
      local2369_7.method1651(local2369_1.field05208.size() - local2369_6);
      local2369_7.field05238 = local2369_1.field05208.size() - local2369_6;
      local2369_7.field05239 = local2369_5;

      for (int index = 0; index < local2369_7.field05238; index++) {
         local2369_7.field05230[index] = local2369_1.field05208.get(local2369_6 + index).x - vec3d.x;
         local2369_7.field05231[index] = local2369_1.field05208.get(local2369_6 + index).y - vec3d.y;
         local2369_7.field05232[index] = local2369_1.field05208.get(local2369_6 + index).z - vec3d.z;
      }

      double local2369_8 = 0.0;
      double local2369_10 = 0.0;
      double local2369_12 = 1.0;
      double local2369_14 = 0.0;

      for (int index2 = 0; index2 < local2369_7.field05238; index2++) {
         double local2369_17 = local2369_7.field05230[Math.min(index2 + 1, local2369_7.field05238 - 1)]
            - local2369_7.field05230[Math.max(index2 - 1, 0)];
         double local2369_19 = local2369_7.field05231[Math.min(index2 + 1, local2369_7.field05238 - 1)]
            - local2369_7.field05231[Math.max(index2 - 1, 0)];
         double local2369_21 = local2369_7.field05232[Math.min(index2 + 1, local2369_7.field05238 - 1)]
            - local2369_7.field05232[Math.max(index2 - 1, 0)];
         double local2369_23 = local2369_19 * local2369_7.field05232[index2] - local2369_21 * local2369_7.field05231[index2];
         double local2369_25 = local2369_21 * local2369_7.field05230[index2] - local2369_17 * local2369_7.field05232[index2];
         double local2369_27 = local2369_17 * local2369_7.field05231[index2] - local2369_19 * local2369_7.field05230[index2];
         if (local2369_23 * local2369_23 + local2369_25 * local2369_25 + local2369_27 * local2369_27 < 1.0E-8) {
            local2369_23 = local2369_10;
            local2369_25 = local2369_12;
            local2369_27 = local2369_14;
         } else {
            double local2369_29 = 1.0 / Math.sqrt(local2369_23 * local2369_23 + local2369_25 * local2369_25 + local2369_27 * local2369_27);
            local2369_23 *= local2369_29;
            local2369_25 *= local2369_29;
            local2369_27 *= local2369_29;
         }

         local2369_10 = local2369_23;
         local2369_7.field05233[index2] = local2369_23;
         local2369_12 = local2369_25;
         local2369_7.field05234[index2] = local2369_25;
         local2369_14 = local2369_27;
         local2369_7.field05235[index2] = local2369_27;
         local2369_7.field05237[index2] = (float)(
            Math.sqrt(
                  local2369_7.field05230[index2] * local2369_7.field05230[index2]
                     + local2369_7.field05231[index2] * local2369_7.field05231[index2]
                     + local2369_7.field05232[index2] * local2369_7.field05232[index2]
               )
               * local2369_3
         );
         if (index2 > 0) {
            local2369_8 += local2369_1.field05208.get(local2369_6 + index2).distanceTo(local2369_1.field05208.get(local2369_6 + index2 - 1));
         }

         local2369_7.field05236[index2] = (float)MathHelper.clamp((local2369_8 - 0.1) / 0.55, 0.0, 1.0);
      }

      return true;
   }

   private static void method1644(VertexConsumer vertexConsumer, Matrix4f matrix4f, ProjectilePredictionModule.Nested0391 local2370_2) {
      int local2370_3 = local2370_2.field05239 >> 16 & 0xFF;
      int local2370_4 = local2370_2.field05239 >> 8 & 0xFF;
      int local2370_5 = local2370_2.field05239 & 0xFF;

      for (int index = 0; index < local2370_2.field05238 - 1; index++) {
         int local2370_7 = (int)(local2370_2.field05236[index] * 255.0F);
         int local2370_8 = (int)(local2370_2.field05236[index + 1] * 255.0F);
         if (local2370_7 > 0 || local2370_8 > 0) {
            float local2370_9 = (float)index / (local2370_2.field05238 - 1);
            float local2370_10 = (float)(index + 1) / (local2370_2.field05238 - 1);
            int local2370_11 = method1633(local2370_3, local2370_9);
            int local2370_12 = method1633(local2370_4, local2370_9);
            int local2370_13 = method1633(local2370_5, local2370_9);
            int local2370_14 = method1633(local2370_3, local2370_10);
            int local2370_15 = method1633(local2370_4, local2370_10);
            int local2370_16 = method1633(local2370_5, local2370_10);
            double local2370_17 = local2370_2.field05233[index] * local2370_2.field05237[index];
            double local2370_19 = local2370_2.field05234[index] * local2370_2.field05237[index];
            double local2370_21 = local2370_2.field05235[index] * local2370_2.field05237[index];
            double local2370_23 = local2370_2.field05233[index + 1] * local2370_2.field05237[index + 1];
            double local2370_25 = local2370_2.field05234[index + 1] * local2370_2.field05237[index + 1];
            double local2370_27 = local2370_2.field05235[index + 1] * local2370_2.field05237[index + 1];
            method1637(
               vertexConsumer,
               matrix4f,
               local2370_2.field05230[index] - local2370_17,
               local2370_2.field05231[index] - local2370_19,
               local2370_2.field05232[index] - local2370_21,
               local2370_11,
               local2370_12,
               local2370_13,
               local2370_7
            );
            method1637(
               vertexConsumer,
               matrix4f,
               local2370_2.field05230[index + 1] - local2370_23,
               local2370_2.field05231[index + 1] - local2370_25,
               local2370_2.field05232[index + 1] - local2370_27,
               local2370_14,
               local2370_15,
               local2370_16,
               local2370_8
            );
            method1637(
               vertexConsumer,
               matrix4f,
               local2370_2.field05230[index + 1] + local2370_23,
               local2370_2.field05231[index + 1] + local2370_25,
               local2370_2.field05232[index + 1] + local2370_27,
               local2370_14,
               local2370_15,
               local2370_16,
               local2370_8
            );
            method1637(
               vertexConsumer,
               matrix4f,
               local2370_2.field05230[index] + local2370_17,
               local2370_2.field05231[index] + local2370_19,
               local2370_2.field05232[index] + local2370_21,
               local2370_11,
               local2370_12,
               local2370_13,
               local2370_7
            );
            method1637(
               vertexConsumer,
               matrix4f,
               local2370_2.field05230[index] + local2370_17,
               local2370_2.field05231[index] + local2370_19,
               local2370_2.field05232[index] + local2370_21,
               local2370_11,
               local2370_12,
               local2370_13,
               local2370_7
            );
            method1637(
               vertexConsumer,
               matrix4f,
               local2370_2.field05230[index + 1] + local2370_23,
               local2370_2.field05231[index + 1] + local2370_25,
               local2370_2.field05232[index + 1] + local2370_27,
               local2370_14,
               local2370_15,
               local2370_16,
               local2370_8
            );
            method1637(
               vertexConsumer,
               matrix4f,
               local2370_2.field05230[index + 1] + local2370_23 * 2.5,
               local2370_2.field05231[index + 1] + local2370_25 * 2.5,
               local2370_2.field05232[index + 1] + local2370_27 * 2.5,
               local2370_14,
               local2370_15,
               local2370_16,
               0
            );
            method1637(
               vertexConsumer,
               matrix4f,
               local2370_2.field05230[index] + local2370_17 * 2.5,
               local2370_2.field05231[index] + local2370_19 * 2.5,
               local2370_2.field05232[index] + local2370_21 * 2.5,
               local2370_11,
               local2370_12,
               local2370_13,
               0
            );
            method1637(
               vertexConsumer,
               matrix4f,
               local2370_2.field05230[index] - local2370_17,
               local2370_2.field05231[index] - local2370_19,
               local2370_2.field05232[index] - local2370_21,
               local2370_11,
               local2370_12,
               local2370_13,
               local2370_7
            );
            method1637(
               vertexConsumer,
               matrix4f,
               local2370_2.field05230[index + 1] - local2370_23,
               local2370_2.field05231[index + 1] - local2370_25,
               local2370_2.field05232[index + 1] - local2370_27,
               local2370_14,
               local2370_15,
               local2370_16,
               local2370_8
            );
            method1637(
               vertexConsumer,
               matrix4f,
               local2370_2.field05230[index + 1] - local2370_23 * 2.5,
               local2370_2.field05231[index + 1] - local2370_25 * 2.5,
               local2370_2.field05232[index + 1] - local2370_27 * 2.5,
               local2370_14,
               local2370_15,
               local2370_16,
               0
            );
            method1637(
               vertexConsumer,
               matrix4f,
               local2370_2.field05230[index] - local2370_17 * 2.5,
               local2370_2.field05231[index] - local2370_19 * 2.5,
               local2370_2.field05232[index] - local2370_21 * 2.5,
               local2370_11,
               local2370_12,
               local2370_13,
               0
            );
         }
      }
   }

   private static final class Nested0389 {
      ArrayList<Vec3d> field05208 = new ArrayList<>();
      Vec3d field05209 = Vec3d.ZERO;
      int field05210 = -1;
      String field05211 = "0.0с";
      boolean field05212;
      boolean field05213;
      ItemStack field05214 = ItemStack.EMPTY;
      ProjectilePredictionModule.Nested0391 field05215 = new ProjectilePredictionModule.Nested0391();
      double[] field05216 = new double[0];
      double[] field05217 = new double[0];
      double[] field05218 = new double[0];
      double[] field05219 = new double[0];
      double[] field05220 = new double[0];

      void method1647(Vec3d vec3d) {
         this.field05208.replaceAll(vec3d2 -> vec3d2.add(vec3d));
         this.field05209 = this.field05209.add(vec3d);
      }

      void method1648(ArrayList<Vec3d> local2374_1, Vec3d vec3d, boolean local2374_3, ItemStack itemStack, float local2374_5) {
         float local2374_6 = 1.0F - (float)Math.exp(-local2374_5 * 100.0F);
         if (!this.field05208.isEmpty() && !(this.field05209.squaredDistanceTo(vec3d) > 4096.0)) {
            this.field05209 = this.field05209.lerp(vec3d, local2374_6);
            this.method1650(local2374_1, local2374_6);
         } else {
            this.field05208 = new ArrayList<>(local2374_1);
            this.field05209 = vec3d;
         }

         if (this.field05210 != local2374_1.size() - 1) {
            this.field05210 = local2374_1.size() - 1;
            this.field05211 = Math.max(0, Math.round(this.field05210 / 2.0F)) / 10 + "." + Math.max(0, Math.round(this.field05210 / 2.0F)) % 10 + "с";
         }

         this.field05212 = local2374_3;
         this.field05214 = itemStack;
      }

      private void method1650(ArrayList<Vec3d> local2376_1, float local2376_2) {
         if (local2376_1.isEmpty()) {
            this.field05208.clear();
         } else if (Math.max(this.field05208.size(), local2376_1.size()) <= 1) {
            this.field05208 = new ArrayList<>(local2376_1);
         } else if (this.field05208.size() == local2376_1.size()) {
            for (int index = 0; index < this.field05208.size(); index++) {
               this.field05208
                  .set(
                     index,
                     this.field05208
                        .get(index)
                        .lerp(local2376_1.get(index), local2376_2 * (0.15F + 0.85F * index / (this.field05208.size() - 1)))
                  );
            }
         } else {
            int local2376_3 = Math.max(this.field05208.size(), local2376_1.size());
            if (this.field05218.length < local2376_3) {
               this.field05216 = new double[Math.max(local2376_3, this.field05218.length == 0 ? 64 : this.field05218.length * 2)];
               this.field05217 = new double[this.field05216.length];
               this.field05218 = new double[this.field05216.length];
               this.field05219 = new double[this.field05216.length];
               this.field05220 = new double[this.field05216.length];
            }

            this.field05216[0] = 0.0;

            for (int index2 = 1; index2 < this.field05208.size(); index2++) {
               this.field05216[index2] = this.field05216[index2 - 1]
                  + this.field05208.get(index2 - 1).distanceTo(this.field05208.get(index2));
            }

            this.field05217[0] = 0.0;

            for (int index3 = 1; index3 < local2376_1.size(); index3++) {
               this.field05217[index3] = this.field05217[index3 - 1] + local2376_1.get(index3 - 1).distanceTo(local2376_1.get(index3));
            }

            int local2376_4 = 0;

            for (int index4 = 0; index4 < local2376_3; index4++) {
               double local2376_6 = this.field05216[this.field05208.size() - 1] * index4 / (local2376_3 - 1);

               while (local2376_4 + 1 < this.field05208.size() && this.field05216[local2376_4 + 1] < local2376_6) {
                  local2376_4++;
               }

               if (local2376_4 + 1 >= this.field05208.size()) {
                  this.field05218[index4] = this.field05208.get(local2376_4).x;
                  this.field05219[index4] = this.field05208.get(local2376_4).y;
                  this.field05220[index4] = this.field05208.get(local2376_4).z;
               } else {
                  double local2376_8 = this.field05216[local2376_4 + 1] - this.field05216[local2376_4];
                  double local2376_10 = local2376_8 <= 1.0E-8 ? 0.0 : (local2376_6 - this.field05216[local2376_4]) / local2376_8;
                  this.field05218[index4] = MathHelper.lerp(local2376_10, this.field05208.get(local2376_4).x, this.field05208.get(local2376_4 + 1).x);
                  this.field05219[index4] = MathHelper.lerp(local2376_10, this.field05208.get(local2376_4).y, this.field05208.get(local2376_4 + 1).y);
                  this.field05220[index4] = MathHelper.lerp(local2376_10, this.field05208.get(local2376_4).z, this.field05208.get(local2376_4 + 1).z);
               }
            }

            int local2376_5 = 0;

            for (int index5 = 0; index5 < local2376_3; index5++) {
               double local2376_7 = this.field05217[local2376_1.size() - 1] * index5 / (local2376_3 - 1);

               while (local2376_5 + 1 < local2376_1.size() && this.field05217[local2376_5 + 1] < local2376_7) {
                  local2376_5++;
               }

               double local2376_9 = local2376_1.get(local2376_5).x;
               double local2376_11 = local2376_1.get(local2376_5).y;
               double local2376_13 = local2376_1.get(local2376_5).z;
               if (local2376_5 + 1 < local2376_1.size()) {
                  double local2376_15 = this.field05217[local2376_5 + 1] - this.field05217[local2376_5];
                  double local2376_17 = local2376_15 <= 1.0E-8 ? 0.0 : (local2376_7 - this.field05217[local2376_5]) / local2376_15;
                  local2376_9 = MathHelper.lerp(local2376_17, local2376_1.get(local2376_5).x, local2376_1.get(local2376_5 + 1).x);
                  local2376_11 = MathHelper.lerp(local2376_17, local2376_1.get(local2376_5).y, local2376_1.get(local2376_5 + 1).y);
                  local2376_13 = MathHelper.lerp(local2376_17, local2376_1.get(local2376_5).z, local2376_1.get(local2376_5 + 1).z);
               }

               float local2376_15 = local2376_2 * (0.15F + 0.85F * index5 / (local2376_3 - 1));
               Vec3d vec3d = new Vec3d(
                  MathHelper.lerp(local2376_15, this.field05218[index5], local2376_9),
                  MathHelper.lerp(local2376_15, this.field05219[index5], local2376_11),
                  MathHelper.lerp(local2376_15, this.field05220[index5], local2376_13)
               );
               if (index5 < this.field05208.size()) {
                  this.field05208.set(index5, vec3d);
               } else {
                  this.field05208.add(vec3d);
               }
            }

            while (this.field05208.size() > local2376_3) {
               this.field05208.removeLast();
            }
         }
      }
   }

   private record Nested0390(int entityId, ProjectilePredictionModule.Nested0394 landing, ArrayList<Vec3d> path, ItemStack icon) {
   }

   private static final class Nested0391 {
      double[] field05230 = new double[0];
      double[] field05231 = new double[0];
      double[] field05232 = new double[0];
      double[] field05233 = new double[0];
      double[] field05234 = new double[0];
      double[] field05235 = new double[0];
      float[] field05236 = new float[0];
      float[] field05237 = new float[0];
      int field05238;
      int field05239;

      void method1651(int local2386_1) {
         if (this.field05230.length < local2386_1) {
            int local2386_2 = Math.max(local2386_1, this.field05230.length == 0 ? 32 : this.field05230.length * 2);
            this.field05230 = new double[local2386_2];
            this.field05231 = new double[local2386_2];
            this.field05232 = new double[local2386_2];
            this.field05233 = new double[local2386_2];
            this.field05234 = new double[local2386_2];
            this.field05235 = new double[local2386_2];
            this.field05236 = new float[local2386_2];
            this.field05237 = new float[local2386_2];
         }
      }
   }

   private record Nested0392(ProjectilePredictionModule.Nested0394 landing, float areaRadiusBlocks, ArrayList<Vec3d> flightPath, ItemStack icon) {
   }

   private record Nested0393(LivingEntity target, Box box) {
   }

   private record Nested0394(Vec3d pos, LivingEntity hitTarget) {
   }
}
