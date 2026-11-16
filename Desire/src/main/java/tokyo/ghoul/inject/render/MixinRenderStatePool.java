package tokyo.ghoul.inject.render;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import tokyo.ghoul.api.system.interfaces.IRenderStatePool;

@Mixin({Entity.class, BlockEntity.class})
public abstract class MixinRenderStatePool implements IRenderStatePool {
    @Unique private Object desire$stateOwner;
    @Unique private Object desire$state;

    @Override
    public Object desire$pooledState(Object owner) {
        return desire$stateOwner == owner ? desire$state : null;
    }

    @Override
    public void desire$poolState(Object owner, Object state) {
        desire$stateOwner = owner;
        desire$state = state;
    }
}
