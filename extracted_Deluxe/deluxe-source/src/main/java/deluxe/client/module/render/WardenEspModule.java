package deluxe.client.module.render;

import deluxe.client.event.EventListener;
import deluxe.client.module.Module;
import deluxe.client.module.ModuleCategory;
import deluxe.client.module.ModuleInfo;
import deluxe.client.event.PacketEvent;
import deluxe.client.event.PlayerTickEvent;
import deluxe.client.render.WorldProjection;

import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.decoration.DisplayEntity.TextDisplayEntity;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;

@ModuleInfo(name = "WardenEsp", description = "Показывает таймер над сундуком вардена через стены.", category = ModuleCategory.RENDER)
public final class WardenEspModule extends Module {
   private static WardenEspModule wardenEspModule;
   private static final Pattern field06932 = Pattern.compile("(?:\\d{1,2}:\\d{2}|\\d+\\s*(?:сек|с|s|мин|m)\\b)");
   private final IntOpenHashSet field06933 = new IntOpenHashSet();
   private final HashMap<BlockPos, Long> field06934 = new HashMap<>();
   private final EventListener<PacketEvent> field06935 = packetEvent -> {
      if (packetEvent.isIncoming()
         && packetEvent.getPacket() instanceof PlaySoundS2CPacket playSoundS2CPacket
         && playSoundS2CPacket.getSound().value().id().getPath().startsWith("entity.warden")) {
         mc.execute(() -> this.method1966(new Vec3d(playSoundS2CPacket.getX(), playSoundS2CPacket.getY(), playSoundS2CPacket.getZ())));
      }
   };
   private final EventListener<PlayerTickEvent> field06936 = playerTickEvent -> {
      this.field06933.clear();
      if (mc.player != null && mc.world != null) {
         long local3094_2 = System.currentTimeMillis();
         this.field06934
            .entrySet()
            .removeIf(
               local3083_3 -> local3094_2 - local3083_3.getValue() > 30000L
                  || !(mc.world.getBlockState(local3083_3.getKey()).getBlock() instanceof ChestBlock)
                  || this.method1968(local3083_3.getKey())
            );

         for (Entity entity : mc.world.getEntities()) {
            if (this.method1962(entity) != null && this.method1958(entity)) {
               this.field06933.add(entity.getId());
            }
         }
      } else {
         this.field06934.clear();
      }
   };

   private WardenEspModule() {
   }

   private void method1956(DrawContext drawContext, Vec3d vec3d, Text text) {
      Vec3d vec3d2 = WorldProjection.method1125(vec3d, drawContext.getScaledWindowWidth(), drawContext.getScaledWindowHeight());
      if (vec3d2 != null) {
         float local3081_5 = Math.max(0.3F, 1.0F - (float)mc.gameRenderer.getCamera().getCameraPos().distanceTo(vec3d) * 0.008F);
         List<OrderedText> local3081_6 = mc.textRenderer.wrapLines(text, 200);
         int local3081_7 = 9 + 1;
         drawContext.getMatrices().pushMatrix();
         drawContext.getMatrices().translate((float)vec3d2.x, (float)vec3d2.y);
         drawContext.getMatrices().scale(local3081_5, local3081_5);
         int local3081_8 = -local3081_6.size() * local3081_7;

         for (int index = 0; index < local3081_6.size(); index++) {
            int local3081_10 = mc.textRenderer.getWidth(local3081_6.get(index));
            int local3081_11 = local3081_8 + index * local3081_7;
            drawContext.fill(-local3081_10 / 2 - 1, local3081_11 - 1, local3081_10 / 2 + 1, local3081_11 + local3081_7 - 1, 1073741824);
            drawContext.drawText(mc.textRenderer, local3081_6.get(index), -local3081_10 / 2, local3081_11, -1, false);
         }

         drawContext.getMatrices().popMatrix();
      }
   }

   @Override
   protected void onDisable() {
      this.field06933.clear();
      this.field06934.clear();
   }

   private boolean method1958(Entity entity) {
      BlockPos blockPos = this.method1964(entity.getBlockPos());
      return blockPos != null && this.method1968(blockPos);
   }

   public static WardenEspModule method1959() {
      if (wardenEspModule == null) {
         wardenEspModule = new WardenEspModule();
      }

      return wardenEspModule;
   }

   public void method1961(DrawContext drawContext, float local3087_2) {
      if (this.isEnabled() && mc.player != null && mc.world != null && (!this.field06933.isEmpty() || !this.field06934.isEmpty())) {
         for (BlockPos blockPos : this.field06934.keySet()) {
            this.method1956(drawContext, Vec3d.ofBottomCenter(blockPos).add(0.0, 1.5, 0.0), Text.literal("Не активирован").withColor(16733525));
         }

         IntIterator intIterator = this.field06933.iterator();

         while (intIterator.hasNext()) {
            int local3087_4 = (Integer)intIterator.next();
            Entity entityById = mc.world.getEntityById(local3087_4);
            Text text = entityById == null ? null : this.method1962(entityById);
            if (text != null) {
               this.method1956(
                  drawContext,
                  entityById.getLerpedPos(local3087_2).add(0.0, entityById instanceof ArmorStandEntity ? entityById.getHeight() + 0.5 : 0.5, 0.0),
                  text
               );
            }
         }

         drawContext.createNewRootLayer();
      }
   }

   private Text method1962(Entity entity) {
      if (entity instanceof TextDisplayEntity textDisplayEntity) {
         return textDisplayEntity.getText();
      } else {
         return entity instanceof ArmorStandEntity armorStandEntity && armorStandEntity.hasCustomName() ? armorStandEntity.getCustomName() : null;
      }
   }

   private BlockPos method1964(BlockPos blockPos) {
      for (int index = 0; index <= 4; index++) {
         for (int index2 = -1; index2 <= 1; index2++) {
            for (int index3 = -1; index3 <= 1; index3++) {
               BlockPos blockPos2 = blockPos.add(index2, -index, index3);
               if (mc.world.getBlockState(blockPos2).getBlock() instanceof ChestBlock) {
                  return blockPos2;
               }
            }
         }
      }

      return null;
   }

   public boolean method1965(Entity entity) {
      return this.isEnabled() && this.field06933.contains(entity.getId());
   }

   private void method1966(Vec3d vec3d) {
      if (mc.world != null) {
         ChunkPos chunkPos = new ChunkPos(BlockPos.ofFloored(vec3d));

         for (int index = chunkPos.x - 1; index <= chunkPos.x + 1; index++) {
            for (int index2 = chunkPos.z - 1; index2 <= chunkPos.z + 1; index2++) {
               if (mc.world.isChunkLoaded(index, index2)) {
                  for (BlockEntity blockEntity : mc.world.getChunk(index, index2).getBlockEntities().values()) {
                     if (blockEntity instanceof ChestBlockEntity
                        && blockEntity.getPos().toCenterPos().squaredDistanceTo(vec3d) <= 256.0
                        && !this.method1968(blockEntity.getPos())) {
                        this.field06934.put(blockEntity.getPos().toImmutable(), System.currentTimeMillis());
                     }
                  }
               }
            }
         }
      }
   }

   private boolean method1968(BlockPos blockPos) {
      for (Entity entity : mc.world.getOtherEntities(null, new Box(blockPos).expand(1.5, 4.0, 1.5))) {
         Text text = this.method1962(entity);
         if (text != null
            && entity.getY() > blockPos.getY()
            && field06932.matcher(text.getString().replaceAll("(?i)§[0-9a-fk-or]", "")).find()) {
            return true;
         }
      }

      return false;
   }
}
