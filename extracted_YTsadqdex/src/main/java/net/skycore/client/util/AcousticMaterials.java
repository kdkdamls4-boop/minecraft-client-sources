package net.skycore.client.util;

import java.util.Set;
import net.minecraft.world.level.block.SoundType;

public final class AcousticMaterials {
   public static final String HIGH_OBSTRUCTION = "minecraft:black_wool,minecraft:blue_wool,minecraft:brown_wool,minecraft:cyan_wool,minecraft:gray_wool,minecraft:green_wool,minecraft:light_blue_wool,minecraft:light_gray_wool,minecraft:lime_wool,minecraft:magenta_wool,minecraft:orange_wool,minecraft:pink_wool,minecraft:purple_wool,minecraft:red_wool,minecraft:white_wool,minecraft:yellow_wool,minecraft:sponge,minecraft:wet_sponge";
   public static final Set<SoundType> LOW_REVERB = Set.of(
      SoundType.WOOD,
      SoundType.GRASS,
      SoundType.LILY_PAD,
      SoundType.WOOL,
      SoundType.SNOW,
      SoundType.POWDER_SNOW,
      SoundType.SLIME_BLOCK,
      SoundType.HONEY_BLOCK,
      SoundType.WET_GRASS,
      SoundType.SWEET_BERRY_BUSH,
      SoundType.CROP,
      SoundType.STEM,
      SoundType.VINE,
      SoundType.NETHER_WART,
      SoundType.ROOTS,
      SoundType.SHROOMLIGHT,
      SoundType.WEEPING_VINES,
      SoundType.CAVE_VINES,
      SoundType.SPORE_BLOSSOM,
      SoundType.AZALEA,
      SoundType.FLOWERING_AZALEA,
      SoundType.MOSS_CARPET,
      SoundType.PINK_PETALS,
      SoundType.MOSS,
      SoundType.BIG_DRIPLEAF,
      SoundType.SMALL_DRIPLEAF,
      SoundType.AZALEA_LEAVES,
      SoundType.SCULK_SENSOR,
      SoundType.SCULK_CATALYST,
      SoundType.SCULK,
      SoundType.SCULK_VEIN,
      SoundType.SCULK_SHRIEKER,
      SoundType.MANGROVE_ROOTS,
      SoundType.MUDDY_MANGROVE_ROOTS,
      SoundType.MUD,
      SoundType.MUD_BRICKS,
      SoundType.PACKED_MUD,
      SoundType.BAMBOO_WOOD,
      SoundType.NETHER_WOOD,
      SoundType.CHERRY_WOOD,
      SoundType.CHERRY_LEAVES,
      SoundType.CHISELED_BOOKSHELF,
      SoundType.SPONGE,
      SoundType.WET_SPONGE
   );
   public static final String IGNORED_EVENTS = "minecraft:music.creative,minecraft:music.credits,minecraft:music.dragon,minecraft:music.end,minecraft:music.game,minecraft:music.menu,minecraft:music.nether.basalt_deltas,minecraft:music.nether.nether_wastes,minecraft:music.nether.soul_sand_valley,minecraft:music.nether.crimson_forest,minecraft:music.nether.warped_forest,minecraft:music.under_water,minecraft:music.overworld.deep_dark,minecraft:music.overworld.dripstone_caves,minecraft:music.overworld.frozen_peaks,minecraft:music.overworld.grove,minecraft:music.overworld.jagged_peaks,minecraft:music.overworld.jungle_and_forest,minecraft:music.overworld.lush_caves,minecraft:music.overworld.meadow,minecraft:music.overworld.old_growth_taiga,minecraft:music.overworld.snowy_slopes,minecraft:music.overworld.stony_peaks,minecraft:music.overworld.swamp,minecraft:ui.button.click,minecraft:ui.toast.challenge_complete,minecraft:ui.toast.in,minecraft:ui.toast.out";
   public static final Set<SoundType> HIGH_REVERB = Set.of(
      SoundType.STONE,
      SoundType.METAL,
      SoundType.GLASS,
      SoundType.BASALT,
      SoundType.NETHER_BRICKS,
      SoundType.BONE_BLOCK,
      SoundType.NETHERITE_BLOCK,
      SoundType.ANCIENT_DEBRIS,
      SoundType.LODESTONE,
      SoundType.GILDED_BLACKSTONE,
      SoundType.AMETHYST,
      SoundType.TUFF,
      SoundType.CALCITE,
      SoundType.DRIPSTONE_BLOCK,
      SoundType.POINTED_DRIPSTONE,
      SoundType.COPPER,
      SoundType.DEEPSLATE,
      SoundType.DEEPSLATE_BRICKS,
      SoundType.DEEPSLATE_TILES,
      SoundType.POLISHED_DEEPSLATE
   );

   private AcousticMaterials() {
   }
}
