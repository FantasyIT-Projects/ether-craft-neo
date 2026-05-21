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
import studio.fantasyit.ether_craft.Config;
import studio.fantasyit.ether_craft.menu.base.ether.EtherSlotContainer;
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
    private final boolean extractableInput;

    public BaseEtherContainerBlockEntity(BlockEntityType<?> type, BlockPos worldPosition, BlockState blockState, int[] slots) {
        if (slots.length != 3)
            throw new IllegalArgumentException("Invalid slots");
        this(type, worldPosition, blockState, slots[0], slots[1], slots[2]);
    }

    public BaseEtherContainerBlockEntity(BlockEntityType<?> type, BlockPos worldPosition, BlockState blockState, int input, int internal, int outputs) {
        this(type, worldPosition, blockState, input, internal, outputs, false);
    }

    public BaseEtherContainerBlockEntity(BlockEntityType<?> type, BlockPos worldPosition, BlockState blockState, int input, int internal, int outputs, boolean extractableInput) {
        super(type, worldPosition, blockState);
        this.input = input;
        this.internal = internal;
        this.output = outputs;
        this.extractableInput = extractableInput;
        inputContainer = new SimpleNotifyContainer(input, this);
        internalContainer = new SimpleNotifyContainer(internal, this);
        outputContainer = new SimpleNotifyContainer(outputs, this);
        container = new CompoundContainer(inputContainer, new CompoundContainer(internalContainer, outputContainer));
        handler = VanillaContainerWrapper.of(container);
        etherContainer = new EtherSlotContainer(this);
        etherJournal = new EtherJournal(this);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        input.read("content", ItemStack.OPTIONAL_CODEC.listOf()).ifPresent(l ->
                ContainerOps.fillContainerByItemList(container, l));
        super.loadAdditional(input);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        output.store("content", ItemStack.OPTIONAL_CODEC.listOf(), ContainerOps.containerToItemList(container));
        super.saveAdditional(output);
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
            receiveEtherNoUpdate((long) amount * Config.etherConvert);
            return amount;
        }
        if (index >= input)
            return 0;
        return handler.insert(index, resource, amount, transaction);
    }

    @Override
    public int extract(int index, ItemResource resource, int amount, @NotNull TransactionContext transaction) {
        if (resource.is(ItemRegistry.ETHER)) {
            return 0;
        }
        if (index < input && !extractableInput)
            return 0;
        if (index >= input && index < input + internal)
            return 0;
        return handler.extract(index, resource, amount, transaction);
    }
}
