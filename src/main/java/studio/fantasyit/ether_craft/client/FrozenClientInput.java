package studio.fantasyit.ether_craft.client;

import net.minecraft.client.player.ClientInput;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec2;

public class FrozenClientInput extends ClientInput {
    public FrozenClientInput() {
        this.keyPresses = Input.EMPTY;
        this.moveVector = Vec2.ZERO;
    }
}
