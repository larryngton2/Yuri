package net.minecraft.client.gui;

import ddlc.yuri.api.font.CustomFontRenderer;
import ddlc.yuri.api.gui.click.novoline.GuiTheme;
import ddlc.yuri.managers.impl.ColorManager;
import ddlc.yuri.utils.render.FontUtils;
import ddlc.yuri.utils.render.RenderUtils;
import ddlc.yuri.utils.render.RoundedUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.audio.SoundHandler;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;

import java.awt.*;

public class GuiButton extends Gui
{
    private static final Color BG_COLOR = new Color(0, 0, 0, 130);
    private static final Color DISABLED_TEXT_COLOR = new Color(120, 120, 120, 180);

    protected int width;
    protected int height;
    public int xPosition;
    public int yPosition;
    public String displayString;
    public int id;
    public boolean enabled;
    public boolean visible;
    protected boolean hovered;
    public float hoverAnim;

    public GuiButton(int buttonId, int x, int y, String buttonText)
    {
        this(buttonId, x, y, 200, 20, buttonText);
    }

    public GuiButton(int buttonId, int x, int y, int widthIn, int heightIn, String buttonText)
    {
        this.width = widthIn;
        this.height = heightIn;
        this.enabled = true;
        this.visible = true;
        this.id = buttonId;
        this.xPosition = x;
        this.yPosition = y;
        this.displayString = buttonText;
    }

    protected int getHoverState(boolean mouseOver)
    {
        int i = 1;

        if (!this.enabled)
        {
            i = 0;
        }
        else if (mouseOver)
        {
            i = 2;
        }

        return i;
    }

    public void drawButton(Minecraft mc, int mouseX, int mouseY)
    {
        if (this.visible)
        {
            this.hovered = mouseX >= this.xPosition && mouseY >= this.yPosition && mouseX < this.xPosition + this.width && mouseY < this.yPosition + this.height;

            // Smooth hover animation calculation matching MenuButton
            float target = (this.hovered && this.enabled) ? 1.00f : 0.00f;
            float speed = 12f / 1000f;
            this.hoverAnim += (target - this.hoverAnim) * (1f - (float) Math.exp(-speed * RenderUtils.delta));

            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);

            // Draw modern background box & theme outline
            Color outlineColor = this.enabled ? ColorManager.getColor() : new Color(60, 60, 60, 180);
            RoundedUtils.drawRoundOutline(this.xPosition, this.yPosition, this.width, this.height, 6f, -0.5f, hovered ? BG_COLOR.brighter().brighter().brighter().brighter().brighter().brighter() : BG_COLOR, outlineColor);

            // Draw animated hover underline expansion
            if (this.hoverAnim > 0.001f && this.enabled)
            {
                float ease = 1f - (1f - this.hoverAnim) * (1f - this.hoverAnim);
                float animatedWidth = this.width * ease;
                float animatedX = this.xPosition + (this.width - animatedWidth) / 2f;
                float animatedHeight = 1.5f;
                float animatedY = this.yPosition + this.height - animatedHeight;

                RoundedUtils.drawRoundedRect(animatedX, animatedY, animatedWidth, animatedHeight, 0.2f, ColorManager.getColor());
            }

            this.mouseDragged(mc, mouseX, mouseY);

            // Text color interpolation
            int textColor = !this.enabled
                    ? DISABLED_TEXT_COLOR.getRGB()
                    : RenderUtils.interpolateColor(GuiTheme.TEXT, ColorManager.getColor(), this.hoverAnim);

            // Render text with custom font (fallback to vanilla font renderer if null)
            CustomFontRenderer fr = FontUtils.getFont("sf", 18);
            if (fr != null)
            {
                float textX = this.xPosition + (this.width - fr.getStringWidth(this.displayString)) / 2f;
                float textY = this.yPosition + (this.height - 8f) / 2f;
                fr.drawStringWithShadow(this.displayString, textX, textY, textColor);
            }
            else
            {
                this.drawCenteredString(mc.fontRendererObj, this.displayString, this.xPosition + this.width / 2, this.yPosition + (this.height - 8) / 2, textColor);
            }

            GlStateManager.disableBlend();
        }
    }

    protected void mouseDragged(Minecraft mc, int mouseX, int mouseY)
    {
    }

    public void mouseReleased(int mouseX, int mouseY)
    {
    }

    public boolean mousePressed(Minecraft mc, int mouseX, int mouseY)
    {
        return this.enabled && this.visible && mouseX >= this.xPosition && mouseY >= this.yPosition && mouseX < this.xPosition + this.width && mouseY < this.yPosition + this.height;
    }

    public boolean isMouseOver()
    {
        return this.hovered;
    }

    public void drawButtonForegroundLayer(int mouseX, int mouseY)
    {
    }

    public void playPressSound(SoundHandler soundHandlerIn)
    {
        soundHandlerIn.playSound(PositionedSoundRecord.create(new ResourceLocation("gui.button.press"), 1.0F));
    }

    public int getButtonWidth()
    {
        return this.width;
    }

    public void setWidth(int width)
    {
        this.width = width;
    }
}