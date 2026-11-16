package tokyo.ghoul.client.features.modules.misc;

import lombok.Getter;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Score;
import net.minecraft.world.scores.Scoreboard;
import tokyo.ghoul.api.module.Category;
import tokyo.ghoul.api.module.Module;
import tokyo.ghoul.api.module.ModuleRegister;

@Getter
@ModuleRegister(name = "Scoreboard Health", description = "Показывает здоровье из скорборда", category = Category.OTHER)
public class ScoreboardHealthModule extends Module {
    @Getter private static final ScoreboardHealthModule instance = new ScoreboardHealthModule();

    public ScoreboardHealthModule() {
    }

    @Override
    public void onEvent() {
    }

    public static float getHealth(LivingEntity entity) {
        ScoreboardHealthModule module = instance;
        if (module == null || !module.isEnabled() || mc.level == null) {
            return entity.getHealth();
        }

        try {
            Scoreboard scoreboard = mc.level.getScoreboard();
            Objective objective = scoreboard.getDisplayObjective(DisplaySlot.BELOW_NAME);
            if (objective == null || !"health".equals(objective.getCriteria().getName())) {
                return entity.getHealth();
            }

            net.minecraft.world.scores.ScoreAccess score = scoreboard.getOrCreatePlayerScore(entity, objective);
            return Math.max(score.get(), entity.getHealth());
        } catch (Exception ignored) {
            return entity.getHealth();
        }
    }
}
