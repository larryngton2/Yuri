package ddlc.yuri.modules.impl.combat;

import ddlc.yuri.Yuri;
import ddlc.yuri.api.events.annotations.EventHook;
import ddlc.yuri.api.events.impl.player.PreUpdateEvent;
import ddlc.yuri.api.events.impl.render.Render2DEvent;
import ddlc.yuri.api.properties.Property;
import ddlc.yuri.api.properties.impl.ModeProperty;
import ddlc.yuri.api.properties.impl.MultiModeProperty;
import ddlc.yuri.api.properties.impl.NumberProperty;
import ddlc.yuri.managers.impl.RotationManager;
import ddlc.yuri.managers.impl.TargetManager;
import ddlc.yuri.modules.Module;
import ddlc.yuri.modules.ModuleCategory;
import ddlc.yuri.modules.ModuleInfo;
import ddlc.yuri.modules.impl.misc.AntiBotModule;
import ddlc.yuri.modules.impl.player.ScaffoldModule;
import ddlc.yuri.utils.client.MathUtils;
import ddlc.yuri.utils.player.FriendUtils;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.passive.IAnimals;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBow;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;

import java.util.ArrayList;
import java.util.Comparator;

@ModuleInfo(label = "Bow Aimbot", category = ModuleCategory.COMBAT, description = "Automatically aims your bow at targets using advanced trajectory prediction")
public final class BowAimbotModule extends Module {

    private final MultiModeProperty<TargetManager.Targets> targets = new MultiModeProperty<>("Targets", TargetManager.Targets.PLAYERS, TargetManager.Targets.HOSTILES, TargetManager.Targets.TEAMMATES);
    private final ModeProperty<TargetMode> mode = new ModeProperty<>("Mode", TargetMode.DISTANCE);
    private final NumberProperty range = new NumberProperty("Range", 80, 0, 256, 1);
    private final Property<Boolean> lockView = new Property<Boolean>("Lock View", true);
    private final NumberProperty minRotSpeed = new NumberProperty("Min Rotation Speed", 3, 0, 10, 0.5f, () -> !lockView.getValue());
    private final NumberProperty maxRotSpeed = new NumberProperty("Max Rotation Speed", 7, 0, 10, 0.5f, () -> !lockView.getValue());
    private final Property<Boolean> throughWalls = new Property<>("Through Walls", false);

    private final ArrayList<Entity> attackList = new ArrayList<>();
    public static EntityLivingBase target;
    private int currentTarget;
    public float velocity;

    public enum TargetMode {
        HEALTH("Health"),
        DISTANCE("Distance");

        private final String name;

        TargetMode(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    @Override
    public void onDisable() {
        super.onDisable();
        attackList.clear();
        target = null;
        currentTarget = 0;
    }

    @EventHook
    public void onPreUpdate(PreUpdateEvent event) {
        setSuffix(mode.getValue().toString());

        if (mc.thePlayer == null || mc.theWorld == null || Yuri.INSTANCE.getModuleManager().getModule(ScaffoldModule.class).isEnabled()) {
            attackList.clear();
            target = null;
            return;
        }

        attackList.clear();

        for (Object obj : mc.theWorld.loadedEntityList) {
            Entity entity = (Entity) obj;
            if (isValidTarget(entity)) {
                attackList.add(entity);
            }
        }

        if (attackList.isEmpty() || mc.thePlayer.getHeldItem() == null || mc.thePlayer.getHeldItem().getItem() == null || !(mc.thePlayer.getHeldItem().getItem() instanceof ItemBow) || !mc.thePlayer.isUsingItem() || mc.thePlayer.getItemInUseDuration() <= 1) {
            target = null;
            return;
        }

        sortTargets();
        currentTarget = Math.min(currentTarget, attackList.size() - 1);
        target = (EntityLivingBase) attackList.get(currentTarget);

        if (target == null) {
            return;
        }

        if (!throughWalls.getValue() && !canSeeEntity(target)) {
            return;
        }

        setSuffix(target.getName());

        float[] rotations = getBowRotations(target);
        if (rotations == null) return;

        float yaw = rotations[0];
        float pitch = rotations[1];

        if (lockView.getValue()) {
            mc.thePlayer.rotationYaw = yaw;
            mc.thePlayer.rotationPitch = pitch;
        } else {
            float rotSpeed = (float) MathUtils.getRandom(
                    minRotSpeed.getValue(),
                    maxRotSpeed.getValue()
            );
            RotationManager.setRotations(
                    yaw, pitch, rotSpeed,
                    RotationManager.MovementFix.NORMAL
            );
        }
    }

    @EventHook
    public void onRender2D(Render2DEvent event) {
        if (target == null || attackList.isEmpty() || mc.thePlayer.getHeldItem() == null || !(mc.thePlayer.getHeldItem().getItem() instanceof ItemBow) || !mc.thePlayer.isUsingItem()) {
            return;
        }

        if (lockView.getValue()) {
            float[] rotations = getBowRotations(target);
            if (rotations != null) {
                mc.thePlayer.rotationYaw = rotations[0];
                mc.thePlayer.rotationPitch = rotations[1];
            }
        }
    }

    public boolean isValidTarget(Entity entity) {
        if (entity == null || entity == mc.thePlayer || entity == mc.thePlayer.ridingEntity || !entity.isEntityAlive()) {
            return false;
        }

        if (mc.thePlayer.getDistanceToEntity(entity) > range.getValue()) {
            return false;
        }

        if (entity instanceof EntityPlayer && !targets.getValue().contains(TargetManager.Targets.TEAMMATES)) {
            if (TargetManager.inTeam(mc.thePlayer, (EntityPlayer) entity)) {
                return false;
            }
        }

        for (TargetManager.Targets t : targets.getValue()) {
            switch (t) {
                case PLAYERS:
                    if (entity instanceof EntityPlayer) {
                        boolean isFriend = FriendUtils.isFriend(entity.getName());
                        boolean isBot = Yuri.INSTANCE.getModuleManager().getModule(AntiBotModule.class).isBot((EntityPlayer) entity);
                        if (!isFriend && !isBot) return true;
                    }
                    break;
                case HOSTILES:
                    if (entity instanceof IMob) {
                        return true;
                    }
                    break;
                case ANIMALS:
                    if (entity instanceof IAnimals && !(entity instanceof IMob)) {
                        return true;
                    }
                    break;
                case TEAMMATES:
                    if (entity instanceof EntityPlayer && TargetManager.inTeam(mc.thePlayer, (EntityPlayer) entity)) {
                        return true;
                    }
                    break;
                case INVISIBLES:
                    if (entity.isInvisible()) {
                        return true;
                    }
                    break;
                default:
                    break;
            }
        }

        return false;
    }

    public void sortTargets() {
        switch (mode.getValue()) {
            case DISTANCE:
                attackList.sort(Comparator.comparingDouble(ent -> mc.thePlayer.getDistanceToEntity(ent)));
                break;
            case HEALTH:
                attackList.sort((ent1, ent2) -> Float.compare(
                        ((EntityLivingBase) ent1).getHealth(),
                        ((EntityLivingBase) ent2).getHealth()
                ));
                break;
        }
    }

    public float[] getBowRotations(EntityLivingBase entity) {
        this.velocity = (float)(72000 - mc.thePlayer.getItemInUseCount()) / 20.0F;
        this.velocity = (this.velocity * this.velocity + this.velocity * 2.0F) / 3.0F;
        if (this.velocity > 1.0F) {
            this.velocity = 1.0F;
        }

        double d = (double)mc.thePlayer.getDistanceToEntity(entity) / 2.5;
        double posX = entity.posX + (entity.posX - entity.prevPosX) * d - mc.thePlayer.posX;
        double posY = entity.posY
                + (entity.posY - entity.prevPosY) * 1.0
                + (double)entity.height * 0.5
                - mc.thePlayer.posY
                - (double)mc.thePlayer.getEyeHeight();
        double posZ = entity.posZ + (entity.posZ - entity.prevPosZ) * d - mc.thePlayer.posZ;

        float yaw = (float)Math.toDegrees(Math.atan2(posZ, posX)) - 90.0F;
        double hDistance = Math.sqrt(posX * posX + posZ * posZ);
        double hDistanceSq = hDistance * hDistance;
        float g = 0.006F;
        float velocitySq = this.velocity * this.velocity;
        float velocityPow4 = velocitySq * velocitySq;

        float neededPitch = (float)(
                -Math.toDegrees(
                        Math.atan(
                                ((double)velocitySq - Math.sqrt((double)velocityPow4 - (double)g * ((double)g * hDistanceSq + 2.0 * posY * (double)velocitySq)))
                                        / ((double)g * hDistance)
                        )
                )
        );

        return Float.isNaN(neededPitch)
                ? new float[]{mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch}
                : new float[]{yaw, neededPitch};
    }

    private boolean canSeeEntity(Entity entity) {
        Vec3 eyes = mc.thePlayer.getPositionEyes(1.0f);
        Vec3 targetPos = new Vec3(entity.posX, entity.posY + entity.getEyeHeight(), entity.posZ);
        MovingObjectPosition result = mc.theWorld.rayTraceBlocks(eyes, targetPos, false, true, false);
        return result == null;
    }
}