package studio.fantasyit.ether_craft.plating.effects;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.plating.data.PlatingData;
import studio.fantasyit.ether_craft.plating.trigger.event.IPlatingKillTrigger;

public class HeadHuntPlatingEffect implements IPlatingEffect, IPlatingKillTrigger {
    public static final Identifier ID = EtherCraft.id("head_hunt");

    @Override
    public Identifier getId() {
        return ID;
    }

    @Override
    public void apply(IPlatingEffect effect, PlatingData data, ItemStack stack, LivingEntity entity, LivingDropsEvent event) {
//        if (!PlatingUtil.canExtractEther(stack, Config.platingHeadHuntEtherPerKill)) return;
//
//        double chance = data.effect();
//        if (entity.getRandom().nextDouble() >= chance) return;
//
//        ItemStack head = HEAD_MAP.get(target.getType());
//        if (head == null || head.isEmpty()) return;
//
//        Level level = target.level();
//        ItemEntity headEntity = new ItemEntity(level, target.getX(), target.getY(), target.getZ(), head.copy());
//        event.getDrops().add(headEntity);
//        PlatingUtil.extractEther(stack, Config.platingHeadHuntEtherPerKill);
    }
}
