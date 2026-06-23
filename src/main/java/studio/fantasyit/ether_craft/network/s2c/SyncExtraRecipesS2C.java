package studio.fantasyit.ether_craft.network.s2c;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.recipe.factory.EtherProcessRecipeManager;

import java.util.ArrayList;
import java.util.List;

public record SyncExtraRecipesS2C(List<EtherProcessRecipeManager.ExtraRecipe> recipes) implements CustomPacketPayload {
    public static final Type<@NotNull SyncExtraRecipesS2C> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(EtherCraft.MODID, "sync_extra_recipes")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, @NotNull SyncExtraRecipesS2C> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.collection(ArrayList::new, EtherProcessRecipeManager.ExtraRecipe.STREAM_CODEC),
                    SyncExtraRecipesS2C::recipes,
                    SyncExtraRecipesS2C::new
            );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            EtherProcessRecipeManager.extraRecipes = new ArrayList<>();
            EtherProcessRecipeManager.extraRecipes.addAll(recipes);
        });
    }
}
