package studio.fantasyit.ether_craft.menu.factory;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import studio.fantasyit.ether_craft.block.factory.EtherProcessFactoryEntity;
import studio.fantasyit.ether_craft.menu.base.BaseContainerMenu;
import studio.fantasyit.ether_craft.menu.base.BaseDataSlot;

import static studio.fantasyit.ether_craft.register.GuiRegistry.ETHER_PROCESS_FACTORY_CONTAINER;

public class EtherProcessFactoryContainerMenu extends BaseContainerMenu {
    public EtherProcessFactoryContainerMenu(int windowId, Player player, BlockPos pos) {
        super(windowId, player, pos,  ETHER_PROCESS_FACTORY_CONTAINER.get());
    }

    @Override
    protected void addMachineSlots() {
//        addSlotArea(entity,0,17,185,1,18,1,18);
        addSlotArea(entity.container, 0, 17,7, 1, 18, inputSlots, 18);
        addSlotArea(entity.container, inputSlots, 39,7, inputSlots, 18, internalSlots/inputSlots, 18);
        addSlotArea(entity.container, inputSlots+internalSlots, 205,7, 1, 18, 9, 18);
        EtherProcessFactoryEntity be = (EtherProcessFactoryEntity) entity;
        for(int i=0;i<be.processingRecipes.length;i++) {
            int finalI = i;
            addDataSlot(new BaseDataSlot(()->be.processingProgress[finalI],(v)->be.processingProgress[finalI]=v));
        }
    }
    @Override
    protected void addPlayerSlots(Inventory playerInventory) {
        int invBaseX = 39;
        int invBaseY = 174;
        addSlotArea(playerInventory, 9,invBaseX,invBaseY , 9, 18, 3, 18);
        addSlotArea(playerInventory, 0,invBaseX, invBaseY+58, 9, 18, 1, 18);
    }
}
