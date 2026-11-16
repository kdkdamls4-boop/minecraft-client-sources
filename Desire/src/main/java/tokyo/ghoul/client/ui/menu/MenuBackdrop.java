package tokyo.ghoul.client.ui.menu;

import com.mojang.blaze3d.vertex.PoseStack;
import lombok.experimental.UtilityClass;
import tokyo.ghoul.api.utils.render.RenderUtil;

import java.awt.Color;

@UtilityClass
public class MenuBackdrop {
    public void draw(PoseStack matrices, float sw, float sh, float alpha) {
        int a = (int) Math.max(0f, Math.min(255f, alpha));
        if (a <= 0 || MenuWallpapers.size() == 0) return;

        drawCover(matrices, MenuWallpapers.selected(), sw, sh, a);

        RenderUtil.GRADIENT_RECT.draw(matrices, 0f, 0f, sw, sh, 0f,
                new Color(5, 7, 13, 168), new Color(5, 7, 13, 168),
                new Color(3, 5, 11, 104), new Color(3, 5, 11, 104));

        float scrimWidth = Math.min(sw * 0.5f, 660f);
        RenderUtil.GRADIENT_RECT.draw(matrices, 0f, 0f, scrimWidth, sh, 0f,
                new Color(2, 4, 9, 232), new Color(2, 4, 9, 0),
                new Color(1, 2, 7, 246), new Color(1, 2, 7, 0));
    }

    public void drawCover(PoseStack matrices, MenuWallpapers.Wallpaper wallpaper, float sw, float sh, int alpha) {
        if (wallpaper == null || alpha <= 0) return;

        float screen = sw / Math.max(1f, sh);
        float u, v, uvWidth, uvHeight;
        if (screen > wallpaper.aspect()) {
            u = 0f;
            uvWidth = 1f;
            uvHeight = wallpaper.aspect() / screen;
            v = (1f - uvHeight) * 0.5f;
        } else {
            v = 0f;
            uvHeight = 1f;
            uvWidth = screen / wallpaper.aspect();
            u = (1f - uvWidth) * 0.5f;
        }

        RenderUtil.TEXTURE_RECT.drawSmooth(matrices, 0f, 0f, sw, sh, 0f,
                new Color(255, 255, 255, alpha), u, v, uvWidth, uvHeight, wallpaper.image());
    }
}
