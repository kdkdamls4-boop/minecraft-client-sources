package deluxe.client.ui;

import deluxe.client.render.FontFace;
import deluxe.client.module.render.InterfaceModule;
import ru.dreamix.protection.api.impl.profile.ProfileAPI;
import deluxe.client.util.GuiScale;
import deluxe.client.render.FontText;
import deluxe.client.ui.UiTheme;
import deluxe.client.ui.panel.AccountManagerPanel;
import deluxe.client.ui.AnimatedValue;
import deluxe.client.render.Render2D;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import net.minecraft.util.Util;

public class MainMenuScreen extends Screen {
   private final AccountManagerPanel accountManagerPanel = new AccountManagerPanel();
   private final CosmeticsSelectionScreen cosmeticsSelectionScreen = new CosmeticsSelectionScreen();
   private boolean field14607 = false;
   private boolean field14608 = false;
   private final AnimatedValue field14609 = new AnimatedValue(260L);
   private final AnimatedValue field14610 = new AnimatedValue(260L);
   private final Map<String, AnimatedValue> field14611 = new HashMap<>();
   private final List<MainMenuScreen.Nested0524> field14612 = new ArrayList<>();
   private boolean field14613;
   private long field14614 = System.currentTimeMillis();

   private String welcomeName() {
      return ProfileAPI.get() != null
            && ProfileAPI.get().getModel() != null
            && ProfileAPI.get().getModel().getName() != null
            && !ProfileAPI.get().getModel().getName().isBlank()
         ? ProfileAPI.get().getModel().getName()
         : "Player";
   }

   @Override
   public boolean shouldCloseOnEsc() {
      return false;
   }

   @Override
   public boolean mouseReleased(Click click) {
      return this.field14608 && click.button() == 0 && this.cosmeticsSelectionScreen.method4636() ? true : super.mouseReleased(click);
   }

   @Override
   protected void init() {
      super.init();
      if (System.currentTimeMillis() - this.field14614 > 600L) {
         this.field14614 = System.currentTimeMillis();
         this.field14613 = false;
         this.field14607 = false;
         this.field14608 = false;
         this.field14609.method3870(0.0);
         this.field14610.method3870(0.0);
         this.accountManagerPanel.method3244();
         this.cosmeticsSelectionScreen.method4628();
      }
   }

   public MainMenuScreen(boolean local7240_1) {
      super(Text.literal("Deluxe"));
      this.field14613 = local7240_1;
   }

   @Override
   public boolean mouseClicked(Click click, boolean local7241_2) {
      if (click.button() == 0) {
         float local7241_3 = spaceFactor();
         float local7241_4 = (float)click.x() * local7241_3;
         float local7241_5 = (float)click.y() * local7241_3;
         if (this.field14607 && this.field14609.method3871() > 0.6 && this.accountManagerPanel.method3243(local7241_4, local7241_5)) {
            return true;
         }

         if (this.field14608 && this.field14610.method3871() > 0.6 && this.cosmeticsSelectionScreen.method4632(local7241_4, local7241_5)) {
            return true;
         }

         for (int index = this.field14612.size() - 1; index >= 0; index--) {
            if (UiTheme.method2879(
               local7241_4,
               local7241_5,
               this.field14612.get(index).x(),
               this.field14612.get(index).y(),
               this.field14612.get(index).w(),
               this.field14612.get(index).h()
            )) {
               this.field14612.get(index).action().run();
               return true;
            }
         }
      }

      return super.mouseClicked(click, local7241_2);
   }

   public MainMenuScreen() {
      this(false);
   }

   private void drawMenuColumn(float local7244_1, float local7244_2, float local7244_3, float local7244_4, float local7244_5, float local7244_6) {
      MinecraftClient minecraftClient = this.client != null ? this.client : MinecraftClient.getInstance();
      float local7244_8 = local7244_1 + 51.5F * local7244_2;
      boolean local7244_9 = local7244_6 < 0.5F;
      float local7244_10 = this.slideInPC(0);
      Render2D.method4705(local7244_10);
      float local7244_11 = local7244_8 - (1.0F - local7244_10) * 16.0F * local7244_2;
      FontText.method1397(
         FontFace.SEMI,
         "Deluxe client  /  Главная",
         local7244_11,
         local7244_3 + 127.5F * local7244_2,
         6.5F * local7244_2,
         UiTheme.field09708
      );
      FontText.method1397(
         FontFace.SEMI, "Рады видеть тебя,", local7244_11, local7244_3 + 142.0F * local7244_2, 20.0F * local7244_2, UiTheme.field09680
      );
      FontText.method1397(
         FontFace.SEMI,
         this.welcomeName() + ".",
         local7244_11,
         local7244_3 + 166.2F * local7244_2,
         20.0F * local7244_2,
         UiTheme.field09680
      );
      FontText.method1397(
         FontFace.SEMI,
         "Всё готово - выбирай режим и начинай играть.",
         local7244_11,
         local7244_3 + 201.0F * local7244_2,
         7.5F * local7244_2,
         UiTheme.field09708
      );
      Render2D.method4650();
      this.drawOnlineButton(
         local7244_8,
         local7244_3 + 227.0F * local7244_2,
         local7244_2,
         local7244_4,
         local7244_5,
         local7244_9,
         () -> minecraftClient.setScreen(new MultiplayerScreen(this))
      );
      this.drawRow(
         local7244_8,
         local7244_3 + 270.0F * local7244_2,
         local7244_2,
         2,
         "assets/deluxe/textures/gui/mainmenu/icons/singleplayer.png",
         6.0F,
         "Одиночная игра",
         null,
         0,
         local7244_4,
         local7244_5,
         local7244_9,
         () -> minecraftClient.setScreen(new SelectWorldScreen(this))
      );
      this.drawRow(
         local7244_8,
         local7244_3 + 295.5F * local7244_2,
         local7244_2,
         3,
         "assets/deluxe/textures/gui/mainmenu/icons/accounts.png",
         6.0F,
         "Ваши аккаунты",
         "assets/deluxe/textures/gui/mainmenu/icons/bell.png",
         UiTheme.field09714,
         local7244_4,
         local7244_5,
         local7244_9,
         this::toggleAlt
      );
      this.drawRow(
         local7244_8,
         local7244_3 + 321.0F * local7244_2,
         local7244_2,
         4,
         "assets/deluxe/textures/gui/mainmenu/icons/cos_menu.png",
         6.5F,
         "Косметика",
         "assets/deluxe/textures/gui/mainmenu/icons/chevron.png",
         UiTheme.field09708,
         local7244_4,
         local7244_5,
         local7244_9,
         this::toggleCosmetics
      );
      this.drawRow(
         local7244_8,
         local7244_3 + 346.5F * local7244_2,
         local7244_2,
         5,
         "assets/deluxe/textures/gui/mainmenu/icons/settings.png",
         7.0F,
         "Настройки игры",
         null,
         0,
         local7244_4,
         local7244_5,
         local7244_9,
         () -> minecraftClient.setScreen(new OptionsScreen(this, minecraftClient.options))
      );
      this.drawRow(
         local7244_8,
         local7244_3 + 379.5F * local7244_2,
         local7244_2,
         6,
         "assets/deluxe/textures/gui/mainmenu/icons/exit.png",
         7.0F,
         "Выйти из игры",
         "assets/deluxe/textures/gui/mainmenu/icons/chevron.png",
         UiTheme.field09708,
         local7244_4,
         local7244_5,
         local7244_9,
         minecraftClient::scheduleStop
      );
   }

   private void drawRow(
      float local7245_1,
      float local7245_2,
      float local7245_3,
      int local7245_4,
      String local7245_5,
      float local7245_6,
      String local7245_7,
      String local7245_8,
      int local7245_9,
      float local7245_10,
      float local7245_11,
      boolean local7245_12,
      Runnable runnable
   ) {
      float local7245_14 = this.slideInPC(local7245_4);
      Render2D.method4705(local7245_14);
      float local7245_15 = local7245_1 - (1.0F - local7245_14) * 16.0F * local7245_3;
      float local7245_16 = this.hoverPC(
         "row" + local7245_7,
         local7245_12 && UiTheme.method2879(local7245_10, local7245_11, local7245_15, local7245_2, 226.5F * local7245_3, 19.5F * local7245_3)
      );
      local7245_15 += local7245_16 * 2.0F * local7245_3;
      Render2D.method4715(
         local7245_15,
         local7245_2,
         226.5F * local7245_3,
         19.5F * local7245_3,
         6.0F * local7245_3,
         UiTheme.method2877(UiTheme.field09680, 0.031F + 0.05F * local7245_16)
      );
      Render2D.method4769(
         local7245_15,
         local7245_2,
         226.5F * local7245_3,
         19.5F * local7245_3,
         6.0F * local7245_3,
         0.5F,
         UiTheme.method2877(UiTheme.field09680, 0.078F + 0.09F * local7245_16)
      );
      UiTheme.method2876(
         local7245_5,
         local7245_15 + 11.0F * local7245_3,
         local7245_2 + 9.75F * local7245_3,
         local7245_6 * local7245_3,
         local7245_6 * local7245_3,
         UiTheme.method2881(UiTheme.field09707, UiTheme.field09680, local7245_16)
      );
      FontText.method1397(
         FontFace.SEMI,
         local7245_7,
         local7245_15 + 18.0F * local7245_3,
         local7245_2 + 6.25F * local7245_3,
         7.0F * local7245_3,
         UiTheme.method2881(UiTheme.field09707, UiTheme.field09680, local7245_16)
      );
      if (local7245_8 != null) {
         UiTheme.method2876(
            local7245_8,
            local7245_15 + 214.5F * local7245_3 + local7245_16 * 1.5F * local7245_3,
            local7245_2 + 9.75F * local7245_3,
            7.0F * local7245_3,
            7.0F * local7245_3,
            local7245_9
         );
      }

      Render2D.method4650();
      if (local7245_12 && local7245_14 > 0.7F) {
         this.field14612.add(new MainMenuScreen.Nested0524(local7245_15, local7245_2, 226.5F * local7245_3, 19.5F * local7245_3, runnable));
      }
   }

   @Override
   public boolean shouldPause() {
      return false;
   }

   private void drawOnlineButton(
      float local7248_1, float local7248_2, float local7248_3, float local7248_4, float local7248_5, boolean local7248_6, Runnable runnable
   ) {
      float local7248_8 = this.slideInPC(1);
      Render2D.method4705(local7248_8);
      float local7248_9 = local7248_1 - (1.0F - local7248_8) * 16.0F * local7248_3;
      float local7248_10 = this.hoverPC(
         "rowОнлайн",
         local7248_6 && UiTheme.method2879(local7248_4, local7248_5, local7248_9, local7248_2, 226.5F * local7248_3, 37.0F * local7248_3)
      );
      Render2D.method4715(
         local7248_9,
         local7248_2,
         226.5F * local7248_3,
         37.0F * local7248_3,
         6.0F * local7248_3,
         UiTheme.method2881(UiTheme.field09686, UiTheme.field09687, local7248_10 * 0.55F)
      );
      Render2D.method4769(
         local7248_9, local7248_2, 226.5F * local7248_3, 37.0F * local7248_3, 6.0F * local7248_3, 0.5F, UiTheme.field09713
      );
      Render2D.method4677(
         "assets/deluxe/textures/gui/mainmenu/icons/pattern.png",
         local7248_9 + 173.0F * local7248_3 + local7248_10 * 2.0F * local7248_3,
         local7248_2 + 5.0F * local7248_3,
         44.0F * local7248_3,
         32.0F * local7248_3,
         0.0F,
         1.0F,
         0.0F,
         0.0F,
         1.0F,
         0.81F,
         UiTheme.method2877(UiTheme.field09680, 0.25F)
      );
      FontText.method1397(
         FontFace.SEMI,
         "Онлайн игра",
         local7248_9 + 8.5F * local7248_3,
         local7248_2 + 6.5F * local7248_3,
         7.0F * local7248_3,
         UiTheme.field09689
      );
      float local7248_11 = local7248_2 + 16.5F * local7248_3;

      for (String local7248_13 : UiTheme.method2873(
         FontFace.SEMI, "Подключайтесь к серверам и играйте вместе с другими.", 5.5F * local7248_3, 118.0F * local7248_3
      )) {
         FontText.method1397(
            FontFace.SEMI,
            local7248_13,
            local7248_9 + 8.5F * local7248_3,
            local7248_11,
            5.5F * local7248_3,
            UiTheme.method2877(UiTheme.field09689, 0.5F)
         );
         local7248_11 += 6.65F * local7248_3;
      }

      Render2D.method4650();
      if (local7248_6 && local7248_8 > 0.7F) {
         this.field14612.add(new MainMenuScreen.Nested0524(local7248_9, local7248_2, 226.5F * local7248_3, 37.0F * local7248_3, runnable));
      }
   }

   @Override
   public void renderBackground(DrawContext drawContext, int local7249_2, int local7249_3, float local7249_4) {
   }

   @Override
   public boolean mouseDragged(Click click, double local7250_2, double local7250_4) {
      return this.field14608
            && click.button() == 0
            && this.cosmeticsSelectionScreen.method4610((float)click.x() * spaceFactor(), (float)click.y() * spaceFactor())
         ? true
         : super.mouseDragged(click, local7250_2, local7250_4);
   }

   private void drawChrome(float local7251_1, float local7251_2, float local7251_3, float local7251_4, float local7251_5) {
      float local7251_6 = UiTheme.method2875(UiTheme.method2885(this.field14614, this.field14613 ? 260L : 60L, 420L));
      Render2D.method4705(local7251_6);
      Render2D.method4777(
         "assets/deluxe/textures/gui/mainmenu/icons/logo.png",
         local7251_1 + 51.5F * local7251_3,
         40.0F * local7251_3 - (1.0F - local7251_6) * 8.0F * local7251_3,
         13.5F * local7251_3,
         10.5F * local7251_3,
         0.0F,
         UiTheme.field09686
      );
      Render2D.method4650();
      float local7251_7 = UiTheme.method2875(UiTheme.method2885(this.field14614, this.field14613 ? 700L : 300L, 420L));
      Render2D.method4705(local7251_7);
      float local7251_8 = this.hoverPC(
         "socialTelegram",
         UiTheme.method2879(
            local7251_4, local7251_5, local7251_1 + 47.5F * local7251_3, local7251_2 - 59.0F * local7251_3, 16.5F * local7251_3, 16.5F * local7251_3
         )
      );
      UiTheme.method2876(
         "assets/deluxe/textures/gui/mainmenu/icons/telegram.png",
         local7251_1 + 55.75F * local7251_3,
         local7251_2 - 50.75F * local7251_3 - local7251_8 * 1.5F * local7251_3,
         8.5F * local7251_3,
         8.5F * local7251_3,
         UiTheme.method2881(UiTheme.field09707, UiTheme.field09680, local7251_8)
      );
      float local7251_9 = this.hoverPC(
         "socialDiscord",
         UiTheme.method2879(
            local7251_4, local7251_5, local7251_1 + 65.0F * local7251_3, local7251_2 - 59.0F * local7251_3, 16.5F * local7251_3, 16.5F * local7251_3
         )
      );
      UiTheme.method2876(
         "assets/deluxe/textures/gui/mainmenu/icons/discord.png",
         local7251_1 + 73.25F * local7251_3,
         local7251_2 - 50.75F * local7251_3 - local7251_9 * 1.5F * local7251_3,
         8.5F * local7251_3,
         8.5F * local7251_3,
         UiTheme.method2881(UiTheme.field09707, UiTheme.field09680, local7251_9)
      );
      Render2D.method4650();
      if (local7251_7 > 0.7F) {
         this.field14612
            .add(
               new MainMenuScreen.Nested0524(
                  local7251_1 + 47.5F * local7251_3,
                  local7251_2 - 59.0F * local7251_3,
                  16.5F * local7251_3,
                  16.5F * local7251_3,
                  () -> Util.getOperatingSystem().open("https://t.me/delnmt")
               )
            );
         this.field14612
            .add(
               new MainMenuScreen.Nested0524(
                  local7251_1 + 65.0F * local7251_3,
                  local7251_2 - 59.0F * local7251_3,
                  16.5F * local7251_3,
                  16.5F * local7251_3,
                  () -> Util.getOperatingSystem().open("https://discord.gg/wWsxQkF2tP")
               )
            );
      }
   }

   @Override
   public boolean mouseScrolled(double local7252_1, double local7252_3, double local7252_5, double local7252_7) {
      float local7252_9 = spaceFactor();
      if (this.field14607 && this.accountManagerPanel.method3245((float)local7252_1 * local7252_9, (float)local7252_3 * local7252_9, (float)local7252_7)) {
         return true;
      } else {
         return this.field14608 && this.cosmeticsSelectionScreen.method4621((float)local7252_1 * local7252_9, (float)local7252_3 * local7252_9, (float)local7252_7)
            ? true
            : super.mouseScrolled(local7252_1, local7252_3, local7252_5, local7252_7);
      }
   }

   @Override
   public void render(DrawContext drawContext, int local7253_2, int local7253_3, float local7253_4) {
      float local7253_5 = this.width * spaceFactor();
      float local7253_6 = this.height * spaceFactor();
      float local7253_7 = Math.min(local7253_5 / 960.0F, local7253_6 / 540.0F);
      float local7253_8 = (local7253_6 - 540.0F * local7253_7) / 2.0F;
      float local7253_9 = local7253_2 * spaceFactor();
      float local7253_10 = local7253_3 * spaceFactor();
      this.field14612.clear();
      InterfaceModule.method2247().method2250();
      UiTheme.field09741 = true;
      Render2D.method4713(drawContext);
      this.field14609.method3869(this.field14607 ? 1.0 : 0.0);
      this.field14610.method3869(this.field14608 ? 1.0 : 0.0);
      float local7253_11 = (float)this.field14609.method3871();
      float local7253_12 = (float)this.field14610.method3871();
      float local7253_13 = Math.max(local7253_11, local7253_12);
      UiTheme.method2882("assets/deluxe/textures/gui/mainmenu/background/menu.png", local7253_5, local7253_6, 1.0F);
      if (this.field14613) {
         float local7253_14 = UiTheme.method2875(UiTheme.method2885(this.field14614, 100L, 600L));
         if (local7253_14 < 0.999F) {
            UiTheme.method2882("assets/deluxe/textures/gui/mainmenu/background/welcome.png", local7253_5, local7253_6, 1.0F - local7253_14);
         }
      }

      float local7253_14 = UiTheme.method2875(
         UiTheme.method2885(this.field14614, this.field14613 ? 150L : 0L, this.field14613 ? 500L : 350L)
      );
      float local7253_15 = -330.0F * local7253_7 * (1.0F - local7253_14);
      float local7253_16 = 329.5F * local7253_7 + Math.min(0.0F, local7253_15);
      if (local7253_16 > 1.0F) {
         Render2D.method4678(drawContext, Math.max(0.0F, local7253_15), 0.0F, local7253_16, local7253_6);
         UiTheme.method2882("assets/deluxe/textures/gui/mainmenu/background/menu_blur.png", local7253_5, local7253_6, 1.0F);
         Render2D.method4661(drawContext);
      }

      Render2D.method4715(
         local7253_15, 0.0F, 329.5F * local7253_7, local7253_6, 0.0F, UiTheme.method2877(UiTheme.field09680, 0.012F)
      );
      Render2D.method4715(
         local7253_15 + 329.5F * local7253_7 - 0.5F * local7253_7,
         0.0F,
         0.5F * local7253_7,
         local7253_6,
         0.0F,
         UiTheme.method2877(UiTheme.field09680, 0.03F)
      );
      Render2D.method4705(local7253_14);
      this.drawChrome(local7253_15, local7253_6, local7253_7, local7253_9, local7253_10);
      if (local7253_13 < 0.999F) {
         Render2D.method4705(1.0F - local7253_13);
         this.drawMenuColumn(local7253_15 - 36.0F * local7253_7 * local7253_13, local7253_7, local7253_8, local7253_9, local7253_10, local7253_13);
         Render2D.method4650();
      }

      if (local7253_11 > 0.001F) {
         Render2D.method4705(local7253_11);
         this.accountManagerPanel
            .method3234(drawContext, local7253_15 + 36.0F * local7253_7 * (1.0F - local7253_11), local7253_7, local7253_8, local7253_9, local7253_10);
         Render2D.method4650();
      }

      if (local7253_12 > 0.001F) {
         Render2D.method4705(local7253_12);
         this.cosmeticsSelectionScreen
            .method4626(
               drawContext,
               local7253_15 + 36.0F * local7253_7 * (1.0F - local7253_12),
               local7253_7,
               local7253_8,
               local7253_5,
               local7253_6,
               local7253_9,
               local7253_10
            );
         Render2D.method4650();
      }

      Render2D.method4650();
      Render2D.method4754();
      if (local7253_11 > 0.5F) {
         this.accountManagerPanel.method3261(drawContext, local7253_15 + 36.0F * local7253_7 * (1.0F - local7253_11), local7253_7, local7253_8);
      }
   }

   @Override
   public boolean keyPressed(KeyInput keyInput) {
      if (this.field14607) {
         if (this.accountManagerPanel.method3248(keyInput.key())) {
            return true;
         }

         if (keyInput.key() == 256) {
            this.toggleAlt();
            return true;
         }
      }

      if (this.field14608) {
         if (this.cosmeticsSelectionScreen.method4601(keyInput.key())) {
            return true;
         }

         if (keyInput.key() == 256) {
            this.toggleCosmetics();
            return true;
         }
      }

      return super.keyPressed(keyInput);
   }

   @Override
   public boolean charTyped(CharInput charInput) {
      if (this.field14607 && this.accountManagerPanel.method3260(charInput.codepoint())) {
         return true;
      } else {
         return this.field14608 && this.cosmeticsSelectionScreen.method4625(charInput.codepoint()) ? true : super.charTyped(charInput);
      }
   }

   private float hoverPC(String local7260_1, boolean local7260_2) {
      AnimatedValue animatedValue = this.field14611.computeIfAbsent(local7260_1, local7254_0 -> new AnimatedValue(150L));
      animatedValue.method3869(local7260_2 ? 1.0 : 0.0);
      return (float)animatedValue.method3871();
   }

   private float slideInPC(int local7261_1) {
      return UiTheme.method2875(UiTheme.method2885(this.field14614, (this.field14613 ? 300 : 80) + local7261_1 * 70L, 450L));
   }

   private void toggleAlt() {
      this.field14607 = !this.field14607;
      if (this.field14607) {
         this.accountManagerPanel.method3251(this::toggleAlt);
      } else {
         this.accountManagerPanel.method3244();
      }
   }

   private void toggleCosmetics() {
      this.field14608 = !this.field14608;
      if (this.field14608) {
         this.cosmeticsSelectionScreen.method4602(this::toggleCosmetics);
      } else {
         this.cosmeticsSelectionScreen.method4628();
      }
   }

   private static float spaceFactor() {
      return GuiScale.method0797() / GuiScale.method0794();
   }

   private record Nested0524(float x, float y, float w, float h, Runnable action) {
   }
}
