package studio.fantasyit.ether_craft.datagen;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.world.item.Items;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.data.DataMapProvider;
import studio.fantasyit.ether_craft.datapack.AccelerateRepeatCounts;
import studio.fantasyit.ether_craft.datapack.AccelerateRepeatCounts.Mode;
import studio.fantasyit.ether_craft.datapack.StoneGeneratorRatio;

import java.util.concurrent.CompletableFuture;

public class DataMapGen extends DataMapProvider {
    public DataMapGen(PackOutput packOutput, CompletableFuture<HolderLookup.Provider> lookupProvider) {
        super(packOutput, lookupProvider);
    }

    @Override
    protected void gather(HolderLookup.Provider provider) {
        this.builder(StoneGeneratorRatio.STONE_GENERATOR_RATIO)
                .replace(true)
                .add(Items.COBBLESTONE.builtInRegistryHolder(), new StoneGeneratorRatio(100, 25), false)
                .add(Items.BASALT.builtInRegistryHolder(), new StoneGeneratorRatio(100, 50), false);

        var rtBuilder = this.builder(AccelerateRepeatCounts.RANDOM_TICK_REPEAT).replace(false);

        // BONE_MEAL — 对 BonemealableBlock 调用 performBonemeal repeat 次
        // CROPS tag 默认 BONE_MEAL,1，个别方块下面单独覆盖
        rtBuilder.add(BlockTags.CROPS, new AccelerateRepeatCounts(Mode.BONE_MEAL, 1), false);
        // 以下覆盖 CROPS tag 的默认值
        rtBuilder.add(Blocks.TORCHFLOWER_CROP.builtInRegistryHolder(), new AccelerateRepeatCounts(Mode.BONE_MEAL, 2), false); // 一次骨粉加 1 级，满 2 级变火把花
        rtBuilder.add(Blocks.PITCHER_CROP.builtInRegistryHolder(), new AccelerateRepeatCounts(Mode.BONE_MEAL, 4), false);  // 一次骨粉加 1 级，满 4 级成熟
        // stems 用 BOTH 模式，在下面单独加
        rtBuilder.add(Blocks.COCOA.builtInRegistryHolder(), new AccelerateRepeatCounts(Mode.BONE_MEAL, 2), false);         // 一次骨粉加 1 级，满 2 级成熟
        rtBuilder.add(Blocks.SWEET_BERRY_BUSH.builtInRegistryHolder(), new AccelerateRepeatCounts(Mode.BONE_MEAL, 3), false); // 一次骨粉加 1 级，满 3 级可收获
        rtBuilder.add(Blocks.MANGROVE_PROPAGULE.builtInRegistryHolder(), new AccelerateRepeatCounts(Mode.BONE_MEAL, 4), false); // 悬挂时一次骨粉加 1 级，满 4 级可掉落（覆盖 SAPLINGS tag）
        // 树苗 — 45% 概率直接长成树（mangrove_propagule 在上面单独覆盖）
        rtBuilder.add(BlockTags.SAPLINGS, new AccelerateRepeatCounts(Mode.BONE_MEAL, 1), false);
        // 蘑菇 — 40% 概率长成巨型蘑菇
        rtBuilder.add(Blocks.RED_MUSHROOM.builtInRegistryHolder(), new AccelerateRepeatCounts(Mode.BONE_MEAL, 1), false);
        rtBuilder.add(Blocks.BROWN_MUSHROOM.builtInRegistryHolder(), new AccelerateRepeatCounts(Mode.BONE_MEAL, 1), false);
        // 竹子 — 一次骨粉长 1~2 节 / 直接长成竹子
        rtBuilder.add(Blocks.BAMBOO.builtInRegistryHolder(), new AccelerateRepeatCounts(Mode.BONE_MEAL, 1), false);
        rtBuilder.add(Blocks.BAMBOO_SAPLING.builtInRegistryHolder(), new AccelerateRepeatCounts(Mode.BONE_MEAL, 1), false);
        // 藤蔓类头部 — 一次骨粉长 1 格
        rtBuilder.add(Blocks.KELP.builtInRegistryHolder(), new AccelerateRepeatCounts(Mode.BONE_MEAL, 1), false);
        rtBuilder.add(Blocks.TWISTING_VINES.builtInRegistryHolder(), new AccelerateRepeatCounts(Mode.BONE_MEAL, 1), false);
        rtBuilder.add(Blocks.WEEPING_VINES.builtInRegistryHolder(), new AccelerateRepeatCounts(Mode.BONE_MEAL, 1), false);
        // 菌岩 — 一次骨粉生长植被
        rtBuilder.add(Blocks.CRIMSON_NYLIUM.builtInRegistryHolder(), new AccelerateRepeatCounts(Mode.BONE_MEAL, 1), false);
        rtBuilder.add(Blocks.WARPED_NYLIUM.builtInRegistryHolder(), new AccelerateRepeatCounts(Mode.BONE_MEAL, 1), false);

        // BOTH — randomTick + boneMeal 各一次
        rtBuilder.add(Blocks.MELON_STEM.builtInRegistryHolder(), new AccelerateRepeatCounts(Mode.BOTH, 1), false);          // randomTick 老化茎+结果 + boneMeal 额外老化
        rtBuilder.add(Blocks.PUMPKIN_STEM.builtInRegistryHolder(), new AccelerateRepeatCounts(Mode.BOTH, 1), false);       // 同上
        rtBuilder.add(Blocks.CAVE_VINES.builtInRegistryHolder(), new AccelerateRepeatCounts(Mode.BOTH, 1), false);         // randomTick 向下生长 + boneMeal 结果
        rtBuilder.add(Blocks.CAVE_VINES_PLANT.builtInRegistryHolder(), new AccelerateRepeatCounts(Mode.BOTH, 1), false);   // 同上

        // RANDOM_TICK — 调用 randomTick repeat 次
        rtBuilder.add(Blocks.SUGAR_CANE.builtInRegistryHolder(), new AccelerateRepeatCounts(Mode.RANDOM_TICK, 15), false); // AGE 0→15 正好长高一格
        rtBuilder.add(Blocks.CACTUS.builtInRegistryHolder(), new AccelerateRepeatCounts(Mode.RANDOM_TICK, 15), false);     // AGE 0→15 正好长高一格
        rtBuilder.add(Blocks.NETHER_WART.builtInRegistryHolder(), new AccelerateRepeatCounts(Mode.RANDOM_TICK, 10), false); // 10% 概率 +1 级，平均 10 次进一级
        rtBuilder.add(Blocks.CHORUS_FLOWER.builtInRegistryHolder(), new AccelerateRepeatCounts(Mode.RANDOM_TICK, 1), false); // 一次随机刻执行完整分支逻辑
        rtBuilder.add(Blocks.VINE.builtInRegistryHolder(), new AccelerateRepeatCounts(Mode.RANDOM_TICK, 1), false);        // 一次随机刻尝试扩散
        rtBuilder.add(Blocks.BUDDING_AMETHYST.builtInRegistryHolder(), new AccelerateRepeatCounts(Mode.RANDOM_TICK, 2), false); // 20% 概率生长，2 次 ≈ 36% 概率至少长一个
        rtBuilder.add(Blocks.POINTED_DRIPSTONE.builtInRegistryHolder(), new AccelerateRepeatCounts(Mode.RANDOM_TICK, 5), false); // 1.14% 概率生长，5 次 ≈ 5.6% 概率生长
    }
}
