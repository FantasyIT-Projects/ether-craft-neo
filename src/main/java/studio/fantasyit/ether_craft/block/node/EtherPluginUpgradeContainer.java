package studio.fantasyit.ether_craft.block.node;

import net.minecraft.resources.Identifier;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.node.AbstractNodePlugin;
import studio.fantasyit.ether_craft.node.NodePluginManager;
import studio.fantasyit.ether_craft.node.NodeProperty;
import studio.fantasyit.ether_craft.node.plugins.InstalledPlugin;

import java.util.function.Predicate;
import java.util.stream.Stream;

public class EtherPluginUpgradeContainer extends SimpleContainer {
    private final Identifier[] pluginId;
    private final AbstractNodePlugin[] plugin;
    private final Predicate<NodePluginManager.PluginType> type;
    private final EtherAdaptNodeEntity entity;

    public EtherPluginUpgradeContainer(int size, Predicate<NodePluginManager.PluginType> typePredicate, EtherAdaptNodeEntity etherAdaptNodeEntity) {
        super(size);
        pluginId = new Identifier[size];
        plugin = new AbstractNodePlugin[size];
        this.type = typePredicate;
        this.entity = etherAdaptNodeEntity;
    }

    public boolean hasPlugin(int index) {
        return pluginId[index] != null && plugin[index] != null;
    }

    public @Nullable AbstractNodePlugin getPlugin(int index) {
        return plugin[index];
    }

    public @Nullable Identifier getPluginId(int index) {
        return pluginId[index];
    }

    @Override
    public void setChanged() {
        super.setChanged();
        for (int i = 0; i < plugin.length; i++) {
            if (!NodePluginManager.Instance.matches(this.type, getItem(i), pluginId[i])) {
                if (plugin[i] != null)
                    plugin[i].onDestroy();
                plugin[i] = null;
                pluginId[i] = NodePluginManager.Instance.getMatchingPluginId(this.type, getItem(i));
                if (pluginId[i] != null) {
                    NodePluginManager.PluginInfo info = NodePluginManager.Instance.getInfoFor(getItem(i), this.type);
                    if (info != null)
                        plugin[i] = NodePluginManager.Instance.get(pluginId[i], this.entity, new InstalledPlugin(info.type(), i, pluginId[i]));
                }
            }
        }
        this.entity.pluginUpdate();
    }

    public static Identifier ID_NULL = EtherCraft.id("null");

    public void saveAddition(ValueOutput output) {
        output.store("items", ItemStack.OPTIONAL_CODEC.listOf(), getItems());
        output.store("plugins", Identifier.CODEC.listOf(), Stream.of(pluginId).map(id -> id == null ? ID_NULL : id).toList());
        for (int i = 0; i < pluginId.length; i++) {
            if (pluginId[i] != null)
                plugin[i].saveAdditional(output.child(String.format("plugin-%d", i)));
        }
    }

    public void loadAddition(ValueInput input) {
        input.read("items", ItemStack.OPTIONAL_CODEC.listOf()).ifPresent(l -> {
            for (int i = 0; i < l.size() && i < getContainerSize(); i++) {
                setItem(i, l.get(i), true);
            }
        });
        input.read("plugins", Identifier.CODEC.listOf()).ifPresent(l -> {
            for (int i = 0; i < l.size() && i < getContainerSize(); i++) {
                pluginId[i] = l.get(i);
                if (pluginId[i].equals(ID_NULL)) {
                    plugin[i] = null;
                }
                if (pluginId[i] != null) {
                    NodePluginManager.PluginInfo info = NodePluginManager.Instance.getInfoFor(getItem(i), this.type);
                    if (info != null)
                        plugin[i] = NodePluginManager.Instance.get(pluginId[i], this.entity, new InstalledPlugin(info.type(), i, pluginId[i]));
                }
            }
        });
        for (int i = 0; i < pluginId.length; i++) {
            if (pluginId[i] != null && plugin[i] != null)
                plugin[i].loadAdditional(input.childOrEmpty(String.format("plugin-%d", i)));
        }
        setChanged();
    }

    public void tick() {
        for (int i = 0; i < plugin.length; i++) {
            AbstractNodePlugin AbstractNodePlugin = plugin[i];
            if (AbstractNodePlugin != null) {
                AbstractNodePlugin.tick();
            }
        }
    }

    public void modifyNodeProperty(NodeProperty nodeProperty) {
        for (AbstractNodePlugin AbstractNodePlugin : plugin) {
            if (AbstractNodePlugin != null)
                AbstractNodePlugin.modifyNodeProperty(nodeProperty);
        }
    }
}