package studio.fantasyit.ether_craft.stream.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.Identifier;
import studio.fantasyit.ether_craft.EtherCraft;

public class EtherStreamLabelData implements IEtherStreamSyncedData {
    public static final Identifier ID = EtherCraft.id("label");

    public static final MapCodec<EtherStreamLabelData> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            ComponentSerialization.CODEC.fieldOf("label").forGetter(t -> t.label),
            Codec.INT.fieldOf("labelColor").forGetter(t -> t.labelColor)
    ).apply(instance, EtherStreamLabelData::new));

    public Component label;
    public int labelColor;

    public EtherStreamLabelData(Component label, int labelColor) {
        this.label = label;
        this.labelColor = labelColor;
    }

    @Override
    public Identifier getId() {
        return ID;
    }

    @Override
    public void toBuffer(FriendlyByteBuf writer) {
        writer.writeJsonWithCodec(ComponentSerialization.CODEC, label);
        writer.writeInt(labelColor);
    }

    public static EtherStreamLabelData fromBuffer(FriendlyByteBuf reader) {
        return new EtherStreamLabelData(reader.readLenientJsonWithCodec(ComponentSerialization.CODEC), reader.readInt());
    }
}
