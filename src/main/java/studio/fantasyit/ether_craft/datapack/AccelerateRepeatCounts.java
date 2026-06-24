package studio.fantasyit.ether_craft.datapack;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.registries.datamaps.DataMapType;
import studio.fantasyit.ether_craft.EtherCraft;

public record AccelerateRepeatCounts(Mode mode, int repeat) {
    public enum Mode {
        RANDOM_TICK,
        BONE_MEAL,
        BOTH
    }

    private static AccelerateRepeatCounts DEFAULT = new AccelerateRepeatCounts(Mode.RANDOM_TICK, 2);
    public static Codec<AccelerateRepeatCounts> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.xmap(Mode::valueOf, Mode::name).fieldOf("mode").forGetter(AccelerateRepeatCounts::mode),
            Codec.INT.fieldOf("repeat").forGetter(AccelerateRepeatCounts::repeat)
    ).apply(i, AccelerateRepeatCounts::new));
    public static final DataMapType<Block, AccelerateRepeatCounts> RANDOM_TICK_REPEAT = DataMapType.builder(
            EtherCraft.id("accelerate_repeat"),
            Registries.BLOCK,
            CODEC
    ).build();

    public static void apply(ServerLevel level, BlockPos blockPos, BlockState blockState) {
        Block block = blockState.getBlock();
        AccelerateRepeatCounts data = blockState.typeHolder().getData(RANDOM_TICK_REPEAT);
        if (data == null) data = DEFAULT;
        if (data.mode == Mode.RANDOM_TICK || data.mode == Mode.BOTH) {
            for (int i = 0; i < data.repeat; i++) {
                blockState.randomTick(level, blockPos, level.getRandom());
                blockState = level.getBlockState(blockPos);
            }
        }
        if (data.mode == Mode.BONE_MEAL || data.mode == Mode.BOTH) {
            if (block instanceof BonemealableBlock b) {
                for (int i = 0; i < data.repeat; i++) {
                    b.performBonemeal(level, level.getRandom(), blockPos, blockState);
                    blockState = level.getBlockState(blockPos);
                }
            }
        }
    }
}
