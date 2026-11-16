package tokyo.ghoul.inject.render;

import com.mojang.blaze3d.shaders.ShaderType;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net.minecraft.client.renderer.ShaderManager$CompilationCache")
public class MixinShaderCompilationCache {
    @Inject(method = "getShaderSource", at = @At("RETURN"), cancellable = true)
    private void defineDepthToClip(Identifier id, ShaderType type, CallbackInfoReturnable<String> cir) {
        String source = cir.getReturnValue();
        if (source == null || !id.getNamespace().equals("desire") || !id.getPath().startsWith("core/targetesp/")) return;
        int version = source.indexOf("#version");
        int lineEnd = version < 0 ? -1 : source.indexOf('\n', version);
        if (lineEnd < 0) return;
        String body = RenderSystem.getDevice().getDeviceInfo().isZZeroToOne() ? "(value)" : "((value) * 2.0 - 1.0)";
        cir.setReturnValue(source.substring(0, lineEnd + 1)
                + "#define DIMASIK_DEPTH_TO_CLIP(value) " + body + "\n"
                + source.substring(lineEnd + 1));
    }
}
