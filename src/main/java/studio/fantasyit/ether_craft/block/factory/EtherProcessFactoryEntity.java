package studio.fantasyit.ether_craft.block.factory;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DynamicOps;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2i;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.block.base.*;
import studio.fantasyit.ether_craft.factory.EtherProcessRecipeManager;
import studio.fantasyit.ether_craft.factory.EtherProcessWorkingChip;
import studio.fantasyit.ether_craft.factory.EtherProcessorRecipeUtil;
import studio.fantasyit.ether_craft.factory.FactoryLevelDef;
import studio.fantasyit.ether_craft.menu.factory.EtherProcessFactoryContainerMenu;
import studio.fantasyit.ether_craft.network.s2c.SyncBlockNameS2C;
import studio.fantasyit.ether_craft.recipe.factory.EtherFactoryRecipeInput;
import studio.fantasyit.ether_craft.recipe.factory.multistep.EtherFactoryMultiStepInput;
import studio.fantasyit.ether_craft.recipe.factory.multistep.MultiStepMatchIO;
import studio.fantasyit.ether_craft.register.DataComponentRegistry;
import studio.fantasyit.ether_craft.register.ItemRegistry;
import studio.fantasyit.ether_craft.register.Tags;

import java.util.*;
import java.util.function.Consumer;

import static studio.fantasyit.ether_craft.register.BlockEntityRegistry.ETHER_PROCESS_FACTORY_ENTITY;

public class EtherProcessFactoryEntity extends BaseEtherContainerBlockEntity implements EtherContainer, MenuProvider, ITickable, IWorldRenderBE {
    public static final int MAX_PROGRESS = 100;
    public final int ROWS;
    public final int COLS;
    public int[] processingProgress;
    public ItemFilter[] filters;
    public SimpleContainer possibleResults;
    // 配方数据
    public MultiStepMatchIO[] processingRecipes;
    public EtherFactoryMultiStepInput[] processingInputs;
    public @Nullable EtherProcessWorkingChip[][] slotChips;
    //渲染用数据
    public int[][] pathBelongings;
    public int[][] pathDepth;
    public int[][] pathDirection;
    public int[] pathMaxDepth;
    //机制数据
    public int[][] currentEther;
    public int pressureBonus = 1;
    public int leak = 0;
    boolean markUpdate = false;
    public String name = "";
    public Component toRenderName = null;

    public static int[] getSlots(BlockState state) {
        FactoryLevelDef d = FactoryLevelDef.getByLevel(state.getValue(EtherProcessFactoryBlock.LEVEL));
        return new int[]{d.slotSize().y, d.slotSize().y * d.slotSize().x, d.slotSize().y};
    }

    public EtherProcessFactoryEntity(BlockPos worldPosition, BlockState blockState) {
        super(ETHER_PROCESS_FACTORY_ENTITY.get(), worldPosition, blockState, getSlots(blockState));
        Vector2i vector2i = getLevelDef().slotSize();
        ROWS = vector2i.y;
        COLS = vector2i.x;
        filters = new ItemFilter[ROWS];
        for (int i = 0; i < ROWS; i++) {
            filters[i] = new ItemFilter(9, this::setChanged);
            filters[i].whitelist = true;
        }
        possibleResults = new SimpleContainer(ROWS);
        processingProgress = new int[ROWS];
        processingRecipes = new MultiStepMatchIO[ROWS];
        processingInputs = new EtherFactoryMultiStepInput[ROWS];
        slotChips = new EtherProcessWorkingChip[ROWS][COLS];
        pathBelongings = new int[ROWS][COLS];
        pathDepth = new int[ROWS][COLS];
        pathDirection = new int[ROWS][COLS];
        pathMaxDepth = new int[ROWS];
        currentEther = new int[ROWS][COLS];
    }

    public FactoryLevelDef getLevelDef() {
        return FactoryLevelDef.getByLevel(getBlockState().getValue(EtherProcessFactoryBlock.LEVEL));
    }


    @Override
    public void setChanged() {
        super.setChanged();
        if (level != null && !level.isClientSide())
            markUpdate = true;
    }

    int looperIndex = 0;

    public void updateChips() {
        EtherContainer etherCap = EtherContainer.ETHER_CONTAINER.getCapability(level, getBlockPos(), getBlockState(), this, null);
        if (etherCap == null)
            return;
        for (int i = 0; i < ROWS; i++) {
            for (int j = 0; j < COLS; j++) {
                ItemStack itemStack = internalContainer.getItem(i * ROWS + j);
                @Nullable EtherProcessWorkingChip originalChip = slotChips[i][j];
                if (originalChip != null && !itemStack.isEmpty() && isSameChip(itemStack, originalChip.item))
                    continue;
                if (itemStack.isEmpty() && originalChip == null)
                    continue;
                long o = 0;
                if (originalChip != null)
                    o = originalChip.ether;
                if (itemStack.is(Tags.PROCESS_CHIP))
                    slotChips[i][j] = new EtherProcessWorkingChip(itemStack, o);
                else if (!itemStack.isEmpty())
                    slotChips[i][j] = EtherProcessWorkingChip.DUMMY;
                else
                    slotChips[i][j] = null;
            }
        }
        //向所有的以太消耗组件填充以太（填充机制：轮询优先填满）

        for (int k = 0; k < pressureBonus; k++) {
            for (int i = 0; i < COLS * ROWS; ++i) {
                if (getEther() == 0)
                    break;
                int idx = looperIndex;
                looperIndex = (looperIndex + 1) % (COLS * ROWS);
                int row = idx / COLS;
                int col = idx % COLS;
                if (slotChips[row][col] == null)
                    continue;
                etherCap.setEther(slotChips[row][col].addEther(etherCap.getEther()));
            }
            for (int i = 0; i < ROWS; i++) {
                for (int j = 0; j < COLS; j++) {
                    if (slotChips[i][j] == null)
                        continue;
                    slotChips[i][j].tick();
                }
            }
        }
        long totalCapacity = 0;
        for (int i = 0; i < ROWS; i++) {
            for (int j = 0; j < COLS; j++) {
                if (slotChips[i][j] == null)
                    continue;
                totalCapacity += slotChips[i][j].maxEther;
                currentEther[i][j] = Math.toIntExact(Math.min(slotChips[i][j].ether, Integer.MAX_VALUE));
            }
        }
        if (totalCapacity > 0) {
            long remaining = getEther() - totalCapacity;

            long remainingMultiplier = remaining / totalCapacity / 10;
            if (remainingMultiplier > 0) {
                pressureBonus = (int) (Math.log(remainingMultiplier + 1) / Math.log(2.0));
            } else {
                pressureBonus = 1;
            }
        } else {
            pressureBonus = 1;
        }
    }

    public void updateRecipe(ServerLevel level) {
        EtherProcessorRecipeUtil.FactoryStructure factoryStructure = EtherProcessorRecipeUtil.processFactoryInput(ROWS, COLS, inputContainer, slotChips);
        leak = factoryStructure.leakingSpeed;
        boolean[] hasRecipe = new boolean[ROWS];
        for (int i = 0; i < factoryStructure.recipes.size(); i++) {
            Optional<MultiStepMatchIO> recipeFor = EtherProcessRecipeManager.getRecipe(level, level.recipeAccess(), factoryStructure.recipes.get(i));
            Integer outputId = factoryStructure.recipes.get(i).outputI();
            if (recipeFor.isPresent()) {
                MultiStepMatchIO currentRecipe = recipeFor.get();
                hasRecipe[outputId] = true;
                if (processingRecipes[outputId] != null && processingRecipes[outputId] == currentRecipe) {
                    continue;
                }
                processingRecipes[outputId] = currentRecipe;
                processingProgress[outputId] = 0;
                processingInputs[outputId] = factoryStructure.recipes.get(i);
                possibleResults.setItem(outputId, currentRecipe.outputs().stream().findFirst().orElse(ItemStack.EMPTY));
            } else {
                hasRecipe[outputId] = false;
            }
        }
        for (int i = 0; i < ROWS; i++) {
            if (!hasRecipe[i]) {
                processingRecipes[i] = null;
                processingProgress[i] = 0;
                processingInputs[i] = null;
                possibleResults.setItem(i, ItemStack.EMPTY);
            }
        }

        for (int i = 0; i < ROWS; i++)
            pathMaxDepth[i] = 0;
        for (int i = 0; i < ROWS; i++)
            for (int j = 0; j < COLS; j++)
                pathBelongings[i][j] = -1;
        for (int i = 0; i < ROWS; i++) {
            if (processingInputs[i] != null) {
                int finalI = i;
                pathMaxDepth[i] = processingInputs[i].maxDepth();
                processingInputs[i].workingPath().forEach(v -> {
                    pathBelongings[v.pos().x][v.pos().y] = finalI;
                    pathDirection[v.pos().x][v.pos().y] = v.next();
                    pathDepth[v.pos().x][v.pos().y] = processingInputs[finalI].maxDepth() - v.depth();
                });
            }
        }
    }

    private void tickChipBehaviors() {
        for (int i = 0; i < ROWS; i++) {
            for (int j = 0; j < COLS; j++) {
                EtherProcessWorkingChip chip = slotChips[i][j];
                if (chip == null || chip.behavior == null)
                    continue;
                chip.behavior.onTick(chip, this);
                if (chip.destroyed) {
                    internalContainer.setItem(i * ROWS + j, ItemStack.EMPTY);
                    internalContainer.setChanged();
                    slotChips[i][j] = null;
                    setChanged();
                }
            }
        }
    }

    private static boolean isSameChip(ItemStack a, ItemStack b) {
        if (a.getItem() != b.getItem())
            return false;
        return Objects.equals(a.get(DataComponentRegistry.CHIP_ID), b.get(DataComponentRegistry.CHIP_ID));
    }

    @Override
    public void tickServer() {
        updateChips();
        tickChipBehaviors();
        boolean changed = false;
        for (int i = 0; i < this.processingRecipes.length; i++) {
            if (this.processingRecipes[i] != null) {
                if (this.processingInputs[i].relevantChip().stream().anyMatch(item -> (!item.canWork()))) {
                    processingProgress[i] = 0;
                } else if (processingProgress[i] < MAX_PROGRESS) {
                    processingProgress[i] += pressureBonus;
                } else {
                    consumeAndPlaceOutput(i);
                    processingProgress[i] = 0;
                }
                changed = true;
            }
        }
        if (changed) {
            setChanged();
        }
        if (markUpdate) {
            markUpdate = false;
            updateRecipe((ServerLevel) level);
            if (!name.isEmpty() && level instanceof ServerLevel sl)
                PacketDistributor.sendToPlayersInDimension(sl, new SyncBlockNameS2C(getBlockPos(), name));
        }
        if (leak > 0) {
            extractEther(leak * 20L * pressureBonus);
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        input.read("name", Codec.STRING).ifPresent(n -> {
            name = n;
            toRenderName = name.isEmpty() ? null : Component.literal(name);
        });
        input.read("progress", Codec.INT.listOf()).ifPresent(l -> {
            for (int i = 0; i < l.size(); i++)
                processingProgress[i] = l.get(i);
        });
        super.loadAdditional(input);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        output.store("name", Codec.STRING, name);
        output.store("progress", Codec.INT.listOf(), Arrays.stream(processingProgress).boxed().toList());
        super.saveAdditional(output);
    }

    @Override
    public Component getDisplayName() {
        return Component.empty();
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int i, Inventory inventory, Player player) {
        return new EtherProcessFactoryContainerMenu(i, player, this.worldPosition);
    }

    @Override
    public int insert(int index, ItemResource resource, int amount, @NotNull TransactionContext transaction) {
        if (!isValid(index, resource))
            return 0;
        return super.insert(index, resource, amount, transaction);
    }

    @Override
    public boolean isValid(int index, ItemResource resource) {
        if (!resource.is(ItemRegistry.ETHER) && index < ROWS) {
            if (!filters[index].isEmpty() && !filters[index].accepts(resource))
                return false;
            if (!internalContainer.getItem(index * COLS).isEmpty())
                return false;
        }
        return super.isValid(index, resource);
    }

    @Override
    public int extract(int index, ItemResource resource, int amount, @NotNull TransactionContext transaction) {
        if (index < ROWS && !filters[index].accepts(resource) && !resource.is(ItemRegistry.ETHER))
            return 0;
        return super.extract(index, resource, amount, transaction);
    }

    @Override
    public @Nullable Component getRenderName() {
        return toRenderName;
    }

    @Override
    public void setRenderName(@Nullable Component name) {
        toRenderName = name;
    }

    public static void appendTooltipLines(ItemStack stack, int level, Item.TooltipContext ctx, TooltipFlag flag, Consumer<Component> tooltipAdder) {
        TypedEntityData<?> beData = stack.get(DataComponents.BLOCK_ENTITY_DATA);
        if (beData == null) return;

        CompoundTag tag = beData.copyTagWithoutId();
        if (tag.isEmpty()) return;

        tag.getString("name").ifPresent(name -> {
            if (!name.isEmpty())
                tooltipAdder.accept(Component.literal(name).withStyle(ChatFormatting.ITALIC, ChatFormatting.GRAY));
        });

        Tag contentTag = tag.get("content");
        if (contentTag != null) {
            DynamicOps<Tag> ops = ctx.registries().createSerializationContext(NbtOps.INSTANCE);
            List<ItemStack> content = ItemStack.OPTIONAL_CODEC.listOf()
                    .parse(ops, contentTag)
                    .result()
                    .orElse(List.of());

            FactoryLevelDef def = FactoryLevelDef.getByLevel(level);
            int rows = def.slotSize().y;
            int cols = def.slotSize().x;
            int internalCount = rows * cols;
            int inputEnd = rows;
            int outputStart = inputEnd + internalCount;
            int outputEnd = outputStart + rows;

            List<String> inputNames = new ArrayList<>();
            for (int i = 0; i < Math.min(inputEnd, content.size()); i++) {
                ItemStack item = content.get(i);
                if (!item.isEmpty())
                    inputNames.add(item.getHoverName().getString() + " x" + item.getCount());
            }
            if (!inputNames.isEmpty())
                tooltipAdder.accept(Component.translatable("tooltip.ether_craft.factory.input",
                        String.join(", ", inputNames)).withStyle(ChatFormatting.GOLD));

            List<String> outputNames = new ArrayList<>();
            for (int i = outputStart; i < Math.min(outputEnd, content.size()); i++) {
                ItemStack item = content.get(i);
                if (!item.isEmpty())
                    outputNames.add(item.getHoverName().getString() + " x" + item.getCount());
            }
            if (!outputNames.isEmpty())
                tooltipAdder.accept(Component.translatable("tooltip.ether_craft.factory.output",
                        String.join(", ", outputNames)).withStyle(ChatFormatting.GOLD));
        }
    }

    private boolean consumeAndPlaceOutput(int row) {
        MultiStepMatchIO recipe = processingRecipes[row];
        EtherFactoryMultiStepInput input = processingInputs[row];

        if (!input.relevantChip().stream().allMatch(EtherProcessWorkingChip::canConsume))
            return false;

        ItemStack[] results = new ItemStack[recipe.outputs().size()];
        for (int j = 0; j < recipe.outputs().size(); j++) {
            results[j] = recipe.outputs().get(j);
        }

        int[] matchingRecipes = EtherProcessorRecipeUtil.getToCostCountByInputAndIngredient(
                input.inputs(),
                recipe.inputs()
        );
        if (Arrays.stream(matchingRecipes).anyMatch(t -> t == -1))
            return false;

        if (!tryPlaceOutputs(row, results))
            return false;

        for (int j = 0; j < input.inputIds().size(); j++) {
            int cNum = recipe.inputs().get(matchingRecipes[j]).count();
            inputContainer.getItem(input.inputIds().get(j)).shrink(cNum);
            inputContainer.setChanged();
        }

        for (EtherProcessWorkingChip c : input.relevantChip()) {
            c.consume();
        }

        return true;
    }

    private boolean tryPlaceOutputs(int startSlot, ItemStack[] results) {
        SimpleContainer sim = new SimpleContainer(ROWS);
        for (int s = 0; s < ROWS; s++)
            sim.setItem(s, outputContainer.getItem(s).copy());

        for (ItemStack result : results) {
            if (result.isEmpty())
                continue;
            boolean placed = false;
            for (int attempt = 0; attempt < ROWS; attempt++) {
                int slot = (startSlot + attempt) % ROWS;
                ItemStack existing = sim.getItem(slot);
                if (existing.isEmpty()) {
                    sim.setItem(slot, result.copy());
                    placed = true;
                    break;
                } else if (ItemStack.isSameItemSameComponents(existing, result)
                        && existing.getCount() + result.getCount() <= result.getMaxStackSize()) {
                    existing.grow(result.getCount());
                    placed = true;
                    break;
                }
            }
            if (!placed)
                return false;
        }

        for (int s = 0; s < ROWS; s++)
            outputContainer.setItem(s, sim.getItem(s));
        outputContainer.setChanged();
        return true;
    }

    private static ItemStack findCopySource(ItemStack outputTemplate, EtherFactoryRecipeInput input) {
        if (!isContainerBlock(outputTemplate))
            return null;
        return input.inputs.stream()
                .filter(EtherProcessFactoryEntity::isContainerBlock)
                .findFirst().orElse(null);
    }

    private static boolean isContainerBlock(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof BlockItem))
            return false;
        Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (!EtherCraft.MODID.equals(id.getNamespace()))
            return false;
        String path = id.getPath();
        return path.startsWith("ether_adapt_node") || path.startsWith("ether_process_factory");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        try (ProblemReporter.ScopedCollector reporter = new ProblemReporter.ScopedCollector(this.problemPath(), LogUtils.getLogger())) {
            TagValueOutput output = TagValueOutput.createWithContext(reporter, registries);
            List<ItemStack> results = new ArrayList<>();
            for (int i = 0; i < ROWS; i++)
                results.add(possibleResults.getItem(i));
            output.store("results", ItemStack.OPTIONAL_CODEC.listOf(), results);
            output.store("progress", Codec.INT.listOf(), Arrays.stream(processingProgress).boxed().toList());
            List<Integer> flattenedPath = new ArrayList<>();
            for (int i = 0; i < ROWS; i++)
                for (int j = 0; j < COLS; j++)
                    flattenedPath.add(pathBelongings[i][j]);
            output.store("pathBelongings", Codec.INT.listOf(), flattenedPath);
            List<Integer> flattenedEther = new ArrayList<>();
            for (int i = 0; i < ROWS; i++)
                for (int j = 0; j < COLS; j++)
                    flattenedEther.add(currentEther[i][j]);
            output.store("currentEther", Codec.INT.listOf(), flattenedEther);
            output.putInt("pressureBonus", pressureBonus);
            output.putInt("leak", leak);
            output.putString("name", name);
            return output.buildResult();
        }
    }

    @Override
    public void handleUpdateTag(ValueInput input) {
        input.read("results", ItemStack.OPTIONAL_CODEC.listOf()).ifPresent(l -> {
            for (int i = 0; i < Math.min(l.size(), ROWS); i++)
                possibleResults.setItem(i, l.get(i));
        });
        input.read("progress", Codec.INT.listOf()).ifPresent(l -> {
            for (int i = 0; i < Math.min(l.size(), processingProgress.length); i++)
                processingProgress[i] = l.get(i);
        });
        input.read("pathBelongings", Codec.INT.listOf()).ifPresent(l -> {
            for (int i = 0; i < Math.min(l.size(), ROWS * COLS); i++)
                pathBelongings[i / COLS][i % COLS] = l.get(i);
        });
        input.read("currentEther", Codec.INT.listOf()).ifPresent(l -> {
            for (int i = 0; i < Math.min(l.size(), ROWS * COLS); i++)
                currentEther[i / COLS][i % COLS] = l.get(i);
        });
        pressureBonus = input.read("pressureBonus", Codec.INT).orElse(pressureBonus);
        leak = input.read("leak", Codec.INT).orElse(leak);
        name = input.getStringOr("name", "");
        if (!name.isEmpty())
            setRenderName(Component.literal(name));
    }
}
