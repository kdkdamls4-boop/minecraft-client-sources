package deluxe.client.ui;

import deluxe.client.cosmetic.CosmeticCatalog;
import deluxe.client.cosmetic.CosmeticCategory;
import deluxe.client.cosmetic.CosmeticEntry;
import deluxe.client.cosmetic.CosmeticPreviewAtlas;
import deluxe.client.cosmetic.CosmeticStore;
import deluxe.client.render.FontFace;
import deluxe.client.render.FontText;
import deluxe.client.ui.UiTheme;
import deluxe.client.cosmetic.CosmeticAccess;
import deluxe.client.ui.AnimatedValue;
import deluxe.client.render.Render2D;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import org.lwjgl.glfw.GLFW;

public final class CosmeticsSelectionScreen {
   private final HashMap<String, AnimatedValue> field15652 = new HashMap<>();
   private final HashMap<Integer, AnimatedValue> field15653 = new HashMap<>();
   private final HashMap<Integer, AnimatedValue> field15654 = new HashMap<>();
   private final EnumMap<CosmeticCategory, AnimatedValue> field15655 = new EnumMap<>(CosmeticCategory.class);
   private final EnumMap<CosmeticCategory, AnimatedValue> field15656 = new EnumMap<>(CosmeticCategory.class);
   private final EnumMap<CosmeticCategory, List<CosmeticEntry>> field15657 = new EnumMap<>(CosmeticCategory.class);
   private final EnumMap<CosmeticCategory, String> field15658 = new EnumMap<>(CosmeticCategory.class);
   private final HashMap<Integer, CosmeticsSelectionScreen.Nested0536> field15659 = new HashMap<>();
   private final ArrayList<CosmeticsSelectionScreen.Nested0537> field15660 = new ArrayList<>();
   private final CosmeticsSelectionScreen.Nested0538 field15661 = new CosmeticsSelectionScreen.Nested0538();
   private final CosmeticsSelectionScreen.Nested0538 field15662 = new CosmeticsSelectionScreen.Nested0538();
   private final CosmeticsSelectionScreen.Nested0538 field15663 = new CosmeticsSelectionScreen.Nested0538();
   private String field15664;
   private int field15665 = -1;
   private float field15666;
   private boolean field15667;
   private String field15668 = "";
   private float field15669;
   private float field15670;
   private float field15671;
   private long field15672 = System.nanoTime() / 1000000L;
   private long field15673;
   private Runnable field15674 = () -> {};
   private float field15675;
   private float field15676;
   private float field15677;
   private float field15678;
   private float field15679;
   private float field15680 = 1.0F;
   private float field15681 = -1.0F;
   private float field15682;
   private float field15683;
   private CosmeticCategory field15684 = CosmeticCategory.HAT;
   private CosmeticCategory field15685 = CosmeticCategory.HAT;
   private long field15686;
   private CosmeticEntry field15687;
   private CosmeticEntry field15688;
   private boolean field15689;
   private float field15690;

   private float method4600(String local7761_1, boolean local7761_2) {
      AnimatedValue animatedValue = this.field15652.computeIfAbsent(local7761_1, local7766_0 -> new AnimatedValue(140L));
      animatedValue.method3869(local7761_2 ? 1.0 : 0.0);
      return (float)animatedValue.method3871();
   }

   public boolean method4601(int local7762_1) {
      if (!this.field15667) {
         return false;
      }

      if (GLFW.glfwGetKey(MinecraftClient.getInstance().getWindow().getHandle(), 341) == 1 && local7762_1 == 86) {
         this.field15668 = this.field15668 + MinecraftClient.getInstance().keyboard.getClipboard().replaceAll("[\\r\\n\\t]", "");
         if (this.field15668.length() > 24) {
            this.field15668 = this.field15668.substring(0, 24);
         }

         return true;
      } else if (local7762_1 == 259) {
         if (!this.field15668.isEmpty()) {
            this.field15668 = this.field15668.substring(0, this.field15668.length() - 1);
         }

         this.field15669 = 0.0F;
         return true;
      } else {
         if (local7762_1 != 257 && local7762_1 != 335 && local7762_1 != 256) {
            return false;
         }

         this.field15667 = false;
         return true;
      }
   }

   public void method4602(Runnable runnable) {
      this.field15673 = System.currentTimeMillis();
      this.field15686 = this.field15673;
      this.field15674 = runnable;
      this.field15667 = false;
      this.field15689 = false;
   }

   private void method4603() {
      if (!this.field15668.equals(this.field15664) || this.field15665 != CosmeticCatalog.method0522()) {
         boolean local7764_1 = !this.field15668.equals(this.field15664);
         this.field15664 = this.field15668;
         this.field15665 = CosmeticCatalog.method0522();
         this.field15657.clear();

         for (CosmeticCategory cosmeticCategory : CosmeticCategory.values()) {
            List<CosmeticEntry> local7764_6 = this.method4635(cosmeticCategory);
            this.field15657.put(cosmeticCategory, local7764_6);
            this.field15658.put(cosmeticCategory, String.valueOf(local7764_6.size()));
         }

         if (local7764_1 && !this.field15668.isBlank() && this.field15657.get(this.field15684).isEmpty()) {
            for (CosmeticCategory cosmeticCategory2 : CosmeticCategory.values()) {
               if (!this.field15657.get(cosmeticCategory2).isEmpty()) {
                  this.method4612(cosmeticCategory2);
                  break;
               }
            }
         }
      }
   }

   private void method4604(CosmeticEntry cosmeticEntry, float local7765_2, float local7765_3, float local7765_4, float local7765_5) {
      float local7765_6 = 158.0F * this.field15680;
      float local7765_7 = 29.0F * this.field15680;
      float local7765_8 = this.field15663
         .method4638(
            FontFace.SEMI_BOLD,
            cosmeticEntry == null ? "Ничего не надето" : this.method4619(cosmeticEntry),
            local7765_2 + 30.0F * this.field15680,
            local7765_3 + 14.5F * this.field15680,
            6.5F * this.field15680,
            cosmeticEntry == null ? UiTheme.field09709 : UiTheme.field09680,
            4.0F * this.field15680,
            240L
         );
      FontText.method1397(
         FontFace.SEMI,
         cosmeticEntry == null ? "Выберите предмет" : "Надето",
         local7765_2 + 30.0F * this.field15680,
         local7765_3 + 5.5F * this.field15680,
         5.5F * this.field15680,
         UiTheme.field09709
      );
      float local7765_9 = cosmeticEntry == null ? 0.0F : UiTheme.method2874(local7765_8);
      if (cosmeticEntry == null) {
         UiTheme.method2876(
            this.field15684.method4312(),
            local7765_2 + 14.5F * this.field15680,
            local7765_3 + 14.5F * this.field15680,
            9.0F * this.field15680,
            9.0F * this.field15680,
            UiTheme.method2877(UiTheme.field09680, 0.2F)
         );
      } else {
         CosmeticPreviewAtlas.Nested0499 local7765_10 = CosmeticPreviewAtlas.method3629(cosmeticEntry);
         float local7765_11 = 18.0F * this.field15680 * Math.max(0.0F, local7765_9);
         if (local7765_10 != null && local7765_11 > 0.5F) {
            Render2D.method4695(
               local7765_10.atlasTexture(),
               local7765_2 + 14.5F * this.field15680 - local7765_11 / 2.0F,
               local7765_3 + 14.5F * this.field15680 - local7765_11 / 2.0F,
               local7765_11,
               local7765_11,
               0.0F,
               local7765_10.u0(),
               local7765_10.v0(),
               local7765_10.u1(),
               local7765_10.v1(),
               UiTheme.field09680
            );
         }

         float local7765_12 = this.method4600(
            "cosTakeOff",
            UiTheme.method2879(
               local7765_4,
               local7765_5,
               local7765_2 + local7765_6 - 22.0F * this.field15680,
               local7765_3 + 4.5F * this.field15680,
               18.0F * this.field15680,
               20.0F * this.field15680
            )
         );
         UiTheme.method2884(
            "c",
            local7765_2 + local7765_6 - 13.0F * this.field15680,
            local7765_3 + 14.5F * this.field15680,
            5.5F * this.field15680,
            UiTheme.method2881(UiTheme.field09708, UiTheme.field09702, local7765_12)
         );
         this.field15660
            .add(
               new CosmeticsSelectionScreen.Nested0537(
                  local7765_2 + local7765_6 - 22.0F * this.field15680,
                  local7765_3 + 4.5F * this.field15680,
                  18.0F * this.field15680,
                  20.0F * this.field15680,
                  false,
                  () -> CosmeticStore.method3517(this.field15684)
               )
            );
      }
   }

   private void method4606(float local7767_1, float local7767_2, float local7767_3, float local7767_4) {
      float local7767_5 = this.method4607(0);
      Render2D.method4705(local7767_5);
      float local7767_6 = local7767_1 - (1.0F - local7767_5) * 14.0F * this.field15680;
      float local7767_7 = this.method4600(
         "cosBack",
         UiTheme.method2879(
            local7767_3,
            local7767_4,
            local7767_6 - 4.0F * this.field15680,
            local7767_2 + 105.0F * this.field15680,
            18.0F * this.field15680,
            18.0F * this.field15680
         )
      );
      UiTheme.method2876(
         "assets/deluxe/textures/gui/mainmenu/icons/back.png",
         local7767_6 + 4.75F * this.field15680 - local7767_7 * 1.5F * this.field15680,
         local7767_2 + 113.75F * this.field15680,
         9.5F * this.field15680,
         9.5F * this.field15680,
         UiTheme.method2881(UiTheme.field09708, UiTheme.field09680, local7767_7)
      );
      this.field15660
         .add(
            new CosmeticsSelectionScreen.Nested0537(
               local7767_6 - 4.0F * this.field15680,
               local7767_2 + 105.0F * this.field15680,
               18.0F * this.field15680,
               18.0F * this.field15680,
               false,
               this.field15674
            )
         );
      FontText.method1397(
         FontFace.SEMI,
         "Deluxe client  /  Главная  /  Косметика",
         local7767_6 + 15.5F * this.field15680,
         local7767_2 + 110.5F * this.field15680,
         6.5F * this.field15680,
         UiTheme.field09708
      );
      FontText.method1397(
         FontFace.SEMI, "Косметика.", local7767_6, local7767_2 + 124.5F * this.field15680, 20.0F * this.field15680, UiTheme.field09680
      );
      FontText.method1397(
         FontFace.SEMI,
         "Собирайте образ - предметы применяются сразу",
         local7767_6,
         local7767_2 + 159.5F * this.field15680,
         7.5F * this.field15680,
         UiTheme.field09708
      );
      Render2D.method4650();
   }

   private float method4607(int local7768_1) {
      return UiTheme.method2875(UiTheme.method2885(this.field15673, 40L + local7768_1 * 55L, 420L));
   }

   private void method4609(String local7770_1, float local7770_2, float local7770_3, float local7770_4, float local7770_5, boolean local7770_6) {
      FontText.method1397(FontFace.SEMI, local7770_1, local7770_2, local7770_3, 6.0F * this.field15680, UiTheme.field09709);
      float local7770_7 = local7770_2 + 226.5F * this.field15680;
      if (local7770_6) {
         String local7770_8 = "Снять всё";
         float local7770_9 = FontText.method1396(FontFace.SEMI, local7770_8, 6.0F * this.field15680);
         float local7770_10 = this.method4600(
            "cosClear",
            UiTheme.method2879(
               local7770_4,
               local7770_5,
               local7770_2 + 226.5F * this.field15680 - local7770_9 - 9.0F * this.field15680,
               local7770_3 - 4.0F * this.field15680,
               local7770_9 + 9.0F * this.field15680,
               14.0F * this.field15680
            )
         );
         UiTheme.method2884(
            "c",
            local7770_2 + 226.5F * this.field15680 - local7770_9 - 5.5F * this.field15680,
            local7770_3 + 3.0F * this.field15680,
            5.0F * this.field15680,
            UiTheme.method2881(UiTheme.field09708, UiTheme.field09680, local7770_10)
         );
         FontText.method1397(
            FontFace.SEMI,
            local7770_8,
            local7770_2 + 226.5F * this.field15680 - local7770_9,
            local7770_3,
            6.0F * this.field15680,
            UiTheme.method2881(UiTheme.field09708, UiTheme.field09680, local7770_10)
         );
         this.field15660
            .add(
               new CosmeticsSelectionScreen.Nested0537(
                  local7770_2 + 226.5F * this.field15680 - local7770_9 - 9.0F * this.field15680,
                  local7770_3 - 4.0F * this.field15680,
                  local7770_9 + 9.0F * this.field15680,
                  14.0F * this.field15680,
                  false,
                  CosmeticStore::method3520
               )
            );
         local7770_7 -= local7770_9 + 11.0F * this.field15680;
      }

      float local7770_8 = local7770_2 + FontText.method1396(FontFace.SEMI, local7770_1, 6.0F * this.field15680) + 6.0F * this.field15680;
      if (local7770_7 - local7770_8 > 2.0F * this.field15680) {
         Render2D.method4715(
            local7770_8,
            local7770_3 + 3.0F * this.field15680,
            local7770_7 - local7770_8,
            0.5F * this.field15680,
            0.25F * this.field15680,
            UiTheme.method2877(UiTheme.field09680, 0.12F)
         );
      }
   }

   public boolean method4610(float local7771_1, float local7771_2) {
      if (!this.field15689) {
         return false;
      }

      float local7771_3 = this.field15676 - this.field15675;
      float local7771_4 = local7771_3 / this.field15680;
      float local7771_5 = Math.max(24.0F * this.field15680, local7771_3 * (local7771_4 / Math.max(local7771_4, this.field15666)));
      this.field15669 = this.field15670 = method4629((local7771_2 - this.field15690 - this.field15675) / Math.max(1.0F, local7771_3 - local7771_5), 0.0F, 1.0F)
         * this.field15671;
      return true;
   }

   private void method4612(CosmeticCategory cosmeticCategory) {
      if (cosmeticCategory != this.field15684) {
         this.field15685 = this.field15684;
         this.field15684 = cosmeticCategory;
         this.field15669 = this.field15670 = 0.0F;
         this.field15686 = System.currentTimeMillis();
      }
   }

   private static float method4613(float local7774_0, float local7774_1, float local7774_2) {
      local7774_0 += (local7774_1 - local7774_0) * (1.0F - (float)Math.exp(-local7774_2 * 22.0F));
      return Math.abs(local7774_1 - local7774_0) < 0.1F ? local7774_1 : local7774_0;
   }

   private void method4615(
      CosmeticEntry cosmeticEntry, float local7776_2, float local7776_3, float local7776_4, float local7776_5, boolean local7776_6, int local7776_7
   ) {
      float local7776_8 = Math.min(
         method4629((local7776_3 + 80.0F * this.field15680 - this.field15675) / (14.0F * this.field15680), 0.0F, 1.0F),
         method4629((this.field15676 - local7776_3) / (14.0F * this.field15680), 0.0F, 1.0F)
      );
      if (!(local7776_8 <= 0.01F)) {
         boolean local7776_9 = cosmeticEntry.beta() && !local7776_6;
         boolean local7776_10 = CosmeticStore.method3512(cosmeticEntry);
         boolean local7776_11 = !this.field15689
            && UiTheme.method2879(local7776_4, local7776_5, local7776_2, local7776_3, 56.0F * this.field15680, 80.0F * this.field15680)
            && local7776_5 >= this.field15675
            && local7776_5 <= this.field15676;
         if (local7776_11) {
            this.field15687 = cosmeticEntry;
         }

         float local7776_12 = this.method4630(this.field15653, cosmeticEntry.id(), local7776_11 && !local7776_9);
         float local7776_13 = this.method4630(this.field15654, cosmeticEntry.id(), local7776_10);
         float local7776_14 = UiTheme.method2875(UiTheme.method2885(this.field15686, Math.min(Math.max(local7776_7, 0), 27) * 9L, 240L))
            * local7776_8;
         boolean local7776_15 = local7776_14 < 0.999F;
         if (local7776_15) {
            Render2D.method4705(local7776_14);
         }

         float local7776_16 = 38.0F * this.field15680;
         float local7776_17 = local7776_2 + (56.0F * this.field15680 - local7776_16) / 2.0F;
         float local7776_18 = local7776_3 + 5.0F * this.field15680;
         CosmeticPreviewAtlas.Nested0499 local7776_19 = CosmeticPreviewAtlas.method3629(cosmeticEntry);
         if (local7776_19 != null) {
            Render2D.method4695(
               local7776_19.atlasTexture(),
               local7776_17,
               local7776_18,
               local7776_16,
               local7776_16,
               0.0F,
               local7776_19.u0(),
               local7776_19.v0(),
               local7776_19.u1(),
               local7776_19.v1(),
               UiTheme.method2877(UiTheme.field09680, local7776_9 ? 0.5F : 1.0F)
            );
         } else {
            Render2D.method4715(
               local7776_17 + 8.0F * this.field15680,
               local7776_18 + 8.0F * this.field15680,
               local7776_16 - 16.0F * this.field15680,
               local7776_16 - 16.0F * this.field15680,
               5.0F * this.field15680,
               UiTheme.method2877(
                  UiTheme.field09680, 0.03F + 0.025F * (float)Math.sin((float)(System.currentTimeMillis() % 1400L) / 1400.0F * Math.PI * 2.0)
               )
            );
         }

         CosmeticsSelectionScreen.Nested0536 local7776_20 = this.method4620(cosmeticEntry);
         FontText.method1397(
            FontFace.SEMI,
            local7776_20.text(),
            local7776_2 + (56.0F * this.field15680 - local7776_20.width()) / 2.0F,
            local7776_3 + 47.0F * this.field15680,
            5.5F * this.field15680,
            local7776_9 ? UiTheme.method2877(UiTheme.field09680, 0.42F) : UiTheme.field09680
         );
         float local7776_21 = local7776_2 + 4.0F * this.field15680;
         float local7776_22 = local7776_3 + 62.0F * this.field15680;
         float local7776_23 = 48.0F * this.field15680;
         float local7776_24 = 14.0F * this.field15680;
         if (local7776_9) {
            UiTheme.method2871(
               local7776_21,
               local7776_22,
               local7776_23,
               local7776_24,
               5.0F * this.field15680,
               UiTheme.method2877(UiTheme.field09701, 0.07F),
               UiTheme.method2877(UiTheme.field09701, 0.2F)
            );
            float local7776_25 = FontText.method1396(FontFace.SEMI_BOLD, "Бета", 5.5F * this.field15680);
            float local7776_26 = local7776_2 + (56.0F * this.field15680 - local7776_25 - 8.0F * this.field15680) / 2.0F;
            UiTheme.method2884(
               "Z", local7776_26 + 2.5F * this.field15680, local7776_22 + 7.0F * this.field15680, 5.0F * this.field15680, UiTheme.field09701
            );
            FontText.method1397(
               FontFace.SEMI_BOLD,
               "Бета",
               local7776_26 + 8.0F * this.field15680,
               local7776_22 + 4.5F * this.field15680,
               5.5F * this.field15680,
               UiTheme.field09701
            );
         } else {
            UiTheme.method2871(
               local7776_21,
               local7776_22,
               local7776_23,
               local7776_24,
               5.0F * this.field15680,
               UiTheme.method2881(
                  UiTheme.method2877(UiTheme.field09680, 0.031F + 0.05F * local7776_12), UiTheme.field09686, local7776_13
               ),
               UiTheme.method2881(
                  UiTheme.method2877(UiTheme.field09680, 0.078F + 0.09F * local7776_12), UiTheme.field09713, local7776_13
               )
            );
            FontText.method1397(
               FontFace.SEMI_BOLD,
               local7776_10 ? "Снять" : "Надеть",
               local7776_2 + (56.0F * this.field15680 - (local7776_10 ? this.field15683 : this.field15682)) / 2.0F,
               local7776_22 + 4.5F * this.field15680,
               5.5F * this.field15680,
               UiTheme.method2881(
                  UiTheme.method2881(UiTheme.field09707, UiTheme.field09680, local7776_12),
                  UiTheme.field09689,
                  local7776_13
               )
            );
            this.field15660
               .add(
                  new CosmeticsSelectionScreen.Nested0537(
                     local7776_2, local7776_3, 56.0F * this.field15680, 80.0F * this.field15680, true, () -> CosmeticStore.method3513(cosmeticEntry)
                  )
               );
         }

         if (local7776_15) {
            Render2D.method4650();
         }
      }
   }

   private void method4617(CosmeticCategory cosmeticCategory, float local7778_2, float local7778_3, float local7778_4, float local7778_5) {
      float local7778_6 = this.method4618(
         this.field15655,
         cosmeticCategory,
         UiTheme.method2879(local7778_4, local7778_5, local7778_2, local7778_3, 226.5F * this.field15680, 19.5F * this.field15680)
      );
      float local7778_7 = this.method4618(this.field15656, cosmeticCategory, cosmeticCategory == this.field15684);
      UiTheme.method2871(
         local7778_2,
         local7778_3,
         226.5F * this.field15680,
         19.5F * this.field15680,
         6.0F * this.field15680,
         UiTheme.method2881(
            UiTheme.method2877(UiTheme.field09680, 0.031F + 0.04F * local7778_6),
            UiTheme.method2877(UiTheme.field09686, 0.16F),
            local7778_7
         ),
         UiTheme.method2881(
            UiTheme.method2877(UiTheme.field09680, 0.078F + 0.08F * local7778_6),
            UiTheme.method2877(UiTheme.field09713, 0.6F),
            local7778_7
         )
      );
      UiTheme.method2876(
         cosmeticCategory.method4312(),
         local7778_2 + 11.0F * this.field15680,
         local7778_3 + 9.75F * this.field15680,
         7.0F * this.field15680,
         7.0F * this.field15680,
         UiTheme.method2881(
            UiTheme.method2881(UiTheme.field09708, UiTheme.field09680, local7778_6),
            UiTheme.field09687,
            local7778_7
         )
      );
      FontText.method1397(
         FontFace.SEMI,
         cosmeticCategory.method4309(),
         local7778_2 + 20.0F * this.field15680,
         local7778_3 + 6.25F * this.field15680,
         7.0F * this.field15680,
         UiTheme.method2881(UiTheme.field09707, UiTheme.field09680, Math.max(local7778_6, local7778_7))
      );
      String orDefault = this.field15658.getOrDefault(cosmeticCategory, "0");
      float local7778_9 = FontText.method1396(FontFace.SEMI, orDefault, 6.0F * this.field15680);
      FontText.method1397(
         FontFace.SEMI,
         orDefault,
         local7778_2 + 218.5F * this.field15680 - local7778_9,
         local7778_3 + 6.75F * this.field15680,
         6.0F * this.field15680,
         UiTheme.method2877(UiTheme.field09680, 0.34F + 0.3F * local7778_7)
      );
      CosmeticEntry cosmeticEntry = CosmeticStore.method3519(cosmeticCategory);
      if (cosmeticEntry != null) {
         float local7778_11 = local7778_2 + 218.5F * this.field15680 - local7778_9 - 19.0F * this.field15680;
         CosmeticPreviewAtlas.Nested0499 local7778_12 = CosmeticPreviewAtlas.method3629(cosmeticEntry);
         if (local7778_12 != null) {
            Render2D.method4695(
               local7778_12.atlasTexture(),
               local7778_11 + 1.0F * this.field15680,
               local7778_3 + 4.0F * this.field15680,
               11.5F * this.field15680,
               11.5F * this.field15680,
               0.0F,
               local7778_12.u0(),
               local7778_12.v0(),
               local7778_12.u1(),
               local7778_12.v1(),
               UiTheme.field09680
            );
         }
      }

      this.field15660
         .add(
            new CosmeticsSelectionScreen.Nested0537(
               local7778_2, local7778_3, 226.5F * this.field15680, 19.5F * this.field15680, false, () -> this.method4612(cosmeticCategory)
            )
         );
   }

   private float method4618(EnumMap<CosmeticCategory, AnimatedValue> local7779_1, CosmeticCategory cosmeticCategory, boolean local7779_3) {
      AnimatedValue animatedValue = local7779_1.computeIfAbsent(cosmeticCategory, cosmeticCategory2 -> new AnimatedValue(160L));
      animatedValue.method3869(local7779_3 ? 1.0 : 0.0);
      return (float)animatedValue.method3871();
   }

   private String method4619(CosmeticEntry cosmeticEntry) {
      CosmeticsSelectionScreen.Nested0536 local7780_2 = this.field15659.get(-cosmeticEntry.id() - 1);
      if (local7780_2 != null) {
         return local7780_2.text();
      }

      String local7780_3 = cosmeticEntry.name();
      float local7780_4 = 100.0F * this.field15680;
      float local7780_5 = 6.5F * this.field15680;

      while (FontText.method1396(FontFace.SEMI_BOLD, local7780_3, local7780_5) > local7780_4 && local7780_3.length() > 3) {
         local7780_3 = local7780_3.substring(0, local7780_3.length() - 2) + "…";
      }

      this.field15659.put(-cosmeticEntry.id() - 1, new CosmeticsSelectionScreen.Nested0536(local7780_3, 0.0F));
      return local7780_3;
   }

   private CosmeticsSelectionScreen.Nested0536 method4620(CosmeticEntry cosmeticEntry) {
      CosmeticsSelectionScreen.Nested0536 local7781_2 = this.field15659.get(cosmeticEntry.id());
      if (local7781_2 != null) {
         return local7781_2;
      }

      String local7781_3 = cosmeticEntry.name().replaceFirst("^[^—]+ — ", "");
      float local7781_4 = 51.0F * this.field15680;
      float local7781_5 = 5.5F * this.field15680;

      while (FontText.method1396(FontFace.SEMI, local7781_3, local7781_5) > local7781_4 && local7781_3.length() > 3) {
         local7781_3 = local7781_3.substring(0, local7781_3.length() - 2) + "…";
      }

      CosmeticsSelectionScreen.Nested0536 local7781_6 = new CosmeticsSelectionScreen.Nested0536(
         local7781_3, FontText.method1396(FontFace.SEMI, local7781_3, local7781_5)
      );
      this.field15659.put(cosmeticEntry.id(), local7781_6);
      return local7781_6;
   }

   public boolean method4621(float local7782_1, float local7782_2, float local7782_3) {
      if (!UiTheme.method2879(local7782_1, local7782_2, this.field15677, this.field15678, 592.0F * this.field15680, 396.0F * this.field15680)) {
         return false;
      }

      this.field15669 = method4629(this.field15669 - local7782_3 * 87.0F, 0.0F, this.field15671);
      return true;
   }

   private void method4622(float local7783_1, float local7783_2, float local7783_3, float local7783_4) {
      float local7783_5 = this.method4607(2);
      Render2D.method4705(local7783_5);
      float local7783_6 = local7783_1 - (1.0F - local7783_5) * 14.0F * this.field15680;
      float local7783_7 = local7783_2 + 214.0F * this.field15680;
      this.method4609("Образ", local7783_6, local7783_7, local7783_3, local7783_4, true);
      local7783_7 += 13.0F * this.field15680;
      boolean local7783_8 = false;

      for (CosmeticCategory cosmeticCategory : CosmeticCategory.values()) {
         if (cosmeticCategory.method4308() && !local7783_8) {
            local7783_8 = true;
            local7783_7 += 5.0F * this.field15680;
            this.method4609("Оружие", local7783_6, local7783_7, local7783_3, local7783_4, false);
            local7783_7 += 13.0F * this.field15680;
         }

         this.method4617(cosmeticCategory, local7783_6, local7783_7, local7783_3, local7783_4);
         local7783_7 += 22.5F * this.field15680;
      }

      Render2D.method4650();
   }

   public boolean method4625(int local7786_1) {
      if (this.field15667 && local7786_1 >= 32 && local7786_1 <= 65535) {
         if (this.field15668.length() < 24) {
            this.field15668 = this.field15668 + (char)local7786_1;
         }

         this.field15669 = 0.0F;
         return true;
      } else {
         return false;
      }
   }

   public void method4626(
      DrawContext drawContext,
      float local7787_2,
      float local7787_3,
      float local7787_4,
      float local7787_5,
      float local7787_6,
      float local7787_7,
      float local7787_8
   ) {
      this.field15680 = local7787_3;
      float local7787_9 = Math.min(0.05F, (float)(System.nanoTime() / 1000000L - this.field15672) / 1000.0F);
      this.field15672 = System.nanoTime() / 1000000L;
      this.field15660.clear();
      this.field15688 = this.field15687;
      this.field15687 = null;
      if (this.field15681 != local7787_3) {
         this.field15659.clear();
         this.field15681 = local7787_3;
         this.field15682 = FontText.method1396(FontFace.SEMI_BOLD, "Надеть", 5.5F * local7787_3);
         this.field15683 = FontText.method1396(FontFace.SEMI_BOLD, "Снять", 5.5F * local7787_3);
      }

      float local7787_10 = local7787_2 + 51.5F * local7787_3;
      this.method4603();
      this.method4606(local7787_10, local7787_4, local7787_7, local7787_8);
      this.method4637(local7787_10, local7787_4, local7787_7, local7787_8);
      this.method4622(local7787_10, local7787_4, local7787_7, local7787_8);
      this.method4634(drawContext, local7787_2, local7787_4, local7787_5, local7787_6, local7787_7, local7787_8, local7787_9);
   }

   public void method4628() {
      this.field15667 = false;
      this.field15689 = false;
   }

   private static float method4629(float local7790_0, float local7790_1, float local7790_2) {
      return Math.max(local7790_1, Math.min(local7790_2, local7790_0));
   }

   private float method4630(HashMap<Integer, AnimatedValue> local7791_1, int local7791_2, boolean local7791_3) {
      AnimatedValue animatedValue = local7791_1.computeIfAbsent(local7791_2, local7785_0 -> new AnimatedValue(140L));
      animatedValue.method3869(local7791_3 ? 1.0 : 0.0);
      return (float)animatedValue.method3871();
   }

   public boolean method4632(float local7793_1, float local7793_2) {
      boolean local7793_3 = this.field15667;
      this.field15667 = false;
      if (this.field15671 > 0.5F
         && UiTheme.method2879(
            local7793_1, local7793_2, this.field15679 - 4.0F * this.field15680, this.field15675, 10.0F * this.field15680, this.field15676 - this.field15675
         )) {
         float local7793_4 = this.field15676 - this.field15675;
         float local7793_5 = local7793_4 / this.field15680;
         float local7793_6 = Math.max(24.0F * this.field15680, local7793_4 * (local7793_5 / Math.max(local7793_5, this.field15666)));
         float local7793_7 = this.field15675 + this.field15670 / this.field15671 * (local7793_4 - local7793_6);
         this.field15690 = local7793_2 >= local7793_7 && local7793_2 <= local7793_7 + local7793_6 ? local7793_2 - local7793_7 : local7793_6 / 2.0F;
         this.field15689 = true;
         this.method4610(local7793_1, local7793_2);
         return true;
      }

      for (int index = this.field15660.size() - 1; index >= 0; index--) {
         CosmeticsSelectionScreen.Nested0537 local7793_5 = this.field15660.get(index);
         if ((!local7793_5.clipped() || !(local7793_2 < this.field15675) && !(local7793_2 > this.field15676))
            && UiTheme.method2879(local7793_1, local7793_2, local7793_5.x(), local7793_5.y(), local7793_5.w(), local7793_5.h())) {
            local7793_5.action().run();
            return true;
         }
      }

      return local7793_3;
   }

   private static String method4633(int local7794_0) {
      int local7794_1 = local7794_0 % 100;
      int local7794_2 = local7794_0 % 10;
      return local7794_1 >= 11 && local7794_1 <= 14
         ? "предметов"
         : (local7794_2 == 1 ? "предмет" : (local7794_2 >= 2 && local7794_2 <= 4 ? "предмета" : "предметов"));
   }

   private void method4634(
      DrawContext drawContext,
      float local7795_2,
      float local7795_3,
      float local7795_4,
      float local7795_5,
      float local7795_6,
      float local7795_7,
      float local7795_8
   ) {
      float local7795_9 = this.method4607(3);
      Render2D.method4705(local7795_9);
      this.field15677 = local7795_2 + 344.0F * this.field15680 + (1.0F - local7795_9) * 14.0F * this.field15680;
      this.field15678 = local7795_3 + 92.0F * this.field15680;
      float local7795_10 = 592.0F * this.field15680;
      float local7795_11 = 396.0F * this.field15680;
      float local7795_12 = this.field15678 + 54.0F * this.field15680;
      float local7795_13 = this.field15677 + 16.0F * this.field15680;
      float local7795_14 = this.field15678 + 15.0F * this.field15680;
      List<CosmeticEntry> local7795_15 = this.field15657.getOrDefault(this.field15684, List.of());
      CosmeticEntry cosmeticEntry = CosmeticStore.method3519(this.field15684);
      float local7795_17 = UiTheme.method2875(UiTheme.method2885(this.field15686, 0L, 280L));
      if (local7795_17 < 0.999F && this.field15685 != this.field15684) {
         UiTheme.method2876(
            this.field15685.method4312(),
            local7795_13 + 13.5F * this.field15680,
            local7795_14 + 13.5F * this.field15680,
            15.0F * this.field15680 * (1.0F - local7795_17 * 0.4F),
            15.0F * this.field15680 * (1.0F - local7795_17 * 0.4F),
            UiTheme.method2877(UiTheme.field09687, 1.0F - local7795_17)
         );
      }

      UiTheme.method2876(
         this.field15684.method4312(),
         local7795_13 + 13.5F * this.field15680,
         local7795_14 + 13.5F * this.field15680,
         15.0F * this.field15680 * (0.6F + 0.4F * local7795_17),
         15.0F * this.field15680 * (0.6F + 0.4F * local7795_17),
         UiTheme.method2877(UiTheme.field09687, local7795_17)
      );
      this.field15661
         .method4638(
            FontFace.SEMI,
            this.field15684.method4309(),
            local7795_13 + 36.0F * this.field15680,
            local7795_14 + 0.5F * this.field15680,
            13.0F * this.field15680,
            UiTheme.field09680,
            7.0F * this.field15680,
            280L
         );
      String local7795_18 = this.field15688 != null
         ? this.field15688.name()
         : this.field15658.getOrDefault(this.field15684, "0") + " " + method4633(local7795_15.size()) + (this.field15668.isBlank() ? "" : " по запросу");
      this.field15662
         .method4638(
            FontFace.SEMI,
            local7795_18,
            local7795_13 + 36.0F * this.field15680,
            local7795_14 + 17.0F * this.field15680,
            6.5F * this.field15680,
            this.field15688 != null ? UiTheme.method2877(UiTheme.field09680, 0.85F) : UiTheme.field09708,
            3.0F * this.field15680,
            170L
         );
      this.method4604(
         cosmeticEntry,
         this.field15677 + local7795_10 - 16.0F * this.field15680 - 158.0F * this.field15680,
         local7795_14 - 1.0F * this.field15680,
         local7795_6,
         local7795_7
      );
      this.field15675 = local7795_12 + 8.0F * this.field15680;
      this.field15676 = this.field15678 + local7795_11 - 10.0F * this.field15680;
      float local7795_19 = (this.field15676 - this.field15675) / this.field15680;
      float local7795_20 = 87.0F;
      int local7795_21 = (local7795_15.size() + 9 - 1) / 9;
      this.field15666 = Math.max(0.0F, local7795_21 * local7795_20 - 7.0F + 8.0F);
      this.field15671 = Math.max(0.0F, this.field15666 - local7795_19);
      this.field15669 = method4629(this.field15669, 0.0F, this.field15671);
      this.field15670 = this.field15689 ? this.field15669 : method4613(this.field15670, this.field15669, local7795_8);
      Render2D.method4678(
         drawContext, local7795_13 - 4.0F * this.field15680, this.field15675, 568.0F * this.field15680, this.field15676 - this.field15675
      );
      int local7795_22 = Math.max(0, (int)Math.floor((this.field15670 - 8.0F) / local7795_20));
      int local7795_23 = Math.min(local7795_21 - 1, (int)Math.ceil((this.field15670 + local7795_19) / local7795_20));
      boolean local7795_24 = CosmeticAccess.method3034();

      for (int index = local7795_22; index <= local7795_23; index++) {
         for (int index2 = 0; index2 < 9; index2++) {
            int local7795_27 = index * 9 + index2;
            if (local7795_27 >= local7795_15.size()) {
               break;
            }

            this.method4615(
               local7795_15.get(local7795_27),
               local7795_13 + index2 * 63.0F * this.field15680,
               this.field15675 + (4.0F + index * local7795_20 - this.field15670) * this.field15680,
               local7795_6,
               local7795_7,
               local7795_24,
               local7795_27 - local7795_22 * 9
            );
         }
      }

      if (local7795_15.isEmpty()) {
         String local7795_25 = this.field15668.isBlank() ? "Здесь пока пусто" : "Ничего не найдено";
         float local7795_26 = this.field15677 + local7795_10 / 2.0F;
         UiTheme.method2876(
            "assets/deluxe/textures/gui/mainmenu/icons/search.png",
            local7795_26,
            this.field15675 + 120.0F * this.field15680,
            14.0F * this.field15680,
            14.0F * this.field15680,
            UiTheme.method2877(UiTheme.field09680, 0.22F)
         );
         FontText.method1397(
            FontFace.SEMI,
            local7795_25,
            local7795_26 - FontText.method1396(FontFace.SEMI, local7795_25, 7.5F * this.field15680) / 2.0F,
            this.field15675 + 137.0F * this.field15680,
            7.5F * this.field15680,
            UiTheme.field09709
         );
      }

      Render2D.method4661(drawContext);
      this.field15679 = this.field15677 + local7795_10 - 9.0F * this.field15680;
      if (this.field15671 > 0.5F) {
         float local7795_25 = this.field15676 - this.field15675;
         float local7795_26 = Math.max(24.0F * this.field15680, local7795_25 * (local7795_19 / Math.max(local7795_19, this.field15666)));
         float local7795_27 = this.field15675 + this.field15670 / this.field15671 * (local7795_25 - local7795_26);
         float local7795_28 = this.method4600(
            "cosThumb",
            this.field15689
               || UiTheme.method2879(
                  local7795_6, local7795_7, this.field15679 - 3.0F * this.field15680, this.field15675, 8.0F * this.field15680, local7795_25
               )
         );
         Render2D.method4715(
            this.field15679,
            this.field15675,
            2.0F * this.field15680,
            local7795_25,
            1.0F * this.field15680,
            UiTheme.method2877(UiTheme.field09680, 0.05F)
         );
         Render2D.method4715(
            this.field15679 - local7795_28 * 0.5F * this.field15680,
            local7795_27,
            (2.0F + local7795_28) * this.field15680,
            local7795_26,
            1.5F * this.field15680,
            UiTheme.method2881(UiTheme.field09710, UiTheme.method2877(UiTheme.field09713, 0.9F), local7795_28)
         );
      }

      Render2D.method4650();
   }

   private List<CosmeticEntry> method4635(CosmeticCategory cosmeticCategory) {
      List<CosmeticEntry> local7796_2 = CosmeticCatalog.method0523(cosmeticCategory);
      if (this.field15668.isBlank()) {
         return local7796_2;
      }

      String local7796_3 = this.field15668.toLowerCase(Locale.ROOT);
      List<CosmeticEntry> local7796_4 = new ArrayList<>();

      for (CosmeticEntry cosmeticEntry : local7796_2) {
         if (cosmeticEntry.name().toLowerCase(Locale.ROOT).contains(local7796_3)) {
            local7796_4.add(cosmeticEntry);
         }
      }

      return local7796_4;
   }

   public boolean method4636() {
      boolean local7797_1 = this.field15689;
      this.field15689 = false;
      return local7797_1;
   }

   private void method4637(float local7798_1, float local7798_2, float local7798_3, float local7798_4) {
      float local7798_5 = this.method4607(1);
      Render2D.method4705(local7798_5);
      float local7798_6 = local7798_1 - (1.0F - local7798_5) * 14.0F * this.field15680;
      float local7798_7 = local7798_2 + 184.5F * this.field15680;
      float local7798_8 = this.method4600("cosFieldFocus", this.field15667);
      Render2D.method4715(
         local7798_6,
         local7798_7,
         226.5F * this.field15680,
         19.5F * this.field15680,
         6.0F * this.field15680,
         UiTheme.method2877(UiTheme.field09680, 0.031F)
      );
      Render2D.method4769(
         local7798_6,
         local7798_7,
         226.5F * this.field15680,
         19.5F * this.field15680,
         6.0F * this.field15680,
         0.5F,
         UiTheme.method2881(UiTheme.field09712, UiTheme.method2877(UiTheme.field09713, 0.8F), local7798_8)
      );
      UiTheme.method2876(
         "assets/deluxe/textures/gui/mainmenu/icons/search.png",
         local7798_6 + 8.5F * this.field15680,
         local7798_7 + 9.75F * this.field15680,
         6.5F * this.field15680,
         6.5F * this.field15680,
         UiTheme.method2881(UiTheme.field09709, UiTheme.method2877(UiTheme.field09713, 0.95F), local7798_8)
      );
      String local7798_9 = this.field15668;
      boolean local7798_10 = local7798_9.isEmpty() && !this.field15667;
      FontText.method1397(
         FontFace.SEMI,
         local7798_10 ? "Найти предмет во всех категориях..." : local7798_9 + (this.field15667 && System.currentTimeMillis() % 1000L < 500L ? "|" : ""),
         local7798_6 + 19.0F * this.field15680,
         local7798_7 + 6.25F * this.field15680,
         7.0F * this.field15680,
         local7798_9.isEmpty() ? UiTheme.field09709 : UiTheme.field09680
      );
      this.field15660
         .add(
            new CosmeticsSelectionScreen.Nested0537(local7798_6, local7798_7, 226.5F * this.field15680, 19.5F * this.field15680, false, () -> this.field15667 = true)
         );
      float local7798_11 = this.method4600("cosSearchClearShown", !local7798_9.isEmpty());
      if (local7798_11 > 0.01F) {
         float local7798_12 = this.method4600(
            "cosSearchClear",
            UiTheme.method2879(
               local7798_3, local7798_4, local7798_6 + 207.0F * this.field15680, local7798_7, 19.5F * this.field15680, 19.5F * this.field15680
            )
         );
         UiTheme.method2884(
            "c",
            local7798_6 + 216.75F * this.field15680,
            local7798_7 + 9.75F * this.field15680,
            5.5F * this.field15680 * local7798_11,
            UiTheme.method2877(
               UiTheme.method2881(UiTheme.field09708, UiTheme.field09680, local7798_12), local7798_11
            )
         );
         if (!local7798_9.isEmpty()) {
            this.field15660
               .add(
                  new CosmeticsSelectionScreen.Nested0537(
                     local7798_6 + 207.0F * this.field15680, local7798_7, 19.5F * this.field15680, 19.5F * this.field15680, false, () -> {
                        this.field15668 = "";
                        this.field15669 = 0.0F;
                     }
                  )
               );
         }
      }

      Render2D.method4650();
   }

   private record Nested0536(String text, float width) {
   }

   private record Nested0537(float x, float y, float w, float h, boolean clipped, Runnable action) {
   }

   private static final class Nested0538 {
      private String field16020;
      private String field16021;
      private long field16022;

      private float method4638(
         FontFace fontFace,
         String local7816_2,
         float local7816_3,
         float local7816_4,
         float local7816_5,
         int local7816_6,
         float local7816_7,
         long local7816_8
      ) {
         if (!local7816_2.equals(this.field16020)) {
            this.field16021 = this.field16020;
            this.field16020 = local7816_2;
            this.field16022 = System.currentTimeMillis();
         }

         float local7816_10 = UiTheme.method2875(UiTheme.method2885(this.field16022, 0L, local7816_8));
         if (local7816_10 < 0.999F && this.field16021 != null) {
            Render2D.method4705(1.0F - local7816_10);
            FontText.method1397(fontFace, this.field16021, local7816_3, local7816_4 - local7816_7 * local7816_10, local7816_5, local7816_6);
            Render2D.method4650();
         }

         Render2D.method4705(local7816_10);
         FontText.method1397(fontFace, this.field16020, local7816_3, local7816_4 + local7816_7 * (1.0F - local7816_10), local7816_5, local7816_6);
         Render2D.method4650();
         return local7816_10;
      }
   }
}
