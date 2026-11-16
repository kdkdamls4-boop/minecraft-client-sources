package deluxe.client.module.movement;

import deluxe.client.module.combat.AttackAuraModule;
import deluxe.client.setting.BooleanSetting;
import deluxe.client.event.EventListener;
import deluxe.client.util.ItemCooldownAccess;
import deluxe.client.setting.KeybindSetting;
import deluxe.client.setting.LegacyNumberSetting;
import deluxe.mixin.LivingEntityMixin;
import deluxe.client.module.Module;
import deluxe.client.module.ModuleCategory;
import deluxe.client.module.ModuleInfo;
import deluxe.client.event.MovementInputEvent;
import deluxe.client.event.PacketEvent;
import deluxe.client.event.PlayerTickEvent;
import deluxe.client.util.InventoryUtils;
import deluxe.client.ui.hud.NotificationHud;
import deluxe.client.util.InventorySlotManager;
import deluxe.client.RotationAngles;
import deluxe.client.RotationManager;
import deluxe.client.RotationSpeed;
import deluxe.client.module.player.ServerHelperModule;

import java.util.Locale;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;

@ModuleInfo(name = "MaceTest", description = "Порт MaceHelper из devdeluxe: заряд ветра по бинду и булава в падение.", category = ModuleCategory.MOVEMENT)
public final class MaceTestModule extends Module {
   private static MaceTestModule maceTestModule;
   private KeybindSetting keybindSetting;
   public BooleanSetting field11670;
   public LegacyNumberSetting legacyNumberSetting;
   public BooleanSetting field11672;
   public BooleanSetting field11673;
   private boolean field11674;
   private boolean field11675;
   private boolean field11676;
   private boolean field11677;
   private boolean field11678;
   private boolean field11679;
   private boolean field11680;
   private boolean field11681;
   private long field11682;
   private int field11683;
   private int field11684;
   private long field11685;
   private float field11686;
   private float field11687;
   private int field11688 = -1;
   private int field11689 = -1;
   private final EventListener<PlayerTickEvent> field11690 = playerTickEvent -> {
      if (mc.player != null) {
         if (this.keybindSetting.isBound()
            && this.keybindSetting.getBinding().method2749(mc.getWindow().getHandle())
            && !this.field11674
            && mc.currentScreen == null
            && InventoryUtils.method0557(Items.WIND_CHARGE) != -1) {
            this.field11675 = true;
            this.field11676 = false;
         }

         this.field11674 = this.keybindSetting.isBound() && this.keybindSetting.getBinding().method2749(mc.getWindow().getHandle());
         if (this.field11683 > 0 && --this.field11683 == 0 && mc.player.isOnGround()) {
            ((LivingEntityMixin)mc.player).setNoJumpDelay(0);
            this.field11684 = 1;
         }

         if (this.field11679) {
            this.field11679 = false;
            this.field11676 = true;
            this.method3318();
         }

         if (this.field11678) {
            this.field11678 = false;
            this.method3326();
         }

         if (this.field11680) {
            if (!mc.player.isOnGround()) {
               this.field11681 = true;
            } else if (this.field11681) {
               this.field11680 = false;
               this.field11681 = false;
               this.method3326();
            }
         }

         if (this.field11677) {
            if (mc.player.isOnGround()) {
               this.field11677 = false;
               this.field11678 = true;
            }
         } else if (this.field11672.isChecked() && mc.currentScreen == null && !this.field11680) {
            if (!mc.player.isOnGround() && !(mc.player.fallDistance < 2.3F)) {
               if (mc.player.getMainHandStack().getItem() != Items.MACE) {
                  if (InventoryUtils.method0557(Items.MACE) != -1) {
                     this.field11677 = true;
                     this.method3322();
                  }
               }
            }
         }
      }
   };
   private final EventListener<MovementInputEvent> field11691 = movementInputEvent -> {
      if (mc.player != null) {
         if (this.field11675) {
            this.field11682 = System.currentTimeMillis();
            if (AttackAuraModule.method1029().isEnabled() && AttackAuraModule.method1029().method1005() != null) {
               RotationManager.method0658().method0653();
            }

            if (!ScreenWalkModule.method0991().method0988()) {
               RotationManager.method0658()
                  .method0652(
                     new RotationAngles(mc.player.getYaw(), 90.0F),
                     new RotationSpeed(360.0F, ServerHelperModule.method0707().modeSetting.isSelected("SpookyTime") ? 360.0F : 30.0F, 20.0F, 20.0F)
                  );
               if (!ServerHelperModule.method0707().modeSetting.isSelected("SpookyTime") && Math.abs(mc.player.getPitch() - 90.0F) >= 3.0F) {
                  return;
               }
            }

            this.field11675 = false;
            this.method3324();
         } else if (this.field11684 > 0) {
            this.field11684--;
            movementInputEvent.method4442(true);
         }
      }
   };
   private final EventListener<PacketEvent> field11692 = packetEvent -> {
      if (mc.player != null) {
         if (packetEvent.isOutgoing()
            && System.currentTimeMillis() < this.field11685
            && packetEvent.getPacket() instanceof PlayerInteractItemC2SPacket playerInteractItemC2SPacket) {
            packetEvent.setPacket(new PlayerInteractItemC2SPacket(playerInteractItemC2SPacket.getHand(), playerInteractItemC2SPacket.getSequence(), this.field11686, this.field11687));
            this.field11685 = 0L;
            this.field11683 = 1;
         }
      }
   };

   @Override
   protected void onDisable() {
      this.field11675 = false;
      this.field11676 = false;
      this.field11677 = false;
      this.field11678 = false;
      this.field11679 = false;
      this.field11685 = 0L;
      this.field11683 = 0;
      this.field11684 = 0;
      this.field11680 = false;
      this.field11681 = false;
      if (mc.player != null) {
         this.method3326();
      }
   }

   private boolean method3316(float local5416_1, float local5416_2) {
      return mc.world
            .raycast(
               new RaycastContext(
                  mc.player.getEyePos(),
                  mc.player.getEyePos().add(new RotationAngles(local5416_2, local5416_1).method4363().multiply(1.5)),
                  ShapeType.COLLIDER,
                  FluidHandling.NONE,
                  mc.player
               )
            )
            .getType()
         == Type.BLOCK;
   }

   public boolean method3317() {
      return this.field11675 || System.currentTimeMillis() - this.field11682 < 300L;
   }

   private void method3318() {
      if (mc.player != null && this.field11673.isChecked() && !this.field11680 && !this.field11677) {
         int local5418_1 = InventoryUtils.method0539(Items.MACE) != -1
            ? InventoryUtils.method0539(Items.MACE)
            : InventoryUtils.method0557(Items.MACE);
         if (local5418_1 != -1) {
            if (mc.player.getItemCooldownManager().isCoolingDown(mc.player.getInventory().getStack(local5418_1))) {
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
                                       .remainingCooldownTicks(mc.player.getInventory().getStack(local5418_1))
                                    / 20.0F
                              )
                           )
                           .setStyle(Style.EMPTY.withColor(Formatting.RED))
                     ),
                  true
               );
            } else {
               this.field11680 = true;
               this.field11681 = false;
               this.method3322();
            }
         }
      }
   }

   public boolean method3319() {
      return this.field11677;
   }

   private void method3322() {
      if (mc.player != null && mc.player.getMainHandStack().getItem() != Items.MACE) {
         if (InventoryUtils.method0539(Items.MACE) != -1) {
            this.field11689 = InventoryUtils.method0554();
            InventoryUtils.method0553(InventoryUtils.method0539(Items.MACE), false);
         } else if (InventoryUtils.method0557(Items.MACE) != -1) {
            this.field11688 = InventoryUtils.method0557(Items.MACE);
            InventoryUtils.method0536(this.field11688, Hand.MAIN_HAND);
            InventoryUtils.method0549(true);
         }
      }
   }

   public static MaceTestModule method3323() {
      if (maceTestModule == null) {
         maceTestModule = new MaceTestModule();
      }

      return maceTestModule;
   }

   private MaceTestModule() {
      this.keybindSetting = this.addSetting(new KeybindSetting("Кнопка", "Кидает заряд ветра под себя с прыжком и берёт булаву."));
      this.field11670 = this.addSetting(
         new BooleanSetting("Помогать киллауре в попадании", "Отдаёт киллауре расширенный радиус наводки на время захода.", false)
      );
      this.legacyNumberSetting = this.addSetting(
         new LegacyNumberSetting("Дистанция наводки", "На какой дистанции киллаура ищет цель во время захода.", 30.0, 3.0, 30.0, 0.1)
      );
      this.legacyNumberSetting.withVisibility(() -> this.field11670.isChecked());
      this.field11672 = this.addSetting(new BooleanSetting("Брать в падение", "Берёт булаву в руку, когда пролетел вниз 2.3 блока.", false));
      this.field11673 = this.addSetting(
         new BooleanSetting("Автовзятие булавы", "После взлёта сама переключается на булаву и держит её до приземления.", true)
      );
   }

   private void method3324() {
      if (mc.player != null && mc.world != null) {
         if (InventoryUtils.method0557(Items.WIND_CHARGE) != -1
            && !mc.player
               .getItemCooldownManager()
               .isCoolingDown(mc.player.getInventory().getStack(InventoryUtils.method0557(Items.WIND_CHARGE)))) {
            this.field11686 = mc.player.getYaw();
            this.field11687 = 90.0F;
            if (!mc.player.isOnGround()
               && mc.world.getBlockState(mc.player.getBlockPos().add(0, -2, 0)).isAir()
               && mc.player.getVelocity().y > 0.4) {
               this.field11687 = 75.0F;

               for (int index = 0; index < 360; index += 45) {
                  if (this.method3316(this.field11687, index)) {
                     this.field11686 = MathHelper.wrapDegrees(mc.player.getYaw() + index);
                  }
               }
            }

            this.field11685 = System.currentTimeMillis() + 500L;
            RotationManager.method0658().method0653();
            if (!ScreenWalkModule.method0991().method0988()) {
               RotationManager.method0658()
                  .method0652(new RotationAngles(this.field11686, this.field11687), new RotationSpeed(360.0F, 360.0F, 20.0F, 20.0F));
            }

            InventorySlotManager.method4176()
               .method4195(mc.player.getInventory().getStack(InventoryUtils.method0557(Items.WIND_CHARGE)).copy(), true);
            this.field11679 = true;
         }
      }
   }

   private void method3326() {
      if (this.field11688 != -1) {
         InventoryUtils.method0536(this.field11688, Hand.MAIN_HAND);
         InventoryUtils.method0549(true);
         this.field11688 = -1;
      }

      if (this.field11689 != -1) {
         InventoryUtils.method0553(this.field11689, false);
         this.field11689 = -1;
      }
   }
}
