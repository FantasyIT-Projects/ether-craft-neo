package studio.fantasyit.ether_craft.plating.effects;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import studio.fantasyit.ether_craft.Config;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.plating.data.PlatingData;
import studio.fantasyit.ether_craft.plating.helper.PlatingUtil;
import studio.fantasyit.ether_craft.plating.trigger.event.IPlatingBlockDropsTrigger;

import java.util.ArrayList;
import java.util.List;

public class BreakToInventoryPlatingEffect implements IPlatingEffect, IPlatingBlockDropsTrigger {
    public static final Identifier ID = EtherCraft.id("break_to_inv");

    @Override
    public Identifier getId() {
        return ID;
    }

    @Override
    public void apply(IPlatingEffect effect, PlatingData data, ItemStack stack, LivingEntity entity, BlockDropsEvent event) {
        if (event.getDrops().isEmpty()) return;
        if (!(entity instanceof Player player)) return;
        if (!PlatingUtil.canExtractEther(stack, Config.platingBreakToInvEtherPerBlock)) return;

        List<ItemEntity> absorbed = new ArrayList<>();
        for (ItemEntity dropEntity : event.getDrops()) {
            if (player.getInventory().add(dropEntity.getItem())) {
                absorbed.add(dropEntity);
            }
        }
        event.getDrops().removeAll(absorbed);
        if (!absorbed.isEmpty()) {
            PlatingUtil.extractEtherWithEntityContext(entity, stack, Config.platingBreakToInvEtherPerBlock);
        }
    }
}
