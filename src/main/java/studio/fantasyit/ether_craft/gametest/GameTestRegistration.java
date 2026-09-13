package studio.fantasyit.ether_craft.gametest;

import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import studio.fantasyit.ether_craft.EtherCraft;

import java.util.List;
import java.util.function.Consumer;

/**
 * 26.1 的 GameTest 只有“注册函数 + 注册测试实例”两条腿：
 * 测试实例在此处用代码注册，避免额外的 {@code data/ether_craft/test_instance/*.json}。
 *
 * <p>环境使用与 {@code minecraft:default} 等价的空 {@code all_of}，
 * 结构固定为原版空结构 {@code minecraft:empty}（本测试不依赖任何世界方块）。
 */
@EventBusSubscriber(modid = EtherCraft.MODID)
public final class GameTestRegistration {
    private static final Identifier STRUCTURE = Identifier.withDefaultNamespace("empty");
    private static final int MAX_TICKS = 200;

    private GameTestRegistration() {
    }

    @SubscribeEvent
    public static void onRegisterGameTests(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition<?>> environment = event.registerEnvironment(
                EtherCraft.id("gametest_default"),
                new TestEnvironmentDefinition.AllOf(List.<Holder<TestEnvironmentDefinition<?>>>of()));

        registerTest(event, environment, "factory_recipe_match_fixture",
                GameTestFunctions.FACTORY_RECIPE_MATCH_FIXTURE.getKey());
        registerTest(event, environment, "factory_recipe_match_datapack",
                GameTestFunctions.FACTORY_RECIPE_MATCH_DATAPACK.getKey());
        registerTest(event, environment, "factory_recipe_cost_mapping",
                GameTestFunctions.FACTORY_RECIPE_COST_MAPPING.getKey());
    }

    private static void registerTest(RegisterGameTestsEvent event,
                                     Holder<TestEnvironmentDefinition<?>> environment,
                                     String name,
                                     ResourceKey<Consumer<GameTestHelper>> function) {
        event.registerTest(EtherCraft.id(name),
                new FunctionGameTestInstance(function, new TestData<>(environment, STRUCTURE, MAX_TICKS, 0, true)));
    }
}
