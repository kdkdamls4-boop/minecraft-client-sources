package tokyo.ghoul.api.utils.combat;

import lombok.experimental.UtilityClass;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import tokyo.ghoul.api.system.interfaces.QuickImports;

@UtilityClass
public class PredictUtils implements QuickImports {

    public Vec3 predictGround(LivingEntity entity, float ticks) {
        if (entity == null) return Vec3.ZERO;

        Vec3 forward = new Vec3(
                entity.getX() - entity.xOld,
                (entity.getY() - entity.yOld) / 8.0,
                entity.getZ() - entity.zOld
        ).scale(ticks);

        return entity.position().add(forward);
    }
}
