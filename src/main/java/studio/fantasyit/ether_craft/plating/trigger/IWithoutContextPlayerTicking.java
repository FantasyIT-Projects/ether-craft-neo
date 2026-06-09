package studio.fantasyit.ether_craft.plating.trigger;

import net.minecraft.world.entity.LivingEntity;

public interface IWithoutContextPlayerTicking {
    void tickPlayer(LivingEntity entity);
}
