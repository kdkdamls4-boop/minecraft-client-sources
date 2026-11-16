package tokyo.ghoul.api.utils.player;

import lombok.experimental.UtilityClass;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import tokyo.ghoul.api.system.interfaces.QuickImports;
import tokyo.ghoul.api.utils.rotation.manager.Rotation;

import java.util.Optional;

@UtilityClass
public class DirectionUtil implements QuickImports {

    public Direction getFaceDirection(Player player, BlockPos targetPos) {
        if (player == null) return Direction.UP;
        Vec3 eyePos = player.getEyePosition();
        Vec3 lookVec = player.getViewVector(1.0F);
        return calculateFaceDirection(eyePos, lookVec, targetPos);
    }

    public Direction getFaceDirection(Rotation rotation, Player player, BlockPos targetPos) {
        if (player == null) return Direction.UP;
        Vec3 eyePos = player.getEyePosition();
        Vec3 lookVec = Vec3.directionFromRotation(rotation.getXRot(), rotation.getYRot());
        return calculateFaceDirection(eyePos, lookVec, targetPos);
    }

    private Direction calculateFaceDirection(Vec3 eyePos, Vec3 lookVec, BlockPos targetPos) {
        double cx = targetPos.getX() + 0.5;
        double cy = targetPos.getY() + 0.5;
        double cz = targetPos.getZ() + 0.5;

        double minX = targetPos.getX();
        double minY = targetPos.getY();
        double minZ = targetPos.getZ();
        double maxX = targetPos.getX() + 1.0;
        double maxY = targetPos.getY() + 1.0;
        double maxZ = targetPos.getZ() + 1.0;

        Direction closestFace = null;
        double closestT = Double.MAX_VALUE;

        double t = tryHitPlaneY(eyePos, lookVec, maxY, minX, maxX, minZ, maxZ);
        if (t > 0 && t < closestT) {
            closestT = t;
            closestFace = Direction.UP;
        }

        t = tryHitPlaneY(eyePos, lookVec, minY, minX, maxX, minZ, maxZ);
        if (t > 0 && t < closestT) {
            closestT = t;
            closestFace = Direction.DOWN;
        }

        t = tryHitPlaneZ(eyePos, lookVec, minZ, minX, maxX, minY, maxY);
        if (t > 0 && t < closestT) {
            closestT = t;
            closestFace = Direction.NORTH;
        }

        t = tryHitPlaneZ(eyePos, lookVec, maxZ, minX, maxX, minY, maxY);
        if (t > 0 && t < closestT) {
            closestT = t;
            closestFace = Direction.SOUTH;
        }

        t = tryHitPlaneX(eyePos, lookVec, minX, minY, maxY, minZ, maxZ);
        if (t > 0 && t < closestT) {
            closestT = t;
            closestFace = Direction.WEST;
        }

        t = tryHitPlaneX(eyePos, lookVec, maxX, minY, maxY, minZ, maxZ);
        if (t > 0 && t < closestT) {
            closestT = t;
            closestFace = Direction.EAST;
        }

        if (closestFace == null) {
            closestFace = getFallbackDirection(eyePos, cx, cy, cz);
        }

        return closestFace;
    }

    public BlockHitResult raycastBlock(Rotation rotation, BlockPos targetPos, double maxDistance) {
        if (mc.player == null) return null;
        return raycastBlock(rotation, mc.player, targetPos, maxDistance);
    }

    public BlockHitResult raycastBlock(Rotation rotation, Player player, BlockPos targetPos, double maxDistance) {
        if (player == null) return null;
        Vec3 eyePos = player.getEyePosition();
        Vec3 lookVec = Vec3.directionFromRotation(rotation.getXRot(), rotation.getYRot());
        Vec3 endPos = eyePos.add(lookVec.scale(maxDistance));

        AABB blockBox = new AABB(targetPos);
        Optional<Vec3> hitOpt = blockBox.clip(eyePos, endPos);
        if (hitOpt.isEmpty()) {
            return null;
        }

        Vec3 hitVec = hitOpt.get();
        Direction side = getHitSide(hitVec, targetPos);
        return new BlockHitResult(hitVec, side, targetPos, false);
    }

    public Direction getHitSide(Vec3 hitVec, BlockPos blockPos) {
        double dx = hitVec.x - (blockPos.getX() + 0.5);
        double dy = hitVec.y - (blockPos.getY() + 0.5);
        double dz = hitVec.z - (blockPos.getZ() + 0.5);

        double absDx = Math.abs(dx);
        double absDy = Math.abs(dy);
        double absDz = Math.abs(dz);

        if (absDx > absDy && absDx > absDz) {
            return dx > 0 ? Direction.EAST : Direction.WEST;
        } else if (absDy > absDz) {
            return dy > 0 ? Direction.UP : Direction.DOWN;
        } else {
            return dz > 0 ? Direction.SOUTH : Direction.NORTH;
        }
    }

    private double tryHitPlaneY(Vec3 eye, Vec3 dir, double yVal,
                                double minX, double maxX,
                                double minZ, double maxZ) {
        if (dir.y == 0) return -1;
        double t = (yVal - eye.y) / dir.y;
        if (t <= 0) return -1;
        double hitX = eye.x + t * dir.x;
        double hitZ = eye.z + t * dir.z;
        if (hitX >= minX && hitX <= maxX && hitZ >= minZ && hitZ <= maxZ) {
            return t;
        }
        return -1;
    }

    private double tryHitPlaneZ(Vec3 eye, Vec3 dir, double zVal,
                                double minX, double maxX,
                                double minY, double maxY) {
        if (dir.z == 0) return -1;
        double t = (zVal - eye.z) / dir.z;
        if (t <= 0) return -1;
        double hitX = eye.x + t * dir.x;
        double hitY = eye.y + t * dir.y;
        if (hitX >= minX && hitX <= maxX && hitY >= minY && hitY <= maxY) {
            return t;
        }
        return -1;
    }

    private double tryHitPlaneX(Vec3 eye, Vec3 dir, double xVal,
                                double minY, double maxY,
                                double minZ, double maxZ) {
        if (dir.x == 0) return -1;
        double t = (xVal - eye.x) / dir.x;
        if (t <= 0) return -1;
        double hitY = eye.y + t * dir.y;
        double hitZ = eye.z + t * dir.z;
        if (hitY >= minY && hitY <= maxY && hitZ >= minZ && hitZ <= maxZ) {
            return t;
        }
        return -1;
    }

    private Direction getFallbackDirection(Vec3 eye, double cx, double cy, double cz) {
        double dx = eye.x - cx;
        double dy = eye.y - cy;
        double dz = eye.z - cz;

        double ax = Math.abs(dx);
        double ay = Math.abs(dy);
        double az = Math.abs(dz);

        if (ay >= ax && ay >= az) {
            return dy > 0 ? Direction.UP : Direction.DOWN;
        } else if (ax >= az) {
            return dx > 0 ? Direction.EAST : Direction.WEST;
        } else {
            return dz > 0 ? Direction.SOUTH : Direction.NORTH;
        }
    }
}
