package studio.fantasyit.ether_craft.node.plugins.function;

import com.mojang.serialization.Codec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import studio.fantasyit.ether_craft.Config;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.block.node.EtherAdaptNodeEntity;
import studio.fantasyit.ether_craft.menu.node.EtherAdaptNodeContainerMenu;
import studio.fantasyit.ether_craft.menu.node.slot.OversizedEtherSlot;
import studio.fantasyit.ether_craft.node.NodeProperty;
import studio.fantasyit.ether_craft.node.plugins.InstalledPlugin;
import studio.fantasyit.ether_craft.node.plugins.base.AbstractNodePlugin;
import studio.fantasyit.ether_craft.node.plugins.base.IEarlyHandleInputPlugin;
import studio.fantasyit.ether_craft.node.plugins.base.ILoadAdditionalPlugin;
import studio.fantasyit.ether_craft.node.plugins.base.IModifyNodePropertyPlugin;
import studio.fantasyit.ether_craft.node.plugins.base.IRegisterSlotsPlugin;
import studio.fantasyit.ether_craft.node.plugins.base.ISaveAdditionalPlugin;
import studio.fantasyit.ether_craft.node.plugins.base.ITickWorkPlugin;

public class FunctionEtherConverter extends AbstractNodePlugin implements IEarlyHandleInputPlugin, IModifyNodePropertyPlugin, ITickWorkPlugin, ISaveAdditionalPlugin, ILoadAdditionalPlugin, IRegisterSlotsPlugin {
    public static final Identifier ID = EtherCraft.id("generator/ether_converter");
    public static final Identifier STATE = EtherCraft.id("generator/ether_converter_state");

    private int remainWorkTicks = 0;

    public FunctionEtherConverter(EtherAdaptNodeEntity nodeEntity, InstalledPlugin installedId) {
        super(nodeEntity, installedId);
    }

    @Override
    public int earlyHandleInput(ItemStack stack, int amount, TransactionContext context) {
        if (amount <= 0)
            return 0;
        long etherToAdd = (long) amount * Config.nodeEtherConverterCoefficient;
        if (context != null)
            nodeEntity.etherStorage.receiveEtherTransactional(etherToAdd, context);
        else
            nodeEntity.receiveEther(etherToAdd);
        remainWorkTicks = 40;
        nodeEntity.setSyncedPluginData(installedId, STATE, 1);
        return amount;
    }

    @Override
    public void modifyNodeProperty(NodeProperty nodeProperty) {
        if (nodeProperty.slotUnlock == 0)
            nodeProperty.slotUnlock = 1;
    }

    @Override
    public void tickWork() {
        if (remainWorkTicks > 0) {
            remainWorkTicks--;
            if (remainWorkTicks <= 0)
                nodeEntity.setSyncedPluginData(installedId, STATE, 0);
        }
        nodeEntity.extractWithPredicate(s -> true, 64);

    }

    @Override
    public void saveAdditional(ValueOutput output) {
        output.putInt("remainWorkTicks", remainWorkTicks);
    }

    @Override
    public void loadAdditional(ValueInput input) {
        remainWorkTicks = input.read("remainWorkTicks", Codec.INT).orElse(0);
    }

    @Override
    public void registerSlots(EtherAdaptNodeContainerMenu menu) {
        menu.addSlotDraw(new OversizedEtherSlot(nodeEntity.etherStorage, 0, 28, 20));
    }
}
