package tokyo.ghoul.client.features.modules.misc;

import lombok.Getter;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.server.level.ParticleStatus;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import tokyo.ghoul.api.event.EventListener;
import tokyo.ghoul.api.event.Listener;
import tokyo.ghoul.api.event.events.client.TickEvent;
import tokyo.ghoul.api.module.Category;
import tokyo.ghoul.api.module.Module;
import tokyo.ghoul.api.module.ModuleRegister;
import tokyo.ghoul.api.module.setting.BooleanSetting;
import tokyo.ghoul.api.module.setting.MultiBooleanSetting;
import tokyo.ghoul.api.module.setting.SliderSetting;

import java.util.ArrayList;
import java.util.List;

@ModuleRegister(name = "Optimization", description = "Оптимизация производительности клиента", category = Category.OTHER)
public class OptimizationModule extends Module {
    @Getter private static final OptimizationModule instance = new OptimizationModule();

    private final MultiBooleanSetting video = new MultiBooleanSetting("Video").value(
            new BooleanSetting("Clouds Off").value(true),
            new BooleanSetting("Minimal Particles").value(true),
            new BooleanSetting("Entity Shadows Off").value(true),
            new BooleanSetting("Smooth Lighting Off").value(false),
            new BooleanSetting("Biome Blend Off").value(false),
            new BooleanSetting("Vignette Off").value(true),
            new BooleanSetting("Weather Radius Min").value(true),
            new BooleanSetting("Disable VSync").value(true),
            new BooleanSetting("Menu Blur Off").value(true),
            new BooleanSetting("Improved Transparency Off").value(true),
            new BooleanSetting("Chunk Fade Off").value(false),
            new BooleanSetting("Fast Leaves").value(false)
    );

    private final BooleanSetting limitRender = new BooleanSetting("Limit Render Distance").value(false)
            .onAction(this::reapplyOptions);
    private final SliderSetting renderDistance = new SliderSetting("Render Distance").value(10f)
            .range(2f, 32f).step(1f).setVisible(limitRender::getValue).onAction(this::reapplyOptions);
    private final BooleanSetting limitSimulation = new BooleanSetting("Limit Simulation Distance").value(false)
            .onAction(this::reapplyOptions);
    private final SliderSetting simulationDistance = new SliderSetting("Simulation Distance").value(8f)
            .range(5f, 32f).step(1f).setVisible(limitSimulation::getValue).onAction(this::reapplyOptions);
    private final SliderSetting entityScaling = new SliderSetting("Entity Distance Scale").value(0.75f)
            .range(0.5f, 5f).step(0.25f).onAction(this::reapplyOptions);

    private final BooleanSetting entityCulling = new BooleanSetting("Entity Culling").value(true);
    private final SliderSetting entityDistance = new SliderSetting("Entity Distance").value(48f)
            .range(16f, 128f).step(4f).setVisible(entityCulling::getValue);
    private final BooleanSetting keepPlayers = new BooleanSetting("Keep Players").value(true)
            .setVisible(entityCulling::getValue);
    private final BooleanSetting itemCulling = new BooleanSetting("Item Culling").value(true);
    private final SliderSetting itemDistance = new SliderSetting("Item Distance").value(24f)
            .range(8f, 64f).step(2f).setVisible(itemCulling::getValue);

    private final BooleanSetting blockEntityCulling = new BooleanSetting("Block Entity Culling").value(true);
    private final SliderSetting blockEntityDistance = new SliderSetting("Block Entity Distance").value(32f)
            .range(8f, 96f).step(4f).setVisible(blockEntityCulling::getValue);

    private final BooleanSetting particleCulling = new BooleanSetting("Particle Culling").value(true);
    private final SliderSetting particleDistance = new SliderSetting("Particle Distance").value(24f)
            .range(8f, 64f).step(2f).setVisible(particleCulling::getValue);

    private final BooleanSetting particleThinning = new BooleanSetting("Thin Distant Particles").value(true);
    private final BooleanSetting particlePhysics = new BooleanSetting("Simple Particle Physics").value(true);
    private final BooleanSetting particleLight = new BooleanSetting("Particle Light Cache").value(true);
    private final BooleanSetting singleItemCopy = new BooleanSetting("Single Item Copy").value(true);
    private final BooleanSetting restingItems = new BooleanSetting("Resting Items").value(true);

    private final BooleanSetting particleLimit = new BooleanSetting("Particle Limit").value(true);
    private final SliderSetting maxParticles = new SliderSetting("Max Particles").value(2000f)
            .range(250f, 10000f).step(250f).setVisible(particleLimit::getValue);

    private final BooleanSetting nameTagCulling = new BooleanSetting("Name Tag Culling").value(true);
    private final SliderSetting nameTagDistance = new SliderSetting("Name Tag Distance").value(24f)
            .range(8f, 64f).step(4f).setVisible(nameTagCulling::getValue);

    private final BooleanSetting statePool = new BooleanSetting("Render State Pool").value(true);

    private static int particleCounter;
    private static boolean worldEntityExtract;
    private static double camX, camY, camZ;

    private final BooleanSetting backgroundLimit = new BooleanSetting("Background FPS Limit").value(true);
    private final SliderSetting backgroundFps = new SliderSetting("Background FPS").value(15f)
            .range(5f, 60f).step(5f).setVisible(backgroundLimit::getValue);

    private final BooleanSetting memoryCleaner = new BooleanSetting("Memory Cleaner").value(true);
    private final SliderSetting memoryThreshold = new SliderSetting("Clean At Memory %").value(75f)
            .range(50f, 95f).step(5f).setVisible(memoryCleaner::getValue);

    private static final long CLEAN_COOLDOWN_MS = 30_000L;
    private long lastClean;

    private final List<Runnable> restores = new ArrayList<>();

    public OptimizationModule() {
        video.getValue().forEach(option -> option.onAction(this::reapplyOptions));
        addSettings(video, limitRender, renderDistance, limitSimulation, simulationDistance, entityScaling,
                entityCulling, entityDistance, keepPlayers, itemCulling, itemDistance,
                blockEntityCulling, blockEntityDistance, particleCulling, particleDistance,
                particleThinning, particlePhysics, particleLight, singleItemCopy, restingItems,
                particleLimit, maxParticles, nameTagCulling, nameTagDistance, statePool,
                backgroundLimit, backgroundFps, memoryCleaner, memoryThreshold);
    }

    @Override
    public void onEvent() {
        EventListener tickEvent = TickEvent.getInstance().subscribe(new Listener<>(event -> cleanMemory()));
        addEvents(tickEvent);
    }

    private void cleanMemory() {
        if (!memoryCleaner.getValue() || mc.level == null) return;
        // Full GC freezes the game, so only collect while the player isn't actually playing
        if (mc.gui.screen() == null && mc.isWindowActive()) return;
        long now = System.currentTimeMillis();
        if (now - lastClean < CLEAN_COOLDOWN_MS) return;
        Runtime rt = Runtime.getRuntime();
        double used = (rt.totalMemory() - rt.freeMemory()) / (double) rt.maxMemory() * 100.0;
        if (used < memoryThreshold.getValue()) return;
        lastClean = now;
        System.gc();
    }

    @Override
    public void onEnable() {
        applyOptions();
    }

    @Override
    public void onDisable() {
        restoreOptions();
        super.onDisable();
    }

    private void reapplyOptions() {
        if (!isEnabled() || mc.options == null) return;
        restoreOptions();
        applyOptions();
    }

    private void applyOptions() {
        Options options = mc.options;
        if (options == null) return;

        if (video.isEnabled("Clouds Off")) override(options.cloudStatus(), CloudStatus.OFF);
        if (video.isEnabled("Minimal Particles")) override(options.particles(), ParticleStatus.MINIMAL);
        if (video.isEnabled("Entity Shadows Off")) override(options.entityShadows(), false);
        if (video.isEnabled("Smooth Lighting Off")) override(options.ambientOcclusion(), false);
        if (video.isEnabled("Biome Blend Off")) override(options.biomeBlendRadius(), 0);
        if (video.isEnabled("Vignette Off")) override(options.vignette(), false);
        if (video.isEnabled("Weather Radius Min")) override(options.weatherRadius(), 3);
        if (video.isEnabled("Disable VSync")) override(options.enableVsync(), false);
        if (video.isEnabled("Menu Blur Off")) override(options.menuBackgroundBlurriness(), 0);
        if (video.isEnabled("Improved Transparency Off")) override(options.improvedTransparency(), false);
        if (video.isEnabled("Chunk Fade Off")) override(options.chunkSectionFadeInTime(), 0.0);
        if (video.isEnabled("Fast Leaves")) override(options.cutoutLeaves(), false);

        if (limitRender.getValue()) {
            override(options.renderDistance(), Math.min(options.renderDistance().get(), renderDistance.getValue().intValue()));
        }
        if (limitSimulation.getValue()) {
            override(options.simulationDistance(), Math.min(options.simulationDistance().get(), simulationDistance.getValue().intValue()));
        }
        override(options.entityDistanceScaling(), Math.min(options.entityDistanceScaling().get(), entityScaling.getValue().doubleValue()));
    }

    private void restoreOptions() {
        for (int i = restores.size() - 1; i >= 0; i--) {
            try {
                restores.get(i).run();
            } catch (Exception ignored) {
            }
        }
        restores.clear();
    }

    private <T> void override(OptionInstance<T> option, T value) {
        T original = option.get();
        if (original.equals(value)) return;
        restores.add(() -> option.set(original));
        option.set(value);
    }

    private static boolean active() {
        return instance.isEnabled() && mc.level != null;
    }

    // Reads camera coords into statics instead of allocating a Vec3 on every particle/block entity query
    private static boolean updateCamera() {
        Entity camera = mc.getCameraEntity();
        if (camera == null) return false;
        camX = camera.getX();
        camY = camera.getEyeY();
        camZ = camera.getZ();
        return true;
    }

    private static double cameraDistSq(double x, double y, double z) {
        double dx = x - camX, dy = y - camY, dz = z - camZ;
        return dx * dx + dy * dy + dz * dz;
    }

    public static boolean shouldCullEntity(Entity entity, double camX, double camY, double camZ) {
        OptimizationModule m = instance;
        if (!active() || entity == mc.player || entity == mc.getCameraEntity()) return false;

        double distSq = entity.distanceToSqr(camX, camY, camZ);
        if (entity instanceof ItemEntity) {
            if (!m.itemCulling.getValue()) return false;
            double limit = m.itemDistance.getValue();
            return distSq > limit * limit;
        }
        if (!m.entityCulling.getValue()) return false;
        if (entity instanceof Player && m.keepPlayers.getValue()) return false;
        double limit = m.entityDistance.getValue();
        return distSq > limit * limit;
    }

    public static boolean shouldCullBlockEntity(BlockEntity blockEntity) {
        OptimizationModule m = instance;
        if (!active() || !m.blockEntityCulling.getValue() || !updateCamera()) return false;
        double limit = m.blockEntityDistance.getValue();
        return blockEntity.getBlockPos().distToCenterSqr(camX, camY, camZ) > limit * limit;
    }

    public static boolean shouldCullParticle(double x, double y, double z) {
        OptimizationModule m = instance;
        if (!active() || !m.particleCulling.getValue() || !updateCamera()) return false;
        double limit = m.particleDistance.getValue();
        return cameraDistSq(x, y, z) > limit * limit;
    }

    public static boolean shouldThinParticle(double x, double y, double z) {
        OptimizationModule m = instance;
        if (!active() || !m.particleThinning.getValue() || !updateCamera()) return false;
        double distSq = cameraDistSq(x, y, z);
        int n = ++particleCounter;
        if (distSq > 32 * 32) return (n & 3) != 0;
        if (distSq > 16 * 16) return (n & 1) != 0;
        return false;
    }

    public static boolean shouldSimplifyParticlePhysics(double x, double y, double z) {
        OptimizationModule m = instance;
        if (!active() || !m.particlePhysics.getValue() || !updateCamera()) return false;
        return cameraDistSq(x, y, z) > 16 * 16;
    }

    public static boolean isParticleLightCache() {
        return active() && instance.particleLight.getValue();
    }

    public static boolean isSingleItemCopy() {
        return active() && instance.singleItemCopy.getValue();
    }

    public static boolean shouldSkipRestingItemTick(ItemEntity item) {
        OptimizationModule m = instance;
        if (!active() || !m.restingItems.getValue() || !item.level().isClientSide()) return false;
        if (!item.onGround() || item.getDeltaMovement().lengthSqr() > 1.0E-6) return false;
        return updateCamera() && cameraDistSq(item.getX(), item.getY(), item.getZ()) > 24 * 24;
    }

    public static int maxParticles() {
        OptimizationModule m = instance;
        return active() && m.particleLimit.getValue() ? m.maxParticles.getValue().intValue() : 0;
    }

    public static boolean shouldCullNameTag(Entity entity) {
        OptimizationModule m = instance;
        if (!active() || !m.nameTagCulling.getValue() || entity instanceof Player || !updateCamera()) return false;
        double limit = m.nameTagDistance.getValue();
        return cameraDistSq(entity.getX(), entity.getY(), entity.getZ()) > limit * limit;
    }

    public static void setWorldEntityExtract(boolean value) {
        worldEntityExtract = value;
    }

    public static boolean isPoolingEntityStates() {
        return worldEntityExtract && active() && instance.statePool.getValue();
    }

    public static boolean isPoolingBlockEntityStates() {
        return active() && instance.statePool.getValue();
    }

    public static int limitFramerate(int current) {
        OptimizationModule m = instance;
        if (!m.isEnabled() || !m.backgroundLimit.getValue() || mc.isWindowActive()) return current;
        return Math.min(current, m.backgroundFps.getValue().intValue());
    }
}
