package deluxe.client.module.combat;

import deluxe.client.util.AuctionItemCatalog;
import deluxe.client.setting.BooleanSetting;
import deluxe.mixin.BossBarHudMixin;
import deluxe.client.event.EventListener;
import deluxe.client.storage.FriendStore;
import deluxe.client.setting.LegacyNumberSetting;
import deluxe.client.module.Module;
import deluxe.client.module.ModuleCategory;
import deluxe.client.module.ModuleInfo;
import deluxe.client.setting.MultiSelectSetting;
import deluxe.client.event.PlayerTickEvent;
import deluxe.client.util.InventoryUtils;
import deluxe.client.util.AttributeBonus;
import deluxe.client.setting.NumericTextSetting;
import deluxe.client.util.InventoryActionQueue;
import deluxe.client.util.EntityUtils;
import deluxe.client.storage.AutoBuyItem;
import deluxe.client.module.player.ServerHelperModule;

import java.util.HashMap;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.component.type.AttributeModifiersComponent.Entry;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.TntEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.attribute.EntityAttributeModifier.Operation;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.projectile.TridentEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.math.MathHelper;

@ModuleInfo(name = "AutoTotem", description = "Автоматически берёт тотем в левую руку по триггерам.", category = ModuleCategory.COMBAT)
public final class AutoTotemModule extends Module {
   private static AutoTotemModule autoTotemModule;
   public MultiSelectSetting multiSelectSetting;
   public LegacyNumberSetting field12771;
   public LegacyNumberSetting field12772;
   public LegacyNumberSetting field12773;
   public LegacyNumberSetting field12774;
   public LegacyNumberSetting field12775;
   public LegacyNumberSetting field12776;
   public BooleanSetting field12777;
   public BooleanSetting field12778;
   public NumericTextSetting numericTextSetting;
   public BooleanSetting field12780;
   private ItemStack field12781 = ItemStack.EMPTY;
   private boolean field12782;
   private long field12783;
   private int field12784 = -1;
   private long field12785;
   private final HashMap<Integer, Double> field12786 = new HashMap<>();
   private final EventListener<PlayerTickEvent> field12787 = playerTickEvent -> {
      if (mc.player != null && mc.world != null) {
         if (mc.world != this.field12788) {
            this.method3682();
            this.field12783 = 0L;
            this.field12786.clear();
            this.field12788 = mc.world;
         }

         this.method3691();
         if (mc.player.currentScreenHandler == mc.player.playerScreenHandler) {
            if (System.currentTimeMillis() - this.field12785 >= 250L) {
               if (this.field12782 && mc.player.getOffHandStack().getItem() != Items.TOTEM_OF_UNDYING && !this.method3690()) {
                  this.method3682();
               }

               if (this.method3687()) {
                  if (!this.method3671() && this.method3686() != -1) {
                     this.method3685();
                  }
               } else if (this.field12782 && System.currentTimeMillis() >= this.field12783) {
                  if (this.method3694() != -1) {
                     this.method3697();
                  }
               }
            }
         }
      }
   };
   private ClientWorld field12788;

   private boolean method3666() {
      for (Entity entity : mc.world.getEntities()) {
         if (entity instanceof TntEntity && entity.squaredDistanceTo(mc.player) < this.field12773.getValue() * this.field12773.getValue()
            )
          {
            return true;
         }
      }

      return false;
   }

   public static AutoTotemModule method3667() {
      if (autoTotemModule == null) {
         autoTotemModule = new AutoTotemModule();
      }

      return autoTotemModule;
   }

   private boolean method3668() {
      for (Entity entity : mc.world.getEntities()) {
         if (entity instanceof TridentEntity
            && entity.squaredDistanceTo(mc.player) < this.field12774.getValue() * this.field12774.getValue()) {
            return true;
         }
      }

      return false;
   }

   public boolean method3669(ItemStack itemStack) {
      AuctionItemCatalog.method1056();
      boolean local6104_2 = false;

      for (AutoBuyItem autoBuyItem : AuctionItemCatalog.method1077()) {
         if (autoBuyItem.item() == Items.TOTEM_OF_UNDYING && autoBuyItem.name().toLowerCase().contains(this.numericTextSetting.getText().toLowerCase())) {
            local6104_2 = true;
            if (AutoBuyItem.CHECKER.test(itemStack, autoBuyItem)) {
               return true;
            }
         }
      }

      return !local6104_2 && itemStack.getName().getString().toLowerCase().contains(this.numericTextSetting.getText().toLowerCase());
   }

   public boolean method3670() {
      return this.isEnabled() && this.field12782;
   }

   private boolean method3671() {
      if (mc.player.getOffHandStack().getItem() != Items.TOTEM_OF_UNDYING) {
         return false;
      } else if (this.method3684()
         && !this.method3669(mc.player.getOffHandStack())
         && this.method3686() != -1
         && this.method3669(mc.player.getInventory().getStack(this.method3686()))) {
         return false;
      } else {
         return (this.field12777.isChecked() || this.field12778.isChecked()) && this.method3673(mc.player.getOffHandStack())
            ? this.method3686() == -1 || this.method3673(mc.player.getInventory().getStack(this.method3686()))
            : true;
      }
   }

   private AutoTotemModule() {
      this.multiSelectSetting = this.addSetting(
         new MultiSelectSetting(
            "Триггеры",
            "Когда автоматически брать тотем в левую руку.",
            new String[]{"Здоровье", "Падение", "Динамит", "Трезубец", "Булава", "Кристалл"},
            "Здоровье"
         )
      );
      this.field12771 = this.addSetting(new LegacyNumberSetting("Здоровье", "При каком здоровье брать тотем.", 8.0, 1.0, 19.0, 0.5));
      this.field12772 = this.addSetting(new LegacyNumberSetting("Дистанция падения", "С какой высоты падения брать тотем.", 6.0, 3.0, 40.0, 0.5));
      this.field12773 = this.addSetting(new LegacyNumberSetting("Радиус динамита", "На каком расстоянии поджённый динамит берёт тотем.", 8.0, 1.0, 16.0, 0.5));
      this.field12774 = this.addSetting(new LegacyNumberSetting("Радиус трезубца", "На каком расстоянии летящий трезубец берёт тотем.", 8.0, 1.0, 16.0, 0.5));
      this.field12776 = this.addSetting(new LegacyNumberSetting("Радиус кристалла", "На каком расстоянии кристалл края берёт тотем.", 6.0, 1.0, 16.0, 0.5));
      this.field12775 = this.addSetting(
         new LegacyNumberSetting("Радиус булавы", "На каком расстоянии по горизонтали игрок с булавой берёт тотем.", 7.0, 1.0, 16.0, 0.5)
      );
      this.field12777 = this.addSetting(
         new BooleanSetting("Сохранять зачарованный тотем", "Берёт обычные тотемы, зачарованный только если обычных нет.", true)
      );
      this.field12778 = this.addSetting(
         new BooleanSetting(
            "Брать зачарованный с сохранением", "Дон-тотем в руке сохраняется, на замену берётся зачарованный тотем с выбранным названием для перебива.", false
         )
      );
      this.numericTextSetting = this.addSetting(new NumericTextSetting("Название тотема", "Часть названия зачарованного тотема на замену.", "Тирана", 32));
      this.field12780 = this.addSetting(new BooleanSetting("Отображать снесение тотема", "Рисует призрака игрока на месте лопнувшего тотема.", false));
      this.numericTextSetting.withVisibility(() -> this.field12778.isChecked());
      this.field12771.withVisibility(() -> this.multiSelectSetting.matchesOption("Здоровье"));
      this.field12772.withVisibility(() -> this.multiSelectSetting.matchesOption("Падение"));
      this.field12773.withVisibility(() -> this.multiSelectSetting.matchesOption("Динамит"));
      this.field12774.withVisibility(() -> this.multiSelectSetting.matchesOption("Трезубец"));
      this.field12775.withVisibility(() -> this.multiSelectSetting.matchesOption("Булава"));
      this.field12776.withVisibility(() -> this.multiSelectSetting.matchesOption("Кристалл"));
   }

   private boolean method3673(ItemStack itemStack) {
      return this.field12778.isChecked()
            && !this.numericTextSetting.getText().isEmpty()
            && !EntityUtils.method2795(itemStack)
            && this.method3669(itemStack)
         ? false
         : EntityUtils.method2795(itemStack) || itemStack.hasEnchantments();
   }

   private boolean method3674() {
      return mc.player.fallDistance >= this.field12772.getValue();
   }

   private float method3675(ItemStack itemStack) {
      if (itemStack.isEmpty()) {
         return 0.0F;
      }

      float local6111_2 = 0.0F;
      AuctionItemCatalog.method1056();

      for (AutoBuyItem autoBuyItem : AuctionItemCatalog.method1077()) {
         if (!autoBuyItem.attributes().isEmpty() && AutoBuyItem.CHECKER.test(itemStack, autoBuyItem)) {
            for (AttributeBonus attributeBonus : autoBuyItem.attributes()) {
               if (attributeBonus.attribute().matches(EntityAttributes.ATTACK_DAMAGE) && attributeBonus.operation() == Operation.ADD_VALUE) {
                  local6111_2 += (float)attributeBonus.amount();
               }
            }

            return local6111_2;
         }
      }

      for (Entry entry : itemStack.getOrDefault(DataComponentTypes.ATTRIBUTE_MODIFIERS, AttributeModifiersComponent.DEFAULT).modifiers()) {
         if (entry.attribute().matches(EntityAttributes.ATTACK_DAMAGE) && entry.modifier().operation() == Operation.ADD_VALUE) {
            local6111_2 += (float)entry.modifier().value();
         }
      }

      return local6111_2;
   }

   private boolean method3676() {
      for (Entity entity : mc.world.getEntities()) {
         if (entity instanceof EndCrystalEntity
            && entity.squaredDistanceTo(mc.player) < this.field12776.getValue() * this.field12776.getValue()) {
            return true;
         }
      }

      return false;
   }

   private float method3677(AbstractClientPlayerEntity abstractClientPlayerEntity) {
      float local6113_2 = 1.0F;

      for (Entry entry : abstractClientPlayerEntity.getMainHandStack()
         .getOrDefault(DataComponentTypes.ATTRIBUTE_MODIFIERS, AttributeModifiersComponent.DEFAULT)
         .modifiers()) {
         if (entry.attribute().matches(EntityAttributes.ATTACK_DAMAGE)
            && entry.slot().matches(EquipmentSlot.MAINHAND)
            && entry.modifier().operation() == Operation.ADD_VALUE) {
            local6113_2 += (float)entry.modifier().value();
         }
      }

      return Math.max(local6113_2, 6.0F) + this.method3675(abstractClientPlayerEntity.getOffHandStack());
   }

   private int method3678() {
      if (this.field12781.isEmpty()) {
         return -1;
      }

      if (this.field12784 != -1 && ItemStack.areItemsAndComponentsEqual(mc.player.getInventory().getStack(this.field12784), this.field12781)) {
         return this.field12784;
      }

      int local6114_1 = InventoryUtils.method0556(itemStack -> ItemStack.areItemsAndComponentsEqual(itemStack, this.field12781), 0, 35);
      return local6114_1 != -1 ? local6114_1 : InventoryUtils.method0556(itemStack2 -> itemStack2.getItem() == this.field12781.getItem(), 0, 35);
   }

   private float method3679(AbstractClientPlayerEntity abstractClientPlayerEntity, float local6115_2) {
      ItemStack mainHandStack = abstractClientPlayerEntity.getMainHandStack();
      int local6115_4 = EnchantmentHelper.getLevel(
         mc.world.getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT).getOrThrow(Enchantments.DENSITY), mainHandStack
      );
      int local6115_5 = EnchantmentHelper.getLevel(
         mc.world.getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT).getOrThrow(Enchantments.BREACH), mainHandStack
      );
      float local6115_6 = local6115_2 <= 3.0F ? 4.0F * local6115_2 : (local6115_2 <= 8.0F ? 12.0F + 2.0F * (local6115_2 - 3.0F) : 22.0F + local6115_2 - 8.0F);
      float local6115_7 = (this.method3677(abstractClientPlayerEntity) + local6115_6 + 0.5F * local6115_4 * local6115_2) * 1.5F;
      float armor = mc.player.getArmor();
      float local6115_9 = (float)mc.player.getAttributeValue(EntityAttributes.ARMOR_TOUGHNESS);
      float local6115_10 = MathHelper.clamp(
         MathHelper.clamp(armor - local6115_7 / (2.0F + local6115_9 / 4.0F), armor * 0.2F, 20.0F) / 25.0F - 0.15F * local6115_5, 0.0F, 1.0F
      );
      float local6115_11 = local6115_7 * (1.0F - local6115_10);
      StatusEffectInstance statusEffect = mc.player.getStatusEffect(StatusEffects.RESISTANCE);
      if (statusEffect != null) {
         local6115_11 *= Math.max(0.0F, 1.0F - (statusEffect.getAmplifier() + 1) * 0.2F);
      }

      int local6115_13 = 0;

      for (EquipmentSlot equipmentSlot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
         local6115_13 += EnchantmentHelper.getLevel(
            mc.world.getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT).getOrThrow(Enchantments.PROTECTION),
            mc.player.getEquippedStack(equipmentSlot)
         );
      }

      return local6115_11 * (1.0F - Math.min(local6115_13, 20) / 25.0F);
   }

   private void method3682() {
      this.field12782 = false;
      this.field12781 = ItemStack.EMPTY;
      this.field12784 = -1;
   }

   private boolean method3684() {
      return this.field12778.isChecked()
         && !this.numericTextSetting.getText().isEmpty()
         && this.multiSelectSetting.matchesOption("Здоровье")
         && mc.player.getHealth() + mc.player.getAbsorptionAmount() <= this.field12771.getValue();
   }

   private void method3685() {
      if (mc.player != null && mc.interactionManager != null && mc.player.currentScreenHandler == mc.player.playerScreenHandler
         )
       {
         if ((this.method3687() || System.currentTimeMillis() < this.field12783) && !this.method3671()) {
            int local6121_1 = this.method3686();
            if (local6121_1 != -1) {
               if (!this.field12782) {
                  this.field12781 = mc.player.getOffHandStack().copy();
                  this.field12784 = local6121_1;
               }

               this.field12782 = true;
               this.field12785 = System.currentTimeMillis();
               InventoryActionQueue.method2656().method2660(local6121_1, 40, 1);
            }
         }
      }
   }

   private int method3686() {
      if (this.method3684()) {
         int local6122_1 = InventoryUtils.method0556(
            itemStack -> itemStack.getItem() == Items.TOTEM_OF_UNDYING
               && itemStack.hasEnchantments()
               && !EntityUtils.method2795(itemStack)
               && this.method3669(itemStack)
               && !mc.player.getItemCooldownManager().isCoolingDown(itemStack),
            0,
            35
         );
         if (local6122_1 != -1) {
            return local6122_1;
         }
      }

      if (!this.field12777.isChecked() && !this.field12778.isChecked()) {
         return InventoryUtils.method0556(
            itemStack2 -> itemStack2.getItem() == Items.TOTEM_OF_UNDYING
               && !AutoSwapModule.method2577().method2580(itemStack2)
               && !mc.player.getItemCooldownManager().isCoolingDown(itemStack2),
            0,
            35
         );
      }

      int local6122_1 = InventoryUtils.method0556(
         itemStack3 -> itemStack3.getItem() == Items.TOTEM_OF_UNDYING
            && !this.method3673(itemStack3)
            && !AutoSwapModule.method2577().method2580(itemStack3)
            && !mc.player.getItemCooldownManager().isCoolingDown(itemStack3),
         0,
         35
      );
      return local6122_1 != -1
         ? local6122_1
         : InventoryUtils.method0556(
            itemStack4 -> itemStack4.getItem() == Items.TOTEM_OF_UNDYING
               && !AutoSwapModule.method2577().method2580(itemStack4)
               && !mc.player.getItemCooldownManager().isCoolingDown(itemStack4),
            0,
            35
         );
   }

   private boolean method3687() {
      if ((
            !this.multiSelectSetting.matchesOption("Здоровье")
               || !(mc.player.getHealth() + mc.player.getAbsorptionAmount() <= this.field12771.getValue())
         )
         && (!this.multiSelectSetting.matchesOption("Падение") || !this.method3674())
         && (!this.multiSelectSetting.matchesOption("Динамит") || !this.method3666())
         && (!this.multiSelectSetting.matchesOption("Трезубец") || !this.method3668())
         && (!this.multiSelectSetting.matchesOption("Булава") || !this.method3700())
         && (!this.multiSelectSetting.matchesOption("Кристалл") || !this.method3676())) {
         return false;
      }

      this.field12783 = System.currentTimeMillis() + 100L;
      return true;
   }

   private boolean method3690() {
      return mc.player.getOffHandStack().isEmpty() && !this.field12781.isEmpty() && this.method3678() != -1;
   }

   private void method3691() {
      this.field12786.keySet().removeIf(local6131_0 -> mc.world.getEntityById(local6131_0) == null);

      for (AbstractClientPlayerEntity abstractClientPlayerEntity : mc.world.getPlayers()) {
         if (abstractClientPlayerEntity != mc.player) {
            if (!abstractClientPlayerEntity.isOnGround()
               && !(abstractClientPlayerEntity.getY() >= abstractClientPlayerEntity.lastY)
               && !abstractClientPlayerEntity.isGliding()
               && !abstractClientPlayerEntity.isTouchingWater()
               && !abstractClientPlayerEntity.isClimbing()) {
               this.field12786.merge(abstractClientPlayerEntity.getId(), abstractClientPlayerEntity.getY(), Math::max);
            } else {
               this.field12786.put(abstractClientPlayerEntity.getId(), abstractClientPlayerEntity.getY());
            }
         }
      }
   }

   private int method3694() {
      if (!this.field12781.isEmpty()) {
         int local6130_1 = this.method3678();
         return local6130_1 != -1 ? local6130_1 : InventoryUtils.method0546(0, 35);
      } else {
         return this.field12784 != -1 && mc.player.getInventory().getStack(this.field12784).isEmpty()
            ? this.field12784
            : InventoryUtils.method0546(0, 35);
      }
   }

   private void method3697() {
      if (mc.player != null && mc.interactionManager != null && mc.player.currentScreenHandler == mc.player.playerScreenHandler
         )
       {
         if (this.field12782 && !this.method3687() && System.currentTimeMillis() >= this.field12783) {
            if (mc.player.getOffHandStack().isEmpty()) {
               int local6133_1 = this.method3678();
               if (local6133_1 != -1) {
                  this.field12785 = System.currentTimeMillis();
                  this.method3682();
                  InventoryActionQueue.method2656().method2660(local6133_1, 40, 1);
               }
            } else if (mc.player.getOffHandStack().getItem() == Items.TOTEM_OF_UNDYING) {
               int local6133_1 = this.method3694();
               if (local6133_1 != -1) {
                  this.field12785 = System.currentTimeMillis();
                  this.method3682();
                  InventoryActionQueue.method2656().method2660(local6133_1, 40, 1);
               }
            }
         }
      }
   }

   private boolean method3700() {
      if (((BossBarHudMixin)mc.inGameHud.getBossBarHud()).getEvents().isEmpty()) {
         return false;
      }

      for (AbstractClientPlayerEntity abstractClientPlayerEntity : mc.world.getPlayers()) {
         if (!ServerHelperModule.method0707().modeSetting.isSelected("SpookyTime")) {
            if (abstractClientPlayerEntity != mc.player
               && !FriendStore.method3431(abstractClientPlayerEntity.getGameProfile().name())
               && abstractClientPlayerEntity.getMainHandStack().getItem() == Items.MACE
               && abstractClientPlayerEntity.getY() - mc.player.getY() >= 2.0
               && abstractClientPlayerEntity.getY() - abstractClientPlayerEntity.lastY < -0.1
               && mc.player.squaredDistanceTo(abstractClientPlayerEntity.getX(), mc.player.getY(), abstractClientPlayerEntity.getZ())
                  <= this.field12775.getValue() * this.field12775.getValue()
               && (abstractClientPlayerEntity.getX() - abstractClientPlayerEntity.lastX) * (mc.player.getX() - abstractClientPlayerEntity.getX())
                     + (abstractClientPlayerEntity.getZ() - abstractClientPlayerEntity.lastZ) * (mc.player.getZ() - abstractClientPlayerEntity.getZ())
                  >= 0.0) {
               return true;
            }
         } else if (abstractClientPlayerEntity != mc.player
            && !FriendStore.method3431(abstractClientPlayerEntity.getGameProfile().name())
            && abstractClientPlayerEntity.getMainHandStack().getItem() == Items.MACE
            && !(abstractClientPlayerEntity.getY() - mc.player.getY() < 2.0)
            && !(abstractClientPlayerEntity.getY() - abstractClientPlayerEntity.lastY >= -0.1)
            && !(
               mc.player.squaredDistanceTo(abstractClientPlayerEntity.getX(), mc.player.getY(), abstractClientPlayerEntity.getZ())
                  > this.field12775.getValue() * this.field12775.getValue()
            )
            && !(
               (abstractClientPlayerEntity.getX() - abstractClientPlayerEntity.lastX) * (mc.player.getX() - abstractClientPlayerEntity.getX())
                     + (abstractClientPlayerEntity.getZ() - abstractClientPlayerEntity.lastZ) * (mc.player.getZ() - abstractClientPlayerEntity.getZ())
                  < 0.0
            )) {
            double local6136_3 = this.field12786.getOrDefault(abstractClientPlayerEntity.getId(), abstractClientPlayerEntity.getY())
               - (mc.player.getY() + mc.player.getHeight());
            if (!(local6136_3 <= 1.5) && this.method3679(abstractClientPlayerEntity, (float)local6136_3) > 2.5F) {
               return true;
            }
         }
      }

      return false;
   }
}
