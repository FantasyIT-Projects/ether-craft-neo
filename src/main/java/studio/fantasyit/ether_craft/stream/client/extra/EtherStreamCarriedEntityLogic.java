package studio.fantasyit.ether_craft.stream.client.extra;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;
import studio.fantasyit.ether_craft.register.AttachmentDataRegistry;
import studio.fantasyit.ether_craft.register.Tags;
import studio.fantasyit.ether_craft.stream.cap.EtherStreamCarryEntityCapability;
import studio.fantasyit.ether_craft.stream.client.data.ClientStreamEntry;
import studio.fantasyit.ether_craft.stream.data.EtherStreamCarryingEntityData;
import studio.fantasyit.ether_craft.stream.data.IEtherStreamSyncedData;

public class EtherStreamCarriedEntityLogic implements IEtherStreamExtraClientLogic {
    @Override
    public boolean shouldAttach(ClientStreamEntry entry) {
        IEtherStreamSyncedData syncedData = entry.getSyncedData(EtherStreamCarryingEntityData.ID);
        return syncedData != null;
    }

    @Override
    public void onTick(ClientStreamEntry entry) {
        EtherStreamCarryingEntityData data = (EtherStreamCarryingEntityData)
                entry.getSyncedData(EtherStreamCarryingEntityData.ID);
        if (entry.isDying || entry.removed) return;
        if (data == null) return;
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return;
        Entity clientEntity = level.getEntity(data.entityId());
        if (clientEntity == null) return;
        if (!data.entityUUID().equals(clientEntity.getUUID())) return;

        Vec3 currentPos = entry.getCurrentPosition();
        clientEntity.setPos(currentPos.x, currentPos.y - clientEntity.getEyeHeight(), currentPos.z);
        clientEntity.setDeltaMovement(entry.motion);

        Vec3 to = currentPos.add(entry.motion);
        HitResult hitResult = level.clipIncludingBorder(new ClipContext(currentPos, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, clientEntity));
        if (hitResult.getType() != HitResult.Type.MISS) {
            to = hitResult.getLocation();
        }

        AABB bb = new AABB(currentPos.subtract(1.5), currentPos.add(1.3));
        HitResult entityHit = ProjectileUtil.getEntityHitResult(level, clientEntity, currentPos, to, bb, t -> !t.is(clientEntity) && (!data.playerOnly() || t instanceof Player), 0.0f);
        if (entityHit != null) {
            hitResult = entityHit;
        }

        boolean hit = false;
        if (hitResult.getType() == HitResult.Type.BLOCK && hitResult instanceof BlockHitResult blockHitResult) {
            BlockState blockState = level.getBlockState(blockHitResult.getBlockPos());
            if (!blockState.is(Tags.ETHER_STREAM_PASS_THROUGH))
                hit = true;
        } else if (hitResult.getType() == HitResult.Type.ENTITY && hitResult instanceof EntityHitResult entityHitResult) {
            if (!entityHitResult.getEntity().is(EntityType.ITEM))
                hit = true;
        }
        if (hit) {
            EtherStreamCarryEntityCapability.dropEntityTo(level, currentPos, entry.motion, clientEntity);
            entry.setRemoved();
        }
    }

    @Override
    public void onRender(ClientStreamEntry stream, Vec3 pos, CameraRenderState camera, PoseStack poseStack, SubmitNodeCollector collector, float partialTick) {
        EtherStreamCarryingEntityData data = (EtherStreamCarryingEntityData)
                stream.getSyncedData(EtherStreamCarryingEntityData.ID);
        if (data == null) return;
        if (Minecraft.getInstance().level == null) return;
        Entity clientEntity = Minecraft.getInstance().level.getEntity(data.entityId());
        if (clientEntity == null) return;
        if (!data.entityUUID().equals(clientEntity.getUUID())) return;
        float dx = (float) (pos.x() - camera.pos.x);
        float dy = (float) (pos.y() - camera.pos.y);
        float dz = (float) (pos.z() - camera.pos.z);

        poseStack.pushPose();
        poseStack.translate(dx, dy, dz);

        int light = LevelRenderer.getLightCoords(Minecraft.getInstance().level, BlockPos.containing(pos));
        double distSq = camera.pos.distanceToSqr(pos);
        Vec3 nameTagAttachment = new Vec3(0, 0.1, 0);

        collector.order(1).submitNameTag(
                poseStack, nameTagAttachment, 0, clientEntity.getName(), false, light, distSq, camera
        );
        poseStack.popPose();
    }

    @Override
    public boolean shouldRender(ClientStreamEntry entry) {
        EtherStreamCarryingEntityData data = (EtherStreamCarryingEntityData)
                entry.getSyncedData(EtherStreamCarryingEntityData.ID);
        LocalPlayer player = Minecraft.getInstance().player;
        return data == null || player == null || !player.getUUID().equals(data.entityUUID());
    }
}
