package studio.fantasyit.ether_craft.recipe.factory;

import com.mojang.serialization.MapCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.fantasyit.ether_craft.base.TreeLike;
import studio.fantasyit.ether_craft.recipe.DelayedIngredient;
import studio.fantasyit.ether_craft.register.ItemRegistry;
import studio.fantasyit.ether_craft.register.RecipeSerializerRegistry;
import studio.fantasyit.ether_craft.register.RecipeTypeRegistry;
import studio.fantasyit.ether_craft.factory.EtherProcessorRecipeUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EtherProcessFactoryRecipe implements Recipe<@NotNull EtherFactoryRecipeInput> {
    private static final String DIRECT_INPUT = "DIRECT_INPUT";
    //树配方。保证ID[0-n]为n个输入物品，且与input下标对应
    public final EtherProcessRecipeJson json;
    public final TreeLike<Integer, List<DelayedIngredient>> process;
    public final List<SizedIngredient> input;
    public final List<ItemStackTemplate> output;

    public static MapCodec<EtherProcessFactoryRecipe> CODEC = EtherProcessRecipeJson.MAP_CODEC.xmap(
            EtherProcessFactoryRecipe::new,
            t -> t.json
    );
    public static StreamCodec<RegistryFriendlyByteBuf, EtherProcessFactoryRecipe> STREAM_CODEC =
            StreamCodec.composite(
                    EtherProcessRecipeJson.STREAM_CODEC,
                    t -> t.json,
                    EtherProcessFactoryRecipe::new
            );


    public EtherProcessFactoryRecipe(EtherProcessRecipeJson json) {
        this.json = json;
        // Step 1: 获取输入、处理流和输出
        List<EtherProcessRecipeJson.InputEntry> inputEntries = json.input();
        EtherProcessRecipeJson.OutputEntry outputEntry = json.output();
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
        TreeLike<Integer, List<DelayedIngredient>> recipeTree = new TreeLike<>(currentId[0], currentId[0]);
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
        Map<Integer, List<DelayedIngredient>> vid2ProcessIngredient = new HashMap<>();
        for (EtherProcessRecipeJson.ProcessEntry entry : processEntries) {
            int id = idMapping.get(entry.id());
            vid2ProcessIngredient.put(id, entry.delayedItem()); // entry.item() 已经是 List<Ingredient>
        }
        vid2ProcessIngredient.put(currentId[0], List.of(DelayedIngredient.of(ItemRegistry.DIRECT_INPUT_ITEM_CHIP.get())));

        // Step 4.2: 输入到 DirectInputItem 芯片的边
        for (EtherProcessRecipeJson.InputEntry entry : inputEntries) {
            int id = idMapping.get(entry.id());
            int dirId = idMapping.get(DIRECT_INPUT + entry.id());
            recipeTree.addEdge(dirId, id, List.of(DelayedIngredient.of(ItemRegistry.DIRECT_INPUT_ITEM_CHIP.get())));
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
        ArrayList<SizedIngredient> inputs = new ArrayList<>();
        for (EtherProcessRecipeJson.InputEntry entry : inputEntries) {
            inputs.add(entry.item());
        }
        List<ItemStackTemplate> outputs = outputEntry.item();

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

    public ItemStack getResultItem() {
        return getResultItem(null);
    }

    public ItemStack getResultItem(int index) {
        return getResultItem(index, null);
    }

    public ItemStack getResultItem(@Nullable ItemStack baseStack) {
        return getResultItem(0, baseStack);
    }

    public ItemStack getResultItem(int index, @Nullable ItemStack baseStack) {
        if (index >= 0 && index < output.size()) {
            ItemStack result = output.get(index).create();
            if (baseStack != null && !baseStack.isEmpty()) {
                result.applyComponents(baseStack.getComponentsPatch());
            }
            return result;
        }
        return null;
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
    public @NotNull RecipeSerializer<@NotNull EtherProcessFactoryRecipe> getSerializer() {
        return RecipeSerializerRegistry.ETHER_PROCESS_RECIPE_SERIALIZER.get();
    }

    @Override
    public RecipeType<? extends Recipe<EtherFactoryRecipeInput>> getType() {
        return RecipeTypeRegistry.ETHER_PROCESS_FACTORY_RECIPE.get();
    }

    @Override
    public @NotNull PlacementInfo placementInfo() {
        return PlacementInfo.NOT_PLACEABLE;
    }

    @Override
    public @NotNull RecipeBookCategory recipeBookCategory() {
        return RecipeBookCategories.CRAFTING_MISC;
    }
}
