package tokyo.ghoul.client.features.modules.render.nametags;

import lombok.Getter;
import tokyo.ghoul.api.event.EventListener;
import tokyo.ghoul.api.event.Listener;
import tokyo.ghoul.api.event.events.render.Render2DEvent;
import tokyo.ghoul.api.module.Category;
import tokyo.ghoul.api.module.Module;
import tokyo.ghoul.api.module.ModuleRegister;
import tokyo.ghoul.api.module.setting.BooleanSetting;
import tokyo.ghoul.api.module.setting.MultiBooleanSetting;

@ModuleRegister(name = "Name Tags", description = "Улучшенные ники над игроками", category = Category.RENDER)
public class NameTagsModule extends Module {
    @Getter private static final NameTagsModule instance = new NameTagsModule();

    public final MultiBooleanSetting rendering = new MultiBooleanSetting("Отображать").value(
            new BooleanSetting("Игроков").value(true),
            new BooleanSetting("Монстров").value(false),
            new BooleanSetting("Друзей").value(true),
            new BooleanSetting("Животных").value(false),
            new BooleanSetting("Себя").value(false),
            new BooleanSetting("Жителей").value(false),
            new BooleanSetting("Голых").value(false),
            new BooleanSetting("Предметы").value(true)
    );
    public final BooleanSetting liquidGlass = new BooleanSetting("Жидкое стекло").value(false);
    public final BooleanSetting viewHead = new BooleanSetting("Отображать голову").value(true);
    public final BooleanSetting viewEffects = new BooleanSetting("Отображать эффекты").value(false);
    public final BooleanSetting viewArmor = new BooleanSetting("Отображать броню").value(false);
    public final BooleanSetting viewEnchants = new BooleanSetting("Отображать зачарования").value(false);
    public final BooleanSetting viewItem = new BooleanSetting("Отображать предметы").value(false);
    public final BooleanSetting viewDisplayName = new BooleanSetting("Отображать название предмета").value(false)
            .setVisible(viewItem::getValue);

    private final NameTagsRender nameTagsRender = new NameTagsRender(this);

    public NameTagsModule() {
        addSettings(rendering, liquidGlass, viewHead, viewEffects, viewArmor, viewEnchants, viewItem, viewDisplayName);
    }

    public static boolean shouldHideVanillaTag() {
        return instance.isEnabled();
    }

    @Override
    public void onEvent() {
        EventListener render2DEvent = Render2DEvent.getInstance().subscribe(new Listener<>(1, event -> {
            nameTagsRender.onRender(event);
        }));
        addEvents(render2DEvent);
    }
}
