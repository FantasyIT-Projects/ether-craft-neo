package studio.fantasyit.ether_craft.stream.vholder;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntSet;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import studio.fantasyit.ether_craft.Config;

import java.util.ArrayList;
import java.util.List;

public class PlayerPayloadAccumulator {
    private final boolean dimensionMode;
    private final Int2ObjectOpenHashMap<List<CustomPacketPayload>> byPlayer = new Int2ObjectOpenHashMap<>();
    private final List<CustomPacketPayload> dimensionAll = new ArrayList<>();

    public PlayerPayloadAccumulator() {
        this.dimensionMode = Config.etherStreamSyncDistance <= 0;
    }

    public void add(IntSet targetIds, CustomPacketPayload payload) {
        if (dimensionMode) {
            dimensionAll.add(payload);
            return;
        }
        for (int id : targetIds) {
            byPlayer.computeIfAbsent(id, k -> new ArrayList<>()).add(payload);
        }
    }

    public void send(ServerLevel level) {
        if (dimensionMode) {
            if (dimensionAll.isEmpty()) return;
            PacketDistributor.sendToPlayersInDimension(level, dimensionAll.get(0),
                    dimensionAll.subList(1, dimensionAll.size()).toArray(CustomPacketPayload[]::new));
            return;
        }
        if (byPlayer.isEmpty()) return;
        for (ServerPlayer p : level.players()) {
            List<CustomPacketPayload> list = byPlayer.get(p.getId());
            if (list == null || list.isEmpty()) continue;
            PacketDistributor.sendToPlayer(p, list.get(0),
                    list.subList(1, list.size()).toArray(CustomPacketPayload[]::new));
        }
    }
}
