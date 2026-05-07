package studio.fantasyit.ether_craft.node.plugins.function;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.item.ItemResource;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.block.node.EtherAdaptNodeEntity;

public class FunctionFurnaceGenerator extends AbstractItemConsumeFunction {
    public static Identifier ID = EtherCraft.id("generator/furnace");
    public FunctionFurnaceGenerator(EtherAdaptNodeEntity nodeEntity) {
        super(nodeEntity);
    }

    @Override
    boolean accepts(ItemResource stack) {
        int burnTime = stack.toStack().getBurnTime(null, nodeEntity.getLevel().fuelValues());
        return burnTime > 0;
    }

    @Override
    ItemStack onConsumeItem(ItemStack itemStack) {
        ItemStack remainStack = itemStack.copyWithCount(itemStack.getCount() - 1);
        this.remainBurnTicks = itemStack.getBurnTime(null, nodeEntity.getLevel().fuelValues());
        return remainStack;
    }

    @Override
    void onBurnTick() {
        nodeEntity.receiveEther(100);
    }
}
