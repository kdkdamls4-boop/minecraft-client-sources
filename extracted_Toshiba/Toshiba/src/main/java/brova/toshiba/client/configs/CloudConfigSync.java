package brova.toshiba.client.configs;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;

public final class CloudConfigSync {

    private static volatile String lastAppliedName = "";

    private final AtomicBoolean saving = new AtomicBoolean();

    private volatile String saveStatus = "";

    public static String getLastAppliedName() {
        return lastAppliedName;
    }

    public String getSaveStatus() {
        return this.saveStatus;
    }

    public boolean isSaving() {
        return this.saving.get();
    }

    public void saveCurrent(String text) {
        if (text == null || !text.matches("[\\p{L}\\p{N}_-]{1,64}")) {
            this.saveStatus = "Название: до 64 букв, цифр, _ или -.";
            return;
        }

        if (this.saving.compareAndSet(false, true)) {
            try {
                this.saveStatus = "Сохранение...";
                String content = ConfigManager.toJson();
                if (content.getBytes(StandardCharsets.UTF_8).length > 5242880) {
                    this.saveStatus = "Конфиг превышает 5 МБ.";
                    return;
                }

                Path configDir = Path.of("toshiba", "configs");
                if (!Files.exists(configDir)) {
                    Files.createDirectories(configDir);
                }

                Path targetFile = configDir.resolve(text + ".json");
                Files.writeString(targetFile, content, StandardCharsets.UTF_8);
                lastAppliedName = text;
                this.saveStatus = "Конфиг сохранён локально: " + text;
            } catch (IOException e) {
                this.saveStatus = "Ошибка сохранения: " + e.getMessage();
            } catch (Exception e) {
                this.saveStatus = "Не удалось прочитать настройки клиента.";
            } finally {
                this.saving.set(false);
            }
        }
    }

    public void start() {
        try {
            Path configDir = Path.of("toshiba", "configs");
            if (!Files.exists(configDir)) {
                Files.createDirectories(configDir);
            }
            this.saveStatus = "Локальное хранилище готово.";
        } catch (Exception ignored) {
        }
    }

    public void stop() {
    }
}
