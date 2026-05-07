package studio.fantasyit.ether_craft.block.base;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.apache.commons.lang3.function.TriConsumer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.fantasyit.ether_craft.menu.base.BaseContainerMenu;

public abstract class BaseMenu<T extends BlockEntity> extends AbstractContainerMenu {
    public final T entity;

    public BaseMenu(int windowId, Player player, BlockPos pos, MenuType<?> container) {
        super(container, windowId);
        entity = (T) player.level().getBlockEntity(pos);
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
        addSlotArea(playerInventory, 9, 10, 70, 9, 18, 3, 18, BaseContainerMenu.SlotSupplier.of(Slot::new));
        addSlotArea(playerInventory, 0, 10, 70 + 58, 9, 18, 1, 18, BaseContainerMenu.SlotSupplier.of(Slot::new));
    }

    protected int addSlotArea(Container container, int startIdx, int x, int y, int slotPreRow, int dx, int slotPreCol, int dy, BaseContainerMenu.SlotSupplier slotSupplier) {
        return addSlotArea(container, startIdx, x, y, slotPreRow, dx, slotPreCol, dy, slotSupplier, null);
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
    protected int addSlotArea(Container container, int startIdx, int x, int y, int slotPreRow, int dx, int slotPreCol, int dy, BaseContainerMenu.SlotSupplier slotSupplier, @Nullable TriConsumer<Slot, Integer, Integer> slotConsumer) {
        int added = 0;
        int index = startIdx;
        int totalSlots = container.getContainerSize();


        for (int j = 0; j < slotPreCol && index < totalSlots; j++) {
            for (int i = 0; i < slotPreRow && index < totalSlots; i++) {
                Slot slot = addSlot(slotSupplier.get(container, index, x, y, i, j));
                if (slotConsumer != null) {
                    slotConsumer.accept(slot, i, j);
                }

                x += dx;
                index++;
                added++;
            }
            x = x - slotPreRow * dx;
            y += dy;
        }
        return added;
    }
}
