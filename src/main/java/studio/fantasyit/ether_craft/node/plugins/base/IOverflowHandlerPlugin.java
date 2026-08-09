package studio.fantasyit.ether_craft.node.plugins.base;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jetbrains.annotations.Nullable;

public interface IOverflowHandlerPlugin {
    int handleOverflow(ItemStack stack, int amount, @Nullable TransactionContext transaction);
}
