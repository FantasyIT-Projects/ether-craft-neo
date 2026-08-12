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

public class LineSectorEntityGetter {
    record SectionEntityList(List<Entity> entities, AABB[] boxes) {
    }

    private final int firstOffset;
    List<List<SectionEntityList>> entitySections;
    Entity[][] flattenAndCanHit;
    AABB[][] boundingBoxes;
    Vec3i pos;
    Vec3i dirVec;
    private final int axisComp;
    private final double axisXMin;
    private final double axisXMax;
    private final double axisYMin;
    private final double axisYMax;
    private final double axisZMin;
    private final double axisZMax;

    public LineSectorEntityGetter(List<List<SectionEntityList>> entityList, BlockPos startPos, Vec3i dirVec, int holderMaxDistance) {
        this.entitySections = entityList;
        this.pos = startPos;
        this.dirVec = dirVec;
        this.flattenAndCanHit = new Entity[entityList.size()][];
        this.boundingBoxes = new AABB[entityList.size()][];

        int posLocal = startPos.getX() * dirVec.getX() + startPos.getY() * dirVec.getY() + startPos.getZ() * dirVec.getZ();
        int dirSign = dirVec.getX() + dirVec.getY() + dirVec.getZ();
        int posCoord = posLocal * dirSign;
        int local = posCoord & 15;
        this.firstOffset = dirSign > 0 ? local : 15 - local;
        this.axisComp = dirVec.getX() != 0 ? 0 : (dirVec.getY() != 0 ? 1 : 2);
        double axisX = pos.getX() + 0.5;
        double axisY = pos.getY() + 0.5;
        double axisZ = pos.getZ() + 0.5;
        this.axisXMin = axisX - 0.3;
        this.axisXMax = axisX + 0.3;
        this.axisYMin = axisY - 0.3;
        this.axisYMax = axisY + 0.3;
        this.axisZMin = axisZ - 0.3;
        this.axisZMax = axisZ + 0.3;
    }

    private int getSecIdx(int offset) {
        int t = (offset + firstOffset) >> 4;
        if (t < 0) return 0;
        if (t >= this.flattenAndCanHit.length) return this.flattenAndCanHit.length - 1;
        return t;
    }

    private int prepareSectionRelatedCanHitEntity(int section) {
        if (this.flattenAndCanHit[section] == null) {
            List<SectionEntityList> sectionList = entitySections.get(section);
            int count = 0;
            for (int k = 0; k < sectionList.size(); k++) {
                SectionEntityList sel = sectionList.get(k);
                if (sel == null) continue;
                List<Entity> entities = sel.entities();
                for (int i = 0; i < entities.size(); i++) {
                    if (!noHitByStream(entities.get(i))) count++;
                }
            }
            Entity[] hitList = new Entity[count];
            AABB[] boxList = new AABB[count];
            int fill = 0;
            for (int k = 0; k < sectionList.size(); k++) {
                SectionEntityList sel = sectionList.get(k);
                if (sel == null) continue;
                List<Entity> entities = sel.entities();
                AABB[] boxes = sel.boxes();
                for (int i = 0; i < entities.size(); i++) {
                    Entity e = entities.get(i);
                    if (noHitByStream(e)) continue;
                    hitList[fill] = e;
                    boxList[fill] = boxes[i];
                    fill++;
                }
            }
            this.flattenAndCanHit[section] = hitList;
            this.boundingBoxes[section] = boxList;
        }
        return section;
    }

    private boolean isOnAxisLine(Entity entity) {
        AABB bb = entity.getBoundingBox();
        if (axisComp == 0) {
            return bb.minY <= axisYMax && bb.maxY >= axisYMin
                    && bb.minZ <= axisZMax && bb.maxZ >= axisZMin;
        }
        if (axisComp == 1) {
            return bb.minX <= axisXMax && bb.maxX >= axisXMin
                    && bb.minZ <= axisZMax && bb.maxZ >= axisZMin;
        }
        return bb.minX <= axisXMax && bb.maxX >= axisXMin
                && bb.minY <= axisYMax && bb.maxY >= axisYMin;
    }

    public List<Entity> getEntityAt(int blockDistance) {
        int x = pos.getX() + dirVec.getX() * blockDistance;
        int y = pos.getY() + dirVec.getY() * blockDistance;
        int z = pos.getZ() + dirVec.getZ() * blockDistance;
        int t = getSecIdx(blockDistance);
        List<Entity> list = null;
        List<SectionEntityList> sectionList = entitySections.get(t);
        for (int k = 0; k < sectionList.size(); k++) {
            SectionEntityList sel = sectionList.get(k);
            if (sel == null) continue;
            List<Entity> entities = sel.entities();
            for (int i = 0; i < entities.size(); i++) {
                Entity entity = entities.get(i);
                if (AABBRayHit.unitBoxIntersects(entity.getBoundingBox(), x, y, z)) {
                    if (list == null) list = new ArrayList<>();
                    list.add(entity);
                }
            }
        }
        return list == null ? List.of() : list;
    }

    public boolean hasEntityContainsAndCanHitAt(int blockDistance, double x, double y, double z) {
        int i = prepareSectionRelatedCanHitEntity(getSecIdx(blockDistance));
        for (AABB aabb : this.boundingBoxes[i]) {
            if (aabb.contains(x, y, z)) {
                return true;
            }
        }
        return false;
    }

    public EntityHitResult getEntityHit(int startOffset, int endOffset,
                                        double oldX, double oldY, double oldZ,
                                        double newX, double newY, double newZ,
                                        double nearestDist, VirtualEtherStream extraSkip) {
        double dx = newX - oldX;
        double dy = newY - oldY;
        double dz = newZ - oldZ;
        double rayLenSqr = dx * dx + dy * dy + dz * dz;
        Entity hitEntity = null;
        Vec3 entityHitAt = null;
        int s1 = getSecIdx(startOffset - 1);
        int s2 = getSecIdx(endOffset + 1);
        for (int i = s1; i <= s2; ++i) {
            int i1 = prepareSectionRelatedCanHitEntity(i);
            AABB[] boxes = boundingBoxes[i1];
            Entity[] list = flattenAndCanHit[i1];
            for (int j = 0; j < list.length; ++j) {
                Entity entity = list[j];
                if (extraSkip.shouldPassThrough(entity)) continue;
                AABB bb = boxes[j];
                if (AABBRayHit.contains(bb, oldX, oldY, oldZ)) {
                    double localDist = entity.distanceToSqr(oldX, oldY, oldZ);
                    if (localDist < nearestDist) {
                        nearestDist = localDist;
                        hitEntity = entity;
                        entityHitAt = bb.getCenter();
                    }
                } else {
                    double t = AABBRayHit.clip(bb, oldX, oldY, oldZ, dx, dy, dz);
                    if (t > 0) {
                        double localDist = t * t * rayLenSqr;
                        if (localDist < nearestDist) {
                            nearestDist = localDist;
                            hitEntity = entity;
                            entityHitAt = new Vec3(oldX + t * dx, oldY + t * dy, oldZ + t * dz);
                        }
                    }
                }
            }
        }
        if (hitEntity != null) {
            return new EntityHitResult(hitEntity, entityHitAt);
        }
        return null;
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
