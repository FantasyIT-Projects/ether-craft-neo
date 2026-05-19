package studio.fantasyit.ether_craft.block.glass.render;

import com.mojang.serialization.*;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.sprite.Material;
import net.neoforged.neoforge.client.model.block.CustomUnbakedBlockStateModel;
import org.jetbrains.annotations.NotNull;
import studio.fantasyit.ether_craft.EtherCraft;

import java.util.stream.Stream;

public class EtherGlassUnbakedModel implements CustomUnbakedBlockStateModel {
    private static final Material[] MATS = new Material[16];

    {
        for (int i = 0; i < 16; i++)
            MATS[i] = new Material(EtherCraft.id("block/glass/" + i), true);
    }


    @Override
    public @NotNull BlockStateModel bake(@NotNull ModelBaker baker) {
        TextureAtlasSprite[] bgs = new TextureAtlasSprite[16];
        for (int i = 0; i < 16; i++) {
            int finalI = i;
            bgs[i] = baker.materials().get(MATS[i], () -> "GLASS_STATE_" + finalI).sprite();
        }
        return new EtherGlassDynamicModel(bgs);
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
