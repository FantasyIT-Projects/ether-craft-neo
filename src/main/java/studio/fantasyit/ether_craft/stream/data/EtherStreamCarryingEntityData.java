package studio.fantasyit.ether_craft.stream.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.Identifier;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.stream.PosDir;

import java.util.UUID;

public record EtherStreamCarryingEntityData(UUID entityUUID, int entityId, PosDir posDir, int streamId) implements IEtherStreamSyncedData {
    public static final Identifier ID = EtherCraft.id("carrying_entity");

    public static final MapCodec<EtherStreamCarryingEntityData> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            UUIDUtil.CODEC.fieldOf("entityUUID").forGetter(EtherStreamCarryingEntityData::entityUUID),
            Codec.INT.fieldOf("entityId").forGetter(EtherStreamCarryingEntityData::entityId),
            PosDir.CODEC.fieldOf("posDir").forGetter(EtherStreamCarryingEntityData::posDir),
            Codec.INT.fieldOf("streamId").forGetter(EtherStreamCarryingEntityData::streamId)
    ).apply(instance, EtherStreamCarryingEntityData::new));

    @Override
    public Identifier getId() {
        return ID;
    }

    @Override
    public void toBuffer(FriendlyByteBuf writer) {
        writer.writeUUID(entityUUID);
        writer.writeInt(entityId);
        PosDir.STREAM_CODEC.encode((RegistryFriendlyByteBuf) writer, posDir);
        writer.writeInt(streamId);
    }

    public static EtherStreamCarryingEntityData fromBuffer(FriendlyByteBuf reader) {
        return new EtherStreamCarryingEntityData(reader.readUUID(), reader.readInt(),
                PosDir.STREAM_CODEC.decode((RegistryFriendlyByteBuf) reader), reader.readInt());
    }
}
