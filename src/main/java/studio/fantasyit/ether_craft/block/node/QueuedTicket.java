package studio.fantasyit.ether_craft.block.node;

import net.minecraft.resources.Identifier;
import studio.fantasyit.ether_craft.node.NodePluginManager;
import studio.fantasyit.ether_craft.node.plugins.InstalledPlugin;
import studio.fantasyit.ether_craft.node.plugins.base.AbstractNodePlugin;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class QueuedTicket {
    public Map<Identifier, List<InstalledPlugin>> queuedPlugins = new IdentityHashMap<>();
    public Map<InstalledPlugin, Integer> queuedCd = new IdentityHashMap<>();

    public boolean allowed(Identifier actionId, InstalledPlugin plugin) {
        if (queuedCd.containsKey(plugin))
            return false;
        List<InstalledPlugin> queue = queuedPlugins.get(actionId);
        if (queue == null) {
            List<InstalledPlugin> fresh = new ArrayList<>(1);
            fresh.add(plugin);
            queuedPlugins.put(actionId, fresh);
            return true;
        }
        if (queue.isEmpty() || queue.getFirst().equals(plugin))
            return true;
        if (!queue.contains(plugin))
            queue.add(plugin);
        return false;
    }

    public void requeue(Identifier actionId, InstalledPlugin plugin, int cd) {
        List<InstalledPlugin> queue = queuedPlugins.get(actionId);
        if (queue == null)
            return;
        if (!queue.isEmpty() && queue.getFirst().equals(plugin))
            queue.removeFirst();
        if (cd > 0)
            queuedCd.put(plugin, cd);
    }

    public void tick(EtherAdaptNodeEntity nodeEntity) {
        Iterator<Map.Entry<InstalledPlugin, Integer>> it = queuedCd.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<InstalledPlugin, Integer> entry = it.next();
            if (entry.getValue() <= 0 || !isCurrentInstance(nodeEntity, entry.getKey()))
                it.remove();
            else
                entry.setValue(entry.getValue() - 1);
        }
        for (List<InstalledPlugin> queue : queuedPlugins.values())
            queue.removeIf(plugin -> !nodeEntity.isPluginInstalled(plugin));
    }

    private boolean isCurrentInstance(EtherAdaptNodeEntity nodeEntity, InstalledPlugin plugin) {
        AbstractNodePlugin current;
        if (plugin.type() == NodePluginManager.PluginType.FUNCTION)
            current = nodeEntity.functionStorage.getPlugin(plugin.id());
        else if (plugin.type() == NodePluginManager.PluginType.FEATURE || plugin.type() == NodePluginManager.PluginType.UPGRADE)
            current = nodeEntity.featureUpgradeStorage.getPlugin(plugin.id());
        else
            return false;
        return current != null && current.installedId == plugin;
    }
}
