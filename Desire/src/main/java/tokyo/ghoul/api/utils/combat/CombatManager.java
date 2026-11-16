package tokyo.ghoul.api.utils.combat;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.EntityHitResult;
import tokyo.ghoul.api.system.interfaces.QuickImports;
import tokyo.ghoul.api.utils.player.PlayerUtil;
import tokyo.ghoul.api.utils.rotation.RaytracingUtil;
import tokyo.ghoul.api.utils.rotation.rotations.FunTimeRotation;

@Getter
@Accessors(fluent = true, chain = true)
public class CombatManager implements QuickImports {
    private final ClickScheduler clickScheduler = new ClickScheduler();
    private final ShieldBreakManager shieldBreakManager = new ShieldBreakManager();
    private final CriticalHitHandler criticalHitHandler = new CriticalHitHandler();

    @Getter @Setter
    private CombatExecutor.CombatConfigurable configurable;

    public void handleAttack() {
        FunTimeRotation.updateAttackState(canAttack());

        if (canAttack()) {
            if (isRaytraceFailed(getRaytraceEntity())) return;

            if (mc.player.isBlocking() && configurable.alwaysShield) {
                mc.gameMode.releaseUsingItem(mc.player);
            }

            mc.gameMode.attack(mc.player, configurable.target);
            mc.player.swing(InteractionHand.MAIN_HAND);
            clickScheduler.recalculate(500);

            FunTimeRotation.attackCount++;
        }
    }

    public boolean canAttack() {
        if (isCooldownNotComplete()) return false;
        if (configurable.noAttackIfEat && PlayerUtil.isEating()) return false;

        if (configurable.shieldBreak && configurable.target instanceof Player player && shieldBreakManager.shouldBreakShield(player)) {
            return true;
        }

        if (!configurable.onlyCrits) return true;

        return criticalHitHandler.canAttack(0, configurable.smartCrits);
    }

    private boolean isRaytraceFailed(Entity targetEntity) {
        return configurable.raytrace && targetEntity != configurable.target && !mc.player.isFallFlying();
    }

    private boolean isCooldownNotComplete() {
        return !clickScheduler.isCooldownComplete();
    }

    private Entity getRaytraceEntity() {
        EntityHitResult entityHitResult = RaytracingUtil.raytraceEntity(
                configurable.distance,
                configurable.rotation,
                configurable.ignoreWalls
        );

        if (entityHitResult != null) {
            return entityHitResult.getEntity();
        }

        return null;
    }
}
