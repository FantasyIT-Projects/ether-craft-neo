package studio.fantasyit.ether_craft.item.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.dispatch.BlockModelRotation;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ResolvedModel;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import net.minecraft.client.resources.model.sprite.TextureSlots;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;
import studio.fantasyit.ether_craft.register.DataComponentRegistry;

import studio.fantasyit.ether_craft.render.LightmapSubmitNodeCollector;

import java.util.List;
import java.util.function.Consumer;

public record AnswerItemOverlaySMR(List<BakedQuad> baseQuads) implements SpecialModelRenderer<ItemStackTemplate> {

    @Override
    public void submit(@Nullable ItemStackTemplate targetStack, PoseStack poseStack, SubmitNodeCollector submitNodeCollector,
                       int light, int overlay, boolean hasFoil, int outline) {
        submitNodeCollector.submitItem(poseStack, ItemDisplayContext.NONE,
                light, overlay, 0, new int[0], baseQuads,
                ItemStackRenderState.FoilType.NONE);

        if (targetStack == null) return;

        ItemModelResolver resolver = Minecraft.getInstance().getItemModelResolver();
        ItemStackRenderState state = new ItemStackRenderState();
        resolver.updateForTopItem(state, targetStack.create(), ItemDisplayContext.GUI,
                Minecraft.getInstance().level, null, 0);

        poseStack.pushPose();
        poseStack.translate(0.5f, 0.1f, 0.55f);
        poseStack.scale(0.5f, 0.5f, 0.001f);
        poseStack.translate(0.5f, 0.5f, 1.0f);
        state.submit(poseStack, new LightmapSubmitNodeCollector(submitNodeCollector), light, overlay, outline);
        poseStack.popPose();
    }

    @Override
    public void getExtents(Consumer<Vector3fc> consumer) {
        consumer.accept(new Vector3f(0, 0, 0));
        consumer.accept(new Vector3f(16, 16, 16));
    }

    @Override
    public @Nullable ItemStackTemplate extractArgument(ItemStack itemStack) {
        return itemStack.get(DataComponentRegistry.TARGET);
    }

    public record Unbaked(Identifier modelId) implements SpecialModelRenderer.Unbaked<ItemStackTemplate> {
        public static final MapCodec<Unbaked> MAP_CODEC = Identifier.CODEC
                .fieldOf("model")
                .xmap(Unbaked::new, Unbaked::modelId);

        @Override
        public MapCodec<Unbaked> type() {
            return MAP_CODEC;
        }

        @Override
        public @Nullable SpecialModelRenderer<ItemStackTemplate> bake(BakingContext ctx) {
            ItemModel.BakingContext itemCtx = (ItemModel.BakingContext) ctx;
            ModelBaker baker = itemCtx.blockModelBaker();
            ResolvedModel model = baker.getModel(this.modelId);
            TextureSlots slots = model.getTopTextureSlots();
            QuadCollection quads = model.bakeTopGeometry(slots, baker, BlockModelRotation.IDENTITY);
            return new AnswerItemOverlaySMR(quads.getAll());
        }
    }
}
