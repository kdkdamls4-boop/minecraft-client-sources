package net.skycore.client.module.render;

import java.util.List;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.skycore.client.SkycoreClient;
import net.skycore.client.config.FriendManager;
import net.skycore.client.module.ClientModule;
import net.skycore.client.module.ModuleCategory;
import net.skycore.client.module.combat.AntiBotModule;
import net.skycore.client.util.AddQuadHelper;
import net.skycore.client.util.CameraViewPoseHelper;
import net.skycore.client.setting.ColorSetting;
import net.skycore.client.setting.NumberSetting;
import net.skycore.client.setting.ModeSetting;
import net.skycore.client.setting.MultiSelectSetting;
import net.skycore.client.setting.BooleanSetting;

public final class Box3DModule extends ClientModule {
   private final ColorSetting customColor;
   private final NumberSetting outlineAlpha;
   public static final Box3DModule INSTANCE = new Box3DModule();
   private final BooleanSetting friendColor;
   private final MultiSelectSetting targets = this.add(new MultiSelectSetting("Targets", List.of("Self", "Friends", "Players"), Set.of("Players")));
   private final NumberSetting fillAlpha;
   private final ModeSetting colorMode;
   private final BooleanSetting fill;

   public void renderWorld() {
      if (this.isEnabled()) {
         Minecraft instance = Minecraft.getInstance();
         if (instance.level != null && instance.player != null && instance.gameRenderer != null) {
            int color = this.colorMode.is("Custom") ? this.customColor.get() : SkycoreClient.themes().accent();
            if (!(this.outlineAlpha.get() <= 0.0) || this.fill.get() && !(this.fillAlpha.get() <= 0.0)) {
               float gameTimeDeltaPartialTick = instance.getDeltaTracker().getGameTimeDeltaPartialTick(true);
               AddQuadHelper.beginFrame();

               for (Player player : instance.level.players()) {
                  int value = player == instance.player ? 1 : 0;
                  if ((value == 0 || !instance.options.getCameraType().isFirstPerson())
                     && player.isAlive()
                     && !player.isRemoved()
                     && !player.isSpectator()
                     && (value != 0 || !AntiBotModule.isBot(player))) {
                     int gameProfile = value == 0 && FriendManager.INSTANCE.isFriend(player.getGameProfile().name()) ? 1 : 0;
                     if (value != 0 ? this.targets.isSelected("Self") : (gameProfile != 0 ? this.targets.isSelected("Friends") : this.targets.isSelected("Players"))) {
                        int alpha = gameProfile != 0 && this.friendColor.get() ? -11141291 : color;
                        int value2 = withAlpha(alpha, this.outlineAlpha.get());
                        int value3 = withAlpha(alpha, this.fillAlpha.get());
                        Vec3 position = CameraViewPoseHelper.interpolatedPosition(player, gameTimeDeltaPartialTick).subtract(player.position());
                        AABB boundingBox = player.getBoundingBox().move(position).inflate(0.025, 0.025, 0.025);
                        if (this.fill.get() && value3 >>> 24 != 0) {
                           AddQuadHelper.addFilledBox(boundingBox, value3);
                        }

                        if (value2 >>> 24 != 0) {
                           AddQuadHelper.addScreenSpaceBoxOutline(boundingBox, value2);
                        }
                     }
                  }
               }

               AddQuadHelper.endFrame();
            }
         }
      }
   }

   private static int withAlpha(int value, double value2) {
      int computedValue = (int)Math.round(Math.max(0.0, Math.min(1.0, value2)) * 255.0);
      return computedValue << 24 | value & 16777215;
   }

   private Box3DModule() {
      super("3DBox", "3D player boxes with a uniform outline and flush fill", ModuleCategory.RENDER);
      this.friendColor = this.add(new BooleanSetting("Friend Color", true));
      this.colorMode = this.add(new ModeSetting("Color", "Sync", "Sync", "Custom"));
      this.customColor = this.add(new ColorSetting("Custom Color", -1, false));
      this.outlineAlpha = this.add(new NumberSetting("Outline Alpha", "", 0.95, 0.0, 1.0, 0.05, ""));
      this.fill = this.add(new BooleanSetting("Fill", true));
      this.fillAlpha = this.add(new NumberSetting("Fill Alpha", "", 0.23, 0.0, 1.0, 0.05, ""));
      this.customColor.visibleWhen(() -> {
         return this.colorMode.is("Custom");
      });
      this.friendColor.visibleWhen(() -> {
         return this.targets.isSelected("Friends");
      });
      this.fillAlpha.visibleWhen(this.fill::get);
   }
}
