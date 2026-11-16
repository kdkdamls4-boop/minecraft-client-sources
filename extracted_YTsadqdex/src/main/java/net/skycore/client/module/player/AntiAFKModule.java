package net.skycore.client.module.player;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.skycore.client.module.ClientModule;
import net.skycore.client.module.ModuleCategory;
import net.skycore.client.setting.MultiSelectSetting;

public final class AntiAFKModule extends ClientModule {
   private long nextAction;
   public static final AntiAFKModule INSTANCE = new AntiAFKModule();
   private final MultiSelectSetting actions = this.add(
      new MultiSelectSetting("Actions", List.of("Jump", "Command", "Swing Arm"), Set.of("Jump", "Command", "Swing Arm"))
   );

   private AntiAFKModule() {
      super("Anti AFK", "Prevents idle disconnects", ModuleCategory.PLAYER);
   }

   @Override
   protected void onTick() {
      Minecraft instance = Minecraft.getInstance();
      if (instance.player != null && instance.level != null && instance.gui.screen() == null && System.currentTimeMillis() >= this.nextAction) {
         if (this.actions.isSelected("Jump") && instance.player.onGround()) {
            instance.player.jumpFromGround();
         }

         if (this.actions.isSelected("Command")) {
            StringBuilder stringBuilder = new StringBuilder(5);

            for (int value = 0; value < 5; value++) {
               stringBuilder.append((char)(97 + ThreadLocalRandom.current().nextInt(26)));
            }

            instance.player.connection.sendCommand(stringBuilder.toString());
         }

         if (this.actions.isSelected("Swing Arm")) {
            instance.player.swing(InteractionHand.MAIN_HAND);
         }

         this.nextAction = System.currentTimeMillis() + 20000L;
      }
   }

   @Override
   protected void onEnable() {
      long timestamp = System.currentTimeMillis() + 20000L;
      this.nextAction = timestamp;
   }
}
