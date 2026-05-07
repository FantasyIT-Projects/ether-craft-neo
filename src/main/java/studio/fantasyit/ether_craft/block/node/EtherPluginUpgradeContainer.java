package studio.fantasyit.ether_craft.block.node;

import net.minecraft.resources.Identifier;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;
import studio.fantasyit.ether_craft.node.AbstractNodePlugin;
import studio.fantasyit.ether_craft.node.NodePluginManager;
import studio.fantasyit.ether_craft.node.NodeProperty;

import java.util.List;
import java.util.function.Predicate;

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
    @Override
    public void setChanged() {
        super.setChanged();
        for (int i = 0; i < plugin.length; i++) {
            if (!NodePluginManager.Instance.matches(this.type, getItem(i), pluginId[i])) {
                pluginId[i] = NodePluginManager.Instance.getMatchingPluginId(this.type, getItem(i));
                if (pluginId[i] != null)
                    plugin[i] = NodePluginManager.Instance.get(pluginId[i], this.entity);
            }
        }
        this.entity.setChanged();
    }

    public void saveAddition(ValueOutput output) {
        output.store("items", ItemStack.OPTIONAL_CODEC.listOf(), getItems());
        output.store("plugins", Identifier.CODEC.listOf(), List.of(pluginId));
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
                if (pluginId[i] != null)
                    plugin[i] = NodePluginManager.Instance.get(pluginId[i], this.entity);
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