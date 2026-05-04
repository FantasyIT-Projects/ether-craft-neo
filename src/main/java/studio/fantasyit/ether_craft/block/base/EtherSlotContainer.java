package studio.fantasyit.ether_craft.block.base;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class EtherSlotContainer implements Container {
    private EtherContainer etherContainer;
    public EtherSlotContainer(EtherContainer etherContainer){
        this.etherContainer = etherContainer;
    }
    @Override
    public int getContainerSize() {
        return 1;
    }

    @Override
    public boolean isEmpty() {
        return true;
    }

    @Override
    public ItemStack getItem(int i) {
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int i, int i1) {
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItemNoUpdate(int i) {
        return ItemStack.EMPTY;
    }

    @Override
    public void setItem(int i, ItemStack itemStack) {
        etherContainer.receiveEther(itemStack.getCount());
    }

    @Override
    public void setChanged() {
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public void clearContent() {
    }
}
