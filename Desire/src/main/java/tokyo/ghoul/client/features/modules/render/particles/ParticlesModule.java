package tokyo.ghoul.client.features.modules.render.particles;

import lombok.Getter;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEnderpearl;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import tokyo.ghoul.api.event.EventListener;
import tokyo.ghoul.api.event.Listener;
import tokyo.ghoul.api.event.events.client.PacketEvent;
import tokyo.ghoul.api.event.events.client.TickEvent;
import tokyo.ghoul.api.event.events.player.other.UpdateEvent;
import tokyo.ghoul.api.event.events.player.world.AttackEvent;
import tokyo.ghoul.api.event.events.render.Render3DEvent;
import tokyo.ghoul.api.module.Category;
import tokyo.ghoul.api.module.Module;
import tokyo.ghoul.api.module.ModuleRegister;
import tokyo.ghoul.api.module.setting.BooleanSetting;
import tokyo.ghoul.api.module.setting.ModeSetting;
import tokyo.ghoul.api.module.setting.MultiBooleanSetting;
import tokyo.ghoul.api.module.setting.SliderSetting;
import tokyo.ghoul.api.system.files.FileUtil;
import tokyo.ghoul.api.utils.color.UIColors;
import tokyo.ghoul.client.features.modules.render.particles.engine.ClientParticle;
import tokyo.ghoul.client.features.modules.render.particles.engine.ClientParticleManager;
import tokyo.ghoul.client.features.modules.render.particles.engine.ClientParticleRenderer;
import tokyo.ghoul.client.features.modules.render.particles.engine.ParticleCollisionMode;
import tokyo.ghoul.client.features.modules.render.particles.engine.ParticleInterpolationMode;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

@ModuleRegister(name = "Particles", description = "Частицы при ударах", category = Category.RENDER)
public class ParticlesModule extends Module {
    private static final String ON_HIT = "Ударе";
    private static final String ON_TOTEM = "Поп тотема";
    private static final String ON_IDLE = "Бездействие";
    private static final String ON_WALK = "Ходьбе";
    private static final String ON_PEARL = "Эндер жемчуг";
    private static final String ON_ARROW = "Стрела";

    private static final String[][] ELEMENTS = {
            {"Орбиз", "orbiznew"}, {"Звезды", "star"}, {"Сердечки", "heart"}, {"Доллары", "dollar"},
            {"Снежинки", "snowflake"}, {"Короны", "crown"}, {"Молнии", "lightning"}, {"Линии", "line"},
            {"Кругляши", "point"}, {"Ромбы", "rhombus"}, {"Искры", "spark"}
    };

    @Getter private static final ParticlesModule instance = new ParticlesModule();

    private final MultiBooleanSetting spawnOn = new MultiBooleanSetting("Спавнить при").value(
            new BooleanSetting(ON_HIT).value(true),
            new BooleanSetting(ON_TOTEM).value(true),
            new BooleanSetting(ON_IDLE).value(false),
            new BooleanSetting(ON_WALK).value(true),
            new BooleanSetting(ON_PEARL).value(false),
            new BooleanSetting(ON_ARROW).value(false)
    );
    private final ModeSetting colorMode = new ModeSetting("Цвет").values("От темы", "Радужные").value("Радужные");
    private final ModeSetting hitPhysics = new ModeSetting("Физика удара").values("Отскок", "Рикошет").value("Рикошет")
            .setVisible(() -> spawnOn.isEnabled(ON_HIT));
    private final SliderSetting hitCount = new SliderSetting("Спавнить при ударе").range(5, 50).step(1).value(15F)
            .setVisible(() -> spawnOn.isEnabled(ON_HIT));
    private final SliderSetting worldCount = new SliderSetting("Спавнить в мире").range(3, 50).step(1).value(20F)
            .setVisible(() -> spawnOn.isEnabled(ON_IDLE));
    private final MultiBooleanSetting elements = new MultiBooleanSetting("Элементы").value(createElementSettings());
    private final BooleanSetting glow = new BooleanSetting("Свечение").value(false);

    private final ClientParticleRenderer renderer = new ClientParticleRenderer();
    private final ConcurrentLinkedQueue<Entity> poppedEntities = new ConcurrentLinkedQueue<>();
    private final Random random = new Random();
    private final Set<Block> passableBlocks = Collections.newSetFromMap(new IdentityHashMap<>());
    private ClientLevel lastLevel;
    private int idleTicks;

    public ParticlesModule() {
        addSettings(spawnOn, colorMode, hitPhysics, hitCount, worldCount, elements, glow);
        fillPassableBlocks();
    }

    private static BooleanSetting[] createElementSettings() {
        BooleanSetting[] settings = new BooleanSetting[ELEMENTS.length];
        for (int i = 0; i < ELEMENTS.length; i++) {
            settings[i] = new BooleanSetting(ELEMENTS[i][0]).value(i == 0);
        }
        return settings;
    }

    @Override
    public void onDisable() {
        reset();
        super.onDisable();
    }

    @Override
    public void onEvent() {
        EventListener tick = TickEvent.getInstance().subscribe(new Listener<>(event -> onTick()));
        EventListener update = UpdateEvent.getInstance().subscribe(new Listener<>(event -> onWalk()));
        EventListener attack = AttackEvent.getInstance().subscribe(new Listener<>(event -> onAttack(event.entity())));
        EventListener packet = PacketEvent.getInstance().subscribe(new Listener<>(this::onPacket));
        EventListener render = Render3DEvent.getInstance().subscribe(new Listener<>(event ->
                renderer.render(event.matrixStack(), event.partialTicks(), selectedTextures(), glow.getValue())));
        addEvents(tick, update, attack, packet, render);
    }

    private void reset() {
        poppedEntities.clear();
        idleTicks = 0;
        lastLevel = null;
        ClientParticleManager.getInstance().clear();
    }


    private void onTick() {
        if (mc.level == null) {
            reset();
            return;
        }
        if (mc.level != lastLevel) {
            ClientParticleManager.getInstance().clear();
            lastLevel = mc.level;
        }

        idleTicks++;
        flushTotemPops(spawnOn.isEnabled(ON_TOTEM));
        if (spawnOn.isEnabled(ON_IDLE)) spawnAmbient();
        spawnProjectileTrails(spawnOn.isEnabled(ON_PEARL), spawnOn.isEnabled(ON_ARROW));

        ClientParticleManager.getInstance().tick();
    }

    private void onWalk() {
        if (!spawnOn.isEnabled(ON_WALK) || mc.player == null) return;
        var player = mc.player;
        if (player.getX() == player.xo && player.getY() == player.yo && player.getZ() == player.zo) return;

        for (int i = 0; i < 2; i++) {
            float heightOffset = player.getBbHeight() * random.nextFloat();
            Vec3 pos = new Vec3(
                    player.getX() + random.nextFloat(-0.5F, 0.5F),
                    player.getY() + heightOffset,
                    player.getZ() + random.nextFloat(-0.5F, 0.5F));
            double speedScale = 2.0F * (1.0F + random.nextFloat());
            Vec3 motion = new Vec3(
                    (player.getDeltaMovement().x + random.nextFloat(-0.1F, 0.1F)) * speedScale,
                    0.0,
                    (player.getDeltaMovement().z + random.nextFloat(-0.1F, 0.1F)) * speedScale);
            spawnAmbientParticle(pos, motion, 8.0E-6F, 800, color(ClientParticleManager.getInstance().size() * 50),
                    random.nextFloat(0.1F, 0.225F));
        }
    }

    private void onAttack(Entity target) {
        if (mc.player == null || mc.level == null || !spawnOn.isEnabled(ON_HIT)) return;
        if (!(target instanceof LivingEntity living)) return;

        if (hitPhysics.is("Рикошет")) {
            spawnRicochetHit(living);
        } else {
            spawnBounceHit(living);
        }
    }

    private void onPacket(PacketEvent.PacketEventData event) {
        if (!event.isReceive() || mc.level == null) return;
        if (event.packet() instanceof ClientboundEntityEventPacket status && status.getEventId() == 35) {
            Entity entity = status.getEntity(mc.level);
            if (entity != null) poppedEntities.add(entity);
        }
    }


    private void flushTotemPops(boolean enabled) {
        if (poppedEntities.isEmpty()) return;
        if (!enabled) {
            poppedEntities.clear();
            return;
        }
        Entity entity;
        while ((entity = poppedEntities.poll()) != null) {
            ClientParticleManager.getInstance().burst(entity, 30);
        }
    }

    private void spawnAmbient() {
        if (idleTicks % 15 != 0) return;
        idleTicks = 0;

        Vec3 camera = mc.gameRenderer.mainCamera().position();
        float spread = 3.0F;
        int count = worldCount.getValue().intValue() * 10;

        for (int i = 0; i < count; i++) {
            double distance = random.nextFloat(1.0F, 30.0F);
            double yaw = Math.toRadians(random.nextFloat(0.0F, 360.0F));
            Vec3 pos = new Vec3(
                    camera.x - Math.sin(yaw) * distance,
                    camera.y + random.nextFloat(-15.0F, 15.0F),
                    camera.z + Math.cos(yaw) * distance);
            Vec3 motion = new Vec3(
                    random.nextFloat(-spread, spread),
                    random.nextFloat(-1.0F, 1.0F),
                    random.nextFloat(-spread, spread));
            spawnAmbientParticle(pos, motion, 8.0E-6F, 2500, color(ClientParticleManager.getInstance().size() * 50),
                    random.nextFloat(0.1F, 0.225F));
        }
    }

    private void spawnProjectileTrails(boolean pearls, boolean arrows) {
        if (!pearls && !arrows) return;
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (pearls && entity instanceof ThrownEnderpearl || arrows && entity instanceof Arrow) {
                spawnTrail(entity);
            }
        }
    }

    private void spawnTrail(Entity entity) {
        double dx = entity.getX() - entity.xo;
        double dy = entity.getY() - entity.yo;
        double dz = entity.getZ() - entity.zo;
        if (Math.sqrt(dx * dx + dy * dy + dz * dz) * 20.0 < 0.35) return;

        Vec3 pos = entity.position();
        Vec3 velocity = entity.getDeltaMovement();
        for (int i = 0; i < 5; i++) {
            float spread = random.nextFloat();
            Vec3 motion = new Vec3(
                    random.nextFloat(-spread, spread) + velocity.x,
                    random.nextFloat(-spread, spread) + velocity.y,
                    random.nextFloat(-spread, spread) + velocity.z);
            spawnAmbientParticle(pos, motion, 8.0E-6F, 1000, color(ClientParticleManager.getInstance().size() * 50),
                    random.nextFloat(0.1F, 0.225F));
        }
    }

    private void spawnRicochetHit(LivingEntity target) {
        int lifetime = msToTicks(3000);
        int count = hitCount.getValue().intValue();

        for (int i = 0; i < count; i++) {
            ClientParticle.Builder builder = new ClientParticle.Builder(
                    target.getX() + (random.nextDouble() - 0.5) * 0.5,
                    target.getY() + random.nextDouble() * target.getBbHeight(),
                    target.getZ() + (random.nextDouble() - 0.5) * 0.5)
                    .motion(
                            (random.nextDouble() * 0.5 - 0.25) * 0.9,
                            random.nextDouble() * 0.25 * 0.01,
                            (random.nextDouble() * 0.5 - 0.25) * 0.9)
                    .collisionMode(ParticleCollisionMode.RICOCHET)
                    .interpolation(ParticleInterpolationMode.LINEAR)
                    .gravity(1.0F)
                    .collide(true)
                    .size(0.6F, 0.6F)
                    .lifetime(lifetime)
                    .maxDistance(15.0);
            ClientParticleManager.getInstance().spawn(builder, color(i * 30), 0.15F, lifetime);
        }
    }

    private void spawnBounceHit(LivingEntity target) {
        float spread = 0.5F;
        Vec3 velocity = target.getDeltaMovement();
        int count = hitCount.getValue().intValue();

        for (int i = 0; i < count; i++) {
            float size = random.nextFloat(0.1F, 0.15F);
            int lifetime = random.nextInt(40, 60);
            double motionX = random.nextFloat(-spread, spread) + velocity.x * 0.35;
            double motionY = random.nextFloat(-0.2F, 0.15F) + Math.max(0.0, velocity.y) * 0.25;
            double motionZ = random.nextFloat(-spread, spread) + velocity.z * 0.35;

            ClientParticle.Builder builder = new ClientParticle.Builder(
                    target.getX() + random.nextDouble(-0.25, 0.25),
                    target.getY() + random.nextDouble(0.1, target.getBbHeight()),
                    target.getZ() + random.nextDouble(-0.25, 0.25))
                    .motion(motionX, motionY, motionZ)
                    .gravity(0.2F)
                    .groundBoost(true)
                    .drag(0.9F)
                    .bounce(2.0F)
                    .collide(true)
                    .size(size, size)
                    .lifetime(lifetime)
                    .delay(random.nextInt(1, 3));
            ClientParticleManager.getInstance().spawn(builder, color(random.nextInt()), size, Math.max(12, lifetime / 2));
        }
    }

    private void spawnAmbientParticle(Vec3 pos, Vec3 motion, float gravity, int lifeMs, int color, float size) {
        if (mc.level == null) return;
        Block block = mc.level.getBlockState(BlockPos.containing(pos.x, pos.y, pos.z)).getBlock();
        if (!passableBlocks.contains(block)) return;

        float clampedSize = Math.max(0.02F, size);
        ClientParticle.Builder builder = new ClientParticle.Builder(pos.x, pos.y, pos.z)
                .motion(motion.x * 0.07, motion.y * 0.07, motion.z * 0.07)
                .gravity((float) Math.max(0.0, gravity * 175.0))
                .lifetime(msToTicks(lifeMs))
                .drag(0.98F)
                .collide(false)
                .size(clampedSize, clampedSize);
        ClientParticleManager.getInstance().spawn(builder, color, size, msToTicks(300));
    }


    private static int msToTicks(int ms) {
        return Math.max(1, Math.round(ms / 50.0F));
    }

    private int color(int index) {
        if (colorMode.is("Радужные")) {
            return Color.HSBtoRGB(random.nextFloat(), 1.0F, 1.0F);
        }
        return UIColors.gradient(index).getRGB();
    }

    private List<Identifier> selectedTextures() {
        List<Identifier> textures = new ArrayList<>();
        for (String[] element : ELEMENTS) {
            if (elements.isEnabled(element[0])) {
                textures.add(FileUtil.getImage("particles/hex/" + element[1]));
            }
        }
        if (textures.isEmpty()) textures.add(FileUtil.getImage("particles/hex/" + ELEMENTS[0][1]));
        return textures;
    }

    private void fillPassableBlocks() {
        Collections.addAll(passableBlocks,
                Blocks.AIR, Blocks.CAVE_AIR, Blocks.VOID_AIR, Blocks.WATER, Blocks.LAVA,
                Blocks.GRASS_BLOCK, Blocks.TALL_GRASS, Blocks.FERN, Blocks.LARGE_FERN, Blocks.DEAD_BUSH,
                Blocks.SEAGRASS, Blocks.TALL_SEAGRASS, Blocks.VINE, Blocks.GLOW_LICHEN, Blocks.SUGAR_CANE,
                Blocks.CACTUS, Blocks.SNOW, Blocks.COBWEB, Blocks.ICE, Blocks.PACKED_ICE, Blocks.BLUE_ICE,
                Blocks.FROSTED_ICE, Blocks.SLIME_BLOCK, Blocks.HONEY_BLOCK);
    }
}
