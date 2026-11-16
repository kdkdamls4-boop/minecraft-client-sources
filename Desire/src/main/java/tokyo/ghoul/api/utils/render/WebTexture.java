package tokyo.ghoul.api.utils.render;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import tokyo.ghoul.api.auth.HttpUtils;

import java.io.ByteArrayInputStream;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public final class WebTexture {
    private static final Map<String, Identifier> READY = new ConcurrentHashMap<>();
    private static final Set<String> PENDING = ConcurrentHashMap.newKeySet();
    private static final Map<String, Long> FAILED = new ConcurrentHashMap<>();
    private static final long RETRY_MS = 30_000L;

    private WebTexture() {}

    public static Identifier get(String url) {
        if (url == null) {
            return null;
        }
        Identifier ready = READY.get(url);
        if (ready != null) {
            return ready;
        }
        Long failedAt = FAILED.get(url);
        if (failedAt != null && System.currentTimeMillis() - failedAt < RETRY_MS) {
            return null;
        }
        if (PENDING.add(url)) {
            CompletableFuture.runAsync(() -> {
                try {
                    NativeImage image = NativeImage.read(new ByteArrayInputStream(HttpUtils.download(url)));
                    Minecraft.getInstance().execute(() -> {
                        try {
                            Identifier id = Identifier.fromNamespaceAndPath("desire",
                                    "webtexture/" + Integer.toHexString(url.hashCode()));
                            Minecraft.getInstance().getTextureManager().register(id, new DynamicTexture(id::toString, image));
                            READY.put(url, id);
                            FAILED.remove(url);
                        } catch (Exception ignored) {
                            fail(url);
                        }
                    });
                } catch (Exception ignored) {
                    fail(url);
                }
            });
        }
        return null;
    }

    private static void fail(String url) {
        FAILED.put(url, System.currentTimeMillis());
        PENDING.remove(url);
    }
}
