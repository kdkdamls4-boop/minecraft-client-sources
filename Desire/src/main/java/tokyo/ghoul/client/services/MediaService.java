package tokyo.ghoul.client.services;

import com.mojang.blaze3d.platform.NativeImage;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class MediaService {
    @Getter
    private static final MediaService instance = new MediaService();

    private static final String SCRIPT_RESOURCE = "/assets/desire/media/media_session.ps1";
    private static final long RESTART_DELAY_MS = 15_000L;

    public record Info(boolean active, String title, String artist, boolean playing,
                       long positionMs, long durationMs, long stampNanos, String coverPath) {
        static final Info NONE = new Info(false, "", "", false, 0L, 0L, 0L, "");
    }

    private static final long IDLE_TIMEOUT_NS = 10_000_000_000L;

    private volatile Info info = Info.NONE;
    private volatile boolean running;
    private volatile long lastUse;
    private boolean shutdownHookAdded;
    private Process process;
    private Path scriptPath;
    private Path commandFile;
    private Path coverDir;
    private long lastStartAttempt;

    private String loadedCoverPath = "";
    private Identifier coverId;
    private float coverAspect = 1f;
    private int coverCounter;

    private MediaService() {
    }

    public static boolean supported() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }

    public void ensureStarted() {
        if (!supported()) return;
        lastUse = System.nanoTime();
        if (running) return;
        startProcess();
    }

    private synchronized void startProcess() {
        if (running) return;
        long now = System.currentTimeMillis();
        if (now - lastStartAttempt < RESTART_DELAY_MS) return;
        lastStartAttempt = now;

        try {
            Path dir = Files.createTempDirectory("desire_media");
            scriptPath = dir.resolve("media_session.ps1");
            commandFile = dir.resolve("command.txt");
            coverDir = dir;
            try (InputStream in = MediaService.class.getResourceAsStream(SCRIPT_RESOURCE)) {
                if (in == null) return;
                Files.copy(in, scriptPath, StandardCopyOption.REPLACE_EXISTING);
            }

            ProcessBuilder builder = new ProcessBuilder("powershell.exe", "-NoProfile", "-NonInteractive",
                    "-ExecutionPolicy", "Bypass", "-WindowStyle", "Hidden", "-File", scriptPath.toString(),
                    commandFile.toString(), coverDir.toString(), String.valueOf(ProcessHandle.current().pid()));
            builder.redirectError(ProcessBuilder.Redirect.DISCARD);
            Process started = builder.start();
            process = started;
            running = true;
            lastUse = System.nanoTime();
            if (!shutdownHookAdded) {
                shutdownHookAdded = true;
                Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                    Process current = process;
                    if (current != null) current.destroyForcibly();
                }));
            }

            Thread reader = new Thread(() -> readLoop(started), "desire-media-reader");
            reader.setDaemon(true);
            reader.start();

            Thread reaper = new Thread(() -> reapWhenIdle(started), "desire-media-reaper");
            reaper.setDaemon(true);
            reaper.start();
        } catch (IOException ignored) {
        }
    }

    private void reapWhenIdle(Process started) {
        try {
            while (started.isAlive()) {
                Thread.sleep(5_000L);
                if (System.nanoTime() - lastUse > IDLE_TIMEOUT_NS) {
                    started.destroyForcibly();
                    return;
                }
            }
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }

    private void readLoop(Process started) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(started.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.replace("﻿", "").trim();
                if (line.isEmpty() || line.charAt(0) != '{') continue;
                try {
                    JSONObject json = new JSONObject(line);
                    if (!json.optBoolean("active", false)) {
                        info = Info.NONE;
                        continue;
                    }
                    info = new Info(true, json.optString("title", ""), json.optString("artist", ""),
                            json.optBoolean("playing", false), json.optLong("pos", 0L), json.optLong("dur", 0L),
                            System.nanoTime(), json.optString("cover", ""));
                } catch (RuntimeException ignored) {
                }
            }
        } catch (IOException ignored) {
        }
        info = Info.NONE;
        running = false;
    }

    public Info info() {
        return info;
    }

    public boolean hasTrack() {
        Info current = info;
        return current.active() && !current.title().isEmpty();
    }

    public long positionMs() {
        Info current = info;
        long position = current.positionMs();
        if (current.playing()) position += (System.nanoTime() - current.stampNanos()) / 1_000_000L;
        if (current.durationMs() > 0L) position = Math.min(position, current.durationMs());
        return Math.max(position, 0L);
    }

    public float progress() {
        Info current = info;
        if (current.durationMs() <= 0L) return 0f;
        return Math.min(1f, Math.max(0f, positionMs() / (float) current.durationMs()));
    }

    public void previous() {
        send("prev");
    }

    public void next() {
        send("next");
    }

    public void toggle() {
        send("toggle");
    }

    private void send(String command) {
        Path file = commandFile;
        if (file == null) return;
        try {
            Files.writeString(file, command, StandardCharsets.UTF_8);
        } catch (IOException ignored) {
        }
    }

    public Identifier coverTexture() {
        String path = info.coverPath();
        if (!path.equals(loadedCoverPath)) {
            loadedCoverPath = path;
            releaseCover();
            if (!path.isEmpty()) loadCover(path);
        }
        return coverId;
    }

    public float coverAspect() {
        return coverAspect;
    }

    private void loadCover(String path) {
        try (InputStream in = Files.newInputStream(Path.of(path))) {
            NativeImage image = NativeImage.read(in);
            coverAspect = image.getHeight() > 0 ? image.getWidth() / (float) image.getHeight() : 1f;
            Identifier id = Identifier.fromNamespaceAndPath("desire", "media/cover_" + (coverCounter++));
            DynamicTexture texture = new DynamicTexture(id::toString, image);
            texture.upload();
            Minecraft.getInstance().getTextureManager().register(id, texture);
            coverId = id;
        } catch (IOException | RuntimeException ignored) {
            coverId = null;
        }
    }

    private void releaseCover() {
        if (coverId != null) {
            Minecraft.getInstance().getTextureManager().release(coverId);
            coverId = null;
        }
    }

    public static String formatTime(long millis) {
        long seconds = Math.max(0L, millis / 1000L);
        return String.format("%d:%02d", seconds / 60L, seconds % 60L);
    }
}
