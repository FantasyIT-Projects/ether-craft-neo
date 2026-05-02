package studio.fantasyit.ether_craft.capability;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.BlockCapability;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.register.BlockAttachmentDataRegistry;

public class EtherContainer{
    public static final BlockCapability<EtherContainer, Void> ITEM_HANDLER_BLOCK =
            BlockCapability.createVoid(
                    EtherCraft.id("ether_container"),
                    EtherContainer.class
            );

    private final BlockEntity be;
    public EtherContainer(BlockEntity be){
        this.be = be;
    }
    public long get(){
        return be.getData(BlockAttachmentDataRegistry.ETHER_CONTAINER);
    }
    public void set(long amount){
        be.setData(BlockAttachmentDataRegistry.ETHER_CONTAINER, amount);
    }
    public void receive(long amount){
        set(get() + amount);
    }
    public long extract(long amount){
        long extracted = Math.min(get(), amount);
        set(get() - extracted);
        return extracted;
    }
}
