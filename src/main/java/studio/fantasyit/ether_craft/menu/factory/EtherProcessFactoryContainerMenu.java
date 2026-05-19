package studio.fantasyit.ether_craft.menu.factory;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector2i;
import studio.fantasyit.ether_craft.block.base.ItemFilter;
import studio.fantasyit.ether_craft.menu.base.IFilterSwitchable;
import studio.fantasyit.ether_craft.block.factory.EtherProcessFactoryEntity;
import studio.fantasyit.ether_craft.factory.FactoryLevelDef;
import studio.fantasyit.ether_craft.menu.base.*;
import studio.fantasyit.ether_craft.menu.base.ether.EtherSlot;
import studio.fantasyit.ether_craft.menu.base.slot.BaseDataSlot;
import studio.fantasyit.ether_craft.menu.base.slot.BaseSlot;
import studio.fantasyit.ether_craft.menu.base.slot.FilterSlot;
import studio.fantasyit.ether_craft.menu.base.slot.ResultSlot;
import studio.fantasyit.ether_craft.menu.factory.slot.FactoryInputSlot;
import studio.fantasyit.ether_craft.menu.factory.slot.InvisibleSlot;
import studio.fantasyit.ether_craft.menu.factory.slot.SingleStackSlot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static studio.fantasyit.ether_craft.register.GuiRegistry.ETHER_PROCESS_FACTORY_CONTAINER;
import static studio.fantasyit.ether_craft.register.ItemRegistry.ETHER;
import static studio.fantasyit.ether_craft.register.ItemRegistry.ETHER_CREATIVE;

public class EtherProcessFactoryContainerMenu extends BaseContainerMenu<@NotNull EtherProcessFactoryEntity> implements IFilterSwitchable {
    public Map<Integer, Vector2i> internalSlotMapping;
    public List<Slot> mainUiSlots;
    public List<BaseSlot> internalAndOutputSlots;
    public List<FilterSlot> filterSlots;
    public Slot etherSlot;
    public int machineSlotEnd;
    private boolean filterActive = false;

    public EtherProcessFactoryContainerMenu(int windowId, Player player, BlockPos pos) {
        super(windowId, player, pos, ETHER_PROCESS_FACTORY_CONTAINER.get());
    }

    @Override
    protected void addMachineSlots() {
        internalSlotMapping = new HashMap<>();
        mainUiSlots = new ArrayList<>();
        internalAndOutputSlots = new ArrayList<>();
        filterSlots = new ArrayList<>();
        FactoryLevelDef factoryDef = entity.getLevelDef();

        addSlotArea(entity.container, 0, factoryDef.posInput().x + 1, factoryDef.posInput().y + 1, 1, 18, entity.ROWS, 18,
                (a, b, c, d, e, f) -> new FactoryInputSlot(a, b, c, d, entity.internalContainer, f * internalSlots / inputSlots),
                (s, i, j) -> mainUiSlots.add(s)
        );
        addSlotArea(entity.container, inputSlots, factoryDef.posInternal().x + 1, factoryDef.posInternal().y + 1, entity.COLS, 18, entity.ROWS, 18,
                SlotSupplier.of(SingleStackSlot::new), (s, i, j) -> {
                    internalSlotMapping.put(s.index, new Vector2i(i, j));
                    mainUiSlots.add(s);
                }
        );
        addSlotArea(entity.container, inputSlots + internalSlots, factoryDef.posOutput().x + 1, factoryDef.posOutput().y + 1, 1, 18, entity.ROWS, 18,
                SlotSupplier.of(ResultSlot::new),
                (s, i, j) -> mainUiSlots.add(s)
        );

        int etherSlotX = factoryDef.panelLeft().x;
        int etherSlotY = factoryDef.panelLeft().y;
        if (factoryDef.showPanel()) {
            etherSlotX += 4;
            etherSlotY += 4;
        }

        etherSlot = addSlot(new EtherSlot(entity.etherContainer, etherSlotX + 1, etherSlotY + 1));
        for (int i = 0; i < entity.processingRecipes.length; i++) {
            int finalI = i;
            addDataSlot(new BaseDataSlot(() -> entity.processingProgress[finalI], (v) -> entity.processingProgress[finalI] = v));
        }
        for (int i = 0; i < this.entity.ROWS; i++) {
            for (int j = 0; j < this.entity.COLS; j++) {
                int finalI = i;
                int finalJ = j;
                addDataSlot(new BaseDataSlot(() -> entity.pathBelongings[finalI][finalJ], (v) -> entity.pathBelongings[finalI][finalJ] = v));
                addDataSlot(new BaseDataSlot(() -> entity.currentEther[finalI][finalJ], (v) -> entity.currentEther[finalI][finalJ] = v));
            }
        }

        addDataSlot(new BaseDataSlot(() -> entity.pressureBonus, (v) -> entity.pressureBonus = v));
        addDataSlot(new BaseDataSlot(() -> entity.leak, (v) -> entity.leak = v));
        addDataSlot(new BaseDataSlot(() -> isFilterActive() ? 1 : 0, (v) -> setFilterActive(v == 1)));
        for (int i = 0; i < outputSlots; i++) {
            addSlot(new InvisibleSlot(entity.possibleResults, i, 0, 0));
        }

        for (int i = this.entity.ROWS; i < mainUiSlots.size(); i++) {
            if (mainUiSlots.get(i) instanceof BaseSlot s)
                internalAndOutputSlots.add(s);
        }

        for (int i = 0; i < inputSlots; i++) {
            addSlotArea(entity.filters[i], 0, factoryDef.posFilterInput().x + 1, factoryDef.posFilterInput().y + i * 18 + 1, this.entity.ROWS, 18, 1, 18,
                    SlotSupplier.of((a, b, c, d) -> new FilterSlot((ItemFilter) a, b, c, d)),
                    (s, _, j) -> filterSlots.add((FilterSlot) s)
            );
        }
        machineSlotEnd = this.slots.size();
    }

    @Override
    protected void addPlayerSlots(Inventory playerInventory) {
        FactoryLevelDef factoryDef = entity.getLevelDef();
        int invBaseX = factoryDef.posPlayer().x + 8;
        int invBaseY = factoryDef.posPlayer().y + 8;
        addSlotArea(playerInventory, 9, invBaseX, invBaseY, 9, 18, 3, 18, SlotSupplier.of(Slot::new));
        addSlotArea(playerInventory, 0, invBaseX, invBaseY + 58, 9, 18, 1, 18, SlotSupplier.of(Slot::new));
    }

    @Override
    public @NotNull ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot.hasItem()) {
            ItemStack stack = slot.getItem();
            itemstack = stack.copy();

            if (mainUiSlots.contains(slot) || slot == etherSlot || filterSlots.contains(slot)) {
                if (!this.moveItemStackTo(stack, machineSlotEnd, machineSlotEnd + 36, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                if (stack.is(ETHER) || stack.is(ETHER_CREATIVE)) {
                    this.moveItemStackTo(stack, etherSlot.index, etherSlot.index + 1, false);
                }
                if (!stack.isEmpty()) {
                    this.moveItemStackTo(stack, 0, inputSlots, false);
                }
                if (isFilterActive()) {
                    for (FilterSlot fs : filterSlots) {
                        if (stack.isEmpty()) break;
                        if (!fs.hasItem()) {
                            fs.set(stack.copyWithCount(1));
                        }
                    }
                }
            }

            if (stack.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }

            if (stack.getCount() == itemstack.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTake(player, stack);
        }
        return itemstack;
    }

    @Override
    public boolean isFilterActive() {
        return filterActive;
    }

    @Override
    public void setFilterActive(boolean active) {
        this.filterActive = active;
    }
}
