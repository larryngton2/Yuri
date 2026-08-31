package ddlc.yuri.modules.impl.combat;

import ddlc.yuri.Yuri;
import ddlc.yuri.api.events.annotations.EventHook;
import ddlc.yuri.api.events.impl.player.HitSlowDownEvent;
import ddlc.yuri.api.events.impl.player.MotionEvent;
import ddlc.yuri.api.events.impl.player.PreUpdateEvent;
import ddlc.yuri.api.events.impl.world.WorldJoinEvent;
import ddlc.yuri.api.properties.Property;
import ddlc.yuri.api.properties.impl.ModeProperty;
import ddlc.yuri.api.properties.impl.MultiModeProperty;
import ddlc.yuri.api.properties.impl.NumberProperty;
import ddlc.yuri.managers.impl.*;
import ddlc.yuri.modules.Module;
import ddlc.yuri.modules.ModuleCategory;
import ddlc.yuri.modules.ModuleInfo;
import ddlc.yuri.modules.impl.player.ScaffoldModule;
import ddlc.yuri.utils.client.LoggingUtils;
import ddlc.yuri.utils.client.MathUtils;
import ddlc.yuri.utils.client.TimerUtils;
import ddlc.yuri.utils.player.InvUtils;
import ddlc.yuri.utils.player.RayCastUtils;
import ddlc.yuri.utils.player.RotationUtils;
import ddlc.yuri.utils.player.packet.PacketUtils;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.util.*;
import org.lwjgl.util.vector.Vector2f;

import java.security.SecureRandom;
import java.util.Arrays;

@ModuleInfo(label = "Aura", description = "Automatically attacks entities around you", category = ModuleCategory.COMBAT)
public class AuraModule extends Module {

    private final MultiModeProperty<TargetManager.Targets> targets = new MultiModeProperty<>("Targets", TargetManager.Targets.PLAYERS, TargetManager.Targets.HOSTILES, TargetManager.Targets.TEAMMATES, TargetManager.Targets.INVISIBLES);
    private static final ModeProperty<TargetManager.Mode> mode = new ModeProperty<>("Mode", TargetManager.Mode.SINGLE);
    public static NumberProperty seekRange = new NumberProperty("Seek Range", 6.0, 3, 6, 0.1);
    public static final Property<Boolean> useOnlyMouse = new Property<>("Simulate Mouse Clicks", true);
    public static NumberProperty attackRange = new NumberProperty("Attack Range", 3.0, 3, 6, 0.1, () -> !useOnlyMouse.getValue());
    public static NumberProperty swingRange = new NumberProperty("Swing Range", 6.0, 3, 6, 0.1);
    public static NumberProperty blockRange = new NumberProperty("Block Range", 6.0, 3, 6, 0.1);
    private static final NumberProperty min = new NumberProperty("Min CPS", 9.0, 0.0, 20.0, 0.5);
    private static final NumberProperty max = new NumberProperty("Max CPS", 13.0, 0.0, 20.0, 0.5);
    public static ModeProperty<AutoBlock> ab = new ModeProperty<>("Auto Block", AutoBlock.FAKE);
    public static Property<Boolean> onlyBlockIfHurt = new Property<>("Only Block If Hurt", false);
    private final NumberProperty blockOnHurtTicks = new NumberProperty("Block On Hurt Ticks", 4, 0, 10, 1, onlyBlockIfHurt::getValue);
    public static final Property<Boolean> throughWalls = new Property<>("Through Walls", false);
    public static ModeProperty<Rotations> rotations = new ModeProperty<>("Rotations", Rotations.NORMAL);
    private final NumberProperty minRotSpeed = new NumberProperty("Min Rotation Speed", 3, 0, 10, 0.5f);
    private final NumberProperty maxRotSpeed = new NumberProperty("Max Rotation Speed", 7, 0, 10, 0.5f);
    private final NumberProperty bodyEase = new NumberProperty("Body Ease", 0.2, 0.01, 1.0, 0.01, () -> rotations.getValue() == Rotations.ML);
    private final NumberProperty mlEase = new NumberProperty("ML Ease", 0.2, 0.01, 1.0, 0.01, () -> rotations.getValue() == Rotations.ML);
    public static final Property<Boolean> advancedPolar = new Property<>("Advanced Polar", false, () -> rotations.getValue() == Rotations.POLAR);
    private static final NumberProperty polarNoiseScale = new NumberProperty("Noise Scale", 2.0, 0.5, 8.0, 0.1, () -> rotations.getValue() == Rotations.POLAR && advancedPolar.getValue());
    private static final NumberProperty polarWarpStrength = new NumberProperty("Warp Strength", 1.5, 0.1, 5.0, 0.1, () -> rotations.getValue() == Rotations.POLAR && advancedPolar.getValue());
    private static final NumberProperty polarNoiseOctaves = new NumberProperty("Noise Octaves", 3, 1, 6, 1, () -> rotations.getValue() == Rotations.POLAR && advancedPolar.getValue());
    private static final NumberProperty polarFlickChance = new NumberProperty("Flick Chance", 50.0, 0.0, 100.0, 1.0, () -> rotations.getValue() == Rotations.POLAR);
    public static final Property<Boolean> rayCast = new Property<>("Ray Cast", true);
    public static final ModeProperty<MoveFix> fix = new ModeProperty<>("Move Fix", MoveFix.SILENT);
    public static final Property<Boolean> sprint = new Property<>("Keep Sprint", false);
    public static final Property<Boolean> hypixelSprint = new Property<>("Hypixel Keep Sprint", false, sprint::getValue);
    public static final Property<Boolean> autoDisable = new Property<>("Auto Disable", true);

    public enum MoveFix {
        NONE("None"),
        STRICT("Strict"),
        SILENT("Silent");

        public final String name;

        MoveFix(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public enum Rotations {
        NORMAL("Normal"),
        ML("ML"),
        POLAR("Polar"),
        NONE("None");

        public final String name;

        Rotations(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public enum AutoBlock {
        FAKE("Fake"),
        VANILLA("Vanilla"),
        NCP("NCP"),
        LEGIT("Legit"),
        HYPIXEL("Hypixel"),
        PREDICTIVE("Predictive"),
        NONE("None");

        public final String name;

        AutoBlock(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public static EntityLivingBase target;
    public static boolean autoBlocking = false;
    public static boolean canAttack = true;
    private static final TimerUtils attackTimer = new TimerUtils();
    private int blockTicks = 0;
    private static long delay = 0;
    public int hitTicks;
    private EntityLivingBase lastTarget;
    private Vec3 smoothedBodyPoint;
    private static final TimerUtils blockTimer = new TimerUtils();

    private int previousTargetId = -1;
    private double previousTargetDistance = -1.0D;

    @EventHook
    public void onPreUpdate(PreUpdateEvent event) {
        setSuffix(mode.getValue().toString());

        if (mc.thePlayer == null || mc.theWorld == null || Yuri.INSTANCE.getModuleManager().getModule(ScaffoldModule.class).isEnabled()) {
            if (target != null || autoBlocking) {
                resetCombatState();
            }
            return;
        }

        TargetManager.setTargets(targets.getValue());
        getTarget();

        if (target != null && !throughWalls.getValue() && !canSeeEntity(target)) {
            target = null;
        }

        if (target == null) {
            unblock();
            canAttack = true;
            return;
        }

        calculateRotations();

        if (ab.getValue() != AutoBlock.NONE && ab.getValue() != AutoBlock.NCP) {
            if (mc.thePlayer.getDistanceToEntity(target) <= blockRange.getValue() && InvUtils.isHoldingSword()) {
                autoblock();
            }
        }

        if (ab.getValue() == AutoBlock.LEGIT && mc.gameSettings.keyBindAttack.isPressed()) {
            mc.gameSettings.keyBindAttack.setPressed(false);
        }

        attack();
    }

    @EventHook
    public void onMotion(MotionEvent event) {
        if (event.isPre()) {
            this.hitTicks++;
            return;
        }

        if (target == null) return;

        if (ab.getValue() == AutoBlock.NCP) {
            if (!autoBlocking && InvUtils.isHoldingSword() && mc.thePlayer.getDistanceToEntity(target) <= blockRange.getValue()) {
                PacketUtils.sendPacket(new C08PacketPlayerBlockPlacement(mc.thePlayer.getHeldItem()));
                autoBlocking = true;
            }
        }
    }

    @EventHook
    public void onHitSlowDown(HitSlowDownEvent e) {
        if (rotations.getValue() == Rotations.POLAR) {
            PolarRotationManager.triggerFlick(polarFlickChance.getValue().floatValue());
        }

        if (sprint.getValue() && !hypixelSprint.getValue()) {
            e.setSprint(true);
            e.setSlowDown(1.0);
        }

        if (hypixelSprint.getValue() && sprint.getValue()) {
            if (!mc.thePlayer.isCollidedHorizontally && mc.thePlayer.isSprinting() && mc.thePlayer.moveForward > 0 && mc.thePlayer.hurtTime <= 4) {
                e.setSprint(true);
            }
        }
    }

    @EventHook
    public void onWorldJoin(WorldJoinEvent e) {
        resetCombatState();
        if (autoDisable.getValue()) {
            toggle();
        }
    }

    private void calculateRotations() {
        if (mc.thePlayer == null || target == null || rotations.getValue() == Rotations.NONE) return;

        if (target != lastTarget) {
            smoothedBodyPoint = null;
            RotationLearnerManager.resetSmoothing();
            PolarRotationManager.reset();
            lastTarget = target;
        }

        float rotSpeed = (float) MathUtils.getRandom(minRotSpeed.getValue(), maxRotSpeed.getValue());
        Vector2f rotation;

        if (rotations.getValue() == Rotations.POLAR) {
            rotation = PolarRotationManager.getPolarRotation(
                    target,
                    polarNoiseScale.getValue(),
                    polarWarpStrength.getValue(),
                    polarNoiseOctaves.getValue().intValue(),
                    polarFlickChance.getValue().floatValue(),
                    seekRange.getValue()
            );
            if (rotation == null) {
                rotation = RotationUtils.calculate(target, false, seekRange.getValue());
            }
        } else if (rotations.getValue() == Rotations.ML && RotationLearnerManager.hasModelLoaded()) {
            rotation = RotationLearnerManager.humanize(getWholeBodyRotation(target), 1.0f, mlEase.getValue().floatValue());
        } else {
            rotation = RotationUtils.calculate(target, false, seekRange.getValue());
        }

        RotationManager.setRotations(rotation, rotSpeed, fix.getValue() != MoveFix.NONE ? fix.getValue() == MoveFix.SILENT ? RotationManager.MovementFix.NORMAL : RotationManager.MovementFix.TRADITIONAL : RotationManager.MovementFix.OFF);
    }

    private Vector2f getWholeBodyRotation(EntityLivingBase entity) {
        AxisAlignedBB box = entity.getEntityBoundingBox();
        double targetX = box.minX + (box.maxX - box.minX) * MathUtils.getRandom(0.0, 1.0);
        double targetY = box.minY + (box.maxY - box.minY) * MathUtils.getRandom(0.0, 1.0);
        double targetZ = box.minZ + (box.maxZ - box.minZ) * MathUtils.getRandom(0.0, 1.0);

        Vec3 desired = new Vec3(targetX, targetY, targetZ);

        if (smoothedBodyPoint == null) {
            smoothedBodyPoint = desired;
        } else {
            double ease;
            ease = bodyEase.getValue();
            smoothedBodyPoint = new Vec3(
                    smoothedBodyPoint.xCoord + (desired.xCoord - smoothedBodyPoint.xCoord) * ease,
                    smoothedBodyPoint.yCoord + (desired.yCoord - smoothedBodyPoint.yCoord) * ease,
                    smoothedBodyPoint.zCoord + (desired.zCoord - smoothedBodyPoint.zCoord) * ease
            );
        }

        Vec3 eyePos = new Vec3(mc.thePlayer.posX, mc.thePlayer.posY + mc.thePlayer.getEyeHeight(), mc.thePlayer.posZ);
        float[] rot = RotationUtils.getRotationsTo(eyePos, smoothedBodyPoint);

        return new Vector2f(rot[0], rot[1]);
    }

    private double wrapAngleTo180(double angle) {
        angle %= 360.0D;
        if (angle >= 180.0D) angle -= 360.0D;
        if (angle < -180.0D) angle += 360.0D;
        return angle;
    }

    private boolean isFacingPlayer(EntityLivingBase target) {
        if (target == null || mc.thePlayer == null) return false;
        double dx = mc.thePlayer.posX - target.posX;
        double dy = (mc.thePlayer.posY + mc.thePlayer.getEyeHeight()) - (target.posY + target.getEyeHeight());
        double dz = mc.thePlayer.posZ - target.posZ;
        double horizontalDistance = Math.sqrt(dx * dx + dz * dz);
        if (horizontalDistance < 0.001D) return true;
        double targetYaw = Math.toDegrees(Math.atan2(-dx, dz));
        double yawDifference = wrapAngleTo180(target.rotationYaw - targetYaw);
        if (Math.abs(yawDifference) > 45.0D) return false;
        double targetPitch = Math.toDegrees(-Math.atan2(dy, horizontalDistance));
        double pitchDifference = wrapAngleTo180(target.rotationPitch - targetPitch);
        return Math.abs(pitchDifference) <= 55.0D;
    }

    private double getTargetClosingSpeed(EntityLivingBase target) {
        if (target == null) return 0.0D;
        double currentDistance = target.getDistanceToEntity(mc.thePlayer);
        if (previousTargetId != target.getEntityId() || previousTargetDistance < 0.0D) return 0.0D;
        return previousTargetDistance - currentDistance;
    }

    private boolean shouldPredictAttack(EntityLivingBase target) {
        if (target == null || mc.thePlayer == null) return false;
        if (target.isDead || target.getHealth() <= 0.0F) return false;
        double distance = target.getDistanceToEntity(mc.thePlayer);
        if (distance <= 2.75D) return true;
        if (distance > 3.35D) return false;
        if (target.isSwingInProgress && isFacingPlayer(target)) return true;
        double closingSpeed = getTargetClosingSpeed(target);
        if (distance <= 3.15D && isFacingPlayer(target) && closingSpeed > 0.01D) return true;
        if (target.isSprinting() && distance <= 3.35D && isFacingPlayer(target) && closingSpeed > 0.0D) return true;
        return false;
    }

    private void updatePredictTargetState(EntityLivingBase target) {
        if (target == null) {
            previousTargetId = -1;
            previousTargetDistance = -1.0D;
            return;
        }
        double distance = target.getDistanceToEntity(mc.thePlayer);
        if (previousTargetId != target.getEntityId()) {
            previousTargetId = target.getEntityId();
            previousTargetDistance = distance;
            return;
        }
        previousTargetDistance = distance;
    }

    private void autoblock() {
        if (mc.thePlayer == null || mc.playerController == null) return;

        if (target == null || mc.thePlayer.getDistanceToEntity(target) > blockRange.getValue() || !InvUtils.isHoldingSword()) {
            if (autoBlocking) unblock();
            blockTimer.reset();
            return;
        }

        if (onlyBlockIfHurt.getValue() && mc.thePlayer.hurtTime < blockOnHurtTicks.getValue().intValue()) {
            if (autoBlocking) unblock();
            return;
        }

        boolean readyToAttack = attackTimer.hasTimeElapsed(delay, false);

        switch (ab.getValue()) {
            case FAKE:
                autoBlocking = true;
                break;
            case LEGIT:
                mc.gameSettings.keyBindUseItem.setPressed(!readyToAttack);
                autoBlocking = true;
                blockTicks++;
                if (mc.gameSettings.keyBindUseItem.isPressed() || mc.thePlayer.isUsingItem()) {
                    blockTicks = 0;
                }
                canAttack = !BadPacketsManager.bad(false, false, false, true, false) && blockTicks >= 1;
                break;
            case PREDICTIVE:
                mc.gameSettings.keyBindUseItem.setPressed(!readyToAttack && shouldPredictAttack(target));
                autoBlocking = true;
                blockTicks++;
                if (mc.gameSettings.keyBindUseItem.isPressed() || mc.thePlayer.isUsingItem()) {
                    blockTicks = 0;
                }
                canAttack = !BadPacketsManager.bad(false, false, false, true, false) && blockTicks >= 1;
                break;
            case VANILLA:
                PacketUtils.sendPacket(new C08PacketPlayerBlockPlacement(mc.thePlayer.getHeldItem()));
                autoBlocking = true;
                break;
            case NCP:
                canAttack = true;
                if (autoBlocking) {
                    PacketUtils.sendPacket(new C07PacketPlayerDigging(C07PacketPlayerDigging.Action.RELEASE_USE_ITEM, BlockPos.ORIGIN, EnumFacing.DOWN));
                    autoBlocking = false;
                }
                break;
            case HYPIXEL:
                mc.gameSettings.keyBindUseItem.setPressed(!BadPacketsManager.bad(true, true, false, false, false) && !readyToAttack && mc.thePlayer.getDistanceToEntity(target) <= 2.5f);
                if (mc.gameSettings.keyBindUseItem.isPressed() || mc.thePlayer.isUsingItem()) {
                    mc.thePlayer.setSprinting(!(Math.abs(MathHelper.wrapAngleTo180_float(mc.thePlayer.rotationYaw) - MathHelper.wrapAngleTo180_float(RotationManager.rotations.x)) > 90));
                    mc.thePlayer.movementInput.moveForward *= 1.8f;
                    mc.thePlayer.movementInput.moveStrafe *= 1.8f;
                }
                SlotManager.swap(mc.thePlayer.inventory.currentItem % 8 + 1, true);
                autoBlocking = true;
                blockTicks++;
                if (mc.thePlayer.isUsingItem()) {
                    blockTicks = 0;
                    SlotManager.swapBack();
                }
                canAttack = !BadPacketsManager.bad(true, false, false, true, false) && blockTicks >= 1;
                break;
        }
    }

    private void unblock() {
        if (!autoBlocking) {
            canAttack = true;
            return;
        }

        blockTimer.reset();
        blockTicks = -1;

        if (ab.getValue() == AutoBlock.FAKE) {
            autoBlocking = false;
            canAttack = true;
            return;
        }

        if (ab.getValue() == AutoBlock.LEGIT || ab.getValue() == AutoBlock.PREDICTIVE) {
            mc.gameSettings.keyBindUseItem.setPressed(false);
            autoBlocking = false;
            canAttack = true;
            return;
        }

        if (ab.getValue() == AutoBlock.HYPIXEL) {
            mc.gameSettings.keyBindUseItem.setPressed(false);
            mc.playerController.onStoppedUsingItem(mc.thePlayer);
            autoBlocking = false;
            canAttack = true;
            return;
        }

        if (InvUtils.isHoldingSword() && ab.getValue() != AutoBlock.LEGIT && ab.getValue() != AutoBlock.HYPIXEL && ab.getValue() != AutoBlock.PREDICTIVE) {
            PacketUtils.sendPacket(new C07PacketPlayerDigging(C07PacketPlayerDigging.Action.RELEASE_USE_ITEM, BlockPos.ORIGIN, EnumFacing.DOWN));
        }

        autoBlocking = false;
        canAttack = true;
    }

    private void attack() {
        if (mc.thePlayer == null || mc.playerController == null || target == null || !canAttack)
            return;
        if (!hitTimerDone()) return;

        double dist = mc.thePlayer.getDistanceToEntity(target);

        if (dist <= attackRange.getValue() && !useOnlyMouse.getValue()) {
            if (rayCast.getValue() && !(RayCastUtils.rayCast(RotationManager.rotations, blockRange.getValue().floatValue()) != null
                    && RayCastUtils.rayCast(RotationManager.rotations, blockRange.getValue().floatValue()).entityHit != null
                    && RayCastUtils.rayCast(RotationManager.rotations, blockRange.getValue().floatValue()).entityHit == target))
                return;
            swing();
            mc.playerController.attackEntity(mc.thePlayer, target);
            this.hitTicks = 0;
        } else if (dist <= swingRange.getValue()) {
            mc.clickMouse();
            this.hitTicks = 0;
        }
    }

    private void swing() {
        mc.thePlayer.swingItem();
    }

    private static boolean hitTimerDone() {
        boolean returnVal = false;
        if (attackTimer.hasTimeElapsed(delay, false)) {
            returnVal = true;
            attackTimer.reset();
            delay = (long) (1000.0 / getCPS());
        }
        return returnVal;
    }

    private void resetCombatState() {
        if (autoBlocking) {
            unblock();
        } else {
            canAttack = true;
        }
        if (SlotManager.isActive()) {
            SlotManager.swapBack();
        }
        target = null;
        lastTarget = null;
        smoothedBodyPoint = null;
        RotationLearnerManager.resetSmoothing();
        PolarRotationManager.reset();
        delay = 0;
        blockTimer.reset();
        blockTicks = -1;
        attackTimer.reset();
        previousTargetId = -1;
        previousTargetDistance = -1.0D;
    }

    @Override
    public void onEnable() {
        delay = (long) (1000.0 / getCPS());
        canAttack = true;
        autoBlocking = false;
        blockTicks = -1;
        TargetManager.configure(Arrays.asList(targets.getValues()));
        attackTimer.reset();
        if (rotations.getValue() == Rotations.ML) {
            if (!RotationLearnerManager.hasModelLoaded()) {
                LoggingUtils.sendChatMessage("ML model not loaded, use .rot load <name> to load a rotation model!");
            }
        }
        super.onEnable();
    }

    private static double getCPS() {
        double minVal = min.getValue();
        double maxVal = max.getValue();
        if (maxVal <= 0) maxVal = 1.0;
        if (minVal < 0) minVal = 0.0;
        if (minVal > maxVal) {
            double t = minVal;
            minVal = maxVal;
            maxVal = t;
        }
        double cps = MathHelper.clamp_double(minVal + ((maxVal - minVal) * new SecureRandom().nextDouble()), minVal, maxVal);
        return Math.max(1.0, cps);
    }

    @Override
    public void onDisable() {
        resetCombatState();
        super.onDisable();
    }

    private void getTarget() {
        target = TargetManager.getTarget();
    }

    private boolean canSeeEntity(Entity entity) {
        if (throughWalls.getValue()) return true;
        Vec3 eyes = mc.thePlayer.getPositionEyes(1.0f);
        Vec3 targetPos = new Vec3(entity.posX, entity.posY + entity.getEyeHeight(), entity.posZ);
        MovingObjectPosition result = mc.theWorld.rayTraceBlocks(eyes, targetPos, false, true, false);
        return result == null;
    }
}