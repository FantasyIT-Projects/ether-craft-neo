package studio.fantasyit.ether_craft.network.c2s;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;
import studio.fantasyit.ether_craft.EtherCraft;

public record PerfVizToggleC2S(boolean enabled) implements CustomPacketPayload {
    public static final Type<@NotNull PerfVizToggleC2S> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(EtherCraft.MODID, "perf_viz_toggle")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, @NotNull PerfVizToggleC2S> CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL,
            PerfVizToggleC2S::enabled,
            PerfVizToggleC2S::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
