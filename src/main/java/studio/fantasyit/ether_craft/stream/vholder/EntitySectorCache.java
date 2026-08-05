package studio.fantasyit.ether_craft.stream.vholder;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.entity.PartEntity;
import net.minecraft.world.level.entity.EntitySection;
import net.minecraft.world.level.entity.EntitySectionStorage;
import net.minecraft.world.phys.AABB;
import studio.fantasyit.ether_craft.stream.IEntityGetter;

import java.util.ArrayList;
import java.util.List;

public class EntitySectorCache implements IEntityGetter {
    private final Long2ObjectOpenHashMap<List<Entity>> cache = new Long2ObjectOpenHashMap<>(1024);
    private long lastTick = -1;

    @Override
    public List<Entity> getEntities(ServerLevel level, AABB box) {
        long tick = level.getGameTime();
        if (lastTick != tick) {
            cache.clear();
            lastTick = tick;
        }
        EntitySectionStorage<Entity> storage = level.entityManager.sectionStorage;
        int xMin = SectionPos.posToSectionCoord(box.minX - 2.0);
        int yMin = SectionPos.posToSectionCoord(box.minY - 4.0);
        int zMin = SectionPos.posToSectionCoord(box.minZ - 2.0);
        int xMax = SectionPos.posToSectionCoord(box.maxX + 2.0);
        int yMax = SectionPos.posToSectionCoord(box.maxY);
        int zMax = SectionPos.posToSectionCoord(box.maxZ + 2.0);

        List<Entity> allEntities = new ArrayList<>();
        for (int x = xMin; x <= xMax; x++) {
            for (int y = yMin; y <= yMax; y++) {
                for (int z = zMin; z <= zMax; z++) {
                    long key = SectionPos.asLong(x, y, z);
                    EntitySection<Entity> section = storage.getSection(key);
                    if (section == null || !section.getStatus().isAccessible()) continue;
                    List<Entity> list = cache.get(key);
                    if (list == null) {
                        list = section.getEntities().toList();
                        cache.put(key, list);
                    }
                    for (Entity e : list) {
                        if (!e.isSpectator() && e.getBoundingBox().intersects(box)) {
                            allEntities.add(e);
                        }
                    }
                }
            }
        }
        for (PartEntity<?> dragonPart : level.dragonParts()) {
            if (!dragonPart.isSpectator() && box.intersects(dragonPart.getBoundingBox())) {
                allEntities.add(dragonPart);
            }
        }
        return allEntities;
    }
}
