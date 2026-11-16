package deluxe.client.module.render;

import deluxe.client.module.movement.AirStuckModule;
import deluxe.client.setting.BooleanSetting;
import deluxe.client.util.Colors;
import deluxe.client.event.EventListener;
import deluxe.client.render.FontFace;
import deluxe.client.storage.FriendStore;
import deluxe.client.event.HudRenderEvent;
import deluxe.client.util.ItemDrawOptions;
import deluxe.client.setting.LegacyNumberSetting;
import deluxe.client.module.Module;
import deluxe.client.module.ModuleCategory;
import deluxe.client.module.ModuleInfo;
import deluxe.client.setting.MultiChoiceSetting;
import deluxe.client.render.PlayerOutlineRenderer;
import deluxe.client.event.PlayerTickEvent;
import deluxe.client.util.GuiScale;
import deluxe.client.render.WorldProjection;
import deluxe.client.render.FontText;
import deluxe.client.render.ItemRenderCache;
import deluxe.client.util.ServerItemType;
import deluxe.client.util.PlayerItemCooldowns;
import deluxe.client.util.EntityUtils;
import deluxe.client.ui.UiTheme;
import deluxe.client.setting.ColorSetting;
import deluxe.client.util.GlintMode;
import deluxe.client.render.ThemeColors;
import deluxe.client.ui.hud.HudRenderContext;
import deluxe.client.ui.hud.HudStyle;
import deluxe.client.render.Render2D;
import deluxe.client.setting.SectionSetting;
import deluxe.client.module.player.ServerHelperModule;
import deluxe.client.event.WorldRenderEvent;

import it.unimi.dsi.fastutil.objects.Object2IntMap.Entry;
import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.FlyingItemEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.projectile.AbstractWindChargeEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.scoreboard.Team;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import org.joml.Vector4f;

@ModuleInfo(name = "Tags", description = "Ники, предметы, броня и руки игроков поверх мира.", category = ModuleCategory.RENDER)
public final class TagsModule extends Module {
   private static TagsModule tagsModule;
   private static int field13981 = new Color(15, 15, 15, 155).getRGB();
   private static int field13982 = new Color(0, 255, 0, 40).getRGB();
   private static int field13983 = new Color(255, 70, 70, 255).getRGB();
   private static int field13984 = new Color(190, 30, 30, 150).getRGB();
   private static int field13985 = new Color(160, 160, 160, 255).getRGB();
   private static int field13986 = new Color(112, 255, 112, 255).getRGB();
   public MultiChoiceSetting field13987;
   public MultiChoiceSetting field13988;
   public BooleanSetting field13989;
   public BooleanSetting field13990;
   public LegacyNumberSetting field13991;
   public LegacyNumberSetting field13992;
   public LegacyNumberSetting field13993;
   public BooleanSetting field13994;
   public ColorSetting field13995;
   public BooleanSetting field13996;
   public BooleanSetting field13997;
   public ColorSetting field13998;
   private final HashMap<Integer, Vec3d> field13999 = new HashMap<>();
   private final HashMap<Integer, Integer> field14000 = new HashMap<>();
   private final HashMap<Integer, Integer> field14001 = new HashMap<>();
   private final HashMap<Integer, Integer> field14002 = new HashMap<>();
   private final EventListener<PlayerTickEvent> field14003 = playerTickEvent -> {
      if (mc.world != null) {
         if (this.field13999.size() > 256) {
            this.field13999.clear();
            this.field14000.clear();
            this.field14001.clear();
            this.field14002.clear();
         }

         for (AbstractClientPlayerEntity abstractClientPlayerEntity : mc.world.getPlayers()) {
            Vec3d lerpedPos = abstractClientPlayerEntity.getInterpolator().getLerpedPos();
            Vec3d vec3d = this.field13999.put(abstractClientPlayerEntity.getId(), lerpedPos);
            boolean local6875_6 = !abstractClientPlayerEntity.hasVehicle()
               && !abstractClientPlayerEntity.isGliding()
               && abstractClientPlayerEntity.getGameMode() != GameMode.CREATIVE
               && abstractClientPlayerEntity.getGameMode() != GameMode.SPECTATOR
               && !abstractClientPlayerEntity.isTouchingWater()
               && !abstractClientPlayerEntity.isInLava()
               && !abstractClientPlayerEntity.isClimbing()
               && mc.world.isSpaceEmpty(abstractClientPlayerEntity, abstractClientPlayerEntity.getBoundingBox().offset(0.0, -0.0625, 0.0));
            boolean local6875_7 = vec3d != null && vec3d.squaredDistanceTo(lerpedPos) < 1.0E-4;
            int local6875_8 = local6875_6 && vec3d != null && !local6875_7 ? this.field14001.getOrDefault(abstractClientPlayerEntity.getId(), 0) + 1 : 0;
            this.field14001.put(abstractClientPlayerEntity.getId(), local6875_8);
            int local6875_9 = local6875_8 >= 25 ? 40 : Math.max(0, this.field14002.getOrDefault(abstractClientPlayerEntity.getId(), 0) - 1);
            this.field14002.put(abstractClientPlayerEntity.getId(), local6875_9);
            this.field14000
               .put(abstractClientPlayerEntity.getId(), local6875_6 && local6875_7 && local6875_9 == 0 ? this.field14000.getOrDefault(abstractClientPlayerEntity.getId(), 0) + 1 : 0);
         }
      }
   };
   private final EventListener<HudRenderEvent> field14004 = hudRenderEvent -> {
      if (mc.player != null && mc.world != null && mc.gameRenderer != null) {
         HudRenderContext.method4333(hudRenderEvent.method4459());
         int scaledWidth = mc.getWindow().getScaledWidth();
         int scaledHeight = mc.getWindow().getScaledHeight();
         float local6889_4 = GuiScale.method0797() / GuiScale.method0794();
         if (this.field14006 != mc.world || this.field14005.size() > 512) {
            this.field14005.clear();
            this.field14006 = mc.world;
         }

         if (this.method4119()) {
            PlayerItemCooldowns.method2683(mc.world);
         }

         for (Entity entity : mc.world.getEntities()) {
            boolean local6889_7 = false;
            if (entity instanceof AbstractClientPlayerEntity abstractClientPlayerEntity2 && this.field13996.isChecked()) {
               this.method4104(abstractClientPlayerEntity2, hudRenderEvent.method4461(), scaledWidth, scaledHeight, local6889_4);
            }

            if (entity instanceof AbstractClientPlayerEntity abstractClientPlayerEntity3) {
               local6889_7 = this.method4100(abstractClientPlayerEntity3, hudRenderEvent.method4461(), scaledWidth, scaledHeight, local6889_4);
            } else if (entity instanceof ItemEntity itemEntity && this.field13988.isIndexSelected(1)) {
               local6889_7 = this.method4107(itemEntity, hudRenderEvent.method4461(), scaledWidth, scaledHeight, local6889_4);
            } else if (entity instanceof ProjectileEntity projectileEntity && this.field13988.isIndexSelected(1)) {
               local6889_7 = this.method4121(projectileEntity, hudRenderEvent.method4461(), scaledWidth, scaledHeight, local6889_4);
            }

            if (local6889_7) {
               Render2D.method4692(hudRenderEvent.method4459());
            }
         }
      }
   };
   private final HashMap<Integer, TagsModule.Nested0518> field14005 = new HashMap<>();
   private ClientWorld field14006;
   private final EventListener<WorldRenderEvent> field14007 = worldRenderEvent -> {};
   private static final int[][] field14008 = new int[][]{{0, 1, 3, 2}, {4, 6, 7, 5}, {0, 4, 5, 1}, {2, 3, 7, 6}, {0, 2, 6, 4}, {1, 5, 7, 3}};

   private ItemStack method4085(ProjectileEntity projectileEntity) {
      ItemStack itemStack = projectileEntity instanceof PersistentProjectileEntity persistentProjectileEntity
         ? persistentProjectileEntity.getItemStack()
         : (projectileEntity instanceof FlyingItemEntity flyingItemEntity ? flyingItemEntity.getStack() : ItemStack.EMPTY);
      return itemStack.isEmpty() && projectileEntity instanceof AbstractWindChargeEntity ? Items.WIND_CHARGE.getDefaultStack() : itemStack;
   }

   private int method4086(AbstractClientPlayerEntity abstractClientPlayerEntity, boolean local6849_2) {
      return this.method4118(abstractClientPlayerEntity) ? field13984 : (local6849_2 ? field13982 : Colors.method3728(ThemeColors.field13661, 133));
   }

   private void method4088(ItemStack itemStack, float local6851_2, float local6851_3) {
      int local6851_5 = 0;
      String local6851_4;
      if (!this.method4112(itemStack) && !this.method4089(itemStack)) {
         if (itemStack.isIn(ItemTags.SWORDS) && (local6851_5 = this.method4113(itemStack, "minecraft:knockback")) > 0) {
            local6851_4 = "От";
         } else {
            if (!this.method4093(itemStack)) {
               return;
            }

            local6851_4 = "По";
            local6851_5 = this.method4113(itemStack, "minecraft:mending");
         }
      } else {
         local6851_4 = "Кр";
      }

      String local6851_6 = local6851_5 > 0 ? String.valueOf(local6851_5) : "";
      float local6851_7 = FontText.method1396(FontFace.SEMI_BOLD, local6851_4, 4.5F);
      float local6851_8 = local6851_2 + 4.0F - (local6851_7 + FontText.method1396(FontFace.SEMI_BOLD, local6851_6, 4.5F)) / 2.0F;
      FontText.method1397(FontFace.SEMI_BOLD, local6851_4, local6851_8, local6851_3 - 5.5F, 4.5F, ThemeColors.field13665);
      if (!local6851_6.isEmpty()) {
         FontText.method1397(FontFace.SEMI_BOLD, local6851_6, local6851_8 + local6851_7, local6851_3 - 5.5F, 4.5F, field13983);
      }
   }

   private boolean method4089(ItemStack itemStack) {
      if (!itemStack.isIn(ItemTags.SWORDS)) {
         return false;
      }

      HashSet<String> local6852_2 = new HashSet<>();

      for (Entry<RegistryEntry<Enchantment>> local6852_4 : itemStack.getEnchantments().getEnchantmentEntries()) {
         local6852_2.add(((RegistryEntry)local6852_4.getKey()).getIdAsString() + local6852_4.getIntValue());
      }

      return local6852_2.contains("minecraft:sharpness8");
   }

   private boolean method4092(ItemStack itemStack) {
      if (!itemStack.isIn(ItemTags.SWORDS)) {
         return false;
      }

      HashSet<String> local6856_2 = new HashSet<>();

      for (Entry<RegistryEntry<Enchantment>> local6856_4 : itemStack.getEnchantments().getEnchantmentEntries()) {
         local6856_2.add(((RegistryEntry)local6856_4.getKey()).getIdAsString() + local6856_4.getIntValue());
      }

      return local6856_2.contains("minecraft:knockback1") || local6856_2.contains("minecraft:knockback2") || local6856_2.contains("minecraft:sharpness8");
   }

   private boolean method4093(ItemStack itemStack) {
      HashSet<String> local6857_2 = new HashSet<>();

      for (Entry<RegistryEntry<Enchantment>> local6857_4 : itemStack.getEnchantments().getEnchantmentEntries()) {
         local6857_2.add(((RegistryEntry)local6857_4.getKey()).getIdAsString() + local6857_4.getIntValue());
      }

      return local6857_2.contains("minecraft:protection5") && (local6857_2.contains("minecraft:mending1") || local6857_2.contains("minecraft:mending2"));
   }

   @Override
   protected void onDisable() {
      PlayerOutlineRenderer.method4386().method4375();
      this.field13999.clear();
      this.field14000.clear();
      this.field14001.clear();
      this.field14002.clear();
      this.field14005.clear();
      this.field14006 = null;
   }

   public boolean method4095() {
      return this.isEnabled() && this.field13990.isChecked();
   }

   private TagsModule() {
      this.field13987 = this.addSetting(
         new MultiChoiceSetting("Отображать", "На ком рисовать элементы и шейдер.")
            .withOptions("Игроков", "Друзей", "Себя")
            .withSelection("Игроков", "Друзей", "Себя")
      );
      this.field13988 = this.addSetting(
         new MultiChoiceSetting("Элементы", "Что рисовать поверх игроков и предметов.")
            .withOptions("Ники", "Предметы", "Броню", "Руки")
            .withSelection("Ники", "Предметы", "Руки")
      );
      this.field13989 = this.addSetting(
         new BooleanSetting("Показывать донатные зелья игроков", "Донатные зелья, которыми задело игроков и тебя, и сколько осталось.", true)
      );
      this.addSetting(new SectionSetting("Применить шейдер"));
      this.field13990 = this.addSetting(new BooleanSetting("Применить шейдер", "Обводка и тень вокруг игроков.", false));
      this.field13991 = this.addSetting(new LegacyNumberSetting("Оутлайн", "Сила обводки.", 0.8, 0.0, 1.0, 0.05));
      this.field13992 = this.addSetting(new LegacyNumberSetting("Подсветка", "Сила свечения вокруг игроков.", 0.55, 0.0, 3.0, 0.05));
      this.field13993 = this.addSetting(new LegacyNumberSetting("Заливка", "Заливка игроков цветом.", 0.0, 0.0, 1.0, 0.05));
      this.field13994 = this.addSetting(new BooleanSetting("Кастомный цвет", "Свой цвет вместо акцента темы.", false));
      this.field13995 = this.addSetting(new ColorSetting("Цвет", "Цвет обводки и тени."));
      this.field13991.withVisibility(() -> this.field13990.isChecked());
      this.field13992.withVisibility(() -> this.field13990.isChecked());
      this.field13993.withVisibility(() -> this.field13990.isChecked());
      this.field13994.withVisibility(() -> this.field13990.isChecked());
      this.field13995.withVisibility(() -> this.field13990.isChecked() && this.field13994.isChecked());
      this.addSetting(new SectionSetting("Бокс"));
      this.field13996 = this.addSetting(new BooleanSetting("Бокс", "Показывать хитбоксы игроков в мире.", false));
      this.field13997 = this.addSetting(new BooleanSetting("Кастомный цвет", "Свой цвет вместо акцента темы.", false));
      this.field13998 = this.addSetting(new ColorSetting("Цвет", "Цвет обводки и заливки хитбокса."));
      this.field13997.withVisibility(() -> this.field13996.isChecked());
      this.field13998.withVisibility(() -> this.field13996.isChecked() && this.field13997.isChecked());
   }

   public float method4097(float local6863_1) {
      return Math.max(0.65F, 1.1F - local6863_1 * 0.004F);
   }

   public float method4098(float local6864_1) {
      return this.isEnabled() && this.field13988.isIndexSelected(1) ? GuiScale.method0800(11.0F * this.method4097(local6864_1)) : 0.0F;
   }

   private void method4099(float local6865_1, float local6865_2, float local6865_3, float local6865_4) {
      Render2D.method4715(
         Math.min(local6865_1, local6865_1 + local6865_3) - (local6865_3 == 0.0F ? 0.875F : 0.5F),
         Math.min(local6865_2, local6865_2 + local6865_4) - (local6865_4 == 0.0F ? 0.875F : 0.5F),
         (local6865_3 == 0.0F ? 0.75F : Math.abs(local6865_3)) + 1.0F,
         (local6865_4 == 0.0F ? 0.75F : Math.abs(local6865_4)) + 1.0F,
         0.0F,
         -16777216
      );
      Render2D.method4715(
         Math.min(local6865_1, local6865_1 + local6865_3) - (local6865_3 == 0.0F ? 0.375F : 0.0F),
         Math.min(local6865_2, local6865_2 + local6865_4) - (local6865_4 == 0.0F ? 0.375F : 0.0F),
         local6865_3 == 0.0F ? 0.75F : Math.abs(local6865_3),
         local6865_4 == 0.0F ? 0.75F : Math.abs(local6865_4),
         0.0F,
         0xFF000000 | this.method4110() & 16777215
      );
   }

   private boolean method4100(AbstractClientPlayerEntity abstractClientPlayerEntity, float local6866_2, int local6866_3, int local6866_4, float local6866_5) {
      if (abstractClientPlayerEntity == mc.player && mc.options.getPerspective().isFirstPerson()) {
         return false;
      }

      if (!this.method4105(abstractClientPlayerEntity)) {
         return false;
      }

      boolean local6866_6 = FriendStore.method3431(abstractClientPlayerEntity.getGameProfile().name());
      float local6866_7 = (float)mc.gameRenderer.getCamera().getCameraPos().distanceTo(abstractClientPlayerEntity.getLerpedPos(local6866_2));
      float local6866_8 = Math.max(0.3F, 1.0F - local6866_7 * 0.008F);
      Vec3d vec3d = WorldProjection.method1125(abstractClientPlayerEntity.getLerpedPos(local6866_2), local6866_3, local6866_4);
      if (vec3d != null) {
         float local6866_10 = (float)vec3d.y * local6866_5 + 2.0F;
         if (this.field13988.isIndexSelected(3)) {
            local6866_10 = this.method4102(
               abstractClientPlayerEntity, local6866_6, (float)vec3d.x * local6866_5, local6866_10, local6866_8 * Math.max(0.7F, 1.0F - local6866_7 * 0.005F)
            );
         }

         if (this.method4119()) {
            this.method4108(abstractClientPlayerEntity, (float)vec3d.x * local6866_5, local6866_10, local6866_8 * Math.max(0.7F, 1.0F - local6866_7 * 0.005F));
         }
      }

      if (this.field13988.isIndexSelected(0)) {
         Vec3d vec3d2 = WorldProjection.method1125(
            abstractClientPlayerEntity.getLerpedPos(local6866_2).add(0.0, abstractClientPlayerEntity.getBoundingBox().maxY - abstractClientPlayerEntity.getBoundingBox().minY + 0.3, 0.0),
            local6866_3,
            local6866_4
         );
         if (vec3d2 != null) {
            this.method4115(abstractClientPlayerEntity, local6866_6, (float)vec3d2.x * local6866_5, (float)vec3d2.y * local6866_5, local6866_7, local6866_8);
         }
      }

      return true;
   }

   public int method4101() {
      return this.field13994.isChecked() ? this.field13995.getValue() : UiTheme.field09686;
   }

   private float method4102(AbstractClientPlayerEntity abstractClientPlayerEntity, boolean local6868_2, float local6868_3, float local6868_4, float local6868_5) {
      ItemStack mainHandStack = abstractClientPlayerEntity.getMainHandStack();
      ItemStack offHandStack = abstractClientPlayerEntity.getOffHandStack();
      if (mainHandStack.isEmpty() && offHandStack.isEmpty()) {
         return local6868_4;
      }

      float local6868_8 = 12.0F;
      HudRenderContext.method4336(local6868_3, local6868_4, local6868_5);
      float local6868_9 = local6868_4 + 1.0F;

      for (ItemStack itemStack : new ItemStack[]{mainHandStack, offHandStack}) {
         if (!itemStack.isEmpty()) {
            String local6868_14 = itemStack.getCount() > 1 ? " " + itemStack.getCount() + "x" : "";
            float local6868_15 = FontText.method1396(FontFace.SEMI_BOLD, itemStack.getName().getString(), 6.0F)
               + FontText.method1396(FontFace.SEMI_BOLD, local6868_14, 6.0F);
            Render2D.method4715(
               local6868_3 - local6868_15 / 2.0F - 2.0F, local6868_9, local6868_15 + 4.0F, local6868_8, 0.0F, this.method4086(abstractClientPlayerEntity, local6868_2)
            );
            FontText.method1400(
               FontFace.SEMI_BOLD,
               itemStack.getName()
                  .copy()
                  .styled(
                     style -> style.getColor() != null && (style.getColor().getRgb() & 16777215) != 16777215
                        ? style
                        : style.withColor(TextColor.fromRgb(ThemeColors.field13665 & 16777215))
                  ),
               local6868_3 - local6868_15 / 2.0F,
               local6868_9 + local6868_8 / 2.0F - 3.65F,
               6.0F,
               ThemeColors.field13665
            );
            if (!local6868_14.isEmpty()) {
               FontText.method1397(
                  FontFace.SEMI_BOLD,
                  local6868_14,
                  local6868_3 - local6868_15 / 2.0F + FontText.method1396(FontFace.SEMI_BOLD, itemStack.getName().getString(), 6.0F),
                  local6868_9 + local6868_8 / 2.0F - 3.65F,
                  6.0F,
                  ThemeColors.field13665
               );
            }

            local6868_9 += local6868_8 + 1.0F;
         }
      }

      HudRenderContext.method4337();
      return local6868_4 + (local6868_9 - local6868_4) * local6868_5;
   }

   private void method4104(AbstractClientPlayerEntity abstractClientPlayerEntity, float local6870_2, int local6870_3, int local6870_4, float local6870_5) {
      if ((abstractClientPlayerEntity != mc.player || !mc.options.getPerspective().isFirstPerson()) && this.method4105(abstractClientPlayerEntity)) {
         Vector4f vector4f = WorldProjection.method1119(
            abstractClientPlayerEntity.getBoundingBox().offset(abstractClientPlayerEntity.getLerpedPos(local6870_2).subtract(abstractClientPlayerEntity.getEntityPos())), local6870_3, local6870_4
         );
         if (vector4f != null && !(vector4f.z <= 0.0F) && !(vector4f.w <= 0.0F)) {
            this.method4099(vector4f.x * local6870_5, vector4f.y * local6870_5, vector4f.z * local6870_5, 0.0F);
            this.method4099(vector4f.x * local6870_5, (vector4f.y + vector4f.w) * local6870_5, vector4f.z * local6870_5, 0.0F);
            this.method4099(vector4f.x * local6870_5, vector4f.y * local6870_5, 0.0F, vector4f.w * local6870_5);
            this.method4099((vector4f.x + vector4f.z) * local6870_5, vector4f.y * local6870_5, 0.0F, vector4f.w * local6870_5);
         }
      }
   }

   public boolean method4105(AbstractClientPlayerEntity abstractClientPlayerEntity) {
      if (abstractClientPlayerEntity == mc.player) {
         return this.field13987.matchesOption("Себя");
      } else {
         return FriendStore.method3431(abstractClientPlayerEntity.getGameProfile().name())
            ? this.field13987.matchesOption("Друзей")
            : this.field13987.matchesOption("Игроков");
      }
   }

   public static TagsModule method4106() {
      if (tagsModule == null) {
         tagsModule = new TagsModule();
      }

      return tagsModule;
   }

   private boolean method4107(ItemEntity itemEntity, float local6873_2, int local6873_3, int local6873_4, float local6873_5) {
      return this.method4120(itemEntity.getStack(), itemEntity.getLerpedPos(local6873_2), itemEntity.getId(), local6873_3, local6873_4, local6873_5);
   }

   private void method4108(AbstractClientPlayerEntity abstractClientPlayerEntity, float local6874_2, float local6874_3, float local6874_4) {
      Map<ServerItemType, Long> local6874_5 = PlayerItemCooldowns.method2679(abstractClientPlayerEntity);
      if (!local6874_5.isEmpty()) {
         long local6874_6 = System.currentTimeMillis();
         float local6874_8 = local6874_3 + 2.0F;
         HudRenderContext.method4336(local6874_2, local6874_3, local6874_4);

         for (java.util.Map.Entry<ServerItemType, Long> local6874_10 : local6874_5.entrySet()) {
            String local6874_11 = " " + Math.max(1L, (local6874_10.getValue() - local6874_6 + 999L) / 1000L) + "с";
            float local6874_12 = FontText.method1396(FontFace.SEMI_BOLD, local6874_10.getKey().method2463(), 5.0F);
            float local6874_13 = local6874_12 + FontText.method1396(FontFace.SEMI_BOLD, local6874_11, 5.0F);
            FontText.method1400(
               FontFace.SEMI_BOLD,
               local6874_10.getKey().method2462(),
               local6874_2 - local6874_13 / 2.0F,
               local6874_8,
               5.0F,
               ThemeColors.field13665
            );
            FontText.method1397(
               FontFace.SEMI_BOLD,
               local6874_11,
               local6874_2 - local6874_13 / 2.0F + local6874_12,
               local6874_8,
               5.0F,
               HudStyle.method4815(1.0F)
            );
            local6874_8 += 6.5F;
         }

         HudRenderContext.method4337();
      }
   }

   public int method4110() {
      return this.field13997.isChecked() ? this.field13998.getValue() : UiTheme.field09686;
   }

   private boolean method4112(ItemStack itemStack) {
      if (!itemStack.isOf(Items.MACE)) {
         return false;
      }

      HashSet<String> local6878_2 = new HashSet<>();

      for (Entry<RegistryEntry<Enchantment>> local6878_4 : itemStack.getEnchantments().getEnchantmentEntries()) {
         local6878_2.add(((RegistryEntry)local6878_4.getKey()).getIdAsString() + local6878_4.getIntValue());
      }

      return local6878_2.contains("minecraft:breach3") && local6878_2.contains("minecraft:density5");
   }

   private int method4113(ItemStack itemStack, String local6879_2) {
      for (Entry<RegistryEntry<Enchantment>> local6879_4 : itemStack.getEnchantments().getEnchantmentEntries()) {
         if (((RegistryEntry)local6879_4.getKey()).getIdAsString().equals(local6879_2)) {
            return local6879_4.getIntValue();
         }
      }

      return 0;
   }

   private boolean method4114() {
      return this.field13988.isIndexSelected(2)
         || ServerHelperModule.method0707().isEnabled() && ServerHelperModule.method0707().modeSetting.isSelected("SpookyTime");
   }

   private void method4115(
      AbstractClientPlayerEntity abstractClientPlayerEntity, boolean local6881_2, float local6881_3, float local6881_4, float local6881_5, float local6881_6
   ) {
      String local6881_7 = abstractClientPlayerEntity.getGameProfile().name();
      String local6881_8 = EntityUtils.method2804(abstractClientPlayerEntity);
      Team team = mc.world.getScoreboard().getScoreHolderTeam(abstractClientPlayerEntity.getNameForScoreboard());
      Text text = team == null ? null : team.getPrefix();
      String local6881_11 = text == null ? "" : text.getString().trim();
      String local6881_12 = String.valueOf(Math.round(local6881_5));
      float local6881_13 = FontText.method1396(FontFace.SEMI_BOLD, local6881_7, 6.0F);
      float local6881_14 = 3.0F;
      float local6881_15 = local6881_11.isEmpty() ? 0.0F : FontText.method1396(FontFace.SEMI_BOLD, local6881_11, 6.0F) + local6881_14;
      boolean local6881_16 = this.method4118(abstractClientPlayerEntity);
      float local6881_17 = local6881_16 ? FontText.method1396(FontFace.SEMI_BOLD, "AIRSTUCK", 6.0F) + local6881_14 : 0.0F;
      float local6881_18 = 2.0F
         + local6881_15
         + local6881_17
         + local6881_13
         + local6881_14
         + FontText.method1396(FontFace.SEMI_BOLD, "[", 6.0F)
         + FontText.method1396(FontFace.SEMI_BOLD, local6881_8, 6.0F)
         + FontText.method1396(FontFace.SEMI_BOLD, "/", 6.0F)
         + FontText.method1396(FontFace.SEMI_BOLD, local6881_12, 6.0F)
         + FontText.method1396(FontFace.SEMI_BOLD, "]", 6.0F)
         + 2.0F;
      float local6881_19 = 12.0F;
      float local6881_20 = local6881_3 - local6881_18 / 2.0F;
      float local6881_21 = local6881_4 - local6881_19 - 3.0F + 6.0F - Math.min(6.0F, local6881_5 * 0.08F);
      float local6881_22 = local6881_21 + local6881_19 / 2.0F - 3.65F;
      HudRenderContext.method4336(local6881_3, local6881_4, local6881_6 * Math.max(0.7F, 1.0F - local6881_5 * 0.005F));
      Render2D.method4715(
         local6881_20,
         local6881_21,
         local6881_18,
         local6881_19,
         0.0F,
         local6881_16 ? field13984 : (local6881_2 ? field13982 : Colors.method3728(ThemeColors.field13661, 133))
      );
      float local6881_23 = local6881_20 + 2.0F;
      if (!local6881_11.isEmpty()) {
         FontText.method1400(FontFace.SEMI_BOLD, text, local6881_23, local6881_22, 6.0F, ThemeColors.field13665);
         local6881_23 += FontText.method1396(FontFace.SEMI_BOLD, local6881_11, 6.0F) + local6881_14;
      }

      if (local6881_16) {
         FontText.method1397(FontFace.SEMI_BOLD, "AIRSTUCK", local6881_23, local6881_22, 6.0F, field13983);
         local6881_23 += FontText.method1396(FontFace.SEMI_BOLD, "AIRSTUCK", 6.0F) + local6881_14;
      }

      FontText.method1397(FontFace.SEMI_BOLD, local6881_7, local6881_23, local6881_22, 6.0F, ThemeColors.field13665);
      local6881_23 += local6881_13 + local6881_14;
      FontText.method1397(FontFace.SEMI_BOLD, "[", local6881_23, local6881_22, 6.0F, field13985);
      local6881_23 += FontText.method1396(FontFace.SEMI_BOLD, "[", 6.0F);
      FontText.method1397(FontFace.SEMI_BOLD, local6881_8, local6881_23, local6881_22, 6.0F, field13983);
      local6881_23 += FontText.method1396(FontFace.SEMI_BOLD, local6881_8, 6.0F);
      FontText.method1397(FontFace.SEMI_BOLD, "/", local6881_23, local6881_22, 6.0F, field13985);
      local6881_23 += FontText.method1396(FontFace.SEMI_BOLD, "/", 6.0F);
      FontText.method1397(FontFace.SEMI_BOLD, local6881_12, local6881_23, local6881_22, 6.0F, field13986);
      local6881_23 += FontText.method1396(FontFace.SEMI_BOLD, local6881_12, 6.0F);
      FontText.method1397(FontFace.SEMI_BOLD, "]", local6881_23, local6881_22, 6.0F, field13985);
      if (!this.method4114()) {
         ArrayList<ItemStack> local6881_24 = new ArrayList<>();
         if (this.method4092(abstractClientPlayerEntity.getMainHandStack()) || this.method4112(abstractClientPlayerEntity.getMainHandStack())) {
            local6881_24.add(abstractClientPlayerEntity.getMainHandStack());
         }

         for (ItemStack itemStack : new ItemStack[]{
            abstractClientPlayerEntity.getEquippedStack(EquipmentSlot.HEAD),
            abstractClientPlayerEntity.getEquippedStack(EquipmentSlot.CHEST),
            abstractClientPlayerEntity.getEquippedStack(EquipmentSlot.LEGS),
            abstractClientPlayerEntity.getEquippedStack(EquipmentSlot.FEET)
         }) {
            if (this.method4093(itemStack)) {
               local6881_24.add(itemStack);
            }
         }

         local6881_23 = local6881_3 - (local6881_24.size() * 8.0F + (local6881_24.size() - 1) * 2.0F) / 2.0F;

         for (ItemStack itemStack2 : local6881_24) {
            this.method4088(itemStack2, local6881_23, local6881_21 - 10.0F - 1.5F + 1.0F);
            ItemRenderCache.method2336(
               itemStack2, local6881_23, local6881_21 - 10.0F - 1.5F + 1.0F, 8.0F, ItemDrawOptions.noDecorations(1.0F).glint(GlintMode.OFF)
            );
            local6881_23 += 10.0F;
         }
      }

      if (this.method4114()) {
         ArrayList<ItemStack> local6881_24 = new ArrayList<>();

         for (ItemStack itemStack3 : new ItemStack[]{
            abstractClientPlayerEntity.getMainHandStack(),
            abstractClientPlayerEntity.getEquippedStack(EquipmentSlot.HEAD),
            abstractClientPlayerEntity.getEquippedStack(EquipmentSlot.CHEST),
            abstractClientPlayerEntity.getEquippedStack(EquipmentSlot.LEGS),
            abstractClientPlayerEntity.getEquippedStack(EquipmentSlot.FEET),
            abstractClientPlayerEntity.getOffHandStack()
         }) {
            if (!itemStack3.isEmpty()) {
               local6881_24.add(itemStack3);
            }
         }

         if (!local6881_24.isEmpty()) {
            local6881_23 = local6881_20 + 2.0F;

            for (ItemStack itemStack4 : local6881_24) {
               this.method4088(itemStack4, local6881_23, local6881_21 - 10.0F - 1.5F + 1.0F);
               ItemRenderCache.method2336(
                  itemStack4, local6881_23, local6881_21 - 10.0F - 1.5F + 1.0F, 8.0F, ItemDrawOptions.decorated(1.0F).glint(GlintMode.OFF)
               );
               local6881_23 += 10.0F;
            }
         }
      }

      HudRenderContext.method4337();
   }

   public boolean method4118(AbstractClientPlayerEntity abstractClientPlayerEntity) {
      if (abstractClientPlayerEntity == mc.player) {
         return AirStuckModule.method2785().method2787();
      } else {
         return abstractClientPlayerEntity.getEquippedStack(EquipmentSlot.CHEST).isOf(Items.ELYTRA) ? false : this.field14000.getOrDefault(abstractClientPlayerEntity.getId(), 0) >= 20;
      }
   }

   private boolean method4119() {
      return this.field13989.isChecked() && ServerItemType.method2469();
   }

   private boolean method4120(ItemStack itemStack, Vec3d vec3d, int local6886_3, int local6886_4, int local6886_5, float local6886_6) {
      if (itemStack.isEmpty()) {
         return false;
      }

      Vec3d vec3d2 = WorldProjection.method1125(vec3d.add(0.0, 0.25, 0.0), local6886_4, local6886_5);
      if (vec3d2 == null) {
         return false;
      }

      TagsModule.Nested0518 local6886_8 = this.field14005.get(local6886_3);
      if (local6886_8 == null || local6886_8.stack() != itemStack && !ItemStack.areItemsAndComponentsEqual(local6886_8.stack(), itemStack)) {
         local6886_8 = new TagsModule.Nested0518(
            itemStack,
            itemStack.getName().getString(),
            itemStack.getName()
               .copy()
               .styled(
                  style -> style.getColor() != null && (style.getColor().getRgb() & 16777215) != 16777215
                     ? style
                     : style.withColor(TextColor.fromRgb(ThemeColors.field13665 & 16777215))
               )
         );
         this.field14005.put(local6886_3, local6886_8);
      }

      String local6886_9 = local6886_8.label();
      float local6886_10 = (float)mc.gameRenderer.getCamera().getCameraPos().distanceTo(vec3d);
      float local6886_11 = this.method4097(local6886_10);
      float local6886_12 = 9.0F;
      float local6886_13 = 2.0F + FontText.method1396(FontFace.SEMI_BOLD, local6886_9, 4.0F) + 2.0F;
      float local6886_14 = (float)vec3d2.x * local6886_6;
      float local6886_15 = (float)vec3d2.y * local6886_6 - local6886_12 - 2.0F;
      float local6886_16 = local6886_14 - local6886_13 / 2.0F;
      float local6886_17 = local6886_15 + local6886_12 / 2.0F;
      HudRenderContext.method4336(local6886_14, (float)vec3d2.y * local6886_6, local6886_11);
      Render2D.method4715(
         local6886_16, local6886_15, local6886_13, local6886_12, 0.0F, Colors.method3728(ThemeColors.field13661, 133)
      );
      FontText.method1400(
         FontFace.SEMI_BOLD,
         local6886_8.styledName(),
         local6886_16 + 2.0F,
         local6886_15 + local6886_12 / 2.0F - 2.45F,
         4.0F,
         ThemeColors.field13665
      );
      HudRenderContext.method4337();
      return true;
   }

   private boolean method4121(ProjectileEntity projectileEntity, float local6887_2, int local6887_3, int local6887_4, float local6887_5) {
      return this.method4120(this.method4085(projectileEntity), projectileEntity.getLerpedPos(local6887_2), projectileEntity.getId(), local6887_3, local6887_4, local6887_5);
   }

   private record Nested0518(ItemStack stack, String label, Text styledName) {
   }
}
