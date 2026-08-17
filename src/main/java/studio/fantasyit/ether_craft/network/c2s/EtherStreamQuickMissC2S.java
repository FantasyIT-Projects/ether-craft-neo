package studio.fantasyit.ether_craft.network.c2s;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.stream.idx.AutoIndexPosDir;
import studio.fantasyit.ether_craft.stream.vholder.VirtualEtherStreamHolderManager;

/**
 * 客户端自愈请求：客户端收到 quick 创建包但缺少派生基座（entry 不存在或 !hasLast()，
 * 通常因之前的 full 创建包在索引映射竞速等场景下被丢弃）时发送。
 * 服务端清除该玩家的 quick 状态（playerLastCreateId），使下一个新流对该玩家回退为全量创建包，
 * 重建 lastCreateEntry，恢复 quick 派生链。
 */
public record EtherStreamQuickMissC2S(AutoIndexPosDir posDir) implements CustomPacketPayload {
    public static final Type<@NotNull EtherStreamQuickMissC2S> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(EtherCraft.MODID, "es_quick_miss")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, @NotNull EtherStreamQuickMissC2S> CODEC =
            AutoIndexPosDir.STREAM_CODEC.map(EtherStreamQuickMissC2S::new, EtherStreamQuickMissC2S::posDir);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(EtherStreamQuickMissC2S message, Player player) {
        if (player.level() instanceof ServerLevel level) {
            VirtualEtherStreamHolderManager.get(level).clearQuickState(player.getId());
        }
    }
}
