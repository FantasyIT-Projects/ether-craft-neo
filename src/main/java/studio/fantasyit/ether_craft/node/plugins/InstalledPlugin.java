package studio.fantasyit.ether_craft.node.plugins;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.fantasyit.ether_craft.node.NodePluginManager;

public record InstalledPlugin(NodePluginManager.PluginType type, int id, Identifier pluginId) {
    public static final StreamCodec<RegistryFriendlyByteBuf, @NotNull InstalledPlugin> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8.map(NodePluginManager.PluginType::valueOf, NodePluginManager.PluginType::name),
            InstalledPlugin::type,
            ByteBufCodecs.INT,
            InstalledPlugin::id,
            Identifier.STREAM_CODEC,
            InstalledPlugin::pluginId,
            InstalledPlugin::new
    );

    public static @Nullable InstalledPlugin readNullable(RegistryFriendlyByteBuf data) {
        boolean hasPlugin = data.readBoolean();
        if (!hasPlugin) return null;
        return new InstalledPlugin(NodePluginManager.PluginType.values()[data.readInt()], data.readInt(), data.readIdentifier());
    }

    public static void writeNullable(InstalledPlugin plugin, RegistryFriendlyByteBuf data) {
        if (plugin != null) {
            plugin.writeToNetwork(data);
            return;
        }
        data.writeBoolean(false);
    }

    public void writeToNetwork(RegistryFriendlyByteBuf data) {
        data.writeBoolean(true);
        data.writeInt(type.ordinal());
        data.writeInt(id);
        data.writeIdentifier(pluginId);
    }
}
