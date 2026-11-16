package tokyo.ghoul.client.features.modules.render.nametags;

import com.mojang.blaze3d.vertex.PoseStack;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector2f;
import tokyo.ghoul.api.event.events.render.Render2DEvent;
import tokyo.ghoul.api.system.configs.FriendManager;
import tokyo.ghoul.api.system.interfaces.QuickImports;
import tokyo.ghoul.api.utils.color.UIColors;
import tokyo.ghoul.api.utils.math.ProjectionUtil;
import tokyo.ghoul.api.utils.render.RenderUtil;
import tokyo.ghoul.api.utils.render.fonts.Font;
import tokyo.ghoul.api.utils.render.fonts.Fonts;
import tokyo.ghoul.client.ui.widget.HudPanelStyle;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public class NameTagsRender implements QuickImports {
    private static final float ITEM_TEXT_SIZE = 11.0F;
    private static final Color FRIEND_GREEN = new Color(64, 255, 64);
    private static final Color OVER_LEVEL = new Color(212, 78, 74);
    private static final Color RECT_BG = new Color(18, 18, 20, 215);
    private static final EquipmentSlot[] ARMOR_ORDER = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };
    private static final EquipmentSlot[] DISPLAY_ORDER = {
            EquipmentSlot.OFFHAND, EquipmentSlot.HEAD, EquipmentSlot.CHEST,
            EquipmentSlot.LEGS, EquipmentSlot.FEET, EquipmentSlot.MAINHAND
    };

    private static final long FADE_OUT_MS = 1000L;
    private static final float LIVING_RAISE = 0.25F;
    private static final float LIVING_PILL_LIFT = 26.0F;
    private static final float ITEM_RAISE_FACTOR = 1.2F;

    private final NameTagsModule module;
    private final Map<Integer, TagState> states = new HashMap<>();
    private ClientLevel stateLevel;

    public NameTagsRender(NameTagsModule module) {
        this.module = module;
    }

    public void onRender(Render2DEvent.Render2DEventData event) {
        if (mc.level == null || mc.player == null) {
            states.clear();
            stateLevel = null;
            return;
        }
        if (stateLevel != mc.level) {
            states.clear();
            stateLevel = mc.level;
        }

        float tickDelta = event.partialTicks();
        long now = System.currentTimeMillis();
        float gs = (float) mc.getWindow().getGuiScale();

        for (TagState state : states.values()) {
            state.targeted = false;
        }
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (!isEntityTarget(entity)) continue;
            TagState state = states.get(entity.getId());
            if (state == null || state.entity != entity) {
                state = new TagState(entity);
                states.put(entity.getId(), state);
            }
            state.targeted = true;
        }

        Vec3 eye = mc.player.getEyePosition(tickDelta);
        List<TagState> drawList = new ArrayList<>();
        Iterator<TagState> iterator = states.values().iterator();
        while (iterator.hasNext()) {
            TagState state = iterator.next();
            boolean visible = state.targeted && locate(state, tickDelta, gs, true);
            state.update(visible, now);

            if (!visible && state.alpha <= 0.001F && !state.shown) {
                iterator.remove();
                continue;
            }
            // затухающий ник держится на последней известной позиции
            if (!visible && state.alpha > 0.001F && !locate(state, tickDelta, gs, false)) continue;
            if (state.alpha <= 0.001F) continue;

            double dx = state.worldX - eye.x;
            double dy = state.worldY - eye.y;
            double dz = state.worldZ - eye.z;
            state.distanceSq = dx * dx + dy * dy + dz * dz;
            drawList.add(state);
        }

        // дальние рисуются первыми, чтобы ближние перекрывали их
        drawList.sort(Comparator.comparingDouble((TagState s) -> s.distanceSq).reversed());
        for (TagState state : drawList) {
            if (state.entity instanceof ItemEntity item) {
                renderItemTag(event, state, item);
            } else if (state.entity instanceof LivingEntity living) {
                renderEntityTag(event, state, living);
            }
        }
    }

    /**
     * Обновляет мировую точку привязки и экранную позицию (в пикселях).
     * Для живых привязка — над головой, для предметов — над ними; fresh=false использует сохранённую точку.
     */
    private boolean locate(TagState state, float tickDelta, float gs, boolean fresh) {
        Entity entity = state.entity;
        if (fresh) {
            double x = Mth.lerp(tickDelta, entity.xOld, entity.getX());
            double y = Mth.lerp(tickDelta, entity.yOld, entity.getY());
            double z = Mth.lerp(tickDelta, entity.zOld, entity.getZ());
            boolean isItem = entity instanceof ItemEntity;
            state.worldX = x;
            state.worldY = y + entity.getBbHeight() * (isItem ? ITEM_RAISE_FACTOR : 1.0F) + (isItem ? 0.0F : LIVING_RAISE);
            state.worldZ = z;
            state.hasWorld = true;
        }
        if (!state.hasWorld) return false;

        Vector2f point = ProjectionUtil.project(state.worldX, state.worldY, state.worldZ);
        if (point.x == Float.MAX_VALUE || point.y == Float.MAX_VALUE) return false;

        float margin = 100.0F / gs;
        if (point.x < -margin || point.x > mc.getWindow().getGuiScaledWidth() + margin
                || point.y < -margin || point.y > mc.getWindow().getGuiScaledHeight() + margin) {
            return false;
        }
        state.screenX = point.x * gs;
        state.screenY = point.y * gs;
        return true;
    }

    private boolean isEntityTarget(Entity entity) {
        if (entity instanceof ItemEntity) {
            return module.rendering.isEnabled("Предметы");
        }
        if (!(entity instanceof LivingEntity living) || !living.isAlive()) {
            return false;
        }
        if (entity instanceof Player player) {
            if (entity == mc.player) {
                return module.rendering.isEnabled("Себя") && !mc.options.getCameraType().isFirstPerson();
            }
            boolean isFriend = FriendManager.getInstance().contains(player.getGameProfile().name());
            if (isFriend) {
                return module.rendering.isEnabled("Друзей");
            }
            if (isNaked(player)) {
                return module.rendering.isEnabled("Голых");
            }
            return module.rendering.isEnabled("Игроков");
        }
        if (entity instanceof Villager) {
            return module.rendering.isEnabled("Жителей");
        }
        if (entity instanceof Animal) {
            return module.rendering.isEnabled("Животных");
        }
        return entity instanceof Mob && module.rendering.isEnabled("Монстров");
    }

    private static boolean isNaked(Player player) {
        for (EquipmentSlot slot : ARMOR_ORDER) {
            if (!player.getItemBySlot(slot).isEmpty()) return false;
        }
        return true;
    }

    private void renderItemTag(Render2DEvent.Render2DEventData event, TagState state, ItemEntity item) {
        float alpha = state.alpha;
        float gs = (float) mc.getWindow().getGuiScale();
        ItemStack stack = item.getItem();
        Font font = Fonts.DIMASIK_MEDIUM;
        float fontSize = 12.0F;

        MutableComponent label = stack.getHoverName().copy();
        int count = stack.getCount();
        if (count > 1) {
            label.append(Component.literal(" / ").setStyle(Style.EMPTY.withColor(ChatFormatting.DARK_GRAY)));
            label.append(Component.literal(String.valueOf(count)).setStyle(Style.EMPTY.withColor(ChatFormatting.RED)));
        }

        float width = componentWidth(font, label, fontSize) + 10.0F;
        float height = 22.0F;
        float x = state.screenX - width * 0.5F;
        float y = state.screenY - 11.0F;

        PoseStack ms = event.matrixStack();
        ms.pushPose();
        ms.scale(1.0F / gs, 1.0F / gs, 1.0F);
        HudPanelStyle.surface(ms, x, y, width, height, 4.0F, 12.0F, alpha, module.liquidGlass.getValue());
        drawComponent(font, ms, label, x + 5.0F, y + 3.0F, fontSize, alpha);
        ms.popPose();
    }

    private static Color rarityColor(ItemStack stack) {
        ChatFormatting formatting = stack.getRarity().color();
        if (formatting == null) return Color.WHITE;
        TextColor color = TextColor.fromLegacyFormat(formatting);
        return color != null ? new Color(0xFF000000 | color.getValue(), true) : Color.WHITE;
    }

    private void renderEntityTag(Render2DEvent.Render2DEventData event, TagState state, LivingEntity entity) {
        float alpha = state.alpha;
        float gs = (float) mc.getWindow().getGuiScale();

        // раскладка оригинала считается в пикселях экрана
        float centerX = state.screenX;
        float centerY = state.screenY - LIVING_PILL_LIFT;

        boolean isFriend = entity instanceof Player p
                && FriendManager.getInstance().contains(p.getGameProfile().name());
        boolean isPlayer = entity instanceof Player;

        Font text = Fonts.DIMASIK_MEDIUM;
        Font icons = Fonts.DIMASIK_ICONS;
        float fontSize = 12.0F;
        float iconSize = 13.0F;
        float healthIconSize = 14.0F;
        float headSize = 18.0F;

        Component name = entity.getDisplayName();
        String hpText = String.valueOf(Math.max(0, Math.round(entity.getHealth() + entity.getAbsorptionAmount())));
        String pingText = pingOf(entity);

        float nameWidth = componentWidth(text, name, fontSize);
        float hpWidth = text.getWidth(hpText, fontSize);
        float pingWidth = pingText.isEmpty() ? 0.0F : text.getWidth(pingText, fontSize);
        float heartWidth = icons.getWidth("E", iconSize);
        float pingIconWidth = icons.getWidth("|", iconSize);

        float width = 5.0F + headSize + 6.0F + nameWidth + 8.0F + heartWidth + 3.0F + hpWidth + 5.0F;
        if (!pingText.isEmpty()) {
            width += 8.0F + pingIconWidth + 6.0F + pingWidth;
        }
        float height = 26.0F;
        float x = centerX - width * 0.5F;
        float y = centerY - height * 0.5F;

        Color accent = withAlpha(isFriend ? FRIEND_GREEN : UIColors.primary(), alpha);
        Color textColor = withAlpha(UIColors.textColor(), alpha);

        PoseStack ms = event.matrixStack();
        ms.pushPose();
        ms.scale(1.0F / gs, 1.0F / gs, 1.0F);

        HudPanelStyle.surface(ms, x, y, width, height, 7.0F, 12.0F, alpha, module.liquidGlass.getValue());
        if (isFriend) {
            RenderUtil.RECT.draw(ms, x, y, width, height, 7.0F, withAlpha(FRIEND_GREEN, alpha * 0.22F));
        }

        float cursor = x + 5.0F;
        if (isPlayer) {
            RenderUtil.TEXTURE_RECT.drawHead(ms, (Player) entity, cursor, y + 4.0F, headSize, headSize, 0.0F, 5.0F,
                    withAlpha(Color.WHITE, alpha));
        } else {
            icons.drawText(ms, "N", cursor + 3.5F, y + 7.5F, iconSize, accent);
        }
        cursor += headSize + 6.0F;

        drawComponent(text, ms, name, cursor, y + 6.0F, fontSize, alpha);
        cursor += nameWidth + 8.0F;

        icons.drawText(ms, "E", cursor, y + 5.0F, healthIconSize, accent);
        cursor += heartWidth + 3.0F;
        text.drawText(ms, hpText, cursor, y + 6.0F, fontSize, textColor);
        cursor += hpWidth;

        if (!pingText.isEmpty()) {
            cursor += 8.0F;
            icons.drawText(ms, "Q", cursor, y + 6.0F, iconSize, accent);
            cursor += pingIconWidth + 6.0F;
            text.drawText(ms, pingText, cursor, y + 6.0F, fontSize, textColor);
        }

        if (module.viewArmor.getValue()) {
            drawArmorRow(event, ms, entity, centerX, y, alpha);
        }
        ms.popPose();

        float guiCenterX = centerX / gs;
        float effectsBaseY = state.screenY / gs + 6.0F;
        if (module.viewItem.getValue()) {
            effectsBaseY = drawHeldItems(ms, entity, guiCenterX, effectsBaseY, alpha);
        }
        if (module.viewEffects.getValue()) {
            renderEffects(ms, entity, guiCenterX, effectsBaseY, alpha);
        }
    }

    private static String pingOf(LivingEntity entity) {
        if (!(entity instanceof Player player) || mc.getConnection() == null) return "";
        PlayerInfo info = mc.getConnection().getPlayerInfo(player.getUUID());
        return info == null ? "" : String.valueOf(Math.max(0, info.getLatency()));
    }

    private float drawHeldItems(PoseStack ms, LivingEntity entity, float centerX, float startY, float alpha) {
        Font font = Fonts.SF_SEMIBOLD;
        float y = startY;
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND}) {
            ItemStack stack = entity.getItemBySlot(slot);
            if (stack.isEmpty()) continue;
            String text = module.viewDisplayName.getValue() ? stack.getDisplayName().getString()
                    : stack.getHoverName().getString();
            float w = font.getWidth(text, ITEM_TEXT_SIZE);
            float x = centerX - w / 2.0F;
            RenderUtil.RECT.draw(ms, x - 1.5F, y - 4.0F, w + 3.0F, 10.0F, 1.0F, withAlpha(RECT_BG, alpha));
            font.drawText(ms, text, x, y - 1.0F, ITEM_TEXT_SIZE, withAlpha(Color.WHITE, alpha));
            y += 10.0F;
        }
        return y;
    }

    /**
     * @param ms матрица, уже отмасштабированная в пиксели экрана
     */
    private void drawArmorRow(Render2DEvent.Render2DEventData event, PoseStack ms, LivingEntity entity,
                              float centerX, float pillTop, float alpha) {
        List<ItemStack> stacks = new ArrayList<>();
        for (EquipmentSlot slot : DISPLAY_ORDER) {
            ItemStack stack = entity.getItemBySlot(slot);
            if (!stack.isEmpty()) stacks.add(stack.copy());
        }
        if (stacks.isEmpty()) return;

        GuiGraphicsExtractor extractor = event.context();
        Font font = Fonts.DIMASIK_MEDIUM;
        float itemSize = 24.0F;
        float step = 28.0F;
        float y = pillTop - itemSize - 6.0F;
        float offsetX = centerX - ((stacks.size() - 1) * step + itemSize) / 2.0F;

        for (ItemStack stack : stacks) {
            NameTagItemRenderer.draw(extractor, ms, stack, offsetX, y, itemSize, alpha);

            if (module.viewEnchants.getValue() && !stack.getEnchantments().isEmpty()) {
                float enchantY = y - 12.0F;
                for (String enchantment : collectEnchants(stack)) {
                    float eWidth = font.getWidth(enchantment, 10.0F);
                    font.drawText(ms, enchantment, offsetX + 12.0F - eWidth * 0.5F, enchantY, 10.0F,
                            withAlpha(isOverLevelled(enchantment) ? OVER_LEVEL : UIColors.textColor(), alpha));
                    enchantY -= 10.0F;
                }
            }
            offsetX += step;
        }
    }

    // Sharpness выше 5 и Protection выше 4 подсвечиваются красным
    private static boolean isOverLevelled(String label) {
        if (label.length() < 3) return false;
        String key = label.substring(0, 2);
        int level;
        try {
            level = Integer.parseInt(label.substring(2));
        } catch (NumberFormatException e) {
            return false;
        }
        return (key.equalsIgnoreCase("Sh") && level > 5) || (key.equalsIgnoreCase("Pr") && level > 4);
    }

    private List<String> collectEnchants(ItemStack stack) {
        List<Object2IntMap.Entry<Holder<Enchantment>>> sorted = new ArrayList<>(stack.getEnchantments().entrySet());
        sorted.sort((a, b) -> Integer.compare(b.getIntValue(), a.getIntValue()));
        List<String> result = new ArrayList<>();
        for (Object2IntMap.Entry<Holder<Enchantment>> entry : sorted) {
            String full = entry.getKey().value().description().getString();
            String shortName = full.substring(0, Math.min(2, full.length()));
            result.add(shortName + entry.getIntValue());
        }
        return result;
    }

    private void renderEffects(PoseStack ms, LivingEntity entity, float centerX, float startY, float alpha) {
        List<MobEffectInstance> effects = List.copyOf(entity.getActiveEffects());
        if (effects.isEmpty()) return;

        Font font = Fonts.SF_SEMIBOLD;
        List<EffectLine> lines = new ArrayList<>();
        for (MobEffectInstance inst : effects) {
            int durationTicks = inst.getDuration();
            if (durationTicks <= 20) continue;
            String name = inst.getEffect().value().getDisplayName().getString();
            int amp = inst.getAmplifier() + 1;
            StringBuilder sb = new StringBuilder(name);
            if (amp > 1) sb.append(' ').append(toRomanNumeral(amp));
            sb.append(' ').append(formatDuration(durationTicks));
            String text = sb.toString();
            lines.add(new EffectLine(text, font.getWidth(text, ITEM_TEXT_SIZE), effectColor(inst)));
        }
        if (lines.size() > 1) {
            lines.sort(Comparator.comparingDouble(e -> e.width));
        }

        float step = font.getHeight(ITEM_TEXT_SIZE) + 2.0F;
        for (int i = 0; i < lines.size(); i++) {
            EffectLine line = lines.get(i);
            font.drawText(ms, line.text, centerX - line.width / 2.0F, startY + i * step, ITEM_TEXT_SIZE,
                    withAlpha(line.color, alpha));
        }
    }

    private static Color effectColor(MobEffectInstance inst) {
        var keyHolder = BuiltInRegistries.MOB_EFFECT.getKey(inst.getEffect().value());
        String key = keyHolder == null ? "" : keyHolder.getPath().toLowerCase(Locale.ROOT);
        if (key.contains("strength")) return new Color(0xFFFF5555, true);
        if (key.contains("fire_resistance")) return new Color(0xFFFFAA00, true);
        if (key.contains("speed")) return new Color(0xFF55FFFF, true);
        if (key.contains("absorption")) return new Color(0xFFFFFF55, true);
        if (key.contains("regeneration")) return new Color(0xFFFF55FF, true);
        if (key.contains("jump_boost")) return new Color(0xFF55FF55, true);
        return Color.WHITE;
    }

    private static String formatDuration(int ticks) {
        int seconds = ticks / 20;
        int minutes = seconds / 60;
        seconds = seconds % 60;
        return String.format("%d:%02d", minutes, seconds);
    }

    private static String toRomanNumeral(int number) {
        if (number < 1 || number > 3999) return String.valueOf(number);
        String[] thousands = {"", "M", "MM", "MMM"};
        String[] hundreds = {"", "C", "CC", "CCC", "CD", "D", "DC", "DCC", "DCCC", "CM"};
        String[] tens = {"", "X", "XX", "XXX", "XL", "L", "LX", "LXX", "LXXX", "XC"};
        String[] ones = {"", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX"};
        return thousands[number / 1000] + hundreds[(number % 1000) / 100] + tens[(number % 100) / 10] + ones[number % 10];
    }

    /** Рисует компонент по кускам со своим цветом каждого, чтобы можно было применить прозрачность. */
    private static void drawComponent(Font font, PoseStack ms, Component component, float x, float y, float size, float alpha) {
        float[] cursor = {x};
        component.visit((style, segment) -> {
            if (!segment.isEmpty()) {
                TextColor color = style.getColor();
                Color base = color == null ? Color.WHITE : new Color(0xFF000000 | color.getValue(), true);
                font.drawText(ms, segment, cursor[0], y, size, withAlpha(base, alpha));
                cursor[0] += font.getWidth(segment, size);
            }
            return Optional.empty();
        }, Style.EMPTY);
    }

    private static float componentWidth(Font font, Component component, float size) {
        float[] width = {0.0F};
        component.visit((style, segment) -> {
            width[0] += font.getWidth(segment, size);
            return Optional.empty();
        }, Style.EMPTY);
        return width[0];
    }

    private static Color withAlpha(Color color, float alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(),
                Math.clamp(Math.round(color.getAlpha() * alpha), 0, 255));
    }

    private record EffectLine(String text, float width, Color color) {
    }

    /**
     * Состояние одной плашки. Появление мгновенное, исчезновение у игроков — плавное за секунду
     * (smootherstep); остальные сущности пропадают сразу.
     */
    private static final class TagState {
        final Entity entity;
        boolean targeted;
        boolean shown;
        boolean hasWorld;
        double worldX, worldY, worldZ;
        float screenX, screenY;
        double distanceSq;
        float alpha;
        private float fadeFrom;
        private long fadeStart;
        private boolean fading;

        TagState(Entity entity) {
            this.entity = entity;
        }

        void update(boolean visible, long now) {
            if (visible) {
                shown = true;
                fading = false;
                alpha = 1.0F;
                return;
            }
            if (!(entity instanceof Player)) {
                shown = false;
                alpha = 0.0F;
                return;
            }
            if (shown) {
                shown = false;
                fading = true;
                fadeFrom = alpha;
                fadeStart = now;
            }
            if (fading) {
                float t = Math.clamp((float) (now - fadeStart) / FADE_OUT_MS, 0.0F, 1.0F);
                float eased = t * t * t * (t * (t * 6.0F - 15.0F) + 10.0F);
                alpha = fadeFrom * (1.0F - eased);
                if (t >= 1.0F) {
                    alpha = 0.0F;
                    fading = false;
                }
            }
        }
    }
}
