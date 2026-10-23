package studio.fantasyit.ether_craft.network.c2s;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import studio.fantasyit.ether_craft.EtherCraft;

import java.util.List;

/**
 * 工作台网格变更：把 9 个标记（行优先，3×3）连同被编辑芯片的槽位索引上传给服务端。
 */
public record CraftChipGridC2S(int chipSlot, List<ItemStack> grid) implements CustomPacketPayload {
    public static final Type<@NotNull CraftChipGridC2S> TYPE = new Type<>(
            EtherCraft.id("craft_chip_grid")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, @NotNull CraftChipGridC2S> CODEC = StreamCodec.composite(
            ByteBufCodecs.INT,
            CraftChipGridC2S::chipSlot,
            ItemStack.OPTIONAL_STREAM_CODEC.apply(ByteBufCodecs.list()),
            CraftChipGridC2S::grid,
            CraftChipGridC2S::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
