package deluxe.client.module.combat;

import deluxe.client.setting.BooleanSetting;
import deluxe.client.event.EventListener;
import deluxe.client.storage.FriendStore;
import deluxe.client.setting.LegacyNumberSetting;
import deluxe.client.util.MathUtils;
import deluxe.client.setting.ModeSetting;
import deluxe.client.module.Module;
import deluxe.client.module.ModuleCategory;
import deluxe.client.module.ModuleInfo;
import deluxe.client.event.MovementInputEvent;
import deluxe.client.setting.MultiSelectSetting;
import deluxe.client.event.PlayerTickEvent;
import deluxe.client.module.combat.AttackController;
import deluxe.client.util.EntityUtils;
import deluxe.client.util.MouseSensitivity;

import java.util.ArrayList;
import java.util.Comparator;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

@ModuleInfo(
   name = "AimAssist",
   description = "Ведёт прицел к цели в пределах FOV, на входе в снап-FOV кидает его в цель резким рывком и бьёт.",
   category = ModuleCategory.COMBAT
)
public final class AimAssistModule extends Module {
   private static AimAssistModule aimAssistModule;
   public ModeSetting field01609;
   public LegacyNumberSetting field01610;
   public LegacyNumberSetting field01611;
   public LegacyNumberSetting field01612;
   public LegacyNumberSetting field01613;
   public LegacyNumberSetting field01614;
   public LegacyNumberSetting field01615;
   public LegacyNumberSetting field01616;
   public BooleanSetting field01617;
   public BooleanSetting field01618;
   public MultiSelectSetting multiSelectSetting;
   public ModeSetting field01620;
   public ModeSetting field01621;
   private LivingEntity field01622;
   private float field01623;
   private float field01624;
   private float field01625;
   private float field01626;
   private boolean field01627 = true;
   private long field01628;
   private final EventListener<MovementInputEvent> field01629 = movementInputEvent -> {
      if (this.field01622 != null && this.method0881()) {
         if (!(EntityUtils.method2816(this.field01622) >= this.field01611.getValue())
            && AttackController.method1170(this.field01622, new float[]{this.field01611.getValue(), 0.0F, this.field01611.getValue()}, "Легитный")) {
            movementInputEvent.method4446(0.0F);
            movementInputEvent.method4449(0.0F);
         }
      }
   };
   private final EventListener<PlayerTickEvent> field01630 = playerTickEvent -> {
      if (mc.player != null && mc.world != null && mc.player.isAlive()) {
         if (this.field01617.isChecked()
            && this.field01622 != null
            && this.method0884(this.field01622) <= this.field01615.getValue()
            && AttackController.method1156(
               this.field01622, true, true, this.method0881(), 0L, new float[]{this.field01611.getValue(), 0.0F, this.field01611.getValue()}
            )) {
            AttackController.method1147(this.field01622, null, null, Hand.MAIN_HAND, false);
         }

         if (this.field01622 == null || !this.method0890(this.field01622)) {
            ArrayList<LivingEntity> local1207_2 = new ArrayList<>();

            for (Entity entity : mc.world.getEntities()) {
               if (entity instanceof LivingEntity livingEntity && this.method0890(livingEntity)) {
                  local1207_2.add(livingEntity);
               }
            }

            if (!local1207_2.isEmpty()) {
               local1207_2.sort(
                  this.field01620.isSelected("Адаптивный")
                     ? Comparator.<LivingEntity>comparingDouble(livingEntity2 -> -livingEntity2.getArmor())
                        .thenComparingDouble(livingEntity3 -> this.method0884(livingEntity3))
                        .thenComparingDouble(entity2 -> EntityUtils.method2816(entity2))
                        .thenComparingDouble(livingEntity4 -> EntityUtils.method2790(livingEntity4))
                     : (
                        this.field01620.isSelected("Дистанция")
                           ? Comparator.<LivingEntity>comparingDouble(entity3 -> EntityUtils.method2816(entity3))
                              .thenComparingDouble(livingEntity5 -> EntityUtils.method2790(livingEntity5))
                           : (
                              this.field01620.isSelected("Здоровье")
                                 ? Comparator.<LivingEntity>comparingDouble(livingEntity6 -> EntityUtils.method2790(livingEntity6))
                                    .thenComparingDouble(entity4 -> EntityUtils.method2816(entity4))
                                 : Comparator.<LivingEntity>comparingDouble(livingEntity7 -> this.method0884(livingEntity7))
                                    .thenComparingDouble(entity5 -> EntityUtils.method2816(entity5))
                                    .thenComparingDouble(livingEntity8 -> EntityUtils.method2790(livingEntity8))
                                    .thenComparingDouble(livingEntity9 -> -livingEntity9.getArmor())
                           )
                     )
               );
               this.field01622 = local1207_2.get(0);
            } else {
               this.field01622 = null;
            }

            this.field01623 = this.field01624 = 0.0F;
            this.field01625 = this.field01626 = 0.0F;
            this.field01627 = true;
         }
      } else {
         this.field01622 = null;
      }
   };

   public boolean method0881() {
      return this.field01617.isChecked() && this.field01618.isChecked();
   }

   @Override
   protected void onDisable() {
      this.field01622 = null;
      this.field01623 = this.field01624 = 0.0F;
      this.field01625 = this.field01626 = 0.0F;
      this.field01627 = true;
   }

   public static AimAssistModule method0883() {
      if (aimAssistModule == null) {
         aimAssistModule = new AimAssistModule();
      }

      return aimAssistModule;
   }

   private double method0884(LivingEntity livingEntity) {
      return Math.hypot(
         MathHelper.wrapDegrees(EntityUtils.method2814(livingEntity.getEyePos(), mc.player.getEyePos())[0] - mc.player.getYaw()),
         EntityUtils.method2814(livingEntity.getEyePos(), mc.player.getEyePos())[1] - mc.player.getPitch()
      );
   }

   private boolean method0890(LivingEntity livingEntity) {
      if (livingEntity != mc.player && livingEntity.age >= 1 && !(livingEntity instanceof ArmorStandEntity)) {
         if (EntityUtils.method2816(livingEntity) > this.field01611.getValue()) {
            return false;
         }

         if (this.method0884(livingEntity) > this.field01610.getValue()) {
            return false;
         }

         if (this.field01621.isSelected("Нет") && !EntityUtils.method2811(mc.player.getEyePos(), livingEntity.getEyePos())) {
            return false;
         }

         if (this.field01621.isSelected("Неполноценные") && !EntityUtils.method2793(mc.player.getEyePos(), livingEntity.getEyePos())) {
            return false;
         }

         if (livingEntity instanceof PlayerEntity) {
            if (FriendStore.method3431(((PlayerEntity)livingEntity).getGameProfile().name())) {
               return false;
            }

            if (!this.multiSelectSetting.matchesOption("Игроки")) {
               return false;
            }

            if (livingEntity.getArmor() == 0 && !this.multiSelectSetting.matchesOption("Голые")) {
               return false;
            }
         } else if (!this.multiSelectSetting.matchesOption("Мобы")) {
            return false;
         }

         return !livingEntity.isInvulnerable() && livingEntity.isAlive();
      } else {
         return false;
      }
   }

   public void method0896(double local1197_1, double local1197_3) {
      if (this.isEnabled() && this.field01622 != null && mc.player != null && mc.world != null) {
         float local1197_5 = Math.max(1.0F, Math.min((float)(System.currentTimeMillis() - this.field01628), 100.0F)) / 50.0F;
         this.field01628 = System.currentTimeMillis();
         Vec3d eyePos = mc.player.getEyePos();
         Box box = this.field01622
            .getBoundingBox()
            .contract(this.field01622.getWidth() * 0.25, this.field01622.getHeight() * 0.2, this.field01622.getWidth() * 0.25);
         Vec3d vec3d = eyePos.add(
            EntityUtils.method2810(mc.player.getYaw(), mc.player.getPitch()).multiply(eyePos.distanceTo(box.getCenter()))
         );
         float[] floatArray = EntityUtils.method2814(
            new Vec3d(
               MathHelper.clamp(vec3d.x, box.minX, box.maxX),
               MathHelper.clamp(vec3d.y, box.minY, box.maxY),
               MathHelper.clamp(vec3d.z, box.minZ, box.maxZ)
            ),
            eyePos
         );
         float local1197_10 = MathHelper.wrapDegrees(floatArray[0] - mc.player.getYaw());
         float local1197_11 = floatArray[1] - mc.player.getPitch();
         if (Math.hypot(local1197_10, local1197_11) > this.field01615.getValue()) {
            this.field01627 = true;
         }

         if (EntityUtils.method2803(this.field01622, this.field01611.getValue(), mc.player.getYaw(), mc.player.getPitch())) {
            this.field01627 = false;
         }

         float local1197_12 = MathUtils.clampUnit(
            (this.field01610.getValue() - (float)Math.hypot(local1197_10, local1197_11)) / Math.max(1.0F, this.field01610.getValue() * 0.25F)
         );
         if (!(local1197_12 <= 0.0F)) {
            if (this.field01609.isSelected("Мышь")) {
               float local1197_13 = (float)Math.hypot(local1197_1 * 0.15, local1197_3 * 0.15);
               float local1197_14 = local1197_13 * this.method0898() * (this.field01614.getValue() / 100.0F) * local1197_12;
               this.field01625 = this.field01625 + MathHelper.clamp(local1197_10, -local1197_14, local1197_14);
               this.field01626 = this.field01626 + MathHelper.clamp(local1197_11, -local1197_14 * 0.6F, local1197_14 * 0.6F);
            } else {
               float local1197_13 = this.field01614.getValue() / 100.0F;
               float local1197_14 = MathHelper.clamp(local1197_10 * local1197_13, -this.field01612.getValue(), this.field01612.getValue()) * local1197_12;
               float local1197_15 = MathHelper.clamp(local1197_11 * local1197_13, -this.field01613.getValue(), this.field01613.getValue()) * local1197_12;
               this.field01623 = this.field01623 + (local1197_14 - this.field01623) * MathUtils.clampUnit(0.4F * local1197_5);
               this.field01624 = this.field01624 + (local1197_15 - this.field01624) * MathUtils.clampUnit(0.4F * local1197_5);
               this.field01625 = this.field01625 + MathHelper.clamp(this.field01623 * local1197_5, -Math.abs(local1197_10), Math.abs(local1197_10));
               this.field01626 = this.field01626 + MathHelper.clamp(this.field01624 * local1197_5, -Math.abs(local1197_11), Math.abs(local1197_11));
            }

            float local1197_13 = MouseSensitivity.method4410(this.field01625);
            float local1197_14 = MouseSensitivity.method4410(this.field01626);
            this.field01625 -= local1197_13;
            this.field01626 -= local1197_14;
            mc.player.setYaw(mc.player.getYaw() + local1197_13);
            mc.player.setPitch(MathHelper.clamp(mc.player.getPitch() + local1197_14, -90.0F, 90.0F));
            mc.player.lastYaw += local1197_13;
            mc.player.lastPitch = MathHelper.clamp(mc.player.lastPitch + local1197_14, -90.0F, 90.0F);
         }
      }
   }

   private float method0898() {
      float attackCooldownProgress = mc.player.getAttackCooldownProgress(1.0F);
      double attributeValue = mc.player.getAttributeValue(EntityAttributes.ATTACK_SPEED);
      if (attributeValue > 1.6) {
         attackCooldownProgress *= (float)(1.6 / attributeValue);
      }

      float local1199_4;
      if (attributeValue < 2.0) {
         local1199_4 = 2.70158F * attackCooldownProgress * attackCooldownProgress * attackCooldownProgress - 1.70158F * attackCooldownProgress * attackCooldownProgress;
      } else if (attackCooldownProgress < 0.5F) {
         local1199_4 = 8.0F * attackCooldownProgress * attackCooldownProgress * attackCooldownProgress * attackCooldownProgress;
      } else {
         local1199_4 = 1.0F - 8.0F * (attackCooldownProgress - 1.0F) * (attackCooldownProgress - 1.0F) * (attackCooldownProgress - 1.0F) * (attackCooldownProgress - 1.0F);
      }

      return (1.0F + 29.0F * local1199_4) / 50.0F;
   }

   private AimAssistModule() {
      this.field01609 = this.addSetting(
         new ModeSetting("Тип доводки", "Скорость ведёт прицел сама, Мышь доворачивает только пока ты крутишь мышкой.", "Скорость", "Скорость", "Мышь")
      );
      this.field01610 = this.addSetting(new LegacyNumberSetting("FOV", "Максимальный угол от прицела до цели, за которым доводки нет.", 45.0, 5.0, 180.0, 1.0));
      this.field01611 = this.addSetting(new LegacyNumberSetting("Радиус", "На какой дистанции держать цель.", 4.0, 1.0, 8.0, 0.1));
      this.field01612 = this.addSetting(new LegacyNumberSetting("Скорость по горизонтали", "Сколько градусов за тик выбирать по яву.", 9.0, 0.5, 40.0, 0.5));
      this.field01613 = this.addSetting(new LegacyNumberSetting("Скорость по вертикали", "Сколько градусов за тик выбирать по питчу.", 6.0, 0.0, 40.0, 0.5));
      this.field01614 = this.addSetting(
         new LegacyNumberSetting(
            "Сила доводки",
            "Какую долю нужного доворота отрабатывать: в Скорость тянет прицел мягче или резче, в Мышь масштабирует твоё движение мышкой.",
            100.0,
            10.0,
            300.0,
            5.0
         )
      );
      this.field01615 = this.addSetting(
         new LegacyNumberSetting("Радиус удара", "Внутри этого угла от прицела цель считается взятой и по ней идёт удар.", 15.0, 0.0, 90.0, 1.0)
      );
      this.field01616 = this.addSetting(
         new LegacyNumberSetting("Плавность снапа", "За сколько миллисекунд рывок дотягивает прицел до хитбокса. 0 — мгновенный бросок.", 60.0, 0.0, 300.0, 5.0)
      );
      this.field01616.withVisibility(() -> false);
      this.field01617 = this.addSetting(new BooleanSetting("Бить по цели", "Бить цель по готовности кулдауна, когда она внутри радиуса удара.", true));
      this.field01618 = this.addSetting(
         new BooleanSetting("Умные криты", "Ждать момент крита и гасить спринт перед ударом, а не бить по готовности кулдауна.", true)
      );
      this.field01618.withVisibility(() -> this.field01617.isChecked());
      this.multiSelectSetting = this.addSetting(
         new MultiSelectSetting("Кого доводить", "Каких существ считать целью.", new String[]{"Игроки", "Голые", "Мобы"}, "Игроки", "Голые")
      );
      this.field01620 = this.addSetting(
         new ModeSetting(
            "Сортировка целей", "По какому признаку выбирать цель из подходящих.", "Наводка", "Наводка", "Адаптивный", "Дистанция", "Здоровье"
         )
      );
      this.field01621 = this.addSetting(
         new ModeSetting("Доводить через блоки", "Разрешить наведение сквозь блоки.", "Нет", "Нет", "Обычные", "Неполноценные")
      );
      this.field01612.withVisibility(() -> this.field01609.isSelected("Скорость"));
      this.field01613.withVisibility(() -> this.field01609.isSelected("Скорость"));
   }

   public LivingEntity method0903() {
      return this.field01622;
   }
}
