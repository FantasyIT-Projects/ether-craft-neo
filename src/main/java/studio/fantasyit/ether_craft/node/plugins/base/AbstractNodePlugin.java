package studio.fantasyit.ether_craft.node.plugins.base;

import net.minecraft.resources.Identifier;
import studio.fantasyit.ether_craft.block.node.EtherAdaptNodeEntity;
import studio.fantasyit.ether_craft.menu.node.EtherAdaptNodeContainerMenu;
import studio.fantasyit.ether_craft.node.plugins.InstalledPlugin;

import java.util.function.Supplier;

public abstract class AbstractNodePlugin {
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

    public PluginMenuContext<?> makeContext(EtherAdaptNodeContainerMenu etherAdaptNodeContainerMenu) {
        return PluginMenuContext.of(etherAdaptNodeContainerMenu, this);
    }
}