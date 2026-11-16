package deluxe.client.module.combat;

import deluxe.client.setting.BooleanSetting;
import deluxe.client.event.EventListener;
import deluxe.client.storage.FriendStore;
import deluxe.client.setting.LegacyNumberSetting;
import deluxe.client.setting.ModeSetting;
import deluxe.client.module.Module;
import deluxe.client.module.ModuleCategory;
import deluxe.client.module.ModuleInfo;
import deluxe.client.event.MovementInputEvent;
import deluxe.client.setting.MultiSelectSetting;
import deluxe.client.event.PacketEvent;
import deluxe.client.event.PlayerTickEvent;
import deluxe.client.event.PostAttackEvent;
import deluxe.client.util.InventoryUtils;
import deluxe.client.module.combat.CriticalHitUtils;
import deluxe.client.module.combat.AttackController;
import deluxe.client.util.InventoryActionQueue;
import deluxe.client.util.EntityUtils;
import deluxe.client.util.InventorySlotManager;
import deluxe.client.module.player.ServerHelperModule;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.MaceItem;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.Full;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.PositionAndOnGround;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;

@ModuleInfo(
   name = "TriggerBot",
   description = "Бьёт того, на кого уже наведён прицел, таймингами ауры и без своего наведения.",
   category = ModuleCategory.COMBAT
)
public final class TriggerBotModule extends Module {
   private static TriggerBotModule triggerBotModule;
   public LegacyNumberSetting legacyNumberSetting;
   public MultiSelectSetting field15573;
   public ModeSetting modeSetting;
   public MultiSelectSetting field15575;
   public MultiSelectSetting field15576;
   public BooleanSetting booleanSetting;
   private LivingEntity field15578;
   private boolean field15579;
   private boolean field15580;
   private int field15581 = -1;
   private int field15582 = -1;
   private long field15583;
   private final EventListener<PostAttackEvent> field15584 = postAttackEvent -> {
      if (mc.player != null && this.field15576.matchesOption("Кип спринт")) {
         mc.player.setSprinting(true);
      }
   };
   private final EventListener<PacketEvent> field15585 = packetEvent -> {
      if (mc.player != null && packetEvent.isOutgoing() && System.currentTimeMillis() < this.field15583) {
         if (packetEvent.getPacket() instanceof PositionAndOnGround || packetEvent.getPacket() instanceof Full) {
            packetEvent.method4423();
         }
      }
   };
   private final EventListener<MovementInputEvent> field15586 = movementInputEvent -> {
      if (mc.player != null && System.currentTimeMillis() < this.field15583) {
         movementInputEvent.method4446(0.0F);
         movementInputEvent.method4449(0.0F);
         movementInputEvent.method4442(false);
         movementInputEvent.method4448(false);
      } else if (this.field15578 != null && this.method4583()) {
         if (!(EntityUtils.method2816(this.field15578) >= this.method4585())
            && AttackController.method1170(this.field15578, this.method4594(), "Легитный")) {
            movementInputEvent.method4446(0.0F);
            movementInputEvent.method4449(0.0F);
         }
      }
   };
   private final EventListener<PlayerTickEvent> field15587 = playerTickEvent -> {
      this.field15578 = null;
      if (mc.player != null && mc.world != null && mc.player.isAlive()) {
         if (System.currentTimeMillis() < this.field15583) {
            mc.player.setVelocity(Vec3d.ZERO);
         }

         this.method4592();
         if (!this.field15575.matchesOption("Открыт инвентарь") || mc.currentScreen == null) {
            if (this.field15575.matchesOption("Ешь") && mc.player.isUsingItem()) {
               ItemStack activeItem = mc.player.getActiveItem();
               if (!activeItem.isEmpty() && (activeItem.get(DataComponentTypes.FOOD) != null || activeItem.get(DataComponentTypes.POTION_CONTENTS) != null)) {
                  return;
               }
            }

            this.field15578 = this.method4584();
            if (this.field15578 != null) {
               AttackController.method1164(this.field15578, true, true, true);
               if (AttackController.method1156(this.field15578, true, true, this.method4583(), 0L, this.method4594())) {
                  AttackController.method1147(this.field15578, null, null, Hand.MAIN_HAND, false);
               }
            }
         }
      }
   };

   private TriggerBotModule() {
      this.legacyNumberSetting = this.addSetting(new LegacyNumberSetting("Радиус удара", "На какой дистанции наносить удар.", 3.0, 3.0, 6.0, 0.1));
      this.field15573 = this.addSetting(
         new MultiSelectSetting("Кого атаковать", "Каких существ считать целью.", new String[]{"Игроки", "Голые", "Мобы"}, "Игроки", "Голые")
      );
      this.modeSetting = this.addSetting(new ModeSetting("Бить через блоки", "Разрешить удар сквозь блоки.", "Нет", "Нет", "Обычные", "Неполноценные"));
      this.field15575 = this.addSetting(
         new MultiSelectSetting("Не бить если", "Условия паузы удара.", new String[]{"Ешь", "Открыт инвентарь"}, "Ешь", "Открыт инвентарь")
      );
      this.field15576 = this.addSetting(
         new MultiSelectSetting("Настройки", "Дополнительные опции удара.", new String[]{"Умные криты", "Кип спринт"}, "Умные криты")
      );
      this.booleanSetting = this.addSetting(
         new BooleanSetting("Авто-булава", "Сама берёт булаву, бьёт смэшем при падении на цель и возвращает прежний предмет.", false)
      );
      this.legacyNumberSetting
         .automaticWhen(
            () -> ServerHelperModule.method0707().isEnabled()
               && ServerHelperModule.method0707().modeSetting.isSelected("SpookyTime")
               && this.legacyNumberSetting.getValue() > 3.65F
         );
   }

   public boolean method4583() {
      return this.field15576.matchesOption("Умные криты");
   }

   private LivingEntity method4584() {
      LivingEntity entity = null;

      for (Entity entity2 : mc.world.getEntities()) {
         if (entity2 instanceof LivingEntity entity3
            && this.method4597(entity3, this.legacyNumberSetting.getValue())
            && AttackController.method1163(entity3, this.legacyNumberSetting.getValue())
            && (entity == null || EntityUtils.method2816(entity3) < EntityUtils.method2816(entity))) {
            entity = entity3;
         }
      }

      return entity;
   }

   private double method4585() {
      return mc.player != null && mc.player.getMainHandStack().isOf(Items.MACE) ? 3.0 : Math.max(3.0, this.legacyNumberSetting.getValue());
   }

   public boolean method4586() {
      return this.method4589() && mc.player != null && !mc.player.isOnGround();
   }

   public boolean method4587() {
      return this.method4586() && MaceItem.shouldDealAdditionalDamage(mc.player);
   }

   public boolean method4589() {
      return this.booleanSetting.isChecked() && this.field15579 || mc.player != null && mc.player.getMainHandStack().isOf(Items.MACE);
   }

   private void method4590() {
      if (this.field15579 && mc.player != null) {
         this.field15579 = false;
         this.field15580 = true;
         if (this.field15581 > 8) {
            InventoryActionQueue.method2656().method2662(this.field15582, this.field15581, 1);
         } else if (this.field15582 != -1) {
            InventoryUtils.method0553(this.field15582, ServerHelperModule.method0707().modeSetting.matchesOption("SpookyTime"));
         }

         this.field15581 = -1;
         this.field15582 = -1;
      }
   }

   private boolean method4591() {
      if (this.field15579 && !mc.player.isOnGround()) {
         return true;
      }

      if (mc.player.isOnGround()) {
         return false;
      }

      Vec3d vec3d = CriticalHitUtils.method0970();
      return vec3d == null
         ? mc.world.isSpaceEmpty(mc.player, mc.player.getBoundingBox().stretch(0.0, -3.5, 0.0))
         : mc.player.getY() + mc.player.getVelocity().y > vec3d.y && mc.player.getY() - vec3d.y > 3.5;
   }

   private void method4592() {
      if (!InventorySlotManager.method4176().method4179() && !InventoryActionQueue.method2656().method2665()) {
         if (!this.booleanSetting.isChecked()) {
            this.method4590();
         } else {
            if (mc.player.isOnGround() || mc.player.getVelocity().y >= 0.0) {
               this.field15580 = false;
            }

            if (!this.method4591()) {
               this.method4590();
            } else {
               int local7752_1 = InventoryUtils.method0539(Items.MACE);
               int local7752_2 = local7752_1 != -1 ? local7752_1 : InventoryUtils.method0557(Items.MACE);
               if (local7752_2 != -1
                  && !this.field15579
                  && !this.field15580
                  && !mc.player.getItemCooldownManager().isCoolingDown(mc.player.getInventory().getStack(local7752_2))) {
                  this.field15579 = true;
                  if (!mc.player.getMainHandStack().isOf(Items.MACE)) {
                     if (ServerHelperModule.method0707().modeSetting.matchesOption("SpookyTime")
                        && !this.method4586()
                        && !MaceHelperModule.method1776().method1775()
                        && !MaceHelperModule.method1776().method1784()
                        && mc.world.isSpaceEmpty(mc.player, mc.player.getBoundingBox().stretch(0.0, -3.0, 0.0))) {
                        this.field15583 = System.currentTimeMillis() + 200L;
                     }

                     this.field15582 = InventoryUtils.method0554();
                     this.field15581 = local7752_2;
                     if (local7752_2 > 8) {
                        InventoryActionQueue.method2656().method2662(local7752_2, InventoryUtils.method0554(), 1);
                     } else {
                        InventoryUtils.method0553(local7752_2, ServerHelperModule.method0707().modeSetting.matchesOption("SpookyTime"));
                     }
                  }
               }
            }
         }
      }
   }

   public static TriggerBotModule method4593() {
      if (triggerBotModule == null) {
         triggerBotModule = new TriggerBotModule();
      }

      return triggerBotModule;
   }

   @Override
   protected void onDisable() {
      this.field15578 = null;
      this.field15583 = 0L;
      this.method4590();
   }

   private float[] method4594() {
      return new float[]{(float)this.method4585(), 0.0F, (float)this.method4585()};
   }

   private boolean method4597(LivingEntity livingEntity, float local7758_2) {
      if (livingEntity != mc.player && livingEntity.age >= 1 && !(livingEntity instanceof ArmorStandEntity)) {
         if (mc.player.squaredDistanceTo(livingEntity) > local7758_2 * local7758_2) {
            return false;
         }

         if (this.modeSetting.isSelected("Нет") && !EntityUtils.method2811(mc.player.getEyePos(), livingEntity.getEyePos())) {
            return false;
         }

         if (this.modeSetting.isSelected("Неполноценные") && !EntityUtils.method2793(mc.player.getEyePos(), livingEntity.getEyePos())) {
            return false;
         }

         if (livingEntity instanceof PlayerEntity) {
            if (FriendStore.method3431(((PlayerEntity)livingEntity).getGameProfile().name())) {
               return false;
            }

            if (!this.field15573.matchesOption("Игроки")) {
               return false;
            }

            if (livingEntity.getArmor() == 0 && !this.field15573.matchesOption("Голые")) {
               return false;
            }
         } else if (!this.field15573.matchesOption("Мобы")) {
            return false;
         }

         return !livingEntity.isInvulnerable() && livingEntity.isAlive();
      } else {
         return false;
      }
   }

   public LivingEntity method4599() {
      return this.field15578;
   }
}
