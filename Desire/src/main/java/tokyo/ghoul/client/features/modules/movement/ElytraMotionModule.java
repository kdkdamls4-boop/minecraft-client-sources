package tokyo.ghoul.client.features.modules.movement;

import lombok.Getter;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.Items;
import tokyo.ghoul.api.event.EventListener;
import tokyo.ghoul.api.event.Listener;
import tokyo.ghoul.api.event.events.player.other.MovementInputEvent;
import tokyo.ghoul.api.event.events.player.other.UpdateEvent;
import tokyo.ghoul.api.module.Category;
import tokyo.ghoul.api.module.Module;
import tokyo.ghoul.api.module.ModuleRegister;
import tokyo.ghoul.api.module.setting.BooleanSetting;
import tokyo.ghoul.api.module.setting.SliderSetting;
import tokyo.ghoul.api.utils.player.DirectionalInput;
import tokyo.ghoul.client.features.modules.combat.AuraModule;

@ModuleRegister(name = "Elytra Motion", description = "Контроль движения на элитрах", category = Category.MOVEMENT)
public class ElytraMotionModule extends Module {
    @Getter private static final ElytraMotionModule instance = new ElytraMotionModule();

    private final SliderSetting distance = new SliderSetting("Distance").value(1.5f).range(1f, 3f).step(0.1f);
    private final BooleanSetting autoFirework = new BooleanSetting("Auto firework").value(true);
    private long lastFireworkTime;

    public ElytraMotionModule() {
        addSettings(distance, autoFirework);
    }

    private boolean elytraChaseActive() {
        AuraModule aura = AuraModule.getInstance();
        return aura.isEnabled() && aura.blockPos;
    }

    private boolean airStuckActive() {
        return AirStuckModule.getInstance().isFrozen();
    }

    private boolean quickElytraActive() {
        return false;
    }

    @Override
    public void onEvent() {
        EventListener tick = UpdateEvent.getInstance().subscribe(new Listener<>(event -> onTick()));
        EventListener input = MovementInputEvent.getInstance().subscribe(new Listener<>(event -> onInput(event)));
        addEvents(tick, input);
    }

    private void onTick() {
        if (mc.player == null || mc.level == null || !mc.player.isFallFlying()) return;
        if (airStuckActive() || quickElytraActive()) return;
        AuraModule aura = AuraModule.getInstance();
        LivingEntity target = aura.isEnabled() ? aura.target : null;
        if (target == null || elytraChaseActive()) return;
        if (!shouldFreeze(target)) return;

        if (System.currentTimeMillis() - lastFireworkTime >= 1000L && autoFirework.getValue()) {
            useFirework();
            lastFireworkTime = System.currentTimeMillis();
        }

        double lookPitch = Math.toRadians(mc.player.getXRot());
        double cosPitch = Math.cos(lookPitch);
        double glide = 0.08 * (1.0 - 0.75 * cosPitch * cosPitch);
        mc.player.setDeltaMovement(0.0, glide, 0.0);
    }

    private void onInput(MovementInputEvent.MovementInputEventData event) {
        if (mc.player == null || mc.level == null || !mc.player.isFallFlying()) return;
        if (airStuckActive() || quickElytraActive()) return;
        AuraModule aura = AuraModule.getInstance();
        LivingEntity target = aura.isEnabled() ? aura.target : null;
        if (target == null || elytraChaseActive()) return;
        if (shouldFreeze(target)) {
            event.setDirectionalInput(DirectionalInput.NONE);
        }
    }

    private boolean shouldFreeze(LivingEntity target) {
        if (target == null || !mc.player.isFallFlying()) return false;
        double dx = target.getX() - target.xo;
        double dy = target.getY() - target.yo;
        double dz = target.getZ() - target.zo;
        double speed = Math.sqrt(dx * dx + dy * dy + dz * dz) * 20.0;
        float range = distance.getValue() + (target.isFallFlying() ? 0.5F : 0.0F);
        return target.distanceTo(mc.player) < range && speed < 25.0;
    }

    private void useFirework() {
        Inventory inventory = mc.player.getInventory();
        int currentSlot = inventory.getSelectedSlot();
        for (int i = 0; i < 9; i++) {
            if (!inventory.getItem(i).is(Items.FIREWORK_ROCKET)) continue;
            if (i != currentSlot) mc.player.connection.send(new ServerboundSetCarriedItemPacket(i));
            mc.player.connection.send(new ServerboundUseItemPacket(InteractionHand.MAIN_HAND,
                    mc.player.inventoryMenu.getStateId(), mc.player.getYRot(), mc.player.getXRot()));
            if (i != currentSlot) mc.player.connection.send(new ServerboundSetCarriedItemPacket(currentSlot));
            return;
        }
        int slot = -1;
        for (int i = 9; i < 45; i++) {
            if (mc.player.inventoryMenu.getSlot(i).getItem().is(Items.FIREWORK_ROCKET)) {
                slot = i;
                break;
            }
        }
        if (slot < 0) return;
        int nextSlot = (currentSlot + 1) % 9;
        mc.gameMode.handleContainerInput(mc.player.inventoryMenu.containerId, slot, nextSlot, ContainerInput.SWAP, (Player) mc.player);
        mc.player.connection.send(new ServerboundSetCarriedItemPacket(nextSlot));
        mc.player.connection.send(new ServerboundUseItemPacket(InteractionHand.MAIN_HAND,
                mc.player.inventoryMenu.getStateId(), mc.player.getYRot(), mc.player.getXRot()));
        mc.player.connection.send(new ServerboundSetCarriedItemPacket(currentSlot));
        mc.gameMode.handleContainerInput(mc.player.inventoryMenu.containerId, slot, nextSlot, ContainerInput.SWAP, (Player) mc.player);
    }
}
