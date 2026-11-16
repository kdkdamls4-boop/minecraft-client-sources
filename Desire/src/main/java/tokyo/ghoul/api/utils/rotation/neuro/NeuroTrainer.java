package tokyo.ghoul.api.utils.rotation.neuro;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtSession;
import tokyo.ghoul.api.utils.rotation.neuro.NeuroRecording.Frame;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public final class NeuroTrainer {
    private static final int HIDDEN = 32;
    private static final int HEAD = 32;
    private static final int WINDOW = 48;
    private static final int MIN_WINDOW = 8;
    private static final int WINDOWS_PER_STEP = 8;
    private static final int MAX_EPOCHS = 60;
    private static final int PATIENCE = 8;
    private static final double LR = 2e-3;
    private static final double WEIGHT_DECAY = 1e-3;
    private static final double GRAD_CLIP = 1.0;
    private static final double CLIP = 5.0;
    private static final float MAX_TICK_DELTA = 90f;
    private static final double MAX_JUMP = 4.0;
    private static final int STATIC_RUN = 20;
    private static final float ENGAGE_ANGLE = 75f;
    private static final float LOG_STD_MIN = -4f;
    private static final float LOG_STD_MAX = 3f;
    public static final int MIN_SAMPLES = 1200;
    public static final int GOOD_SAMPLES = 6000;
    private static final float SIM_TEMPERATURE = 0.35f;
    private static final int VAL_EVERY = 5;
    private static final int MIN_ATTACK_GAP = 9;
    private static final int MAX_ATTACK_GAP = 40;
    private static final int MAX_STORED_INTERVALS = 512;
    private static final double MAX_EXPORT_ERROR = 1e-3;
    private static final int SIM_EPISODES = 6;
    private static final int SIM_TICKS = 300;
    private static final int SIM_SETTLE = 20;
    private static final double SIM_EYE_HEIGHT = 1.62;

    public record Result(int samples, int droppedStatic, int epochs, double valLoss, double valMae,
                         int attackIntervals, double exportError, double simError, double simOnTarget) {
    }

    private record Sequence(float[][] x, float[][] y) {
    }

    private record Dataset(List<Sequence> sequences, List<Integer> attackIntervals, int samples, int droppedStatic) {
    }

    private record Window(double[][] x, double[][] y, int start, int end) {
    }

    private NeuroTrainer() {
    }

    public static Result train(List<List<Frame>> recordings, File output, Consumer<String> log) throws Exception {
        Dataset data = buildDataset(recordings);
        if (data.samples < MIN_SAMPLES) {
            throw new IllegalStateException("мало сэмплов боя: " + data.samples + " (нужно от " + MIN_SAMPLES
                    + ", это минута активного боя; статичных тиков выкинуто " + data.droppedStatic + ")");
        }
        if (data.samples < GOOD_SAMPLES) {
            log.accept("Сэмплов " + data.samples + " — модель обучится, но от " + GOOD_SAMPLES + " поведение заметно живее");
        }

        int f = NeuroFeatures.SIZE;
        float[] errLimit = errLimits(data.sequences);
        for (Sequence s : data.sequences) clipErrors(s.x, errLimit);

        float[] xMean = new float[f], xStd = new float[f];
        float[] yMean = new float[2], yStd = new float[2];
        stats(data.sequences, true, xMean, xStd);
        stats(data.sequences, false, yMean, yStd);

        List<Window> train = new ArrayList<>();
        List<Window> val = new ArrayList<>();
        int windowIndex = 0;
        for (Sequence s : data.sequences) {
            double[][] x = new double[s.x.length][];
            double[][] y = new double[s.y.length][2];
            for (int t = 0; t < s.x.length; t++) {
                x[t] = normalize(s.x[t], xMean, xStd);
                y[t][0] = (s.y[t][0] - yMean[0]) / yStd[0];
                y[t][1] = (s.y[t][1] - yMean[1]) / yStd[1];
            }
            for (int start = 0; start < s.x.length; start += WINDOW) {
                int end = Math.min(s.x.length, start + WINDOW);
                if (end - start < MIN_WINDOW) continue;
                Window w = new Window(x, y, start, end);
                if (windowIndex++ % VAL_EVERY == VAL_EVERY - 1) val.add(w);
                else train.add(w);
            }
        }
        if (val.isEmpty()) val.add(train.remove(train.size() - 1));
        log.accept(String.format(Locale.ROOT, "Датасет: %d сэмплов, %d окон (валидация %d), статичных тиков выкинуто %d",
                data.samples, train.size() + val.size(), val.size(), data.droppedStatic));

        Net net = new Net(f, HIDDEN, HEAD, new Random(42));
        Adam adam = new Adam(net.p.length);
        Random rng = new Random(1337);
        double bestLoss = Double.POSITIVE_INFINITY;
        double bestMae = 0;
        double[] bestParams = net.p.clone();
        int stale = 0;
        int epochs = 0;

        for (int epoch = 1; epoch <= MAX_EPOCHS; epoch++) {
            epochs = epoch;
            Collections.shuffle(train, rng);
            double[] trainAcc = new double[3];
            for (int i = 0; i < train.size(); i += WINDOWS_PER_STEP) {
                Arrays.fill(net.g, 0);
                double before = trainAcc[1];
                for (int j = i; j < Math.min(train.size(), i + WINDOWS_PER_STEP); j++) {
                    net.window(train.get(j), true, trainAcc);
                }
                adam.step(net, 1.0 / (trainAcc[1] - before));
            }

            double[] valAcc = new double[3];
            for (Window w : val) net.window(w, false, valAcc);
            double valLoss = valAcc[0] / valAcc[1];
            double valMae = valAcc[2] / valAcc[1] * yStd[0];

            boolean improved = valLoss < bestLoss - 1e-4;
            if (improved) {
                bestLoss = valLoss;
                bestMae = valMae;
                bestParams = net.p.clone();
                stale = 0;
            } else {
                stale++;
            }
            if (epoch == 1 || epoch % 5 == 0 || stale >= PATIENCE) {
                log.accept(String.format(Locale.ROOT, "эпоха %d: train %.3f, val %.3f, ошибка yaw %.2f°%s",
                        epoch, trainAcc[0] / trainAcc[1], valLoss, valMae, improved ? " *" : ""));
            }
            if (stale >= PATIENCE) break;
        }
        net.p = bestParams;

        List<Integer> intervals = new ArrayList<>(data.attackIntervals);
        if (intervals.size() > MAX_STORED_INTERVALS) {
            Collections.shuffle(intervals, new Random(7));
            intervals = new ArrayList<>(intervals.subList(0, MAX_STORED_INTERVALS));
        }
        Collections.sort(intervals);

        byte[] model = export(net, xMean, xStd, yMean, yStd, errLimit, intervals, data.samples);
        Window check = val.get(0);
        double exportError = verifyExport(model, net, check.x);
        if (exportError > MAX_EXPORT_ERROR) {
            throw new IllegalStateException(String.format(Locale.ROOT,
                    "ONNX разошёлся с Java на %.5f — модель не сохранена", exportError));
        }

        double[] sim = simulate(net, xMean, xStd, yMean, yStd, errLimit);
        log.accept(String.format(Locale.ROOT, "Симуляция боя: средняя ошибка прицела %.1f°, в хитбоксе %.0f%% тиков",
                sim[0], sim[1] * 100));
        if (sim[1] < 0.25) {
            log.accept("§cМодель плохо держит цель в симуляции — запиши больше боя с аимом по цели");
        }

        output.getParentFile().mkdirs();
        File temp = new File(output.getParentFile(), output.getName() + ".tmp");
        Files.write(temp.toPath(), model);
        Files.move(temp.toPath(), output.toPath(), StandardCopyOption.REPLACE_EXISTING);

        return new Result(data.samples, data.droppedStatic, epochs, bestLoss, bestMae,
                intervals.size(), exportError, sim[0], sim[1]);
    }


    private static Dataset buildDataset(List<List<Frame>> recordings) {
        List<Sequence> sequences = new ArrayList<>();
        List<Integer> intervals = new ArrayList<>();
        int samples = 0;
        int dropped = 0;

        for (List<Frame> frames : recordings) {
            int n = frames.size();
            if (n < 2) continue;

            boolean[] valid = new boolean[n - 1];
            boolean[] moving = new boolean[n - 1];
            for (int t = 0; t < n - 1; t++) {
                Frame a = frames.get(t), b = frames.get(t + 1);
                float dYaw = NeuroFeatures.wrap(b.yaw() - a.yaw());
                float dPitch = b.pitch() - a.pitch();
                valid[t] = a.hasTarget() && b.hasTarget()
                        && Math.abs(dYaw) <= MAX_TICK_DELTA && Math.abs(dPitch) <= MAX_TICK_DELTA
                        && dist(a.tgtX(), a.tgtY(), a.tgtZ(), b.tgtX(), b.tgtY(), b.tgtZ()) <= MAX_JUMP
                        && dist(a.eyeX(), a.eyeY(), a.eyeZ(), b.eyeX(), b.eyeY(), b.eyeZ()) <= MAX_JUMP;
                moving[t] = Math.abs(dYaw) > 1e-4f || Math.abs(dPitch) > 1e-4f;
                if (valid[t]) {
                    float[] aim = NeuroFeatures.angles(a.eyeX(), a.eyeY(), a.eyeZ(), a.tgtX(), a.tgtY(), a.tgtZ());
                    valid[t] = Math.abs(NeuroFeatures.wrap(aim[0] - a.yaw())) <= ENGAGE_ANGLE;
                }
            }

            for (int t = 0; t < n - 1; ) {
                if (!valid[t] || moving[t]) {
                    t++;
                    continue;
                }
                int end = t;
                while (end < n - 1 && valid[end] && !moving[end]) end++;
                if (end - t >= STATIC_RUN) {
                    Arrays.fill(valid, t, end, false);
                    dropped += end - t;
                }
                t = end;
            }

            int[] sinceAttack = new int[n];
            int lastAttack = -NeuroFeatures.MAX_SINCE_ATTACK;
            int lastTargetAttack = -1;
            for (int t = 0; t < n; t++) {
                Frame frame = frames.get(t);
                if (frame.attack()) {
                    if (frame.hasTarget() && lastTargetAttack >= 0) {
                        int gap = t - lastTargetAttack;
                        if (gap >= MIN_ATTACK_GAP && gap <= MAX_ATTACK_GAP) intervals.add(gap);
                    }
                    lastTargetAttack = frame.hasTarget() ? t : -1;
                    lastAttack = t;
                }
                sinceAttack[t] = t - lastAttack;
            }

            for (int t = 0; t < n - 1; ) {
                if (!valid[t]) {
                    t++;
                    continue;
                }
                int end = t;
                while (end < n - 1 && valid[end]) end++;
                if (end - t >= MIN_WINDOW) {
                    sequences.add(sequence(frames, t, end - t, sinceAttack));
                    samples += end - t;
                }
                t = end;
            }
        }
        return new Dataset(sequences, intervals, samples, dropped);
    }

    private static Sequence sequence(List<Frame> frames, int from, int length, int[] sinceAttack) {
        int reaction = NeuroFeatures.REACTION;
        float[][] x = new float[length][];
        float[][] y = new float[length][2];
        for (int i = 0; i < length; i++) {
            int t = from + i;
            Frame cur = frames.get(t);
            Frame next = frames.get(t + 1);

            boolean observed = i >= reaction;
            float errYaw = 0, errPitch = 0, velYaw = 0, velPitch = 0, distance = 0, size = 0;
            if (observed) {
                Frame seen = frames.get(t - reaction);
                float[] aim = NeuroFeatures.angles(cur.eyeX(), cur.eyeY(), cur.eyeZ(), seen.tgtX(), seen.tgtY(), seen.tgtZ());
                errYaw = NeuroFeatures.wrap(aim[0] - cur.yaw());
                errPitch = aim[1] - cur.pitch();
                distance = (float) dist(cur.eyeX(), cur.eyeY(), cur.eyeZ(), seen.tgtX(), seen.tgtY(), seen.tgtZ());
                size = (float) Math.toDegrees(Math.atan2(seen.tgtHalfWidth(), Math.max(0.1, distance)));
                if (i >= reaction + 1) {
                    Frame prev = frames.get(t - reaction - 1);
                    float[] prevAim = NeuroFeatures.angles(cur.eyeX(), cur.eyeY(), cur.eyeZ(), prev.tgtX(), prev.tgtY(), prev.tgtZ());
                    velYaw = NeuroFeatures.wrap(aim[0] - prevAim[0]);
                    velPitch = aim[1] - prevAim[1];
                }
            }
            x[i] = NeuroFeatures.build(errYaw, errPitch, velYaw, velPitch, distance, size, sinceAttack[t], observed);
            y[i][0] = NeuroFeatures.wrap(next.yaw() - cur.yaw());
            y[i][1] = next.pitch() - cur.pitch();
        }
        return new Sequence(x, y);
    }

    private static float[] errLimits(List<Sequence> sequences) {
        float[] limits = new float[2];
        for (int axis = 0; axis < 2; axis++) {
            List<Float> values = new ArrayList<>();
            for (Sequence s : sequences) {
                for (float[] x : s.x) {
                    if (x[7] > 0.5f) values.add(Math.abs(x[axis]));
                }
            }
            if (values.isEmpty()) {
                limits[axis] = 180f;
                continue;
            }
            Collections.sort(values);
            float p99 = values.get(Math.min(values.size() - 1, (int) (values.size() * 0.99)));
            limits[axis] = Math.max(15f, p99);
        }
        return limits;
    }

    private static void clipErrors(float[][] x, float[] limit) {
        for (float[] row : x) {
            row[0] = Math.max(-limit[0], Math.min(limit[0], row[0]));
            row[1] = Math.max(-limit[1], Math.min(limit[1], row[1]));
        }
    }

    private static void stats(List<Sequence> sequences, boolean input, float[] mean, float[] std) {
        int dims = mean.length;
        double[] sum = new double[dims];
        double[] sumSq = new double[dims];
        long count = 0;
        for (Sequence s : sequences) {
            for (float[] row : input ? s.x : s.y) {
                for (int i = 0; i < dims; i++) {
                    sum[i] += row[i];
                    sumSq[i] += (double) row[i] * row[i];
                }
                count++;
            }
        }
        for (int i = 0; i < dims; i++) {
            double m = sum[i] / count;
            double variance = Math.max(0, sumSq[i] / count - m * m);
            mean[i] = (float) m;
            std[i] = variance < 1e-8 ? 1f : (float) Math.sqrt(variance);
        }
    }

    private static double[] normalize(float[] raw, float[] mean, float[] std) {
        double[] z = new double[raw.length];
        for (int i = 0; i < raw.length; i++) {
            float v = (raw[i] - mean[i]) / std[i];
            z[i] = Math.max(-CLIP, Math.min(CLIP, v));
        }
        return z;
    }

    private static double dist(double ax, double ay, double az, double bx, double by, double bz) {
        double dx = ax - bx, dy = ay - by, dz = az - bz;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }


    private static final class Net {
        final int f, h, head;
        final int oWx, oWh, ob, oW1, ob1, oW2, ob2, oL;
        double[] p;
        final double[] g;

        Net(int f, int h, int head, Random random) {
            this.f = f;
            this.h = h;
            this.head = head;
            int gates = 3 * h;
            oWx = 0;
            oWh = oWx + gates * f;
            ob = oWh + gates * h;
            oW1 = ob + gates;
            ob1 = oW1 + head * h;
            oW2 = ob1 + head;
            ob2 = oW2 + NeuroFeatures.OUTPUTS * head;
            oL = ob2 + NeuroFeatures.OUTPUTS;
            p = new double[oL + NeuroFeatures.OUTPUTS * f];
            g = new double[p.length];

            double gruScale = 1.0 / Math.sqrt(h);
            for (int i = oWx; i < ob; i++) p[i] = (random.nextDouble() * 2 - 1) * gruScale;
            double headScale = Math.sqrt(6.0 / (h + head));
            for (int i = oW1; i < ob1; i++) p[i] = (random.nextDouble() * 2 - 1) * headScale;
            double outScale = 0.1 * Math.sqrt(6.0 / (head + NeuroFeatures.OUTPUTS));
            for (int i = oW2; i < ob2; i++) p[i] = (random.nextDouble() * 2 - 1) * outScale;
        }

        private double gate(int row, double[] x, double[] hv) {
            double s = p[ob + row];
            int wx = oWx + row * f;
            for (int i = 0; i < f; i++) s += p[wx + i] * x[i];
            int wh = oWh + row * h;
            for (int k = 0; k < h; k++) s += p[wh + k] * hv[k];
            return s;
        }

        void step(double[] x, double[] hp, double[] z, double[] r, double[] hc, double[] rh,
                  double[] hn, double[] a1, double[] out) {
            for (int j = 0; j < h; j++) {
                z[j] = sigmoid(gate(j, x, hp));
                r[j] = sigmoid(gate(h + j, x, hp));
            }
            for (int k = 0; k < h; k++) rh[k] = r[k] * hp[k];
            for (int j = 0; j < h; j++) {
                hc[j] = Math.tanh(gate(2 * h + j, x, rh));
                hn[j] = (1 - z[j]) * hc[j] + z[j] * hp[j];
            }
            for (int j = 0; j < head; j++) {
                double s = p[ob1 + j];
                int row = oW1 + j * h;
                for (int k = 0; k < h; k++) s += p[row + k] * hn[k];
                a1[j] = Math.tanh(s);
            }
            for (int o = 0; o < NeuroFeatures.OUTPUTS; o++) {
                double s = p[ob2 + o];
                int row = oW2 + o * head;
                for (int j = 0; j < head; j++) s += p[row + j] * a1[j];
                int lin = oL + o * f;
                for (int i = 0; i < f; i++) s += p[lin + i] * x[i];
                out[o] = s;
            }
        }

        void window(Window w, boolean backward, double[] acc) {
            double[] hp = new double[h];
            double[] z = new double[h], r = new double[h], hc = new double[h], rh = new double[h];
            double[] hn = new double[h], a1 = new double[head], out = new double[NeuroFeatures.OUTPUTS];
            for (int t = Math.max(0, w.start - WINDOW); t < w.start; t++) {
                step(w.x[t], hp, z, r, hc, rh, hn, a1, out);
                System.arraycopy(hn, 0, hp, 0, h);
            }

            int n = w.end - w.start;
            double[] h0 = hp;
            double[][] Z = new double[n][h], R = new double[n][h], HC = new double[n][h], RH = new double[n][h];
            double[][] HS = new double[n][h], A1 = new double[n][head], OUT = new double[n][NeuroFeatures.OUTPUTS];
            double[][] dOut = backward ? new double[n][NeuroFeatures.OUTPUTS] : null;

            for (int t = 0; t < n; t++) {
                step(w.x[w.start + t], t == 0 ? h0 : HS[t - 1], Z[t], R[t], HC[t], RH[t], HS[t], A1[t], OUT[t]);
                double[] y = w.y[w.start + t];
                for (int d = 0; d < 2; d++) {
                    double mu = OUT[t][d];
                    double rawLogStd = OUT[t][2 + d];
                    double logStd = Math.max(LOG_STD_MIN, Math.min(LOG_STD_MAX, rawLogStd));
                    double inv = Math.exp(-logStd);
                    double e = (y[d] - mu) * inv;
                    acc[0] += 0.5 * e * e + logStd;
                    if (backward) {
                        dOut[t][d] = -e * inv;
                        dOut[t][2 + d] = rawLogStd > LOG_STD_MIN && rawLogStd < LOG_STD_MAX ? 1 - e * e : 0;
                    }
                }
                acc[2] += Math.abs(y[0] - OUT[t][0]);
            }
            acc[1] += n;
            if (!backward) return;

            double[] dhNext = new double[h];
            double[] dh = new double[h], dhPrev = new double[h], da1 = new double[head];
            double[] dah = new double[h], daz = new double[h], dar = new double[h], drh = new double[h];
            for (int t = n - 1; t >= 0; t--) {
                double[] x = w.x[w.start + t];
                double[] hPrev = t == 0 ? h0 : HS[t - 1];

                Arrays.fill(da1, 0);
                for (int o = 0; o < NeuroFeatures.OUTPUTS; o++) {
                    double d = dOut[t][o];
                    if (d == 0) continue;
                    int row = oW2 + o * head;
                    g[ob2 + o] += d;
                    int lin = oL + o * f;
                    for (int i = 0; i < f; i++) g[lin + i] += d * x[i];
                    for (int j = 0; j < head; j++) {
                        g[row + j] += d * A1[t][j];
                        da1[j] += p[row + j] * d;
                    }
                }

                System.arraycopy(dhNext, 0, dh, 0, h);
                for (int j = 0; j < head; j++) {
                    double dp = da1[j] * (1 - A1[t][j] * A1[t][j]);
                    g[ob1 + j] += dp;
                    int row = oW1 + j * h;
                    for (int k = 0; k < h; k++) {
                        g[row + k] += dp * HS[t][k];
                        dh[k] += p[row + k] * dp;
                    }
                }

                for (int j = 0; j < h; j++) {
                    double zj = Z[t][j], hcj = HC[t][j];
                    dah[j] = dh[j] * (1 - zj) * (1 - hcj * hcj);
                    daz[j] = dh[j] * (hPrev[j] - hcj) * zj * (1 - zj);
                    dhPrev[j] = dh[j] * zj;
                }

                Arrays.fill(drh, 0);
                for (int j = 0; j < h; j++) {
                    double d = dah[j];
                    int row = 2 * h + j;
                    g[ob + row] += d;
                    int wx = oWx + row * f;
                    for (int i = 0; i < f; i++) g[wx + i] += d * x[i];
                    int wh = oWh + row * h;
                    for (int k = 0; k < h; k++) {
                        g[wh + k] += d * RH[t][k];
                        drh[k] += p[wh + k] * d;
                    }
                }
                for (int k = 0; k < h; k++) {
                    double rk = R[t][k];
                    dar[k] = drh[k] * hPrev[k] * rk * (1 - rk);
                    dhPrev[k] += drh[k] * rk;
                }

                for (int gateIndex = 0; gateIndex < 2; gateIndex++) {
                    double[] da = gateIndex == 0 ? daz : dar;
                    for (int j = 0; j < h; j++) {
                        double d = da[j];
                        int row = gateIndex * h + j;
                        g[ob + row] += d;
                        int wx = oWx + row * f;
                        for (int i = 0; i < f; i++) g[wx + i] += d * x[i];
                        int wh = oWh + row * h;
                        for (int k = 0; k < h; k++) {
                            g[wh + k] += d * hPrev[k];
                            dhPrev[k] += p[wh + k] * d;
                        }
                    }
                }

                double[] swap = dhNext;
                dhNext = dhPrev;
                dhPrev = swap;
            }
        }

        boolean isBias(int i) {
            return (i >= ob && i < oW1) || (i >= ob1 && i < oW2) || (i >= ob2 && i < oL);
        }

        private static double sigmoid(double v) {
            return 1.0 / (1.0 + Math.exp(-v));
        }
    }

    private static final class Adam {
        private static final double BETA1 = 0.9;
        private static final double BETA2 = 0.999;
        private final double[] m, v;
        private int t;

        Adam(int size) {
            m = new double[size];
            v = new double[size];
        }

        void step(Net net, double scale) {
            double norm = 0;
            for (double gi : net.g) norm += gi * gi * scale * scale;
            norm = Math.sqrt(norm);
            double k = scale * (norm > GRAD_CLIP ? GRAD_CLIP / norm : 1.0);

            t++;
            double c1 = 1 - Math.pow(BETA1, t);
            double c2 = 1 - Math.pow(BETA2, t);
            for (int i = 0; i < net.p.length; i++) {
                double gi = net.g[i] * k;
                m[i] = BETA1 * m[i] + (1 - BETA1) * gi;
                v[i] = BETA2 * v[i] + (1 - BETA2) * gi * gi;
                net.p[i] -= LR * (m[i] / c1) / (Math.sqrt(v[i] / c2) + 1e-8);
                if (!net.isBias(i)) net.p[i] -= LR * WEIGHT_DECAY * net.p[i];
            }
        }
    }


    private static byte[] export(Net net, float[] xMean, float[] xStd, float[] yMean, float[] yStd,
                                 float[] errLimit, List<Integer> intervals, int samples) {
        int h = net.h, f = net.f, gates = 3 * h;
        float[][] w = new float[gates][f];
        float[][] r = new float[gates][h];
        float[] b = new float[gates];
        for (int row = 0; row < gates; row++) {
            for (int i = 0; i < f; i++) w[row][i] = (float) net.p[net.oWx + row * f + i];
            for (int k = 0; k < h; k++) r[row][k] = (float) net.p[net.oWh + row * h + k];
            b[row] = (float) net.p[net.ob + row];
        }
        float[][] w1 = new float[h][net.head];
        float[] b1 = new float[net.head];
        for (int j = 0; j < net.head; j++) {
            for (int k = 0; k < h; k++) w1[k][j] = (float) net.p[net.oW1 + j * h + k];
            b1[j] = (float) net.p[net.ob1 + j];
        }
        float[][] w2 = new float[net.head][NeuroFeatures.OUTPUTS];
        float[] b2 = new float[NeuroFeatures.OUTPUTS];
        for (int o = 0; o < NeuroFeatures.OUTPUTS; o++) {
            for (int j = 0; j < net.head; j++) w2[j][o] = (float) net.p[net.oW2 + o * net.head + j];
            b2[o] = (float) net.p[net.ob2 + o];
        }
        float[][] lin = new float[f][NeuroFeatures.OUTPUTS];
        for (int o = 0; o < NeuroFeatures.OUTPUTS; o++) {
            for (int i = 0; i < f; i++) lin[i][o] = (float) net.p[net.oL + o * f + i];
        }

        return new OnnxWriter()
                .input(NeuroModel.INPUT, 1, f)
                .input(NeuroModel.STATE_IN, 1, 1, h)
                .reshape(NeuroModel.INPUT, "x_seq", 1, 1, f)
                .gru("x_seq", NeuroModel.STATE_IN, "y_seq", NeuroModel.STATE_OUT, w, r, b)
                .reshape(NeuroModel.STATE_OUT, "h_flat", 1, h)
                .gemm("h_flat", "head_pre", w1, b1)
                .tanh("head_pre", "head")
                .gemm("head", "head_out", w2, b2)
                .gemm(NeuroModel.INPUT, "linear_out", lin, new float[NeuroFeatures.OUTPUTS])
                .add("head_out", "linear_out", NeuroModel.OUTPUT)
                .output(NeuroModel.OUTPUT, 1, NeuroFeatures.OUTPUTS)
                .output(NeuroModel.STATE_OUT, 1, 1, h)
                .meta("arch", NeuroFeatures.ARCH)
                .meta("features", Integer.toString(f))
                .meta("hidden", Integer.toString(h))
                .meta("reaction", Integer.toString(NeuroFeatures.REACTION))
                .meta("x_mean", csv(xMean))
                .meta("x_std", csv(xStd))
                .meta("y_mean", csv(yMean))
                .meta("y_std", csv(yStd))
                .meta("err_limit", csv(errLimit))
                .meta("log_std_range", csv(new float[]{LOG_STD_MIN, LOG_STD_MAX}))
                .meta("attack_intervals", intervals.stream().map(String::valueOf).collect(Collectors.joining(",")))
                .meta("samples", Integer.toString(samples))
                .build();
    }

    private static double verifyExport(byte[] model, Net net, double[][] sequence) throws Exception {
        OrtEnvironment env = OrtEnvironment.getEnvironment();
        int h = net.h;
        double[] hp = new double[h];
        double[] z = new double[h], r = new double[h], hc = new double[h], rh = new double[h];
        double[] hn = new double[h], a1 = new double[net.head], out = new double[NeuroFeatures.OUTPUTS];
        float[][][] state = new float[1][1][h];
        double maxError = 0;

        try (OrtSession.SessionOptions options = new OrtSession.SessionOptions();
             OrtSession session = env.createSession(model, options)) {
            int steps = Math.min(sequence.length, 300);
            for (int t = 0; t < steps; t++) {
                float[][] x = new float[1][net.f];
                double[] xd = new double[net.f];
                for (int i = 0; i < net.f; i++) {
                    x[0][i] = (float) sequence[t][i];
                    xd[i] = x[0][i];
                }
                float[] ortOut;
                try (OnnxTensor xt = OnnxTensor.createTensor(env, x);
                     OnnxTensor ht = OnnxTensor.createTensor(env, state);
                     OrtSession.Result result = session.run(Map.of(NeuroModel.INPUT, xt, NeuroModel.STATE_IN, ht))) {
                    ortOut = ((float[][]) result.get(NeuroModel.OUTPUT).orElseThrow().getValue())[0];
                    state = (float[][][]) result.get(NeuroModel.STATE_OUT).orElseThrow().getValue();
                }
                net.step(xd, hp, z, r, hc, rh, hn, a1, out);
                System.arraycopy(hn, 0, hp, 0, h);
                for (int o = 0; o < NeuroFeatures.OUTPUTS; o++) {
                    maxError = Math.max(maxError, Math.abs(ortOut[o] - out[o]));
                }
            }
        }
        return maxError;
    }

    private static double[] simulate(Net net, float[] xMean, float[] xStd, float[] yMean, float[] yStd, float[] errLimit) {
        Random random = new Random(99);
        int h = net.h;
        int reaction = NeuroFeatures.REACTION;
        double[] z = new double[h], r = new double[h], hc = new double[h], rh = new double[h];
        double[] hn = new double[h], a1 = new double[net.head], out = new double[NeuroFeatures.OUTPUTS];
        double errorSum = 0;
        int onTarget = 0;
        int counted = 0;

        for (int episode = 0; episode < SIM_EPISODES; episode++) {
            double distance = 2.6 + random.nextDouble() * 0.8;
            double angle = random.nextDouble() * Math.PI * 2;
            double angularVel = 0;
            double feetY = 0, velY = 0;
            List<double[]> trail = new ArrayList<>();
            double[] hp = new double[h];
            int lastAttack = -NeuroFeatures.MAX_SINCE_ATTACK;

            double cx0 = -Math.sin(angle) * distance, cz0 = Math.cos(angle) * distance;
            float[] start = NeuroFeatures.angles(0, SIM_EYE_HEIGHT, 0, cx0, 0.9, cz0);
            float yaw = start[0] + (float) ((random.nextBoolean() ? 1 : -1) * (20 + random.nextDouble() * 40));
            float pitch = Math.max(-90f, Math.min(90f, start[1] + (float) (random.nextGaussian() * 8)));

            for (int tick = 0; tick < SIM_TICKS; tick++) {
                angularVel = 0.85 * angularVel + random.nextGaussian() * 0.03;
                if (random.nextDouble() < 0.03) angularVel = -angularVel;
                angle += angularVel;
                distance = Math.max(2.2, Math.min(3.8, distance + random.nextGaussian() * 0.05));
                if (feetY <= 0 && random.nextDouble() < 0.04) velY = 0.42;
                feetY += velY;
                velY = (velY - 0.08) * 0.98;
                if (feetY <= 0) {
                    feetY = 0;
                    velY = 0;
                }
                double cx = -Math.sin(angle) * distance, cy = feetY + 0.9, cz = Math.cos(angle) * distance;
                trail.add(new double[]{cx, cy, cz});
                if (trail.size() > reaction + 2) trail.remove(0);

                int seenIndex = trail.size() - 1 - reaction;
                boolean observed = seenIndex >= 0;
                float errYaw = 0, errPitch = 0, velYaw = 0, velPitch = 0, dist = 0, size = 0;
                if (observed) {
                    double[] seen = trail.get(seenIndex);
                    float[] aim = NeuroFeatures.angles(0, SIM_EYE_HEIGHT, 0, seen[0], seen[1], seen[2]);
                    errYaw = NeuroFeatures.wrap(aim[0] - yaw);
                    errPitch = aim[1] - pitch;
                    dist = (float) NeuroTrainer.dist(0, SIM_EYE_HEIGHT, 0, seen[0], seen[1], seen[2]);
                    size = (float) Math.toDegrees(Math.atan2(0.3, Math.max(0.1, dist)));
                    if (seenIndex >= 1) {
                        double[] prev = trail.get(seenIndex - 1);
                        float[] prevAim = NeuroFeatures.angles(0, SIM_EYE_HEIGHT, 0, prev[0], prev[1], prev[2]);
                        velYaw = NeuroFeatures.wrap(aim[0] - prevAim[0]);
                        velPitch = aim[1] - prevAim[1];
                    }
                }
                float[] raw = NeuroFeatures.build(errYaw, errPitch, velYaw, velPitch, dist, size, tick - lastAttack, observed);
                clipErrors(new float[][]{raw}, errLimit);

                net.step(normalize(raw, xMean, xStd), hp, z, r, hc, rh, hn, a1, out);
                System.arraycopy(hn, 0, hp, 0, h);
                double sigmaYaw = Math.exp(Math.max(LOG_STD_MIN, Math.min(LOG_STD_MAX, out[2]))) * yStd[0];
                double sigmaPitch = Math.exp(Math.max(LOG_STD_MIN, Math.min(LOG_STD_MAX, out[3]))) * yStd[1];
                double yawStep = NeuroControl.step((float) (out[0] * yStd[0] + yMean[0]), (float) sigmaYaw,
                        random.nextGaussian(), SIM_TEMPERATURE, errYaw, size, observed);
                double pitchStep = NeuroControl.step((float) (out[1] * yStd[1] + yMean[1]), (float) sigmaPitch,
                        random.nextGaussian(), SIM_TEMPERATURE, errPitch, size * 2, observed);
                yaw += (float) Math.max(-50, Math.min(50, yawStep));
                pitch = (float) Math.max(-90, Math.min(90, pitch + Math.max(-35, Math.min(35, pitchStep))));

                float[] truth = NeuroFeatures.angles(0, SIM_EYE_HEIGHT, 0, cx, cy, cz);
                double offYaw = NeuroFeatures.wrap(truth[0] - yaw);
                double offPitch = truth[1] - pitch;
                double halfYaw = Math.toDegrees(Math.atan2(0.3, distance));
                double halfPitch = Math.toDegrees(Math.atan2(0.9, distance));
                boolean inBox = Math.abs(offYaw) <= halfYaw && Math.abs(offPitch) <= halfPitch;
                if (inBox && tick - lastAttack >= 10) lastAttack = tick;
                if (tick >= SIM_SETTLE) {
                    errorSum += Math.hypot(offYaw, offPitch);
                    if (inBox) onTarget++;
                    counted++;
                }
            }
        }
        return new double[]{errorSum / counted, onTarget / (double) counted};
    }

    private static String csv(float[] values) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            if (i > 0) builder.append(',');
            builder.append(values[i]);
        }
        return builder.toString();
    }
}
