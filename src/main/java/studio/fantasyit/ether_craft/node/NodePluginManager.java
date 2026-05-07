package studio.fantasyit.ether_craft.node;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import org.jetbrains.annotations.Nullable;
import studio.fantasyit.ether_craft.block.node.EtherAdaptNodeEntity;
import studio.fantasyit.ether_craft.node.plugins.InstalledPlugin;
import studio.fantasyit.ether_craft.node.plugins.MainPageDummyPlugin;
import studio.fantasyit.ether_craft.node.plugins.function.FunctionFurnaceGenerator;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

public class NodePluginManager {

    public enum PluginType {
        FUNCTION,
        FEATURE,
        UPGRADE,
        DUMMY
    }

    public final static Predicate<PluginType> FUNCTION_TYPE = t -> t == PluginType.FUNCTION;
    public final static Predicate<PluginType> FEATURE_UPGRADE_TYPE = t -> t == PluginType.FEATURE || t == PluginType.UPGRADE;

    public final static InstalledPlugin MAIN_PAGE = new InstalledPlugin(PluginType.DUMMY, 0, MainPageDummyPlugin.ID);
    public final static PluginInfo MAIN_PAGE_INFO = new PluginInfo(PluginType.UPGRADE, MainPageDummyPlugin.ID, MainPageDummyPlugin::new, _ -> false, Items.BARRIER.getDefaultInstance());

    public record PluginInfo(PluginType type, Identifier id,
                             Function<EtherAdaptNodeEntity, AbstractNodePlugin> constructor,
                             Predicate<ItemStack> predicate,
                             ItemStack icon
    ) {
    }

    public static NodePluginManager Instance = new NodePluginManager();
    public List<PluginInfo> plugins;

    public NodePluginManager() {
        plugins = new ArrayList<>();
    }

    public void collect() {
        plugins.clear();
        plugins.add(MAIN_PAGE_INFO);
        //TODO
        registerPlugin(PluginType.FUNCTION, FunctionFurnaceGenerator.ID, FunctionFurnaceGenerator::new, Items.FURNACE);
    }

    public boolean matches(Predicate<NodePluginManager.PluginType> type, ItemStack itemStack, @Nullable Identifier identifier) {
        if (identifier == null)
            return itemStack.isEmpty();
        for (PluginInfo info : plugins) {
            if (info.predicate().test(itemStack) && type.test(info.type()))
                return info.id().equals(identifier);
        }
        return false;
    }

    public @Nullable Identifier getMatchingPluginId(Predicate<NodePluginManager.PluginType> type, ItemStack itemStack) {
        for (PluginInfo info : plugins) {
            if (info.predicate().test(itemStack) && type.test(info.type())) {
                return info.id();
            }
        }
        return null;
    }

    public @Nullable AbstractNodePlugin get(Identifier id, EtherAdaptNodeEntity nodeEntity) {
        for (PluginInfo info : plugins) {
            if (info.id().equals(id)) {
                return info.constructor().apply(nodeEntity);
            }
        }
        return null;
    }

    public PluginInfo getInfoFor(ItemStack stack, Predicate<PluginType> featureUpgradeType) {
        for (PluginInfo info : plugins) {
            if (info.predicate().test(stack) && featureUpgradeType.test(info.type())) {
                return info;
            }
        }
        return null;
    }

    public void registerPlugin(PluginType type, Identifier id, Function<EtherAdaptNodeEntity, AbstractNodePlugin> plugin, ItemLike item) {
        registerPlugin(type, id, plugin, item.asItem().getDefaultInstance());
    }

    public void registerPlugin(PluginType type, Identifier id, Function<EtherAdaptNodeEntity, AbstractNodePlugin> plugin, ItemStack item) {
        registerPlugin(type, id, plugin, t -> ItemStack.isSameItem(item, t), item);
    }

    public void registerPlugin(PluginType type, Identifier id, Function<EtherAdaptNodeEntity, AbstractNodePlugin> plugin, Predicate<ItemStack> predicate, ItemStack icon) {
        plugins.add(new PluginInfo(type, id, plugin, predicate, icon));
    }
}
