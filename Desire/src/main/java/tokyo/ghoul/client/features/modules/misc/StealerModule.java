package tokyo.ghoul.client.features.modules.misc;

import lombok.Getter;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.ShulkerBoxMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import tokyo.ghoul.api.event.EventListener;
import tokyo.ghoul.api.event.Listener;
import tokyo.ghoul.api.event.events.player.other.UpdateEvent;
import tokyo.ghoul.api.module.Category;
import tokyo.ghoul.api.module.Module;
import tokyo.ghoul.api.module.ModuleRegister;
import tokyo.ghoul.api.module.setting.BooleanSetting;
import tokyo.ghoul.api.module.setting.ModeSetting;
import tokyo.ghoul.api.module.setting.SliderSetting;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@ModuleRegister(name = "Stealer", description = "Быстро забирает предметы из сундуков", category = Category.OTHER)
public class StealerModule extends Module {
    @Getter private static final StealerModule instance = new StealerModule();

    private static final int FUNTIME_DELAY = 200;
    private static final int HOLYWORLD_DELAY = 220;
    private static final double MISS_CHANCE = 0.15;
    private static final int MAX_CLICKS_PER_TICK = 10;
    private static final double RAY_STEP = 0.05;

    private final ModeSetting mode = new ModeSetting("Mode").values("Funtime", "HolyWorld", "Custom");
    private final BooleanSetting miss = new BooleanSetting("Sometimes Miss").value(true)
            .setVisible(() -> mode.is("Funtime"));
    private final SliderSetting customDelay = new SliderSetting("Delay (ms)").value(150f)
            .range(1f, 1000f).step(1f).setVisible(() -> mode.is("Custom"));
    private final BooleanSetting autoClose = new BooleanSetting("Auto Close").value(true);
    private final BooleanSetting openWalls = new BooleanSetting("OpenWalls").value(false);
    private final SliderSetting range = new SliderSetting("Range").value(4f)
            .range(1f, 6f).step(0.5f).setVisible(openWalls::getValue);

    private int lastContainerId = -1;
    private long lastClick;

    public StealerModule() {
        addSettings(mode, miss, customDelay, autoClose, openWalls, range);
    }

    @Override
    public void onEvent() {
        EventListener update = UpdateEvent.getInstance().subscribe(new Listener<>(event -> {
            if (mc.player == null || mc.level == null || mc.gameMode == null) return;
            if (mc.gui.screen() instanceof AbstractContainerScreen<?> screen && isLootable(screen.getMenu())) {
                steal(screen.getMenu());
            } else {
                lastContainerId = -1;
            }
        }));
        addEvents(update);
    }

    @Override
    public void onDisable() {
        lastContainerId = -1;
        super.onDisable();
    }

    private static boolean isLootable(AbstractContainerMenu menu) {
        return menu instanceof ChestMenu || menu instanceof ShulkerBoxMenu;
    }

    private static boolean isContainer(BlockState state) {
        Block block = state.getBlock();
        return block instanceof ChestBlock || block instanceof BarrelBlock || block instanceof ShulkerBoxBlock;
    }

    private int delay() {
        return switch (mode.getValue()) {
            case "Funtime" -> FUNTIME_DELAY;
            case "HolyWorld" -> HOLYWORLD_DELAY;
            default -> customDelay.getValue().intValue();
        };
    }

    private void steal(AbstractContainerMenu menu) {
        long now = System.currentTimeMillis();
        if (menu.containerId != lastContainerId) {
            lastContainerId = menu.containerId;
            lastClick = now;
            return;
        }

        List<Slot> filled = new ArrayList<>();
        List<Slot> empty = new ArrayList<>();
        for (Slot slot : menu.slots) {
            if (slot.container instanceof Inventory) continue;
            (slot.hasItem() ? filled : empty).add(slot);
        }

        if (filled.isEmpty() || mc.player.getInventory().getFreeSlot() == -1) {
            if (autoClose.getValue()) mc.player.closeContainer();
            return;
        }

        int delay = Math.max(1, delay());
        int clicks = 0;
        while (now - lastClick >= delay && clicks < MAX_CLICKS_PER_TICK && !filled.isEmpty()) {
            Slot target;
            if (mode.is("Funtime") && miss.getValue() && !empty.isEmpty()
                    && ThreadLocalRandom.current().nextDouble() < MISS_CHANCE) {
                target = empty.get(ThreadLocalRandom.current().nextInt(empty.size()));
            } else {
                target = filled.remove(0);
            }
            mc.gameMode.handleContainerInput(menu.containerId, target.index, 0, ContainerInput.QUICK_MOVE, mc.player);
            lastClick += delay;
            clicks++;
        }
        if (now - lastClick > delay * 2L) lastClick = now;
    }

    public static boolean tryOpenThroughWalls() {
        StealerModule m = instance;
        if (!m.isEnabled() || !m.openWalls.getValue()) return false;
        if (mc.player == null || mc.level == null || mc.gameMode == null || mc.gui.screen() != null) return false;

        HitResult vanilla = mc.hitResult;
        if (vanilla instanceof BlockHitResult block && vanilla.getType() == HitResult.Type.BLOCK
                && isContainer(mc.level.getBlockState(block.getBlockPos()))) {
            return false;
        }

        BlockHitResult hit = m.rayThroughWalls();
        if (hit == null) return false;

        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, hit);
        mc.player.swing(InteractionHand.MAIN_HAND);
        return true;
    }

    private BlockHitResult rayThroughWalls() {
        Vec3 eyes = mc.player.getEyePosition();
        Vec3 look = mc.player.getViewVector(1.0F);
        double reach = range.getValue();
        Vec3 end = eyes.add(look.scale(reach));

        BlockPos last = null;
        for (double d = 0; d <= reach; d += RAY_STEP) {
            Vec3 point = eyes.add(look.scale(d));
            BlockPos pos = BlockPos.containing(point);
            if (pos.equals(last)) continue;
            last = pos;

            BlockState state = mc.level.getBlockState(pos);
            if (!isContainer(state)) continue;
            BlockHitResult hit = state.getShape(mc.level, pos).clip(eyes, end, pos);
            if (hit != null) return hit;
        }
        return null;
    }
}
