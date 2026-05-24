package studio.fantasyit.ether_craft.node.tabs.feature;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import studio.fantasyit.ether_craft.menu.base.widget.ScrollableWidget;
import studio.fantasyit.ether_craft.menu.node.EtherAdaptNodeAsset;
import studio.fantasyit.ether_craft.menu.node.EtherAdaptNodeScreen;
import studio.fantasyit.ether_craft.menu.node.ScreenMenuSyncer;
import studio.fantasyit.ether_craft.network.c2s.SyncScreenDataC2S;
import studio.fantasyit.ether_craft.node.plugins.base.PluginMenuContext;
import studio.fantasyit.ether_craft.node.plugins.feature.AbstractDirectionalFilterFeature;
import studio.fantasyit.ether_craft.node.plugins.feature.FeatureEtherStreamEmitter;

public class EtherStreamEmitterScreen extends DirectionalFilterScreen {
    private ScrollableWidget minEtherScroll;
    private final FeatureEtherStreamEmitter.MenuContext context;

    public EtherStreamEmitterScreen(PluginMenuContext<AbstractDirectionalFilterFeature> _context, EtherAdaptNodeScreen screen) {
        super(_context, screen);
        //noinspection rawtypes
        this.context = (FeatureEtherStreamEmitter.MenuContext) (PluginMenuContext) _context;
        screen.registerMenuSyncer(new ScreenMenuSyncer<>(() -> context.plugin.minEther, v -> {
            if (minEtherScroll != null)
                minEtherScroll.setValue(v - context.scrollMin);
        }));
        screen.registerMenuSyncer(new ScreenMenuSyncer<>(() -> context.scrollMin, v -> {
            if (minEtherScroll != null && context.scrollMax > context.scrollMin) {
                minEtherScroll.setMaxValue(context.scrollMax - context.scrollMin);
                minEtherScroll.setValue(context.plugin.minEther - context.scrollMin);
            }
        }));
        screen.registerMenuSyncer(new ScreenMenuSyncer<>(() -> context.scrollMin, v -> {
            if (minEtherScroll != null && context.scrollMax > context.scrollMin) {
                minEtherScroll.setMaxValue(context.scrollMax - context.scrollMin);
                minEtherScroll.setValue(context.plugin.minEther - context.scrollMin);
            }
        }));
    }


    @Override
    public void createWidget() {
        super.createWidget();
        FeatureEtherStreamEmitter plugin = (FeatureEtherStreamEmitter) this.plugin;

        minEtherScroll = new ScrollableWidget(
                lx(90), ly(12),
                0,
                EtherAdaptNodeAsset.SCROLL_BACK,
                EtherAdaptNodeAsset.SCROLL_BLOCK,
                EtherAdaptNodeAsset.SCROLL_BLOCK_HOVER,
                EtherAdaptNodeAsset.SCROLL_BLOCK_HOVER,
                v -> {
                    ClientPacketDistributor.sendToServer(new SyncScreenDataC2S(
                            plugin.installedId,
                            FeatureEtherStreamEmitter.SYNC_MIN_ETHER,
                            0,
                            v
                    ));
                }
        );
        minEtherScroll.setValue(plugin.minEther);
        screen.addRenderableWidget(minEtherScroll);
    }

    @Override
    public void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractWidgetRenderState(graphics, mouseX, mouseY, a);
        if (minEtherScroll != null) {
            graphics.centeredText(screen.getMinecraft().font,
                    Component.translatable("ether_craft.gui.node.emitter.min_ether", minEtherScroll.getValue() + context.scrollMin),
                    lx(90) + 5, ly(12) + EtherAdaptNodeAsset.SCROLL_BACK.h + 2, 0xFFFFFFFF);
        }
    }
}
