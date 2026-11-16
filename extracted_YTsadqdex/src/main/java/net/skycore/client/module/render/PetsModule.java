package net.skycore.client.module.render;

import net.minecraft.client.Minecraft;
import net.skycore.client.module.ClientModule;
import net.skycore.client.module.ModuleCategory;
import net.skycore.client.setting.ModeSetting;
import net.skycore.client.util.LegacyRigHelper;
import net.skycore.client.util.LoadMeshHelper;
import net.skycore.client.util.NextIdleHopDelayHelper;

public final class PetsModule extends ClientModule {
   private final LoadMeshHelper renderer;
   private final NextIdleHopDelayHelper controller;
   private final ModeSetting pet = this.add(new ModeSetting("Pet", "Dog", "Dog", "Dragon", "Spike", "Minun", "Mama ptica", "AngryBird"));

   @Override
   protected void onTick() {
      NextIdleHopDelayHelper controller2 = this.controller;
      LegacyRigHelper legacyRigHelper = this.selectedPet();
      controller2.tick(legacyRigHelper);
   }

   @Override
   protected void onEnable() {
      this.controller.reset();
   }

   public PetsModule() {
      super("Pets", "A small companion that follows you", ModuleCategory.RENDER);
      this.controller = new NextIdleHopDelayHelper();
      this.renderer = new LoadMeshHelper();
   }

   public void selectPet(LegacyRigHelper legacyRigHelper) {
      if (legacyRigHelper != null) {
         this.pet.set(legacyRigHelper.label());
         this.controller.reset();
      }
   }

   public LegacyRigHelper selectedPet() {
      return LegacyRigHelper.fromLabel(this.pet.get());
   }

   public void renderWorld() {
      if (this.isEnabled()) {
         Minecraft instance = Minecraft.getInstance();
         if (instance.level != null) {
            if (instance.player != null) {
               float gameTimeDeltaPartialTick = instance.getDeltaTracker().getGameTimeDeltaPartialTick(true);
               this.renderer.render(this.selectedPet(), this.controller, gameTimeDeltaPartialTick);
               return;
            }
         }
      }
   }

   @Override
   protected void onDisable() {
      this.controller.reset();
   }
}
