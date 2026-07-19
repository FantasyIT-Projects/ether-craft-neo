package studio.fantasyit.ether_craft.node.plugins.function;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.transfer.item.ItemResource;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.block.node.EtherAdaptNodeEntity;
import studio.fantasyit.ether_craft.datapack.StoneGeneratorRatio;
import studio.fantasyit.ether_craft.node.plugins.InstalledPlugin;
import studio.fantasyit.ether_craft.node.plugins.upgrade.IGeneratorAdjuster;

public class FunctionStoneGenerator extends AbstractItemConsumeFunction {
    public static Identifier ID = EtherCraft.id("generator/stone");

    public FunctionStoneGenerator(EtherAdaptNodeEntity nodeEntity, InstalledPlugin ID) {
        super(nodeEntity, ID);
    }

    @Override
    boolean accepts(ItemResource stack) {
        return StoneGeneratorRatio.get(stack.toStack()) != null;
    }

    @Override
    IGeneratorAdjuster.AdjustedParameters onConsumeItem(ItemStack itemStack) {
        StoneGeneratorRatio stoneGeneratorRatio = StoneGeneratorRatio.get(itemStack);
        if (stoneGeneratorRatio == null)
            return new IGeneratorAdjuster.AdjustedParameters(0, 0);
        int ept = stoneGeneratorRatio.etherPerTick();

        if (itemStack.is(Items.COBBLED_DEEPSLATE) || itemStack.is(Items.BASALT))
            nodeEntity.setSyncedPluginData(installedId, WORKING_MATERIAL, WorkingMaterial.DEEPSLATE.ordinal());
        else
            nodeEntity.setSyncedPluginData(installedId, WORKING_MATERIAL, WorkingMaterial.STONE.ordinal());

        itemStack.shrink(1);
        return new IGeneratorAdjuster.AdjustedParameters(stoneGeneratorRatio.burnTicks(), ept);
    }

    @Override
    public void tickWork() {
        super.tickWork();
        if (remainBurnTicks == 0 && nodeEntity.getSyncedPluginData(installedId, WORKING_MATERIAL) != WorkingMaterial.IDLE.ordinal())
            nodeEntity.setSyncedPluginData(installedId, WORKING_MATERIAL, WorkingMaterial.IDLE.ordinal());
    }
}
