package studio.fantasyit.ether_craft.node.plugins;

import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.block.node.EtherAdaptNodeEntity;
import studio.fantasyit.ether_craft.block.node.OversizedEtherSlot;
import studio.fantasyit.ether_craft.menu.base.BaseContainerMenu;
import studio.fantasyit.ether_craft.menu.factory.SingleStackSlot;
import studio.fantasyit.ether_craft.menu.node.EtherAdaptNodeContainerMenu;
import studio.fantasyit.ether_craft.node.AbstractNodePlugin;
import studio.fantasyit.ether_craft.node.NodeProperty;

public class MainPageDummyPlugin extends AbstractNodePlugin {
    public static final Identifier ID = EtherCraft.id("main_page_dummy");

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

    public static int[][] SLOT_POS = {
            {92, 49},
            {110, 49},
            {110, 31},
            {128, 31},
            {128, 13},
            {146, 13}
    };

    @Override
    public void registerSlots(EtherAdaptNodeContainerMenu menu) {
        menu.addSlot(new OversizedEtherSlot(nodeEntity.etherStorage, 0, 28, 19));
        menu.addSlot(new SingleStackSlot(nodeEntity.functionStorage, 0, 28, 45));

        int slots = nodeEntity.getUpgradeCount();
        for (int i = 0; i < slots; i++) {
            menu.addSlotDraw(new SingleStackSlot(nodeEntity.featureUpgradeStorage, i, SLOT_POS[i][0], SLOT_POS[i][1]));
        }

        menu.addSlotAreaDraw(nodeEntity.normalStorage, 0, 9, 76, 9, 18, 3, 18, BaseContainerMenu.SlotSupplier.of(Slot::new));
    }
}
