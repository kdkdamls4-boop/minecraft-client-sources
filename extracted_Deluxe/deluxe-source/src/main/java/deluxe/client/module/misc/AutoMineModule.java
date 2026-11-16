package deluxe.client.module.misc;

import deluxe.mixin.BossBarHudMixin;
import deluxe.client.util.ClientChat;
import deluxe.client.event.ClientTickEvent;
import deluxe.client.util.Colors;
import deluxe.client.event.EventListener;
import deluxe.client.setting.LegacyNumberSetting;
import deluxe.client.setting.ModeSetting;
import deluxe.client.module.Module;
import deluxe.client.module.ModuleCategory;
import deluxe.client.module.ModuleInfo;
import deluxe.client.event.MovementInputEvent;
import deluxe.client.event.PacketEvent;
import deluxe.client.util.InventoryUtils;
import deluxe.client.setting.NumericTextSetting;
import deluxe.client.util.InventoryActionQueue;
import deluxe.client.RotationAngles;
import deluxe.client.RotationManager;
import deluxe.client.RotationSpeed;
import deluxe.client.util.Stopwatch;
import deluxe.client.event.WorldRenderEvent;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.block.Blocks;
import net.minecraft.client.gui.hud.ClientBossBar;
import net.minecraft.client.gui.screen.DisconnectedScreen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexRendering;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShapes;
import org.joml.Matrix4f;

@ModuleInfo(
   name = "AutoMine",
   description = "Перебирает анархии, находит автошахту с таймером меньше минуты, релогается на точке и копает алмазы.",
   category = ModuleCategory.MISC
)
public final class AutoMineModule extends Module {
   private static AutoMineModule autoMineModule;
   private static final Pattern field12129 = Pattern.compile("^(\\d{1,2}):(\\d{2})$");
   private static final Pattern field12130 = Pattern.compile(".*Телепортация через[:\\s]*(\\d+(?:[.,]\\d+)?)\\s*сек.*");
   private static final Pattern field12131 = Pattern.compile(".*успешно купили.*");
   private static final String[] field12132 = new String[]{
      "an101",
      "an102",
      "an103",
      "an104",
      "an105",
      "an106",
      "an107",
      "an108",
      "an201",
      "an202",
      "an203",
      "an204",
      "an205",
      "an206",
      "an207",
      "an208",
      "an209",
      "an210",
      "an301",
      "an302",
      "an303",
      "an304",
      "an305",
      "an501",
      "an502",
      "an503",
      "an601",
      "an602",
      "an603"
   };
   private static final ArrayList<String> field12133 = new ArrayList<>();
   private static final int[][] field12134;
   private static final BlockPos field12135;
   private static final BlockPos field12136;
   private static final BlockPos field12137;
   private static final BlockPos field12138;
   private static final String[] field12139;
   public ModeSetting modeSetting;
   public NumericTextSetting numericTextSetting;
   public LegacyNumberSetting legacyNumberSetting;
   private AutoMineModule.Nested0489 field12143;
   private final Stopwatch field12144 = new Stopwatch();
   private final Stopwatch field12145 = new Stopwatch();
   private final Stopwatch field12146 = new Stopwatch();
   private final Stopwatch field12147 = new Stopwatch();
   private ServerInfo field12148;
   private boolean field12149;
   private boolean field12150;
   private String field12151;
   private boolean field12152;
   private boolean field12153;
   private int[] field12154;
   private BlockPos field12155;
   private boolean field12156;
   private int field12157;
   private long field12158;
   private final ArrayList<BlockPos> field12159 = new ArrayList<>();
   private long field12160;
   private long field12161;
   private long field12162;
   private final ArrayDeque<BlockPos> field12163 = new ArrayDeque<>();
   private Runnable field12164;
   private long field12165;
   private ItemStack field12166 = ItemStack.EMPTY;
   private boolean field12167;
   private boolean field12168;
   private final EventListener<ClientTickEvent> field12169 = clientTickEvent -> {
      if (clientTickEvent.isPre()) {
         if (mc.player != null && mc.world != null && mc.interactionManager != null) {
            if (mc.getCurrentServerEntry() != null) {
               this.field12148 = mc.getCurrentServerEntry();
            }

            this.field12147.reset();
            if (this.modeSetting.isSelected("FunTime")) {
               mc.options.attackKey.setPressed(false);
            }

            if (!this.field12153) {
               for (String local5649_5 : field12139) {
                  ClientChat.method2258(local5649_5);
               }

               this.field12153 = true;
            }

            if (this.field12155 == null) {
               this.method3499();
               this.field12159.clear();
            }

            this.field12155 = null;
            if (!this.modeSetting.isSelected("FunTime") || !this.method3489()) {
               if (this.field12164 != null) {
                  if (System.currentTimeMillis() >= this.field12165) {
                     Runnable runnable = this.field12164;
                     this.field12164 = null;
                     runnable.run();
                  }
               } else if (this.field12143 != AutoMineModule.Nested0489.SCAN_TITLE
                     && this.field12143 != AutoMineModule.Nested0489.STAND_READY
                     && this.field12143 != AutoMineModule.Nested0489.DIG_MINE
                  || !mc.player.getMainHandStack().isIn(ItemTags.PICKAXES)
                  || !mc.player.getMainHandStack().isDamageable()
                  || mc.player.getMainHandStack().getMaxDamage() - mc.player.getMainHandStack().getDamage() > 1
                     && !(
                        (mc.player.getMainHandStack().getMaxDamage() - mc.player.getMainHandStack().getDamage())
                              * 100.0
                              / mc.player.getMainHandStack().getMaxDamage()
                           < 6.0
                     )) {
                  switch (this.field12143) {
                     case SCAN_TITLE:
                        {
ScoreboardObjective objectiveForSlot = mc.world.getScoreboard().getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR);
                        if (objectiveForSlot != null) {
                           String[] stringArray = objectiveForSlot.getDisplayName().getString().split("-");
                           if (stringArray.length > 1 && !stringArray[1].replaceAll("\\D", "").isEmpty()) {
                              this.field12151 = "an" + stringArray[1].replaceAll("\\D", "");
                           }
                        }

                        Matcher matcher = field12129.matcher(this.method3474("Автошахт", field12129, 150.0).orElse(""));
                        if (!matcher.matches()) {
                           if (this.field12158 != 0L) {
                              if (!this.field12145.hasExceededMillis(this.field12158)) {
                                 this.method3482();
                                 return;
                              }

                              this.field12158 = 0L;
                           }

                           this.field12145.reset();
                           if (!this.field12144.hasExceededMillis(3000L)) {
                              return;
                           }

                           if (this.field12152) {
                              this.method3485();
                              return;
                           }

                           mc.player.networkHandler.sendChatCommand("warp mine");
                           this.field12152 = true;
                           this.field12143 = AutoMineModule.Nested0489.WARP_TELEPORT;
                           this.field12144.reset();
                           return;
                        }

                        int local5649_4 = Integer.parseInt(matcher.group(1)) * 60 + Integer.parseInt(matcher.group(2));
                        if (this.field12151 == null) {
                           this.method3485();
                           return;
                        }

                        if (this.field12158 == 0L || !this.field12145.hasExceededMillis(this.field12158)) {
                           if (this.method3482()) {
                              if (this.field12158 == 0L) {
                                 this.field12158 = 17000L;
                                 this.field12145.reset();
                              }

                              return;
                           }

                           if (this.field12158 == 0L && !this.field12145.hasExceededMillis(600L)) {
                              return;
                           }
                        }

                        if (local5649_4 >= 60) {
                           this.method3485();
                           return;
                        }

                        this.field12154 = field12134[ThreadLocalRandom.current().nextInt(field12134.length)];
                        mc.interactionManager.cancelBlockBreaking();
                        RotationManager.method0658().method0653();
                        this.field12156 = false;
                        ClientChat.method2258("#go " + this.field12154[0] + " " + this.field12154[1] + " " + this.field12154[2]);
                        this.field12143 = AutoMineModule.Nested0489.BARITONE_RIDE;
                        break;
}
                     case WARP_TELEPORT:
                        {
for (ClientBossBar clientBossBar : ((BossBarHudMixin)mc.inGameHud.getBossBarHud()).getEvents().values()) {
                           Matcher matcher2 = field12130.matcher(clientBossBar.getName().getString().replaceAll("(?i)§[0-9a-fk-or]", "").trim());
                           if (matcher2.matches()) {
                              this.method3478((long)(Double.parseDouble(matcher2.group(1).replace(',', '.')) * 1000.0) + 200L, () -> {
                                 this.field12143 = AutoMineModule.Nested0489.SCAN_TITLE;
                                 this.field12144.reset();
                                 this.field12145.reset();
                                 this.field12158 = 0L;
                              });
                              return;
                           }
                        }

                        if (this.field12144.hasExceededMillis(3000L)) {
                           this.field12143 = AutoMineModule.Nested0489.SCAN_TITLE;
                           this.field12144.reset();
                           this.field12145.reset();
                           this.field12158 = 0L;
                        }
                        break;
}
                     case BARITONE_RIDE:
                        {
if (mc.interactionManager.isBreakingBlock()) {
                           mc.interactionManager.cancelBlockBreaking();
                        }

                        if (mc.player.squaredDistanceTo(this.field12154[0] + 0.5, this.field12154[1], this.field12154[2] + 0.5) > 2.25) {
                           return;
                        }

                        ClientChat.method2258("#stop");
                        this.field12143 = AutoMineModule.Nested0489.STAND_READY;
                        break;
}
                     case STAND_READY:
                        {
Matcher matcher3 = field12129.matcher(this.method3474("Автошахт", field12129, 150.0).orElse(""));
                        if (!matcher3.matches()) {
                           return;
                        }

                        if (Integer.parseInt(matcher3.group(1)) * 60 + Integer.parseInt(matcher3.group(2))
                           <= (this.modeSetting.isSelected("FunTime") ? 3 : 2)) {
                           mc.interactionManager.cancelBlockBreaking();
                           mc.player.networkHandler.sendChatCommand("hub");
                           this.method3478(this.modeSetting.isSelected("FunTime") ? 2500L : 600L, () -> {
                              mc.player.networkHandler.sendChatCommand(this.field12151);
                              this.method3478(1500L, () -> {
                                 this.field12143 = AutoMineModule.Nested0489.DIG_MINE;
                                 this.field12144.reset();
                                 this.field12145.reset();
                              });
                           });
                           return;
                        }

                        if (Integer.parseInt(matcher3.group(1)) * 60 + Integer.parseInt(matcher3.group(2)) >= 20) {
                           this.method3482();
                           return;
                        }

                        if (mc.player.squaredDistanceTo(this.field12154[0] + 0.5, this.field12154[1], this.field12154[2] + 0.5) > 2.25) {
                           mc.interactionManager.cancelBlockBreaking();
                           RotationManager.method0658().method0653();
                           this.field12156 = false;
                           ClientChat.method2258("#go " + this.field12154[0] + " " + this.field12154[1] + " " + this.field12154[2]);
                           this.field12143 = AutoMineModule.Nested0489.BARITONE_RIDE;
                        }
                        break;
}
                     case DIG_MINE:
                        {
if (this.field12145.hasExceededMillis(17000L)) {
                           mc.interactionManager.cancelBlockBreaking();
                           this.method3485();
                           return;
                        }

                        if (this.method3482()) {
                           this.field12144.reset();
                           return;
                        }

                        if (this.field12144.hasExceededMillis(5000L)) {
                           this.method3485();
                        }
                        break;
}
                     case HOME_TELEPORT:
                        {
for (ClientBossBar clientBossBar2 : ((BossBarHudMixin)mc.inGameHud.getBossBarHud()).getEvents().values()) {
                           Matcher matcher4 = field12130.matcher(clientBossBar2.getName().getString().replaceAll("(?i)§[0-9a-fk-or]", "").trim());
                           if (matcher4.matches()) {
                              this.method3478((long)(Double.parseDouble(matcher4.group(1).replace(',', '.')) * 1000.0) + 500L, () -> {
                                 this.field12143 = AutoMineModule.Nested0489.DUMP_DIAMONDS;
                                 this.field12144.reset();
                              });
                              return;
                           }
                        }

                        if (this.field12144.hasExceededMillis(3000L)) {
                           this.field12143 = AutoMineModule.Nested0489.DUMP_DIAMONDS;
                           this.field12144.reset();
                        }
                        break;
}
                     case DUMP_DIAMONDS:
                        {
if (!this.field12144.hasExceededMillis(150L)) {
                           return;
                        }

                        int local5649_2 = -1;
                        int local5649_3 = 0;

                        for (; local5649_3 <= 35; local5649_3++) {
                           if (mc.player.getInventory().getStack(local5649_3).isOf(Items.DIAMOND)
                              && (
                                 local5649_2 == -1
                                    || mc.player.getInventory().getStack(local5649_3).getCount()
                                       > mc.player.getInventory().getStack(local5649_2).getCount()
                              )) {
                              local5649_2 = local5649_3;
                           }
                        }

                        if (local5649_2 == -1
                           || InventoryUtils.method0556(itemStack -> itemStack.isOf(Items.DIAMOND), 0, local5649_2 - 1) == -1
                              && InventoryUtils.method0556(itemStack2 -> itemStack2.isOf(Items.DIAMOND), local5649_2 + 1, 35) == -1) {
                           mc.player.networkHandler.sendChatCommand("ah search опыт");
                           this.field12143 = AutoMineModule.Nested0489.AH_BUY;
                           this.field12144.reset();
                           return;
                        }

                        InventoryUtils.method0547(
                           InventoryUtils.method0533(
                              InventoryUtils.method0556(itemStack3 -> itemStack3.isOf(Items.DIAMOND), 0, local5649_2 - 1) != -1
                                 ? InventoryUtils.method0556(itemStack4 -> itemStack4.isOf(Items.DIAMOND), 0, local5649_2 - 1)
                                 : InventoryUtils.method0556(itemStack5 -> itemStack5.isOf(Items.DIAMOND), local5649_2 + 1, 35)
                           ),
                           1,
                           SlotActionType.THROW,
                           false
                        );
                        this.field12144.reset();
                        break;
}
                     case AH_BUY:
                        {
if (mc.player.getMainHandStack().isIn(ItemTags.PICKAXES)
                           && mc.player.getMainHandStack().getMaxDamage() - mc.player.getMainHandStack().getDamage()
                              >= mc.player.getMainHandStack().getMaxDamage() * 0.9F
                           && !mc.player.getOffHandStack().isOf(Items.EXPERIENCE_BOTTLE)
                           && InventoryUtils.method0557(Items.EXPERIENCE_BOTTLE) == -1) {
                           if (mc.currentScreen != null) {
                              mc.player.closeHandledScreen();
                           }

                           this.field12149 = false;
                           this.field12143 = AutoMineModule.Nested0489.SPREAD_DIAMONDS;
                           this.field12144.reset();
                           return;
                        }

                        if (!this.field12150 && InventoryUtils.method0557(Items.EXPERIENCE_BOTTLE) == -1) {
                           if (!(mc.currentScreen instanceof GenericContainerScreen genericContainerScreen)) {
                              if (this.field12144.hasExceededMillis(3000L)) {
                                 mc.player.networkHandler.sendChatCommand("ah search опыт");
                                 this.field12144.reset();
                              }

                              return;
                           }

                           if (!genericContainerScreen.getTitle().getString().contains("Подозрительная цена")
                              && !genericContainerScreen.getTitle().getString().contains("Подтверждение покупки")) {
                              if (!AuctionHelperModule.method2310().method2331(genericContainerScreen.getTitle().getString())) {
                                 return;
                              }

                              if (this.field12149) {
                                 if (!this.field12146.hasExceededMillis(3000L)) {
                                    return;
                                 }

                                 this.field12149 = false;
                              }

                              int local5649_3 = -1;
                              long local5649_4 = Long.MAX_VALUE;

                              for (int index = 0; index < mc.player.currentScreenHandler.slots.size() - 36 - 9; index++) {
                                 if (mc.player.currentScreenHandler.getSlot(index).hasStack()
                                    && mc.player.currentScreenHandler.getSlot(index).getStack().isOf(Items.EXPERIENCE_BOTTLE)) {
                                    String local5649_7 = mc.player
                                       .currentScreenHandler
                                       .getSlot(index)
                                       .getStack()
                                       .getName()
                                       .getString()
                                       .replaceAll("(?i)§[0-9a-fk-or]", "");
                                    if (!AuctionHelperModule.method2310().method2321(local5649_7).contains("не актуален")
                                       && !AuctionHelperModule.method2310().method2321(local5649_7).matches(".*\\[\\s*\\d+\\s*ур.*")
                                       && !mc.player
                                          .currentScreenHandler
                                          .getSlot(index)
                                          .getStack()
                                          .getOrDefault(DataComponentTypes.LORE, LoreComponent.DEFAULT)
                                          .lines()
                                          .stream()
                                          .anyMatch(
                                             text -> AuctionHelperModule.method2310()
                                                .method2321(text.getString().replaceAll("(?i)§[0-9a-fk-or]", ""))
                                                .matches(".*содержит.*опыт.*")
                                          )
                                       && this.method3507(mc.player.currentScreenHandler.getSlot(index).getStack())
                                       && AuctionHelperModule.method2310().method2314(mc.player.currentScreenHandler.getSlot(index).getStack())
                                          >= 0
                                       && !(
                                          AuctionHelperModule.method2310().method2314(mc.player.currentScreenHandler.getSlot(index).getStack())
                                             > this.legacyNumberSetting.getValue()
                                       )
                                       && AuctionHelperModule.method2310().method2314(mc.player.currentScreenHandler.getSlot(index).getStack())
                                          < local5649_4) {
                                       local5649_3 = index;
                                       local5649_4 = AuctionHelperModule.method2310()
                                          .method2314(mc.player.currentScreenHandler.getSlot(index).getStack());
                                    }
                                 }
                              }

                              if (local5649_3 != -1) {
                                 InventoryUtils.method0547(local5649_3, 0, SlotActionType.PICKUP, false);
                                 this.field12149 = true;
                                 this.field12146.reset();
                                 this.field12144.reset();
                                 return;
                              }

                              if (this.field12144.hasExceededMillis(400L)) {
                                 InventoryUtils.method0547(mc.player.currentScreenHandler.slots.size() - 36 - 5, 0, SlotActionType.PICKUP, false);
                                 this.field12144.reset();
                              }
                              break;
                           }

                           if (mc.player
                              .currentScreenHandler
                              .slots
                              .stream()
                              .anyMatch(
                                 slot -> slot.hasStack()
                                    && AuctionHelperModule.method2310()
                                       .method2321(slot.getStack().getName().getString().replaceAll("(?i)§[0-9a-fk-or]", ""))
                                       .contains("не актуален")
                              )) {
                              mc.player.closeHandledScreen();
                              mc.player.networkHandler.sendChatCommand("ah search опыт");
                              this.field12149 = false;
                              this.field12144.reset();
                              return;
                           }

                           InventoryUtils.method0547(0, 0, SlotActionType.QUICK_MOVE, false);
                           this.field12144.reset();
                           return;
                        }

                        if (mc.currentScreen != null) {
                           mc.player.closeHandledScreen();
                        }

                        this.field12150 = false;
                        this.field12149 = false;
                        this.field12143 = AutoMineModule.Nested0489.XP_THROW;
                        this.field12144.reset();
                        return;
}
                     case XP_THROW:
                        {
mc.player.closeHandledScreen();
                        this.method3469();
                        if (!mc.player.currentScreenHandler.getCursorStack().isEmpty()) {
                           InventoryUtils.method0547(InventoryUtils.method0533(InventoryUtils.method0546(0, 35)), 0, SlotActionType.PICKUP, false);
                           return;
                        }

                        if (mc.player.getMainHandStack().isIn(ItemTags.PICKAXES)
                           && mc.player.getMainHandStack().getMaxDamage() - mc.player.getMainHandStack().getDamage()
                              >= mc.player.getMainHandStack().getMaxDamage() * 0.9F
                           && !mc.player.getOffHandStack().isOf(Items.EXPERIENCE_BOTTLE)
                           && InventoryUtils.method0557(Items.EXPERIENCE_BOTTLE) == -1) {
                           if (!this.field12166.isEmpty()
                              && mc.player.getOffHandStack().isEmpty()
                              && InventoryUtils.method0555(itemStack6 -> ItemStack.areItemsAndComponentsEqual(itemStack6, this.field12166)) != -1) {
                              InventoryUtils.method0547(
                                 InventoryUtils.method0533(
                                    InventoryUtils.method0555(itemStack7 -> ItemStack.areItemsAndComponentsEqual(itemStack7, this.field12166))
                                 ),
                                 0,
                                 SlotActionType.PICKUP,
                                 false
                              );
                              InventoryUtils.method0547(InventoryUtils.method0533(40), 0, SlotActionType.PICKUP, false);
                              this.field12166 = ItemStack.EMPTY;
                              return;
                           }

                           this.field12166 = ItemStack.EMPTY;
                           this.field12143 = AutoMineModule.Nested0489.SPREAD_DIAMONDS;
                           this.field12144.reset();
                           return;
                        }

                        if (mc.player.getOffHandStack().isOf(Items.EXPERIENCE_BOTTLE)) {
                           RotationManager.method0658()
                              .method0660(
                                 new RotationAngles(mc.player.getYaw(), 90.0F),
                                 new RotationSpeed(0.0F, Math.abs(90.0F - mc.player.getPitch()) * 0.3F, 30.0F, 30.0F),
                                 RotationManager.ReturnMode.Empty
                              );
                           if (mc.player.getPitch() < 75.0F) {
                              return;
                           }

                           InventoryUtils.method0542(Hand.OFF_HAND);
                           return;
                        }

                        if (InventoryUtils.method0557(Items.EXPERIENCE_BOTTLE) != -1) {
                           InventoryUtils.method0547(
                              InventoryUtils.method0533(InventoryUtils.method0557(Items.EXPERIENCE_BOTTLE)), 0, SlotActionType.PICKUP, false
                           );
                           InventoryUtils.method0547(InventoryUtils.method0533(40), 0, SlotActionType.PICKUP, false);
                           if (!mc.player.currentScreenHandler.getCursorStack().isEmpty()) {
                              InventoryUtils.method0547(
                                 InventoryUtils.method0533(InventoryUtils.method0546(0, 35)), 0, SlotActionType.PICKUP, false
                              );
                           }

                           return;
                        }

                        if (!this.field12144.hasExceededMillis(1500L)) {
                           return;
                        }

                        if (mc.player.getMainHandStack().getMaxDamage() - mc.player.getMainHandStack().getDamage()
                           < mc.player.getMainHandStack().getMaxDamage() * 0.93F) {
                           mc.player.networkHandler.sendChatCommand("ah search опыт");
                           this.field12143 = AutoMineModule.Nested0489.AH_BUY;
                           this.field12144.reset();
                           return;
                        }

                        this.field12143 = AutoMineModule.Nested0489.SPREAD_DIAMONDS;
                        this.field12144.reset();
                        break;
}
                     case SPREAD_DIAMONDS:
                        {
if (!this.field12144.hasExceededMillis(150L)) {
                           return;
                        }

                        if (!mc.player.currentScreenHandler.getCursorStack().isEmpty()) {
                           if (!mc.player.currentScreenHandler.getCursorStack().isOf(Items.DIAMOND)) {
                              InventoryUtils.method0547(
                                 InventoryUtils.method0533(InventoryUtils.method0546(0, 35)), 0, SlotActionType.PICKUP, false
                              );
                              this.field12144.reset();
                              return;
                           }

                           if (InventoryUtils.method0546(0, 35) != -1) {
                              InventoryUtils.method0547(
                                 InventoryUtils.method0533(InventoryUtils.method0546(0, 35)), 1, SlotActionType.PICKUP, false
                              );
                           } else {
                              InventoryUtils.method0547(-999, 0, SlotActionType.PICKUP, false);
                           }

                           this.field12144.reset();
                           return;
                        }

                        if (InventoryUtils.method0555(itemStack8 -> itemStack8.isOf(Items.DIAMOND) && itemStack8.getCount() > 1) != -1
                           && InventoryUtils.method0546(0, 35) != -1) {
                           InventoryUtils.method0547(
                              InventoryUtils.method0533(
                                 InventoryUtils.method0555(itemStack9 -> itemStack9.isOf(Items.DIAMOND) && itemStack9.getCount() > 1)
                              ),
                              0,
                              SlotActionType.PICKUP,
                              false
                           );
                           this.field12144.reset();
                           return;
                        }

                        if (InventoryUtils.method0555(itemStack10 -> itemStack10.isOf(Items.DIAMOND) && itemStack10.getCount() > 1) != -1) {
                           InventoryUtils.method0547(
                              InventoryUtils.method0533(
                                 InventoryUtils.method0555(itemStack11 -> itemStack11.isOf(Items.DIAMOND) && itemStack11.getCount() > 1)
                              ),
                              1,
                              SlotActionType.THROW,
                              false
                           );
                           return;
                        }

                        this.method3485();
}
                  }
               } else {
                  mc.interactionManager.cancelBlockBreaking();
                  if (!mc.player.getOffHandStack().isEmpty()
                     && !mc.player.getOffHandStack().isOf(Items.EXPERIENCE_BOTTLE)
                     && InventoryUtils.method0546(0, 35) != -1) {
                     this.field12166 = mc.player.getOffHandStack().copy();
                     InventoryUtils.method0547(InventoryUtils.method0533(40), 0, SlotActionType.PICKUP, false);
                     InventoryUtils.method0547(InventoryUtils.method0533(InventoryUtils.method0546(0, 35)), 0, SlotActionType.PICKUP, false);
                  }

                  mc.player.networkHandler.sendChatCommand("an" + this.numericTextSetting.getText().replaceAll("\\D", ""));
                  this.field12151 = "an" + this.numericTextSetting.getText().replaceAll("\\D", "");
                  this.method3478(1500L, () -> {
                     mc.player.networkHandler.sendChatCommand("home");
                     this.field12143 = AutoMineModule.Nested0489.HOME_TELEPORT;
                     this.field12144.reset();
                  });
               }
            }
         } else {
            this.method3459();
         }
      }
   };
   private final EventListener<PacketEvent> field12170 = packetEvent -> {
      if (packetEvent.isIncoming()
         && packetEvent.getPacket() instanceof GameMessageS2CPacket gameMessageS2CPacket
         && field12131.matcher(AuctionHelperModule.method2310().method2321(gameMessageS2CPacket.content().getString().replaceAll("(?i)§[0-9a-fk-or]", ""))).matches()) {
         this.field12150 = true;
      }
   };
   private final EventListener<PacketEvent> field12171 = packetEvent2 -> {
      if (packetEvent2.isIncoming() && packetEvent2.getPacket() instanceof GameMessageS2CPacket gameMessageS2CPacket2) {
         if (Formatting.strip(gameMessageS2CPacket2.content().getString()).toLowerCase().contains("delnmt")) {
            packetEvent2.method4424(true);
         }
      }
   };
   private final EventListener<MovementInputEvent> field12172 = movementInputEvent -> {
      if (mc.player != null && this.field12155 != null && this.field12143 != AutoMineModule.Nested0489.BARITONE_RIDE) {
         movementInputEvent.method4446(1.0F);
         RotationManager.method0659(
            movementInputEvent,
            mc.player.getYaw(),
            (float)(
                  Math.atan2(this.field12155.getZ() + 0.5 - mc.player.getZ(), this.field12155.getX() + 0.5 - mc.player.getX())
                     * 180.0
                     / Math.PI
               )
               - 90.0F
         );
         if (!mc.player.horizontalCollision) {
            this.field12161 = 0L;
         } else {
            if (this.field12161 == 0L) {
               this.field12161 = System.currentTimeMillis();
            }
         }
      }
   };
   private final EventListener<WorldRenderEvent> field12173 = worldRenderEvent -> {
      if (mc.player != null && mc.world != null && !this.field12159.isEmpty()) {
         for (BlockPos blockPos : this.field12159) {
            worldRenderEvent.method0647().push();
            worldRenderEvent.method0647()
               .translate(
                  blockPos.getX() - mc.gameRenderer.getCamera().getCameraPos().x,
                  blockPos.getY() - mc.gameRenderer.getCamera().getCameraPos().y,
                  blockPos.getZ() - mc.gameRenderer.getCamera().getCameraPos().z
               );
            Matrix4f positionMatrix = worldRenderEvent.method0647().peek().getPositionMatrix();
            VertexConsumer vertexConsumer = mc.getBufferBuilders().getEntityVertexConsumers().getBuffer(RenderLayers.debugQuads());
            float[] floatArray = new float[]{0.0F, 1.0F};
            float[] floatArray2 = new float[]{0.0F, 1.0F};
            float[] floatArray3 = new float[]{0.0F, 1.0F};
            int[][] intArrayArray = new int[][]{{0, 1, 3, 2}, {4, 6, 7, 5}, {0, 4, 5, 1}, {2, 3, 7, 6}, {0, 2, 6, 4}, {1, 5, 7, 3}};

            for (int[] intArray : intArrayArray) {
               for (int local5645_17 : intArray) {
                  vertexConsumer.vertex(positionMatrix, floatArray[local5645_17 >> 2], floatArray2[local5645_17 >> 1 & 1], floatArray3[local5645_17 & 1])
                     .color(Colors.method3727(blockPos.equals(this.field12155) ? -16711826 : -16718337, 0.27450982F));
               }
            }

            VertexRendering.drawOutline(
               worldRenderEvent.method0647(),
               mc.getBufferBuilders().getEntityVertexConsumers().getBuffer(RenderLayers.lines()),
               VoxelShapes.cuboid(new Box(0.0, 0.0, 0.0, 1.0, 1.0, 1.0)),
               0.0,
               0.0,
               0.0,
               Colors.method3727(blockPos.equals(this.field12155) ? -16711826 : -16718337, 0.8F),
               1.0F
            );
            mc.getBufferBuilders().getEntityVertexConsumers().draw();
            worldRenderEvent.method0647().pop();
         }
      }
   };

   private void method3459() {
      if (this.field12148 != null
         && (
            mc.currentScreen instanceof DisconnectedScreen
               || mc.currentScreen instanceof TitleScreen
               || mc.currentScreen instanceof MultiplayerScreen
         )
         && this.field12147.hasExceededMillis(5000L)) {
         this.field12147.reset();
         this.field12143 = AutoMineModule.Nested0489.SCAN_TITLE;
         this.field12151 = null;
         this.field12152 = false;
         this.field12164 = null;
         this.field12149 = false;
         this.field12150 = false;
         this.field12158 = 0L;
         this.field12144.reset();
         this.field12145.reset();
         ConnectScreen.connect(new MultiplayerScreen(new TitleScreen()), mc, ServerAddress.parse(this.field12148.address), this.field12148, false, null);
      }
   }

   public static AutoMineModule method3460() {
      if (autoMineModule == null) {
         autoMineModule = new AutoMineModule();
      }

      return autoMineModule;
   }

   private boolean method3463(BlockPos blockPos) {
      return !this.method3477(blockPos) && (!this.method3493(blockPos) || !this.method3493(blockPos.up()));
   }

   private boolean method3467(BlockPos blockPos) {
      Vec3d vec3d = new Vec3d(blockPos.getX() + 0.5 - mc.player.getX(), 0.0, blockPos.getZ() + 0.5 - mc.player.getZ());
      if (vec3d.lengthSquared() < 0.01) {
         return false;
      }

      Vec3d vec3d2 = new Vec3d(-vec3d.z, 0.0, vec3d.x).normalize().multiply(0.35);

      for (double local5650_4 = 0.0; local5650_4 <= Math.min(vec3d.length(), 48.0); local5650_4 += 0.25) {
         Vec3d vec3d3 = mc.player.getEntityPos().add(vec3d.normalize().multiply(local5650_4));
         if (this.method3463(BlockPos.ofFloored(vec3d3))
            || this.method3463(BlockPos.ofFloored(vec3d3.add(vec3d2)))
            || this.method3463(BlockPos.ofFloored(vec3d3.subtract(vec3d2)))) {
            return true;
         }
      }

      return false;
   }

   private void method3469() {
      if (!mc.player.getMainHandStack().isIn(ItemTags.PICKAXES)
         && InventoryUtils.method0555(itemStack -> itemStack.isIn(ItemTags.PICKAXES)) != -1) {
         if (InventoryUtils.method0555(itemStack2 -> itemStack2.isIn(ItemTags.PICKAXES)) <= 8) {
            InventoryUtils.method0553(InventoryUtils.method0555(itemStack3 -> itemStack3.isIn(ItemTags.PICKAXES)), false);
         } else if (!InventoryActionQueue.method2656().method2665()) {
            InventoryActionQueue.method2656()
               .method2662(InventoryUtils.method0555(itemStack4 -> itemStack4.isIn(ItemTags.PICKAXES)), InventoryUtils.method0554(), 1);
         }
      }
   }

   private Optional<String> method3474(String local5657_1, Pattern pattern, double local5657_3) {
      if (mc.player != null && mc.world != null && local5657_1 != null && pattern != null) {
         List<ArmorStandEntity> local5657_5 = mc.world
            .getOtherEntities(
               mc.player,
               mc.player.getBoundingBox().expand(local5657_3),
               entity -> entity instanceof ArmorStandEntity armorStandEntity && armorStandEntity.hasCustomName()
            )
            .stream()
            .map(ArmorStandEntity.class::cast)
            .sorted(Comparator.comparingDouble(armorStandEntity2 -> armorStandEntity2.squaredDistanceTo(mc.player)))
            .toList();

         for (ArmorStandEntity armorStandEntity3 : local5657_5) {
            if (armorStandEntity3.getName()
               .getString()
               .replaceAll("(?i)§[0-9a-fk-or]", "")
               .trim()
               .toLowerCase()
               .replace("-", "")
               .replace(" ", "")
               .contains(local5657_1.toLowerCase())) {
               ArmorStandEntity armorStandEntity4 = local5657_5.stream()
                  .filter(armorStandEntity5 -> pattern.matcher(armorStandEntity5.getName().getString().replaceAll("(?i)§[0-9a-fk-or]", "").trim()).matches())
                  .min(Comparator.comparingDouble(armorStandEntity6 -> Math.abs(armorStandEntity6.getY() - armorStandEntity3.getY())))
                  .orElse(null);
               if (armorStandEntity4 != null) {
                  return Optional.of(armorStandEntity4.getName().getString().replaceAll("(?i)§[0-9a-fk-or]", "").trim());
               }
            }
         }

         return Optional.empty();
      } else {
         return Optional.empty();
      }
   }

   static {
      for (int[] intArray2 : new int[][]{{101, 114}, {201, 228}, {301, 325}, {501, 516}, {901, 904}}) {
         for (int index2 = intArray2[0]; index2 <= intArray2[1]; index2++) {
            field12133.add("an" + index2);
         }
      }

      field12134 = new int[][]{
         {-53, 69, 33},
         {-55, 69, 39},
         {-55, 69, 30},
         {-50, 69, 33},
         {-67, 69, 25},
         {-55, 69, 25},
         {-51, 69, 30},
         {-52, 69, 43},
         {-62, 69, 40},
         {-67, 69, 40},
         {-67, 69, 26},
         {-63, 69, 27},
         {-61, 69, 30},
         {-56, 69, 32}
      };
      field12135 = new BlockPos(-47, 78, 31);
      field12136 = new BlockPos(-47, 78, 37);
      field12137 = new BlockPos(-69, 69, 24);
      field12138 = new BlockPos(-49, 78, 44);
      field12139 = new String[]{
         "#set renderPath false",
         "#set renderGoal false",
         "#set allowPlace true",
         "#set allowSprint true",
         "#set costHeuristic 1.0",
         "#set allowParkour true",
         "#set allowDiagonalDescend true",
         "#set allowDownward true",
         "#set allowBreak true",
         "#set allowDiagonalAscend true",
         "#set allowParkourAscend true",
         "#set allowParkourPlace true",
         "#set maxFallHeightNoWater 255",
         "#set maxFallHeightBucket 255",
         "#set allowWaterBucketFall false",
         "#set assumeWalkOnLava true",
         "#set assumeWalkOnWater true",
         "#set sprintInWater true",
         "#set blockBreakAdditionalPenalty 0.0",
         "#set chunkCaching false",
         "#set pruneRegionsFromRAM true",
         "#set renderCachedChunks false",
         "#set blocksToDisallowBreaking minecraft:andesite,minecraft:infested_stone_bricks,minecraft:stone_bricks,minecraft:infested_chiseled_stone_bricks,minecraft:chiseled_stone_bricks,minecraft:andesite_wall,minecraft:stone_brick_stairs,minecraft:gray_concrete_powder,minecraft:light_gray_concrete,minecraft:ladder,minecraft:gray_wool,minecraft:red_wool,minecraft:stone_stairs,minecraft:red_terracotta,minecraft:red_sand,minecraft:red_concrete,minecraft:andesite_stairs,minecraft:brick_slab,minecraft:stone_slab,minecraft:stone_brick_slab,minecraft:red_glazed_terracotta,minecraft:red_concrete_powder,minecraft:crimson_stairs,minecraft:snow_block,minecraft:snow,minecraft:coarse_dirt,minecraft:dirt,minecraft:brown_wool,minecraft:oak_leaves,minecraft:brown_concrete,minecraft:white_concrete,minecraft:brown_concrete_powder,minecraft:white_concrete_powder,minecraft:oak_slab,minecraft:stripped_spruce_wood,minecraft:spruce_stairs,minecraft:spruce_fence,minecraft:prismarine,minecraft:clay,minecraft:light_gray_concrete_powder,minecraft:light_blue_stained_glass,minecraft:dark_oak_slab,minecraft:stripped_spruce_log,minecraft:short_grass,minecraft:podzol,minecraft:grass_block,minecraft:ice,minecraft:packed_ice,minecraft:blue_ice,minecraft:blue_terracotta,minecraft:brown_terracotta,minecraft:blue_concrete_powder,minecraft:glass_pane,minecraft:glass,minecraft:orange_terracotta,minecraft:gray_terracotta,minecraft:stripped_oak_log,minecraft:stripped_dark_oak_log,minecraft:spruce_leaves,minecraft:mud_bricks,minecraft:mud_brick_stairs,minecraft:packed_mud,minecraft:cobblestone,minecraft:cobblestone_wall,minecraft:cobblestone_stairs,minecraft:cobblestone_slab,minecraft:stone_brick_wall,minecraft:polished_andesite,minecraft:andesite_slab,minecraft:gravel,minecraft:tuff,minecraft:orange_glazed_terracotta,minecraft:brown_glazed_terracotta,minecraft:note_block,minecraft:jukebox,minecraft:barrel,minecraft:stripped_dark_oak_wood,minecraft:stripped_oak_wood"
      };
   }

   private boolean method3477(BlockPos blockPos) {
      return blockPos.getX() >= field12137.getX()
         && blockPos.getX() <= field12138.getX()
         && blockPos.getY() >= field12137.getY()
         && blockPos.getY() <= field12138.getY()
         && blockPos.getZ() >= field12137.getZ()
         && blockPos.getZ() <= field12138.getZ();
   }

   private void method3478(long local5662_1, Runnable runnable) {
      this.field12164 = runnable;
      this.field12165 = System.currentTimeMillis() + local5662_1;
   }

   private AutoMineModule() {
      this.modeSetting = this.addSetting(new ModeSetting("Сервер", "Какой список анархий перебирать.", "SpookyTime", "SpookyTime", "FunTime"));
      this.numericTextSetting = this.addSetting(
         new NumericTextSetting("Анархия дома", "Номер анархии, куда лететь чинить кирку: там должен стоять /home рядом с аукционом.", "101", 4).method1539()
      );
      this.legacyNumberSetting = this.addSetting(
         new LegacyNumberSetting("Цена стака опыта", "Дороже этого стак опыта на аукционе не покупать.", 200000.0, 10000.0, 1000000.0, 10000.0)
      );
   }

   private boolean method3482() {
      if (this.field12143 == AutoMineModule.Nested0489.BARITONE_RIDE) {
         return false;
      }

      this.method3469();
      if (System.currentTimeMillis() >= this.field12160) {
         this.field12160 = System.currentTimeMillis() + 200L;
         this.field12159.clear();

         for (BlockPos blockPos : BlockPos.iterate(mc.player.getBlockPos().add(-35, -35, -35), mc.player.getBlockPos().add(35, 35, 35))) {
            if ((
                  mc.world.getBlockState(blockPos).isOf(Blocks.DIAMOND_ORE)
                     || mc.world.getBlockState(blockPos).isOf(Blocks.DEEPSLATE_DIAMOND_ORE)
               )
               && this.method3477(blockPos)
               && !(blockPos.getY() - mc.player.getY() > 6.0)
               && !(mc.player.getEyePos().squaredDistanceTo(blockPos.getX() + 0.5, blockPos.getY() + 0.5, blockPos.getZ() + 0.5) > 1225.0)) {
               this.field12159.add(blockPos.toImmutable());
            }
         }

         this.field12157 = this.field12159.size();
      }

      this.field12159
         .removeIf(
            blockPos2 -> !mc.world.getBlockState(blockPos2).isOf(Blocks.DIAMOND_ORE)
               && !mc.world.getBlockState(blockPos2).isOf(Blocks.DEEPSLATE_DIAMOND_ORE)
         );
      BlockPos blockPos3 = null;
      double local5667_2 = Double.MAX_VALUE;
      boolean local5667_4 = mc.player.getBlockPos().getY() == field12135.getY();

      for (BlockPos blockPos4 : this.field12159) {
         double local5667_7 = mc.player.getEyePos().squaredDistanceTo(blockPos4.getX() + 0.5, blockPos4.getY() + 0.5, blockPos4.getZ() + 0.5)
            + (local5667_4 && this.method3467(blockPos4) ? 4096.0 : 0.0);
         if (!(local5667_7 >= local5667_2)) {
            local5667_2 = local5667_7;
            blockPos3 = blockPos4;
         }
      }

      if (blockPos3 == null) {
         return false;
      }

      if (!this.field12163.isEmpty()
         && (
            this.field12163.peekFirst().getY() != mc.player.getBlockPos().getY()
               || !this.method3493(this.field12163.peekFirst())
               || !this.method3493(this.field12163.peekFirst().up())
               || Math.hypot(
                     this.field12163.peekFirst().getX() + 0.5 - mc.player.getX(), this.field12163.peekFirst().getZ() + 0.5 - mc.player.getZ()
                  )
                  > 3.0
         )) {
         this.field12163.clear();
      }

      while (
         !this.field12163.isEmpty()
            && Math.hypot(
                  this.field12163.peekFirst().getX() + 0.5 - mc.player.getX(), this.field12163.peekFirst().getZ() + 0.5 - mc.player.getZ()
               )
               < 0.3
      ) {
         this.field12163.pollFirst();
      }

      if (this.field12163.isEmpty() && local5667_4 && local5667_2 >= 4096.0 && System.currentTimeMillis() >= this.field12162) {
         this.field12162 = System.currentTimeMillis() + 1000L;
         this.method3503(blockPos3);
      }

      boolean local5667_5 = !this.field12163.isEmpty();
      if (!local5667_5 && this.field12161 != 0L && System.currentTimeMillis() - this.field12161 >= 34L) {
         Box box = mc.player
            .getBoundingBox()
            .offset(
               new Vec3d(blockPos3.getX() + 0.5 - mc.player.getX(), 0.0, blockPos3.getZ() + 0.5 - mc.player.getZ())
                  .normalize()
                  .multiply(0.3)
            );
         BlockPos blockPos5 = null;

         for (BlockPos blockPos6 : BlockPos.iterate(
            BlockPos.ofFloored(box.minX, box.minY, box.minZ), BlockPos.ofFloored(box.maxX, box.maxY, box.maxZ)
         )) {
            if (!mc.world.getBlockState(blockPos6).getCollisionShape(mc.world, blockPos6).isEmpty()
               && this.method3477(blockPos6)
               && (
                  blockPos5 == null
                     || blockPos6.getY() < blockPos5.getY()
                     || blockPos6.getY() == blockPos5.getY()
                        && mc.player.getEyePos().squaredDistanceTo(blockPos6.getX() + 0.5, blockPos6.getY() + 0.5, blockPos6.getZ() + 0.5)
                           < mc.player.getEyePos().squaredDistanceTo(blockPos5.getX() + 0.5, blockPos5.getY() + 0.5, blockPos5.getZ() + 0.5)
               )) {
               blockPos5 = blockPos6.toImmutable();
            }
         }

         if (blockPos5 != null) {
            blockPos3 = blockPos5;
         }
      }

      if (local5667_5) {
         blockPos3 = this.field12163.peekFirst();
      }

      this.field12155 = blockPos3;
      this.field12156 = true;
      boolean local5667_6 = (blockPos3.getX() + 0.5 - mc.player.getEyePos().x) * (blockPos3.getX() + 0.5 - mc.player.getEyePos().x)
            + (blockPos3.getZ() + 0.5 - mc.player.getEyePos().z) * (blockPos3.getZ() + 0.5 - mc.player.getEyePos().z)
         < 0.0925;
      float local5667_7 = local5667_6
         ? mc.player.getYaw()
         : (float)(
            Math.toDegrees(Math.atan2(blockPos3.getZ() + 0.5 - mc.player.getEyePos().z, blockPos3.getX() + 0.5 - mc.player.getEyePos().x))
               - 90.0
         );
      float local5667_8 = (float)(
         -Math.toDegrees(
            Math.atan2(
               blockPos3.getY() + 0.5 - mc.player.getEyePos().y,
               Math.sqrt(
                  (blockPos3.getX() + 0.5 - mc.player.getEyePos().x) * (blockPos3.getX() + 0.5 - mc.player.getEyePos().x)
                     + (blockPos3.getZ() + 0.5 - mc.player.getEyePos().z) * (blockPos3.getZ() + 0.5 - mc.player.getEyePos().z)
               )
            )
         )
      );
      if (local5667_5) {
         local5667_8 = mc.player.getPitch();
      }

      boolean local5667_9 = local5667_5
         ? Math.abs(MathHelper.wrapDegrees(local5667_7 - mc.player.getYaw())) < 3.0F
         : mc.player.raycast(mc.player.getBlockInteractionRange(), 1.0F, false) instanceof BlockHitResult blockHitResult
            && blockHitResult.getType() == Type.BLOCK
            && blockHitResult.getBlockPos().equals(blockPos3);
      if (!local5667_9) {
         RotationManager.method0658()
            .method0660(
               new RotationAngles(local5667_7, local5667_8),
               new RotationSpeed(
                  Math.abs(MathHelper.wrapDegrees(local5667_7 - mc.player.getYaw())) * 0.3F,
                  Math.abs(local5667_8 - mc.player.getPitch()) * 0.3F,
                  30.0F,
                  30.0F
               ),
               RotationManager.ReturnMode.Empty
            );
      }

      if (local5667_5) {
         return true;
      }

      if (mc.player.raycast(mc.player.getBlockInteractionRange(), 1.0F, false) instanceof BlockHitResult blockHitResult2
         && blockHitResult2.getType() == Type.BLOCK) {
         mc.crosshairTarget = blockHitResult2;
         mc.interactionManager.updateBlockBreakingProgress(blockHitResult2.getBlockPos(), blockHitResult2.getSide());
         mc.player.swingHand(Hand.MAIN_HAND);
      }

      return true;
   }

   private void method3485() {
      String local5670_1;
      do {
         local5670_1 = this.modeSetting.isSelected("FunTime")
            ? field12133.get(ThreadLocalRandom.current().nextInt(field12133.size()))
            : field12132[ThreadLocalRandom.current().nextInt(field12132.length)];
      } while (local5670_1.equals(this.field12151));

      mc.player.networkHandler.sendChatCommand(local5670_1);
      this.field12151 = local5670_1;
      this.field12152 = false;
      this.method3478(1500L, () -> {
         this.field12143 = AutoMineModule.Nested0489.SCAN_TITLE;
         this.field12144.reset();
         this.field12145.reset();
         this.field12158 = 0L;
      });
   }

   @Override
   protected void onEnable() {
      this.field12143 = AutoMineModule.Nested0489.SCAN_TITLE;
      this.field12151 = null;
      this.field12152 = false;
      this.field12153 = false;
      this.field12164 = null;
      this.field12155 = null;
      this.field12156 = false;
      this.field12149 = false;
      this.field12150 = false;
      this.field12158 = 0L;
      this.field12161 = 0L;
      this.field12163.clear();
      this.field12168 = false;
      this.field12144.reset();
      this.field12145.reset();
   }

   private boolean method3489() {
      if (mc.player.getHungerManager().getFoodLevel() <= 6
         || this.field12143 == AutoMineModule.Nested0489.STAND_READY && mc.player.getHungerManager().getFoodLevel() < 20) {
         if (InventoryUtils.method0555(itemStack -> itemStack.contains(DataComponentTypes.FOOD)) == -1) {
            if (this.field12167) {
               mc.options.useKey.setPressed(false);
            }

            this.field12167 = false;
            return false;
         } else if (!mc.player.getMainHandStack().contains(DataComponentTypes.FOOD)) {
            if (InventoryUtils.method0555(itemStack2 -> itemStack2.contains(DataComponentTypes.FOOD)) <= 8) {
               InventoryUtils.method0553(InventoryUtils.method0555(itemStack3 -> itemStack3.contains(DataComponentTypes.FOOD)), false);
            } else if (!InventoryActionQueue.method2656().method2665()) {
               InventoryActionQueue.method2656()
                  .method2662(InventoryUtils.method0555(itemStack4 -> itemStack4.contains(DataComponentTypes.FOOD)), InventoryUtils.method0554(), 1);
            }

            return true;
         } else {
            mc.interactionManager.cancelBlockBreaking();
            mc.options.useKey.setPressed(true);
            this.field12167 = true;
            return true;
         }
      } else {
         if (this.field12167) {
            mc.options.useKey.setPressed(false);
         }

         this.field12167 = false;
         return false;
      }
   }

   private boolean method3493(BlockPos blockPos) {
      return mc.world.getBlockState(blockPos).getCollisionShape(mc.world, blockPos).isEmpty();
   }

   @Override
   protected void onDisable() {
      if (this.field12167) {
         mc.options.useKey.setPressed(false);
      }

      this.field12167 = false;
      this.field12168 = false;
      mc.options.sneakKey.setPressed(false);
      mc.options.attackKey.setPressed(false);
      this.field12166 = ItemStack.EMPTY;
      if (mc.player != null) {
         if (this.field12143 == AutoMineModule.Nested0489.BARITONE_RIDE) {
            ClientChat.method2258("#stop");
         }

         if ((this.field12143 == AutoMineModule.Nested0489.DIG_MINE || this.field12143 == AutoMineModule.Nested0489.STAND_READY)
            && mc.interactionManager != null) {
            mc.interactionManager.cancelBlockBreaking();
         }
      }
   }

   private void method3499() {
      this.field12156 = false;
   }

   public boolean method3500(String local5687_1) {
      return local5687_1.replaceAll("(?i)§[0-9a-fk-or]", "").strip().startsWith("[Baritone]")
         || local5687_1.replaceAll("(?i)§[0-9a-fk-or]", "").strip().startsWith("Baritone");
   }

   private void method3503(BlockPos blockPos) {
      BlockPos blockPos2 = mc.player.getBlockPos();
      BlockPos blockPos3 = null;
      boolean local5690_4 = field12135.getX() == field12136.getX();
      HashMap<BlockPos, BlockPos> local5690_5 = new HashMap<>();
      ArrayDeque<BlockPos> local5690_6 = new ArrayDeque<>();
      local5690_5.put(blockPos2, blockPos2);
      local5690_6.add(blockPos2);

      while (!local5690_6.isEmpty() && blockPos3 == null) {
         BlockPos blockPos4 = local5690_6.pollFirst();
         if (local5690_4
            ? blockPos4.getX() == field12135.getX()
               && blockPos4.getZ() > Math.min(field12135.getZ(), field12136.getZ())
               && blockPos4.getZ() < Math.max(field12135.getZ(), field12136.getZ())
            : blockPos4.getZ() == field12135.getZ()
               && blockPos4.getX() > Math.min(field12135.getX(), field12136.getX())
               && blockPos4.getX() < Math.max(field12135.getX(), field12136.getX())) {
            blockPos3 = blockPos4;
         }

         for (Direction direction : net.minecraft.util.math.Direction.Type.HORIZONTAL) {
            BlockPos blockPos5 = blockPos4.offset(direction);
            if (!local5690_5.containsKey(blockPos5)
               && Math.abs(blockPos5.getX() - blockPos2.getX()) <= 40
               && Math.abs(blockPos5.getZ() - blockPos2.getZ()) <= 40
               && this.method3493(blockPos5)
               && this.method3493(blockPos5.up())
               && (!this.method3493(blockPos5.down()) || !this.method3493(blockPos5.down(2)))) {
               local5690_5.put(blockPos5, blockPos4);
               local5690_6.add(blockPos5);
            }
         }
      }

      this.field12163.clear();
      if (blockPos3 != null) {
         for (BlockPos blockPos6 = blockPos3; !blockPos6.equals(blockPos2); blockPos6 = local5690_5.get(blockPos6)) {
            this.field12163.addFirst(blockPos6);
         }

         int local5690_7 = (local5690_4 ? blockPos.getX() < blockPos3.getX() : blockPos.getZ() < blockPos3.getZ()) ? -1 : 1;
         this.field12163.addLast(local5690_4 ? blockPos3.add(local5690_7, 0, 0) : blockPos3.add(0, 0, local5690_7));
         this.field12163.addLast(local5690_4 ? blockPos3.add(local5690_7 * 2, 0, 0) : blockPos3.add(0, 0, local5690_7 * 2));
         mc.interactionManager.cancelBlockBreaking();
      }
   }

   public boolean method3507(ItemStack itemStack) {
      for (Text text : itemStack.getOrDefault(DataComponentTypes.LORE, LoreComponent.DEFAULT).lines()) {
         if (!text.getString().trim().isEmpty()
            && !AuctionHelperModule.method2310().method2321(text.getString()).matches(".*(опыт|содержит|ур|шт).*")) {
            return true;
         }
      }

      return false;
   }

   private enum Nested0489 {
      SCAN_TITLE,
      WARP_TELEPORT,
      BARITONE_RIDE,
      STAND_READY,
      DIG_MINE,
      HOME_TELEPORT,
      DUMP_DIAMONDS,
      AH_BUY,
      XP_THROW,
      SPREAD_DIAMONDS;
   }
}
