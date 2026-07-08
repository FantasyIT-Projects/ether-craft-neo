package studio.fantasyit.ether_craft.stream.vholder;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.phys.Vec3;
import studio.fantasyit.ether_craft.stream.CapabilityFactoryManager;
import studio.fantasyit.ether_craft.stream.EtherConsumer;
import studio.fantasyit.ether_craft.stream.PosDir;
import studio.fantasyit.ether_craft.stream.cap.IStreamCapability;
import studio.fantasyit.ether_craft.stream.data.IEtherStreamSyncedData;
import studio.fantasyit.ether_craft.stream.data.SyncedEtherStreamDataManager;

import java.util.List;

public record VirtualEtherStreamData(
        int streamId,
        Vec3 pos,
        float startOffset,
        float startSpeed,
        PosDir posDir,
        int ether,
        int tickCount,
        EtherConsumer.State consumerState,
        List<IStreamCapability> capabilities,
        List<IEtherStreamSyncedData> toSyncData
) {
    public static final Codec<VirtualEtherStreamData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("streamId").forGetter(VirtualEtherStreamData::streamId),
            Vec3.CODEC.fieldOf("pos").forGetter(VirtualEtherStreamData::pos),
            Codec.FLOAT.fieldOf("startOffset").forGetter(VirtualEtherStreamData::startOffset),
            Codec.FLOAT.fieldOf("startSpeed").forGetter(VirtualEtherStreamData::startSpeed),
            PosDir.CODEC.fieldOf("posDir").forGetter(VirtualEtherStreamData::posDir),
            Codec.INT.fieldOf("ether").forGetter(VirtualEtherStreamData::ether),
            Codec.INT.fieldOf("tickCount").forGetter(VirtualEtherStreamData::tickCount),
            EtherConsumer.State.CODEC.fieldOf("consumerState").forGetter(VirtualEtherStreamData::consumerState),
            CapabilityFactoryManager.CODEC.listOf().fieldOf("capabilities").forGetter(VirtualEtherStreamData::capabilities),
            SyncedEtherStreamDataManager.CODEC.listOf().fieldOf("toSyncData").forGetter(VirtualEtherStreamData::toSyncData)
    ).apply(instance, VirtualEtherStreamData::new));
}
