package studio.fantasyit.ether_craft.block.factory;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import studio.fantasyit.ether_craft.block.base.BaseIOBlockEntity;
import studio.fantasyit.ether_craft.recipe.factory.EtherFactoryRecipeInput;
import studio.fantasyit.ether_craft.recipe.factory.EtherProcessFactoryRecipe;
import studio.fantasyit.ether_craft.util.EtherProcessorRecipeUtil;

import java.util.List;
import java.util.Optional;

public class EtherProcessFactoryEntity extends BaseIOBlockEntity {
    private static int OUTPUT_NUM = 9;
    public int[] processingProgress;
    public EtherProcessFactoryRecipe[] processingRecipes;
    public EtherFactoryRecipeInput[] processingInputs;

    public EtherProcessFactoryEntity(BlockEntityType<?> type, BlockPos worldPosition, BlockState blockState) {
        super(type, worldPosition, blockState, OUTPUT_NUM, OUTPUT_NUM * OUTPUT_NUM, OUTPUT_NUM);
        processingProgress = new int[OUTPUT_NUM];
        processingRecipes = new EtherProcessFactoryRecipe[OUTPUT_NUM];
        processingInputs = new EtherFactoryRecipeInput[OUTPUT_NUM];
    }


    @Override
    public void setChanged() {
        super.setChanged();
        if (level != null && !level.isClientSide())
            updateRecipe((ServerLevel) level);
    }

    public void updateRecipe(ServerLevel level) {
        EtherProcessorRecipeUtil.FactoryStructure factoryStructure = EtherProcessorRecipeUtil.processFactoryInput(inputHandler, 9, 9);
        boolean[] hasRecipe = new boolean[OUTPUT_NUM];
        for (int i = 0; i < factoryStructure.recipes.size(); i++) {
            Optional<EtherProcessFactoryRecipe> recipeFor = level.recipeAccess().getRecipeFor(RecipeTypeRegistry.ETHER_PROCESS_FACTORY_RECIPE.get(),
                    factoryStructure.recipes.get(i),
                    level);
            Integer outputId = factoryStructure.recipes.get(i).outputId;
            if (recipeFor.isPresent()) {
                EtherProcessFactoryRecipe currentRecipe = recipeFor.get();
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
        for (int i = 0; i < OUTPUT_NUM; i++) {
            if (!hasRecipe[i]) {
                processingRecipes[i] = null;
                processingProgress[i] = 0;
                processingInputs[i] = null;
            }
        }
    }


    @Override
    public void tickServer() {
        tickAllComponents();
        boolean changed = false;
        for (int i = 0; i < this.processingRecipes.length; i++) {
            if (this.processingRecipes[i] != null) {
                if (this.processingInputs[i].relevantComponent.stream().allMatch(item -> (!item.canWork()))) {
                    processingProgress[i] = 0;
                } else if (processingProgress[i] < 100) {
                    processingProgress[i]++;
                } else {
                    processingProgress[i] = 0;
                    try (Transaction transaction = Transaction.openRoot()) {
                        for (int j = 0; j < processingRecipes[i].output.size(); j++) {
                            ItemStack r = processingRecipes[i].getResultItem();
                            outputHandler.insert(i, ItemResource.of(r), r.getCount(), transaction);
                        }
                        int[] matchingRecipes = EtherProcessorRecipeUtil.getToCostCountByInputAndIngredient(
                                processingInputs[i].inputs,
                                processingRecipes[i].input
                        );
                        for (int j = 0; j < processingInputs[i].inputIds.size(); j++) {
                            ItemResource r = inputHandler.getResource(processingInputs[i].inputIds.get(j));
                            int cNum = processingRecipes[i].input.get(matchingRecipes[j]).count();
                            inputHandler.extract(processingInputs[i].inputIds.get(j), r,cNum, transaction);
                        }
                        transaction.commit();
                    }
                    processingInputs[i].relevantComponent.forEach(BaseEtherConsumeComponentItem::consume);
                }
                changed = true;
            }
        }
        if (changed) {
            setChanged();
        }
    }

    @Override
    protected void saveAdditional(CompoundTag p_187471_) {
        super.saveAdditional(p_187471_);
        p_187471_.putIntArray("processingProgress", processingProgress);
    }

    @Override
    public void load(CompoundTag p_155245_) {
        super.load(p_155245_);
        if (level != null && !level.isClientSide) {
            updateRecipe(level);
            processingProgress = p_155245_.getIntArray("processingProgress");
            for (int i = 0; i < processingProgress.length; i++) {
                if (processingRecipes[i] == null) processingProgress[i] = 0;
            }
        }
    }

    int looperIndex = 0;

    protected void tickAllComponents() {
        for (int i = 0; i < OUTPUT_NUM * OUTPUT_NUM; ++i) {
            if (inputHandler.getStackInSlot(OUTPUT_NUM + i).getItem() instanceof BaseEtherConsumeComponentItem componentItem) {
                componentItem.tick();
            }
        }
        //向所有的以太消耗组件填充以太（填充机制：轮询优先填满）
        for (int i = 0; i < COLS * ROWS; ++i) {
            if (factory.ether == 0) break;
            if (factory.input.getStackInSlot(ROWS + looperIndex).getItem() instanceof BaseEtherConsumeComponentItem componentItem) {
                factory.ether = componentItem.addEther(factory.ether);
            }
            looperIndex = (looperIndex + 1) % (COLS * ROWS);
        }
    }
}
