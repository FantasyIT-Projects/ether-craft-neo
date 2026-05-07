package studio.fantasyit.ether_craft.node.plugins;

import net.minecraft.resources.Identifier;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.block.node.EtherAdaptNodeEntity;
import studio.fantasyit.ether_craft.menu.base.ether.EtherSlot;
import studio.fantasyit.ether_craft.menu.node.EtherAdaptNodeContainerMenu;
import studio.fantasyit.ether_craft.node.AbstractNodePlugin;
import studio.fantasyit.ether_craft.node.NodeProperty;

public class MainPageDummyPlugin extends AbstractNodePlugin {
    public static final Identifier ID = EtherCraft.id( "main_page_dummy");
    public MainPageDummyPlugin(EtherAdaptNodeEntity nodeEntity) {
        super(nodeEntity);
    }

    @Override
    public void modifyNodeProperty(NodeProperty nodeProperty) {

    }

    @Override
    public void tick() {

    }

    @Override
    public void saveAdditional(ValueOutput output) {

    }

    @Override
    public void loadAdditional(ValueInput input) {

    }

    @Override
    public void registerSlots(EtherAdaptNodeContainerMenu menu) {
        menu.addSlot(new EtherSlot(nodeEntity.etherStorage, 26, 23));
    }
}
