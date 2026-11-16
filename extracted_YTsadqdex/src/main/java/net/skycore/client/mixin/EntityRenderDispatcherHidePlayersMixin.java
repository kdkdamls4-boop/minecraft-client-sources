package net.skycore.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.skycore.client.module.render.NoRenderModule;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderDispatcher.class)
public abstract class EntityRenderDispatcherHidePlayersMixin {
   @Inject(
      method = "submit(Lnet/minecraft/client/renderer/entity/state/EntityRenderState;Lnet/minecraft/client/renderer/state/level/CameraRenderState;DDDLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;)V",
      at = @At("HEAD"),
      cancellable = true
   )
   private <S extends EntityRenderState> void skycore$hidePlayers(
      S s, CameraRenderState cameraRenderState, double value, double value2, double value3, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CallbackInfo callbackInfo
   ) {
      if (s instanceof AvatarRenderState avatarRenderState) {
         Minecraft instance = Minecraft.getInstance();
         LocalPlayer player = instance.player;
         if (player != null) {
            boolean firstPerson = !instance.options.getCameraType().isFirstPerson();
            if (NoRenderModule.shouldRemoveThirdPersonSelf() && firstPerson && avatarRenderState.id == player.getId()) {
               callbackInfo.cancel();
            } else {
               if (NoRenderModule.shouldRemoveOtherPlayers() && avatarRenderState.id != player.getId()) {
                  callbackInfo.cancel();
               }
            }
         }
      }
   }
}
