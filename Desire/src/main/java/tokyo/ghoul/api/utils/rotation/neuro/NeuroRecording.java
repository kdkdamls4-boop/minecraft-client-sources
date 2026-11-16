package tokyo.ghoul.api.utils.rotation.neuro;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class NeuroRecording {
    private static final String HEADER = "yaw,pitch,eyeX,eyeY,eyeZ,hasTarget,tgtX,tgtY,tgtZ,tgtHalfWidth,tgtHeight,attack";

    public record Frame(float yaw, float pitch,
                        double eyeX, double eyeY, double eyeZ,
                        boolean hasTarget, double tgtX, double tgtY, double tgtZ,
                        float tgtHalfWidth, float tgtHeight,
                        boolean attack) {
    }

    private NeuroRecording() {
    }

    public static void write(File file, List<Frame> frames) throws IOException {
        file.getParentFile().mkdirs();
        try (BufferedWriter writer = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8)) {
            writer.write(HEADER);
            writer.newLine();
            for (Frame f : frames) {
                writer.write(String.format(Locale.ROOT, "%.5f,%.5f,%.4f,%.4f,%.4f,%d,%.4f,%.4f,%.4f,%.3f,%.3f,%d",
                        f.yaw, f.pitch, f.eyeX, f.eyeY, f.eyeZ, f.hasTarget ? 1 : 0,
                        f.tgtX, f.tgtY, f.tgtZ, f.tgtHalfWidth, f.tgtHeight, f.attack ? 1 : 0));
                writer.newLine();
            }
        }
    }

    public static List<Frame> read(File file) throws IOException {
        List<Frame> frames = new ArrayList<>();
        try (BufferedReader reader = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
            String line = reader.readLine();
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) continue;
                String[] p = line.split(",");
                if (p.length < 12) continue;
                frames.add(new Frame(
                        Float.parseFloat(p[0]), Float.parseFloat(p[1]),
                        Double.parseDouble(p[2]), Double.parseDouble(p[3]), Double.parseDouble(p[4]),
                        p[5].equals("1"),
                        Double.parseDouble(p[6]), Double.parseDouble(p[7]), Double.parseDouble(p[8]),
                        Float.parseFloat(p[9]), Float.parseFloat(p[10]),
                        p[11].equals("1")));
            }
        }
        return frames;
    }
}
