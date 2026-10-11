# 工厂树形可视化浮层设计

**Date**: 2026-09-14
**Status**: Implemented

## Goal

在加工中心（Ether Process Factory）GUI 右侧新增一个独立小面板（位于过滤器面板正上方），点击其中的按钮后，
在 GUI 上方（`getTopPos()` 以上的横条区域）绘制**当前工厂布局**的树形可视化：

- 按输出位置（输出槽行号 `outputI`）分组，每组一棵树，纵向堆叠并带分组表头。
- 节点 = 通路上的工艺芯片格；叶子 = 左侧输入槽物品；根 = 该输出物品。
- 不使用槽位（slot）实现，直接绘制 `ItemStack`；支持缩放（滚轮 / `+`、`-`）与拖拽平移（左键拖拽 / WASD、方向键）。

## 关键决策

1. **数据来源为已同步的 path 数据，不新增网络包。**
   `pathBelongings / pathDepth / pathDirection` 与 `pathMaxDepth` 已经过
   `EtherProcessFactoryContainerMenu` 的 DataSlot 同步到客户端，容器内容本身也随菜单同步，
   因此可以在客户端直接重建"当前工厂的树"。
   （前置修复：`EtherProcessFactoryEntity.updateRecipe` 中写入下标已统一为 `[row][col]`，
   与 GUI 读取、保存/更新包方向一致。）

2. **不读取配方匹配结果。** `processingRecipes / processingInputs` 只在服务端存在；
   使用物理布局（通路 + 芯片 + 输入/输出槽）足以表达"当前工厂在做什么"，
   且永远与玩家搭建一致，不会出现客户端/服务端不一致。

3. **默认悬浮位置：GUI 正上方、与 GUI 同宽。** 初始矩形从 `getLeftPos()` 起、宽 `imageWidth`、
   高度取 GUI 上方剩余空间；窗口可拖动（标题栏）与缩放（左/右/下边框 + 右下角抓手），
   尺寸与遮挡完全由玩家决定，**内容高度不再有任何下限**。通用实现在 `util/DraggableWindow`。

4. **复用 treeRecipe 渲染栈，不用槽位。** Layer 1（`TreeDiagramSpec` / `TreeLayoutCalculator`）、
   Layer 2（`TreeDiagramWidget` / `TreeDiagramViewport` / `ViewportInputHandler` / `EdgeBatchRenderer`）
   全部复用；JEI 侧不受影响。

## 架构

```
FactoryTreeModel          (menu/factory)  客户端：读同步数据 -> 每组一棵逻辑树
        |
        v
TreeDiagramSpec (每组一个) + TreeDiagramLayoutComposer (render/data, 纯数据)
        |
        v
FactoryTreeOverlay        (menu/factory)  持有 DraggableWindow + TreeDiagramWidget、绑定 NodeRenderer/表头
        |
DraggableWindow           (util)          通用可拖动窗口：几何 + 拖动/缩放 + 标题栏/边框/抓手
        |
        v
EtherProcessFactoryScreen               小面板 + 按钮 + 渲染/输入接线
```

## 数据到树的映射

对每个输出行 `g`：

| 数据 | 含义 |
|---|---|
| `pathBelongings[row][col] == g` | 该格属于输出 `g` 的通路 |
| `pathDepth[row][col]` | 距输出的步数（越大越靠近输出）→ 父 = `depth + 1` 的邻接格 |
| `pathDepth == pathMaxDepth[g]` | 根格，其父为输出节点 |
| `pathDirection[row][col]` 含 LEFT 且 `col == 0` | 该格向左越界接入输入槽 `row` |
| 该格 4 邻 + 自身的工艺芯片（`effectSide` / `effectSelf`，排除 `separator_chip`） | 芯片节点物品（空则该格为纯通路，折叠） |
| `inputContainer.getItem(row)` | 输入叶子物品（空时用 `filters[row]` 首项画 ghost） |
| `possibleResults.getItem(g)` | 输出物品（空时回退 `outputContainer`） |

> **为什么芯片要从邻格取**：工艺芯片默认 `effect = {separate:true, effectSelf:false, effectSide:true}`，
> 只有 `concat_chip` 是 `separate:false, effectSelf:true`。`separate=true` 的芯片在
> `EtherProcessorRecipeUtil.processFactoryInput` 的 markMatrix 里是 `-1`（墙，不在通路上），
> 靠 `effectSide` 作用于**相邻**的通路格。因此"通路格本身是空格或连接芯片格，普通工艺芯片挂在它旁边"。
> `FactoryTreeModel.chipsOf` 按同一规则收集芯片（与运行期 `scanForTrees` 同源），
> 浮层的树因此与 JEI「以太加工」配方树一致；一个通路格同时被上下两颗芯片作用时，节点会纵向堆叠多个芯片槽。

**连接芯片 = 步骤边界。** 与运行期 `MultiStepBuilder` 的分步语义保持一致：通路上出现 `concat_chip` 时，
该格向下游的部分是一个独立步骤，向上游的部分是另一个独立步骤（其输出经连接芯片送入下游步骤）。
因此一个输出可能对应**多组**（"识别为多个单独配方"），每组各画一棵树：

| 元素 | 含义 |
|---|---|
| `Group.stepIndex` | 同输出下的步骤序号（0 = 最靠近输出的父步骤） |
| `Group.attachRow/attachCol` | 本步骤接入父步骤的连接芯片格；-1 = 本步骤就是最下游步骤 |
| 节点 `b<row>_<col>`（`isBoundary`） | 下游步骤里的连接芯片边界，表示"输入来自上游步骤"（绘制上游产物） |
| 上游步骤的 `out` 节点 | 展示中间产物（运行期写在连接芯片格上的 `possibleIntermediateResults`） |

节点 id 为组内局部的 `c<row>_<col>` / `in<row>` / `b<row>_<col>` / `out`，合成时统一加组前缀 `o<g>s<step>_`。
分组表头显示"输出槽 #g · 步骤 k/N"。
进度与以太配色在渲染时实时读取（`processingProgress[g]`、`maxMultiplier[g]`、`currentEther[row][col]`），
因此物品/进度变化**不需要重建布局**。

**重建节流**：`FactoryTreeModel.structureSignature` 对 path 四个数组 + 通路格芯片身份求哈希，
只有签名变化（或首次打开）才重建。

## 布局合成

`TreeDiagramLayoutComposer` 对每组调用 `TreeLayoutCalculator`，然后：

- 组内根节点 `nextId` 指向显式输出节点（`OutputSpec(0)`），输出以节点形式绘制；
- 丢弃显式输出节点射向 `PositionedOutput` 锚点的悬空边；
- 节点/边按 `(0, yOffset)` 平移到该组的纵向槽位并加组前缀；
- 画布宽度 = 各组最大宽度与视口宽度的较大者，高度 = 各组高度累加 + 表头 + 组间距；
- 表头矩形（高 18、宽 = 画布宽）作为 `TreeDiagramWidget` 的"装饰块"绘制：
  只渲染、不参与命中测试，因此在表头上按住同样可以拖拽平移。

## 通用窗口 `DraggableWindow`

拖动 / 缩放 / 标题栏 / 边框 / 右下角抓手全部抽到 `studio.fantasyit.ether_craft.util.DraggableWindow`，
与工厂无关，可被其它界面复用：

- **内容由调用方负责**：`renderBackground()` → 内容 → `renderChrome()`；
  内容区由 `contentX/contentY/contentWidth/contentHeight` 给出（移动/缩放后随之变化）。
- **输入**：`mouseClicked()` 消费标题栏/边框的按下；`mouseDragged()` 返回
  `DragAction.NONE/MOVE/RESIZE`，调用方据此同步内容区（`RESIZE` 时重排布局）；
  `onMouseReleased()` 结束拖拽。
- `setMinSize` 限制缩放下限，`setShowSize` 控制标题栏右侧的尺寸文本；窗口高度没有下限。

## Layer 2 增量

- `TreeDiagramWidget#addDecoration / clearDecorations / clearBindings`：装饰块与绑定清理，
  既有调用方（JEI）行为不变。
- `TreeDiagramWidget#setBounds` 与 `TreeDiagramViewport#setViewSize / setPan / setMaxZoom`：窗口移动/缩放后
  更新控件位置与视口尺寸（保留当前缩放与平移），并允许浮层放大到 `MAX_ZOOM`（3.0）。
  JEI 侧不调用这些方法、`maxZoom` 保持默认 1.0，行为不变。
- `TreeDiagramWidget#mouseClicked`：命中节点但**没有点击处理器**时不再消费事件，
  继续走视口逻辑，使"从节点上按下"也能拖拽平移。
- `TreeDiagramViewport#setCanvasSize`：`minZoom` 依赖画布尺寸，而 `TreeDiagramWidget` 先以 0×0 构造，
  必须在 `setLayout` 时刷新，否则永远无法缩小（这同时修掉了一个既有缺陷）。

## 输入

| 操作 | 行为 |
|---|---|
| 标题栏左键拖拽 | 移动窗口（clamp 到屏幕内，至少保留 32px 可见） |
| 左/右/下边框、右下角左键拖拽 | 缩放窗口（下限 72×（标题栏+10））；标题栏固定用于移动，不参与缩放 |
| 内容区左键拖拽 | 平移（视口 clamp 到画布范围） |
| 滚轮 | 以鼠标为锚点缩放（`minZoom` ~ 3.0，上限由 `TreeDiagramViewport#setMaxZoom` 指定） |
| `+` / `-` / 小键盘加减 | 以视图中心缩放 |
| WASD / 方向键 | 平移 |
| ESC | 优先关闭浮层（不关闭容器） |
| 打开/关闭 | 小面板中的按钮；重开时保留当前视图状态 |

打开浮层时，屏幕的鼠标事件先交给浮层；`nameEdit` 聚焦时键盘不转发给浮层。

## 边界情况

- 无通路 / 布局非法 → 空态文案，不崩溃。
- 通路上的空芯片格 → 作为纯通路折叠，不产生孤立节点。
- 输入槽为空 → 有过滤器内容时画 ghost，否则画空格子。
- **窗口高度不再有下限**（原 24px 限制已取消）：标题栏始终绘制，内容区按实际尺寸裁剪，
  玩家可随时拖动/缩放窗口。
- 画布小于视图 → 初始 fit 到整张画布 + 居中，且 pan 被 clamp。

## 验证

- `idea_build_project` 编译通过。
- GameTest（`FactoryTreeViewGameTests`，纯逻辑）：
  - `factory_tree_model_fixture`：真实布局（通路＝空行、芯片挂在通路格邻格）建树、父子关系、输入叶子、无芯片折叠、分隔芯片/普通物品不算芯片、无通路空分组；
  - `factory_tree_model_signature`：签名稳定性与变化检测；
  - `factory_tree_layout_compose`：分组前缀、纵向堆叠、悬空输出边丢弃、表头宽度统一。
- `GameTestServer` 全绿（7/7）。客户端渲染与交互观感需人工评估。

## 范围外

- 不新增网络包、不改存档格式、不改配方系统、不改 JEI 渲染。
- 浮层状态不落盘，关闭容器即销毁。
- 不修改既有 GUI 内部的进度叠加层与既有 path 数组读写逻辑。
