package studio.fantasyit.ether_craft.node.plugins.upgrade;

import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.WritableBookContent;
import net.minecraft.world.item.component.WrittenBookContent;
import org.jetbrains.annotations.Nullable;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.block.node.EtherAdaptNodeBlock;
import studio.fantasyit.ether_craft.block.node.EtherAdaptNodeEntity;
import studio.fantasyit.ether_craft.node.plugins.InstalledPlugin;
import studio.fantasyit.ether_craft.node.plugins.base.AbstractNodePlugin;
import studio.fantasyit.ether_craft.node.plugins.base.IEtherStreamCapabilityProviderPlugin;
import studio.fantasyit.ether_craft.stream.IEtherStreamLike;
import studio.fantasyit.ether_craft.stream.data.EtherStreamLabelData;
import studio.fantasyit.ether_craft.stream.data.StreamExtraProperty;

import java.util.ArrayList;
import java.util.List;

public class EtherStreamRandomBookUpgrade extends AbstractNodePlugin implements IEtherStreamCapabilityProviderPlugin {
    public static final Identifier ID = EtherCraft.id("ether_stream_random_book_upgrade");

    public EtherStreamRandomBookUpgrade(EtherAdaptNodeEntity nodeEntity, InstalledPlugin installedId) {
        super(nodeEntity, installedId);
    }

    @Override
    public void provideCapabilities(IEtherStreamLike entity, StreamExtraProperty extraProperty) {
        List<ItemStack> candidates = new ArrayList<>();
        for (int i = 0; i < nodeEntity.normalStorage.getContainerSize(); i++) {
            ItemStack item = nodeEntity.normalStorage.getItem(i);
            if (item.isEmpty())
                continue;
            if (item.is(Items.WRITTEN_BOOK)) {
                WrittenBookContent writtenBookContent = item.get(DataComponents.WRITTEN_BOOK_CONTENT);
                if (writtenBookContent != null && !writtenBookContent.pages().isEmpty())
                    candidates.add(item);
            } else if (item.is(Items.WRITABLE_BOOK)) {
                WritableBookContent writableBookContent = item.get(DataComponents.WRITABLE_BOOK_CONTENT);
                if (writableBookContent != null && !writableBookContent.pages().isEmpty())
                    candidates.add(item);
            }
        }
        if (candidates.isEmpty())
            return;
        ItemStack selected = candidates.get(nodeEntity.getLevel().getRandom().nextInt(candidates.size()));
        List<Component> allLines = new ArrayList<>();
        if (selected.is(Items.WRITTEN_BOOK)) {
            WrittenBookContent writtenBookContent = selected.get(DataComponents.WRITTEN_BOOK_CONTENT);
            if (writtenBookContent != null)
                writtenBookContent.pages().forEach(t -> allLines.add(t.raw()));
        } else if (selected.is(Items.WRITABLE_BOOK)) {
            WritableBookContent writableBookContent = selected.get(DataComponents.WRITABLE_BOOK_CONTENT);
            if (writableBookContent != null)
                writableBookContent.getPages(false).map(Component::literal).forEach(allLines::add);
        }
        if (allLines.isEmpty())
            return;
        Component component = allLines.get(nodeEntity.getLevel().getRandom().nextInt(allLines.size()));
        @Nullable Direction sourceDir = nodeEntity.getBlockState().getValueOrElse(EtherAdaptNodeBlock.FACING, Direction.NORTH);
        EtherStreamLabelData etherStreamLabelCapability = new EtherStreamLabelData(component, 0xFFFFFFFF, sourceDir);
        entity.setSyncedData(etherStreamLabelCapability);
    }
}
