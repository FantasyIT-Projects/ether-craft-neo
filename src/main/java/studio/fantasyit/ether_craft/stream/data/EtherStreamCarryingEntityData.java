package studio.fantasyit.ether_craft.stream.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;
import studio.fantasyit.ether_craft.EtherCraft;

import java.util.UUID;

public record EtherStreamCarryingEntityData(UUID uuid, int entityId) implements IEtherStreamSyncedData {
    public static final Identifier ID = EtherCraft.id("carrying_entity");

    public static final Codec<EtherStreamCarryingEntityData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            UUIDUtil.CODEC.fieldOf("uuid").forGetter(EtherStreamCarryingEntityData::uuid),
            Codec.INT.fieldOf("entityId").forGetter(EtherStreamCarryingEntityData::entityId)
    ).apply(instance, EtherStreamCarryingEntityData::new));

    @Override
    public Identifier getId() {
        return ID;
    }

    @Override
    public void toBuffer(FriendlyByteBuf writer) {
        writer.writeUUID(uuid);
        writer.writeInt(entityId);
    }

    public static EtherStreamCarryingEntityData fromBuffer(FriendlyByteBuf reader) {
        return new EtherStreamCarryingEntityData(reader.readUUID(), reader.readInt());
    }
}
