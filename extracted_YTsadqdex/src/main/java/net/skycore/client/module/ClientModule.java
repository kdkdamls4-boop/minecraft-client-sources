package net.skycore.client.module;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import net.skycore.client.setting.Setting;

public abstract class ClientModule {
   private static final List<ClientModule.ModuleToggleListener> TOGGLE_LISTENERS = new CopyOnWriteArrayList<>();
   private int keyBind;
   private final List<Setting<?>> settings = new ArrayList<>();
   private final String description;
   private static final ThreadLocal<Integer> SILENT = ThreadLocal.withInitial(() -> {
      byte flag = -1;
      return 0;
   });
   private boolean enabled;
   private BindMode bindMode;
   private final String name;
   private final ModuleCategory category;

   public final String getName() {
      return this.name;
   }

   public static void addToggleListener(ClientModule.ModuleToggleListener elements) {
      if (elements != null) {
         TOGGLE_LISTENERS.add(elements);
      }
   }

   public final String getDescription() {
      return this.description;
   }

   public final ModuleCategory getCategory() {
      return this.category;
   }

   public final void setKeyBind(int keyBind2) {
      this.keyBind = keyBind2;
   }

   public final List<Setting<?>> getSettings() {
      return Collections.unmodifiableList(this.settings);
   }

   protected final void addSettings(Setting<?>... settingValues) {

      for (Setting setting : settingValues) {
         this.add(setting);
      }
   }

   protected ClientModule(String name2, String description2, ModuleCategory category2) {
      this.keyBind = -1;
      this.bindMode = BindMode.TOGGLE;
      this.name = Objects.requireNonNull(name2);
      this.description = description2 == null ? "" : description2;
      this.category = Objects.requireNonNull(category2);
   }

   public static void setToggleListener(ClientModule.ModuleToggleListener toggleListener) {
      TOGGLE_LISTENERS.clear();
      if (toggleListener != null) {
         TOGGLE_LISTENERS.add(toggleListener);
      }
   }

   protected final <T extends Setting<?>> T add(T t) {
      var settings2 = this.settings;
      Setting setting = Objects.requireNonNull(t);
      settings2.add(setting);
      return (T)t;
   }

   public final void setEnabled(boolean enabled2) {
      if (this.enabled != enabled2) {
         this.enabled = enabled2;
         if (enabled2) {
            this.onEnable();
         } else {
            this.onDisable();
         }

         if (!silent()) {
            for (ClientModule.ModuleToggleListener elements : TOGGLE_LISTENERS) {
               elements.onToggle(this, enabled2);
            }
         }
      }
   }

   public final void tick() {
      if (this.enabled) {
         this.onTick();
      }
   }

   public final void setBindMode(BindMode bindMode2) {
      BindMode bindMode3 = bindMode2 == null ? BindMode.TOGGLE : bindMode2;
      this.bindMode = bindMode3;
   }

   public final BindMode getBindMode() {
      return this.bindMode;
   }

   protected void onDisable() {
   }

   public final void toggle() {
      this.setEnabled(!this.enabled);
   }

   public final boolean isEnabled() {
      return this.enabled;
   }

   public boolean affectsGameplay() {
      if (this.category != ModuleCategory.COMBAT) {
         if (this.category != ModuleCategory.MOVEMENT) {
            if (this.category != ModuleCategory.PLAYER) {
               return false;
            }
         }
      }

      return true;
   }

   protected void onEnable() {
   }

   // $VF: Could not verify finally blocks. A semaphore variable has been added to preserve control flow.
   // Please report this to the Vineflower issue tracker, at https://github.com/Vineflower/vineflower/issues with a copy of the class file (if you have the rights to distribute it!)
   public static void runSilently(Runnable runnable) {
      int intermediate = -1;
      int intermediate2 = -1;
      SILENT.set(SILENT.get() + 1);
      boolean condition = false /* VF: Semaphore variable */;

      try {
         condition = true;
         intermediate2 = (byte)-1;
         runnable.run();
         condition = false;
      } finally {
         if (condition) {
            ThreadLocal sILENT = SILENT;
            intermediate = Math.max(0, SILENT.get() - 1);
            Integer value = intermediate;
            sILENT.set(value);
         }
      }

      ThreadLocal sILENT2 = SILENT;
      Integer value2 = SILENT.get();
      intermediate2 = value2 - 1;
      intermediate = Math.max(0, intermediate2);
      Integer value3 = intermediate;
      sILENT2.set(value3);
   }

   public final int getKeyBind() {
      return this.keyBind;
   }

   private static boolean silent() {
      return SILENT.get() > 0;
   }

   protected void onTick() {
   }

   @FunctionalInterface
   public interface ModuleToggleListener {
      public void onToggle(ClientModule clientModule, boolean condition);
   }
}
