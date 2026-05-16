package studio.fantasyit.ether_craft.block.base;

import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

public interface IWorldRenderBE {
    @Nullable
    Component getRenderName();

    void setRenderName(@Nullable Component name);
}
