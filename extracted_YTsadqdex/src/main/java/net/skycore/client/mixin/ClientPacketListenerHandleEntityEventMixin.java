package net.skycore.client.mixin;

import com.mojang.datafixers.util.Pair;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.network.protocol.game.ClientboundSetCursorItemPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket;
import net.minecraft.network.protocol.game.ClientboundSetPlayerInventoryPacket;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData.DataValue;
import net.minecraft.world.item.ItemStack;
import net.skycore.client.SkycoreClient;
import net.skycore.client.module.combat.AutoTotemModule;
import net.skycore.client.module.render.ParticlesModule;
import net.skycore.client.module.render.ThunderShatterModule;
import net.skycore.client.util.FindTextureInTagHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerHandleEntityEventMixin {
   @Inject(method = "handleEntityEvent", at = @At("HEAD"))
   private void skycore$handleEntityEvent(ClientboundEntityEventPacket clientboundEntityEventPacket, CallbackInfo callbackInfo) {
      if (Minecraft.getInstance().isSameThread()) {
         SkycoreClient.modules().get(AutoTotemModule.class).ifPresent(autoTotemModule -> autoTotemModule.onEntityEvent(clientboundEntityEventPacket));
         ThunderShatterModule.INSTANCE.onEntityEvent(clientboundEntityEventPacket);
         ParticlesModule.INSTANCE.onEntityEvent(clientboundEntityEventPacket);
      }
   }

   @Inject(method = "handleBlockEntityData", at = @At("HEAD"))
   private void skycore$normalizeSkullProfileOnUpdatePacket(ClientboundBlockEntityDataPacket clientboundBlockEntityDataPacket, CallbackInfo callbackInfo) {
      if (Minecraft.getInstance().isSameThread()) {
         FindTextureInTagHelper.normalize(clientboundBlockEntityDataPacket.getTag());
      }
   }

   @Inject(method = "handleContainerContent", at = @At("HEAD"))
   private void skycore$fixSkullItemsOnContent(ClientboundContainerSetContentPacket clientboundContainerSetContentPacket, CallbackInfo callbackInfo) {
      if (Minecraft.getInstance().isSameThread()) {
         for (ItemStack itemStack : clientboundContainerSetContentPacket.items()) {
            FindTextureInTagHelper.fixItemStack(itemStack);
         }

         FindTextureInTagHelper.fixItemStack(clientboundContainerSetContentPacket.carriedItem());
      }
   }

   @Inject(method = "handleContainerSetSlot", at = @At("HEAD"))
   private void skycore$fixSkullItemOnSlot(ClientboundContainerSetSlotPacket clientboundContainerSetSlotPacket, CallbackInfo callbackInfo) {
      if (Minecraft.getInstance().isSameThread()) {
         FindTextureInTagHelper.fixItemStack(clientboundContainerSetSlotPacket.getItem());
      }
   }

   @Inject(method = "handleSetCursorItem", at = @At("HEAD"))
   private void skycore$fixSkullItemOnCursor(ClientboundSetCursorItemPacket clientboundSetCursorItemPacket, CallbackInfo callbackInfo) {
      if (Minecraft.getInstance().isSameThread()) {
         FindTextureInTagHelper.fixItemStack(clientboundSetCursorItemPacket.contents());
      }
   }

   @Inject(method = "handleSetPlayerInventory", at = @At("HEAD"))
   private void skycore$fixSkullItemOnInventory(ClientboundSetPlayerInventoryPacket clientboundSetPlayerInventoryPacket, CallbackInfo callbackInfo) {
      if (Minecraft.getInstance().isSameThread()) {
         FindTextureInTagHelper.fixItemStack(clientboundSetPlayerInventoryPacket.contents());
      }
   }

   @Inject(method = "handleSetEquipment", at = @At("HEAD"))
   private void skycore$fixSkullItemOnEquipment(ClientboundSetEquipmentPacket clientboundSetEquipmentPacket, CallbackInfo callbackInfo) {
      if (Minecraft.getInstance().isSameThread()) {
         for (Pair pair : clientboundSetEquipmentPacket.getSlots()) {
            FindTextureInTagHelper.fixItemStack((ItemStack)pair.getSecond());
         }
      }
   }

   @Inject(method = "handleSetEntityData", at = @At("HEAD"))
   private void skycore$fixSkullItemOnEntityData(ClientboundSetEntityDataPacket clientboundSetEntityDataPacket, CallbackInfo callbackInfo) {
      if (Minecraft.getInstance().isSameThread()) {
         for (DataValue dataValue : clientboundSetEntityDataPacket.packedItems()) {
            if (dataValue.serializer() == EntityDataSerializers.ITEM_STACK && dataValue.value() instanceof ItemStack itemStack) {
               FindTextureInTagHelper.fixItemStack(itemStack);
            }
         }
      }
   }
}
