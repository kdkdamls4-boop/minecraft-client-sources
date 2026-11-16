package net.skycore.client.util;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.skycore.client.SkycoreClient;
import net.skycore.client.module.render.CosmeticsModule;

public final class CosmeticsRenderLayer extends RenderLayer<AvatarRenderState, PlayerModel> {
   public CosmeticsRenderLayer(RenderLayerParent<AvatarRenderState, PlayerModel> renderLayerParent) {
      super(renderLayerParent);
   }

   public void submit(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int value, AvatarRenderState avatarRenderState, float value2, float value3) {
      CosmeticsModule cosmeticsModule = SkycoreClient.modules().get(CosmeticsModule.class).orElse(null);
      if (cosmeticsModule != null) {
         if (cosmeticsModule.isEnabled()) {
            Minecraft instance = Minecraft.getInstance();
            if (instance.player == null) {
               return;
            }

            if (avatarRenderState.id != instance.player.getId()) {
               return;
            }

            EnsureIndexHelper ensureIndexHelper = cosmeticsModule.renderer();
            PlayerModel parentModel = this.getParentModel();
            String text = cosmeticsModule.selected("wings");
            String text2 = cosmeticsModule.selected("backpacks");
            String text3 = cosmeticsModule.selected("hats");
            String text4 = cosmeticsModule.selected("masks");
            ensureIndexHelper.submitAttached(poseStack, submitNodeCollector, parentModel, value, text, text2, text3, text4, cosmeticsModule.selected("pets"));
            return;
         }
      }
   }
}
