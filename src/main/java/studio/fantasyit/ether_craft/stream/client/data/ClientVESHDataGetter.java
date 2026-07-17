package studio.fantasyit.ether_craft.stream.client.data;

import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;

public class ClientVESHDataGetter {
    public static ClientVESHData get() {
        Level level = Minecraft.getInstance().level;
        if (level == null) return ClientVESHData.DUMMY;
        return ClientVESHData.getWithCurrentLevel(level);
    }
}
