package studio.fantasyit.ether_craft.block.node;

import net.minecraft.resources.Identifier;
import studio.fantasyit.ether_craft.node.plugins.InstalledPlugin;

import java.util.IdentityHashMap;
import java.util.Map;

public class QueuedTicket {
    private final Map<Identifier, Slot> slots = new IdentityHashMap<>(4);
    private final Map<InstalledPlugin, Node> byPlugin = new IdentityHashMap<>(8);
    private long tickCount;

    private static final class Slot {
        Node first, last;

        void append(Node node) {
            node.slot = this;
            node.qPrev = last;
            node.qNext = null;
            if (last != null)
                last.qNext = node;
            last = node;
            if (first == null)
                first = node;
        }

        void remove(Node node) {
            if (node.qPrev != null)
                node.qPrev.qNext = node.qNext;
            else
                first = node.qNext;
            if (node.qNext != null)
                node.qNext.qPrev = node.qPrev;
            else
                last = node.qPrev;
            node.slot = null;
            node.qPrev = null;
            node.qNext = null;
        }
    }

    private static final class Node {
        final InstalledPlugin plugin;
        Slot slot;
        Node qPrev, qNext;
        long cdTargetTick;
        boolean inCd;

        Node(InstalledPlugin plugin) {
            this.plugin = plugin;
        }
    }

    public boolean allowed(Identifier actionId, InstalledPlugin plugin) {
        Node node = byPlugin.get(plugin);
        if (node == null) {
            node = new Node(plugin);
            byPlugin.put(plugin, node);
        }
        if (node.inCd) {
            if (tickCount < node.cdTargetTick)
                return false;
            node.inCd = false;
        }
        if (node.slot != null)
            return node.slot.first == node;
        Slot slot = slots.get(actionId);
        if (slot == null) {
            slot = new Slot();
            slots.put(actionId, slot);
        }
        boolean wasEmpty = slot.first == null;
        slot.append(node);
        return wasEmpty;
    }

    public void requeue(Identifier actionId, InstalledPlugin plugin, int cd) {
        Slot slot = slots.get(actionId);
        if (slot == null)
            return;
        Node node = byPlugin.get(plugin);
        if (node == null) {
            node = new Node(plugin);
            byPlugin.put(plugin, node);
        }
        if (node.slot == slot && slot.first == node)
            slot.remove(node);
        if (cd > 0) {
            node.inCd = true;
            node.cdTargetTick = tickCount + cd + 1;
        }
    }

    public void tick(EtherAdaptNodeEntity nodeEntity) {
        tickCount++;
        for (Slot slot : slots.values()) {
            for (Node node = slot.first; node != null; ) {
                Node next = node.qNext;
                if (!nodeEntity.isPluginInstalled(node.plugin))
                    slot.remove(node);
                node = next;
            }
        }
    }
}
