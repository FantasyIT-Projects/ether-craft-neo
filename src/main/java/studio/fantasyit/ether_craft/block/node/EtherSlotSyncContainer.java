package studio.fantasyit.ether_craft.block.node;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import studio.fantasyit.ether_craft.Config;
import studio.fantasyit.ether_craft.block.base.EtherContainer;
import studio.fantasyit.ether_craft.block.base.EtherJournal;
import studio.fantasyit.ether_craft.register.ItemRegistry;

public class EtherSlotSyncContainer implements Container, ResourceHandler<ItemResource> {
    private final EtherJournal journal;
    private EtherContainer etherContainer;

    public EtherSlotSyncContainer(EtherContainer etherContainer) {
        this.etherContainer = etherContainer;
        this.journal = new EtherJournal(etherContainer);
    }

    @Override
    public int getContainerSize() {
        return 1;
    }

    @Override
    public boolean isEmpty() {
        return etherContainer.getEther() < Config.etherConvert;
    }

    @Override
    public ItemStack getItem(int i) {
        if (etherContainer.getEther() < Config.etherConvert)
            return ItemStack.EMPTY;
        return ItemRegistry.ETHER.get().getDefaultInstance().copyWithCount(
                Math.min(
                        (int) (etherContainer.getEther() / Config.etherConvert),
                        ItemRegistry.ETHER.get().getDefaultInstance().getMaxStackSize()
                )
        );
    }

    @Override
    public ItemStack removeItem(int i, int i1) {
        if (etherContainer.getEther() < Config.etherConvert)
            return ItemStack.EMPTY;

        int toRemove = Math.min(i1, (int) (etherContainer.getEther() / Config.etherConvert));
        etherContainer.extractEther((long) toRemove * Config.etherConvert);
        return ItemRegistry.ETHER.get().getDefaultInstance().copyWithCount(toRemove);
    }

    @Override
    public ItemStack removeItemNoUpdate(int i) {
        if (etherContainer.getEther() < Config.etherConvert)
            return ItemStack.EMPTY;
        ItemStack item = getItem(i);
        int toRemove = Math.min(item.getCount(), (int) (etherContainer.getEther() / Config.etherConvert));
        etherContainer.extractEther((long) toRemove * Config.etherConvert);
        return ItemRegistry.ETHER.get().getDefaultInstance().copyWithCount(toRemove);
    }

    @Override
    public void setItem(int i, ItemStack itemStack) {
        if (itemStack.is(ItemRegistry.ETHER_CREATIVE)) {
            etherContainer.setEtherNoUpdate(Integer.MAX_VALUE);
        }
        ItemStack originalItem = getItem(i);
        int originalCount = originalItem.getCount();
        int diff = itemStack.getCount() - originalCount;
        if (diff > 0) {
            etherContainer.receiveEther((long) diff * Config.etherConvert);
        } else if (diff < 0) {
            etherContainer.extractEther((long) -diff * Config.etherConvert);
        }
    }

    @Override
    public void setChanged() {
        etherContainer.syncClient();
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public void clearContent() {
    }

    @Override
    public int size() {
        return 1;
    }

    @Override
    public ItemResource getResource(int index) {
        return ItemResource.of(ItemRegistry.ETHER.get());
    }

    @Override
    public long getAmountAsLong(int index) {
        return etherContainer.getEther() / Config.etherConvert;
    }

    @Override
    public long getCapacityAsLong(int index, ItemResource resource) {
        if (etherContainer.getMaxEther() == 0)
            return Integer.MAX_VALUE;
        return etherContainer.getMaxEther() / Config.etherConvert;
    }

    @Override
    public boolean isValid(int index, ItemResource resource) {
        return resource.is(ItemRegistry.ETHER);
    }

    @Override
    public int insert(int index, ItemResource resource, int amount, TransactionContext transaction) {
        if (!resource.is(ItemRegistry.ETHER))
            return 0;
        journal.updateSnapshots(transaction);
        long canReceive = etherContainer.getCanReceive((long) amount * Config.etherConvert);
        int canInsert = (int) (canReceive / Config.etherConvert);
        etherContainer.receiveEtherNoUpdate((long) canInsert * Config.etherConvert);
        return canInsert;
    }

    @Override
    public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
        if (!resource.is(ItemRegistry.ETHER))
            return 0;
        journal.updateSnapshots(transaction);
        long canExtract = Math.min(etherContainer.getEther() / Config.etherConvert, amount);
        etherContainer.extractEtherNoUpdate(canExtract * Config.etherConvert);
        return (int) canExtract;
    }

    @Override
    public int getMaxStackSize(ItemStack itemStack) {
        return this.getMaxStackSize();
    }

    @Override
    public int getMaxStackSize() {
        int extraToInsert = ItemRegistry.ETHER.get().getDefaultMaxStackSize();
        if (etherContainer.getMaxEther() == 0)
            return extraToInsert * 2;
        int maxAllowToInsert = (int) ((etherContainer.getMaxEther() - etherContainer.getEther()) / Config.etherConvert);
        int d = Math.min(extraToInsert, maxAllowToInsert);
        int currentSz = getItem(0).getCount();
        return currentSz + d;
    }
}
