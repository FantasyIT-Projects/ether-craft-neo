package studio.fantasyit.ether_craft.stream.vholder;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;

public record VirtualEtherStreamHolderData(
        int nextId,
        List<VirtualEtherStreamData> streams
) {
    public static final Codec<VirtualEtherStreamHolderData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("nextId").forGetter(VirtualEtherStreamHolderData::nextId),
            VirtualEtherStreamData.CODEC.listOf().fieldOf("streams").forGetter(VirtualEtherStreamHolderData::streams)
    ).apply(instance, VirtualEtherStreamHolderData::new));
}
