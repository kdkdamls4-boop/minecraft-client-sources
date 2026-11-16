package tokyo.ghoul.client.ui.widget;

import lombok.Getter;
import net.minecraft.client.gui.screens.ChatScreen;
import tokyo.ghoul.api.event.Listener;
import tokyo.ghoul.api.event.events.render.Render2DEvent;
import tokyo.ghoul.api.system.interfaces.QuickImports;
import tokyo.ghoul.client.features.modules.render.InterfaceModule;
import tokyo.ghoul.client.ui.widget.overlay.*;

import java.util.ArrayList;
import java.util.List;

@Getter
public class WidgetManager implements QuickImports {
    @Getter private final static WidgetManager instance = new WidgetManager();

    private final List<Widget> widgets = new ArrayList<>();

    public void load() {
        register(
                new WatermarkWidget(),
                new KeybindsWidget(),
                new PotionsWidget(),
                new StaffsWidget(),
                new CooldownsWidget(),
                new CoordsWidget(),
                new MusicWidget(),
                new MusicMiniWidget(),
                NotifyWidget.getInstance(),

                new TargetInfoWidget(),
                new ScoreboardWidget(),
                new HotbarWidget(),
                new AutoBuyReportWidget()
        );

        tokyo.ghoul.client.ui.notifications.NotifyManager.getInstance().init();

        InterfaceModule.getInstance().init();

        Render2DEvent.getInstance().subscribe(new Listener<>(event -> {
            if (!InterfaceModule.getInstance().isEnabled()) return;

            var screen = mc.gui.screen();
            if (screen != null && !(screen instanceof ChatScreen)) return;

            HudGrid.render(event.matrixStack());

            for (Widget widget : widgets) {
                if (widget.isEnabled()) widget.render(event);
            }
        }));
    }

    public void register(Widget... widgets) {
        this.widgets.addAll(List.of(widgets));
    }
}
