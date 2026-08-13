package studio.fantasyit.ether_craft.network.s2c;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.register.AttachmentDataRegistry;
import studio.fantasyit.ether_craft.stream.PosDir;
import studio.fantasyit.ether_craft.stream.idx.ReverseIndexMappingManager;

import java.util.ArrayList;
import java.util.List;

public record IndexMappingSyncS2C(
        List<Entry> entries,
        List<Integer> removals
) implements CustomPacketPayload {

    public record Entry(int id, PosDir posDir) {
        public static final StreamCodec<RegistryFriendlyByteBuf, Entry> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Entry::id,
                PosDir.STREAM_CODEC, Entry::posDir,
                Entry::new
        );
    }

    public static final Type<@NotNull IndexMappingSyncS2C> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(EtherCraft.MODID, "index_mapping_sync")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, @NotNull IndexMappingSyncS2C> CODEC = StreamCodec.composite(
            ByteBufCodecs.collection(ArrayList::new, Entry.CODEC), IndexMappingSyncS2C::entries,
            ByteBufCodecs.collection(ArrayList::new, ByteBufCodecs.VAR_INT), IndexMappingSyncS2C::removals,
            IndexMappingSyncS2C::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ReverseIndexMappingManager reverse = ctx.player().level().getData(AttachmentDataRegistry.REVERSE_INDEX_MAPPING_MANAGER);
            // 先删后加：同一包内先移除旧绑定，再写入新绑定，杜绝 id 复用导致的错配
            for (int id : removals) {
                reverse.id2PosDir.remove(id);
            }
            for (Entry entry : entries) {
                reverse.id2PosDir.put(entry.id(), entry.posDir());
            }
        });
    }
}
