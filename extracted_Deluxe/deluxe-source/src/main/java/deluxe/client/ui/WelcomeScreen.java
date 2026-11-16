package deluxe.client.ui;

import deluxe.client.storage.AccountEntry;
import deluxe.client.render.AccountPreviewRenderer;
import deluxe.client.render.FontFace;
import deluxe.client.module.render.InterfaceModule;
import ru.dreamix.protection.api.impl.profile.ProfileAPI;
import deluxe.client.util.GuiScale;
import deluxe.client.render.FontText;
import deluxe.client.ui.UiTheme;
import deluxe.client.ui.AnimatedValue;
import deluxe.client.render.Render2D;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public class WelcomeScreen extends Screen {
   private final long field10932 = System.currentTimeMillis();
   private long field10933;
   private long field10934;
   private final AnimatedValue animatedValue = new AnimatedValue(160L);
   private float field10936;
   private float field10937;
   private float field10938;
   private float field10939;

   @Override
   public boolean mouseClicked(Click click, boolean local5201_2) {
      if (click.button() == 0) {
         long local5201_3 = System.currentTimeMillis();
         if (this.field10933 == 0L) {
            if (local5201_3 - this.field10932 > 400L) {
               this.field10933 = local5201_3;
            }

            return true;
         }

         float local5201_5 = spaceFactor();
         if (this.field10934 == 0L
            && UiTheme.method2879(
               (float)click.x() * local5201_5, (float)click.y() * local5201_5, this.field10936, this.field10937, this.field10938, this.field10939
            )) {
            this.field10934 = local5201_3;
            return true;
         }
      }

      return super.mouseClicked(click, local5201_2);
   }

   @Override
   public boolean shouldCloseOnEsc() {
      return false;
   }

   @Override
   public boolean shouldPause() {
      return false;
   }

   @Override
   public void renderBackground(DrawContext drawContext, int local5204_2, int local5204_3, float local5204_4) {
   }

   private static float spaceFactor() {
      return GuiScale.method0797() / GuiScale.method0794();
   }

   private void drawSessionChip(float local5206_1, float local5206_2, float local5206_3) {
      MinecraftClient minecraftClient = this.client != null ? this.client : MinecraftClient.getInstance();
      String local5206_5 = minecraftClient != null && minecraftClient.getSession() != null ? minecraftClient.getSession().getUsername() : "Player";
      String local5206_6 = AccountPreviewRenderer.method0456(local5206_5, AccountEntry.method3449(local5206_5)).body().texturePath().toString();
      Render2D.method4677(
         local5206_6,
         local5206_1,
         local5206_2,
         15.0F * local5206_3,
         15.0F * local5206_3,
         5.0F * local5206_3,
         1.0F,
         0.125F,
         0.125F,
         0.25F,
         0.25F,
         UiTheme.field09680
      );
      Render2D.method4677(
         local5206_6,
         local5206_1,
         local5206_2,
         15.0F * local5206_3,
         15.0F * local5206_3,
         5.0F * local5206_3,
         1.0F,
         0.625F,
         0.125F,
         0.75F,
         0.25F,
         UiTheme.field09680
      );
      UiTheme.method2876(
         "assets/deluxe/textures/gui/mainmenu/icons/person.png",
         local5206_1 + 22.2F * local5206_3,
         local5206_2 + 3.2F * local5206_3,
         3.4F * local5206_3,
         4.5F * local5206_3,
         UiTheme.field09708
      );
      FontText.method1397(
         FontFace.SEMI,
         "Активная сессия",
         local5206_1 + 26.5F * local5206_3,
         local5206_2 + 0.5F * local5206_3,
         5.0F * local5206_3,
         UiTheme.field09708
      );
      FontText.method1397(
         FontFace.SEMI_BOLD,
         ProfileAPI.get() != null
               && ProfileAPI.get().getModel() != null
               && ProfileAPI.get().getModel().getName() != null
               && !ProfileAPI.get().getModel().getName().isBlank()
            ? ProfileAPI.get().getModel().getName()
            : local5206_5,
         local5206_1 + 20.0F * local5206_3,
         local5206_2 + 7.5F * local5206_3,
         6.5F * local5206_3,
         UiTheme.field09680
      );
   }

   @Override
   public void render(DrawContext drawContext, int local5207_2, int local5207_3, float local5207_4) {
      InterfaceModule.method2247().method2250();
      float local5207_5 = this.width * spaceFactor();
      float local5207_6 = this.height * spaceFactor();
      float local5207_7 = Math.min(local5207_5 / 960.0F, local5207_6 / 540.0F);
      float local5207_8 = (local5207_6 - 540.0F * local5207_7) / 2.0F;
      long local5207_9 = System.currentTimeMillis();
      UiTheme.field09741 = true;
      Render2D.method4713(drawContext);
      if (this.field10933 == 0L && local5207_9 - this.field10932 >= 2700L) {
         this.field10933 = local5207_9;
      }

      float local5207_11 = this.field10933 == 0L ? 0.0F : UiTheme.method2875(UiTheme.method2885(this.field10933, 0L, 450L));
      UiTheme.method2882("assets/deluxe/textures/gui/mainmenu/background/loading.png", local5207_5, local5207_6, 1.0F);
      if (local5207_11 > 0.001F) {
         UiTheme.method2882("assets/deluxe/textures/gui/mainmenu/background/welcome.png", local5207_5, local5207_6, local5207_11);
      }

      if (local5207_11 < 0.999F) {
         Render2D.method4705(1.0F - local5207_11);
         UiTheme.method2878(this.field10932, local5207_5, local5207_6);
         Render2D.method4650();
      }

      if (local5207_11 > 0.001F) {
         float local5207_12 = this.field10934 == 0L ? 0.0F : UiTheme.method2875(UiTheme.method2885(this.field10934, 0L, 320L));
         Render2D.method4705(local5207_11 * (1.0F - local5207_12));
         this.drawWelcomePhase(
            local5207_6, local5207_7, local5207_8, local5207_2 * spaceFactor(), local5207_3 * spaceFactor(), local5207_12 * 26.0F * local5207_7
         );
         Render2D.method4650();
      }

      Render2D.method4754();
   }

   @Override
   public void tick() {
      if (this.field10934 != 0L && System.currentTimeMillis() - this.field10934 >= 340L) {
         MinecraftClient minecraftClient = this.client != null ? this.client : MinecraftClient.getInstance();
         minecraftClient.setScreen(new MainMenuScreen(true));
      }
   }

   public WelcomeScreen() {
      super(Text.literal("Deluxe"));
   }

   private void drawWelcomePhase(float local5210_1, float local5210_2, float local5210_3, float local5210_4, float local5210_5, float local5210_6) {
      long local5210_7 = this.field10933;
      float local5210_9 = 51.5F * local5210_2 - local5210_6;
      float local5210_10 = UiTheme.method2875(UiTheme.method2885(local5210_7, 0L, 420L));
      Render2D.method4705(local5210_10);
      Render2D.method4777(
         "assets/deluxe/textures/gui/mainmenu/icons/logo.png",
         local5210_9 - (1.0F - local5210_10) * 14.0F * local5210_2,
         40.0F * local5210_2,
         13.5F * local5210_2,
         10.5F * local5210_2,
         0.0F,
         UiTheme.field09686
      );
      Render2D.method4650();
      float local5210_11 = UiTheme.method2875(UiTheme.method2885(local5210_7, 90L, 460L));
      Render2D.method4705(local5210_11);
      float local5210_12 = local5210_9 - (1.0F - local5210_11) * 18.0F * local5210_2;
      FontText.method1397(
         FontFace.SEMI, "Добро пожаловать в", local5210_12, local5210_3 + 209.0F * local5210_2, 20.0F * local5210_2, UiTheme.field09680
      );
      Render2D.method4777(
         "assets/deluxe/textures/gui/mainmenu/icons/deluxe_serif.png",
         local5210_12,
         local5210_3 + 236.0F * local5210_2,
         68.04957F * local5210_2,
         15.4F * local5210_2,
         0.0F,
         UiTheme.field09686
      );
      Render2D.method4650();
      float local5210_13 = UiTheme.method2875(UiTheme.method2885(local5210_7, 170L, 460L));
      Render2D.method4705(local5210_13);
      float local5210_14 = local5210_3 + 268.5F * local5210_2;

      for (String local5210_16 : UiTheme.method2873(
         FontFace.SEMI,
         "Всё готово для комфортной игры. Настройте клиент под себя, используйте любимые функции и откройте больше возможностей вместе с Deluxe.",
         7.5F * local5210_2,
         246.0F * local5210_2
      )) {
         FontText.method1397(
            FontFace.SEMI,
            local5210_16,
            local5210_9 - (1.0F - local5210_13) * 18.0F * local5210_2,
            local5210_14,
            7.5F * local5210_2,
            UiTheme.field09708
         );
         local5210_14 += 9.0F * local5210_2;
      }

      Render2D.method4650();
      float local5210_15 = UiTheme.method2875(UiTheme.method2885(local5210_7, 250L, 460L));
      Render2D.method4705(local5210_15);
      this.field10936 = local5210_9 - (1.0F - local5210_15) * 18.0F * local5210_2;
      this.field10937 = local5210_3 + 311.0F * local5210_2;
      this.field10938 = 231.0F * local5210_2;
      this.field10939 = 19.5F * local5210_2;
      this.animatedValue
         .method3869(
            this.field10934 == 0L
                  && UiTheme.method2879(local5210_4, local5210_5, this.field10936, this.field10937, this.field10938, this.field10939)
               ? 1.0
               : 0.0
         );
      float local5210_16 = (float)this.animatedValue.method3871();
      Render2D.method4715(
         this.field10936,
         this.field10937,
         this.field10938,
         this.field10939,
         6.0F * local5210_2,
         UiTheme.method2881(UiTheme.field09686, UiTheme.field09687, local5210_16 * 0.65F)
      );
      Render2D.method4769(this.field10936, this.field10937, this.field10938, this.field10939, 6.0F * local5210_2, 0.5F, UiTheme.field09713);
      UiTheme.method2876(
         "assets/deluxe/textures/gui/mainmenu/icons/arrow.png",
         this.field10936 + 9.5F * local5210_2 + local5210_16 * 1.5F * local5210_2,
         this.field10937 + this.field10939 / 2.0F,
         6.0F * local5210_2,
         6.0F * local5210_2,
         UiTheme.field09689
      );
      FontText.method1397(
         FontFace.SEMI,
         "Перейти в главное меню",
         this.field10936 + 19.5F * local5210_2,
         this.field10937 + this.field10939 / 2.0F - 3.5F * local5210_2,
         7.0F * local5210_2,
         UiTheme.field09689
      );
      Render2D.method4650();
      float local5210_17 = UiTheme.method2875(UiTheme.method2885(local5210_7, 330L, 460L));
      Render2D.method4705(local5210_17);
      this.drawSessionChip(local5210_9, local5210_1 - 51.0F * local5210_2 + (1.0F - local5210_17) * 10.0F * local5210_2, local5210_2);
      Render2D.method4650();
   }
}
