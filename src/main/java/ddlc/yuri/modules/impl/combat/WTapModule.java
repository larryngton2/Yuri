package ddlc.yuri.modules.impl.combat;

import ddlc.yuri.api.events.annotations.EventHook;
import ddlc.yuri.api.events.impl.player.PlayerAttackEvent;
import ddlc.yuri.api.events.impl.player.PreUpdateEvent;
import ddlc.yuri.api.properties.impl.NumberProperty;
import ddlc.yuri.modules.Module;
import ddlc.yuri.modules.ModuleCategory;
import ddlc.yuri.modules.ModuleInfo;

@ModuleInfo(label = "W Tap", description = "Automatically w-taps to combo giving the target more knockback", category = ModuleCategory.COMBAT)
public final class WTapModule extends Module {

    private final NumberProperty chance = new NumberProperty("Chance", 100, 0, 100, 1);

    private boolean unsprint, wtap;

    @EventHook
    public void onAttack(PlayerAttackEvent event) {
        if (event.target == null) return;

        wtap = Math.random() * 100 < chance.getValue()
                && event.target.hurtTime >= 6;

        if (!wtap || unsprint) return;

        if (mc.thePlayer.isSprinting()
                || mc.gameSettings.keyBindSprint.isKeyDown()) {
            mc.gameSettings.keyBindSprint.pressed = true;
            unsprint = true;
        }
    }

    @EventHook
    public void onPreUpdate(PreUpdateEvent event) {
        if (!wtap) return;

        if (unsprint && Math.random() * 100 < chance.getValue()) {
            mc.gameSettings.keyBindSprint.pressed = false;
            mc.thePlayer.setSprinting(false);
            unsprint = false;
        }
    }
}
