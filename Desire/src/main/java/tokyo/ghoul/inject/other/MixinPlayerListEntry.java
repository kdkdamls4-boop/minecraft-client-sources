package tokyo.ghoul.inject.other;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.world.entity.player.PlayerSkin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import tokyo.ghoul.client.features.commands.CommandSkin;
import tokyo.ghoul.client.features.modules.render.AutoCape;

@Mixin(PlayerInfo.class)
public class MixinPlayerListEntry {
    @ModifyReturnValue(method = "getSkin", at = @At("RETURN"))
    private PlayerSkin skinTexturesHook(PlayerSkin original) {
        PlayerInfo self = (PlayerInfo) (Object) this;

        var customSkin = CommandSkin.getCustomSkinTextures();
        if (customSkin != null && AutoCape.shouldApply(self)) {
            original = customSkin.get();
        }

        return original;
    }

    @ModifyExpressionValue(method = "createSkinLookup", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;isLocalPlayer(Ljava/util/UUID;)Z"))
    private static boolean texturesSupplierHook(boolean original) {
        return original || CommandSkin.getCustomSkinTextures() != null;
    }
}
