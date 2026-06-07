package studio.fantasyit.ether_craft.network.s2c;

import net.minecraft.client.Minecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;
import studio.fantasyit.ether_craft.EtherCraft;

public record PlatingSoulStateS2C(boolean active) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<@NotNull PlatingSoulStateS2C> TYPE = new CustomPacketPayload.Type<>(
            Identifier.fromNamespaceAndPath(EtherCraft.MODID, "plating_soul_state")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, @NotNull PlatingSoulStateS2C> CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL,
            PlatingSoulStateS2C::active,
            PlatingSoulStateS2C::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static boolean clientSoulActive = false;
    private static double clientSoulX, clientSoulY, clientSoulZ;

    public static void handle(PlatingSoulStateS2C packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            clientSoulActive = packet.active();
            if (packet.active()) {
                var player = context.player();
                clientSoulX = player.getX();
                clientSoulY = player.getY() + player.getEyeHeight();
                clientSoulZ = player.getZ();
                Minecraft.getInstance().setCameraEntity(null);
            } else {
                Minecraft.getInstance().setCameraEntity(Minecraft.getInstance().player);
            }
        });
    }

    public static boolean isClientSoulActive() { return clientSoulActive; }
    public static double getClientSoulX() { return clientSoulX; }
    public static double getClientSoulY() { return clientSoulY; }
    public static double getClientSoulZ() { return clientSoulZ; }
    public static void updateClientSoulPos(double x, double y, double z) {
        clientSoulX = x;
        clientSoulY = y;
        clientSoulZ = z;
    }
}
