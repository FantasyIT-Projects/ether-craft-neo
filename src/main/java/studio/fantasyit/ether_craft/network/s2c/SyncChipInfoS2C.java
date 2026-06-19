package studio.fantasyit.ether_craft.network.s2c;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.factory.EtherProcessChipManager;

import java.util.HashMap;
import java.util.Map;

public record SyncChipInfoS2C(Map<Identifier, EtherProcessChipManager.ProcessChipRecord> chipInfo)
        implements CustomPacketPayload {

    public static final Type<@NotNull SyncChipInfoS2C> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(EtherCraft.MODID, "sync_chip_info")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, @NotNull SyncChipInfoS2C> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.map(HashMap::new, Identifier.STREAM_CODEC, EtherProcessChipManager.ProcessChipRecord.STREAM_CODEC),
                    SyncChipInfoS2C::chipInfo,
                    SyncChipInfoS2C::new
            );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            EtherProcessChipManager.update(chipInfo);
        });
    }
}
