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
    public void onRender(ClientStreamEntry stream, Vec3 currentPos, CameraRenderState camera, PoseStack poseStack, SubmitNodeCollector collector, float partialTick) {
        @Nullable EtherStreamLabelData labelData = (EtherStreamLabelData) stream.getSyncedData(EtherStreamLabelData.ID);
        if (labelData == null) return;
        Vec3 motion = stream.motion;
        if (motion.lengthSqr() < 0.0001) return;

        Font font = Minecraft.getInstance().font;
        List<EtherStreamLabelData.Segment> segments = labelData.getSegments();
        if (segments.isEmpty()) return;

        //先计算没有clip的完整字符串长度，后续备用
        float unclippedTotalWidth = labelData.getFullWidthIfAbsent(() -> {
            float u = 0;
            for (EtherStreamLabelData.Segment seg : segments)
                u += font.width(seg.text()) * seg.scale();
            return u;
        });

        //这里先判断一手是不是竖直的流，如果是的话，需要添加额外的变换
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
                poseStack.mulPose(new Quaternionf().rotateZ((float) Math.toRadians(90)));
            }
            poseStack.scale(LABEL_SCALE, -LABEL_SCALE, LABEL_SCALE);

            boolean textReverse = steep ? (motion.y > 0) : faceNormal.equals(normal);

            //因为两个面的文字序不一样，这里需要根据面来计算文字序
            List<EtherStreamLabelData.Segment> renderSegments;
            double maxLen = 60 * stream.motion.length() * 100;
            if (stream.isDying) {
                int deathClip = (int) ((60 - stream.deathTick) * stream.motion.length() * 100);
                if (unclippedTotalWidth > maxLen) {
                    deathClip += (int) (unclippedTotalWidth - maxLen);
                }
                renderSegments = clipSegments(segments, deathClip - 70, font, !textReverse);
            } else {
                double distanceTraveled = currentPos.distanceTo(stream.startPos);
                int keepPixels = (int) (distanceTraveled * 100);
                int clip = Math.max(0, (int) (unclippedTotalWidth - Math.min(keepPixels, maxLen)));
                renderSegments = clipSegments(segments, clip - 70, font, textReverse);
            }

            if (renderSegments.isEmpty()) {
                poseStack.popPose();
                continue;
            }

            //裁剪后的实际的长度（计算文本对齐用）
            float totalWidth = 0;
            for (EtherStreamLabelData.Segment seg : renderSegments) {
                totalWidth += font.width(seg.text()) * seg.scale();
            }

            //对于死后的流，需要自己控制移动动画
            if (stream.isDying) {
                float deathOffset = (60f - stream.deathTick + partialTick) * (float) stream.motion.length() * 100;
                float t = unclippedTotalWidth;
                if (maxLen < unclippedTotalWidth) {
                    t = 0;
                    for (EtherStreamLabelData.Segment seg : clipSegments(segments, (int) (unclippedTotalWidth - maxLen) - 70, font, !textReverse))
                        t += font.width(seg.text()) * seg.scale();
                }
                deathOffset -= t - totalWidth;
                if (steep)
                    poseStack.translate(motion.y > 0 ? deathOffset : -deathOffset, 0, 0);
                else
                    poseStack.translate(faceNormal.equals(normal) ? deathOffset : -deathOffset, 0, 0);
            }


            float startX;
            if (steep) {
                startX = dir.y < 0 ? 0 : -totalWidth;
            } else {
                startX = faceNormal.equals(normal) ? -totalWidth : 0;
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

    @Override
    public boolean shouldAlwaysRender(ClientStreamEntry entry, Vec3 currentPos, CameraRenderState camera) {
        @Nullable EtherStreamLabelData labelData = (EtherStreamLabelData) entry.getSyncedData(EtherStreamLabelData.ID);
        if (labelData == null) return false;
        Font font = Minecraft.getInstance().font;
        double maxLen = 60 * entry.motion.length() * 100;
        float totalWidth = 0;
        for (EtherStreamLabelData.Segment seg : labelData.getSegments()) {
            totalWidth += font.width(seg.text()) * seg.scale();
            if (totalWidth > maxLen) break;
        }
        if (totalWidth > maxLen) {
            totalWidth = (float) maxLen;
        }
        Vec3 farest = currentPos.subtract(entry.motion.normalize().scale(totalWidth * LABEL_SCALE));
        return camera.cullFrustum.pointInFrustum(farest.x, farest.y, farest.z);
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
                    String prefix = font.plainSubstrByWidth(seg.text(), (int) Math.ceil(unscaledClip));
                    String remainingText;
                    if (unscaledClip < 1e-3)
                        remainingText = seg.text();
                    else if (prefix.length() >= seg.text().length())
                        remainingText = "";
                    else
                        remainingText = seg.text().substring(prefix.length() + 1);

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
