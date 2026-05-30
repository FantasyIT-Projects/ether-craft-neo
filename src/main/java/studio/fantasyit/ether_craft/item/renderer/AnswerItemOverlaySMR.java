package studio.fantasyit.ether_craft.item.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;
import studio.fantasyit.ether_craft.register.DataComponentRegistry;

import java.util.function.Consumer;

public record AnswerItemOverlaySMR() implements SpecialModelRenderer<ItemStackTemplate> {

    @Override
    public void submit(@Nullable ItemStackTemplate targetStack, PoseStack poseStack, SubmitNodeCollector submitNodeCollector,
                       int light, int overlay, boolean hasFoil, int outline) {
        if (targetStack == null) return;

        ItemModelResolver resolver = Minecraft.getInstance().getItemModelResolver();
        ItemStackRenderState state = new ItemStackRenderState();
        resolver.updateForTopItem(state, targetStack.create(), ItemDisplayContext.GUI,
                Minecraft.getInstance().level, null, 0);

        poseStack.pushPose();
        poseStack.translate(0.7f, 0.3f, 0.54f);
        poseStack.scale(0.5f, 0.5f, 0.001f);
        state.submit(poseStack, submitNodeCollector, light, overlay, outline);
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

    public record Unbaked() implements SpecialModelRenderer.Unbaked<ItemStackTemplate> {
        public static final MapCodec<Unbaked> MAP_CODEC = MapCodec.unit(new Unbaked());

        @Override
        public MapCodec<Unbaked> type() {
            return MAP_CODEC;
        }

        @Override
        public @Nullable SpecialModelRenderer<ItemStackTemplate> bake(BakingContext ctx) {
            return new AnswerItemOverlaySMR();
        }
    }
}
