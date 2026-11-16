package tokyo.ghoul.client.features.modules.render;

import lombok.Getter;
import tokyo.ghoul.api.module.Category;
import tokyo.ghoul.api.module.Module;
import tokyo.ghoul.api.module.ModuleRegister;
import tokyo.ghoul.api.module.setting.*;
import tokyo.ghoul.client.features.modules.render.targetesp.render.*;
import net.minecraft.client.renderer.RenderPipelines;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import org.joml.Vector4f;

@ModuleRegister(name = "Hands", description = "Настройка рендера рук", category = Category.RENDER)
public class HandsModule extends Module {

   public static volatile boolean f_28498;
   public static RenderTarget f_28499;
   private final ModeSetting f_28500;
   private final BooleanSetting f_28501;
   private final BooleanSetting f_28502;
   private final SliderSetting f_28503;
   private final SliderSetting f_28504;
   private final SliderSetting f_28505;
   private final BooleanSetting f_28506;
   private final SliderSetting f_28507;
   private final SliderSetting f_28508;
   private final ColorSetting f_28509;
   private final SliderSetting f_28510;
   private final BooleanSetting f_28511;
   private final SliderSetting f_28512;
   private final ModeSetting f_28513;
   private final ColorSetting f_28514;
   private final BooleanSetting f_28515;
   private final ColorSetting f_28516;
   private final SliderSetting f_28517;
   private final SliderSetting f_28518;
   private final SliderSetting f_28519;
   private final SliderSetting f_28520;
   private final BooleanSetting f_28521;
   private final SliderSetting f_28522;
   private final BooleanSetting f_28523;
   private final SliderSetting f_28524;
   private final BooleanSetting f_28525;
   private final SliderSetting f_28526;
   private final SliderSetting f_28527;
   private final ColorSetting f_28528;
   private final ColorSetting f_28529;
   private final SliderSetting f_28530;
   private final BooleanSetting f_28531;
   private final ColorSetting f_28532;
   private final SliderSetting f_28533;
   private final BooleanSetting f_28534;
   private final ModeSetting f_28535;
   private final SliderSetting f_28536;
   private final SliderSetting f_28537;
   private final SliderSetting f_28538;
   private final SliderSetting f_28539;
   private final SliderSetting f_28540;
   private final SliderSetting f_28541;
   private final SliderSetting f_28542;
   private final SliderSetting f_28543;
   private final SliderSetting f_28544;
   private final SliderSetting f_28545;
   private final SliderSetting f_28546;
   private final SliderSetting f_28547;
   private final BooleanSetting f_28548;
   private final SliderSetting f_28549;
   private final SliderSetting f_28550;
   private final BooleanSetting f_28551;
   private final SliderSetting f_28552;
   private final SliderSetting f_28553;
   private final SliderSetting f_28554;
   private final SliderSetting f_28555;
   private final SliderSetting f_28556;
   private final SliderSetting f_28557;
   private final SliderSetting f_28558;
   private final SliderSetting f_28559;
   private final BooleanSetting f_28560;
   private final SliderSetting f_28561;
   private TextureTarget f_28562;
   private boolean f_28563;
   private long f_28564;

   public float m_8108() {
      return this.f_28507.getValue();
   }

   public int m_8109() {
      int var1 = Math.round(this.f_28520.getValue());
      return var1 == 1 ? 3 : (var1 == 3 ? 8 : 5);
   }

   public int m_8110() {
      return this.f_28501.getValue() ? tokyo.ghoul.api.utils.color.UIColors.primary().getRGB() : this.f_28514.getValue().getRGB();
   }

   public float m_8111() {
      return this.f_28519.getValue();
   }

   public float m_8112() {
      return this.f_28539.getValue();
   }

   public float m_8113() {
      return this.f_28541.getValue();
   }

   public float m_8114() {
      return this.f_28503.getValue();
   }

   public void m_8115() {
      if (this.m_8181() && !this.f_28563 && mc.player != null && mc.level != null) {
         RenderTarget var1 = mc.gameRenderer.mainRenderTarget();
         int var2 = Math.max(1, var1.width);
         int var3 = Math.max(1, var1.height);
         if (this.f_28562 == null) {
            this.f_28562 = new TextureTarget("hands-capture", var2, var3, true, GpuFormat.RGBA8_UNORM);
         } else if (this.f_28562.width != var2 || this.f_28562.height != var3) {
            this.f_28562.resize(var2, var3);
         }

         RenderSystem.getDevice()
            .createCommandEncoder()
            .clearColorAndDepthTextures(this.f_28562.getColorTexture(), new Vector4f(0.0F), this.f_28562.getDepthTexture(), 0.0);
         f_28499 = this.f_28562;
         f_28498 = true;
         this.f_28563 = true;
      }
   }

   public float m_8116() {
      return this.f_28527.getValue();
   }

   public float m_8118() {
      return this.f_28549.getValue();
   }

   public int m_8119() {
      if (this.m_8127()) {
         return 4;
      } else {
         return this.m_8125() ? 0 : (this.f_28500.is("Шейдер") ? 1 : (this.f_28500.is("Зеркало") ? 2 : 3));
      }
   }

   public int m_8120() {
      if (this.f_28501.getValue()) {
         return this.f_28515.getValue()
            ? tokyo.ghoul.api.utils.color.UIColors.secondary().getRGB()
            : tokyo.ghoul.api.utils.color.UIColors.primary().getRGB();
      } else {
         return this.f_28515.getValue() ? this.f_28516.getValue().getRGB() : this.f_28514.getValue().getRGB();
      }
   }

   public float m_8121() {
      return this.f_28552.getValue();
   }

   public int m_8122() {
      return this.f_28501.getValue() ? tokyo.ghoul.api.utils.color.UIColors.primary().getRGB() : this.f_28509.getValue().getRGB();
   }

   public boolean m_8123() {
      return this.f_28511.getValue();
   }

   public boolean m_8124() {
      return this.f_28548.getValue();
   }

   public boolean m_8125() {
      return this.f_28500.is("Заливка");
   }

   public boolean m_8127() {
      return this.f_28500.is("Nothing");
   }

   public boolean m_8129() {
      return this.f_28531.getValue();
   }

   public void m_8130() {
      if (this.f_28563) {
         f_28498 = false;
         f_28499 = null;
         this.f_28563 = false;
         if (this.isEnabled() && this.f_28562 != null && mc.gameRenderer.mainRenderTarget() != null) {
            HandsEffectRenderer.f_14920.m_5320(this.f_28562, mc.gameRenderer.mainRenderTarget(), this);
         }
      }
   }

   public boolean m_8131() {
      return this.f_28500.is("Шейдер") || this.m_8173();
   }

   public boolean m_8132() {
      return this.f_28523.getValue();
   }

   public float m_8133() {
      return this.f_28526.getValue();
   }

   public boolean m_8134() {
      return this.m_8186() && this.f_28535.is("Энергия");
   }

   public float m_8136() {
      return this.f_28537.getValue();
   }

   public HandsModule() {
            this.f_28500 = new ModeSetting("Режим").value("Nothing").values("Nothing", "Заливка", "Шейдер", "Зеркало", "Шейдер + Зеркало");
      this.f_28501 = new BooleanSetting("Авто цвет под тему").value(true);
      this.f_28502 = new BooleanSetting("Авто цвет под предмет").value(false);
      this.f_28503 = new SliderSetting("Смешивание").value(0.5F).range(0.0F, 1.0F).step(0.01F).setVisible(this::m_8151);
      this.f_28504 = new SliderSetting("Искажение зеркала").value(0.0F).range(0.0F, 1.0F).step(0.05F).setVisible(this::m_8151);
      this.f_28505 = new SliderSetting("Сила размытия").value(1.0F).range(0.1F, 3.0F).step(0.05F).setVisible(this::m_8151);
      this.f_28506 = new BooleanSetting("Радужная").value(false).setVisible(this::m_8125);
      this.f_28507 = new SliderSetting("Скорость радуги").value(0.4F).range(0.0F, 2.0F).step(0.05F).setVisible(() -> this.m_8125() && this.f_28506.getValue());
      this.f_28508 = new SliderSetting("Масштаб радуги").value(1.0F).range(0.2F, 3.0F).step(0.1F).setVisible(() -> this.m_8125() && this.f_28506.getValue());
      this.f_28509 = new ColorSetting("Цвет заливки").value(new java.awt.Color(-48060, true)).setVisible(() -> this.m_8125() && !this.f_28506.getValue() && !this.f_28501.getValue());
      this.f_28510 = new SliderSetting("Прозрачность заливки").value(0.8F).range(0.0F, 1.0F).step(0.05F).setVisible(this::m_8125);
      this.f_28511 = new BooleanSetting("Сохранить тени").value(true).setVisible(this::m_8125);
      this.f_28512 = new SliderSetting("Сила теней").value(0.3F).range(0.0F, 1.0F).step(0.05F).setVisible(() -> this.m_8125() && this.f_28511.getValue());
      this.f_28513 = new ModeSetting("Стиль шейдера").value("Шейдер").values("Шейдер", "HandShader", "Туманность", "Космос", "Градиент", "Homie")
         .setVisible(this::m_8131);
      this.f_28514 = new ColorSetting("Цвет шейдера 1").value(new java.awt.Color(-3632385, true)).setVisible(() -> this.m_8131() && !this.f_28501.getValue());
      this.f_28515 = new BooleanSetting("Второй цвет").value(false).setVisible(this::m_8131);
      this.f_28516 = new ColorSetting("Цвет шейдера 2").value(new java.awt.Color(-11534136, true)).setVisible(() -> this.m_8131() && this.f_28515.getValue() && !this.f_28501.getValue());
      this.f_28517 = new SliderSetting("Скорость шейдера").value(1.5F).range(0.05F, 2.5F).step(0.05F).setVisible(this::m_8131);
      this.f_28518 = new SliderSetting("Масштаб шейдера").value(1.5F).range(0.2F, 5.0F).step(0.1F).setVisible(this::m_8131);
      this.f_28519 = new SliderSetting("Свечение шейдера").value(1.2F).range(0.0F, 3.0F).step(0.05F).setVisible(this::m_8131);
      this.f_28520 = new SliderSetting("Качество шейдера").value(2.0F).range(1.0F, 3.0F).step(1.0F).setVisible(() -> this.m_8131() && !this.f_28513.is("Градиент"));
      this.f_28521 = new BooleanSetting("Звёзды").value(true).setVisible(this::m_8131);
      this.f_28522 = new SliderSetting("Прозрачность шейдера").value(1.0F).range(0.0F, 1.0F).step(0.05F).setVisible(this::m_8131);
      this.f_28523 = new BooleanSetting("Сохранить тени шейдера").value(false).setVisible(this::m_8131);
      this.f_28524 = new SliderSetting("Сила теней шейдера").value(0.5F).range(0.0F, 1.0F).step(0.05F).setVisible(() -> this.m_8131() && this.f_28523.getValue());
      this.f_28525 = new BooleanSetting("Глов").value(true);
      this.f_28526 = new SliderSetting("Размытие").value(3.0F).range(1.0F, 5.0F).step(1.0F).setVisible(this.f_28525::getValue);
      this.f_28527 = new SliderSetting("Яркость").value(1.0F).range(0.5F, 5.0F).step(0.1F).setVisible(this.f_28525::getValue);
      this.f_28528 = new ColorSetting("Цвет глова 1").value(new java.awt.Color(-11713, true)).setVisible(() -> this.f_28525.getValue() && !this.f_28501.getValue() && !this.f_28502.getValue());
      this.f_28529 = new ColorSetting("Цвет глова 2").value(new java.awt.Color(-54784, true)).setVisible(() -> this.f_28525.getValue() && !this.f_28501.getValue() && !this.f_28502.getValue());
      this.f_28530 = new SliderSetting("Внутреннее свечение").value(0.65F).range(0.0F, 1.5F).step(0.05F).setVisible(this.f_28525::getValue);
      this.f_28531 = new BooleanSetting("Обводка").value(true);
      this.f_28532 = new ColorSetting("Цвет обводки").value(new java.awt.Color(-1, true)).setVisible(() -> this.f_28531.getValue() && !this.f_28501.getValue() && !this.f_28502.getValue());
      this.f_28533 = new SliderSetting("Толщина обводки").value(1.0F).range(0.5F, 2.0F).step(0.5F).setVisible(this.f_28531::getValue);
      this.f_28534 = new BooleanSetting("Шлейф").value(true).setVisible(this.f_28525::getValue);
      this.f_28535 = new ModeSetting("Режим шлейфа").value("Обычный").values("Обычный", "Энергия", "Ленты")
         .setVisible(this::m_8186);
      this.f_28536 = new SliderSetting("Длина энергии").value(1.0F).range(0.4F, 2.0F).step(0.05F).setVisible(this::m_8134);
      this.f_28537 = new SliderSetting("Яркость энергии").value(1.35F).range(0.5F, 2.5F).step(0.05F).setVisible(this::m_8134);
      this.f_28538 = new SliderSetting("Движение энергии").value(1.3F).range(0.0F, 2.0F).step(0.05F).setVisible(this::m_8134);
      this.f_28539 = new SliderSetting("Пульсация энергии").value(1.0F).range(0.0F, 2.0F).step(0.05F).setVisible(this::m_8134);
      this.f_28540 = new SliderSetting("Шлейф за камерой").value(0.05F).range(0.0F, 2.0F).step(0.05F).setVisible(this::m_8134);
      this.f_28541 = new SliderSetting("Длина лент").value(0.3F).range(0.15F, 3.0F).step(0.05F).setVisible(this::m_8153);
      this.f_28542 = new SliderSetting("Ширина лент").value(0.55F).range(0.25F, 1.5F).step(0.05F).setVisible(this::m_8153);
      this.f_28543 = new SliderSetting("Яркость лент").value(1.0F).range(0.0F, 2.5F).step(0.05F).setVisible(this::m_8153);
      this.f_28544 = new SliderSetting("Сила лент за камерой").value(0.1F).range(0.0F, 2.0F).step(0.05F).setVisible(this::m_8153);
      this.f_28545 = new SliderSetting("Подъём лент").value(0.32F).range(0.0F, 1.0F).step(0.01F).setVisible(this::m_8153);
      this.f_28546 = new SliderSetting("Изгиб лент").value(2.0F).range(0.0F, 2.0F).step(0.05F).setVisible(this::m_8153);
      this.f_28547 = new SliderSetting("Скорость волн").value(1.5F).range(0.0F, 3.0F).step(0.05F).setVisible(this::m_8153);
      this.f_28548 = new BooleanSetting("Ленты при ударе").value(true).setVisible(this::m_8153);
      this.f_28549 = new SliderSetting("Длина при ударе").value(0.0F).range(0.15F, 3.0F).step(0.05F).setVisible(() -> this.m_8153() && this.f_28548.getValue());
      this.f_28550 = new SliderSetting("Сила при ударе").value(1.15F).range(0.0F, 2.5F).step(0.05F).setVisible(() -> this.m_8153() && this.f_28548.getValue());
      this.f_28551 = new BooleanSetting("Ленты при еде").value(true).setVisible(this::m_8153);
      this.f_28552 = new SliderSetting("Длина при еде").value(0.65F).range(0.15F, 3.0F).step(0.05F).setVisible(() -> this.m_8153() && this.f_28551.getValue());
      this.f_28553 = new SliderSetting("Сила при еде").value(0.7F).range(0.0F, 2.5F).step(0.05F).setVisible(() -> this.m_8153() && this.f_28551.getValue());
      this.f_28554 = new SliderSetting("Скорость затухания").value(0.04F).range(0.002F, 0.2F).step(0.002F).setVisible(this::m_8165);
      this.f_28555 = new SliderSetting("Подъём").value(0.2F).range(0.0F, 1.5F).step(0.05F).setVisible(this::m_8165);
      this.f_28556 = new SliderSetting("Качание").value(0.0F).range(0.0F, 0.2F).step(0.005F).setVisible(this::m_8165);
      this.f_28557 = new SliderSetting("Шлейф от камеры").value(0.0F).range(0.0F, 2.0F).step(0.05F).setVisible(this::m_8165);
      this.f_28558 = new SliderSetting("Турбулентность").value(0.02F).range(0.0F, 0.6F).step(0.01F).setVisible(this::m_8165);
      this.f_28559 = new SliderSetting("Мерцание").value(0.2F).range(0.0F, 0.2F).step(0.01F).setVisible(this::m_8165);
      this.f_28560 = new BooleanSetting("Сдув при ударе").value(false).setVisible(this::m_8165);
      this.f_28561 = new SliderSetting("Сила сдува").value(4.0F).range(1.0F, 10.0F).step(0.5F).setVisible(() -> this.m_8165() && this.f_28560.getValue());
      addSettings(
         this.f_28500,
         this.f_28501,
         this.f_28502,
         this.f_28503,
         this.f_28504,
         this.f_28505,
         this.f_28506,
         this.f_28507,
         this.f_28508,
         this.f_28509,
         this.f_28510,
         this.f_28511,
         this.f_28512,
         this.f_28513,
         this.f_28514,
         this.f_28515,
         this.f_28516,
         this.f_28517,
         this.f_28518,
         this.f_28519,
         this.f_28520,
         this.f_28521,
         this.f_28522,
         this.f_28523,
         this.f_28524,
         this.f_28525,
         this.f_28526,
         this.f_28527,
         this.f_28528,
         this.f_28529,
         this.f_28530,
         this.f_28531,
         this.f_28532,
         this.f_28533,
         this.f_28534,
         this.f_28535,
         this.f_28536,
         this.f_28537,
         this.f_28538,
         this.f_28539,
         this.f_28540,
         this.f_28541,
         this.f_28542,
         this.f_28543,
         this.f_28544,
         this.f_28545,
         this.f_28546,
         this.f_28547,
         this.f_28548,
         this.f_28549,
         this.f_28550,
         this.f_28551,
         this.f_28552,
         this.f_28553,
         this.f_28554,
         this.f_28555,
         this.f_28556,
         this.f_28557,
         this.f_28558,
         this.f_28559,
         this.f_28560,
         this.f_28561
      );
   }

   public float m_8137() {
      return this.f_28518.getValue();
   }

   public float m_8138() {
      return this.f_28553.getValue();
   }

   @Override
   public void onEnable() {
      this.f_28564 = System.currentTimeMillis();
   }

   public float m_8139() {
      return this.f_28556.getValue();
   }

   public float m_8140() {
      return this.f_28510.getValue();
   }

   public int m_8141() {
      return tokyo.ghoul.api.utils.color.UIColors.primary().getRGB();
   }

   public float m_8143() {
      return this.f_28546.getValue();
   }

   public boolean m_8144() {
      return this.f_28560.getValue();
   }

   public boolean m_8145() {
      return this.f_28551.getValue();
   }

   public float m_8147() {
      return this.f_28540.getValue();
   }

   public float m_8148() {
      return this.f_28557.getValue();
   }

   public float m_8149() {
      return this.f_28555.getValue();
   }

   public float m_8150() {
      return this.f_28550.getValue();
   }

   public boolean m_8151() {
      return this.f_28500.is("Зеркало") || this.m_8173();
   }

   public float m_8152() {
      return this.f_28517.getValue();
   }

   public boolean m_8153() {
      return this.m_8186() && this.f_28535.is("Ленты");
   }

   public int m_8155() {
      if (this.f_28502.getValue()) {
         return -1;
      } else {
         return this.f_28501.getValue() ? tokyo.ghoul.api.utils.color.UIColors.primary().getRGB() : this.f_28532.getValue().getRGB();
      }
   }

   public float m_8156() {
      return this.f_28508.getValue();
   }

   public float m_8158() {
      return this.f_28504.getValue();
   }

   public int m_8159() {
      if (this.f_28513.is("HandShader")) {
         return 4;
      } else if (this.f_28513.is("Туманность")) {
         return 1;
      } else if (this.f_28513.is("Космос")) {
         return 2;
      } else if (this.f_28513.is("Градиент")) {
         return 3;
      } else {
         return this.f_28513.is("Homie") ? 5 : 0;
      }
   }

   public float m_8160() {
      return this.f_28533.getValue();
   }

   public float m_8161() {
      return this.f_28554.getValue();
   }

   public float m_8162() {
      return this.f_28545.getValue();
   }

   public float m_8163() {
      return this.f_28543.getValue();
   }

   public float m_8164() {
      return this.f_28547.getValue();
   }

   public boolean m_8165() {
      return this.m_8186() && this.f_28535.is("Обычный");
   }

   public int m_8166() {
      return this.f_28501.getValue() ? tokyo.ghoul.api.utils.color.UIColors.primary().getRGB() : this.f_28528.getValue().getRGB();
   }

   public float m_8167() {
      return this.f_28536.getValue();
   }

   public float m_8168() {
      return this.f_28505.getValue();
   }

   public float m_8169() {
      return this.f_28559.getValue();
   }

   public float m_8170() {
      return this.f_28542.getValue();
   }

   public int m_8171() {
      return this.f_28501.getValue() ? tokyo.ghoul.api.utils.color.UIColors.secondary().getRGB() : this.f_28529.getValue().getRGB();
   }

   public float m_8172() {
      return this.f_28512.getValue();
   }

   public boolean m_8173() {
      return this.f_28500.is("Шейдер + Зеркало");
   }

   public float m_8174() {
      return this.f_28544.getValue();
   }

   public float m_8177() {
      return this.f_28561.getValue();
   }

   @Override
   public void onDisable() {
      f_28498 = false;
      f_28499 = null;
      this.f_28563 = false;
      if (this.f_28562 != null) {
         this.f_28562.destroyBuffers();
         this.f_28562 = null;
      }

      HandsEffectRenderer.f_14920.m_5303();
   }

   public float m_8179() {
      return this.f_28558.getValue();
   }

   public boolean m_8181() {
      return this.isEnabled() && (!this.m_8127() || this.f_28525.getValue() || this.f_28531.getValue());
   }

   public float m_8182() {
      return this.f_28530.getValue();
   }

   public float m_8183() {
      return (float)(System.currentTimeMillis() - this.f_28564) / 1000.0F;
   }

   public boolean m_8186() {
      return this.f_28525.getValue() && this.f_28534.getValue();
   }

   public float m_8188() {
      return this.f_28522.getValue();
   }

   public boolean m_8189() {
      return this.f_28502.getValue();
   }

   public boolean m_8190() {
      return this.f_28521.getValue();
   }

   public boolean m_8191() {
      return this.f_28506.getValue();
   }

   public int m_8193() {
      return this.m_8153() ? 2 : (this.m_8134() ? 1 : 0);
   }

   public float m_8194() {
      return this.f_28524.getValue();
   }

   public boolean m_8195() {
      return this.f_28525.getValue();
   }

   public float m_8196() {
      return this.f_28538.getValue();
   }

   @Override
   public void onEvent() {
   }

   @Getter private static final HandsModule instance = new HandsModule();
}
