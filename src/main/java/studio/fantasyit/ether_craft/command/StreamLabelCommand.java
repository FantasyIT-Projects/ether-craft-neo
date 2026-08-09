package studio.fantasyit.ether_craft.command;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import studio.fantasyit.ether_craft.EtherCraft;

import static studio.fantasyit.ether_craft.register.AttachmentDataRegistry.STREAM_LABEL_OVERRIDE;

@EventBusSubscriber(modid = EtherCraft.MODID)
public class StreamLabelCommand {
    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(Commands.literal("ether_craft")
                .then(Commands.literal("stream_label")
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            boolean next = !player.getData(STREAM_LABEL_OVERRIDE);
                            player.setData(STREAM_LABEL_OVERRIDE, next);
                            ctx.getSource().sendSuccess(() -> Component.translatable("command.ether_craft.stream_label.toggled", next ? "on" : "off"), false);
                            return 1;
                        })));
    }
}
