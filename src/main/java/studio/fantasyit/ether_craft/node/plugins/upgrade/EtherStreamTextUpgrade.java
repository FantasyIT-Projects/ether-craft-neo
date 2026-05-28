package studio.fantasyit.ether_craft.node.plugins.upgrade;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.WrittenBookContent;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.block.node.EtherAdaptNodeEntity;
import studio.fantasyit.ether_craft.entity.stream.EtherStreamEntity;
import studio.fantasyit.ether_craft.node.plugins.InstalledPlugin;
import studio.fantasyit.ether_craft.node.plugins.base.AbstractNodePlugin;
import studio.fantasyit.ether_craft.node.plugins.base.IEtherStreamCapabilityProviderPlugin;
import studio.fantasyit.ether_craft.stream.EtherStreamLabelCapability;

import java.util.ArrayList;
import java.util.List;

public class EtherStreamTextUpgrade extends AbstractNodePlugin implements IEtherStreamCapabilityProviderPlugin {
    public static final Identifier ID = EtherCraft.id("ether_stream_text_upgrade");

    public EtherStreamTextUpgrade(EtherAdaptNodeEntity nodeEntity, InstalledPlugin installedId) {
        super(nodeEntity, installedId);
    }

    @Override
    public void provideCapabilities(EtherStreamEntity entity) {
        ItemStack item = nodeEntity.featureUpgradeStorage.getItem(installedId.id());
        if (item.is(Items.WRITTEN_BOOK)) {
            WrittenBookContent writtenBookContent = item.get(DataComponents.WRITTEN_BOOK_CONTENT);
            if (writtenBookContent == null) return;
            List<Component> allLines = new ArrayList<>();
            writtenBookContent.pages().forEach(t -> allLines.add(t.raw()));
            if (allLines.isEmpty()) return;
            Component component = allLines.get(nodeEntity.getLevel().getRandom().nextInt(allLines.size()));
            EtherStreamLabelCapability etherStreamLabelCapability = new EtherStreamLabelCapability();
            etherStreamLabelCapability.setLabel(component);
            etherStreamLabelCapability.setColor(0xFFFFFFFF);
            entity.addCapability(etherStreamLabelCapability);
        }
    }
}
