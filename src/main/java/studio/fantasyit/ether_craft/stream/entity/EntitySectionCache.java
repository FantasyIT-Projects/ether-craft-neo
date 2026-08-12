package studio.fantasyit.ether_craft.stream.entity;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.entity.EntitySection;
import net.minecraft.world.level.entity.EntitySectionStorage;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.entity.PartEntity;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class EntitySectionCache {
    record SectionCacheData(List<Entity> all, AABB[] allBoxes,
                            List<Entity> cross, AABB[] crossBoxes) {
    }

    private final Long2ObjectOpenHashMap<SectionCacheData> sectionCache = new Long2ObjectOpenHashMap<>(1024);

    public List<Entity> getEntities(ServerLevel level, BlockPos pos) {
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();
        return query(level, x, y, z, x + 1.0, y + 1.0, z + 1.0);
    }

    public List<Entity> getEntities(ServerLevel level, BlockPos pos1, BlockPos pos2) {
        int minX = Math.min(pos1.getX(), pos2.getX());
        int minY = Math.min(pos1.getY(), pos2.getY());
        int minZ = Math.min(pos1.getZ(), pos2.getZ());
        int maxX = Math.max(pos1.getX(), pos2.getX());
        int maxY = Math.max(pos1.getY(), pos2.getY());
        int maxZ = Math.max(pos1.getZ(), pos2.getZ());
        return query(level, minX, minY, minZ, maxX + 1.0, maxY + 1.0, maxZ + 1.0);
    }

    public List<Entity> getEntities(ServerLevel level, AABB box) {
        return query(level, box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ);
    }

    private List<Entity> query(ServerLevel level, double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        EntitySectionStorage<Entity> storage = level.entityManager.sectionStorage;
        List<Entity> result = new ArrayList<>();
        int blockMinX = (int) minX;
        int blockMinY = (int) minY;
        int blockMinZ = (int) minZ;

        int blockMaxX = (int) maxX - 1;
        int blockMaxY = (int) maxY - 1;
        int blockMaxZ = (int) maxZ - 1;

        int sectionMinX = blockMinX >> 4;
        int sectionMinY = blockMinY >> 4;
        int sectionMinZ = blockMinZ >> 4;

        int sectionMaxX = blockMaxX >> 4;
        int sectionMaxY = blockMaxY >> 4;
        int sectionMaxZ = blockMaxZ >> 4;

        for (int sectionX = sectionMinX; sectionX <= sectionMaxX; sectionX++) {
            int sectionBlockMinX = sectionX << 4;
            boolean nearMinX = (Math.max(blockMinX, sectionBlockMinX) - sectionBlockMinX) < 2;
            boolean nearMaxX = (Math.min(blockMaxX, sectionBlockMinX + 15) - sectionBlockMinX) >= 14;

            for (int sectionY = sectionMinY; sectionY <= sectionMaxY; sectionY++) {
                int sectionBlockMinY = sectionY << 4;
                boolean nearMinY = (Math.max(blockMinY, sectionBlockMinY) - sectionBlockMinY) < 2;
                boolean nearMaxY = (Math.min(blockMaxY, sectionBlockMinY + 15) - sectionBlockMinY) >= 14;

                for (int sectionZ = sectionMinZ; sectionZ <= sectionMaxZ; sectionZ++) {
                    int sectionBlockMinZ = sectionZ << 4;
                    boolean nearMinZ = (Math.max(blockMinZ, sectionBlockMinZ) - sectionBlockMinZ) < 2;
                    boolean nearMaxZ = (Math.min(blockMaxZ, sectionBlockMinZ + 15) - sectionBlockMinZ) >= 14;

                    long key = SectionPos.asLong(sectionX, sectionY, sectionZ);
                    collectAll(storage, key, minX, minY, minZ, maxX, maxY, maxZ, result);
                    // 收集所有相邻区块（6个正交 + 12个对角线 + 8个顶角）
                    for (int dx = -1; dx <= 1; dx++) {
                        if (dx == -1 && !(nearMinX && sectionX == sectionMinX)) continue;
                        if (dx == 1 && !(nearMaxX && sectionX == sectionMaxX)) continue;

                        for (int dy = -1; dy <= 1; dy++) {
                            if (dy == -1 && !(nearMinY && sectionY == sectionMinY)) continue;
                            if (dy == 1 && !(nearMaxY && sectionY == sectionMaxY)) continue;

                            for (int dz = -1; dz <= 1; dz++) {
                                if (dz == -1 && !(nearMinZ && sectionZ == sectionMinZ)) continue;
                                if (dz == 1 && !(nearMaxZ && sectionZ == sectionMaxZ)) continue;
                                if (dx == 0 && dy == 0 && dz == 0) continue; // 跳过自身

                                collectCross(storage, SectionPos.offset(key, dx, dy, dz),
                                        minX, minY, minZ, maxX, maxY, maxZ, result);
                            }
                        }
                    }
                }
            }
        }
        for (PartEntity<?> dragonPart : level.dragonParts()) {
            if (!dragonPart.isSpectator() && intersects(dragonPart, minX, minY, minZ, maxX, maxY, maxZ)) {
                result.add(dragonPart);
            }
        }
        if (result.isEmpty()) return new ArrayList<>();
        return result;
    }

    private void collectAll(EntitySectionStorage<Entity> storage, long key, double minX, double minY, double minZ, double maxX, double maxY, double maxZ, List<Entity> result) {
        SectionCacheData data = getOrCacheSection(storage, key);
        if (data == null) return;
        List<Entity> list = data.all();
        if (list.isEmpty()) return;
        for (Entity e : list) {
            if (!e.isSpectator() && intersects(e, minX, minY, minZ, maxX, maxY, maxZ)) {
                result.add(e);
            }
        }
    }

    private void collectCross(EntitySectionStorage<Entity> storage, long key, double minX, double minY, double minZ, double maxX, double maxY, double maxZ, List<Entity> result) {
        SectionCacheData data = getOrCacheSection(storage, key);
        if (data == null) return;
        List<Entity> list = data.cross();
        if (list.isEmpty()) return;
        for (Entity e : list) {
            if (!e.isSpectator() && intersects(e, minX, minY, minZ, maxX, maxY, maxZ)) {
                result.add(e);
            }
        }
    }

    private static boolean intersects(Entity e, double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        AABB bb = e.getBoundingBox();
        return bb.maxX > minX && bb.minX < maxX
                && bb.maxY > minY && bb.minY < maxY
                && bb.maxZ > minZ && bb.minZ < maxZ;
    }

    private @Nullable SectionCacheData getOrCacheSection(EntitySectionStorage<Entity> storage, long key) {
        SectionCacheData d = sectionCache.get(key);
        if (d == null) {
            EntitySection<Entity> section = storage.getSection(key);
            if (section == null || !section.getStatus().isAccessible()) return null;
            d = getAndCacheSection(section, key, SectionPos.x(key), SectionPos.y(key), SectionPos.z(key));
        }
        return d;
    }

    private SectionCacheData getAndCacheSection(EntitySection<Entity> section, long key, int x, int y, int z) {
        List<Entity> all = new ArrayList<>();
        List<AABB> allBoxes = new ArrayList<>();
        List<Entity> cross = new ArrayList<>();
        List<AABB> crossBoxes = new ArrayList<>();
        int minX = x << 4;
        int minY = y << 4;
        int minZ = z << 4;
        int maxX = minX + 16;
        int maxY = minY + 16;
        int maxZ = minZ + 16;

        section.getEntities().forEach(entity -> {
            all.add(entity);
            AABB inflated = entity.getBoundingBox().inflate(0.3);
            allBoxes.add(inflated);
            if (
                    inflated.minX < minX || inflated.maxX > maxX ||
                            inflated.minZ < minZ || inflated.maxZ > maxZ ||
                            inflated.minY < minY || inflated.maxY > maxY
            ) {
                cross.add(entity);
                crossBoxes.add(inflated);
            }
        });
        SectionCacheData data = new SectionCacheData(
                all, allBoxes.toArray(new AABB[0]),
                cross, crossBoxes.toArray(new AABB[0])
        );
        sectionCache.put(key, data);
        return data;
    }

    public void clear() {
        sectionCache.clear();
    }

    public @Nullable LineSectionEntityGetter getLineSectorGetter(ServerLevel level, BlockPos pos, Vec3i dirVec, int holderMaxDistance) {
        if (holderMaxDistance <= 0) return null;
        int a = dirVec.getX() != 0 ? 0 : (dirVec.getY() != 0 ? 1 : 2);
        int p1 = (a + 1) % 3;
        int p2 = (a + 2) % 3;
        int dirSign = coord(dirVec, a);

        int blockMin = coord(pos, a);
        int blockMax = blockMin + dirSign * holderMaxDistance;
        int sFirst = blockMin >> 4;
        int sLast = blockMax >> 4;
        int lineCount = Math.abs(sLast - sFirst) + 1;

        int c1 = coord(pos, p1) >> 4;
        int c2 = coord(pos, p2) >> 4;
        int off1 = coord(pos, p1) - (c1 << 4);
        int off2 = coord(pos, p2) - (c2 << 4);
        boolean nearMin1 = off1 < 2;
        boolean nearMax1 = off1 >= 14;
        boolean nearMin2 = off2 < 2;
        boolean nearMax2 = off2 >= 14;

        int dim1 = 1 + (nearMin1 ? 1 : 0) + (nearMax1 ? 1 : 0);
        int dim2 = 1 + (nearMin2 ? 1 : 0) + (nearMax2 ? 1 : 0);
        int base1 = nearMin1 ? 1 : 0;
        int base2 = nearMin2 ? 1 : 0;
        int widthA = lineCount + 2;
        int strideI = dim1 * dim2;

        EntitySectionStorage<Entity> storage = level.entityManager.sectionStorage;
        SectionCacheData[] grid = new SectionCacheData[widthA * strideI];
        for (int i = 0; i < widthA; i++) {
            int cA = sFirst - dirSign + dirSign * i;
            for (int j = 0; j < dim1; j++) {
                int cB = c1 - base1 + j;
                for (int k = 0; k < dim2; k++) {
                    int cC = c2 - base2 + k;
                    long key = keyOf(a, cA, cB, cC);
                    grid[i * strideI + j * dim2 + k] = getOrCacheSection(storage, key);
                }
            }
        }

        Entity[][] rawEntities = new Entity[lineCount][];
        AABB[][] rawBoxes = new AABB[lineCount][];
        Collection<PartEntity<?>> parts = level.dragonParts();
        boolean hasParts = !parts.isEmpty();
        double p1c = coord(pos, p1);
        double p2c = coord(pos, p2);
        double lineMin = Math.min(blockMin, blockMax);
        double lineMax = Math.max(blockMin, blockMax);

        for (int t = 0; t < lineCount; t++) {
            int sectionBlockMin = (sFirst + dirSign * t) << 4;
            double scanMinA = Math.max(lineMin, sectionBlockMin);
            double scanMaxA = Math.min(lineMax, sectionBlockMin + 16);

            int count = 0;
            for (int gi = t; gi <= t + 2; gi++) {
                int row = gi * strideI;
                for (int j = 0; j < dim1; j++) {
                    int cell = row + j * dim2;
                    for (int k = 0; k < dim2; k++) {
                        SectionCacheData d = grid[cell + k];
                        if (d == null) continue;
                        count += (gi == t + 1 && j == base1 && k == base2) ? d.all().size() : d.cross().size();
                    }
                }
            }
            if (hasParts) {
                for (PartEntity<?> dp : parts) {
                    if (!dp.isSpectator() && intersectsAxis(dp, a, scanMinA, scanMaxA, p1c, p2c)) count++;
                }
            }

            Entity[] raw = new Entity[count];
            AABB[] boxes = new AABB[count];
            int fill = 0;
            for (int gi = t; gi <= t + 2; gi++) {
                int row = gi * strideI;
                for (int j = 0; j < dim1; j++) {
                    int cell = row + j * dim2;
                    for (int k = 0; k < dim2; k++) {
                        SectionCacheData d = grid[cell + k];
                        if (d == null) continue;
                        if (gi == t + 1 && j == base1 && k == base2) {
                            fill = appendEntities(d.all(), d.allBoxes(), raw, boxes, fill);
                        } else {
                            fill = appendEntities(d.cross(), d.crossBoxes(), raw, boxes, fill);
                        }
                    }
                }
            }
            if (hasParts) {
                for (PartEntity<?> dp : parts) {
                    if (!dp.isSpectator() && intersectsAxis(dp, a, scanMinA, scanMaxA, p1c, p2c)) {
                        raw[fill] = dp;
                        boxes[fill] = dp.getBoundingBox().inflate(0.3);
                        fill++;
                    }
                }
            }
            rawEntities[t] = raw;
            rawBoxes[t] = boxes;
        }
        return new LineSectionEntityGetter(rawEntities, rawBoxes, pos, dirVec, holderMaxDistance);
    }

    private static int coord(Vec3i v, int axis) {
        return switch (axis) {
            case 0 -> v.getX();
            case 1 -> v.getY();
            default -> v.getZ();
        };
    }

    private static long keyOf(int axis, int cA, int cB, int cC) {
        return switch (axis) {
            case 0 -> SectionPos.asLong(cA, cB, cC);
            case 1 -> SectionPos.asLong(cC, cA, cB);
            default -> SectionPos.asLong(cB, cC, cA);
        };
    }

    private static boolean intersectsAxis(Entity e, int axis, double minA, double maxA, double p1c, double p2c) {
        AABB bb = e.getBoundingBox();
        return switch (axis) {
            case 0 -> bb.maxX > minA && bb.minX < maxA
                    && bb.maxY > p1c && bb.minY < p1c + 1.0
                    && bb.maxZ > p2c && bb.minZ < p2c + 1.0;
            case 1 -> bb.maxY > minA && bb.minY < maxA
                    && bb.maxZ > p1c && bb.minZ < p1c + 1.0
                    && bb.maxX > p2c && bb.minX < p2c + 1.0;
            default -> bb.maxZ > minA && bb.minZ < maxA
                    && bb.maxX > p1c && bb.minX < p1c + 1.0
                    && bb.maxY > p2c && bb.minY < p2c + 1.0;
        };
    }

    private static int appendEntities(List<Entity> src, AABB[] srcBoxes, Entity[] raw, AABB[] boxes, int fill) {
        for (int i = 0; i < src.size(); i++) {
            raw[fill] = src.get(i);
            boxes[fill] = srcBoxes[i];
            fill++;
        }
        return fill;
    }
}
