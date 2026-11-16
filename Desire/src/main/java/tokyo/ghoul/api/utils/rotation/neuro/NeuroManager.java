package tokyo.ghoul.api.utils.rotation.neuro;

import lombok.Getter;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import tokyo.ghoul.api.event.Listener;
import tokyo.ghoul.api.event.events.player.other.UpdateEvent;
import tokyo.ghoul.api.event.events.player.world.AttackEvent;
import tokyo.ghoul.api.system.backend.ClientInfo;
import tokyo.ghoul.api.system.interfaces.QuickImports;
import tokyo.ghoul.api.utils.rotation.neuro.NeuroRecording.Frame;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NeuroManager implements QuickImports {
    @Getter private static final NeuroManager instance = new NeuroManager();

    private static final double RECORD_RANGE = 8.0;
    private static final double PICK_FOV = 45.0;
    private static final double STICKY_FOV = 100.0;

    private final File root = new File(System.getProperty("user.dir"), ClientInfo.NAME + "/neuro");
    @Getter private final File recordsDir = new File(root, "records");
    @Getter private final File modelsDir = new File(root, "models");

    private final ExecutorService worker = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "desire-neuro");
        thread.setDaemon(true);
        return thread;
    });

    @Getter private volatile NeuroModel activeModel;
    @Getter private volatile boolean training;

    private String recordingName;
    private final List<Frame> frames = new ArrayList<>();
    private LivingEntity recordTarget;
    private boolean attackedThisTick;

    public void load() {
        recordsDir.mkdirs();
        modelsDir.mkdirs();
        UpdateEvent.getInstance().subscribe(new Listener<>(event -> onTick()));
        AttackEvent.getInstance().subscribe(new Listener<>(event -> {
            if (recordingName != null) attackedThisTick = true;
        }));
    }


    public boolean isRecording() {
        return recordingName != null;
    }

    public void startRecording(String name) {
        if (isRecording()) {
            print("Уже идёт запись §e" + recordingName + "§r, сначала §e.neuro stop");
            return;
        }
        recordingName = sanitize(name);
        frames.clear();
        recordTarget = null;
        attackedThisTick = false;
        print("Запись §e" + recordingName + "§r начата. Дерись сам, Aura выключи — пишется только ручной аим по цели");
    }

    public void stopRecording() {
        if (!isRecording()) {
            print("Запись не идёт");
            return;
        }
        String name = recordingName;
        List<Frame> copy = new ArrayList<>(frames);
        recordingName = null;
        frames.clear();
        recordTarget = null;

        long withTarget = copy.stream().filter(Frame::hasTarget).count();
        long attacks = copy.stream().filter(Frame::attack).count();
        File file = new File(recordsDir, name + ".csv");
        worker.execute(() -> {
            try {
                NeuroRecording.write(file, copy);
                print(String.format(Locale.ROOT, "Запись §e%s§r сохранена: %d тиков, с целью %d (%.1f с), ударов %d",
                        name, copy.size(), withTarget, withTarget / 20.0, attacks));
            } catch (Exception e) {
                print("Не удалось сохранить запись: " + e.getMessage());
            }
        });
    }

    private void onTick() {
        if (!isRecording()) return;
        if (mc.player == null || mc.level == null) {
            print("Мир выгружен — запись остановлена");
            stopRecording();
            return;
        }

        boolean attack = attackedThisTick;
        attackedThisTick = false;

        if (mc.gui.screen() != null) return;

        recordTarget = pickTarget();
        Vec3 eye = mc.player.getEyePosition();
        if (recordTarget != null) {
            AABB box = recordTarget.getBoundingBox();
            Vec3 center = box.getCenter();
            frames.add(new Frame(mc.player.getYRot(), mc.player.getXRot(), eye.x, eye.y, eye.z,
                    true, center.x, center.y, center.z,
                    (float) (box.getXsize() / 2.0), (float) box.getYsize(), attack));
        } else {
            frames.add(new Frame(mc.player.getYRot(), mc.player.getXRot(), eye.x, eye.y, eye.z,
                    false, 0, 0, 0, 0, 0, attack));
        }
    }

    private LivingEntity pickTarget() {
        if (recordTarget != null && isRecordable(recordTarget) && angleToCrosshair(recordTarget) <= STICKY_FOV) {
            return recordTarget;
        }
        LivingEntity best = null;
        double bestAngle = PICK_FOV;
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity instanceof LivingEntity living && isRecordable(living)) {
                double angle = angleToCrosshair(living);
                if (angle < bestAngle) {
                    bestAngle = angle;
                    best = living;
                }
            }
        }
        return best;
    }

    private boolean isRecordable(LivingEntity entity) {
        return entity != mc.player && entity.isAlive() && !(entity instanceof ArmorStand)
                && mc.player.getEyePosition().distanceTo(entity.getBoundingBox().getCenter()) <= RECORD_RANGE;
    }

    private double angleToCrosshair(LivingEntity entity) {
        Vec3 look = mc.player.getViewVector(1.0f).normalize();
        Vec3 to = entity.getBoundingBox().getCenter().subtract(mc.player.getEyePosition()).normalize();
        return Math.toDegrees(Math.acos(Mth.clamp(look.dot(to), -1.0, 1.0)));
    }


    public void train(String name) {
        if (training) {
            print("Обучение уже идёт");
            return;
        }
        List<File> sources = new ArrayList<>();
        if (name == null) {
            sources.addAll(Arrays.asList(listFiles(recordsDir, ".csv")));
        } else {
            File file = new File(recordsDir, sanitize(name) + ".csv");
            if (file.isFile()) sources.add(file);
        }
        if (sources.isEmpty()) {
            print(name == null ? "Записей нет — сначала §e.neuro record <название>" : "Запись §e" + name + "§r не найдена");
            return;
        }

        String modelName = name == null ? "all" : sanitize(name);
        File output = new File(modelsDir, modelName + ".onnx");
        training = true;
        print("Обучение §e" + modelName + "§r на " + sources.size() + " записях...");

        worker.execute(() -> {
            try {
                List<List<Frame>> recordings = new ArrayList<>();
                for (File source : sources) recordings.add(NeuroRecording.read(source));

                long started = System.currentTimeMillis();
                NeuroTrainer.Result result = NeuroTrainer.train(recordings, output, line -> print("§7" + line));
                print(String.format(Locale.ROOT,
                        "Модель §e%s§r готова за %.1f с: %d сэмплов, %d эпох, val NLL %.3f, ошибка yaw %.2f°, интервалов ударов %d",
                        modelName, (System.currentTimeMillis() - started) / 1000.0, result.samples(), result.epochs(),
                        result.valLoss(), result.valMae(), result.attackIntervals()));
                print(String.format(Locale.ROOT, "§7Симуляция: ошибка прицела %.1f°, в хитбоксе %.0f%%, сверка ONNX %.1e",
                        result.simError(), result.simOnTarget() * 100, result.exportError()));
                if (result.attackIntervals() == 0) {
                    print("§7В записи нет ударов по цели — Aura будет бить по кулдауну");
                }
                print("Включить: §e.neuro play " + modelName);
            } catch (Exception e) {
                print("Обучение не удалось: " + e.getMessage());
            } finally {
                training = false;
            }
        });
    }


    public void play(String name) {
        File file = new File(modelsDir, sanitize(name) + ".onnx");
        if (!file.isFile()) {
            print("Модель §e" + name + "§r не найдена, список — §e.neuro list");
            return;
        }
        worker.execute(() -> {
            try {
                NeuroModel model = NeuroModel.load(file);
                mc.execute(() -> {
                    NeuroModel previous = activeModel;
                    activeModel = model;
                    if (previous != null) previous.close();
                    tokyo.ghoul.client.features.modules.combat.AuraModule.getInstance().getAimMode().setValue("Neuro");
                    print("Нейро-ротация §e" + model.getName() + "§r активна, Aim mode → Neuro");
                });
            } catch (Throwable e) {
                print("Не удалось загрузить модель: " + e.getMessage());
            }
        });
    }

    public void list() {
        File[] records = listFiles(recordsDir, ".csv");
        File[] models = listFiles(modelsDir, ".onnx");
        print("Записи (" + records.length + "):");
        for (File file : records) {
            print(String.format(Locale.ROOT, " §e%s§r — %.1f КБ", stripExtension(file), file.length() / 1024.0));
        }
        NeuroModel active = activeModel;
        print("Модели (" + models.length + "):");
        for (File file : models) {
            String model = stripExtension(file);
            boolean current = active != null && active.getName().equals(model);
            print(" §e" + model + "§r" + (current ? " §a(активна)" : ""));
        }
        if (isRecording()) {
            print("Идёт запись §e" + recordingName + "§r: " + frames.size() + " тиков");
        }
    }

    public List<String> recordNames() {
        return Arrays.stream(listFiles(recordsDir, ".csv")).map(NeuroManager::stripExtension).toList();
    }

    public List<String> modelNames() {
        return Arrays.stream(listFiles(modelsDir, ".onnx")).map(NeuroManager::stripExtension).toList();
    }

    private static File[] listFiles(File dir, String extension) {
        File[] files = dir.listFiles((d, n) -> n.endsWith(extension));
        if (files == null) return new File[0];
        Arrays.sort(files, Comparator.comparing(File::getName));
        return files;
    }

    private static String stripExtension(File file) {
        String name = file.getName();
        int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(0, dot) : name;
    }

    private static String sanitize(String name) {
        return name.replaceAll("[^A-Za-z0-9_\\-а-яА-ЯёЁ]", "_");
    }
}
