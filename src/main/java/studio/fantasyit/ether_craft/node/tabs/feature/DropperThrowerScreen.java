package studio.fantasyit.ether_craft.node.tabs.feature;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import studio.fantasyit.ether_craft.menu.base.widget.ScrollableWidget;
import studio.fantasyit.ether_craft.menu.node.EtherAdaptNodeAsset;
import studio.fantasyit.ether_craft.menu.node.EtherAdaptNodeScreen;
import studio.fantasyit.ether_craft.menu.node.ScreenMenuSyncer;
import studio.fantasyit.ether_craft.network.c2s.SyncScreenDataC2S;
import studio.fantasyit.ether_craft.node.plugins.base.PluginMenuContext;
import studio.fantasyit.ether_craft.node.plugins.feature.AbstractDirectionalFilterFeature;
import studio.fantasyit.ether_craft.node.plugins.feature.FeatureDropperThrower;

import java.util.List;

public class DropperThrowerScreen extends DirectionalFilterScreen {
    private ScrollableWidget throwCountScroll;

    public DropperThrowerScreen(PluginMenuContext<AbstractDirectionalFilterFeature> context, EtherAdaptNodeScreen screen) {
        super(context, screen);
        FeatureDropperThrower plugin = (FeatureDropperThrower) context.plugin;
        screen.registerMenuSyncer(new ScreenMenuSyncer<>(() -> plugin.throwCount, v -> {
            if (throwCountScroll != null)
                throwCountScroll.setValue(v - 1);
        }));
    }

    @Override
    public void createWidget() {
        super.createWidget();
        FeatureDropperThrower plugin = (FeatureDropperThrower) this.plugin;

        throwCountScroll = new ScrollableWidget(
                lx(90), ly(12),
                63,
                EtherAdaptNodeAsset.SCROLL_BACK,
                EtherAdaptNodeAsset.SCROLL_BLOCK,
                EtherAdaptNodeAsset.SCROLL_BLOCK_HOVER,
                EtherAdaptNodeAsset.SCROLL_BLOCK_HOVER,
                v -> {
                    ClientPacketDistributor.sendToServer(new SyncScreenDataC2S(
                            plugin.installedId,
                            FeatureDropperThrower.SYNC_THROW_COUNT,
                            0,
                            v + 1
                    ));
                }
        );
        throwCountScroll.setValue(plugin.throwCount - 1);
        screen.addRenderableWidget(throwCountScroll);
        collectTooltipArea(
                new Rect2i(lx(90), ly(12),
                        EtherAdaptNodeAsset.SCROLL_BLOCK.w, EtherAdaptNodeAsset.SCROLL_BACK.h),
                () -> List.of(Component.translatable("menu.ether_craft.scroll_tooltip"))
        );
    }

    @Override
    public void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractWidgetRenderState(graphics, mouseX, mouseY, a);
        if (throwCountScroll != null) {
            graphics.centeredText(screen.getMinecraft().font,
                    Component.translatable("ether_craft.gui.node.dropper_thrower.throw_count", throwCountScroll.getValue() + 1),
                    lx(90) + 5, ly(12) + EtherAdaptNodeAsset.SCROLL_BACK.h + 2, 0xFFFFFFFF);
        }
    }
}
