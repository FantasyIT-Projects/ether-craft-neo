package studio.fantasyit.ether_craft.attachment;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import studio.fantasyit.ether_craft.register.EntityRegistry;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChainedEmitterEntityHitCache {
    public record PosDir(BlockPos pos, Direction dir) {
    }

    private Map<PosDir, List<Entity>> cache = new HashMap<>();
    private Map<PosDir, Float> maxDist = new HashMap<>();
    private Map<PosDir, Float> curDist = new HashMap<>();

    public void beforeTick() {
        maxDist.clear();
        for (PosDir posDir : curDist.keySet())
            maxDist.put(posDir, curDist.get(posDir));
        cache.clear();
        curDist.clear();
    }

    @SuppressWarnings("deprecation")
    private @Nullable List<Entity> getAllEntitiesRect(Entity source, PosDir posDir) {
        if (cache.containsKey(posDir)) return cache.get(posDir);
        float dist = (float) source.position().distanceTo(posDir.pos.getCenter());
        if (!curDist.containsKey(posDir) || dist > curDist.get(posDir)) {
            curDist.put(posDir, dist);
        }
        if (!maxDist.containsKey(posDir)) return null;
        Vec3 unit = posDir.dir.getUnitVec3().scale(maxDist.get(posDir) + 0.5f);
        List<Entity> entities = source.level().getEntities(
                source,
                new AABB(posDir.pos).expandTowards(unit).inflate(1)
        );
        List<Entity> result = new ArrayList<>();
        for (Entity entity : entities) {
            if (entity.is(EntityRegistry.ETHER_STREAM_ENTITY)) continue;
            result.add(entity);
        }
        cache.put(posDir, result);
        return result;
    }

    public @Nullable List<Entity> getAllEntities(Entity source, PosDir posDir, float backwardDist, float forwardDist) {
        List<Entity> result = getAllEntitiesRect(source, posDir);
        if (result == null) return null;
        float dist = (float) source.position().distanceTo(posDir.pos.getCenter());
        List<Entity> filtered = new ArrayList<>();
        for (Entity entity : result) {
            float newDist = (float) entity.position().distanceTo(posDir.pos.getCenter());
            if (dist - backwardDist < newDist && newDist < dist + forwardDist)
                filtered.add(entity);
        }
        return filtered;
    }

    public @Nullable List<Entity> getAllEntities(Level level, Vec3 pos, PosDir posDir, float backwardDist, float forwardDist) {
        float dist = (float) pos.distanceTo(posDir.pos.getCenter());
        if (!curDist.containsKey(posDir) || dist > curDist.get(posDir)) {
            curDist.put(posDir, dist);
        }
        if (!maxDist.containsKey(posDir)) return null;

        if (!cache.containsKey(posDir)) {
            Vec3 unit = posDir.dir.getUnitVec3().scale(maxDist.get(posDir) + 0.5f);
            List<Entity> entities = level.getEntities(
                    null,
                    new AABB(posDir.pos).expandTowards(unit).inflate(1)
            );
            List<Entity> result = new ArrayList<>();
            for (Entity entity : entities) {
                result.add(entity);
            }
            cache.put(posDir, result);
        }

        List<Entity> result = cache.get(posDir);
        List<Entity> filtered = new ArrayList<>();
        for (Entity entity : result) {
            float newDist = (float) entity.position().distanceTo(posDir.pos.getCenter());
            if (dist - backwardDist < newDist && newDist < dist + forwardDist)
                filtered.add(entity);
        }
        return filtered;
    }
}
