package studio.fantasyit.ether_craft.node.plugins.function;

import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.item.ItemResource;
import studio.fantasyit.ether_craft.Config;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.block.node.EtherAdaptNodeEntity;
import studio.fantasyit.ether_craft.menu.node.EtherAdaptNodeContainerMenu;
import studio.fantasyit.ether_craft.menu.node.slot.OversizedEtherSlot;
import studio.fantasyit.ether_craft.node.plugins.InstalledPlugin;
import studio.fantasyit.ether_craft.register.Tags;

public class FunctionEquipmentConsumeGenerator extends AbstractItemConsumeFunction {
    public static Identifier ID = EtherCraft.id("generator/equipment");

    int etherToGenerate = 0;

    public FunctionEquipmentConsumeGenerator(EtherAdaptNodeEntity nodeEntity, InstalledPlugin ID) {
        super(nodeEntity, ID);
    }

    @Override
    boolean accepts(ItemResource stack) {
        return stack.toStack().is(Tags.CONSUMABLE_EQUIPMENTS);
    }

    @Override
    ItemStack onConsumeItem(ItemStack itemStack) {
        int base = 1;
        int enchantBonus = 0;
        for (Object2IntMap.Entry<Holder<Enchantment>> entry : itemStack.getEnchantments().entrySet()) {
            if (entry.getKey().is(EnchantmentTags.CURSE)) continue;
            enchantBonus += 1 << (entry.getIntValue() - 1);
        }
        etherToGenerate = (base + enchantBonus) * Config.equipmentEtherCoefficient;

        nodeEntity.setSyncedPluginData(installedId, WORKING_MATERIAL, WorkingMaterial.ANY.ordinal());
        return ItemStack.EMPTY;
    }

    @Override
    void onBurnTick() {
        nodeEntity.receiveEther(etherToGenerate);
        etherToGenerate = 0;
    }

    @Override
    public void tickWork() {
        super.tickWork();
        if (remainBurnTicks == 0 && nodeEntity.getSyncedPluginData(installedId, WORKING_MATERIAL) != WorkingMaterial.IDLE.ordinal())
            nodeEntity.setSyncedPluginData(installedId, WORKING_MATERIAL, WorkingMaterial.IDLE.ordinal());
    }

    @Override
    public void registerSlots(EtherAdaptNodeContainerMenu menu) {
        menu.addSlotDraw(new OversizedEtherSlot(nodeEntity.etherStorage, 0, 28, 20));
        menu.addSlotDraw(new net.minecraft.world.inventory.Slot(container, 0, 28, 44));
    }

    @Override
    public void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        etherToGenerate = input.read("etherToGenerate", Codec.INT).orElse(0);
    }

    @Override
    public void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.store("etherToGenerate", Codec.INT, etherToGenerate);
    }
}
