package studio.fantasyit.ether_craft.node.plugins.feature;

import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import studio.fantasyit.ether_craft.block.base.ItemFilter;
import studio.fantasyit.ether_craft.block.node.EtherAdaptNodeEntity;
import studio.fantasyit.ether_craft.menu.node.EtherAdaptNodeContainerMenu;
import studio.fantasyit.ether_craft.network.c2s.SyncScreenDataC2S;
import studio.fantasyit.ether_craft.node.NodeProperty;
import studio.fantasyit.ether_craft.node.filter.FilterGuiRegCommon;
import studio.fantasyit.ether_craft.node.plugins.InstalledPlugin;

public abstract class AbstractDirectionalFilterFeature extends AbstractDirectionalFeature {
    public ItemFilter filter = new ItemFilter(21, nodeEntity::setChanged);
    public AbstractDirectionalFilterFeature(EtherAdaptNodeEntity nodeEntity, InstalledPlugin ID) {
        super(nodeEntity, ID);
    }


    @Override
    public void modifyNodeProperty(NodeProperty nodeProperty) {
    }

    @Override
    public void registerSlots(EtherAdaptNodeContainerMenu menu) {
        super.registerSlots(menu);
        FilterGuiRegCommon.slots(menu, filter);
    }

    

    @Override
    public void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        filter.serialize(output);
    }

    @Override
    public void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        filter.deserialize(input);
    }
    @Override
    public void syncScreenData(SyncScreenDataC2S message) {
        super.syncScreenData(message);
        FilterGuiRegCommon.sync(message, filter, nodeEntity);
    }

}
