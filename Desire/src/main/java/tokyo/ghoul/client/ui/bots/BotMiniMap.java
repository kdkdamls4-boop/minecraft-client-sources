package tokyo.ghoul.client.ui.bots;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.MapColor;

public class BotMiniMap {
    public static final int SIZE = 64;
    private static final long UPDATE_MS = 400L;
    private static final int UNLOADED = 0xFF0B0E16;

    private static int counter;

    private final Identifier id;
    private final DynamicTexture texture;
    private long lastUpdateMs;

    public BotMiniMap() {
        this.id = Identifier.fromNamespaceAndPath("desire", "bots/minimap_" + (counter++));
        this.texture = new DynamicTexture(this.id::toString, SIZE, SIZE, true);
        Minecraft.getInstance().getTextureManager().register(this.id, this.texture);
    }

    public Identifier update(ClientLevel level, Entity center) {
        long now = System.currentTimeMillis();
        if (now - this.lastUpdateMs < UPDATE_MS) {
            return this.id;
        }
        this.lastUpdateMs = now;

        NativeImage image = this.texture.getPixels();
        if (image == null) {
            return this.id;
        }

        int originX = center.getBlockX() - SIZE / 2;
        int originZ = center.getBlockZ() - SIZE / 2;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int x = 0; x < SIZE; x++) {
            int worldX = originX + x;
            int northY = surfaceY(level, worldX, originZ - 1);
            for (int z = 0; z < SIZE; z++) {
                int worldZ = originZ + z;
                if (!level.hasChunk(SectionPos.blockToSectionCoord(worldX), SectionPos.blockToSectionCoord(worldZ))) {
                    image.setPixel(x, z, UNLOADED);
                    northY = Integer.MIN_VALUE;
                    continue;
                }

                int y = surfaceY(level, worldX, worldZ);
                MapColor color = MapColor.NONE;
                int depth = 0;
                while (y >= level.getMinY() && depth < 24) {
                    pos.set(worldX, y, worldZ);
                    BlockState state = level.getBlockState(pos);
                    color = state.getMapColor(level, pos);
                    if (color != MapColor.NONE) {
                        break;
                    }
                    y--;
                    depth++;
                }

                MapColor.Brightness brightness;
                if (northY == Integer.MIN_VALUE || y == northY) {
                    brightness = MapColor.Brightness.NORMAL;
                } else {
                    brightness = y > northY ? MapColor.Brightness.HIGH : MapColor.Brightness.LOW;
                }
                image.setPixel(x, z, color == MapColor.NONE ? UNLOADED : color.calculateARGBColor(brightness));
                northY = y;
            }
        }

        this.texture.upload();
        return this.id;
    }

    private static int surfaceY(ClientLevel level, int x, int z) {
        return level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1;
    }

    public Identifier id() {
        return this.id;
    }

    public void release() {
        Minecraft.getInstance().getTextureManager().release(this.id);
    }
}
