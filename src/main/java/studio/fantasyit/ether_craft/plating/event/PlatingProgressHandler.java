package studio.fantasyit.ether_craft.plating.event;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import studio.fantasyit.ether_craft.Config;
import studio.fantasyit.ether_craft.plating.PlatingManager;
import studio.fantasyit.ether_craft.plating.data.PlatingData;
import studio.fantasyit.ether_craft.plating.data.ProgressingPlatingData;
import studio.fantasyit.ether_craft.plating.effects.IPlatingEffect;
import studio.fantasyit.ether_craft.plating.helper.PlatingUtil;
import studio.fantasyit.ether_craft.plating.trigger.inst.IInstanceTrigger;
import studio.fantasyit.ether_craft.register.DataComponentRegistry;

import java.util.ArrayList;
import java.util.List;

public class PlatingProgressHandler {
    public static void tick(ItemStack stack, ServerLevel level) {
        long startTime = stack.getOrDefault(DataComponentRegistry.PLATING_START_TIME, 0L);
        long elapsed = level.getGameTime() - startTime;
        if (elapsed < Config.platingDurationTicks) return;

        List<PlatingData> existing = new ArrayList<>(PlatingUtil.getPlatingData(stack));
        int ether = PlatingUtil.getEther(stack);
        List<ProgressingPlatingData> inProgress = PlatingUtil.getInProgress(stack);

        for (var eff : inProgress) {
            IPlatingEffect effect = PlatingManager.getEffect(eff.id());
            if (effect != null) {
                double value = eff.formula().getEffect(ether);
                if (value > 0) {
                    PlatingData platingData = new PlatingData(eff.id(), value);
                    existing.add(platingData);
                    if (effect instanceof IInstanceTrigger iit)
                        iit.onPlatted(platingData, stack);
                }
            }
        }

        stack.remove(DataComponentRegistry.PLATING_IN_PROGRESS);
        stack.remove(DataComponentRegistry.PLATING_START_TIME);
        stack.set(DataComponentRegistry.PLATING_DATA, existing);
    }
}
