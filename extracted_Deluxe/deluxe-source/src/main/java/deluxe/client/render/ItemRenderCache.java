package deluxe.client.render;

import deluxe.client.util.Colors;
import deluxe.client.DeluxeClient;
import deluxe.client.render.FontFace;
import deluxe.client.util.ItemDrawOptions;
import deluxe.client.render.ItemRenderer;
import deluxe.client.render.Render2D;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderPass;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.render.state.ItemGuiElementRenderState;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.resource.ResourceType;
import net.minecraft.resource.SynchronousResourceReloader;
import net.minecraft.util.Identifier;

public final class ItemRenderCache {
   private static final FontFace fontFace = FontFace.SEMI_BOLD;
   private static boolean field08304;

   public static void method2335() {
      method2349();
      DeluxeClient.logger().info("[RenderItem] RenderItem initialized");
   }

   public static void method2336(ItemStack itemStack, float local3634_1, float local3634_2, float local3634_3, ItemDrawOptions itemDrawOptions) {
      ItemDrawData itemDrawData = new ItemDrawData(itemStack, local3634_1, local3634_2, local3634_3, itemDrawOptions, 0);
      method2347().method1656(itemDrawData);
      method2356(itemDrawData);
   }

   private static ItemStack method2337(String local3635_0) {
      if (local3635_0 != null && !local3635_0.isBlank()) {
         Identifier identifier = local3635_0.indexOf(58) >= 0 ? Identifier.tryParse(local3635_0.trim()) : Identifier.ofVanilla(local3635_0.trim());
         if (identifier == null) {
            return ItemStack.EMPTY;
         }

         Item item = Registries.ITEM.getOptionalValue(identifier).orElse(null);
         return item == null ? ItemStack.EMPTY : item.getDefaultStack();
      } else {
         return ItemStack.EMPTY;
      }
   }

   public static void method2339(DrawContext drawContext) {
      method2347().method1667(drawContext);
   }

   public static void method2340() {
      method2347().method1672();
   }

   private static void method2341(int local3639_0, float local3639_1, float local3639_2, float local3639_3, float local3639_4) {
      String local3639_5 = Integer.toString(local3639_0);
      float local3639_6 = Math.max(3.0F, Math.round(local3639_3 * 0.6F * 2.0F) / 2.0F);
      float local3639_7 = FontText.method1396(fontFace, local3639_5, local3639_6);
      float local3639_8 = Math.round((local3639_1 + local3639_3 - local3639_7) * 2.0F) / 2.0F;
      float local3639_9 = Math.round((local3639_2 + local3639_3 - local3639_6) * 2.0F) / 2.0F;
      int local3639_10 = Math.round(255.0F * local3639_4);
      FontText.method1397(fontFace, local3639_5, local3639_8, local3639_9, local3639_6, Colors.method3726(255, 255, 255, local3639_10));
   }

   public static void method2342(ItemStack itemStack, float local3640_1, float local3640_2, float local3640_3) {
      method2336(itemStack, local3640_1, local3640_2, local3640_3, ItemDrawOptions.defaults());
   }

   private static void method2343(ItemStack itemStack, float local3641_1, float local3641_2, float local3641_3, float local3641_4) {
      float local3641_5 = Math.max(8.0F, local3641_3 * 0.78F);
      float local3641_6 = Math.max(0.7F, local3641_3 * 0.04F);
      float local3641_7 = local3641_1 + (local3641_3 - local3641_5) * 0.5F;
      float local3641_8 = local3641_2 + local3641_3 - local3641_6 - Math.max(0.35F, local3641_3 * 0.03F);
      float local3641_9 = Math.max(0.0F, Math.min(1.0F, itemStack.getItemBarStep() / 13.0F));
      int itemBarColor = itemStack.getItemBarColor();
      Render2D.method4715(
         local3641_7, local3641_8, local3641_5, local3641_6, local3641_6 * 0.5F, Colors.method3726(0, 0, 0, Math.round(150.0F * local3641_4))
      );
      Render2D.method4715(
         local3641_7,
         local3641_8,
         Math.max(1.0F, local3641_5 * local3641_9),
         local3641_6,
         local3641_6 * 0.5F,
         Colors.method3726(itemBarColor >>> 16 & 0xFF, itemBarColor >>> 8 & 0xFF, itemBarColor & 0xFF, Math.round(240.0F * local3641_4))
      );
   }

   public static void method2344(String local3642_0, float local3642_1, float local3642_2, float local3642_3) {
      method2336(method2337(local3642_0), local3642_1, local3642_2, local3642_3, ItemDrawOptions.defaults());
   }

   public static void method2345(String local3643_0, float local3643_1, float local3643_2, float local3643_3, ItemDrawOptions itemDrawOptions) {
      method2336(method2337(local3643_0), local3643_1, local3643_2, local3643_3, itemDrawOptions);
   }

   public static void method2346() {
      method2347().method1693();
   }

   private static ItemRenderer method2347() {
      return ItemRenderer.method1673();
   }

   public static void method2348() {
      method2347().method1679();
   }

   private ItemRenderCache() {
   }

   private static void method2349() {
      if (!field08304) {
         Identifier identifier = Identifier.of("deluxe", "render_item_cache");
         ResourceLoader.get(ResourceType.CLIENT_RESOURCES).registerReloader(identifier, (SynchronousResourceReloader)resourceManager -> method2346());
         field08304 = true;
      }
   }

   public static void method2350() {
      ItemRenderer.method1676();
      DeluxeClient.logger().info("[RenderItem] RenderItem closed");
   }

   public static void method2351() {
      method2347().method1677();
   }

   public static boolean method2352(RenderPipeline renderPipeline) {
      return method2347().method1661(renderPipeline);
   }

   public static void method2353(ItemStack itemStack, float local3652_1, float local3652_2, float local3652_3, float local3652_4) {
      method2336(itemStack, local3652_1, local3652_2, local3652_3, ItemDrawOptions.defaults().alpha(local3652_4));
   }

   public static int method2354(ItemGuiElementRenderState itemGuiElementRenderState) {
      return method2347().method1690(itemGuiElementRenderState);
   }

   public static void method2355() {
      method2347().method1686();
   }

   private static void method2356(ItemDrawData itemDrawData) {
      if (itemDrawData != null && itemDrawData.visible()) {
         ItemStack itemStack = itemDrawData.stack();
         ItemDrawOptions itemDrawOptions = itemDrawData.options();
         if (itemDrawOptions.showDurability() && itemStack.isItemBarVisible()) {
            method2343(itemStack, itemDrawData.x(), itemDrawData.y(), itemDrawData.size(), itemDrawOptions.alpha());
         }

         if (itemDrawOptions.showCount() && itemStack.getCount() > 1) {
            method2341(itemStack.getCount(), itemDrawData.x(), itemDrawData.y(), itemDrawData.size(), itemDrawOptions.alpha());
         }
      }
   }

   public static void method2357(ItemGuiElementRenderState itemGuiElementRenderState) {
      method2347().method1657(itemGuiElementRenderState);
   }

   public static void method2358(RenderPass renderPass) {
      method2347().method1691(renderPass);
   }
}
