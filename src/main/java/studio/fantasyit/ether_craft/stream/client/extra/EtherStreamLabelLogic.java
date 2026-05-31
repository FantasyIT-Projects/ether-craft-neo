package studio.fantasyit.ether_craft.stream.client.extra;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import studio.fantasyit.ether_craft.stream.client.data.ClientStreamEntry;
import studio.fantasyit.ether_craft.stream.data.EtherStreamLabelData;

public class EtherStreamLabelLogic implements IEtherStreamExtraClientLogic {
    private static final float LABEL_SCALE = 0.010416667F;

    @Override
    public boolean shouldDelayDeath(ClientStreamEntry entry) {
        return true;
    }

    @Override
    public void onTick(ClientStreamEntry entry) {
    }

    @Override
    public void onRender(ClientStreamEntry stream, Vec3 currentPos, CameraRenderState camera, PoseStack poseStack, SubmitNodeCollector collector) {
        @Nullable EtherStreamLabelData labelData = (EtherStreamLabelData) stream.getSyncedData(EtherStreamLabelData.ID);
        if (labelData == null) return;
        Vec3 motion = stream.motion;
        if (motion.lengthSqr() < 0.0001) return;

        Font font = Minecraft.getInstance().font;
        String fullText = labelData.label.getString();
        int fullTextWidth = font.width(fullText);
        if (fullTextWidth == 0) return;

        String visibleText = fullText;
        int visibleTextWidth = fullTextWidth;

        if (stream.isDying) {
            // Right-clip: deathTick counts down 60->0, consume text from right
            int moveFromDeath = (int) ((60f - stream.deathTick) * stream.motion.length() * 100);
            int clipPixels = Math.min(moveFromDeath, fullTextWidth);
            visibleText = font.plainSubstrByWidth(fullText, Math.max(0, fullTextWidth - clipPixels));
            visibleTextWidth = font.width(visibleText);
        }

        Vec3 dir = motion.normalize();
        Vec3 up = new Vec3(0.0, 1.0, 0.0);
        boolean vertical = Math.abs(dir.dot(up)) > 0.999;
        Vec3 normal;
        if (vertical) {
            normal = dir.cross(new Vec3(1.0, 0.0, 0.0)).normalize();
        } else {
            normal = dir.cross(up).normalize();
        }

        FormattedCharSequence text = FormattedCharSequence.forward(visibleText, net.minecraft.network.chat.Style.EMPTY);

        // Render on both faces so the label is visible from either side
        for (Vec3 faceNormal : new Vec3[]{normal, normal.scale(-1)}) {
            poseStack.pushPose();

            poseStack.translate(
                    currentPos.x - camera.pos.x,
                    currentPos.y - camera.pos.y,
                    currentPos.z - camera.pos.z
            );

            Quaternionf rotation = new Quaternionf().rotateTo(
                    new org.joml.Vector3f(0, 0, 1),
                    new org.joml.Vector3f((float) faceNormal.x, (float) faceNormal.y, (float) faceNormal.z));
            poseStack.mulPose(rotation);
            if (vertical) {
                poseStack.mulPose(new Quaternionf().rotateZ((float) Math.toRadians(faceNormal == normal ? -90 : 90)));
            }
            poseStack.scale(LABEL_SCALE, -LABEL_SCALE, LABEL_SCALE);

            float textX;
            if (vertical) {
                textX = faceNormal == normal ? 0 : -visibleTextWidth;
            } else {
                textX = faceNormal == normal ? -visibleTextWidth : 0;
            }
            collector.submitText(poseStack, textX, 0, text, false,
                    Font.DisplayMode.NORMAL, 0xF000F0, labelData.labelColor, 0, 0);

            poseStack.popPose();
        }
    }
}
