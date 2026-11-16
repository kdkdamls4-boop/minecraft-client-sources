package deluxe.mixin;

import deluxe.client.module.combat.BlockSlotModule;
import deluxe.client.module.misc.ChestStealerModule;
import deluxe.client.event.EventBus;
import deluxe.client.module.combat.NoEntityTraceModule;
import deluxe.client.module.player.NoPushModule;
import deluxe.client.event.PlayerSlowdownEvent;
import deluxe.client.event.PlayerTickEvent;
import deluxe.client.module.combat.AttackController;
import deluxe.client.util.GroundTicksAccess;
import deluxe.client.RotationManager;
import deluxe.client.event.ScreenCloseEvent;
import deluxe.client.module.movement.SpiderModule;
import ru.dreamix.fabricloader.VMBridge;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.util.hit.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientPlayerEntity.class)
public abstract class ClientPlayerEntityMixin2 implements GroundTicksAccess {
   @Unique
   private int ticksOnGround;

   @Inject(method = "tick()V", at = @At("HEAD"))
   private void tick(CallbackInfo callbackInfo) {
      AttackController.method1151();
      if (EventBus.getInstance().hasListeners(PlayerTickEvent.class)) {
         EventBus.getInstance().post(new PlayerTickEvent());
      }
   }

   @Inject(method = "sendMovementPackets()V", at = @At("HEAD"))
   private void countGroundTicks(CallbackInfo callbackInfo) {
      if (((ClientPlayerEntity)(Object)this).isOnGround()) {
         this.ticksOnGround++;
      } else {
         this.ticksOnGround = 0;
      }
   }

   @Inject(method = "sendMovementPackets()V", at = @At("HEAD"))
   private void spiderPreMotion(CallbackInfo callbackInfo) {
      SpiderModule.method2836().method2850();
   }

   @Override
   public int ticksOnGround() {
      return this.ticksOnGround;
   }

   @Inject(method = "closeHandledScreen()V", at = @At("HEAD"), cancellable = true)
   private void closeContainer(CallbackInfo callbackInfo) {
      if (MinecraftClient.getInstance().currentScreen == null) {
         callbackInfo.cancel();
      } else if (EventBus.getInstance().hasListeners(ScreenCloseEvent.class)) {
         ScreenCloseEvent screenCloseEvent = new ScreenCloseEvent(MinecraftClient.getInstance().currentScreen);
         EventBus.getInstance().post(screenCloseEvent);
         if (screenCloseEvent.method4421()) {
            callbackInfo.cancel();
         }
      }
   }

   @Inject(method = "pushOutOfBlocks(DD)V", at = @At("HEAD"), cancellable = true)
   private void removeBlockPush(double local8401_1, double local8401_3, CallbackInfo callbackInfo) {
      if (NoPushModule.method3305().method3306("Блоки")) {
         callbackInfo.cancel();
      }
   }

   @Inject(method = "getActiveItemSpeedMultiplier()F", at = @At("HEAD"), cancellable = true)
   private void liftItemUseSlow(CallbackInfoReturnable<Float> local8402_1) {
      if (EventBus.getInstance().hasListeners(PlayerSlowdownEvent.class)) {
         PlayerSlowdownEvent playerSlowdownEvent = new PlayerSlowdownEvent();
         EventBus.getInstance().post(playerSlowdownEvent);
         if (playerSlowdownEvent.method4421()) {
            local8402_1.setReturnValue(1.0F);
         }
      }
   }

   @Inject(method = "getCrosshairTarget(FLnet/minecraft/entity/Entity;)Lnet/minecraft/util/hit/HitResult;", at = @At("HEAD"), cancellable = true)
   private void traceThroughEntities(float local8403_1, Entity entity, CallbackInfoReturnable<HitResult> local8403_3) {
      if (NoEntityTraceModule.method3535().isEnabled()
         || ChestStealerModule.method0936().isEnabled() && ChestStealerModule.method0936().field01792.isChecked()) {
         local8403_3.setReturnValue(entity.raycast(((ClientPlayerEntity)(Object)this).getBlockInteractionRange(), local8403_1, false));
      }
   }

   @Inject(method = "dropSelectedItem(Z)Z", at = @At("HEAD"), cancellable = true)
   private void blockSelectedSlotDrop(boolean local8404_1, CallbackInfoReturnable<Boolean> local8404_2) {
      if (BlockSlotModule.method1380().method1378(((ClientPlayerEntity)(Object)this).getInventory().getSelectedSlot())) {
         local8404_2.setReturnValue(false);
      }
   }

   @Redirect(method = "tickMovementInput()V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;getPitch()F"))
   private float steerHandBobPitch(ClientPlayerEntity clientPlayerEntity) {
      return RotationManager.method0658().method0667() ? RotationManager.method0658().method0655() : clientPlayerEntity.getPitch();
   }

   @Redirect(method = "tickMovementInput()V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;getYaw()F"))
   private float steerHandBobYaw(ClientPlayerEntity clientPlayerEntity) {
      return RotationManager.method0658().method0667() ? RotationManager.method0658().method0666() : clientPlayerEntity.getYaw();
   }

   static {
      VMBridge.identifyClass(ClientPlayerEntityMixin2.class, "ZdXUcnyB");
   }
}
