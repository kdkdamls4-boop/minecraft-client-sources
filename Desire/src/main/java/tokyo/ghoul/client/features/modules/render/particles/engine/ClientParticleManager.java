package tokyo.ghoul.client.features.modules.render.particles.engine;

import lombok.Getter;
import net.minecraft.world.entity.Entity;
import tokyo.ghoul.api.system.interfaces.QuickImports;

import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

public final class ClientParticleManager implements QuickImports {
    @Getter private static final ClientParticleManager instance = new ClientParticleManager();

    private final ArrayDeque<Entry> entries = new ArrayDeque<>();
    private final ArrayDeque<Entry> pendingEntries = new ArrayDeque<>();
    private final ArrayDeque<ParticleEmitter> emitters = new ArrayDeque<>();
    private final ArrayDeque<ParticleEmitter> pendingEmitters = new ArrayDeque<>();
    private int limit = 16384;
    private boolean ticking;

    private ClientParticleManager() {}

    public void setLimit(int limit) {
        this.limit = Math.max(1, limit);
        trim(0);
    }

    public int size() {
        return entries.size() + pendingEntries.size();
    }

    public boolean isEmpty() {
        return entries.isEmpty() && pendingEntries.isEmpty() && emitters.isEmpty() && pendingEmitters.isEmpty();
    }

    public Iterable<Entry> entries() {
        return entries;
    }

    public ClientParticle spawn(ClientParticle.Builder builder, int color, float size, int fadeTicks) {
        Objects.requireNonNull(builder, "builder");
        ClientParticle particle = new ClientParticle(builder);
        add(particle, color, size, fadeTicks);
        return particle;
    }

    public boolean add(ClientParticle particle, int color, float size, int fadeTicks) {
        if (particle == null || !particle.isAlive()) return false;

        Entry entry = new Entry(particle, color, Math.max(0.01F, size), resolveFadeTicks(particle, fadeTicks));
        trim(1);
        if (ticking) {
            pendingEntries.addLast(entry);
        } else {
            entry.prevAlpha = entry.alpha;
            entry.alpha = computeAlpha(particle, entry.fadeTicks);
            entries.addLast(entry);
        }
        return true;
    }

    public boolean addEmitter(ParticleEmitter emitter) {
        if (emitter == null || !emitter.isActive()) return false;
        (ticking ? pendingEmitters : emitters).addLast(emitter);
        return true;
    }

    public EntityBurstEmitter burst(Entity entity, int duration) {
        EntityBurstEmitter emitter = new EntityBurstEmitter(entity, duration);
        addEmitter(emitter);
        return emitter;
    }

    public void clear() {
        entries.clear();
        pendingEntries.clear();
        emitters.clear();
        pendingEmitters.clear();
    }

    public void tick() {
        ticking = true;
        try {
            tickParticles();
            tickEmitters();
        } finally {
            ticking = false;
        }
        flushPendingEntries();
        flushPendingEmitters();
    }

    public void updateVisibility() {
        if (entries.isEmpty()) return;
        Entity viewer = mc.player;
        for (Entry entry : entries) {
            ClientParticle particle = entry.particle;
            entry.visible = particle.isAlive() && withinDistance(viewer, particle);
        }
    }

    private static boolean withinDistance(Entity viewer, ClientParticle particle) {
        double maxSq = particle.getMaxDistanceSq();
        if (maxSq <= 0.0 || viewer == null) return true;
        return viewer.distanceToSqr(particle.getX(), particle.getY(), particle.getZ()) <= maxSq;
    }

    private void tickParticles() {
        Iterator<Entry> iterator = entries.iterator();
        while (iterator.hasNext()) {
            Entry entry = iterator.next();
            ClientParticle particle = entry.particle;
            particle.tick();
            if (!particle.isAlive()) {
                iterator.remove();
            } else {
                entry.prevAlpha = entry.alpha;
                entry.alpha = computeAlpha(particle, entry.fadeTicks);
            }
        }
    }

    private void tickEmitters() {
        Iterator<ParticleEmitter> iterator = emitters.iterator();
        while (iterator.hasNext()) {
            ParticleEmitter emitter = iterator.next();
            emitter.tick(this);
            if (!emitter.isActive()) iterator.remove();
        }
    }

    private void flushPendingEntries() {
        Entry entry;
        while ((entry = pendingEntries.pollFirst()) != null) {
            if (!entry.particle.isAlive()) continue;
            entry.prevAlpha = entry.alpha;
            entry.alpha = computeAlpha(entry.particle, entry.fadeTicks);
            entries.addLast(entry);
        }
    }

    private void flushPendingEmitters() {
        ParticleEmitter emitter;
        while ((emitter = pendingEmitters.pollFirst()) != null) {
            if (emitter.isActive()) emitters.addLast(emitter);
        }
    }

    private void trim(int incoming) {
        int overflow = entries.size() + pendingEntries.size() + incoming - limit;
        while (overflow > 0) {
            Entry oldest = entries.pollFirst();
            if (oldest == null) oldest = pendingEntries.pollFirst();
            if (oldest == null) break;
            oldest.particle.kill();
            overflow--;
        }
    }

    private static int resolveFadeTicks(ClientParticle particle, int fadeTicks) {
        int lifetime = Math.max(1, particle.getLifetime());
        return fadeTicks <= 0 ? Math.max(1, lifetime / 2) : Math.max(1, Math.min(fadeTicks, lifetime));
    }

    private static float computeAlpha(ClientParticle particle, int fadeTicks) {
        int lifetime = Math.max(1, particle.getLifetime());
        float age = Math.min(lifetime, particle.getAge());

        if (particle.getInterpolationMode() == ParticleInterpolationMode.LINEAR) {
            return Math.clamp(1.0F - age / lifetime, 0.0F, 1.0F);
        }

        int fade = Math.max(1, Math.min(fadeTicks, lifetime));
        float fadeStart = lifetime - fade;
        float fadeOut = age > fadeStart ? (lifetime - age) / fade : 1.0F;
        float fadeIn = age / Math.max(1.0F, Math.min(3.0F, fade));
        float t = Math.clamp(Math.min(fadeIn, fadeOut), 0.0F, 1.0F);
        return t * t * (3.0F - 2.0F * t);
    }

    @Getter
    public static final class Entry {
        private final ClientParticle particle;
        private final int color;
        private final float size;
        private final int fadeTicks;
        private final float textureSeed = ThreadLocalRandom.current().nextFloat();
        private boolean visible = true;
        private float prevAlpha;
        private float alpha;

        private Entry(ClientParticle particle, int color, float size, int fadeTicks) {
            this.particle = particle;
            this.color = color;
            this.size = size;
            this.fadeTicks = fadeTicks;
        }

        public float alpha(float partialTicks) {
            return prevAlpha + (alpha - prevAlpha) * partialTicks;
        }
    }
}
