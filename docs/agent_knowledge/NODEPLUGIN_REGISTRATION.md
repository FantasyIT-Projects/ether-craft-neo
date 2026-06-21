# NodePlugin 注册指南

## 整体架构

Ether Craft 的 NodePlugin 系统是一个**命令式注册**驱动的插件框架，不依赖注解扫描或 ServiceLoader。所有注册通过三个 Manager 单例的 `collect()` 方法集中完成：

```
┌────────────────────────────────────────────────────────┐
│                    触发时机                             │
│                                                         │
│  TagsUpdatedEvent ──→ NodePluginManager.collect()      │ 服务端/通用
│                    ──→ TabManager.collect()             │ 服务端/通用
│                                                         │
│  FMLClientSetupEvent ──→ PluginRenderManager.collect() │ 仅客户端
└────────────────────────────────────────────────────────┘
```

### 三大注册中心

| 注册中心 | 职责 | 物理端 | 触发事件 | 文件 |
|---|---|---|---|---|
| `NodePluginManager` | 插件定义（类型、ID、构造器、图标、物品匹配） | 通用 | `TagsUpdatedEvent` | `node/NodePluginManager.java` |
| `EtherAdaptNodeUpgradeTabManager` | UI 标签页控件提供者（GUI） | 通用（注册）/ 客户端（使用） | `TagsUpdatedEvent` | `node/EtherAdaptNodeUpgradeTabManager.java` |
| `PluginRenderManager` | 方块模型渲染器 | 仅客户端 | `FMLClientSetupEvent` | `node/PluginRenderManager.java` |

### 插件类型 (PluginType)

```java
enum PluginType {
    FUNCTION,   // 功能插件：消耗物品产生以太（如熔炉发电机）
    FEATURE,    // 特性插件：提供方向性功能（如以太流发射器）
    UPGRADE,    // 升级插件：修改节点属性（如存储扩容）
    DUMMY       // 占位插件：主页面（不占用实际插槽）
}
```

### 核心数据流

```
物品放入容器 ──→ NodePluginManager.getMatchingPluginId()  匹配物品 → 插件 ID
                                        │
                                        ▼
              NodePluginManager.get(id, entity, installed) 构造插件实例
                                        │
                    ┌───────────────────┼───────────────────┐
                    ▼                   ▼                   ▼
            modifyNodeProperty()    tick()            registerSlots()
            (修改节点属性)          (每tick逻辑)       (注册菜单槽位)
```

---

## 1. 服务端/通用注册：NodePluginManager

### 注册入口

在 `TagReloadEvent.onTagReload()` 中触发：

```java
// event/TagReloadEvent.java
@EventBusSubscriber
public class TagReloadEvent {
    @SubscribeEvent
    public static void onTagReload(TagsUpdatedEvent event) {
        NodePluginManager.Instance.collect();
        EtherAdaptNodeUpgradeTabManager.instance.collect();
    }
}
```

### 注册 API

`NodePluginManager` 提供两个 `registerPlugin` 重载：

```java
// 重载1：自动从物品推导匹配谓词
public void registerPlugin(
    PluginType type,          // 插件类型
    Identifier id,            // 唯一标识符
    BiFunction<EtherAdaptNodeEntity, InstalledPlugin, AbstractNodePlugin> plugin,  // 构造器工厂
    ItemLike item             // 匹配物品（用于 UI 图标 + 物品匹配）
)

// 重载2：自定义物品匹配谓词
public void registerPlugin(
    PluginType type,
    Identifier id,
    BiFunction<EtherAdaptNodeEntity, InstalledPlugin, AbstractNodePlugin> plugin,
    Predicate<ItemStack> predicate,  // 自定义匹配逻辑
    ItemLike icon                    // UI 图标
)
```

### 实际注册示例

在 `NodePluginManager.collect()` 中添加注册调用：

```java
public void collect() {
    plugins.clear();
    plugins.add(MAIN_PAGE_INFO);  // 主页面占位插件（必须保留）

    // 注册一个 FUNCTION 类型插件
    registerPlugin(PluginType.FUNCTION,
        FunctionFurnaceGenerator.ID,      // ether_craft:generator/furnace
        FunctionFurnaceGenerator::new,    // 构造器引用
        Items.FURNACE                     // 匹配熔炉物品
    );

    // 注册一个 FEATURE 类型插件
    registerPlugin(PluginType.FEATURE,
        FeatureEtherStreamEmitter.ID,     // ether_craft:ether_stream_emitter
        FeatureEtherStreamEmitter::new,   // 构造器引用
        Items.DISPENSER                   // 匹配发射器物品
    );

    // 自定义谓词的 UPGRADE 注册（示例）
    registerPlugin(PluginType.UPGRADE,
        EtherStorageUpgrade.ID,
        EtherStorageUpgrade::new,
        stack -> stack.is(Items.DIAMOND),  // 自定义匹配：只要是钻石
        Items.DIAMOND
    );
}
```

### 注册数据结构

```java
public record PluginInfo(
    PluginType type,              // 插件类型
    Identifier id,                // 唯一标识符
    BiFunction<EtherAdaptNodeEntity, InstalledPlugin, AbstractNodePlugin> constructor,  // 构造器
    Predicate<ItemStack> predicate,  // 物品匹配谓词
    ItemLike icon                 // UI 图标物品
) {}
```

---

## 2. 客户端注册：PluginRenderManager（渲染器）

### 注册入口

在 `ClientStartupEvent.init()` 中触发（`Dist.CLIENT`）：

```java
// event/ClientStartupEvent.java
@EventBusSubscriber(value = Dist.CLIENT)
public class ClientStartupEvent {
    @SubscribeEvent
    public static void init(FMLClientSetupEvent event) {
        PluginRenderManager.Instance.collect();
    }
}
```

### 注册 API

```java
// 完整渲染器注册
public void register(Identifier id, PluginRender renderer)

// 便捷注册：仅设置静态 Atlas 纹理
public void register(Identifier id, EtherAdapterNodeAtlas.AtlasUV renderer)
```

`PluginRender` 接口签名：

```java
public interface PluginRender {
    void render(
        Direction key,                           // 渲染的面朝向
        Integer dTick,                           // 玩家 tick 计数（用于动画）
        EtherAdaptNodeEntity entity,             // 节点方块实体
        EtherAdapterNodeRenderState state        // 渲染状态（设置纹理、叠加层）
    );
}
```

### 实际注册示例

```java
public void collect() {
    // 方式1：完整自定义渲染逻辑
    register(FunctionFurnaceGenerator.ID, (face, dTick, nodeEntity, state) -> {
        // 读取插件同步数据
        int materialOrdinal = nodeEntity.getSyncedPluginData(
            FunctionFurnaceGenerator.WORKING_MATERIAL);
        WorkingMaterial value = WorkingMaterial.values()[materialOrdinal];

        // 设置该面的基础纹理
        state.setSideAtlas(face,
            value == IDLE ? FUNCTION_BURNER_EMPTY : FUNCTION_BURNER_WORKING);

        // 添加动态叠加层（动画帧）
        if (value == COAL) {
            state.addOverlay(face, OVERLAY_FUNCTION_BURNER_COAL.get(dTick));
        } else if (value == LAVA) {
            state.addOverlay(face, OVERLAY_FUNCTION_BURNER_LAVA.get(dTick));
        }

        // 添加以太填充进度条
        long maxEther = nodeEntity.getMaxEther();
        if (maxEther != 0)
            state.addOverlay(face,
                OVERLAY_FUNCTION_BURNER_FILL.get(
                    (int) Math.min(nodeEntity.getEther() * 10 / maxEther, 9)));
    });

    // 方式2：仅设置静态纹理（无动画叠加层）
    register(somePluginId,
        new AtlasUV(atlasPath, x, y, width, height, imageWidth, imageHeight));
}
```

### 渲染调度流程

```
EtherAdapterNodeRenderer.render() 
    └── 遍历已安装的插件
        └── PluginRenderManager.render(face, installedPlugin, entity, state)
            └── 查找 pluginRenderer Map
                └── 调用注册的 PluginRender::render()
```

---

## 3. 客户端注册：EtherAdaptNodeUpgradeTabManager（GUI 标签页）

### 注册入口

与 `NodePluginManager` 相同，在 `TagReloadEvent` 中触发（通用侧注册，实际实例化在客户端）。

### 注册 API

```java
public void register(
    Identifier identifier,        // 插件 ID
    BiFunction<AbstractNodePlugin, EtherAdaptNodeScreen, BaseEtherNodeTabWidgetProvider<?>> widget
)
```

使用 `wrap()` 辅助方法绕过泛型擦除：

```java
public <T extends AbstractNodePlugin> BiFunction<...> wrap(
    BiFunction<T, EtherAdaptNodeScreen, BaseEtherNodeTabWidgetProvider<T>> construct
)
```

### 实际注册示例

```java
public void collect() {
    widgets.clear();

    // 主页标签
    register(MainPageDummyPlugin.ID,
        wrap(MainPageProvider::new));

    // 燃烧发电机标签
    register(FunctionFurnaceGenerator.ID,
        wrap(ItemConsumeScreen::new));

    // 以太流发射器标签
    register(FeatureEtherStreamEmitter.ID,
        wrap(DirectionalFilterScreen::new));
}
```

### Tab Provider 基类

所有标签控件继承 `BaseEtherNodeTabWidgetProvider<T>`：

```java
public abstract class BaseEtherNodeTabWidgetProvider<T extends AbstractNodePlugin> {
    protected T context;                   // 插件实例
    protected EtherAdaptNodeScreen screen; // 当前屏幕

    // 构造器中注册图像资产和提示区域
    void collectImageAsset(ImageAsset asset, int x, int y);
    void collectTooltipArea(Rect2i area, Supplier<List<Component>> tooltip);

    // 渲染回调
    void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a);
    void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a);
    void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY);

    // 生命周期
    void createWidget();  // 创建交互控件
    void tick();          // 每帧逻辑
}
```

### MainPageProvider 示例

```java
public class MainPageProvider extends BaseEtherNodeTabWidgetProvider<MainPageDummyPlugin> {
    public MainPageProvider(MainPageDummyPlugin menuContext, EtherAdaptNodeScreen screen) {
        super(menuContext, screen);
        // 注册静态图像
        collectImageAsset(EtherAdaptNodeAsset.SLOT_LARGE, 26, 43);
        collectImageAsset(EtherAdaptNodeAsset.ETHER_BAR_CTR, 26, 38);
        // 注册提示区域
        collectTooltipArea(new Rect2i(lx(26), ly(38), 14, 4),
            () -> List.of(Component.translatable("menu.ether_craft.ether_bar_tooltip",
                screen.getMenu().entity.getEther()))
        );
    }

    @Override
    public void extractWidgetRenderState(GuiGraphicsExtractor graphics, ...) {
        super.extractWidgetRenderState(graphics, ...);
        // 渲染以太条进度
        UIUtil.renderEtherBarProgress(
            screen.getMenu().entity.getEther(),
            screen.getMenu().entity.getMaxEther(),
            lx(27), ly(39), 12, 2, graphics
        );
    }
}
```

---

## 4. 插件实现基类：AbstractNodePlugin

所有插件必须继承 `AbstractNodePlugin`：

```java
public abstract class AbstractNodePlugin implements ISyncTargetMenu {
    protected final EtherAdaptNodeEntity nodeEntity;
    public InstalledPlugin installedId;

    public AbstractNodePlugin(EtherAdaptNodeEntity nodeEntity, InstalledPlugin installedId);

    // 必须重写的方法（按需）
    void modifyNodeProperty(NodeProperty nodeProperty);   // 修改节点属性（maxEther, slotUnlock, streamMaxStorage 等）
    void tick();                                          // 每 tick 逻辑
    void saveAdditional(ValueOutput output);              // 插件数据持久化
    void loadAdditional(ValueInput input);                // 插件数据加载
    boolean inputFilter(ItemResource resource);           // 输入物品过滤
    boolean outputFilter(ItemResource resource);          // 输出物品过滤
    int earlyHandleInput(ItemResource, int amount, ...);  // 前置物品处理
    void onDestroy();                                     // 插件销毁回调
    void onWrenchRotate(Direction.Axis axis);             // 扳手旋转回调
    void registerSlots(EtherAdaptNodeContainerMenu menu); // 注册菜单槽位
    void syncScreenData(SyncScreenDataC2S message);       // 客户端→服务端屏幕数据同步

    // 工具方法
    void queueWithCd(Identifier action, int cdTicks, Supplier<Boolean> runnable);  // 带冷却的任务队列
}
```

### NodeProperty 可修改属性

```java
public class NodeProperty {
    public int maxEther;          // 最大以太容量
    public int slotUnlock;        // 解锁的槽位数
    public int streamMaxStorage;  // 以太流最大携带物品数
    public int streamPreventDecay; // 以太流防衰减等级
}
```

### InstalledPlugin 记录

```java
public record InstalledPlugin(
    NodePluginManager.PluginType type,  // 插件类型
    int id,                              // 容器中的槽位索引
    Identifier pluginId                  // 插件唯一 ID
) {}
```

---

## 5. 添加新插件的完整流程

以添加一个新的 FUNCTION 类型插件为例：

### 步骤 1：创建插件类

```java
// node/plugins/function/MyNewFunction.java
public class MyNewFunction extends AbstractNodePlugin {
    public static final Identifier ID = EtherCraft.id("function/my_new");

    public MyNewFunction(EtherAdaptNodeEntity nodeEntity, InstalledPlugin installedId) {
        super(nodeEntity, installedId);
    }

    @Override
    public void tick() {
        // 每 tick 逻辑
    }

    @Override
    public void modifyNodeProperty(NodeProperty nodeProperty) {
        nodeProperty.maxEther += 1000;  // 增加以太容量
    }
}
```

### 步骤 2：在 NodePluginManager 中注册

```java
// NodePluginManager.collect() 中添加：
registerPlugin(PluginType.FUNCTION,
    MyNewFunction.ID,
    MyNewFunction::new,
    Items.STONE  // 用石头作为激活物品
);
```

### 步骤 3：创建 GUI 标签页 Provider

```java
// node/tabs/function/MyNewFunctionScreen.java
public class MyNewFunctionScreen extends BaseEtherNodeTabWidgetProvider<MyNewFunction> {
    public MyNewFunctionScreen(MyNewFunction ctx, EtherAdaptNodeScreen screen) {
        super(ctx, screen);
        // 注册 UI 元素...
    }

    @Override
    public void extractWidgetRenderState(GuiGraphicsExtractor graphics, ...) {
        super.extractWidgetRenderState(graphics, ...);
        // 自定义渲染逻辑...
    }
}
```

### 步骤 4：在 TabManager 中注册标签页

```java
// EtherAdaptNodeUpgradeTabManager.collect() 中添加：
register(MyNewFunction.ID, wrap(MyNewFunctionScreen::new));
```

### 步骤 5（可选）：注册渲染器

```java
// PluginRenderManager.collect() 中添加：
register(MyNewFunction.ID, (face, dTick, nodeEntity, state) -> {
    state.setSideAtlas(face, MY_NEW_TEXTURE);
    // 更多渲染逻辑...
});
```

---

## 6. 架构图

```
                            ┌─────────────────────┐
                            │   TagsUpdatedEvent   │ (服务端+客户端)
                            └─────────┬───────────┘
                                      │
                    ┌─────────────────┼─────────────────┐
                    ▼                                    ▼
    ┌──────────────────────────┐      ┌──────────────────────────┐
    │  NodePluginManager       │      │  TabManager              │
    │  ┌────────────────────┐  │      │  widget Map:             │
    │  │ PluginInfo 列表    │  │      │  pluginId → TabProvider  │
    │  │ - type             │  │      └──────────────────────────┘
    │  │ - id               │  │
    │  │ - constructor      │  │
    │  │ - predicate        │  │      ┌──────────────────────────┐
    │  │ - icon             │  │      │  PluginRenderManager     │
    │  └────────────────────┘  │      │  (Dist.CLIENT)           │
    └──────────┬───────────────┘      │  render Map:             │
               │                      │  pluginId → PluginRender │
               │ 构造插件实例          └──────────┬───────────────┘
               ▼                                  │
    ┌──────────────────────────┐                 │ FMLClientSetupEvent
    │  AbstractNodePlugin      │                 │
    │  ├─ modifyNodeProperty() │                 │
    │  ├─ tick()               │                 │
    │  ├─ inputFilter()        │                 │
    │  ├─ outputFilter()       │                 │
    │  ├─ registerSlots()      │                 │
    │  └─ onDestroy()          │                 │
    └──────────────────────────┘                 │
                                                 ▼
                                   ┌──────────────────────────┐
                                   │  PluginRender::render()   │
                                   │  state.setSideAtlas()     │
                                   │  state.addOverlay()       │
                                   └──────────────────────────┘
```
