package studio.fantasyit.ether_craft.stream.vholder;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import studio.fantasyit.ether_craft.stream.EtherConsumer;
import studio.fantasyit.ether_craft.stream.cap.IStreamCapability;
import studio.fantasyit.ether_craft.stream.CapabilityFactoryManager;

import java.util.List;
import java.util.Optional;

public record VirtualEtherStreamData(
        int streamId,
        Vec3 pos,
        Vec3 startPos,
        Vec3 motion,
        Direction direction,
        int ether,
        int tickCount,
        @Nullable Component label,
        int labelColor,
        EtherConsumer.State consumerState,
        List<IStreamCapability> capabilities
) {
    public static final Codec<VirtualEtherStreamData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("streamId").forGetter(VirtualEtherStreamData::streamId),
            Vec3.CODEC.fieldOf("pos").forGetter(VirtualEtherStreamData::pos),
            Vec3.CODEC.fieldOf("startPos").forGetter(VirtualEtherStreamData::startPos),
            Vec3.CODEC.fieldOf("motion").forGetter(VirtualEtherStreamData::motion),
            Direction.CODEC.fieldOf("direction").forGetter(VirtualEtherStreamData::direction),
            Codec.INT.fieldOf("ether").forGetter(VirtualEtherStreamData::ether),
            Codec.INT.fieldOf("tickCount").forGetter(VirtualEtherStreamData::tickCount),
            ComponentSerialization.CODEC.optionalFieldOf("label").forGetter(d -> Optional.ofNullable(d.label)),
            Codec.INT.fieldOf("labelColor").forGetter(VirtualEtherStreamData::labelColor),
            EtherConsumer.State.CODEC.fieldOf("consumerState").forGetter(VirtualEtherStreamData::consumerState),
            CapabilityFactoryManager.CODEC.listOf().fieldOf("capabilities").forGetter(VirtualEtherStreamData::capabilities)
    ).apply(instance, (streamId, pos, startPos, motion, direction, ether, tickCount, label, labelColor, consumerState, capabilities) ->
            new VirtualEtherStreamData(streamId, pos, startPos, motion, direction, ether, tickCount, label.orElse(null), labelColor, consumerState, capabilities)
    ));
}
