package studio.fantasyit.ether_craft.util;

import net.minecraft.core.NonNullList;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class ContainerOps {
    static public void tryPlaceToItemHandler(Container container, ResourceHandler<@NotNull ItemResource> target) {
        try (var transaction = Transaction.openRoot()) {
            for (int i = 0; i < container.getContainerSize(); i++) {
                var itemStack = container.getItem(i);
                if (!itemStack.isEmpty()) {
                    var itemResource = ItemResource.of(itemStack);
                    var amount = target.insert(itemResource, itemStack.getCount(), transaction);
                    if (amount > 0) {
                        container.setItem(i, itemStack.copyWithCount(itemStack.getCount() - amount));
                    }
                }
            }
            transaction.commit();
        }
    }

    static public NonNullList<ItemStack> containerToItemList(Container container) {
        NonNullList<ItemStack> itemStack = NonNullList.create();
        for (int i = 0; i < container.getContainerSize(); i++) {
            itemStack.add(container.getItem(i));
        }
        return itemStack;
    }

    static public void fillContainerByItemList(Container container, List<ItemStack> itemStack) {
        for (int i = 0; i < container.getContainerSize(); i++) {
            container.setItem(i, itemStack.get(i));
        }
    }
}
