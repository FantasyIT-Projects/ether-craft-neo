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

public class EntitySectorCache {
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
        ensureCached(storage, key);
        SectionCacheData data = sectionCache.get(key);
        if (data == null) return;
        List<Entity> list = data.all();
        if (list == null || list.isEmpty()) return;
        for (Entity e : list) {
            if (!e.isSpectator() && intersects(e, minX, minY, minZ, maxX, maxY, maxZ)) {
                result.add(e);
            }
        }
    }

    private void collectCross(EntitySectionStorage<Entity> storage, long key, double minX, double minY, double minZ, double maxX, double maxY, double maxZ, List<Entity> result) {
        ensureCached(storage, key);
        SectionCacheData data = sectionCache.get(key);
        if (data == null) return;
        List<Entity> list = data.cross();
        if (list == null || list.isEmpty()) return;
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

    private void ensureCached(EntitySectionStorage<Entity> storage, long key) {
        if (sectionCache.containsKey(key)) return;
        EntitySection<Entity> section = storage.getSection(key);
        if (section == null || !section.getStatus().isAccessible()) return;
        getAndCacheSection(section, key, SectionPos.x(key), SectionPos.y(key), SectionPos.z(key));
    }

    private void getAndCacheSection(EntitySection<Entity> section, long key, int x, int y, int z) {
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
        sectionCache.put(key, new SectionCacheData(
                all, allBoxes.toArray(new AABB[0]),
                cross, crossBoxes.toArray(new AABB[0])
        ));
    }

    public void clear() {
        sectionCache.clear();
    }

    public @Nullable LineSectorEntityGetter getLineSectorGetter(ServerLevel level, BlockPos pos, Vec3i dirVec, int holderMaxDistance) {
        if (holderMaxDistance <= 0) return null;
        int blockMinX = pos.getX();
        int blockMinY = pos.getY();
        int blockMinZ = pos.getZ();

        int blockMaxX = blockMinX + dirVec.getX() * holderMaxDistance;
        int blockMaxY = blockMinY + dirVec.getY() * holderMaxDistance;
        int blockMaxZ = blockMinZ + dirVec.getZ() * holderMaxDistance;

        int sectionMinX = blockMinX >> 4;
        int sectionMinY = blockMinY >> 4;
        int sectionMinZ = blockMinZ >> 4;

        int sectionMaxX = blockMaxX >> 4;
        int sectionMaxY = blockMaxY >> 4;
        int sectionMaxZ = blockMaxZ >> 4;

        boolean notAloneX = dirVec.getX() == 0;
        boolean notAloneY = dirVec.getY() == 0;
        boolean notAloneZ = dirVec.getZ() == 0;

        int sectionLineCount = (sectionMaxX - sectionMinX) * dirVec.getX() + (sectionMaxZ - sectionMinZ) * dirVec.getZ() + (sectionMaxY - sectionMinY) * dirVec.getY() + 1;
        if (sectionLineCount <= 0) return null;

        // 沿线垂直轴 section 坐标恒定，因此其 near 判断恒定；运动轴邻居恒包含。
        // 故邻居偏移集合对沿线所有 section 相同，可整体预计算一次。
        int[] sectionMinCoord = {sectionMinX, sectionMinY, sectionMinZ};
        int[] posCoord = {blockMinX, blockMinY, blockMinZ};
        int[] blockMaxCoord = {blockMaxX, blockMaxY, blockMaxZ};
        boolean[] notAlone = {notAloneX, notAloneY, notAloneZ};
        int[] nearMin = new int[3];
        int[] nearMax = new int[3];
        for (int a = 0; a < 3; a++) {
            int sectionBlockMin = sectionMinCoord[a] << 4;
            nearMin[a] = posCoord[a] - sectionBlockMin < 2 ? 1 : 0;
            nearMax[a] = blockMaxCoord[a] - sectionBlockMin >= 14 ? 1 : 0;
        }
        List<int[]> neighborOffsets = new ArrayList<>(26);
        for (int dx = -1; dx <= 1; dx++) {
            if (dx == -1 && notAlone[0] && nearMin[0] == 0) continue;
            if (dx == 1 && notAlone[0] && nearMax[0] == 0) continue;
            for (int dy = -1; dy <= 1; dy++) {
                if (dy == -1 && notAlone[1] && nearMin[1] == 0) continue;
                if (dy == 1 && notAlone[1] && nearMax[1] == 0) continue;
                for (int dz = -1; dz <= 1; dz++) {
                    if (dz == -1 && notAlone[2] && nearMin[2] == 0) continue;
                    if (dz == 1 && notAlone[2] && nearMax[2] == 0) continue;
                    if (dx == 0 && dy == 0 && dz == 0) continue;
                    neighborOffsets.add(new int[]{dx, dy, dz});
                }
            }
        }

        EntitySectionStorage<Entity> storage = level.entityManager.sectionStorage;
        List<List<LineSectorEntityGetter.SectionEntityList>> entitySections = new ArrayList<>(sectionLineCount);
        Collection<PartEntity<?>> partEntities = level.dragonParts();
        for (int sectionDistance = 0; sectionDistance < sectionLineCount; sectionDistance++) {
            int sectionX = sectionMinX + dirVec.getX() * sectionDistance;
            int sectionY = sectionMinY + dirVec.getY() * sectionDistance;
            int sectionZ = sectionMinZ + dirVec.getZ() * sectionDistance;

            int sectionBlockMinX = sectionX << 4;
            int sectionBlockMinY = sectionY << 4;
            int sectionBlockMinZ = sectionZ << 4;

            double scanMinX = Math.max(blockMinX, sectionBlockMinX);
            double scanMinY = Math.max(blockMinY, sectionBlockMinY);
            double scanMinZ = Math.max(blockMinZ, sectionBlockMinZ);
            double scanMaxX = Math.min(blockMaxX, sectionBlockMinX + 15) + 1.0;
            double scanMaxY = Math.min(blockMaxY, sectionBlockMinY + 15) + 1.0;
            double scanMaxZ = Math.min(blockMaxZ, sectionBlockMinZ + 15) + 1.0;

            long key = SectionPos.asLong(sectionX, sectionY, sectionZ);
            List<LineSectorEntityGetter.SectionEntityList> list = new ArrayList<>(neighborOffsets.size() + 2);

            ensureCached(storage, key);
            SectionCacheData self = sectionCache.get(key);
            if (self != null && !self.all().isEmpty()) {
                list.add(new LineSectorEntityGetter.SectionEntityList(self.all(), self.allBoxes()));
            }

            for (int[] off : neighborOffsets) {
                long ok = SectionPos.offset(key, off[0], off[1], off[2]);
                ensureCached(storage, ok);
                SectionCacheData data = sectionCache.get(ok);
                if (data != null && !data.cross().isEmpty()) {
                    list.add(new LineSectorEntityGetter.SectionEntityList(data.cross(), data.crossBoxes()));
                }
            }

            if (!partEntities.isEmpty()) {
                List<Entity> elc = new ArrayList<>();
                List<AABB> elcBoxes = new ArrayList<>();
                for (PartEntity<?> dragonPart : partEntities) {
                    if (!dragonPart.isSpectator() && intersects(dragonPart, scanMinX, scanMinY, scanMinZ, scanMaxX, scanMaxY, scanMaxZ)) {
                        elc.add(dragonPart);
                        elcBoxes.add(dragonPart.getBoundingBox().inflate(0.3));
                    }
                }
                if (!elc.isEmpty()) {
                    list.add(new LineSectorEntityGetter.SectionEntityList(elc, elcBoxes.toArray(new AABB[0])));
                }
            }
            entitySections.add(list);
        }
        return new LineSectorEntityGetter(entitySections, pos, dirVec, holderMaxDistance);
    }
}
