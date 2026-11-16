package tokyo.ghoul.client.features.modules.combat;

import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.network.protocol.game.ClientboundSetHealthPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.ThrownTrident;
import net.minecraft.world.entity.vehicle.minecart.MinecartTNT;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import tokyo.ghoul.api.event.EventListener;
import tokyo.ghoul.api.event.Listener;
import tokyo.ghoul.api.event.events.client.PacketEvent;
import tokyo.ghoul.api.event.events.client.TickEvent;
import tokyo.ghoul.api.event.events.player.other.MovementInputEvent;
import tokyo.ghoul.api.event.events.player.other.UpdateEvent;
import tokyo.ghoul.api.module.Category;
import tokyo.ghoul.api.module.Module;
import tokyo.ghoul.api.module.ModuleRegister;
import tokyo.ghoul.api.module.setting.BooleanSetting;
import tokyo.ghoul.api.module.setting.ModeSetting;
import tokyo.ghoul.api.module.setting.MultiBooleanSetting;
import tokyo.ghoul.api.module.setting.SliderSetting;
import tokyo.ghoul.api.system.configs.FriendManager;
import tokyo.ghoul.api.utils.inventory.InventoryActionManager;
import tokyo.ghoul.api.utils.inventory.InventoryActions;
import tokyo.ghoul.api.utils.inventory.SphereItems;
import tokyo.ghoul.api.utils.other.ServerUtil;
import tokyo.ghoul.api.utils.other.TickPacketOrderTracker;
import tokyo.ghoul.api.utils.player.DirectionalInput;
import tokyo.ghoul.api.utils.player.MoveUtil;
import tokyo.ghoul.inject.accessor.MinecraftAccessor;
import tokyo.ghoul.inject.accessor.MultiPlayerGameModeAccessor;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@ModuleRegister(name = "Auto Totem", description = "Автоматическая установка тотема", category = Category.COMBAT)
public class AutoTotemModule extends Module {
    @Getter private static final AutoTotemModule instance = new AutoTotemModule();

    private static final String CHECK_GOLDEN_HEARTS = "Золотые сердца";
    private static final String CHECK_CRYSTALS = "Кристаллы";
    private static final String CHECK_ANCHOR = "Якорь";
    private static final String CHECK_FALL = "Падение";

    private static final String OPTION_SWAP_BACK = "Возвращать предмет";
    private static final String OPTION_NO_BALL_SWITCH = "Не менять шар";
    private static final String OPTION_SAVE_ENCHANTED = "Сохранять зачарованные";
    private static final String OPTION_CHECK_COOLDOWN = "Проверять кулдаун";
    private static final String OPTION_NO_EAT_SWITCH = "Не менять при еде";

    private final SliderSetting health = new SliderSetting("Здоровье").value(4.0F).range(2.0F, 20.0F).step(1.0F);
    private final SliderSetting healthElytra = new SliderSetting("Здоровье на элитре").value(4.0F).range(4.0F, 20.0F).step(1.0F);
    private final MultiBooleanSetting checks = new MultiBooleanSetting("Проверки").value(
            new BooleanSetting(CHECK_GOLDEN_HEARTS).value(true),
            new BooleanSetting(CHECK_CRYSTALS).value(false),
            new BooleanSetting(CHECK_ANCHOR).value(false),
            new BooleanSetting(CHECK_FALL).value(true)
    );
    private final MultiBooleanSetting options = new MultiBooleanSetting("Опции").value(
            new BooleanSetting(OPTION_SWAP_BACK).value(true),
            new BooleanSetting(OPTION_NO_BALL_SWITCH).value(false),
            new BooleanSetting(OPTION_SAVE_ENCHANTED).value(true),
            new BooleanSetting(OPTION_CHECK_COOLDOWN).value(true),
            new BooleanSetting(OPTION_NO_EAT_SWITCH).value(false)
    );
    private final BooleanSetting talisman = new BooleanSetting("Талисман").value(false);
    private final ModeSetting talismanType = new ModeSetting("Тип талисмана").value("Талисман Крушителя")
            .values("Талисман Крушителя", "Талисман Карателя", "Талисман Ярости", "Талисман Раздора",
                    "Талисман Демона", "Талисман Вихря", "Талисман Тирана", "Талисман Мрака")
            .setVisible(talisman::getValue);
    private final BooleanSetting mace = new BooleanSetting("Булава").value(true);
    private final SliderSetting distMace = new SliderSetting("Дистанция булавы").value(6.0F).range(4.0F, 16.0F).step(1.0F)
            .setVisible(mace::getValue);
    private final SliderSetting distMaceY = new SliderSetting("Высота булавы").value(12.0F).range(4.0F, 32.0F).step(1.0F)
            .setVisible(mace::getValue);
    private final SliderSetting hpMace = new SliderSetting("Здоровье от булавы").value(12.0F).range(5.0F, 20.0F).step(1.0F)
            .setVisible(mace::getValue);
    private final BooleanSetting maceWalls = new BooleanSetting("Булава сквозь стены").value(true)
            .setVisible(mace::getValue);
    private final BooleanSetting macePvp = new BooleanSetting("Булава только в PvP").value(false)
            .setVisible(mace::getValue);
    private final BooleanSetting tnt = new BooleanSetting("Динамит").value(false);
    private final SliderSetting distTnt = new SliderSetting("Дистанция до динамита").value(10.0F).range(4.0F, 128.0F).step(1.0F)
            .setVisible(tnt::getValue);
    private final BooleanSetting trident = new BooleanSetting("Трезубец").value(false);
    private final SliderSetting distTrident = new SliderSetting("Дистанция до трезубца").value(10.0F).range(4.0F, 128.0F).step(1.0F)
            .setVisible(trident::getValue);

    private final Map<UUID, Double> peakYByPlayer = new HashMap<>();
    private final Set<UUID> airbornePlayers = new HashSet<>();
    private int lastSourceSlot;
    private int swapBackSlot = -1;
    private int cancelMoveTicks;
    private long lastTotemPopTime;
    private long lastSwapTime;
    private boolean swapInProgress;

    public AutoTotemModule() {
        addSettings(health, healthElytra, checks, options, talisman, talismanType,
                mace, distMace, distMaceY, hpMace, maceWalls, macePvp, tnt, distTnt, trident, distTrident);
    }

    @Override
    public void onEvent() {
        EventListener input = MovementInputEvent.getInstance().subscribe(new Listener<>(-5, this::onMoveInput));
        EventListener tick = TickEvent.getInstance().subscribe(new Listener<>(5, event -> onWorldTick()));
        EventListener playerTick = UpdateEvent.getInstance().subscribe(new Listener<>(5, event -> onPlayerTick()));
        EventListener packet = PacketEvent.getInstance().subscribe(new Listener<>(this::onPacket));
        addEvents(input, tick, playerTick, packet);
    }

    @Override
    public void onEnable() {
        reset();
        super.onEnable();
    }

    @Override
    public void onDisable() {
        reset();
        super.onDisable();
    }

    private void reset() {
        swapBackSlot = -1;
        cancelMoveTicks = 0;
        lastTotemPopTime = 0L;
        lastSwapTime = 0L;
        swapInProgress = false;
        peakYByPlayer.clear();
    }

    /** Модуль сейчас занимает левую руку (CrystalAura, использование через левую руку). */
    public boolean isTotemPending() {
        if (!isEnabled() || ServerUtil.isNullWorld()) return false;
        return swapInProgress || isInDanger() && !isValidTotem(mc.player.getOffhandItem()) && findTotemSlot() != -1;
    }

    public static boolean isBusyStatic() {
        return instance != null && instance.isTotemPending();
    }

    public boolean isSwapping() {
        if (!isEnabled() || !ServerUtil.isDonut() || ServerUtil.isNullWorld()) return false;
        return swapInProgress || isInDanger() && !isValidTotem(mc.player.getOffhandItem()) && findTotemSlot() != -1;
    }

    private void onMoveInput(MovementInputEvent.MovementInputEventData event) {
        if (cancelMoveTicks > 0) {
            event.setDirectionalInput(DirectionalInput.NONE);
            event.setJump(false);
            event.setSneak(false);
            cancelMoveTicks--;
        }
    }

    private void onWorldTick() {
        swapInProgress = false;
        if (!ServerUtil.isNullWorld()) {
            updatePeakHeights();
            update();
        }
    }

    private void onPlayerTick() {
        if (ServerUtil.isDonut() && !ServerUtil.isNullWorld()) {
            update();
        }
    }

    private void onPacket(PacketEvent.PacketEventData event) {
        if (!event.isReceive()) return;

        ClientboundEntityEventPacket pop = event.packet() instanceof ClientboundEntityEventPacket status && status.getEventId() == 35 ? status : null;
        if (pop != null || event.packet() instanceof ClientboundSetHealthPacket
                || event.packet() instanceof ClientboundContainerSetSlotPacket
                || event.packet() instanceof ClientboundContainerSetContentPacket) {
            mc.execute(() -> {
                if (!isEnabled() || ServerUtil.isNullWorld()) return;
                if (pop != null && pop.getEntity(mc.level) == mc.player) {
                    lastTotemPopTime = System.currentTimeMillis();
                }
                update();
            });
        }
    }

    private void update() {
        if (!ServerUtil.isDonut() && System.currentTimeMillis() - lastSwapTime < 300L) return;

        boolean danger = isInDanger();
        boolean totemInMainHand = ServerUtil.isDonut() && danger
                && !isValidTotem(mc.player.getOffhandItem()) && isValidTotem(mc.player.getMainHandItem());

        if (totemInMainHand && swapHands()) {
            lastSwapTime = System.currentTimeMillis();
        } else if (danger && !isHoldingTotem() && equipTotem()) {
            lastSwapTime = System.currentTimeMillis();
        } else {
            if (!danger && swapBackSlot != -1) {
                ItemStack offhand = mc.player.getOffhandItem();
                boolean canReturn = offhand.isEmpty() || offhand.is(Items.TOTEM_OF_UNDYING);
                if (options.isEnabled(OPTION_SWAP_BACK) && canReturn) {
                    if (!mc.player.isUsingItem() && swapSlotToOffhand(swapBackSlot)) {
                        swapBackSlot = -1;
                        return;
                    }
                } else {
                    swapBackSlot = -1;
                }
            }

            if (ServerUtil.isDonut()) {
                refillHotbarTotem();
            }
        }
    }

    private void refillHotbarTotem() {
        if (mc.player.getInventory().getItem(8).is(Items.TOTEM_OF_UNDYING)) return;

        int slot = findInventoryTotem();
        if (slot == -1 || mc.player.isUsingItem() || InventoryActionManager.INSTANCE.isBusy()) return;

        int hotbar = findHotbarIndex(Items.TOTEM_OF_UNDYING);
        if (hotbar != -1 && isMovementActive()) return;

        if (moveToHotbarSlot(slot) && lastSourceSlot == 8) {
            lastSourceSlot = slot;
        }
    }

    private boolean moveToHotbarSlot(int slot) {
        if (!isInventoryOpen()) return false;

        swapInProgress = true;
        if (isMovementActive()) {
            cancelMoveTicks = 1;
            return false;
        }

        InventoryActions.click(slot, 8, ContainerInput.SWAP);
        InventoryActions.sendFakeClick();
        mc.player.connection.send(new ServerboundContainerClosePacket(0));
        if (swapBackSlot == 44) {
            swapBackSlot = slot;
        }
        return true;
    }

    private int findInventoryTotem() {
        Inventory inventory = mc.player.getInventory();
        boolean saveEnchanted = options.isEnabled(OPTION_SAVE_ENCHANTED);
        int glint = -1;
        for (int i = 9; i < 36; i++) {
            ItemStack stack = inventory.getItem(i);
            if (!stack.is(Items.TOTEM_OF_UNDYING)) continue;
            if (!saveEnchanted || !stack.hasFoil()) return i;
            if (glint == -1) glint = i;
        }
        return glint;
    }

    private boolean equipTotem() {
        if (options.isEnabled(OPTION_CHECK_COOLDOWN) && isTotemOnCooldown()) return false;

        int slot = findTotemSlot();
        if (slot == -1) return false;
        if (options.isEnabled(OPTION_NO_EAT_SWITCH) && mc.player.isUsingItem()) return false;

        boolean hadOffhand = !mc.player.getOffhandItem().isEmpty();
        if (!swapSlotToOffhand(slot)) return false;

        if (mc.player.isUsingItem()) {
            mc.gameMode.releaseUsingItem(mc.player);
            ((MinecraftAccessor) mc).setRightClickDelay(2);
        }

        if (options.isEnabled(OPTION_SWAP_BACK) && swapBackSlot == -1 && hadOffhand) {
            swapBackSlot = lastSourceSlot < 9 ? lastSourceSlot + 36 : lastSourceSlot;
        }

        if (ServerUtil.isDonut() && lastSourceSlot == 8) {
            refillHotbarTotem();
        }
        return true;
    }

    private boolean swapHands() {
        swapInProgress = true;
        if (!TickPacketOrderTracker.isTickClean()) return false;

        mc.player.connection.send(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.SWAP_ITEM_WITH_OFFHAND, BlockPos.ZERO, Direction.DOWN));
        ItemStack main = mc.player.getMainHandItem();
        mc.player.setItemInHand(InteractionHand.MAIN_HAND, mc.player.getOffhandItem());
        mc.player.setItemInHand(InteractionHand.OFF_HAND, main);
        return true;
    }

    // slot — индекс слота меню игрока (хотбар 36..44)
    private boolean swapSlotToOffhand(int slot) {
        boolean donut = ServerUtil.isDonut();
        if (donut) {
            swapInProgress = true;
            if (!TickPacketOrderTracker.isTickClean()) return false;
        }

        int index = slot >= 36 ? slot - 36 : slot;
        int hotbar = -1;
        if (!donut) {
            InventoryActionManager.INSTANCE.enqueueNow(() -> InventoryActions.swapToOffhand(slot), 1,
                    mc.player.containerMenu == mc.player.inventoryMenu).verify().atMovementPacket();
        } else if (index < 9) {
            sendOffhandSwap(index);
            hotbar = index;
        } else {
            if (!moveToHotbarSlot(slot)) return false;
            sendOffhandSwap(8);
            hotbar = 8;
        }

        if (hotbar != -1) {
            Inventory inventory = mc.player.getInventory();
            ItemStack stack = inventory.getItem(hotbar);
            inventory.setItem(hotbar, mc.player.getOffhandItem());
            inventory.setItem(Inventory.SLOT_OFFHAND, stack);
        }

        lastSourceSlot = donut && index >= 9 ? 8 : index;
        return true;
    }

    private void sendOffhandSwap(int hotbar) {
        ((MultiPlayerGameModeAccessor) mc.gameMode).ghoul$ensureHasSentCarriedItem();
        int selected = mc.player.getInventory().getSelectedSlot();
        if (hotbar != selected) {
            mc.player.connection.send(new ServerboundSetCarriedItemPacket(hotbar));
        }
        mc.player.connection.send(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.SWAP_ITEM_WITH_OFFHAND, BlockPos.ZERO, Direction.DOWN));
        if (hotbar != selected) {
            mc.player.connection.send(new ServerboundSetCarriedItemPacket(selected));
        }
    }

    private int findTotemSlot() {
        int slot = talisman.getValue() ? findTalismanSlot() : -1;
        return slot != -1 ? slot : findItemSlotId(Items.TOTEM_OF_UNDYING, options.isEnabled(OPTION_SAVE_ENCHANTED), true);
    }

    public boolean isInDanger() {
        if (ServerUtil.isNullWorld()) return false;

        boolean elytra = mc.player.getItemBySlot(EquipmentSlot.CHEST).is(Items.ELYTRA);
        boolean goldenHearts = checks.isEnabled(CHECK_GOLDEN_HEARTS) && System.currentTimeMillis() - lastTotemPopTime > 5000L;
        float hp = mc.player.getHealth() + (goldenHearts ? mc.player.getAbsorptionAmount() : 0.0F);
        if (hp <= (elytra ? healthElytra.getValue() : health.getValue())) return true;

        boolean falling = checks.isEnabled(CHECK_FALL) && mc.player.fallDistance > 5.0;
        boolean keepBall = options.isEnabled(OPTION_NO_BALL_SWITCH) && !falling && mc.player.getOffhandItem().is(Items.PLAYER_HEAD);
        return !keepBall && (isFallDangerous() || isMaceThreat() || isEntityThreat() || isAnchorNearby());
    }

    private boolean isFallDangerous() {
        if (!checks.isEnabled(CHECK_FALL) || mc.player.fallDistance <= 10.0) return false;
        if (mc.player.isFallFlying() || mc.player.isInWater() || mc.player.hasEffect(MobEffects.SLOW_FALLING)) return false;
        return !hasSafeLanding();
    }

    private boolean hasSafeLanding() {
        BlockPos.MutableBlockPos pos = mc.player.blockPosition().mutable();
        int bottom = mc.level.getMinY();

        while (pos.getY() > bottom) {
            pos.move(Direction.DOWN);
            BlockState state = mc.level.getBlockState(pos);
            if (!state.getFluidState().isEmpty()) return true;
            if (state.is(Blocks.SLIME_BLOCK)) return !mc.player.isShiftKeyDown();
            if (state.is(Blocks.POWDER_SNOW) || state.is(Blocks.COBWEB) || state.is(Blocks.SWEET_BERRY_BUSH) || state.is(Blocks.SCAFFOLDING)) {
                return true;
            }
            if (!state.getCollisionShape(mc.level, pos).isEmpty()) return false;
        }
        return false;
    }

    private boolean isMaceThreat() {
        if (!mace.getValue()) return false;
        if (macePvp.getValue() && !ServerUtil.isInPvp()) return false;
        if (mc.player.getHealth() + mc.player.getAbsorptionAmount() > hpMace.getValue()) return false;

        float dist = distMace.getValue();
        float distY = distMaceY.getValue();
        for (Player player : mc.level.players()) {
            if (player == mc.player || isFriend(player) || !player.getMainHandItem().is(Items.MACE) || getFallDistance(player) <= 0.5) {
                continue;
            }
            double dx = player.getX() - mc.player.getX();
            double dz = player.getZ() - mc.player.getZ();
            if (dx * dx + dz * dz > dist * dist || Math.abs(player.getY() - mc.player.getY()) > distY) continue;
            if (maceWalls.getValue() || canSee(player)) return true;
        }
        return false;
    }

    private void updatePeakHeights() {
        if (!mace.getValue()) {
            peakYByPlayer.clear();
            return;
        }

        airbornePlayers.clear();
        for (Player player : mc.level.players()) {
            if (player != mc.player && !player.onGround() && !player.isInWater() && !player.onClimbable()) {
                UUID uuid = player.getUUID();
                airbornePlayers.add(uuid);
                peakYByPlayer.merge(uuid, player.getY(), Math::max);
            }
        }
        peakYByPlayer.keySet().retainAll(airbornePlayers);
    }

    private double getFallDistance(Player player) {
        return peakYByPlayer.getOrDefault(player.getUUID(), player.getY()) - player.getY();
    }

    private boolean canSee(Player player) {
        HitResult hit = mc.level.clip(new ClipContext(mc.player.getEyePosition(), player.getEyePosition(),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mc.player));
        return hit.getType() == HitResult.Type.MISS;
    }

    private boolean isEntityThreat() {
        boolean crystals = checks.isEnabled(CHECK_CRYSTALS);
        boolean tntCheck = tnt.getValue();
        boolean tridentCheck = trident.getValue();
        if (!crystals && !tntCheck && !tridentCheck) return false;

        float tntRange = distTnt.getValue();
        float tridentRange = distTrident.getValue();
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (crystals && entity instanceof EndCrystal && mc.player.distanceTo(entity) <= 6.0F) return true;
            if (tntCheck && isPrimedExplosive(entity) && mc.player.distanceTo(entity) <= tntRange) return true;
            if (tridentCheck && entity instanceof ThrownTrident thrown && !thrown.noPhysics
                    && !isFriendlyTrident(thrown) && mc.player.distanceTo(thrown) <= tridentRange) {
                return true;
            }
        }
        return false;
    }

    private static boolean isPrimedExplosive(Entity entity) {
        if (entity instanceof PrimedTnt primed) return primed.getFuse() > 0;
        return entity instanceof MinecartTNT minecart && minecart.isPrimed();
    }

    private boolean isFriendlyTrident(ThrownTrident thrown) {
        Entity owner = thrown.getOwner();
        if (owner == null) return false;
        return owner == mc.player || owner instanceof Player player && isFriend(player);
    }

    private static boolean isFriend(Player player) {
        return FriendManager.getInstance().contains(player.getGameProfile().name());
    }

    private boolean isAnchorNearby() {
        if (!checks.isEnabled(CHECK_ANCHOR)) return false;

        BlockPos origin = mc.player.blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-6, -6, -6), origin.offset(6, 6, 6))) {
            if (mc.level.getBlockState(pos).is(Blocks.RESPAWN_ANCHOR)) return true;
        }
        return false;
    }

    private boolean isHoldingTotem() {
        return isValidTotem(mc.player.getOffhandItem()) || isValidTotem(mc.player.getMainHandItem());
    }

    private boolean isValidTotem(ItemStack stack) {
        if (!stack.is(Items.TOTEM_OF_UNDYING)) return false;

        if (talisman.getValue()) {
            if (isTalisman(stack)) return true;
            if (findTalismanSlot() != -1) return false;
        }

        boolean enchanted = options.isEnabled(OPTION_SAVE_ENCHANTED) && stack.hasFoil();
        return !enchanted || findItemSlotId(Items.TOTEM_OF_UNDYING, true, false) == -1;
    }

    private int findTalismanSlot() {
        Inventory inventory = mc.player.getInventory();
        for (int i = 0; i < 36; i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.is(Items.TOTEM_OF_UNDYING) && isTalisman(stack)) {
                return i < 9 ? i + 36 : i;
            }
        }
        return -1;
    }

    private boolean isTalisman(ItemStack stack) {
        return SphereItems.idOf(stack).equals(talismanType.getValue());
    }

    private boolean isTotemOnCooldown() {
        Inventory inventory = mc.player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.is(Items.TOTEM_OF_UNDYING) && mc.player.getCooldowns().isOnCooldown(stack)) return true;
        }
        return false;
    }

    // слот меню игрока с предметом; preferPlain — сначала без блеска, fallback — тогда вернуть с блеском
    private static int findItemSlotId(Item item, boolean preferPlain, boolean fallback) {
        Inventory inventory = mc.player.getInventory();
        int glint = -1;
        for (int i = 0; i < 36; i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.isEmpty() || !stack.is(item)) continue;

            int slot = i < 9 ? i + 36 : i;
            if (!preferPlain || !stack.hasFoil()) return slot;
            if (glint == -1) glint = slot;
        }
        return fallback ? glint : -1;
    }

    private static int findHotbarIndex(Item item) {
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getItem(i).is(item)) return i;
        }
        return -1;
    }

    private static boolean isInventoryOpen() {
        return mc.player.containerMenu == mc.player.inventoryMenu && mc.player.containerMenu.getCarried().isEmpty();
    }

    private static boolean isMovementActive() {
        return mc.player.isSprinting() || MoveUtil.isMoving() || mc.player.input != null && mc.player.input.keyPresses.jump();
    }
}
