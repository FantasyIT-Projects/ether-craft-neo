package studio.fantasyit.ether_craft.menu.factory.window;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * 工厂 GUI 的浮动窗口层：维护窗口 z 序，统一派发输入与渲染。
 *
 * <ul>
 *   <li>列表末尾 = 最上层；点击命中某窗口时把它提到末尾（置顶）；</li>
 *   <li>渲染按列表顺序（先下层后上层），点击按逆序（先上层）；</li>
 *   <li>拖拽 / 松开只派发给按下时吃到点击的窗口，避免拖到一半被别的窗口抢走。</li>
 * </ul>
 */
public class FactoryWindowLayer {
    private final List<FactoryFloatingWindow> windows = new ArrayList<>();
    private @Nullable FactoryFloatingWindow active;

    /** 加入窗口并置顶（重复加入时只调整顺序）。 */
    public void add(FactoryFloatingWindow window) {
        windows.remove(window);
        windows.add(window);
        active = window;
    }

    public void remove(FactoryFloatingWindow window) {
        windows.remove(window);
        if (active == window) active = null;
    }

    public void clear() {
        windows.clear();
        active = null;
    }

    public boolean isEmpty() {
        return windows.isEmpty();
    }

    public int size() {
        return windows.size();
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        for (int i = windows.size() - 1; i >= 0; i--) {
            FactoryFloatingWindow window = windows.get(i);
            if (!window.contains(mouseX, mouseY)) continue;
            boolean consumed = window.mouseClicked(mouseX, mouseY, button);
            if (consumed) {
                bringToFront(i);
                active = window;
            }
            return consumed;
        }
        return false;
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (active == null) return false;
        return active.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        for (int i = windows.size() - 1; i >= 0; i--) {
            FactoryFloatingWindow window = windows.get(i);
            if (window.contains(mouseX, mouseY))
                return window.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }
        return false;
    }

    public void onMouseReleased() {
        if (active != null) active.onMouseReleased();
        active = null;
    }

    /** 键盘交给正在操作的窗口，没有则交给最上层窗口（用于视口平移 / 缩放）。 */
    public boolean keyPressed(int keyCode) {
        FactoryFloatingWindow window = active;
        if (window == null) {
            if (windows.isEmpty()) return false;
            window = windows.get(windows.size() - 1);
        }
        return window.keyPressed(keyCode);
    }

    public void render(GuiGraphicsExtractor graphics, Font font, int mouseX, int mouseY) {
        for (FactoryFloatingWindow window : windows) {
            window.render(graphics, font, mouseX, mouseY);
        }
    }

    /** 只给鼠标所在的**最上层**窗口绘制 tooltip，避免多层窗口同时弹提示。 */
    public void renderTooltips(GuiGraphicsExtractor graphics, Font font, int mouseX, int mouseY) {
        for (int i = windows.size() - 1; i >= 0; i--) {
            FactoryFloatingWindow window = windows.get(i);
            if (!window.contains(mouseX, mouseY)) continue;
            window.renderTooltips(graphics, font, mouseX, mouseY);
            return;
        }
    }

    private void bringToFront(int index) {
        if (index == windows.size() - 1) return;
        FactoryFloatingWindow window = windows.remove(index);
        windows.add(window);
    }
}
