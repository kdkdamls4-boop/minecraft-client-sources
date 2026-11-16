package tokyo.ghoul.client.features.modules.combat;

import lombok.Getter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import tokyo.ghoul.api.event.EventListener;
import tokyo.ghoul.api.event.Listener;
import tokyo.ghoul.api.event.events.player.world.AttackEvent;
import tokyo.ghoul.api.module.Category;
import tokyo.ghoul.api.module.Module;
import tokyo.ghoul.api.module.ModuleRegister;
import tokyo.ghoul.api.system.configs.FriendManager;

@ModuleRegister(name = "No Friend Damage", description = "Не даёт бить друзей", category = Category.COMBAT)
public class NoFriendDamageModule extends Module {
    @Getter private static final NoFriendDamageModule instance = new NoFriendDamageModule();

    @Override
    public void onEvent() {
        EventListener attack = AttackEvent.getInstance().subscribe(new Listener<>(1000, this::onAttack));
        addEvents(attack);
    }

    private void onAttack(AttackEvent.AttackEventData event) {
        Entity entity = event.entity();
        if (!(entity instanceof Player player)) return;
        if (!FriendManager.getInstance().contains(player.getGameProfile().name())) return;

        AuraModule aura = AuraModule.getInstance();
        if (aura.isEnabled() && aura.target == entity) return;

        AttackEvent.getInstance().setCancel(true);
    }
}
