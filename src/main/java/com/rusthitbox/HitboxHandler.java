package com.rusthitbox;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Mod.EventBusSubscriber(modid = RustHitbox.MODID)
public class HitboxHandler {

    private static final String DONE_TAG = "rusthitbox_bullet";

    // ---------------------------------------------------------------
    // Hitbox tracing
    // ---------------------------------------------------------------

    /**
     * Traces the segment from -> to against the body parts of the player.
     * Returns the first part hit, or (if fallback) the part closest to the segment, or null.
     */
    public static BodyPart trace(EntityPlayer target, Vec3d from, Vec3d to, boolean fallback) {
        double px = target.height / 32.0D;
        double pad = HitboxConfig.hitboxPadding;

        BodyPart best = null;
        double bestT = Double.MAX_VALUE;

        for (BodyPart part : BodyPart.values()) {
            float yaw = part.followsHead ? target.rotationYawHead : target.renderYawOffset;
            double[] a = toLocal(target, from, yaw);
            double[] b = toLocal(target, to, yaw);
            double[] min = {part.minX * px - pad, part.minY * px - pad, part.minZ * px - pad};
            double[] max = {part.maxX * px + pad, part.maxY * px + pad, part.maxZ * px + pad};
            double t = intersect(a, b, min, max);
            if (t >= 0 && t < bestT) {
                bestT = t;
                best = part;
            }
        }
        if (best != null || !fallback) {
            return best;
        }

        // Fallback: the part closest to the segment
        double bestDist = Double.MAX_VALUE;
        for (BodyPart part : BodyPart.values()) {
            float yaw = part.followsHead ? target.rotationYawHead : target.renderYawOffset;
            double[] a = toLocal(target, from, yaw);
            double[] b = toLocal(target, to, yaw);
            double[] min = {part.minX * px, part.minY * px, part.minZ * px};
            double[] max = {part.maxX * px, part.maxY * px, part.maxZ * px};
            for (int i = 0; i <= 50; i++) {
                double f = i / 50.0D;
                double x = a[0] + (b[0] - a[0]) * f;
                double y = a[1] + (b[1] - a[1]) * f;
                double z = a[2] + (b[2] - a[2]) * f;
                double d = distToBox(x, y, z, min, max);
                if (d < bestDist) {
                    bestDist = d;
                    best = part;
                }
            }
        }
        return best;
    }

    private static double[] toLocal(EntityPlayer target, Vec3d p, float yawDeg) {
        double dx = p.x - target.posX;
        double dy = p.y - target.posY;
        double dz = p.z - target.posZ;
        double r = Math.toRadians(yawDeg);
        double c = Math.cos(r);
        double s = Math.sin(r);
        return new double[] {dx * c + dz * s, dy, -dx * s + dz * c};
    }

    /** Slab test. Returns entry t in [0,1] or -1 if the segment misses the box. */
    private static double intersect(double[] a, double[] b, double[] min, double[] max) {
        double t0 = 0.0D;
        double t1 = 1.0D;
        for (int i = 0; i < 3; i++) {
            double d = b[i] - a[i];
            if (Math.abs(d) < 1.0E-9D) {
                if (a[i] < min[i] || a[i] > max[i]) {
                    return -1.0D;
                }
            } else {
                double u = (min[i] - a[i]) / d;
                double v = (max[i] - a[i]) / d;
                if (u > v) {
                    double tmp = u;
                    u = v;
                    v = tmp;
                }
                if (u > t0) t0 = u;
                if (v < t1) t1 = v;
                if (t0 > t1) {
                    return -1.0D;
                }
            }
        }
        return t0;
    }

    private static double distToBox(double x, double y, double z, double[] min, double[] max) {
        double dx = Math.max(Math.max(min[0] - x, 0.0D), x - max[0]);
        double dy = Math.max(Math.max(min[1] - y, 0.0D), y - max[1]);
        double dz = Math.max(Math.max(min[2] - z, 0.0D), z - max[2]);
        return dx * dx + dy * dy + dz * dz;
    }

    private static Vec3d[] projectileSegment(Entity projectile, double back, double forward) {
        Vec3d pos = new Vec3d(projectile.posX, projectile.posY, projectile.posZ);
        Vec3d motion = new Vec3d(projectile.motionX, projectile.motionY, projectile.motionZ);
        return new Vec3d[] {pos.subtract(motion.scale(back)), pos.add(motion.scale(forward))};
    }

    // ---------------------------------------------------------------
    // Projectiles: bullets/arrows pass through the empty space around the model
    // ---------------------------------------------------------------

    @SubscribeEvent
    public static void onArrowImpact(ProjectileImpactEvent.Arrow event) {
        if (!HitboxConfig.enabled || !HitboxConfig.preciseProjectiles) return;
        EntityArrow arrow = event.getArrow();
        if (arrow.world.isRemote) return;
        RayTraceResult result = event.getRayTraceResult();
        if (result == null || result.typeOfHit != RayTraceResult.Type.ENTITY) return;
        if (!(result.entityHit instanceof EntityPlayer)) return;

        Vec3d[] seg = projectileSegment(arrow, 0.2D, 1.0D);
        if (trace((EntityPlayer) result.entityHit, seg[0], seg[1], false) == null) {
            event.setCanceled(true); // missed the model: the bullet keeps flying
        }
    }

    // ---------------------------------------------------------------
    // Damage per body part
    // ---------------------------------------------------------------

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onHurt(LivingHurtEvent event) {
        if (!HitboxConfig.enabled) return;
        EntityLivingBase victim = event.getEntityLiving();
        if (!(victim instanceof EntityPlayer) || victim.world.isRemote) return;
        EntityPlayer target = (EntityPlayer) victim;

        DamageSource source = event.getSource();
        Entity immediate = source.getImmediateSource();
        Entity attacker = source.getTrueSource();
        if (immediate == null) return;

        BodyPart part;
        if (immediate == attacker && attacker instanceof EntityLivingBase) {
            // melee: ray from the attacker's eyes along the look vector
            EntityLivingBase living = (EntityLivingBase) attacker;
            Vec3d eyes = living.getPositionEyes(1.0F);
            Vec3d end = eyes.add(living.getLookVec().scale(6.0D));
            part = trace(target, eyes, end, true);
        } else if (immediate != target) {
            Vec3d[] seg = projectileSegment(immediate, 0.5D, 1.5D);
            part = trace(target, seg[0], seg[1], true);
        } else {
            return;
        }
        if (part == null) return;

        float mult = (float) part.multiplier();
        event.setAmount(event.getAmount() * mult);

        if (HitboxConfig.showHitMessage && attacker instanceof EntityPlayer && attacker != target) {
            ((EntityPlayer) attacker).sendStatusMessage(
                    new TextComponentString(part.color + part.title + " \u00a77x" + String.format("%.1f", mult)), true);
        }
    }

    // ---------------------------------------------------------------
    // MineRust Addons: fast bullets with the same damage
    // ---------------------------------------------------------------

    @SubscribeEvent
    public static void onJoin(EntityJoinWorldEvent event) {
        if (event.getWorld().isRemote || !HitboxConfig.ballistics.fixMineRustBullets) return;
        Entity entity = event.getEntity();
        if (!(entity instanceof EntityArrow)) return;

        String cls = entity.getClass().getName();
        if (!cls.startsWith("net.mcreator.minerustaddons.") || !cls.contains("$EntityArrowCustom")) return;

        NBTTagCompound data = entity.getEntityData();
        if (data.getBoolean(DONE_TAG)) return;
        data.setBoolean(DONE_TAG, true);

        String weapon = cls.substring(cls.lastIndexOf('.') + 1, cls.indexOf('$')).toLowerCase();
        double newSpeed = speedFor(weapon);
        if (newSpeed <= 0) return;

        EntityArrow arrow = (EntityArrow) entity;
        double speed = Math.sqrt(arrow.motionX * arrow.motionX + arrow.motionY * arrow.motionY + arrow.motionZ * arrow.motionZ);
        if (speed < 0.05D || newSpeed <= speed) return;

        // vanilla arrow damage = speed * damage, so keep the original damage at the new speed
        double k = newSpeed / speed;
        arrow.motionX *= k;
        arrow.motionY *= k;
        arrow.motionZ *= k;
        arrow.setDamage(arrow.getDamage() / k);
        if (!HitboxConfig.ballistics.bulletGravity) {
            arrow.setNoGravity(true);
        }
    }

    private static double speedFor(String weapon) {
        String[] rifles = {"akrifle", "lr300", "m249", "m39", "riflesemiauto", "boltactionrifle", "l96"};
        String[] smgs = {"thompson", "mp5", "smg", "m92", "pistolsemiauto", "python", "revolver"};
        String[] shotguns = {"pipeshotgun", "spas12", "eoka"};
        for (String s : rifles) if (weapon.equals("mcreator" + s)) return HitboxConfig.ballistics.rifleBulletSpeed;
        for (String s : smgs) if (weapon.equals("mcreator" + s)) return HitboxConfig.ballistics.pistolBulletSpeed;
        for (String s : shotguns) if (weapon.equals("mcreator" + s)) return HitboxConfig.ballistics.shotgunBulletSpeed;
        return -1; // bows, crossbow, spears, nailgun stay as they are
    }

    @SubscribeEvent
    public static void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
        if (RustHitbox.MODID.equals(event.getModID())) {
            ConfigManager.sync(RustHitbox.MODID, Config.Type.INSTANCE);
        }
    }
}
