package studio.fantasyit.ether_craft.node.plugins.base;

import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jetbrains.annotations.Nullable;
import studio.fantasyit.ether_craft.block.node.EtherAdaptNodeEntity;
import studio.fantasyit.ether_craft.menu.node.EtherAdaptNodeContainerMenu;
import studio.fantasyit.ether_craft.network.base.ISyncTargetMenu;
import studio.fantasyit.ether_craft.network.c2s.SyncScreenDataC2S;
import studio.fantasyit.ether_craft.node.NodeProperty;
import studio.fantasyit.ether_craft.node.plugins.InstalledPlugin;

import java.util.function.Supplier;

public abstract class AbstractNodePlugin implements ISyncTargetMenu {
    protected final EtherAdaptNodeEntity nodeEntity;
    public InstalledPlugin installedId;
    private long nextActionTick = Long.MIN_VALUE;

    public AbstractNodePlugin(EtherAdaptNodeEntity nodeEntity, InstalledPlugin installedId) {
        this.nodeEntity = nodeEntity;
        this.installedId = installedId;
    }

    public void queueWithCd(Identifier action, int cd, Supplier<Boolean> runnable) {
        Level level = nodeEntity.getLevel();
        if (level != null && level.getGameTime() < nextActionTick)
            return;
        if (!nodeEntity.ticket.allowed(action, installedId))
            return;
        if (runnable.get()) {
            nodeEntity.ticket.requeue(action, installedId, cd);
            if (level != null)
                nextActionTick = level.getGameTime() + cd + 1;
        }
    }

    public void modifyNodeProperty(NodeProperty nodeProperty) {
    }

    public void tickInput() {
    }

    public void tickWork() {
    }

    public void tickOutput() {
    }

    public void saveAdditional(ValueOutput output) {
    }

    public void loadAdditional(ValueInput input) {
    }

    public int earlyHandleInput(ItemStack stack, int amount, @Nullable TransactionContext context) {
        return 0;
    }

    public void onDestroy() {
    }

    public void onBlockUpdate() {
    }

    public boolean shouldSyncEther() {
        return true;
    }

    public void onWrenchRotate(Direction.Axis axis) {
    }

    public void registerSlots(EtherAdaptNodeContainerMenu menu) {
    }

    @Override
    public void syncScreenData(SyncScreenDataC2S message) {
    }

    public PluginMenuContext<?> makeContext(EtherAdaptNodeContainerMenu etherAdaptNodeContainerMenu) {
        return PluginMenuContext.of(etherAdaptNodeContainerMenu, this);
    }

    public boolean preTick() {
        return true;
    }

    public int handleOverflow(ItemStack stack, int amount, @Nullable TransactionContext transaction) {
        return 0;
    }
}