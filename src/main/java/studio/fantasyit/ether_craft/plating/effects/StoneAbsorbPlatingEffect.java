package studio.fantasyit.ether_craft.plating.effects;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import studio.fantasyit.ether_craft.Config;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.plating.data.PlatingData;
import studio.fantasyit.ether_craft.plating.helper.PlatingUtil;
import studio.fantasyit.ether_craft.plating.trigger.event.IPlatingBlockDropsTrigger;
import studio.fantasyit.ether_craft.register.Tags;

public class StoneAbsorbPlatingEffect implements IPlatingEffect, IPlatingBlockDropsTrigger {
    public static final Identifier ID = EtherCraft.id("stone_absorb");

    @Override
    public Identifier getId() {
        return ID;
    }

    @Override
    public void apply(IPlatingEffect effect, PlatingData data, ItemStack stack, LivingEntity entity, BlockDropsEvent event) {
        if (!event.getState().is(Tags.STONE_ABSORBABLE)) return;
        if (event.getDrops().isEmpty()) return;

        event.getDrops().clear();
        event.setDroppedExperience(0);
        PlatingUtil.addEther(stack, Config.platingStoneAbsorbEtherPerBlock);
    }
}
