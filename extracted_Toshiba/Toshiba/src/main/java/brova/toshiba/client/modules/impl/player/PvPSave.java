package brova.toshiba.client.modules.impl.player;

import brova.toshiba.client.modules.Module;
import brova.toshiba.client.modules.Module.ModuleCategory;
import brova.toshiba.client.utils.text.ChatUtil;
import java.lang.reflect.Field;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.gui.hud.BossBarHud;
import net.minecraft.client.gui.hud.ClientBossBar;

public class PvPSave extends Module {

    private boolean stateFlag;

    public PvPSave() {
        super("PvPSave", "РџСЂРµРґРѕС‚РІСЂР°С‰Р°РµС‚ РІС‹С…РѕРґ РІРѕ РІСЂРµРјСЏ PvP", ModuleCategory.PLAYER);
    }

    public boolean shouldBlockHub(String text) {
        if (!this.isEnable() || !this.isTrimWithStartsWith(text)) {
            return false;
        } else if (!isCombatActive()) {
            this.stateFlag = false;
            return false;
        } else if (this.stateFlag) {
            this.stateFlag = false;
            return false;
        } else {
            this.stateFlag = true;
            ChatUtil.sendMessage("Р РµР¶РёРј Р±РѕСЏ Р°РєС‚РёРІРµРЅ. Р§С‚РѕР±С‹ РІС‹Р№С‚Рё РЅР° С…Р°Р±, РїРѕРІС‚РѕСЂРёС‚Рµ РєРѕРјР°РЅРґСѓ /hub.");
            return true;
        }
    }

    public boolean shouldConfirmDisconnect() {
        return this.isEnable() && isCombatActive();
    }

    @Override
    public void onDisable() {
        this.stateFlag = false;
        super.onDisable();
    }

    public static boolean isCombatActive() {
        if (mc.inGameHud == null) {
            return false;
        } else {
            try {
                BossBarHud bossBarHud = mc.inGameHud.getBossBarHud();
                for (Field field : bossBarHud.getClass().getDeclaredFields()) {
                    if (Map.class.isAssignableFrom(field.getType())) {
                        field.setAccessible(true);
                        Map<?, ?> entries = (Map<?, ?>) field.get(bossBarHud);
                        return entries.values().stream().filter(ClientBossBar.class::isInstance).map(ClientBossBar.class::cast).map(input -> input.getName().getString().toLowerCase(Locale.ROOT)).anyMatch(inputValue -> inputValue.contains("pvp") || inputValue.contains("РїРІРї") || inputValue.contains("Р±РѕСЏ") || inputValue.contains("Р±РѕР№"));
                    }
                }
            } catch (ReflectiveOperationException failure) {
            }
            return false;
        }
    }

    private boolean isTrimWithStartsWith(String text) {
        String textValue = text.trim();
        if (textValue.startsWith("/")) {
            textValue = textValue.substring(1);
        }
        return textValue.equalsIgnoreCase("hub");
    }
}
