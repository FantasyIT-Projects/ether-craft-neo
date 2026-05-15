package studio.fantasyit.ether_craft.node.tabs;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;
import studio.fantasyit.ether_craft.menu.base.RangeLimitPlaceContainer;
import studio.fantasyit.ether_craft.menu.base.slot.BaseSlot;
import studio.fantasyit.ether_craft.menu.base.widget.IASwitchButton;
import studio.fantasyit.ether_craft.menu.node.EtherAdaptNodeAsset;
import studio.fantasyit.ether_craft.menu.node.EtherAdaptNodeScreen;
import studio.fantasyit.ether_craft.menu.node.slot.RangeLimitFilterSlot;
import studio.fantasyit.ether_craft.node.plugins.MainPageDummyPlugin;
import studio.fantasyit.ether_craft.node.plugins.MainPageDummyPlugin.MainPageContext;
import studio.fantasyit.ether_craft.node.plugins.base.PluginMenuContext;
import studio.fantasyit.ether_craft.util.UIUtil;

import java.util.List;

public class MainPageProvider extends BaseEtherNodeTabWidgetProvider<MainPageDummyPlugin> {

    private IASwitchButton filterBtn;

    public MainPageProvider(PluginMenuContext<MainPageDummyPlugin> context, EtherAdaptNodeScreen screen) {
        super(context, screen);
        collectImageAsset(EtherAdaptNodeAsset.SLOT_LARGE, 26, 43);
        collectImageAsset(EtherAdaptNodeAsset.ETHER_BAR_CTR, 26, 38);
        collectImageAsset(EtherAdaptNodeAsset.HAMMER, 48, 45);
        collectImageAsset(EtherAdaptNodeAsset.ICON_UPGRADE, 148, 51);
        collectTooltipArea(new Rect2i(lx(26), ly(38), EtherAdaptNodeAsset.ETHER_BAR_CTR.w, EtherAdaptNodeAsset.ETHER_BAR_CTR.h),
                () -> List.of(Component.translatable("menu.ether_craft.ether_bar_tooltip", screen.getMenu().entity.getEther()))
        );
    }

    private MainPageContext ctx() {
        return (MainPageContext) context;
    }

    @Override
    public void createWidget() {
        if (!screen.getMenu().entity.nodeProperty.enableFilter) {
            ctx().filterSlots.forEach(s -> s.setActive(false));
            return;
        }
        filterBtn = screen.addRenderableWidget(new IASwitchButton(
                lx(EtherAdaptNodeAsset.UI_BASE.w + 10), ly(141),
                EtherAdaptNodeAsset.BTN_BLANK,
                EtherAdaptNodeAsset.BTN_BLANK_HOVER,
                EtherAdaptNodeAsset.BTN_BLANK_DOWN,
                EtherAdaptNodeAsset.BTN_BLANK_DOWN_HOVER,
                null,
                Component.translatable("ether_craft.gui.node.filter_mode"),
                Component.translatable("ether_craft.gui.node.filter_mode"),
                (currentlyDown) -> {
                    boolean activate = !currentlyDown;
                    ctx().filterSlots.forEach(s -> s.setActive(activate));
                    ctx().mainSlots.forEach(s -> {
                        if (s instanceof BaseSlot bs) bs.setActive(!activate);
                    });
                    return true;
                }
        ));
        filterBtn.setHidden(true);
        filterBtn.setDown(false);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractBackground(graphics, mouseX, mouseY, a);
        if (screen.getMenu().entity.etherStorage.isEmpty())
            EtherAdaptNodeAsset.SLOT_ETHER.blit(graphics, lx(26), ly(17));
        else
            EtherAdaptNodeAsset.SLOT_LARGE.blit(graphics, lx(26), ly(17));
        if (screen.getMenu().entity.nodeProperty.enableFilter)
            EtherAdaptNodeAsset.FILTER_PANEL.blit(graphics, lx(EtherAdaptNodeAsset.UI_BASE.w + 5), ly(138));
        if (screen.getMenu().entity.nodeProperty.enableFilter) {
            for (Slot s : ctx().filterSlots) {
                if (s.hasItem())
                    UIUtil.renderItemStackSlotPlaceholder(graphics, s.getItem(), lx(s.x), ly(s.y));
            }
        }
    }

    @Override
    public void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractWidgetRenderState(graphics, mouseX, mouseY, a);
        UIUtil.renderEtherBarProgress(
                screen.getMenu().entity.getEther(),
                screen.getMenu().entity.getMaxEther(),
                lx(27), ly(39), EtherAdaptNodeAsset.ETHER_BAR_CTR.w - 2, 2, graphics
        );
        for (Slot s : screen.getMenu().slots) {
            if (s instanceof RangeLimitFilterSlot rls && !rls.valid()) {
                graphics.fill(lx(s.x - 1), ly(s.y - 1), lx(s.x + 17), ly(s.y + 17), 0x60000000);
            }
        }
        RangeLimitPlaceContainer normalStorage = screen.getMenu().entity.normalStorage;
        int accessibleCount = normalStorage.getAccessibleCount();
        if (accessibleCount < normalStorage.getContainerSize() - 7) {
            EtherAdaptNodeAsset.LOCK.blit(graphics, lx(83), ly(78) + 18 * Math.ceilDiv(accessibleCount, 7));
        } else {
            int lastX = 7 + (accessibleCount % 7) * 18;
            EtherAdaptNodeAsset.LOCK.blit(graphics, lx(lastX + 186 - EtherAdaptNodeAsset.LOCK.w / 2), ly(114));
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (filterBtn != null)
            filterBtn.setHidden(!screen.getMenu().entity.nodeProperty.enableFilter);
        if (ctx().mainSlots != null)
            for (RangeLimitFilterSlot s : ctx().mainSlots) {
                s.setEnableFilter(screen.getMenu().entity.nodeProperty.enableFilter);
            }
    }
}
