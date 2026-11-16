package deluxe.client.module.movement;

import deluxe.mixin.ClientCommonNetworkHandlerMixin;
import deluxe.mixin.ClientPlayerInteractionManagerMixin2;
import deluxe.client.event.ClientTickEvent;
import deluxe.client.event.EventListener;
import deluxe.mixin.LivingEntityMixin;
import deluxe.client.setting.ModeSetting;
import deluxe.client.module.Module;
import deluxe.client.module.ModuleCategory;
import deluxe.client.module.ModuleInfo;
import deluxe.client.event.MouseLookEvent;
import deluxe.client.event.MovementInputEvent;
import deluxe.client.event.PacketEvent;
import deluxe.client.event.PlayerTickEvent;
import deluxe.client.util.InventoryUtils;
import deluxe.client.util.InventoryActionQueue;
import deluxe.client.util.EntityUtils;
import deluxe.client.RotationAngles;
import deluxe.client.RotationManager;
import deluxe.client.RotationSpeed;
import deluxe.client.module.movement.SimulatedPlayerInput;
import deluxe.client.util.Stopwatch;

import java.util.ArrayList;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.EquippableComponent;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.Items;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.c2s.common.KeepAliveC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action;
import net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;

@ModuleInfo(name = "Spider", description = "Позволяет лазить по стенам.", category = ModuleCategory.MOVEMENT)
public final class SpiderModule extends Module {
   private static SpiderModule spiderModule;
   public ModeSetting modeSetting;
   private final Stopwatch field09608 = new Stopwatch();
   private final Stopwatch field09609 = new Stopwatch();
   private Integer field09610;
   private BlockHitResult field09611;
   private int field09612 = -1;
   private boolean field09613;
   private boolean field09614;
   private volatile KeepAliveC2SPacket field09615;
   private volatile ClientConnection field09616;
   private volatile long field09617;
   private final EventListener<MouseLookEvent> field09618 = event -> {
      if (mc.player != null && mc.world != null && mc.interactionManager != null && this.modeSetting.isSelected("Ladders")) {
         this.onMouseLook(event);
      }
   };
   private final EventListener<ClientTickEvent> field09619 = clientTickEvent -> {
      if (clientTickEvent.isPre()) {
         if (this.field09615 != null && System.currentTimeMillis() - this.field09617 >= MathHelper.clamp(480L - 3L * this.method2851(), 0L, 800L)) {
            this.method2849();
         }

         if (mc.player != null && mc.world != null && mc.interactionManager != null) {
            if (this.field09614 && (!this.modeSetting.isSelected("FunTime") || this.field09608.hasExceededMillis(100L))) {
               this.method2845(false);
            }

            if (this.modeSetting.isSelected("FunTime")) {
               if (this.field09608.hasExceededMillis(100L)) {
                  int local4466_2 = this.method2839();
                  if (local4466_2 != -1) {
                     if (!mc.player.getMainHandStack().isOf(Items.PLAYER_HEAD)) {
                        Box box = this.method2844(SimulatedPlayerInput.method1491(1).field04608.contract(1.0E-5));
                        if (!this.method2838(box).isEmpty()
                           && this.field09612 == -1
                           && !InventoryActionQueue.method2656().method2665()
                           && this.field09609.hasExceededMillis(400L)) {
                           this.field09612 = local4466_2;
                           InventoryActionQueue.method2656().method2662(local4466_2, InventoryUtils.method0554(), 1);
                           this.field09609.reset();
                        }
                     } else {
                        ArrayList<BlockHitResult> local4466_3 = this.method2838(this.method2844(mc.player.getBoundingBox().contract(1.0E-5)));
                        if (!local4466_3.isEmpty()) {
                           this.field09611 = local4466_3.getFirst();
                           float[] floatArray = EntityUtils.method2814(this.field09611.getPos(), mc.player.getEyePos());
                           RotationManager.method0658()
                              .method0660(
                                 new RotationAngles(RotationAngles.method4366(), floatArray[1]),
                                 new RotationSpeed(360.0F, 360.0F, 20.0F, 20.0F),
                                 RotationManager.ReturnMode.Smooth
                              );
                           this.field09608.reset();
                           this.method2845(true);
                        } else {
                           if (this.field09612 != -1 && !InventoryActionQueue.method2656().method2665()) {
                              InventoryActionQueue.method2656().method2662(this.field09612, InventoryUtils.method0554(), 1);
                              this.field09612 = -1;
                              this.field09609.reset();
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   };
   private final EventListener<PlayerTickEvent> field09620 = playerTickEvent -> {
      if (this.field09611 != null && mc.player != null && mc.world != null && mc.interactionManager != null) {
         BlockHitResult blockHitResult = this.field09611;
         this.field09611 = null;
         this.method2843();
         ((ClientPlayerInteractionManagerMixin2)mc.interactionManager)
            .invokeStartPrediction(mc.world, local4461_1 -> new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, blockHitResult, local4461_1));
         this.field09613 = true;
      }
   };
   private final EventListener<MovementInputEvent> field09621 = movementInputEvent -> {
      if (this.modeSetting.isSelected("FunTime") && !this.field09608.hasExceededMillis(100L)) {
         movementInputEvent.method4442(true);
      }
   };
   private final EventListener<PacketEvent> field09622 = packetEvent -> {
      if (mc.player != null && this.modeSetting.isSelected("FunTime")) {
         if (packetEvent.isOutgoing()) {
            if (packetEvent.getPacket() instanceof KeepAliveC2SPacket keepAliveC2SPacket && this.field09615 == null && mc.getNetworkHandler() != null) {
               this.field09617 = System.currentTimeMillis();
               this.field09616 = ((ClientCommonNetworkHandlerMixin)mc.getNetworkHandler()).connection();
               this.field09615 = keepAliveC2SPacket;
               packetEvent.method4423();
            }
         } else {
            if (packetEvent.getPacket() instanceof ScreenHandlerSlotUpdateS2CPacket screenHandlerSlotUpdateS2CPacket
               && screenHandlerSlotUpdateS2CPacket.getSlot() == mc.player.getInventory().getSelectedSlot() + 36
               && mc.player.getOffHandStack().getItem() == screenHandlerSlotUpdateS2CPacket.getStack().getItem()
               && !this.field09608.hasExceededMillis(300L)) {
               packetEvent.method4423();
            }
         }
      }
   };

   public static SpiderModule method2836() {
      if (spiderModule == null) {
         spiderModule = new SpiderModule();
      }

      return spiderModule;
   }

   @Override
   protected void onEnable() {
      this.field09611 = null;
      this.field09613 = false;
      if (this.modeSetting.isSelected("FunTime") && mc.player != null) {
         StatusEffectInstance statusEffect = mc.player.getStatusEffect(StatusEffects.JUMP_BOOST);
         if (statusEffect == null || statusEffect.getDuration() <= 200) {
            int local4460_2 = InventoryUtils.method0555(
               itemStack -> {
                  EquippableComponent equippableComponent = itemStack.get(DataComponentTypes.EQUIPPABLE);
                  return equippableComponent != null
                     && equippableComponent.slot() == EquipmentSlot.FEET
                     && itemStack.getOrDefault(DataComponentTypes.LORE, LoreComponent.DEFAULT).toString().contains("Попрыгун");
               }
            );
            if (local4460_2 != -1) {
               InventoryActionQueue.method2656().method2668(local4460_2, 8, 1);
            }
         }
      }
   }

   private ArrayList<BlockHitResult> method2838(Box box) {
      ArrayList<BlockHitResult> local4462_2 = new ArrayList<>();

      for (BlockPos blockPos : BlockPos.iterate(box)) {
         for (Direction direction : Direction.values()) {
            if (direction.getOffsetY() == 0 && this.method2848(blockPos.offset(direction))) {
               BlockHitResult blockHitResult = FlightModule.method2902().method2894(blockPos.toImmutable(), direction);
               if (blockHitResult != null) {
                  local4462_2.add(blockHitResult);
               }
            }
         }
      }

      return local4462_2;
   }

   private int method2839() {
      return mc.player.getOffHandStack().isOf(Items.PLAYER_HEAD)
         ? mc.player.playerScreenHandler.slots.getLast().id
         : InventoryUtils.method0557(Items.PLAYER_HEAD);
   }

   private void onMouseLook(MouseLookEvent event) {
      int local4464_2 = InventoryUtils.method0557(Items.LADDER);
      if (local4464_2 != -1) {
         if (!mc.player.getMainHandStack().isOf(Items.LADDER)) {
            this.field09610 = local4464_2;
            InventoryUtils.method0536(local4464_2, Hand.MAIN_HAND);
         } else if (mc.options.jumpKey.isPressed()) {
            Vec3d vec3d = new Vec3d(mc.player.getX(), mc.player.getY() + 1.0, mc.player.getZ());
            BlockHitResult blockHitResult = mc.world
               .raycast(
                  new RaycastContext(
                     vec3d,
                     vec3d.add(Vec3d.of(mc.player.getHorizontalFacing().getVector()).multiply(0.5)),
                     ShapeType.COLLIDER,
                     FluidHandling.NONE,
                     mc.player
                  )
               );
            if (blockHitResult.getType() != Type.BLOCK) {
               return;
            }

            RotationManager.method0658()
               .method0660(
                  new RotationAngles(EntityUtils.method2814(blockHitResult.getPos(), mc.player.getEyePos())[0], -59.0F),
                  new RotationSpeed(60.0F, 60.0F, 10.0F, 10.0F),
                  RotationManager.ReturnMode.Empty
               );
            if (!mc.player.horizontalCollision && mc.world.getBlockState(blockHitResult.getBlockPos().offset(blockHitResult.getSide())).isAir()) {
               mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, blockHitResult);
               mc.player.swingHand(Hand.MAIN_HAND);
               this.field09608.reset();
            }

            if (mc.world
               .getBlockState(
                  BlockPos.ofFloored(
                     mc.player.getX(),
                     (mc.player.getBoundingBox().minY + mc.player.getBoundingBox().maxY) / 2.0,
                     mc.player.getZ()
                  )
               )
               .isOf(Blocks.LADDER)) {
               event.method4021(true);
               mc.player.setOnGround(true);
               event.method4020(0.6);
               event.method4424(true);
            }
         }
      }
   }

   private void method2843() {
      if (mc.world != null && mc.getNetworkHandler() != null) {
         ScoreboardObjective objectiveForSlot = mc.world.getScoreboard().getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR);
         if (objectiveForSlot != null) {
            String[] stringArray = objectiveForSlot.getDisplayName().getString().split("-");
            if (stringArray.length >= 2 && !stringArray[1].replaceAll("\\D", "").isEmpty() && Integer.parseInt(stringArray[1].replaceAll("\\D", "")) > 100) {
               ((ClientCommonNetworkHandlerMixin)mc.getNetworkHandler())
                  .connection()
                  .send(new PlayerActionC2SPacket(Action.SWAP_ITEM_WITH_OFFHAND, BlockPos.ORIGIN, Direction.DOWN), null);
            }
         }
      }
   }

   private Box method2844(Box box) {
      return box.withMaxY(box.minY + 1.0).withMinY(box.minY + 0.5);
   }

   private SpiderModule() {
      this.modeSetting = this.addSetting(new ModeSetting("Режим", "Под какую проверку подгонять залаз.", "FunTime", "FunTime", "Ladders"));
   }

   private void method2845(boolean local4470_1) {
      this.field09614 = local4470_1;
      mc.options.sneakKey.setPressed(local4470_1);
   }

   @Override
   protected void onDisable() {
      if (this.field09610 != null) {
         InventoryUtils.method0536(this.field09610, Hand.MAIN_HAND);
         this.field09610 = null;
      }

      if (this.field09612 != -1) {
         InventoryActionQueue.method2656().method2662(this.field09612, InventoryUtils.method0554(), 1);
         this.field09612 = -1;
      }

      this.field09611 = null;
      this.field09613 = false;
      if (this.field09614) {
         this.method2845(false);
      }

      this.method2849();
   }

   private boolean method2848(BlockPos blockPos) {
      BlockState blockState = mc.world.getBlockState(blockPos);
      return FlightModule.method2902().method2893(blockState) && !blockState.getOutlineShape(mc.world, BlockPos.ORIGIN).isEmpty();
   }

   private void method2849() {
      KeepAliveC2SPacket keepAliveC2SPacket = this.field09615;
      ClientConnection clientConnection = this.field09616;
      this.field09615 = null;
      this.field09616 = null;
      if (keepAliveC2SPacket != null
         && clientConnection != null
         && mc.getNetworkHandler() != null
         && ((ClientCommonNetworkHandlerMixin)mc.getNetworkHandler()).connection() == clientConnection) {
         clientConnection.send(keepAliveC2SPacket, null);
      }
   }

   public void method2850() {
      if (this.isEnabled() && this.modeSetting.isSelected("FunTime") && mc.player != null && this.field09613) {
         this.field09613 = false;
         this.method2843();
         mc.player
            .setVelocity(mc.player.getVelocity().x, ((LivingEntityMixin)mc.player).invokeGetJumpPower(), mc.player.getVelocity().z);
         mc.player.setOnGround(true);
      }
   }

   private int method2851() {
      return mc.player != null
            && mc.getNetworkHandler() != null
            && mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid()) != null
         ? mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid()).getLatency()
         : 0;
   }

   public boolean method2853() {
      return this.isEnabled() && this.modeSetting.isSelected("FunTime") ? !this.field09608.hasExceededMillis(100L) : false;
   }
}
