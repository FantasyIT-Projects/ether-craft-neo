package studio.fantasyit.ether_craft.menu.node;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.jetbrains.annotations.NotNull;
import oshi.util.tuples.Pair;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.block.node.EtherAdaptNodeEntity;
import studio.fantasyit.ether_craft.network.c2s.TriggerSwitchTabC2S;
import studio.fantasyit.ether_craft.node.NodePluginManager;
import studio.fantasyit.ether_craft.node.plugins.InstalledPlugin;
import studio.fantasyit.ether_craft.node.tabs.BaseEtherNodeTabWidgetProvider;

import java.util.ArrayList;
import java.util.List;

public class EtherAdaptNodeScreen extends AbstractContainerScreen<@NotNull EtherAdaptNodeContainerMenu> {
    public static final Identifier BACKGROUND = EtherCraft.id("textures/gui/ether_adapt_node_main.png");
    BaseEtherNodeTabWidgetProvider<?> tabProvider;
    EtherAdaptNodeEntity be;
    List<TabWidget> tabs = new ArrayList<>();
    List<Identifier> pluginId = new ArrayList<>();

    public EtherAdaptNodeScreen(EtherAdaptNodeContainerMenu menu, Inventory p_97742_, Component p_97743_) {
        super(menu, p_97742_, p_97743_, 237, 256);
        be = menu.entity;
        inventoryLabelY = imageHeight - 81;
        tabProvider = EtherAdaptNodeUpgradeTabManager.instance.getWidget(menu.installedPlugin.pluginId(), menu.plugin, this);
    }

    @Override
    protected void init() {
        super.init();
        if (tabProvider == null) return;
        tabProvider.createWidget();
    }

    @Override
    protected void containerTick() {
        updateTabs();
        if (tabProvider != null)
            tabProvider.tick();
    }

    protected void updateTabs() {
        List<Pair<NodePluginManager.PluginInfo, InstalledPlugin>> tabList = be.getTabProvider();
        boolean changed = false;
        if (pluginId.size() != tabList.size())
            changed = true;
        else
            for (int i = 0; i < tabList.size(); i++) {
                if (!pluginId.get(i).equals(tabList.get(i).getA().id())) {
                    changed = true;
                    break;
                }
            }

        if (!changed) return;

        tabs.forEach(this::removeWidget);
        tabs.clear();
        pluginId.clear();
        int x = getLeftPos();
        for (Pair<NodePluginManager.PluginInfo, InstalledPlugin> pair : tabList) {
            TabWidget tab = new TabWidget(x, getTopPos() - 20, Component.literal(""), pair.getA().icon(), menu.installedPlugin.equals(pair.getB()), this.makeTabSwitchEvent(pair.getB()));
        }
    }

    private Runnable makeTabSwitchEvent(InstalledPlugin b) {
        return () -> {
            ClientPacketDistributor.sendToServer(new TriggerSwitchTabC2S(b));
        };
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractRenderState(graphics, mouseX, mouseY, a);
        if (tabProvider != null)
            tabProvider.extractWidgetRenderState(graphics, mouseX, mouseY, a);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractBackground(graphics, mouseX, mouseY, a);

        if(tabProvider != null)
            tabProvider.extractBackground(graphics, mouseX, mouseY, a);
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractTooltip(graphics, mouseX, mouseY);
    }

    @Override
    public <T extends GuiEventListener & Renderable & NarratableEntry> T addRenderableWidget(T widget) {
        return super.addRenderableWidget(widget);
    }

    @Override
    public <T extends Renderable> T addRenderableOnly(T renderable) {
        return super.addRenderableOnly(renderable);
    }
}
