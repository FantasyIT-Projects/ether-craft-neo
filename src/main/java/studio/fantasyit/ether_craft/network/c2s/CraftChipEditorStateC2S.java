package studio.fantasyit.ether_craft.network.c2s;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jetbrains.annotations.NotNull;
import studio.fantasyit.ether_craft.EtherCraft;

/**
 * 工作台网格编辑器的开关：打开时由服务端把芯片的 3×3 图案写进 {@code craftGrid}，
 * 由原版槽位同步推给客户端；关闭时服务端清空并退出编辑态。
 */
public record CraftChipEditorStateC2S(int chipSlot, boolean open) implements CustomPacketPayload {
    public static final Type<@NotNull CraftChipEditorStateC2S> TYPE = new Type<>(
            EtherCraft.id("craft_chip_editor_state")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, @NotNull CraftChipEditorStateC2S> CODEC = StreamCodec.composite(
            ByteBufCodecs.INT,
            CraftChipEditorStateC2S::chipSlot,
            ByteBufCodecs.BOOL,
            CraftChipEditorStateC2S::open,
            CraftChipEditorStateC2S::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
