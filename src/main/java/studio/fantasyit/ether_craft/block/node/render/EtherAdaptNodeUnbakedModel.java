package studio.fantasyit.ether_craft.block.node.render;

import com.mojang.serialization.*;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.sprite.Material;
import net.neoforged.neoforge.client.model.block.CustomUnbakedBlockStateModel;
import org.jetbrains.annotations.NotNull;
import studio.fantasyit.ether_craft.EtherCraft;

import java.util.stream.Stream;

public class EtherAdaptNodeUnbakedModel implements CustomUnbakedBlockStateModel {
    private static final Material[] MAT_LEVEL = new Material[3];
    private static final Material[] MAT_LEVEL_EMPTY = new Material[3];

    {
        for (int i = 0; i < 3; i++) {
            MAT_LEVEL[i] = new Material(EtherCraft.id("block/node/ether_adapt_node_lv" + (i + 1)));
            MAT_LEVEL_EMPTY[i] = new Material(EtherCraft.id("block/node/ether_adapt_node_lv" + (i + 1) + "_empty"));
        }
    }


    @Override
    public @NotNull BlockStateModel bake(@NotNull ModelBaker baker) {
        TextureAtlasSprite[] levels = new TextureAtlasSprite[3];
        TextureAtlasSprite[] levelsEmpty = new TextureAtlasSprite[3];
        for (int i = 0; i < 3; i++) {
            int finalI = i;
            levels[i] = baker.materials().get(MAT_LEVEL[i], () -> "EAN_NORMAL_STATE_" + finalI).sprite();
            levelsEmpty[i] = baker.materials().get(MAT_LEVEL_EMPTY[i], () -> "GLASS_STATE_" + finalI).sprite();
        }
        return new EtherAdaptNodeDynamicModel(levels, levelsEmpty);
    }

    @Override
    public @NotNull MapCodec<? extends CustomUnbakedBlockStateModel> codec() {
        return CODEC;
    }

    public static final MapCodec<EtherAdaptNodeUnbakedModel> CODEC = new MapCodec<>() {
        @Override
        public <T> Stream<T> keys(DynamicOps<T> ops) {
            return Stream.empty();
        }

        @Override
        public <T> DataResult<EtherAdaptNodeUnbakedModel> decode(DynamicOps<T> ops, MapLike<T> input) {
            return DataResult.success(new EtherAdaptNodeUnbakedModel());
        }

        @Override
        public <T> RecordBuilder<T> encode(EtherAdaptNodeUnbakedModel input, DynamicOps<T> ops, RecordBuilder<T> prefix) {
            return prefix;
        }
    };

    @Override
    public void resolveDependencies(Resolver resolver) {

    }
}
