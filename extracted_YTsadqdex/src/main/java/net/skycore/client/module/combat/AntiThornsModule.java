package net.skycore.client.module.combat;

import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ClientboundDamageEventPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.util.Util;
import net.minecraft.world.damagesource.DamageTypes;
import net.skycore.client.SkycoreClient;
import net.skycore.client.combat.ThornsTracker;
import net.skycore.client.event.PacketReceiveEvent;
import net.skycore.client.module.ClientModule;
import net.skycore.client.module.ModuleCategory;

public final class AntiThornsModule extends ClientModule {
   public static final AntiThornsModule INSTANCE = new AntiThornsModule();
   private final ThornsTracker window = new ThornsTracker();

   private AntiThornsModule() {
      super("AntiThorns", "Убирает отбрасывание от шипов при полёте на элитрах", ModuleCategory.COMBAT);
      SkycoreClient.events().subscribe(PacketReceiveEvent.class, this::onPacket);
   }

   private void onPacket(PacketReceiveEvent packetReceiveEvent) {
      Minecraft instance = Minecraft.getInstance();
      if (this.isEnabled() && instance.player != null && instance.level != null) {
         long millis = Util.getMillis();
         if (packetReceiveEvent.getPacket() instanceof ClientboundDamageEventPacket clientboundDamageEventPacket
            && clientboundDamageEventPacket.entityId() == instance.player.getId()
            && clientboundDamageEventPacket.sourceType().is(DamageTypes.THORNS)) {
            this.window.markDamage(millis);
         } else {
            if (packetReceiveEvent.getPacket() instanceof ClientboundSetEntityMotionPacket clientboundSetEntityMotionPacket
               && clientboundSetEntityMotionPacket.id() == instance.player.getId()
               && this.window.consumeVelocity(millis, instance.player.isFallFlying())) {
               packetReceiveEvent.cancel();
            }
         }
      }
   }
}
