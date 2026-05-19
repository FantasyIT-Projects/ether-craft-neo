package studio.fantasyit.ether_craft.factory;

import org.joml.Vector2i;
import org.joml.Vector4i;

public record FactoryLevelDef(
        Vector2i mainSize,
        Vector2i guiSize,
        Vector2i slotSize,
        Vector2i posInput,
        Vector2i posInternal,
        Vector2i posOutput,
        Vector2i panelLeft,
        Vector2i panelRight,
        boolean showPanel,
        Vector2i posFilterMark,
        Vector2i posFilterInput,
        Vector2i posPlayer
) {
    private static final int PLAYER_INV_HEIGHT = 91;
    private static final int PLAYER_INV_WIDTH = 176;

    public FactoryLevelDef(
            Vector4i sz,
            Vector2i size,
            Vector2i posInput,
            Vector2i panelLeft,
            Vector2i panelRight,
            boolean showPanel
    ) {
        Vector2i basePos = new Vector2i(sz.x, sz.y);
        Vector2i uisz = new Vector2i(sz.z - sz.x + 1, sz.w - sz.y + 1);
        this(
                uisz,
                uisz.add(0, PLAYER_INV_HEIGHT + 2, new Vector2i()),
                size,
                posInput.sub(basePos, new Vector2i()),
                posInput.sub(basePos, new Vector2i()).add(18 + 4, 0, new Vector2i()),
                posInput.sub(basePos, new Vector2i()).add(18 + 4 + 18 * size.x + 4, 0, new Vector2i()),
                panelLeft.sub(basePos, new Vector2i()),
                panelRight.sub(basePos, new Vector2i()),
                showPanel,
                posInput.sub(basePos, new Vector2i()).add(18 + 6, 0, new Vector2i()),
                posInput.sub(basePos, new Vector2i()).add(18 + 27, 0, new Vector2i()),
                new Vector2i((uisz.x - PLAYER_INV_WIDTH) / 2, uisz.y + 2)
        );
    }

    public static final FactoryLevelDef LEVEL_1_3x3 = new FactoryLevelDef(
            new Vector4i(288, 334, 463, 417),
            new Vector2i(3, 3),
            new Vector2i(327, 353),
            new Vector2i(300, 371),
            new Vector2i(435, 372),
            false
    );
    public static final FactoryLevelDef LEVEL_2_5x5 = new FactoryLevelDef(
            new Vector4i(288, 204, 463, 314),
            new Vector2i(5, 5),
            new Vector2i(309, 219),
            new Vector2i(261, 279),
            new Vector2i(465, 279),
            true
    );


    public static final FactoryLevelDef LEVEL_3_7x7 = new FactoryLevelDef(
            new Vector4i(27, 274, 220, 420),
            new Vector2i(7, 7),
            new Vector2i(39, 289),
            new Vector2i(0, 385),
            new Vector2i(222, 385),
            true
    );

    public static final FactoryLevelDef LEVEL_4_9x9 = new FactoryLevelDef(
            new Vector4i(0, 0, 229, 182),
            new Vector2i(9, 9),
            new Vector2i(12, 15),
            new Vector2i(0, 184),
            new Vector2i(204, 184),
            true
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
