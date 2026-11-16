package tokyo.ghoul.client.features.modules.movement;

import lombok.Getter;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import tokyo.ghoul.api.event.EventListener;
import tokyo.ghoul.api.event.Listener;
import tokyo.ghoul.api.event.events.player.other.MovementInputEvent;
import tokyo.ghoul.api.event.events.player.other.UpdateEvent;
import tokyo.ghoul.api.module.Category;
import tokyo.ghoul.api.module.Module;
import tokyo.ghoul.api.module.ModuleRegister;
import tokyo.ghoul.api.utils.player.DirectionalInput;
import tokyo.ghoul.api.utils.rotation.manager.Rotation;
import tokyo.ghoul.api.utils.rotation.manager.RotationManager;
import tokyo.ghoul.api.utils.rotation.manager.RotationStrategy;
import tokyo.ghoul.api.utils.rotation.rotations.InstantRotation;
import tokyo.ghoul.api.utils.task.TaskPriority;
import tokyo.ghoul.client.ui.widget.overlay.NotifyWidget;

@Getter
@ModuleRegister(name = "Elytra Fly", description = "Управляемый полёт на элитрах", category = Category.MOVEMENT)
public class ElytraFlyModule extends Module {
    @Getter private static final ElytraFlyModule instance = new ElytraFlyModule();

    private static final double Y_SPEED = 0.0305;
    private static final boolean BLOCK_MOVEMENT = true;
    private static final boolean PITCH_LOCK = true;
    private static final boolean GROUND_CHECK = true;
    private static final boolean AUTO_SWAP = true;

    private boolean elytraEquippedByModule = false;
    private static boolean pendingChestplateSwap = false;

    public ElytraFlyModule() {
        UpdateEvent.getInstance().subscribe(new Listener<>(event -> {
            if (pendingChestplateSwap && mc.player != null) {
                if (mc.player.onGround()) {
                    ItemStack chest = mc.player.getItemBySlot(EquipmentSlot.CHEST);
                    if (chest.is(Items.ELYTRA)) {
                        tokyo.ghoul.client.features.modules.player.ElytraSwapModule.swap(tokyo.ghoul.client.features.modules.player.ElytraSwapModule.SwapItem.CHESTPLATE);
                    }
                    pendingChestplateSwap = false;
                }
            }
        }));
    }

    @Override
    public void onEnable() {
        super.onEnable();
        if (mc.player == null) return;

        if (GROUND_CHECK) {
            if (!mc.player.onGround()) {
                print("§c[Elytra Fly] Необходимо включать на земле!");
                NotifyWidget.text("Elytra Fly: включать только на земле");
                setEnabled(false);
                return;
            }
            if (mc.player.getDeltaMovement().horizontalDistance() > 0.1) {
                print("§c[Elytra Fly] Необходимо стоять на месте!");
                NotifyWidget.text("Elytra Fly: необходимо стоять на месте");
                setEnabled(false);
                return;
            }
        }

        elytraEquippedByModule = false;
        pendingChestplateSwap = false;

        if (AUTO_SWAP) {
            ItemStack chest = mc.player.getItemBySlot(EquipmentSlot.CHEST);
            if (!chest.is(Items.ELYTRA)) {
                int elytraSlot = tokyo.ghoul.client.features.modules.player.ElytraSwapModule.findBestElytraSlot();
                if (elytraSlot != -1) {
                    tokyo.ghoul.client.features.modules.player.ElytraSwapModule.swap(tokyo.ghoul.client.features.modules.player.ElytraSwapModule.SwapItem.ELYTRA);
                    elytraEquippedByModule = true;
                }
            }
        }
    }

    @Override
    public void onDisable() {
        if (AUTO_SWAP && elytraEquippedByModule && mc.player != null) {
            if (mc.player.onGround()) {
                ItemStack chest = mc.player.getItemBySlot(EquipmentSlot.CHEST);
                if (chest.is(Items.ELYTRA)) {
                    tokyo.ghoul.client.features.modules.player.ElytraSwapModule.swap(tokyo.ghoul.client.features.modules.player.ElytraSwapModule.SwapItem.CHESTPLATE);
                }
                pendingChestplateSwap = false;
            } else {
                pendingChestplateSwap = true;
            }
            elytraEquippedByModule = false;
        }
        super.onDisable();
    }

    @Override
    public void onEvent() {
        EventListener input = MovementInputEvent.getInstance().subscribe(new Listener<>(this::onInput));
        EventListener tick = UpdateEvent.getInstance().subscribe(new Listener<>(event -> onTick()));
        addEvents(input, tick);
    }

    private void onInput(MovementInputEvent.MovementInputEventData e) {
        if (mc.player == null) return;

        ItemStack chestStack = mc.player.getItemBySlot(EquipmentSlot.CHEST);
        if (!chestStack.is(Items.ELYTRA)) return;
        if (chestStack.isDamaged() && chestStack.getDamageValue() >= chestStack.getMaxDamage() - 1) return;
        if (mc.player.isInWater() || mc.player.isInLava()) return;

        if (mc.player.isFallFlying()) {
            if (BLOCK_MOVEMENT) {
                e.setDirectionalInput(DirectionalInput.NONE);
                e.setJump(false);
                e.setSneak(false);
            }
        } else {
            e.setJump(mc.player.tickCount % 2 == 0);
        }
    }

    private void onTick() {
        if (mc.player == null || mc.level == null) return;

        ItemStack chestStack = mc.player.getItemBySlot(EquipmentSlot.CHEST);
        if (!chestStack.is(Items.ELYTRA)) return;

        if (!mc.player.isFallFlying()) return;

        if (PITCH_LOCK) {
            mc.player.setXRot(0.0F);
            Rotation rotation = new Rotation(mc.player.getYRot(), 0.0F);
            RotationStrategy strategy = new RotationStrategy(new InstantRotation(), false).clientLook(true);
            RotationManager.getInstance().addRotation(rotation, strategy, TaskPriority.HIGH, this);
        }

        Vec3 velocity = mc.player.getDeltaMovement();
        mc.player.setDeltaMovement(
                velocity.x,
                velocity.y + Y_SPEED,
                velocity.z
        );
    }
}
