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

import java.util.ArrayList;
import java.util.Map;

public record SyncEtherAdaptNodeExtraS2C(
        Map<Direction, InstalledPlugin> pluginDirection,
        BlockPos pos,
        Identifier levelId
) implements CustomPacketPayload {
    public static final Type<@NotNull SyncEtherAdaptNodeExtraS2C> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(
                    EtherCraft.MODID, "sync_ether_adapt_node_extra_value"
            )
    );

    record PDMap(InstalledPlugin plugin, Direction direction) {
        public static final StreamCodec<RegistryFriendlyByteBuf, @NotNull PDMap> CODEC = StreamCodec.composite(
                InstalledPlugin.STREAM_CODEC,
                PDMap::plugin,
                Direction.STREAM_CODEC,
                PDMap::direction,
                PDMap::new
        );

        public static ArrayList<PDMap> fromMap(Map<Direction, InstalledPlugin> map) {
            return new ArrayList<>(map.entrySet().stream().map(entry -> new PDMap(entry.getValue(), entry.getKey())).toList());
        }

        public static Map<Direction, InstalledPlugin> toMap(ArrayList<PDMap> list) {
            return list.stream().collect(java.util.stream.Collectors.toMap(PDMap::direction, PDMap::plugin));
        }
    }

    public static final StreamCodec<RegistryFriendlyByteBuf, @NotNull SyncEtherAdaptNodeExtraS2C> CODEC = StreamCodec.composite(
            ByteBufCodecs.collection(ArrayList::new, PDMap.CODEC).map(PDMap::toMap, PDMap::fromMap),
            SyncEtherAdaptNodeExtraS2C::pluginDirection,
            BlockPos.STREAM_CODEC,
            SyncEtherAdaptNodeExtraS2C::pos,
            Identifier.STREAM_CODEC,
            SyncEtherAdaptNodeExtraS2C::levelId,
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
                    nodeEntity.fromNetwork(pluginDirection);
                }
            }
        });

    }
}
