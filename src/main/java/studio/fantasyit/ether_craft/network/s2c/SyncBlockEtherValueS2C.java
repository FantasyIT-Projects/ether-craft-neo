package studio.fantasyit.ether_craft.network.s2c;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.block.base.EtherContainer;

public record SyncBlockEtherValueS2C(
        long ether,
        BlockPos pos,
        Identifier levelId
) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<@NotNull SyncBlockEtherValueS2C> TYPE = new CustomPacketPayload.Type<>(
            Identifier.fromNamespaceAndPath(
                    EtherCraft.MODID, "sync_block_ether_value"
            )
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, @NotNull SyncBlockEtherValueS2C> CODEC = StreamCodec.composite(
            ByteBufCodecs.LONG,
            SyncBlockEtherValueS2C::ether,
            BlockPos.STREAM_CODEC,
            SyncBlockEtherValueS2C::pos,
            Identifier.STREAM_CODEC,
            SyncBlockEtherValueS2C::levelId,
            SyncBlockEtherValueS2C::new
    );

    @Override
    public @NotNull Type<@NotNull SyncBlockEtherValueS2C> type() {
        return TYPE;
    }

    public void handle(IPayloadContext iPayloadContext) {
        iPayloadContext.enqueueWork(()->{
            Level level = iPayloadContext.player().level();
            if(level.dimension().identifier().equals(levelId)){
                EtherContainer capability = level.getCapability(EtherContainer.ETHER_CONTAINER, pos);
                if(capability != null){
                    capability.setEther(ether);
                }
            }
        });

    }
}
