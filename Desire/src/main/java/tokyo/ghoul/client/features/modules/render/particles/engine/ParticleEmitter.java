package tokyo.ghoul.client.features.modules.render.particles.engine;

public abstract class ParticleEmitter {
    private boolean finished;

    protected abstract void emit(ClientParticleManager manager);

    public final boolean isActive() {
        return !finished;
    }

    public final void tick(ClientParticleManager manager) {
        if (finished || manager == null) return;
        try {
            emit(manager);
        } catch (Throwable throwable) {
            finish();
        }
    }

    public final void finish() {
        finished = true;
    }
}
