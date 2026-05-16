package studio.fantasyit.ether_craft.network.s2c;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.block.factory.EtherProcessFactoryEntity;
import studio.fantasyit.ether_craft.block.node.EtherAdaptNodeEntity;

public record SyncBlockNameS2C(BlockPos pos, String name) implements CustomPacketPayload {
    public static final Type<@NotNull SyncBlockNameS2C> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(EtherCraft.MODID, "sync_block_name")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, @NotNull SyncBlockNameS2C> CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC,
            SyncBlockNameS2C::pos,
            ByteBufCodecs.STRING_UTF8,
            SyncBlockNameS2C::name,
            SyncBlockNameS2C::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            Level level = ctx.player().level();
            var be = level.getBlockEntity(pos);
            if (be instanceof EtherAdaptNodeEntity node) {
                node.name = name;
            } else if (be instanceof EtherProcessFactoryEntity factory) {
                factory.name = name;
            }
        });
    }
}
