package studio.fantasyit.ether_craft.stream.entity;

import net.minecraft.world.phys.AABB;

public final class AABBRayHit {
    private static final double EPSILON = 1.0E-7;

    private AABBRayHit() {
    }

    /**
     * 手写 slab 射线与 AABB 求交。返回射线参数 t（oldPos + t * dir 为命中点），
     * 命中要求 0 < t < 1，未命中返回 -1。语义对齐 {@link AABB#clip(net.minecraft.world.phys.Vec3, net.minecraft.world.phys.Vec3)}，
     * 但不做任何对象分配。
     */
    public static double clip(AABB bb, double ox, double oy, double oz, double dx, double dy, double dz) {
        double tMin = 0.0;
        double tMax = 1.0;

        if (Math.abs(dx) < EPSILON) {
            if (ox < bb.minX || ox > bb.maxX) return -1;
        } else {
            double invD = 1.0 / dx;
            double t1 = (bb.minX - ox) * invD;
            double t2 = (bb.maxX - ox) * invD;
            double tNear = Math.min(t1, t2);
            double tFar = Math.max(t1, t2);
            tMin = Math.max(tMin, tNear);
            tMax = Math.min(tMax, tFar);
            if (tMin > tMax) return -1;
        }

        if (Math.abs(dy) < EPSILON) {
            if (oy < bb.minY || oy > bb.maxY) return -1;
        } else {
            double invD = 1.0 / dy;
            double t1 = (bb.minY - oy) * invD;
            double t2 = (bb.maxY - oy) * invD;
            double tNear = Math.min(t1, t2);
            double tFar = Math.max(t1, t2);
            tMin = Math.max(tMin, tNear);
            tMax = Math.min(tMax, tFar);
            if (tMin > tMax) return -1;
        }

        if (Math.abs(dz) < EPSILON) {
            if (oz < bb.minZ || oz > bb.maxZ) return -1;
        } else {
            double invD = 1.0 / dz;
            double t1 = (bb.minZ - oz) * invD;
            double t2 = (bb.maxZ - oz) * invD;
            double tNear = Math.min(t1, t2);
            double tFar = Math.max(t1, t2);
            tMin = Math.max(tMin, tNear);
            tMax = Math.min(tMax, tFar);
            if (tMin > tMax) return -1;
        }

        if (tMin > 0.0 && tMin < 1.0) return tMin;
        return -1;
    }

    public static boolean contains(AABB bb, double x, double y, double z) {
        return bb.contains(x, y, z);
    }

    /** 单位方块盒 [x, x+1)³ 与实体 AABB 是否相交，无分配。 */
    public static boolean unitBoxIntersects(AABB bb, int x, int y, int z) {
        return bb.maxX > x && bb.minX < x + 1.0
                && bb.maxY > y && bb.minY < y + 1.0
                && bb.maxZ > z && bb.minZ < z + 1.0;
    }
}
