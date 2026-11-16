package deluxe.client.module.combat;

import deluxe.client.setting.BooleanSetting;
import deluxe.client.event.EventListener;
import deluxe.client.util.ItemCooldownAccess;
import deluxe.client.setting.KeybindSetting;
import deluxe.mixin.LivingEntityMixin;
import deluxe.client.module.Module;
import deluxe.client.module.ModuleCategory;
import deluxe.client.module.ModuleInfo;
import deluxe.client.event.MovementInputEvent;
import deluxe.client.event.PacketEvent;
import deluxe.client.event.PlayerTickEvent;
import deluxe.client.util.InventoryUtils;
import deluxe.client.util.InventoryActionQueue;
import deluxe.client.ui.hud.NotificationHud;
import deluxe.client.util.InventorySlotManager;
import deluxe.client.RotationAngles;
import deluxe.client.RotationManager;
import deluxe.client.RotationSpeed;

import java.util.Locale;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

@ModuleInfo(name = "MaceHelper", description = "Взлёт на заряде ветра под смэш булавой по бинду.", category = ModuleCategory.COMBAT)
public final class MaceHelperModule extends Module {
   private static MaceHelperModule maceHelperModule;
   public KeybindSetting keybindSetting;
   public BooleanSetting field05761;
   public BooleanSetting field05762;
   public BooleanSetting field05763;
   public BooleanSetting field05764;
   private boolean field05765;
   private boolean field05766;
   private boolean field05767;
   private int field05768 = -1;
   private long field05769;
   private boolean field05770;
   private boolean field05771;
   private boolean field05772;
   private int field05773 = -1;
   private int field05774 = -1;
   private boolean field05775;
   private boolean field05776;
   private boolean field05777;
   private boolean field05778;
   private long field05779;
   private long field05780;
   private long field05781;
   private long field05782;
   private final EventListener<PlayerTickEvent> field05783 = playerTickEvent -> {
      if (mc.player != null && mc.interactionManager != null && mc.world != null) {
         boolean local2701_2 = this.keybindSetting.isBound() && this.keybindSetting.getBinding().method2749(mc.getWindow().getHandle());
         if (local2701_2 && !this.field05765 && mc.currentScreen == null) {
            this.field05766 = !this.field05766;
            this.field05781 = System.currentTimeMillis() + 1500L;
         }

         this.field05765 = local2701_2;
         if (!mc.player.isOnGround()) {
            this.field05780 = 0L;
         } else if (this.field05780 == 0L) {
            this.field05780 = System.currentTimeMillis();
         }

         if (mc.player.isOnGround()
            && this.field05780 != 0L
            && System.currentTimeMillis() - this.field05780 > 500L
            && !this.field05767
            && this.field05768 < 0
            && System.currentTimeMillis() >= this.field05782) {
            this.field05775 = false;
            this.field05776 = false;
            this.field05770 = false;
            this.field05771 = false;
            if (this.field05772
               && mc.currentScreen == null
               && !InventorySlotManager.method4176().method4179()
               && !InventoryActionQueue.method2656().method2665()) {
               this.method1782();
            }
         }

         if (this.field05770) {
            if (!mc.player.isOnGround()) {
               this.field05771 = true;
            } else if (this.field05771) {
               this.field05770 = false;
               this.field05771 = false;
            }
         }

         if (this.field05775) {
            if (!mc.player.isOnGround()) {
               this.field05776 = true;
            } else if (this.field05776) {
               this.field05775 = false;
               this.field05776 = false;
               this.field05777 = false;
            }
         }

         if (this.field05772) {
            if (!mc.player.isOnGround()) {
               this.field05778 = true;
            } else if (this.field05778
               && mc.currentScreen == null
               && !InventorySlotManager.method4176().method4179()
               && !InventoryActionQueue.method2656().method2665()) {
               this.method1782();
            }
         }

         if (this.field05777 && !InventorySlotManager.method4176().method4179() && !InventoryActionQueue.method2656().method2665()) {
            this.field05777 = false;
            this.method1777();
         }

         if (this.field05767 && (System.currentTimeMillis() > this.field05781 || InventoryUtils.method0557(Items.WIND_CHARGE) == -1)) {
            this.method1779("бросок сорвался");
         }

         if (this.field05766 && System.currentTimeMillis() > this.field05781) {
            this.method1779("не вышло кинуть");
         }

         if (this.field05767) {
            RotationManager.method0658()
               .method0652(new RotationAngles(RotationAngles.method4366(), 90.0F), new RotationSpeed(360.0F, 360.0F, 44.0F, 44.0F));
         }

         if (this.field05768 > 0) {
            this.field05768--;
         }

         if (this.field05766) {
            int local2701_3 = InventoryUtils.method0557(Items.WIND_CHARGE);
            if (local2701_3 == -1) {
               this.field05766 = false;
               NotificationHud.method2691(
                  this.category.getIcon(), "Заряд ветра", Text.literal("нет в инвентаре").setStyle(Style.EMPTY.withColor(Formatting.RED)), true
               );
            } else if (mc.player.getItemCooldownManager().isCoolingDown(mc.player.getInventory().getStack(local2701_3))) {
               this.field05766 = false;
               NotificationHud.method2691(
                  this.category.getIcon(),
                  "Заряд ветра",
                  Text.literal("в кд ещё ")
                     .setStyle(Style.EMPTY.withColor(Formatting.GRAY))
                     .append(
                        Text.literal(
                              String.format(
                                 Locale.US,
                                 "%.1fс",
                                 ((ItemCooldownAccess)mc.player.getItemCooldownManager())
                                       .remainingCooldownTicks(mc.player.getInventory().getStack(local2701_3))
                                    / 20.0F
                              )
                           )
                           .setStyle(Style.EMPTY.withColor(Formatting.RED))
                     ),
                  true
               );
            } else if (this.field05764.isChecked()
               && !mc.world.isSpaceEmpty(mc.player, mc.player.getBoundingBox().stretch(0.0, 3.0, 0.0))) {
               this.field05766 = false;
               NotificationHud.method2691(
                  this.category.getIcon(), "Заряд ветра", Text.literal("сверху блоки").setStyle(Style.EMPTY.withColor(Formatting.RED)), true
               );
            } else {
               RotationManager.method0658()
                  .method0652(new RotationAngles(RotationAngles.method4366(), 90.0F), new RotationSpeed(360.0F, 360.0F, 44.0F, 44.0F));
               if (!(mc.player.getPitch() < 85.0F)) {
                  if (mc.player.isOnGround() && System.currentTimeMillis() - this.field05780 >= 40L) {
                     this.field05766 = false;
                     this.field05767 = true;
                     this.field05781 = System.currentTimeMillis() + 800L;
                     this.field05780 = System.currentTimeMillis();
                     int local2701_4 = InventoryUtils.method0554();
                     this.field05775 = true;
                     this.field05776 = false;
                     if (this.field05763.isChecked()) {
                        this.field05770 = true;
                        this.field05771 = false;
                     }

                     this.field05777 = true;
                     InventorySlotManager.method4176().method4174(mc.player.getInventory().getStack(local2701_3).copy());
                  }
               }
            }
         }
      }
   };
   private final EventListener<MovementInputEvent> field05784 = movementInputEvent -> {
      if (mc.player != null) {
         if (this.field05766 || this.field05767 || this.field05768 > 0) {
            movementInputEvent.method4442(false);
         }

         if (this.field05767 || this.field05768 > 0) {
            movementInputEvent.method4446(0.0F);
            movementInputEvent.method4449(0.0F);
         }

         if (this.field05768 == 0) {
            this.field05768 = -1;
            ((LivingEntityMixin)mc.player).setNoJumpDelay(0);
            movementInputEvent.method4442(true);
         }
      }
   };
   private final EventListener<PacketEvent> field05785 = packetEvent -> {
      if (packetEvent.isOutgoing()
         && mc.player != null
         && System.currentTimeMillis() < this.field05779
         && packetEvent.getPacket() instanceof PlayerInteractItemC2SPacket playerInteractItemC2SPacket) {
         packetEvent.setPacket(new PlayerInteractItemC2SPacket(playerInteractItemC2SPacket.getHand(), playerInteractItemC2SPacket.getSequence(), playerInteractItemC2SPacket.getYaw(), 90.0F));
         this.field05779 = 0L;
      }

      if (packetEvent.isOutgoing()
         && mc.player != null
         && packetEvent.getPacket() instanceof PlayerInteractItemC2SPacket playerInteractItemC2SPacket2
         && (mc.player.getStackInHand(playerInteractItemC2SPacket2.getHand()).isOf(Items.WIND_CHARGE) || this.field05767)) {
         this.field05767 = false;
         this.field05768 = 1;
         this.field05782 = System.currentTimeMillis() + 600L;
      }

      if (packetEvent.isIncoming()
         && mc.player != null
         && packetEvent.getPacket() instanceof ExplosionS2CPacket explosionS2CPacket
         && explosionS2CPacket.playerKnockback().isPresent()
         && explosionS2CPacket.center().squaredDistanceTo(mc.player.getEntityPos()) < 64.0) {
         this.field05782 = 0L;
      }

      if (packetEvent.isIncoming() && packetEvent.getPacket() instanceof PlayerPositionLookS2CPacket) {
         this.field05769 = System.currentTimeMillis();
      }
   };

   public boolean method1775() {
      return this.field05772;
   }

   public static MaceHelperModule method1776() {
      if (maceHelperModule == null) {
         maceHelperModule = new MaceHelperModule();
      }

      return maceHelperModule;
   }

   private void method1777() {
      int local2700_1 = InventoryUtils.method0539(Items.MACE);
      int local2700_2 = local2700_1 != -1 ? local2700_1 : InventoryUtils.method0557(Items.MACE);
      if (this.field05762.isChecked() && mc.player != null && !this.field05772 && local2700_2 != -1) {
         if (mc.player.getItemCooldownManager().isCoolingDown(mc.player.getInventory().getStack(local2700_2))) {
            NotificationHud.method2691(
               this.category.getIcon(),
               "Булава",
               Text.literal("в кд ещё ")
                  .setStyle(Style.EMPTY.withColor(Formatting.GRAY))
                  .append(
                     Text.literal(
                           String.format(
                              Locale.US,
                              "%.1fс",
                              ((ItemCooldownAccess)mc.player.getItemCooldownManager())
                                    .remainingCooldownTicks(mc.player.getInventory().getStack(local2700_2))
                                 / 20.0F
                           )
                        )
                        .setStyle(Style.EMPTY.withColor(Formatting.RED))
                  ),
               true
            );
         } else {
            this.field05772 = true;
            this.field05778 = false;
            if (!mc.player.getMainHandStack().isOf(Items.MACE)) {
               this.field05774 = InventoryUtils.method0554();
               this.field05773 = local2700_2;
               if (local2700_2 > 8) {
                  InventoryActionQueue.method2656().method2662(local2700_2, InventoryUtils.method0554(), 2);
               } else {
                  InventoryUtils.method0553(local2700_2, false);
               }
            }
         }
      }
   }

   private void method1779(String local2702_1) {
      this.field05766 = false;
      this.field05767 = false;
      this.field05779 = 0L;
      this.field05782 = 0L;
      if (mc.player.isOnGround()) {
         this.field05777 = false;
         this.field05775 = false;
         this.field05776 = false;
         NotificationHud.method2691(
            this.category.getIcon(), "Заряд ветра", Text.literal(local2702_1).setStyle(Style.EMPTY.withColor(Formatting.RED)), true
         );
      }
   }

   private MaceHelperModule() {
      this.keybindSetting = this.addSetting(new KeybindSetting("Кинуть заряд ветра", "Кидает заряд ветра под себя с прыжком для максимального взлёта."));
      this.field05761 = this.addSetting(
         new BooleanSetting("Усиление урона", "Держит удар булавой до самого приземления, если падаешь на цель смэш пройдёт с максимальным падением.", true)
      );
      this.field05762 = this.addSetting(
         new BooleanSetting("Автовзятие булавы", "После взлёта сама переключается на булаву и держит её до приземления.", true)
      );
      this.field05763 = this.addSetting(
         new BooleanSetting("Помогать AttackAura в наведении", "После взлёта расширяет радиус прицеливания булавы до приземления.", true)
      );
      this.field05764 = this.addSetting(
         new BooleanSetting("Обходить препятствие сверху", "Не кидает, если над головой блоки и взлёт упрётся в потолок.", true)
      );
   }

   private void method1782() {
      if (this.field05772 && mc.player != null) {
         this.field05772 = false;
         if (this.field05773 > 8) {
            InventoryActionQueue.method2656().method2662(this.field05774, this.field05773, 2);
         } else if (this.field05774 != -1) {
            InventoryUtils.method0553(this.field05774, false);
         }

         this.field05773 = -1;
         this.field05774 = -1;
      }
   }

   public boolean method1783() {
      return this.field05766 || this.field05767;
   }

   @Override
   protected void onDisable() {
      this.method1782();
      this.field05766 = false;
      this.field05767 = false;
      this.field05768 = -1;
      this.field05770 = false;
      this.field05771 = false;
      this.field05775 = false;
      this.field05776 = false;
      this.field05777 = false;
      this.field05778 = false;
      this.field05779 = 0L;
      this.field05780 = 0L;
      this.field05781 = 0L;
      this.field05782 = 0L;
   }

   public boolean method1784() {
      return this.field05775;
   }
}
