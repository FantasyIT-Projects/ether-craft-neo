package studio.fantasyit.ether_craft.node;

import net.minecraft.resources.Identifier;
import studio.fantasyit.ether_craft.menu.node.EtherAdaptNodeScreen;
import studio.fantasyit.ether_craft.node.plugins.MainPageDummyPlugin;
import studio.fantasyit.ether_craft.node.plugins.feature.FeatureContainerInteract;
import studio.fantasyit.ether_craft.node.plugins.feature.FeatureDropperThrower;
import studio.fantasyit.ether_craft.node.plugins.feature.FeatureEtherStreamEmitter;
import studio.fantasyit.ether_craft.node.plugins.function.FunctionFurnaceGenerator;
import studio.fantasyit.ether_craft.node.plugins.function.FunctionMagnet;
import studio.fantasyit.ether_craft.node.plugins.function.FunctionNodeProcess;
import studio.fantasyit.ether_craft.node.tabs.BaseEtherNodeTabWidgetProvider;
import studio.fantasyit.ether_craft.node.tabs.MainPageProvider;
import studio.fantasyit.ether_craft.node.tabs.feature.ContainerInteractScreen;
import studio.fantasyit.ether_craft.node.tabs.feature.DirectionalFilterScreen;
import studio.fantasyit.ether_craft.node.tabs.function.FunctionNodeProcessScreen;
import studio.fantasyit.ether_craft.node.tabs.function.ItemConsumeScreen;
import studio.fantasyit.ether_craft.node.tabs.function.MagnetFunctionScreen;

import java.util.HashMap;
import java.util.function.BiFunction;

public class EtherAdaptNodeUpgradeTabManager {
    public static EtherAdaptNodeUpgradeTabManager instance = new EtherAdaptNodeUpgradeTabManager();
    private HashMap<Identifier, BiFunction<AbstractNodePlugin, EtherAdaptNodeScreen, BaseEtherNodeTabWidgetProvider<?>>> widgets = new HashMap<>();

    public void collect() {
        widgets.clear();
        register(MainPageDummyPlugin.ID, wrap(MainPageProvider::new));
        register(FunctionFurnaceGenerator.ID, wrap(ItemConsumeScreen::new));
        register(FunctionMagnet.ID, wrap(MagnetFunctionScreen::new));
        register(FunctionNodeProcess.ID, wrap(FunctionNodeProcessScreen::new));
        register(FeatureEtherStreamEmitter.ID, wrap(DirectionalFilterScreen::new));
        register(FeatureDropperThrower.ID, wrap(DirectionalFilterScreen::new));
        register(FeatureContainerInteract.ID, wrap(ContainerInteractScreen::new));
    }

    public <T extends AbstractNodePlugin> BiFunction<AbstractNodePlugin, EtherAdaptNodeScreen, BaseEtherNodeTabWidgetProvider<?>> wrap(BiFunction<T, EtherAdaptNodeScreen, BaseEtherNodeTabWidgetProvider<T>> construct) {
        return (BiFunction) construct;
    }

    public void register(Identifier identifier, BiFunction<AbstractNodePlugin, EtherAdaptNodeScreen, BaseEtherNodeTabWidgetProvider<?>> widget) {
        widgets.put(identifier, widget);
    }

    public <T extends AbstractNodePlugin> BaseEtherNodeTabWidgetProvider<T> getWidget(Identifier identifier, T node, EtherAdaptNodeScreen menu) {
        return (BaseEtherNodeTabWidgetProvider<T>) widgets.get(identifier).apply(node, menu);
    }
}
