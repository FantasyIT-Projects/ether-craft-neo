package studio.fantasyit.ether_craft.datagen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.stream.Stream;

public record GridSourceFile(
        List<List<Cell>> grid,
        @Nullable String outputItemId,
        List<String> inputItems,
        int outputRow
) {
    public record Cell(String type, @Nullable String chipId) {
        public boolean isChip() {
            return "chip".equals(type) && chipId != null;
        }

        public boolean isBlock() {
            return "block".equals(type);
        }

        public boolean isEmpty() {
            return "empty".equals(type);
        }

        static final MapCodec<Cell> CODEC = new MapCodec<>() {
            @Override
            public <T> Stream<T> keys(DynamicOps<T> ops) {
                return Stream.of(ops.createString("type"), ops.createString("chipId"));
            }

            @Override
            public <T> DataResult<Cell> decode(DynamicOps<T> ops, MapLike<T> input) {
                String type = ops.getStringValue(input.get("type")).getOrThrow();
                String chipId = ops.getStringValue(input.get("chipId")).result().orElse(null);
                return DataResult.success(new Cell(type, chipId));
            }

            @Override
            public <T> RecordBuilder<T> encode(Cell input, DynamicOps<T> ops, RecordBuilder<T> prefix) {
                prefix.add("type", ops.createString(input.type()));
                if (input.chipId() != null) {
                    prefix.add("chipId", ops.createString(input.chipId()));
                }
                return prefix;
            }
        };
    }

    static final MapCodec<GridSourceFile> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
            Codec.list(Codec.list(Cell.CODEC.codec())).fieldOf("grid").forGetter(GridSourceFile::grid),
            Codec.STRING.optionalFieldOf("outputItemId", null).forGetter(GridSourceFile::outputItemId),
            Codec.STRING.listOf().optionalFieldOf("inputItems", List.of()).forGetter(GridSourceFile::inputItems),
            Codec.INT.optionalFieldOf("outputRow", 4).forGetter(GridSourceFile::outputRow)
    ).apply(inst, GridSourceFile::new));
}
