package deluxe.client.module.misc;

import deluxe.client.module.Module;
import deluxe.client.module.ModuleCategory;
import deluxe.client.module.ModuleInfo;
import deluxe.client.setting.NumberSetting;

import org.lwjgl.openal.AL10;
import org.lwjgl.openal.AL11;
import org.lwjgl.openal.EXTEfx;

@ModuleInfo(name = "BassBoost", description = "Усиление низких частот всех игровых звуков через эквалайзер OpenAL.", category = ModuleCategory.MISC)
public final class BassBoostModule extends Module {
   private static BassBoostModule bassBoostModule;
   public NumberSetting numberSetting;
   private volatile int field14539 = -1;
   private int field14540 = -1;
   private volatile boolean field14541;
   private volatile int field14542;

   public static void method4261() {
      if (bassBoostModule != null) {
         bassBoostModule.field14539 = -1;
         bassBoostModule.field14540 = -1;
         bassBoostModule.field14541 = false;
         bassBoostModule.field14542 = 0;
      }
   }

   @Override
   protected void onDisable() {
      if (this.field14539 != -1) {
         EXTEfx.alAuxiliaryEffectSloti(this.field14539, 1, 0);
      }
   }

   public static void method4262(int local7137_0) {
      if (bassBoostModule != null && bassBoostModule.isEnabled() && !bassBoostModule.field14541) {
         if (bassBoostModule.field14539 == -1) {
            bassBoostModule.method4263();
         }

         if (bassBoostModule.field14539 != -1) {
            bassBoostModule.method4265();
            AL11.alSource3i(local7137_0, 131078, bassBoostModule.field14539, 0, 0);
         }
      }
   }

   private void method4263() {
      try {
         AL10.alGetError();
         this.field14540 = EXTEfx.alGenEffects();
         int local7138_1 = EXTEfx.alGenAuxiliaryEffectSlots();
         EXTEfx.alEffecti(this.field14540, 32769, 12);
         EXTEfx.alEffectf(this.field14540, 3, 0.126F);
         EXTEfx.alEffectf(this.field14540, 6, 0.126F);
         EXTEfx.alEffectf(this.field14540, 9, 0.126F);
         if (AL10.alGetError() != 0) {
            this.field14541 = true;
            return;
         }

         this.field14539 = local7138_1;
      } catch (Throwable throwable) {
         this.field14541 = true;
      }
   }

   @Override
   protected void onEnable() {
      this.field14542 = 0;
   }

   public static BassBoostModule method4264() {
      if (bassBoostModule == null) {
         bassBoostModule = new BassBoostModule();
      }

      return bassBoostModule;
   }

   private BassBoostModule() {
      this.numberSetting = this.addSetting(
         new NumberSetting("Басы", "Насколько сильнее качают низкие частоты.").withRange(1.0F, 18.0F).withStep(1.0F).setValue(8.0F).withSuffix(" дБ")
      );
   }

   private void method4265() {
      if (this.numberSetting.intValue() != this.field14542) {
         this.field14542 = this.numberSetting.intValue();
         EXTEfx.alEffectf(this.field14540, 1, Math.max(0.126F, Math.min(7.943F, (float)Math.pow(10.0, this.field14542 / 20.0) - 1.0F)));
         EXTEfx.alAuxiliaryEffectSloti(this.field14539, 1, this.field14540);
      }
   }
}
