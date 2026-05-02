package studio.fantasyit.ether_craft.util;

import net.minecraft.core.NonNullList;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;

import java.util.stream.Collectors;

public class ContainerOps {
    static public NonNullList<ItemStack> tryPlaceItemToContainer(NonNullList<ItemStack> itemStack,Container container) {
        return itemStack.stream()
                .filter(itemStack1 -> !itemStack1.isEmpty())
                .map(itemStack1 -> {
                    for (int i = 0; i < container.getContainerSize(); i++) {
                        if (container.canPlaceItem(i, itemStack1)) {
                            ItemStack originalItemStack = container.getItem(i);
                            if (originalItemStack.isEmpty()) {
                                container.setItem(i, itemStack1);
                                return ItemStack.EMPTY;
                            } else if (originalItemStack.is(itemStack1.getItem())) {
                                int count = itemStack1.getCount();
                                if (count + originalItemStack.getCount() > originalItemStack.getMaxStackSize()) {
                                    count = originalItemStack.getMaxStackSize() - originalItemStack.getCount();
                                }
                                originalItemStack.grow(count);
                                itemStack1.shrink(count);
                                return itemStack1;
                            }
                        }
                    }
                    return itemStack1;
                })
                .filter(itemStack1 -> !itemStack1.isEmpty())
                .collect(Collectors.toCollection(NonNullList::create));
    }
    static public ItemStack getFromResourceHandler(ResourceHandler<@org.jetbrains.annotations.NotNull ItemResource> resourceHandler, int index){
        int amount = resourceHandler.getAmountAsInt(index);
        return resourceHandler.getResource(index).toStack( amount);
    }
}
