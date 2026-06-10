package studio.fantasyit.ether_craft.event;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import studio.fantasyit.ether_craft.Config;
import studio.fantasyit.ether_craft.attachment.EtherInactivateConvertData;
import studio.fantasyit.ether_craft.plating.helper.PlatingUtil;
import studio.fantasyit.ether_craft.plating.event.PlatingProgressHandler;
import studio.fantasyit.ether_craft.register.ItemRegistry;

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
                PlatingProgressHandler.tick(stack, (ServerLevel) event.getEntity().level());
                ie.setItem(stack);
            }
        }
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (event.getLevel().isClientSide()) return;

        ((ServerLevel) event.getLevel()).getDataStorage().computeIfAbsent(EtherInactivateConvertData.ID).tick();
    }
}
