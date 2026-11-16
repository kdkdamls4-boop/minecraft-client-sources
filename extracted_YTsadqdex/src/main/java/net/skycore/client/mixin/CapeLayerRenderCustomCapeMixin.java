package net.skycore.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.CapeLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Items;
import net.skycore.client.config.FriendManager;
import net.skycore.client.module.render.CapeModule;
import net.skycore.client.util.AddBackVertexHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CapeLayer.class)
public abstract class CapeLayerRenderCustomCapeMixin {
   @Inject(
      method = "submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/renderer/entity/state/AvatarRenderState;FF)V",
      at = @At("HEAD"),
      cancellable = true
   )
   private void skycore$renderCustomCape(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int coordinate, AvatarRenderState avatarRenderState, float coordinate2, float coordinate3, CallbackInfo callbackInfo) {
      if (CapeModule.INSTANCE.isEnabled()) {
         Minecraft instance = Minecraft.getInstance();
         if (instance.level != null) {
            if (instance.level.getEntity(avatarRenderState.id) instanceof AbstractClientPlayer player) {
               if (player.getItemBySlot(EquipmentSlot.CHEST).is(Items.ELYTRA)) {
                  callbackInfo.cancel();
               } else {
                  boolean condition = player == instance.player;
                  boolean gameProfile = FriendManager.INSTANCE.isFriend(player.getGameProfile().name());
                  if (!condition || !instance.options.getCameraType().isFirstPerson()) {
                     if (condition || gameProfile) {
                        CapeModule.INSTANCE.syncConfigIfNeeded();
                        float gameTimeDeltaPartialTick = instance.getDeltaTracker().getGameTimeDeltaPartialTick(true);
                        AddBackVertexHelper.render(poseStack, submitNodeCollector, player, gameTimeDeltaPartialTick, coordinate);
                        callbackInfo.cancel();
                     }
                  }
               }
            }
         }
      }
   }
}
