package studio.fantasyit.ether_craft.node.plugins.base;

import studio.fantasyit.ether_craft.menu.node.EtherAdaptNodeContainerMenu;

public class PluginMenuContext<T extends AbstractNodePlugin> {
    public static <T extends AbstractNodePlugin> PluginMenuContext<T> of(EtherAdaptNodeContainerMenu menu, T plugin) {
        return new PluginMenuContext<T>(menu, plugin);
    }

    public T plugin;

    public PluginMenuContext(EtherAdaptNodeContainerMenu menu, T plugin) {
        this.plugin = plugin;
        plugin.registerSlots(menu);
    }
}
