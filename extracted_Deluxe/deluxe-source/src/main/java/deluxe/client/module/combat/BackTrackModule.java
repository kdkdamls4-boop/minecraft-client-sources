package deluxe.client.module.combat;

import deluxe.client.setting.BooleanSetting;
import deluxe.client.util.Colors;
import deluxe.mixin.EntityS2CPacketMixin;
import deluxe.client.event.EventListener;
import deluxe.client.setting.LegacyNumberSetting;
import deluxe.client.module.Module;
import deluxe.client.module.ModuleCategory;
import deluxe.client.module.ModuleInfo;
import deluxe.client.event.PacketEvent;
import deluxe.client.event.PlayerTickEvent;
import deluxe.client.render.ThemeColors;
import deluxe.client.util.Stopwatch;
import deluxe.client.event.WorldRenderEvent;

import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexRendering;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.EntityPositionS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityPositionSyncS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShapes;
import org.joml.Matrix4f;

@ModuleInfo(name = "BackTrack", description = "Замораживает цель на прошлой позиции, реальную показывает боксом.", category = ModuleCategory.COMBAT)
public final class BackTrackModule extends Module {
   private static BackTrackModule backTrackModule;
   public LegacyNumberSetting field09876;
   public LegacyNumberSetting field09877;
   public BooleanSetting booleanSetting;
   private volatile int field09879 = -1;
   private volatile Vec3d field09880;
   private final CopyOnWriteArrayList<BackTrackModule.Nested0459> field09881 = new CopyOnWriteArrayList<>();
   private final Stopwatch stopwatch = new Stopwatch();
   private Box field09883;
   private float field09884;
   private long field09885;
   private final EventListener<PacketEvent> field09886 = packetEvent -> {
      if (!packetEvent.isOutgoing()) {
         if (packetEvent.getPacket() instanceof PlayerPositionLookS2CPacket) {
            this.field09879 = -1;
            this.field09880 = null;
         } else if (this.field09879 != -1) {
            Packet<?> packet = packetEvent.getPacket();
            if (packet instanceof EntityS2CPacket entityS2CPacket && ((EntityS2CPacketMixin)entityS2CPacket).entityId() == this.field09879) {
               if (entityS2CPacket.isPositionChanged() && this.field09880 != null) {
                  this.field09880 = this.field09880.add(entityS2CPacket.getDeltaX() / 4096.0, entityS2CPacket.getDeltaY() / 4096.0, entityS2CPacket.getDeltaZ() / 4096.0);
               }
            } else if (packet instanceof EntityPositionSyncS2CPacket entityPositionSyncS2CPacket && entityPositionSyncS2CPacket.id() == this.field09879) {
               this.field09880 = entityPositionSyncS2CPacket.values().position();
            } else if (packet instanceof EntityPositionS2CPacket entityPositionS2CPacket && entityPositionS2CPacket.entityId() == this.field09879) {
               if (entityPositionS2CPacket.relatives().isEmpty()) {
                  this.field09880 = entityPositionS2CPacket.change().position();
               }
            } else if (!(packet instanceof EntityVelocityUpdateS2CPacket entityVelocityUpdateS2CPacket) || entityVelocityUpdateS2CPacket.getEntityId() != this.field09879) {
               return;
            }

            this.field09881.add(new BackTrackModule.Nested0459(packet, System.currentTimeMillis()));
            packetEvent.method4423();
         }
      }
   };
   private final EventListener<PlayerTickEvent> field09887 = playerTickEvent -> {
      Entity entity = this.field09879 != -1 && mc.world != null ? mc.world.getEntityById(this.field09879) : null;
      boolean local4551_3 = entity != null
         && this.field09880 != null
         && mc.player != null
         && this.field09880.squaredDistanceTo(mc.player.getEntityPos())
            < entity.getEntityPos().squaredDistanceTo(mc.player.getEntityPos());
      this.method2903(entity != null && !local4551_3 ? (long)this.field09876.getValue() : 0L);
      if (this.field09879 != -1) {
         if (entity == null
            || !entity.isAlive()
            || mc.player == null
            || this.stopwatch.hasExceededMillis(1500L)
            || this.field09880 != null && this.field09880.distanceTo(entity.getEntityPos()) > this.field09877.getValue()) {
            this.method2906();
         }
      }
   };
   private final EventListener<WorldRenderEvent> field09888 = worldRenderEvent -> {
      if (mc.player != null && mc.world != null && this.booleanSetting.isChecked()) {
         float local4552_2 = Math.max(1.0F, Math.min((float)(System.currentTimeMillis() - this.field09885), 100.0F)) / 16.666666F;
         this.field09885 = System.currentTimeMillis();
         Entity entity2 = this.field09879 == -1 ? null : mc.world.getEntityById(this.field09879);
         Vec3d vec3d = this.field09880;
         Box box = entity2 != null && vec3d != null && !(vec3d.distanceTo(entity2.getEntityPos()) < 0.3)
            ? entity2.getBoundingBox().offset(vec3d.subtract(entity2.getEntityPos()))
            : null;
         if (box != null) {
            this.field09883 = this.field09883 != null && !(this.field09884 < 0.003921569F)
               ? new Box(
                  MathHelper.lerp(Math.min(1.0F, 0.45F * local4552_2), this.field09883.minX, box.minX),
                  MathHelper.lerp(Math.min(1.0F, 0.45F * local4552_2), this.field09883.minY, box.minY),
                  MathHelper.lerp(Math.min(1.0F, 0.45F * local4552_2), this.field09883.minZ, box.minZ),
                  MathHelper.lerp(Math.min(1.0F, 0.45F * local4552_2), this.field09883.maxX, box.maxX),
                  MathHelper.lerp(Math.min(1.0F, 0.45F * local4552_2), this.field09883.maxY, box.maxY),
                  MathHelper.lerp(Math.min(1.0F, 0.45F * local4552_2), this.field09883.maxZ, box.maxZ)
               )
               : box;
         }

         this.field09884 = MathHelper.clamp(this.field09884 + (box == null ? -0.25F : 0.25F) * local4552_2, 0.0F, 1.0F);
         if (this.field09883 != null && !(this.field09884 < 0.003921569F)) {
            worldRenderEvent.method0647().push();
            worldRenderEvent.method0647()
               .translate(
                  this.field09883.minX - mc.gameRenderer.getCamera().getCameraPos().x,
                  this.field09883.minY - mc.gameRenderer.getCamera().getCameraPos().y,
                  this.field09883.minZ - mc.gameRenderer.getCamera().getCameraPos().z
               );
            Matrix4f positionMatrix = worldRenderEvent.method0647().peek().getPositionMatrix();
            VertexConsumer vertexConsumer = mc.getBufferBuilders().getEntityVertexConsumers().getBuffer(RenderLayers.debugQuads());
            float[] floatArray = new float[]{0.0F, (float)(this.field09883.maxX - this.field09883.minX)};
            float[] floatArray2 = new float[]{0.0F, (float)(this.field09883.maxY - this.field09883.minY)};
            float[] floatArray3 = new float[]{0.0F, (float)(this.field09883.maxZ - this.field09883.minZ)};
            int[][] intArrayArray = new int[][]{{0, 1, 3, 2}, {4, 6, 7, 5}, {0, 4, 5, 1}, {2, 3, 7, 6}, {0, 2, 6, 4}, {1, 5, 7, 3}};

            for (int[] intArray : intArrayArray) {
               for (int local4552_19 : intArray) {
                  vertexConsumer.vertex(positionMatrix, floatArray[local4552_19 >> 2], floatArray2[local4552_19 >> 1 & 1], floatArray3[local4552_19 & 1])
                     .color(Colors.method3727(ThemeColors.field13669, 0.14F * this.field09884));
               }
            }

            VertexRendering.drawOutline(
               worldRenderEvent.method0647(),
               mc.getBufferBuilders().getEntityVertexConsumers().getBuffer(RenderLayers.lines()),
               VoxelShapes.cuboid(new Box(0.0, 0.0, 0.0, floatArray[1], floatArray2[1], floatArray3[1])),
               0.0,
               0.0,
               0.0,
               Colors.method3727(ThemeColors.field13669, 0.85F * this.field09884),
               2.0F
            );
            mc.getBufferBuilders().getEntityVertexConsumers().draw();
            worldRenderEvent.method0647().pop();
         }
      } else {
         this.field09883 = null;
         this.field09884 = 0.0F;
      }
   };

   private void method2903(long local4550_1) {
      for (BackTrackModule.Nested0459 local4550_4 : this.field09881) {
         if (System.currentTimeMillis() - local4550_4.field09921 < local4550_1) {
            break;
         }

         if (mc.getNetworkHandler() != null) {
            ((Packet<ClientPlayNetworkHandler>)local4550_4.field09920).apply(mc.getNetworkHandler());
         }

         this.field09881.remove(local4550_4);
      }
   }

   @Override
   protected void onDisable() {
      this.method2906();
      this.field09883 = null;
      this.field09884 = 0.0F;
   }

   private void method2906() {
      this.method2903(0L);
      this.field09879 = -1;
      this.field09880 = null;
   }

   private BackTrackModule() {
      this.field09876 = this.addSetting(new LegacyNumberSetting("Задержка", "", 10.0, 10.0, 600.0, 10.0));
      this.field09877 = this.addSetting(new LegacyNumberSetting("Максимальный отрыв", "", 3.7, 0.5, 6.0, 0.1));
      this.booleanSetting = this.addSetting(new BooleanSetting("Показывать бокс", "Рисует реальную позицию цели боксом.", true));
   }

   public void method2908(Entity entity) {
      if (this.isEnabled() && entity instanceof LivingEntity && entity != mc.player) {
         if (entity.getId() != this.field09879) {
            this.method2906();
            this.field09879 = entity.getId();
            this.field09880 = entity.getTrackedPosition().getPos();
         }

         this.stopwatch.reset();
      }
   }

   public static BackTrackModule method2909() {
      if (backTrackModule == null) {
         backTrackModule = new BackTrackModule();
      }

      return backTrackModule;
   }

   private static final class Nested0459 {
      private final Packet<?> field09920;
      private final long field09921;

         public Nested0459(Packet<?> local4559_1, long local4559_2) {
         this.field09920 = local4559_1;
         this.field09921 = local4559_2;
      }
   }
}
