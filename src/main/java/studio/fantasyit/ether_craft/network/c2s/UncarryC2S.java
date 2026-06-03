package studio.fantasyit.ether_craft.network.c2s;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.register.AttachmentDataRegistry;
import studio.fantasyit.ether_craft.stream.PosDir;
import studio.fantasyit.ether_craft.stream.cap.EtherStreamCarryEntityCapability;
import studio.fantasyit.ether_craft.stream.data.EtherStreamCarryingEntityData;
import studio.fantasyit.ether_craft.stream.data.IEtherStreamSyncedData;
import studio.fantasyit.ether_craft.stream.vholder.VirtualEtherStream;
import studio.fantasyit.ether_craft.stream.vholder.VirtualEtherStreamHolder;
import studio.fantasyit.ether_craft.stream.vholder.VirtualEtherStreamHolderManager;

import java.util.Optional;

public record UncarryC2S(PosDir posDir, int streamId) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<@NotNull UncarryC2S> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(EtherCraft.MODID, "uncarry"));

    public static final StreamCodec<RegistryFriendlyByteBuf, @NotNull UncarryC2S> CODEC = StreamCodec.composite(
            PosDir.STREAM_CODEC, UncarryC2S::posDir,
            ByteBufCodecs.VAR_INT, UncarryC2S::streamId,
            UncarryC2S::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(UncarryC2S msg, Player player) {
        if (!(player.level() instanceof ServerLevel level)) return;
        VirtualEtherStreamHolderManager mgr = VirtualEtherStreamHolderManager.get(level);
        VirtualEtherStreamHolder holder = mgr.getHolder(msg.posDir());
        if (holder == null) return;
        VirtualEtherStream ves = holder.findStreamById(msg.streamId());
        if (ves == null) return;

        IEtherStreamSyncedData synced = ves.getSyncedData(EtherStreamCarryingEntityData.ID);
        if (!(synced instanceof EtherStreamCarryingEntityData data)) return;
        if (!data.entityUUID().equals(player.getUUID())) return;

        ves.clearSyncedData(EtherStreamCarryingEntityData.ID);
        player.noPhysics = false;
        EtherStreamCarryEntityCapability.dropEntityTo(level, ves.position(), ves.deltaMovement(), player);
        player.setData(AttachmentDataRegistry.CARRY_COOLDOWN.get(), level.getGameTime());
        player.setData(AttachmentDataRegistry.CARRY_COOLDOWN_SOURCE.get(), Optional.empty());
    }
}
