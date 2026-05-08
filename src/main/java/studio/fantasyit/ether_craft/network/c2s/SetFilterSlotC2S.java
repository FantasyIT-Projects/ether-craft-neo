package studio.fantasyit.ether_craft.network.c2s;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.node.plugins.InstalledPlugin;

public record SetFilterSlotC2S(int slot, ItemStack stack) implements CustomPacketPayload {
    public static final Type<@NotNull SetFilterSlotC2S> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(
                    EtherCraft.MODID, "set_filter_slot"
            )
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, @NotNull SetFilterSlotC2S> CODEC = StreamCodec.composite(
            ByteBufCodecs.INT,
            SetFilterSlotC2S::slot,
            ItemStack.OPTIONAL_STREAM_CODEC,
            SetFilterSlotC2S::stack,
            SetFilterSlotC2S::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
