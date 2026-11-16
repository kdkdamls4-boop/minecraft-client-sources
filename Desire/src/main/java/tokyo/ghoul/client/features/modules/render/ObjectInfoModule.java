package tokyo.ghoul.client.features.modules.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import lombok.Getter;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import tokyo.ghoul.api.event.EventListener;
import tokyo.ghoul.api.event.Listener;
import tokyo.ghoul.api.event.events.client.PacketEvent;
import tokyo.ghoul.api.event.events.render.Render2DEvent;
import tokyo.ghoul.api.event.events.render.Render3DEvent;
import tokyo.ghoul.api.module.Category;
import tokyo.ghoul.api.module.Module;
import tokyo.ghoul.api.module.ModuleRegister;
import tokyo.ghoul.api.module.setting.BooleanSetting;
import tokyo.ghoul.api.module.setting.ModeSetting;
import tokyo.ghoul.api.module.setting.SliderSetting;
import tokyo.ghoul.api.system.files.FileUtil;
import tokyo.ghoul.api.utils.color.UIColors;
import tokyo.ghoul.api.utils.math.ProjectionUtil;
import tokyo.ghoul.api.utils.math.TimerUtil;
import tokyo.ghoul.api.utils.render.KawaseBlurProgram;
import tokyo.ghoul.api.utils.render.RenderUtil;
import tokyo.ghoul.api.utils.render.fonts.Font;
import tokyo.ghoul.api.utils.render.fonts.Fonts;
import tokyo.ghoul.api.utils.render.pipeline.EvaLayers;
import tokyo.ghoul.client.ui.widget.overlay.NotifyWidget;

import java.awt.Color;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Getter
@ModuleRegister(name = "Object Info", description = "Информация об объектах", category = Category.RENDER)
public class ObjectInfoModule extends Module {
    @Getter private static final ObjectInfoModule instance = new ObjectInfoModule();

    private final ModeSetting mode = new ModeSetting("Режим").values("FunTime", "HolyWorld").value("FunTime");
    private final BooleanSetting dynamiteNotifications = new BooleanSetting("Уведомления о динамите")
            .value(false)
            .setVisible(() -> mode.is("HolyWorld"));

    private final BooleanSetting glow = new BooleanSetting("Свечение обводки").value(true)
            .setVisible(() -> mode.is("FunTime"));
    private final SliderSetting glowSize = new SliderSetting("Размер свечения").value(1.0F).range(0.5F, 2.5F).step(0.1F)
            .setVisible(() -> mode.is("HolyWorld") || glow.getValue());
    private final SliderSetting glowStrength = new SliderSetting("Яркость свечения").value(1.0F).range(0.2F, 1.5F).step(0.05F)
            .setVisible(() -> mode.is("HolyWorld") || glow.getValue());

    private static final float GLOW_BASE_SIZE = 0.25F;
    private static final float BOX_GLOW_ALPHA = 0.3F;
    private static final int[][] BOX_EDGES = {
            {0, 1}, {1, 2}, {2, 3}, {3, 0},
            {4, 5}, {5, 6}, {6, 7}, {7, 4},
            {0, 4}, {1, 5}, {2, 6}, {3, 7}
    };

    private final Map<BlockPos, Info> infos = new ConcurrentHashMap<>();

    private volatile boolean stunActive = false;
    private final TimerUtil stunTimer = new TimerUtil();
    private static final long STUN_DURATION = 15000L;
    private static final long PARTICLE_CAPTURE_WINDOW = 2000L;
    private final List<Vec3> stunParticles = new CopyOnWriteArrayList<>();

    private volatile boolean explosiveActive = false;
    private final TimerUtil explosiveTimer = new TimerUtil();
    private static final long EXPLOSIVE_DURATION = 10000L;
    private final List<Vec3> explosiveParticles = new CopyOnWriteArrayList<>();

    private static final long SHATTER_DURATION = 2000L;
    private static final float GRAVITY = 14.0f;
    private static final float SPREAD_FORCE = 5.0f;

    public ObjectInfoModule() {
        addSettings(mode, dynamiteNotifications, glow, glowSize, glowStrength);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        clearAll();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        clearAll();
    }

    private void clearAll() {
        infos.clear();
        stunActive = false;
        stunParticles.clear();
        explosiveActive = false;
        explosiveParticles.clear();
    }

    @Override
    public void onEvent() {
        EventListener packet = PacketEvent.getInstance().subscribe(new Listener<>(this::onPacket));
        EventListener render2D = Render2DEvent.getInstance().subscribe(new Listener<>(2, this::onRender2D));
        EventListener render3D = Render3DEvent.getInstance().subscribe(new Listener<>(this::onRender3D));
        addEvents(packet, render2D, render3D);
    }

    private void onPacket(PacketEvent.PacketEventData event) {
        if (!event.isReceive()) return;
        Packet<?> packet = event.packet();

        if (mode.is("HolyWorld")) {
            if (packet instanceof ClientboundSoundPacket sound) {
                String soundName = sound.getSound().value().location().toString();
                if (soundName.contains("minecraft:block.beacon.deactivate")
                        || soundName.contains("minecraft:block.beacon.power_select")
                        || soundName.contains("minecraft:block.glass.break")) {
                    stunActive = true;
                    stunTimer.reset();
                    stunParticles.clear();
                }
                if (soundName.contains("minecraft:entity.generic.explode")
                        || soundName.contains("minecraft:entity.tnt.primed")) {
                    explosiveActive = true;
                    explosiveTimer.reset();
                    explosiveParticles.clear();
                }
                if (soundName.contains("minecraft:entity.tnt.primed")) {
                    notifyDynamite(sound.getX(), sound.getY(), sound.getZ());
                }
            }
            if (packet instanceof ClientboundLevelParticlesPacket particlePacket) {
                Vec3 particlePos = new Vec3(particlePacket.getX(), particlePacket.getY(), particlePacket.getZ());
                if (stunActive && !stunTimer.finished(PARTICLE_CAPTURE_WINDOW)) {
                    stunParticles.add(particlePos);
                }
                if (explosiveActive && !explosiveTimer.finished(PARTICLE_CAPTURE_WINDOW)) {
                    explosiveParticles.add(particlePos);
                }
            }
            return;
        }

        if (!(packet instanceof ClientboundSoundPacket sound)) return;

        String soundName = sound.getSound().value().location().toString();
        BlockPos pos = BlockPos.containing(sound.getX(), sound.getY(), sound.getZ());
        float volume = sound.getVolume();
        float pitch = sound.getPitch();

        if (soundName.contains("minecraft:block.piston.extend") || soundName.contains("minecraft:block.piston.contract")) {
            if ((volume == 0.5f || volume == 0.7f) && pitch == 0.5f) {
                infos.put(pos, new Info(pos.above(), ObjType.TRAP));
            }
        }

        if (soundName.contains("minecraft:block.anvil.place")) {
            if ((volume == 0.5f || volume == 0.7f) && (pitch == 1.1f || pitch == 0.5f)) {
                infos.put(pos, new Info(pos.above(), ObjType.PLAST));
            }
        }

        if (soundName.contains("minecraft:entity.generic.explode") && (volume == 1.0f || pitch == 1.0f)) {
            infos.put(pos, new Info(pos, ObjType.STAN));
            infos.put(pos.above(), new Info(pos.above(), ObjType.BOOM_TRAP));
        }

        if (soundName.contains("minecraft:block.beacon.deactivate") && (volume == 1.5f || pitch == 1.0f)) {
            infos.put(pos, new Info(pos, ObjType.STAN));
        }
    }

    private void notifyDynamite(double x, double y, double z) {
        if (!dynamiteNotifications.getValue() || mc.player == null) return;
        int distance = Math.max(1, Math.round((float) mc.player.position().distanceTo(new Vec3(x, y, z))));
        String msg = "Обнаружен динамит - " + distance + "м";
        print(msg);
        NotifyWidget.text(msg);
    }

    private void onRender2D(Render2DEvent.Render2DEventData event) {
        if (mc.player == null || mc.level == null) return;

        if (mode.is("HolyWorld")) {
            renderHolyWorldHud(event);
        }

        Iterator<Map.Entry<BlockPos, Info>> iterator = infos.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<BlockPos, Info> entry = iterator.next();
            Info info = entry.getValue();
            if (!mode.is("HolyWorld")) {
                Vec3 center = new Vec3(info.pos.getX() + 0.5, info.pos.getY() + 0.5, info.pos.getZ() + 0.5);
                drawBadge(event, center, info.type.name, info.type.icon, UIColors.primary(), 1.0f, info.start, info.type.time);
            }
            if (info.start.finished(info.type.time)) {
                iterator.remove();
            }
        }
    }

    private void renderHolyWorldHud(Render2DEvent.Render2DEventData event) {
        if (stunActive) {
            if (stunTimer.finished(STUN_DURATION)) {
                stunActive = false;
                stunParticles.clear();
            } else if (!stunParticles.isEmpty()) {
                Vec3 center = getParticleCenter(stunParticles);
                if (center != null) {
                    float shatter = getShatterProgress(stunTimer, STUN_DURATION);
                    float fadeAlpha = shatter > 0 ? 1.0f - shatter : 1.0f;
                    double fallY = shatter > 0 ? -0.5 * GRAVITY * shatter * shatter : 0;
                    drawBadge(event, center.add(0, fallY, 0), "Стан", Items.NETHER_STAR, UIColors.primary(), fadeAlpha, stunTimer, STUN_DURATION);
                }
            }
        }

        if (explosiveActive) {
            if (explosiveTimer.finished(EXPLOSIVE_DURATION)) {
                explosiveActive = false;
                explosiveParticles.clear();
            } else if (!explosiveParticles.isEmpty()) {
                Vec3 center = getParticleCenter(explosiveParticles);
                if (center != null) {
                    float shatter = getShatterProgress(explosiveTimer, EXPLOSIVE_DURATION);
                    float fadeAlpha = shatter > 0 ? 1.0f - shatter : 1.0f;
                    double fallY = shatter > 0 ? -0.5 * GRAVITY * shatter * shatter : 0;
                    drawBadge(event, center.add(0, fallY, 0), "Взрывная трапка", Items.FIRE_CHARGE, UIColors.primary(), fadeAlpha, explosiveTimer, EXPLOSIVE_DURATION);
                }
            }
        }
    }

    private void drawBadge(Render2DEvent.Render2DEventData event, Vec3 worldPos, String label, Item icon,
                           Color accentColor, float fadeAlpha, TimerUtil timer, long duration) {
        if (mc.player == null) return;

        Vector2f screenPos = ProjectionUtil.projectToScreen(worldPos);
        if (screenPos == null) return;

        float elapsed = timer.getElapsedTime();
        float secondsRemaining = Math.max(0.05f, (duration - elapsed) / 1000.0f);
        String timeText = String.format(java.util.Locale.US, "%.1fс", secondsRemaining);

        Font font = Fonts.SF_SEMIBOLD;
        float fontSize = 9.0f;
        float labelWidth = font.getWidth(label, fontSize);
        float timeWidth = font.getWidth(timeText, fontSize);
        float iconSize = 14.0f;
        float padX = 4.0f;
        float iconGap = 4.5f;
        float timeGap = 5.0f;
        float boxHeight = 18.0f;
        float timeBadgePadX = 3.5f;
        float timeBadgeHeight = 11.5f;
        float timeBadgeWidth = timeWidth + timeBadgePadX * 2.0f;
        float boxWidth = padX + iconSize + iconGap + labelWidth + timeGap + timeBadgeWidth + padX;

        float distance = (float) mc.player.position().distanceTo(worldPos);
        float scale = Mth.clamp(1.0f - distance / 80.0f, 0.65f, 1.0f);

        PoseStack ms = event.matrixStack();
        GuiGraphicsExtractor extractor = event.context();

        ms.pushPose();
        ms.translate(screenPos.x, screenPos.y, 0.0f);
        ms.scale(scale, scale, 1.0f);

        float x = -boxWidth / 2.0f;
        float y = -boxHeight / 2.0f;
        float radius = boxHeight / 2.0f;

        if (!KawaseBlurProgram.fbos.isEmpty()) {
            RenderUtil.BLUR_RECT.draw(ms, x, y, boxWidth, boxHeight, radius, new Color(255, 255, 255, (int) (255 * fadeAlpha)), 0f);
        }
        RenderUtil.RECT.draw(ms, x, y, boxWidth, boxHeight, radius, new Color(11, 12, 22, (int) (204 * fadeAlpha)));
        RenderUtil.RECT.drawOutline(ms, x, y, boxWidth, boxHeight, radius, 1.0f, new Color(154, 175, 255, (int) (22 * fadeAlpha)));

        float itemScale = iconSize / 16.0f;
        float itemX = x + padX;
        float itemY = y + (boxHeight - iconSize) / 2.0f;

        extractor.pose().pushMatrix();
        extractor.pose().translate(screenPos.x, screenPos.y);
        extractor.pose().scale(scale);
        extractor.pose().translate(itemX, itemY);
        extractor.pose().scale(itemScale);
        extractor.item(new ItemStack(icon), 0, 0);
        extractor.pose().popMatrix();

        float textY = y + (boxHeight - font.getHeight(fontSize)) / 2.0f;
        float labelX = itemX + iconSize + iconGap;
        font.drawText(ms, label, labelX, textY, fontSize, new Color(255, 255, 255, (int) (255 * fadeAlpha)));

        float timeBadgeX = x + boxWidth - padX - timeBadgeWidth;
        float timeBadgeY = y + (boxHeight - timeBadgeHeight) / 2.0f;
        float timeBadgeRadius = timeBadgeHeight / 2.0f;
        Color timeBadgeFill = new Color(accentColor.getRed(), accentColor.getGreen(), accentColor.getBlue(), (int) (38 * fadeAlpha));
        Color timeBadgeStroke = new Color(accentColor.getRed(), accentColor.getGreen(), accentColor.getBlue(), (int) (70 * fadeAlpha));
        RenderUtil.RECT.draw(ms, timeBadgeX, timeBadgeY, timeBadgeWidth, timeBadgeHeight, timeBadgeRadius, timeBadgeFill);
        RenderUtil.RECT.drawOutline(ms, timeBadgeX, timeBadgeY, timeBadgeWidth, timeBadgeHeight, timeBadgeRadius, 0.8f, timeBadgeStroke);
        font.drawText(ms, timeText, timeBadgeX + timeBadgePadX, textY, fontSize, new Color(accentColor.getRed(), accentColor.getGreen(), accentColor.getBlue(), (int) (255 * fadeAlpha)));

        ms.popPose();
    }

    private void onRender3D(Render3DEvent.Render3DEventData event) {
        if (mc.player == null || mc.level == null) return;
        var camera = event.cameraState();
        if (camera == null || !camera.initialized) return;

        if (mode.is("HolyWorld")) {
            renderHolyWorld3D(event);
        } else {
            renderFunTime3D(event);
        }
    }

    private void renderFunTime3D(Render3DEvent.Render3DEventData event) {
        if (infos.isEmpty()) return;
        var camera = event.cameraState();
        double cx = camera.pos.x;
        double cy = camera.pos.y;
        double cz = camera.pos.z;

        PoseStack matrices = event.matrixStack();

        event.collector().order(1000).submitCustomGeometry(matrices, EvaLayers.QUADS, (pose, buffer) -> {
            for (Info info : infos.values()) {
                if (info.start.finished(info.type.time)) continue;
                float x1 = (float) (info.pos.getX() - cx);
                float y1 = (float) (info.pos.getY() - cy);
                float z1 = (float) (info.pos.getZ() - cz);
                float x2 = x1 + 1.0f, y2 = y1 + 1.0f, z2 = z1 + 1.0f;

                int fill = ((info.type.color.getRGB() & 0x00FFFFFF) | (75 << 24));
                drawBoxQuads(buffer, pose.pose(), x1, y1, z1, x2, y2, z2, fill);
            }
        });

        event.collector().order(1000).submitCustomGeometry(matrices, EvaLayers.DEBUG_LINES, (pose, buffer) -> {
            for (Info info : infos.values()) {
                if (info.start.finished(info.type.time)) continue;
                float x1 = (float) (info.pos.getX() - cx);
                float y1 = (float) (info.pos.getY() - cy);
                float z1 = (float) (info.pos.getZ() - cz);
                float x2 = x1 + 1.0f, y2 = y1 + 1.0f, z2 = z1 + 1.0f;

                int line = ((info.type.color.getRGB() & 0x00FFFFFF) | (220 << 24));
                drawBoxLines(buffer, pose.pose(), x1, y1, z1, x2, y2, z2, line);
            }
        });

        if (!glow.getValue()) return;
        Vec3 cameraPos = new Vec3(cx, cy, cz);
        Identifier bloom = FileUtil.getImage("particles/glow");
        float size = GLOW_BASE_SIZE * glowSize.getValue();
        event.collector().order(1000).submitCustomGeometry(matrices, EvaLayers.textured(bloom), (pose, buffer) -> {
            for (Info info : infos.values()) {
                long remaining = info.type.time - info.start.getElapsedTime();
                if (remaining <= 0) continue;
                float fade = Mth.clamp(remaining / 1000.0F, 0.0F, 1.0F);
                float alpha = Math.min(1.0F, BOX_GLOW_ALPHA * glowStrength.getValue()) * fade;
                int x = info.pos.getX();
                int y = info.pos.getY();
                int z = info.pos.getZ();
                Vec3[] corners = {
                        new Vec3(x, y, z), new Vec3(x + 1, y, z), new Vec3(x + 1, y, z + 1), new Vec3(x, y, z + 1),
                        new Vec3(x, y + 1, z), new Vec3(x + 1, y + 1, z), new Vec3(x + 1, y + 1, z + 1), new Vec3(x, y + 1, z + 1)
                };
                drawEdges(pose.pose(), buffer, corners, BOX_EDGES, size, alpha, info.type.color.getRGB(), event, cameraPos);
            }
        });
    }

    private void renderHolyWorld3D(Render3DEvent.Render3DEventData event) {
        boolean hasStun = stunActive && !stunTimer.finished(STUN_DURATION) && !stunParticles.isEmpty();
        boolean hasExplosive = explosiveActive && !explosiveTimer.finished(EXPLOSIVE_DURATION) && !explosiveParticles.isEmpty();

        if (!hasStun && !hasExplosive) return;

        List<Vec3> stunSnapshot = hasStun ? new ArrayList<>(stunParticles) : Collections.emptyList();
        List<Vec3> explosiveSnapshot = hasExplosive ? new ArrayList<>(explosiveParticles) : Collections.emptyList();

        var camera = event.cameraState();
        Vec3 cameraPos = new Vec3(camera.pos.x, camera.pos.y, camera.pos.z);
        int themeColor = UIColors.primary().getRGB();
        Identifier bloom = FileUtil.getImage("particles/glow");
        PoseStack matrices = event.matrixStack();

        event.collector().order(1000).submitCustomGeometry(matrices, EvaLayers.textured(bloom), (pose, buffer) -> {
            if (hasStun && !stunSnapshot.isEmpty()) {
                float shatterProgress = getShatterProgress(stunTimer, STUN_DURATION);
                float alpha = shatterProgress > 0 ? 1.0f - shatterProgress : 1.0f;
                drawGlowBox(pose.pose(), buffer, stunSnapshot, themeColor, event, cameraPos, shatterProgress, alpha);
            }
            if (hasExplosive && !explosiveSnapshot.isEmpty()) {
                float shatterProgress = getShatterProgress(explosiveTimer, EXPLOSIVE_DURATION);
                float alpha = shatterProgress > 0 ? 1.0f - shatterProgress : 1.0f;
                drawGlowBox(pose.pose(), buffer, explosiveSnapshot, themeColor, event, cameraPos, shatterProgress, alpha);
            }
        });
    }

    private void drawGlowBox(Matrix4f poseMatrix, VertexConsumer buffer, List<Vec3> particles, int color,
                             Render3DEvent.Render3DEventData event, Vec3 cameraPos, float shatterProgress, float alphaMultiplier) {
        if (particles == null || particles.isEmpty()) return;

        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE, minZ = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE, maxZ = -Double.MAX_VALUE;
        boolean found = false;
        for (Vec3 p : particles) {
            if (p == null) continue;
            found = true;
            if (p.x < minX) minX = p.x; if (p.x > maxX) maxX = p.x;
            if (p.y < minY) minY = p.y; if (p.y > maxY) maxY = p.y;
            if (p.z < minZ) minZ = p.z; if (p.z > maxZ) maxZ = p.z;
        }
        if (!found) return;

        double pad = 0.5;
        minX -= pad; minY -= pad; minZ -= pad;
        maxX += pad; maxY += pad; maxZ += pad;

        Vec3 center = new Vec3((minX + maxX) / 2, (minY + maxY) / 2, (minZ + maxZ) / 2);

        Vec3[] corners = {
                new Vec3(minX, minY, minZ), new Vec3(maxX, minY, minZ),
                new Vec3(maxX, minY, maxZ), new Vec3(minX, minY, maxZ),
                new Vec3(minX, maxY, minZ), new Vec3(maxX, maxY, minZ),
                new Vec3(maxX, maxY, maxZ), new Vec3(minX, maxY, maxZ)
        };
        int[][] edges = {
                {0,1},{1,2},{2,3},{3,0},
                {4,5},{5,6},{6,7},{7,4},
                {0,4},{1,5},{2,6},{3,7}
        };

        float glowSizeScaled = GLOW_BASE_SIZE * glowSize.getValue();
        float alphaScaled = Math.min(1.0F, alphaMultiplier * glowStrength.getValue());
        if (shatterProgress <= 0) {
            drawEdges(poseMatrix, buffer, corners, edges, glowSizeScaled, alphaScaled, color, event, cameraPos);
        } else {
            float t = shatterProgress;
            Vec3[][] edgePairs = new Vec3[12][2];
            for (int i = 0; i < edges.length; i++) {
                Vec3 a = corners[edges[i][0]];
                Vec3 b = corners[edges[i][1]];
                Vec3 edgeCenter = a.add(b).scale(0.5);

                long seed = Double.doubleToLongBits(edgeCenter.x * 73 + edgeCenter.y * 37 + edgeCenter.z * 53 + i * 11);
                double rx = ((seed >> 16) & 0xFFF) / 2048.0 - 1.0;
                double rz = ((seed >> 4) & 0xFFF) / 2048.0 - 1.0;
                double ry = ((seed >> 8) & 0xFFF) / 4096.0;

                double dirX = (edgeCenter.x - center.x) + rx * 2.0;
                double dirZ = (edgeCenter.z - center.z) + rz * 2.0;
                double dirY = ry * 2.0;
                double len = Math.sqrt(dirX * dirX + dirZ * dirZ);
                if (len > 0.01) { dirX /= len; dirZ /= len; }

                double offX = dirX * SPREAD_FORCE * t;
                double offZ = dirZ * SPREAD_FORCE * t;
                double offY = dirY * t - 0.5 * GRAVITY * t * t;

                edgePairs[i][0] = a.add(offX, offY, offZ);
                edgePairs[i][1] = b.add(offX, offY, offZ);
            }

            for (Vec3[] edge : edgePairs) {
                Vec3 a = edge[0];
                Vec3 b = edge[1];
                double dist = a.distanceTo(b);
                int steps = Math.max(6, (int) (dist * 40));
                for (int j = 0; j <= steps; j++) {
                    float st = (float) j / steps;
                    Vec3 interpolated = a.add(b.subtract(a).scale(st));
                    drawGlow(poseMatrix, interpolated, buffer, glowSizeScaled, alphaScaled, color, event, cameraPos);
                }
            }
        }
    }

    private void drawEdges(Matrix4f poseMatrix, VertexConsumer buffer, Vec3[] corners, int[][] edges,
                           float glowSize, float alpha, int color, Render3DEvent.Render3DEventData event, Vec3 cameraPos) {
        for (int[] edge : edges) {
            Vec3 a = corners[edge[0]];
            Vec3 b = corners[edge[1]];
            double dist = a.distanceTo(b);
            int steps = Math.max(6, (int) (dist * 40));
            for (int j = 0; j <= steps; j++) {
                float t = (float) j / steps;
                Vec3 interpolated = a.add(b.subtract(a).scale(t));
                drawGlow(poseMatrix, interpolated, buffer, glowSize, alpha, color, event, cameraPos);
            }
        }
    }

    private void drawGlow(Matrix4f poseMatrix, Vec3 pos, VertexConsumer buffer, float size, float alpha,
                          int baseColor, Render3DEvent.Render3DEventData event, Vec3 cameraPos) {
        int glowAlpha = (int) (alpha * 255);
        if (glowAlpha <= 0) return;
        int glowColor = (glowAlpha << 24) | (baseColor & 0xFFFFFF);

        Matrix4f matrix = new Matrix4f(poseMatrix);
        matrix.translate((float) (pos.x - cameraPos.x), (float) (pos.y - cameraPos.y), (float) (pos.z - cameraPos.z));
        if (event.cameraState().orientation != null) {
            matrix.rotate(event.cameraState().orientation);
        }
        float half = size / 2.0f;
        buffer.addVertex(matrix, -half, -half, 0f).setUv(0f, 1f).setColor(glowColor);
        buffer.addVertex(matrix, half, -half, 0f).setUv(1f, 1f).setColor(glowColor);
        buffer.addVertex(matrix, half, half, 0f).setUv(1f, 0f).setColor(glowColor);
        buffer.addVertex(matrix, -half, half, 0f).setUv(0f, 0f).setColor(glowColor);
    }

    private static void drawBoxQuads(VertexConsumer buffer, Matrix4f matrix, float x1, float y1, float z1, float x2, float y2, float z2, int argb) {
        buffer.addVertex(matrix, x1, y1, z1).setColor(argb);
        buffer.addVertex(matrix, x2, y1, z1).setColor(argb);
        buffer.addVertex(matrix, x2, y1, z2).setColor(argb);
        buffer.addVertex(matrix, x1, y1, z2).setColor(argb);
        buffer.addVertex(matrix, x1, y2, z2).setColor(argb);
        buffer.addVertex(matrix, x2, y2, z2).setColor(argb);
        buffer.addVertex(matrix, x2, y2, z1).setColor(argb);
        buffer.addVertex(matrix, x1, y2, z1).setColor(argb);
        buffer.addVertex(matrix, x1, y1, z1).setColor(argb);
        buffer.addVertex(matrix, x1, y2, z1).setColor(argb);
        buffer.addVertex(matrix, x2, y2, z1).setColor(argb);
        buffer.addVertex(matrix, x2, y1, z1).setColor(argb);
        buffer.addVertex(matrix, x2, y1, z2).setColor(argb);
        buffer.addVertex(matrix, x2, y2, z2).setColor(argb);
        buffer.addVertex(matrix, x1, y2, z2).setColor(argb);
        buffer.addVertex(matrix, x1, y1, z2).setColor(argb);
        buffer.addVertex(matrix, x1, y1, z2).setColor(argb);
        buffer.addVertex(matrix, x1, y2, z2).setColor(argb);
        buffer.addVertex(matrix, x1, y2, z1).setColor(argb);
        buffer.addVertex(matrix, x1, y1, z1).setColor(argb);
        buffer.addVertex(matrix, x2, y1, z1).setColor(argb);
        buffer.addVertex(matrix, x2, y2, z1).setColor(argb);
        buffer.addVertex(matrix, x2, y2, z2).setColor(argb);
        buffer.addVertex(matrix, x2, y1, z2).setColor(argb);
    }

    private static void drawBoxLines(VertexConsumer buffer, Matrix4f matrix, float x1, float y1, float z1, float x2, float y2, float z2, int argb) {
        line(buffer, matrix, x1, y1, z1, x2, y1, z1, argb);
        line(buffer, matrix, x2, y1, z1, x2, y1, z2, argb);
        line(buffer, matrix, x2, y1, z2, x1, y1, z2, argb);
        line(buffer, matrix, x1, y1, z2, x1, y1, z1, argb);

        line(buffer, matrix, x1, y2, z1, x2, y2, z1, argb);
        line(buffer, matrix, x2, y2, z1, x2, y2, z2, argb);
        line(buffer, matrix, x2, y2, z2, x1, y2, z2, argb);
        line(buffer, matrix, x1, y2, z2, x1, y2, z1, argb);

        line(buffer, matrix, x1, y1, z1, x1, y2, z1, argb);
        line(buffer, matrix, x2, y1, z1, x2, y2, z1, argb);
        line(buffer, matrix, x2, y1, z2, x2, y2, z2, argb);
        line(buffer, matrix, x1, y1, z2, x1, y2, z2, argb);
    }

    private static void line(VertexConsumer buffer, Matrix4f matrix, float x1, float y1, float z1, float x2, float y2, float z2, int argb) {
        buffer.addVertex(matrix, x1, y1, z1).setColor(argb);
        buffer.addVertex(matrix, x2, y2, z2).setColor(argb);
    }

    private float getShatterProgress(TimerUtil timer, long duration) {
        long elapsed = timer.getElapsedTime();
        long shatterStart = duration - SHATTER_DURATION;
        if (elapsed < shatterStart) return 0f;
        return Mth.clamp((float) (elapsed - shatterStart) / SHATTER_DURATION, 0f, 1f);
    }

    private Vec3 getParticleCenter(List<Vec3> particles) {
        if (particles == null || particles.isEmpty()) return null;
        double sumX = 0, sumY = 0, sumZ = 0;
        int count = 0;
        for (Vec3 p : particles) {
            if (p == null) continue;
            sumX += p.x;
            sumY += p.y;
            sumZ += p.z;
            count++;
        }
        if (count == 0) return null;
        return new Vec3(sumX / count, sumY / count, sumZ / count);
    }

    public static class Info {
        public final BlockPos pos;
        public final ObjType type;
        public final TimerUtil start = new TimerUtil();

        public Info(BlockPos pos, ObjType type) {
            this.pos = pos;
            this.type = type;
        }
    }

    public enum ObjType {
        TRAP("Трапка", Items.PISTON, 15098L, new Color(255, 60, 60)),
        BOOM_TRAP("Взрывная трапка", Items.FIRE_CHARGE, 10927L, new Color(255, 140, 0)),
        STAN("Стан", Items.NETHER_STAR, 15098L, new Color(255, 255, 255)),
        PLAST("Пласт", Items.ANVIL, 20071L, new Color(255, 255, 0));

        public final String name;
        public final Item icon;
        public final long time;
        public final Color color;

        ObjType(String name, Item icon, long time, Color color) {
            this.name = name;
            this.icon = icon;
            this.time = time;
            this.color = color;
        }
    }
}
