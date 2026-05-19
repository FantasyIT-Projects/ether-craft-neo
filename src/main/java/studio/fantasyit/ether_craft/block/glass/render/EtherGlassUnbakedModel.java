package studio.fantasyit.ether_craft.block.glass.render;

import com.mojang.serialization.*;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.client.resources.model.sprite.Material;
import net.neoforged.neoforge.client.model.block.CustomUnbakedBlockStateModel;
import org.jetbrains.annotations.NotNull;
import studio.fantasyit.ether_craft.EtherCraft;

import java.util.stream.Stream;

public class EtherGlassUnbakedModel implements CustomUnbakedBlockStateModel {
    private static final Material BORDER_MATERIAL = new Material(EtherCraft.id("block/glass/border"), true);
    private static final Material CORNER_MATERIAL = new Material(EtherCraft.id("block/glass/border-corner"), true);
    private static final Material BACKGROUND_MATERIAL = new Material(EtherCraft.id("block/glass/background"), true);
    private static final Material BORDER_BACKGROUND_MATERIAL = new Material(EtherCraft.id("block/glass/border-background"), true);


    @Override
    public @NotNull BlockStateModel bake(@NotNull ModelBaker baker) {
        var borderBg = baker.materials().get(BORDER_BACKGROUND_MATERIAL, () -> "BORDER_BG");
        var border = baker.materials().get(BORDER_MATERIAL, () -> "BORDER");
        var bg = baker.materials().get(BACKGROUND_MATERIAL, () -> "BG");
        var borderCorner = baker.materials().get(CORNER_MATERIAL, () -> "CORNER");
        return new EtherGlassDynamicModel(bg.sprite(), borderBg.sprite(), borderCorner.sprite(), border.sprite());
    }

    @Override
    public @NotNull MapCodec<? extends CustomUnbakedBlockStateModel> codec() {
        return CODEC;
    }

    public static final MapCodec<EtherGlassUnbakedModel> CODEC = new MapCodec<>() {
        @Override
        public <T> Stream<T> keys(DynamicOps<T> ops) {
            return Stream.empty();
        }

        @Override
        public <T> DataResult<EtherGlassUnbakedModel> decode(DynamicOps<T> ops, MapLike<T> input) {
            return DataResult.success(new EtherGlassUnbakedModel());
        }

        @Override
        public <T> RecordBuilder<T> encode(EtherGlassUnbakedModel input, DynamicOps<T> ops, RecordBuilder<T> prefix) {
            return prefix;
        }
    };

    @Override
    public void resolveDependencies(Resolver resolver) {

    }
}
