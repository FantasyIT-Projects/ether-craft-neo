package studio.fantasyit.ether_craft.node.plugins.base;

import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import studio.fantasyit.ether_craft.block.node.EtherAdaptNodeEntity;
import studio.fantasyit.ether_craft.menu.node.EtherAdaptNodeContainerMenu;
import studio.fantasyit.ether_craft.network.base.ISyncTargetMenu;
import studio.fantasyit.ether_craft.network.c2s.SyncScreenDataC2S;
import studio.fantasyit.ether_craft.node.NodeProperty;
import studio.fantasyit.ether_craft.node.plugins.InstalledPlugin;

import java.util.function.Supplier;

public abstract class AbstractNodePlugin implements ISyncTargetMenu {
    protected final EtherAdaptNodeEntity nodeEntity;
    public InstalledPlugin installedId;

    public AbstractNodePlugin(EtherAdaptNodeEntity nodeEntity, InstalledPlugin installedId) {
        this.nodeEntity = nodeEntity;
        this.installedId = installedId;
    }

    public void queueWithCd(Identifier action, int cd, Supplier<Boolean> runnable) {
        if (!nodeEntity.ticket.allowed(action, installedId))
            return;
        if (runnable.get())
            nodeEntity.ticket.requeue(action, installedId, cd);
    }

    public void modifyNodeProperty(NodeProperty nodeProperty) {
    }

    public void tick() {
    }

    public void saveAdditional(ValueOutput output) {
    }

    public void loadAdditional(ValueInput input) {
    }

    public boolean inputFilter(ItemResource resource) {
        return true;
    }

    public boolean outputFilter(ItemResource resource) {
        return true;
    }

    public int earlyHandleInput(ItemResource resource, int amount, TransactionContext context) {
        return 0;
    }

    public void onDestroy() {
    }

    public void onWrenchRotate(Direction.Axis axis) {
    }

    public void registerSlots(EtherAdaptNodeContainerMenu menu) {
    }

    @Override
    public void syncScreenData(SyncScreenDataC2S message) {
    }

    public PluginMenuContext<?> makeContext(EtherAdaptNodeContainerMenu etherAdaptNodeContainerMenu) {
        return PluginMenuContext.of(etherAdaptNodeContainerMenu, this);
    }
}