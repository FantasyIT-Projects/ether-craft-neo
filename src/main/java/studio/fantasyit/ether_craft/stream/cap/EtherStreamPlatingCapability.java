package studio.fantasyit.ether_craft.stream.cap;

import com.mojang.serialization.Codec;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import org.jetbrains.annotations.UnknownNullability;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.plating.PlatingUtil;
import studio.fantasyit.ether_craft.recipe.plating.PlatingRecipe;
import studio.fantasyit.ether_craft.register.RecipeTypeRegistry;
import studio.fantasyit.ether_craft.stream.IEtherStreamLike;

import java.util.*;
import java.util.stream.Collectors;

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

        AABB currentBlockPos = new AABB(streamEntity.blockPosition());
        List<ItemEntity> entities = level.getEntities(EntityTypeTest.forClass(ItemEntity.class), currentBlockPos, t -> t.isAlive() && !t.hasPickUpDelay());

        Optional<IStreamCapability> optStorage = streamEntity.getCapability(EtherStreamStorageCapability.ID);
        EtherStreamStorageCapability storage = optStorage.filter(EtherStreamStorageCapability.class::isInstance)
                .map(EtherStreamStorageCapability.class::cast).orElse(null);

        for (ItemEntity itemEntity : entities) {
            ItemStack stack = itemEntity.getItem();
            if (stack.isEmpty()) continue;

            if (PlatingUtil.isPlatingInProgress(stack)) {
                handleInProgress(streamEntity, level, itemEntity, stack, storage);
            } else if (PlatingUtil.hasPlating(stack)) {
                handleCompleted(streamEntity, itemEntity, stack);
            } else if (PlatingUtil.canPlate(stack) && matchesAnyFilter(stack, level)) {
                handleNewPlating(streamEntity, level, itemEntity, stack, storage);
            }
        }
    }

    private void handleInProgress(IEtherStreamLike streamEntity, ServerLevel level, ItemEntity itemEntity, ItemStack stack, EtherStreamStorageCapability storage) {
        if (storage == null) return;
        List<PlatingRecipe> recipes = getSortedRecipes(level);
        List<ItemStack> availableItems = getStorageItems(storage);
        if (availableItems.stream().allMatch(ItemStack::isEmpty)) return;

        Set<Identifier> existingEffects = new HashSet<>(PlatingUtil.getInProgress(stack));
        List<PlatingRecipe> matched = matchExactCover(availableItems, existingEffects, recipes);
        if (matched == null) {
            streamEntity.consumeEtherInternal(streamEntity.getEther());
            return;
        }

        consumeStorageItems(storage, availableItems, matched);
        List<Identifier> effectIds = matched.stream().map(r -> r.effectId).toList();
        PlatingUtil.overwritePlating(stack, effectIds, level.getGameTime());
        itemEntity.setItem(stack);
    }

    private void handleCompleted(IEtherStreamLike streamEntity, ItemEntity itemEntity, ItemStack stack) {
        int ether = streamEntity.getEther();
        if (ether <= 0) return;
        PlatingUtil.addEther(stack, ether);
        streamEntity.consumeEther(ether);
        itemEntity.setItem(stack);
    }

    private void handleNewPlating(IEtherStreamLike streamEntity, ServerLevel level, ItemEntity itemEntity, ItemStack stack, EtherStreamStorageCapability storage) {
        if (storage == null) return;
        List<PlatingRecipe> recipes = getSortedRecipes(level);
        List<ItemStack> availableItems = getStorageItems(storage);
        if (availableItems.stream().allMatch(ItemStack::isEmpty)) return;

        List<PlatingRecipe> matched = matchExactCover(availableItems, Set.of(), recipes);
        if (matched == null) {
            streamEntity.consumeEtherInternal(streamEntity.getEther());
            return;
        }

        consumeStorageItems(storage, availableItems, matched);
        List<Identifier> effectIds = matched.stream().map(r -> r.effectId).toList();
        PlatingUtil.startPlating(stack, effectIds, level.getGameTime());
        itemEntity.setItem(stack);
    }

    private List<PlatingRecipe> getSortedRecipes(ServerLevel level) {
        return level.getServer().getRecipeManager().getRecipes().stream()
                .filter(h -> h.value().getType() == RecipeTypeRegistry.PLATING_RECIPE.get())
                .map(h -> (PlatingRecipe) h.value())
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

    @org.jetbrains.annotations.Nullable
    List<PlatingRecipe> matchExactCover(List<ItemStack> availableItems, Set<Identifier> usedEffects, List<PlatingRecipe> recipes) {
        Map<Item, Long> itemCounts = availableItems.stream()
                .filter(s -> !s.isEmpty())
                .collect(Collectors.groupingBy(ItemStack::getItem, Collectors.summingLong(ItemStack::getCount)));
        return solve(itemCounts, usedEffects, recipes);
    }

    @org.jetbrains.annotations.Nullable
    private List<PlatingRecipe> solve(Map<Item, Long> remaining, Set<Identifier> usedEffects, List<PlatingRecipe> recipes) {
        if (remaining.values().stream().allMatch(c -> c == 0)) return new ArrayList<>();

        for (PlatingRecipe recipe : recipes) {
            if (usedEffects.contains(recipe.effectId)) continue;
            Map<Item, Long> afterConsume = tryConsume(remaining, recipe);
            if (afterConsume != null) {
                Set<Identifier> newUsed = new HashSet<>(usedEffects);
                newUsed.add(recipe.effectId);
                List<PlatingRecipe> result = solve(afterConsume, newUsed, recipes);
                if (result != null) {
                    result.add(0, recipe);
                    return result;
                }
            }
        }
        return null;
    }

    @org.jetbrains.annotations.Nullable
    private Map<Item, Long> tryConsume(Map<Item, Long> counts, PlatingRecipe recipe) {
        Map<Item, Long> copy = new HashMap<>(counts);
        for (var ingredient : recipe.input) {
            int needed = ingredient.count();
            var iter = copy.entrySet().iterator();
            while (iter.hasNext()) {
                var entry = iter.next();
                if (entry.getValue() <= 0) continue;
                if (ingredient.ingredient().test(new ItemStack(entry.getKey()))) {
                    long take = Math.min(needed, entry.getValue());
                    needed -= (int) take;
                    entry.setValue(entry.getValue() - take);
                    if (entry.getValue() <= 0) iter.remove();
                    if (needed == 0) break;
                }
            }
            if (needed > 0) return null;
        }
        return copy;
    }

    private void consumeStorageItems(EtherStreamStorageCapability storage, List<ItemStack> available, List<PlatingRecipe> matched) {
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

    private boolean matchesAnyFilter(ItemStack stack, ServerLevel level) {
        return getSortedRecipes(level).stream().anyMatch(r -> r.matchesFilter(stack));
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
    public void onDestroy(IEtherStreamLike streamEntity) {
    }

    @Override
    public void serialize(ValueOutput output) {
    }

    @Override
    public void deserialize(ValueInput input) {
    }
}