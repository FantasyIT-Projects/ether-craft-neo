# Spec: Ether Stream Label Capability

**日期:** 2026-05-24  
**状态:** Design  
**目标:** 为以太流添加可渲染的文字标签 cap，文字在3D世界中固定显示，支持裁切

---

## 1. 概述

新增 `EtherStreamLabelCapability`，挂载在 `EtherStreamEntity` 上。渲染时在以太流实体运动方向的正交面上绘制文字标签，文字右对齐于当前位置，超出起始位置的部分被裁切。

## 2. 组件

### 2.1 `EtherStreamLabelCapability` (新建)

- **路径:** `stream/EtherStreamLabelCapability.java`
- **实现:** `IStreamCapability`
- **ID:** `ether_craft:label` (`EtherCraft.id("label")`)

**字段:**

| 字段 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `label` | `Component` | null | 要渲染的文字内容 |
| `startPos` | `Vec3` | null | 起始位置，裁切基准 |
| `color` | `int` | 0xFFFFFFFF | ARGB 文字颜色 |

**方法:**
- `setLabel(Component)`, `setStartPos(Vec3)`, `setColor(int)` — setters
- `getId()` → `"ether_craft:label"`
- `getConsumption()` → `0`
- `tick(EtherStreamEntity)` → no-op
- `hitEntity(...)` → false
- `hitBlock(...)` → false
- `onDestroy(EtherStreamEntity)` → no-op
- `serialize(ValueOutput)` / `deserialize(ValueInput)` — 持久化 label (序列化为 String)、startPos、color

### 2.2 `EtherStreamEntityRenderState` 扩展 (修改)

新增字段：

```java
Component label;       // 来自 cap
Vec3 startPos;         // 来自 cap
Vec3 motion;           // 实体运动方向
int labelColor;        // 来自 cap，默认 0xFFFFFFFF
```

### 2.3 `EtherStreamEntityRenderer` 修改

**`extractRenderState()` 修改:**
- 检查 `entity.getCapability(EtherStreamLabelCapability.ID)`
- 如果存在，将 label、startPos、color 复制到 state
- 记录 `state.motion = entity.getDeltaMovement()`

**`render()` 修改:**
- 在尾部粒子渲染之后，调用 label 渲染逻辑
- 独立方法 `renderLabel(state, poseStack, camera, bufferSource, font)`

## 3. 渲染算法

### 3.1 文字离屏渲染 → Framebuffer 裁切

```
步骤1: 渲染完整文字到离屏 RenderTarget
步骤2: 计算裁切比例 clipRatio
步骤3: 在世界空间绘制 textured quad，UV 只采样未裁切部分
```

### 3.2 姿态计算

运动方向 V（归一化），当前世界位置 P：

```
正交法线: N = cross(V, UP)   UP = (0,1,0)
若 V 几乎平行于 UP，则 N = cross(V, (1,0,0))

PoseStack:
  translate(P.x, P.y, P.z)
  rotate(让 quad 面朝向 N)
  scale(FACTOR, -FACTOR, FACTOR)   // FACTOR = 0.010416667 (sign scale)
```

### 3.3 裁切计算

```
totalWidth = font.width(label)          // 字体单位
worldDist = |P - startPos|             // 世界单位
fontUnitsAvailable = worldDist / FACTOR // 转换为字体单位

clipRatio = max(0, 1 - fontUnitsAvailable / totalWidth)  // 0~1
```

### 3.4 Quad + UV 裁切

Quad 在局部空间覆盖 `x = [-totalWidth, 0]`，`y = [0, lineHeight]`：

```
UV: u ∈ [clipRatio, 1.0], v ∈ [0, 1]
顶点: 
  (-totalWidth*(1-clipRatio), 0, 0)  uv(clipRatio, 1.0)
  (0,                       0, 0)  uv(1.0,      1.0)
  (0,                  lineH, 0)  uv(1.0,      0)
  (-totalWidth*(1-clipRatio), lineH, 0)  uv(clipRatio, 0)
```

这样 quad 的右侧(0)始终对齐当前位置，左侧按 clipRatio 裁切。

## 4. Framebuffer 管理

- **尺寸:** `textWidth × lineHeight`（`lineHeight = font.lineHeight = 9`）
- **缓存:** `RenderTarget` 实例静态缓存，仅当 label 内容变化（hashCode）或 textWidth 变化时重建
- **每帧流程:** `fb.bindWrite(true)` → `fb.clear(Minecraft.ON_OSX)` → 渲染文字 → `fb.unbindWrite()`
- **文字渲染到 FB:** 使用 `font.drawInBatch(label, 0, 0, color, false, orthoMatrix, bufferSource, NORMAL, 0, FULLBRIGHT)`
- **清理:** 在 `RenderTarget.onDestroy()` 或 `ClientResourceReloadEvent` 释放

## 5. Quad 渲染

- **RenderType:** 自定义或使用 `RenderType.text()` 变体，要求：alpha blending, no cull, no depth write
- **顶点格式:** `DefaultVertexFormat.POSITION_TEX`（位置3f + 纹理UV 2f）
- **光照:** fullbright (`0xF000F0`，同尾部粒子)
- **混合:** `TRANSLUCENT` 或 `ADDITIVE`

## 6. 边界情况

| 情况 | 处理 |
|------|------|
| `label` 为 null | 跳过渲染 |
| `startPos` 为 null | 跳过渲染（无法计算裁切） |
| 运动方向为 (0,0,0) | 跳过渲染（无法确定正交面） |
| V 平行于 (0,1,0) | N = cross(V, (1,0,0)) 避免零向量 |
| `totalWidth == 0` | 跳过渲染（空文字） |
| `clipRatio >= 1.0` | 跳过渲染（完全被裁切） |

## 7. 使用方式

节点升级插件在发射以太流时添加 cap：

```java
// 在 FeatureEtherStreamEmitter.tickOutput() 或 IEtherStreamCapabilityProviderPlugin 中
EtherStreamLabelCapability labelCap = new EtherStreamLabelCapability();
labelCap.setLabel(Component.literal("物品运输中"));
labelCap.setStartPos(entity.position());
labelCap.setColor(0xFFFFFFFF);
entity.addCapability(labelCap);
```

## 8. 文件清单

| 操作 | 文件路径 |
|------|---------|
| **新建** | `src/main/java/studio/fantasyit/ether_craft/stream/EtherStreamLabelCapability.java` |
| **修改** | `src/main/java/studio/fantasyit/ether_craft/entity/render/EtherStreamEntityRenderState.java` |
| **修改** | `src/main/java/studio/fantasyit/ether_craft/entity/render/EtherStreamEntityRenderer.java` |
