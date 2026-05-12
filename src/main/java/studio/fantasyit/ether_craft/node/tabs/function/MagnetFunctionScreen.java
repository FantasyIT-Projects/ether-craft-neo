package studio.fantasyit.ether_craft.node.tabs.function;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import studio.fantasyit.ether_craft.menu.base.widget.ScrollableWidget;
import studio.fantasyit.ether_craft.menu.node.EtherAdaptNodeAsset;
import studio.fantasyit.ether_craft.menu.node.EtherAdaptNodeScreen;
import studio.fantasyit.ether_craft.network.c2s.SyncScreenDataC2S;
import studio.fantasyit.ether_craft.node.filter.FilterGuiRegClient;
import studio.fantasyit.ether_craft.node.plugins.function.MagnetFunctionFeature;
import studio.fantasyit.ether_craft.node.tabs.BaseEtherNodeTabWidgetProvider;

public class MagnetFunctionScreen extends BaseEtherNodeTabWidgetProvider<MagnetFunctionFeature> {
    private static final int CENTER_RANGE = 10;
    private static final int SHAPE_RANGE = 9;

    private static final int[][] SCROLL_POS = {
            {93, 40}, {106, 40}, {119, 40},
            {140, 40}, {153, 40}, {166, 40},
    };
    private static final int[] MAX_VALUES = {20, 20, 20, 9, 9, 9};

    private final ScrollableWidget[] scrolls = new ScrollableWidget[6];

    public MagnetFunctionScreen(MagnetFunctionFeature menuContext, EtherAdaptNodeScreen screen) {
        super(menuContext, screen);
    }

    @Override
    public void createWidget() {
        FilterGuiRegClient.widget(screen, context.filter.whitelist);

        int[] startValues = {
                context.centerX + CENTER_RANGE,
                context.centerY + CENTER_RANGE,
                context.centerZ + CENTER_RANGE,
                context.shapeX - 1,
                context.shapeY - 1,
                context.shapeZ - 1
        };

        for (int i = 0; i < 6; i++) {
            final int idx = i;
            scrolls[i] = new ScrollableWidget(
                    lx(SCROLL_POS[i][0]), ly(SCROLL_POS[i][1]),
                    MAX_VALUES[i],
                    EtherAdaptNodeAsset.SCROLL_BACK,
                    EtherAdaptNodeAsset.SCROLL_BLOCK,
                    EtherAdaptNodeAsset.SCROLL_BLOCK,
                    EtherAdaptNodeAsset.SCROLL_BLOCK,
                    v -> {
                        int actual = idx < 3 ? v - CENTER_RANGE : v + 1;
                        ClientPacketDistributor.sendToServer(new SyncScreenDataC2S(
                                MagnetFunctionFeature.SYNC_VALUE, idx, actual));
                    }
            );
            scrolls[i].setValue(Math.clamp(startValues[i], 0, MAX_VALUES[i]));
            screen.addRenderableWidget(scrolls[i]);
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractBackground(graphics, mouseX, mouseY, a);
    }

    @Override
    public void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractWidgetRenderState(graphics, mouseX, mouseY, a);
        String[] labels = {"CX", "CY", "CZ", "SX", "SY", "SZ"};
        for (int i = 0; i < 6; i++) {
            graphics.text(screen.getMinecraft().font,
                    Component.literal(labels[i] + ":" + (i < 3 ? scrolls[i].getValue() - CENTER_RANGE : scrolls[i].getValue() + 1)),
                    lx(SCROLL_POS[i][0]), ly(SCROLL_POS[i][1]) - 10, 0xFFFFFF);
        }
    }
}
