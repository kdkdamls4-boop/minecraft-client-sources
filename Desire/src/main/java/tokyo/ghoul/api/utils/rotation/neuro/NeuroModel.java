package tokyo.ghoul.api.utils.rotation.neuro;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import lombok.Getter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;
import java.util.Random;

public final class NeuroModel implements AutoCloseable {
    public static final String INPUT = "x";
    public static final String STATE_IN = "h_in";
    public static final String OUTPUT = "out";
    public static final String STATE_OUT = "h_out";

    @Getter private final String name;
    @Getter private final int reaction;
    @Getter private final int hidden;
    private final OrtEnvironment env;
    private final OrtSession session;
    private final float[] xMean, xStd, yMean, yStd;
    private final float[] errLimit;
    private final float logStdMin, logStdMax;
    private final int[] attackIntervals;

    public record Prediction(float muYaw, float muPitch, float sigmaYaw, float sigmaPitch) {
    }

    private NeuroModel(String name, OrtEnvironment env, OrtSession session) throws OrtException {
        this.name = name;
        this.env = env;
        this.session = session;

        Map<String, String> meta = session.getMetadata().getCustomMetadata();
        int features = Integer.parseInt(meta.getOrDefault("features", "0"));
        String arch = meta.getOrDefault("arch", "?");
        if (features != NeuroFeatures.SIZE || !arch.equals(NeuroFeatures.ARCH)) {
            throw new IllegalStateException("модель обучена под другой формат (" + arch + ", вход " + features
                    + "), переобучи .neuro train");
        }
        this.hidden = Integer.parseInt(meta.getOrDefault("hidden", "0"));
        if (hidden <= 0) throw new IllegalStateException("в модели не указан размер состояния");
        this.reaction = Integer.parseInt(meta.getOrDefault("reaction", Integer.toString(NeuroFeatures.REACTION)));
        this.xMean = floats(meta.get("x_mean"));
        this.xStd = floats(meta.get("x_std"));
        this.yMean = floats(meta.get("y_mean"));
        this.yStd = floats(meta.get("y_std"));
        this.errLimit = meta.containsKey("err_limit") ? floats(meta.get("err_limit")) : null;
        float[] range = floats(meta.getOrDefault("log_std_range", "-4,3"));
        this.logStdMin = range[0];
        this.logStdMax = range[1];
        String intervals = meta.getOrDefault("attack_intervals", "");
        this.attackIntervals = intervals.isBlank() ? new int[0]
                : java.util.Arrays.stream(intervals.split(",")).mapToInt(Integer::parseInt).toArray();
    }

    public static NeuroModel load(File file) throws IOException, OrtException {
        OrtEnvironment env = OrtEnvironment.getEnvironment();
        OrtSession.SessionOptions options = new OrtSession.SessionOptions();
        options.setIntraOpNumThreads(1);
        options.setInterOpNumThreads(1);
        OrtSession session = env.createSession(Files.readAllBytes(file.toPath()), options);
        String name = file.getName().replaceFirst("\\.onnx$", "");
        try {
            return new NeuroModel(name, env, session);
        } catch (RuntimeException | OrtException e) {
            session.close();
            throw e;
        }
    }

    public float[] newState() {
        return new float[hidden];
    }

    public Prediction predict(float[] features, float[] state) throws OrtException {
        float[][] input = new float[1][features.length];
        for (int i = 0; i < features.length; i++) {
            float value = features[i];
            if (errLimit != null && i < errLimit.length) value = Math.max(-errLimit[i], Math.min(errLimit[i], value));
            float z = (value - xMean[i]) / xStd[i];
            input[0][i] = Math.max(-5f, Math.min(5f, z));
        }
        float[][][] stateIn = new float[1][1][];
        stateIn[0][0] = state;

        try (OnnxTensor tensor = OnnxTensor.createTensor(env, input);
             OnnxTensor stateTensor = OnnxTensor.createTensor(env, stateIn);
             OrtSession.Result result = session.run(Map.of(INPUT, tensor, STATE_IN, stateTensor))) {
            float[] out = ((float[][]) result.get(OUTPUT).orElseThrow().getValue())[0];
            float[] next = ((float[][][]) result.get(STATE_OUT).orElseThrow().getValue())[0][0];
            System.arraycopy(next, 0, state, 0, hidden);
            return new Prediction(
                    out[0] * yStd[0] + yMean[0],
                    out[1] * yStd[1] + yMean[1],
                    (float) Math.exp(clamp(out[2])) * yStd[0],
                    (float) Math.exp(clamp(out[3])) * yStd[1]);
        }
    }

    public int sampleAttackInterval(Random random) {
        if (attackIntervals.length == 0) return -1;
        return attackIntervals[random.nextInt(attackIntervals.length)];
    }

    private float clamp(float logStd) {
        return Math.max(logStdMin, Math.min(logStdMax, logStd));
    }

    private static float[] floats(String csv) {
        if (csv == null || csv.isBlank()) throw new IllegalStateException("в модели нет нормализации");
        String[] parts = csv.split(",");
        float[] values = new float[parts.length];
        for (int i = 0; i < parts.length; i++) values[i] = Float.parseFloat(parts[i].trim());
        return values;
    }

    @Override
    public void close() {
        try {
            session.close();
        } catch (OrtException ignored) {
        }
    }
}
