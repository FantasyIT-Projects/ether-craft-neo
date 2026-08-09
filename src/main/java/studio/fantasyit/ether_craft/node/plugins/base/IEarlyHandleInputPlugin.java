package studio.fantasyit.ether_craft.node.plugins.base;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jetbrains.annotations.Nullable;

public interface IEarlyHandleInputPlugin {
    int earlyHandleInput(ItemStack stack, int amount, @Nullable TransactionContext context);
}
