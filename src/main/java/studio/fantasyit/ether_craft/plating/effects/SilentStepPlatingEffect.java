package studio.fantasyit.ether_craft.plating.effects;

import net.minecraft.resources.Identifier;
import net.minecraft.tags.GameEventTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.VanillaGameEvent;
import studio.fantasyit.ether_craft.Config;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.plating.data.PlatingData;
import studio.fantasyit.ether_craft.plating.helper.PlatingUtil;
import studio.fantasyit.ether_craft.plating.trigger.event.IPlatingGameEventTrigger;

public class SilentStepPlatingEffect implements IPlatingEffect, IPlatingGameEventTrigger {
    public static final Identifier ID = EtherCraft.id("silent_step");

    @Override
    public Identifier getId() {
        return ID;
    }

    @Override
    public void apply(IPlatingEffect effect, PlatingData data, ItemStack stack, LivingEntity entity, VanillaGameEvent event) {
        if (event.getVanillaEvent().is(GameEventTags.VIBRATIONS)
                || event.getVanillaEvent().is(GameEventTags.IGNORE_VIBRATIONS_SNEAKING)) {
            if (entity.equals(event.getContext().sourceEntity())) {
                if (!PlatingUtil.canExtractEther(stack, Config.platingSilentStepEtherPerTick)) return;
                PlatingUtil.extractEtherWithEntityContext(entity, stack, Config.platingSilentStepEtherPerTick);
                event.setCanceled(true);
            }
        }
    }
}
