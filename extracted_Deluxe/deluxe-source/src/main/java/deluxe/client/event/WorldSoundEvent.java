package deluxe.client.event;

import net.minecraft.util.math.BlockPos;

public class WorldSoundEvent extends TickEvent {
   private final int field10849;
   private final BlockPos field10850;

   public WorldSoundEvent(int local5150_1, BlockPos blockPos) {
      this.field10849 = local5150_1;
      this.field10850 = blockPos;
   }

   public int method3189() {
      return this.field10849;
   }

   public BlockPos method3190() {
      return this.field10850;
   }
}
