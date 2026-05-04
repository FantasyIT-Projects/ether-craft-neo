package studio.fantasyit.ether_craft.block.base;

import net.minecraft.core.BlockPos;
import net.minecraft.world.CompoundContainer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.VanillaContainerWrapper;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jetbrains.annotations.NotNull;
import studio.fantasyit.ether_craft.register.ItemRegistry;
import studio.fantasyit.ether_craft.util.ContainerOps;

public class BaseEtherContainerBlockEntity extends BlockEntity implements ResourceHandler<@NotNull ItemResource>, EtherContainer {
    public final SimpleContainer inputContainer;
    public final SimpleContainer internalContainer;
    public final SimpleContainer outputContainer;
    public final EtherSlotContainer etherContainer;
    public final CompoundContainer container;
    public final ResourceHandler<@NotNull ItemResource> handler;
    public final int input;
    public final int internal;
    public final int output;
    private final SnapshotJournal<@NotNull Long> etherJournal;

    public BaseEtherContainerBlockEntity(BlockEntityType<?> type, BlockPos worldPosition, BlockState blockState, int input, int internal, int outputs) {
        super(type, worldPosition, blockState);
        this.input = input;
        this.internal = internal;
        this.output = outputs;
        inputContainer = new SimpleNotifyContainer(input, this);
        internalContainer = new SimpleNotifyContainer(internal, this);
        outputContainer = new SimpleNotifyContainer(outputs, this);
        container = new CompoundContainer(inputContainer, new CompoundContainer(internalContainer, outputContainer));
        handler = VanillaContainerWrapper.of(container);
        etherContainer = new EtherSlotContainer(this);
        etherJournal = new SnapshotJournal<Long>() {
            @Override
            protected Long createSnapshot() {
                return getEther();
            }

            @Override
            protected void revertToSnapshot(Long snapshot) {
                setEther(snapshot);
            }

            @Override
            protected void onRootCommit(Long originalState) {
                if (originalState != getEther())
                    syncClient();
            }
        };
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        input.read("content", ItemStack.OPTIONAL_CODEC.listOf()).ifPresent(l ->
                ContainerOps.fillContainerByItemList(container, l));
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        output.store("content", ItemStack.OPTIONAL_CODEC.listOf(), ContainerOps.containerToItemList(container));
    }

    @Override
    public int size() {
        return handler.size();
    }

    @Override
    public ItemResource getResource(int index) {
        return handler.getResource(index);
    }

    @Override
    public long getAmountAsLong(int index) {
        return handler.getAmountAsLong(index);
    }

    @Override
    public long getCapacityAsLong(int index, ItemResource resource) {
        return handler.getCapacityAsLong(index, resource);
    }

    @Override
    public boolean isValid(int index, ItemResource resource) {
        return handler.isValid(index, resource);
    }

    @Override
    public int insert(int index, ItemResource resource, int amount, @NotNull TransactionContext transaction) {
        if (resource.is(ItemRegistry.ETHER)) {
            etherJournal.updateSnapshots(transaction);
            receiveEtherNoUpdate(amount * 1000L);
            return amount;
        }
        return handler.insert(resource, amount, transaction);
    }

    @Override
    public int extract(int index, ItemResource resource, int amount, @NotNull TransactionContext transaction) {
        if (resource.is(ItemRegistry.ETHER)) {
            return 0;
        }
        return handler.extract(resource, amount, transaction);
    }
}
