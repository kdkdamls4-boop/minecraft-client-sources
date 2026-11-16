package deluxe.client.ui.panel;

import deluxe.client.ui.AnimatedValue;
import deluxe.client.render.FontText;
import deluxe.client.util.GuiScale;
import deluxe.client.util.NicknameGenerator;
import deluxe.client.ui.UiTheme;

import deluxe.client.storage.AccountEntry;
import deluxe.client.render.AccountPreviewRenderer;
import deluxe.client.storage.AccountStore;
import deluxe.client.render.FontFace;
import deluxe.client.render.Render2D;

import java.util.ArrayList;
import java.util.HashMap;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import org.lwjgl.glfw.GLFW;

public final class AccountManagerPanel {
   private AccountStore accountStore = AccountStore.method4153();
   private AccountManagerPanel.Nested0482 field11128 = AccountManagerPanel.Nested0482.NONE;
   private String field11129 = "";
   private String field11130 = "";
   private boolean field11131;
   private NicknameGenerator nicknameGenerator = new NicknameGenerator();
   private HashMap<String, AnimatedValue> field11133 = new HashMap<>();
   private float field11134;
   private float field11135;
   private float field11136;
   private long field11137 = System.nanoTime() / 1000000L;
   private long field11138;
   private long __;
   private String field11139;
   private Runnable field11140 = () -> {};
   private float field11141;
   private float field11142;
   private float field11143 = 1.0F;
   private ArrayList<AccountManagerPanel.Nested0483> field11144 = new ArrayList<>();
   private ArrayList<AccountEntry> field11145 = new ArrayList<>();

   public void method3234(DrawContext drawContext, float local5260_2, float local5260_3, float local5260_4, float local5260_5, float local5260_6) {
      this.field11143 = local5260_3;
      float local5260_7 = Math.min(0.05F, (float)(System.nanoTime() / 1000000L - this.field11137) / 1000.0F);
      this.field11137 = System.nanoTime() / 1000000L;
      this.field11144.clear();
      AccountEntry accountEntry = this.accountStore.getAccountEntry();
      String local5260_9 = accountEntry == null ? null : accountEntry.method3455();
      if (local5260_9 != null && !local5260_9.equals(this.field11139)) {
         this.field11139 = local5260_9;
         this.__ = System.currentTimeMillis();
      }

      float local5260_10 = local5260_2 + 51.5F * local5260_3;
      this.method3253(local5260_10, local5260_4, local5260_5, local5260_6);
      this.method3254(local5260_10, local5260_4);
      this.method3239(drawContext, local5260_10, local5260_4, local5260_5, local5260_6, local5260_7);
      this.method3252(local5260_10, local5260_4, local5260_5, local5260_6, accountEntry);
      this.method3238(local5260_10, local5260_4, local5260_5, local5260_6);
   }

   private float method3237(int local5263_1) {
      return UiTheme.method2875(UiTheme.method2885(this.field11138, 40L + local5263_1 * 55L, 420L));
   }

   private void method3238(float local5264_1, float local5264_2, float local5264_3, float local5264_4) {
      float local5264_5 = this.method3237(4);
      Render2D.method4705(local5264_5);
      float local5264_6 = local5264_1 - (1.0F - local5264_5) * 14.0F * this.field11143;
      float local5264_7 = local5264_2 + 411.5F * this.field11143;
      float local5264_8 = this.method3241(
         "altGenerate", UiTheme.method2879(local5264_3, local5264_4, local5264_6, local5264_7, 109.5F * this.field11143, 19.5F * this.field11143)
      );
      Render2D.method4715(
         local5264_6,
         local5264_7,
         109.5F * this.field11143,
         19.5F * this.field11143,
         6.0F * this.field11143,
         UiTheme.method2877(UiTheme.field09680, 0.031F + 0.05F * local5264_8)
      );
      Render2D.method4769(
         local5264_6,
         local5264_7,
         109.5F * this.field11143,
         19.5F * this.field11143,
         6.0F * this.field11143,
         0.5F,
         UiTheme.method2877(UiTheme.field09680, 0.078F + 0.09F * local5264_8)
      );
      UiTheme.method2876(
         "assets/deluxe/textures/gui/mainmenu/icons/dice.png",
         local5264_6 + 13.75F * this.field11143,
         local5264_7 + 9.75F * this.field11143,
         7.5F * this.field11143,
         7.5F * this.field11143,
         UiTheme.method2881(UiTheme.field09707, UiTheme.field09680, local5264_8)
      );
      FontText.method1397(
         FontFace.SEMI,
         "Сгенерировать аккаунт",
         local5264_6 + 21.5F * this.field11143,
         local5264_7 + 6.25F * this.field11143,
         7.0F * this.field11143,
         UiTheme.method2881(UiTheme.field09707, UiTheme.field09680, local5264_8)
      );
      this.field11144
         .add(
            new AccountManagerPanel.Nested0483(
               local5264_6,
               local5264_7,
               109.5F * this.field11143,
               19.5F * this.field11143,
               false,
               () -> this.accountStore.method4156(this.accountStore.method4145(this.nicknameGenerator.method2627()))
            )
         );
      float local5264_9 = local5264_6 + 117.0F * this.field11143;
      float local5264_10 = this.method3241(
         "altCreate", UiTheme.method2879(local5264_3, local5264_4, local5264_9, local5264_7, 109.5F * this.field11143, 19.5F * this.field11143)
      );
      float local5264_11 = this.method3241("altCreateActive", this.field11128 == AccountManagerPanel.Nested0482.CREATE);
      Render2D.method4715(
         local5264_9,
         local5264_7,
         109.5F * this.field11143,
         19.5F * this.field11143,
         6.0F * this.field11143,
         UiTheme.method2881(
            UiTheme.method2877(UiTheme.field09680, 0.031F + 0.05F * local5264_10),
            UiTheme.method2877(UiTheme.field09686, 0.85F),
            local5264_11
         )
      );
      Render2D.method4769(
         local5264_9,
         local5264_7,
         109.5F * this.field11143,
         19.5F * this.field11143,
         6.0F * this.field11143,
         0.5F,
         UiTheme.method2881(
            UiTheme.method2877(UiTheme.field09680, 0.078F + 0.09F * local5264_10), UiTheme.field09713, local5264_11
         )
      );
      UiTheme.method2876(
         "assets/deluxe/textures/gui/mainmenu/icons/plus.png",
         local5264_9 + 25.75F * this.field11143,
         local5264_7 + 9.75F * this.field11143,
         6.5F * this.field11143,
         6.5F * this.field11143,
         UiTheme.method2881(UiTheme.field09707, UiTheme.field09680, Math.max(local5264_10, local5264_11))
      );
      FontText.method1397(
         FontFace.SEMI,
         this.field11128 == AccountManagerPanel.Nested0482.CREATE ? "Подтвердить" : "Создать аккаунт",
         local5264_9 + 32.5F * this.field11143,
         local5264_7 + 6.25F * this.field11143,
         7.0F * this.field11143,
         UiTheme.method2881(UiTheme.field09707, UiTheme.field09680, Math.max(local5264_10, local5264_11))
      );
      this.field11144.add(new AccountManagerPanel.Nested0483(local5264_9, local5264_7, 109.5F * this.field11143, 19.5F * this.field11143, false, () -> {
         if (this.field11128 != AccountManagerPanel.Nested0482.CREATE) {
            this.field11128 = AccountManagerPanel.Nested0482.CREATE;
            this.field11130 = "";
            this.field11131 = false;
         } else {
            this.method3255();
         }
      }));
      Render2D.method4650();
   }

   private void method3239(DrawContext drawContext, float local5265_2, float local5265_3, float local5265_4, float local5265_5, float local5265_6) {
      float local5265_7 = this.method3237(2);
      Render2D.method4705(local5265_7);
      float local5265_8 = local5265_2 - (1.0F - local5265_7) * 14.0F * this.field11143;
      this.field11141 = local5265_3 + 223.5F * this.field11143;
      this.field11142 = this.field11141 + 138.5F * this.field11143;
      ArrayList<AccountEntry> local5265_9 = this.accountStore.method4155(this.field11129);
      this.field11145 = local5265_9;
      this.field11136 = Math.max(0.0F, local5265_9.size() * 22.5F - 4.5F - 138.5F);
      this.field11134 = method3250(this.field11134, 0.0F, this.field11136);
      this.field11135 = method3246(this.field11135, this.field11134, local5265_6);
      Render2D.method4678(drawContext, local5265_8, this.field11141, 227.0F * this.field11143, 138.5F * this.field11143);
      if (local5265_9.isEmpty()) {
         String local5265_10 = this.field11129.isEmpty() ? "Аккаунтов пока нет" : "Ничего не найдено";
         FontText.method1397(
            FontFace.SEMI,
            local5265_10,
            local5265_8 + (227.0F * this.field11143 - FontText.method1396(FontFace.SEMI, local5265_10, 7.0F * this.field11143)) / 2.0F,
            this.field11141 + 64.0F * this.field11143,
            7.0F * this.field11143,
            UiTheme.field09709
         );
      }

      float local5265_10 = this.field11141 - this.method3242(this.field11135) * this.field11143;
      int local5265_11 = 0;

      for (AccountEntry accountEntry : local5265_9) {
         if (local5265_10 + 16.5F * this.field11143 >= this.field11141 - 6.0F * this.field11143 && local5265_10 <= this.field11142 + 6.0F * this.field11143) {
            this.method3257(accountEntry, local5265_8, local5265_10, local5265_11, local5265_4, local5265_5);
         }

         local5265_10 += 22.5F * this.field11143;
         local5265_11++;
      }

      Render2D.method4661(drawContext);
      if (this.field11136 > 0.5F) {
         float local5265_12 = Math.max(16.619999F, 138.5F * (138.5F / (local5265_9.size() * 22.5F - 4.5F))) * this.field11143;
         Render2D.method4715(
            local5265_8 + 226.5F * this.field11143,
            this.field11141,
            0.5F * this.field11143,
            138.5F * this.field11143,
            0.25F * this.field11143,
            UiTheme.method2877(UiTheme.field09680, 0.05F)
         );
         Render2D.method4715(
            local5265_8 + 226.5F * this.field11143,
            this.field11141 + this.field11135 / this.field11136 * (138.5F * this.field11143 - local5265_12),
            0.5F * this.field11143,
            local5265_12,
            0.25F * this.field11143,
            UiTheme.field09710
         );
      }

      Render2D.method4650();
   }

   private float method3241(String local5267_1, boolean local5267_2) {
      AnimatedValue animatedValue = this.field11133.computeIfAbsent(local5267_1, local5283_0 -> new AnimatedValue(140L));
      animatedValue.method3869(local5267_2 ? 1.0 : 0.0);
      return (float)animatedValue.method3871();
   }

   private float method3242(float local5268_1) {
      float local5268_2 = this.field11143 * GuiScale.method0794() / GuiScale.method0797();
      return local5268_2 > 1.0E-4F ? Math.round(local5268_1 * local5268_2) / local5268_2 : local5268_1;
   }

   public boolean method3243(float local5269_1, float local5269_2) {
      boolean local5269_3 = this.field11128 != AccountManagerPanel.Nested0482.NONE;
      this.field11128 = this.field11128 == AccountManagerPanel.Nested0482.SEARCH ? AccountManagerPanel.Nested0482.NONE : this.field11128;

      for (int index = this.field11144.size() - 1; index >= 0; index--) {
         AccountManagerPanel.Nested0483 local5269_5 = this.field11144.get(index);
         if ((!local5269_5.clipped() || !(local5269_2 < this.field11141) && !(local5269_2 > this.field11142))
            && UiTheme.method2879(local5269_1, local5269_2, local5269_5.x(), local5269_5.y(), local5269_5.w(), local5269_5.h())) {
            local5269_5.action().run();
            return true;
         }
      }

      if (local5269_3) {
         this.field11128 = AccountManagerPanel.Nested0482.NONE;
         this.field11131 = false;
         return true;
      } else {
         return false;
      }
   }

   public void method3244() {
      this.field11128 = AccountManagerPanel.Nested0482.NONE;
      this.field11131 = false;
   }

   public boolean method3245(float local5271_1, float local5271_2, float local5271_3) {
      if (local5271_2 >= this.field11141 - 30.0F * this.field11143 && local5271_2 <= this.field11142 + 10.0F * this.field11143) {
         this.field11134 = method3250(this.field11134 - local5271_3 * 22.5F, 0.0F, this.field11136);
         return true;
      } else {
         return false;
      }
   }

   private static float method3246(float local5272_0, float local5272_1, float local5272_2) {
      local5272_0 += (local5272_1 - local5272_0) * (1.0F - (float)Math.exp(-local5272_2 * 22.0F));
      return Math.abs(local5272_1 - local5272_0) < 0.1F ? local5272_1 : local5272_0;
   }

   public boolean method3248(int local5274_1) {
      if (this.field11128 == AccountManagerPanel.Nested0482.NONE) {
         return false;
      }

      boolean local5274_2 = this.field11128 == AccountManagerPanel.Nested0482.SEARCH;
      if (GLFW.glfwGetKey(MinecraftClient.getInstance().getWindow().getHandle(), 341) == 1
         || GLFW.glfwGetKey(MinecraftClient.getInstance().getWindow().getHandle(), 345) == 1) {
         if (local5274_1 == 65) {
            this.field11131 = true;
            return true;
         }

         if (local5274_1 == 67) {
            String var10001 = local5274_2 ? this.field11129 : this.field11130;
            MinecraftClient.getInstance().keyboard.setClipboard(var10001);
            return true;
         }

         if (local5274_1 == 86) {
            String local5274_3 = MinecraftClient.getInstance().keyboard.getClipboard().replaceAll("[\\r\\n\\t]", "");
            if (this.field11131) {
               if (local5274_2) {
                  this.field11129 = "";
               } else {
                  this.field11130 = "";
               }

               this.field11131 = false;
            }

            if (local5274_2) {
               this.field11129 = (this.field11129 + local5274_3).length() > 24
                  ? (this.field11129 + local5274_3).substring(0, 24)
                  : this.field11129 + local5274_3;
            } else {
               this.field11130 = (this.field11130 + local5274_3).length() > 16
                  ? (this.field11130 + local5274_3).substring(0, 16)
                  : this.field11130 + local5274_3;
            }

            return true;
         }
      }

      if (local5274_1 == 259) {
         if (this.field11131) {
            if (local5274_2) {
               this.field11129 = "";
            } else {
               this.field11130 = "";
            }

            this.field11131 = false;
            return true;
         } else {
            if (local5274_2 && !this.field11129.isEmpty()) {
               this.field11129 = this.field11129.substring(0, this.field11129.length() - 1);
            } else if (!local5274_2 && !this.field11130.isEmpty()) {
               this.field11130 = this.field11130.substring(0, this.field11130.length() - 1);
            }

            return true;
         }
      } else if (local5274_1 != 257 && local5274_1 != 335) {
         if (local5274_1 == 256) {
            this.field11128 = AccountManagerPanel.Nested0482.NONE;
            this.field11131 = false;
            return true;
         } else {
            return false;
         }
      } else {
         if (local5274_2) {
            this.field11128 = AccountManagerPanel.Nested0482.NONE;
            this.field11131 = false;
         } else {
            this.method3255();
         }

         return true;
      }
   }

   private static float method3250(float local5276_0, float local5276_1, float local5276_2) {
      return Math.max(local5276_1, Math.min(local5276_2, local5276_0));
   }

   public void method3251(Runnable runnable) {
      this.field11138 = System.currentTimeMillis();
      this.field11140 = runnable;
      this.field11128 = AccountManagerPanel.Nested0482.NONE;
      this.field11131 = false;
      this.field11130 = "";
      this.__ = 0L;
      this.field11139 = this.accountStore.getAccountEntry() == null ? null : this.accountStore.getAccountEntry().method3455();
   }

   private void method3252(float local5279_1, float local5279_2, float local5279_3, float local5279_4, AccountEntry accountEntry) {
      float local5279_6 = this.method3237(3);
      Render2D.method4705(local5279_6);
      float local5279_7 = local5279_1 - (1.0F - local5279_6) * 14.0F * this.field11143;
      float local5279_8 = local5279_2 + 372.5F * this.field11143;
      if (accountEntry == null) {
         Render2D.method4715(
            local5279_7,
            local5279_8,
            226.5F * this.field11143,
            32.0F * this.field11143,
            6.0F * this.field11143,
            UiTheme.method2877(UiTheme.field09680, 0.031F)
         );
         Render2D.method4769(
            local5279_7, local5279_8, 226.5F * this.field11143, 32.0F * this.field11143, 6.0F * this.field11143, 0.5F, UiTheme.field09712
         );
         String local5279_9 = "Выберите аккаунт из списка";
         FontText.method1397(
            FontFace.SEMI,
            local5279_9,
            local5279_7 + (226.5F * this.field11143 - FontText.method1396(FontFace.SEMI, local5279_9, 7.0F * this.field11143)) / 2.0F,
            local5279_8 + 12.5F * this.field11143,
            7.0F * this.field11143,
            UiTheme.field09709
         );
         Render2D.method4650();
      } else {
         float local5279_9 = this.__ == 0L ? 1.0F : UiTheme.method2875(UiTheme.method2885(this.__, 0L, 340L));
         Render2D.method4715(
            local5279_7, local5279_8, 226.5F * this.field11143, 32.0F * this.field11143, 6.0F * this.field11143, UiTheme.field09686
         );
         Render2D.method4769(
            local5279_7, local5279_8, 226.5F * this.field11143, 32.0F * this.field11143, 6.0F * this.field11143, 0.5F, UiTheme.field09713
         );
         Render2D.method4705(local5279_9);
         float local5279_10 = (1.0F - local5279_9) * 4.0F * this.field11143;
         UiTheme.method2876(
            "assets/deluxe/textures/gui/mainmenu/icons/user.png",
            local5279_7 + 39.75F * this.field11143,
            local5279_8 + 10.0F * this.field11143 + local5279_10,
            5.5F * this.field11143,
            5.5F * this.field11143,
            UiTheme.method2877(UiTheme.field09689, 0.7F)
         );
         FontText.method1397(
            FontFace.SEMI,
            "Выбранный аккаунт",
            local5279_7 + 44.0F * this.field11143,
            local5279_8 + 7.0F * this.field11143 + local5279_10,
            6.0F * this.field11143,
            UiTheme.method2877(UiTheme.field09689, 0.7F)
         );
         FontText.method1397(
            FontFace.SEMI_BOLD,
            accountEntry.method3455(),
            local5279_7 + 38.0F * this.field11143,
            local5279_8 + 16.0F * this.field11143 + local5279_10,
            7.0F * this.field11143,
            UiTheme.field09689
         );
         Render2D.method4650();
         float local5279_11 = this.method3241(
            "altRename",
            UiTheme.method2879(
               local5279_3,
               local5279_4,
               local5279_7 + 185.0F * this.field11143,
               local5279_8 + 10.5F * this.field11143,
               11.0F * this.field11143,
               11.0F * this.field11143
            )
         );
         UiTheme.method2876(
            "assets/deluxe/textures/gui/mainmenu/icons/rename.png",
            local5279_7 + 190.5F * this.field11143,
            local5279_8 + 16.0F * this.field11143,
            7.0F * this.field11143,
            7.0F * this.field11143,
            UiTheme.method2877(UiTheme.field09689, 0.72F + 0.28F * local5279_11)
         );
         this.field11144
            .add(
               new AccountManagerPanel.Nested0483(
                  local5279_7 + 185.0F * this.field11143,
                  local5279_8 + 10.5F * this.field11143,
                  11.0F * this.field11143,
                  11.0F * this.field11143,
                  false,
                  () -> {
                     this.field11128 = AccountManagerPanel.Nested0482.RENAME;
                     this.field11130 = accountEntry.method3455();
                     this.field11131 = true;
                  }
               )
            );
         float local5279_12 = this.method3241(
            "altDelete",
            UiTheme.method2879(
               local5279_3,
               local5279_4,
               local5279_7 + 203.0F * this.field11143,
               local5279_8 + 10.5F * this.field11143,
               11.0F * this.field11143,
               11.0F * this.field11143
            )
         );
         UiTheme.method2876(
            "assets/deluxe/textures/gui/mainmenu/icons/delete.png",
            local5279_7 + 208.75F * this.field11143,
            local5279_8 + 16.0F * this.field11143,
            7.5F * this.field11143,
            7.0F * this.field11143,
            UiTheme.method2881(UiTheme.method2877(UiTheme.field09689, 0.72F), UiTheme.field09714, local5279_12)
         );
         this.field11144
            .add(
               new AccountManagerPanel.Nested0483(
                  local5279_7 + 203.0F * this.field11143,
                  local5279_8 + 10.5F * this.field11143,
                  11.0F * this.field11143,
                  11.0F * this.field11143,
                  false,
                  () -> this.accountStore.method4142(accountEntry)
               )
            );
         Render2D.method4650();
      }
   }

   private void method3253(float local5280_1, float local5280_2, float local5280_3, float local5280_4) {
      float local5280_5 = this.method3237(0);
      Render2D.method4705(local5280_5);
      float local5280_6 = local5280_1 - (1.0F - local5280_5) * 14.0F * this.field11143;
      float local5280_7 = this.method3241(
         "altBack",
         UiTheme.method2879(
            local5280_3,
            local5280_4,
            local5280_6 - 4.0F * this.field11143,
            local5280_2 + 105.0F * this.field11143,
            18.0F * this.field11143,
            18.0F * this.field11143
         )
      );
      UiTheme.method2876(
         "assets/deluxe/textures/gui/mainmenu/icons/back.png",
         local5280_6 + 4.75F * this.field11143 - local5280_7 * 1.5F * this.field11143,
         local5280_2 + 113.75F * this.field11143,
         9.5F * this.field11143,
         9.5F * this.field11143,
         UiTheme.method2881(UiTheme.field09708, UiTheme.field09680, local5280_7)
      );
      this.field11144
         .add(
            new AccountManagerPanel.Nested0483(
               local5280_6 - 4.0F * this.field11143,
               local5280_2 + 105.0F * this.field11143,
               18.0F * this.field11143,
               18.0F * this.field11143,
               false,
               this.field11140
            )
         );
      FontText.method1397(
         FontFace.SEMI,
         "Deluxe client  /  Главная  /  Аккаунты",
         local5280_6 + 15.5F * this.field11143,
         local5280_2 + 110.5F * this.field11143,
         6.5F * this.field11143,
         UiTheme.field09708
      );
      FontText.method1397(
         FontFace.SEMI,
         "Управление аккаунтами.",
         local5280_6,
         local5280_2 + 124.5F * this.field11143,
         20.0F * this.field11143,
         UiTheme.field09680
      );
      FontText.method1397(
         FontFace.SEMI,
         "Создавайте и выбирайте игровые профили",
         local5280_6,
         local5280_2 + 159.5F * this.field11143,
         7.5F * this.field11143,
         UiTheme.field09708
      );
      Render2D.method4650();
   }

   private void method3254(float local5281_1, float local5281_2) {
      float local5281_3 = this.method3237(1);
      Render2D.method4705(local5281_3);
      float local5281_4 = local5281_1 - (1.0F - local5281_3) * 14.0F * this.field11143;
      float local5281_5 = local5281_2 + 184.5F * this.field11143;
      boolean local5281_6 = this.field11128 != AccountManagerPanel.Nested0482.NONE;
      float local5281_7 = this.method3241("altFieldFocus", local5281_6);
      Render2D.method4715(
         local5281_4,
         local5281_5,
         226.5F * this.field11143,
         19.5F * this.field11143,
         6.0F * this.field11143,
         UiTheme.method2877(UiTheme.field09680, 0.031F)
      );
      Render2D.method4769(
         local5281_4,
         local5281_5,
         226.5F * this.field11143,
         19.5F * this.field11143,
         6.0F * this.field11143,
         0.5F,
         UiTheme.method2881(UiTheme.field09712, UiTheme.method2877(UiTheme.field09713, 0.8F), local5281_7)
      );
      UiTheme.method2876(
         this.field11128 == AccountManagerPanel.Nested0482.CREATE
            ? "assets/deluxe/textures/gui/mainmenu/icons/plus.png"
            : (
               this.field11128 == AccountManagerPanel.Nested0482.RENAME
                  ? "assets/deluxe/textures/gui/mainmenu/icons/rename.png"
                  : "assets/deluxe/textures/gui/mainmenu/icons/search.png"
            ),
         local5281_4 + 8.5F * this.field11143,
         local5281_5 + 9.75F * this.field11143,
         6.5F * this.field11143,
         6.5F * this.field11143,
         UiTheme.method2881(UiTheme.field09709, UiTheme.method2877(UiTheme.field09713, 0.95F), local5281_7)
      );
      String local5281_8 = this.field11128 != AccountManagerPanel.Nested0482.SEARCH && this.field11128 != AccountManagerPanel.Nested0482.NONE
         ? this.field11130
         : this.field11129;
      String local5281_9 = this.field11128 == AccountManagerPanel.Nested0482.CREATE
         ? "Введите ник нового аккаунта..."
         : (this.field11128 == AccountManagerPanel.Nested0482.RENAME ? "Новый ник аккаунта..." : "Найти аккаунт...");
      if (local5281_6 && this.field11131 && !local5281_8.isEmpty()) {
         Render2D.method4715(
            local5281_4 + 19.0F * this.field11143,
            local5281_5 + 5.5F * this.field11143,
            FontText.method1396(FontFace.SEMI, local5281_8, 7.0F * this.field11143) + 1.0F * this.field11143,
            9.0F * this.field11143,
            2.0F * this.field11143,
            UiTheme.method2877(UiTheme.field09686, 0.3F)
         );
      }

      FontText.method1397(
         FontFace.SEMI,
         (local5281_8.isEmpty() && !local5281_6 ? local5281_9 : local5281_8) + (local5281_6 && System.currentTimeMillis() % 1000L < 500L ? "|" : ""),
         local5281_4 + 19.0F * this.field11143,
         local5281_5 + 6.25F * this.field11143,
         7.0F * this.field11143,
         local5281_8.isEmpty() ? UiTheme.field09709 : UiTheme.field09680
      );
      if (local5281_8.isEmpty() && local5281_6) {
         FontText.method1397(
            FontFace.SEMI,
            local5281_9,
            local5281_4
               + 19.0F * this.field11143
               + FontText.method1396(FontFace.SEMI, "|", 7.0F * this.field11143)
               + 1.5F * this.field11143,
            local5281_5 + 6.25F * this.field11143,
            7.0F * this.field11143,
            UiTheme.method2877(UiTheme.field09680, 0.16F)
         );
      }

      this.field11144.add(new AccountManagerPanel.Nested0483(local5281_4, local5281_5, 226.5F * this.field11143, 19.5F * this.field11143, false, () -> {
         if (this.field11128 == AccountManagerPanel.Nested0482.NONE) {
            this.field11128 = AccountManagerPanel.Nested0482.SEARCH;
         }
      }));
      float local5281_10 = local5281_2 + 210.0F * this.field11143;
      FontText.method1397(FontFace.SEMI, "Ваши аккаунты", local5281_4, local5281_10, 6.0F * this.field11143, UiTheme.field09708);
      Render2D.method4715(
         local5281_4 + 51.5F * this.field11143,
         local5281_10 + 3.5F * this.field11143,
         175.0F * this.field11143,
         0.5F * this.field11143,
         0.25F * this.field11143,
         UiTheme.field09710
      );
      Render2D.method4650();
   }

   private void method3255() {
      if (this.field11128 == AccountManagerPanel.Nested0482.CREATE && !this.field11130.isBlank()) {
         this.accountStore.method4156(this.accountStore.method4145(this.field11130));
      }

      if (this.field11128 == AccountManagerPanel.Nested0482.RENAME && !this.field11130.isBlank()) {
         this.accountStore.method4137(this.accountStore.getAccountEntry(), this.field11130);
      }

      this.field11130 = "";
      this.field11131 = false;
      this.field11128 = AccountManagerPanel.Nested0482.NONE;
   }

   private void method3257(AccountEntry accountEntry, float local5284_2, float local5284_3, int local5284_4, float local5284_5, float local5284_6) {
      float local5284_7 = UiTheme.method2875(UiTheme.method2885(this.field11138, 200L + Math.min(local5284_4, 8) * 45L, 380L));
      float local5284_8 = UiTheme.method2875(Math.min(1.0F, (float)(System.currentTimeMillis() - accountEntry.method3446()) / 260.0F));
      Render2D.method4705(local5284_7 * local5284_8);
      float local5284_9 = local5284_2 + (1.0F - local5284_7) * 8.0F * this.field11143 + (1.0F - local5284_8) * 8.0F * this.field11143;
      float local5284_10 = this.method3241("altRowActive" + accountEntry.method3455(), this.accountStore.method4150(accountEntry));
      float local5284_11 = this.method3241(
         "altRow" + accountEntry.method3455(),
         local5284_10 < 0.5F
            && UiTheme.method2879(local5284_5, local5284_6, local5284_9, local5284_3, 218.5F * this.field11143, 16.5F * this.field11143)
            && local5284_6 >= this.field11141
            && local5284_6 <= this.field11142
      );
      Render2D.method4715(
         local5284_9,
         local5284_3,
         218.5F * this.field11143,
         16.5F * this.field11143,
         5.0F * this.field11143,
         UiTheme.method2881(
            UiTheme.method2877(UiTheme.field09680, 0.031F + 0.045F * local5284_11), UiTheme.field09686, local5284_10
         )
      );
      Render2D.method4769(
         local5284_9,
         local5284_3,
         218.5F * this.field11143,
         16.5F * this.field11143,
         5.0F * this.field11143,
         0.5F,
         UiTheme.method2881(
            UiTheme.method2877(UiTheme.field09680, 0.078F + 0.09F * local5284_11), UiTheme.field09713, local5284_10
         )
      );
      FontText.method1397(
         local5284_10 > 0.5F ? FontFace.SEMI_BOLD : FontFace.SEMI,
         accountEntry.method3455(),
         local5284_9 + 20.5F * this.field11143,
         local5284_3 + 5.25F * this.field11143,
         6.0F * this.field11143,
         UiTheme.method2881(
            UiTheme.method2881(UiTheme.field09707, UiTheme.field09680, local5284_11),
            UiTheme.field09680,
            local5284_10
         )
      );
      this.field11144
         .add(
            new AccountManagerPanel.Nested0483(
               local5284_9, local5284_3, 218.5F * this.field11143, 16.5F * this.field11143, true, () -> this.accountStore.method4156(accountEntry)
            )
         );
      Render2D.method4650();
   }

   public boolean method3260(int local5287_1) {
      if (local5287_1 >= 32 && local5287_1 <= 65535 && this.field11128 != AccountManagerPanel.Nested0482.NONE) {
         char local5287_2 = (char)local5287_1;
         if (this.field11128 == AccountManagerPanel.Nested0482.SEARCH) {
            if (this.field11131) {
               this.field11129 = "";
               this.field11131 = false;
            }

            if (this.field11129.length() < 24) {
               this.field11129 = this.field11129 + local5287_2;
            }
         } else {
            if (this.field11131) {
               this.field11130 = "";
               this.field11131 = false;
            }

            if (this.field11130.length() < 16) {
               this.field11130 = this.field11130 + local5287_2;
            }
         }

         return true;
      } else {
         return false;
      }
   }

   public void method3261(DrawContext drawContext, float local5288_2, float local5288_3, float local5288_4) {
      float local5288_5 = this.method3237(2);
      if (!(local5288_5 < 0.05F)) {
         float local5288_6 = GuiScale.method0797() / GuiScale.method0794();
         float local5288_7 = local5288_2 + 51.5F * local5288_3;
         float local5288_8 = local5288_7 - (1.0F - local5288_5) * 14.0F * local5288_3;
         float local5288_9 = this.field11141 - this.method3242(this.field11135) * local5288_3;

         for (AccountEntry accountEntry : this.field11145) {
            if (local5288_9 + 16.5F * local5288_3 >= this.field11141 - 2.0F * local5288_3 && local5288_9 <= this.field11142 + 2.0F * local5288_3) {
               float local5288_12 = Math.max(local5288_9 + 1.5F * local5288_3, this.field11141);
               float local5288_13 = Math.min(local5288_9 + 15.0F * local5288_3, this.field11142);
               if (local5288_13 - local5288_12 > 2.0F * local5288_3) {
                  drawContext.enableScissor(
                     GuiScale.method0798(local5288_8 + 3.5F * local5288_3),
                     GuiScale.method0798(local5288_12),
                     GuiScale.method0798(local5288_8 + 19.5F * local5288_3),
                     GuiScale.method0798(local5288_13)
                  );

                  try {
                     AccountPreviewRenderer.method0454(
                        drawContext,
                        accountEntry,
                        local5288_6,
                        local5288_8 + 11.5F * local5288_3,
                        local5288_9 + 16.5F * local5288_3,
                        19.0F * local5288_3 * local5288_5
                     );
                  } catch (RuntimeException exception) {
                  }

                  drawContext.disableScissor();
               }
            }

            local5288_9 += 22.5F * local5288_3;
         }

         AccountEntry accountEntry2 = this.accountStore.getAccountEntry();
         if (accountEntry2 != null) {
            float local5288_11 = this.method3237(3);
            if (!(local5288_11 < 0.05F)) {
               float local5288_12 = this.__ == 0L ? 1.0F : UiTheme.method2875(UiTheme.method2885(this.__, 0L, 340L));
               float local5288_13 = local5288_7 - (1.0F - local5288_11) * 14.0F * local5288_3;
               float local5288_14 = local5288_4 + 372.5F * local5288_3;
               float local5288_15 = 34.0F * local5288_3 * local5288_11 * (0.75F + 0.25F * local5288_12);
               drawContext.enableScissor(
                  GuiScale.method0798(local5288_13 + 4.5F * local5288_3),
                  GuiScale.method0798(local5288_14 + 1.0F * local5288_3),
                  GuiScale.method0798(local5288_13 + 33.5F * local5288_3),
                  GuiScale.method0798(local5288_14 + 31.5F * local5288_3)
               );

               try {
                  AccountPreviewRenderer.method0454(
                     drawContext,
                     accountEntry2,
                     local5288_6,
                     local5288_13 + 19.0F * local5288_3,
                     local5288_14 + 2.0F * local5288_3 + local5288_15 * 0.8F,
                     local5288_15
                  );
               } catch (RuntimeException exception2) {
               }

               drawContext.disableScissor();
            }
         }
      }
   }

   public enum Nested0482 {
      NONE,
      SEARCH,
      CREATE,
      RENAME;
   }

   private record Nested0483(float x, float y, float w, float h, boolean clipped, Runnable action) {
   }
}
