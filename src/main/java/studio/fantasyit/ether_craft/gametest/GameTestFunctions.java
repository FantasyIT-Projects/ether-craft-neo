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

    public static void register(IEventBus modEventBus) {
        FUNCTIONS.register(modEventBus);
    }
}
