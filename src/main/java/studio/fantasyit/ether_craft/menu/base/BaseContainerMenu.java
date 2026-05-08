package studio.fantasyit.ether_craft.menu.base;

import com.mojang.datafixers.util.Function4;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import studio.fantasyit.ether_craft.block.base.BaseEtherContainerBlockEntity;

public abstract class BaseContainerMenu extends BaseMenu<@NotNull BaseEtherContainerBlockEntity> {
    public final BlockPos pos;
    protected final int inputSlots;
    protected final int outputSlots;
    protected final int internalSlots;

    public BaseContainerMenu(int windowId, Player player, BlockPos pos, MenuType<?> container) {
        super(windowId, player, pos, container);
        this.pos = pos;
        this.inputSlots = entity.inputContainer.getContainerSize();
        this.internalSlots = entity.internalContainer.getContainerSize();
        this.outputSlots = entity.outputContainer.getContainerSize();
        addMachineSlots();
        addPlayerSlots(player.getInventory());
        entity.syncClient();
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
            } else if (!this.moveItemStackTo(stack, 0, inputSlots, false)) {
                return ItemStack.EMPTY;
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

    public interface SlotSupplier {
        Slot get(Container container, int index, int x, int y, int i, int j);

        static SlotSupplier of(Function4<Container, Integer, Integer, Integer, Slot> slotSupplier) {
            return (container, index, x, y, i, j) -> slotSupplier.apply(container, index, x, y);
        }
    }
}
