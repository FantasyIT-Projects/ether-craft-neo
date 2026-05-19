package studio.fantasyit.ether_craft.event;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import studio.fantasyit.ether_craft.Config;
import studio.fantasyit.ether_craft.attachment.EtherInactivateConvertData;
import studio.fantasyit.ether_craft.register.ItemRegistry;

@EventBusSubscriber
public class ItemEntityTickEvent {
    @SubscribeEvent
    public static void onItemEntityTick(EntityTickEvent.Post event) {
        if (event.getEntity().level().isClientSide()) return;
        if (event.getEntity() instanceof ItemEntity ie && ie.getItem().is(ItemRegistry.ETHER.get()) && ie.getItem().count() == ie.getItem().getMaxStackSize()) {
            int i = ((ServerLevel) event.getEntity().level()).getDataStorage().computeIfAbsent(EtherInactivateConvertData.ID)
                    .entityTick(ie.getUUID(), event.getEntity().tickCount);
            if (i > Config.etherInactivateTick) {
                ie.setItem(ItemRegistry.INACTIVATED_ETHER.get().getDefaultInstance().copyWithCount(1));
                ((ServerLevel) event.getEntity().level()).getDataStorage().computeIfAbsent(EtherInactivateConvertData.ID)
                        .reset(ie.getUUID());
            }
        }
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (event.getLevel().isClientSide()) return;

        ((ServerLevel) event.getLevel()).getDataStorage().computeIfAbsent(EtherInactivateConvertData.ID).tick();
    }
}
