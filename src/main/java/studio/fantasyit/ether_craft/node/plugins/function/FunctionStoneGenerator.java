package studio.fantasyit.ether_craft.node.plugins.function;

import com.mojang.serialization.Codec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.block.node.EtherAdaptNodeEntity;
import studio.fantasyit.ether_craft.datapack.StoneGeneratorRatio;
import studio.fantasyit.ether_craft.node.plugins.InstalledPlugin;

public class FunctionStoneGenerator extends AbstractItemConsumeFunction {
    public static Identifier ID = EtherCraft.id("generator/furnace");
    public static Identifier WORKING_MATERIAL = EtherCraft.id("generator/furnace/material");

    int ept = 0;

    public FunctionStoneGenerator(EtherAdaptNodeEntity nodeEntity, InstalledPlugin ID) {
        super(nodeEntity, ID);
    }

    @Override
    boolean accepts(ItemResource stack) {
        return StoneGeneratorRatio.get(stack.toStack()) != null;
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
        StoneGeneratorRatio stoneGeneratorRatio = StoneGeneratorRatio.get(itemStack);
        if (stoneGeneratorRatio == null)
            return itemStack;
        ItemStack remainStack = itemStack.copyWithCount(itemStack.getCount() - 1);
        this.remainBurnTicks = stoneGeneratorRatio.burnTicks();
        ept = stoneGeneratorRatio.etherPerTick();

        if (itemStack.is(Items.COBBLED_DEEPSLATE))
            nodeEntity.setSyncedPluginData(WORKING_MATERIAL, WorkingMaterial.DEEPSLATE.ordinal());
        else
            nodeEntity.setSyncedPluginData(WORKING_MATERIAL, WorkingMaterial.STONE.ordinal());

        return remainStack;
    }

    @Override
    void onBurnTick() {
        nodeEntity.receiveEther(ept);
    }

    @Override
    public void tick() {
        super.tick();
        if (remainBurnTicks == 0)
            nodeEntity.setSyncedPluginData(WORKING_MATERIAL, WorkingMaterial.IDLE.ordinal());
    }

    @Override
    public void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        ept = input.read("ept", Codec.INT).orElse(0);
    }

    @Override
    public void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.store("ept", Codec.INT, ept);
    }
}
