package studio.fantasyit.ether_craft.gametest;

import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.joml.Vector2i;
import studio.fantasyit.ether_craft.block.base.ItemFilter;
import studio.fantasyit.ether_craft.factory.CraftGridResolver;
import studio.fantasyit.ether_craft.factory.FactoryLevelDef;
import studio.fantasyit.ether_craft.item.CraftTableChipData;
import studio.fantasyit.ether_craft.menu.factory.EtherProcessFactoryAsset;

import java.util.ArrayList;
import java.util.List;

/**
 * 工厂 GUI 布局与工作台网格解析的回归测试。
 *
 * <p>{@link #layoutInvariants}：四级 {@link FactoryLevelDef} 的坐标自洽性（18px 栅格、
 * 主板/侧栏/背包都在窗口内、L2/L3 主板居中）。改动布局常量后这里会第一时间报错。
 *
 * <p>{@link #craftChipGridResolve}：{@link CraftGridResolver} 的解析与回填闭环。
 */
public final class FactoryLayoutGameTests {
    private FactoryLayoutGameTests() {
    }

    public static void layoutInvariants(GameTestHelper helper) {
        for (int level = 1; level <= 4; level++) {
            FactoryLevelDef def = FactoryLevelDef.getByLevel(level);
            String tag = "L" + level;

            int n = def.slotSize().x;
            helper.assertTrue(n == def.slotSize().y, tag + " 内部网格应为正方形");
            helper.assertTrue(n == level * 2 + 1, tag + " 内部网格边长应为 " + (level * 2 + 1));

            // 输入列 → 内部网格 → 输出列 的 18px 栅格关系
            helper.assertTrue(def.posInternal().x == def.posInput().x + 22,
                    tag + " 内部网格应比输入列右移 22px");
            helper.assertTrue(def.posInternal().y == def.posInput().y, tag + " 输入列与内部网格需同高");
            helper.assertTrue(def.posOutput().x == def.posInternal().x + 18 * n + 4,
                    tag + " 输出列应紧贴内部网格右侧 4px");
            helper.assertTrue(def.posOutput().y == def.posInput().y, tag + " 输出列与输入列需同高");
            helper.assertTrue(def.posFilterMark().x == def.posInput().x + 24, tag + " 过滤标记列位置异常");
            helper.assertTrue(def.posFilterInput().x == def.posInput().x + 45, tag + " 过滤输入列位置异常");

            // 主板
            Vector2i origin = def.mainOrigin();
            Vector2i size = def.mainSize();
            Vector2i gui = def.guiSize();
            helper.assertTrue(origin.x >= 0 && origin.y >= 0, tag + " 主板原点不应为负");
            helper.assertTrue(origin.x + size.x <= gui.x && origin.y + size.y <= gui.y,
                    tag + " 主板不应越出窗口");
            helper.assertTrue(gui.y == size.y + 2 + 91, tag + " 窗口高度应为 主板高 + 2 + 91");

            // 槽位都要落在主板内
            helper.assertTrue(def.posInput().x >= origin.x && def.posInput().y >= origin.y,
                    tag + " 输入列应落在主板内");
            helper.assertTrue(def.posInput().y + 18 * n <= origin.y + size.y, tag + " 输入列底部越界");
            helper.assertTrue(def.posInternal().x + 18 * n <= origin.x + size.x, tag + " 内部网格右侧越界");
            helper.assertTrue(def.posOutput().x + 18 <= origin.x + size.x, tag + " 输出列右侧越界");

            // 侧栏
            helper.assertTrue(def.panelLeft().x >= 0 && def.panelLeft().y >= 0, tag + " 以太组不应为负");
            helper.assertTrue(def.panelLeft().x + FactoryLevelDef.ETHER_PANEL_W <= gui.x,
                    tag + " 以太组右侧越界");
            helper.assertTrue(def.panelLeft().y + FactoryLevelDef.ETHER_PANEL_H <= gui.y,
                    tag + " 以太组底部越界");
            helper.assertTrue(def.panelRight().x >= 0 && def.panelRight().y >= 0, tag + " 工具组不应为负");
            helper.assertTrue(def.panelRight().x + FactoryLevelDef.TOOL_PANEL_W <= gui.x,
                    tag + " 工具组右侧越界");
            helper.assertTrue(def.panelRight().y + FactoryLevelDef.TOOL_PANEL_H <= gui.y,
                    tag + " 工具组底部越界");

            // 3 个按钮必须落在工具组底板内（按钮 16px，间距 20）
            int lastButtonBottom = FactoryLevelDef.TOOL_BTN_OFFSET_Y + FactoryLevelDef.TOOL_BTN_GAP * 2 + 16;
            helper.assertTrue(lastButtonBottom <= FactoryLevelDef.TOOL_PANEL_H,
                    tag + " 第 3 个按钮会溢出工具组底板");
            helper.assertTrue(FactoryLevelDef.TOOL_BTN_OFFSET_X + 16 <= FactoryLevelDef.TOOL_PANEL_W,
                    tag + " 按钮宽度超出工具组底板");

            // 玩家背包水平居中、紧贴主板下方
            helper.assertTrue(def.posPlayer().x == (gui.x - FactoryLevelDef.PLAYER_INV_WIDTH) / 2,
                    tag + " 玩家背包应水平居中");
            helper.assertTrue(def.posPlayer().y == size.y + 2, tag + " 玩家背包应紧贴主板下方");
            helper.assertTrue(def.posPlayer().x + FactoryLevelDef.PLAYER_INV_WIDTH <= gui.x,
                    tag + " 玩家背包右侧越界");
            helper.assertTrue(def.posPlayer().y + 90 <= gui.y, tag + " 玩家背包底部越界");

            // L2 / L3 主板水平居中；L1 顶满宽度；L4 主板顶满宽度（侧栏落在背包两侧）
            if (level == 2 || level == 3) {
                helper.assertTrue(origin.x * 2 + size.x == gui.x, tag + " 主板应水平居中");
                helper.assertTrue(def.showPanel(), tag + " 侧栏应有独立底板");
            } else {
                helper.assertTrue(size.x == gui.x, tag + " 主板应顶满窗口宽度");
            }
            if (level == 1) {
                helper.assertTrue(!def.showPanel(), "L1 侧栏不应铺底板");
            }
        }

        // 贴图坐标必须落在图集内（256×256）
        helper.assertTrue(EtherProcessFactoryAsset.RECIPE.w > 0 && EtherProcessFactoryAsset.VIEWER.w > 0,
                "配方/可视化图标应可用");
        helper.assertTrue(EtherProcessFactoryAsset.CRAFTING_GRID.w == 66
                        && EtherProcessFactoryAsset.CRAFTING_GRID.h == 66,
                "工作台网格底板应为 66×66");
        helper.succeed();
    }

    public static void craftChipGridResolve(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();

        // 1) 全空 → 不解析
        helper.assertTrue(CraftGridResolver.resolve(level, List.of()) == null, "空网格不应解析出配方");
        List<ItemStack> empty = emptyGrid();
        helper.assertTrue(CraftGridResolver.resolve(level, empty) == null, "全空网格不应解析出配方");

        // 2) 2×2 橡木木板 → 命中某个原版合成配方，且该配方对同一网格成立
        List<ItemStack> planks = emptyGrid();
        planks.set(0, new ItemStack(Items.OAK_PLANKS));
        planks.set(1, new ItemStack(Items.OAK_PLANKS));
        planks.set(3, new ItemStack(Items.OAK_PLANKS));
        planks.set(4, new ItemStack(Items.OAK_PLANKS));
        CraftTableChipData data = CraftGridResolver.resolve(level, planks);
        helper.assertTrue(data != null, "2×2 橡木木板应解析出配方");
        helper.assertTrue(data.marked().size() == 4, "标记列表应有 4 项，实际 " + data.marked().size());
        helper.assertTrue(data.grid().size() == 9, "应持久化 9 格图案（含空位），实际 " + data.grid().size());
        helper.assertTrue(data.grid().get(2).isEmpty() && data.grid().get(5).isEmpty(),
                "空格子必须保留为空，实际 2=" + data.grid().get(2) + " 5=" + data.grid().get(5));
        helper.assertTrue(recipeMatches(helper, level, data.recipe(), planks),
                "解析出的配方应对该网格成立: " + data.recipe());

        // 3) 回填：把解析出的配方摆回 3×3（必须逐格还原、保留空位）
        ItemFilter filter = new ItemFilter(9, () -> {
        });
        CraftGridResolver.fill(data, filter);
        int filled = 0;
        for (int i = 0; i < 9; i++)
            if (!filter.getItem(i).isEmpty()) filled++;
        helper.assertTrue(filled == 4, "回填后应有 4 个标记，实际 " + filled);
        helper.assertTrue(filter.getItem(2).isEmpty() && filter.getItem(5).isEmpty(),
                "回填后空格子必须仍为空");

        // 4) 无配方物品 → 不解析
        List<ItemStack> star = emptyGrid();
        star.set(4, new ItemStack(Items.NETHER_STAR));
        helper.assertTrue(CraftGridResolver.resolve(level, star) == null, "下界之星不应解析出合成配方");

        helper.succeed();
    }

    /** 配方 → 3×3 标记 → 反查 的闭环（JEI 转移走的是同一条路径）。 */
    public static void craftChipGridMark(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Identifier id = Identifier.withDefaultNamespace("crafting_table");
        CraftingRecipe recipe = findRecipe(level, id);
        helper.assertTrue(recipe != null, "应存在合成配方 " + id);

        ItemFilter grid = new ItemFilter(9, () -> {
        });
        CraftGridResolver.markFromRecipe(level, recipe, grid);

        int filled = 0;
        for (int i = 0; i < 9; i++)
            if (!grid.getItem(i).isEmpty()) filled++;
        helper.assertTrue(filled == 4, "2×2 配方应写入 4 个标记，实际 " + filled);

        List<ItemStack> cells = new ArrayList<>(9);
        for (int i = 0; i < 9; i++) cells.add(grid.getItem(i));
        CraftTableChipData data = CraftGridResolver.resolve(level, cells);
        helper.assertTrue(data != null, "标记应能反查出配方");
        helper.assertTrue(id.equals(data.recipe()), "应反查回同一条配方 " + id + "，实际 " + data.recipe());

        // 清空后写空网格
        CraftGridResolver.markFromRecipe(level, recipe, new ItemFilter(9, () -> {
        }));
        helper.succeed();
    }

    private static CraftingRecipe findRecipe(ServerLevel level, Identifier id) {
        for (RecipeHolder<?> holder : level.recipeAccess().getRecipes())
            if (holder.id().identifier().equals(id) && holder.value() instanceof CraftingRecipe crafting)
                return crafting;
        return null;
    }

    // ------------------------------------------------------------------ 辅助

    private static List<ItemStack> emptyGrid() {
        List<ItemStack> grid = new ArrayList<>(9);
        for (int i = 0; i < 9; i++) grid.add(ItemStack.EMPTY);
        return grid;
    }

    private static boolean recipeMatches(GameTestHelper helper, ServerLevel level, Identifier id, List<ItemStack> grid) {
        List<ItemStack> cells = new ArrayList<>(9);
        for (ItemStack stack : grid) cells.add(stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1));
        CraftingInput input = CraftingInput.of(3, 3, cells);
        for (RecipeHolder<?> holder : level.recipeAccess().getRecipes()) {
            if (holder.id().identifier().equals(id) && holder.value() instanceof CraftingRecipe recipe)
                return recipe.matches(input, level);
        }
        helper.assertTrue(false, "找不到配方 " + id);
        return false;
    }
}
