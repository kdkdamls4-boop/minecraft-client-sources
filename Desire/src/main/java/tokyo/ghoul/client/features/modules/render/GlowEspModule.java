package tokyo.ghoul.client.features.modules.render;

import com.mojang.blaze3d.pipeline.RenderTarget;
import lombok.Getter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import tokyo.ghoul.api.module.Category;
import tokyo.ghoul.api.module.Module;
import tokyo.ghoul.api.module.ModuleRegister;
import tokyo.ghoul.api.module.setting.BooleanSetting;
import tokyo.ghoul.api.module.setting.ColorSetting;
import tokyo.ghoul.api.module.setting.MultiBooleanSetting;
import tokyo.ghoul.api.module.setting.SliderSetting;
import tokyo.ghoul.api.system.configs.FriendManager;
import tokyo.ghoul.api.utils.color.UIColors;
import tokyo.ghoul.client.features.modules.render.targetesp.render.EspBloomPass;

import java.awt.Color;

@ModuleRegister(name = "Glow Esp", description = "Свечение игроков сквозь стены", category = Category.RENDER)
public class GlowEspModule extends Module {
    @Getter private static final GlowEspModule instance = new GlowEspModule();

    private final MultiBooleanSetting targets = new MultiBooleanSetting("Цели").value(
            new BooleanSetting("Игроки").value(true),
            new BooleanSetting("Мобы").value(false),
            new BooleanSetting("Себя").value(false),
            new BooleanSetting("Друзья").value(true));
    private final BooleanSetting themeColor = new BooleanSetting("Цвет от темы").value(true);
    private final ColorSetting color = new ColorSetting("Цвет").value(new Color(255, 70, 120))
            .setVisible(() -> !themeColor.getValue());
    private final BooleanSetting friendColor = new BooleanSetting("Свой цвет друзьям").value(true);
    private final ColorSetting friendsColor = new ColorSetting("Цвет друзей").value(new Color(85, 255, 130))
            .setVisible(friendColor::getValue);
    private final SliderSetting radius = new SliderSetting("Размытие").value(6f).range(1f, 12f).step(0.5f);
    private final SliderSetting intensity = new SliderSetting("Яркость").value(1.4f).range(0.3f, 3f).step(0.05f);
    private final SliderSetting inner = new SliderSetting("Подсветка модели").value(0.35f).range(0f, 1f).step(0.05f);
    private final BooleanSetting outline = new BooleanSetting("Обводка").value(true);
    private final SliderSetting outlineWidth = new SliderSetting("Толщина обводки").value(1.5f).range(0.5f, 4f).step(0.5f)
            .setVisible(outline::getValue);

    public GlowEspModule() {
        addSettings(targets, themeColor, color, friendColor, friendsColor, radius, intensity, inner, outline, outlineWidth);
    }

    @Override
    public void onEvent() {
    }

    public boolean replacesOutline() {
        return isEnabled() && mc.player != null;
    }

    public boolean shouldGlow(Entity entity) {
        if (!isEnabled() || mc.player == null || !(entity instanceof LivingEntity living) || !living.isAlive()) return false;
        if (entity == mc.player) return targets.isEnabled("Себя");
        if (entity instanceof Player player) {
            boolean friend = FriendManager.getInstance().contains(player.getName().getString());
            return friend ? targets.isEnabled("Друзья") : targets.isEnabled("Игроки");
        }
        return entity instanceof Mob && targets.isEnabled("Мобы");
    }

    public int color(Entity entity) {
        if (friendColor.getValue() && entity instanceof Player player && entity != mc.player
                && FriendManager.getInstance().contains(player.getName().getString())) {
            return friendsColor.getValue().getRGB() & 0xFFFFFF;
        }
        Color base = themeColor.getValue() ? UIColors.primary() : color.getValue();
        return base.getRGB() & 0xFFFFFF;
    }

    public void composite(RenderTarget silhouettes, RenderTarget main) {
        int glow = themeColor.getValue() ? UIColors.primary().getRGB() : color.getValue().getRGB();
        int outlineColor = outline.getValue() ? (0xFF << 24) | (glow & 0xFFFFFF) : 0;
        EspBloomPass.m_8385(silhouettes, main, glow, radius.getValue(), intensity.getValue(), 1.0f,
                true, inner.getValue(), outlineColor, outline.getValue() ? outlineWidth.getValue() : 0f);
    }
}
