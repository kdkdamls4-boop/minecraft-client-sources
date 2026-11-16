package deluxe.client.module.misc;

import deluxe.client.setting.BooleanSetting;
import deluxe.mixin.BossBarHudMixin;
import deluxe.client.ui.ClickGuiScreen;
import deluxe.client.util.ClientChat;
import deluxe.client.util.Easings;
import deluxe.client.event.EventBus;
import deluxe.client.event.EventListener;
import deluxe.client.render.FontFace;
import deluxe.client.event.HudRenderEvent;
import deluxe.client.util.MathUtils;
import deluxe.mixin.MinecraftClientMixin3;
import deluxe.mixin.MinecraftClientMixin6;
import deluxe.client.setting.ModeSetting;
import deluxe.client.module.Module;
import deluxe.client.module.ModuleCategory;
import deluxe.client.module.ModuleInfo;
import deluxe.client.event.PacketEvent;
import deluxe.client.event.PlayerTickEvent;
import deluxe.client.util.InventoryUtils;
import deluxe.client.render.FontText;
import deluxe.client.util.EntityUtils;
import deluxe.client.storage.VillagerTaskQueue;
import deluxe.client.util.RaycastUtils;
import deluxe.client.render.ThemeColors;
import deluxe.client.util.AimPointUtils;
import deluxe.client.util.MerchantScreenAccess;
import deluxe.client.RotationAngles;
import deluxe.client.RotationManager;
import deluxe.client.RotationSpeed;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.entity.TrappedChestBlockEntity;
import net.minecraft.client.gui.screen.ingame.CraftingScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.MerchantScreen;
import net.minecraft.client.util.InputUtil;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.MerchantEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.LookAndOnGround;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.screen.CraftingScreenHandler;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.MerchantScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.village.TradeOffer;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;
import net.minecraft.world.chunk.ChunkSection;

@ModuleInfo(
   name = "AutoVillager",
   description = "Обходит жителей, скупает товар за изумруды, крафтит блоки и сдаёт их в сундук.",
   category = ModuleCategory.MISC
)
public final class AutoVillagerModule extends Module {
   private static AutoVillagerModule autoVillagerModule;
   private static final Pattern field07043 = Pattern.compile("\\d+");
   private static final String[] field07087 = new String[]{
      "#set allowBreak false",
      "#set allowPlace false",
      "#set allowSprint true",
      "#set sprintInWater true",
      "#set allowParkour true",
      "#set allowDiagonalDescend true",
      "#set allowDownward true",
      "#set costHeuristic 3.5",
      "#set primaryTimeoutMS 100",
      "#set movementTimeoutTicks 40",
      "#set freeLook true"
   };
   private static final String[] field07088 = new String[]{"#set allowBreak true", "#set allowPlace true"};
   public ModeSetting modeSetting;
   private int field07090;
   public BooleanSetting booleanSetting;
   private VillagerTaskQueue villagerTaskQueue = new VillagerTaskQueue();
   private BlockPos field07093;
   private BlockPos field07094;
   private BlockPos field07095;
   private BlockPos field07096;
   private LivingEntity field07097;
   private boolean field07098;
   private boolean field07099;
   private boolean field07100;
   private boolean field07101;
   private int field07102;
   private int field07103;
   private int field07104;
   private int field07105;
   private int field07106;
   private int field07107;
   private int field07108 = Integer.MAX_VALUE;
   private int field07109;
   private boolean field07110;
   private boolean field07111;
   private boolean field07112;
   private boolean field07113;
   private boolean field07114;
   private BlockPos field07115;
   private long field07116;
   private long field07117;
   private int field07118;
   private int field07119;
   private int field07120;
   private int field07121;
   private int field07122;
   private int field07123;
   private int field07124;
   private int field07125;
   private int field07126;
   private int field07127;
   private int field07128;
   private int field07129;
   private int field07130;
   private int field07131;
   private int field07132;
   private int field07133;
   private int field07134;
   private int field07135;
   private int field07136;
   private int field07137;
   private boolean field07138;
   private int field07139;
   private int field07140;
   private Vec3d field07141 = Vec3d.ZERO;
   private float field07142;
   private float field07143;
   private float field07144;
   private float field07145;
   private float field07146;
   private float field07147;
   private float field07148;
   private long field07149;
   private long field07150;
   private long field07151;
   private boolean field07152;
   private long field07153;
   private long field07154;
   private Set<UUID> field07155 = new HashSet<>();
   private Set<UUID> field07156 = new HashSet<>();
   private HashMap<UUID, Integer> field07157 = new HashMap<>();
   private boolean field07158;
   private boolean field07159;
   private BlockPos field07160;
   private int field07161;
   private int field07162;
   private final EventListener<PlayerTickEvent> field07163 = event -> this.onPlayerTick(event);
   private final EventListener<HudRenderEvent> field07164 = event2 -> this.onHudRender(event2);
   private final EventListener<PacketEvent> field07165 = event3 -> this.onPacket(event3);

   private void method1984() {
      mc.player
         .networkHandler
         .sendPacket(
            new LookAndOnGround(mc.player.getYaw(), mc.player.getPitch(), mc.player.isOnGround(), mc.player.horizontalCollision)
         );
   }

   private VillagerTaskQueue.Nested0471 method1987(VillagerTaskQueue.Nested0471 local3209_1) {
      local3209_1.method3139(PlayerTickEvent.class, playerTickEvent -> {
         for (String local3331_4 : field07087) {
            ClientChat.method2258(local3331_4);
         }

         return true;
      });
      this.method2026(local3209_1);
      this.method2017(local3209_1, () -> !this.field07101);
      this.method1997(local3209_1);
      this.method2014(local3209_1, true);
      int local3209_2 = this.method2110() + 2;

      for (int index = 0; index < local3209_2; index++) {
         this.method2036(local3209_1, true);
         this.method2137(local3209_1, true);
         this.method2115(local3209_1);
      }

      this.method2036(local3209_1, false);
      this.method2137(local3209_1, false);
      this.method2017(local3209_1, () -> this.field07101);
      return local3209_1;
   }

   private void method1991(BlockPos blockPos) {
      RotationManager.method0658().method0653();
      this.field07138 = true;
      ClientChat.method2258("#go " + blockPos.getX() + " " + blockPos.getY() + " " + blockPos.getZ());
   }

   private boolean method1993(BlockPos blockPos, BlockPos blockPos2) {
      if (!this.method2039(blockPos) && (blockPos2 == null || !(mc.player.squaredDistanceTo(Vec3d.ofBottomCenter(blockPos2)) < 6.0))) {
         this.field07134++;
         if (mc.player.getEntityPos().squaredDistanceTo(this.field07141) < 0.0025) {
            this.field07139++;
         } else {
            this.field07139 = 0;
         }

         this.field07141 = mc.player.getEntityPos();
         if (this.field07134 % 20 == 0 && this.field07135 < 6) {
            this.field07135++;
            this.method1991(blockPos2 != null ? blockPos2 : blockPos);
         }

         if (this.field07134 > 12) {
            this.method2072(blockPos2 != null ? blockPos2 : blockPos);
         }

         if (this.field07134 > 12 && this.field07139 > 12 && this.field07140 >= 3) {
            this.method2005();
            return true;
         } else if (this.field07134 > 600) {
            this.method2005();
            return true;
         } else {
            return false;
         }
      } else {
         this.field07134 = 0;
         return true;
      }
   }

   private void method1997(VillagerTaskQueue.Nested0471 local3219_1) {
      local3219_1.method3139(PlayerTickEvent.class, playerTickEvent -> this.method2120());
   }

   private void method1998() {
      if (this.field07114) {
         mc.options.useKey.setPressed(InputUtil.isKeyPressed(mc.getWindow(), mc.options.useKey.getDefaultKey().getCode()));
         this.field07114 = false;
      }
   }

   private boolean method1999() {
      return InventoryUtils.method0535(Items.GOLD_BLOCK) > 0;
   }

   private boolean method2000() {
      return this.method2080() > this.field07119 || InventoryUtils.method0535(Items.EMERALD) < this.field07118;
   }

   private boolean method2002(MerchantScreen merchantScreen, int local3224_2) {
      return ((MerchantScreenAccess)merchantScreen).deluxe$getSelectedIndex() != local3224_2
         ? false
         : merchantScreen.getScreenHandler().getSlot(2) != null
            && merchantScreen.getScreenHandler().getSlot(2).hasStack()
            && merchantScreen.getScreenHandler().getSlot(2).getStack().isOf(this.method2030());
   }

   private void method2003() {
      this.field07129 = 1;
      this.field07130 = 0;
      this.field07131 = 0;
      this.field07132 = 0;
   }

   private void method2005() {
      if (this.field07138) {
         this.field07138 = false;
         ClientChat.method2258("#stop");
      }
   }

   private LivingEntity method2007(boolean local3230_1) {
      return mc.world
         .getEntitiesByClass(
            MerchantEntity.class,
            mc.player.getBoundingBox().expand(128.0),
            merchantEntity -> merchantEntity.isAlive() && (!local3230_1 || !this.field07155.contains(merchantEntity.getUuid()))
         )
         .stream()
         .min(Comparator.comparingDouble(merchantEntity2 -> mc.player.squaredDistanceTo(merchantEntity2)))
         .orElse(null);
   }

   private void method2008(LivingEntity livingEntity) {
      if (livingEntity != null && livingEntity.isAlive()) {
         if (this.method2057(livingEntity)) {
            ((MinecraftClientMixin3)mc).setRightClickDelay(0);
            this.method2032();
            this.method2109(livingEntity);
            if (EntityUtils.method2803(
               livingEntity, mc.player.getEntityInteractionRange() + 1.0, mc.player.getYaw(), mc.player.getPitch()
            )) {
               EntityHitResult entityHitResult = RaycastUtils.method3292(
                  mc.player.getEntityInteractionRange() + 1.0,
                  new RotationAngles(mc.player.getYaw(), mc.player.getPitch()),
                  entity -> entity.getId() == livingEntity.getId()
               );
               if (entityHitResult == null) {
                  entityHitResult = new EntityHitResult(livingEntity, livingEntity.getBoundingBox().getCenter());
               }

               mc.crosshairTarget = entityHitResult;
               mc.targetedEntity = livingEntity;
               mc.player.networkHandler.sendPacket(PlayerInteractEntityC2SPacket.interactAt(livingEntity, false, Hand.MAIN_HAND, entityHitResult.getPos()));
               mc.player.networkHandler.sendPacket(PlayerInteractEntityC2SPacket.interact(livingEntity, false, Hand.MAIN_HAND));
               mc.interactionManager.interactEntity(mc.player, livingEntity, Hand.MAIN_HAND);
               ((MinecraftClientMixin6)mc).invokeStartUseItem();
            }
         }
      }
   }

   private String method2009() {
      if (this.field07110) {
         return String.format(
            "Перезаход через: %d:%02d",
            (int)((Math.max(0L, this.field07116 - System.currentTimeMillis()) + 999L) / 1000L) / 60,
            (int)((Math.max(0L, this.field07116 - System.currentTimeMillis()) + 999L) / 1000L) % 60
         );
      } else {
         return this.field07111
            ? String.format(
               "Перезапуск через: %d:%02d",
               (int)((Math.max(0L, this.field07117 - System.currentTimeMillis()) + 999L) / 1000L) / 60,
               (int)((Math.max(0L, this.field07117 - System.currentTimeMillis()) + 999L) / 1000L) % 60
            )
            : null;
      }
   }

   private void method2010() {
      if (mc.currentScreen instanceof ClickGuiScreen) {
         mc.setScreen(null);
      }

      this.method2012();
   }

   private void method2012() {
      if (method2049()) {
         if (mc.currentScreen instanceof HandledScreen) {
            InventoryUtils.method0549(true);
            mc.player.closeHandledScreen();
         } else if (mc.currentScreen != null) {
            mc.setScreen(null);
         }
      }
   }

   private Slot method2013(ScreenHandler screenHandler) {
      Slot slot = null;

      for (Slot slot2 : screenHandler.slots) {
         if (slot2.id >= 10
            && slot2.hasStack()
            && slot2.getStack().isOf(Items.GOLD_INGOT)
            && (slot == null || slot2.getStack().getCount() > slot.getStack().getCount())) {
            slot = slot2;
         }
      }

      return slot;
   }

   private void method2014(VillagerTaskQueue.Nested0471 local3237_1, boolean local3237_2) {
      local3237_1.method3139(PlayerTickEvent.class, playerTickEvent -> {
         if (this.field07129 == 0 && !this.field07098) {
            this.field07133 = 0;
         }

         return this.method2044(local3237_2);
      });
   }

   public static boolean method2015() {
      return autoVillagerModule != null && (autoVillagerModule.field07153 > 0L || autoVillagerModule.isEnabled() || autoVillagerModule.field07112);
   }

   private LivingEntity method2016() {
      return mc.world
         .getEntitiesByClass(
            MerchantEntity.class,
            mc.player.getBoundingBox().expand(128.0),
            merchantEntity -> merchantEntity.isAlive()
               && !this.field07155.contains(merchantEntity.getUuid())
               && this.field07157.getOrDefault(merchantEntity.getUuid(), -1) > 0
         )
         .stream()
         .max(Comparator.comparingInt(merchantEntity2 -> this.field07157.getOrDefault(merchantEntity2.getUuid(), -1)))
         .orElse(null);
   }

   private void method2017(VillagerTaskQueue.Nested0471 local3240_1, BooleanSupplier booleanSupplier) {
      BooleanSupplier booleanSupplier2 = () -> booleanSupplier.getAsBoolean() && this.method2048() > 0;
      local3240_1.method3139(PlayerTickEvent.class, playerTickEvent -> {
         if (!booleanSupplier2.getAsBoolean()) {
            return true;
         }

         if (!this.isEnabled()) {
            return true;
         }

         this.method2105();
         this.method2005();
         this.field07109 = 0;
         return true;
      });
      local3240_1.method3139(PlayerTickEvent.class, playerTickEvent2 -> {
         if (!booleanSupplier2.getAsBoolean()) {
            return true;
         } else {
            return this.isEnabled() && this.field07101 ? this.method2080() <= 0 || ++this.field07109 >= 200 : true;
         }
      });
      local3240_1.method3139(PlayerTickEvent.class, playerTickEvent3 -> {
         if (!booleanSupplier2.getAsBoolean()) {
            return true;
         }

         if (!this.isEnabled()) {
            return true;
         }

         this.method2010();
         mc.player.networkHandler.sendChatCommand("hub");
         this.field07110 = true;
         this.field07116 = System.currentTimeMillis() + 90000L;
         return true;
      });
      local3240_1.method3139(PlayerTickEvent.class, playerTickEvent4 -> {
         if (!booleanSupplier2.getAsBoolean()) {
            return true;
         } else {
            return !this.isEnabled() ? true : System.currentTimeMillis() >= this.field07116;
         }
      });
      local3240_1.method3139(PlayerTickEvent.class, playerTickEvent5 -> {
         if (!booleanSupplier2.getAsBoolean()) {
            return true;
         }

         if (!this.isEnabled()) {
            return true;
         }

         this.field07110 = false;
         this.method2010();
         this.method2121();
         this.field07113 = true;
         this.field07137 = 0;
         mc.player.networkHandler.sendChatCommand("an" + this.method2048());
         return true;
      });
      local3240_1.method3139(PlayerTickEvent.class, playerTickEvent6 -> {
         if (!booleanSupplier2.getAsBoolean()) {
            return true;
         }

         if (!this.isEnabled()) {
            return true;
         }

         if (this.method2056() == this.method2048()) {
            return true;
         }

         if (++this.field07137 % 400 == 0) {
            mc.player.networkHandler.sendChatCommand("an" + this.method2048());
         }

         return this.field07137 >= 1200;
      });
      this.method2100(local3240_1, 3);
      local3240_1.method3139(PlayerTickEvent.class, playerTickEvent7 -> {
         if (!booleanSupplier2.getAsBoolean()) {
            return true;
         }

         this.field07110 = false;
         this.field07112 = true;
         this.field07111 = true;
         this.field07117 = System.currentTimeMillis() + 5000L;
         this.method2121();
         this.setEnabled(false);
         return true;
      });
   }

   private BlockPos method2018(Predicate<BlockState> local3241_1) {
      BlockPos blockPos = null;
      double local3241_3 = Double.MAX_VALUE;

      for (int index = mc.player.getChunkPos().x - 9; index <= mc.player.getChunkPos().x + 9; index++) {
         for (int index2 = mc.player.getChunkPos().z - 9; index2 <= mc.player.getChunkPos().z + 9; index2++) {
            if (mc.world.isChunkLoaded(index, index2)) {
               for (int index3 = 0; index3 < mc.world.getChunk(index, index2).getSectionArray().length; index3++) {
                  ChunkSection chunkSection = mc.world.getChunk(index, index2).getSectionArray()[index3];
                  if (!chunkSection.isEmpty() && chunkSection.hasAny(local3241_1)) {
                     for (int index4 = 0; index4 < 16; index4++) {
                        for (int index5 = 0; index5 < 16; index5++) {
                           for (int index6 = 0; index6 < 16; index6++) {
                              if (local3241_1.test(chunkSection.getBlockState(index4, index5, index6))
                                 && !(
                                    mc.player
                                          .squaredDistanceTo(
                                             Vec3d.ofCenter(
                                                new BlockPos(
                                                   index * 16 + index4,
                                                   mc.world.sectionIndexToCoord(index3) * 16 + index5,
                                                   index2 * 16 + index6
                                                )
                                             )
                                          )
                                       > 16384.0
                                 )
                                 && !(
                                    mc.player
                                          .squaredDistanceTo(
                                             Vec3d.ofCenter(
                                                new BlockPos(
                                                   index * 16 + index4,
                                                   mc.world.sectionIndexToCoord(index3) * 16 + index5,
                                                   index2 * 16 + index6
                                                )
                                             )
                                          )
                                       >= local3241_3
                                 )) {
                                 local3241_3 = mc.player
                                    .squaredDistanceTo(
                                       Vec3d.ofCenter(
                                          new BlockPos(
                                             index * 16 + index4,
                                             mc.world.sectionIndexToCoord(index3) * 16 + index5,
                                             index2 * 16 + index6
                                          )
                                       )
                                    );
                                 blockPos = new BlockPos(
                                    index * 16 + index4,
                                    mc.world.sectionIndexToCoord(index3) * 16 + index5,
                                    index2 * 16 + index6
                                 );
                              }
                           }
                        }
                     }
                  }
               }
            }
         }
      }

      return blockPos;
   }

   private void onPlayerTick(PlayerTickEvent event) {
      if (this.method2056() > 0) {
         this.field07090 = this.method2056();
      }

      if (!this.booleanSetting.isChecked() || !this.field07113 && (!this.field07112 || System.currentTimeMillis() >= this.field07117) && !this.isEnabled()) {
         if (this.field07114) {
            this.method1998();
         }
      } else {
         this.method2073();
      }

      if (this.field07112) {
         if (System.currentTimeMillis() >= this.field07117) {
            if (mc.player != null && mc.world != null) {
               if (this.method2056() == this.method2048()) {
                  if (this.method2110() != 0 || ++this.field07161 >= 200) {
                     this.method2068();
                  }
               }
            }
         }
      }
   }

   private boolean method2021(TradeOffer tradeOffer, Item item) {
      return !tradeOffer.getSellItem().isEmpty() && tradeOffer.getSellItem().isOf(item);
   }

   private boolean method2022(Item item) {
      return item == Items.GOLD_INGOT || item == Items.GOLD_BLOCK || item == this.method2030();
   }

   private boolean method2023(ItemStack itemStack) {
      if (itemStack.get(DataComponentTypes.FOOD) == null) {
         return false;
      } else {
         return itemStack.get(DataComponentTypes.FOOD).canAlwaysEat()
            ? mc.player.getHungerManager().isNotFull()
            : mc.player.getHungerManager().getFoodLevel() <= 17;
      }
   }

   @Override
   protected void onDisable() {
      this.method1998();
      this.villagerTaskQueue.method3132();
      this.method2121();
      if (!this.field07112 && !this.field07111 && !this.field07113) {
         this.field07112 = false;
         this.field07113 = false;
         this.field07153 = 0L;
         this.field07154 = 0L;
         this.field07109 = 0;
         this.field07090 = 0;
         this.field07115 = null;
         this.field07110 = false;
         this.field07111 = false;
         this.field07116 = 0L;
         this.field07117 = 0L;
         this.field07093 = null;
         this.field07094 = null;
         this.field07095 = null;
         this.field07096 = null;
         this.field07097 = null;
         this.field07098 = false;
         this.field07099 = false;
         this.field07100 = false;
         this.field07101 = true;
         this.field07102 = 0;
         this.field07134 = 0;
         this.field07135 = 0;
         this.method2052();
         this.field07103 = 0;
         this.field07104 = 0;
         this.field07118 = 0;
         this.field07119 = 0;
         this.field07120 = 0;
         this.field07121 = 0;
         this.field07122 = 0;
         this.field07123 = 0;
         this.field07124 = 0;
         this.field07125 = 0;
         this.field07126 = 0;
         this.field07127 = 0;
         this.field07128 = 0;
         this.method2064();
         this.field07152 = false;
         this.field07155.clear();
         this.field07156.clear();
         this.field07157.clear();
         this.field07158 = false;
      } else {
         EventBus.getInstance().subscribe(this);
      }
   }

   private void method2026(VillagerTaskQueue.Nested0471 local3250_1) {
      local3250_1.method3139(PlayerTickEvent.class, playerTickEvent -> {
         if (!this.isEnabled()) {
            return true;
         }

         this.field07101 = true;
         this.field07126 = 0;
         this.field07121 = 0;
         this.field07122 = 0;
         this.field07123 = 0;
         this.field07124 = 0;
         this.field07125 = 0;
         this.field07127 = 0;
         this.method2105();
         this.field07097 = null;
         return mc.currentScreen == null || !(mc.currentScreen instanceof HandledScreen);
      });
      local3250_1.method3139(PlayerTickEvent.class, playerTickEvent2 -> {
         if (!this.isEnabled()) {
            return true;
         } else {
            this.field07097 = this.method2007(false);
            if (this.field07097 != null) {
               this.method2052();
               this.field07126 = 0;
               return true;
            } else {
               return ++this.field07126 >= 5;
            }
         }
      });
      this.method2108(local3250_1);
      this.method2033(local3250_1);
      local3250_1.method3139(PlayerTickEvent.class, playerTickEvent3 -> {
         if (mc.currentScreen instanceof MerchantScreen merchantScreen) {
            if (merchantScreen.getScreenHandler().getRecipes() != null && !merchantScreen.getScreenHandler().getRecipes().isEmpty()) {
               this.field07127 = 0;
               return true;
            } else {
               return ++this.field07127 >= 10;
            }
         } else {
            return true;
         }
      });
      local3250_1.method3139(
         PlayerTickEvent.class,
         playerTickEvent4 -> {
            if (mc.currentScreen instanceof MerchantScreen merchantScreen2
               && merchantScreen2.getScreenHandler().getRecipes() != null
               && !merchantScreen2.getScreenHandler().getRecipes().isEmpty()) {
               this.field07101 = this.method2097(merchantScreen2.getScreenHandler()) >= 0;
               if (!this.field07101) {
                  ClientChat.method2267("У проверочного жителя нет нужного трейда — цикл уйдёт в хаб на перезаход.");
               }
            }

            this.method2012();
            return true;
         }
      );
   }

   private void method2028() {
      this.method2005();
      mc.player.networkHandler.sendChatCommand("shop");
   }

   private Item method2029() {
      return this.modeSetting.isSelected("Слеза Гаста") ? null : this.method2030();
   }

   private Item method2030() {
      return this.modeSetting.isSelected("Слеза Гаста") ? Items.GHAST_TEAR : Items.GOLD_INGOT;
   }

   private boolean method2031() {
      int local3255_1 = 0;

      for (int index = 0; index < 36; index++) {
         if (mc.player.getInventory().getStack(index).isEmpty()) {
            if (++local3255_1 > 2) {
               return false;
            }
         }
      }

      return local3255_1 <= 2;
   }

   private void method2032() {
      if (mc.player.isSneaking()) {
         mc.player.setSneaking(false);
      }
   }

   private void method2033(VillagerTaskQueue.Nested0471 local3257_1) {
      local3257_1.method3139(PlayerTickEvent.class, playerTickEvent -> {
         if (!this.isEnabled() || this.field07097 == null) {
            return true;
         }

         if (!this.field07159) {
            return true;
         }

         if (mc.currentScreen instanceof MerchantScreen) {
            this.field07123 = 0;
            return true;
         }

         if (this.method2067() == null) {
            return true;
         }

         this.method2032();
         if (this.field07123 % 3 == 0) {
            this.method2008(this.method2067());
         }

         return mc.currentScreen instanceof MerchantScreen || ++this.field07123 >= 30;
      });
      local3257_1.method3139(PlayerTickEvent.class, playerTickEvent2 -> {
         if (!this.isEnabled() || this.field07097 == null) {
            return true;
         } else if (mc.currentScreen instanceof MerchantScreen) {
            RotationManager.method0658().method0653();
            this.field07127 = 0;
            return true;
         } else {
            return ++this.field07127 >= 10;
         }
      });
   }

   private Slot method2034(Item item) {
      if (mc.currentScreen instanceof HandledScreen<?> local3258_2) {
         for (int index = 0; index < this.method2041(local3258_2.getScreenHandler()); index++) {
            if (local3258_2.getScreenHandler().getSlot(index) != null
               && local3258_2.getScreenHandler().getSlot(index).hasStack()
               && local3258_2.getScreenHandler().getSlot(index).getStack().isOf(item)) {
               return local3258_2.getScreenHandler().getSlot(index);
            }
         }

         return null;
      } else {
         return null;
      }
   }

   private void method2036(VillagerTaskQueue.Nested0471 local3260_1, boolean local3260_2) {
      local3260_1.method3139(PlayerTickEvent.class, playerTickEvent -> {
         if (!this.isEnabled()) {
            return true;
         }

         this.method2105();
         this.field07103 = 0;
         this.field07104 = 0;
         this.field07102 = 0;
         this.field07099 = this.method2127() >= 9 && (!local3260_2 || this.method2047());
         if (!this.field07099) {
            this.field07093 = null;
            this.field07094 = null;
            return mc.currentScreen == null || !(mc.currentScreen instanceof HandledScreen);
         }

         this.field07093 = this.method2018(blockState -> blockState.isOf(Blocks.CRAFTING_TABLE));
         this.field07094 = this.field07093 != null ? this.method2117(this.field07093) : null;
         this.field07099 = this.field07093 != null;
         if (this.field07093 == null && !local3260_2) {
            ClientChat.method2267("Верстак не найден в радиусе 128 блоков — крафт блоков пропускаю.");
         }

         return mc.currentScreen == null || !(mc.currentScreen instanceof HandledScreen);
      });
      local3260_1.method3139(PlayerTickEvent.class, playerTickEvent2 -> {
         if (this.isEnabled() && this.field07099) {
            this.field07134 = 0;
            this.field07135 = 0;
            this.field07139 = 0;
            this.field07140 = 0;
            this.field07141 = mc.player.getEntityPos();
            this.method1991(this.field07094 != null ? this.field07094 : this.field07093);
            return true;
         } else {
            return true;
         }
      });
      local3260_1.method3139(
         PlayerTickEvent.class,
         playerTickEvent3 -> this.isEnabled() && this.field07099 && this.field07093 != null ? this.method1993(this.field07093, this.field07094) : true
      );
      local3260_1.method3139(PlayerTickEvent.class, playerTickEvent4 -> {
         if (this.field07099) {
            this.method2005();
         }

         return true;
      });
      local3260_1.method3139(PlayerTickEvent.class, playerTickEvent5 -> {
         if (!this.isEnabled() || !this.field07099 || this.field07093 == null) {
            return true;
         }

         if (mc.currentScreen instanceof CraftingScreen) {
            this.field07102 = 0;
            return true;
         }

         if (!this.method2039(this.field07093)) {
            if (this.field07102 % 20 == 1) {
               this.method1991(this.field07094 != null ? this.field07094 : this.field07093);
            }

            return ++this.field07102 >= 30;
         } else {
            if (this.field07102 % 5 == 0) {
               this.method2079(this.field07093, this.field07094);
            }

            return ++this.field07102 >= 30;
         }
      });
      local3260_1.method3139(PlayerTickEvent.class, playerTickEvent6 -> this.method2089());
      local3260_1.method3139(PlayerTickEvent.class, playerTickEvent7 -> {
         this.method2012();
         this.field07099 = false;
         InventoryUtils.method0541();
         return true;
      });
   }

   private void method2037(VillagerTaskQueue.Nested0471 local3261_1) {
      local3261_1.method3139(PlayerTickEvent.class, playerTickEvent -> {
         this.field07121 = 0;
         this.field07122 = 0;
         this.field07123 = 0;
         this.field07126 = 0;
         this.field07127 = 0;
         this.method2105();
         this.field07097 = null;
         return mc.currentScreen == null || !(mc.currentScreen instanceof HandledScreen);
      });
      local3261_1.method3139(PlayerTickEvent.class, playerTickEvent2 -> {
         if (!this.isEnabled()) {
            return true;
         }

         if (this.field07101 && !this.method2132()) {
            this.field07097 = this.method2083();
            if (this.field07097 != null) {
               this.method2052();
               this.field07126 = 0;
               return true;
            } else {
               return !this.field07156.isEmpty() ? true : ++this.field07126 >= 5;
            }
         } else {
            this.field07097 = null;
            return true;
         }
      });
      this.method2108(local3261_1);
      this.method2033(local3261_1);
      local3261_1.method3139(PlayerTickEvent.class, playerTickEvent3 -> {
         if (mc.currentScreen instanceof MerchantScreen merchantScreen) {
            if (merchantScreen.getScreenHandler().getRecipes() != null && !merchantScreen.getScreenHandler().getRecipes().isEmpty()) {
               this.field07127 = 0;
               return true;
            } else {
               return ++this.field07127 >= 10;
            }
         } else {
            return true;
         }
      });
      local3261_1.method3139(PlayerTickEvent.class, playerTickEvent4 -> {
         if (this.field07097 != null) {
            if (mc.currentScreen instanceof MerchantScreen merchantScreen2) {
               this.field07157.put(this.field07097.getUuid(), this.method2071(merchantScreen2.getScreenHandler()));
            }

            this.field07156.add(this.field07097.getUuid());
         }

         this.method2012();
         return true;
      });
   }

   private boolean method2039(BlockPos blockPos) {
      return blockPos != null && mc.player.getEyePos().distanceTo(Vec3d.ofCenter(blockPos)) <= mc.player.getBlockInteractionRange() - 0.1;
   }

   private int method2041(ScreenHandler screenHandler) {
      return screenHandler instanceof GenericContainerScreenHandler genericContainerScreenHandler ? genericContainerScreenHandler.getRows() * 9 : Math.max(0, screenHandler.slots.size() - 36);
   }

   private boolean method2044(boolean local3268_1) {
      if (!this.isEnabled() || !this.field07101 || this.method2031() || this.method2132()) {
         this.method2064();
         this.field07098 = false;
         return true;
      }

      if (local3268_1 && this.field07129 == 0 && this.method2094()) {
         this.method2064();
         this.field07098 = false;
         return true;
      }

      this.field07098 = true;
      switch (this.field07129) {
         case 0:
            if (this.field07133 >= 8) {
               this.method2064();
               this.field07098 = false;
               return true;
            }

            this.method2005();
            this.method2105();
            this.method2003();
            return false;
         case 1:
            if (mc.currentScreen instanceof HandledScreen) {
               this.field07130 = 0;
               this.field07129 = 2;
               return false;
            } else {
               this.field07130++;
               if (this.field07130 <= 3) {
                  return false;
               } else {
                  if (this.field07130 - 3 == 1 || (this.field07130 - 3) % 80 == 0) {
                     this.method2028();
                  }

                  if (this.field07130 - 3 >= 640) {
                     this.field07133++;
                     this.field07129 = 0;
                     this.field07130 = 0;
                  }

                  return false;
               }
            }
         case 2:
            if (!(mc.currentScreen instanceof HandledScreen)) {
               this.field07129 = 0;
               return false;
            } else {
               this.field07130++;
               if (this.field07130 <= 2) {
                  return false;
               } else if (this.method2029() == null) {
                  this.field07129 = 4;
                  this.field07130 = 0;
                  return false;
               } else {
                  if (this.method2034(this.method2029()) == null) {
                     if (this.field07130 >= 62) {
                        this.field07133++;
                        this.field07129 = 0;
                        this.field07130 = 0;
                     }

                     return false;
                  }

                  InventoryUtils.method0544(this.method2034(this.method2029()), 0, SlotActionType.PICKUP, false);
                  this.field07130 = 0;
                  this.field07129 = 4;
                  return false;
               }
            }
         case 3:
         default:
            this.field07129 = 0;
            return false;
         case 4:
            if (this.field07131 >= 4) {
               this.field07129 = 5;
               this.field07130 = 0;
               return false;
            } else if (!(mc.currentScreen instanceof HandledScreen)) {
               this.field07129 = 0;
               this.field07130 = 0;
               return false;
            } else if (this.field07130 < 10) {
               this.field07130++;
               return false;
            } else {
               if (this.method2034(Items.EMERALD) == null) {
                  this.field07132++;
                  if (this.field07132 >= 30) {
                     this.field07132 = 0;
                     this.field07133++;
                     this.field07129 = 0;
                     this.field07130 = 0;
                  }

                  return false;
               }

               this.field07132 = 0;
               this.field07130 = 0;
               InventoryUtils.method0544(this.method2034(Items.EMERALD), 1, SlotActionType.QUICK_MOVE, false);
               this.field07131++;
               return false;
            }
         case 5:
            if (mc.currentScreen != null) {
               this.method2012();
               return false;
            } else {
               InventoryUtils.method0541();
               if (this.method2094()) {
                  this.method2064();
                  this.field07098 = false;
                  return true;
               } else {
                  this.field07133++;
                  this.field07131 = 0;
                  this.field07129 = 0;
                  this.field07130 = 0;
                  return false;
               }
            }
      }
   }

   private boolean method2047() {
      int local3271_1 = 0;

      for (int index = 0; index < 36; index++) {
         if (mc.player.getInventory().getStack(index).isEmpty()) {
            if (++local3271_1 > 10) {
               return false;
            }
         }
      }

      return local3271_1 <= 10;
   }

   private int method2048() {
      return this.field07090;
   }

   private static boolean method2049() {
      return mc.player != null && mc.world != null && mc.interactionManager != null;
   }

   private boolean method2051(MerchantScreen merchantScreen) {
      return merchantScreen.getScreenHandler().getSlot(2) == null
         || !merchantScreen.getScreenHandler().getSlot(2).hasStack()
         || !merchantScreen.getScreenHandler().getSlot(2).getStack().isOf(this.method2030());
   }

   private void method2052() {
      this.field07142 = this.field07143 = 0.0F;
      this.field07149 = this.field07150 = 0L;
      this.field07151 = 0L;
   }

   private boolean method2053(MerchantScreen merchantScreen) {
      if (this.method2097(merchantScreen.getScreenHandler()) < 0) {
         return false;
      }

      if (this.method2002(merchantScreen, this.method2097(merchantScreen.getScreenHandler()))) {
         return true;
      }

      ((MerchantScreenAccess)merchantScreen).deluxe$selectTrade(this.method2097(merchantScreen.getScreenHandler()));
      return false;
   }

   private int method2056() {
      if (mc.world == null) {
         return -1;
      }

      ScoreboardObjective objectiveForSlot = mc.world.getScoreboard().getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR);
      if (objectiveForSlot == null) {
         return -1;
      }

      Matcher matcher = field07043.matcher(objectiveForSlot.getDisplayName().getString().replaceAll("(?i)§[0-9a-fk-or]", ""));
      int local3280_3 = -1;

      while (matcher.find()) {
         local3280_3 = Integer.parseInt(matcher.group());
      }

      return local3280_3;
   }

   private boolean method2057(LivingEntity livingEntity) {
      return this.method2104(mc.player.getEyePos(), livingEntity);
   }

   private AutoVillagerModule() {
      this.modeSetting = this.addSetting(new ModeSetting("Обмен", "Что выкупать у жителей.", "Золото", "Золото", "Слеза Гаста"));
      this.booleanSetting = this.addSetting(new BooleanSetting("Использовать еду", "Есть из левой руки во время работы и ожидания перезахода.", true));
   }

   private BlockPos method2060(LivingEntity livingEntity) {
      BlockPos blockPos = null;
      double local3285_3 = Double.MAX_VALUE;
      BlockPos blockPos2 = null;
      double local3285_6 = Double.MAX_VALUE;

      for (int index = -2; index <= 2; index++) {
         for (int index2 = -2; index2 <= 2; index2++) {
            if (index != 0 || index2 != 0) {
               for (int index3 = -1; index3 <= 1; index3++) {
                  if (this.method2077(livingEntity.getBlockPos().add(index, index3, index2))) {
                     if (this.method2104(
                        Vec3d.ofBottomCenter(livingEntity.getBlockPos().add(index, index3, index2))
                           .add(0.0, mc.player.getStandingEyeHeight(), 0.0),
                        livingEntity
                     )) {
                        if (livingEntity.getBlockPos().add(index, index3, index2).getSquaredDistance(livingEntity.getBlockPos()) * 100.0
                              + mc.player
                                 .squaredDistanceTo(Vec3d.ofBottomCenter(livingEntity.getBlockPos().add(index, index3, index2)))
                           < local3285_3) {
                           local3285_3 = livingEntity.getBlockPos().add(index, index3, index2).getSquaredDistance(livingEntity.getBlockPos())
                                 * 100.0
                              + mc.player
                                 .squaredDistanceTo(Vec3d.ofBottomCenter(livingEntity.getBlockPos().add(index, index3, index2)));
                           blockPos = livingEntity.getBlockPos().add(index, index3, index2);
                        }
                     } else if (livingEntity.getBlockPos().add(index, index3, index2).getSquaredDistance(livingEntity.getBlockPos()) * 100.0
                           + mc.player.squaredDistanceTo(Vec3d.ofBottomCenter(livingEntity.getBlockPos().add(index, index3, index2)))
                        < local3285_6) {
                        local3285_6 = livingEntity.getBlockPos().add(index, index3, index2).getSquaredDistance(livingEntity.getBlockPos())
                              * 100.0
                           + mc.player.squaredDistanceTo(Vec3d.ofBottomCenter(livingEntity.getBlockPos().add(index, index3, index2)));
                        blockPos2 = livingEntity.getBlockPos().add(index, index3, index2);
                     }
                  }
               }
            }
         }
      }

      if (blockPos != null) {
         return blockPos;
      } else {
         return blockPos2 != null ? blockPos2 : livingEntity.getBlockPos();
      }
   }

   private float method2062() {
      return 2.0F + ((BossBarHudMixin)mc.inGameHud.getBossBarHud()).getEvents().size() * 19.0F;
   }

   private void method2064() {
      this.field07129 = 0;
      this.field07130 = 0;
      this.field07131 = 0;
      this.field07132 = 0;
      this.field07133 = 0;
   }

   private void onPacket(PacketEvent event) {
      if (event.isIncoming() && event.getPacket() instanceof PlayerPositionLookS2CPacket) {
         if (this.field07113 || this.field07112) {
            if (mc.player != null && mc.world != null) {
               if (this.method2056() == this.method2048()) {
                  if (this.method2110() != 0 || ++this.field07161 >= 200) {
                     this.method2068();
                  }
               }
            }
         }
      }
   }

   private LivingEntity method2067() {
      if (this.field07097 == null) {
         return null;
      } else if (mc.world.getEntityById(this.field07097.getId()) instanceof LivingEntity livingEntity && livingEntity.isAlive()) {
         this.field07097 = livingEntity;
         return livingEntity;
      } else {
         this.field07097 = mc.world
            .getEntitiesByClass(
               MerchantEntity.class,
               mc.player.getBoundingBox().expand(128.0),
               merchantEntity -> merchantEntity.isAlive() && merchantEntity.getUuid().equals(this.field07097.getUuid())
            )
            .stream()
            .findFirst()
            .orElse(null);
         return this.field07097;
      }
   }

   private void method2068() {
      if (this.field07113 || this.field07112) {
         if (this.isEnabled()) {
            this.setEnabled(false);
         }

         this.field07112 = false;
         this.field07111 = false;
         this.field07117 = 0L;
         this.field07113 = false;
         this.field07110 = false;
         this.method1998();
         this.villagerTaskQueue.method3132();
         this.method2121();
         EventBus.getInstance().unsubscribe(this);
         this.field07158 = true;
         this.field07161 = 0;
         this.setEnabled(true);
      }
   }

   private static String method2070(long local3295_0) {
      return String.format("%02d:%02d:%02d", local3295_0 / 1000L / 3600L, local3295_0 / 1000L % 3600L / 60L, local3295_0 / 1000L % 60L);
   }

   private int method2071(MerchantScreenHandler merchantScreenHandler) {
      if (merchantScreenHandler.getRecipes() == null) {
         return -1;
      }

      int local3296_2 = -1;

      for (TradeOffer tradeOffer : merchantScreenHandler.getRecipes()) {
         if (tradeOffer != null
            && !tradeOffer.isDisabled()
            && this.method2021(tradeOffer, this.method2030())
            && this.method2112(tradeOffer)
            && tradeOffer.getSellItem().getCount() * 100 / Math.max(1, tradeOffer.getDisplayedFirstBuyItem().getCount()) > local3296_2) {
            local3296_2 = tradeOffer.getSellItem().getCount() * 100 / Math.max(1, tradeOffer.getDisplayedFirstBuyItem().getCount());
         }
      }

      return local3296_2;
   }

   private boolean method2072(BlockPos blockPos) {
      if (blockPos != null && this.field07139 > 12 && this.field07140 < 3) {
         this.field07140++;
         this.field07139 = 0;
         this.method1991(blockPos);
         return true;
      } else {
         return false;
      }
   }

   private void method2073() {
      if (mc.player != null && mc.world != null && mc.currentScreen == null) {
         if (this.method2134(mc.player.getOffHandStack()) && this.method2023(mc.player.getOffHandStack())
            || mc.player.isUsingItem() && mc.player.getActiveHand() == Hand.OFF_HAND) {
            if (mc.player.isUsingItem() && mc.player.getActiveHand() == Hand.OFF_HAND) {
               mc.options.useKey.setPressed(true);
               this.field07114 = true;
            } else {
               InventoryUtils.method0542(Hand.OFF_HAND);
               mc.options.useKey.setPressed(true);
               this.field07114 = true;
            }
         } else {
            this.method1998();
         }
      }
   }

   private boolean method2074(MerchantScreen merchantScreen) {
      return this.method2097(merchantScreen.getScreenHandler()) >= 0 && this.method2002(merchantScreen, this.method2097(merchantScreen.getScreenHandler()));
   }

   private void method2075() {
      if (this.field07097 != null) {
         this.field07155.add(this.field07097.getUuid());
      }

      this.method2012();
      InventoryUtils.method0541();
   }

   private boolean method2076(BlockPos blockPos) {
      return mc.world.getBlockState(blockPos).getCollisionShape(mc.world, blockPos).isEmpty();
   }

   private boolean method2077(BlockPos blockPos) {
      return this.method2076(blockPos)
         && this.method2076(blockPos.up())
         && !mc.world.getBlockState(blockPos.down()).getCollisionShape(mc.world, blockPos.down()).isEmpty();
   }

   private void method2078(ScreenHandler screenHandler) {
      if (this.method2102() > 0) {
         for (Slot slot : screenHandler.slots) {
            if (slot.id >= 10
               && slot.hasStack()
               && slot.getStack().isOf(Items.GOLD_INGOT)
               && slot.getStack().getCount() < slot.getStack().getMaxCount()) {
               InventoryUtils.method0544(slot, 0, SlotActionType.PICKUP, false);
               if (this.method2102() <= 0) {
                  return;
               }
            }
         }

         for (Slot slot2 : screenHandler.slots) {
            if (slot2.id >= 10 && !slot2.hasStack()) {
               InventoryUtils.method0544(slot2, 0, SlotActionType.PICKUP, false);
               return;
            }
         }
      }
   }

   private boolean method2079(BlockPos blockPos, BlockPos blockPos2) {
      this.method2032();
      RotationManager.method0658().method0653();
      Direction direction = this.method2139(blockPos, blockPos2);
      this.method2090(
         AimPointUtils.method4450(
            Vec3d.ofCenter(blockPos)
               .add(direction.getOffsetX() * 0.5, direction.getOffsetY() * 0.5, direction.getOffsetZ() * 0.5)
               .subtract(mc.player.getEyePos())
         )
      );
      this.method1984();
      mc.crosshairTarget = new BlockHitResult(
         Vec3d.ofCenter(blockPos).add(direction.getOffsetX() * 0.5, direction.getOffsetY() * 0.5, direction.getOffsetZ() * 0.5),
         direction,
         blockPos,
         false
      );
      ActionResult actionResult = mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, (BlockHitResult)mc.crosshairTarget);
      mc.player.swingHand(Hand.MAIN_HAND);
      return actionResult.isAccepted();
   }

   private int method2080() {
      return InventoryUtils.method0535(this.method2030());
   }

   private LivingEntity method2083() {
      return mc.world
         .getEntitiesByClass(
            MerchantEntity.class,
            mc.player.getBoundingBox().expand(128.0),
            merchantEntity -> merchantEntity.isAlive() && !this.field07156.contains(merchantEntity.getUuid())
         )
         .stream()
         .min(Comparator.comparingDouble(merchantEntity2 -> mc.player.squaredDistanceTo(merchantEntity2)))
         .orElse(null);
   }

   private long method2085() {
      return this.field07153 > 0L ? System.currentTimeMillis() - this.field07153 : 0L;
   }

   private void method2087(ScreenHandler screenHandler) {
      for (int index = 0;
         index < 9 && this.method2013(screenHandler) != null && this.method2013(screenHandler).getStack().getCount() >= 9;
         index++
      ) {
         InventoryUtils.method0544(this.method2013(screenHandler), 0, SlotActionType.PICKUP, false);
         InventoryUtils.method0547(-999, 0, SlotActionType.QUICK_CRAFT, false);

         for (int index2 = 1; index2 <= 9; index2++) {
            InventoryUtils.method0547(index2, 1, SlotActionType.QUICK_CRAFT, false);
         }

         InventoryUtils.method0547(-999, 2, SlotActionType.QUICK_CRAFT, false);
         this.method2078(screenHandler);
      }

      InventoryUtils.method0547(0, 0, SlotActionType.QUICK_MOVE, false);

      for (int index3 = 1; index3 <= 9; index3++) {
         if (screenHandler.getSlot(index3) != null && screenHandler.getSlot(index3).hasStack()) {
            InventoryUtils.method0547(index3, 0, SlotActionType.QUICK_MOVE, false);
         }
      }
   }

   private boolean method2088() {
      if (!this.isEnabled()) {
         this.field07100 = false;
         return true;
      }

      if (mc.player.currentScreenHandler instanceof GenericContainerScreenHandler genericContainerScreenHandler) {
         if (++this.field07106 > 200) {
            return true;
         }

         if (InventoryUtils.method0535(Items.GOLD_INGOT) + InventoryUtils.method0535(Items.GOLD_BLOCK) + this.method2080() < this.field07108) {
            this.field07108 = InventoryUtils.method0535(Items.GOLD_INGOT) + InventoryUtils.method0535(Items.GOLD_BLOCK) + this.method2080();
            this.field07107 = 0;
         }

         if (++this.field07107 > 20) {
            return true;
         }

         for (Slot slot : genericContainerScreenHandler.slots) {
            if (slot.id >= genericContainerScreenHandler.getRows() * 9 && slot.hasStack() && this.method2022(slot.getStack().getItem())) {
               this.field07105 = 0;
               if (this.field07106 % 2 == 0) {
                  InventoryUtils.method0544(slot, 0, SlotActionType.QUICK_MOVE, false);
               }

               return false;
            }
         }

         if (this.method2135()) {
            this.field07105 = 0;
            return false;
         } else {
            return ++this.field07105 >= 3;
         }
      } else {
         if (this.field07100 && this.field07106 == 0) {
            ClientChat.method2267(
               "Сундук нашёлся, но не открылся ("
                  + (this.method2039(this.field07095) ? "стою рядом, клик не открывает" : "не смог подойти")
                  + ") — сдача пропущена."
            );
         }

         return true;
      }
   }

   private boolean method2089() {
      if (!this.isEnabled()) {
         this.field07099 = false;
         return true;
      }

      if (!(mc.currentScreen instanceof CraftingScreen)) {
         if (this.field07099) {
            ClientChat.method2267(
               "Верстак нашёлся, но не открылся ("
                  + (this.method2039(this.field07093) ? "стою рядом, клик не открывает" : "не смог подойти")
                  + ") — крафт пропущен."
            );
         }

         return true;
      } else {
         if (!(mc.player.currentScreenHandler instanceof CraftingScreenHandler)) {
            return true;
         }

         if (this.method2127() < 9) {
            return true;
         }

         if (this.field07103 >= 256) {
            return true;
         }

         if (this.field07104 > 0) {
            this.field07104--;
            return false;
         }

         if (this.method2095(mc.player.currentScreenHandler) >= 9) {
            this.method2122(mc.player.currentScreenHandler);
         } else {
            this.method2087(mc.player.currentScreenHandler);
         }

         this.field07103++;
         this.field07104 = 4;
         return false;
      }
   }

   private void method2090(RotationAngles rotationAngles) {
      mc.player.setYaw(rotationAngles.method4362());
      mc.player.setPitch(MathHelper.clamp(rotationAngles.method4365(), -90.0F, 90.0F));
      mc.player.headYaw = rotationAngles.method4362();
      mc.player.bodyYaw = rotationAngles.method4362();
      mc.player.lastYaw = rotationAngles.method4362();
      mc.player.lastPitch = MathHelper.clamp(rotationAngles.method4365(), -90.0F, 90.0F);
   }

   private boolean method2094() {
      return InventoryUtils.method0535(Items.EMERALD) >= 16;
   }

   private int method2095(ScreenHandler screenHandler) {
      int local3320_2 = 0;

      for (Slot slot : screenHandler.slots) {
         if (slot.id >= 10 && slot.hasStack() && slot.getStack().isOf(Items.GOLD_INGOT)) {
            local3320_2++;
         }
      }

      return local3320_2;
   }

   private int method2097(MerchantScreenHandler merchantScreenHandler) {
      if (merchantScreenHandler.getRecipes() == null) {
         return -1;
      }

      int local3322_2 = -1;
      int local3322_3 = -1;

      for (int index = 0; index < merchantScreenHandler.getRecipes().size(); index++) {
         TradeOffer tradeOffer = merchantScreenHandler.getRecipes().get(index);
         if (tradeOffer != null
            && !tradeOffer.isDisabled()
            && this.method2021(tradeOffer, this.method2030())
            && (
               tradeOffer.getSellItem().getCount() + (this.method2112(tradeOffer) ? 10000 : 0) > local3322_3
                  || tradeOffer.getSellItem().getCount() + (this.method2112(tradeOffer) ? 10000 : 0) == local3322_3 && index < local3322_2
            )) {
            local3322_3 = tradeOffer.getSellItem().getCount() + (this.method2112(tradeOffer) ? 10000 : 0);
            local3322_2 = index;
         }
      }

      return local3322_2;
   }

   private void method2100(VillagerTaskQueue.Nested0471 local3325_1, int local3325_2) {
      if (local3325_2 > 0) {
         local3325_1.method3139(PlayerTickEvent.class, playerTickEvent -> {
            this.field07162 = 0;
            return true;
         });
         local3325_1.method3139(PlayerTickEvent.class, playerTickEvent2 -> ++this.field07162 >= local3325_2);
      }
   }

   private int method2102() {
      return mc.player.currentScreenHandler.getCursorStack().isOf(Items.GOLD_INGOT)
         ? mc.player.currentScreenHandler.getCursorStack().getCount()
         : 0;
   }

   private boolean method2104(Vec3d vec3d, LivingEntity livingEntity) {
      for (Vec3d vec3d2 : new Vec3d[]{livingEntity.getEyePos(), livingEntity.getBoundingBox().getCenter()}) {
         if (mc.world.raycast(new RaycastContext(vec3d, vec3d2, ShapeType.COLLIDER, FluidHandling.NONE, mc.player)).getType()
            == Type.MISS) {
            return true;
         }
      }

      return false;
   }

   private void method2105() {
      if (method2049()) {
         if (mc.currentScreen instanceof HandledScreen) {
            InventoryUtils.method0549(true);
            mc.player.closeHandledScreen();
         } else if (mc.currentScreen != null) {
            mc.setScreen(null);
         }
      }
   }

   private void method2108(VillagerTaskQueue.Nested0471 local3333_1) {
      local3333_1.method3139(PlayerTickEvent.class, playerTickEvent -> {
         if (!this.isEnabled() || this.field07097 == null) {
            return true;
         }

         if (this.method2067() == null) {
            return true;
         }

         this.field07124 = 0;
         this.field07125 = 0;
         this.field07139 = 0;
         this.field07140 = 0;
         this.field07159 = false;
         this.field07141 = mc.player.getEntityPos();
         this.field07160 = this.method2060(this.method2067());
         this.method1991(this.field07160);
         return true;
      });
      local3333_1.method3139(
         PlayerTickEvent.class,
         playerTickEvent2 -> {
            if (!this.isEnabled() || this.field07097 == null) {
               return true;
            }

            if (this.method2067() == null) {
               return true;
            }

            if (mc.player.squaredDistanceTo(this.method2067()) < 4.0) {
               this.field07124 = 0;
               this.field07159 = true;
               this.method2005();
               return true;
            }

            if (mc.player.squaredDistanceTo(this.method2067()) < 9.0 && this.method2057(this.method2067())) {
               this.field07124 = 0;
               this.field07159 = true;
               this.method2005();
               return true;
            }

            this.field07124++;
            if (mc.player.getEntityPos().squaredDistanceTo(this.field07141) < 0.0025) {
               this.field07139++;
            } else {
               this.field07139 = 0;
            }

            this.field07141 = mc.player.getEntityPos();
            if (this.field07124 % 20 == 0 && !this.method2060(this.method2067()).equals(this.field07160)) {
               this.field07125++;
               this.field07160 = this.method2060(this.method2067());
               this.method1991(this.field07160);
            }

            if (this.field07124 > 12) {
               this.method2072(this.field07160);
            }

            if (mc.player.distanceTo(this.method2067()) < mc.player.getEntityInteractionRange() + 1.0
               && this.method2057(this.method2067())
               && this.field07124 > 100) {
               this.field07159 = true;
               this.method2005();
               return true;
            }

            if (this.field07124 <= 600 && (this.field07124 <= 12 || this.field07139 <= 12 || this.field07140 < 3)) {
               return false;
            }

            this.method2005();
            return true;
         }
      );
      local3333_1.method3139(
         PlayerTickEvent.class,
         playerTickEvent3 -> {
            if (!this.isEnabled() || this.field07097 == null) {
               return true;
            } else if (!this.field07159) {
               return true;
            } else if (mc.currentScreen instanceof MerchantScreen) {
               return true;
            } else if (this.method2067() == null) {
               return true;
            } else {
               this.method2032();
               this.method2109(this.method2067());
               if (mc.currentScreen instanceof MerchantScreen) {
                  return true;
               } else if (EntityUtils.method2803(
                  this.method2067(), mc.player.getEntityInteractionRange() + 1.0, mc.player.getYaw(), mc.player.getPitch()
               )) {
                  this.field07121 = 0;
                  return true;
               } else if (mc.player.distanceTo(this.method2067()) < 3.0F && this.method2057(this.method2067())) {
                  this.field07121++;
                  return this.field07121 >= 6;
               } else {
                  this.field07121 = 0;
                  return ++this.field07122 >= 30;
               }
            }
         }
      );
   }

   private void method2109(LivingEntity livingEntity) {
      if (this.field07151 == 0L) {
         this.field07151 = System.currentTimeMillis();
      }

      if (System.currentTimeMillis() - this.field07149 > this.field07150) {
         this.field07149 = System.currentTimeMillis();
         this.field07150 = (long)MathUtils.randomFloat(260.0F, 620.0F);
         this.field07144 = MathUtils.randomFloat(252.0F, 274.0F);
         this.field07145 = MathUtils.randomFloat(14.0F, 24.0F);
         this.field07146 = MathUtils.randomFloat(13.0F, 21.0F);
         this.field07147 = MathUtils.randomFloat(0.04F, 0.12F) * (ThreadLocalRandom.current().nextBoolean() ? 1.0F : -1.0F);
         this.field07148 = MathUtils.randomFloat(0.4F, 0.9F);
      }

      RotationAngles rotationAngles = AimPointUtils.method4450(
         livingEntity.getEntityPos()
            .add(0.0, MathHelper.clamp(livingEntity.getHeight() * 0.62, 0.1, Math.max(0.1, livingEntity.getHeight() - 0.1)), 0.0)
            .subtract(mc.player.getEyePos())
      );
      float local3334_3 = MathHelper.wrapDegrees(rotationAngles.method4362() - mc.player.getYaw());
      float local3334_4 = MathHelper.clamp(rotationAngles.method4365(), -90.0F, 90.0F) - mc.player.getPitch();
      float local3334_5 = (float)Math.hypot(local3334_3, local3334_4);
      float local3334_6 = this.field07144
         * (float)Easings.EASE_OUT_CIRC.apply(MathUtils.clampUnit(local3334_5 / this.field07145))
         * MathHelper.lerp(MathHelper.clamp((float)(System.currentTimeMillis() - this.field07151) / 220.0F, 0.0F, 1.0F), 0.78F, 1.0F);
      this.field07142 = this.field07142
         + MathHelper.clamp(
            local3334_6 * (local3334_5 < 0.001F ? 0.0F : local3334_3 / local3334_5) * 1.25F - this.field07142, -this.field07146 * 1.2F, this.field07146 * 1.2F
         );
      this.field07143 = this.field07143
         + MathHelper.clamp(
            local3334_6 * (local3334_5 < 0.001F ? 0.0F : local3334_4 / local3334_5) + this.field07142 * this.field07147 - this.field07143,
            -this.field07146 * 0.68F,
            this.field07146 * 0.68F
         );
      this.field07142 = MathHelper.clamp(this.field07142, -Math.abs(local3334_3), Math.abs(local3334_3));
      this.field07143 = MathHelper.clamp(
         this.field07143,
         -Math.abs(local3334_4) - Math.abs(this.field07142 * this.field07147),
         Math.abs(local3334_4) + Math.abs(this.field07142 * this.field07147)
      );
      if (local3334_5 < this.field07148) {
         this.field07142 = this.field07143 = 0.0F;
      }

      RotationManager.method0658()
         .method0648(
            rotationAngles,
            new RotationSpeed(
               Math.min(Math.abs(this.field07142), EntityUtils.method2797(rotationAngles, 1.0F, 1.5F)),
               Math.min(Math.abs(this.field07143), EntityUtils.method2801(rotationAngles, 2.5F, 4.0F)),
               30.0F,
               30.0F
            ),
            1000L
         );
   }

   private int method2110() {
      return mc.world.getEntitiesByClass(MerchantEntity.class, mc.player.getBoundingBox().expand(128.0), Entity::isAlive).size();
   }

   private boolean method2112(TradeOffer tradeOffer) {
      return tradeOffer.getDisplayedFirstBuyItem().isOf(Items.EMERALD) ? true : tradeOffer.getDisplayedSecondBuyItem().isOf(Items.EMERALD);
   }

   private void method2115(VillagerTaskQueue.Nested0471 local3340_1) {
      this.method2014(local3340_1, true);
      local3340_1.method3139(PlayerTickEvent.class, playerTickEvent -> {
         this.field07121 = 0;
         this.field07122 = 0;
         this.field07123 = 0;
         this.field07126 = 0;
         this.field07127 = 0;
         this.field07128 = 0;
         this.method2105();
         this.field07097 = null;
         return mc.currentScreen == null || !(mc.currentScreen instanceof HandledScreen);
      });
      local3340_1.method3139(PlayerTickEvent.class, playerTickEvent2 -> {
         if (!this.isEnabled()) {
            return true;
         }

         if (this.field07101 && !this.method2031() && !this.method2132()) {
            this.field07097 = this.method2007(true);
            if (this.field07097 != null) {
               this.method2052();
               this.field07126 = 0;
               return true;
            } else {
               return true;
            }
         } else {
            this.field07097 = null;
            return true;
         }
      });
      this.method2108(local3340_1);
      this.method2033(local3340_1);
      local3340_1.method3139(PlayerTickEvent.class, playerTickEvent3 -> {
         if (mc.currentScreen instanceof MerchantScreen merchantScreen) {
            if (this.method2097(merchantScreen.getScreenHandler()) < 0) {
               return true;
            } else if (this.method2053(merchantScreen)) {
               this.field07128 = 0;
               return true;
            } else {
               return ++this.field07128 >= 6;
            }
         } else {
            return true;
         }
      });
      local3340_1.method3139(PlayerTickEvent.class, playerTickEvent4 -> {
         if (mc.currentScreen instanceof MerchantScreen merchantScreen2) {
            if (this.method2097(merchantScreen2.getScreenHandler()) < 0) {
               return true;
            } else if (this.method2053(merchantScreen2) && this.method2074(merchantScreen2)) {
               this.field07118 = InventoryUtils.method0535(Items.EMERALD);
               this.field07119 = this.method2080();
               this.field07120 = 0;
               InventoryUtils.method0544(merchantScreen2.getScreenHandler().getSlot(2), 0, SlotActionType.QUICK_MOVE, false);
               return true;
            } else {
               return false;
            }
         } else {
            return true;
         }
      });
      local3340_1.method3139(PlayerTickEvent.class, playerTickEvent5 -> {
         if (this.method2000()) {
            return true;
         }

         if (mc.currentScreen instanceof MerchantScreen merchantScreen3) {
            if (this.method2097(merchantScreen3.getScreenHandler()) < 0) {
               return true;
            }

            if (this.method2051(merchantScreen3)) {
               return true;
            }

            if (this.field07120 < 3) {
               this.field07120++;
               if (this.method2053(merchantScreen3) && this.method2074(merchantScreen3)) {
                  InventoryUtils.method0544(merchantScreen3.getScreenHandler().getSlot(2), 0, SlotActionType.QUICK_MOVE, false);
                  return false;
               } else {
                  return this.field07120 >= 3;
               }
            } else {
               return true;
            }
         } else {
            return true;
         }
      });
      local3340_1.method3139(PlayerTickEvent.class, playerTickEvent6 -> {
         this.method2075();
         return true;
      });
   }

   private BlockPos method2117(BlockPos blockPos) {
      BlockPos blockPos2 = null;
      double local3342_3 = Double.MAX_VALUE;

      for (int index = -1; index <= 1; index++) {
         for (int index2 = -1; index2 <= 1; index2++) {
            for (int index3 = -1; index3 <= 1; index3++) {
               if ((index != 0 || index2 != 0)
                  && this.method2077(blockPos.add(index, index3, index2))
                  && !(mc.player.squaredDistanceTo(Vec3d.ofBottomCenter(blockPos.add(index, index3, index2))) >= local3342_3)) {
                  local3342_3 = mc.player.squaredDistanceTo(Vec3d.ofBottomCenter(blockPos.add(index, index3, index2)));
                  blockPos2 = blockPos.add(index, index3, index2);
               }
            }
         }
      }

      return blockPos2;
   }

   private boolean method2118() {
      return mc.currentScreen instanceof HandledScreen && mc.player.currentScreenHandler instanceof GenericContainerScreenHandler;
   }

   private boolean method2120() {
      if (this.isEnabled() && this.field07101 && this.field07115 != null) {
         if (mc.player.squaredDistanceTo(Vec3d.ofBottomCenter(this.field07115)) < 2.5) {
            if (this.field07152) {
               this.method2005();
            }

            this.field07152 = false;
            return true;
         } else {
            if (!this.field07152) {
               this.field07136 = 0;
               this.field07139 = 0;
               this.field07140 = 0;
               this.field07141 = mc.player.getEntityPos();
               this.method1991(this.field07115);
               this.field07152 = true;
               return false;
            }

            this.field07136++;
            if (mc.player.getEntityPos().squaredDistanceTo(this.field07141) < 0.0025) {
               this.field07139++;
            } else {
               this.field07139 = 0;
            }

            this.field07141 = mc.player.getEntityPos();
            if (this.field07136 > 12) {
               this.method2072(this.field07115);
            }

            if ((this.field07136 <= 12 || this.field07139 <= 12 || this.field07140 < 3) && this.field07136 <= 600) {
               return false;
            }

            this.method2005();
            this.field07152 = false;
            return true;
         }
      } else {
         this.field07152 = false;
         return true;
      }
   }

   private void method2121() {
      if (mc.player != null) {
         this.method2005();

         for (String local3346_4 : field07088) {
            ClientChat.method2258(local3346_4);
         }

         mc.player.setSneaking(false);
      }
   }

   private void method2122(ScreenHandler screenHandler) {
      for (int index = 1; index <= 9 && this.method2013(screenHandler) != null; index++) {
         InventoryUtils.method0544(this.method2013(screenHandler), 0, SlotActionType.PICKUP, false);
         InventoryUtils.method0547(index, 0, SlotActionType.PICKUP, false);
      }

      this.method2078(screenHandler);
      InventoryUtils.method0547(0, 0, SlotActionType.QUICK_MOVE, false);

      for (int index2 = 1; index2 <= 9; index2++) {
         if (screenHandler.getSlot(index2) != null && screenHandler.getSlot(index2).hasStack()) {
            InventoryUtils.method0547(index2, 0, SlotActionType.QUICK_MOVE, false);
         }
      }
   }

   private BlockPos method2124() {
      BlockPos blockPos = null;
      double local3349_2 = Double.MAX_VALUE;

      for (int index = mc.player.getChunkPos().x - 9; index <= mc.player.getChunkPos().x + 9; index++) {
         for (int index2 = mc.player.getChunkPos().z - 9; index2 <= mc.player.getChunkPos().z + 9; index2++) {
            if (mc.world.isChunkLoaded(index, index2)) {
               for (BlockEntity blockEntity : mc.world.getChunk(index, index2).getBlockEntities().values()) {
                  if (blockEntity instanceof ChestBlockEntity
                     && !(blockEntity instanceof TrappedChestBlockEntity)
                     && !(mc.player.squaredDistanceTo(Vec3d.ofCenter(blockEntity.getPos())) > 16384.0)
                     && !(mc.player.squaredDistanceTo(Vec3d.ofCenter(blockEntity.getPos())) >= local3349_2)) {
                     local3349_2 = mc.player.squaredDistanceTo(Vec3d.ofCenter(blockEntity.getPos()));
                     blockPos = blockEntity.getPos().toImmutable();
                  }
               }
            }
         }
      }

      return blockPos;
   }

   public static AutoVillagerModule method2125() {
      if (autoVillagerModule == null) {
         autoVillagerModule = new AutoVillagerModule();
      }

      return autoVillagerModule;
   }

   @Override
   protected void onEnable() {
      this.field07093 = null;
      this.field07094 = null;
      this.field07095 = null;
      this.field07096 = null;
      this.field07097 = null;
      this.field07098 = false;
      this.field07099 = false;
      this.field07100 = false;
      this.field07101 = true;
      this.field07102 = 0;
      this.field07134 = 0;
      this.field07135 = 0;
      this.field07136 = 0;
      this.field07138 = true;
      this.method2052();
      this.field07103 = 0;
      this.field07104 = 0;
      this.field07110 = false;
      this.field07111 = false;
      this.field07112 = false;
      this.field07113 = false;
      this.method1998();
      this.field07116 = 0L;
      this.field07117 = 0L;
      this.field07118 = 0;
      this.field07119 = 0;
      this.field07120 = 0;
      this.field07121 = 0;
      this.field07122 = 0;
      this.field07123 = 0;
      this.field07124 = 0;
      this.field07125 = 0;
      this.field07126 = 0;
      this.field07127 = 0;
      this.field07128 = 0;
      this.method2064();
      this.field07152 = false;
      this.field07155.clear();
      if (!this.field07158) {
         this.field07156.clear();
         this.field07157.clear();
      }

      this.field07158 = false;
      if (mc.player != null && mc.world != null) {
         if (this.field07115 == null) {
            this.field07115 = mc.player.getBlockPos().toImmutable();
         }

         if (this.field07153 == 0L) {
            this.field07153 = System.currentTimeMillis();
         }

         this.field07154 = System.currentTimeMillis();
         this.villagerTaskQueue.method3131(this.method1987(new VillagerTaskQueue.Nested0471().method3138(12000)));
      } else {
         this.setEnabled(false);
      }
   }

   private int method2127() {
      return InventoryUtils.method0535(Items.GOLD_INGOT);
   }

   private void onHudRender(HudRenderEvent event) {
      if (mc.player != null && mc.world != null) {
         if (this.isEnabled() || this.field07112 || this.field07153 != 0L) {
            FontText.method1405(
               FontFace.SEMI, "AutoVillager — Информация", event.method4462() / 2.0F, this.method2062(), 7.0F, ThemeColors.field13665
            );
            FontText.method1405(
               FontFace.SEMI,
               "Текущее состояние: WORKING",
               event.method4462() / 2.0F,
               this.method2062() + 10.0F,
               7.0F,
               ThemeColors.field13665
            );
            FontText.method1405(
               FontFace.SEMI,
               "Время работы: " + method2070(this.method2085()),
               event.method4462() / 2.0F,
               this.method2062() + 20.0F,
               7.0F,
               ThemeColors.field13665
            );
            if (this.method2009() != null) {
               FontText.method1405(
                  FontFace.SEMI, this.method2009(), event.method4462() / 2.0F, this.method2062() + 31.0F, 7.0F, ThemeColors.field13665
               );
            }
         }
      }
   }

   private boolean method2132() {
      return this.field07154 > 0L && System.currentTimeMillis() - this.field07154 >= 300000L;
   }

   private boolean method2134(ItemStack itemStack) {
      return !itemStack.isEmpty() && itemStack.contains(DataComponentTypes.FOOD);
   }

   private boolean method2135() {
      return InventoryUtils.method0535(Items.GOLD_INGOT) > 0 || InventoryUtils.method0535(Items.GOLD_BLOCK) > 0 || this.method2080() > 0;
   }

   private void method2137(VillagerTaskQueue.Nested0471 local3363_1, boolean local3363_2) {
      local3363_1.method3139(PlayerTickEvent.class, playerTickEvent -> {
         if (!this.isEnabled()) {
            return true;
         }

         this.method2105();
         this.field07102 = 0;
         this.field07105 = 0;
         this.field07106 = 0;
         this.field07107 = 0;
         this.field07108 = Integer.MAX_VALUE;
         this.field07100 = this.method2135() && (!local3363_2 || this.method2047() || this.method1999());
         if (!this.field07100) {
            this.field07095 = null;
            this.field07096 = null;
            return mc.currentScreen == null || !(mc.currentScreen instanceof HandledScreen);
         }

         this.field07095 = this.method2124();
         this.field07096 = this.field07095 != null ? this.method2117(this.field07095) : null;
         this.field07100 = this.field07095 != null;
         if (this.field07095 == null && !local3363_2) {
            ClientChat.method2267("Сундук не найден в радиусе 128 блоков — сдача пропускается.");
         }

         return mc.currentScreen == null || !(mc.currentScreen instanceof HandledScreen);
      });
      local3363_1.method3139(PlayerTickEvent.class, playerTickEvent2 -> {
         if (this.isEnabled() && this.field07100) {
            this.field07134 = 0;
            this.field07135 = 0;
            this.field07139 = 0;
            this.field07140 = 0;
            this.field07141 = mc.player.getEntityPos();
            this.method1991(this.field07096 != null ? this.field07096 : this.field07095);
            return true;
         } else {
            return true;
         }
      });
      local3363_1.method3139(
         PlayerTickEvent.class,
         playerTickEvent3 -> this.isEnabled() && this.field07100 && this.field07095 != null ? this.method1993(this.field07095, this.field07096) : true
      );
      local3363_1.method3139(PlayerTickEvent.class, playerTickEvent4 -> {
         if (this.field07100) {
            this.method2005();
         }

         return true;
      });
      local3363_1.method3139(PlayerTickEvent.class, playerTickEvent5 -> {
         if (!this.isEnabled() || !this.field07100 || this.field07095 == null) {
            return true;
         }

         if (this.method2118()) {
            this.field07102 = 0;
            return true;
         }

         if (!this.method2039(this.field07095)) {
            if (this.field07102 % 20 == 1) {
               this.method1991(this.field07096 != null ? this.field07096 : this.field07095);
            }

            return ++this.field07102 >= 30;
         } else {
            if (this.field07102 % 5 == 0) {
               this.method2079(this.field07095, this.field07096);
            }

            return ++this.field07102 >= 30;
         }
      });
      local3363_1.method3139(PlayerTickEvent.class, playerTickEvent6 -> this.method2088());
      local3363_1.method3139(PlayerTickEvent.class, playerTickEvent7 -> {
         this.method2012();
         this.field07100 = false;
         InventoryUtils.method0541();
         return true;
      });
   }

   private Direction method2139(BlockPos blockPos, BlockPos blockPos2) {
      if (blockPos2 == null
         || blockPos2.getX() - blockPos.getX() == 0 && blockPos2.getY() - blockPos.getY() == 0 && blockPos2.getZ() - blockPos.getZ() == 0) {
         if (Math.abs(mc.player.getEyePos().subtract(Vec3d.ofCenter(blockPos)).x)
            >= Math.abs(mc.player.getEyePos().subtract(Vec3d.ofCenter(blockPos)).z)) {
            return mc.player.getEyePos().subtract(Vec3d.ofCenter(blockPos)).x >= 0.0 ? Direction.EAST : Direction.WEST;
         } else {
            return mc.player.getEyePos().subtract(Vec3d.ofCenter(blockPos)).z >= 0.0 ? Direction.SOUTH : Direction.NORTH;
         }
      } else if (Math.abs(blockPos2.getX() - blockPos.getX()) >= Math.abs(blockPos2.getY() - blockPos.getY())
         && Math.abs(blockPos2.getX() - blockPos.getX()) >= Math.abs(blockPos2.getZ() - blockPos.getZ())) {
         return blockPos2.getX() - blockPos.getX() >= 0 ? Direction.EAST : Direction.WEST;
      } else if (Math.abs(blockPos2.getZ() - blockPos.getZ()) >= Math.abs(blockPos2.getY() - blockPos.getY())) {
         return blockPos2.getZ() - blockPos.getZ() >= 0 ? Direction.SOUTH : Direction.NORTH;
      } else {
         return blockPos2.getY() - blockPos.getY() >= 0 ? Direction.UP : Direction.DOWN;
      }
   }
}
