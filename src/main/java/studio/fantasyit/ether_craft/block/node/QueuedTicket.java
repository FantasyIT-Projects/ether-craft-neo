package studio.fantasyit.ether_craft.block.node;

import net.minecraft.resources.Identifier;
import studio.fantasyit.ether_craft.node.plugins.InstalledPlugin;

import java.util.*;

public class QueuedTicket {
    public Map<Identifier, List<InstalledPlugin>> queuedPlugins = new HashMap<>();
    public Map<InstalledPlugin, Integer> queuedCd = new HashMap<>();

    public boolean allowed(Identifier actionId, InstalledPlugin plugin) {
        if (queuedCd.containsKey(plugin))
            return false;
        if (!queuedPlugins.containsKey(actionId))
            queuedPlugins.put(actionId, new ArrayList<>());
        if (!queuedPlugins.get(actionId).contains(plugin)) {
            queuedPlugins.get(actionId).add(plugin);
        }
        if (queuedPlugins.get(actionId).getFirst().equals(plugin)) {
            return true;
        }
        return false;
    }

    public void requeue(Identifier actionId, InstalledPlugin plugin, int cd) {
        if (!queuedPlugins.containsKey(actionId))
            return;
        if (queuedPlugins.get(actionId).getFirst().equals(plugin))
            queuedPlugins.get(actionId).removeFirst();
        if (cd > 0)
            queuedCd.put(plugin, cd);
    }

    public void tick(EtherAdaptNodeEntity nodeEntity) {
        HashSet<InstalledPlugin> keys = new HashSet<>(queuedCd.keySet());
        for (InstalledPlugin plugin : keys) {
            if (queuedCd.get(plugin) <= 0 || !nodeEntity.isPluginInstalled(plugin)) {
                queuedCd.remove(plugin);
            } else {
                queuedCd.put(plugin, queuedCd.get(plugin) - 1);
            }
        }
        for (Identifier actionId : queuedPlugins.keySet()) {
            queuedPlugins.get(actionId).removeIf(plugin -> !nodeEntity.isPluginInstalled(plugin));
        }
    }
}
