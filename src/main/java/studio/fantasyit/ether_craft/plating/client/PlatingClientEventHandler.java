package studio.fantasyit.ether_craft.plating.client;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.mixin.plating.CameraAccessor;
import studio.fantasyit.ether_craft.network.c2s.PlatingTriggerC2S;
import studio.fantasyit.ether_craft.network.s2c.PlatingSoulStateS2C;

@EventBusSubscriber(modid = EtherCraft.MODID, value = Dist.CLIENT)
public class PlatingClientEventHandler {

    private static boolean wasUsePressed = false;

    @SubscribeEvent
    public static void onCameraSetup(ViewportEvent.ComputeCameraAngles event) {
        if (!PlatingSoulStateS2C.isClientSoulActive()) return;
        ((CameraAccessor) (Object) event.getCamera()).ether_craft$setPosition(new Vec3(
                PlatingSoulStateS2C.getClientSoulX(),
                PlatingSoulStateS2C.getClientSoulY(),
                PlatingSoulStateS2C.getClientSoulZ()
        ));
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (!PlatingSoulStateS2C.isClientSoulActive()) return;
        var mc = Minecraft.getInstance();
        if (mc.player == null) return;

        float speed = 0.5f;
        var look = mc.player.getLookAngle();
        Vec3 forward = new Vec3(look.x, 0, look.z).normalize();
        if (forward.lengthSqr() < 0.001) forward = new Vec3(0, 0, 1);
        Vec3 right = new Vec3(-forward.z, 0, forward.x);

        var move = mc.player.input.getMoveVector();
        float fwd = move.y;
        float str = move.x;

        double x = PlatingSoulStateS2C.getClientSoulX();
        double y = PlatingSoulStateS2C.getClientSoulY();
        double z = PlatingSoulStateS2C.getClientSoulZ();

        x += (forward.x * fwd + right.x * str) * speed;
        z += (forward.z * fwd + right.z * str) * speed;

        if (mc.options.keyJump.isDown()) y += speed;
        if (mc.options.keyShift.isDown()) y -= speed;

        PlatingSoulStateS2C.updateClientSoulPos(x, y, z);

        boolean useNow = mc.options.keyUse.isDown();
        if (useNow && !wasUsePressed) {
            ClientPacketDistributor.sendToServer(new PlatingTriggerC2S(
                    Identifier.fromNamespaceAndPath(EtherCraft.MODID, "soul_projection")
            ));
        }
        wasUsePressed = useNow;
    }
}
