package studio.fantasyit.ether_craft.integration.jei;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.level.ItemLike;
import studio.fantasyit.ether_craft.node.NodePluginManager;

public record NodePluginInfoRecipe(
        NodePluginManager.PluginType pluginType,
        Identifier pluginId,
        ItemStackTemplate icon
) {
    public static final Codec<NodePluginInfoRecipe> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            NodePluginManager.PluginType.CODEC.fieldOf("type").forGetter(NodePluginInfoRecipe::pluginType),
            Identifier.CODEC.fieldOf("plugin").forGetter(NodePluginInfoRecipe::pluginId)
    ).apply(instance, (type, pluginId) -> {
        ItemStackTemplate icon = null;
        for (var info : NodePluginManager.ALL_PLUGINS) {
            if (info.id().equals(pluginId)) {
                icon = info.icon();
                break;
            }
        }
        return new NodePluginInfoRecipe(type, pluginId, icon);
    }));

    public static NodePluginInfoRecipe fromPluginInfo(NodePluginManager.PluginInfo info) {
        return new NodePluginInfoRecipe(info.type(), info.id(), info.icon());
    }
}

