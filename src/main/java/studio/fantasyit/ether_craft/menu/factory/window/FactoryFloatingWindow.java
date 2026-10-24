package studio.fantasyit.ether_craft.menu.factory.window;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * 叠在工厂 GUI 之上的浮动窗口（树形预览窗口 / 配方窗口）。
 *
 * <p>由 {@link FactoryWindowLayer} 按 z 序统一渲染与派发输入；实现方自己决定一次点击是否消费。
 */
public interface FactoryFloatingWindow {
    /** 鼠标是否落在窗口矩形内（含标题栏与边框）。 */
    boolean contains(double mouseX, double mouseY);

    /** 绘制窗口（背景 + 内容 + 窗口装饰）。 */
    void render(GuiGraphicsExtractor graphics, Font font, int mouseX, int mouseY);

    /** 绘制窗口内的 tooltip，需在 GUI 自身 tooltip 之后调用。 */
    void renderTooltips(GuiGraphicsExtractor graphics, Font font, int mouseX, int mouseY);

    /** @return 是否消费这次点击；返回 false 时事件继续下发给 GUI 与槽位。 */
    boolean mouseClicked(double mouseX, double mouseY, int button);

    boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY);

    boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY);

    void onMouseReleased();

    boolean keyPressed(int keyCode);
}
