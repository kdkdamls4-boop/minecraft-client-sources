package net.skycore.client.module.movement;

import java.util.concurrent.TimeUnit;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.skycore.client.module.ClientModule;
import net.skycore.client.module.ModuleCategory;

public final class WebIgnoreModule extends ClientModule {
   public static final WebIgnoreModule INSTANCE = new WebIgnoreModule();
   private volatile long tempUntilNanos;

   public static boolean shouldIgnore(BlockState blockState) {
      if (blockState != null && blockState.is(Blocks.COBWEB)) {
         if (INSTANCE.isEnabled()) {
            return true;
         }

         if (INSTANCE.isTempActive()) {
            return true;
         }
      }

      return false;
   }

   public boolean isTempActive() {
      return System.nanoTime() < this.tempUntilNanos;
   }

   public void requestTemp(int value) {
      if (value > 0) {
         long timestamp = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(value * 50L);
         long maximum = Math.max(this.tempUntilNanos, timestamp);
         this.tempUntilNanos = maximum;
      }
   }

   private WebIgnoreModule() {
      super("Web Ignore", "Allows breaking and interacting through cobwebs", ModuleCategory.MOVEMENT);
   }
}
