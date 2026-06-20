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

import java.util.ArrayList;
import java.util.List;

public class EtherStreamLabelLogic implements IEtherStreamExtraClientLogic {
    private static final float LABEL_SCALE = 0.010416667F;

    @Override
    public boolean shouldAttach(ClientStreamEntry entry) {
        return entry.getSyncedData(EtherStreamLabelData.ID) != null;
    }

    @Override
    public boolean shouldRender(ClientStreamEntry entry) {
        return entry.getSyncedData(EtherStreamLabelData.ID) == null;
    }

    @Override
    public boolean shouldDelayDeath(ClientStreamEntry entry) {
        return true;
    }

    @Override
    public void onRender(ClientStreamEntry stream, Vec3 currentPos, CameraRenderState camera, PoseStack poseStack, SubmitNodeCollector collector) {
        @Nullable EtherStreamLabelData labelData = (EtherStreamLabelData) stream.getSyncedData(EtherStreamLabelData.ID);
        if (labelData == null) return;
        Vec3 motion = stream.motion;
        if (motion.lengthSqr() < 0.0001) return;

        Font font = Minecraft.getInstance().font;
        List<EtherStreamLabelData.Segment> segments = labelData.getSegments();
        if (segments.isEmpty()) return;

        List<EtherStreamLabelData.Segment> renderSegments = segments;
        if (stream.isDying) {
            int clip = (int) ((60f - stream.deathTick) * stream.motion.length() * 100);
            renderSegments = clipSegments(segments, clip, font, false);
            if (renderSegments.isEmpty()) return;
        } else {
            double distanceTraveled = currentPos.distanceTo(stream.startPos);
            int keepPixels = (int) (distanceTraveled * 100);
            float totalWidth = 0;
            for (EtherStreamLabelData.Segment seg : segments)
                totalWidth += font.width(seg.text()) * seg.scale();
            int clip = Math.max(0, (int) totalWidth - keepPixels);
            renderSegments = clipSegments(segments, clip, font, true);
            if (renderSegments.isEmpty()) return;
        }

        float totalWidth = 0;
        for (EtherStreamLabelData.Segment seg : renderSegments) {
            totalWidth += font.width(seg.text()) * seg.scale();
        }

        Vec3 dir = motion.normalize();
        Vec3 up = new Vec3(0.0, 1.0, 0.0);
        boolean steep = Math.abs(dir.dot(up)) > 0.707;
        Vec3 normal;
        if (steep && labelData.sourceDirection != null) {
            normal = dir.cross(labelData.sourceDirection.getUnitVec3()).normalize();
        } else {
            normal = dir.cross(up).normalize();
        }

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
            if (steep) {
                poseStack.mulPose(new Quaternionf().rotateZ((float) Math.toRadians(faceNormal == normal ? -90 : 90)));
            }
            poseStack.scale(LABEL_SCALE, -LABEL_SCALE, LABEL_SCALE);

            float startX;
            if (steep) {
                startX = faceNormal == normal ? 0 : -totalWidth;
            } else {
                startX = faceNormal == normal ? -totalWidth : 0;
            }

            float cursor = startX;
            for (EtherStreamLabelData.Segment seg : renderSegments) {
                poseStack.pushPose();
                poseStack.translate(cursor, 0, 0);
                poseStack.scale(seg.scale(), seg.scale(), 1);

                int color = seg.color() != null ? seg.color().getValue() : labelData.labelColor;
                color = 0xff000000 | color;
                FormattedCharSequence text = FormattedCharSequence.forward(seg.text(), net.minecraft.network.chat.Style.EMPTY);
                collector.submitText(poseStack, 0, 0, text, false,
                        Font.DisplayMode.NORMAL, 0xFFF000F0, color, 0, 0);

                poseStack.popPose();
                cursor += font.width(seg.text()) * seg.scale();
            }

            poseStack.popPose();
        }
    }

    private List<EtherStreamLabelData.Segment> clipSegments(List<EtherStreamLabelData.Segment> segments, int clipPixels, Font font, boolean fromStart) {
        int remaining = clipPixels;
        List<EtherStreamLabelData.Segment> result = new ArrayList<>();

        int i = fromStart ? 0 : segments.size() - 1;
        int end = fromStart ? segments.size() : -1;
        int step = fromStart ? 1 : -1;

        for (; i != end; i += step) {
            EtherStreamLabelData.Segment seg = segments.get(i);
            int segWidth = (int) (font.width(seg.text()) * seg.scale());

            if (remaining <= 0) {
                if (fromStart) {
                    for (int j = i; j < segments.size(); j++) result.add(segments.get(j));
                } else {
                    for (int j = 0; j <= i; j++) result.add(segments.get(j));
                }
                return result;
            }

            if (remaining >= segWidth) {
                remaining -= segWidth;
            } else {
                float unscaledClip = remaining / seg.scale();
                if (fromStart) {
                    String prefix = font.plainSubstrByWidth(seg.text(), (int) unscaledClip);
                    String remainingText = seg.text().substring(prefix.length());
                    if (!remainingText.isEmpty())
                        result.add(new EtherStreamLabelData.Segment(remainingText, seg.scale(), seg.color()));
                    for (int j = i + 1; j < segments.size(); j++) result.add(segments.get(j));
                } else {
                    String clippedText = font.plainSubstrByWidth(seg.text(),
                            Math.max(0, font.width(seg.text()) - (int) unscaledClip));
                    for (int j = 0; j < i; j++) result.add(segments.get(j));
                    if (!clippedText.isEmpty())
                        result.add(new EtherStreamLabelData.Segment(clippedText, seg.scale(), seg.color()));
                }
                return result;
            }
        }

        return result;
    }
}
