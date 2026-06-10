package studio.fantasyit.ether_craft.plating.trigger.inst;

import net.minecraft.world.entity.LivingEntity;
import studio.fantasyit.ether_craft.plating.data.PlatingData;

public interface IEffectStartAndEndTrigger {
    void onEffectStarts(LivingEntity entity, PlatingData platingData);
    void onEffectEnds(LivingEntity entity);
}
