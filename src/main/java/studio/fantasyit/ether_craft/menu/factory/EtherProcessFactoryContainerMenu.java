package studio.fantasyit.ether_craft.menu.factory;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2i;
import studio.fantasyit.ether_craft.block.base.ItemFilter;
import studio.fantasyit.ether_craft.block.factory.EtherProcessFactoryEntity;
import studio.fantasyit.ether_craft.factory.FactoryLevelDef;
import studio.fantasyit.ether_craft.menu.base.BaseContainerMenu;
import studio.fantasyit.ether_craft.menu.base.IFilterSwitchable;
import studio.fantasyit.ether_craft.menu.base.ether.EtherSlot;
import studio.fantasyit.ether_craft.menu.base.slot.BaseDataSlot;
import studio.fantasyit.ether_craft.menu.base.slot.BaseSlot;
import studio.fantasyit.ether_craft.menu.base.slot.FilterSlot;
import studio.fantasyit.ether_craft.menu.base.slot.ResultSlot;
import studio.fantasyit.ether_craft.menu.factory.slot.FactoryInputSlot;
import studio.fantasyit.ether_craft.menu.factory.slot.InvisibleSlot;
import studio.fantasyit.ether_craft.menu.factory.slot.SingleStackSlot;
import studio.fantasyit.ether_craft.register.DataComponentRegistry;

import java.util.*;

import static studio.fantasyit.ether_craft.register.GuiRegistry.ETHER_PROCESS_FACTORY_CONTAINER;
import static studio.fantasyit.ether_craft.register.ItemRegistry.*;

public class EtherProcessFactoryContainerMenu extends BaseContainerMenu<@NotNull EtherProcessFactoryEntity> implements IFilterSwitchable {
    public Map<Integer, Vector2i> internalSlotMapping;
    public List<Slot> mainUiSlots;
    public List<BaseSlot> internalAndOutputSlots;
    public List<FilterSlot> filterSlots;
    public Slot etherSlot;
    public int machineSlotEnd;
    private boolean filterActive = false;

    public int quickPlaceChipSlotId = -1;
    public Set<Identifier> selectedChips = new HashSet<>();
    public Inventory playerInventory;
    public int playerSlotStart;

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
            int finalI = i;
            addDataSlot(new BaseDataSlot(() -> entity.pathMaxDepth[finalI], (v) -> entity.pathMaxDepth[finalI] = v));
            for (int j = 0; j < this.entity.COLS; j++) {
                int finalJ = j;
                addDataSlot(new BaseDataSlot(() -> entity.pathBelongings[finalI][finalJ], (v) -> entity.pathBelongings[finalI][finalJ] = v));
                addDataSlot(new BaseDataSlot(() -> entity.pathDepth[finalI][finalJ], (v) -> entity.pathDepth[finalI][finalJ] = v));
                addDataSlot(new BaseDataSlot(() -> entity.pathDirection[finalI][finalJ], (v) -> entity.pathDirection[finalI][finalJ] = v));
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

        addDataSlot(new BaseDataSlot(() -> quickPlaceChipSlotId, (v) -> quickPlaceChipSlotId = v));
    }

    @Override
    protected void addPlayerSlots(Inventory playerInventory) {
        playerSlotStart = slots.size();
        this.playerInventory = playerInventory;
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
                        if (fs.handler.hasAnyMatching(s -> ItemStack.isSameItemSameComponents(s, stack)))
                            continue;
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

    /// /// 扳手快速放置

    public void onSwitchItem(boolean reverse) {
        if (quickPlaceChipSlotId == -1 && reverse) {
            selectedChips.clear();
            int lastMatch = -1;
            for (int i = 0; i < playerInventory.getContainerSize(); i++) {
                Identifier id = playerInventory.getItem(i).get(DataComponentRegistry.CHIP_ID);
                if (id != null) {
                    lastMatch = i;
                    selectedChips.add(id);
                }
            }
            quickPlaceChipSlotId = lastMatch;
        } else {
            int dir = reverse ? -1 : 1;
            if (quickPlaceChipSlotId == -1)
                quickPlaceChipSlotId = 0;
            boolean found = false;
            for (int i = quickPlaceChipSlotId + dir; i >= 0 && i < playerInventory.getContainerSize(); i += dir) {
                Identifier id = playerInventory.getItem(i).get(DataComponentRegistry.CHIP_ID);
                if (id != null && (reverse || !selectedChips.contains(id))) {
                    quickPlaceChipSlotId = i;
                    if (reverse)
                        selectedChips.remove(id);
                    else
                        selectedChips.add(id);
                    found = true;
                    break;
                }
            }
            if (!found) {
                quickPlaceChipSlotId = -1;
                selectedChips.clear();
            }
        }
    }

    @Override
    public void clicked(int slotIndex, int buttonNum, ContainerInput containerInput, Player player) {
        if (getCarried().is(WRENCH) && containerInput == ContainerInput.QUICK_CRAFT) {
            int header = AbstractContainerMenu.getQuickcraftHeader(buttonNum);
            if (header == AbstractContainerMenu.QUICKCRAFT_HEADER_START) {
                resetQuickCraft();
            } else if (header == AbstractContainerMenu.QUICKCRAFT_HEADER_CONTINUE
                    && quickPlaceChipSlotId != -1 && internalSlotMapping.containsKey(slotIndex)) {
                placeChipInSlot(slotIndex, false);
            } else if (header == AbstractContainerMenu.QUICKCRAFT_HEADER_END) {
                resetQuickCraft();
            }
            return;
        }

        if (getCarried().is(WRENCH) && internalSlotMapping.containsKey(slotIndex) && containerInput == ContainerInput.CLONE) {
            Slot clickedSlot = getSlot(slotIndex);
            if (clickedSlot.hasItem() && clickedSlot.getItem().get(DataComponentRegistry.CHIP_ID) != null) {
                Identifier targetId = clickedSlot.getItem().get(DataComponentRegistry.CHIP_ID);
                if (playerInventory.hasAnyMatching(s -> Objects.equals(s.get(DataComponentRegistry.CHIP_ID), targetId))) {
                    selectedChips.clear();
                    quickPlaceChipSlotId = -1;
                    for (int i = 0; i < playerInventory.getContainerSize(); i++) {
                        Identifier id = playerInventory.getItem(i).get(DataComponentRegistry.CHIP_ID);
                        if (id != null) {
                            selectedChips.add(id);
                            if (Objects.equals(id, targetId)) {
                                quickPlaceChipSlotId = i;
                                break;
                            }
                        }
                    }
                }
            }
        } else if (getCarried().is(WRENCH) && quickPlaceChipSlotId != -1 && internalSlotMapping.containsKey(slotIndex) && containerInput != ContainerInput.QUICK_MOVE) {
            placeChipInSlot(slotIndex, true);
        } else
            super.clicked(slotIndex, buttonNum, containerInput, player);
    }

    private void placeChipInSlot(int slotIndex, boolean allowAutoSwitch) {
        ItemStack toPlace = playerInventory.getItem(quickPlaceChipSlotId);
        Slot clickedSlot = getSlot(slotIndex);
        Identifier refId = toPlace.get(DataComponentRegistry.CHIP_ID);
        boolean available = true;
        if (!toPlace.isEmpty() && refId != null) {
            if (clickedSlot.hasItem()) {
                available = moveItemStackTo(clickedSlot.getItem(), playerSlotStart, playerSlotStart + 36, false);
            }
        }
        if (available) {
            moveItemStackTo(toPlace, clickedSlot.index, clickedSlot.index + 1, false);
            if (playerInventory.getItem(quickPlaceChipSlotId).isEmpty())
                advanceChip(refId, allowAutoSwitch);
        }
    }

    private void advanceChip(@Nullable Identifier refId, boolean allowAutoSwitch) {
        for (int i = quickPlaceChipSlotId + 1; i < playerInventory.getContainerSize(); i++) {
            Identifier id = playerInventory.getItem(i).get(DataComponentRegistry.CHIP_ID);
            if (Objects.equals(id, refId)) {
                quickPlaceChipSlotId = i;
                return;
            }
        }
        if (allowAutoSwitch) {
            if (refId != null)
                selectedChips.add(refId);
            onSwitchItem(false);
        } else {
            quickPlaceChipSlotId = -1;
            selectedChips.clear();
        }
    }
}
