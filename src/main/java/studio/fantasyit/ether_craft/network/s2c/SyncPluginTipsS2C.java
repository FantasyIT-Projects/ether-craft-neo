package studio.fantasyit.ether_craft.network.s2c;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.node.tip.NodePluginTipManager;
import studio.fantasyit.ether_craft.node.tip.TipInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public record SyncPluginTipsS2C(List<Entry> entries) implements CustomPacketPayload {
    public static final Type<@NotNull SyncPluginTipsS2C> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(EtherCraft.MODID, "sync_plugin_tips")
    );

    public record Entry(Identifier pluginId, TipInfo tipInfo) {
        public static final StreamCodec<RegistryFriendlyByteBuf, Entry> STREAM_CODEC = StreamCodec.composite(
                Identifier.STREAM_CODEC,
                Entry::pluginId,
                TipInfo.STREAM_CODEC,
                Entry::tipInfo,
                Entry::new
        );
    }

    public static final StreamCodec<RegistryFriendlyByteBuf, @NotNull SyncPluginTipsS2C> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.collection(ArrayList::new, Entry.STREAM_CODEC),
                    SyncPluginTipsS2C::entries,
                    SyncPluginTipsS2C::new
            );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            Map<Identifier, TipInfo> map = new HashMap<>();
            for (Entry entry : entries) {
                map.put(entry.pluginId(), entry.tipInfo());
            }
            NodePluginTipManager.INSTANCE.setClientTips(map);
        });
    }
}
