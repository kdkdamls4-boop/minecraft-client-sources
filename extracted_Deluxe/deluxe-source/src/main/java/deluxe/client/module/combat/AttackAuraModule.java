package deluxe.client.module.combat;

import deluxe.client.setting.BooleanSetting;
import deluxe.mixin.BossBarHudMixin;
import deluxe.client.event.EventListener;
import deluxe.client.storage.FriendStore;
import deluxe.client.setting.LegacyNumberSetting;
import deluxe.client.module.movement.MaceTestModule;
import deluxe.client.setting.ModeSetting;
import deluxe.client.module.Module;
import deluxe.client.module.ModuleCategory;
import deluxe.client.module.ModuleInfo;
import deluxe.client.event.MovementInputEvent;
import deluxe.client.setting.MultiSelectSetting;
import deluxe.client.event.PacketEvent;
import deluxe.client.event.PlayerTickEvent;
import deluxe.client.util.InventoryUtils;
import deluxe.client.module.combat.AdaptiveAuraRotation;
import deluxe.client.scripting.LuaRotationScripts;
import deluxe.client.module.combat.AuraRotationStrategy;
import deluxe.client.module.combat.CriticalHitUtils;
import deluxe.client.event.SubTickEvent;
import deluxe.client.module.combat.AttackController;
import deluxe.client.module.combat.TimedAuraRotation;
import deluxe.client.module.combat.CooldownAuraRotation;
import deluxe.client.setting.NumericTextSetting;
import deluxe.client.util.InventoryActionQueue;
import deluxe.client.util.EntityUtils;
import deluxe.client.util.ValueClamps;
import deluxe.client.module.combat.SmoothAuraRotation;
import deluxe.client.module.combat.ShieldBreaker;
import deluxe.client.module.combat.PredictiveAuraRotation;
import deluxe.client.util.InventorySlotManager;
import deluxe.client.RotationAngles;
import deluxe.client.RotationManager;
import deluxe.client.module.player.ServerHelperModule;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.client.gui.hud.ClientBossBar;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.MaceItem;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.Full;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.PositionAndOnGround;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;

@ModuleInfo(name = "AttackAura", description = "Автоатака по ближайшей цели с человечными ротациями.", category = ModuleCategory.COMBAT)
public final class AttackAuraModule extends Module {
   private static AttackAuraModule attackAuraModule;
   public ModeSetting field02093;
   public LegacyNumberSetting field02094;
   public LegacyNumberSetting field02095;
   public MultiSelectSetting field02096;
   public ModeSetting field02097;
   public ModeSetting field02098;
   public BooleanSetting field02099;
   public ModeSetting field02100;
   public MultiSelectSetting field02101;
   public MultiSelectSetting field02102;
   public BooleanSetting field02103;
   public BooleanSetting field02104;
   public NumericTextSetting numericTextSetting;
   private final CooldownAuraRotation cooldownAuraRotation = new CooldownAuraRotation();
   private final TimedAuraRotation timedAuraRotation = new TimedAuraRotation();
   private final ShieldBreaker shieldBreaker = new ShieldBreaker();
   private final AdaptiveAuraRotation adaptiveAuraRotation = new AdaptiveAuraRotation();
   private final PredictiveAuraRotation predictiveAuraRotation = new PredictiveAuraRotation();
   private final SmoothAuraRotation smoothAuraRotation = new SmoothAuraRotation();
   private static final Pattern field02112 = Pattern.compile("(\\d+)\\s*сек");
   private LivingEntity field02113;
   public int field02114;
   private boolean field02115;
   private boolean field02116;
   private LivingEntity field02117;
   private LivingEntity field02118;
   private int field02119 = -1;
   private int field02120 = -1;
   private int field02121;
   private boolean field02122;
   private long field02123;
   private final EventListener<PlayerTickEvent> field02124 = playerTickEvent -> {
      if (mc.player != null && mc.world != null) {
         if (!mc.player.isAlive()) {
            this.toggle();
         } else {
            if (System.currentTimeMillis() < this.field02123) {
               mc.player.setVelocity(Vec3d.ZERO);
            }

            CriticalHitUtils.method0961();
            this.method1001();
            if (MaceHelperModule.method1776().method1784()
               && MaceHelperModule.method1776().field05762.isChecked()
               && this.field02118 != null
               && this.field02118.isAlive()
               && !this.field02118.isRemoved()) {
               this.field02113 = this.field02118;
            } else if (this.field02113 == null || !this.method1028(this.field02113)) {
               this.method1031();
            }

            if (!MaceHelperModule.method1776().method1784() && this.field02113 != null) {
               this.field02118 = this.field02113;
            }

            this.method0995();
            if (this.method1018() == null || !this.method1018().method0844(this.field02113, (float)this.method1023())) {
               this.method1013();
            }
         }
      }
   };
   private final EventListener<SubTickEvent> field02125 = mouseScrollEvent -> {
      if (this.method1018() != null) {
         this.method1018().method0677(this.field02113, mouseScrollEvent.getTickProgress());
      }
   };
   private final EventListener<MovementInputEvent> field02126 = movementInputEvent -> {
      if (mc.player != null && System.currentTimeMillis() < this.field02123) {
         movementInputEvent.method4446(0.0F);
         movementInputEvent.method4449(0.0F);
         movementInputEvent.method4442(false);
         movementInputEvent.method4448(false);
      } else {
         if (mc.player != null && this.field02099.isChecked() && RotationManager.method0658().method0667()) {
            RotationManager.method0650(movementInputEvent, RotationAngles.method4366());
         }

         if (this.field02113 != null) {
            if (EntityUtils.method2816(this.field02113) < this.method1023()
               && AttackController.method1170(this.field02113, this.method1007(), this.field02098.getSelectedOption())) {
               movementInputEvent.method4442(false);
               movementInputEvent.method4446(0.0F);
               movementInputEvent.method4449(0.0F);
            } else {
               if (!this.field02099.isChecked() && RotationManager.method0658().method0667()) {
                  RotationManager.method0659(
                     movementInputEvent,
                     mc.player.getYaw(),
                     MathHelper.wrapDegrees(
                        (float)Math.toDegrees(Math.atan2(this.field02113.getZ() - mc.player.getZ(), this.field02113.getX() - mc.player.getX()))
                           - 90.0F
                     )
                  );
               }
            }
         }
      }
   };
   private final EventListener<PacketEvent> field02127 = packetEvent -> {
      if (mc.player != null && packetEvent.isOutgoing()) {
         if (System.currentTimeMillis() < this.field02123) {
            if (packetEvent.getPacket() instanceof PositionAndOnGround || packetEvent.getPacket() instanceof Full) {
               packetEvent.method4423();
            }
         }
      }
   };

   private boolean method0993() {
      if (mc.player.isTouchingWater() || mc.player.isInLava()) {
         return false;
      } else if (this.field02115 && !mc.player.isOnGround()) {
         return true;
      } else {
         return mc.player.isOnGround() ? false : CriticalHitUtils.method0969() >= 3.5;
      }
   }

   private void method0995() {
      if (this.field02113 != null) {
         if (!MaceHelperModule.method1776().method1783()) {
            boolean local1360_1 = AttackController.method1156(this.field02113, false, true, true, 0L, this.method1007());
            if (this.method1018() != null) {
               this.method1018().method0676(this.field02113, local1360_1, (float)this.method1023());
            } else if (this.field02093.isSelected("FunTime")) {
               this.cooldownAuraRotation.method1340(this.field02113, local1360_1, (float)this.method1023());
            } else if (this.field02093.isSelected("SpookyTime")) {
               if (this.method1030()) {
                  this.shieldBreaker.method3646(this.field02113, local1360_1, (float)this.method1023());
               } else {
                  this.timedAuraRotation.method1304(this.field02113, local1360_1, (float)this.method1023());
               }
            } else if (!this.field02093.isSelected("Legit")) {
               LuaRotationScripts.method0776(this.field02093.getSelectedOption(), this.field02113, local1360_1, (float)this.method1023());
            }
         }
      }
   }

   public float method0996() {
      return MathHelper.clamp(mc.player.handSwingTicks / this.method1017(8.0F, 12.0F), 0.0F, 1.0F);
   }

   public boolean method0998() {
      return (this.method1038() || CriticalHitUtils.method0962() && mc.player != null && !mc.player.isOnGround())
         && MaceItem.shouldDealAdditionalDamage(mc.player);
   }

   private void method1001() {
      if (!InventorySlotManager.method4176().method4179() && !InventoryActionQueue.method2656().method2665()) {
         if (this.field02103.isChecked() && mc.player != null) {
            if (mc.player.isOnGround() || mc.player.getVelocity().y >= 0.0) {
               this.field02116 = false;
            }

            this.method1032();
            if (this.method0993()) {
               int local1366_1 = InventoryUtils.method0539(Items.MACE);
               int local1366_2 = local1366_1 != -1 ? local1366_1 : InventoryUtils.method0557(Items.MACE);
               if (local1366_2 != -1
                  && !this.field02115
                  && !this.field02116
                  && !mc.player.getItemCooldownManager().isCoolingDown(mc.player.getInventory().getStack(local1366_2))) {
                  this.field02115 = true;
                  if (mc.player.getMainHandStack().isOf(Items.MACE)) {
                     return;
                  }

                  if (ServerHelperModule.method0707().modeSetting.matchesOption("SpookyTime")
                     && !MaceHelperModule.method1776().method1784()
                     && mc.world.isSpaceEmpty(mc.player, mc.player.getBoundingBox().stretch(0.0, -3.0, 0.0))
                     && !mc.world.isSpaceEmpty(mc.player, mc.player.getBoundingBox().stretch(0.0, -8.0, 0.0))) {
                     this.field02123 = System.currentTimeMillis() + 200L;
                  }

                  this.field02120 = InventoryUtils.method0554();
                  this.field02119 = local1366_2;
                  if (local1366_2 > 8) {
                     InventoryActionQueue.method2656().method2662(local1366_2, InventoryUtils.method0554(), 1);
                  } else {
                     InventoryUtils.method0553(local1366_2, ServerHelperModule.method0707().modeSetting.matchesOption("SpookyTime"));
                  }
               }
            } else {
               this.method1004();
            }
         } else {
            this.field02117 = null;
            this.method1004();
         }
      }
   }

   @Override
   protected void onDisable() {
      this.field02113 = null;
      this.field02118 = null;
      this.field02114 = 0;
      this.field02122 = false;
      this.field02123 = 0L;
      CriticalHitUtils.method0967();
      this.method1004();
      this.timedAuraRotation.method1305();
      this.shieldBreaker.method3647();
      this.method1035();
      this.predictiveAuraRotation.method3833();
      this.smoothAuraRotation.method3340();
   }

   public void method1002() {
      if (this.isEnabled() && this.field02093.isSelected("Legit") && this.field02113 != null) {
         this.smoothAuraRotation.method3341(this.field02113);
      }
   }

   private void method1004() {
      if (this.field02115 && mc.player != null) {
         this.field02115 = false;
         this.field02116 = true;
         if (this.field02119 > 8) {
            InventoryActionQueue.method2656().method2662(this.field02120, this.field02119, 1);
         } else if (this.field02120 != -1) {
            InventoryUtils.method0553(this.field02120, ServerHelperModule.method0707().modeSetting.matchesOption("SpookyTime"));
         }

         this.field02119 = -1;
         this.field02120 = -1;
      }
   }

   public LivingEntity method1005() {
      return this.field02113;
   }

   public float[] method1007() {
      if (MaceHelperModule.method1776().method1784()) {
         return new float[]{3.0F, 11.0F, 14.0F};
      }

      if (!this.method1038() || !MaceHelperModule.method1776().field05763.isChecked() && !MaceTestModule.method3323().field11670.isChecked()) {
         if (this.method1038()) {
            return CriticalHitUtils.method0964() > 2.0 ? new float[]{3.0F, 25.0F, 28.0F} : new float[]{3.0F, 15.0F, 18.0F};
         } else {
            return this.field02103.isChecked() && InventoryUtils.method0557(Items.MACE) != -1 && CriticalHitUtils.method0964() > 2.0
               ? new float[]{
                  this.field02094.getValue(), this.field02095.getValue() + 10.0F, this.field02094.getValue() + this.field02095.getValue() + 10.0F
               }
               : new float[]{this.field02094.getValue(), this.field02095.getValue(), this.field02094.getValue() + this.field02095.getValue()};
         }
      } else {
         return CriticalHitUtils.method0964() > 2.0 ? new float[]{3.0F, 21.0F, 24.0F} : new float[]{3.0F, 11.0F, 14.0F};
      }
   }

   private void method1013() {
      if (this.field02113 != null) {
         if (!(EntityUtils.method2816(this.field02113) >= this.method1023())) {
            if (this.field02101.matchesOption("Ешь") && mc.player.isUsingItem()) {
               ItemStack activeItem = mc.player.getActiveItem();
               if (!activeItem.isEmpty() && (activeItem.get(DataComponentTypes.FOOD) != null || activeItem.get(DataComponentTypes.POTION_CONTENTS) != null)) {
                  return;
               }
            }

            if (!this.field02101.matchesOption("Открыт инвентарь") || mc.currentScreen == null) {
               AttackController.method1164(this.field02113, false, true, false);
               if (System.currentTimeMillis() >= this.field02123) {
                  if (AttackController.method1156(this.field02113, true, true, true, 0L, this.method1007())) {
                     if (!this.field02100.isSelected("Неполноценные") || EntityUtils.method2808(this.field02113, this.method1023())) {
                        if (this.field02100.isSelected("Shard")
                           && mc.player.networkHandler != null
                           && !EntityUtils.method2811(mc.player.getEyePos(), this.field02113.getEyePos())) {
                           ArrayList<BlockPos> local1379_1 = new ArrayList<>();
                           Vec3d eyePos = mc.player.getEyePos();
                           Vec3d vec3d = EntityUtils.method2810(mc.player.getYaw(), mc.player.getPitch()).normalize();
                           double local1379_4 = this.field02113
                              .getBoundingBox()
                              .raycast(
                                 eyePos,
                                 eyePos.add(
                                    vec3d.multiply(Math.max(mc.player.distanceTo(this.field02113), this.field02094.getValue() + 2.0F))
                                 )
                              )
                              .map(eyePos::distanceTo)
                              .orElse(EntityUtils.method2816(this.field02113));

                           for (double local1379_6 = 0.0; local1379_6 <= local1379_4; local1379_6 += 0.1) {
                              BlockPos blockPos = BlockPos.ofFloored(eyePos.add(vec3d.multiply(local1379_6)));
                              if (!local1379_1.contains(blockPos)) {
                                 local1379_1.add(blockPos);
                                 if (!mc.world.getBlockState(blockPos).isAir()
                                    && !mc.world.getBlockState(blockPos).getCollisionShape(mc.world, blockPos).isEmpty()) {
                                    BlockHitResult blockHitResult = mc.world
                                       .raycast(
                                          new RaycastContext(
                                             eyePos.add(vec3d.multiply(local1379_6 - 0.2)),
                                             eyePos.add(vec3d.multiply(local1379_6 + 0.2)),
                                             ShapeType.COLLIDER,
                                             FluidHandling.NONE,
                                             mc.player
                                          )
                                       );
                                    Direction direction = blockHitResult.getType() == Type.BLOCK ? blockHitResult.getSide() : Direction.UP;
                                    mc.player
                                       .networkHandler
                                       .sendPacket(new PlayerActionC2SPacket(Action.START_DESTROY_BLOCK, blockPos, direction));
                                    mc.player
                                       .networkHandler
                                       .sendPacket(new PlayerActionC2SPacket(Action.STOP_DESTROY_BLOCK, blockPos, direction));
                                 }
                              }
                           }
                        }

                        int local1379_1 = this.field02104.isChecked() && !this.numericTextSetting.getText().isBlank()
                           ? InventoryUtils.method0555(
                              itemStack -> !itemStack.isEmpty()
                                 && itemStack.getName().getString().toLowerCase().contains(this.numericTextSetting.getText().toLowerCase().trim())
                           )
                           : -1;
                        if (AttackController.method1147(
                           this.field02113,
                           local1379_1 == -1 ? null : () -> InventoryUtils.method0536(local1379_1, Hand.OFF_HAND),
                           local1379_1 == -1 ? null : () -> {
                              mc.player.currentScreenHandler.nextRevision();
                              InventoryUtils.method0536(local1379_1, Hand.OFF_HAND);
                           },
                           Hand.MAIN_HAND,
                           false
                        )) {
                           this.field02114++;
                           CriticalHitUtils.method0971();
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private boolean method1014(LivingEntity playerEntity, float local1380_2) {
      if (playerEntity != mc.player && playerEntity.age >= 1 && !(playerEntity instanceof ArmorStandEntity)) {
         if (mc.player.squaredDistanceTo(playerEntity) > local1380_2 * local1380_2) {
            return false;
         }

         if (this.field02100.isSelected("Нет") && !EntityUtils.method2811(mc.player.getEyePos(), playerEntity.getEyePos())) {
            return false;
         }

         if (playerEntity instanceof PlayerEntity) {
            if (FriendStore.method3431(((PlayerEntity)playerEntity).getGameProfile().name())) {
               return false;
            }

            if (this.method1024((PlayerEntity)playerEntity)) {
               return this.field02096.matchesOption("Боты") && !playerEntity.isInvulnerable() && playerEntity.isAlive();
            }

            if (!this.field02096.matchesOption("Игроки")) {
               return false;
            }

            if (playerEntity.getArmor() == 0 && !this.field02096.matchesOption("Голые")) {
               return false;
            }
         } else if (!this.field02096.matchesOption("Мобы")) {
            return false;
         }

         return !playerEntity.isInvulnerable() && playerEntity.isAlive();
      } else {
         return false;
      }
   }

   public float method1017(float local1383_1, float local1383_2) {
      return ValueClamps.method3114(local1383_2, local1383_1, new SecureRandom().nextFloat());
   }

   private AuraRotationStrategy method1018() {
      return switch (this.field02093.getSelectedOption()) {
         case "Aim" -> this.adaptiveAuraRotation;
         default -> null;
      };
   }

   public boolean method1019() {
      return this.field02102.matchesOption("Умные криты");
   }

   private AttackAuraModule() {
      this.field02093 = this.addSetting(new ModeSetting("Тип наведения", "Стиль движения прицела к цели.", "FunTime", "FunTime", "SpookyTime", "Legit"));
      this.field02094 = this.addSetting(new LegacyNumberSetting("Радиус удара", "На какой дистанции наносить удар.", 3.0, 3.0, 6.0, 0.1));
      this.field02095 = this.addSetting(
         new LegacyNumberSetting("Радиус прицеливания", "На сколько блоков дальше радиуса удара вести цель.", 5.0, 0.0, 10.0, 0.1)
      );
      this.field02096 = this.addSetting(
         new MultiSelectSetting("Кого атаковать", "Каких существ считать целью.", new String[]{"Игроки", "Голые", "Мобы", "Боты"}, "Игроки", "Голые")
      );
      this.field02097 = this.addSetting(
         new ModeSetting(
            "Сортировка целей", "По какому признаку выбирать цель из подходящих.", "Наводка", "Наводка", "Адаптивный", "Дистанция", "Здоровье"
         )
      );
      this.field02098 = this.addSetting(new ModeSetting("Тип спринта", "Как гасить спринт перед ударом для крита.", "Легитный", "Легитный", "Быстрый"));
      this.field02099 = this.addSetting(new BooleanSetting("Свободная коррекция движения", "WASD ведёт туда, куда смотришь ты, а не ротация.", true));
      this.field02100 = this.addSetting(
         new ModeSetting("Бить через блоки", "Разрешить наведение сквозь блоки.", "Нет", "Нет", "Обычные", "Неполноценные", "Shard")
      );
      this.field02101 = this.addSetting(new MultiSelectSetting("Не бить если", "Условия паузы автоатаки.", new String[]{"Ешь", "Открыт инвентарь"}));
      this.field02102 = this.addSetting(new MultiSelectSetting("Настройки", "Дополнительные опции удара.", new String[]{"Умные криты"}));
      this.field02103 = this.addSetting(
         new BooleanSetting("Авто-булава", "Сама берёт булаву, бьёт смэшем при падении на цель и возвращает прежний предмет.", false)
      );
      this.field02104 = this.addSetting(
         new BooleanSetting("Максимальный урон", "На момент удара уводит выбранный предмет в левую руку и сразу возвращает обратно.", false)
      );
      this.numericTextSetting = this.addSetting(
         new NumericTextSetting("Предмет для свапа", "Часть названия предмета, который уходит в левую руку на удар.", "карател", 32)
      );
      this.numericTextSetting.withVisibility(() -> this.field02104.isChecked());
      this.field02094
         .automaticWhen(
            () -> ServerHelperModule.method0707().isEnabled()
               && ServerHelperModule.method0707().modeSetting.isSelected("SpookyTime")
               && this.field02094.getValue() > 3.65F
         );
   }

   private double method1020(LivingEntity livingEntity) {
      return Math.hypot(
         MathHelper.wrapDegrees(EntityUtils.method2814(livingEntity.getEyePos(), mc.player.getEyePos())[0] - mc.player.getYaw()),
         EntityUtils.method2814(livingEntity.getEyePos(), mc.player.getEyePos())[1] - mc.player.getPitch()
      );
   }

   public double method1023() {
      return mc.player != null && mc.player.getMainHandStack().isOf(Items.MACE) ? 3.0 : Math.max(3.0, this.field02094.getValue());
   }

   private boolean method1024(PlayerEntity playerEntity) {
      if (mc.getNetworkHandler() == null) {
         return false;
      }

      PlayerListEntry playerListEntry = mc.getNetworkHandler().getPlayerListEntry(playerEntity.getUuid());
      String local1391_3 = playerEntity.getGameProfile().name();
      return playerListEntry == null || local1391_3 == null || !local1391_3.matches("\\w{3,16}") || !local1391_3.equals(playerListEntry.getProfile().name());
   }

   private boolean method1028(LivingEntity playerEntity) {
      return this.method1014(playerEntity, this.method1007()[2]);
   }

   public static AttackAuraModule method1029() {
      if (attackAuraModule == null) {
         attackAuraModule = new AttackAuraModule();
      }

      return attackAuraModule;
   }

   private boolean method1030() {
      if (mc.player != null && mc.world != null) {
         ScoreboardObjective objectiveForSlot = mc.world.getScoreboard().getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR);
         if (objectiveForSlot != null) {
            String[] stringArray = objectiveForSlot.getDisplayName().getString().split("-");
            if (stringArray.length > 1 && stringArray[1].replaceAll("\\D", "").matches("70[24]")) {
               return true;
            }
         }

         if (!mc.world.getRegistryKey().getValue().getPath().equals("arenas")) {
            return false;
         }

         for (ClientBossBar clientBossBar : ((BossBarHudMixin)mc.inGameHud.getBossBarHud()).getEvents().values()) {
            Matcher matcher = field02112.matcher(clientBossBar.getName().getString().replaceAll("(?i)§[0-9a-fk-or]", ""));
            if (clientBossBar.getPercent() > 0.05F && matcher.find() && Integer.parseInt(matcher.group(1)) / clientBossBar.getPercent() < 60.0F) {
               return true;
            }
         }

         return false;
      } else {
         return false;
      }
   }

   private void method1031() {
      this.field02113 = null;
      ArrayList<LivingEntity> local1398_1 = new ArrayList<>();

      for (Entity entity : mc.world.getEntities()) {
         if (entity instanceof LivingEntity playerEntity && this.method1028(playerEntity)) {
            local1398_1.add(playerEntity);
         }
      }

      if (!local1398_1.isEmpty()) {
         local1398_1.sort(
            this.field02097.isSelected("Адаптивный")
               ? Comparator.<LivingEntity>comparingDouble(livingEntity -> -livingEntity.getArmor())
                  .thenComparingDouble(livingEntity2 -> this.method1020(livingEntity2))
                  .thenComparingDouble(entity2 -> EntityUtils.method2816(entity2))
                  .thenComparingDouble(livingEntity3 -> EntityUtils.method2790(livingEntity3))
               : (
                  this.field02097.isSelected("Дистанция")
                     ? Comparator.<LivingEntity>comparingDouble(entity3 -> EntityUtils.method2816(entity3))
                        .thenComparingDouble(livingEntity4 -> EntityUtils.method2790(livingEntity4))
                     : (
                        this.field02097.isSelected("Здоровье")
                           ? Comparator.<LivingEntity>comparingDouble(livingEntity5 -> EntityUtils.method2790(livingEntity5))
                              .thenComparingDouble(entity4 -> EntityUtils.method2816(entity4))
                           : Comparator.<LivingEntity>comparingDouble(livingEntity6 -> this.method1020(livingEntity6))
                              .thenComparingDouble(entity5 -> EntityUtils.method2816(entity5))
                              .thenComparingDouble(livingEntity7 -> EntityUtils.method2790(livingEntity7))
                              .thenComparingDouble(livingEntity8 -> -livingEntity8.getArmor())
                     )
               )
         );
         this.field02113 = local1398_1.get(0);
      }

      this.timedAuraRotation.method1305();
      this.shieldBreaker.method3647();
      this.method1035();
      this.predictiveAuraRotation.method3833();
      this.smoothAuraRotation.method3340();
   }

   private void method1032() {
      this.field02117 = null;
      double local1399_1 = Double.MAX_VALUE;

      for (Entity entity : mc.world.getEntities()) {
         if (entity instanceof LivingEntity playerEntity
            && this.method1014(playerEntity, 64.0F)
            && !playerEntity.isBlocking()
            && !(Math.hypot(playerEntity.getX() - mc.player.getX(), playerEntity.getZ() - mc.player.getZ()) > 6.0)
            && mc.player.squaredDistanceTo(playerEntity) < local1399_1) {
            local1399_1 = mc.player.squaredDistanceTo(playerEntity);
            this.field02117 = playerEntity;
         }
      }
   }

   private void method1035() {
      this.adaptiveAuraRotation.l_();
   }

   public boolean method1037() {
      return this.field02103.isChecked() && this.field02115
         || MaceHelperModule.method1776().method1775()
         || mc.player != null && mc.player.getMainHandStack().isOf(Items.MACE);
   }

   @Override
   protected void onEnable() {
      this.timedAuraRotation.method1305();
      this.shieldBreaker.method3647();
      this.method1035();
      this.predictiveAuraRotation.method3833();
      this.smoothAuraRotation.method3340();
   }

   public boolean method1038() {
      return this.method1037() && mc.player != null && !mc.player.isOnGround();
   }
}
