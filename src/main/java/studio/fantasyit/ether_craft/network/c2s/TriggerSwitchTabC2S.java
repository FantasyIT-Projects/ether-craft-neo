package studio.fantasyit.ether_craft.network.c2s;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.node.NodePluginManager;
import studio.fantasyit.ether_craft.node.plugins.InstalledPlugin;

public record TriggerSwitchTabC2S(InstalledPlugin plugin) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<@NotNull TriggerSwitchTabC2S> TYPE = new CustomPacketPayload.Type<>(
            Identifier.fromNamespaceAndPath(
                    EtherCraft.MODID, "trigger_switch_tab"
            )
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, @NotNull TriggerSwitchTabC2S> CODEC = StreamCodec.composite(
            InstalledPlugin.STREAM_CODEC,
            TriggerSwitchTabC2S::plugin,
            TriggerSwitchTabC2S::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
