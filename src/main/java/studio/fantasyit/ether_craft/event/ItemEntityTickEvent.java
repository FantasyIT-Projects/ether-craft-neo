package studio.fantasyit.ether_craft.event;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import studio.fantasyit.ether_craft.Config;
import studio.fantasyit.ether_craft.attachment.EtherInactivateConvertData;
import studio.fantasyit.ether_craft.plating.PlatingData;
import studio.fantasyit.ether_craft.plating.PlatingManager;
import studio.fantasyit.ether_craft.plating.PlatingUtil;
import studio.fantasyit.ether_craft.plating.effects.IPlatingEffect;
import studio.fantasyit.ether_craft.register.DataComponentRegistry;
import studio.fantasyit.ether_craft.register.ItemRegistry;

import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber
public class ItemEntityTickEvent {
    @SubscribeEvent
    public static void onItemEntityTick(EntityTickEvent.Post event) {
        if (event.getEntity().level().isClientSide()) return;
        if (event.getEntity() instanceof ItemEntity ie) {
            ItemStack stack = ie.getItem();
            if (stack.is(ItemRegistry.ETHER.get()) && stack.count() == stack.getMaxStackSize()) {
                int i = ((ServerLevel) event.getEntity().level()).getDataStorage().computeIfAbsent(EtherInactivateConvertData.ID)
                        .entityTick(ie.getUUID(), event.getEntity().tickCount);
                if (i > Config.etherInactivateConvertTick) {
                    ie.setItem(ItemRegistry.INACTIVATED_ETHER.get().getDefaultInstance().copyWithCount(1));
                    ((ServerLevel) event.getEntity().level()).getDataStorage().computeIfAbsent(EtherInactivateConvertData.ID)
                            .reset(ie.getUUID());
                }
            }
            if (PlatingUtil.isPlatingInProgress(stack)) {
                tickPlating(stack, (ServerLevel) event.getEntity().level());
                ie.setItem(stack);
            }
        }
    }

    private static void tickPlating(ItemStack stack, ServerLevel level) {
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

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (event.getLevel().isClientSide()) return;

        ((ServerLevel) event.getLevel()).getDataStorage().computeIfAbsent(EtherInactivateConvertData.ID).tick();
    }
}
