package studio.fantasyit.ether_craft.perf;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.network.s2c.PerfVizSyncS2C;
import studio.fantasyit.ether_craft.stream.PosDir;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@EventBusSubscriber(modid = EtherCraft.MODID)
public class ServerPerf {
    private static final int SEND_INTERVAL_TICKS = 5;

    private static final Set<UUID> enabledPlayers = new HashSet<>();
    private static int sendCooldown = 0;

    private static Object currentKey = null;
    private static long currentStartNanos = 0L;

    public static boolean isRecording() {
        return !enabledPlayers.isEmpty();
    }

    public static void startRecording(PosDir posDir) {
        if (!isRecording()) return;
        currentKey = posDir;
        currentStartNanos = System.nanoTime();
    }

    public static void startRecording(BlockPos blockPos) {
        if (!isRecording()) return;
        currentKey = blockPos;
        currentStartNanos = System.nanoTime();
    }

    public static void end(Level level) {
        if (!isRecording()) return;
        Object key = currentKey;
        currentKey = null;
        if (key == null) return;
        if (!(level instanceof ServerLevel serverLevel)) return;
        long elapsed = System.nanoTime() - currentStartNanos;
        PerfTickData data = PerfTickData.get(serverLevel);
        long gameTime = serverLevel.getGameTime();
        if (key instanceof PosDir posDir) {
            data.recordVESH(posDir, elapsed, gameTime);
        } else if (key instanceof BlockPos blockPos) {
            data.recordBlock(blockPos, elapsed, gameTime);
        }
    }

    public static void setPlayer(Player player, boolean enabled) {
        UUID uuid = player.getUUID();
        if (enabled) {
            enabledPlayers.add(uuid);
        } else {
            enabledPlayers.remove(uuid);
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        enabledPlayers.remove(event.getEntity().getUUID());
    }

    public static void tick(MinecraftServer server) {
        if (enabledPlayers.isEmpty()) return;
        if (++sendCooldown < SEND_INTERVAL_TICKS) return;
        sendCooldown = 0;
        for (ServerLevel level : server.getAllLevels()) {
            PerfTickData data = PerfTickData.get(level);
            data.purge(level.getGameTime());
            PerfVizSyncS2C payload = new PerfVizSyncS2C(data.veshSnapshots(), data.blockSnapshots());
            for (UUID uuid : enabledPlayers) {
                ServerPlayer player = server.getPlayerList().getPlayer(uuid);
                if (player != null && player.level() == level) {
                    PacketDistributor.sendToPlayer(player, payload);
                }
            }
        }
    }
}
