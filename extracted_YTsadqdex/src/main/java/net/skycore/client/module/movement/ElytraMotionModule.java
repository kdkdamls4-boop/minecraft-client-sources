package net.skycore.client.module.movement;

import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.skycore.client.SkycoreClient;
import net.skycore.client.combat.CriticalHelper;
import net.skycore.client.combat.RayTraceHelper;
import net.skycore.client.event.PlayerInputEvent;
import net.skycore.client.event.PlayerTickEndEvent;
import net.skycore.client.module.ClientModule;
import net.skycore.client.module.ModuleCategory;
import net.skycore.client.module.ModuleManager;
import net.skycore.client.module.combat.AttackAuraModule;
import net.skycore.client.module.combat.ElytraTargetModule;
import net.skycore.client.setting.NumberSetting;

public final class ElytraMotionModule extends ClientModule {
   private Vec3 savedDelta;
   private boolean improveFreezeLatch;
   private boolean freezing;
   private final NumberSetting distance = this.add(new NumberSetting("Дистанция", "", 1.5, 1.0, 3.0, 0.1, " blocks"));
   public static final ElytraMotionModule INSTANCE = new ElytraMotionModule();

   private void setImproveFreezeActive(boolean improveFreezeLatch2) {
      if (this.improveFreezeLatch != improveFreezeLatch2) {
         this.improveFreezeLatch = improveFreezeLatch2;
         Minecraft instance = Minecraft.getInstance();
         LocalPlayer player = instance.player;
         if (!improveFreezeLatch2 && player != null) {
            player.setDeltaMovement(this.savedDelta);
         }
      }
   }

   @Override
   protected void onEnable() {
      this.freezing = false;
   }

   private ElytraMotionModule() {
      super("Elytra Motion", "Stops elytra movement near aura targets", ModuleCategory.MOVEMENT);
      this.savedDelta = Vec3.ZERO;
      SkycoreClient.events().subscribe(PlayerTickEndEvent.class, this::onPreTick);
      SkycoreClient.events().subscribe(PlayerInputEvent.class, this::onInput);
   }

   private boolean isLookingAt(LocalPlayer player, LivingEntity entity) {
      ModuleManager moduleManager = SkycoreClient.modules();
      var optional = moduleManager.get(AttackAuraModule.class);
      AttackAuraModule attackAuraModule = (AttackAuraModule)optional.orElse(null);
      double distance2;
      if (attackAuraModule != null) {
         distance2 = attackAuraModule.attackDistance();
      } else {
         distance2 = 3.0;
      }

      double value = distance2;
      float yRot = player.getYRot();
      float xRot = player.getXRot();
      return RayTraceHelper.rayTraceEntity(player, entity, yRot, xRot, value, false, false);
   }

   private LivingEntity auraTarget() {
      ModuleManager moduleManager = SkycoreClient.modules();
      AttackAuraModule attackAuraModule = moduleManager.get(AttackAuraModule.class).orElse(null);
      return attackAuraModule == null ? null : attackAuraModule.currentTarget();
   }

   @Override
   protected void onDisable() {
      this.setImproveFreezeActive(false);
      this.freezing = false;
   }

   public boolean isFreezing() {
      return this.freezing;
   }

   public Vec3 onMove(Vec3 position) {
      if (!this.isEnabled()) {
         return position;
      }

      Minecraft instance = Minecraft.getInstance();
      LocalPlayer player = instance.player;
      if (player != null && instance.level != null) {
         this.freezing = false;
         LivingEntity entity = this.auraTarget();
         int strictDistance = entity != null
               && player.isFallFlying()
               && CriticalHelper.getStrictDistance(entity) < this.distance.get()
               && !ElytraTargetModule.INSTANCE.isPredicting(entity)
               && this.isLookingAt(player, entity)
            ? 1
            : 0;
         this.setImproveFreezeActive((strictDistance != 0));
         if (strictDistance == 0) {
            return position;
         }

         this.freezing = true;
         return position.scale(0.0);
      } else {
         return position;
      }
   }

   private void onPreTick(PlayerTickEndEvent event) {
      LocalPlayer player = event.player();
      if (player != null) {
         if (!this.improveFreezeLatch) {
            this.savedDelta = player.getDeltaMovement();
         }
      }
   }

   private void onInput(PlayerInputEvent playerInputEvent) {
      if (this.improveFreezeLatch) {
         playerInputEvent.clearMovement(false, true);
         playerInputEvent.setJump(false);
      }
   }
}
