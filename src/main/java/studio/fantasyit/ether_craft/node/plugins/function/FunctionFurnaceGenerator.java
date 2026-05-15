package studio.fantasyit.ether_craft.node.plugins.function;

import net.minecraft.resources.Identifier;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.block.node.EtherAdaptNodeEntity;
import studio.fantasyit.ether_craft.node.plugins.InstalledPlugin;

public class FunctionFurnaceGenerator extends AbstractItemConsumeFunction {
    public static Identifier ID = EtherCraft.id("generator/furnace");

    public FunctionFurnaceGenerator(EtherAdaptNodeEntity nodeEntity, InstalledPlugin ID) {
        super(nodeEntity, ID);
    }

    @Override
    boolean accepts(ItemResource stack) {
        int burnTime = stack.toStack().getBurnTime(null, nodeEntity.getLevel().fuelValues());
        return burnTime > 0;
    }

    @Override
    ItemStack onConsumeItem(ItemStack itemStack) {
        if (itemStack.getCraftingRemainder() != null) {
            try (Transaction t = Transaction.openRoot()) {
                if (nodeEntity.insert(ItemResource.of(itemStack.getCraftingRemainder()), 1, t) == 0)
                    return itemStack;
                t.commit();
            }
        }
        ItemStack remainStack = itemStack.copyWithCount(itemStack.getCount() - 1);
        this.remainBurnTicks = itemStack.getBurnTime(null, nodeEntity.getLevel().fuelValues());

        if (itemStack.is(ItemTags.LOGS) || itemStack.is(ItemTags.PLANKS))
            nodeEntity.setSyncedPluginData(installedId, WORKING_MATERIAL, WorkingMaterial.WOOD.ordinal());
        else if (itemStack.is(Items.LAVA_BUCKET))
            nodeEntity.setSyncedPluginData(installedId, WORKING_MATERIAL, WorkingMaterial.LAVA.ordinal());
        else if (itemStack.is(ItemTags.COALS))
            nodeEntity.setSyncedPluginData(installedId, WORKING_MATERIAL, WorkingMaterial.COAL.ordinal());
        else
            nodeEntity.setSyncedPluginData(installedId, WORKING_MATERIAL, WorkingMaterial.ANY.ordinal());

        return remainStack;
    }

    @Override
    void onBurnTick() {
        nodeEntity.receiveEther(100);
    }

    @Override
    public void tick() {
        super.tick();
        if(remainBurnTicks == 0)
            nodeEntity.setSyncedPluginData(installedId, WORKING_MATERIAL, WorkingMaterial.IDLE.ordinal());
    }
}
