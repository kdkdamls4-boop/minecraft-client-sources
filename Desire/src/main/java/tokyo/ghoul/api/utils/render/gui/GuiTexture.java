package tokyo.ghoul.api.utils.render.gui;

import com.github.weisj.jsvg.SVGDocument;
import com.github.weisj.jsvg.parser.LoaderContext;
import com.github.weisj.jsvg.parser.SVGLoader;
import com.github.weisj.jsvg.view.FloatSize;
import com.github.weisj.jsvg.view.ViewBox;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import tokyo.ghoul.api.utils.render.pipeline.EvaTextures;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class GuiTexture {
    private static final Map<Identifier, GuiTexture> CACHE = new ConcurrentHashMap<>();
    private static final int MAX_VARIANTS = 8;
    private static final int SUPERSAMPLE = 2;
    private static final int MAX_TEXTURE_SIZE = 2048;

    private final Identifier textureId;
    private final boolean svg;
    private volatile SVGDocument document;
    private TextureSetup staticSetup;
    private final LinkedHashMap<Long, TextureSetup> svgVariants = new LinkedHashMap<>(8, 0.75F, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Long, TextureSetup> eldest) {
            if (size() <= MAX_VARIANTS) {
                return false;
            }
            Minecraft.getInstance().getTextureManager().release(variantId(GuiTexture.this.textureId, eldest.getKey()));
            return true;
        }
    };

    private GuiTexture(Identifier textureId) {
        this.textureId = textureId;
        this.svg = textureId.getPath().endsWith(".svg");
    }

    public static GuiTexture load(Identifier textureId) {
        return CACHE.computeIfAbsent(textureId, GuiTexture::new);
    }

    public TextureSetup textureSetup(float deviceWidth, float deviceHeight) {
        if (!this.svg) {
            return staticSetup();
        }
        long key = packSize(quantize(deviceWidth), quantize(deviceHeight));
        TextureSetup cached = this.svgVariants.get(key);
        if (cached != null) {
            return cached;
        }
        TextureSetup created = createSvgSetup(key);
        this.svgVariants.put(key, created);
        return created;
    }

    public TextureSetup textureSetup() {
        if (!this.svg) {
            return staticSetup();
        }
        FloatSize size = document().size();
        return textureSetup((float) size.getWidth(), (float) size.getHeight());
    }

    private TextureSetup staticSetup() {
        if (this.staticSetup == null) {
            DynamicTexture texture = new DynamicTexture(() -> this.textureId.toString(), readPng(this.textureId));
            texture.upload();
            Minecraft.getInstance().getTextureManager().register(this.textureId, texture);
            this.staticSetup = TextureSetup.singleTexture(texture.getTextureView(), EvaTextures.sampler());
        }
        return this.staticSetup;
    }

    private TextureSetup createSvgSetup(long key) {
        int width = Math.min(MAX_TEXTURE_SIZE, unpackWidth(key) * SUPERSAMPLE);
        int height = Math.min(MAX_TEXTURE_SIZE, unpackHeight(key) * SUPERSAMPLE);
        Identifier variantId = variantId(this.textureId, key);
        DynamicTexture texture = new DynamicTexture(variantId::toString, rasterizeSvg(document(), width, height));
        texture.upload();
        Minecraft.getInstance().getTextureManager().register(variantId, texture);
        return TextureSetup.singleTexture(texture.getTextureView(), EvaTextures.sampler());
    }

    private SVGDocument document() {
        SVGDocument loaded = this.document;
        if (loaded == null) {
            loaded = loadDocument(this.textureId);
            this.document = loaded;
        }
        return loaded;
    }

    private static int quantize(float devicePx) {
        return Math.max(1, (int) Math.ceil(devicePx));
    }

    private static long packSize(int width, int height) {
        return ((long) width << 32) | (height & 0xFFFFFFFFL);
    }

    private static int unpackWidth(long key) {
        return (int) (key >>> 32);
    }

    private static int unpackHeight(long key) {
        return (int) key;
    }

    private static Identifier variantId(Identifier base, long key) {
        return base.withPath(path -> path + "/" + unpackWidth(key) + "x" + unpackHeight(key));
    }

    private static NativeImage readPng(Identifier textureId) {
        try (InputStream inputStream = open(textureId)) {
            return NativeImage.read(inputStream);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read GUI texture: " + textureId, exception);
        }
    }

    private static SVGDocument loadDocument(Identifier textureId) {
        try (InputStream inputStream = open(textureId)) {
            URI documentUri = URI.create("resource://" + textureId.getNamespace() + "/" + textureId.getPath());
            SVGDocument document = new SVGLoader().load(inputStream, documentUri, LoaderContext.createDefault());
            if (document == null) {
                throw new IOException("Failed to parse SVG document: " + textureId);
            }
            return document;
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read GUI texture: " + textureId, exception);
        }
    }

    private static InputStream open(Identifier textureId) throws IOException {
        Minecraft mc = Minecraft.getInstance();
        try {
            return mc.getResourceManager().open(textureId);
        } catch (IOException exception) {
            String resourcePath = "/assets/" + textureId.getNamespace() + "/" + textureId.getPath();
            InputStream classpath = GuiTexture.class.getResourceAsStream(resourcePath);
            if (classpath != null) {
                return classpath;
            }
            throw exception;
        }
    }

    private static NativeImage rasterizeSvg(SVGDocument document, int width, int height) {
        BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB_PRE);
        Graphics2D graphics = bufferedImage.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            graphics.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
            graphics.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            graphics.setColor(Color.WHITE);
            graphics.setPaint(Color.WHITE);
            document.render(null, graphics, new ViewBox(0, 0, width, height));
        } finally {
            graphics.dispose();
        }

        NativeImage nativeImage = new NativeImage(NativeImage.Format.RGBA, width, height, false);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int argb = bufferedImage.getRGB(x, y);
                if (((argb >> 24) & 0xFF) == 0) {
                    argb = 0x00FFFFFF;
                }
                nativeImage.setPixel(x, y, argb);
            }
        }
        return nativeImage;
    }
}
