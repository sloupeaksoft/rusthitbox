package com.rusthitbox;

/**
 * Player model parts in skin pixels (the model is 32 px tall).
 * X = left/right, Y = up from the feet, Z = forward/back.
 */
public enum BodyPart {
    HEAD(-4, 24, -4, 4, 32, 4, true, "\u00a7c", "\u0413\u041e\u041b\u041e\u0412\u0410"),
    BODY(-4, 12, -2, 4, 24, 2, false, "\u00a76", "\u0422\u0415\u041b\u041e"),
    RIGHT_ARM(-8, 12, -2, -4, 24, 2, false, "\u00a7e", "\u041f\u0420\u0410\u0412\u0410\u042f \u0420\u0423\u041a\u0410"),
    LEFT_ARM(4, 12, -2, 8, 24, 2, false, "\u00a7e", "\u041b\u0415\u0412\u0410\u042f \u0420\u0423\u041a\u0410"),
    RIGHT_LEG(-4, 0, -2, 0, 12, 2, false, "\u00a7a", "\u041f\u0420\u0410\u0412\u0410\u042f \u041d\u041e\u0413\u0410"),
    LEFT_LEG(0, 0, -2, 4, 12, 2, false, "\u00a7a", "\u041b\u0415\u0412\u0410\u042f \u041d\u041e\u0413\u0410");

    public final double minX, minY, minZ, maxX, maxY, maxZ;
    public final boolean followsHead;
    public final String color;
    public final String title;

    BodyPart(double minX, double minY, double minZ, double maxX, double maxY, double maxZ,
             boolean followsHead, String color, String title) {
        this.minX = minX;
        this.minY = minY;
        this.minZ = minZ;
        this.maxX = maxX;
        this.maxY = maxY;
        this.maxZ = maxZ;
        this.followsHead = followsHead;
        this.color = color;
        this.title = title;
    }

    public double multiplier() {
        switch (this) {
            case HEAD: return HitboxConfig.headMultiplier;
            case BODY: return HitboxConfig.bodyMultiplier;
            case RIGHT_ARM:
            case LEFT_ARM: return HitboxConfig.armMultiplier;
            default: return HitboxConfig.legMultiplier;
        }
    }
}
