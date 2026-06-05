package studio.fantasyit.ether_craft.stream.client.data;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

public class ClientVESHEntry {
    public final Object2ObjectOpenHashMap<Integer, ClientStreamEntry> streams = new Object2ObjectOpenHashMap<>();
}