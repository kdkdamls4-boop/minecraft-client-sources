package tokyo.ghoul.client.ui.widget.overlay;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemCooldowns;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector4f;
import tokyo.ghoul.api.utils.animation.AnimationUtil;
import tokyo.ghoul.api.utils.animation.Easing;
import tokyo.ghoul.api.utils.render.RenderUtil;
import tokyo.ghoul.api.utils.render.fonts.Fonts;
import tokyo.ghoul.client.ui.widget.Widget;

import java.awt.Color;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static tokyo.ghoul.client.ui.widget.RelicStyle.*;

public class CooldownsWidget extends Widget {
    private static final String TITLE = "Cooldowns";

    private final AnimationUtil globalAlpha = new AnimationUtil();
    private final Map<String, Entry> entries = new LinkedHashMap<>();

    public CooldownsWidget() {
        super(100f, 100f);
        enableHudSettingsMenu();
    }

    @Override
    public String getName() { return TITLE; }

    private static class Entry {
        final AnimationUtil anim = new AnimationUtil();
        final ItemStack stack;
        ItemCooldowns.CooldownInstance cooldown;

        Entry(ItemStack stack, ItemCooldowns.CooldownInstance cooldown) {
            this.stack = stack;
            this.cooldown = cooldown;
        }
    }

    private record Line(String name, Identifier texture, String time, float anim) {}

    @Override
    public void render(PoseStack ms) {
        if (mc.player == null) return;

        Map<String, Entry> current = collectCooldowns();

        current.forEach((name, entry) -> {
            Entry existing = entries.get(name);
            if (existing == null) {
                entry.anim.update();
                entry.anim.run(1f, 240, Easing.EXPO_OUT);
                entries.put(name, entry);
            } else {
                existing.cooldown = entry.cooldown;
                existing.anim.update();
                existing.anim.run(1f, 240, Easing.EXPO_OUT);
            }
        });
        entries.entrySet().removeIf(e -> {
            if (current.containsKey(e.getKey())) return false;
            AnimationUtil anim = e.getValue().anim;
            anim.update();
            anim.run(0f, 200, Easing.EXPO_OUT);
            return anim.getValue() < 0.01;
        });

        boolean hasVisible = entries.values().stream().anyMatch(e -> e.anim.getValue() > 0.01);
        boolean editing = getDraggable().isDragging() || mc.gui.screen() instanceof ChatScreen;
        globalAlpha.update();
        globalAlpha.run(hasVisible || editing ? 1f : 0f, 300, Easing.EXPO_OUT);
        float alpha = (float) globalAlpha.getValue();
        if (alpha <= 0.01f && !hasVisible) return;

        List<Line> lines = new ArrayList<>();
        for (Map.Entry<String, Entry> e : entries.entrySet()) {
            float anim = (float) e.getValue().anim.getValue();
            if (anim <= 0.01f) continue;
            lines.add(new Line(e.getKey(), itemTexture(e.getValue().stack), remaining(e.getValue()), anim));
        }
        if (lines.isEmpty() && editing) {
            lines.add(new Line("Ender Pearl", previewTexture("ender_pearl"), "0:26", 1f));
            lines.add(new Line("Golden Apple", previewTexture("golden_apple"), "0:24", 1f));
        }

        float posX = getDraggable().getX();
        float posY = getDraggable().getY();

        float h = u(128f), corner = u(40f), stroke = u(4f), gap = u(32f);
        float padX = u(39f), itemSize = u(48f), iconGap = u(45f);
        float nameValueGap = u(62f), sectionPad = u(34f);
        float nameSize = u(48f), valueSize = u(42f);

        var nameFont = Fonts.HUD_MEDIUM;
        var valueFont = Fonts.HUD_SEMIBOLD;

        float maxW = 0f;
        float y = posY;
        for (Line line : lines) {
            float rowA = line.anim() * alpha;

            float nameW = width(nameFont, line.name(), nameSize);
            float valueW = width(valueFont, line.time(), valueSize);
            float w = padX + itemSize + iconGap + nameW + nameValueGap + sectionPad + valueW + padX;
            maxW = Math.max(maxW, w);

            float rowY = y + (line.anim() - 1f) * u(14f);
            float cy = rowY + h / 2f;

            panel(ms, posX, rowY, w, h, corner, stroke, BORDER, FILL, rowA, liquidGlass());

            float sectionX = posX + w - padX - valueW - sectionPad;
            float r = Math.max(0f, corner - stroke);
            RenderUtil.RECT.draw(ms, sectionX, rowY + stroke, posX + w - stroke - sectionX, h - stroke * 2f,
                    new Vector4f(0f, r, 0f, r), a(new Color(255, 255, 255, 7), rowA));

            float itemX = posX + padX;
            float itemY = cy - itemSize / 2f;
            if (line.texture() != null) {
                RenderUtil.TEXTURE_RECT.draw(ms, itemX, itemY, itemSize, itemSize, 0f,
                        a(Color.WHITE, rowA), 0f, 0f, 1f, 1f, line.texture());
            } else {
                RenderUtil.RECT.draw(ms, itemX, itemY, itemSize, itemSize, u(8f),
                        a(new Color(50, 55, 65, 220), rowA));
            }

            text(ms, nameFont, line.name(), itemX + itemSize + iconGap, cy, nameSize, TEXT, rowA);
            text(ms, valueFont, line.time(), posX + w - padX - valueW, cy, valueSize, ACCENT, rowA);

            y += (h + gap) * line.anim();
        }

        getDraggable().setWidth(Math.max(maxW, u(300f)));
        getDraggable().setHeight(Math.max(y - posY - gap, h));
        renderHudSettingsMenu(ms);
    }

    private Map<String, Entry> collectCooldowns() {
        Map<String, Entry> data = new LinkedHashMap<>();
        ItemCooldowns manager = mc.player.getCooldowns();
        float tickDelta = mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);

        for (int i = 0; i < mc.player.getInventory().getContainerSize(); i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);
            if (stack.isEmpty() || !manager.isOnCooldown(stack)) continue;

            Identifier groupId = manager.getCooldownGroup(stack);
            ItemCooldowns.CooldownInstance instance = manager.cooldowns.get(groupId);
            if (instance == null) continue;

            int remaining = Math.max(0, instance.endTime() - (manager.tickCount + (int) tickDelta));
            if (remaining > 0) data.putIfAbsent(stack.getHoverName().getString(), new Entry(stack, instance));
        }
        return data;
    }

    private String remaining(Entry entry) {
        if (mc.player == null) return "0:00";
        int ticks = Math.max(0, entry.cooldown.endTime() - mc.player.getCooldowns().tickCount);
        int seconds = (int) Math.ceil(ticks / 20f);
        return String.format("%d:%02d", seconds / 60, seconds % 60);
    }

    private static Identifier previewTexture(String item) {
        return Identifier.fromNamespaceAndPath("minecraft", "textures/item/" + item + ".png");
    }

    private Identifier itemTexture(ItemStack stack) {
        var key = BuiltInRegistries.ITEM.getKey(stack.getItem());
        Identifier texture = Identifier.fromNamespaceAndPath(key.getNamespace(), "textures/item/" + key.getPath() + ".png");
        if (mc.getResourceManager().getResource(texture).isEmpty()) return null;
        return texture;
    }
}
