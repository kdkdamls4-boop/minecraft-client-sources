package tokyo.ghoul.api.utils.rotation.misc;

import lombok.experimental.UtilityClass;
import tokyo.ghoul.api.system.interfaces.QuickImports;

@UtilityClass
public class GCDFixer implements QuickImports {

    public float getFixRotate(float delta) {
        return Math.round(delta / getGCDValue()) * getGCDValue();
    }

    public float getGCDValue() {
        return (float) (getGCD() * 0.15);
    }

    public double getGCD() {
        float mouseFactor = (float) (mc.options.sensitivity().get() * 0.6 + 0.2);
        return mouseFactor * mouseFactor * mouseFactor * 8.0f;
    }
}
