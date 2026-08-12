# JProfiler 性能测试流程

## 一、启动被测进程

1. 在 `build/moddev/clientRunProgramArgs.txt` 末尾追加自动进入存档的参数：
   ```
   --quickPlaySingleplayer
   "存档名"
   ```
2. 通过 IDE 启动 `Client` 运行配置（`waitForExit=false`）
3. 等待 90s 让游戏完全加载

## 二、附加 JProfiler

```bash
# 找到 Minecraft JVM
JProfiler_list_jvms
# → net.neoforged.devlaunch.Main @D:...\clientRunProgramArgs.txt

# 附加并开始录制 CPU
JProfiler_attach(pid=找到的PID)
```

## 三、采集数据

等待 60s 让被测场景充分运行，然后：

```bash
JProfiler_check_status(stopRecording=true)     # 停止录制
# 等 10s
JProfiler_check_status()                       # 确认 data_ready
```

## 四、对比分析

### 服务端 tick 占比（核心指标）

```bash
JProfiler_get_performance_hotspots(
    subsystem="cpu",
    view="call_tree",
    packageFilter="<被测模块包名>"
)
```

在 call tree 中定位被测逻辑占服务端 tick 的比例：
```
MinecraftServer.lambda$spin$0          ← 服务端 tick 总耗时
└─ <被测逻辑入口方法>
   └─ <被测 tick 实现>                 ← 看这个占服务端 tick 的 %
```

### 子方法明细

```bash
JProfiler_get_performance_hotspots(
    subsystem="cpu",
    packageFilter="<被测模块包名>"
)
```

重点关注各子方法的 self-time + children，找出耗时集中点。

### 深入展开

```bash
JProfiler_expand_performance_hotspot(id=展开id)
```

## 五、关闭进程

```bash
Stop-Process -Id PID -Force
```

## 六、还原 arg 文件

删除 `clientRunProgramArgs.txt` 中追加的 `--quickPlaySingleplayer` 和存档名行。

## 注意事项

- JProfiler 每次录制后的 `.jps` 文件是临时的，保存在 `~/.jprofiler16/mcp/` 下，会话关闭后自动删除。如需保留，手动拷贝。
- 不同运行实例的存档场景可能不稳定（对象数量、entity 数量波动），直接对比绝对值有局限。更可靠的方式是看 **占比**（被测逻辑 / MinecraftServer.tick）而非绝对值。
- 采样模式下，self-time 可能因 JIT 内联或方法体过小而无法分离到子调用。当 self-time 居高不下但无子方法明细时，说明是"万箭齐发"型的聚合开销，需从架构层面减量而非降常数。
