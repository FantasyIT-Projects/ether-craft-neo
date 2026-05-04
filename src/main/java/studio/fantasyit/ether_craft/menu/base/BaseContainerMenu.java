package studio.fantasyit.ether_craft.menu.base;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import studio.fantasyit.ether_craft.block.base.BaseEtherContainerBlockEntity;

public abstract class BaseContainerMenu extends AbstractContainerMenu {
    public final BlockPos pos;

    public final BaseEtherContainerBlockEntity entity;
    protected final int inputSlots;
    protected final int outputSlots;
    protected final int internalSlots;

    public BaseContainerMenu(int windowId, Player player, BlockPos pos, MenuType<?> container) {
        super(container, windowId);
        this.pos = pos;

        if (player.level().getBlockEntity(pos) instanceof BaseEtherContainerBlockEntity _entity) {
            this.inputSlots = _entity.inputContainer.getContainerSize();
            this.internalSlots = _entity.internalContainer.getContainerSize();
            this.outputSlots = _entity.outputContainer.getContainerSize();
            this.entity = _entity;
            addMachineSlots();
        } else {
            this.inputSlots = 0;
            this.internalSlots = 0;
            this.outputSlots = 0;
            this.entity = null;
        }
        addPlayerSlots(player.getInventory());
    }

    /**
     * 添加机器输入输出槽位到GUI
     */
    protected abstract void addMachineSlots();

    /**
     * 添加玩家背包槽位到GUI
     *
     * @param playerInventory 玩家背包
     */
    protected void addPlayerSlots(Inventory playerInventory) {
        addSlotArea(playerInventory, 9, 10, 70, 9, 18, 3, 18);
        addSlotArea(playerInventory, 0, 10, 70 + 58, 9, 18, 1, 18);
    }

    /**
     * 添加一个Slot组，左上角x，y，每行slotPreRow个，每列slotPreCol个，间隔dx，dy。（即总共添加最多两者乘积个）
     *
     * @param container  支持Container
     * @param startIdx   从第idx个开始
     * @param x          左上角X
     * @param y          左上角Y
     * @param slotPreRow 每行个数
     * @param dx         横向格子间隔
     * @param slotPreCol 每列个数
     * @param dy         纵向格子间隔
     * @return 成功添加的个数
     */
    protected int addSlotArea(Container container, int startIdx, int x, int y, int slotPreRow, int dx, int slotPreCol, int dy) {
        int added = 0;
        int index = startIdx;
        int totalSlots = container.getContainerSize();


        for (int j = 0; j < slotPreCol && index < totalSlots; j++) {
            for (int i = 0; i < slotPreRow && index < totalSlots; i++) {
                addSlot(new Slot(container, index, x, y));

                x += dx;
                index++;
                added++;
            }
            x = x - slotPreRow * dx;
            y += dy;
        }
        return added;
    }

    @Override
    public @NotNull ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        int slotCnt = inputSlots + outputSlots + internalSlots;
        if (slot.hasItem()) {
            ItemStack stack = slot.getItem();
            itemstack = stack.copy();
            if (index < slotCnt) {
                if (!this.moveItemStackTo(stack, slotCnt, Inventory.INVENTORY_SIZE + slotCnt, true)) {
                    return ItemStack.EMPTY;
                }
            }
            else if (!this.moveItemStackTo(stack, inputSlots, inputSlots + 1, false)) {
                if (index < 27 + slotCnt) {
                    if (!this.moveItemStackTo(stack, 27 + slotCnt, 36 + slotCnt, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (index < Inventory.INVENTORY_SIZE + slotCnt && !this.moveItemStackTo(stack, slotCnt, 27 + slotCnt, false)) {
                    return ItemStack.EMPTY;
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
    public boolean stillValid(Player p_38874_) {
        return true;
    }
}
