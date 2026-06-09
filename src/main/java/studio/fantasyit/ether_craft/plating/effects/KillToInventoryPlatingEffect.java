package studio.fantasyit.ether_craft.plating.effects;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import studio.fantasyit.ether_craft.Config;
import studio.fantasyit.ether_craft.plating.PlatingData;
import studio.fantasyit.ether_craft.plating.helper.PlatingUtil;
import studio.fantasyit.ether_craft.plating.trigger.IPlatingKillTrigger;

import java.util.ArrayList;
import java.util.List;

public class KillToInventoryPlatingEffect implements IPlatingKillTrigger {

    @Override
    public double getEffectByEther(long ether) {
        return Math.sqrt(ether) / 10.0;
    }

    @Override
    public void onKill(PlatingData data, ItemStack stack, Player player, LivingEntity target, LivingDropsEvent event) {
        if (event.getDrops().isEmpty()) return;
        if (!PlatingUtil.canExtractEther(stack, Config.platingKillToInvEtherPerKill)) return;

        List<ItemEntity> absorbed = new ArrayList<>();
        for (ItemEntity entity : event.getDrops()) {
            if (player.getInventory().add(entity.getItem())) {
                absorbed.add(entity);
            }
        }
        event.getDrops().removeAll(absorbed);
        if (!absorbed.isEmpty()) {
            PlatingUtil.extractEther(stack, Config.platingKillToInvEtherPerKill);
        }
    }
}
