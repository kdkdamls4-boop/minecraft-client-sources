package deluxe.client.cosmetic;

import deluxe.client.module.misc.GlobalsModule;

import java.util.function.Consumer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.client.render.item.ItemRenderState.LayerRenderState;
import net.minecraft.client.render.item.model.special.SpecialModelRenderer;
import net.minecraft.client.render.model.json.Transformation;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.ItemTags;
import org.joml.Vector3fc;

public final class CosmeticItemRenderer {
   private static final CosmeticCategory[] recoveredClass0304Array = new CosmeticCategory[]{
      CosmeticCategory.SWORD, CosmeticCategory.MACE, CosmeticCategory.SHIELD
   };
   private static final CosmeticItemRenderer.Nested0384 field04166 = new CosmeticItemRenderer.Nested0384();

   public static boolean method1381(ItemRenderState itemRenderState, ItemStack itemStack, ItemDisplayContext itemDisplayContext, LivingEntity livingEntity) {
      CosmeticEntry cosmeticEntry = method1384(itemStack, livingEntity);
      if (cosmeticEntry == null) {
         return false;
      }

      itemRenderState.markAnimated();
      CosmeticModel cosmeticModel = CosmeticModel.method1889(cosmeticEntry);
      if (cosmeticModel == null) {
         return false;
      }

      boolean local2021_6 = itemStack.isOf(Items.SHIELD)
         && itemDisplayContext != ItemDisplayContext.GUI
         && itemDisplayContext != ItemDisplayContext.GROUND
         && itemDisplayContext != ItemDisplayContext.FIXED
         && itemDisplayContext != ItemDisplayContext.ON_SHELF;
      itemRenderState.addModelKey(cosmeticEntry);
      LayerRenderState layerRenderState = itemRenderState.newLayer();
      layerRenderState.setUseLight(true);
      layerRenderState.setVertices(cosmeticModel::method1892);
      layerRenderState.setTransform(local2021_6 ? Transformation.IDENTITY : cosmeticModel.method1880(itemDisplayContext));
      layerRenderState.setSpecialModel(field04166, new CosmeticItemRenderer.Nested0383(cosmeticModel, local2021_6));
      return true;
   }

   private static boolean method1382(CosmeticPlacement cosmeticPlacement, ItemStack itemStack) {
      for (String local2022_3 : cosmeticPlacement.targets()) {
         boolean local2022_4 = switch (local2022_3) {
            case "swords" -> itemStack.isIn(ItemTags.SWORDS);
            case "axes" -> itemStack.isIn(ItemTags.AXES);
            case "pickaxes" -> itemStack.isIn(ItemTags.PICKAXES);
            case "shovels" -> itemStack.isIn(ItemTags.SHOVELS);
            case "hoes" -> itemStack.isIn(ItemTags.HOES);
            case "mace" -> itemStack.isOf(Items.MACE);
            case "shield" -> itemStack.isOf(Items.SHIELD);
            case "trident" -> itemStack.isOf(Items.TRIDENT);
            default -> false;
         };
         if (local2022_4) {
            return true;
         }
      }

      return false;
   }

   public static float method1383() {
      MinecraftClient minecraftClient = MinecraftClient.getInstance();
      return minecraftClient.player == null ? 0.0F : (minecraftClient.player.age + minecraftClient.getRenderTickCounter().getTickProgress(false)) / 20.0F;
   }

   private CosmeticItemRenderer() {
   }

   public static CosmeticEntry method1384(ItemStack itemStack, LivingEntity livingEntity) {
      if (!itemStack.isEmpty() && (itemStack.isIn(ItemTags.SWORDS) || itemStack.isOf(Items.MACE) || itemStack.isOf(Items.SHIELD))) {
         MinecraftClient minecraftClient = MinecraftClient.getInstance();
         boolean local2026_3 = livingEntity == null || livingEntity == minecraftClient.player;
         if (local2026_3 || livingEntity instanceof AbstractClientPlayerEntity && GlobalsModule.method0643().method0639()) {
            for (CosmeticCategory cosmeticCategory : recoveredClass0304Array) {
               CosmeticEntry cosmeticEntry = local2026_3
                  ? CosmeticStore.method3519(cosmeticCategory)
                  : GlobalsModule.method0643().method0641(((AbstractClientPlayerEntity)livingEntity).getGameProfile().name(), cosmeticCategory);
               if (cosmeticEntry != null && cosmeticEntry.placement() != null && method1382(cosmeticEntry.placement(), itemStack)) {
                  return cosmeticEntry;
               }
            }

            return null;
         } else {
            return null;
         }
      } else {
         return null;
      }
   }

   private record Nested0383(CosmeticModel model, boolean heldShield) {
   }

   private static final class Nested0384 implements SpecialModelRenderer<CosmeticItemRenderer.Nested0383> {
      public CosmeticItemRenderer.Nested0383 getData(ItemStack itemStack) {
         return null;
      }

      public void render(
         CosmeticItemRenderer.Nested0383 local2036_1,
         ItemDisplayContext itemDisplayContext,
         MatrixStack matrixStack,
         OrderedRenderCommandQueue orderedRenderCommandQueue,
         int local2036_5,
         int local2036_6,
         boolean local2036_7,
         int local2036_8
      ) {
         if (local2036_1 != null) {
            if (!local2036_1.heldShield()) {
               local2036_1.model().method1882(matrixStack, orderedRenderCommandQueue, local2036_5, CosmeticItemRenderer.method1383(), 0.0F);
            } else {
               matrixStack.push();
               matrixStack.translate(0.5F, 0.5F, 0.5F);
               local2036_1.model().method1877(matrixStack, itemDisplayContext);
               local2036_1.model().method1882(matrixStack, orderedRenderCommandQueue, local2036_5, CosmeticItemRenderer.method1383(), 0.0F);
               matrixStack.pop();
            }
         }
      }

      @Override
      public void collectVertices(Consumer<Vector3fc> local2037_1) {
      }
   }
}
