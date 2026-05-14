package studio.fantasyit.ether_craft.node.plugins.function;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.block.base.ItemFilter;
import studio.fantasyit.ether_craft.block.node.EtherAdaptNodeEntity;
import studio.fantasyit.ether_craft.menu.base.slot.FilterSlot;
import studio.fantasyit.ether_craft.menu.node.EtherAdaptNodeContainerMenu;
import studio.fantasyit.ether_craft.network.c2s.SyncScreenDataC2S;
import studio.fantasyit.ether_craft.node.AbstractNodePlugin;
import studio.fantasyit.ether_craft.node.NodeProperty;
import studio.fantasyit.ether_craft.node.filter.FilterGuiRegCommon;
import studio.fantasyit.ether_craft.node.plugins.InstalledPlugin;
import studio.fantasyit.ether_craft.recipe.node.NodeProcessRecipe;

import java.util.ArrayList;
import java.util.List;

public class FunctionNodeProcess extends AbstractNodePlugin {
    public static final Identifier ID = EtherCraft.id("node_process");
    public static final String FILTER_PREFIX = "node_process/";

    public ItemFilter targetItemFilter;
    public SimpleContainer inputSlots = new SimpleContainer(9);

    NodeProcessRecipe targetRecipe = null;

    public int progressing = 0;

    public FunctionNodeProcess(EtherAdaptNodeEntity nodeEntity, InstalledPlugin installedId) {
        super(nodeEntity, installedId);
        targetItemFilter = new ItemFilter(1, nodeEntity::setChanged);
    }

    @Override
    public void modifyNodeProperty(NodeProperty nodeProperty) {
        if (nodeProperty.slotUnlock == 0)
            nodeProperty.slotUnlock = 1;
    }

    @Override
    public void registerSlots(EtherAdaptNodeContainerMenu menu) {
        super.registerSlots(menu);
        for (int i = 0; i < inputSlots.getContainerSize(); i++) {
            int col = i % 3;
            int row = i / 3;
            menu.addSlotDraw(new Slot(inputSlots, i, 44 + col * 18, 77 + row * 18));
        }
        FilterSlot filterSlot = new FilterSlot(targetItemFilter, 0, 25, 31);
        menu.addSlotDraw(filterSlot);
        filterSlot.setActive(false);
    }

    @Override
    public void syncScreenData(SyncScreenDataC2S message) {
        super.syncScreenData(message);
        FilterGuiRegCommon.sync(message, targetItemFilter, FILTER_PREFIX);
    }

    @Override
    public void tick() {
        if (nodeEntity.getLevel() == null || nodeEntity.getLevel().isClientSide())
            return;
        pullItems();
        tryProcess();
    }

    private List<NodeProcessRecipe> getRecipes() {
        List<NodeProcessRecipe> result = new ArrayList<>();
        if (nodeEntity.getLevel() instanceof ServerLevel serverLevel) {
            for (RecipeHolder<?> holder : serverLevel.getServer().getRecipeManager().getRecipes()) {
                if (holder.value() instanceof NodeProcessRecipe recipe) {
                    result.add(recipe);
                }
            }
        }
        return result;
    }

    private void pullItems() {
        ItemStack targetItem = targetItemFilter.getItem(0);
        if (!targetItem.isEmpty()) {
            NodeProcessRecipe recipe = null;
            for (NodeProcessRecipe r : getRecipes()) {
                if (ItemStack.isSameItemSameComponents(r.result.create(), targetItem)) {
                    recipe = r;
                    break;
                }
            }
            if (recipe != null) {
                for (SizedIngredient ingredient : recipe.ingredients) {
                    boolean alreadyHas = false;
                    for (int i = 0; i < inputSlots.getContainerSize(); i++) {
                        ItemStack stack = inputSlots.getItem(i);
                        if (!stack.isEmpty() && ingredient.ingredient().test(stack)) {
                            alreadyHas = true;
                            break;
                        }
                    }
                    if (alreadyHas)
                        continue;

                    try (Transaction tx = Transaction.openRoot()) {
                        ItemStack pulled = nodeEntity.extractWithPredicate(
                                res -> ingredient.ingredient().test(res.toStack()),
                                tx,
                                64
                        );
                        if (!pulled.isEmpty()) {
                            for (int j = 0; j < inputSlots.getContainerSize(); j++) {
                                if (inputSlots.getItem(j).isEmpty()) {
                                    inputSlots.setItem(j, pulled);
                                    tx.commit();
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        } else {
            for (int i = 0; i < inputSlots.getContainerSize(); i++) {
                if (!inputSlots.getItem(i).isEmpty())
                    continue;
                try (Transaction tx = Transaction.openRoot()) {
                    ItemStack pulled = nodeEntity.extractWithPredicate(
                            res -> true, tx, 64
                    );
                    if (!pulled.isEmpty()) {
                        inputSlots.setItem(i, pulled);
                        tx.commit();
                    }
                }
            }
        }
    }

    private void tryProcess() {
        if (targetRecipe != null) {
            List<ItemStack> inputList = new ArrayList<>();
            for (int i = 0; i < inputSlots.getContainerSize(); i++) {
                inputList.add(inputSlots.getItem(i));
            }
            if (targetRecipe.matchesSubset(inputList) && nodeEntity.getEther() >= targetRecipe.etherCost) {
                nodeEntity.extractEther(targetRecipe.etherCost);
                for (SizedIngredient ingredient : targetRecipe.ingredients) {
                    int needed = ingredient.count();
                    for (int i = 0; i < inputSlots.getContainerSize() && needed > 0; i++) {
                        ItemStack slot = inputSlots.getItem(i);
                        if (!slot.isEmpty() && ingredient.ingredient().test(slot)) {
                            int take = Math.min(needed, slot.getCount());
                            slot.shrink(take);
                            needed -= take;
                        }
                    }
                }
                try (Transaction tx = Transaction.openRoot()) {
                    ItemStack result = targetRecipe.result.create();
                    int inserted = nodeEntity.insert(ItemResource.of(result), result.getCount(), tx);
                    if (inserted > 0)
                        tx.commit();
                }
                targetRecipe = null;
            } else if (targetItemFilter.getItem(0).isEmpty()) {
                targetRecipe = null;
            }
        } else {
            ItemStack targetItem = targetItemFilter.getItem(0);
            if (!targetItem.isEmpty()) {
                for (NodeProcessRecipe recipe : getRecipes()) {
                    if (ItemStack.isSameItemSameComponents(recipe.result.create(), targetItem)) {
                        targetRecipe = recipe;
                        break;
                    }
                }
                if (targetRecipe == null) {
                    targetItemFilter.setItemNoTrigger(0, ItemStack.EMPTY);
                }
            } else {
                List<ItemStack> inputList = new ArrayList<>();
                for (int i = 0; i < inputSlots.getContainerSize(); i++) {
                    inputList.add(inputSlots.getItem(i));
                }
                for (NodeProcessRecipe recipe : getRecipes()) {
                    if (recipe.matchesSubset(inputList)) {
                        targetRecipe = recipe;
                        break;
                    }
                }
            }
        }
    }

    @Override
    public void saveAdditional(ValueOutput output) {
        targetItemFilter.serialize(output.child("targetItemFilter"));
        output.store("inputSlots", ItemStack.OPTIONAL_CODEC.listOf(), containerToList(inputSlots));
    }

    @Override
    public void loadAdditional(ValueInput input) {
        targetItemFilter.deserialize(input.childOrEmpty("targetItemFilter"));
        input.read("inputSlots", ItemStack.OPTIONAL_CODEC.listOf()).ifPresent(l -> fillContainer(inputSlots, l));
        targetRecipe = null;
    }

    private static List<ItemStack> containerToList(SimpleContainer c) {
        List<ItemStack> list = new ArrayList<>();
        for (int i = 0; i < c.getContainerSize(); i++)
            list.add(c.getItem(i));
        return list;
    }

    private static void fillContainer(SimpleContainer c, List<ItemStack> items) {
        for (int i = 0; i < c.getContainerSize(); i++)
            c.setItem(i, items.get(i));
    }
}
