package studio.fantasyit.ether_craft.integration.jade;

import net.minecraft.world.entity.item.ItemEntity;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;
import studio.fantasyit.ether_craft.block.factory.EtherProcessFactoryBlock;
import studio.fantasyit.ether_craft.block.node.EtherAdaptNodeBlock;

@WailaPlugin
public class JadePlugin implements IWailaPlugin {
    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(EtherAdaptNodeServerDataProvider.INSTANCE, EtherAdaptNodeBlock.class);
        registration.registerBlockDataProvider(EtherProcessFactoryServerDataProvider.INSTANCE, EtherProcessFactoryBlock.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(EtherAdaptNodeProvider.INSTANCE, EtherAdaptNodeBlock.class);
        registration.registerBlockComponent(EtherProcessFactoryProvider.INSTANCE, EtherProcessFactoryBlock.class);
        registration.registerEntityComponent(PlatingItemEntityProvider.INSTANCE, ItemEntity.class);
    }
}
