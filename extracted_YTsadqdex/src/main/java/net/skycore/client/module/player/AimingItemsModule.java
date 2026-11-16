package net.skycore.client.module.player;

import java.util.List;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.skycore.client.combat.Rotation;
import net.skycore.client.combat.RotationManager;
import net.skycore.client.module.ClientModule;
import net.skycore.client.module.ModuleCategory;
import net.skycore.client.setting.MultiSelectSetting;

public final class AimingItemsModule extends ClientModule {
   public static final AimingItemsModule INSTANCE = new AimingItemsModule();
   private final MultiSelectSetting loot = this.add(new MultiSelectSetting("Pick Up", List.of("Heads", "Elytra"), Set.of("Heads", "Elytra")));

   private boolean wanted(ItemEntity itemEntity) {
      if (this.loot.isSelected("Heads")) {
         ItemStack item = itemEntity.getItem();
         if (item.is(Items.PLAYER_HEAD)) {
            return true;
         }
      }

      if (this.loot.isSelected("Elytra")) {
         ItemStack item2 = itemEntity.getItem();
         if (item2.is(Items.ELYTRA)) {
            return true;
         }
      }

      return false;
   }

   private AimingItemsModule() {
      super("Aiming Items", "Aims the camera at selected items", ModuleCategory.PLAYER);
   }

   @Override
   protected void onTick() {
      Minecraft instance = Minecraft.getInstance();
      if (instance.player != null && instance.level != null) {
         ItemEntity itemEntity = null;
         double mAX_VALUE = Double.MAX_VALUE;

         for (Entity entity : instance.level.entitiesForRendering()) {
            if (entity instanceof ItemEntity itemEntity2 && this.wanted(itemEntity2)) {
               double distanceSquared = instance.player.distanceToSqr(itemEntity2);
               if (distanceSquared < mAX_VALUE) {
                  mAX_VALUE = distanceSquared;
                  itemEntity = itemEntity2;
               }
            }
         }

         if (itemEntity != null) {
            Vec3 boundingBox = itemEntity.getBoundingBox().getCenter().subtract(instance.player.getEyePosition());
            float angle = (float)Math.toDegrees(Math.atan2(boundingBox.z, boundingBox.x)) - 90.0F;
            float angle2 = (float)(-Math.toDegrees(Math.atan2(boundingBox.y, Math.hypot(boundingBox.x, boundingBox.z))));
            RotationManager.update(new Rotation(angle, angle2), 360.0F, 360.0F, 360.0F, 360.0F, 40, Integer.MAX_VALUE, true);
         }
      }
   }
}
