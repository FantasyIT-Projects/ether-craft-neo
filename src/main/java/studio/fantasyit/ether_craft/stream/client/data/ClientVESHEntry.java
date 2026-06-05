package studio.fantasyit.ether_craft.stream.client.data;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.world.level.Level;
import studio.fantasyit.ether_craft.stream.PosDir;

import java.util.ArrayList;
import java.util.List;

public class ClientVESHEntry {
    public final PosDir posDir;
    public final Object2ObjectOpenHashMap<Integer, ClientStreamEntry> streams = new Object2ObjectOpenHashMap<>();
    public final List<ClientStreamEntry> steamsIterable = new ArrayList<>();

    public ClientVESHEntry(PosDir posDir) {
        this.posDir = posDir;
    }

    public void tick(Level lv) {
        steamsIterable.forEach(t -> t.tick(lv));
        steamsIterable.stream().filter(e -> e.removed).forEach(t -> {
            t.attachedLogic.forEach(logic -> logic.onDestroy(t));
        });
        steamsIterable.removeIf(e -> e.removed);
        streams.values().removeIf(e -> e.removed);
    }

    public void addStream(int i, ClientStreamEntry clientStreamEntry) {
        streams.put(i, clientStreamEntry);
        steamsIterable.add(clientStreamEntry);
    }
}