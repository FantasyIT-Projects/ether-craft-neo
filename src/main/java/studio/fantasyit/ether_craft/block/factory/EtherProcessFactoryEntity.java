package studio.fantasyit.ether_craft.block.factory;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.fantasyit.ether_craft.block.base.BaseIOBlockEntity;
import studio.fantasyit.ether_craft.block.base.EtherContainer;
import studio.fantasyit.ether_craft.menu.factory.EtherProcessFactoryContainerMenu;
import studio.fantasyit.ether_craft.recipe.factory.EtherFactoryRecipeInput;
import studio.fantasyit.ether_craft.recipe.factory.EtherProcessFactoryRecipe;
import studio.fantasyit.ether_craft.register.ItemRegistry;
import studio.fantasyit.ether_craft.register.RecipeTypeRegistry;
import studio.fantasyit.ether_craft.util.EtherProcessorRecipeUtil;

import java.util.Optional;

import static studio.fantasyit.ether_craft.register.BlockEntityRegistry.ETHER_PROCESS_FACTORY_ENTITY;

public class EtherProcessFactoryEntity extends BaseIOBlockEntity implements EtherContainer, MenuProvider {
    private static int ROWS = 9;
    private static int COLS = 9;
    public int[] processingProgress;
    public EtherProcessFactoryRecipe[] processingRecipes;
    public EtherFactoryRecipeInput[] processingInputs;
    public @Nullable EtherProcessWorkingChip[][] slotChips;

    public EtherProcessFactoryEntity(BlockPos worldPosition, BlockState blockState) {
        super(ETHER_PROCESS_FACTORY_ENTITY.get(), worldPosition, blockState, ROWS, COLS * ROWS, ROWS);
        processingProgress = new int[ROWS];
        processingRecipes = new EtherProcessFactoryRecipe[ROWS];
        processingInputs = new EtherFactoryRecipeInput[ROWS];
        slotChips = new EtherProcessWorkingChip[ROWS][COLS];
    }


    @Override
    public void setChanged() {
        super.setChanged();
        if (level != null && !level.isClientSide())
            updateRecipe((ServerLevel) level);
    }

    int looperIndex = 0;

    public void updateChips() {
        EtherContainer etherCap = EtherContainer.ETHER_CONTAINER.getCapability(level, getBlockPos(), getBlockState(), this, null);
        if (etherCap == null)
            return;
        for (int i = 0; i < ROWS; i++) {
            for (int j = 0; j < COLS; j++) {
                ItemStack itemStack = internalContainer.getItem(i * COLS + j);
                @Nullable EtherProcessWorkingChip originalChip = slotChips[i][j];
                if (originalChip != null && ItemStack.isSameItemSameComponents(itemStack, originalChip.item))
                    continue;
                if (itemStack.isEmpty() && originalChip == null)
                    continue;
                long o = 0;
                if (originalChip != null)
                    o = originalChip.ether;
                if (itemStack.is(ItemRegistry.PROCESS_CHIP_ITEM))
                    slotChips[i][j] = new EtherProcessWorkingChip(itemStack, o);
                else if (!itemStack.isEmpty())
                    slotChips[i][j] = EtherProcessWorkingChip.DUMMY;
                else
                    slotChips[i][j] = null;
            }
        }
        //向所有的以太消耗组件填充以太（填充机制：轮询优先填满）
        for (int i = 0; i < COLS * ROWS; ++i) {
            if (etherCap.getEther() == 0) break;
            int idx = (i + looperIndex) % (COLS * ROWS);
            int row = idx / COLS;
            int col = idx % COLS;
            if (slotChips[row][col] == null)
                continue;
            etherCap.setEther(slotChips[row][col].addEther(etherCap.getEther()));
            looperIndex = (looperIndex + 1) % (COLS * ROWS);
        }
    }

    public void updateRecipe(ServerLevel level) {
        EtherProcessorRecipeUtil.FactoryStructure factoryStructure = EtherProcessorRecipeUtil.processFactoryInput(9, 9, inputContainer, slotChips);
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
            } else {
                hasRecipe[outputId] = false;
            }
        }
        for (int i = 0; i < ROWS; i++) {
            if (!hasRecipe[i]) {
                processingRecipes[i] = null;
                processingProgress[i] = 0;
                processingInputs[i] = null;
            }
        }
    }

    public void tickServer() {
        updateChips();
        boolean changed = false;
        for (int i = 0; i < this.processingRecipes.length; i++) {
            if (this.processingRecipes[i] != null) {
                if (this.processingInputs[i].relevantComponent.stream().allMatch(item -> (!item.canWork()))) {
                    processingProgress[i] = 0;
                } else if (processingProgress[i] < 100) {
                    processingProgress[i]++;
                } else {
                    processingProgress[i] = 0;
                    for (int j = 0; j < processingRecipes[i].output.size(); j++) {
                        ItemStack r = processingRecipes[i].getResultItem();
                        int oCnt = outputContainer.getItem(i).getCount();
                        if (outputContainer.canPlaceItem(i, r))
                            outputContainer.setItem(i, r.copyWithCount(oCnt));
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
                }
                changed = true;
            }
        }
        if (changed) {
            setChanged();
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        input.read("progress", Codec.INT.listOf()).ifPresent(l -> {
            for (int i = 0; i < l.size(); i++)
                processingProgress[i] = l.get(i);
        });
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
        return new EtherProcessFactoryContainerMenu(i, player, this.worldPosition);
    }
}
