package studio.fantasyit.ether_craft.menu.factory;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import org.joml.Vector2i;
import studio.fantasyit.ether_craft.block.base.EtherSlot;
import studio.fantasyit.ether_craft.block.factory.EtherProcessFactoryEntity;
import studio.fantasyit.ether_craft.menu.base.BaseContainerMenu;
import studio.fantasyit.ether_craft.menu.base.BaseDataSlot;

import java.util.HashMap;
import java.util.Map;

import static studio.fantasyit.ether_craft.register.GuiRegistry.ETHER_PROCESS_FACTORY_CONTAINER;

public class EtherProcessFactoryContainerMenu extends BaseContainerMenu {
    public Map<Integer, Vector2i> internalSlotMapping;

    public EtherProcessFactoryContainerMenu(int windowId, Player player, BlockPos pos) {
        super(windowId, player, pos, ETHER_PROCESS_FACTORY_CONTAINER.get());
    }

    @Override
    protected void addMachineSlots() {
        internalSlotMapping = new HashMap<>();
        addSlotArea(entity.container, 0, 17, 7, 1, 18, inputSlots, 18);
        addSlotArea(entity.container, inputSlots, 39, 7, inputSlots, 18, internalSlots / inputSlots, 18,
                (s, i, j) -> internalSlotMapping.put(s.index, new Vector2i(i, j))
        );
        addSlotArea(entity.container, inputSlots + internalSlots, 205, 7, 1, 18, 9, 18);
        addSlot(new EtherSlot(entity.etherContainer, 16, 173));
        EtherProcessFactoryEntity be = (EtherProcessFactoryEntity) entity;
        for (int i = 0; i < be.processingRecipes.length; i++) {
            int finalI = i;
            addDataSlot(new BaseDataSlot(() -> be.processingProgress[finalI], (v) -> be.processingProgress[finalI] = v));
        }
        for (int i = 0; i < EtherProcessFactoryEntity.ROWS; i++) {
            for (int j = 0; j < EtherProcessFactoryEntity.COLS; j++) {
                int finalI = i;
                int finalJ = j;
                addDataSlot(new BaseDataSlot(() -> be.pathBelongings[finalI][finalJ], (v) -> be.pathBelongings[finalI][finalJ] = v));
                addDataSlot(new BaseDataSlot(() -> be.currentEther[finalI][finalJ], (v) -> be.currentEther[finalI][finalJ] = v));
            }
        }
    }

    @Override
    protected void addPlayerSlots(Inventory playerInventory) {
        int invBaseX = 39;
        int invBaseY = 174;
        addSlotArea(playerInventory, 9, invBaseX, invBaseY, 9, 18, 3, 18);
        addSlotArea(playerInventory, 0, invBaseX, invBaseY + 58, 9, 18, 1, 18);
    }
}
