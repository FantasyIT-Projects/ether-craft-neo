package studio.fantasyit.ether_craft.plating.event;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import studio.fantasyit.ether_craft.Config;
import studio.fantasyit.ether_craft.plating.PlatingData;
import studio.fantasyit.ether_craft.plating.PlatingManager;
import studio.fantasyit.ether_craft.plating.helper.PlatingUtil;
import studio.fantasyit.ether_craft.plating.effects.IPlatingEffect;
import studio.fantasyit.ether_craft.register.DataComponentRegistry;

import java.util.ArrayList;
import java.util.List;

public class PlatingItemEntityTicker {
    public static void tick(ItemStack stack, ServerLevel level) {
        long startTime = stack.getOrDefault(DataComponentRegistry.PLATING_START_TIME, 0L);
        long elapsed = level.getGameTime() - startTime;
        if (elapsed < Config.platingDurationTicks) return;

        List<PlatingData> existing = new ArrayList<>(PlatingUtil.getPlatingData(stack));
        int ether = PlatingUtil.getEther(stack);
        List<Identifier> inProgress = PlatingUtil.getInProgress(stack);

        for (var effectId : inProgress) {
            IPlatingEffect effect = PlatingManager.getEffect(effectId);
            if (effect != null) {
                double value = effect.getEffectByEther(ether);
                existing.add(new PlatingData(effectId, value));
            }
        }

        stack.remove(DataComponentRegistry.PLATING_IN_PROGRESS);
        stack.remove(DataComponentRegistry.PLATING_START_TIME);
        stack.set(DataComponentRegistry.PLATING_DATA, existing);
    }
}
