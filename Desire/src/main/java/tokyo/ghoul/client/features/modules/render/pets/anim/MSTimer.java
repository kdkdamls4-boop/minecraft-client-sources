package tokyo.ghoul.client.features.modules.render.pets.anim;

public class MSTimer {
    private long millis;

    public MSTimer() {
        reset();
    }

    public boolean finished(float delay) {
        return System.currentTimeMillis() - delay >= millis;
    }

    public boolean finished(long delay) {
        return System.currentTimeMillis() - delay >= millis;
    }

    public void setMillis(long millis) {
        this.millis = millis;
    }

    public long getMillis() {
        return millis;
    }

    public void reset() {
        millis = System.currentTimeMillis();
    }

    public long getElapsedTime() {
        return System.currentTimeMillis() - millis;
    }
}
