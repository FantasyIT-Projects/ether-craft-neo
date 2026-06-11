package studio.fantasyit.ether_craft.stream.cap;

import com.mojang.serialization.Codec;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.plating.data.ProgressingPlatingData;
import studio.fantasyit.ether_craft.plating.helper.PlatingUtil;
import studio.fantasyit.ether_craft.recipe.plating.PlatingRecipe;
import studio.fantasyit.ether_craft.register.RecipeTypeRegistry;
import studio.fantasyit.ether_craft.stream.IEtherStreamLike;

import java.util.*;

public class EtherStreamPlatingCapability implements IStreamCapability {
    public static final Identifier ID = EtherCraft.id("plating");
    public static final Codec<EtherStreamPlatingCapability> CODEC = Codec.BOOL.xmap(b -> new EtherStreamPlatingCapability(), c -> true);

    @Override
    public Identifier getId() {
        return ID;
    }

    @Override
    public void tick(@UnknownNullability IEtherStreamLike streamEntity) {
        if (!(streamEntity.level() instanceof ServerLevel level)) return;


        Optional<IStreamCapability> optStorage = streamEntity.getCapability(EtherStreamStorageCapability.ID);
        EtherStreamStorageCapability storage = optStorage.filter(EtherStreamStorageCapability.class::isInstance)
                .map(EtherStreamStorageCapability.class::cast).orElse(null);

        AABB currentBlockPos = new AABB(streamEntity.blockPosition());
        List<ItemEntity> entities = level.getEntities(EntityTypeTest.forClass(ItemEntity.class), currentBlockPos, Entity::isAlive);

        if (entities.isEmpty()) return;

        for (ItemEntity itemEntity : entities) {
            ItemStack stack = itemEntity.getItem();
            if (stack.isEmpty()) continue;
            boolean canProcess = PlatingUtil.isPlatingInProgress(stack) || PlatingUtil.canPlate(stack);
            if (!canProcess) continue;

            List<PlatingRecipe> recipes = getSortedRecipes(level, stack);
            boolean success = handlePlating(streamEntity, level, itemEntity, stack, storage, recipes);
            if (success) {
                int ether = streamEntity.getEther();
                PlatingUtil.addEther(stack, ether);
                streamEntity.consumeEther(ether);
                itemEntity.setItem(stack);
                break;
            }
        }
        streamEntity.removeInstantly();
    }

    private boolean handlePlating(IEtherStreamLike streamEntity, ServerLevel level, ItemEntity itemEntity, ItemStack stack, EtherStreamStorageCapability storage, List<PlatingRecipe> recipes) {
        if (storage == null) return false;
        List<ItemStack> availableItems = getStorageItems(storage);

        List<PlatingRecipe> matched = matchExactCover(availableItems, Set.of(), recipes);
        if (matched == null) {
            streamEntity.consumeEtherInternal(streamEntity.getEther());
            return false;
        }

        consumeStorageItems(storage, matched);
        List<ProgressingPlatingData> effectIds = matched.stream().map(PlatingRecipe::makeProcessing).toList();
        PlatingUtil.startPlating(stack, effectIds, level.getGameTime());
        return true;
    }

    private List<PlatingRecipe> getSortedRecipes(ServerLevel level, ItemStack stack) {
        return level.getServer().getRecipeManager().getRecipes().stream()
                .filter(h -> h.value().getType() == RecipeTypeRegistry.PLATING_RECIPE.get())
                .map(h -> (PlatingRecipe) h.value())
                .filter(r -> r.matchesFilter(stack))
                .sorted(Comparator.comparingInt(PlatingRecipe::inputSize).reversed())
                .toList();
    }

    private List<ItemStack> getStorageItems(EtherStreamStorageCapability storage) {
        List<ItemStack> items = new ArrayList<>();
        for (int i = 0; i < storage.getContainerSize(); i++) {
            ItemStack s = storage.getItem(i);
            if (!s.isEmpty()) {
                items.add(s.copy());
            }
        }
        return items;
    }

    @Nullable
    List<PlatingRecipe> matchExactCover(List<ItemStack> availableItems, Set<Identifier> usedEffects, List<PlatingRecipe> recipes) {
        if (availableItems.stream().allMatch(ItemStack::isEmpty)) return new ArrayList<>();

        for (PlatingRecipe recipe : recipes) {
            if (usedEffects.contains(recipe.effectId)) continue;
            List<ItemStack> remaining = tryConsumeRecipe(deepCopy(availableItems), recipe);
            if (remaining != null) {
                Set<Identifier> nextUsed = new HashSet<>(usedEffects);
                nextUsed.add(recipe.effectId);
                List<PlatingRecipe> result = matchExactCover(remaining, nextUsed, recipes);
                if (result != null) {
                    result.addFirst(recipe);
                    return result;
                }
            }
        }
        return null;
    }

    @Nullable
    private List<ItemStack> tryConsumeRecipe(List<ItemStack> stacks, PlatingRecipe recipe) {
        for (var ingredient : recipe.input) {
            int needed = ingredient.count();
            for (ItemStack s : stacks) {
                if (!s.isEmpty() && ingredient.ingredient().test(s)) {
                    int take = Math.min(needed, s.getCount());
                    s.shrink(take);
                    needed -= take;
                    if (needed == 0) break;
                }
            }
            if (needed > 0) return null;
        }
        stacks.removeIf(ItemStack::isEmpty);
        return stacks;
    }

    private List<ItemStack> deepCopy(List<ItemStack> stacks) {
        List<ItemStack> copy = new ArrayList<>(stacks.size());
        for (ItemStack s : stacks) {
            copy.add(s.copy());
        }
        return copy;
    }

    private void consumeStorageItems(EtherStreamStorageCapability storage, List<PlatingRecipe> matched) {
        for (PlatingRecipe recipe : matched) {
            for (var ingredient : recipe.input) {
                int needed = ingredient.count();
                for (int slot = 0; slot < storage.getContainerSize() && needed > 0; slot++) {
                    ItemStack slotStack = storage.getItem(slot);
                    if (!slotStack.isEmpty() && ingredient.ingredient().test(slotStack)) {
                        int take = Math.min(needed, slotStack.getCount());
                        slotStack.shrink(take);
                        storage.setItem(slot, slotStack);
                        needed -= take;
                    }
                }
            }
        }
    }

    private boolean matchesAnyFilter(ItemStack stack, List<PlatingRecipe> recipes) {
        return recipes.stream().anyMatch(r -> r.matchesFilter(stack));
    }

    @Override
    public boolean hitEntity(ServerLevel level, IEtherStreamLike streamEntity, EntityHitResult hit, Entity entity) {
        return false;
    }

    @Override
    public boolean hitBlock(ServerLevel level, IEtherStreamLike streamEntity, BlockHitResult hit, BlockState blockState) {
        return false;
    }

    @Override
    public void onDestroy(IEtherStreamLike streamEntity, @Nullable HitResult hitResult) {
    }

    @Override
    public void serialize(ValueOutput output) {
    }

    @Override
    public void deserialize(ValueInput input) {
    }
}