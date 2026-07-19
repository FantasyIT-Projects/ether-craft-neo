package studio.fantasyit.ether_craft.node.plugins.function;

import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.item.ItemResource;
import studio.fantasyit.ether_craft.Config;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.block.node.EtherAdaptNodeEntity;
import studio.fantasyit.ether_craft.node.plugins.InstalledPlugin;
import studio.fantasyit.ether_craft.node.plugins.upgrade.IGeneratorAdjuster;
import studio.fantasyit.ether_craft.register.Tags;

public class FunctionEquipmentConsumeGenerator extends AbstractItemConsumeFunction {
    public static Identifier ID = EtherCraft.id("generator/equipment");
    public static final Identifier STATE = EtherCraft.id("generator/ether_converter_state");


    public FunctionEquipmentConsumeGenerator(EtherAdaptNodeEntity nodeEntity, InstalledPlugin ID) {
        super(nodeEntity, ID);
    }

    @Override
    boolean accepts(ItemResource stack) {
        return stack.toStack().is(Tags.CONSUMABLE_EQUIPMENTS) || stack.toStack().has(DataComponents.STORED_ENCHANTMENTS);
    }

    @Override
    IGeneratorAdjuster.AdjustedParameters onConsumeItem(ItemStack itemStack) {
        int enchantBonus = 0;
        int etherToGenerate = 0;
        for (Object2IntMap.Entry<Holder<Enchantment>> entry : EnchantmentHelper.getEnchantmentsForCrafting(itemStack).entrySet()) {
            if (entry.getKey().is(EnchantmentTags.CURSE)) continue;

            int level = entry.getIntValue();
            if (entry.getKey().is(EnchantmentTags.TREASURE))
                level += 2;

            if (level >= 5)
                enchantBonus += level * level;
            else
                enchantBonus += (1 << level);
        }
        etherToGenerate = enchantBonus * Config.nodeEquipmentGeneratorCoefficient + Config.nodeEquipmentGeneratorBaseAmount;
        nodeEntity.setSyncedPluginData(installedId, WORKING_MATERIAL, WorkingMaterial.ANY.ordinal());
        itemStack.shrink(1);
        return new IGeneratorAdjuster.AdjustedParameters(Config.nodeEquipmentGeneratorBurnTick, etherToGenerate);
    }


    @Override
    public void tickWork() {
        super.tickWork();
        if (remainBurnTicks == 0 && nodeEntity.getSyncedPluginData(installedId, WORKING_MATERIAL) != WorkingMaterial.IDLE.ordinal())
            nodeEntity.setSyncedPluginData(installedId, WORKING_MATERIAL, WorkingMaterial.IDLE.ordinal());

        nodeEntity.setSyncedPluginData(installedId, STATE, remainBurnTicks > 0 ? 1 : 0);
    }
}
