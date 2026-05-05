package studio.fantasyit.ether_craft.entity;

import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

public class EtherStreamEntityRender extends EntityRenderer<@NotNull EtherStreamEntity, @NotNull EntityRenderState> {
    public EtherStreamEntityRender(EntityRendererProvider.Context p_174296_) {
        super(p_174296_);
    }

    @Override
    public EntityRenderState createRenderState() {
        return new EntityRenderState();
    }
}
