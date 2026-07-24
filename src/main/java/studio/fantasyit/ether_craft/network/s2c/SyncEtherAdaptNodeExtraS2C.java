package studio.fantasyit.ether_craft.network.s2c;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.block.node.EtherAdaptNodeEntity;
import studio.fantasyit.ether_craft.node.plugins.InstalledPlugin;
import studio.fantasyit.ether_craft.util.SerializeUtil;

import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;

public record SyncEtherAdaptNodeExtraS2C(
        Optional<InstalledPlugin> functionPlugin,
        Map<Direction, InstalledPlugin> pluginDirection,
        Map<InstalledPlugin, Map<Identifier, Integer>> pluginValue,
        BlockPos pos,
        Identifier levelId,
        int maxEther,
        int slotUnlock
) implements CustomPacketPayload {
    public static final Type<@NotNull SyncEtherAdaptNodeExtraS2C> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(
                    EtherCraft.MODID, "ean_extra"
            )
    );



    public static final StreamCodec<RegistryFriendlyByteBuf, @NotNull SyncEtherAdaptNodeExtraS2C> CODEC = StreamCodec.composite(
            ByteBufCodecs.optional(InstalledPlugin.STREAM_CODEC),
            SyncEtherAdaptNodeExtraS2C::functionPlugin,
            ByteBufCodecs.collection(ArrayList::new, SerializeUtil.PDMap.STREAM_CODEC).map(SerializeUtil.PDMap::toMap, SerializeUtil.PDMap::fromMap),
            SyncEtherAdaptNodeExtraS2C::pluginDirection,
            ByteBufCodecs.collection(ArrayList::new, SerializeUtil.PIMap.STREAM_CODEC).map(SerializeUtil.PIMap::toMap, SerializeUtil.PIMap::fromMap),
            SyncEtherAdaptNodeExtraS2C::pluginValue,
            BlockPos.STREAM_CODEC,
            SyncEtherAdaptNodeExtraS2C::pos,
            Identifier.STREAM_CODEC,
            SyncEtherAdaptNodeExtraS2C::levelId,
            ByteBufCodecs.INT,
            SyncEtherAdaptNodeExtraS2C::maxEther,
            ByteBufCodecs.INT,
            SyncEtherAdaptNodeExtraS2C::slotUnlock,
            SyncEtherAdaptNodeExtraS2C::new
    );

    @Override
    public @NotNull Type<@NotNull SyncEtherAdaptNodeExtraS2C> type() {
        return TYPE;
    }

    public void handle(IPayloadContext iPayloadContext) {
        iPayloadContext.enqueueWork(() -> {
            Level level = iPayloadContext.player().level();
            if (level.dimension().identifier().equals(levelId)) {
                if (level.getBlockEntity(pos) instanceof EtherAdaptNodeEntity nodeEntity) {
                    nodeEntity.fromNetwork(pluginDirection, functionPlugin.orElse(null), pluginValue, maxEther, slotUnlock);
                }
            }
        });

    }
}
