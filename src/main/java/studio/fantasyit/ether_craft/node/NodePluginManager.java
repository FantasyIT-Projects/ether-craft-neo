package studio.fantasyit.ether_craft.node;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import org.jetbrains.annotations.Nullable;
import studio.fantasyit.ether_craft.block.node.EtherAdaptNodeEntity;
import studio.fantasyit.ether_craft.node.plugins.InstalledPlugin;
import studio.fantasyit.ether_craft.node.plugins.MainPageDummyPlugin;
import studio.fantasyit.ether_craft.node.plugins.feature.FeatureContainerInteract;
import studio.fantasyit.ether_craft.node.plugins.feature.FeatureDropperThrower;
import studio.fantasyit.ether_craft.node.plugins.feature.FeatureEtherStreamEmitter;
import studio.fantasyit.ether_craft.node.plugins.function.FunctionFurnaceGenerator;
import studio.fantasyit.ether_craft.node.plugins.function.FunctionMagnet;
import studio.fantasyit.ether_craft.node.plugins.upgrade.EtherStreamBreakBlockUpgrade;
import studio.fantasyit.ether_craft.node.plugins.upgrade.EtherStreamStorageUpgrade;
import studio.fantasyit.ether_craft.node.plugins.upgrade.EtherStreamPreventDecayUpgrade;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
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
    public final static PluginInfo MAIN_PAGE_INFO = new PluginInfo(PluginType.UPGRADE, MainPageDummyPlugin.ID, MainPageDummyPlugin::new, _ -> false, Items.BARRIER);

    public record PluginInfo(PluginType type, Identifier id,
                             BiFunction<EtherAdaptNodeEntity, InstalledPlugin, AbstractNodePlugin> constructor,
                             Predicate<ItemStack> predicate,
                             ItemLike icon
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
        registerPlugin(PluginType.FUNCTION, FunctionMagnet.ID, FunctionMagnet::new, Items.IRON_BLOCK);
        registerPlugin(PluginType.FEATURE, FeatureEtherStreamEmitter.ID, FeatureEtherStreamEmitter::new, Items.DISPENSER);
        registerPlugin(PluginType.FEATURE, FeatureDropperThrower.ID, FeatureDropperThrower::new, Items.DROPPER);
        registerPlugin(PluginType.FEATURE, FeatureContainerInteract.ID, FeatureContainerInteract::new, Items.HOPPER);
        registerPlugin(PluginType.UPGRADE, EtherStreamStorageUpgrade.ID, EtherStreamStorageUpgrade::new, Items.CHEST);
        registerPlugin(PluginType.UPGRADE, EtherStreamPreventDecayUpgrade.ID, EtherStreamPreventDecayUpgrade::new, Items.AMETHYST_SHARD);
        registerPlugin(PluginType.UPGRADE, EtherStreamBreakBlockUpgrade.ID, EtherStreamBreakBlockUpgrade::new, Items.IRON_PICKAXE);
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

    public @Nullable AbstractNodePlugin get(Identifier id, EtherAdaptNodeEntity nodeEntity, InstalledPlugin installedPlugin) {
        for (PluginInfo info : plugins) {
            if (info.id().equals(id)) {
                return info.constructor().apply(nodeEntity, installedPlugin);
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

    public void registerPlugin(PluginType type, Identifier id, BiFunction<EtherAdaptNodeEntity, InstalledPlugin, AbstractNodePlugin> plugin, ItemLike item) {
        registerPlugin(type, id, plugin, t -> t.is(item.asItem()), item);
    }

    public void registerPlugin(PluginType type, Identifier id, BiFunction<EtherAdaptNodeEntity, InstalledPlugin, AbstractNodePlugin> plugin, Predicate<ItemStack> predicate, ItemLike icon) {
        plugins.add(new PluginInfo(type, id, plugin, predicate, icon));
    }
}
