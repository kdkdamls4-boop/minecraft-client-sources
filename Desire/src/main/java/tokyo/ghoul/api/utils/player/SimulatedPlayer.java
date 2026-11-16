package tokyo.ghoul.api.utils.player;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import tokyo.ghoul.api.system.interfaces.QuickImports;

import java.util.List;

public class SimulatedPlayer implements QuickImports {
    public Vec3 pos;
    public Vec3 velocity;
    public double fallDistance;
    public boolean isFallFlying;
    public boolean onGround;
    public boolean horizontalCollision;
    public AABB boundingBox;

    public static SimulatedPlayer fromClientPlayer(net.minecraft.world.entity.player.Input ignored) {
        SimulatedPlayer sim = new SimulatedPlayer();
        sim.pos = mc.player.position();
        sim.velocity = mc.player.getDeltaMovement();
        sim.fallDistance = mc.player.fallDistance;
        sim.isFallFlying = mc.player.isFallFlying();
        sim.onGround = mc.player.onGround();
        sim.horizontalCollision = mc.player.horizontalCollision;
        sim.boundingBox = mc.player.getBoundingBox();
        return sim;
    }

    public static SimulatedPlayer simulateLocalPlayer(int ticks) {
        SimulatedPlayer sim = fromClientPlayer(mc.player.input.keyPresses);
        for (int i = 0; i < ticks; i++) {
            sim.tick();
        }
        return sim;
    }

    public void tick() {
        if (onGround) {
            isFallFlying = false;
        }

        double gravity = isFallFlying ? 0.05 : 0.08;
        if (!onGround) {
            velocity = velocity.add(0.0, -gravity, 0.0);
        }

        Vec3 before = velocity;
        Vec3 movement = collide(velocity);
        horizontalCollision = Math.abs(movement.x - before.x) > 1.0E-5 || Math.abs(movement.z - before.z) > 1.0E-5;
        pos = pos.add(movement);
        velocity = new Vec3(movement.x, movement.y, movement.z);
        if (mc.player != null) {
            boundingBox = mc.player.getDimensions(mc.player.getPose()).makeBoundingBox(pos);
        }

        if (velocity.y < 0.0) {
            fallDistance += Math.max(0.0, -velocity.y);
        }

        Vec3 down = collide(new Vec3(0.0, -0.001, 0.0));
        onGround = velocity.y <= 0.0 && down.y > -0.001;
        if (onGround) {
            velocity = new Vec3(velocity.x * 0.6, 0.0, velocity.z * 0.6);
            fallDistance = 0.0;
        } else {
            velocity = velocity.multiply(0.99, 0.98, 0.99);
        }
    }

    private Vec3 collide(Vec3 movement) {
        AABB box = mc.player.getDimensions(mc.player.getPose()).makeBoundingBox(pos);
        return Entity.collideBoundingBox(mc.player, movement, box, mc.level, List.of());
    }
}
