package studio.fantasyit.ether_craft.block.factory;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2i;
import studio.fantasyit.ether_craft.block.base.BaseEtherContainerBlockEntity;
import studio.fantasyit.ether_craft.block.base.EtherContainer;
import studio.fantasyit.ether_craft.block.base.ITickable;
import studio.fantasyit.ether_craft.block.base.ItemFilter;
import studio.fantasyit.ether_craft.menu.factory.EtherProcessFactoryContainerMenu;
import studio.fantasyit.ether_craft.recipe.factory.EtherFactoryRecipeInput;
import studio.fantasyit.ether_craft.recipe.factory.EtherProcessFactoryRecipe;
import studio.fantasyit.ether_craft.register.ItemRegistry;
import studio.fantasyit.ether_craft.register.RecipeTypeRegistry;
import studio.fantasyit.ether_craft.register.Tags;
import studio.fantasyit.ether_craft.util.EtherProcessorRecipeUtil;

import java.util.Arrays;
import java.util.Optional;

import static studio.fantasyit.ether_craft.register.BlockEntityRegistry.ETHER_PROCESS_FACTORY_ENTITY;

public class EtherProcessFactoryEntity extends BaseEtherContainerBlockEntity implements EtherContainer, MenuProvider, ITickable {
    public static final int MAX_PROGRESS = 100;
    public final int ROWS;
    public final int COLS;
    public int[] processingProgress;
    public ItemFilter[] filters;
    public SimpleContainer possibleResults;
    public EtherProcessFactoryRecipe[] processingRecipes;
    public EtherFactoryRecipeInput[] processingInputs;
    public @Nullable EtherProcessWorkingChip[][] slotChips;
    public int[][] pathBelongings;
    public int[][] currentEther;
    public int pressureBonus = 1;
    public int leak = 0;
    boolean markUpdate = false;
    public String name = "";

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
        for (int i = 0; i < ROWS; i++)
            filters[i] = new ItemFilter(9, this::setChanged);
        possibleResults = new SimpleContainer(ROWS);
        processingProgress = new int[ROWS];
        processingRecipes = new EtherProcessFactoryRecipe[ROWS];
        processingInputs = new EtherFactoryRecipeInput[ROWS];
        slotChips = new EtherProcessWorkingChip[ROWS][COLS];
        pathBelongings = new int[ROWS][COLS];
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
                if (originalChip != null && ItemStack.isSameItemSameComponents(itemStack, originalChip.item) && !itemStack.isEmpty())
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
            Optional<RecipeHolder<@NotNull EtherProcessFactoryRecipe>> recipeFor = level.recipeAccess().getRecipeFor(RecipeTypeRegistry.ETHER_PROCESS_FACTORY_RECIPE.get(),
                    factoryStructure.recipes.get(i),
                    level);
            Integer outputId = factoryStructure.recipes.get(i).outputId;
            if (recipeFor.isPresent()) {
                EtherProcessFactoryRecipe currentRecipe = recipeFor.get().value();
                hasRecipe[outputId] = true;
                if (processingRecipes[outputId] != null && processingRecipes[outputId] == currentRecipe) {
                    continue;
                }
                processingRecipes[outputId] = currentRecipe;
                processingProgress[outputId] = 0;
                processingInputs[outputId] = factoryStructure.recipes.get(i);
                possibleResults.setItem(outputId, currentRecipe.getResultItem());
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
            for (int j = 0; j < COLS; j++)
                pathBelongings[i][j] = -1;
        for (int i = 0; i < ROWS; i++) {
            if (processingInputs[i] != null) {
                int finalI = i;
                processingInputs[i].workingPath.forEach(v -> pathBelongings[v.y][v.x] = finalI);
            }
        }
    }

    @Override
    public void tickServer() {
        updateChips();
        boolean changed = false;
        for (int i = 0; i < this.processingRecipes.length; i++) {
            if (this.processingRecipes[i] != null) {
                if (this.processingInputs[i].relevantChips.stream().anyMatch(item -> (!item.canWork()))) {
                    processingProgress[i] = 0;
                } else if (processingProgress[i] < MAX_PROGRESS) {
                    processingProgress[i] += pressureBonus;
                } else {
                    processingProgress[i] = 0;
                    if (!processingInputs[i].relevantChips.stream().allMatch(EtherProcessWorkingChip::canConsume))
                        continue;
                    for (int j = 0; j < processingRecipes[i].output.size(); j++) {
                        ItemStack r = processingRecipes[i].getResultItem();
                        int oCnt = outputContainer.getItem(i).getCount();
                        int nCnt = Math.min(oCnt + r.getCount(), r.getMaxStackSize());
                        if (outputContainer.canPlaceItem(i, r))
                            outputContainer.setItem(i, r.copyWithCount(nCnt));
                    }
                    int[] matchingRecipes = EtherProcessorRecipeUtil.getToCostCountByInputAndIngredient(
                            processingInputs[i].inputs,
                            processingRecipes[i].input
                    );
                    for (int j = 0; j < processingInputs[i].inputIds.size(); j++) {
                        int cNum = processingRecipes[i].input.get(matchingRecipes[j]).count();
                        inputContainer.getItem(processingInputs[i].inputIds.get(j)).shrink(cNum);
                        inputContainer.setChanged();
                    }

                    for (EtherProcessWorkingChip c : processingInputs[i].relevantChips) {
                        c.consume();
                    }
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
        }
        if (leak > 0) {
            extractEther(leak * 20L * pressureBonus);
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        input.read("name", Codec.STRING).ifPresent(n -> name = n);
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
        if (index < ROWS && !filters[index].accepts(resource) && !resource.is(ItemRegistry.ETHER))
            return 0;
        return super.insert(index, resource, amount, transaction);
    }

    @Override
    public int extract(int index, ItemResource resource, int amount, @NotNull TransactionContext transaction) {
        if (index < ROWS && !filters[index].accepts(resource) && !resource.is(ItemRegistry.ETHER))
            return 0;
        return super.extract(index, resource, amount, transaction);
    }
}
