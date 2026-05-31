package studio.fantasyit.ether_craft.node.plugins.upgrade;

import net.minecraft.resources.Identifier;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.block.base.ItemFilter;
import studio.fantasyit.ether_craft.block.node.EtherAdaptNodeEntity;
import studio.fantasyit.ether_craft.menu.base.slot.BaseDataSlot;
import studio.fantasyit.ether_craft.menu.node.EtherAdaptNodeContainerMenu;
import studio.fantasyit.ether_craft.network.c2s.SyncScreenDataC2S;
import studio.fantasyit.ether_craft.node.filter.FilterGuiRegCommon;
import studio.fantasyit.ether_craft.node.plugins.InstalledPlugin;
import studio.fantasyit.ether_craft.node.plugins.base.AbstractNodePlugin;

public class DestructionUpgrade extends AbstractNodePlugin {
    public static final Identifier ID = EtherCraft.id("destruction");
    public static final Identifier SYNC_MODE = EtherCraft.id("destruction/mode");

    public enum DestroyMode {
        OVERFLOW, ALL
    }

    public ItemFilter filter = new ItemFilter(21, nodeEntity::setChanged);
    public DestroyMode destroyMode = DestroyMode.OVERFLOW;

    public DestructionUpgrade(EtherAdaptNodeEntity nodeEntity, InstalledPlugin installedId) {
        super(nodeEntity, installedId);
        filter.whitelist = true;
    }

    @Override
    public int earlyHandleInput(ItemResource resource, int amount, TransactionContext context) {
        if (destroyMode != DestroyMode.ALL) return 0;
        if (!filter.accepts(resource)) return 0;
        return amount;
    }

    @Override
    public int handleOverflow(ItemResource resource, int amount, TransactionContext transaction) {
        if (destroyMode != DestroyMode.OVERFLOW) return 0;
        if (!filter.accepts(resource)) return 0;
        return amount;
    }

    @Override
    public void saveAdditional(ValueOutput output) {
        output.putBoolean("desMode", destroyMode == DestroyMode.ALL);
        filter.serialize(output);
    }

    @Override
    public void loadAdditional(ValueInput input) {
        destroyMode = input.getBooleanOr("desMode", false) ? DestroyMode.ALL : DestroyMode.OVERFLOW;
        filter.deserialize(input);
        filter.whitelist = true;
    }

    @Override
    public void syncScreenData(SyncScreenDataC2S message) {
        FilterGuiRegCommon.sync(message, filter);
        if (message.id().equals(SYNC_MODE)) {
            destroyMode = message.data() == 1 ? DestroyMode.ALL : DestroyMode.OVERFLOW;
            nodeEntity.pluginUpdate();
        }
    }

    @Override
    public void registerSlots(EtherAdaptNodeContainerMenu menu) {
        FilterGuiRegCommon.slots(menu, filter);
        menu.addDataSlot(new BaseDataSlot(() -> destroyMode == DestroyMode.ALL ? 1 : 0, t -> destroyMode = (t == 1 ? DestroyMode.ALL : DestroyMode.OVERFLOW)));
    }
}
