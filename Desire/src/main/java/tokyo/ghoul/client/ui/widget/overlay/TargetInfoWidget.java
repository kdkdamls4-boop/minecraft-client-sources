package tokyo.ghoul.client.ui.widget.overlay;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import org.joml.Vector4f;
import tokyo.ghoul.api.system.draggable.Draggable;
import tokyo.ghoul.api.utils.animation.AnimationUtil;
import tokyo.ghoul.api.utils.animation.Easing;
import tokyo.ghoul.api.utils.color.ColorUtil;
import tokyo.ghoul.api.utils.color.UIColors;
import tokyo.ghoul.api.utils.math.MathUtil;
import tokyo.ghoul.api.utils.render.RenderUtil;
import tokyo.ghoul.api.utils.render.fonts.Fonts;
import tokyo.ghoul.client.features.modules.combat.AuraModule;
import tokyo.ghoul.client.ui.widget.HudIcons;
import tokyo.ghoul.client.ui.widget.RelicStyle;
import tokyo.ghoul.client.ui.widget.Widget;
import net.minecraft.client.gui.screens.ChatScreen;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class TargetInfoWidget extends Widget {
    private static final EquipmentSlot[] ARMOR_SLOTS = { EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET };
    private static final String[] COLOR_PILLS = { "Клиентский", "Здоровье" };
    private static final String[] BG_PILLS = { "Обычный", "Жидкое стекло" };

    public TargetInfoWidget() {
        super(30f, 30f);
        enableHudSettingsMenu();
    }

    @Override
    public String getName() { return "Target info"; }

    private final AnimationUtil showAnimation = new AnimationUtil();
    private float healthAnimation;
    private float delayedHealth;
    private float absorptionAnimation;
    private float cooldownAnimation;
    private LivingEntity target;
    private float lastHealth = -1f;
    private final List<BarParticle> particles = new ArrayList<>();
    private final Random random = new Random();
    private int hpColorMode;
    private int bgMode = -1;
    private float lastMenuH;
    private final float[] colorX = new float[2], colorY = new float[2], colorW = new float[2], colorH = new float[2];
    private final float[] bgPillX = new float[2], bgPillY = new float[2], bgPillW = new float[2], bgPillH = new float[2];

    private int getBgMode() {
        if (bgMode != -1) return bgMode;
        return isWatermarkLiquidGlass() ? 1 : 0;
    }

    private void setBgMode(int mode) {
        this.bgMode = mode == 1 ? 1 : 0;
        if (this.bgMode == 0) {
            setShowBackground(true);
            setLiquidGlass(false);
        } else {
            setShowBackground(false);
            setLiquidGlass(true);
        }
    }

    @Override
    protected boolean liquidGlass() {
        return getBgMode() == 1;
    }

    @Override
    protected boolean showBackground() {
        return getBgMode() == 0;
    }

    @Override
    public void render(PoseStack ms) {
        showAnimation.update();
        var particleIterator = particles.iterator();
        while (particleIterator.hasNext()) if (particleIterator.next().update()) particleIterator.remove();
        LivingEntity current = getTarget();
        boolean hasTarget = current != null;
        if (hasTarget && current != target) {
            target = current;
            lastHealth = -1f;
        }
        showAnimation.run(hasTarget ? 1.0 : 0.0, getDuration(), getEasing());
        if (!hasTarget && showAnimation.getValue() <= 0.01) {
            target = null;
            return;
        }

        float anim = (float) showAnimation.getValue();
        if (anim <= 0.01f || target == null) return;
        int fa = (int) (anim * 255f);

        float currentHp = target.getHealth();
        float maxHp = target.getMaxHealth();
        float absorption = target.getAbsorptionAmount();
        boolean damaged = lastHealth > 0f && currentHp < lastHealth - 0.01f;
        if (hasTarget) lastHealth = currentHp;

        healthAnimation = Mth.clamp(MathUtil.interpolate(healthAnimation, currentHp / maxHp, 0.1f), 0f, 1f);
        if (delayedHealth < healthAnimation) delayedHealth = healthAnimation;
        else delayedHealth = Mth.clamp(MathUtil.interpolate(delayedHealth, healthAnimation, 0.06f), 0f, 1f);
        absorptionAnimation = Mth.clamp(MathUtil.interpolate(absorptionAnimation, absorption > 0 ? Math.min(absorption / maxHp, 1f) : 0f, 0.08f), 0f, 1f);

        renderOldBar(ms, anim, currentHp, maxHp, absorption, damaged);
        renderHudSettingsMenu(ms);
    }

    private void renderOldBar(PoseStack ms, float anim, float currentHp, float maxHp, float absorption, boolean damaged) {
        float x = getDraggable().getX(), y = getDraggable().getY();
        int fa = (int) (anim * 255f);

        float stroke = RelicStyle.u(4f), corner = RelicStyle.u(28f);
        float panelH = RelicStyle.u(181f);
        float sideW = RelicStyle.u(183f);
        float headSize = RelicStyle.u(128f), headR = RelicStyle.u(22f);
        float nameSize = RelicStyle.u(46f), hpSize = RelicStyle.u(47f);
        float contentGap = RelicStyle.u(23f), padRight = RelicStyle.u(47f);
        float barH = RelicStyle.u(12f);

        String name = target.getName().getString();
        if (name.length() > 14) name = name.substring(0, 14);

        int hpVal = (int) Math.ceil(currentHp);
        String hpText = String.valueOf(hpVal);
        if (absorption > 0f) hpText += "+" + (int) Math.ceil(absorption);

        float nameW = RelicStyle.width(Fonts.HUD_MEDIUM, name, nameSize);
        float hpW = RelicStyle.width(Fonts.HUD_BOLD, hpText, hpSize);

        float rightW = Math.max(RelicStyle.u(332f), nameW + RelicStyle.u(80f) + hpW);
        float totalWidth = sideW + contentGap + rightW + padRight;

        RelicStyle.panel(ms, x, y, totalWidth, panelH, corner, stroke, RelicStyle.BORDER, RelicStyle.FILL, anim, liquidGlass());
        if (!liquidGlass()) {
            float r = Math.max(0f, corner - stroke);
            RenderUtil.RECT.draw(ms, x + sideW, y + stroke, totalWidth - sideW - stroke, panelH - stroke * 2f,
                    new Vector4f(0f, r, 0f, r), RelicStyle.a(new Color(255, 255, 255, 7), anim));
        }

        float headX = x + (sideW - headSize) / 2f;
        float headY = y + (panelH - headSize) / 2f;
        if (target instanceof Player player) {
            RenderUtil.TEXTURE_RECT.drawHead(ms, player, headX, headY, headSize, headSize, 0f, headR,
                    ColorUtil.setAlpha(Color.WHITE, fa));
        } else {
            RenderUtil.RECT.draw(ms, headX, headY, headSize, headSize, headR,
                    ColorUtil.setAlpha(new Color(30, 30, 30), fa));
            Fonts.HUD_BOLD.drawCenteredText(ms, "?", headX + headSize / 2f, headY + headSize / 2f - nameSize / 2f,
                    nameSize, ColorUtil.setAlpha(Color.WHITE, (int) (140 * anim)));
        }
        drawEatingOverlay(ms, headX, headY, headSize, headR, anim);

        float contentX = x + sideW + contentGap;
        float nameCy = y + RelicStyle.u(76.5f);
        RelicStyle.text(ms, Fonts.HUD_MEDIUM, name, contentX, nameCy, nameSize, RelicStyle.TEXT, anim);

        Color theme = hpColorMode == 0 ? RelicStyle.accent() : hpFillColor(currentHp, maxHp, 255);
        float barX = contentX;
        float barW = rightW;
        float barY = y + RelicStyle.u(130f) - barH / 2f;
        float barR = barH / 2f;

        RelicStyle.text(ms, Fonts.HUD_BOLD, hpText, barX + barW - RelicStyle.u(6f) - hpW, nameCy, hpSize, hpColorMode == 0 ? RelicStyle.ACCENT : theme, anim);

        RenderUtil.RECT.draw(ms, barX, barY, barW, barH, barR, RelicStyle.a(new Color(theme.getRed(), theme.getGreen(), theme.getBlue(), 33), anim));

        float fillW = Mth.clamp(barW * healthAnimation, 0f, barW);
        if (fillW > k(0.5f)) {
            float glowW = Math.max(fillW + k(12f), k(20f));
            RenderUtil.TEXTURE_RECT.draw(ms, barX + fillW / 2f - glowW / 2f, barY + barH / 2f - k(10f),
                    glowW, k(20f), 0.01f, ColorUtil.setAlpha(theme, (int) (24 * anim)), 0f, 0f, 1f, 1f, GLOW);
        }

        RenderUtil.RECT.draw(ms, barX, barY, Mth.clamp(barW * delayedHealth, 0f, barW), barH, barR,
                RelicStyle.a(new Color(theme.getRed(), theme.getGreen(), theme.getBlue(), 80), anim));
        if (absorptionAnimation < healthAnimation && fillW > 0.5f) {
            Color left = RelicStyle.a(new Color(theme.getRed(), theme.getGreen(), theme.getBlue(), 45), anim);
            Color right = RelicStyle.a(hpColorMode == 0 ? RelicStyle.accent2() : theme, anim);
            RenderUtil.GRADIENT_RECT.draw(ms, barX, barY, fillW, barH, barR, left, right, left, right);
        }
        if (absorption > 0f) {
            Color gold1 = new Color(255, 209, 0, fa);
            Color gold2 = new Color(255, 246, 20, fa);
            RenderUtil.GRADIENT_RECT.draw(ms, barX, barY, Mth.clamp(barW * absorptionAnimation, 0f, barW), barH, barR,
                    gold1, gold2, gold1, gold2);
        }

        if (damaged && fillW > k(1f)) {
            int count = 5 + random.nextInt(4);
            for (int i = 0; i < count; i++) {
                float angle = (float) (-Math.PI / 2 + (random.nextFloat() - 0.5f) * 1.6);
                float speed = 0.4f + random.nextFloat() * 0.7f;
                particles.add(new BarParticle(
                        barX + random.nextFloat() * fillW,
                        barY + random.nextFloat() * barH,
                        (float) Math.cos(angle) * speed,
                        (float) Math.sin(angle) * speed,
                        14f + random.nextFloat() * 10f,
                        k(0.7f) + random.nextFloat() * k(0.7f)));
            }
        }

        for (BarParticle particle : particles) {
            RenderUtil.RECT.draw(ms, particle.x - particle.size / 2f, particle.y - particle.size / 2f,
                    particle.size, particle.size, particle.size / 2f,
                    ColorUtil.setAlpha(theme, (int) (particle.life / particle.maxLife * fa)));
        }

        float chipH = RelicStyle.u(62f);
        float chipY = y - RelicStyle.u(20f) - chipH;
        List<ItemStack> hands = List.of(target.getMainHandItem(), target.getOffhandItem());
        List<ItemStack> armor = new ArrayList<>();
        for (EquipmentSlot slot : ARMOR_SLOTS) armor.add(target.getItemBySlot(slot));
        drawSlotChip(ms, x + RelicStyle.u(42f), chipY, chipH, hands, anim);
        float armorW = RelicStyle.u(44.5f) * armor.size() + RelicStyle.u(18f);
        drawSlotChip(ms, x + totalWidth - RelicStyle.u(39f) - armorW, chipY, chipH, armor, anim);

        getDraggable().setWidth(totalWidth);
        getDraggable().setHeight(panelH);
    }

    private void drawSlotChip(PoseStack ms, float x, float y, float h, List<ItemStack> stacks, float anim) {
        float step = RelicStyle.u(44.5f);
        float w = step * stacks.size() + RelicStyle.u(18f);
        RelicStyle.panel(ms, x, y, w, h, h * 0.4f, RelicStyle.u(3.5f), RelicStyle.BORDER, RelicStyle.FILL, anim, liquidGlass());

        float itemSize = RelicStyle.u(34f);
        float cross = RelicStyle.u(13f);
        for (int i = 0; i < stacks.size(); i++) {
            float cx = x + RelicStyle.u(9f) + step * (i + 0.5f);
            float cy = y + h / 2f;
            ItemStack stack = stacks.get(i);
            Identifier texture = stack.isEmpty() ? null : itemTexture(stack);
            if (texture != null) {
                RenderUtil.TEXTURE_RECT.draw(ms, cx - itemSize / 2f, cy - itemSize / 2f, itemSize, itemSize, 0f,
                        RelicStyle.a(Color.WHITE, anim), 0f, 0f, 1f, 1f, texture);
            } else {
                HudIcons.draw(ms, cx - cross / 2f, cy - cross / 2f, cross, cross, HudIcons.WM_CROSS,
                        RelicStyle.a(RelicStyle.SLOT_X, anim));
            }
        }
    }

    private Color[] getHealthBarColors(float currentHP, float maxHP) {
        if (currentHP >= maxHP * 0.7f) {
            return new Color[]{
                    new Color(0, 40, 8), new Color(0, 80, 15), new Color(0, 60, 12),
                    new Color(0, 160, 40), new Color(0, 190, 45)
            };
        }
        if (currentHP >= maxHP * 0.35f) {
            return new Color[]{
                    new Color(50, 55, 25), new Color(85, 70, 50), new Color(55, 50, 22),
                    new Color(140, 130, 60), new Color(160, 150, 70)
            };
        }
        return new Color[]{
                new Color(50, 35, 25), new Color(70, 45, 40), new Color(80, 42, 32),
                new Color(160, 90, 70), new Color(180, 100, 75)
        };
    }

    private Color hpFillColor(float currentHp, float maxHp, int alpha) {
        if (hpColorMode == 0) return UIColors.primary(alpha);
        return ColorUtil.setAlpha(getHealthBarColors(currentHp, maxHp)[4], alpha);
    }

    private Color hpTrackColor(float currentHp, float maxHp, int alpha) {
        if (hpColorMode == 0) return mul(UIColors.primary(alpha), 0.30f);
        return ColorUtil.setAlpha(getHealthBarColors(currentHp, maxHp)[0], alpha);
    }

    private Color hpDelayedColor(float currentHp, float maxHp, int alpha) {
        if (hpColorMode == 0) return mul(UIColors.primary(alpha), 0.55f);
        return ColorUtil.setAlpha(getHealthBarColors(currentHp, maxHp)[3], alpha);
    }

    private Color mul(Color color, float factor) {
        return new Color((int) (color.getRed() * factor), (int) (color.getGreen() * factor),
                (int) (color.getBlue() * factor), color.getAlpha());
    }

    private static class BarParticle {
        float x, y, vx, vy, life, maxLife, size;

        BarParticle(float x, float y, float vx, float vy, float life, float size) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            this.life = life;
            this.maxLife = life;
            this.size = size;
        }

        boolean update() {
            x += vx;
            y += vy;
            vy += 0.02f;
            vx *= 0.97f;
            vy *= 0.97f;
            life -= 1f;
            return life <= 0;
        }
    }

    private void drawEquipment(PoseStack ms, float centerX, float y, float anim) {
        float size = k(6.5f), gap = k(1f);

        List<ItemStack> stacks = new ArrayList<>();
        for (EquipmentSlot slot : ARMOR_SLOTS) stacks.add(target.getItemBySlot(slot));
        stacks.add(target.getMainHandItem());
        stacks.add(target.getOffhandItem());
        stacks.removeIf(ItemStack::isEmpty);
        if (stacks.isEmpty()) return;

        float totalW = stacks.size() * size + (stacks.size() - 1) * gap;
        float currentX = centerX - totalW / 2f;
        for (ItemStack stack : stacks) {
            Identifier texture = itemTexture(stack);
            if (texture != null) {
                RenderUtil.TEXTURE_RECT.draw(ms, currentX, y, size, size, 0f,
                        ColorUtil.setAlpha(Color.WHITE, (int) (255 * anim)), 0f, 0f, 1f, 1f, texture);
            } else {
                RenderUtil.RECT.draw(ms, currentX, y, size, size, k(1f), new Color(18, 18, 26, (int) (255 * anim)));
                Fonts.SF_BOLD.drawCenteredText(ms, "?", currentX + size / 2f, y + size / 2f - k(2.2f), k(5f),
                        ColorUtil.setAlpha(Color.WHITE, (int) (255 * anim)));
            }
            currentX += size + gap;
        }
    }

    private void drawEatingOverlay(PoseStack ms, float headX, float headY, float headSize, float headRadius, float alpha) {
        if (target == null || !target.isUsingItem()) return;
        ItemStack use = target.getUseItem();
        if (use.isEmpty()) return;
        ItemUseAnimation animation = use.getUseAnimation();
        if (animation != ItemUseAnimation.EAT && animation != ItemUseAnimation.DRINK) return;

        RenderUtil.RECT.draw(ms, headX, headY, headSize, headSize, headRadius,
                ColorUtil.setAlpha(new Color(0, 0, 0), (int) (165 * alpha)));

        float itemSize = headSize * 0.55f;
        float itemX = headX + (headSize - itemSize) / 2f;
        float itemY = headY + (headSize - itemSize) / 2f - headSize * 0.10f;
        Identifier texture = itemTexture(use);
        if (texture != null) {
            RenderUtil.TEXTURE_RECT.draw(ms, itemX, itemY, itemSize, itemSize, 0f,
                    ColorUtil.setAlpha(Color.WHITE, (int) (255 * alpha)), 0f, 0f, 1f, 1f, texture);
        }

        int remainingTicks = Math.max(0, target.getUseItemRemainingTicks());
        String timer = String.format(Locale.ROOT, "%.1f", remainingTicks / 20.0f).replace('.', ',') + " с";
        float timerSize = Math.max(5.5f, headSize * 0.24f);
        float timerW = Fonts.SF_MEDIUM.getWidth(timer, timerSize);
        float timerX = headX + (headSize - timerW) / 2f;
        float timerY = headY + headSize - timerSize - headSize * 0.05f;
        Fonts.SF_MEDIUM.drawText(ms, timer, timerX, timerY, timerSize,
                ColorUtil.setAlpha(Color.WHITE, (int) (255 * alpha)));
    }

    private Identifier itemTexture(ItemStack stack) {
        var key = BuiltInRegistries.ITEM.getKey(stack.getItem());
        Identifier texture = Identifier.fromNamespaceAndPath(key.getNamespace(), "textures/item/" + key.getPath() + ".png");
        if (mc.getResourceManager().getResource(texture).isEmpty()) return null;
        return texture;
    }

    private LivingEntity getTarget() {
        AuraModule aura = AuraModule.getInstance();
        if (aura.isEnabled() && aura.target != null) return aura.target;
        if (mc.gui.screen() instanceof ChatScreen && mc.player != null) return mc.player;
        return null;
    }

    @Override
    protected void handleHudSettingsPress(int button) {
        Draggable d = getDraggable();
        float mx = d.normaliseX(), my = d.normaliseY();

        if (d.isHovering()) {
            if (button == 1) {
                if (isMenuOpen()) closeTargetMenu();
                else {
                    setMenuScreen(mc.gui.screen());
                    setMenuOpen(true);
                    getMenuAnim().run(1f, 150, Easing.EXPO_OUT);
                }
            } else if (isMenuOpen()) {
                closeTargetMenu();
            }
            return;
        }
        if (!isMenuOpen()) return;

        int color = colorPillAt(mx, my);
        if (color >= 0) {
            hpColorMode = color;
            return;
        }
        int bg = bgPillAt(mx, my);
        if (bg >= 0) {
            setBgMode(bg);
            return;
        }
        if (mx >= getMenuX() && mx <= getMenuX() + getMenuW() && my >= getMenuY() && my <= getMenuY() + lastMenuH) {
            return;
        }
        closeTargetMenu();
    }

    @Override
    protected void renderHudSettingsMenu(PoseStack ms) {
        if (!isHudSettingsMenu()) return;
        if (isMenuOpen() && mc.gui.screen() != getMenuScreen()) closeTargetMenu();
        if (isMenuOpen() && getDraggable().isDragging()) closeTargetMenu();

        getMenuAnim().update();
        float anim = (float) getMenuAnim().getValue();

        float textS = k(7.5f), pad = k(6f), corner = k(2f);
        float tileSize = textS * 0.9f;
        float tileH = tileSize * 1.8f;
        float pillGap = k(4f);
        float colorTotal = 0f;
        for (String pill : COLOR_PILLS) {
            colorTotal += Fonts.PS_REGULAR.getWidth(pill, tileSize) + tileSize + pillGap;
        }
        colorTotal -= pillGap;
        float bgTotal = 0f;
        for (String pill : BG_PILLS) {
            bgTotal += Fonts.PS_REGULAR.getWidth(pill, tileSize) + tileSize + pillGap;
        }
        bgTotal -= pillGap;
        float colorLabelW = Fonts.PS_REGULAR.getWidth("Цвет отображения хп", textS);
        float bgLabelW = Fonts.PS_REGULAR.getWidth("Фон", textS);
        float colorLabelH = k(12f);
        float colorRowH = tileH + k(8f);
        float bgLabelH = k(12f);
        float bgRowH = tileH + k(8f);
        setMenuW(Math.max(getDraggable().getWidth(), Math.max(Math.max(colorLabelW, colorTotal), Math.max(bgLabelW, bgTotal)) + pad * 2f));
        setMenuRowH(colorRowH);
        setMenuX(getDraggable().getX());
        setMenuY(getDraggable().getY() + getDraggable().getHeight() + k(3f));

        if (anim <= 0.01f) return;

        int a = (int) (255 * anim);
        float slide = (1f - anim) * k(4f);
        float top = getMenuY() - slide;
        float menuH = colorLabelH + colorRowH + bgLabelH + bgRowH;
        lastMenuH = menuH;

        RelicStyle.menuPanel(ms, getMenuX(), top, getMenuW(), menuH, corner, anim);

        float colorLabelY = top;
        Fonts.PS_REGULAR.drawText(ms, "Цвет отображения хп", getMenuX() + pad, colorLabelY + (colorLabelH - textS) / 2f + k(0.5f),
                textS, ColorUtil.setAlpha(UIColors.textColor(), a));

        float colorRowY = colorLabelY + colorLabelH;
        float colorCursor = getMenuX() + pad;
        for (int i = 0; i < COLOR_PILLS.length; i++) {
            float tileW = Fonts.PS_REGULAR.getWidth(COLOR_PILLS[i], tileSize) + tileSize;
            colorX[i] = colorCursor;
            colorY[i] = colorRowY + (colorRowH - tileH) / 2f;
            colorW[i] = tileW;
            colorH[i] = tileH;
            boolean on = hpColorMode == i;
            Color fill = on
                    ? ColorUtil.setAlpha(UIColors.primary(), a)
                    : ColorUtil.setAlpha(new Color(28, 30, 34), a);
            Color text = on
                    ? ColorUtil.setAlpha(new Color(12, 14, 16), a)
                    : ColorUtil.setAlpha(new Color(180, 184, 190), a);
            RenderUtil.RECT.draw(ms, colorX[i], colorY[i], colorW[i], colorH[i], colorH[i] * 0.28f, fill);
            Fonts.PS_REGULAR.drawCenteredText(ms, COLOR_PILLS[i], colorX[i] + colorW[i] / 2f,
                    colorY[i] + colorH[i] / 2f - tileSize / 2f, tileSize, text);
            colorCursor += tileW + pillGap;
        }

        float bgLabelY = colorRowY + colorRowH;
        Fonts.PS_REGULAR.drawText(ms, "Фон", getMenuX() + pad, bgLabelY + (bgLabelH - textS) / 2f + k(0.5f),
                textS, ColorUtil.setAlpha(UIColors.textColor(), a));

        float bgRowY = bgLabelY + bgLabelH;
        float bgCursor = getMenuX() + pad;
        int activeBg = getBgMode();
        for (int i = 0; i < BG_PILLS.length; i++) {
            float tileW = Fonts.PS_REGULAR.getWidth(BG_PILLS[i], tileSize) + tileSize;
            bgPillX[i] = bgCursor;
            bgPillY[i] = bgRowY + (bgRowH - tileH) / 2f;
            bgPillW[i] = tileW;
            bgPillH[i] = tileH;
            boolean on = activeBg == i;
            Color fill = on
                    ? ColorUtil.setAlpha(UIColors.primary(), a)
                    : ColorUtil.setAlpha(new Color(28, 30, 34), a);
            Color text = on
                    ? ColorUtil.setAlpha(new Color(12, 14, 16), a)
                    : ColorUtil.setAlpha(new Color(180, 184, 190), a);
            RenderUtil.RECT.draw(ms, bgPillX[i], bgPillY[i], bgPillW[i], bgPillH[i], bgPillH[i] * 0.28f, fill);
            Fonts.PS_REGULAR.drawCenteredText(ms, BG_PILLS[i], bgPillX[i] + bgPillW[i] / 2f,
                    bgPillY[i] + bgPillH[i] / 2f - tileSize / 2f, tileSize, text);
            bgCursor += tileW + pillGap;
        }
    }

    private void closeTargetMenu() {
        setMenuOpen(false);
        getMenuAnim().run(0f, 150, Easing.EXPO_OUT);
    }

    private int colorPillAt(float mx, float my) {
        for (int i = 0; i < COLOR_PILLS.length; i++) {
            if (mx >= colorX[i] && mx <= colorX[i] + colorW[i] && my >= colorY[i] && my <= colorY[i] + colorH[i]) return i;
        }
        return -1;
    }

    private int bgPillAt(float mx, float my) {
        for (int i = 0; i < BG_PILLS.length; i++) {
            if (mx >= bgPillX[i] && mx <= bgPillX[i] + bgPillW[i] && my >= bgPillY[i] && my <= bgPillY[i] + bgPillH[i]) return i;
        }
        return -1;
    }
}
