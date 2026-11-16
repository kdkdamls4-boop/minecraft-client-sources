package net.skycore.client.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.skycore.client.command.FakeplayerCommand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class EntityCrystalDamageFakePlayersMixin {
   @Inject(method = "remove", at = @At("HEAD"))
   private void skycore$crystalDamageFakePlayers(RemovalReason removalReason, CallbackInfo callbackInfo) {
      Entity entity = (Entity)(Object)this;
      if (entity instanceof EndCrystal) {
         if (removalReason == RemovalReason.KILLED || removalReason == RemovalReason.DISCARDED) {
            if (!FakeplayerCommand.INSTANCE.getFakePlayers().isEmpty()) {
               FakeplayerCommand.INSTANCE.onCrystalExploded(entity.position());
            }
         }
      }
   }
}
