package deluxe.client.module.render;

import deluxe.client.setting.BooleanSetting;
import deluxe.mixin.ChatHudMixin;
import deluxe.client.event.ContainerRenderEvent;
import deluxe.client.event.EventListener;
import deluxe.mixin.HandledScreenMixin;
import deluxe.client.util.MathUtils;
import deluxe.client.module.Module;
import deluxe.client.module.ModuleCategory;
import deluxe.client.module.ModuleInfo;
import deluxe.client.module.ModuleManager;
import deluxe.client.event.PacketEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.entity.EntityPosition;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;

@ModuleInfo(name = "SmoothFeature", description = "Плавные мелочи клиента: предметы под курсором, чат, камера при флагах.", category = ModuleCategory.RENDER)
public final class SmoothFeatureModule extends Module {
   private static SmoothFeatureModule smoothFeatureModule;
   public BooleanSetting field11538;
   public BooleanSetting field11539;
   public BooleanSetting field11540;
   public BooleanSetting field11541;
   private final HashMap<Slot, Float> field11542 = new HashMap<>();
   private long field11543;
   private boolean field11544;
   private String field11545 = "";
   private int field11546 = 1;
   private long field11547;
   private boolean field11548;
   private Vec3d field11549;
   private long field11550;
   private long field11551;
   private final EventListener<ContainerRenderEvent> field11552 = containerRenderEvent -> {
      long local5342_2 = System.currentTimeMillis();
      float local5342_4 = 1.0F - (float)Math.exp(-Math.min(100L, local5342_2 - this.field11543) / 60.0);
      this.field11543 = local5342_2;
      Slot slot = ((HandledScreenMixin)containerRenderEvent.method3212()).hoveredSlot();
      if (slot != null && this.field11542.size() < 256) {
         this.field11542.putIfAbsent(slot, 0.0F);
      }

      this.field11542.entrySet().removeIf(local5336_1 -> local5336_1.getValue() < 0.004F && local5336_1.getKey() != slot);

      for (Entry<Slot, Float> local5342_7 : this.field11542.entrySet()) {
         local5342_7.setValue(local5342_7.getValue() + ((local5342_7.getKey() == slot ? 1.0F : 0.0F) - local5342_7.getValue()) * local5342_4);
      }
   };
   private final EventListener<PacketEvent> field11553 = packetEvent -> {
      if (mc.player != null
         && this.field11541.isChecked()
         && packetEvent.isIncoming()
         && packetEvent.getPacket() instanceof PlayerPositionLookS2CPacket playerPositionLookS2CPacket) {
         Vec3d vec3d = EntityPosition.apply(EntityPosition.fromEntity(mc.player), playerPositionLookS2CPacket.change(), playerPositionLookS2CPacket.relatives()).position();
         double local5334_4 = mc.player.getEntityPos().distanceTo(vec3d);
         if (!(local5334_4 < 0.05) && !(local5334_4 > 24.0)) {
            this.field11549 = mc.player.getEntityPos().subtract(vec3d);
            this.field11550 = System.currentTimeMillis();
            this.field11551 = (long)MathUtils.method3827(160.0F, 420.0F, 140.0F + (float)local5334_4 * 40.0F);
         }
      }
   };

   public void method3274(DrawContext drawContext, Slot slot) {
      this.field11544 = false;
      if (this.isEnabled() && this.field11538.isChecked()) {
         Float local5333_3 = this.field11542.get(slot);
         if (local5333_3 != null && !(local5333_3 < 0.004F)) {
            this.field11544 = true;
            float local5333_4 = 1.0F + 0.13F * local5333_3;
            float local5333_5 = slot.x + 8.0F;
            float local5333_6 = slot.y + 8.0F;
            drawContext.getMatrices().pushMatrix();
            drawContext.getMatrices().translate(local5333_5, local5333_6);
            drawContext.getMatrices().scale(local5333_4, local5333_4);
            drawContext.getMatrices().translate(-local5333_5, -local5333_6);
         }
      }
   }

   public void method3276() {
      this.field11547 = System.currentTimeMillis();
   }

   private SmoothFeatureModule() {
      this.field11538 = this.addSetting(
         new BooleanSetting("Увеличение предмета под курсором", "Плавно подращивает предмет при наводке во всех экранах и шалкерах.", true)
      );
      this.field11539 = this.addSetting(new BooleanSetting("Плавный чат", "Новые сообщения мягко выезжают снизу, а не появляются рывком.", true));
      this.field11540 = this.addSetting(new BooleanSetting("Антиспам чата", "Повторы одного сообщения не плодятся, а помечаются счётчиком (x2).", true));
      this.field11541 = this.addSetting(
         new BooleanSetting("Плавная камера при флагах", "Откат позиции сервером не дёргает камеру, а плавно доводит её.", false)
      );
      ModuleManager.unregisterModule(this);
      this.setEnabledWithoutCallbacks(true);
   }

   @Override
   protected void onDisable() {
      this.field11542.clear();
      this.field11544 = this.field11548 = false;
      this.field11545 = "";
      this.field11546 = 1;
      this.field11549 = null;
   }

   public static SmoothFeatureModule method3278() {
      if (smoothFeatureModule == null) {
         smoothFeatureModule = new SmoothFeatureModule();
      }

      return smoothFeatureModule;
   }

   public void method3279(DrawContext drawContext) {
      if (this.field11544) {
         this.field11544 = false;
         drawContext.getMatrices().popMatrix();
      }
   }

   public void method3280(DrawContext drawContext) {
      if (this.field11548) {
         this.field11548 = false;
         drawContext.getMatrices().popMatrix();
      }
   }

   public boolean method3282() {
      return this.isEnabled()
         && this.field11541.isChecked()
         && this.field11549 != null
         && System.currentTimeMillis() - this.field11550 < this.field11551
         && !FreeCameraModule.method0910().method0913();
   }

   public Vec3d method3283() {
      float local5344_1 = MathUtils.clampUnit((float)(System.currentTimeMillis() - this.field11550) / (float)this.field11551);
      return this.field11549.multiply((1.0F - local5344_1) * (1.0F - local5344_1) * (1.0F - local5344_1));
   }

   public void method3284(ChatHud chatHud, DrawContext drawContext) {
      this.field11548 = false;
      if (this.isEnabled() && this.field11539.isChecked() && ((ChatHudMixin)chatHud).chatScrollbarPos() == 0) {
         float local5345_3 = MathUtils.clampUnit((float)(System.currentTimeMillis() - this.field11547) / 240.0F);
         if (!(local5345_3 >= 1.0F)) {
            float local5345_4 = 1.0F - (1.0F - local5345_3) * (1.0F - local5345_3) * (1.0F - local5345_3);
            this.field11548 = true;
            drawContext.getMatrices().pushMatrix();
            drawContext.getMatrices().translate(0.0F, 9.0F * mc.options.getChatScale().getValue().floatValue() * (1.0F - local5345_4));
         }
      }
   }

   public boolean method3285(ChatHud chatHud, Text text) {
      if (this.isEnabled() && this.field11540.isChecked()) {
         String string = text.getString();
         List<ChatHudLine> local5346_4 = ((ChatHudMixin)chatHud).allMessages();
         if (!string.isBlank() && !local5346_4.isEmpty() && string.equals(this.field11545)) {
            this.field11546++;
            ChatHudLine chatHudLine = local5346_4.get(0);
            local5346_4.set(
               0,
               new ChatHudLine(
                  chatHudLine.creationTick(),
                  text.copy().append(Text.literal(" (x" + this.field11546 + ")").formatted(Formatting.GRAY)),
                  chatHudLine.signature(),
                  chatHudLine.indicator()
               )
            );
            ((ChatHudMixin)chatHud).invokeRefreshTrimmedMessages();
            this.method3276();
            return true;
         } else {
            this.field11545 = string;
            this.field11546 = 1;
            return false;
         }
      } else {
         return false;
      }
   }
}
