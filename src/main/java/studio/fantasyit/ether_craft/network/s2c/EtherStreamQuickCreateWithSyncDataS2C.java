package studio.fantasyit.ether_craft.network.s2c;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.network.base.IEtherQuickCreator;
import studio.fantasyit.ether_craft.register.AttachmentDataRegistry;
import studio.fantasyit.ether_craft.stream.PosDir;
import studio.fantasyit.ether_craft.stream.client.data.ClientVESHDataGetter;
import studio.fantasyit.ether_craft.stream.data.IEtherStreamSyncedData;
import studio.fantasyit.ether_craft.stream.data.SyncedEtherStreamDataManager;
import studio.fantasyit.ether_craft.stream.idx.AutoIndexPosDir;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 轻量快速创建包（带同步数据）：客户端用上一次完整创建包的快照派生新流，
 * 并附带与快照不同的 syncedData（label/显示物品/携带实体等）。
 * tickCount（可选 firstTick）与 ether 为可选的覆盖值，语义同 EtherStreamQuickCreateS2C。
 */
public record EtherStreamQuickCreateWithSyncDataS2C(
        AutoIndexPosDir posDir,
        Optional<Integer> tickCount,
        Optional<Integer> ether,
        List<IEtherStreamSyncedData> syncedData
) implements CustomPacketPayload, IEtherQuickCreator {

    public static final Type<@NotNull EtherStreamQuickCreateWithSyncDataS2C> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(EtherCraft.MODID, "es_quick_sync")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, @NotNull EtherStreamQuickCreateWithSyncDataS2C> CODEC = new StreamCodec<>() {
        @Override
        public EtherStreamQuickCreateWithSyncDataS2C decode(RegistryFriendlyByteBuf buf) {
            AutoIndexPosDir posDir = AutoIndexPosDir.STREAM_CODEC.decode(buf);
            int mask = buf.readByte() & 0xFF;
            Optional<Integer> tickCount = (mask & 0x01) != 0
                    ? Optional.of((mask >> 1) & 0x3F)
                    : Optional.empty();
            Optional<Integer> ether = (mask & 0x80) != 0
                    ? Optional.of(ByteBufCodecs.VAR_INT.decode(buf))
                    : Optional.empty();
            List<IEtherStreamSyncedData> syncedData = ByteBufCodecs
                    .collection(ArrayList::new, SyncedEtherStreamDataManager.STREAM_CODEC)
                    .decode(buf);
            return new EtherStreamQuickCreateWithSyncDataS2C(posDir, tickCount, ether, syncedData);
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buf, EtherStreamQuickCreateWithSyncDataS2C value) {
            AutoIndexPosDir.STREAM_CODEC.encode(buf, value.posDir());
            byte mask = 0;
            if (value.tickCount().isPresent())
                mask |= (byte) (0x01 | ((value.tickCount().get() & 0x3F) << 1));
            if (value.ether().isPresent()) mask |= (byte) 0x80;
            buf.writeByte(mask);
            value.ether().ifPresent(v -> ByteBufCodecs.VAR_INT.encode(buf, v));
            ByteBufCodecs
                    .collection(ArrayList::new, SyncedEtherStreamDataManager.STREAM_CODEC)
                    .encode(buf, new ArrayList<>(value.syncedData()));
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            PosDir resolved = posDir.resolve(ctx.player().level().getData(AttachmentDataRegistry.REVERSE_INDEX_MAPPING_MANAGER));
            if (resolved == null) return;
            ClientVESHDataGetter.get().handleQuickCreate(resolved, this);
        });
    }
}
