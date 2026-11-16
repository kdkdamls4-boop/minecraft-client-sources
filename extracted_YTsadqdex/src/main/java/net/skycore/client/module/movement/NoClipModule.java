package net.skycore.client.module.movement;

import net.skycore.client.SkycoreClient;
import net.skycore.client.event.PlayerTickEndEvent;
import net.skycore.client.module.ClientModule;
import net.skycore.client.module.ModuleCategory;
import net.skycore.client.setting.BooleanSetting;

public final class NoClipModule extends ClientModule {
   private final BooleanSetting test;
   private long next;
   private int clips;
   public static final NoClipModule INSTANCE = new NoClipModule();
   private final BooleanSetting fast = this.add(new BooleanSetting("Fast", false));

   private NoClipModule() {
      super("No Clip", "Moves forward through blocks", ModuleCategory.MOVEMENT);
      this.test = this.add(new BooleanSetting("test", false));
      this.fast.visibleWhen(() -> {
         return !this.test.get();
      });
      SkycoreClient.events().subscribe(PlayerTickEndEvent.class, this::onPreTick);
   }

   @Override
   protected void onTick() {
      this.newTick();
   }

   @Override
   protected void onEnable() {
      this.next = 0L;
      this.clips = 0;
   }

   private void onPreTick(PlayerTickEndEvent event) {
   }

   public void newTick() {
   }
}
