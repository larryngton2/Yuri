package ddlc.yuri.modules.impl.combat;

import ddlc.yuri.Yuri;
import ddlc.yuri.api.events.annotations.EventHook;
import ddlc.yuri.api.events.impl.player.PlayerAttackEvent;
import ddlc.yuri.api.events.impl.player.PreUpdateEvent;
import ddlc.yuri.api.properties.impl.NumberProperty;
import ddlc.yuri.managers.impl.RotationManager;
import ddlc.yuri.modules.Module;
import ddlc.yuri.modules.ModuleCategory;
import ddlc.yuri.modules.ModuleInfo;
import ddlc.yuri.modules.impl.player.ScaffoldModule;
import ddlc.yuri.utils.client.TimerUtils;
import ddlc.yuri.utils.player.PlayerUtils;
import ddlc.yuri.utils.player.packet.PacketUtils;
import net.minecraft.item.Item;
import net.minecraft.item.ItemPotion;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import org.lwjgl.util.vector.Vector2f;

@ModuleInfo(label = "Auto Pot", category = ModuleCategory.COMBAT, description = "Automatically throws a potion when your health is low")
public final class AutoPotModule extends Module {

    private final NumberProperty health = new NumberProperty("Health", 15, 1, 20, 1);
    private final NumberProperty delay = new NumberProperty("Delay", 0, 0, 100, 5);

    private final TimerUtils stopWatch = new TimerUtils();
    private int attackTicks;
    private long nextThrow;

    @EventHook
    public void onUpdate(PreUpdateEvent event) {
        this.attackTicks++;

        if (mc.currentScreen != null) {
            this.attackTicks = 0;
        }

        if (mc.thePlayer.onGroundTicks <= 1 || !stopWatch.hasTimeElapsed(nextThrow) || attackTicks < 10 || Yuri.INSTANCE.getModuleManager().getModule(ScaffoldModule.class).isEnabled()) {
            return;
        }

        for (int i = 0; i < 9; i++) {
            final ItemStack stack = mc.thePlayer.inventory.getStackInSlot(i);

            if (stack == null) {
                continue;
            }

            final Item item = stack.getItem();

            if (item instanceof ItemPotion) {
                final ItemPotion potion = (ItemPotion) item;
                final PotionEffect effect = potion.getEffects(stack).get(0);

                final int potId = effect.getPotionID();
                final boolean isSplash = ItemPotion.isSplash(stack.getMetadata());
                final boolean isSpeedOrJump = potId == Potion.moveSpeed.id || potId == Potion.jump.id;
                final boolean isHealOrRegen = potId == Potion.regeneration.id || potId == Potion.heal.id;
                final boolean isGood = PlayerUtils.goodPotion(potId) || isSpeedOrJump;

                if (!isSplash || !isGood || (isHealOrRegen && mc.thePlayer.getHealth() > this.health.getValue().floatValue())) {
                    continue;
                }

                if (mc.thePlayer.isPotionActive(effect.getPotionID()) &&
                        mc.thePlayer.getActivePotionEffect(effect.getPotionID()).getDuration() != 0) {
                    continue;
                }

                RotationManager.setRotations(new Vector2f((float) (mc.thePlayer.rotationYaw + (Math.random() - 0.5) * 3), (float) (87 + Math.random() * 3)), 10.0f, RotationManager.MovementFix.NORMAL);

                mc.thePlayer.inventory.currentItem = i;

                if (RotationManager.rotations.y > 85) {
                    mc.playerController.syncCurrentPlayItem();
                    PacketUtils.sendPacket(new C08PacketPlayerBlockPlacement(mc.thePlayer.inventory.getCurrentItem()));

                    this.nextThrow = delay.getValue().longValue() * 10;
                    stopWatch.reset();
                    break;
                }
            }
        }
    }


    @EventHook
    public void onAttack(PlayerAttackEvent event) {
        this.attackTicks = 0;
    }
}
