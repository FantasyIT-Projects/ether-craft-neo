package studio.fantasyit.ether_craft.node.plugins.feature;

import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.block.node.EtherAdaptNodeEntity;
import studio.fantasyit.ether_craft.menu.base.slot.BaseDataSlot;
import studio.fantasyit.ether_craft.menu.node.EtherAdaptNodeContainerMenu;
import studio.fantasyit.ether_craft.network.c2s.SyncScreenDataC2S;
import studio.fantasyit.ether_craft.node.plugins.InstalledPlugin;
import studio.fantasyit.ether_craft.node.plugins.base.AbstractNodePlugin;
import studio.fantasyit.ether_craft.node.plugins.base.ILoadAdditionalPlugin;
import studio.fantasyit.ether_craft.node.plugins.base.IRegisterSlotsPlugin;
import studio.fantasyit.ether_craft.node.plugins.base.ISaveAdditionalPlugin;
import studio.fantasyit.ether_craft.node.plugins.base.ISyncScreenDataPlugin;

public abstract class AbstractDirectionalFeature extends AbstractNodePlugin implements ISaveAdditionalPlugin, ILoadAdditionalPlugin, ISyncScreenDataPlugin, IRegisterSlotsPlugin {
    public static final Identifier SYNC_DIRECTION = EtherCraft.id("directional_feature/direction");
    public @Nullable Direction direction;

    public AbstractDirectionalFeature(EtherAdaptNodeEntity nodeEntity, InstalledPlugin ID) {
        super(nodeEntity, ID);
    }

    @Override
    public void saveAdditional(ValueOutput output) {
        output.storeNullable("direction", Direction.CODEC, direction);
    }

    @Override
    public void loadAdditional(ValueInput input) {
        direction = input.read("direction", Direction.CODEC).orElse(null);
    }

    @Override
    public void syncScreenData(SyncScreenDataC2S message) {
        if (message.id().equals(SYNC_DIRECTION)) {
            direction = resolveDirection(message.data());
            nodeEntity.pluginUpdate();
        }
    }

    @Override
    public void registerSlots(EtherAdaptNodeContainerMenu menu) {
        menu.addDataSlot(new BaseDataSlot(() -> direction == null ? -1 : direction.ordinal(), t -> direction = resolveDirection(t)));
    }

    private static @Nullable Direction resolveDirection(int data) {
        if (data == -1) return null;
        if (data < 0 || data >= Direction.values().length) return null;
        return Direction.values()[data];
    }
}
