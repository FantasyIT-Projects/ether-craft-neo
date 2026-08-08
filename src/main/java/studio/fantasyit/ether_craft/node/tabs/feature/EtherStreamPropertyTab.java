package studio.fantasyit.ether_craft.node.tabs.feature;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;
import studio.fantasyit.ether_craft.menu.base.widget.IASwitchButton;
import studio.fantasyit.ether_craft.menu.node.EtherAdaptNodeAsset;
import studio.fantasyit.ether_craft.menu.node.EtherAdaptNodeScreen;
import studio.fantasyit.ether_craft.menu.node.ScreenMenuSyncer;
import studio.fantasyit.ether_craft.network.c2s.SyncScreenDataC2S;
import studio.fantasyit.ether_craft.node.plugins.base.PluginMenuContext;
import studio.fantasyit.ether_craft.node.plugins.feature.FeatureEtherStreamProperty;
import studio.fantasyit.ether_craft.node.tabs.BaseEtherNodeTabWidgetProvider;

public class EtherStreamPropertyTab extends BaseEtherNodeTabWidgetProvider<FeatureEtherStreamProperty> {
    private static final int ROW_X = 8;
    private static final int ROW_START_Y = 12;
    private static final int ROW_DY = 20;
    private static final int LABEL_X = ROW_X + 20;

    private EditBox travelEdit;
    private boolean wasFocused = false;
    private boolean lastLimitEnabled;
    private float lastMaxTravel = -1f;

    public EtherStreamPropertyTab(PluginMenuContext<FeatureEtherStreamProperty> context, EtherAdaptNodeScreen screen) {
        super(context, screen);
    }

    private IASwitchButton makeSwitch(int y, Component onMsg, Component offMsg, Identifier syncId, boolean storedValue, boolean invert) {
        boolean isDown = invert != storedValue;
        IASwitchButton button = new IASwitchButton(
                lx(ROW_X), ly(y),
                EtherAdaptNodeAsset.BTN_BLANK,
                EtherAdaptNodeAsset.BTN_BLANK_HOVER,
                EtherAdaptNodeAsset.BTN_BLANK_DOWN,
                EtherAdaptNodeAsset.BTN_BLANK_DOWN_HOVER,
                null, onMsg, offMsg,
                t -> {
                    int data = invert ? (t ? 1 : 0) : ((!t) ? 1 : 0);
                    ClientPacketDistributor.sendToServer(new SyncScreenDataC2S(plugin.installedId, syncId, 0, data));
                    return true;
                }
        );
        button.setDown(isDown);
        screen.addRenderableWidget(button);
        return button;
    }

    @Override
    public void createWidget() {
        IASwitchButton displayTimeButton = makeSwitch(ROW_START_Y,
                Component.translatable("ether_craft.gui.node.stream_property.display_time_on"),
                Component.translatable("ether_craft.gui.node.stream_property.display_time_off"),
                FeatureEtherStreamProperty.SYNC_DISPLAY_TIME, plugin.isDisplayTime, false);
        screen.registerMenuSyncer(new ScreenMenuSyncer<>(() -> plugin.isDisplayTime, displayTimeButton::setDown));

        IASwitchButton limitButton = makeSwitch(ROW_START_Y + ROW_DY,
                Component.translatable("ether_craft.gui.node.stream_property.limit_on"),
                Component.translatable("ether_craft.gui.node.stream_property.limit_off"),
                FeatureEtherStreamProperty.SYNC_LIMIT_ENABLED, plugin.limitEnabled, false);
        screen.registerMenuSyncer(new ScreenMenuSyncer<>(() -> plugin.limitEnabled, limitButton::setDown));

        Font font = Minecraft.getInstance().font;
        Component limitLabel = Component.translatable("ether_craft.gui.node.stream_property.limit_label");
        int editX = LABEL_X + font.width(limitLabel) + 4;
        int editWidth = Math.min(70, 168 - editX);
        travelEdit = new EditBox(font, lx(editX), ly(ROW_START_Y + ROW_DY), editWidth, 12,
                Component.translatable("ether_craft.gui.node.stream_property.max_travel_placeholder")) {
            @Override
            public boolean keyPressed(@NotNull KeyEvent event) {
                if (event.key() == GLFW.GLFW_KEY_ENTER || event.key() == GLFW.GLFW_KEY_KP_ENTER) {
                    screen.setFocused(null);
                    return true;
                }
                return super.keyPressed(event);
            }
        };
        travelEdit.setMaxLength(10);
        travelEdit.setValue(format(plugin.maxTravelLength));
        travelEdit.setVisible(plugin.limitEnabled);
        screen.addRenderableWidget(travelEdit);

        IASwitchButton noEntityHitButton = makeSwitch(ROW_START_Y + ROW_DY * 2,
                Component.translatable("ether_craft.gui.node.stream_property.no_entity_on"),
                Component.translatable("ether_craft.gui.node.stream_property.no_entity_off"),
                FeatureEtherStreamProperty.SYNC_NO_ENTITY_HIT, plugin.noEntityHit, true);
        screen.registerMenuSyncer(new ScreenMenuSyncer<>(() -> !plugin.noEntityHit, noEntityHitButton::setDown));

        IASwitchButton noBlockHitButton = makeSwitch(ROW_START_Y + ROW_DY * 3,
                Component.translatable("ether_craft.gui.node.stream_property.no_block_on"),
                Component.translatable("ether_craft.gui.node.stream_property.no_block_off"),
                FeatureEtherStreamProperty.SYNC_NO_BLOCK_HIT, plugin.noBlockHit, true);
        screen.registerMenuSyncer(new ScreenMenuSyncer<>(() -> !plugin.noBlockHit, noBlockHitButton::setDown));

        lastLimitEnabled = plugin.limitEnabled;
        lastMaxTravel = plugin.maxTravelLength;
    }

    @Override
    public void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractWidgetRenderState(graphics, mouseX, mouseY, a);
        Font font = screen.getMinecraft().font;
        graphics.text(font, Component.translatable("ether_craft.gui.node.stream_property.display_time_label"),
                lx(LABEL_X), ly(ROW_START_Y + 3), 0xFFFFFFFF);
        graphics.text(font, Component.translatable("ether_craft.gui.node.stream_property.limit_label"),
                lx(LABEL_X), ly(ROW_START_Y + ROW_DY + 3), 0xFFFFFFFF);
        graphics.text(font, Component.translatable("ether_craft.gui.node.stream_property.no_entity_label"),
                lx(LABEL_X), ly(ROW_START_Y + ROW_DY * 2 + 3), 0xFFFFFFFF);
        graphics.text(font, Component.translatable("ether_craft.gui.node.stream_property.no_block_label"),
                lx(LABEL_X), ly(ROW_START_Y + ROW_DY * 3 + 3), 0xFFFFFFFF);
        graphics.text(font, Component.translatable("ether_craft.gui.node.stream_property.status",
                        plugin.isDisplayTime, plugin.maxTravelLength, !plugin.noEntityHit, !plugin.noBlockHit),
                lx(ROW_X), ly(ROW_START_Y + ROW_DY * 4 + 2), 0xFFFFFFFF);
    }

    @Override
    public void tick() {
        super.tick();
        if (travelEdit == null) return;
        boolean limit = plugin.limitEnabled;
        if (limit != lastLimitEnabled) {
            lastLimitEnabled = limit;
            travelEdit.setVisible(limit);
            if (!limit) {
                travelEdit.setValue(format(plugin.maxTravelLength));
            }
        }
        if (limit && Math.abs(plugin.maxTravelLength - lastMaxTravel) > 0.005f) {
            lastMaxTravel = plugin.maxTravelLength;
            if (!travelEdit.isFocused()) {
                travelEdit.setValue(format(plugin.maxTravelLength));
            }
        }
        boolean focused = travelEdit.isFocused();
        if (wasFocused && !focused) {
            commitTravel();
        }
        wasFocused = focused;
    }

    private void commitTravel() {
        String text = travelEdit.getValue().trim();
        if (text.isEmpty()) return;
        try {
            float v = Float.parseFloat(text);
            if (v < 0f) v = 0f;
            if (v > FeatureEtherStreamProperty.MAX_TRAVEL_LENGTH) v = FeatureEtherStreamProperty.MAX_TRAVEL_LENGTH;
            ClientPacketDistributor.sendToServer(new SyncScreenDataC2S(
                    plugin.installedId,
                    FeatureEtherStreamProperty.SYNC_MAX_TRAVEL,
                    0, FeatureEtherStreamProperty.toIntData(v)));
        } catch (NumberFormatException ignored) {
        }
    }

    private static String format(float v) {
        return Float.toString(v);
    }
}
