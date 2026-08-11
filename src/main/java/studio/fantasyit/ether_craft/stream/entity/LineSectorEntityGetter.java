package studio.fantasyit.ether_craft.stream.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import studio.fantasyit.ether_craft.plating.helper.PlatingUtil;
import studio.fantasyit.ether_craft.register.Tags;
import studio.fantasyit.ether_craft.stream.vholder.VirtualEtherStream;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class LineSectorEntityGetter {
    private final int firstOffset;
    List<List<List<Entity>>> entitySections;
    List<Entity>[] flatten;
    List<Entity>[] flattenAndCanHit;
    List<AABB>[] boundingBoxes;
    Vec3i pos;
    Vec3i dirVec;
    private final int axisComp;
    private final double axisX;
    private final double axisY;
    private final double axisZ;

    public LineSectorEntityGetter(List<List<List<Entity>>> entityList, BlockPos startPos, Vec3i dirVec, int holderMaxDistance) {
        this.entitySections = entityList;
        this.pos = startPos;
        this.dirVec = dirVec;
        this.flatten = new List[entityList.size()];
        this.flattenAndCanHit = new List[entityList.size()];
        this.boundingBoxes = new List[entityList.size()];

        int posLocal = startPos.getX() * dirVec.getX() + startPos.getY() * dirVec.getY() + startPos.getZ() * dirVec.getZ();
        this.firstOffset = posLocal - ((posLocal >> 4) << 4);
        this.axisComp = dirVec.getX() != 0 ? 0 : (dirVec.getY() != 0 ? 1 : 2);
        this.axisX = pos.getX() + 0.5;
        this.axisY = pos.getY() + 0.5;
        this.axisZ = pos.getZ() + 0.5;
    }

    private int getSecIdx(int offset) {
        int t = (offset + firstOffset) >> 4;
        if (t >= this.flatten.length) return this.flatten.length - 1;
        return t;
    }

    private List<Entity> getSectionRelatedEntity(int distance) {
        int t = getSecIdx(distance);
        if (t >= this.flatten.length) return List.of();
        if (this.flatten[t] == null) {
            int tc = 0;
            for (List<Entity> el : entitySections.get(t)) {
                if (el == null) continue;
                tc += el.size();
            }
            ArrayList<Entity> objects = new ArrayList<>(tc);
            for (List<Entity> el : entitySections.get(t)) {
                if (el == null) continue;
                objects.addAll(el);
            }
            this.flatten[t] = objects;
            return objects;
        }
        return this.flatten[t];
    }

    private int prepareSectionRelatedCanHitEntity(int distance) {
        int t = getSecIdx(distance);
        if (this.flattenAndCanHit[t] == null) {
            ArrayList<Entity> objects = new ArrayList<>(getSectionRelatedEntity(distance));
            objects.removeIf(this::noHitByStream);
            ArrayList<Entity> hitList = new ArrayList<>(objects.size());
            ArrayList<AABB> boundingBoxes = new ArrayList<>(objects.size());
            for (Entity e : objects) {
                hitList.add(e);
                boundingBoxes.add(e.getBoundingBox().inflate(0.3));
            }
            this.flattenAndCanHit[t] = hitList;
            this.boundingBoxes[t] = boundingBoxes;
        }
        return t;
    }

    private boolean isOnAxisLine(Entity entity) {
        AABB bb = entity.getBoundingBox();
        if (axisComp == 0) {
            return (bb.minY - 0.3) <= axisY && (bb.maxY + 0.3) >= axisY
                    && (bb.minZ - 0.3) <= axisZ && (bb.maxZ + 0.3) >= axisZ;
        }
        if (axisComp == 1) {
            return (bb.minX - 0.3) <= axisX && (bb.maxX + 0.3) >= axisX
                    && (bb.minZ - 0.3) <= axisZ && (bb.maxZ + 0.3) >= axisZ;
        }
        return (bb.minX - 0.3) <= axisX && (bb.maxX + 0.3) >= axisX
                && (bb.minY - 0.3) <= axisY && (bb.maxY + 0.3) >= axisY;
    }

    public List<Entity> getEntityAt(int blockDistance) {
        AABB aabb = new AABB(new BlockPos(pos.getX() + dirVec.getX() * blockDistance, pos.getY() + dirVec.getY() * blockDistance, pos.getZ() + dirVec.getZ() * blockDistance));
        List<Entity> entities = getSectionRelatedEntity(blockDistance);
        List<Entity> list = new ArrayList<>();
        for (Entity entity : entities) {
            if (entity.getBoundingBox().intersects(aabb)) {
                list.add(entity);
            }
        }
        return list;
    }

    public boolean hasEntityContainsAndCanHitAt(int blockDistance, double x, double y, double z) {
        int i = prepareSectionRelatedCanHitEntity(blockDistance);
        for (AABB aabb : this.boundingBoxes[i]) {
            if (aabb.contains(x, y, z)) {
                return true;
            }
        }
        return false;
    }

    public EntityHitResult getEntityHit(int startOffset, int endOffset, Vec3 oldPos, Vec3 newPos, double nearestDist, VirtualEtherStream extraSkip) {
        Entity hitEntity = null;
        EntityHitResult hit = null;
        Vec3 entityHitAt = null;
        int s1 = getSecIdx(startOffset);
        int s2 = getSecIdx(endOffset);
        for (int i = s1; i <= s2; ++i) {
            int i1 = prepareSectionRelatedCanHitEntity(i);
            List<AABB> boxes = boundingBoxes[i1];
            List<Entity> list = flattenAndCanHit[i1];
            for (int j = 0; j < list.size(); ++j) {
                Entity entity = list.get(j);
                if (extraSkip.shouldPassThrough(entity)) continue;
                AABB bb = boxes.get(j);
                double localDist = entity.distanceToSqr(oldPos);
                boolean currentCanHit = bb.contains(oldPos) && localDist < nearestDist;
                Vec3 localHitAt = bb.getCenter();
                if (!currentCanHit) {
                    Optional<Vec3> clip = bb.clip(oldPos, newPos);
                    if (clip.isPresent()) {
                        localDist = clip.get().distanceToSqr(oldPos);
                        if (localDist < nearestDist) {
                            currentCanHit = true;
                            localHitAt = clip.get();
                        }
                    }
                }
                if (currentCanHit) {
                    nearestDist = localDist;
                    hitEntity = entity;
                    entityHitAt = localHitAt;
                }

            }
        }
        if (hitEntity != null) {
            hit = new EntityHitResult(hitEntity, entityHitAt);
        }
        return hit;
    }

    private boolean noHitByStream(Entity entity) {
        if (!isOnAxisLine(entity)) return true;
        if (entity instanceof ItemEntity ie) {
            if (PlatingUtil.isPlatedItemEntity(ie)) return false;
            return !ie.getItem().is(Items.GLASS);
        }
        return entity.is(Tags.ETHER_STREAM_PASS_THROUGH_ENTITY);
    }
}
