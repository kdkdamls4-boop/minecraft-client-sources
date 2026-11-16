package net.skycore.client.module.render;

import java.util.List;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.skycore.client.SkycoreClient;
import net.skycore.client.integration.PartyClient;
import net.skycore.client.integration.PartyMember;
import net.skycore.client.module.ClientModule;
import net.skycore.client.module.ModuleCategory;
import net.skycore.client.util.BeginFrameHelper;
import net.skycore.client.util.CaptureLevelProjectionHelper;
import net.skycore.client.util.CurrentGuiGraphicsExtractorHelper;
import net.skycore.client.util.InterBoldHelper;
import net.skycore.client.util.TextHeightHelper;
import net.skycore.client.waypoint.WaypointManager;

public final class WaypointModule extends ClientModule {
   private static final Identifier PARTY_WAYPOINT = Identifier.fromNamespaceAndPath("skycore", "textures/hud/party-waypoint.png");

   private void drawArrow(Minecraft minecraft, GuiGraphicsExtractor guiGraphicsExtractor, WaypointManager.Waypoint waypoint) {
      Vec3 position = minecraft.gameRenderer.gameRenderState().levelRenderState.cameraRenderState.pos;
      double x = waypoint.x() - position.x;
      double z = waypoint.z() - position.z;
      double yRot = Math.toRadians(minecraft.player.getYRot());
      float computedValue = (float)Math.atan2(-(z * Math.cos(yRot) - x * Math.sin(yRot)), -(x * Math.cos(yRot) + z * Math.sin(yRot)));
      float window = minecraft.getWindow().getGuiScaledWidth() * 0.5F;
      float window2 = minecraft.getWindow().getGuiScaledHeight() * 0.5F - 75.0F;
      guiGraphicsExtractor.pose().pushMatrix();
      guiGraphicsExtractor.pose().translate(window, window2);
      guiGraphicsExtractor.pose().rotate(computedValue);
      BeginFrameHelper.texture(-9.0F, -9.0F, 18.0F, 18.0F, ArrowsModule.ARROW_TEXTURE).draw();
      BeginFrameHelper.flush();
      guiGraphicsExtractor.pose().popMatrix();
      String x2 = Math.round(Math.hypot(waypoint.x() - minecraft.player.getX(), waypoint.z() - minecraft.player.getZ())) + "m";
      TextHeightHelper textHeightHelper = InterBoldHelper.sfSemibold();
      BeginFrameHelper.text(window - textHeightHelper.getWidth(x2, 7.0F) * 0.5F, window2 - 15.5F, 7.0F, x2).font(textHeightHelper).color(-1).draw();
   }

   private void drawPartyLabel(float width, float coordinate, String text) {
      TextHeightHelper textHeightHelper = InterBoldHelper.forWeight(500);
      float width2 = width - textHeightHelper.getWidth(text, 11.0F) * 0.5F;
      BeginFrameHelper.TextBuilder textBuilder = BeginFrameHelper.text(width2, coordinate, 11.0F, text);
      BeginFrameHelper.TextBuilder textBuilder2 = textBuilder.font(textHeightHelper);
      textBuilder2.color(-1).draw();
   }

   private void drawPartyTag(Minecraft minecraft, PartyMember partyMember) {
      Player player = null;

      for (Player player2 : minecraft.level.players()) {
         if (player2.getGameProfile().name().equalsIgnoreCase(partyMember.getUsername())) {
            player = player2;
            break;
         }
      }

      double x = player != null ? player.getX() : partyMember.getX();
      double y = player != null ? player.getY() + player.getBbHeight() + 0.35 : partyMember.getY() + 1.8;
      double z = player != null ? player.getZ() : partyMember.getZ();
      Vec3 position = minecraft.gameRenderer.gameRenderState().levelRenderState.cameraRenderState.pos;
      Vec3 position2 = new Vec3(x, y, z).subtract(position);
      double length = position2.length();
      if (!(length < 0.001)) {
         CaptureLevelProjectionHelper.ScreenPoint screenPoint = CaptureLevelProjectionHelper.projectToScreen(
            minecraft, position.add(position2.normalize().scale(Math.min(length, 64.0)))
         );
         if (screenPoint != null) {
            TextHeightHelper textHeightHelper = InterBoldHelper.forWeight(500);
            String username = partyMember.getUsername();
            String x2 = Math.round(Math.hypot(x - minecraft.player.getX(), z - minecraft.player.getZ())) + "m";
            float value = 24.0F;
            float y2 = screenPoint.y() - 36.0F;
            BeginFrameHelper.texture(screenPoint.x() - value * 0.5F, y2 - value * 0.5F, value, value, PARTY_WAYPOINT).draw();
            float height = y2 + value * 0.5F + 3.0F;
            this.drawPartyLabel(screenPoint.x(), height, x2);
            this.drawPartyLabel(screenPoint.x(), height + textHeightHelper.textHeight(11.0F), username);
         }
      }
   }

   public WaypointModule() {
      super("Waypoint", "GPS: стрелка направления", ModuleCategory.RENDER);
      ClientModule.runSilently(() -> {
         this.setEnabled(true);
      });
   }

   public void render(GuiRenderState guiRenderState, DeltaTracker deltaTracker) {
      Minecraft instance = Minecraft.getInstance();
      if (this.isEnabled() && instance.player != null && instance.level != null) {
         WaypointManager.Waypoint waypoint = SkycoreClient.waypoints().resolveActive();
         var membersForRender = PartyClient.getMembersForRender();
         if (waypoint != null || !membersForRender.isEmpty()) {
            GuiGraphicsExtractor guiGraphicsExtractor = new GuiGraphicsExtractor(
               instance, guiRenderState, (int)instance.mouseHandler.getScaledXPos(instance.getWindow()), (int)instance.mouseHandler.getScaledYPos(instance.getWindow())
            );
            CurrentGuiGraphicsExtractorHelper.enter2D(guiGraphicsExtractor, deltaTracker);
            BeginFrameHelper.beginFrame();

            try {
               for (PartyMember partyMember : membersForRender) {
                  this.drawPartyTag(instance, partyMember);
               }

               if (waypoint != null) {
                  this.drawArrow(instance, guiGraphicsExtractor, waypoint);
               }

               BeginFrameHelper.flush();
            } finally {
               CurrentGuiGraphicsExtractorHelper.exit2D();
            }
         }
      }
   }
}
