package studio.fantasyit.ether_craft.node.plugins.function;

import com.mojang.serialization.Codec;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import studio.fantasyit.ether_craft.block.node.EtherAdaptNodeEntity;
import studio.fantasyit.ether_craft.node.AbstractNodePlugin;
import studio.fantasyit.ether_craft.node.NodeProperty;
import studio.fantasyit.ether_craft.node.filter.ItemFilter;
import studio.fantasyit.ether_craft.util.ContainerOps;

public abstract class AbstractItemConsumeFunction extends AbstractNodePlugin {
    ItemFilter filter = new ItemFilter(18, nodeEntity::setChanged);
    SimpleContainer container = new SimpleContainer(1);
    int remainBurnTicks = 0;

    public AbstractItemConsumeFunction(EtherAdaptNodeEntity nodeEntity) {
        super(nodeEntity);
    }

    @Override
    public void modifyNodeProperty(NodeProperty nodeProperty) {
        if (nodeProperty.slotUnlock == 0)
            nodeProperty.slotUnlock = 1;
    }

    abstract boolean accepts(ItemResource stack);

    abstract ItemStack onConsumeItem(ItemStack itemStack);

    abstract void onBurnTick();

    @Override
    public void tick() {
        ItemStack oItemStack = container.getItem(0);
        int remain = oItemStack.isEmpty() ? 64 : oItemStack.getMaxStackSize() - oItemStack.getCount();
        try (var transaction = Transaction.openRoot()) {
            ItemStack toPlace = nodeEntity.extractWithPredicate(stack ->
                            accepts(stack) && container.canPlaceItem(0, stack.toStack()) && filter.accepts(stack),
                    transaction, remain
            );
            if (!toPlace.isEmpty() && oItemStack.getCount() + toPlace.getCount() <= toPlace.getMaxStackSize()) {
                ItemStack newStack = oItemStack.isEmpty() ? toPlace.copy() : oItemStack.copyWithCount(oItemStack.getCount() + toPlace.getCount());
                container.setItem(0, newStack);
                transaction.commit();
            }
        }

        if (remainBurnTicks == 0 && !container.getItem(0).isEmpty()) {
            ItemStack toConsumeItemStack = container.getItem(0);
            ItemStack newStack = onConsumeItem(toConsumeItemStack);
            container.setItem(0, newStack);
            onBurnTick();
        } else if (remainBurnTicks > 0) {
            onBurnTick();
            remainBurnTicks--;
        }

    }

    @Override
    public void saveAdditional(ValueOutput output) {
        output.store("container", ItemStack.OPTIONAL_CODEC.listOf(), ContainerOps.containerToItemList(container));
        output.store("remainBurnTicks", Codec.INT, remainBurnTicks);
        filter.serialize(output.child("filter"));
    }

    @Override
    public void loadAdditional(ValueInput input) {
        input.read("container", ItemStack.OPTIONAL_CODEC.listOf()).ifPresent(l -> ContainerOps.fillContainerByItemList(container, l));
        input.read("remainBurnTicks", Codec.INT).ifPresent(i -> remainBurnTicks = i);
        filter.deserialize(input.childOrEmpty("filter"));
    }

}
