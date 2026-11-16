package tokyo.ghoul.client.ui.clickgui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.Mth;
import tokyo.ghoul.api.module.Module;
import tokyo.ghoul.api.module.ModuleManager;
import tokyo.ghoul.api.utils.animation.AnimationUtil;
import tokyo.ghoul.api.utils.animation.Easing;
import tokyo.ghoul.api.utils.color.UIColors;
import tokyo.ghoul.api.utils.math.MouseUtil;
import tokyo.ghoul.api.utils.render.RenderUtil;
import tokyo.ghoul.api.utils.render.ScissorUtil;
import tokyo.ghoul.api.utils.render.fonts.Fonts;
import tokyo.ghoul.client.ui.clickgui.module.ModuleComponent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClickGuiModuleList {
    private final Map<Module, ModuleComponent> components = new HashMap<>();
    private final Map<ClickGuiTab, Float> tabScrolls = new HashMap<>();
    private ClickGuiTab activeTab = ClickGuiTab.COMBAT;
    private float scroll;
    private final AnimationUtil scrollAnimation = new AnimationUtil();
    private boolean wasSearching = false;

    public void rebuild() {
        Map<Module, ModuleComponent> old = new HashMap<>(components);
        components.clear();
        for (Module module : ModuleManager.getInstance().getModules()) {
            if (module.isHidden()) continue;
            components.put(module, old.getOrDefault(module, new ModuleComponent(module)));
        }
    }

    public boolean isEmpty() {
        return components.isEmpty();
    }

    public void resetScroll() {
        scroll = 0f;
        scrollAnimation.setValue(0);
        if (activeTab != null) {
            tabScrolls.put(activeTab, 0f);
        }
    }

    public void render(GuiGraphicsExtractor context, float windowX, float windowY, float alpha, ClickGuiTab tab, String query, int mouseX, int mouseY, float delta) {
        boolean searching = query != null && !query.isEmpty();
        if (searching) {
            if (!wasSearching) {
                tabScrolls.put(activeTab, scroll);
                wasSearching = true;
                scroll = 0f;
                scrollAnimation.setValue(0);
            }
        } else {
            if (wasSearching) {
                wasSearching = false;
                scroll = tabScrolls.getOrDefault(tab, 0f);
                scrollAnimation.setValue(scroll);
                activeTab = tab;
            } else if (tab != activeTab) {
                tabScrolls.put(activeTab, scroll);
                activeTab = tab;
                scroll = tabScrolls.getOrDefault(tab, 0f);
                scrollAnimation.setValue(scroll);
            }
        }

        if (isSplitTab(tab) && !searching) {
            renderCombatGroups(context, windowX, windowY, alpha, mouseX, mouseY, delta, tab);
            return;
        }

        List<ModuleComponent> visible = visible(tab, query);
        float contentX = ClickGuiLayout.contentX(windowX) + ClickGuiLayout.scaled(20f);
        float contentY = ClickGuiLayout.contentY(windowY) + ClickGuiLayout.scaled(20f);
        float areaW = ClickGuiLayout.contentWidth() - ClickGuiLayout.scaled(40f);
        float areaH = ClickGuiLayout.contentHeight() - ClickGuiLayout.scaled(24f);

        if (query == null || query.isEmpty()) {
            Fonts.PS_BOLD.drawGradientText(RenderUtil.matrices(), tab.getLabel().toUpperCase(),
                    contentX, contentY - ClickGuiLayout.scaled(13f), ClickGuiLayout.scaled(11f),
                    UIColors.primary((int) (alpha * 255f)), UIColors.secondary((int) (alpha * 255f)), 9f);
        }

        scrollAnimation.update();
        scrollAnimation.run(scroll, 220, Easing.EXPO_OUT);
        float offset = (float) scrollAnimation.getValue();

        float leftH = 0f;
        float rightH = 0f;
        ScissorUtil.start(RenderUtil.matrices(), contentX, contentY, areaW, areaH);

        for (int i = 0; i < visible.size(); i++) {
            boolean right = i % 2 == 1;
            ModuleComponent card = visible.get(i);
            float x = contentX + (right ? ClickGuiLayout.cardWidth() + ClickGuiLayout.cardGap() : 0f);
            float y = contentY + offset + (right ? rightH : leftH);

            card.setX(x);
            card.setY(y);
            card.setWidth(ClickGuiLayout.cardWidth());
            card.setAlpha(alpha);
            card.render(context, mouseX, mouseY, delta);

            if (right) rightH += card.getHeight() + ClickGuiLayout.rowGap();
            else leftH += card.getHeight() + ClickGuiLayout.rowGap();
        }

        ScissorUtil.stop(RenderUtil.matrices());

        float contentH = Math.max(leftH, rightH);
        scroll = Mth.clamp(scroll, Math.min(areaH - contentH, 0f), 0f);
        if (!searching) {
            tabScrolls.put(activeTab, scroll);
        }
    }

    private void renderCombatGroups(GuiGraphicsExtractor context, float windowX, float windowY, float alpha,
                                    int mouseX, int mouseY, float delta, ClickGuiTab tab) {
        CombatGroups groups = combatGroups(tab);
        float contentX = ClickGuiLayout.contentX(windowX) + ClickGuiLayout.scaled(20f);
        float contentY = ClickGuiLayout.contentY(windowY) + ClickGuiLayout.scaled(20f);
        float areaW = ClickGuiLayout.contentWidth() - ClickGuiLayout.scaled(40f);
        float areaH = ClickGuiLayout.contentHeight() - ClickGuiLayout.scaled(24f);
        float rightX = contentX + ClickGuiLayout.cardWidth() + ClickGuiLayout.cardGap();

        scrollAnimation.update();
        scrollAnimation.run(scroll, 220, Easing.EXPO_OUT);
        float offset = (float) scrollAnimation.getValue();

        float titleSize = ClickGuiLayout.scaled(11f);
        float titleLift = ClickGuiLayout.scaled(13f);
        int a = (int) (alpha * 255f);
        Fonts.PS_BOLD.drawGradientText(RenderUtil.matrices(), groups.leftTitle, contentX, contentY - titleLift + offset, titleSize,
                UIColors.primary(a), UIColors.secondary(a), 9f);
        Fonts.PS_BOLD.drawGradientText(RenderUtil.matrices(), groups.rightTitle, rightX, contentY - titleLift + offset, titleSize,
                UIColors.primary(a), UIColors.secondary(a), 9f);

        ScissorUtil.start(RenderUtil.matrices(), contentX, contentY - titleLift, areaW, areaH + titleLift);
        float combatH = renderColumn(context, groups.combat, contentX, contentY + offset, alpha, mouseX, mouseY, delta);
        float utilityH = renderColumn(context, groups.utility, rightX, contentY + offset, alpha, mouseX, mouseY, delta);

        float otherH = 0f;
        if (!groups.other.isEmpty()) {
            float otherTitleY = contentY + offset + combatH + ClickGuiLayout.scaled(12f);
            Fonts.PS_BOLD.drawGradientText(RenderUtil.matrices(), groups.otherTitle, contentX, otherTitleY, titleSize,
                    UIColors.primary(a), UIColors.secondary(a), 9f);
            otherH = renderColumn(context, groups.other, contentX, otherTitleY + titleLift, alpha, mouseX, mouseY, delta);
        }
        ScissorUtil.stop(RenderUtil.matrices());

        float leftH = combatH + (groups.other.isEmpty() ? 0f : ClickGuiLayout.scaled(12f) + titleLift + otherH);
        float contentH = Math.max(leftH, utilityH);
        scroll = Mth.clamp(scroll, Math.min(areaH - contentH, 0f), 0f);
        tabScrolls.put(activeTab, scroll);
    }

    private static float renderColumn(GuiGraphicsExtractor context, List<ModuleComponent> cards, float x, float y,
                                      float alpha, int mouseX, int mouseY, float delta) {
        float height = 0f;
        for (ModuleComponent card : cards) {
            card.setX(x);
            card.setY(y + height);
            card.setWidth(ClickGuiLayout.cardWidth());
            card.setAlpha(alpha);
            card.render(context, mouseX, mouseY, delta);
            height += card.getHeight() + ClickGuiLayout.rowGap();
        }
        return height;
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button, float windowX, float windowY, ClickGuiTab tab, String query) {
        if (!hovered(mouseX, mouseY, windowX, windowY)) return false;
        for (ModuleComponent card : visible(tab, query)) {
            if (MouseUtil.isHovered(mouseX, mouseY, card.getX(), card.getY(), card.getWidth(), card.getHeight())) {
                card.mouseClicked(mouseX, mouseY, button);
                return true;
            }
        }
        return false;
    }

    public void mouseReleased(double mouseX, double mouseY, int button, ClickGuiTab tab, String query) {
        for (ModuleComponent card : visible(tab, query)) {
            card.mouseReleased(mouseX, mouseY, button);
        }
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double amount, float windowX, float windowY) {
        if (!hovered(mouseX, mouseY, windowX, windowY)) return false;
        scroll += (float) (amount * ClickGuiLayout.scaled(16f));
        if (!wasSearching) {
            tabScrolls.put(activeTab, scroll);
        }
        return true;
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers, ClickGuiTab tab, String query) {
        for (ModuleComponent card : visible(tab, query)) {
            card.keyPressed(keyCode, scanCode, modifiers);
        }
        return false;
    }

    public boolean charTyped(char chr, int modifiers, ClickGuiTab tab, String query) {
        for (ModuleComponent card : visible(tab, query)) {
            card.charTyped(chr, modifiers);
        }
        return false;
    }

    private boolean hovered(double mouseX, double mouseY, float windowX, float windowY) {
        return MouseUtil.isHovered(mouseX, mouseY,
                ClickGuiLayout.contentX(windowX),
                ClickGuiLayout.contentY(windowY),
                ClickGuiLayout.contentWidth(),
                ClickGuiLayout.contentHeight());
    }

    private List<ModuleComponent> visible(ClickGuiTab tab, String query) {
        String filter = query == null ? "" : query.toLowerCase();
        if (isSplitTab(tab) && filter.isEmpty()) {
            CombatGroups groups = combatGroups(tab);
            List<ModuleComponent> result = new ArrayList<>(groups.combat);
            result.addAll(groups.other);
            result.addAll(groups.utility);
            return result;
        }
        List<Module> modules = ModuleManager.getInstance().getModules().stream()
                .filter(module -> !module.isHidden())
                .filter(module -> filter.isEmpty()
                        ? tab.isCategory() && module.getCategory() == tab.getCategory()
                        : module.getName().toLowerCase().contains(filter))
                .sorted(Comparator.comparing(Module::getName))
                .toList();

        List<ModuleComponent> result = new ArrayList<>();
        for (Module module : modules) {
            result.add(components.computeIfAbsent(module, ModuleComponent::new));
        }
        return result;
    }

    private CombatGroups combatGroups(ClickGuiTab tab) {
        if (tab != ClickGuiTab.COMBAT) {
            List<Module> modules = ModuleManager.getInstance().getModules().stream()
                    .filter(feature -> !feature.isHidden())
                    .filter(feature -> feature.getCategory() == tab.getCategory())
                    .sorted(Comparator.comparing(Module::getName))
                    .toList();
            List<ModuleComponent> left = new ArrayList<>();
            List<ModuleComponent> right = new ArrayList<>();
            List<ModuleComponent> lower = new ArrayList<>();
            for (Module feature : modules) {
                String name = feature.getName().replace(" ", "").toLowerCase();
                ModuleComponent component = components.computeIfAbsent(feature, ModuleComponent::new);
                if (tab == ClickGuiTab.RENDER) {
                    if (name.contains("hud") || name.contains("hand") || name.contains("view") || name.contains("swing")
                            || name.contains("hold") || name.contains("interface") || name.contains("click")) right.add(component);
                    else if (name.contains("world") || name.contains("sky") || name.contains("particle")
                            || name.contains("esp") || name.contains("trail") || name.contains("marker")) left.add(component);
                    else lower.add(component);
                } else if (tab == ClickGuiTab.MOVEMENT) {
                    if (name.contains("web") || name.contains("push") || name.contains("inventory") || name.contains("air")) {
                        right.add(component);
                    } else {
                        left.add(component);
                    }
                } else if (tab == ClickGuiTab.PLAYER) {
                    if (name.contains("swap") || name.contains("tool") || name.contains("leave")) right.add(component);
                    else left.add(component);
                } else {
                    if (left.size() <= right.size()) left.add(component);
                    else right.add(component);
                }
            }
            return switch (tab) {
                case RENDER -> new CombatGroups("WORLD", left, "INTERFACE", right, "OTHER", lower);
                case OTHER -> new CombatGroups("MAIN", left, "UTILITY", right, "OTHER", lower);
                default -> new CombatGroups("MAIN", left, "OTHER", right, "", lower);
            };
        }
        List<Module> combat = ModuleManager.getInstance().getModules().stream()
                .filter(feature -> !feature.isHidden())
                .filter(feature -> feature.getCategory() == tokyo.ghoul.api.module.Category.COMBAT
                        || feature.getName().equalsIgnoreCase("AutoSwap"))
                .sorted(Comparator.comparing(Module::getName))
                .toList();
        List<ModuleComponent> combatCards = new ArrayList<>();
        List<ModuleComponent> otherCards = new ArrayList<>();
        List<ModuleComponent> utilityCards = new ArrayList<>();
        for (Module feature : combat) {
            String name = feature.getName().replace(" ", "").toLowerCase();
            ModuleComponent component = components.computeIfAbsent(feature, ModuleComponent::new);
            if (name.equals("autoswap") || name.equals("autototem")) {
                otherCards.add(component);
            } else if (name.equals("autoexplosion") || name.equals("crystalaura")) {
                utilityCards.add(component);
            } else {
                combatCards.add(component);
            }
        }
        return new CombatGroups("COMBAT", combatCards, "UTILITY", utilityCards, "OTHER", otherCards);
    }

    private static boolean isSplitTab(ClickGuiTab tab) {
        return tab == ClickGuiTab.COMBAT || tab == ClickGuiTab.MOVEMENT || tab == ClickGuiTab.PLAYER
                || tab == ClickGuiTab.RENDER || tab == ClickGuiTab.OTHER;
    }

    private record CombatGroups(String leftTitle, List<ModuleComponent> combat, String rightTitle,
                                List<ModuleComponent> utility, String otherTitle, List<ModuleComponent> other) { }
}
