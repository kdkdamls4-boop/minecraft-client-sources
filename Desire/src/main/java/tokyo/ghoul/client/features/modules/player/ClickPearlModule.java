package tokyo.ghoul.client.features.modules.player;

import lombok.Getter;
import net.minecraft.world.item.Items;
import tokyo.ghoul.api.event.Listener;
import tokyo.ghoul.api.event.EventListener;
import tokyo.ghoul.api.event.events.client.KeyEvent;
import tokyo.ghoul.api.event.events.client.TickEvent;
import tokyo.ghoul.api.event.events.player.other.UpdateEvent;
import tokyo.ghoul.api.module.Category;
import tokyo.ghoul.api.module.Module;
import tokyo.ghoul.api.module.ModuleRegister;
import tokyo.ghoul.api.module.setting.BindSetting;
import tokyo.ghoul.api.module.setting.ModeSetting;
import tokyo.ghoul.api.utils.player.InventoryUtil;

@Getter
@ModuleRegister(name = "Click Pearl", description = "Бросок жемчуга по бинду", category = Category.PLAYER)
public class ClickPearlModule extends Module {
    @Getter private static final ClickPearlModule instance = new ClickPearlModule();

    private final ModeSetting mode = new ModeSetting("Мод").value("Обычный").values("Обычный", "Легитный");
    private final BindSetting key = new BindSetting("Клавиша броска").value(-999);

    private boolean pearlUsed;
    private int ticksExisted;

    public ClickPearlModule() {
        addSettings(mode, key);
    }

    @Override
    public void onEvent() {
        EventListener keyEvent = KeyEvent.getInstance().subscribe(new Listener<>(event -> {
            if (event.action() == 0) return;
            int pressed = event.mouse() ? -100 + event.key() : event.key();
            if (pressed == key.getValue()) {
                if (mode.is("Обычный")) InventoryUtil.swapAndUseHvH(Items.ENDER_PEARL);
                else pearlUsed = true;
            }
        }));

        EventListener updateEvent = UpdateEvent.getInstance().subscribe(new Listener<>(event -> {
            if (mc.player == null) return;
            if (!pearlUsed && ticksExisted > 0) ticksExisted--;
            if (pearlUsed || ticksExisted > 0) mc.player.setSprinting(false);
        }));

        EventListener tickEvent = TickEvent.getInstance().subscribe(new Listener<>(event -> {
            if (mc.player == null || !pearlUsed) return;

            int slotHotbar = InventoryUtil.searchItem(Items.ENDER_PEARL, 0, 9);
            if (slotHotbar != -1) {
                InventoryUtil.swapAndUseLegit(Items.ENDER_PEARL);
            } else {
                if (ticksExisted == 0) {
                    ticksExisted++;
                    return;
                }
                InventoryUtil.swapAndUseLegit(Items.ENDER_PEARL);
                ticksExisted = 2;
            }
            pearlUsed = false;
        }));

        addEvents(keyEvent, updateEvent, tickEvent);
    }
}
