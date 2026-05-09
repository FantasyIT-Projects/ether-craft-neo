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
import studio.fantasyit.ether_craft.block.node.EtherAdaptNodeEntity;
import studio.fantasyit.ether_craft.network.c2s.TriggerSwitchTabC2S;
import studio.fantasyit.ether_craft.node.EtherAdaptNodeUpgradeTabManager;
import studio.fantasyit.ether_craft.node.NodePluginManager;
import studio.fantasyit.ether_craft.node.plugins.InstalledPlugin;
import studio.fantasyit.ether_craft.node.tabs.BaseEtherNodeTabWidgetProvider;

import java.util.ArrayList;
import java.util.List;

import static studio.fantasyit.ether_craft.menu.node.EtherAdaptNodeAsset.UI_BASE;

public class EtherAdaptNodeScreen extends AbstractContainerScreen<@NotNull EtherAdaptNodeContainerMenu> {


    BaseEtherNodeTabWidgetProvider<?> tabProvider;
    EtherAdaptNodeEntity be;
    List<TabWidget> tabs = new ArrayList<>();
    List<Identifier> pluginId = new ArrayList<>();

    public EtherAdaptNodeScreen(EtherAdaptNodeContainerMenu menu, Inventory p_97742_, Component p_97743_) {
        super(menu, p_97742_, p_97743_, UI_BASE.w, UI_BASE.h);
        be = menu.entity;
        inventoryLabelY = imageHeight - 81;
        tabProvider = EtherAdaptNodeUpgradeTabManager.instance.getWidget(menu.installedPlugin.pluginId(), menu.plugin, this);
    }

    @Override
    protected void init() {
        super.init();
        if (tabProvider == null) return;
        tabProvider.createWidget();
        updateTabs();
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
        int x = getLeftPos() + 3;
        for (Pair<NodePluginManager.PluginInfo, InstalledPlugin> pair : tabList) {
            //TODO
            TabWidget tab = new TabWidget(x, getTopPos() - 21, Component.literal("PLUGIN TODO"), pair.getA().icon().asItem().getDefaultInstance(), menu.installedPlugin.equals(pair.getB()), this.makeTabSwitchEvent(pair.getB()));
            tabs.add(tab);
            addRenderableWidget(tab);
            x += tab.getWidth();
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
        UI_BASE.blit(graphics, getLeftPos(), getTopPos());
        if (tabProvider != null)
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
    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int xm, int ym) {
    }
}
