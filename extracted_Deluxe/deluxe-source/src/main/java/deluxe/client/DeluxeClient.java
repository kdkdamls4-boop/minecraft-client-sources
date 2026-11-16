package deluxe.client;

import deluxe.client.storage.AccountStore;
import deluxe.client.command.AutowardenCommand;
import deluxe.client.command.BlockespCommand;
import deluxe.client.ui.ClickGuiScreen;
import deluxe.client.command.ClientCommand;
import deluxe.client.event.ClientTickEvent;
import deluxe.client.command.CommandDispatcher;
import deluxe.client.command.ConfigCommand;
import deluxe.client.cosmetic.CosmeticFeatureRenderer;
import deluxe.client.ui.MainMenuScreen;
import deluxe.client.event.Event;
import deluxe.client.event.EventBus;
import deluxe.client.command.FriendCommand;
import deluxe.client.command.HelpCommand;
import deluxe.client.ui.hud.HudLayout;
import deluxe.client.command.IrcCommand;
import deluxe.client.command.MacroCommand;
import deluxe.client.storage.MacroStore;
import deluxe.client.module.ModuleManager;
import deluxe.client.command.PartyCommand;
import ru.dreamix.protection.api.impl.profile.ProfileAPI;
import deluxe.client.command.RctCommand;
import deluxe.client.command.WaypointCommand;
import deluxe.client.util.FrameTimer;
import deluxe.client.util.WaypointManager;
import deluxe.client.command.ResourcePackCommand;
import deluxe.client.ui.UiWarmup;
import deluxe.client.render.ThemePaletteBuffer;
import deluxe.client.render.ItemRenderCache;
import deluxe.client.render.PipelineWarmup;
import deluxe.client.storage.ConfigStore;
import deluxe.client.util.IrcClient;
import deluxe.client.cosmetic.CosmeticRenderDispatcher;
import deluxe.client.cosmetic.CosmeticWarmup;
import deluxe.client.render.Render2D;
import ru.dreamix.protection.api.impl.profile.objects.RoleObject;
import deluxe.client.command.StaffCommand;

import java.util.ArrayList;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents.ClientStopping;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.EndTick;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.StartTick;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.option.KeyBinding.Category;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.util.InputUtil.Type;
import net.minecraft.resource.ResourceType;
import net.minecraft.resource.SynchronousResourceReloader;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeluxeClient implements ModInitializer, ClientModInitializer {
   public static final String TEXT_DELUXE = "deluxe";
   private static final DeluxeClient deluxeClient = new DeluxeClient();
   private static final Logger LOGGER = LoggerFactory.getLogger("deluxe");
   private long loadTimeMillis;
   private boolean loaded;
   private int autosaveTicks;
   private CommandDispatcher commandDispatcher;
   private KeyBinding openGuiKey;
   private final FrameTimer frameTimer = new FrameTimer();

   public boolean loaded() {
      return this.loaded;
   }

   public static Identifier id(String local6600_0) {
      return Identifier.of("deluxe", local6600_0);
   }

   public KeyBinding openGuiKey() {
      return this.openGuiKey;
   }

   private void registerOpenKey() {
      this.openGuiKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.deluxe.clickgui", Type.KEYSYM, 344, Category.GAMEPLAY));
      ClientTickEvents.END_CLIENT_TICK.register((EndTick)minecraftClient -> {
         while (this.openGuiKey.wasPressed()) {
            if (minecraftClient.currentScreen == null) {
               minecraftClient.setScreen(ClickGuiScreen.instance());
            }
         }
      });
      if (Boolean.getBoolean("deluxe.openGui") || System.getenv("DELUXE_OPEN_GUI") != null) {
         ClientTickEvents.END_CLIENT_TICK.register((EndTick)minecraftClient2 -> {
            if (minecraftClient2.currentScreen instanceof MainMenuScreen) {
               minecraftClient2.setScreen(ClickGuiScreen.instance());
            }
         });
      }
   }

   public CommandDispatcher commandDispatcher() {
      return this.commandDispatcher;
   }

   private void registerTicks() {
      ClientTickEvents.START_CLIENT_TICK.register((StartTick)minecraftClient -> {
         if (EventBus.getInstance().hasListeners(ClientTickEvent.class)) {
            EventBus.getInstance().post(new ClientTickEvent(Event.Stage.PRE));
         }
      });
      ClientTickEvents.END_CLIENT_TICK.register((EndTick)minecraftClient2 -> {
         if (EventBus.getInstance().hasListeners(ClientTickEvent.class)) {
            EventBus.getInstance().post(new ClientTickEvent(Event.Stage.POST));
         }

         if (this.loaded && ++this.autosaveTicks >= 1200) {
            this.autosaveTicks = 0;
            ConfigStore.autosave();
         }
      });
   }

   public static Logger logger() {
      return LOGGER;
   }

   @Override
   public String toString() {
      return "Deluxe(loadTimeMs="
         + this.loadTimeMs()
         + ", loaded="
         + this.loaded()
         + ", autosaveTicks="
         + this.autosaveTicks()
         + ", commandDispatcher="
         + this.commandDispatcher()
         + ", openGuiKey="
         + this.openGuiKey()
         + ", framelibrary="
         + this.framelibrary()
         + ")";
   }

   public int autosaveTicks() {
      return this.autosaveTicks;
   }

   private void registerCosmetics() {
      CosmeticWarmup.method2917();
      CosmeticWarmup.method2911();
      LivingEntityFeatureRendererRegistrationCallback.EVENT
         .register((LivingEntityFeatureRendererRegistrationCallback)(local6603_0, local6603_1, registrationHelper, context) -> {
            if (local6603_1 instanceof PlayerEntityRenderer<?> local6603_4) {
               registrationHelper.register(new CosmeticFeatureRenderer(local6603_4));
            }
         });
      CosmeticRenderDispatcher.method2887();
   }

   @Override
   public boolean equals(Object local6612_1) {
      if (local6612_1 == this) {
         return true;
      } else if (!(local6612_1 instanceof DeluxeClient currentDeluxeClient)) {
         return false;
      } else {
         if (!currentDeluxeClient.canEqual(this)) {
            return false;
         }

         if (this.loadTimeMs() != currentDeluxeClient.loadTimeMs()) {
            return false;
         }

         if (this.loaded() != currentDeluxeClient.loaded()) {
            return false;
         }

         if (this.autosaveTicks() != currentDeluxeClient.autosaveTicks()) {
            return false;
         }

         Object local6612_3 = this.commandDispatcher();
         Object local6612_4 = currentDeluxeClient.commandDispatcher();
         if (local6612_3 == null ? local6612_4 == null : local6612_3.equals(local6612_4)) {
            Object local6612_5 = this.openGuiKey();
            Object local6612_6 = currentDeluxeClient.openGuiKey();
            if (local6612_5 == null ? local6612_6 == null : local6612_5.equals(local6612_6)) {
               Object local6612_7 = this.framelibrary();
               Object local6612_8 = currentDeluxeClient.framelibrary();
               return local6612_7 == null ? local6612_8 == null : local6612_7.equals(local6612_8);
            } else {
               return false;
            }
         } else {
            return false;
         }
      }
   }

   public DeluxeClient commandDispatcher(CommandDispatcher newCommandDispatcher) {
      this.commandDispatcher = newCommandDispatcher;
      return this;
   }

   public void unload() {
      ConfigStore.saveActiveConfig();
      ItemRenderCache.method2350();
      Render2D.method4714();
      this.loaded = false;
      LOGGER.info("shutdown");
   }

   public void load() {
      ProfileAPI.create("user", 1, RoleObject.ADMIN);
      this.loadTimeMillis = System.currentTimeMillis();
      System.setProperty("sun.font.layout.ffm", "false");
      Render2D.method4728();
      ItemRenderCache.method2335();
      ThemePaletteBuffer.method2143();
      UiWarmup.method1949();
      HudLayout.method4568();
      ModuleManager.initializeModules();
      EventBus.getInstance().subscribe(AccountStore.method4153());
      EventBus.getInstance().subscribe(WaypointManager.method0947());
      EventBus.getInstance().subscribe(MacroStore.method4213());
      EventBus.getInstance().subscribe(IrcClient.method2834());
      this.registerCosmetics();
      this.initCommands();
      this.registerOpenKey();
      this.registerTicks();
      ConfigStore.loadLastConfig();
      ClientLifecycleEvents.CLIENT_STOPPING.register((ClientStopping)minecraftClient -> this.unload());
      Runtime.getRuntime().addShutdownHook(new Thread(() -> {
         if (this.loaded) {
            ConfigStore.saveActiveConfig();
         }
      }));
      ResourceLoader.get(ResourceType.CLIENT_RESOURCES).registerReloader(id("render_warmup"), (SynchronousResourceReloader)resourceManager -> {
         MinecraftClient minecraftClient2 = MinecraftClient.getInstance();
         if (minecraftClient2 != null) {
            minecraftClient2.execute(() -> {
               Render2D.method4652();
               PipelineWarmup.resetWarmup();
               UiWarmup.method1947();
            });
         }
      });
      this.loaded = true;
   }

   public DeluxeClient loaded(boolean newLoaded) {
      this.loaded = newLoaded;
      return this;
   }

   public DeluxeClient loadTimeMs(long newLoadTimeMillis) {
      this.loadTimeMillis = newLoadTimeMillis;
      return this;
   }

   public DeluxeClient autosaveTicks(int newAutosaveTicks) {
      this.autosaveTicks = newAutosaveTicks;
      return this;
   }

   public void onInitializeClient() {
      deluxeClient.load();
   }

   public FrameTimer framelibrary() {
      return this.frameTimer;
   }

   public DeluxeClient openGuiKey(KeyBinding keyBinding) {
      this.openGuiKey = keyBinding;
      return this;
   }

   @Override
   public int hashCode() {
      int local6627_1 = 59;
      int local6627_2 = 1;
      long local6627_3 = this.loadTimeMs();
      local6627_2 = local6627_2 * 59 + (int)(local6627_3 >>> 32 ^ local6627_3);
      local6627_2 = local6627_2 * 59 + (this.loaded() ? 79 : 97);
      local6627_2 = local6627_2 * 59 + this.autosaveTicks();
      Object local6627_5 = this.commandDispatcher();
      local6627_2 = local6627_2 * 59 + (local6627_5 == null ? 43 : local6627_5.hashCode());
      Object local6627_6 = this.openGuiKey();
      local6627_2 = local6627_2 * 59 + (local6627_6 == null ? 43 : local6627_6.hashCode());
      Object local6627_7 = this.framelibrary();
      return local6627_2 * 59 + (local6627_7 == null ? 43 : local6627_7.hashCode());
   }

   public static DeluxeClient inst() {
      return deluxeClient;
   }

   private void initCommands() {
      ArrayList<ClientCommand> local6629_1 = new ArrayList<>();
      local6629_1.add(new ResourcePackCommand());
      local6629_1.add(new ConfigCommand());
      local6629_1.add(new FriendCommand());
      local6629_1.add(new WaypointCommand());
      local6629_1.add(new MacroCommand());
      local6629_1.add(new StaffCommand());
      local6629_1.add(new BlockespCommand());
      local6629_1.add(new AutowardenCommand());
      local6629_1.add(new RctCommand());
      local6629_1.add(new IrcCommand());
      local6629_1.add(new PartyCommand());
      local6629_1.add(new HelpCommand(local6629_1));
      this.commandDispatcher = new CommandDispatcher(local6629_1, ".");
   }

   public void onInitialize() {
      LOGGER.info("Deluxe initialized");
   }

   public long loadTimeMs() {
      return this.loadTimeMillis;
   }

   protected boolean canEqual(Object local6635_1) {
      return local6635_1 instanceof DeluxeClient;
   }
}
