package deluxe.client.module.player;

import deluxe.client.setting.BooleanSetting;
import deluxe.client.event.EventListener;
import deluxe.client.setting.KeybindSetting;
import deluxe.client.module.Module;
import deluxe.client.module.ModuleCategory;
import deluxe.client.module.ModuleInfo;
import deluxe.client.event.MovementInputEvent;
import deluxe.client.event.PacketEvent;
import deluxe.client.event.PlayerTickEvent;
import deluxe.client.util.InventoryUtils;
import deluxe.client.util.InventoryActionQueue;
import deluxe.client.ui.hud.NotificationHud;
import deluxe.client.util.InventorySlotManager;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.component.type.AttributeModifiersComponent.Entry;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.PositionAndOnGround;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;

@ModuleInfo(name = "ElytraHelper", description = "Выполняет действия с элитрой по нажатию назначенной клавиши.", category = ModuleCategory.PLAYER)
public final class ElytraHelperModule extends Module {
   private static ElytraHelperModule elytraHelperModule;
   public KeybindSetting field02055;
   public KeybindSetting field02056;
   public BooleanSetting field02057;
   public BooleanSetting field02058;
   public BooleanSetting field02059;
   public BooleanSetting field02060;
   private boolean field02061;
   private boolean field02062;
   private int field02063;
   private boolean field02064;
   private boolean field02065;
   private final EventListener<PlayerTickEvent> field02066 = playerTickEvent -> {
      if (mc.player != null && mc.interactionManager != null) {
         if (mc.currentScreen != null) {
            this.field02064 = this.field02055.getBinding().method2749(mc.getWindow().getHandle());
            this.field02065 = this.field02056.getBinding().method2749(mc.getWindow().getHandle());
         } else {
            if (this.field02055.getBinding().method2749(mc.getWindow().getHandle()) && !this.field02064) {
               this.method0980();
            }

            this.field02064 = this.field02055.getBinding().method2749(mc.getWindow().getHandle());
            if (this.field02056.getBinding().method2749(mc.getWindow().getHandle()) && !this.field02065) {
               this.method0983();
            }

            this.field02065 = this.field02056.getBinding().method2749(mc.getWindow().getHandle());
         }
      }
   };
   private final EventListener<MovementInputEvent> field02067 = event -> {
      if (mc.player != null) {
         if (mc.player.age < 5) {
            this.field02061 = false;
         } else if (this.field02061 && this.method0978()) {
            this.onMovementInput(event);
         }
      }
   };
   public final EventListener<PacketEvent> field02068 = packetEvent -> {
      if (packetEvent.isIncoming()
         && packetEvent.getPacket() instanceof GameMessageS2CPacket gameMessageS2CPacket
         && this.field02061
         && gameMessageS2CPacket.content().getString().contains("Эту команду могут писать только донатеры выше рангом")) {
         this.field02062 = true;
      }
   };

   private boolean method0976(int local1336_1) {
      return this.field02063 + local1336_1 < mc.player.age;
   }

   private void onMovementInput(MovementInputEvent event) {
      boolean local1337_2 = mc.player.getEquippedStack(EquipmentSlot.CHEST).getItem() == Items.ELYTRA;
      if (!mc.player.isGliding() && local1337_2) {
         event.method4442(mc.player.age % 2 == 0);
         this.field02063 = mc.player.age;
         if (!this.field02060.isChecked()) {
            this.field02061 = false;
         }
      }

      if (mc.player.isGliding()
         && this.field02060.isChecked()
         && !mc.player.isTouchingWater()
         && !mc.player.isOnGround()
         && this.method0976(2)) {
         this.method0983();
         this.field02061 = false;
      }

      if (this.method0976(10)) {
         this.field02061 = false;
      }
   }

   private boolean method0978() {
      if (this.field02059.isChecked() && !this.field02062) {
         if (this.field02063 + 1 == mc.player.age) {
            mc.player.networkHandler.sendChatCommand("fly");
         }

         if (mc.player.getAbilities().allowFlying && this.field02063 <= mc.player.age && mc.player.isOnGround()) {
            double x = mc.player.getX();
            double local1338_3 = mc.player.getY() + 0.19999997F;
            double z = mc.player.getZ();
            mc.player
               .networkHandler
               .sendPacket(new PositionAndOnGround(x, local1338_3, z, false, mc.player.horizontalCollision));
            mc.player.setPosition(x, local1338_3, z);
            this.field02063 = mc.player.age + 9;
         }

         if (mc.player.getAbilities().allowFlying && !mc.player.isOnGround()) {
            mc.player.getAbilities().flying = true;
            mc.player.setVelocity(Vec3d.ZERO);
            mc.player.sendAbilitiesUpdate();
            this.field02061 = false;
         }

         if (!this.method0976(15) && (!this.method0976(3) || mc.player.getEquippedStack(EquipmentSlot.CHEST).getItem() == Items.ELYTRA)) {
            return false;
         }

         this.field02061 = false;
         return false;
      } else {
         return true;
      }
   }

   private void method0980() {
      if (!InventoryActionQueue.method2656().method2665() && !InventorySlotManager.method4176().method4179()) {
         boolean local1340_1 = mc.player.getEquippedStack(EquipmentSlot.CHEST).getItem() == Items.ELYTRA;
         int local1340_2 = local1340_1 ? this.method0986() : InventoryUtils.method0557(Items.ELYTRA);
         if (local1340_2 == -1) {
            NotificationHud.method2691(
               this.category.getIcon(),
               (local1340_1 ? Items.NETHERITE_CHESTPLATE : Items.ELYTRA).getDefaultStack().getName().getString(),
               Text.literal("нет в инвентаре").setStyle(Style.EMPTY.withColor(Formatting.RED)),
               true
            );
         } else {
            if (this.field02057.isChecked()) {
               InventorySlotManager.method4176().method4195(mc.player.getInventory().getStack(local1340_2).copy(), true);
            } else {
               InventoryActionQueue.method2656().method2668(local1340_2, 6, 1);
            }

            if (this.field02058.isChecked() && !local1340_1) {
               this.field02063 = mc.player.age;
               this.field02061 = true;
            }
         }
      }
   }

   private void method0983() {
      if (mc.player.isGliding()) {
         if (!InventorySlotManager.method4176().method4185(Items.FIREWORK_ROCKET.getDefaultStack())) {
            NotificationHud.method2691(
               this.category.getIcon(),
               Items.FIREWORK_ROCKET.getDefaultStack().getName().getString(),
               Text.literal("нет в инвентаре").setStyle(Style.EMPTY.withColor(Formatting.RED)),
               true
            );
         } else {
            InventorySlotManager.method4176().method4195(Items.FIREWORK_ROCKET.getDefaultStack(), true);
         }
      }
   }

   public static ElytraHelperModule method0984() {
      if (elytraHelperModule == null) {
         elytraHelperModule = new ElytraHelperModule();
      }

      return elytraHelperModule;
   }

   @Override
   protected void onDisable() {
      this.field02061 = false;
   }

   private int method0986() {
      RegistryEntry<Enchantment> orThrow = mc.world.getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT).getOrThrow(Enchantments.PROTECTION);
      int local1347_2 = -1;
      double local1347_3 = 0.0;

      for (int index = 0; index < 36; index++) {
         ItemStack stack = mc.player.getInventory().getStack(index);
         if (!stack.isEmpty()
            && stack.getItem() != Items.ELYTRA
            && stack.get(DataComponentTypes.EQUIPPABLE) != null
            && stack.get(DataComponentTypes.EQUIPPABLE).slot() == EquipmentSlot.CHEST) {
            double level = EnchantmentHelper.getLevel(orThrow, stack);
            AttributeModifiersComponent attributeModifiersComponent = stack.get(DataComponentTypes.ATTRIBUTE_MODIFIERS);
            if (attributeModifiersComponent != null) {
               for (Entry entry : attributeModifiersComponent.modifiers()) {
                  if (entry.attribute() == EntityAttributes.ARMOR || entry.attribute() == EntityAttributes.ARMOR_TOUGHNESS) {
                     level += entry.modifier().value();
                  }
               }
            }

            if (level > local1347_3) {
               local1347_3 = level;
               local1347_2 = index;
            }
         }
      }

      return local1347_2;
   }

   private ElytraHelperModule() {
      this.field02055 = this.addSetting(new KeybindSetting("Кнопка переключения", "Надевает элитры вместо нагрудника и обратно."));
      this.field02056 = this.addSetting(new KeybindSetting("Кнопка фейерверка", "Использует фейерверк в полёте по нажатию."));
      this.field02057 = this.addSetting(
         new BooleanSetting(
            "Свап использованием", "Меняет нагрудник правым кликом по надеваемому предмету (1.21.2+) вместо трёх кликов по слотам брони.", true
         )
      );
      this.field02058 = this.addSetting(new BooleanSetting("Автостарт после свапа", "Сам взлетает после свапа на элитры.", false));
      this.field02059 = this.addSetting(
         new BooleanSetting("Использовать /fly при свапе", "Взлетает через /fly, пока сервер разрешает.", false)
            .withVisibility(() -> this.field02058.isChecked())
      );
      this.field02060 = this.addSetting(
         new BooleanSetting("Автофейерверк", "Запускает фейерверк, как только полёт начался.", false).withVisibility(() -> this.field02058.isChecked())
      );
   }
}
