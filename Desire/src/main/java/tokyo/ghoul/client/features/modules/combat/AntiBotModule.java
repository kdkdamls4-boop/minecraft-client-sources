package tokyo.ghoul.client.features.modules.combat;

import lombok.Getter;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.core.component.DataComponents;
import tokyo.ghoul.api.event.Listener;
import tokyo.ghoul.api.event.EventListener;
import tokyo.ghoul.api.event.events.player.other.UpdateEvent;
import tokyo.ghoul.api.module.Category;
import tokyo.ghoul.api.module.Module;
import tokyo.ghoul.api.module.ModuleRegister;

import java.util.ArrayList;
import java.util.List;

@ModuleRegister(name = "Anti Bot", description = "Игнорирует ботов на сервере", category = Category.COMBAT)
public class AntiBotModule extends Module {
    @Getter private static final AntiBotModule instance = new AntiBotModule();

    private final List<Player> bots = new ArrayList<>();

    public AntiBotModule() {
    }

    @Override
    public void onEvent() {
        EventListener updateEvent = UpdateEvent.getInstance().subscribe(new Listener<>(event -> onUpdate()));
        addEvents(updateEvent);
    }

    private void onUpdate() {
        if (mc.player == null || mc.level == null) return;

        bots.removeIf(bot -> bot == null || !bot.isAlive()
                || !mc.level.players().contains(bot) || !isBotCandidate(bot));

        for (Player player : mc.level.players()) {
            if (player != null && player != mc.player && isBotCandidate(player) && !bots.contains(player)) {
                bots.add(player);
            }
        }
    }

    private boolean armorCheck(Player entity) {
        return getItemByArmorSlot(entity, EquipmentSlot.HEAD).is(Items.LEATHER_HELMET)
                && isNotColored(entity, EquipmentSlot.HEAD) && !getItemByArmorSlot(entity, EquipmentSlot.HEAD).isEnchanted()
                || getItemByArmorSlot(entity, EquipmentSlot.CHEST).is(Items.LEATHER_CHESTPLATE)
                && isNotColored(entity, EquipmentSlot.CHEST) && !getItemByArmorSlot(entity, EquipmentSlot.CHEST).isEnchanted()
                || getItemByArmorSlot(entity, EquipmentSlot.LEGS).is(Items.LEATHER_LEGGINGS)
                && isNotColored(entity, EquipmentSlot.LEGS) && !getItemByArmorSlot(entity, EquipmentSlot.LEGS).isEnchanted()
                || getItemByArmorSlot(entity, EquipmentSlot.FEET).is(Items.LEATHER_BOOTS)
                && isNotColored(entity, EquipmentSlot.FEET) && !getItemByArmorSlot(entity, EquipmentSlot.FEET).isEnchanted()
                || getItemByArmorSlot(entity, EquipmentSlot.CHEST).is(Items.IRON_CHESTPLATE)
                && !getItemByArmorSlot(entity, EquipmentSlot.CHEST).isEnchanted()
                || getItemByArmorSlot(entity, EquipmentSlot.LEGS).is(Items.IRON_LEGGINGS)
                && !getItemByArmorSlot(entity, EquipmentSlot.LEGS).isEnchanted();
    }

    private ItemStack getItemByArmorSlot(Player entity, EquipmentSlot slot) {
        return entity.getItemBySlot(slot);
    }

    private boolean isNotColored(Player entity, EquipmentSlot slot) {
        return !getItemByArmorSlot(entity, slot).has(DataComponents.DYED_COLOR);
    }

    private boolean isNaked(Player entity) {
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            if (!entity.getItemBySlot(slot).isEmpty()) return false;
        }
        return true;
    }

    private boolean isInPlayerList(Player entity) {
        return mc.getConnection() != null
                && mc.getConnection().getPlayerInfo(entity.getUUID()) != null;
    }

    private boolean isBotCandidate(Player entity) {
        return (entity.isInvisible() && isNaked(entity) && !isInPlayerList(entity))
                || armorCheck(entity);
    }

    public static boolean isBot(Player player) {
        AntiBotModule module = instance;
        if (module == null || player == null || player == mc.player) return false;
        if (!module.isEnabled()) return false;

        if (module.isBotCandidate(player)) {
            if (!module.bots.contains(player)) module.bots.add(player);
            return true;
        }

        module.bots.remove(player);
        return false;
    }
}
