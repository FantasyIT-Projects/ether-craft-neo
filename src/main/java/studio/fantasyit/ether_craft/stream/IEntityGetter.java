package studio.fantasyit.ether_craft.stream;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;

import java.util.List;

public interface IEntityGetter {
    List<Entity> getEntities(ServerLevel level, AABB box);

    IEntityGetter DUMMY = (level, box) -> level.getEntities((Entity) null, box);
}
