package tokyo.ghoul.client.features.modules.misc.automine;

import net.minecraft.client.player.ClientInput;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec2;

final class MineInput extends ClientInput {
    boolean forward;
    boolean jump;
    boolean sprint;
    boolean sneak;

    @Override
    public void tick() {
        keyPresses = new Input(forward, false, false, false, jump, sneak, sprint && !sneak);
        moveVector = new Vec2(0.0F, forward ? 1.0F : 0.0F);
    }

    void release() {
        forward = false;
        jump = false;
        sprint = false;
        sneak = false;
    }
}
