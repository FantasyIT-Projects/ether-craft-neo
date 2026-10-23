package studio.fantasyit.ether_craft.factory;

import org.joml.Vector2i;

/**
 * 以太加工中心的四级 GUI 布局定义。
 *
 * <p>所有坐标都是「窗口相对坐标」（相对 GUI 左上角），槽位按 18px 栅格排布：
 * <ul>
 *     <li>主板：{@link #mainOrigin} 起、{@link #mainSize} 大小，用 MAIN_BG 九宫格铺满；</li>
 *     <li>输入列 / 内部 N×N / 输出列：
 *     {@code posInput → posInput+(22,0) = posInternal → posInternal+(18N+4,0) = posOutput}；</li>
 *     <li>左侧以太组：26×36（18×18 以太槽 + 18×4 以太条）；右侧工具组：26×65（3 个 16×16 按钮，垂直间距 20）；</li>
 *     <li>玩家背包：{@link #posPlayer} 起，176×90，水平居中；窗口高度 = 主板高 + 2 + 91。</li>
 * </ul>
 *
 * <p>{@link #showPanel} 表示左右两组是否各自铺一块 26 宽的九宫格底板；
 * 为 {@code false} 时（仅 L1）槽位与按钮直接画在主板底色上。
 */
public record FactoryLevelDef(
        /** 主板在窗口内的左上角 */
        Vector2i mainOrigin,
        /** 主板（九宫格）尺寸 */
        Vector2i mainSize,
        /** 整个窗口尺寸（含侧栏与玩家背包） */
        Vector2i guiSize,
        /** 内部芯片网格 N×N */
        Vector2i slotSize,
        Vector2i posInput,
        Vector2i posInternal,
        Vector2i posOutput,
        /** 左侧以太组落点（showPanel 时为 26×36 面板左上角，否则为以太槽落点） */
        Vector2i panelLeft,
        /** 右侧工具组落点（26×65 面板左上角，按钮自 +5,+4 起、垂直间距 20） */
        Vector2i panelRight,
        boolean showPanel,
        Vector2i posFilterMark,
        Vector2i posFilterInput,
        Vector2i posPlayer
) {
    public static final int PLAYER_INV_WIDTH = 176;
    public static final int PLAYER_INV_HEIGHT = 91;
    /** 右侧工具组 26×65 底板尺寸。 */
    public static final int TOOL_PANEL_W = 26;
    public static final int TOOL_PANEL_H = 65;
    /** 工具组内按钮的间距与首个按钮相对面板的偏移。 */
    public static final int TOOL_BTN_GAP = 20;
    public static final int TOOL_BTN_OFFSET_X = 5;
    public static final int TOOL_BTN_OFFSET_Y = 4;
    /** 左侧以太组 26×36 底板尺寸。 */
    public static final int ETHER_PANEL_W = 26;
    public static final int ETHER_PANEL_H = 36;

    /** L1 · 3×3：主板左右各有 39px 边距，侧栏与按钮直接画在主板内。 */
    public static final FactoryLevelDef LEVEL_1_3x3 = new FactoryLevelDef(
            new Vector2i(0, 0),
            new Vector2i(176, 84),
            new Vector2i(176, 177),
            new Vector2i(3, 3),
            new Vector2i(39, 19),
            new Vector2i(61, 19),
            new Vector2i(119, 19),
            new Vector2i(12, 37),
            new Vector2i(142, 12),
            false,
            new Vector2i(63, 19),
            new Vector2i(84, 19),
            new Vector2i(0, 86)
    );

    /** L2 · 5×5：主板 176 宽、水平居中，左右各 27px 侧栏。 */
    public static final FactoryLevelDef LEVEL_2_5x5 = new FactoryLevelDef(
            new Vector2i(27, 0),
            new Vector2i(176, 111),
            new Vector2i(230, 204),
            new Vector2i(5, 5),
            new Vector2i(48, 15),
            new Vector2i(70, 15),
            new Vector2i(164, 15),
            new Vector2i(0, 75),
            new Vector2i(204, 46),
            true,
            new Vector2i(72, 15),
            new Vector2i(93, 15),
            new Vector2i(27, 113)
    );

    /** L3 · 7×7：主板 194 宽、水平居中，左右各 27px 侧栏。 */
    public static final FactoryLevelDef LEVEL_3_7x7 = new FactoryLevelDef(
            new Vector2i(27, 0),
            new Vector2i(194, 147),
            new Vector2i(248, 240),
            new Vector2i(7, 7),
            new Vector2i(39, 15),
            new Vector2i(61, 15),
            new Vector2i(191, 15),
            new Vector2i(0, 111),
            new Vector2i(222, 82),
            true,
            new Vector2i(63, 15),
            new Vector2i(84, 15),
            new Vector2i(36, 149)
    );

    /** L4 · 9×9：主板顶满窗口宽度，左右侧栏落在玩家背包两侧。 */
    public static final FactoryLevelDef LEVEL_4_9x9 = new FactoryLevelDef(
            new Vector2i(0, 0),
            new Vector2i(230, 183),
            new Vector2i(230, 276),
            new Vector2i(9, 9),
            new Vector2i(12, 15),
            new Vector2i(34, 15),
            new Vector2i(200, 15),
            new Vector2i(0, 184),
            new Vector2i(204, 184),
            true,
            new Vector2i(36, 15),
            new Vector2i(57, 15),
            new Vector2i(27, 185)
    );

    public static FactoryLevelDef getByLevel(int level) {
        return switch (level) {
            case 1 -> LEVEL_1_3x3;
            case 2 -> LEVEL_2_5x5;
            case 3 -> LEVEL_3_7x7;
            case 4 -> LEVEL_4_9x9;
            default -> throw new IllegalArgumentException("Invalid level: " + level);
        };
    }
}
