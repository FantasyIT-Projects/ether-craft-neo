package studio.fantasyit.ether_craft.gametest;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import studio.fantasyit.ether_craft.EtherCraft;

import java.util.function.Consumer;

/**
 * GameTest 测试函数注册（{@code BuiltInRegistries.TEST_FUNCTION}）。
 * 测试实例由 {@link GameTestRegistration} 在 {@code RegisterGameTestsEvent} 中绑定到这些函数。
 */
public final class GameTestFunctions {
    private GameTestFunctions() {
    }

    private static final DeferredRegister<Consumer<GameTestHelper>> FUNCTIONS =
            DeferredRegister.create(BuiltInRegistries.TEST_FUNCTION, EtherCraft.MODID);

    public static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> FACTORY_RECIPE_MATCH_FIXTURE =
            FUNCTIONS.register("factory_recipe_match_fixture", () -> FactoryRecipeMatchGameTests::recipeMatchFixture);

    public static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> FACTORY_RECIPE_MATCH_DATAPACK =
            FUNCTIONS.register("factory_recipe_match_datapack", () -> FactoryRecipeMatchGameTests::recipeMatchDatapack);

    public static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> FACTORY_RECIPE_COST_MAPPING =
            FUNCTIONS.register("factory_recipe_cost_mapping", () -> FactoryRecipeMatchGameTests::recipeCostMapping);

    public static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> FACTORY_TREE_MODEL_FIXTURE =
            FUNCTIONS.register("factory_tree_model_fixture", () -> FactoryTreeViewGameTests::treeModelFixture);

    public static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> FACTORY_TREE_MODEL_SIGNATURE =
            FUNCTIONS.register("factory_tree_model_signature", () -> FactoryTreeViewGameTests::treeModelSignature);

    public static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> FACTORY_TREE_LAYOUT_COMPOSE =
            FUNCTIONS.register("factory_tree_layout_compose", () -> FactoryTreeViewGameTests::treeLayoutCompose);

    public static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> FACTORY_CONCAT_FLOW_FIXTURE =
            FUNCTIONS.register("factory_concat_flow_fixture", () -> FactoryConcatFlowGameTests::concatFlowFixture);

    public static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> FACTORY_CONCAT_GUIDE_LAYOUT =
            FUNCTIONS.register("factory_concat_guide_layout", () -> FactoryConcatFlowGameTests::concatGuideLayoutFixture);

    public static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> FACTORY_TREE_MODEL_CONCAT =
            FUNCTIONS.register("factory_tree_model_concat", () -> FactoryTreeViewGameTests::treeModelConcatSplit);

    public static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> FACTORY_CHIP_CHANNEL_ARROWS =
            FUNCTIONS.register("factory_chip_channel_arrows", () -> FactoryChipArrowGameTests::channelArrowsFixture);

    public static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> CRAFT_TABLE_NOT_SPECIAL =
            FUNCTIONS.register("craft_table_not_special", () -> SpecialRecipeGameTests::craftTableNotSpecial);

    public static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> CRAFT_TABLE_MATCHED =
            FUNCTIONS.register("craft_table_matched", () -> SpecialRecipeGameTests::craftTableMatched);

    public static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> CRAFT_TABLE_NO_MATCH =
            FUNCTIONS.register("craft_table_no_match", () -> SpecialRecipeGameTests::craftTableNoMatch);

    public static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> CRAFT_TABLE_INPUTS_IGNORED =
            FUNCTIONS.register("craft_table_inputs_ignored", () -> SpecialRecipeGameTests::craftTableInputsIgnored);

    public static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> CRAFT_TABLE_CACHE_REUSE =
            FUNCTIONS.register("craft_table_cache_reuse", () -> SpecialRecipeGameTests::craftTableCacheReuse);

    public static void register(IEventBus modEventBus) {
        FUNCTIONS.register(modEventBus);
    }
}
