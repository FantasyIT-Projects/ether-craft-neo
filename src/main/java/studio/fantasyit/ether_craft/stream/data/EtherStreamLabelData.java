package studio.fantasyit.ether_craft.stream.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;
import studio.fantasyit.ether_craft.EtherCraft;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class EtherStreamLabelData implements IEtherStreamSyncedData {
    public static final Identifier ID = EtherCraft.id("label");

    public static final MapCodec<EtherStreamLabelData> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            ComponentSerialization.CODEC.fieldOf("label").forGetter(t -> t.label),
            Codec.INT.fieldOf("labelColor").forGetter(t -> t.labelColor),
            Direction.CODEC.optionalFieldOf("sourceDirection").forGetter(t -> Optional.ofNullable(t.sourceDirection))
    ).apply(instance, (label, color, dirOpt) -> new EtherStreamLabelData(label, color, dirOpt.orElse(null))));

    public Component label;
    public int labelColor;
    public @Nullable Direction sourceDirection;

    public EtherStreamLabelData(Component label, int labelColor) {
        this(label, labelColor, null);
    }

    public EtherStreamLabelData(Component label, int labelColor, @Nullable Direction sourceDirection) {
        this.label = label;
        this.labelColor = labelColor;
        this.sourceDirection = sourceDirection;
    }

    @Override
    public Identifier getId() {
        return ID;
    }

    @Override
    public void toBuffer(FriendlyByteBuf writer) {
        writer.writeJsonWithCodec(ComponentSerialization.CODEC, label);
        writer.writeInt(labelColor);
        writer.writeNullable(sourceDirection, FriendlyByteBuf::writeEnum);
    }

    public static EtherStreamLabelData fromBuffer(FriendlyByteBuf reader) {
        return new EtherStreamLabelData(
                reader.readLenientJsonWithCodec(ComponentSerialization.CODEC),
                reader.readInt(),
                reader.readNullable(buf -> buf.readEnum(Direction.class))
        );
    }

    public record Segment(String text, float scale, @Nullable TextColor color) {}

    @Nullable
    private List<Segment> parsedSegments;

    public List<Segment> getSegments() {
        if (parsedSegments == null) {
            parsedSegments = parseSegments(label.getString());
        }
        return parsedSegments;
    }

    private List<Segment> parseSegments(String raw) {
        List<Segment> segments = new ArrayList<>();
        StringBuilder buffer = new StringBuilder();
        float scale = 1.0f;
        TextColor color = null;

        int i = 0;
        while (i < raw.length()) {
            char c = raw.charAt(i);
            if (c == '[') {
                if (i + 1 < raw.length() && raw.charAt(i + 1) == '[') {
                    buffer.append('[');
                    i += 2;
                    continue;
                }
                if (!buffer.isEmpty()) {
                    segments.add(new Segment(buffer.toString(), scale, color));
                    buffer.setLength(0);
                }
                int end = raw.indexOf(']', i + 1);
                if (end == -1) {
                    buffer.append(c);
                    i++;
                    continue;
                }
                String tag = raw.substring(i + 1, end);
                if (tag.equals("/")) {
                    scale = 1.0f;
                    color = null;
                } else {
                    for (String part : tag.split(",")) {
                        part = part.trim();
                        int eq = part.indexOf('=');
                        if (eq == -1) continue;
                        String key = part.substring(0, eq).trim().toLowerCase();
                        String val = part.substring(eq + 1).trim();
                        switch (key) {
                            case "size" -> {
                                try {
                                    scale = Float.parseFloat(val);
                                } catch (NumberFormatException ignored) {}
                            }
                            case "color" -> {
                                TextColor parsed = TextColor.parseColor(val).result().orElse(null);
                                if (parsed != null) color = parsed;
                            }
                        }
                    }
                }
                i = end + 1;
            } else if (c == ']') {
                if (i + 1 < raw.length() && raw.charAt(i + 1) == ']') {
                    buffer.append(']');
                    i += 2;
                    continue;
                }
                buffer.append(']');
                i++;
            } else {
                buffer.append(c);
                i++;
            }
        }
        if (!buffer.isEmpty()) {
            segments.add(new Segment(buffer.toString(), scale, color));
        }
        return segments;
    }
}
