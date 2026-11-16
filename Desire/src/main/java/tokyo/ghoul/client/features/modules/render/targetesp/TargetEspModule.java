package tokyo.ghoul.client.features.modules.render.targetesp;

import lombok.Getter;
import net.minecraft.world.entity.LivingEntity;
import tokyo.ghoul.api.module.Category;
import tokyo.ghoul.api.module.Module;
import tokyo.ghoul.api.module.ModuleRegister;
import tokyo.ghoul.api.module.setting.BooleanSetting;
import tokyo.ghoul.api.module.setting.ColorSetting;
import tokyo.ghoul.api.module.setting.ModeSetting;
import tokyo.ghoul.api.module.setting.SliderSetting;
import tokyo.ghoul.api.utils.color.UIColors;
import tokyo.ghoul.client.features.modules.combat.AuraModule;
import tokyo.ghoul.client.features.modules.render.targetesp.render.TargetEspFrame;
import tokyo.ghoul.client.features.modules.render.targetesp.render.TargetEspRenderer;

import java.awt.Color;

@ModuleRegister(name = "Target Esp", description = "Подсветка текущей цели", category = Category.RENDER)
public class TargetEspModule extends Module {
    @Getter private static final TargetEspModule instance = new TargetEspModule();

    private final ModeSetting mode = new ModeSetting("Отображение цели").value("Кольцо").values(
            "Карты", "Квадрат", "Кольцо", "Кристалики", "Молнии", "Пентограмма", "Призраки");

    private final ModeSetting ringType = new ModeSetting("Тип кольца").value("Тип 1").values("Тип 1", "Тип 2", "Тип 3")
            .setVisible(() -> mode.is("Кольцо"));
    private final ModeSetting squareType = new ModeSetting("Тип квадрата").value("Тип 1").values("Тип 1", "Тип 2")
            .setVisible(() -> mode.is("Квадрат"));
    private final ModeSetting ghostType = new ModeSetting("Тип призраков").value("Тип 1")
            .values("Тип 1", "Тип 2", "Тип 3", "Тип 4", "Тип 5", "Тип 6", "Тип 7", "Тип 8")
            .setVisible(() -> mode.is("Призраки"));
    private final ModeSetting crystalType = new ModeSetting("Тип кристалликов").value("Тип 1").values("Тип 1", "Тип 2", "Тип 3", "Тип 4", "Тип 5")
            .setVisible(() -> mode.is("Кристалики"));
    private final ModeSetting pentagramType = new ModeSetting("Тип пентограммы").value("Тип 1").values("Тип 1", "Тип 2")
            .setVisible(() -> mode.is("Пентограмма"));
    private final SliderSetting cardCount = new SliderSetting("Кол-во карт").value(4f).range(2f, 8f).step(1f)
            .setVisible(() -> mode.is("Карты"));
    private final SliderSetting cardSpeed = new SliderSetting("Скорость карт").value(1f).range(0.1f, 3f).step(0.1f)
            .setVisible(() -> mode.is("Карты"));
    private final BooleanSetting cardsThroughWalls = new BooleanSetting("Просвечивать карты").value(false)
            .setVisible(() -> mode.is("Карты"));
    private final ModeSetting lightningType = new ModeSetting("Тип молний").value("Тип 1").values("Тип 1", "Тип 2", "Тип 3")
            .setVisible(() -> mode.is("Молнии"));

    private final BooleanSetting jellyThroughWalls = new BooleanSetting("Сквозь стены").value(true).setVisible(this::jelly);
    private final SliderSetting jellyParticleSize = new SliderSetting("Размер частиц желе").value(0.2f).range(0.08f, 0.45f).step(0.01f)
            .setVisible(this::jelly);
    private final SliderSetting jellyLayers = new SliderSetting("Слои желе").value(14f).range(6f, 22f).step(1f).setVisible(this::jelly);

    private final BooleanSetting crystalsThroughWalls = new BooleanSetting("Просвечивать кристаллы").value(true)
            .setVisible(() -> mode.is("Кристалики"));
    private final BooleanSetting lightningThroughWalls = new BooleanSetting("Просвечивать").value(false)
            .setVisible(() -> mode.is("Молнии"));
    private final BooleanSetting lighting = new BooleanSetting("Освещение").value(true)
            .setVisible(() -> mode.is("Призраки") || mode.is("Кристалики") || mode.is("Молнии"));
    private final SliderSetting ringSpeed = new SliderSetting("Скорость кольца").value(1.2f).range(0.1f, 5f).step(0.1f)
            .setVisible(() -> mode.is("Кольцо") && !jelly());
    private final SliderSetting souls = new SliderSetting("Кол-во душ").value(3f).range(2f, 4f).step(1f)
            .setVisible(() -> mode.is("Призраки") && ghostType.is("Тип 6"));
    private final BooleanSetting redOnHit = new BooleanSetting("Красный при ударе").value(true);
    private final BooleanSetting themeColor = new BooleanSetting("Авто цвет под тему").value(true);
    private final ColorSetting color = new ColorSetting("Цвет отображения").value(new Color(-65400, true))
            .setVisible(() -> !themeColor.getValue());
    private final ColorSetting gradientColor = new ColorSetting("Цвет градиента кристаллов").value(new Color(-11486721, true))
            .setVisible(() -> !themeColor.getValue() && (mode.is("Кристалики") && crystalType.is("Тип 5") || mode.is("Карты")
                    || mode.is("Кольцо") && ringType.is("Тип 2")
                    || mode.is("Призраки") && (ghostType.is("Тип 2") || ghostType.is("Тип 3") || ghostType.is("Тип 4") || ghostType.is("Тип 5"))));
    private final BooleanSetting ghostGlow = new BooleanSetting("Свечение").value(false)
            .setVisible(() -> mode.is("Призраки") && ghostType.is("Тип 2"));
    private final BooleanSetting ghostThroughWalls = new BooleanSetting("Просвечивать призраков").value(true)
            .setVisible(() -> mode.is("Призраки") && (ghostType.is("Тип 2") || ghostType.is("Тип 4") || ghostType.is("Тип 5")));
    private final BooleanSetting distortion = new BooleanSetting("Искажение").value(true).setVisible(this::distortionAvailable);
    private final SliderSetting distortionStrength = new SliderSetting("Сила искажения").value(12f).range(3f, 30f).step(1f)
            .setVisible(() -> distortionAvailable() && distortion.getValue());

    public TargetEspModule() {
        addSettings(mode, ringType, squareType, ghostType, crystalType, pentagramType, lightningType,
                cardCount, cardSpeed, cardsThroughWalls,
                jellyThroughWalls, jellyParticleSize, jellyLayers, crystalsThroughWalls, lightningThroughWalls, lighting,
                ringSpeed, souls, redOnHit, themeColor, color, gradientColor, ghostGlow, ghostThroughWalls,
                distortion, distortionStrength);
    }

    @Override
    public void onEvent() {
    }

    private boolean jelly() {
        return mode.is("Кольцо") && ringType.is("Тип 3");
    }

    private boolean distortionAvailable() {
        boolean ghostNoDistortion = mode.is("Призраки")
                && (ghostType.is("Тип 2") || ghostType.is("Тип 4") || ghostType.is("Тип 5"));
        return !jelly() && !ghostNoDistortion;
    }

    private static LivingEntity target() {
        AuraModule aura = AuraModule.getInstance();
        return aura.isEnabled() && aura.target != null && aura.target.isAlive() ? aura.target : null;
    }

    public void renderWorld(TargetEspFrame frame) {
        TargetEspRenderer.m_7905(distortion.getValue(), distortionStrength.getValue());
        TargetEspRenderer.m_7919(ghostGlow.getValue(), ghostThroughWalls.getValue());
        TargetEspRenderer.m_7889(jellyThroughWalls.getValue(), jellyParticleSize.getValue(), jellyLayers.getValue().intValue());
        TargetEspRenderer.setExtraSettings(pentagramType.getValue(), cardCount.getValue().intValue(), cardSpeed.getValue(),
                cardsThroughWalls.getValue());
        TargetEspRenderer.m_7921(
                ringSpeed.getValue(),
                ringType.getValue(),
                souls.getValue().intValue(),
                redOnHit.getValue(),
                ghostType.getValue(),
                crystalType.getValue(),
                crystalsThroughWalls.getValue(),
                lightningType.getValue(),
                lightningThroughWalls.getValue(),
                lighting.getValue(),
                squareType.getValue());

        int first = themeColor.getValue() ? UIColors.primary().getRGB() : color.getValue().getRGB();
        int second = themeColor.getValue() ? UIColors.secondary().getRGB() : gradientColor.getValue().getRGB();
        LivingEntity target = target();
        if (target != null) {
            TargetEspRenderer.m_7914(frame, target, mode.getValue(), first, second);
        } else {
            TargetEspRenderer.m_7925(frame);
        }
    }
}
