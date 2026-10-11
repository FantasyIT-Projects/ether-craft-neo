# 游戏内测试流程（In-Game Testing）

> **定位**：在**真实客户端 + 真实世界**中验证 mod 行为，覆盖 GameTest（无渲染、无玩家、纯服务端）触及不到的场景。
> **约束**：**不改 mod 源码、不依赖任何 mod API**，因此本流程对任意 Minecraft mod 项目通用。
> **原则**：后台操作（不抢焦点）、程序化断言、测试后完整恢复现场。

## ⚠️ 铁律：任何操作都不需要让游戏窗口获得焦点

> **这一条优先级最高，排查任何注入类故障时的第一原则。**
>
> - 注入一律走 `PostMessage`（后台消息）：**最小化、非前台、被遮挡都能收到**。
> - **绝对不要尝试“激活 / 前置 / 恢复 / resume 游戏窗口”来让注入生效或排查故障** —— 那既无必要，还会立刻触发 Minecraft 的 `grabMouse()` 抢走鼠标（§2.4），把问题搞得更糟。
> - 从非前台进程调用 `SetForegroundWindow` / `LockSetForegroundWindow` 本来就会**返回 False**，这条路上没有可行空间。
> - 唯一“投不进去”的真实原因是 UIPI（目标进程完整性级别更高，见 §2.5）。故障排查请从这个方向找，而不是去抢焦点。

---

## 1. 前提条件

| 项 | 要求 | 原因 |
| --- | --- | --- |
| 测试存档 | 已开启作弊（`level.dat` 中 `allowCommands=1`） | 否则无法在游戏内执行命令 |
| 测试存档 | 失焦不暂停（`pauseOnLostFocus=false`） | 窗口不在前台时游戏仍需继续 tick |
| 隔离 | 始终使用**存档副本** | 原存档零改动，测完删除副本 |
| 进程 | 启动前无残留游戏进程 | 避免存档锁与资源冲突 |
| 显示 | 窗口化（非独占全屏） | 截图链路的兜底方案依赖窗口渲染 |
| 鼠标 | 启动后立即最小化窗口 | 防止 `grabMouse()` 把光标夹进窗口（见 §2.4） |

存档副本、启动参数等**均属临时现场**，测试结束必须还原，见 §7。

---

## 2. 三条核心链路

### 2.1 命令注入：后台窗口消息（不抢焦点）

直接向游戏窗口投递窗口消息，**无需将窗口置前、无需焦点**（窗口最小化时同样有效），不影响用户正在进行的其他操作。**任何时候都不要为了让注入生效去激活窗口**（见开篇铁律）。

| 动作 | 消息 | 参数 |
| --- | --- | --- |
| 打开聊天框 | `WM_KEYDOWN` + `WM_KEYUP` | `VK_T` = 0x54 |
| 输入文本 | `WM_CHAR` | 逐字符；`wParam` 为字符码 |
| 提交命令 | `WM_KEYDOWN` + `WM_KEYUP` | `VK_RETURN` = 0x0D |

要点：

- `WM_KEYDOWN/UP` 的 `lParam` **必须包含 scan code**（bit 16–23），由 `MapVirtualKey(vk, 0)` 生成，否则引擎（GLFW）无法正确识别按键。
- 文本走 `WM_CHAR` 而非按键模拟，可**绕过键盘布局与大小写问题**，直接输入命令所需字符。
- 逐字符发送间隔 20–40 ms；每条命令之间留 ≥ 700 ms，等上一条提交后再开下一条。

### 2.2 状态读取：游戏日志

游戏日志提供两条**可程序化断言**的通道（无需截图、无需读像素）：

| 日志标记 | 格式 | 用途 |
| --- | --- | --- |
| `COMMAND_TRACING` | `Command is executed. Command: '<命令>', executor: ...` | 确认命令**确实被执行**（排除注入失败） |
| `ChatComponent` | `[CHAT] <反馈文本>` | 读取命令**输出**（注意为本地化文本） |

先以 `COMMAND_TRACING` 判定“命令是否执行”，再以 `[CHAT]` 判定“执行结果”，二者构成完整的执行—反馈闭环。

### 2.3 截图：F2

后台注入 `VK_F2`（0x71），游戏将 PNG 落盘至 `run/screenshots/`，文件名形如 `<日期>_<时间>.png`。

获取新截图的方式：**记录目录快照 → 触发 → 比对差集**，不要把固定文件名写死。

---

### 2.4 启动即最小化：防止抢占鼠标

Minecraft 进入世界时会调用 `MouseHandler.grabMouse()`，把光标甩到窗口中心并把 `ClipCursor` 夹进窗口矩形，会直接打断用户。其唯一守卫是：

```java
public void grabMouse() {
    if (this.minecraft.isWindowActive() && !this.mouseGrabbed) { ... }
}
```

`isWindowActive()` 等价于 `window.isFocused()`，而 GLFW 的 focused 由 **WM_SETFOCUS / WM_KILLFOCUS（键盘焦点）** 驱动，**不是前台窗口**。因此：

- `WS_EX_NOACTIVATE` 这类“阻止成为前台窗口”的手段**无效**；
- 从**非前台进程**调用 `SetForegroundWindow` / `LockSetForegroundWindow` 一律**返回 False**，挪不走焦点；
- **`ShowWindow(hwnd, SW_MINIMIZE)` 不需要前台权限**，且必定剥夺焦点 —— 这是可靠做法。

**时机**：窗口出现约 1 秒后即可最小化，远早于进入世界（约 60 秒），因此进入世界时 `isWindowActive()` 已为 false，`grabMouse()` 被直接跳过。

**实测验证**：

| 指标 | 结果 |
| --- | --- |
| 最小化后 `hwndFocus` | 0（无键盘焦点） |
| 触发 `setScreen(null)`（即 `grabMouse()` 调用点） | 光标未被夹取 |
| 世界 tick（最小化期间） | 16 秒推进 319 刻（满速 320） |
| 命令注入 | 不受影响 |

**代价**：窗口不可见，F2 截图失效；需要观察时再恢复窗口（恢复并获得焦点后 Minecraft 会照常抓鼠标，属原生行为）。

---

### 2.5 输入注入的完整编码与时序

> 与 GUI 无关的部分，就是「`PostMessage` + 正确的 `lParam` 编码 + tick 级时序 + 日志/截图断言」；**坐标是绝对还是相对、要不要做单位换算，才是唯一 GUI 相关的部分**。

#### 投递方式

- 用 **`PostMessage` 而不是 `SendMessage`**：异步入队、不阻塞调用方、**不需要焦点**；同一窗口的消息按投递顺序处理，所以 `MOVE → DOWN → UP` 的先后有保证。
- **句柄每次重取**（`(Get-Process -Id <pid>).MainWindowHandle`）：窗口重建 / 最小化恢复后可能变化，**不要缓存**。

#### 消息编码（最容易写错的部分）

| 动作 | 消息 | wParam | lParam |
| --- | --- | --- | --- |
| 按下 | `WM_KEYDOWN` 0x100 | VK 码 | `1 \| scan<<16` |
| 抬起 | `WM_KEYUP` 0x101 | VK 码 | `1 \| scan<<16 \| bit30 \| bit31` |
| 字符 | `WM_CHAR` 0x102 | 字符码 | `1 \| scan<<16`（基本被忽略） |
| 移动 | `WM_MOUSEMOVE` 0x200 | 0 | `x \| y<<16` |
| 左键 | `WM_LBUTTONDOWN` 0x201 / `UP` 0x202 | 1 / 0 | 同上 |
| 右键 | `WM_RBUTTONDOWN` 0x204 / `UP` 0x205 | 2 / 0 | 同上 |

- **scan code 必须有**：`MapVirtualKey(vk, 0) << 16`，否则 GLFW 无法识别按键。这是“消息发出去了但游戏没反应”的**头号原因**。
- **文本一定走 `WM_CHAR`**，不要用按键模拟 —— 可绕过键盘布局、Shift、输入法；并且 `WM_KEYDOWN` **不会**自动产生 `WM_CHAR`，必须自己补发。

#### 时序

输入是被游戏**按 tick（50 ms）轮询**的，所以：

- 字符间隔 **20–40 ms**；
- 打开聊天框后等 **~350 ms**；
- 每条命令之间 **≥ 700 ms**；
- 同一 tick 内到达的 down/up **可能被合并或漏掉**。

#### 唯一真正“投不进去”的情况

**UIPI**：目标进程完整性级别更高（游戏以管理员运行，而注入进程没有）时，`PostMessage` 会被**静默拒绝**。其余情况（最小化、非前台、被遮挡）都能收到。

#### 断言方式

不要靠“我以为点到了”：**注入 → 等 → 查日志**（`COMMAND_TRACING` 证明命令执行、`[CHAT]` 读结果）；视觉类再用**截图目录差集**定位新文件，别写死文件名。

---

## 3. 标准流程

### 阶段 0 · 准备
1. 确认无残留游戏进程。
2. 复制目标存档为临时副本（副本名用纯 ASCII，避免匹配问题）。
3. 备份启动参数文件，按需追加“自动进入世界”的启动参数。
4. 记录待断言日志与截图目录的当前状态（作为差集基线）。

### 阶段 1 · 启动
1. 以**非阻塞**方式通过项目现有客户端运行配置（或等价命令）启动，并拿到进程 ID。
2. **窗口出现后立即最小化**（§2.4）：一次性操作，之后不做任何干预。

### 阶段 2 · 等待就绪
轮询日志，以世界加载完成的**特征行**判定就绪；不要用固定 sleep 硬等。同时可用进程的主窗口句柄确认窗口已创建。

### 阶段 3 · 执行测试步骤
按“注入命令 → 等待 → （可选）截图”的节奏推进。步骤之间保留足够 tick 间隔（视角或状态变更可能存在插值/延迟）。

### 阶段 4 · 断言
优先用日志断言（§5.1）；纯视觉内容再补截图断言（§5.2）。**先确认命令执行，再判断结果。**

### 阶段 5 · 恢复现场
按 §7 清单逐项还原，并做一次一致性检查。

---

## 4. 命令注入参考实现（Windows / PowerShell）

```powershell
Add-Type -TypeDefinition @'
using System;
using System.Runtime.InteropServices;
public class BgSend {
  [DllImport("user32.dll")] public static extern bool PostMessage(IntPtr h, uint msg, IntPtr wp, IntPtr lp);
  [DllImport("user32.dll")] public static extern uint MapVirtualKey(uint code, uint mapType);
  public const uint WM_KEYDOWN = 0x100, WM_KEYUP = 0x101, WM_CHAR = 0x102;
  public static void Key(IntPtr h, int vk, bool up) {
    uint sc = MapVirtualKey((uint)vk, 0);
    long lp = 1L | ((long)sc << 16) | (up ? (((long)1 << 30) | ((long)1 << 31)) : 0);
    PostMessage(h, up ? WM_KEYUP : WM_KEYDOWN, (IntPtr)vk, (IntPtr)lp);
  }
  public static void Press(IntPtr h, int vk) {
    Key(h, vk, false); System.Threading.Thread.Sleep(40);
    Key(h, vk, true);  System.Threading.Thread.Sleep(40);
  }
  public static void Type(IntPtr h, string s) {
    foreach (char c in s) {
      uint sc = MapVirtualKey((uint)c, 0);
      PostMessage(h, WM_CHAR, (IntPtr)c, (IntPtr)(1L | ((long)sc << 16)));
      System.Threading.Thread.Sleep(28);
    }
  }
}
'@

# 动态获取窗口句柄（不要写死；窗口重建后句柄会变）
$h = (Get-Process -Id <PID>).MainWindowHandle

[BgSend]::Press($h, 0x54)        # T   ：打开聊天框
[BgSend]::Type($h, '<命令>')      # 逐字符输入命令（含前导 /）
[BgSend]::Press($h, 0x0D)        # Enter：提交
[BgSend]::Press($h, 0x71)        # F2  ：截图
```

> 窗口句柄可用 `Get-Process -Id <PID>` 的 `MainWindowHandle` 获取；每次注入前都重新读取一次，避免使用过期句柄。

---

## 5. 断言模式

### 5.1 日志断言（首选）

1. **执行确认**：在日志中检索 `COMMAND_TRACING`，命令原文应与注入内容一致——这是“注入成功”的硬证据。
2. **结果确认**：检索 `[CHAT]`，按命令的**本地化输出文案**匹配期望值。
3. **状态确认**：用只读命令直接导出目标状态（例如读取方块数据、实体数据、物品内容），把输出当作可解析的断言素材。

### 5.2 视觉断言（补充）

1. 注入 F2 截图，取目录差集定位新文件。
2. 读取 PNG 进行人眼/视觉比对，或在需要时做像素级比较。
3. 截图包含 HUD 与聊天栏，本身也是“命令反馈”的第二证据源。

---

## 6. 已知陷阱

1. **启动参数文件末尾无换行**：追加参数时会与最后一行粘连（可能被注释符吞掉）。追加前确认文件以换行结尾，或改为“按行读入 + 按行重写”。
2. **自动进世界参数按文件夹名匹配**：存档显示名可能与文件夹名不同，判定以**文件夹**为准。
3. **命令输出是本地化文本**：断言关键字须用游戏当前语言的实际文案；执行与否以 §2.2 的执行日志为准。
4. **相对坐标依赖执行者位置**：真实位置未知时优先使用相对坐标，避免操作落在未加载区块或错误位置。
5. **日志/反馈存在滞后**：注入后保留数百毫秒再读取，必要时轮询等待。
6. **绝对不要尝试让窗口获取焦点**：注入不依赖焦点（最小化也能收到），后台消息链路是刻意选择。抢焦点既打断用户，又会触发 `grabMouse()` 抢鼠标（§2.4）；而且从非前台进程调用 `SetForegroundWindow` 本来就会失败。**排查注入故障时不要走这条路**。
7. **避免破坏性操作**：测试中禁止会破坏世界的操作（例如左键破坏方块）；优先用命令直接构造与读取状态。
8. **临时现场必须可还原**：任何新增文件、修改的参数、复制的存档，都要在动手前记入还原清单。
9. **非前台进程抢不动焦点**：`SetForegroundWindow` / `LockSetForegroundWindow` 从非前台进程调用返回 False；要剥夺焦点请用 `ShowWindow(SW_MINIMIZE)`（§2.4）。

---

## 7. 恢复现场清单

- [ ] 关闭本次启动的游戏进程，并确认已退出
- [ ] 删除临时存档副本
- [ ] 还原启动参数文件（并删除备份）
- [ ] 删除本次生成的截图等临时产物
- [ ] 确认无新增崩溃报告
- [ ] 确认原存档与源码未被改动（可用版本控制状态复核）
- [ ] 确认无残留后台任务 / 进程

---

## 8. 与 GameTest 的分工

| 维度 | GameTest（服务端） | 本流程（游戏内） |
| --- | --- | --- |
| 运行环境 | 无渲染、无玩家的测试服务器 | 真实客户端 + 真实世界 |
| 适用 | 纯逻辑（数据、算法、注册、匹配） | 渲染、GUI、玩家交互、真实世界状态 |
| 断言 | 代码内直接断言 | 日志 + 截图 |
| 成本 | 低、快 | 高、慢 |
| 定位 | 回归主力 | 场景补充 |

建议：能用 GameTest 覆盖的逻辑一律用 GameTest；只有必须“真的在游戏里跑一遍”的场景才走本流程。

---

## 9. 实测补充与修正（2026-09-15 · ether_craft 树形浮层）

以下结论均在 MC 26.1.2 / Windows 上复核过（起始窗口 698×422，后调整为 860×420；DPI 120 → 内容缩放 1.25）。

### 9.1 F2 截图与最小化（修正 §2.4 的“F2 失效”说法）

| 状态 | 结果 |
| --- | --- |
| 启动即最小化、**窗口从未渲染过** | F2 仍提示“已将截图保存为 …”，但落盘的是 **1×1 空 PNG**（68 字节） |
| 曾经显示过一次（哪怕之后又最小化） | F2 得到**完整截图**，尺寸 = 帧缓冲尺寸，且内容是最新帧 |

⇒ §2.4 的结论应改为：“最小化不必然导致截图失效；**只有帧缓冲尚未建立时**才会得到 1×1 图”。
**断言前必须校验尺寸**（PNG 头部 / `read_image`），否则会拿着 1×1 图白忙。

### 9.2 鼠标消息的粒度（重要）

| 输入 | 未聚焦（后台/最小化） | 说明 |
| --- | --- | --- |
| 左/中/右键**点击** | ✅ 有效 | 世界内右键开容器 GUI、GUI 内点按钮/滑槽都实测通过 |
| 滚轮 | ✅ 有效 | 但见 §9.3 的 lParam 要求 |
| **移动 / 拖拽 / 悬停** | ❌ 无效 | `MouseHandler.handleAccumulatedMovement()` 整块被 `isWindowActive()` 门控 |

`isWindowActive()` = GLFW `focused` = **WM_SETFOCUS / WM_KILLFOCUS（键盘焦点）**，与“前台窗口”无关。
实测：`GetForegroundWindow()` 已命中游戏窗口（并用 `AttachThreadInput` + `SetForegroundWindow` 强行前置）后，
`isWindowActive()` 仍为 false —— **拖拽类交互无法在后台窗口上测试**；要测必须让窗口真正获得键盘焦点
（代价是 `grabMouse()` 抓走鼠标，见 §2.4）。

### 9.3 滚轮：`WM_MOUSEWHEEL` 的 lParam 必须带屏幕坐标

`WM_MOUSEWHEEL` 的 `lParam` 语义是**屏幕坐标**（不是客户区）。若传 0，GLFW 会把光标位置重置到原点，
MC 随后按错误位置派发滚轮事件（表现为“滚到了别的控件/热栏”，目标控件收不到）。

```csharp
// 目标客户区坐标 (cx, cy) → 屏幕坐标
RECT r; GetWindowRect(h, out r);
int sx = r.Left + cx, sy = r.Top + cy;
PostMessage(h, WM_MOUSEWHEEL, (IntPtr)((long)(delta & 0xFFFF) << 16),
            (IntPtr)((long)(uint)(sx & 0xFFFF) | ((long)(uint)(sy & 0xFFFF) << 16)));
```

### 9.4 窗口尺寸 / GUI 缩放（决定浮层有没有空间）

`com.mojang.blaze3d.platform.Window#calculateScale(maxScale, enforceUnicode)`：

```java
for (scale = 1; scale != maxScale && scale < fbW && scale < fbH
     && fbW / (scale + 1) >= 320 && fbH / (scale + 1) >= 240; ++scale) {}
```

- 帧缓冲宽/高每翻一倍才允许升一级，且必须满足 320×240 的最小 GUI 空间，否则停在低倍率。
- 854×480 帧缓冲、`guiScale=4` 时实际倍率只有 **2×**；把帧缓冲高度压到 < 480（客户区高度 < 384 物理像素）即可降到 **1×**。
- 240 GUI 高的大型界面（如 7×7 工厂 GUI）需要 1× 才能完整显示 + 给上方浮层留出高度。

坐标换算（DPI 上例中 framebuffer/window ≈ 1.25）：

```
scaled   = clientPx * (framebufferW / windowW)   // move/click 注入用 clientPx
clientPx = imagePx  * (windowW / framebufferW)   // 从截图量到像素后反推
```

### 9.5 JEI 会吞掉字母键

GUI 内注入字母（A/W/S/D…）会被 JEI 的搜索框捕获，目标界面收不到按键。
**测键盘操作请用方向键/功能键**，或先用鼠标点一下非输入类控件。

### 9.6 用世界数据包搭建场景（强烈推荐）

比几十条 `/data modify` 聊天命令快一个数量级（且不受聊天框 256 字符限制）：

- 目录：`saves/<世界>/datapacks/<名字>/`，必须含 `pack.mcmeta`；
- `pack_format` 取 `version.json` 的 `pack_version.data_major`（26.1 = **101**）；
- 函数放 `data/<命名空间>/function/<名>.mcfunction`（1.21+ 为单数 `function`）；
- 世界运行中也能加载：写入文件 → `/reload` → `/datapack list` 出现 `file/<名字>（世界）` → `/function …`；
- 实测一个 function 里 35 行 `setblock` + `data modify block … content[i] set value {…}` 即可一次性灌好含芯片（含连接芯片）的工厂布局。

### 9.7 注入脚本

参考实现保留在 `build/agent-test/driver.ps1`（`build/` 已被忽略，不入库）：步骤脚本支持
`cmd:/…`、`key:0x…`、`shot`、`move:x,y`、`click:l|r|m`、`scroll:delta`、`drag:x1,y1,x2,y2`、
`show`（SW_SHOWNOACTIVATE，不抢焦点）、`focus`、`min`、`size:w,h`、`info`。

- 每个 `cmd:` 约 1.2 s + 8 ms/字符，**单批命令别超过 ~10 条**，否则前台调用 200 s 超时；
- 句柄每次重取（`(Get-Process -Id <pid>).MainWindowHandle`）。

