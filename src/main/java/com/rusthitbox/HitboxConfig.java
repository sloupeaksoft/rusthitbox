package com.rusthitbox;

import net.minecraftforge.common.config.Config;

@Config(modid = RustHitbox.MODID, name = RustHitbox.MODID)
public class HitboxConfig {

    @Config.Comment("Enable body-part hitboxes for players")
    public static boolean enabled = true;

    @Config.Comment("Damage multiplier: head")
    @Config.RangeDouble(min = 0.0D, max = 10.0D)
    public static double headMultiplier = 2.0D;

    @Config.Comment("Damage multiplier: body (chest)")
    @Config.RangeDouble(min = 0.0D, max = 10.0D)
    public static double bodyMultiplier = 1.0D;

    @Config.Comment("Damage multiplier: arms")
    @Config.RangeDouble(min = 0.0D, max = 10.0D)
    public static double armMultiplier = 0.5D;

    @Config.Comment("Damage multiplier: legs")
    @Config.RangeDouble(min = 0.0D, max = 10.0D)
    public static double legMultiplier = 0.6D;

    @Config.Comment("Bullets/arrows that pass next to the model (not touching head, body, arms or legs) fly through")
    public static boolean preciseProjectiles = true;

    @Config.Comment("Extra size of every hitbox in blocks (compensates network lag)")
    @Config.RangeDouble(min = 0.0D, max = 0.5D)
    public static double hitboxPadding = 0.05D;

    @Config.Comment("Show the hit body part to the shooter above the hotbar")
    public static boolean showHitMessage = true;

    @Config.Comment("MineRust Addons bullets")
    public static Ballistics ballistics = new Ballistics();

    public static class Ballistics {
        @Config.Comment("Make MineRust Addons guns shoot fast bullets (damage stays the same)")
        public boolean fixMineRustBullets = true;

        @Config.Comment("Rifle bullet speed, blocks per tick (AK, LR-300, M249, M39, SAR, Bolt, L96)")
        @Config.RangeDouble(min = 0.5D, max = 20.0D)
        public double rifleBulletSpeed = 7.0D;

        @Config.Comment("Pistol/SMG bullet speed, blocks per tick (Thompson, MP5, Custom SMG, M92, P2, Python, Revolver)")
        @Config.RangeDouble(min = 0.5D, max = 20.0D)
        public double pistolBulletSpeed = 5.0D;

        @Config.Comment("Shotgun speed, blocks per tick (Pipe shotgun, Spas-12, Eoka)")
        @Config.RangeDouble(min = 0.5D, max = 20.0D)
        public double shotgunBulletSpeed = 4.0D;

        @Config.Comment("Bullet drop (gravity). false = bullets fly perfectly straight")
        public boolean bulletGravity = true;
    }
}
