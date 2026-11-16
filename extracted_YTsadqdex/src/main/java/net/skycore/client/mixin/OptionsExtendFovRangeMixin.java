package net.skycore.client.mixin;

import com.mojang.serialization.Codec;
import java.io.File;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.OptionInstance.IntRange;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Options.class)
public abstract class OptionsExtendFovRangeMixin {
   private static final int SKYCORE_MAX_FOV = 150;
   @Shadow
   @Final
   @Mutable
   private OptionInstance<Integer> fov;

   @Inject(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Options;load()V"))
   private void skycore$extendFovRange(Minecraft minecraft, File file, CallbackInfo callbackInfo) {
      this.fov = new OptionInstance<>("options.fov", OptionInstance.noTooltip(), (text, value) -> {
         return switch (value) {
            case 70 -> Options.genericValueLabel(text, Component.translatable("options.fov.min"));
            case 150 -> Options.genericValueLabel(text, Component.translatable("options.fov.max"));
            default -> Options.genericValueLabel(text, value);
         };
      }, new IntRange(30, 150), Codec.DOUBLE.xmap(value2 -> (int)(value2 * 40.0 + 70.0), value3 -> (value3.intValue() - 70.0) / 40.0), 70, OptionInstance.NO_ACTION);
   }
}
