package tokyo.ghoul.inject.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Avatar;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import tokyo.ghoul.client.features.modules.combat.AuraModule;
import tokyo.ghoul.client.features.modules.render.AutoCape;
import tokyo.ghoul.client.features.modules.render.cosmetics.CosmeticsModule;
import tokyo.ghoul.client.features.modules.render.nametags.NameTagsModule;

@Mixin(AvatarRenderer.class)
public abstract class MixinAvatarRenderer {
    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;F)V", at = @At("TAIL"))
    private void flipLocalAvatar(Avatar avatar, AvatarRenderState state, float tickDelta, CallbackInfo ci) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || avatar.getId() != minecraft.player.getId()) return;
        if (!AuraModule.getInstance().shouldFlipModelVisual()) return;
        state.bodyRot += 180.0F;
    }

    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;F)V", at = @At("TAIL"))
    private void desire$hideCapeOnElytra(Avatar avatar, AvatarRenderState state, float tickDelta, CallbackInfo ci) {
        if (AutoCape.hidesCapeForChest(state.chestEquipment, state.isFallFlying)
                || CosmeticsModule.getInstance().appliesCustomModel(state)) {
            state.showCape = false;
        }
    }

    @Inject(method = "getTextureLocation(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;)Lnet/minecraft/resources/Identifier;", at = @At("HEAD"), cancellable = true)
    private void customModelsTexture(AvatarRenderState state, CallbackInfoReturnable<Identifier> cir) {
        CosmeticsModule feature = CosmeticsModule.getInstance();
        if (!feature.appliesCustomModel(state)) return;
        cir.setReturnValue(feature.customModelTexture(state));
    }

    @Inject(method = "shouldShowName(Lnet/minecraft/world/entity/Avatar;D)Z", at = @At("HEAD"), cancellable = true)
    private void hideVanillaNameTag(Avatar avatar, double distanceSqr, CallbackInfoReturnable<Boolean> cir) {
        if (NameTagsModule.shouldHideVanillaTag()) {
            cir.setReturnValue(false);
        }
    }
}
