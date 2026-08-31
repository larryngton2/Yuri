package ddlc.yuri.modules.impl.render;

import ddlc.yuri.api.events.annotations.EventHook;
import ddlc.yuri.api.events.annotations.EventPriority;
import ddlc.yuri.api.events.impl.player.PreUpdateEvent;
import ddlc.yuri.api.events.impl.render.Render2DEvent;
import ddlc.yuri.api.events.impl.render.Shader2DEvent;
import ddlc.yuri.api.properties.Property;
import ddlc.yuri.managers.impl.ColorManager;
import ddlc.yuri.modules.Module;
import ddlc.yuri.modules.ModuleCategory;
import ddlc.yuri.modules.ModuleInfo;
import ddlc.yuri.utils.render.FontUtils;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.ScaledResolution;

import java.util.ArrayList;
import java.util.List;

@ModuleInfo(label = "Client Info", category = ModuleCategory.RENDER, description = "Renders a HUD to show your client info")
public final class ClientInfoModule extends Module {

    private final Property<Boolean> fps = new Property<>("FPS", true);
    private final Property<Boolean> speed = new Property<>("Speed", true);
    private final Property<Boolean> coords = new Property<>("Coords", true);

    private ScaledResolution sr = new ScaledResolution(mc);

    private int x = 2;
    private double y = sr.getScaledHeight() - 2;

    private double bpsValue = 0.0;
    private int xCoord = 0;
    private int yCoord = 0;
    private int zCoord = 0;

    @EventHook
    public void onPreUpdate(PreUpdateEvent event) {
        if (mc.thePlayer != null) {
            double deltaX = mc.thePlayer.posX - mc.thePlayer.prevPosX;
            double deltaZ = mc.thePlayer.posZ - mc.thePlayer.prevPosZ;
            bpsValue = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ) * 20.0 * mc.timer.timerSpeed;
            xCoord = mc.thePlayer.getPosition().getX();
            yCoord = mc.thePlayer.getPosition().getY();
            zCoord = mc.thePlayer.getPosition().getZ();
        }
    }

    @EventHook(EventPriority.VERY_HIGH)
    public void onRender2D(Render2DEvent event) {
        renderClientInfo();
    }

    @EventHook(EventPriority.VERY_HIGH)
    public void onShader2D(Shader2DEvent event) {
        if (event.getShaderType() == Shader2DEvent.ShaderType.BLUR) return;
        renderClientInfo();
    }

    private void renderClientInfo() {
        sr = new ScaledResolution(mc);

        double targetY = (mc.currentScreen instanceof GuiChat) ? (sr.getScaledHeight() - 15) : (sr.getScaledHeight() - 2);

        double speedFactor = 0.125;
        y = y + (targetY - y) * speedFactor;

        int lineHeight = FontUtils.getFont("sf", 18).getHeight() + 2;
        List<String> lines = new ArrayList<>();

        if (fps.getValue()) {
            lines.add("FPS: " + "\u00a7f" + mc.getDebugFPS());
        }

        if (speed.getValue()) {
            lines.add(String.format("BPS: \u00a7f%.2f", bpsValue));
        }

        if (coords.getValue()) {
            lines.add("X: " + "\u00a7f" + xCoord + "\u00a7r" + " Y: " + "\u00a7f" + yCoord + "\u00a7r" + " Z: " + "\u00a7f" + zCoord);
        }

        for (int i = lines.size() - 1; i >= 0; i--) {
            FontUtils.getFont("sf", 18).drawStringWithShadow(
                    lines.get(i),
                    x,
                    (float) (y - (lines.size() - i) * lineHeight),
                    ColorManager.getColor().getRGB()
            );
        }
    }
}