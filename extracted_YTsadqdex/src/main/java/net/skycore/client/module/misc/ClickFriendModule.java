package net.skycore.client.module.misc;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.EntityHitResult;
import net.skycore.client.SkycoreClient;
import net.skycore.client.config.FriendManager;
import net.skycore.client.event.MouseButtonEvent;
import net.skycore.client.module.ClientModule;
import net.skycore.client.module.ModuleCategory;
import net.skycore.client.util.SendDonationHelper;

public final class ClickFriendModule extends ClientModule {
   public static final ClickFriendModule INSTANCE = new ClickFriendModule();

   private ClickFriendModule() {
      super("Click Friend", "Adds a player as a friend with the middle mouse button", ModuleCategory.MISC);
      SkycoreClient.events().subscribe(MouseButtonEvent.class, this::onMouse);
   }

   private void onMouse(MouseButtonEvent mouseButtonEvent) {
      Minecraft minecraft = mouseButtonEvent.client();
      if (this.isEnabled() && mouseButtonEvent.pressed() && mouseButtonEvent.button().button() == 2 && minecraft.player != null) {
         if (minecraft.hitResult instanceof EntityHitResult entityHitResult && entityHitResult.getEntity() instanceof Player player) {
            String gameProfile = player.getGameProfile().name();
            int value = FriendManager.INSTANCE.isFriend(gameProfile) && FriendManager.INSTANCE.remove(gameProfile) ? 1 : 0;
            if (value == 0) {
               FriendManager.INSTANCE.add(gameProfile);
            }

            SendDonationHelper.displayInfo(gameProfile + (value != 0 ? " removed from friends" : " added to friends"));
         }
      }
   }
}
