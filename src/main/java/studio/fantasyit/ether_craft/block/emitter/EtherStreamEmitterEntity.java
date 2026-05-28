package studio.fantasyit.ether_craft.block.emitter;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.fantasyit.ether_craft.block.base.BaseEtherContainerBlockEntity;
import studio.fantasyit.ether_craft.block.base.EtherContainer;
import studio.fantasyit.ether_craft.block.base.ITickable;
import studio.fantasyit.ether_craft.block.node.EtherAdaptNodeBlock;
import studio.fantasyit.ether_craft.entity.stream.EtherStreamEntity;
import studio.fantasyit.ether_craft.stream.EtherStreamStorageCapability;
import studio.fantasyit.ether_craft.stream.IEtherStreamLike;

import static studio.fantasyit.ether_craft.register.BlockEntityRegistry.ETHER_STREAM_EMITTER_ENTITY;

public class EtherStreamEmitterEntity extends BaseEtherContainerBlockEntity implements EtherContainer, MenuProvider, ITickable {
    private boolean markUpdate = false;

    public EtherStreamEmitterEntity(BlockPos worldPosition, BlockState blockState) {
        super(ETHER_STREAM_EMITTER_ENTITY.get(), worldPosition, blockState, 9, 9, 0);
    }

    @Override
    public void setChanged() {
        super.setChanged();
        if (level != null && !level.isClientSide())
            markUpdate = true;
    }

    @Override
    public void tickServer() {
        if (this.getEther() > 1000) {
            if (level != null) {
                @NotNull Direction targetDirection = this.getBlockState().getValue(EtherAdaptNodeBlock.FACING);
                Vec3 dir = targetDirection.getUnitVec3().multiply(0.55f, 0.55f, 0.55f);
                IEtherStreamLike entity = EtherStreamEntity.create(
                        this.level,
                        (int) this.getEther(),
                        this.getBlockPos().getCenter().add(dir),
                        dir.multiply(0.1f, 0.1f, 0.1f)
                );
                EtherStreamStorageCapability itemStorage = new EtherStreamStorageCapability(this.inputContainer.getContainerSize());

                for (int i = 0; i < this.inputContainer.getContainerSize(); i++) {
                    try (Transaction transaction = Transaction.openRoot()) {
                        @NotNull ItemResource res = this.handler.getResource(i);
                        if (res.isEmpty()) continue;
                        int extracted = this.handler.extract(i, res, Integer.MAX_VALUE, transaction);
                        int insert = itemStorage.handler.insert(i, res, extracted, transaction);
                        if (insert == extracted)
                            transaction.commit();
                    }
                }
                entity.addCapability(itemStorage);

                this.setEther(0);
                if (entity instanceof Entity e)
                    level.addFreshEntity(e);
            }
        }
        if (markUpdate) {
            markUpdate = false;
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
    }

    @Override
    public Component getDisplayName() {
        return Component.empty();
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int i, Inventory inventory, Player player) {
        return null;
    }
}
