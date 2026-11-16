package deluxe.client.module.render;

import deluxe.client.util.TargetHudStyle;

import deluxe.mixin.BossBarHudMixin;
import deluxe.client.util.Colors;
import deluxe.client.event.EventListener;
import deluxe.client.render.FontFace;
import deluxe.client.event.HudRenderEvent;
import deluxe.client.util.ItemDrawOptions;
import deluxe.client.setting.ModeSetting;
import deluxe.client.module.Module;
import deluxe.client.module.ModuleCategory;
import deluxe.client.module.ModuleInfo;
import deluxe.client.event.PacketEvent;
import deluxe.client.event.PlayerTickEvent;
import deluxe.client.util.GuiScale;
import deluxe.client.render.WorldProjection;
import deluxe.client.render.FontText;
import deluxe.client.render.ItemRenderCache;
import deluxe.client.render.ShapeDrawing;
import deluxe.client.util.TargetHudStyle;
import deluxe.client.ui.AnimatedValue;
import deluxe.client.render.ThemeColors;
import deluxe.client.ui.hud.HudRenderContext;
import deluxe.client.ui.hud.HudStyle;
import deluxe.client.event.WorldRenderEvent;

import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.gui.hud.ClientBossBar;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexRendering;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.boss.BossBar.Color;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ChunkDeltaUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShapes;
import org.joml.Matrix4f;

@ModuleInfo(name = "StructureInfo", description = "Определяет трапки и пласты.", category = ModuleCategory.RENDER)
public final class StructureInfoModule extends Module {
   private static StructureInfoModule structureInfoModule;
   public ModeSetting modeSetting;
   private static final HashSet<Block> field11036 = new HashSet<>();
   private static final ItemStack field11037;
   private static final ItemStack field11038;
   private static final ItemStack field11039;
   private final ConcurrentHashMap<BlockPos, StructureInfoModule.Nested0479> field11040 = new ConcurrentHashMap<>();
   private final ConcurrentHashMap<BlockPos, Block> field11041 = new ConcurrentHashMap<>();
   private final HashSet<UUID> field11042 = new HashSet<>();
   private ClientWorld field11043;
   private final EventListener<PacketEvent> field11044 = packetEvent -> {
      if (packetEvent.isIncoming() && mc.world != null && mc.player != null) {
         if (packetEvent.getPacket() instanceof ChunkDeltaUpdateS2CPacket chunkDeltaUpdateS2CPacket) {
            if (this.modeSetting.isSelected("FunTime/SpookyTime")) {
               mc.execute(() -> chunkDeltaUpdateS2CPacket.visitUpdates((blockPos, blockState) -> {
                  if (!blockState.isAir() && field11036.contains(blockState.getBlock())) {
                     this.field11041.put(blockPos.toImmutable(), blockState.getBlock());
                  }
               }));
            }
         } else if (packetEvent.getPacket() instanceof BlockUpdateS2CPacket blockUpdateS2CPacket) {
            if (this.modeSetting.isSelected("FunTime/SpookyTime")) {
               if (!blockUpdateS2CPacket.getState().isAir() && field11036.contains(blockUpdateS2CPacket.getState().getBlock())) {
                  mc.execute(() -> this.field11041.put(blockUpdateS2CPacket.getPos().toImmutable(), blockUpdateS2CPacket.getState().getBlock()));
               }
            }
         } else if (packetEvent.getPacket() instanceof PlaySoundS2CPacket playSoundS2CPacket) {
            if (this.modeSetting.isSelected("HolyWorld")) {
               mc.execute(() -> {
                  if (mc.world != null && this.isEnabled()) {
                     BlockPos blockPos2 = new BlockPos((int)playSoundS2CPacket.getX(), (int)playSoundS2CPacket.getY(), (int)playSoundS2CPacket.getZ());
                     String local5242_3 = playSoundS2CPacket.getSound().value().id().toString();
                     if (this.modeSetting.isSelected("HolyWorld") && local5242_3.contains("block.beacon.deactivate")) {
                        this.field11040.put(blockPos2, new StructureInfoModule.Nested0479(blockPos2, "Стан", field11039, 15000L, null, true));
                     }
                  }
               });
            }
         }
      }
   };
   private final EventListener<PlayerTickEvent> field11045 = playerTickEvent -> {
      this.field11040.entrySet().removeIf(local5239_0 -> System.currentTimeMillis() - local5239_0.getValue().field11114 > local5239_0.getValue().field11115);
      if (mc.player != null && mc.world != null) {
         if (mc.world != this.field11043) {
            this.field11043 = mc.world;
            this.field11041.clear();
            this.field11040.clear();
            this.method3233();
         }

         Map<UUID, ClientBossBar> events = ((BossBarHudMixin)mc.inGameHud.getBossBarHud()).getEvents();
         HashSet<UUID> local5240_3 = new HashSet<>();

         for (StructureInfoModule.Nested0479 local5240_5 : this.field11040.values()) {
            if (local5240_5.field11116 != null && this.method3227(local5240_5.field11116)) {
               long local5240_6 = Math.max(0L, local5240_5.field11115 - (System.currentTimeMillis() - local5240_5.field11114));
               Text text = Text.literal(
                     local5240_5.field11112 + " " + (local5240_6 >= 60000L ? local5240_6 / 60000L + "мин" : (local5240_6 + 999L) / 1000L + "с")
                  )
                  .setStyle(Style.EMPTY.withColor(16777215).withBold(false));
               ClientBossBar clientBossBar = events.get(local5240_5.field11118);
               if (clientBossBar == null) {
                  clientBossBar = new ClientBossBar(
                     local5240_5.field11118,
                     text,
                     (float)local5240_6 / (float)local5240_5.field11115,
                     Color.PURPLE,
                     net.minecraft.entity.boss.BossBar.Style.PROGRESS,
                     false,
                     false,
                     false
                  );
                  events.put(local5240_5.field11118, clientBossBar);
               }

               clientBossBar.setName(text);
               clientBossBar.setPercent((float)local5240_6 / (float)local5240_5.field11115);
               local5240_3.add(local5240_5.field11118);
               this.field11042.add(local5240_5.field11118);
            }
         }

         this.field11042.removeIf(uUID -> {
            if (local5240_3.contains(uUID)) {
               return false;
            }

            events.remove(uUID);
            return true;
         });
         if (!this.field11041.isEmpty()) {
            if (this.field11041.size() >= 2) {
               HashSet<Block> local5240_4 = new HashSet<>(this.field11041.values());
               StructureInfoModule.Nested0481 local5240_5 = null;

               for (StructureInfoModule.Nested0481 local5240_9 : StructureInfoModule.Nested0481.values()) {
                  if (local5240_9.field11126.equals(local5240_4)) {
                     local5240_5 = local5240_9;
                     break;
                  }
               }

               StructureInfoModule.Nested0480 local5240_6 = null;
               StructureInfoModule.Nested0480[] nested0480Array = StructureInfoModule.Nested0480.values();
               int local5240_8 = nested0480Array.length;
               int local5240_9 = 0;

               label115: {
                  StructureInfoModule.Nested0480 local5240_10;
                  while (true) {
                     if (local5240_9 >= local5240_8) {
                        break label115;
                     }

                     local5240_10 = nested0480Array[local5240_9];
                     if (local5240_10.field11122.equals(local5240_4)) {
                        break;
                     }

                     if (!local5240_10.field11123.isEmpty()) {
                        HashSet<Block> local5240_11 = new HashSet<>(local5240_10.field11122);
                        local5240_11.addAll(local5240_10.field11123);
                        if (local5240_11.equals(local5240_4)) {
                           break;
                        }
                     }

                     local5240_9++;
                  }

                  local5240_6 = local5240_10;
               }

               if (local5240_5 != null || local5240_6 != null) {
                  int local5240_7 = Integer.MAX_VALUE;
                  local5240_8 = Integer.MAX_VALUE;
                  local5240_9 = Integer.MAX_VALUE;
                  int local5240_10 = Integer.MIN_VALUE;
                  int local5240_11 = Integer.MIN_VALUE;
                  int local5240_12 = Integer.MIN_VALUE;

                  for (BlockPos blockPos3 : this.field11041.keySet()) {
                     local5240_7 = Math.min(local5240_7, blockPos3.getX());
                     local5240_8 = Math.min(local5240_8, blockPos3.getY());
                     local5240_9 = Math.min(local5240_9, blockPos3.getZ());
                     local5240_10 = Math.max(local5240_10, blockPos3.getX() + 1);
                     local5240_11 = Math.max(local5240_11, blockPos3.getY() + 1);
                     local5240_12 = Math.max(local5240_12, blockPos3.getZ() + 1);
                  }

                  Box box = new Box(local5240_7, local5240_8, local5240_9, local5240_10, local5240_11, local5240_12);
                  BlockPos blockPos4 = BlockPos.ofFloored(box.getCenter());
                  boolean local5240_15 = mc.getCurrentServerEntry() != null
                     && mc.getCurrentServerEntry().address.toLowerCase(Locale.ROOT).contains("spooky");
                  TargetHudStyle.method3844(
                     local5240_5 != null,
                     local5240_5 != null ? local5240_5 == StructureInfoModule.Nested0481.DRAGON : local5240_6 == StructureInfoModule.Nested0480.DRAGON
                  );
                  if (local5240_5 != null) {
                     this.field11040
                        .put(
                           blockPos4,
                           new StructureInfoModule.Nested0479(blockPos4, local5240_5.field11124, field11037, local5240_5.field11125, box, false)
                        );
                  } else {
                     this.field11040
                        .put(
                           blockPos4,
                           new StructureInfoModule.Nested0479(
                              blockPos4,
                              local5240_6.field11121,
                              field11038,
                              local5240_6 == StructureInfoModule.Nested0480.DRAGON && local5240_15
                                 ? 20000L
                                 : (box.getLengthY() <= 2.0 ? 60000L : 20000L),
                              box,
                              false
                           )
                        );
                  }
               }
            }

            this.field11041.clear();
         }
      } else {
         this.field11041.clear();
      }
   };
   private final EventListener<WorldRenderEvent> field11046 = worldRenderEvent -> {
      if (mc.player != null && mc.world != null && this.modeSetting.isSelected("HolyWorld")) {
         for (StructureInfoModule.Nested0479 local5236_3 : this.field11040.values()) {
            if (local5236_3.field11117
               && !(mc.player.getEntityPos().distanceTo(Vec3d.ofCenter(local5236_3.field11110)) > 100.0)
               && WorldProjection.method1122(Vec3d.ofCenter(local5236_3.field11110), 1.0F, 1.0F) != null) {
               Box box2 = new Box(local5236_3.field11110.up()).contract(0.0, 0.2, 0.0).expand(1.99);
               worldRenderEvent.method0647().push();
               worldRenderEvent.method0647()
                  .translate(
                     box2.minX - mc.gameRenderer.getCamera().getCameraPos().x,
                     box2.minY - mc.gameRenderer.getCamera().getCameraPos().y,
                     box2.minZ - mc.gameRenderer.getCamera().getCameraPos().z
                  );
               Matrix4f positionMatrix = worldRenderEvent.method0647().peek().getPositionMatrix();
               VertexConsumer vertexConsumer = mc.getBufferBuilders().getEntityVertexConsumers().getBuffer(RenderLayers.debugQuads());
               float[] floatArray = new float[]{0.0F, (float)(box2.maxX - box2.minX)};
               float[] floatArray2 = new float[]{0.0F, (float)(box2.maxY - box2.minY)};
               float[] floatArray3 = new float[]{0.0F, (float)(box2.maxZ - box2.minZ)};
               int[][] intArrayArray = new int[][]{{0, 1, 3, 2}, {4, 6, 7, 5}, {0, 4, 5, 1}, {2, 3, 7, 6}, {0, 2, 6, 4}, {1, 5, 7, 3}};

               for (int[] intArray : intArrayArray) {
                  for (int local5236_18 : intArray) {
                     vertexConsumer.vertex(positionMatrix, floatArray[local5236_18 >> 2], floatArray2[local5236_18 >> 1 & 1], floatArray3[local5236_18 & 1])
                        .color(Colors.method3727(ThemeColors.field13669, 0.3529412F));
                  }
               }

               VertexRendering.drawOutline(
                  worldRenderEvent.method0647(),
                  mc.getBufferBuilders().getEntityVertexConsumers().getBuffer(RenderLayers.lines()),
                  VoxelShapes.cuboid(
                     new Box(0.0, 0.0, 0.0, box2.maxX - box2.minX, box2.maxY - box2.minY, box2.maxZ - box2.minZ)
                  ),
                  0.0,
                  0.0,
                  0.0,
                  Colors.method3727(ThemeColors.field13669, 0.55F),
                  1.0F
               );
               mc.getBufferBuilders().getEntityVertexConsumers().draw();
               worldRenderEvent.method0647().pop();
            }
         }
      }
   };

   @Override
   protected void onEnable() {
      this.field11040.clear();
      this.field11041.clear();
      this.method3233();
   }

   @Override
   protected void onDisable() {
      this.field11040.clear();
      this.field11041.clear();
      this.method3233();
   }

   public void onHudRender(HudRenderEvent event) {
      if (mc.player != null && mc.world != null) {
         for (StructureInfoModule.Nested0479 local5233_3 : this.field11040.values()) {
            if (!(mc.player.getEntityPos().distanceTo(local5233_3.field11111) > 100.0) && !(this.method3229(local5233_3) < 0.01F)) {
               local5233_3.animatedValue.method3869(local5233_3.field11116 != null && this.method3227(local5233_3.field11116) ? 0.0 : 1.0);
               if (!(this.method3229(local5233_3) * local5233_3.animatedValue.method3871() < 0.01F)) {
                  Vec3d vec3d = WorldProjection.method1125(local5233_3.field11111, event.method4462(), event.method4460());
                  if (vec3d != null) {
                     float local5233_5 = (float)mc.gameRenderer.getCamera().getCameraPos().distanceTo(local5233_3.field11111);
                     HudRenderContext.method4336(
                        (float)vec3d.x * GuiScale.method0797() / GuiScale.method0794(),
                        (float)vec3d.y * GuiScale.method0797() / GuiScale.method0794(),
                        Math.max(0.3F, 1.0F - local5233_5 * 0.008F)
                           * Math.max(0.7F, 1.0F - local5233_5 * 0.005F)
                           * (0.85F + 0.15F * this.method3229(local5233_3) * (float)local5233_3.animatedValue.method3871())
                     );
                     this.method3221(
                        local5233_3,
                        (float)vec3d.x * GuiScale.method0797() / GuiScale.method0794(),
                        (float)vec3d.y * GuiScale.method0797() / GuiScale.method0794()
                     );
                     HudRenderContext.method4337();
                  }
               }
            }
         }
      }
   }

   private StructureInfoModule() {
      this.modeSetting = this.addSetting(
         new ModeSetting("Сервер", "На каком сервере определять структуры.", "FunTime/SpookyTime", "FunTime/SpookyTime", "HolyWorld")
      );
   }

   private void method3221(StructureInfoModule.Nested0479 local5235_1, float local5235_2, float local5235_3) {
      long local5235_4 = Math.max(0L, local5235_1.field11115 - (System.currentTimeMillis() - local5235_1.field11114));
      String local5235_6 = local5235_4 >= 60000L ? local5235_4 / 60000L + "мин" : (local5235_4 + 999L) / 1000L + "с";
      float local5235_7 = this.method3229(local5235_1) * (float)local5235_1.animatedValue.method3871();
      float local5235_8 = 13.0F;
      float local5235_9 = FontText.method1396(FontFace.SEMI_BOLD, local5235_1.field11112, 6.0F);
      float local5235_10 = FontText.method1396(FontFace.SEMI_BOLD, local5235_6, 6.0F);
      float local5235_11 = 16.0F + local5235_9 + 3.0F + local5235_10 + 4.0F;
      float local5235_12 = Math.round((local5235_2 - local5235_11 / 2.0F) * 2.0F) / 2.0F;
      float local5235_13 = Math.round((local5235_3 - local5235_8 / 2.0F) * 2.0F) / 2.0F;
      HudStyle.method4812(local5235_12, local5235_13, local5235_11, local5235_8, local5235_7, 4.5F, ShapeDrawing.Nested0502.LOW);
      ItemRenderCache.method2336(local5235_1.field11113, local5235_12 + 4.0F, local5235_13 + 2.5F, 8.0F, ItemDrawOptions.noDecorations(local5235_7));
      float local5235_14 = local5235_13 + local5235_8 / 2.0F - 3.5F;
      FontText.method1397(
         FontFace.SEMI_BOLD, local5235_1.field11112, local5235_12 + 16.0F, local5235_14, 6.0F, HudStyle.method4820(local5235_7)
      );
      FontText.method1397(
         FontFace.SEMI_BOLD, local5235_6, local5235_12 + 16.0F + local5235_9 + 3.0F, local5235_14, 6.0F, HudStyle.method4815(local5235_7)
      );
   }

   private boolean method3227(Box box) {
      return mc.player.getX() >= box.minX
         && mc.player.getX() < box.maxX
         && mc.player.getZ() >= box.minZ
         && mc.player.getZ() < box.maxZ
         && mc.player.getY() >= box.minY - 1.0
         && mc.player.getY() < box.maxY + 1.0;
   }

   private float method3229(StructureInfoModule.Nested0479 local5243_1) {
      float local5243_2 = Math.min(
         Math.min(1.0F, (float)(System.currentTimeMillis() - local5243_1.field11114) / 220.0F),
         Math.min(1.0F, (float)(local5243_1.field11115 - (System.currentTimeMillis() - local5243_1.field11114)) / 220.0F)
      );
      return local5243_2 * local5243_2 * (3.0F - 2.0F * local5243_2);
   }

   static {
      for (StructureInfoModule.Nested0481 local5244_3 : StructureInfoModule.Nested0481.values()) {
         field11036.addAll(local5244_3.field11126);
      }

      for (StructureInfoModule.Nested0480 local5244_3 : StructureInfoModule.Nested0480.values()) {
         field11036.addAll(local5244_3.field11122);
         field11036.addAll(local5244_3.field11123);
      }

      field11037 = Items.NETHERITE_SCRAP.getDefaultStack();
      field11038 = Items.DRIED_KELP.getDefaultStack();
      field11039 = Items.NETHER_STAR.getDefaultStack();
   }

   public static StructureInfoModule method3231() {
      if (structureInfoModule == null) {
         structureInfoModule = new StructureInfoModule();
      }

      return structureInfoModule;
   }

   private void method3233() {
      if (mc.inGameHud != null) {
         Map<UUID, ClientBossBar> events = ((BossBarHudMixin)mc.inGameHud.getBossBarHud()).getEvents();

         for (UUID uUID : this.field11042) {
            events.remove(uUID);
         }

         this.field11042.clear();
      }
   }

   private static final class Nested0479 {
      final BlockPos field11110;
      final Vec3d field11111;
      final String field11112;
      final ItemStack field11113;
      final long field11114;
      final long field11115;
      final Box field11116;
      final boolean field11117;
      final UUID field11118;
      final AnimatedValue animatedValue = new AnimatedValue(220L).method3870(1.0);

      Nested0479(BlockPos blockPos, String local5249_2, ItemStack itemStack, long local5249_4, Box box, boolean local5249_7) {
         this.field11110 = blockPos.toImmutable();
         this.field11111 = box == null ? Vec3d.ofCenter(blockPos) : box.getCenter();
         this.field11112 = local5249_2;
         this.field11113 = itemStack;
         this.field11114 = System.currentTimeMillis();
         this.field11115 = local5249_4;
         this.field11116 = box;
         this.field11117 = local5249_7;
         this.field11118 = UUID.randomUUID();
      }
   }

   private enum Nested0480 {
      DEFAULT("Обычный", Set.of(Blocks.ANDESITE, Blocks.COBBLESTONE, Blocks.DEAD_TUBE_CORAL_BLOCK), Set.of()),
      DRAGON(
         "Драконий",
         Set.of(
            Blocks.CHISELED_POLISHED_BLACKSTONE,
            Blocks.LODESTONE,
            Blocks.POLISHED_BASALT,
            Blocks.POLISHED_BLACKSTONE,
            Blocks.POLISHED_BLACKSTONE_BRICKS,
            Blocks.SMOOTH_STONE
         ),
         Set.of(Blocks.NETHERITE_BLOCK)
      ),
      PEARL("Жемчужный", Set.of(Blocks.DARK_PRISMARINE, Blocks.PRISMARINE, Blocks.PRISMARINE_BRICKS), Set.of()),
      MYSTIC("Мистический", Set.of(Blocks.END_STONE_BRICKS, Blocks.PURPLE_STAINED_GLASS, Blocks.PURPUR_BLOCK, Blocks.PURPUR_PILLAR), Set.of()),
      GHOST("Призрачный", Set.of(Blocks.CHISELED_QUARTZ_BLOCK, Blocks.QUARTZ_PILLAR, Blocks.SMOOTH_QUARTZ, Blocks.WHITE_STAINED_GLASS), Set.of()),
      NIGHTMARISH("Кошмарный", Set.of(Blocks.BLACKSTONE, Blocks.BONE_BLOCK, Blocks.POLISHED_BASALT), Set.of()),
      SPOOKY(
         "Зловещий",
         Set.of(Blocks.INFESTED_CRACKED_STONE_BRICKS, Blocks.INFESTED_MOSSY_STONE_BRICKS, Blocks.INFESTED_STONE_BRICKS, Blocks.LODESTONE),
         Set.of(Blocks.INFESTED_CHISELED_STONE_BRICKS)
      ),
      LETHAL("Смертельный", Set.of(Blocks.CHISELED_NETHER_BRICKS, Blocks.MAGMA_BLOCK, Blocks.NETHER_BRICKS, Blocks.RED_NETHER_BRICKS), Set.of()),
      CRUEL(
         "Жестокий",
         Set.of(Blocks.BLACKSTONE, Blocks.CRYING_OBSIDIAN, Blocks.GILDED_BLACKSTONE, Blocks.NETHERITE_BLOCK, Blocks.POLISHED_BLACKSTONE_BRICKS),
         Set.of()
      ),
      ICY("Ледяной", Set.of(Blocks.ICE, Blocks.PACKED_ICE, Blocks.SNOW_BLOCK), Set.of()),
      INSIDIOUS("Коварный", Set.of(Blocks.CHISELED_NETHER_BRICKS, Blocks.NETHER_BRICKS, Blocks.NETHER_WART_BLOCK, Blocks.RED_NETHER_BRICKS), Set.of()),
      CURSED(
         "Проклятый",
         Set.of(Blocks.BLACKSTONE, Blocks.CHISELED_POLISHED_BLACKSTONE, Blocks.CRYING_OBSIDIAN, Blocks.POLISHED_BLACKSTONE_BRICKS, Blocks.RESPAWN_ANCHOR),
         Set.of()
      ),
      EXCRUCIATING("Мучительный", Set.of(Blocks.BLACKSTONE, Blocks.COAL_BLOCK, Blocks.NETHERITE_BLOCK, Blocks.POLISHED_BLACKSTONE_BRICKS), Set.of()),
      ROYAL(
         "Царский", Set.of(Blocks.DIORITE, Blocks.LIGHT_GRAY_STAINED_GLASS, Blocks.NETHERITE_BLOCK, Blocks.POLISHED_DIORITE), Set.of(Blocks.GILDED_BLACKSTONE)
      ),
      SINGULAR("Сингулярный", Set.of(Blocks.BLACK_CONCRETE, Blocks.COAL_BLOCK), Set.of()),
      UNVOIDABLE("Неизбежный", Set.of(Blocks.ANDESITE, Blocks.DEAD_TUBE_CORAL_BLOCK, Blocks.POLISHED_BASALT), Set.of());

      final String field11121;
      final Set<Block> field11122;
      final Set<Block> field11123;

      Nested0480(String local5252_3, Set<Block> local5252_4, Set<Block> local5252_5) {
         this.field11121 = local5252_3 + " пласт";
         this.field11122 = local5252_4;
         this.field11123 = local5252_5;
      }
   }

   private enum Nested0481 {
      DEFAULT("Обычная", 15000L, Set.of(Blocks.ANDESITE, Blocks.ANDESITE_STAIRS, Blocks.COBBLESTONE, Blocks.DEAD_TUBE_CORAL_BLOCK)),
      DRAGON(
         "Драконья",
         30000L,
         Set.of(
            Blocks.CHISELED_POLISHED_BLACKSTONE,
            Blocks.LODESTONE,
            Blocks.NETHERITE_BLOCK,
            Blocks.POLISHED_BASALT,
            Blocks.POLISHED_BLACKSTONE,
            Blocks.POLISHED_BLACKSTONE_BRICK_SLAB,
            Blocks.POLISHED_BLACKSTONE_BRICK_STAIRS,
            Blocks.POLISHED_BLACKSTONE_BRICKS,
            Blocks.RESPAWN_ANCHOR,
            Blocks.SMOOTH_STONE_SLAB
         )
      ),
      PEARL(
         "Жемчужная",
         15000L,
         Set.of(
            Blocks.DARK_PRISMARINE, Blocks.DARK_PRISMARINE_SLAB, Blocks.DARK_PRISMARINE_STAIRS, Blocks.PRISMARINE, Blocks.PRISMARINE_BRICKS, Blocks.SEA_LANTERN
         )
      ),
      MYSTIC(
         "Мистическая",
         15000L,
         Set.of(
            Blocks.END_STONE,
            Blocks.END_STONE_BRICK_WALL,
            Blocks.END_STONE_BRICKS,
            Blocks.PURPLE_STAINED_GLASS,
            Blocks.PURPUR_BLOCK,
            Blocks.PURPUR_PILLAR,
            Blocks.PURPUR_STAIRS
         )
      ),
      GHOST(
         "Призрачная",
         15000L,
         Set.of(
            Blocks.CHISELED_QUARTZ_BLOCK,
            Blocks.DIORITE,
            Blocks.QUARTZ_PILLAR,
            Blocks.QUARTZ_STAIRS,
            Blocks.SEA_LANTERN,
            Blocks.SMOOTH_QUARTZ_SLAB,
            Blocks.WHITE_STAINED_GLASS
         )
      ),
      NIGHTMARISH(
         "Кошмарная", 15000L, Set.of(Blocks.BLACKSTONE, Blocks.BLACKSTONE_SLAB, Blocks.BONE_BLOCK, Blocks.CHISELED_QUARTZ_BLOCK, Blocks.POLISHED_BASALT)
      ),
      SPOOKY(
         "Зловещая",
         15000L,
         Set.of(
            Blocks.INFESTED_CHISELED_STONE_BRICKS,
            Blocks.INFESTED_CRACKED_STONE_BRICKS,
            Blocks.INFESTED_MOSSY_STONE_BRICKS,
            Blocks.INFESTED_STONE_BRICKS,
            Blocks.LODESTONE,
            Blocks.STONE_BRICK_SLAB,
            Blocks.STONE_BRICK_STAIRS,
            Blocks.STONE_BRICK_WALL
         )
      ),
      LETHAL(
         "Смертельная",
         15000L,
         Set.of(
            Blocks.CHISELED_NETHER_BRICKS,
            Blocks.MAGMA_BLOCK,
            Blocks.NETHER_BRICKS,
            Blocks.RED_NETHER_BRICK_STAIRS,
            Blocks.RED_NETHER_BRICK_WALL,
            Blocks.RED_NETHER_BRICKS
         )
      ),
      CRUEL(
         "Жестокая",
         15000L,
         Set.of(
            Blocks.BLACKSTONE,
            Blocks.BLACKSTONE_SLAB,
            Blocks.CRYING_OBSIDIAN,
            Blocks.GILDED_BLACKSTONE,
            Blocks.NETHERITE_BLOCK,
            Blocks.POLISHED_BLACKSTONE_BRICK_SLAB,
            Blocks.POLISHED_BLACKSTONE_BRICKS
         )
      ),
      ICY("Ледяная", 15000L, Set.of(Blocks.ICE, Blocks.PACKED_ICE, Blocks.SNOW, Blocks.SNOW_BLOCK, Blocks.WHITE_CONCRETE)),
      INSIDIOUS(
         "Коварная",
         15000L,
         Set.of(
            Blocks.CHISELED_NETHER_BRICKS,
            Blocks.NETHER_BRICK_SLAB,
            Blocks.NETHER_BRICK_STAIRS,
            Blocks.NETHER_BRICKS,
            Blocks.NETHER_WART_BLOCK,
            Blocks.RED_NETHER_BRICK_STAIRS,
            Blocks.RED_NETHER_BRICKS
         )
      ),
      CURSED(
         "Проклятая",
         15000L,
         Set.of(
            Blocks.BLACKSTONE,
            Blocks.BLACKSTONE_SLAB,
            Blocks.BLACKSTONE_STAIRS,
            Blocks.CHISELED_POLISHED_BLACKSTONE,
            Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS,
            Blocks.CRYING_OBSIDIAN,
            Blocks.NETHERITE_BLOCK,
            Blocks.POLISHED_BLACKSTONE_BRICK_STAIRS,
            Blocks.POLISHED_BLACKSTONE_BRICKS,
            Blocks.RESPAWN_ANCHOR
         )
      ),
      EXCRUCIATING(
         "Мучительная",
         15000L,
         Set.of(
            Blocks.BLACK_CONCRETE,
            Blocks.BLACKSTONE,
            Blocks.BLACKSTONE_STAIRS,
            Blocks.COAL_BLOCK,
            Blocks.NETHERITE_BLOCK,
            Blocks.POLISHED_BLACKSTONE_BRICK_SLAB,
            Blocks.POLISHED_BLACKSTONE_BRICK_STAIRS,
            Blocks.POLISHED_BLACKSTONE_BRICKS,
            Blocks.POLISHED_BLACKSTONE_WALL
         )
      ),
      ROYAL(
         "Царская",
         15000L,
         Set.of(
            Blocks.DIORITE,
            Blocks.DIORITE_STAIRS,
            Blocks.DIORITE_WALL,
            Blocks.GILDED_BLACKSTONE,
            Blocks.LIGHT_GRAY_STAINED_GLASS,
            Blocks.NETHERITE_BLOCK,
            Blocks.POLISHED_DIORITE,
            Blocks.POLISHED_DIORITE_SLAB
         )
      ),
      SINGULAR("Сингулярная", 15000L, Set.of(Blocks.BLACK_CONCRETE, Blocks.BLACKSTONE_SLAB, Blocks.COAL_BLOCK)),
      UNVOIDABLE("Неизбежная", 15000L, Set.of(Blocks.ANDESITE, Blocks.ANDESITE_STAIRS, Blocks.DEAD_TUBE_CORAL_BLOCK, Blocks.POLISHED_BASALT));

      final String field11124;
      final long field11125;
      final Set<Block> field11126;

      Nested0481(String local5257_3, long local5257_4, Set<Block> local5257_6) {
         this.field11124 = local5257_3 + " трапка";
         this.field11125 = local5257_4;
         this.field11126 = local5257_6;
      }
   }
}
