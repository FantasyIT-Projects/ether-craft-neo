package studio.fantasyit.ether_craft.plating.effects;

import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import studio.fantasyit.ether_craft.Config;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.plating.PlatingData;
import studio.fantasyit.ether_craft.plating.helper.PlatingUtil;
import studio.fantasyit.ether_craft.plating.trigger.IPlatingBlockDropsTrigger;

public class StoneAbsorbPlatingEffect implements IPlatingBlockDropsTrigger {

    private static final TagKey<Block> STONE_ABSORBABLE = TagKey.create(
            Registries.BLOCK, EtherCraft.id("stone_absorbable"));

    @Override
    public double getEffectByEther(long ether) {
        return 1.0;
    }

    @Override
    public void onBlockDrops(PlatingData data, ItemStack stack, Player player, BlockDropsEvent event) {
        if (!event.getState().is(STONE_ABSORBABLE)) return;
        if (event.getDrops().isEmpty()) return;

        event.getDrops().clear();
        event.setDroppedExperience(0);
        PlatingUtil.addEther(stack, Config.platingStoneAbsorbEtherPerBlock);
    }
}
