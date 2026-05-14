package studio.fantasyit.ether_craft.node.plugins.function;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import studio.fantasyit.ether_craft.Config;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.block.base.ItemFilter;
import studio.fantasyit.ether_craft.block.node.EtherAdaptNodeEntity;
import studio.fantasyit.ether_craft.menu.base.slot.BaseDataSlot;
import studio.fantasyit.ether_craft.menu.node.EtherAdaptNodeContainerMenu;
import studio.fantasyit.ether_craft.network.c2s.SyncScreenDataC2S;
import studio.fantasyit.ether_craft.node.plugins.base.AbstractNodePlugin;
import studio.fantasyit.ether_craft.node.filter.FilterGuiRegCommon;
import studio.fantasyit.ether_craft.node.plugins.InstalledPlugin;

import java.util.List;

public class FunctionMagnet extends AbstractNodePlugin {
    public static final Identifier ID = EtherCraft.id("magnet");
    public static final Identifier SYNC_VALUE = EtherCraft.id("magnet_function_feature/sync");
    public static final String FILTER_PREFIX = "magnet_function_feature/";
    public int centerX = 0, centerY = 0, centerZ = 0;
    public int shapeX = 0, shapeY = 0, shapeZ = 0;
    public ItemFilter filter;

    public FunctionMagnet(EtherAdaptNodeEntity nodeEntity, InstalledPlugin installedId) {
        super(nodeEntity, installedId);
        filter = new ItemFilter(21, nodeEntity::setChanged);
    }

    @Override
    public void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        filter.deserialize(input);
        centerX = input.getIntOr("centerX", 0);
        centerY = input.getIntOr("centerY", 1);
        centerZ = input.getIntOr("centerZ", 0);
        shapeX = input.getIntOr("shapeX", 1);
        shapeY = input.getIntOr("shapeY", 1);
        shapeZ = input.getIntOr("shapeZ", 1);
    }

    @Override
    public void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        filter.serialize(output);
        output.putInt("centerX", centerX);
        output.putInt("centerY", centerY);
        output.putInt("centerZ", centerZ);
        output.putInt("shapeX", shapeX);
        output.putInt("shapeY", shapeY);
        output.putInt("shapeZ", shapeZ);
    }

    @Override
    public void tick() {
        if (shapeX == 0 || shapeY == 0 || shapeZ == 0)
            return;
        if (shapeX + shapeY + shapeZ == 1 && centerX == 0 && centerY == 0 && centerZ == 0)
            return;
        if (nodeEntity.getLevel() != null) {
            List<ItemEntity> ie = nodeEntity.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(nodeEntity.getBlockPos()).move(centerX, centerY, centerZ).inflate(shapeX, shapeY, shapeZ));
            for (ItemEntity itemEntity : ie) {
                if (nodeEntity.getEther() < Config.nodeMagnetConsumePreStack)
                    break;
                ItemResource res = ItemResource.of(itemEntity.getItem());
                if (filter.accepts(res)) {
                    int count = itemEntity.getItem().getCount();
                    try (Transaction t = Transaction.openRoot()) {
                        int insert = nodeEntity.insert(res, count, t);
                        if (insert != 0) {
                            itemEntity.getItem().shrink(insert);
                            if (itemEntity.getItem().isEmpty())
                                itemEntity.discard();
                            nodeEntity.extractEther(Config.nodeMagnetConsumePreStack);
                            t.commit();
                        }
                    }
                }
            }
        }
    }

    @Override
    public void registerSlots(EtherAdaptNodeContainerMenu menu) {
        super.registerSlots(menu);
        menu.addDataSlot(new BaseDataSlot(() -> centerX, t -> centerX = t));
        menu.addDataSlot(new BaseDataSlot(() -> centerY, t -> centerY = t));
        menu.addDataSlot(new BaseDataSlot(() -> centerZ, t -> centerZ = t));
        menu.addDataSlot(new BaseDataSlot(() -> shapeX, t -> shapeX = t));
        menu.addDataSlot(new BaseDataSlot(() -> shapeY, t -> shapeY = t));
        menu.addDataSlot(new BaseDataSlot(() -> shapeZ, t -> shapeZ = t));
        FilterGuiRegCommon.slots(menu, filter);
    }

    @Override
    public void syncScreenData(SyncScreenDataC2S message) {
        FilterGuiRegCommon.sync(message, filter, FILTER_PREFIX);
        if (message.id().equals(SYNC_VALUE)) {
            switch (message.index()) {
                case 0 -> centerX = message.data();
                case 1 -> centerY = message.data();
                case 2 -> centerZ = message.data();
                case 3 -> shapeX = message.data();
                case 4 -> shapeY = message.data();
                case 5 -> shapeZ = message.data();
            }
        }
    }
}
