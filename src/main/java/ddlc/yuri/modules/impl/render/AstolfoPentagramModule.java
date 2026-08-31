package ddlc.yuri.modules.impl.render;

import ddlc.yuri.api.events.annotations.EventHook;
import ddlc.yuri.api.events.impl.render.Render3DEvent;
import ddlc.yuri.managers.impl.ColorManager;
import ddlc.yuri.modules.Module;
import ddlc.yuri.modules.ModuleCategory;
import ddlc.yuri.modules.ModuleInfo;
import ddlc.yuri.utils.render.RenderUtils;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.opengl.GL11;

import java.awt.*;


@ModuleInfo(label = "Astolfo Pentagram", description = "Displays a pentagram under your feet", category = ModuleCategory.RENDER)
public class AstolfoPentagramModule extends Module {

    @EventHook
    public void onRender3D(Render3DEvent event) {
        renderAstolfoPentagram();
    }

    private void renderAstolfoPentagram() {
        if (mc.thePlayer == null) return;

        final float partialTicks = mc.timer.renderPartialTicks;
        final Color color = ColorManager.getColor();

        final double x = mc.thePlayer.prevPosX + (mc.thePlayer.posX - mc.thePlayer.prevPosX) * partialTicks - mc.getRenderManager().renderPosX;
        final double y = mc.thePlayer.prevPosY + (mc.thePlayer.posY - mc.thePlayer.prevPosY) * partialTicks - mc.getRenderManager().renderPosY + 0.02;
        final double z = mc.thePlayer.prevPosZ + (mc.thePlayer.posZ - mc.thePlayer.prevPosZ) * partialTicks - mc.getRenderManager().renderPosZ;

        final double radius = 1.4;
        final double rotation = (System.currentTimeMillis() % 6000L) / 6000.0 * 360.0;

        GL11.glPushMatrix();
        GL11.glDisable(3553);
        GL11.glEnable(2848);
        GL11.glEnable(3042);
        GL11.glBlendFunc(770, 771);
        GL11.glLineWidth(2.0F);
        GL11.glDepthMask(false);
        GlStateManager.disableCull();

        RenderUtils.color(color.getRGB());
        GL11.glBegin(GL11.GL_LINE_LOOP);

        for (int i = 0; i < 5; i++) {
            double angle = Math.toRadians(rotation + i * 144.0);
            double vecX = x + radius * Math.cos(angle);
            double vecZ = z + radius * Math.sin(angle);
            GL11.glVertex3d(vecX, y, vecZ);
        }

        GL11.glEnd();

        GL11.glDepthMask(true);
        GlStateManager.enableCull();
        GL11.glDisable(2848);
        GL11.glEnable(3553);
        GL11.glPopMatrix();
        RenderUtils.resetColor();
    }
}
