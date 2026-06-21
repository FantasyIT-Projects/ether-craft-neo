# Ether Stream Label — 多段大小/颜色渲染设计

## 目标

支持玩家在成书（Written Book）中输入标记语法，使 Ether Stream 标签能够**混合编排不同大小和颜色**的文本片段。

## 当前局限

- `EtherStreamLabelData` 只存一个 `Component label` + 一个 `int labelColor`
- `EtherStreamLabelLogic` 将整段文本作为一个 `FormattedCharSequence` 渲染，颜色/缩放无法局部变化

## 语法设计（方案 A）

采用 `[attr=value]` BBCode 风格标签，**玩家直接在成书中输入**：

```
正常字 [color=red]红色字[/] [size=1.5]大号字[/]
[color=#00FF00,size=2]绿色大号字[/] [size=0.7]小号字[/]
```

### 规则

| 语法 | 含义 | 默认值 |
|------|------|--------|
| `[size=X]` | 字号倍数（float） | `1.0` |
| `[color=X]` | 颜色，命名色或 `#RRGGBB` | 无（使用 `labelColor`） |
| `[color=X,size=Y]` | 组合属性 | — |
| `[/]` | 重置为默认值 | `size=1.0, color=null` |
| `[[` | 转义为字面 `[` | — |
| `]]` | 转义为字面 `]` | — |

- 不支持嵌套（`[/]` 总是回到初始状态，而非栈式弹出）
- 属性顺序不敏感：`[color=red,size=2]` 等价于 `[size=2,color=red]`
- color 值使用 `TextColor.parseColor()` 解析，支持全部的 Minecraft 命名色（`red`, `blue`, `dark_green` 等）和 hex 格式 `#RRGGBB`

## 数据模型变更

### `EtherStreamLabelData` — 新增 `Segment` 与解析

```java
public record Segment(String text, float scale, @Nullable TextColor color) {}

// 新增字段（transient，不参与序列化）
private List<Segment> parsedSegments;

// 解析入口
public List<Segment> getSegments() {
    if (parsedSegments == null)
        parsedSegments = parseSegments(label.getString());
    return parsedSegments;
}
```

- **序列化不变**：网络/CODEC 仍传输 `Component label + int labelColor`，向后兼容
- **解析时机**：客户端首次读取时懒解析（在 `getSegments()` 中），不增加网络负担
- **color 为 null** 表示使用 `labelColor`

### Parser 算法

```
state = {scale: 1.0, color: null}
segments = []
buffer = ""

从左到右遍历字符:
  if 遇到 '[':
    if 下一个字符是 '[':
      buffer += '['; continue
    flush buffer 为当前 state 的 segment
    解析标签内容:
      if "[/]":
        state = {scale: 1.0, color: null}
      else:
        解析 key=value 对, 更新 state
  else if 遇到 ']' 且下一个是 ']':
    buffer += ']'; continue
  else:
    buffer += char

最后 flush buffer
```

## 渲染变更

### `EtherStreamLabelLogic.onRender()` — 逐段渲染

```java
// 替代原来的单次 submitText
float cursorX = 0;
for (Segment seg : labelData.getSegments()) {
    poseStack.pushPose();
    poseStack.translate(cursorX, 0, 0);
    poseStack.scale(seg.scale(), seg.scale(), 1);

    int color = seg.color() != null ? seg.color().getValue() : labelData.labelColor;
    FormattedCharSequence text = FormattedCharSequence.forward(seg.text(), Style.EMPTY);
    collector.submitText(poseStack, 0, 0, text, false,
            Font.DisplayMode.NORMAL, 0xF000F0, color, 0, 0);

    poseStack.popPose();
    cursorX += font.width(seg.text()) * seg.scale();
}
```

### 死亡动画 — 逐段右裁切

```java
int clipPixels = (int) ((60f - deathTick) * motion.length() * 100);
renderSegmentsClipped(segments, clipPixels);
```

算法：从右向左遍历 segment，累计裁切直到 `clipPixels` 耗尽，末段部分裁切用 `font.plainSubstrByWidth(text, remainingUnscaledPixels)` 处理。

## 边界情况

1. **空标签** `[]` → 忽略，不改变状态
2. **无效属性值** `[color=invalid]` → `TextColor.parseColor()` 抛异常 → 视为不解析，该标签保持原样作为文本输出（玩家能看到原始标记，便于调试）
3. **未闭合标签** 文本结束但 `[/]` 缺失 → 当前 state 保持到文本结束，合理
4. **color 与 labelColor 关系**：segment.color 为 null 时回退到 `labelColor`，使 color 标签改变的颜色始终为绝对色值覆盖

## 需要修改的文件

| 文件 | 改动 |
|------|------|
| `stream/data/EtherStreamLabelData.java` | 添加 `Segment` record、`parseSegments()`、`getSegments()` |
| `stream/client/extra/EtherStreamLabelLogic.java` | 重构 `onRender()` 为逐段渲染，重构死亡动画裁切 |
