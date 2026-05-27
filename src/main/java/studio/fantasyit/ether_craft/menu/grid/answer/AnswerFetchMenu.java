package studio.fantasyit.ether_craft.menu.grid.answer;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import studio.fantasyit.ether_craft.item.EtherProcessRecipeAnswerItem;
import studio.fantasyit.ether_craft.recipe.grid.EtherProcessFactoryGrid;
import studio.fantasyit.ether_craft.recipe.grid.EtherProcessFactoryGridInput;
import studio.fantasyit.ether_craft.register.GuiRegistry;

import java.util.List;

public class AnswerFetchMenu extends AbstractContainerMenu {
    public static final int GRID_COLS = 7;
    public static final int GRID_ROWS = 3;
    public static final int SLOTS_PER_PAGE = GRID_COLS * GRID_ROWS;

    public static final int DISPLAY_X = 8;
    public static final int DISPLAY_Y = 18;

    public final Player player;
    public final InteractionHand hand;
    public final SimpleContainer displayContainer;
    public final List<ItemStack> assembledResults;

    public int currentPage;
    public int totalPages;

    public AnswerFetchMenu(int windowId, Player player, InteractionHand hand) {
        super(GuiRegistry.ANSWER_FETCH.get(), windowId);
        this.player = player;
        this.hand = hand;
        this.displayContainer = new SimpleContainer(SLOTS_PER_PAGE);

        ItemStack held = player.getItemInHand(hand);
        EtherProcessRecipeAnswerItem answerItem = (EtherProcessRecipeAnswerItem) held.getItem();
        EtherProcessFactoryGridInput input = answerItem.getInput(held);
        List<EtherProcessFactoryGrid> grids = answerItem.getCompatibleGrids(held, (ServerLevel) player.level());
        this.assembledResults = grids.stream().map(g -> g.assemble(input)).toList();
        this.totalPages = Math.max(1, (assembledResults.size() + SLOTS_PER_PAGE - 1) / SLOTS_PER_PAGE);

        addDisplaySlots();
        refreshPage();
        addDataSlots();
    }

    public AnswerFetchMenu(int windowId, Inventory inv, RegistryFriendlyByteBuf data) {
        super(GuiRegistry.ANSWER_FETCH.get(), windowId);
        this.player = inv.player;
        this.hand = data.readEnum(InteractionHand.class);
        this.displayContainer = new SimpleContainer(SLOTS_PER_PAGE);
        this.assembledResults = List.of();
        this.totalPages = 0;

        addDisplaySlots();
        addDataSlots();
    }

    private void addDisplaySlots() {
        for (int row = 0; row < GRID_ROWS; row++) {
            for (int col = 0; col < GRID_COLS; col++) {
                int idx = row * GRID_COLS + col;
                addSlot(new ReadOnlySlot(displayContainer, idx,
                        DISPLAY_X + col * 18, DISPLAY_Y + row * 18));
            }
        }
    }

    private void addDataSlots() {
        addDataSlot(new DataSlot() {
            @Override
            public int get() {
                return currentPage;
            }

            @Override
            public void set(int value) {
                currentPage = value;
            }
        });
        addDataSlot(new DataSlot() {
            @Override
            public int get() {
                return totalPages;
            }

            @Override
            public void set(int value) {
                totalPages = value;
            }
        });
    }

    private void refreshPage() {
        int start = currentPage * SLOTS_PER_PAGE;
        for (int i = 0; i < SLOTS_PER_PAGE; i++) {
            int idx = start + i;
            displayContainer.setItem(i, idx < assembledResults.size() ? assembledResults.get(idx) : ItemStack.EMPTY);
        }
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == 0 && currentPage > 0) {
            currentPage--;
            refreshPage();
            return true;
        }
        if (id == 1 && currentPage < totalPages - 1) {
            currentPage++;
            refreshPage();
            return true;
        }
        return false;
    }

    @Override
    public void clicked(int slotIndex, int button, ContainerInput containerInput, Player player) {
        if (slotIndex >= 0 && slotIndex < SLOTS_PER_PAGE) {
            Slot slot = slots.get(slotIndex);
            if (slot instanceof ReadOnlySlot && slot.container == displayContainer) {
                int resultIndex = currentPage * SLOTS_PER_PAGE + slot.getContainerSlot();
                if (resultIndex < assembledResults.size()) {
                    player.setItemInHand(hand, assembledResults.get(resultIndex).copy());
                    player.closeContainer();
                    return;
                }
            }
        }
        super.clicked(slotIndex, button, containerInput, player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int i) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return player.getItemInHand(hand).getItem() instanceof EtherProcessRecipeAnswerItem;
    }
}
