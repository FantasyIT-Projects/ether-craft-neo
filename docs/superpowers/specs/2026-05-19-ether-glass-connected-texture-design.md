# Ether Glass 连接纹理设计

基于 AE2 QuartzGlass 连接纹理实现方案，为 ether_glass 添加运行时连接纹理渲染。

## 目标

将 ether_glass 从普通 Block（cube_all 模型）升级为具有连接纹理的玻璃方块：
- 相邻同类玻璃方块之间面完全隐藏（无缝连接）
- 与非同类方块相邻的边缘显示边框
- 使用 AE2 风格的 4-bit 边框掩码系统

## 文件变更

```
block/glass/
├── EtherGlassBlock.java          ← 新增：方块类，继承 TransparentBlock
├── EtherGlassState.java          ← 新增：不可变渲染状态容器
└── render/
    ├── EtherGlassModel.java      ← 新增：DynamicBlockStateModel 实现
    └── RenderHelper.java         ← 新增：面顶点生成工具

assets/ether_craft/textures/block/glass/
├── ether_glass.png               ← 新增：面材质 (16×16)
├── ether_glass_frame0001.png     ← 新增：4-bit 边框纹理 (共15张)
├── ...
└── ether_glass_frame1111.png
```

### 修改文件

| 文件 | 变更 |
|------|------|
| `register/BlockRegistry.java` | `ETHER_GLASS` 构造改为 `new EtherGlassBlock(...)`，添加 `noOcclusion()` + `SoundType.GLASS` |
| `register/BERRegister.java` | 重命名为 `ClientRenderRegister`，添加 `RegisterModelLoadersEvent` 订阅 |
| `datagen/ModelDataGen.java` | `createTrivialCube` → 生成自定义 blockstate JSON 引用 `ether_craft:ether_glass` 模型类型 |
| 所有引用 `BERRegister` 的文件 | 更新 import |

## 架构

### EtherGlassBlock

继承 `TransparentBlock`，覆写 `skipRendering`：相邻方块是同类 `EtherGlassBlock` 时隐藏中间面。使用 `getRenderShape() ==` 防御性检查以兼容 FramedBlocks 等模组。

### EtherGlassState

不可变数据容器，存储：
- `int[] masks[6]` — 每面的 4-bit 边框掩码（位 0=上,1=右,2=下,3=左）
- `boolean[] adjacentGlassBlocks[6]` — 该面是否邻接同类方块

提供 `DEFAULT` 静态实例（四边框全开、无邻接），用于物品渲染等无世界上下文的场景。

### RenderHelper

纯工具类，为 6 个 Direction 生成面对应的 4 个角顶点（`Vector3f`）。使用 `EnumMap<Direction, List<Vector3f>>` 缓存。X/Y/Z 轴各有不同的顶点公式，负方向需要翻转顶点顺序。

### EtherGlassModel（核心）

实现 `DynamicBlockStateModel`，在 `collectParts()` 中运行时检查邻接并选择纹理：

```text
for each Direction face:
  ├─ if hasAdjacentGlassBlock(face) → skip（隐藏连接面）
  ├─ 渲染面材质 quad (ether_glass.png)
  └─ if edgeMask != 0 → 渲染对应边框 quad (ether_glass_frameXXXX.png)
```

**位掩码计算**：对每个面的上/右/下/左四个边缘方向，检查该方向的邻居是否是同类玻璃。使用双向 `isGlassBlock` 检查（`getAppearance` 往返验证）保证对称性。

**边框纹理索引**：4-bit 掩码 `[上,右,下,左]`，值 1~15 对应 `frame0001` ~ `frame1111`。掩码 0 表示无边框，不渲染边框 quad。

**注册**：`Unbaked` record 实现 `CustomUnbakedBlockStateModel`，ID 为 `ether_craft:ether_glass`，通过 `RegisterModelLoadersEvent` 注册 `MapCodec`。

### 注册

`ClientRenderRegister`（原 `BERRegister`）：
- 保留原有 `EntityRenderersEvent.RegisterRenderers` 订阅（BER 注册）
- 新增 `RegisterModelLoadersEvent` 订阅（模型加载器注册）

### 纹理

16 张 16×16 PNG：
- 1 张面材质 `ether_glass.png`
- 15 张边框纹理，命名 `ether_glass_frameXXXX.png`，XXXX 为 4 位二进制补零

边框纹理在透明背景上绘制对应边缘的边框（2~4px 宽），其余部分全透明。面材质由独立 quad 负责。

## 关键设计要点

1. **对称检查**：使用 `state.getAppearance()` 进行双向验证，A 认为连接了 B，B 也必须认为连接了 A
2. **面与边框分离**：面材质和边框是两个独立 quad，互不依赖
3. **物品渲染**：`EtherGlassState.DEFAULT` 四边框全开，物品形式显示完整边框
4. **无 client source set**：所有代码在 `src/main/java/`，客户端代码通过 `@EventBusSubscriber(Dist.CLIENT)` 隔离
