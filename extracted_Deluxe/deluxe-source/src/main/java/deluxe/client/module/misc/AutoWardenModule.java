package deluxe.client.module.misc;

import deluxe.mixin.BossBarHudMixin;
import deluxe.client.util.ClientChat;
import deluxe.client.util.Colors;
import deluxe.client.event.EventListener;
import deluxe.client.render.FontFace;
import deluxe.client.event.HudRenderEvent;
import deluxe.client.module.render.InterfaceModule;
import deluxe.client.setting.LegacyNumberSetting;
import deluxe.client.util.MathUtils;
import deluxe.client.module.Module;
import deluxe.client.module.ModuleCategory;
import deluxe.client.module.ModuleInfo;
import deluxe.client.event.PacketEvent;
import deluxe.client.event.PlayerTickEvent;
import deluxe.client.util.InventoryUtils;
import deluxe.client.render.FontText;
import deluxe.client.ui.hud.WatermarkHud;
import deluxe.client.util.InventoryActionQueue;
import deluxe.client.module.movement.MovementPauseController;
import deluxe.client.render.ThemeColors;
import deluxe.client.util.InventorySlotManager;
import deluxe.client.util.AimPointUtils;
import deluxe.client.RotationAngles;
import deluxe.client.RotationManager;
import deluxe.client.RotationSpeed;
import deluxe.client.util.Stopwatch;
import deluxe.client.event.WorldRenderEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.block.Blocks;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.SlabBlock;
import net.minecraft.block.StairsBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.entity.TrappedChestBlockEntity;
import net.minecraft.block.enums.ChestType;
import net.minecraft.client.gui.hud.ClientBossBar;
import net.minecraft.client.gui.screen.DeathScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.decoration.DisplayEntity.TextDisplayEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.WardenEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.DeathMessageS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;

@ModuleInfo(
   name = "AutoWarden",
   description = "Летит на арену по клан-хому, лутает сундуки с таймером под инвизом и сдаёт лут в двойной сундук на базе.",
   category = ModuleCategory.MISC
)
public final class AutoWardenModule extends Module {
   private static AutoWardenModule autoWardenModule;
   private static final Pattern field04776 = Pattern.compile("(?:(\\d{1,2}):(\\d{2})|(\\d+)\\s*(?:сек|сек\\.|с|s)|(\\d+)\\s*(?:мин|мин\\.|m))");
   private static final Pattern field04777 = Pattern.compile("\\d+");
   private static final Pattern field04778 = Pattern.compile(".*Телепортация через[:\\s]*(\\d+(?:[.,]\\d+)?)\\s*сек.*");
   private static final String[] field04788 = new String[]{
      "#set allowBreak false",
      "#set allowPlace false",
      "#set allowSprint true",
      "#set sprintInWater true",
      "#set allowParkour true",
      "#set allowDiagonalDescend true",
      "#set allowDownward true",
      "#set avoidance true",
      "#set allowVines false",
      "#set blocksToAvoid twisting_vines,twisting_vines_plant,weeping_vines,weeping_vines_plant,sculk_sensor,sculk_shrieker",
      "#set costHeuristic 3.5",
      "#set primaryTimeoutMS 100",
      "#set movementTimeoutTicks 40",
      "#set freeLook true",
      "#set renderPath true",
      "#set renderPathAsLine true",
      "#set renderGoal false"
   };
   private static final String[] field04789 = new String[]{
      "#set allowBreak true",
      "#set allowPlace true",
      "#set renderPath true",
      "#set renderGoal true",
      "#set renderPathAsLine false",
      "#set colorCurrentPath 255,0,0",
      "#set colorNextPath 255,0,255"
   };
   public LegacyNumberSetting field04790;
   public LegacyNumberSetting field04791;
   private final Stopwatch field04792 = new Stopwatch();
   private final Stopwatch field04793 = new Stopwatch();
   private final ArrayList<BlockPos> field04794 = new ArrayList<>();
   private final ArrayList<BlockPos> field04795 = new ArrayList<>();
   public final ArrayList<Integer> field04796 = new ArrayList<>();
   private AutoWardenModule.Nested0388 field04797;
   private AutoWardenModule.Nested0388 field04798;
   private String field04799;
   private int field04800;
   private boolean field04801;
   private boolean field04802;
   private BlockPos field04803;
   private BlockPos field04804;
   private BlockPos field04805;
   private final Stopwatch field04806 = new Stopwatch();
   private final ArrayList<BlockPos> field04807 = new ArrayList<>();
   private final HashMap<BlockPos, Long> field04808 = new HashMap<>();
   private boolean field04809;
   private boolean field04810;
   private boolean field04811;
   private boolean field04812;
   private boolean field04813;
   private int field04814;
   private int field04815;
   private int field04816;
   private int field04817;
   private int field04818;
   private int field04819;
   private int field04820 = -1;
   private Vec3d field04821 = Vec3d.ZERO;
   private long field04822;
   private long field04823;
   private long field04824;
   private long field04825;
   private long field04826;
   private long field04827;
   private long field04828;
   private float field04829;
   private float field04830;
   private float field04831;
   private int field04832;
   private int field04833;
   private int field04834;
   private int field04835;
   private final EventListener<PlayerTickEvent> field04836 = playerTickEvent -> this.method1545();
   private final EventListener<PacketEvent> field04837 = event -> this.onPacket(event);
   private final EventListener<HudRenderEvent> field04838 = event2 -> this.onHudRender(event2);

   private boolean method1541(BlockPos blockPos) {
      return mc.world.getBlockState(blockPos).getCollisionShape(mc.world, blockPos).isEmpty()
         && mc.world.getBlockState(blockPos.up()).getCollisionShape(mc.world, blockPos.up()).isEmpty()
         && !mc.world.getBlockState(blockPos.down()).getCollisionShape(mc.world, blockPos.down()).isEmpty()
         && !this.method1571(blockPos)
         && !this.method1571(blockPos.up())
         && !this.method1571(blockPos.down());
   }

   public static boolean method1542() {
      return autoWardenModule != null && autoWardenModule.isEnabled();
   }

   private void method1545() {
      if (mc.player != null && mc.world != null && mc.interactionManager != null) {
         if (this.field04832 <= 0) {
            this.field04832 = this.method1588();
         }

         if (mc.player.getEntityPos().squaredDistanceTo(this.field04821) < 0.0025) {
            this.field04816++;
         } else {
            this.field04816 = 0;
         }

         this.field04821 = mc.player.getEntityPos();
         if (!this.field04812 && !(mc.currentScreen instanceof DeathScreen)) {
            this.method1582();
            this.method1578();
            this.method1615();
            if (this.method1596()) {
               this.method1603();
            } else if (this.method1622() || this.method1616()) {
               return;
            }

            if (!this.method1559() && this.field04797 != AutoWardenModule.Nested0388.WARP) {
               if (this.field04832 > 0) {
                  ClientChat.method2267(
                     "Измерение «" + mc.world.getRegistryKey().getValue().getPath() + "» вместо оверворлда — ухожу домой сдаваться."
                  );
                  this.method1575("home", this.field04832, AutoWardenModule.Nested0388.DEPOSIT);
               }
            } else {
               switch (this.field04797) {
                  case SUPPLY:
                     if (this.method1562()) {
                        if (this.method1553() || this.field04792.hasExceededMillis(15000L) && mc.player.currentScreenHandler.getCursorStack().isEmpty()) {
                           this.method1570();
                           if (this.field04803 != null) {
                              this.field04795.add(this.field04803);
                           }

                           this.method1552(
                              InventoryUtils.method0555(this::method1557) == -1
                                    && this.method1590(blockPos -> this.method1549(blockPos, ChestType.SINGLE) && !this.field04795.contains(blockPos))
                                       != null
                                 ? AutoWardenModule.Nested0388.SUPPLY
                                 : AutoWardenModule.Nested0388.BUFF
                           );
                           return;
                        }

                        return;
                     }

                     if (mc.player.age % 20 == 0
                        && this.method1620()
                        && this.method1590(
                              blockPos2 -> this.method1549(blockPos2, ChestType.LEFT)
                                 && !this.field04795.contains(blockPos2)
                                 && mc.player.squaredDistanceTo(Vec3d.ofCenter(blockPos2)) < 1024.0
                           )
                           != null) {
                        this.method1552(AutoWardenModule.Nested0388.DEPOSIT);
                        return;
                     }

                     if (!this.method1599()) {
                        this.method1552(AutoWardenModule.Nested0388.BUFF);
                        return;
                     }

                     this.method1602(ChestType.SINGLE, AutoWardenModule.Nested0388.BUFF);
                     break;
                  case BUFF:
                     if (mc.player.getHealth() < 16.0F && mc.player.getHungerManager().getFoodLevel() >= 18) {
                        return;
                     }

                     if (this.method1614() > 0 && this.method1588() > 0 && this.method1588() != this.method1614()) {
                        this.method1575("clan home " + this.field04790.intValue(), this.method1614(), AutoWardenModule.Nested0388.HUNT);
                        return;
                     }

                     this.method1575("clan home " + this.field04790.intValue(), 0, AutoWardenModule.Nested0388.HUNT);
                     break;
                  case WARP:
                     if (this.field04800 > 0) {
                        if (this.method1588() != this.field04800) {
                           if (this.field04801 && !this.field04792.hasExceededMillis(20000L)) {
                              return;
                           }

                           this.method1570();
                           this.method1604();
                           mc.player.networkHandler.sendChatCommand("an" + this.field04800);
                           this.field04801 = true;
                           this.field04792.reset();
                           return;
                        }

                        if (this.field04801 && !this.field04792.hasExceededMillis(4000L)) {
                           return;
                        }

                        this.field04800 = 0;
                        this.field04801 = false;
                        this.field04802 = false;
                        this.field04792.reset();
                        return;
                     }

                     if (this.field04799 == null) {
                        this.method1552(this.field04798);
                        return;
                     }

                     if (!this.field04801) {
                        this.method1570();
                        this.method1604();
                        mc.player.networkHandler.sendChatCommand(this.field04799);
                        this.field04801 = true;
                        this.field04792.reset();
                        return;
                     }

                     if (!this.field04802) {
                        if (this.method1607()) {
                           this.field04792.reset();
                        }

                        if (this.field04792.hasExceededMillis(8000L)) {
                           this.field04801 = false;
                        }

                        return;
                     }

                     if (this.method1607()) {
                        return;
                     }

                     if (this.method1563()) {
                        return;
                     }

                     if (this.field04798 == AutoWardenModule.Nested0388.HUNT) {
                        if (this.method1588() > 0 && this.method1614() > 0 && this.method1588() != this.method1614()) {
                           ClientChat.method2267(
                              "Клан-хом утащил на анархию " + this.method1588() + " вместо " + this.method1614() + " — убираю её из списка выездных."
                           );
                           this.field04796.remove(Integer.valueOf(this.method1614()));
                        }

                        this.field04794.clear();
                        this.field04835 = 0;
                        this.field04808.clear();
                     }

                     this.method1552(this.field04798);
                     break;
                  case HUNT:
                     if (!this.method1586(mc.player.getBlockPos())) {
                        if (mc.player.age % 20 == 0
                           && this.method1620()
                           && this.method1590(
                                 blockPos3 -> this.method1549(blockPos3, ChestType.LEFT)
                                    && !this.field04795.contains(blockPos3)
                                    && mc.player.squaredDistanceTo(Vec3d.ofCenter(blockPos3)) < 1024.0
                              )
                              != null) {
                           this.method1604();
                           this.method1552(AutoWardenModule.Nested0388.DEPOSIT);
                           return;
                        }

                        if (!this.field04793.hasExceededMillis(3000L)) {
                           return;
                        }

                        this.field04793.reset();
                        this.method1579(this.method1613(mc.player.getBlockPos()));
                        return;
                     }

                     if (this.method1562()) {
                        if (!this.method1612(true, slot -> !this.method1594(slot.getStack()))) {
                           return;
                        }

                        this.method1570();
                        if (this.field04813) {
                           this.field04835++;
                        }

                        this.field04813 = false;
                        return;
                     }

                     if (this.field04835 < 2
                        && !this.method1585()
                        && !(mc.player.getHealth() < 7.0F)
                        && mc.player.getHungerManager().getFoodLevel() > 6) {
                        if (mc.player.age % 5 == 0) {
                           BlockPos blockPos4 = this.method1554();
                           if (blockPos4 == null) {
                              blockPos4 = this.method1580();
                           }

                           if (blockPos4 != null) {
                              this.field04810 = this.method1558(blockPos4) < 0;
                              this.field04803 = blockPos4;
                              this.field04804 = this.method1619(blockPos4);
                              this.method1579(this.field04804);
                              this.method1552(AutoWardenModule.Nested0388.CHEST);
                              return;
                           }
                        }

                        this.method1597();
                        break;
                     }

                     if (!((BossBarHudMixin)mc.inGameHud.getBossBarHud()).getEvents().isEmpty()) {
                        this.method1597();
                        return;
                     }

                     this.method1604();
                     this.method1552(AutoWardenModule.Nested0388.ARENA);
                     return;
                  case CHEST:
                     if (this.field04803 == null || !(mc.world.getBlockEntity(this.field04803) instanceof ChestBlockEntity)) {
                        this.method1604();
                        this.method1552(AutoWardenModule.Nested0388.HUNT);
                        return;
                     }

                     if (this.method1562()) {
                        if (!this.method1612(true, slot2 -> !this.method1594(slot2.getStack())) && !this.field04792.hasExceededMillis(10000L)) {
                           return;
                        }

                        this.method1570();
                        this.field04794.add(this.field04803);
                        if (this.field04813) {
                           this.field04835++;
                        }

                        this.method1552(AutoWardenModule.Nested0388.HUNT);
                        return;
                     }

                     if (this.field04810 && this.method1558(this.field04803) >= 0) {
                        this.field04794.add(this.field04803);
                        this.method1604();
                        this.method1552(AutoWardenModule.Nested0388.HUNT);
                        return;
                     }

                     if (!this.method1551(this.field04803, this.field04804)) {
                        return;
                     }

                     if (this.method1558(this.field04803) > 0 && !this.field04792.hasExceededMillis(360000L)) {
                        this.method1604();
                        return;
                     }

                     if (this.field04815++ > 200) {
                        this.field04794.add(this.field04803);
                        this.method1552(AutoWardenModule.Nested0388.HUNT);
                        return;
                     }

                     if (!this.method1609(this.field04803)) {
                        if (this.field04815 % 20 == 1) {
                           this.method1579(this.field04804 != null ? this.field04804 : this.field04803);
                        }

                        return;
                     }

                     this.method1604();
                     this.method1573(this.field04803);
                     break;
                  case ARENA:
                     if (this.method1563()) {
                        return;
                     }

                     if (this.method1565() != -1) {
                        if (mc.player.age % 2 != 0) {
                           return;
                        }

                        InventoryUtils.method0547(this.method1565(), 0, SlotActionType.PICKUP, false);
                        this.field04792.reset();
                        return;
                     }

                     if (this.field04802) {
                        if (!this.field04792.hasExceededMillis(1500L)) {
                           return;
                        }

                        this.method1575("home home", this.field04832, AutoWardenModule.Nested0388.DEPOSIT);
                        return;
                     }

                     if (!this.field04801) {
                        mc.player.networkHandler.sendChatCommand("darena");
                        this.field04801 = true;
                        this.field04792.reset();
                        return;
                     }

                     if (!this.field04792.hasExceededMillis(4000L)) {
                        return;
                     }

                     this.method1570();
                     this.field04801 = false;
                     break;
                  case DEPOSIT:
                     if (this.method1562()) {
                        if (!this.method1612(false, this::method1574)) {
                           if (this.method1605((GenericContainerScreenHandler)mc.player.currentScreenHandler) != -1) {
                              return;
                           }

                           ClientChat.method2267("Двойной сундук заполнен, ищу следующий.");
                           this.field04795.add(this.field04803);
                           this.method1570();
                           this.method1552(AutoWardenModule.Nested0388.DEPOSIT);
                           return;
                        }

                        if (!this.method1595()) {
                           return;
                        }

                        this.method1570();
                        if (++this.field04834 > 2) {
                           this.field04833++;
                        }

                        this.method1552(AutoWardenModule.Nested0388.SUPPLY);
                        return;
                     }

                     this.method1602(ChestType.LEFT, AutoWardenModule.Nested0388.DEPOSIT);
               }
            }
         } else {
            this.method1617();
         }
      }
   }

   private boolean method1546(ItemStack itemStack, RegistryEntry<StatusEffect> local2260_2) {
      if (itemStack.isOf(Items.POTION) && itemStack.get(DataComponentTypes.POTION_CONTENTS) != null) {
         for (StatusEffectInstance statusEffectInstance : itemStack.get(DataComponentTypes.POTION_CONTENTS).getEffects()) {
            if (statusEffectInstance.getEffectType().value() == local2260_2.value()) {
               return true;
            }
         }

         return false;
      } else {
         return false;
      }
   }

   private double method1547(BlockPos blockPos) {
      double local2261_2 = Double.MAX_VALUE;

      for (BlockPos blockPos2 : this.field04807) {
         local2261_2 = Math.min(local2261_2, Math.hypot(blockPos2.getX() - blockPos.getX(), blockPos2.getZ() - blockPos.getZ()));
      }

      return local2261_2;
   }

   private boolean method1549(BlockPos blockPos, ChestType chestType) {
      if (!mc.world.getBlockState(blockPos).contains(ChestBlock.CHEST_TYPE)) {
         return false;
      } else {
         return chestType == ChestType.SINGLE
            ? mc.world.getBlockState(blockPos).get(ChestBlock.CHEST_TYPE) == ChestType.SINGLE
            : mc.world.getBlockState(blockPos).get(ChestBlock.CHEST_TYPE) != ChestType.SINGLE;
      }
   }

   private AutoWardenModule() {
      this.field04790 = this.addSetting(
         new LegacyNumberSetting("Дом клана", "Номер клан-хома, на который телепортироваться в начале круга.", 1.0, 1.0, 9.0, 1.0)
      );
      this.field04791 = this.addSetting(
         new LegacyNumberSetting("Открывать от", "Идти к сундуку, если на его таймере осталось не больше стольких секунд.", 45.0, 5.0, 300.0, 1.0)
      );
   }

   private boolean method1550(ItemStack itemStack) {
      return this.method1546(itemStack, StatusEffects.INVISIBILITY);
   }

   private boolean method1551(BlockPos blockPos, BlockPos blockPos2) {
      if ((blockPos2 == null || !(mc.player.squaredDistanceTo(Vec3d.ofBottomCenter(blockPos2)) < 0.6))
         && !(mc.player.getEyePos().distanceTo(Vec3d.ofCenter(blockPos)) <= 2.5)
         && (!this.method1609(blockPos) || blockPos2 != null && this.field04814 <= 80)) {
         this.field04814++;
         if (this.field04814 % 20 == 0) {
            this.method1579(blockPos2 != null ? blockPos2 : blockPos);
         }

         if (this.field04814 > 300 || this.field04814 > 12 && this.field04816 > 20) {
            this.method1604();
            return true;
         } else {
            return false;
         }
      } else {
         this.field04814 = 0;
         return true;
      }
   }

   private void method1552(AutoWardenModule.Nested0388 local2268_1) {
      this.field04797 = local2268_1;
      this.field04792.reset();
      this.field04814 = 0;
      this.field04815 = 0;
      this.field04817 = 0;
      this.field04818 = 0;
      this.field04820 = -1;
      this.field04813 = false;
      this.field04801 = false;
      this.field04802 = false;
      this.method1603();
      if (local2268_1 != AutoWardenModule.Nested0388.CHEST) {
         this.field04803 = null;
         this.field04804 = null;
      }

      if (local2268_1 == AutoWardenModule.Nested0388.BUFF) {
         this.field04795.clear();
      }
   }

   private boolean method1553() {
      if (mc.player.currentScreenHandler instanceof GenericContainerScreenHandler genericContainerScreenHandler) {
         MovementPauseController.getInstance().pauseForTicks(1);
         if (mc.player.age % 2 != 0) {
            return false;
         }

         if (!genericContainerScreenHandler.getCursorStack().isEmpty()) {
            if (this.method1583(genericContainerScreenHandler.getCursorStack()) && InventoryUtils.method0546(0, 35) != -1) {
               InventoryUtils.method0547(
                  InventoryUtils.method0533(InventoryUtils.method0546(0, 35)), this.field04818++ < 4 ? 1 : 0, SlotActionType.PICKUP, false
               );
               return false;
            } else {
               InventoryUtils.method0547(this.field04820 != -1 ? this.field04820 : this.method1568(genericContainerScreenHandler), 0, SlotActionType.PICKUP, false);
               this.field04820 = -1;
               return false;
            }
         } else {
            this.field04820 = -1;
            this.field04818 = 0;
            if (InventoryUtils.method0546(0, 35) == -1) {
               return true;
            }

            for (Slot slot : genericContainerScreenHandler.slots) {
               if (slot.id < genericContainerScreenHandler.getRows() * 9 && slot.hasStack() && this.method1583(slot.getStack())) {
                  this.field04817 = 0;
                  if (slot.getStack().getCount() == 1) {
                     InventoryUtils.method0544(slot, 0, SlotActionType.QUICK_MOVE, false);
                     return false;
                  }

                  this.field04820 = slot.id;
                  InventoryUtils.method0544(slot, 0, SlotActionType.PICKUP, false);
                  return false;
               }
            }

            return ++this.field04817 >= 3;
         }
      } else {
         return true;
      }
   }

   private BlockPos method1554() {
      BlockPos blockPos = null;
      double local2270_2 = Double.MAX_VALUE;

      for (int index = mc.player.getChunkPos().x - 9; index <= mc.player.getChunkPos().x + 9; index++) {
         for (int index2 = mc.player.getChunkPos().z - 9; index2 <= mc.player.getChunkPos().z + 9; index2++) {
            if (mc.world.isChunkLoaded(index, index2)) {
               for (BlockEntity blockEntity : mc.world.getChunk(index, index2).getBlockEntities().values()) {
                  if (blockEntity instanceof ChestBlockEntity && !(blockEntity instanceof TrappedChestBlockEntity)) {
                     BlockPos blockPos2 = blockEntity.getPos().toImmutable();
                     if (this.method1586(blockPos2) && this.method1549(blockPos2, ChestType.SINGLE)) {
                        int local2270_9 = this.method1558(blockPos2);
                        if (local2270_9 >= 0) {
                           this.field04808.put(blockPos2, System.currentTimeMillis() + local2270_9 * 1000L);
                        } else {
                           this.field04808.remove(blockPos2);
                        }

                        if (!this.field04794.contains(blockPos2)
                           && local2270_9 <= this.field04791.intValue()
                           && !this.method1572(blockPos2)
                           && (local2270_9 >= 0 || !(mc.player.squaredDistanceTo(Vec3d.ofCenter(blockPos2)) > 225.0))
                           && !(mc.player.squaredDistanceTo(Vec3d.ofCenter(blockPos2)) >= local2270_2)) {
                           local2270_2 = mc.player.squaredDistanceTo(Vec3d.ofCenter(blockPos2));
                           blockPos = blockPos2;
                        }
                     }
                  }
               }
            }
         }
      }

      return blockPos;
   }

   private boolean method1557(ItemStack itemStack) {
      return itemStack.isOf(Items.GOLDEN_CARROT);
   }

   private int method1558(BlockPos blockPos) {
      if (blockPos != null && mc.world != null) {
         for (Entity entity : mc.world
            .getOtherEntities(
               mc.player,
               new Box(blockPos).expand(3.0, 4.0, 3.0),
               entity2 -> entity2 instanceof ArmorStandEntity armorStandEntity && armorStandEntity.hasCustomName() || entity2 instanceof TextDisplayEntity
            )) {
            Matcher matcher = field04776.matcher(
               (entity instanceof TextDisplayEntity textDisplayEntity ? textDisplayEntity.getText() : entity.getName())
                  .getString()
                  .replaceAll("(?i)§[0-9a-fk-or]", "")
                  .trim()
            );
            if (matcher.find()) {
               if (matcher.group(1) != null) {
                  return Integer.parseInt(matcher.group(1)) * 60 + Integer.parseInt(matcher.group(2));
               }

               if (matcher.group(3) != null) {
                  return Integer.parseInt(matcher.group(3));
               }

               return Integer.parseInt(matcher.group(4)) * 60;
            }
         }

         return -1;
      } else {
         return -1;
      }
   }

   private boolean method1559() {
      return mc.world != null && mc.world.getRegistryKey().getValue().getPath().equals("overworld");
   }

   private boolean method1562() {
      return mc.currentScreen instanceof HandledScreen && mc.player.currentScreenHandler instanceof GenericContainerScreenHandler;
   }

   @Override
   protected void onDisable() {
      this.method1603();
      MovementPauseController.getInstance().pauseForTicks(-1);
      this.field04824 = 0L;
      WardenHelperModule.method1355().setEnabledWithoutCallbacks(false);
      if (mc.player != null) {
         this.method1604();

         for (String local2279_4 : field04789) {
            ClientChat.method2258(local2279_4);
         }

         RotationManager.method0658().method0653();
         mc.player.setSneaking(false);
      }
   }

   private boolean method1563() {
      if (mc.player.currentScreenHandler != mc.player.playerScreenHandler || InventoryUtils.method0557(Items.GLASS_BOTTLE) == -1) {
         this.field04819 = 0;
         return false;
      }

      if (this.field04819 >= 5) {
         return false;
      }

      if (mc.player.age % 5 != 0) {
         return true;
      }

      this.field04819++;
      InventoryUtils.method0547(InventoryUtils.method0533(InventoryUtils.method0557(Items.GLASS_BOTTLE)), 1, SlotActionType.THROW, false);
      return true;
   }

   private int method1564() {
      for (int index = 8; index >= 0; index--) {
         if (!mc.player.getInventory().getStack(index).isEmpty() && !this.method1608(mc.player.getInventory().getStack(index))) {
            return index;
         }
      }

      return -1;
   }

   private int method1565() {
      if (mc.currentScreen instanceof HandledScreen<?> local2282_1) {
         if (local2282_1.getScreenHandler().slots.size() > 24
            && local2282_1.getScreenHandler().getSlot(24).hasStack()
            && local2282_1.getScreenHandler().getSlot(24).getStack().isOf(Items.PUFFERFISH)) {
            return 24;
         }

         for (Slot slot : local2282_1.getScreenHandler().slots) {
            if (slot.hasStack() && slot.getStack().getName().getString().replaceAll("(?i)§[0-9a-fk-or]", "").contains("На Смотровую")) {
               return slot.id;
            }
         }

         return -1;
      } else {
         return -1;
      }
   }

   private long method1566() {
      return mc.player.getStatusEffect(StatusEffects.INVISIBILITY) == null
         ? 0L
         : mc.player.getStatusEffect(StatusEffects.INVISIBILITY).getDuration() * 50L;
   }

   private int method1568(GenericContainerScreenHandler genericContainerScreenHandler) {
      return this.method1605(genericContainerScreenHandler) != -1 ? this.method1605(genericContainerScreenHandler) : InventoryUtils.method0533(InventoryUtils.method0546(0, 35));
   }

   private boolean method1569(double local2286_1) {
      for (PlayerEntity playerEntity : mc.world.getPlayers()) {
         if (playerEntity != mc.player && mc.player.squaredDistanceTo(playerEntity) <= local2286_1 * local2286_1) {
            return true;
         }
      }

      return false;
   }

   private void method1570() {
      if (mc.currentScreen != null) {
         mc.player.closeHandledScreen();
      }

      InventoryUtils.method0541();
      MovementPauseController.getInstance().pauseForTicks(-1);
   }

   private boolean method1571(BlockPos blockPos) {
      return mc.world.getBlockState(blockPos).isOf(Blocks.TWISTING_VINES)
         || mc.world.getBlockState(blockPos).isOf(Blocks.TWISTING_VINES_PLANT)
         || mc.world.getBlockState(blockPos).isOf(Blocks.WEEPING_VINES)
         || mc.world.getBlockState(blockPos).isOf(Blocks.WEEPING_VINES_PLANT)
         || mc.world.getBlockState(blockPos).isOf(Blocks.SCULK_SENSOR)
         || mc.world.getBlockState(blockPos).isOf(Blocks.SCULK_SHRIEKER);
   }

   private boolean method1572(BlockPos blockPos) {
      if (!mc.world.getEntitiesByClass(WardenEntity.class, new Box(blockPos).expand(6.0), wardenEntity -> wardenEntity.isAlive()).isEmpty()) {
         return true;
      }

      for (PlayerEntity playerEntity : mc.world.getPlayers()) {
         if (playerEntity != mc.player
            && playerEntity.getEntityPos().squaredDistanceTo(Vec3d.ofCenter(blockPos)) <= 25.0
            && this.method1621(playerEntity)) {
            return true;
         }
      }

      return false;
   }

   private void method1573(BlockPos blockPos) {
      if (mc.player.isSneaking()) {
         mc.player.setSneaking(false);
      }

      Vec3d vec3d = Vec3d.ofCenter(blockPos);
      RotationAngles rotationAngles = AimPointUtils.method4450(vec3d.subtract(mc.player.getEyePos()));
      float local2290_4 = (float)(
         (
               Math.sin(mc.player.age * 0.31F) * 0.5
                  + Math.sin(mc.player.age * 0.73F + 1.1F) * 0.3
                  + Math.sin(mc.player.age * 1.7F + 2.6F) * 0.2
            )
            * 8.0
      );
      RotationManager.method0658()
         .method0648(
            new RotationAngles(rotationAngles.method4362() + local2290_4, MathHelper.clamp(rotationAngles.method4365() + local2290_4 / 4.0F, -90.0F, 90.0F)),
            new RotationSpeed(40.0F, 40.0F, 30.0F, 30.0F),
            300L
         );
      if (mc.player.age % 2 == 0
         && (
            this.field04815 > 60
               || !(Math.abs(MathHelper.wrapDegrees(rotationAngles.method4362() + local2290_4 - mc.player.getYaw())) > 5.0F)
                  && !(Math.abs(rotationAngles.method4365() + local2290_4 / 4.0F - mc.player.getPitch()) > 5.0F)
         )) {
         BlockHitResult blockHitResult = mc.world
            .raycast(new RaycastContext(mc.player.getEyePos(), vec3d, ShapeType.COLLIDER, FluidHandling.NONE, mc.player));
         if (!this.method1592(blockHitResult.getBlockPos(), blockPos)) {
            blockHitResult = new BlockHitResult(vec3d, Direction.UP, blockPos, false);
         }

         mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, blockHitResult);
         mc.player.swingHand(Hand.MAIN_HAND);
         this.field04792.reset();
      }
   }

   private boolean method1574(Slot slot) {
      if (this.method1550(slot.getStack())) {
         return slot.getIndex() != InventoryUtils.method0555(this::method1550);
      } else if (this.method1618(slot.getStack())) {
         return slot.getIndex() != InventoryUtils.method0555(this::method1618);
      } else {
         return this.method1557(slot.getStack()) ? slot.getIndex() != InventoryUtils.method0555(this::method1557) : true;
      }
   }

   private void method1575(String local2292_1, int local2292_2, AutoWardenModule.Nested0388 local2292_3) {
      this.field04799 = local2292_1;
      this.field04800 = local2292_2;
      this.field04798 = local2292_3;
      this.method1552(AutoWardenModule.Nested0388.WARP);
   }

   @Override
   protected void onEnable() {
      this.field04794.clear();
      this.field04795.clear();
      this.field04807.clear();
      this.field04808.clear();
      this.field04803 = null;
      this.field04804 = null;
      this.field04800 = 0;
      this.field04799 = null;
      this.field04809 = true;
      this.field04810 = false;
      this.field04811 = false;
      this.field04812 = false;
      this.field04821 = Vec3d.ZERO;
      this.field04822 = 0L;
      this.field04827 = 0L;
      this.field04828 = 0L;
      this.field04823 = 0L;
      this.field04832 = 0;
      this.field04833 = 0;
      this.field04834 = 0;
      this.field04835 = 0;
      this.field04816 = 0;
      this.field04824 = System.currentTimeMillis();
      this.field04793.reset();
      WardenHelperModule.method1355().setEnabledWithoutCallbacks(true);
      this.method1575("home", 0, AutoWardenModule.Nested0388.DEPOSIT);
      if (mc.player != null && mc.world != null) {
         for (String local2293_4 : field04788) {
            ClientChat.method2258(local2293_4);
         }

         ClientChat.method2258(
            "#set colorCurrentPath "
               + (ThemeColors.field13669 >> 16 & 0xFF)
               + ","
               + (ThemeColors.field13669 >> 8 & 0xFF)
               + ","
               + (ThemeColors.field13669 & 0xFF)
         );
         ClientChat.method2258(
            "#set colorNextPath "
               + (ThemeColors.field13669 >> 16 & 0xFF)
               + ","
               + (ThemeColors.field13669 >> 8 & 0xFF)
               + ","
               + (ThemeColors.field13669 & 0xFF)
         );
      } else {
         this.setEnabled(false);
      }
   }

   private Vec3d method1577(BlockPos blockPos) {
      Vec3d vec3d = Vec3d.ofCenter(blockPos);
      Vec3d vec3d2 = null;
      double local2295_4 = Double.MAX_VALUE;

      for (double local2295_6 = -0.4; local2295_6 <= 0.41; local2295_6 += 0.4) {
         for (double local2295_8 = -0.4; local2295_8 <= 0.41; local2295_8 += 0.4) {
            for (double local2295_10 = -0.4; local2295_10 <= 0.41; local2295_10 += 0.4) {
               Vec3d vec3d3 = vec3d.add(local2295_6, local2295_8, local2295_10);
               if (!(vec3d3.squaredDistanceTo(vec3d) >= local2295_4)
                  && this.method1592(
                     mc.world
                        .raycast(new RaycastContext(mc.player.getEyePos(), vec3d3, ShapeType.COLLIDER, FluidHandling.NONE, mc.player))
                        .getBlockPos(),
                     blockPos
                  )) {
                  local2295_4 = vec3d3.squaredDistanceTo(vec3d);
                  vec3d2 = vec3d3;
               }
            }
         }
      }

      return vec3d2;
   }

   private void method1578() {
      if (!(mc.world.getBlockState(mc.player.getBlockPos().down()).getBlock() instanceof SlabBlock)
         && !(mc.world.getBlockState(mc.player.getBlockPos().down()).getBlock() instanceof StairsBlock)) {
         if (mc.player.isSneaking()) {
            mc.player.setSneaking(false);
         }
      } else {
         mc.player.setSneaking(true);
      }
   }

   private void method1579(BlockPos blockPos) {
      if (blockPos != null) {
         if (!this.field04809 || !blockPos.equals(this.field04805) || this.field04806.hasExceededMillis(3000L)) {
            RotationManager.method0658().method0653();
            this.field04809 = true;
            this.field04805 = blockPos;
            this.field04806.reset();
            ClientChat.method2258("#go " + blockPos.getX() + " " + blockPos.getY() + " " + blockPos.getZ());
         }
      }
   }

   private BlockPos method1580() {
      BlockPos blockPos = null;
      long local2298_2 = System.currentTimeMillis() + this.field04791.intValue() * 1000L;

      for (BlockPos blockPos2 : this.field04808.keySet()) {
         if (mc.world.getBlockEntity(blockPos2) instanceof ChestBlockEntity
            && !this.field04794.contains(blockPos2)
            && this.field04808.get(blockPos2) < local2298_2) {
            local2298_2 = this.field04808.get(blockPos2);
            blockPos = blockPos2;
         }
      }

      return blockPos;
   }

   private void onPacket(PacketEvent event) {
      if (event.isIncoming()) {
         if (event.getPacket() instanceof DeathMessageS2CPacket) {
            this.field04812 = true;
         }

         if (event.getPacket() instanceof PlayerPositionLookS2CPacket) {
            this.field04802 = true;
         }
      }
   }

   private void method1582() {
      for (PlayerEntity playerEntity : mc.world.getPlayers()) {
         if (playerEntity != mc.player
            && !(mc.player.squaredDistanceTo(playerEntity) > 900.0)
            && !(
               Math.abs(
                     MathHelper.wrapDegrees(
                        playerEntity.getHeadYaw()
                           - (float)Math.toDegrees(Math.atan2(playerEntity.getX() - mc.player.getX(), mc.player.getZ() - playerEntity.getZ()))
                     )
                  )
                  > 25.0F
            )
            && !(
               (mc.player.getX() - playerEntity.getX()) * (playerEntity.getX() - playerEntity.lastX)
                     + (mc.player.getZ() - playerEntity.getZ()) * (playerEntity.getZ() - playerEntity.lastZ)
                  <= 0.01
            )) {
            this.field04828 = System.currentTimeMillis();
            if (this.field04827 == 0L) {
               this.field04827 = System.currentTimeMillis();
            }

            return;
         }
      }

      if (System.currentTimeMillis() - this.field04828 > 1500L) {
         this.field04827 = 0L;
      }
   }

   private boolean method1583(ItemStack itemStack) {
      return this.method1550(itemStack) && InventoryUtils.method0555(this::method1550) == -1
         || this.method1618(itemStack) && InventoryUtils.method0555(this::method1618) == -1
         || this.method1557(itemStack) && InventoryUtils.method0555(this::method1557) == -1;
   }

   public static AutoWardenModule method1584() {
      if (autoWardenModule == null) {
         autoWardenModule = new AutoWardenModule();
      }

      return autoWardenModule;
   }

   private boolean method1585() {
      int local2303_1 = 0;

      for (int index = 0; index < 36; index++) {
         if (!mc.player.getInventory().getStack(index).isEmpty()) {
            if (++local2303_1 >= 24) {
               return true;
            }
         }
      }

      return false;
   }

   private boolean method1586(BlockPos blockPos) {
      return this.method1559() && blockPos.getX() >= -2060 && blockPos.getX() <= -1928 && blockPos.getZ() >= -2069 && blockPos.getZ() <= -1936;
   }

   private long method1587() {
      return mc.player.getStatusEffect(StatusEffects.SPEED) == null ? 0L : mc.player.getStatusEffect(StatusEffects.SPEED).getDuration() * 50L;
   }

   private int method1588() {
      if (mc.world == null) {
         return -1;
      }

      ScoreboardObjective objectiveForSlot = mc.world.getScoreboard().getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR);
      if (objectiveForSlot == null) {
         return -1;
      }

      Matcher matcher = field04777.matcher(objectiveForSlot.getDisplayName().getString().replaceAll("(?i)§[0-9a-fk-or]", ""));
      int local2306_3 = -1;

      while (matcher.find()) {
         local2306_3 = Integer.parseInt(matcher.group());
      }

      return local2306_3;
   }

   private BlockPos method1590(Predicate<BlockPos> local2308_1) {
      BlockPos blockPos = null;
      double local2308_3 = Double.MAX_VALUE;

      for (int index = mc.player.getChunkPos().x - 9; index <= mc.player.getChunkPos().x + 9; index++) {
         for (int index2 = mc.player.getChunkPos().z - 9; index2 <= mc.player.getChunkPos().z + 9; index2++) {
            if (mc.world.isChunkLoaded(index, index2)) {
               for (BlockEntity blockEntity : mc.world.getChunk(index, index2).getBlockEntities().values()) {
                  if (blockEntity instanceof ChestBlockEntity
                     && !(blockEntity instanceof TrappedChestBlockEntity)
                     && !(mc.player.squaredDistanceTo(Vec3d.ofCenter(blockEntity.getPos())) > 16384.0)
                     && !(mc.player.squaredDistanceTo(Vec3d.ofCenter(blockEntity.getPos())) >= local2308_3)
                     && local2308_1.test(blockEntity.getPos())) {
                     local2308_3 = mc.player.squaredDistanceTo(Vec3d.ofCenter(blockEntity.getPos()));
                     blockPos = blockEntity.getPos().toImmutable();
                  }
               }
            }
         }
      }

      return blockPos;
   }

   private boolean method1592(BlockPos blockPos, BlockPos blockPos2) {
      return blockPos.equals(blockPos2)
         || blockPos.getSquaredDistance(blockPos2) <= 1.0
            && mc.world.getBlockEntity(blockPos) instanceof ChestBlockEntity
            && !this.method1549(blockPos2, ChestType.SINGLE);
   }

   private void onWorldRender(WorldRenderEvent event) {
      if (mc.player != null && mc.world != null && this.field04809 && this.field04805 != null) {
         Vec3d vec3d = mc.player.getEntityPos().add(0.0, 0.9, 0.0);
         Vec3d vec3d2 = Vec3d.ofCenter(this.field04805);
         Vec3d vec3d3 = vec3d2.subtract(vec3d).normalize();
         event.method0647().push();
         event.method0647()
            .translate(
               vec3d.x - mc.gameRenderer.getCamera().getCameraPos().x,
               vec3d.y - mc.gameRenderer.getCamera().getCameraPos().y,
               vec3d.z - mc.gameRenderer.getCamera().getCameraPos().z
            );
         VertexConsumer vertexConsumer = mc.getBufferBuilders().getEntityVertexConsumers().getBuffer(RenderLayers.lines());
         vertexConsumer.vertex(event.method0647().peek(), 0.0F, 0.0F, 0.0F)
            .color(Colors.method3727(ThemeColors.field13669, 0.35F))
            .normal(event.method0647().peek(), (float)vec3d3.x, (float)vec3d3.y, (float)vec3d3.z)
            .lineWidth(2.5F);
         vertexConsumer.vertex(
               event.method0647().peek(),
               (float)(vec3d2.x - vec3d.x),
               (float)(vec3d2.y - vec3d.y),
               (float)(vec3d2.z - vec3d.z)
            )
            .color(ThemeColors.field13669)
            .normal(event.method0647().peek(), (float)vec3d3.x, (float)vec3d3.y, (float)vec3d3.z)
            .lineWidth(2.5F);
         mc.getBufferBuilders().getEntityVertexConsumers().draw();
         event.method0647().pop();
      }
   }

   private boolean method1594(ItemStack itemStack) {
      return itemStack.isOf(Items.PIGLIN_HEAD)
         || itemStack.isOf(Items.ZOMBIE_HEAD)
         || itemStack.isOf(Items.DISC_FRAGMENT_5)
         || itemStack.isOf(Items.ENDER_CHEST)
         || itemStack.isOf(Items.RIB_ARMOR_TRIM_SMITHING_TEMPLATE)
         || itemStack.isOf(Items.PUFFERFISH)
         || itemStack.isOf(Items.ECHO_SHARD)
         || itemStack.isOf(Items.SCULK)
         || itemStack.isOf(Items.CREEPER_HEAD)
         || itemStack.isOf(Items.SKULL_BANNER_PATTERN)
         || itemStack.isOf(Items.NAUTILUS_SHELL)
         || itemStack.isOf(Items.BOLT_ARMOR_TRIM_SMITHING_TEMPLATE)
         || itemStack.isOf(Items.COOKED_MUTTON)
         || itemStack.isOf(Items.SCULK_SENSOR)
         || itemStack.isOf(Items.GHAST_TEAR)
         || itemStack.isOf(Items.BLAZE_SPAWN_EGG)
         || itemStack.isOf(Items.ENCHANTING_TABLE)
         || itemStack.contains(DataComponentTypes.JUKEBOX_PLAYABLE);
   }

   private boolean method1595() {
      if (mc.player.currentScreenHandler instanceof GenericContainerScreenHandler genericContainerScreenHandler) {
         if (mc.player.age % 2 != 0) {
            return false;
         }

         if (!genericContainerScreenHandler.getCursorStack().isEmpty()) {
            if (!this.method1557(genericContainerScreenHandler.getCursorStack())) {
               return true;
            } else if (this.field04820 != -1 && !genericContainerScreenHandler.slots.get(this.field04820).hasStack()) {
               InventoryUtils.method0547(this.field04820, 1, SlotActionType.PICKUP, false);
               return false;
            } else if (this.method1605(genericContainerScreenHandler) == -1) {
               InventoryUtils.method0547(this.field04820, 0, SlotActionType.PICKUP, false);
               this.field04820 = -1;
               ClientChat.method2267("Двойной сундук полон — излишек морковки остаётся с собой.");
               return true;
            } else {
               InventoryUtils.method0547(this.method1605(genericContainerScreenHandler), 0, SlotActionType.PICKUP, false);
               return false;
            }
         } else {
            int local2313_2 = 0;

            for (Slot slot : genericContainerScreenHandler.slots) {
               if (slot.id >= genericContainerScreenHandler.getRows() * 9 && slot.hasStack() && this.method1557(slot.getStack())) {
                  local2313_2 += slot.getStack().getCount();
               }
            }

            if (local2313_2 <= 1) {
               return true;
            }

            for (Slot slot2 : genericContainerScreenHandler.slots) {
               if (slot2.id >= genericContainerScreenHandler.getRows() * 9 && slot2.hasStack() && this.method1557(slot2.getStack())) {
                  this.field04820 = slot2.id;
                  InventoryUtils.method0544(slot2, 0, SlotActionType.PICKUP, false);
                  return false;
               }
            }

            return true;
         }
      } else {
         return true;
      }
   }

   private boolean method1596() {
      return this.field04827 != 0L && System.currentTimeMillis() - this.field04827 > 2500L;
   }

   private void method1597() {
      if (this.field04793.hasExceededMillis(this.field04816 >= 8 ? 1500L : 7000L)) {
         this.field04816 = 0;
         this.field04793.reset();
         BlockPos blockPos = null;
         double local2315_2 = -1.0;

         for (int index = 0; index < 24; index++) {
            BlockPos blockPos2 = new BlockPos(
               ThreadLocalRandom.current().nextInt(-2051, -1937), mc.player.getBlockPos().getY(), ThreadLocalRandom.current().nextInt(-2060, -1945)
            );
            if (!(Math.min(this.method1547(blockPos2), this.method1606(blockPos2)) <= local2315_2)) {
               local2315_2 = Math.min(this.method1547(blockPos2), this.method1606(blockPos2));
               blockPos = blockPos2;
               if (local2315_2 >= 50.0) {
                  break;
               }
            }
         }

         this.field04807.add(blockPos);
         if (this.field04807.size() > 5) {
            this.field04807.remove(0);
         }

         this.method1579(blockPos);
      }
   }

   private boolean method1598() {
      if (mc.currentScreen instanceof HandledScreen) {
         return false;
      }

      if (this.method1596()) {
         this.field04822 = System.currentTimeMillis() + 6000L;
      }

      if (System.currentTimeMillis() > this.field04822) {
         return false;
      }

      if (!this.field04793.hasExceededMillis(this.method1596() ? 1000L : 2500L)) {
         return this.field04797 == AutoWardenModule.Nested0388.HUNT || this.field04797 == AutoWardenModule.Nested0388.CHEST;
      }

      this.field04793.reset();
      this.method1603();
      BlockPos blockPos = null;
      double local2316_2 = -1.0;

      for (int index = 0; index < 360; index += 30) {
         BlockPos blockPos2 = mc.player
            .getBlockPos()
            .add(
               (int)(Math.cos(Math.toRadians(index)) * (this.method1596() ? 32.0 : 25.0)),
               0,
               (int)(Math.sin(Math.toRadians(index)) * (this.method1596() ? 32.0 : 25.0))
            );
         if (this.field04797 == AutoWardenModule.Nested0388.HUNT || this.field04797 == AutoWardenModule.Nested0388.CHEST) {
            blockPos2 = this.method1613(blockPos2);
         }

         if (!(this.method1606(blockPos2) <= local2316_2)) {
            local2316_2 = this.method1606(blockPos2);
            blockPos = blockPos2;
         }
      }

      this.method1579(blockPos);
      return this.field04797 == AutoWardenModule.Nested0388.HUNT || this.field04797 == AutoWardenModule.Nested0388.CHEST;
   }

   private boolean method1599() {
      return InventoryUtils.method0555(this::method1550) == -1 || InventoryUtils.method0555(this::method1557) == -1;
   }

   private boolean method1600() {
      for (PlayerEntity playerEntity : mc.world.getPlayers()) {
         if (playerEntity != mc.player
            && !(mc.player.squaredDistanceTo(playerEntity) > 625.0)
            && (
               mc.player.squaredDistanceTo(playerEntity) < 100.0
                  || this.method1621(playerEntity)
                  || Math.abs(
                        MathHelper.wrapDegrees(
                           playerEntity.getHeadYaw()
                              - (float)Math.toDegrees(Math.atan2(playerEntity.getX() - mc.player.getX(), mc.player.getZ() - playerEntity.getZ()))
                        )
                     )
                     < 20.0F
            )) {
            return true;
         }
      }

      return false;
   }

   private boolean method1601(Predicate<ItemStack> local2319_1, float local2319_2) {
      this.field04811 = true;
      if (mc.player.getVelocity().horizontalLengthSquared() > 0.001) {
         this.method1604();
         return true;
      }

      RotationManager.method0658()
         .method0648(new RotationAngles(mc.player.getYaw(), local2319_2), new RotationSpeed(35.0F, 35.0F, 30.0F, 30.0F), 500L);
      if (Math.abs(mc.player.getPitch() - local2319_2) > 15.0F) {
         return true;
      }

      if (InventoryUtils.method0556(local2319_1, 0, 8) == -1) {
         if (!InventoryActionQueue.method2656().method2665()) {
            InventoryActionQueue.method2656().method2662(InventoryUtils.method0555(local2319_1), 8, 1);
         }

         return true;
      } else {
         if (InventoryUtils.method0554() != InventoryUtils.method0556(local2319_1, 0, 8)) {
            InventoryUtils.method0553(InventoryUtils.method0556(local2319_1, 0, 8), false);
            return true;
         }

         if (mc.currentScreen == null) {
            mc.options.useKey.setPressed(true);
            return true;
         }

         if (!mc.player.isUsingItem()) {
            InventoryUtils.method0542(Hand.MAIN_HAND);
         }

         return true;
      }
   }

   private void method1602(ChestType chestType, AutoWardenModule.Nested0388 local2320_2) {
      if (this.field04803 == null) {
         if (mc.player.age % 5 == 0) {
            this.field04803 = this.method1590(blockPos -> this.method1549(blockPos, chestType) && !this.field04795.contains(blockPos));
            if (this.field04803 == null) {
               if (this.field04792.hasExceededMillis(20000L)) {
                  if (!this.field04795.isEmpty()) {
                     this.field04795.clear();
                     this.field04792.reset();
                  } else {
                     if (chestType == ChestType.SINGLE) {
                        ClientChat.method2267("Рядом нет сундука с ресурсами.");
                        ClientChat.method2267(
                           "Поставь маленький сундук рядом с собой и заполни его: &7Зелье невидимости&r, &7золотые морковки&r, по желанию &7Зелье скорости&r."
                        );
                        ClientChat.method2267(
                           "Ещё на Warden оставь &7/clan home&r с любым названием, а дома — большой сундук, куда бот будет складывать ресурсы."
                        );
                     } else if (local2320_2 == this.field04797) {
                        ClientChat.method2267("Не вижу двойного сундука для сдачи — он должен стоять там, куда ведёт /home home. Жду.");
                        this.field04792.reset();
                        return;
                     }

                     this.method1552(local2320_2);
                  }
               }
            } else {
               this.field04804 = this.method1619(this.field04803);
               if (this.field04797 == AutoWardenModule.Nested0388.DEPOSIT) {
                  ClientChat.method2267("Сдаю лут: двойной сундук " + this.field04803.toShortString() + ".");
               }

               this.method1579(this.field04804 != null ? this.field04804 : this.field04803);
            }
         }
      } else if (this.method1551(this.field04803, this.field04804)) {
         if (this.field04815++ > 200) {
            ClientChat.method2267(
               "Не смог открыть сундук "
                  + this.field04803.toShortString()
                  + " за 10с"
                  + (this.method1609(this.field04803) ? " (стою рядом, но клик не проходит)" : " (не могу подойти)")
                  + "."
            );
            this.field04795.add(this.field04803);
            this.method1552(local2320_2);
         } else if (!this.method1609(this.field04803)) {
            if (this.field04815 % 20 == 1) {
               this.method1579(this.field04804 != null ? this.field04804 : this.field04803);
            }
         } else {
            this.method1604();
            this.method1573(this.field04803);
         }
      }
   }

   private void method1603() {
      if (mc.options != null) {
         mc.options.useKey.setPressed(false);
      }
   }

   private void method1604() {
      if (this.field04809) {
         this.field04809 = false;
         this.field04805 = null;
         ClientChat.method2258("#stop");
      }
   }

   private int method1605(GenericContainerScreenHandler genericContainerScreenHandler) {
      for (Slot slot : genericContainerScreenHandler.slots) {
         if (slot.id < genericContainerScreenHandler.getRows() * 9 && !slot.hasStack()) {
            return slot.id;
         }
      }

      return -1;
   }

   private double method1606(BlockPos blockPos) {
      double local2324_2 = Double.MAX_VALUE;

      for (Entity entity : mc.world
         .getOtherEntities(
            mc.player, new Box(blockPos).expand(64.0), entity2 -> entity2 instanceof PlayerEntity || entity2 instanceof WardenEntity
         )) {
         local2324_2 = Math.min(local2324_2, Math.hypot(entity.getX() - blockPos.getX(), entity.getZ() - blockPos.getZ()));
      }

      return local2324_2;
   }

   private boolean method1607() {
      for (ClientBossBar clientBossBar : ((BossBarHudMixin)mc.inGameHud.getBossBarHud()).getEvents().values()) {
         if (field04778.matcher(clientBossBar.getName().getString().replaceAll("(?i)§[0-9a-fk-or]", "").trim()).matches()) {
            return true;
         }
      }

      return false;
   }

   private boolean method1608(ItemStack itemStack) {
      return this.method1557(itemStack) || this.method1550(itemStack) || this.method1618(itemStack);
   }

   private boolean method1609(BlockPos blockPos) {
      return blockPos != null && mc.player.getEyePos().distanceTo(Vec3d.ofCenter(blockPos)) <= mc.player.getBlockInteractionRange() - 1.0;
   }

   private void onHudRender(HudRenderEvent event) {
      if (mc.player != null && mc.world != null && this.isEnabled()) {
         float local2328_2 = InterfaceModule.method2247().method2243("watermark") && WatermarkHud.method2504() > 0.0F
            ? WatermarkHud.method2504() * event.method4460() + 6.0F
            : 2.0F + ((BossBarHudMixin)mc.inGameHud.getBossBarHud()).getEvents().size() * 19.0F;
         long local2328_3 = this.field04824 > 0L ? System.currentTimeMillis() - this.field04824 : 0L;
         FontText.method1405(
            FontFace.SEMI, "AutoWarden — Информация", event.method4462() / 2.0F, local2328_2, 7.0F, ThemeColors.field13665
         );
         FontText.method1405(
            FontFace.SEMI,
            System.currentTimeMillis() <= this.field04822 ? "Стадия: УБЕГАЮ ОТ ИГРОКА" : "Стадия: " + this.field04797.name(),
            event.method4462() / 2.0F,
            local2328_2 + 10.0F,
            7.0F,
            ThemeColors.field13665
         );
         FontText.method1405(
            FontFace.SEMI,
            "Время работы: " + String.format("%02d:%02d:%02d", local2328_3 / 3600000L, local2328_3 / 60000L % 60L, local2328_3 / 1000L % 60L),
            event.method4462() / 2.0F,
            local2328_2 + 20.0F,
            7.0F,
            ThemeColors.field13665
         );
         if (this.field04797 == AutoWardenModule.Nested0388.CHEST && this.field04803 != null && this.method1558(this.field04803) >= 0) {
            FontText.method1405(
               FontFace.SEMI,
               "Жду сундук: " + this.method1558(this.field04803) + "с",
               event.method4462() / 2.0F,
               local2328_2 + 31.0F,
               7.0F,
               ThemeColors.field13665
            );
         }

         if (this.field04797 == AutoWardenModule.Nested0388.HUNT) {
            FontText.method1405(
               FontFace.SEMI,
               "Слутано сундуков: " + this.field04835,
               event.method4462() / 2.0F,
               local2328_2 + 31.0F,
               7.0F,
               ThemeColors.field13665
            );
         }
      }
   }

   private boolean method1611(ItemStack itemStack) {
      return !itemStack.isOf(Items.PHANTOM_MEMBRANE) && !itemStack.isOf(Items.ELYTRA)
         ? itemStack.getName().getString().replaceAll("(?i)§[0-9a-fk-or]", "").toLowerCase().contains("снотворное")
            || itemStack.getName().getString().replaceAll("(?i)§[0-9a-fk-or]", "").toLowerCase().contains("ассасина")
            || itemStack.getName().getString().replaceAll("(?i)§[0-9a-fk-or]", "").toLowerCase().contains("святая")
            || itemStack.getName().getString().replaceAll("(?i)§[0-9a-fk-or]", "").toLowerCase().contains("радиации")
            || itemStack.getName().getString().replaceAll("(?i)§[0-9a-fk-or]", "").toLowerCase().contains("драконья")
            || itemStack.getName().getString().replaceAll("(?i)§[0-9a-fk-or]", "").toLowerCase().contains("неизбежная")
            || itemStack.getName().getString().replaceAll("(?i)§[0-9a-fk-or]", "").toLowerCase().contains("обледенения")
            || itemStack.getName().getString().replaceAll("(?i)§[0-9a-fk-or]", "").toLowerCase().contains("световая")
            || itemStack.getName().getString().replaceAll("(?i)§[0-9a-fk-or]", "").toLowerCase().contains("палладина")
         : true;
   }

   private boolean method1612(boolean local2330_1, Predicate<Slot> local2330_2) {
      if (mc.player.currentScreenHandler instanceof GenericContainerScreenHandler genericContainerScreenHandler) {
         MovementPauseController.getInstance().pauseForTicks(1);

         for (Slot slot : genericContainerScreenHandler.slots) {
            if (slot.id >= genericContainerScreenHandler.getRows() * 9 != local2330_1 && slot.hasStack() && local2330_2.test(slot)) {
               this.field04817 = 0;
               this.field04813 = true;
               if (mc.player.age % 2 == 0) {
                  InventoryUtils.method0544(slot, 0, SlotActionType.QUICK_MOVE, false);
               }

               return false;
            }
         }

         return ++this.field04817 >= 3;
      } else {
         return true;
      }
   }

   private BlockPos method1613(BlockPos blockPos) {
      return new BlockPos(MathHelper.clamp(blockPos.getX(), -2051, -1937), blockPos.getY(), MathHelper.clamp(blockPos.getZ(), -2060, -1945));
   }

   private int method1614() {
      return !this.field04796.isEmpty() && this.field04834 >= 2 ? this.field04796.get(this.field04833 % this.field04796.size()) : this.field04832;
   }

   private void method1615() {
      if (this.field04797 == AutoWardenModule.Nested0388.HUNT || this.field04797 == AutoWardenModule.Nested0388.CHEST) {
         if (!(mc.currentScreen instanceof HandledScreen) && !(mc.player.getVelocity().horizontalLengthSquared() < 0.004)) {
            if (System.currentTimeMillis() - this.field04825 > this.field04826) {
               this.field04825 = System.currentTimeMillis();
               this.field04826 = (long)MathUtils.randomFloat(420.0F, 950.0F);
               this.field04831 = MathUtils.randomFloat(9.0F, 17.0F);
               this.field04829 = MathUtils.randomFloat(-14.0F, 14.0F);
               this.field04830 = MathUtils.randomFloat(-8.0F, 16.0F);
            }

            float local2333_1 = (float)Math.toDegrees(Math.atan2(-mc.player.getVelocity().x, mc.player.getVelocity().z)) + this.field04829;
            float local2333_2 = (float)((Math.sin(mc.player.age * 0.11F) * 0.6 + Math.sin(mc.player.age * 0.27F + 1.4F) * 0.4) * 3.0);
            RotationManager.method0658()
               .method0665(
                  new RotationAngles(local2333_1 + local2333_2, this.field04830 + local2333_2 / 3.0F),
                  new RotationSpeed(this.field04831, this.field04831 * 0.6F, 30.0F, 30.0F),
                  0L,
                  RotationManager.ReturnMode.Empty
               );
         }
      }
   }

   private boolean method1616() {
      if (!(mc.currentScreen instanceof HandledScreen)
         && !this.field04811
         && !mc.player.isUsingItem()
         && !InventoryActionQueue.method2656().method2665()
         && !InventorySlotManager.method4176().method4179()
         && mc.player.age % 5 == 0) {
         if ((this.field04797 == AutoWardenModule.Nested0388.HUNT || this.field04797 == AutoWardenModule.Nested0388.CHEST)
            && InventoryUtils.method0539(Items.GLASS_BOTTLE) != -1) {
            if (InventoryUtils.method0554() != InventoryUtils.method0539(Items.GLASS_BOTTLE)) {
               InventoryUtils.method0553(InventoryUtils.method0539(Items.GLASS_BOTTLE), false);
               return false;
            } else {
               mc.player.dropSelectedItem(true);
               return false;
            }
         } else {
            if ((this.field04797 == AutoWardenModule.Nested0388.HUNT || this.field04797 == AutoWardenModule.Nested0388.CHEST)
               && InventoryUtils.method0557(Items.GLASS_BOTTLE) != -1
               && InventoryUtils.method0546(0, 8) != -1) {
               InventoryActionQueue.method2656().method2662(InventoryUtils.method0557(Items.GLASS_BOTTLE), InventoryUtils.method0546(0, 8), 1);
               return false;
            }

            if (mc.player.getMainHandStack().isEmpty()) {
               return false;
            }

            if (InventoryUtils.method0546(0, 8) != -1) {
               InventoryUtils.method0553(InventoryUtils.method0546(0, 8), false);
               return false;
            }

            if (this.method1564() != -1 && InventoryUtils.method0546(9, 35) != -1) {
               this.method1604();
               if (mc.player.getVelocity().horizontalLengthSquared() > 0.001) {
                  return true;
               }

               InventoryActionQueue.method2656().method2662(this.method1564(), InventoryUtils.method0546(9, 35), 1);
               return true;
            } else {
               return false;
            }
         }
      } else {
         return false;
      }
   }

   private void method1617() {
      this.field04812 = false;
      this.method1603();
      MovementPauseController.getInstance().pauseForTicks(-1);
      this.method1604();
      this.field04794.clear();
      if (mc.currentScreen instanceof DeathScreen) {
         mc.player.requestRespawn();
         mc.setScreen(null);
      }

      this.method1575("home", this.field04832, AutoWardenModule.Nested0388.DEPOSIT);
   }

   private boolean method1618(ItemStack itemStack) {
      return this.method1546(itemStack, StatusEffects.SPEED);
   }

   private BlockPos method1619(BlockPos blockPos) {
      BlockPos blockPos2 = null;
      double local2337_3 = Double.MAX_VALUE;

      for (int index = -1; index <= 1; index++) {
         for (int index2 = -1; index2 <= 1; index2++) {
            for (int index3 = -1; index3 <= 1; index3++) {
               if ((index != 0 || index2 != 0)
                  && this.method1541(blockPos.add(index, index3, index2))
                  && !(mc.player.squaredDistanceTo(Vec3d.ofBottomCenter(blockPos.add(index, index3, index2))) >= local2337_3)) {
                  local2337_3 = mc.player.squaredDistanceTo(Vec3d.ofBottomCenter(blockPos.add(index, index3, index2)));
                  blockPos2 = blockPos.add(index, index3, index2);
               }
            }
         }
      }

      return blockPos2;
   }

   private boolean method1620() {
      int local2338_1 = 0;

      for (int index = 0; index < 36; index++) {
         if (!mc.player.getInventory().getStack(index).isEmpty()) {
            if (this.method1557(mc.player.getInventory().getStack(index))) {
               local2338_1 += mc.player.getInventory().getStack(index).getCount();
            } else if (!this.method1608(mc.player.getInventory().getStack(index))) {
               return true;
            }
         }
      }

      return local2338_1 > 1;
   }

   private boolean method1621(PlayerEntity playerEntity) {
      for (EquipmentSlot equipmentSlot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
         if (!playerEntity.getEquippedStack(equipmentSlot).isEmpty()) {
            return true;
         }
      }

      return false;
   }

   private boolean method1622() {
      this.field04811 = false;
      if (mc.currentScreen instanceof HandledScreen) {
         this.method1603();
         return false;
      }

      boolean local2340_1 = this.field04797 == AutoWardenModule.Nested0388.HUNT
         || this.field04797 == AutoWardenModule.Nested0388.BUFF && this.method1588() == this.method1614() && !this.field04792.hasExceededMillis(20000L);
      if (local2340_1 && this.method1566() <= 240000L && InventoryUtils.method0555(this::method1550) != -1) {
         return this.method1601(this::method1550, 90.0F);
      }

      if (local2340_1 && this.method1587() <= 50000L && InventoryUtils.method0555(this::method1618) != -1) {
         return this.method1601(this::method1618, 90.0F);
      }

      if (this.field04797 != AutoWardenModule.Nested0388.HUNT
         && this.field04797 != AutoWardenModule.Nested0388.CHEST
         && mc.player.getHungerManager().getFoodLevel() < 19
         && InventoryUtils.method0555(this::method1557) != -1) {
         return this.method1601(this::method1557, -90.0F);
      }

      this.method1603();
      return false;
   }

   private enum Nested0388 {
      SUPPLY,
      BUFF,
      WARP,
      HUNT,
      CHEST,
      ARENA,
      DEPOSIT;
   }
}
