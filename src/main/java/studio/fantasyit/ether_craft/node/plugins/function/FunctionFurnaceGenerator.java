package studio.fantasyit.ether_craft.node.plugins.function;

import net.minecraft.resources.Identifier;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import studio.fantasyit.ether_craft.Config;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.block.node.EtherAdaptNodeEntity;
import studio.fantasyit.ether_craft.node.plugins.InstalledPlugin;
import studio.fantasyit.ether_craft.node.plugins.upgrade.IGeneratorAdjuster;

public class FunctionFurnaceGenerator extends AbstractItemConsumeFunction {
    public static Identifier ID = EtherCraft.id("generator/furnace");
    public static Identifier ID_BLAST = EtherCraft.id("generator/blast");

    public FunctionFurnaceGenerator(EtherAdaptNodeEntity nodeEntity, InstalledPlugin ID) {
        super(nodeEntity, ID);
    }

    @Override
    boolean accepts(ItemResource stack) {
        int burnTime = stack.toStack().getBurnTime(null, nodeEntity.getLevel().fuelValues());
        return burnTime >= Config.nodeFurnaceBurnTimeFactor;
    }

    @Override
    IGeneratorAdjuster.AdjustedParameters onConsumeItem(ItemStack itemStack) {
        if (itemStack.getCraftingRemainder() != null) {
            try (Transaction t = Transaction.openRoot()) {
                if (nodeEntity.insert(ItemResource.of(itemStack.getCraftingRemainder()), 1, t) == 0)
                    return new IGeneratorAdjuster.AdjustedParameters(0, 0);
                t.commit();
            }
        }
        int factor = Config.nodeFurnaceBurnTimeFactor;
        if (this.installedId.pluginId().equals(ID_BLAST))
            factor = Config.nodeBlastFurnaceBurnTimeFactor;

        int pt;
        if (this.installedId.pluginId().equals(ID_BLAST))
            pt = Config.nodeBlastFurnaceEtherPerTick;
        else
            pt = Config.nodeFurnaceEtherPerTick;

        int bt = itemStack.getBurnTime(null, nodeEntity.getLevel().fuelValues()) / factor;

        if (itemStack.is(ItemTags.LOGS) || itemStack.is(ItemTags.PLANKS))
            nodeEntity.setSyncedPluginData(installedId, WORKING_MATERIAL, WorkingMaterial.WOOD.ordinal());
        else if (itemStack.is(Items.LAVA_BUCKET))
            nodeEntity.setSyncedPluginData(installedId, WORKING_MATERIAL, WorkingMaterial.LAVA.ordinal());
        else if (itemStack.is(ItemTags.COALS))
            nodeEntity.setSyncedPluginData(installedId, WORKING_MATERIAL, WorkingMaterial.COAL.ordinal());
        else
            nodeEntity.setSyncedPluginData(installedId, WORKING_MATERIAL, WorkingMaterial.ANY.ordinal());

        itemStack.shrink(1);
        return new IGeneratorAdjuster.AdjustedParameters(bt, pt);
    }

    @Override
    public void tickWork() {
        super.tickWork();
        if (remainBurnTicks == 0 && nodeEntity.getSyncedPluginData(installedId, WORKING_MATERIAL) != WorkingMaterial.IDLE.ordinal())
            nodeEntity.setSyncedPluginData(installedId, WORKING_MATERIAL, WorkingMaterial.IDLE.ordinal());
    }
}
