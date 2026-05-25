package studio.fantasyit.ether_craft.network.c2s;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;
import studio.fantasyit.ether_craft.EtherCraft;

public record FactoryMenuSwitchItemC2S(
        boolean reverse
) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<@NotNull FactoryMenuSwitchItemC2S> TYPE = new CustomPacketPayload.Type<>(
            Identifier.fromNamespaceAndPath(EtherCraft.MODID, "factory_menu_switch_item_c2s")
    );
    public static final StreamCodec<FriendlyByteBuf, FactoryMenuSwitchItemC2S> CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL,
            FactoryMenuSwitchItemC2S::reverse,
            FactoryMenuSwitchItemC2S::new
    );


    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
