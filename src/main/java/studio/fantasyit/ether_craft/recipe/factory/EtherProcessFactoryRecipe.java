package studio.fantasyit.ether_craft.recipe.factory;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import studio.fantasyit.ether_craft.base.TreeLike;
import studio.fantasyit.ether_craft.util.EtherProcessorRecipeUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EtherProcessFactoryRecipe implements Recipe<@NotNull EtherFactoryRecipeInput> {
    private static final String DIRECT_INPUT = "DIRECT_INPUT";
    //树配方。保证ID[0-n]为n个输入物品，且与input下标对应
    protected final EtherProcessRecipeJson json;
    protected final TreeLike<Integer, List<Ingredient>> process;
    protected final ArrayList<Ingredient> input;
    protected final ArrayList<Ingredient> output;
    protected final Identifier res;

    public EtherProcessFactoryRecipe(Identifier res, EtherProcessRecipeJson json) {
        this.json = json;
        this.res = res;
        // Step 1: 获取输入、处理流和输出
        List<EtherProcessRecipeJson.InputEntry> inputEntries = json.input();
        List<EtherProcessRecipeJson.OutputEntry> outputEntries = json.output();
        List<EtherProcessRecipeJson.ProcessEntry> processEntries = json.process();

        // Step 2: StringId ==> IntegerId 映射
        Map<String, Integer> idMapping = new HashMap<>();
        final int[] currentId = {0};

        for (EtherProcessRecipeJson.InputEntry entry : inputEntries) {
            String id = entry.id();
            idMapping.put(id, currentId[0]++);
        }
        for (EtherProcessRecipeJson.InputEntry entry : inputEntries) {
            String id = DIRECT_INPUT + entry.id();
            idMapping.put(id, currentId[0]++);
        }
        for (EtherProcessRecipeJson.ProcessEntry entry : processEntries) {
            String id = entry.id();
            idMapping.put(id, currentId[0]++);
        }

        // Step 3: 建树
        TreeLike<Integer, List<Ingredient>> recipeTree = new TreeLike<>(currentId[0], currentId[0]);
        for (EtherProcessRecipeJson.InputEntry entry : inputEntries) {
            int id = idMapping.get(entry.id());
            int dirId = idMapping.get(DIRECT_INPUT + entry.id());
            recipeTree.addNode(id, id);
            recipeTree.addNode(dirId, dirId);
        }
        for (EtherProcessRecipeJson.ProcessEntry entry : processEntries) {
            int id = idMapping.get(entry.id());
            recipeTree.addNode(id, id);
        }

        // Step 4.1: 虚拟节点 -> 处理器列表
        Map<Integer, List<Ingredient>> vid2ProcessIngredient = new HashMap<>();
        for (EtherProcessRecipeJson.ProcessEntry entry : processEntries) {
            int id = idMapping.get(entry.id());
            vid2ProcessIngredient.put(id, entry.item()); // entry.item() 已经是 List<Ingredient>
        }
        vid2ProcessIngredient.put(currentId[0], List.of(Ingredient.of(ItemRegistry.DIRECT_INPUT_ITEM_CHIP.get())));

        // Step 4.2: 输入到 DirectInputItem 芯片的边
        for (EtherProcessRecipeJson.InputEntry entry : inputEntries) {
            int id = idMapping.get(entry.id());
            int dirId = idMapping.get(DIRECT_INPUT + entry.id());
            recipeTree.addEdge(dirId, id, List.of(Ingredient.of(ItemRegistry.DIRECT_INPUT_ITEM_CHIP.get())));
        }

        // Step 4.3: 加入边（输入和工序）
        for (EtherProcessRecipeJson.InputEntry entry : inputEntries) {
            int id = idMapping.get(DIRECT_INPUT + entry.id()); // 注意这里使用 DIRECT_INPUT + id
            String nextIdStr = entry.next();
            Integer nxtId = null;
            if (nextIdStr != null && !nextIdStr.isEmpty()) {
                nxtId = idMapping.get(nextIdStr);
            }
            if (nxtId == null) {
                nxtId = recipeTree.getRoot().value;
            }
            recipeTree.addEdge(nxtId, id, vid2ProcessIngredient.get(nxtId));
        }
        for (EtherProcessRecipeJson.ProcessEntry entry : processEntries) {
            int id = idMapping.get(entry.id());
            String nextIdStr = entry.next();
            Integer nxtId = null;
            if (nextIdStr != null && !nextIdStr.isEmpty()) {
                nxtId = idMapping.get(nextIdStr);
            }
            if (nxtId == null) {
                nxtId = recipeTree.getRoot().value;
            }
            recipeTree.addEdge(nxtId, id, vid2ProcessIngredient.get(nxtId));
        }

        // Step 5: 构建输入输出 Ingredient 列表
        ArrayList<Ingredient> inputs = new ArrayList<>();
        for (EtherProcessRecipeJson.InputEntry entry : inputEntries) {
            inputs.add(entry.item());
        }
        ArrayList<Ingredient> outputs = new ArrayList<>();
        for (EtherProcessRecipeJson.OutputEntry entry : outputEntries) {
            outputs.add(entry.item());
        }

        this.process = recipeTree;
        this.input = inputs;
        this.output = outputs;
    }

    @Override
    public boolean matches(EtherFactoryRecipeInput itemStacks, Level level) {
        return EtherProcessorRecipeUtil.isRecipeCompatible(process, input, itemStacks);
    }

    @Override
    public ItemStack assemble(EtherFactoryRecipeInput itemStacks) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean showNotification() {
        return false;
    }

    @Override
    public @NotNull String group() {
        return "";
    }

    @Override
    public RecipeSerializer<@NotNull Recipe<@NotNull EtherFactoryRecipeInput>> getSerializer() {
        return new RecipeSerializer();
    }

    @Override
    public RecipeType<? extends Recipe<EtherFactoryRecipeInput>> getType() {
        return null;
    }

    @Override
    public PlacementInfo placementInfo() {
        return null;
    }

    @Override
    public RecipeBookCategory recipeBookCategory() {
        return null;
    }
}
