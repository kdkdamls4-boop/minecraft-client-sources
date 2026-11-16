package net.skycore.client.module.render;

import java.util.List;
import java.util.Set;
import net.skycore.client.hud.ArmorHudHud;
import net.skycore.client.hud.BindsHud;
import net.skycore.client.hud.CooldownsHud;
import net.skycore.client.hud.HotbarHud;
import net.skycore.client.hud.HotkeysHud;
import net.skycore.client.hud.InformationHud;
import net.skycore.client.hud.NotificationsHud;
import net.skycore.client.hud.PotionsHud;
import net.skycore.client.hud.TargetHudHud;
import net.skycore.client.hud.WatermarkHud;
import net.skycore.client.module.ClientModule;
import net.skycore.client.module.ModuleCategory;
import net.skycore.client.setting.MultiSelectSetting;
import net.skycore.client.setting.BooleanSetting;

public final class InterfaceModule extends ClientModule {
   private final HotkeysHud hotkeys;
   private final TargetHudHud targetHud;
   private final CooldownsHud cooldowns;
   private final BooleanSetting glow;
   private final PotionsHud potions;
   private final HotbarHud hotbar;
   private final NotificationsHud notifications;
   private final MultiSelectSetting elements = this.add(
      new MultiSelectSetting(
         "Elements",
         List.of("Watermark", "Information", "Hotkeys", "Binds", "Hotbar", "Potions", "Cooldowns", "ArmorHUD", "TargetHUD", "Notifications"),
         Set.of("Watermark", "Information", "Hotkeys", "Binds", "Hotbar", "Potions", "Cooldowns", "ArmorHUD", "TargetHUD", "Notifications")
      )
   );
   private final ArmorHudHud armorHud;
   private final BindsHud binds;
   private final InformationHud information;
   private final WatermarkHud watermark;

   public boolean isNotificationsEnabled() {
      if (this.isEnabled()) {
         if (this.elements.isSelected("Notifications")) {
            return true;
         }
      }

      return false;
   }

   public BindsHud binds() {
      return this.binds;
   }

   public HotkeysHud hotkeys() {
      return this.hotkeys;
   }

   public boolean isArmorHudEnabled() {
      return this.elements.isSelected("ArmorHUD");
   }

   public boolean isWatermarkEnabled() {
      return this.isEnabled() && this.elements.isSelected("Watermark");
   }

   public HotbarHud hotbar() {
      return this.hotbar;
   }

   public boolean isCooldownsEnabled() {
      if (this.isEnabled()) {
         if (this.elements.isSelected("Cooldowns")) {
            return true;
         }
      }

      return false;
   }

   public TargetHudHud targetHud() {
      return this.targetHud;
   }

   public boolean isInformationEnabled() {
      return this.elements.isSelected("Information");
   }

   public boolean isPotionsEnabled() {
      if (this.isEnabled()) {
         if (this.elements.isSelected("Potions")) {
            return true;
         }
      }

      return false;
   }

   public WatermarkHud watermark() {
      return this.watermark;
   }

   public boolean isHotkeysEnabled() {
      if (this.isEnabled()) {
         if (this.elements.isSelected("Hotkeys")) {
            return true;
         }
      }

      return false;
   }

   public boolean isBindsEnabled() {
      return this.isEnabled() && this.elements.isSelected("Binds");
   }

   public MultiSelectSetting elements() {
      return this.elements;
   }

   public CooldownsHud cooldowns() {
      return this.cooldowns;
   }

   public boolean isHotbarEnabled() {
      if (this.isEnabled()) {
         if (this.elements.isSelected("Hotbar")) {
            return true;
         }
      }

      return false;
   }

   public PotionsHud potions() {
      return this.potions;
   }

   public NotificationsHud notifications() {
      return this.notifications;
   }

   public boolean glowEnabled() {
      return this.isEnabled() && this.glow.get();
   }

   public InterfaceModule() {
      super("Interface", "HUD elements", ModuleCategory.RENDER);
      this.glow = this.add(new BooleanSetting("Glow", false));
      this.watermark = new WatermarkHud();
      this.information = new InformationHud();
      this.hotkeys = new HotkeysHud();
      this.binds = new BindsHud();
      this.hotbar = new HotbarHud();
      this.potions = new PotionsHud();
      this.cooldowns = new CooldownsHud();
      this.armorHud = new ArmorHudHud();
      this.targetHud = new TargetHudHud();
      this.notifications = new NotificationsHud();
      this.glow.visibleWhen(() -> {
         return !this.elements.get().isEmpty();
      });
      this.setEnabled(true);
   }

   public InformationHud information() {
      return this.information;
   }

   public ArmorHudHud armorHud() {
      return this.armorHud;
   }

   public boolean isTargetHudEnabled() {
      if (this.isEnabled()) {
         if (this.elements.isSelected("TargetHUD")) {
            return true;
         }
      }

      return false;
   }
}
