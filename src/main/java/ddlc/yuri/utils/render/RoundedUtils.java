package ddlc.yuri.utils.render;

import ddlc.yuri.utils.render.shader.RoundedShaderUtils;
import ddlc.yuri.utils.render.shader.ShaderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

import java.awt.*;

import static org.lwjgl.opengl.GL11.*;

public class RoundedUtils extends RenderUtils {
    public static RoundedShaderUtils roundedShader = new RoundedShaderUtils("roundedRect");
    public static RoundedShaderUtils roundedOutlineShader = new RoundedShaderUtils("roundRectOutline");

    private static void setupRoundedRectUniforms(float x, float y, float width, float height, float radius, RoundedShaderUtils roundedTexturedShader) {
        ScaledResolution sr = new ScaledResolution(Minecraft.getMinecraft());
        roundedTexturedShader.setUniformf("location", x * sr.getScaleFactor(),
                (Minecraft.getMinecraft().displayHeight - (height * sr.getScaleFactor())) - (y * sr.getScaleFactor()));
        roundedTexturedShader.setUniformf("rectSize", width * sr.getScaleFactor(), height * sr.getScaleFactor());
        roundedTexturedShader.setUniformf("radius", radius * sr.getScaleFactor());
    }


    public static void drawRoundedRect(float x, float y, float width, float height, float radius, Color color) {
        drawRoundedRect(x, y, width, height, radius, false, color);
    }

    public static void drawRoundedRect(float x, float y, float width, float height, float radius, boolean blur, Color color) {
        GlStateManager.resetColor();
        GlStateManager.enableBlend();
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.enableAlpha();
        GlStateManager.alphaFunc(GL_GREATER, (float) (0 * .01));
        roundedShader.init();

        setupRoundedRectUniforms(x, y, width, height, radius, roundedShader);
        roundedShader.setUniformi("blur", blur ? 1 : 0);
        roundedShader.setUniformf("color", color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f, color.getAlpha() / 255f);

        RoundedShaderUtils.drawQuads(x - 1, y - 1, width + 2, height + 2);
        roundedShader.unload();
        GlStateManager.disableBlend();
    }

    public static void drawRoundedRect(double x, double y, double width, double height, double radius, Color color) {
        drawRoundedRect(x, y, width, height, radius, false, color);
    }

    public static void drawRoundedRect(double x, double y, double width, double height, double radius, boolean blur, Color color) {
        GlStateManager.resetColor();
        GlStateManager.enableBlend();
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.enableAlpha();
        GlStateManager.alphaFunc(GL_GREATER, (float) (0 * .01));
        roundedShader.init();

        setupRoundedRectUniforms((float) x, (float) y, (float) width, (float) height, (float) radius, roundedShader);
        roundedShader.setUniformi("blur", blur ? 1 : 0);
        roundedShader.setUniformf("color", color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f, color.getAlpha() / 255f);

        RoundedShaderUtils.drawQuads((float) x - 1, (float) y - 1, (float) width + 2,  (float) height + 2);
        roundedShader.unload();
        GlStateManager.disableBlend();
    }

    public static void drawRoundOutline(float x, float y, float width, float height, float radius, float outlineThickness, Color color, Color outlineColor) {
        resetColor();
        GLUtils.startBlend();
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        setAlphaLimit(0);
        roundedOutlineShader.init();

        ScaledResolution sr = new ScaledResolution(Minecraft.getMinecraft());
        setupRoundedRectUniforms(x, y, width, height, radius, roundedOutlineShader);
        roundedOutlineShader.setUniformf("outlineThickness", outlineThickness * sr.getScaleFactor());
        roundedOutlineShader.setUniformf("color", color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f, color.getAlpha() / 255f);
        roundedOutlineShader.setUniformf("outlineColor", outlineColor.getRed() / 255f, outlineColor.getGreen() / 255f, outlineColor.getBlue() / 255f, outlineColor.getAlpha() / 255f);


        ShaderUtils.drawQuads(x - (2 + outlineThickness), y - (2 + outlineThickness), width + (4 + outlineThickness * 2), height + (4 + outlineThickness * 2));
        roundedOutlineShader.unload();
        GLUtils.endBlend();
    }

    public static void drawCustomRoundOutline(float x, float y, float width, float height, float radius, float outlineThickness,
                                              boolean topLeft, boolean topRight, boolean bottomRight, boolean bottomLeft,
                                              Color color, Color outlineColor) {
        resetColor();
        GLUtils.startBlend();
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        setAlphaLimit(0);
        roundedOutlineShader.init();

        ScaledResolution sr = new ScaledResolution(Minecraft.getMinecraft());
        setupRoundedRectUniforms(x, y, width, height, radius, roundedOutlineShader);
        roundedOutlineShader.setUniformf("outlineThickness", outlineThickness * sr.getScaleFactor());
        roundedOutlineShader.setUniformf("color", color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f, color.getAlpha() / 255f);
        roundedOutlineShader.setUniformf("outlineColor", outlineColor.getRed() / 255f, outlineColor.getGreen() / 255f, outlineColor.getBlue() / 255f, outlineColor.getAlpha() / 255f);
        roundedOutlineShader.setUniformf("corners",
                topLeft ? 1.0f : 0.0f,
                topRight ? 1.0f : 0.0f,
                bottomRight ? 1.0f : 0.0f,
                bottomLeft ? 1.0f : 0.0f
        );

        ShaderUtils.drawQuads(x - (2 + outlineThickness), y - (2 + outlineThickness), width + (4 + outlineThickness * 2), height + (4 + outlineThickness * 2));
        roundedOutlineShader.unload();
        GLUtils.endBlend();
    }

    public static void drawCustomRoundOutline(double x, double y, double width, double height, double radius, double outlineThickness,
                                              boolean topLeft, boolean topRight, boolean bottomRight, boolean bottomLeft,
                                              Color color, Color outlineColor) {
        drawCustomRoundOutline((float) x, (float) y, (float) width, (float) height, (float) radius, (float) outlineThickness,
                topLeft, topRight, bottomRight, bottomLeft, color, outlineColor);
    }

    public static void drawCustomRoundedRect(float x, float y, float width, float height, float radius,
                                             boolean topLeft, boolean topRight, boolean bottomRight, boolean bottomLeft,
                                             Color color) {
        drawCustomRoundedRect(x, y, width, height, radius, false, topLeft, topRight, bottomRight, bottomLeft, color);
    }

    public static void drawCustomRoundedRect(float x, float y, float width, float height, float radius, boolean blur,
                                             boolean topLeft, boolean topRight, boolean bottomRight, boolean bottomLeft,
                                             Color color) {
        GlStateManager.resetColor();
        GlStateManager.enableBlend();
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.enableAlpha();
        GlStateManager.alphaFunc(GL_GREATER, (float) (0 * .01));

        roundedShader.init();

        setupRoundedRectUniforms(x, y, width, height, radius, roundedShader);

        roundedShader.setUniformf("corners",
                topLeft ? 1.0f : 0.0f,
                topRight ? 1.0f : 0.0f,
                bottomRight ? 1.0f : 0.0f,
                bottomLeft ? 1.0f : 0.0f
        );

        roundedShader.setUniformi("blur", blur ? 1 : 0);
        roundedShader.setUniformf("color", color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f, color.getAlpha() / 255f);

        RoundedShaderUtils.drawQuads(x - 1, y - 1, width + 2, height + 2);
        roundedShader.unload();
        GlStateManager.disableBlend();
    }

    public static void drawCustomRoundedRect(double x, double y, double width, double height, double radius,
                                             boolean topLeft, boolean topRight, boolean bottomRight, boolean bottomLeft,
                                             Color color) {
        drawCustomRoundedRect((float) x, (float) y, (float) width, (float) height, (float) radius,
                topLeft, topRight, bottomRight, bottomLeft, color);
    }

    public static void drawRoundedImage(ResourceLocation resourceLocation, float x, float y, float imgWidth, float imgHeight, float radius) {
        GL11.glEnable(GL11.GL_STENCIL_TEST);
        GL11.glClear(GL11.GL_STENCIL_BUFFER_BIT);
        GL11.glColorMask(false, false, false, false);
        GL11.glStencilFunc(GL11.GL_ALWAYS, 1, 0xFF);
        GL11.glStencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_REPLACE);

        RoundedUtils.drawRoundedRect(x, y, imgWidth, imgHeight, radius, Color.WHITE);

        GL11.glColorMask(true, true, true, true);
        GL11.glStencilFunc(GL11.GL_EQUAL, 1, 0xFF);
        GL11.glStencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_KEEP);

        drawImage(resourceLocation, x, y, imgWidth, imgHeight);

        GL11.glDisable(GL11.GL_STENCIL_TEST);
    }

    public static void drawRoundedImage(ResourceLocation resourceLocation, float x, float y, float croppedX, float croppedY, float croppedWidth, float croppedHeight, float imgWidth, float imgHeight, float radius) {
        GL11.glEnable(GL11.GL_STENCIL_TEST);
        GL11.glClear(GL11.GL_STENCIL_BUFFER_BIT);
        GL11.glColorMask(false, false, false, false);
        GL11.glStencilFunc(GL11.GL_ALWAYS, 1, 0xFF);
        GL11.glStencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_REPLACE);

        RoundedUtils.drawRoundedRect(x, y, imgWidth, imgHeight, radius, Color.WHITE);

        GL11.glColorMask(true, true, true, true);
        GL11.glStencilFunc(GL11.GL_EQUAL, 1, 0xFF);
        GL11.glStencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_KEEP);

        drawImage(resourceLocation, x, y, croppedX, croppedY, croppedWidth, croppedHeight, imgWidth, imgHeight);

        GL11.glDisable(GL11.GL_STENCIL_TEST);
    }

    public static void drawRoundedGif(GifTexture gif, float x, float y, float imgWidth, float imgHeight, float radius) {
        if (gif == null) return;
        drawRoundedImage(gif.getCurrentFrame(), x, y, imgWidth, imgHeight, radius);
    }
}