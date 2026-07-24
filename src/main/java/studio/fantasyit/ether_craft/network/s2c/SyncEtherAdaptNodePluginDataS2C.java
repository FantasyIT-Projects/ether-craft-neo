package studio.fantasyit.ether_craft.network.s2c;

import net.minecraft.core.BlockPos;
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

public record SyncEtherAdaptNodePluginDataS2C(
        InstalledPlugin plugin,
        Identifier key,
        Integer pluginValue,
        BlockPos pos
) implements CustomPacketPayload {
    public static final Type<@NotNull SyncEtherAdaptNodePluginDataS2C> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(
                    EtherCraft.MODID, "ean_plugin_data"
            )
    );


    public static final StreamCodec<RegistryFriendlyByteBuf, @NotNull SyncEtherAdaptNodePluginDataS2C> CODEC = StreamCodec.composite(
            InstalledPlugin.STREAM_CODEC,
            SyncEtherAdaptNodePluginDataS2C::plugin,
            Identifier.STREAM_CODEC,
            SyncEtherAdaptNodePluginDataS2C::key,
            ByteBufCodecs.VAR_INT,
            SyncEtherAdaptNodePluginDataS2C::pluginValue,
            BlockPos.STREAM_CODEC,
            SyncEtherAdaptNodePluginDataS2C::pos,
            SyncEtherAdaptNodePluginDataS2C::new
    );

    @Override
    public @NotNull Type<@NotNull SyncEtherAdaptNodePluginDataS2C> type() {
        return TYPE;
    }

    public void handle(IPayloadContext iPayloadContext) {
        iPayloadContext.enqueueWork(() -> {
            Level level = iPayloadContext.player().level();
            if (level.getBlockEntity(pos) instanceof EtherAdaptNodeEntity nodeEntity) {
                nodeEntity.setSyncedPluginDataNoSync(plugin, key, pluginValue);
            }
        });
    }
}
