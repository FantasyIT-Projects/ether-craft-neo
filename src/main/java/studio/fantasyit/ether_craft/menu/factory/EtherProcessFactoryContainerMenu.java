package studio.fantasyit.ether_craft.menu.factory;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2i;
import studio.fantasyit.ether_craft.block.base.ItemFilter;
import studio.fantasyit.ether_craft.block.factory.EtherProcessFactoryEntity;
import studio.fantasyit.ether_craft.factory.CraftGridResolver;
import studio.fantasyit.ether_craft.factory.FactoryLevelDef;
import studio.fantasyit.ether_craft.item.CraftTableChipData;
import studio.fantasyit.ether_craft.menu.base.BaseContainerMenu;
import studio.fantasyit.ether_craft.menu.base.IFilterSwitchable;
import studio.fantasyit.ether_craft.menu.base.ether.EtherContainerSyncer;
import studio.fantasyit.ether_craft.menu.base.ether.EtherSlot;
import studio.fantasyit.ether_craft.menu.base.slot.BaseDataSlot;
import studio.fantasyit.ether_craft.menu.base.slot.BaseSlot;
import studio.fantasyit.ether_craft.menu.base.slot.FilterSlot;
import studio.fantasyit.ether_craft.menu.base.slot.ResultSlot;
import studio.fantasyit.ether_craft.menu.factory.slot.FactoryInputSlot;
import studio.fantasyit.ether_craft.menu.factory.slot.InvisibleSlot;
import studio.fantasyit.ether_craft.menu.factory.slot.SingleStackSlot;
import studio.fantasyit.ether_craft.network.c2s.CraftChipGridC2S;
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
    private EtherContainerSyncer syncer;

    /** 工作台网格（3×3 标记格子）：只存 1 个样本、不吞物品、拿不走。 */
    public ItemFilter craftGrid;
    public List<FilterSlot> craftGridSlots;
    /** 正在编辑的芯片所在槽位索引；-1 表示浮层未打开。 */
    public int editingChipSlot = -1;

    public EtherProcessFactoryContainerMenu(int windowId, Player player, BlockPos pos) {
        super(windowId, player, pos, ETHER_PROCESS_FACTORY_CONTAINER.get());
    }

    @Override
    protected void addMachineSlots() {
        internalSlotMapping = new HashMap<>();
        mainUiSlots = new ArrayList<>();
        internalAndOutputSlots = new ArrayList<>();
        filterSlots = new ArrayList<>();
        // 注意：不能挂在 setChanged 上——原版槽位同步落盘时也会触发它，会造成"同步→上传"回环。
        craftGrid = new ItemFilter(9, () -> {
        });
        craftGridSlots = new ArrayList<>();
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
            etherSlotY += 7;
        }

        etherSlot = addSlot(new EtherSlot(entity.etherContainer, etherSlotX + 1, etherSlotY + 1));
        for (int i = 0; i < entity.processingRecipes.length; i++) {
            int finalI = i;
            addDataSlot(new BaseDataSlot(() -> entity.processingProgress[finalI], (v) -> entity.processingProgress[finalI] = v));
        }
        for (int i = 0; i < this.entity.ROWS; i++) {
            int finalI = i;
            addDataSlot(new BaseDataSlot(() -> entity.pathMaxDepth[finalI], (v) -> entity.pathMaxDepth[finalI] = v));
            addDataSlot(new BaseDataSlot(() -> entity.maxMultiplier[finalI], (v) -> entity.maxMultiplier[finalI] = v));
            for (int j = 0; j < this.entity.COLS; j++) {
                int finalJ = j;
                addDataSlot(new BaseDataSlot(() -> entity.pathBelongings[finalI][finalJ], (v) -> entity.pathBelongings[finalI][finalJ] = v));
                addDataSlot(new BaseDataSlot(() -> entity.pathDepth[finalI][finalJ], (v) -> entity.pathDepth[finalI][finalJ] = v));
                addDataSlot(new BaseDataSlot(() -> entity.pathDirection[finalI][finalJ], (v) -> entity.pathDirection[finalI][finalJ] = v));
                addDataSlot(new BaseDataSlot(() -> entity.currentEther[finalI][finalJ], (v) -> entity.currentEther[finalI][finalJ] = v));
                addDataSlot(new BaseDataSlot(() -> entity.chipColor[finalI][finalJ], (v) -> entity.chipColor[finalI][finalJ] = v));
            }
        }

        addDataSlot(new BaseDataSlot(() -> entity.leak, (v) -> entity.leak = v));
        addDataSlot(new BaseDataSlot(() -> entity.chipEtherMax, (v) -> entity.chipEtherMax = v));
        addDataSlot(new BaseDataSlot(() -> isFilterActive() ? 1 : 0, (v) -> setFilterActive(v == 1)));
        for (int i = 0; i < outputSlots; i++) {
            addSlot(new InvisibleSlot(entity.possibleResults, i, 0, 0));
        }

        for (int i = 0; i < this.entity.possibleIntermediateResults.getContainerSize(); i++) {
            addSlot(new InvisibleSlot(entity.possibleIntermediateResults, i, 0, 0));
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
        this.syncer = new EtherContainerSyncer(this.entity, this::addDataSlot);
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

        // 工作台网格的 9 个标记格子：追加在玩家背包之后，保证 machineSlotEnd / playerSlotStart 不变。
        // 位置由客户端在打开浮层时重建槽位对象决定（Slot.x/y 为 final）。
        addSlotArea(craftGrid, 0, 0, 0, 3, 18, 3, 18,
                SlotSupplier.of((a, b, c, d) -> new FilterSlot((ItemFilter) a, b, c, d)),
                (s, _, _) -> {
                    FilterSlot slot = (FilterSlot) s;
                    slot.setActive(false);
                    craftGridSlots.add(slot);
                });
    }

    @Override
    public @NotNull ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot.hasItem()) {
            ItemStack stack = slot.getItem();
            itemstack = stack.copy();

            if (mainUiSlots.contains(slot) || slot == etherSlot || filterSlots.contains(slot)
                    || (craftGridSlots != null && craftGridSlots.contains(slot))) {
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

    /**
     * 由 GUI 局部槽位坐标反算内部网格坐标。
     *
     * @return (x = 列, y = 行)；不在内部芯片格上时返回 null
     */
    public @Nullable Vector2i internalCellAt(int slotX, int slotY) {
        FactoryLevelDef factoryDef = entity.getLevelDef();
        int baseX = factoryDef.posInternal().x + 1;
        int baseY = factoryDef.posInternal().y + 1;
        int col = (slotX - baseX) / 18;
        int row = (slotY - baseY) / 18;
        if (col < 0 || row < 0 || col >= entity.COLS || row >= entity.ROWS) return null;
        if (baseX + col * 18 != slotX || baseY + row * 18 != slotY) return null;
        return new Vector2i(col, row);
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
        // 浮层关闭时，9 个工作台网格格子不参与交互
        if (editingChipSlot < 0 && craftGridSlots != null
                && slotIndex >= 0 && slotIndex < slots.size()
                && craftGridSlots.contains(getSlot(slotIndex))) {
            return;
        }
        // 工作台芯片的右键语义是"打开工作台网格"（由界面层处理）；浮层未开时这里也拒绝拿起，
        // 避免界面层没拦到（例如坐标/时序问题）时把芯片叼在手上。
        if (containerInput == ContainerInput.PICKUP && buttonNum == 1 && editingChipSlot < 0
                && slotIndex >= 0 && slotIndex < slots.size()
                && internalSlotMapping.containsKey(slotIndex)
                && getSlot(slotIndex).getItem().is(CRAFT_TABLE_CHIP_ITEM)) {
            return;
        }
        if (containerInput == ContainerInput.PICKUP_ALL && internalSlotMapping.containsKey(slotIndex)) {
            return;
        }
        if (!getCarried().is(WRENCH) && internalSlotMapping.containsKey(slotIndex)
                && containerInput == ContainerInput.PICKUP && !getCarried().isEmpty()) {
            Slot slot = getSlot(slotIndex);
            if (slot.hasItem()) {
                ItemStack slotItem = slot.getItem().copy();
                boolean moved = moveItemStackTo(slotItem, playerSlotStart, playerSlotStart + 36, false);
                if (moved) {
                    slot.set(slotItem.isEmpty() ? ItemStack.EMPTY : slotItem);
                }
                if (slot.getItem().isEmpty()) {
                    int amount = buttonNum == 0 ? getCarried().getCount() : 1;
                    setCarried(slot.safeInsert(getCarried(), amount));
                    return;
                }
            }
        }
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

    public void clearCraftGrid() {
        if (craftGrid == null) return;
        for (int i = 0; i < 9; i++) craftGrid.setItemNoTrigger(i, ItemStack.EMPTY);
    }

    /**
     * 服务端：进入编辑态，并把芯片里已存的配方解成 3×3 图案写进 {@code craftGrid}。
     *
     * <p>{@code craftGrid} 是普通槽位容器，写完由原版槽位同步推给客户端，
     * 客户端不做任何配方解析（客户端拿不到 {@code RecipeManager}）。
     */
    public void beginCraftGridEditing(int chipSlotIndex) {
        if (chipSlotIndex < 0 || chipSlotIndex >= slots.size()) return;
        ItemStack chip = getSlot(chipSlotIndex).getItem();
        if (!chip.is(CRAFT_TABLE_CHIP_ITEM)) return;
        editingChipSlot = chipSlotIndex;
        clearCraftGrid();
        CraftTableChipData data = chip.get(DataComponentRegistry.CRAFT_TABLE_CHIP_DATA);
        if (data != null) CraftGridResolver.fill(data, craftGrid);
        broadcastChanges();
    }

    /** 服务端：退出编辑态并清空网格。 */
    public void endCraftGridEditing() {
        editingChipSlot = -1;
        clearCraftGrid();
        broadcastChanges();
    }

    /** 客户端：把当前 9 个标记上传给服务端（用户改动 / JEI 转移后调用）。 */
    public void uploadCraftGrid() {
        if (editingChipSlot < 0 || craftGrid == null || playerInventory == null) return;
        if (!playerInventory.player.level().isClientSide()) return;
        List<ItemStack> grid = new ArrayList<>(9);
        for (int i = 0; i < 9; i++) grid.add(craftGrid.getItem(i));
        ClientPacketDistributor.sendToServer(new CraftChipGridC2S(editingChipSlot, grid));
    }

    /** 客户端：把一条合成配方按图案写进工作台网格（JEI「+」转移走这条路），并同步给服务端。 */
    public void applyRecipeToCraftGrid(CraftingRecipe recipe) {
        if (craftGrid == null) return;
        CraftGridResolver.markFromRecipe(playerInventory.player.level(), recipe, craftGrid);
        uploadCraftGrid();
    }

    /** 服务端：把 3×3 标记解析成原版合成配方，写回芯片数据组件。 */
    public void applyCraftGridToChip(int chipSlotIndex, List<ItemStack> grid) {
        if (chipSlotIndex < 0 || chipSlotIndex >= slots.size()) return;
        Slot slot = getSlot(chipSlotIndex);
        ItemStack chip = slot.getItem();
        if (!chip.is(CRAFT_TABLE_CHIP_ITEM)) return;
        CraftTableChipData data = CraftGridResolver.resolve(playerInventory.player.level(), grid);
        if (data == null) chip.remove(DataComponentRegistry.CRAFT_TABLE_CHIP_DATA);
        else chip.set(DataComponentRegistry.CRAFT_TABLE_CHIP_DATA, data);
        slot.set(chip);
        slot.setChanged();
    }
}
