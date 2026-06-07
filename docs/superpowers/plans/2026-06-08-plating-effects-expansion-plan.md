# Plating Effects Expansion Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add 5 new plating effects (Dash, High Jump, Soul Projection, No Gravity, Coyote Time) with refactored trigger interfaces and consolidated plating package.

**Architecture:** Split `IPlatingEffect` into 9 granular trigger interfaces under `plating/trigger/`. Move `PlatingEventHandler` and `PlatingTooltipHandler` from `event/` to `plating/`. Use NeoForge `AttachmentType` for player state (soul projection, coyote time). Use `PlatingData.coolDownUntil` for item-level cooldowns. Mixin `LivingEntity.jumpFromGround()` for coyote time. Network packets (`PlatingTriggerC2S`, `PlatingSoulStateS2C`) for right-click triggers and soul state sync.

**Tech Stack:** NeoForge 26.1.2, Minecraft 1.26.1, Java 25

**Execution Dependency Note:** Tasks must be executed in order. Key dependencies:
- `PlatingTriggerC2SHandler` needs `PlatingEventHelper` (Task 11 must precede Task 12)
- `PlatingSoulStateS2C` is self-contained (no cross-file dependency)
- `Network.java` (Task 10) references all packet/handler classes — compile errors expected until all packet/handler files exist
- Full build at Task 25 catches remaining issues

---

## File Structure Map

### Create (19 files)

| # | File | Responsibility |
|---|------|---------------|
| 1 | `plating/trigger/IPlatingAttackTrigger.java` | onAttack trigger interface |
| 2 | `plating/trigger/IPlatingBreakBlockTrigger.java` | onBreakBlock trigger interface |
| 3 | `plating/trigger/IPlatingUseTrigger.java` | onUse trigger interface |
| 4 | `plating/trigger/IPlatingUseOnBlockTrigger.java` | onUseOnBlock trigger interface |
| 5 | `plating/trigger/IPlatingUseOnEntityTrigger.java` | onUseOnEntity trigger interface |
| 6 | `plating/trigger/IPlatingHoldTickTrigger.java` | onHoldTick trigger interface |
| 7 | `plating/trigger/IPlatingRightClickTrigger.java` | Right-click trigger (new) |
| 8 | `plating/trigger/IPlatingArrowShotTrigger.java` | Arrow shot trigger (new) |
| 9 | `plating/trigger/IPlatingJumpTrigger.java` | Jump trigger (new) |
| 10 | `plating/effects/DashPlatingEffect.java` | Dash: burst forward |
| 11 | `plating/effects/HighJumpPlatingEffect.java` | High jump: launch upward |
| 12 | `plating/effects/SoulProjectionPlatingEffect.java` | Soul projection: camera detach |
| 13 | `plating/effects/NoGravityPlatingEffect.java` | No-gravity arrows |
| 14 | `plating/effects/CoyoteTimePlatingEffect.java` | Coyote time jump |
| 15 | `plating/event/PlatingItemEntityTicker.java` | Extracted tickPlating |
| 16 | `plating/client/PlatingClientEventHandler.java` | Client-side soul camera, right-click intercept |
| 17 | `network/c2s/PlatingTriggerC2S.java` | C2S: right-click trigger |
| 18 | `network/s2c/PlatingSoulStateS2C.java` | S2C: soul projection state sync |
| 19 | `mixin/plating/LivingEntityJumpMixin.java` | Mixin: coyote time jump |

### Modify (13 files)

| # | File | Change |
|---|------|--------|
| 20 | `plating/PlatingData.java` | Add `coolDownUntil`, `copyWithCoolDown()`, `isCd()` |
| 21 | `plating/PlatingUtil.java` | Add `updatePlatingData()` |
| 22 | `plating/PlatingManager.java` | Register 5 new effects in `init()` |
| 23 | `plating/effects/IPlatingEffect.java` | Strip to `getEffectByEther` only |
| 24 | `plating/effects/DamagePlatingEffect.java` | `implements IPlatingAttackTrigger` |
| 25 | `event/PlatingEventHandler.java` → `plating/event/PlatingEventHandler.java` | Move + rewrite dispatch, add new events |
| 26 | `event/PlatingTooltipHandler.java` → `plating/client/PlatingTooltipHandler.java` | Move file |
| 27 | `event/ItemEntityTickEvent.java` | Delegate tickPlating to PlatingItemEntityTicker |
| 28 | `network/Network.java` | Register PlatingTriggerC2S + PlatingSoulStateS2C |
| 29 | `Config.java` | Add 8 new config entries |
| 30 | `register/AttachmentDataRegistry.java` | Register `PLATING_PLAYER` attachment |
| 31 | `resources/ether_craft.mixins.json` | Register LivingEntityJumpMixin |
| 32 | `plating/attachment/PlatingPlayerAttachment.java` | New player attachment data class |

---

### Task 1: Trigger Interface Scaffolding (9 interfaces)

**Files:**
- Create: `plating/trigger/IPlatingAttackTrigger.java`
- Create: `plating/trigger/IPlatingBreakBlockTrigger.java`
- Create: `plating/trigger/IPlatingUseTrigger.java`
- Create: `plating/trigger/IPlatingUseOnBlockTrigger.java`
- Create: `plating/trigger/IPlatingUseOnEntityTrigger.java`
- Create: `plating/trigger/IPlatingHoldTickTrigger.java`
- Create: `plating/trigger/IPlatingRightClickTrigger.java`
- Create: `plating/trigger/IPlatingArrowShotTrigger.java`
- Create: `plating/trigger/IPlatingJumpTrigger.java`

**All files below go in `src/main/java/studio/fantasyit/ether_craft/plating/trigger/`**

- [ ] **Step 1: Create `IPlatingAttackTrigger.java`**

```java
package studio.fantasyit.ether_craft.plating.trigger;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import studio.fantasyit.ether_craft.plating.PlatingData;
import studio.fantasyit.ether_craft.plating.effects.IPlatingEffect;

public interface IPlatingAttackTrigger extends IPlatingEffect {
    default boolean onAttack(PlatingData data, ItemStack stack, Player player, Entity target) {
        return false;
    }
}
```

- [ ] **Step 2: Create `IPlatingBreakBlockTrigger.java`**

```java
package studio.fantasyit.ether_craft.plating.trigger;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import studio.fantasyit.ether_craft.plating.PlatingData;
import studio.fantasyit.ether_craft.plating.effects.IPlatingEffect;

public interface IPlatingBreakBlockTrigger extends IPlatingEffect {
    default boolean onBreakBlock(PlatingData data, ItemStack stack, Player player, BlockPos pos, BlockState state) {
        return false;
    }
}
```

- [ ] **Step 3: Create `IPlatingUseTrigger.java`**

```java
package studio.fantasyit.ether_craft.plating.trigger;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import studio.fantasyit.ether_craft.plating.PlatingData;
import studio.fantasyit.ether_craft.plating.effects.IPlatingEffect;

public interface IPlatingUseTrigger extends IPlatingEffect {
    default void onUse(PlatingData data, ItemStack stack, Player player) {
    }
}
```

- [ ] **Step 4: Create `IPlatingUseOnBlockTrigger.java`**

```java
package studio.fantasyit.ether_craft.plating.trigger;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import studio.fantasyit.ether_craft.plating.PlatingData;
import studio.fantasyit.ether_craft.plating.effects.IPlatingEffect;

public interface IPlatingUseOnBlockTrigger extends IPlatingEffect {
    default void onUseOnBlock(PlatingData data, ItemStack stack, Player player, BlockPos pos, BlockState state) {
    }
}
```

- [ ] **Step 5: Create `IPlatingUseOnEntityTrigger.java`**

```java
package studio.fantasyit.ether_craft.plating.trigger;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import studio.fantasyit.ether_craft.plating.PlatingData;
import studio.fantasyit.ether_craft.plating.effects.IPlatingEffect;

public interface IPlatingUseOnEntityTrigger extends IPlatingEffect {
    default void onUseOnEntity(PlatingData data, ItemStack stack, Player player, Entity target) {
    }
}
```

- [ ] **Step 6: Create `IPlatingHoldTickTrigger.java`**

```java
package studio.fantasyit.ether_craft.plating.trigger;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import studio.fantasyit.ether_craft.plating.PlatingData;
import studio.fantasyit.ether_craft.plating.effects.IPlatingEffect;

public interface IPlatingHoldTickTrigger extends IPlatingEffect {
    default void onHoldTick(PlatingData data, ItemStack stack, Player player) {
    }
}
```

- [ ] **Step 7: Create `IPlatingRightClickTrigger.java`**

```java
package studio.fantasyit.ether_craft.plating.trigger;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import studio.fantasyit.ether_craft.plating.PlatingData;
import studio.fantasyit.ether_craft.plating.effects.IPlatingEffect;

public interface IPlatingRightClickTrigger extends IPlatingEffect {
    boolean onRightClick(PlatingData data, ItemStack stack, Player player);
}
```

- [ ] **Step 8: Create `IPlatingArrowShotTrigger.java`**

```java
package studio.fantasyit.ether_craft.plating.trigger;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import studio.fantasyit.ether_craft.plating.PlatingData;
import studio.fantasyit.ether_craft.plating.effects.IPlatingEffect;

public interface IPlatingArrowShotTrigger extends IPlatingEffect {
    void onArrowShot(PlatingData data, ItemStack stack, Player player, AbstractArrow arrow);
}
```

- [ ] **Step 9: Create `IPlatingJumpTrigger.java`**

```java
package studio.fantasyit.ether_craft.plating.trigger;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import studio.fantasyit.ether_craft.plating.PlatingData;
import studio.fantasyit.ether_craft.plating.effects.IPlatingEffect;

public interface IPlatingJumpTrigger extends IPlatingEffect {
    boolean canJump(PlatingData data, ItemStack stack, Player player);
}
```

- [ ] **Step 10: Build and verify**

Run: `idea_build_project` (only trigger files)
Expected: No errors in trigger files.

- [ ] **Step 11: Commit**

```bash
git add src/main/java/studio/fantasyit/ether_craft/plating/trigger/
git commit -m "feat: add plating trigger interfaces (attack, break, use, right-click, arrow, jump)"
```

---

### Task 2: Refactor IPlatingEffect base interface

**Files:**
- Modify: `plating/effects/IPlatingEffect.java`

- [ ] **Step 1: Rewrite IPlatingEffect — strip to getEffectByEther only**

Replace entire file content:

```java
package studio.fantasyit.ether_craft.plating.effects;

public interface IPlatingEffect {
    double getEffectByEther(long ether);
}
```

- [ ] **Step 2: Commit**

```bash
git add src/main/java/studio/fantasyit/ether_craft/plating/effects/IPlatingEffect.java
git commit -m "refactor: strip IPlatingEffect to getEffectByEther only"
```

---

### Task 3: Update DamagePlatingEffect

**Files:**
- Modify: `plating/effects/DamagePlatingEffect.java`

- [ ] **Step 1: Change to implements IPlatingAttackTrigger**

Replace the class declaration and remove `@Override`:

```java
package studio.fantasyit.ether_craft.plating.effects;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import studio.fantasyit.ether_craft.plating.PlatingData;
import studio.fantasyit.ether_craft.plating.PlatingUtil;
import studio.fantasyit.ether_craft.plating.trigger.IPlatingAttackTrigger;

public class DamagePlatingEffect implements IPlatingAttackTrigger {
    @Override
    public double getEffectByEther(long ether) {
        return Math.sqrt(ether) / 10.0;
    }

    @Override
    public boolean onAttack(PlatingData data, ItemStack stack, Player player, Entity target) {
        if (!(target instanceof LivingEntity living)) return false;
        if (!PlatingUtil.canExtractEther(stack, 1)) return false;
        PlatingUtil.extractEther(stack, 1);
        living.hurt(living.damageSources().playerAttack(player), (float) data.effect());
        return true;
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add src/main/java/studio/fantasyit/ether_craft/plating/effects/DamagePlatingEffect.java
git commit -m "refactor: DamagePlatingEffect implements IPlatingAttackTrigger"
```

---

### Task 4: Update PlatingData with coolDownUntil

**Files:**
- Modify: `plating/PlatingData.java`

- [ ] **Step 1: Update PlatingData record**

Read current file, then replace with:

```java
package studio.fantasyit.ether_craft.plating;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public record PlatingData(Identifier id, double effect, @Nullable Long coolDownUntil) {
    public PlatingData(Identifier id, double effect) {
        this(id, effect, null);
    }

    public static final Codec<PlatingData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Identifier.CODEC.fieldOf("id").forGetter(PlatingData::id),
            Codec.DOUBLE.fieldOf("effect").forGetter(PlatingData::effect),
            Codec.LONG.optionalFieldOf("coolDownUntil").forGetter(d -> Optional.ofNullable(d.coolDownUntil))
    ).apply(instance, (id, effect, cd) -> new PlatingData(id, effect, cd.orElse(null))));

    public static final StreamCodec<RegistryFriendlyByteBuf, PlatingData> STREAM_CODEC = StreamCodec.composite(
            Identifier.STREAM_CODEC, PlatingData::id,
            ByteBufCodecs.DOUBLE, PlatingData::effect,
            ByteBufCodecs.optional(ByteBufCodecs.VAR_LONG), d -> Optional.ofNullable(d.coolDownUntil),
            (id, effect, cd) -> new PlatingData(id, effect, cd.orElse(null))
    );

    public PlatingData copyWithCoolDown(ServerLevel level, long cdTicks) {
        return new PlatingData(id, effect, level.getGameTime() + cdTicks);
    }

    public boolean isCd(ServerLevel level) {
        return coolDownUntil != null && level.getGameTime() < coolDownUntil;
    }
}
```

- [ ] **Step 2: Build and verify**

Run: `idea_build_project` with filesToRebuild: `["plating/PlatingData.java"]`
Expected: No errors.

- [ ] **Step 3: Commit**

```bash
git add src/main/java/studio/fantasyit/ether_craft/plating/PlatingData.java
git commit -m "feat: add coolDownUntil to PlatingData"
```

---

### Task 5: Add updatePlatingData to PlatingUtil

**Files:**
- Modify: `plating/PlatingUtil.java`

- [ ] **Step 1: Add updatePlatingData method**

Add this method before the closing `}`:

```java
    public static void updatePlatingData(ItemStack stack, PlatingData updated) {
        List<PlatingData> list = new ArrayList<>(getPlatingData(stack));
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).id().equals(updated.id())) {
                list.set(i, updated);
                break;
            }
        }
        stack.set(DataComponentRegistry.PLATING_DATA, List.copyOf(list));
    }
```

Note: Needs `import java.util.ArrayList;` and `import java.util.List;` at top (already present).

- [ ] **Step 2: Commit**

```bash
git add src/main/java/studio/fantasyit/ether_craft/plating/PlatingUtil.java
git commit -m "feat: add updatePlatingData to PlatingUtil"
```

---

### Task 6: Create PlatingPlayerAttachment

**Files:**
- Create: `plating/attachment/PlatingPlayerAttachment.java`

- [ ] **Step 1: Create attachment data class**

```java
package studio.fantasyit.ether_craft.plating.attachment;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class PlatingPlayerAttachment {
    public boolean soulActive;
    @Nullable
    public Vec3 soulCameraPos;
    public long lastOnGroundTick;

    public PlatingPlayerAttachment() {
        this.soulActive = false;
        this.soulCameraPos = null;
        this.lastOnGroundTick = -100;
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add src/main/java/studio/fantasyit/ether_craft/plating/attachment/PlatingPlayerAttachment.java
git commit -m "feat: add PlatingPlayerAttachment"
```

---

### Task 7: Register PLATING_PLAYER attachment

**Files:**
- Modify: `register/AttachmentDataRegistry.java`

- [ ] **Step 1: Add PLATING_PLAYER attachment**

After the existing `ESBS_CACHE` entry (before `register` method), add:

```java
    public static final Supplier<AttachmentType<PlatingPlayerAttachment>> PLATING_PLAYER = ATTACHMENT_TYPES.register(
            "plating_player", () -> AttachmentType.builder(PlatingPlayerAttachment::new)
                    .serialize(ExtraCodecs.JSON.comapFlatMap(json -> {
                        // no persistence needed
                        return net.minecraft.Util.deserialize(new PlatingPlayerAttachment());
                    }, p -> null).fieldOf("dummy"))
                    .build()
    );
```

Wait — for a pure runtime attachment with no serialization needed, we skip `.serialize()`. Actually NeoForge requires serialize. Let's use a simpler approach without serialization — just `.build()`:

```java
    public static final Supplier<AttachmentType<PlatingPlayerAttachment>> PLATING_PLAYER = ATTACHMENT_TYPES.register(
            "plating_player", () -> AttachmentType.builder(PlatingPlayerAttachment::new).build()
    );
```

Also add the import at top:
```java
import studio.fantasyit.ether_craft.plating.attachment.PlatingPlayerAttachment;
```

- [ ] **Step 2: Commit**

```bash
git add src/main/java/studio/fantasyit/ether_craft/register/AttachmentDataRegistry.java
git commit -m "feat: register PLATING_PLAYER attachment"
```

---

### Task 8: Create PlatingTriggerC2S packet

**Files:**
- Create: `network/c2s/PlatingTriggerC2S.java`

- [ ] **Step 1: Create packet record**

```java
package studio.fantasyit.ether_craft.network.c2s;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;
import studio.fantasyit.ether_craft.EtherCraft;

public record PlatingTriggerC2S(Identifier effectId) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<@NotNull PlatingTriggerC2S> TYPE = new CustomPacketPayload.Type<>(
            Identifier.fromNamespaceAndPath(EtherCraft.MODID, "plating_trigger")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, @NotNull PlatingTriggerC2S> CODEC = StreamCodec.composite(
            Identifier.STREAM_CODEC,
            PlatingTriggerC2S::effectId,
            PlatingTriggerC2S::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
```

- [ ] **Step 2: Create handler logic inline (no separate handler file)**

The handler will be a static method in `PlatingEventHandler`. Create a handler file:

```java
package studio.fantasyit.ether_craft.network.c2s;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import studio.fantasyit.ether_craft.plating.PlatingData;
import studio.fantasyit.ether_craft.plating.PlatingUtil;
import studio.fantasyit.ether_craft.plating.effects.IPlatingEffect;
import studio.fantasyit.ether_craft.plating.trigger.IPlatingRightClickTrigger;

import java.util.List;

public class PlatingTriggerC2SHandler {
    public static void handle(PlatingTriggerC2S packet, Player player) {
        for (var entry : PlatingEventHelper.getPlatedEquipment(player)) {
            ItemStack stack = entry.stack();
            List<PlatingData> data = PlatingUtil.getPlatingData(stack);
            for (PlatingData d : data) {
                if (d.id().equals(packet.effectId())) {
                    IPlatingEffect effect = PlatingEventHelper.getEffect(d.id());
                    if (effect instanceof IPlatingRightClickTrigger trigger) {
                        trigger.onRightClick(d, stack, player);
                    }
                }
            }
        }
    }
}
```

Wait, this introduces a dependency on a helper. Let me instead keep it self-contained — inline the equipment iteration and manager lookup directly in the handler. Actually, let me create a shared helper class `PlatingEventHelper` in `plating/event/` that the handler and event handler both use.

Let me restructure: create `PlatingEventHelper` with static methods `getPlatedEquipment(Player)`, `getEffect(Identifier)`, and the `PlatingTrigger` functional interface. This avoids duplication.

- [ ] **Step 3: Commit**

```bash
git add src/main/java/studio/fantasyit/ether_craft/network/c2s/PlatingTriggerC2S.java
git commit -m "feat: add PlatingTriggerC2S packet"
```

---

### Task 9: Create PlatingSoulStateS2C packet

**Files:**
- Create: `network/s2c/PlatingSoulStateS2C.java`

- [ ] **Step 1: Create packet record (self-contained, handles camera directly)**

```java
package studio.fantasyit.ether_craft.network.s2c;

import net.minecraft.client.Minecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;
import studio.fantasyit.ether_craft.EtherCraft;

public record PlatingSoulStateS2C(boolean active) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<@NotNull PlatingSoulStateS2C> TYPE = new CustomPacketPayload.Type<>(
            Identifier.fromNamespaceAndPath(EtherCraft.MODID, "plating_soul_state")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, @NotNull PlatingSoulStateS2C> CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL,
            PlatingSoulStateS2C::active,
            PlatingSoulStateS2C::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static boolean clientSoulActive = false;
    private static double clientSoulX, clientSoulY, clientSoulZ;

    public static void handle(PlatingSoulStateS2C packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            clientSoulActive = packet.active();
            if (packet.active()) {
                var player = context.player();
                clientSoulX = player.getX();
                clientSoulY = player.getY() + player.getEyeHeight();
                clientSoulZ = player.getZ();
                Minecraft.getInstance().setCameraEntity(null);
            } else {
                Minecraft.getInstance().setCameraEntity(Minecraft.getInstance().player);
            }
        });
    }

    public static boolean isClientSoulActive() { return clientSoulActive; }
    public static double getClientSoulX() { return clientSoulX; }
    public static double getClientSoulY() { return clientSoulY; }
    public static double getClientSoulZ() { return clientSoulZ; }
    public static void updateClientSoulPos(double x, double y, double z) {
        clientSoulX = x;
        clientSoulY = y;
        clientSoulZ = z;
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add src/main/java/studio/fantasyit/ether_craft/network/s2c/PlatingSoulStateS2C.java
git commit -m "feat: add PlatingSoulStateS2C packet"
```

---

### Task 10: Update Network.java — register new packets

**Files:**
- Modify: `network/Network.java`

- [ ] **Step 1: Add C2S registration in commonMsg()**

Inside `commonMsg()`, after the existing playToServer blocks (before `}`), add:

```java
        event.playToServer(
                PlatingTriggerC2S.TYPE,
                PlatingTriggerC2S.CODEC,
                wrapWithPlayer(PlatingTriggerC2SHandler::handle)
        );
```

And in the playToClient section, add:

```java
        event.playToClient(
                PlatingSoulStateS2C.TYPE,
                PlatingSoulStateS2C.CODEC,
                PlatingSoulStateS2C::handle
        );
```

Update imports to add:
```java
import studio.fantasyit.ether_craft.network.c2s.PlatingTriggerC2S;
import studio.fantasyit.ether_craft.network.c2s.PlatingTriggerC2SHandler;
import studio.fantasyit.ether_craft.network.s2c.PlatingSoulStateS2C;
```

- [ ] **Step 2: Commit**

```bash
git add src/main/java/studio/fantasyit/ether_craft/network/Network.java
git commit -m "feat: register PlatingTriggerC2S and PlatingSoulStateS2C"
```

---

### Task 11: Create PlatingEventHelper (shared event utilities)

**Files:**
- Create: `plating/event/PlatingEventHelper.java`

- [ ] **Step 1: Create helper class**

Extract shared logic from the old `PlatingEventHandler`:

```java
package studio.fantasyit.ether_craft.plating.event;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import studio.fantasyit.ether_craft.plating.PlatingData;
import studio.fantasyit.ether_craft.plating.PlatingManager;
import studio.fantasyit.ether_craft.plating.effects.IPlatingEffect;

import java.util.List;

public class PlatingEventHelper {

    public static ItemStack[] getPlatedEquipment(Player player) {
        return new ItemStack[]{
                player.getMainHandItem(),
                player.getOffhandItem(),
                player.getItemBySlot(EquipmentSlot.HEAD),
                player.getItemBySlot(EquipmentSlot.CHEST),
                player.getItemBySlot(EquipmentSlot.LEGS),
                player.getItemBySlot(EquipmentSlot.FEET)
        };
    }

    @Nullable
    public static IPlatingEffect getEffect(Identifier id) {
        return PlatingManager.getEffect(id);
    }

    @FunctionalInterface
    public interface PlatingTrigger {
        void apply(IPlatingEffect effect, PlatingData data, ItemStack stack, Player player);
    }

    public static void forEachPlating(ItemStack stack, Player player, PlatingTrigger trigger) {
        List<PlatingData> data = studio.fantasyit.ether_craft.plating.PlatingUtil.getPlatingData(stack);
        if (data.isEmpty()) return;
        for (PlatingData d : data) {
            IPlatingEffect effect = PlatingManager.getEffect(d.id());
            if (effect != null) {
                trigger.apply(effect, d, stack, player);
            }
        }
    }

    public static void forEachPlatingOnEquipment(Player player, PlatingTrigger trigger) {
        for (ItemStack stack : getPlatedEquipment(player)) {
            forEachPlating(stack, player, trigger);
        }
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add src/main/java/studio/fantasyit/ether_craft/plating/event/PlatingEventHelper.java
git commit -m "refactor: extract PlatingEventHelper from PlatingEventHandler"
```

---

### Task 12: Move + rewrite PlatingEventHandler

**Files:**
- Create: `plating/event/PlatingEventHandler.java` (new location)
- Modify: `event/PlatingEventHandler.java` → delete after move
- Create: `plating/event/PlatingItemEntityTicker.java` (new)
- Create: `network/c2s/PlatingTriggerC2SHandler.java` (new — separate file from Task 8)

**Note:** Since we created `PlatingTriggerC2S` in Task 8, we now also need its handler. Let me fix: the handler is a separate file.

- [ ] **Step 1: Create PlatingTriggerC2SHandler**

```java
package studio.fantasyit.ether_craft.network.c2s;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import studio.fantasyit.ether_craft.plating.PlatingData;
import studio.fantasyit.ether_craft.plating.PlatingUtil;
import studio.fantasyit.ether_craft.plating.event.PlatingEventHelper;
import studio.fantasyit.ether_craft.plating.effects.IPlatingEffect;
import studio.fantasyit.ether_craft.plating.trigger.IPlatingRightClickTrigger;

import java.util.List;

public class PlatingTriggerC2SHandler {
    public static void handle(PlatingTriggerC2S packet, Player player) {
        for (ItemStack stack : PlatingEventHelper.getPlatedEquipment(player)) {
            List<PlatingData> data = PlatingUtil.getPlatingData(stack);
            for (PlatingData d : data) {
                if (d.id().equals(packet.effectId())) {
                    IPlatingEffect effect = PlatingEventHelper.getEffect(d.id());
                    if (effect instanceof IPlatingRightClickTrigger trigger) {
                        trigger.onRightClick(d, stack, player);
                        return;
                    }
                }
            }
        }
    }
}
```

- [ ] **Step 2: Create PlatingItemEntityTicker.java**

Extract `tickPlating` from `ItemEntityTickEvent`:

```java
package studio.fantasyit.ether_craft.plating.event;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import studio.fantasyit.ether_craft.Config;
import studio.fantasyit.ether_craft.plating.PlatingData;
import studio.fantasyit.ether_craft.plating.PlatingManager;
import studio.fantasyit.ether_craft.plating.PlatingUtil;
import studio.fantasyit.ether_craft.plating.effects.IPlatingEffect;
import studio.fantasyit.ether_craft.register.DataComponentRegistry;

import java.util.ArrayList;
import java.util.List;

public class PlatingItemEntityTicker {
    public static void tick(ItemStack stack, ServerLevel level) {
        long startTime = stack.getOrDefault(DataComponentRegistry.PLATING_START_TIME, 0L);
        long elapsed = level.getGameTime() - startTime;
        if (elapsed < Config.platingDurationTicks) return;

        List<PlatingData> existing = new ArrayList<>(PlatingUtil.getPlatingData(stack));
        int ether = PlatingUtil.getEther(stack);
        List<Identifier> inProgress = PlatingUtil.getInProgress(stack);

        for (var effectId : inProgress) {
            IPlatingEffect effect = PlatingManager.getEffect(effectId);
            if (effect != null) {
                double value = effect.getEffectByEther(ether);
                existing.add(new PlatingData(effectId, value));
            }
        }

        stack.remove(DataComponentRegistry.PLATING_IN_PROGRESS);
        stack.remove(DataComponentRegistry.PLATING_START_TIME);
        stack.set(DataComponentRegistry.PLATING_DATA, existing);
    }
}
```

- [ ] **Step 3: Create new PlatingEventHandler.java in plating/event/**

```java
package studio.fantasyit.ether_craft.plating.event;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.entity.player.UseItemOnBlockEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.plating.attachment.PlatingPlayerAttachment;
import studio.fantasyit.ether_craft.plating.trigger.*;
import studio.fantasyit.ether_craft.register.AttachmentDataRegistry;

@EventBusSubscriber(modid = EtherCraft.MODID)
public class PlatingEventHandler {

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) return;

        var attachment = player.getData(AttachmentDataRegistry.PLATING_PLAYER);

        PlatingEventHelper.forEachPlatingOnEquipment(player, (effect, data, stack, p) -> {
            if (effect instanceof IPlatingHoldTickTrigger holdTick) {
                holdTick.onHoldTick(data, stack, player);
            }
        });
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) return;

        for (ItemStack stack : PlatingEventHelper.getPlatedEquipment(player)) {
            PlatingEventHelper.forEachPlating(stack, player, (effect, data, s, p) -> {
                if (effect instanceof IPlatingRightClickTrigger) {
                    event.setCanceled(true);
                }
            });
        }
    }

    @SubscribeEvent
    public static void onUseItem(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (player.level().isClientSide()) return;
        ItemStack stack = event.getItem();
        PlatingEventHelper.forEachPlating(stack, player, (effect, data, s, p) -> {
            if (effect instanceof IPlatingUseTrigger use) {
                use.onUse(data, s, p);
            }
        });
    }

    @SubscribeEvent
    public static void onAttack(AttackEntityEvent event) {
        if (event.getEntity().level().isClientSide()) return;
        Player player = event.getEntity();
        ItemStack stack = player.getMainHandItem();
        PlatingEventHelper.forEachPlating(stack, player, (effect, data, s, p) -> {
            if (effect instanceof IPlatingAttackTrigger attack) {
                if (attack.onAttack(data, s, p, event.getTarget())) {
                    event.setCanceled(true);
                }
            }
        });
    }

    @SubscribeEvent
    public static void onBreakBlock(BreakBlockEvent event) {
        if (event.getLevel().isClientSide()) return;
        Player player = event.getPlayer();
        if (player == null) return;
        ItemStack stack = player.getMainHandItem();
        PlatingEventHelper.forEachPlating(stack, player, (effect, data, s, p) -> {
            if (effect instanceof IPlatingBreakBlockTrigger breakBlock) {
                if (breakBlock.onBreakBlock(data, s, p, event.getPos(), event.getState())) {
                    event.setCanceled(true);
                }
            }
        });
    }

    @SubscribeEvent
    public static void onUseOnBlock(UseItemOnBlockEvent event) {
        if (event.getLevel().isClientSide()) return;
        Player player = event.getPlayer();
        if (player == null) return;
        ItemStack stack = event.getItemStack();
        PlatingEventHelper.forEachPlating(stack, player, (effect, data, s, p) -> {
            if (effect instanceof IPlatingUseOnBlockTrigger useOnBlock) {
                useOnBlock.onUseOnBlock(data, s, p, event.getPos(), event.getLevel().getBlockState(event.getPos()));
            }
        });
    }

    @SubscribeEvent
    public static void onArrowShot(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) return;
        if (!(event.getEntity() instanceof AbstractArrow arrow)) return;
        if (!(arrow.getOwner() instanceof Player player)) return;
        if (!(player.level() instanceof ServerLevel)) return;

        ItemStack held = player.getMainHandItem();
        if (!(held.getItem() instanceof BowItem || held.getItem() instanceof CrossbowItem)) {
            held = player.getOffhandItem();
            if (!(held.getItem() instanceof BowItem || held.getItem() instanceof CrossbowItem)) return;
        }

        PlatingEventHelper.forEachPlating(held, player, (effect, data, stack, p) -> {
            if (effect instanceof IPlatingArrowShotTrigger arrowShot) {
                arrowShot.onArrowShot(data, stack, player, arrow);
            }
        });
    }

    public static boolean tryJump(Player player) {
        if (player.level().isClientSide()) return false;

        boolean[] result = {false};
        PlatingEventHelper.forEachPlatingOnEquipment(player, (effect, data, stack, p) -> {
            if (effect instanceof IPlatingJumpTrigger jump) {
                if (jump.canJump(data, stack, player)) {
                    result[0] = true;
                }
            }
        });
        return result[0];
    }
}
```

- [ ] **Step 4: Delete old event/PlatingEventHandler.java**

```bash
git rm src/main/java/studio/fantasyit/ether_craft/event/PlatingEventHandler.java
```

- [ ] **Step 5: Build and verify**

Run: `idea_build_project`
Expected: No errors. Fix any import issues.

- [ ] **Step 6: Commit**

```bash
git add src/main/java/studio/fantasyit/ether_craft/plating/event/
git add src/main/java/studio/fantasyit/ether_craft/network/c2s/PlatingTriggerC2SHandler.java
git commit -m "refactor: move PlatingEventHandler to plating/event/, extract ticker and helper"
```

---

### Task 13: Move PlatingTooltipHandler to plating/client/

**Files:**
- Create: `plating/client/PlatingTooltipHandler.java` (copy from event/)
- Delete: `event/PlatingTooltipHandler.java`

- [ ] **Step 1: Move file to new package**

Read the current `event/PlatingTooltipHandler.java` content. Change the package declaration:

Old: `package studio.fantasyit.ether_craft.event;`
New: `package studio.fantasyit.ether_craft.client;`

All other code stays the same. The `@EventBusSubscriber(modid = EtherCraft.MODID, value = Dist.CLIENT)` annotation means it auto-registers regardless of package.

- [ ] **Step 2: Delete old file, add new**

```bash
git rm src/main/java/studio/fantasyit/ether_craft/event/PlatingTooltipHandler.java
# the new file was created via Write tool
```

- [ ] **Step 3: Commit**

```bash
git add src/main/java/studio/fantasyit/ether_craft/plating/client/PlatingTooltipHandler.java
git commit -m "refactor: move PlatingTooltipHandler to plating/client/"
```

---

### Task 14: Update ItemEntityTickEvent to delegate

**Files:**
- Modify: `event/ItemEntityTickEvent.java`

- [ ] **Step 1: Replace tickPlating logic with delegate**

Replace line 39-42:
```java
            if (PlatingUtil.isPlatingInProgress(stack)) {
                tickPlating(stack, (ServerLevel) event.getEntity().level());
                ie.setItem(stack);
            }
```

With:
```java
            if (PlatingUtil.isPlatingInProgress(stack)) {
                PlatingItemEntityTicker.tick(stack, (ServerLevel) event.getEntity().level());
                ie.setItem(stack);
            }
```

Remove the `tickPlating()` method (lines 46-66) entirely.

Remove unused imports: `PlatingData`, `PlatingManager`, `IPlatingEffect`, `DataComponentRegistry` (if no longer used). Add import:
```java
import studio.fantasyit.ether_craft.plating.event.PlatingItemEntityTicker;
```

- [ ] **Step 2: Commit**

```bash
git add src/main/java/studio/fantasyit/ether_craft/event/ItemEntityTickEvent.java
git commit -m "refactor: delegate tickPlating to PlatingItemEntityTicker"
```

---

### Task 15: Add config entries

**Files:**
- Modify: `Config.java`

- [ ] **Step 1: Add 8 new config entries**

After the `PLATING_DURATION_TICKS` entry (after line 197, before `static final ModConfigSpec SPEC`), add:

```java
    // -- plating.dash --
    private static final ModConfigSpec.IntValue PLATING_DASH_CD_TICKS = BUILDER
            .comment("Cooldown ticks for Dash plating effect")
            .defineInRange("plating.dash.cd_ticks", 20, 0, 12000);
    private static final ModConfigSpec.IntValue PLATING_DASH_ETHER_COST = BUILDER
            .comment("Ether cost for Dash plating effect")
            .defineInRange("plating.dash.ether_cost", 5, 0, Integer.MAX_VALUE);

    // -- plating.high_jump --
    private static final ModConfigSpec.IntValue PLATING_HIGH_JUMP_CD_TICKS = BUILDER
            .comment("Cooldown ticks for High Jump plating effect")
            .defineInRange("plating.high_jump.cd_ticks", 20, 0, 12000);
    private static final ModConfigSpec.IntValue PLATING_HIGH_JUMP_ETHER_COST = BUILDER
            .comment("Ether cost for High Jump plating effect")
            .defineInRange("plating.high_jump.ether_cost", 5, 0, Integer.MAX_VALUE);

    // -- plating.soul --
    private static final ModConfigSpec.IntValue PLATING_SOUL_ETHER_PER_TICK = BUILDER
            .comment("Ether consumed per tick by Soul Projection plating effect")
            .defineInRange("plating.soul.ether_per_tick", 1, 0, Integer.MAX_VALUE);
    private static final ModConfigSpec.IntValue PLATING_SOUL_MAX_RANGE = BUILDER
            .comment("Maximum range in blocks for Soul Projection camera")
            .defineInRange("plating.soul.max_range", 64, 1, 512);

    // -- plating.no_gravity --
    private static final ModConfigSpec.IntValue PLATING_NO_GRAVITY_ETHER_PER_ARROW = BUILDER
            .comment("Ether cost per arrow for No Gravity plating effect")
            .defineInRange("plating.no_gravity.ether_per_arrow", 1, 0, Integer.MAX_VALUE);

    // -- plating.coyote_time --
    private static final ModConfigSpec.IntValue PLATING_COYOTE_TIME_ETHER_PER_JUMP = BUILDER
            .comment("Ether cost per delayed jump for Coyote Time plating effect")
            .defineInRange("plating.coyote_time.ether_per_jump", 1, 0, Integer.MAX_VALUE);
```

- [ ] **Step 2: Add public static fields**

After `public static int platingDurationTicks;`, add:

```java
    public static int platingDashCdTicks;
    public static int platingDashEtherCost;
    public static int platingHighJumpCdTicks;
    public static int platingHighJumpEtherCost;
    public static int platingSoulEtherPerTick;
    public static int platingSoulMaxRange;
    public static int platingNoGravityEtherPerArrow;
    public static int platingCoyoteTimeEtherPerJump;
```

- [ ] **Step 3: Add onLoad assignments**

In the `onLoad` method, after `platingDurationTicks = PLATING_DURATION_TICKS.get();`, add:

```java
        platingDashCdTicks = PLATING_DASH_CD_TICKS.get();
        platingDashEtherCost = PLATING_DASH_ETHER_COST.get();
        platingHighJumpCdTicks = PLATING_HIGH_JUMP_CD_TICKS.get();
        platingHighJumpEtherCost = PLATING_HIGH_JUMP_ETHER_COST.get();
        platingSoulEtherPerTick = PLATING_SOUL_ETHER_PER_TICK.get();
        platingSoulMaxRange = PLATING_SOUL_MAX_RANGE.get();
        platingNoGravityEtherPerArrow = PLATING_NO_GRAVITY_ETHER_PER_ARROW.get();
        platingCoyoteTimeEtherPerJump = PLATING_COYOTE_TIME_ETHER_PER_JUMP.get();
```

- [ ] **Step 4: Commit**

```bash
git add src/main/java/studio/fantasyit/ether_craft/Config.java
git commit -m "feat: add plating effect config entries"
```

---

### Task 16: Create DashPlatingEffect

**Files:**
- Create: `plating/effects/DashPlatingEffect.java`

- [ ] **Step 1: Implement DashPlatingEffect**

```java
package studio.fantasyit.ether_craft.plating.effects;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import studio.fantasyit.ether_craft.Config;
import studio.fantasyit.ether_craft.plating.PlatingData;
import studio.fantasyit.ether_craft.plating.PlatingUtil;
import studio.fantasyit.ether_craft.plating.trigger.IPlatingRightClickTrigger;

public class DashPlatingEffect implements IPlatingRightClickTrigger {

    @Override
    public double getEffectByEther(long ether) {
        return Math.sqrt(ether) / 10.0;
    }

    @Override
    public boolean onRightClick(PlatingData data, ItemStack stack, Player player) {
        if (!(player.level() instanceof ServerLevel level)) return false;

        if (data.isCd(level)) return false;

        if (!PlatingUtil.canExtractEther(stack, Config.platingDashEtherCost)) return false;
        PlatingUtil.extractEther(stack, Config.platingDashEtherCost);

        Vec3 look = player.getLookAngle();
        double distance = data.effect() * 0.5;
        player.setDeltaMovement(look.x * distance, 0.1, look.z * distance);
        player.hurtMarked = true;

        PlatingData updated = data.copyWithCoolDown(level, Config.platingDashCdTicks);
        PlatingUtil.updatePlatingData(stack, updated);

        return true;
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add src/main/java/studio/fantasyit/ether_craft/plating/effects/DashPlatingEffect.java
git commit -m "feat: add DashPlatingEffect"
```

---

### Task 17: Create HighJumpPlatingEffect

**Files:**
- Create: `plating/effects/HighJumpPlatingEffect.java`

- [ ] **Step 1: Implement HighJumpPlatingEffect**

```java
package studio.fantasyit.ether_craft.plating.effects;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import studio.fantasyit.ether_craft.Config;
import studio.fantasyit.ether_craft.plating.PlatingData;
import studio.fantasyit.ether_craft.plating.PlatingUtil;
import studio.fantasyit.ether_craft.plating.trigger.IPlatingRightClickTrigger;

public class HighJumpPlatingEffect implements IPlatingRightClickTrigger {

    @Override
    public double getEffectByEther(long ether) {
        return Math.sqrt(ether) / 10.0;
    }

    @Override
    public boolean onRightClick(PlatingData data, ItemStack stack, Player player) {
        if (!(player.level() instanceof ServerLevel level)) return false;

        if (data.isCd(level)) return false;

        if (!PlatingUtil.canExtractEther(stack, Config.platingHighJumpEtherCost)) return false;
        PlatingUtil.extractEther(stack, Config.platingHighJumpEtherCost);

        double height = data.effect() * 1.0;
        player.setDeltaMovement(player.getDeltaMovement().x, height, player.getDeltaMovement().z);
        player.hurtMarked = true;

        player.addEffect(new MobEffectInstance(MobEffects.JUMP, 60, 0, false, false));

        PlatingData updated = data.copyWithCoolDown(level, Config.platingHighJumpCdTicks);
        PlatingUtil.updatePlatingData(stack, updated);

        return true;
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add src/main/java/studio/fantasyit/ether_craft/plating/effects/HighJumpPlatingEffect.java
git commit -m "feat: add HighJumpPlatingEffect"
```

---

### Task 18: Create SoulProjectionPlatingEffect

**Files:**
- Create: `plating/effects/SoulProjectionPlatingEffect.java`

- [ ] **Step 1: Implement SoulProjectionPlatingEffect**

```java
package studio.fantasyit.ether_craft.plating.effects;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import studio.fantasyit.ether_craft.Config;
import studio.fantasyit.ether_craft.network.s2c.PlatingSoulStateS2C;
import studio.fantasyit.ether_craft.plating.PlatingData;
import studio.fantasyit.ether_craft.plating.PlatingUtil;
import studio.fantasyit.ether_craft.plating.attachment.PlatingPlayerAttachment;
import studio.fantasyit.ether_craft.plating.trigger.IPlatingHoldTickTrigger;
import studio.fantasyit.ether_craft.plating.trigger.IPlatingRightClickTrigger;
import studio.fantasyit.ether_craft.register.AttachmentDataRegistry;

public class SoulProjectionPlatingEffect implements IPlatingRightClickTrigger, IPlatingHoldTickTrigger {

    @Override
    public double getEffectByEther(long ether) {
        return Math.sqrt(ether) / 10.0;
    }

    @Override
    public boolean onRightClick(PlatingData data, ItemStack stack, Player player) {
        if (!(player.level() instanceof ServerLevel level)) return false;

        var attachment = player.getData(AttachmentDataRegistry.PLATING_PLAYER);

        if (attachment.soulActive) {
            attachment.soulActive = false;
            attachment.soulCameraPos = null;
            PacketDistributor.sendToPlayer((ServerPlayer) player, new PlatingSoulStateS2C(false));
        } else {
            if (!PlatingUtil.canExtractEther(stack, Config.platingSoulEtherPerTick)) return false;
            attachment.soulActive = true;
            attachment.soulCameraPos = player.position().add(0, player.getEyeHeight(), 0);
            PacketDistributor.sendToPlayer((ServerPlayer) player, new PlatingSoulStateS2C(true));
        }

        return true;
    }

    @Override
    public void onHoldTick(PlatingData data, ItemStack stack, Player player) {
        if (!(player.level() instanceof ServerLevel level)) return;

        var attachment = player.getData(AttachmentDataRegistry.PLATING_PLAYER);
        if (!attachment.soulActive) return;

        if (!PlatingUtil.canExtractEther(stack, Config.platingSoulEtherPerTick)) {
            attachment.soulActive = false;
            attachment.soulCameraPos = null;
            PacketDistributor.sendToPlayer((ServerPlayer) player, new PlatingSoulStateS2C(false));
            return;
        }
        PlatingUtil.extractEther(stack, Config.platingSoulEtherPerTick);

        if (attachment.soulCameraPos != null) {
            double dist = attachment.soulCameraPos.distanceTo(player.position());
            if (dist > Config.platingSoulMaxRange) {
                attachment.soulActive = false;
                attachment.soulCameraPos = null;
                PacketDistributor.sendToPlayer((ServerPlayer) player, new PlatingSoulStateS2C(false));
            }
        }
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add src/main/java/studio/fantasyit/ether_craft/plating/effects/SoulProjectionPlatingEffect.java
git commit -m "feat: add SoulProjectionPlatingEffect"
```

---

### Task 19: Create NoGravityPlatingEffect

**Files:**
- Create: `plating/effects/NoGravityPlatingEffect.java`

- [ ] **Step 1: Implement NoGravityPlatingEffect**

```java
package studio.fantasyit.ether_craft.plating.effects;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import studio.fantasyit.ether_craft.Config;
import studio.fantasyit.ether_craft.plating.PlatingData;
import studio.fantasyit.ether_craft.plating.PlatingUtil;
import studio.fantasyit.ether_craft.plating.trigger.IPlatingArrowShotTrigger;

public class NoGravityPlatingEffect implements IPlatingArrowShotTrigger {

    @Override
    public double getEffectByEther(long ether) {
        return Math.sqrt(ether) / 10.0;
    }

    @Override
    public void onArrowShot(PlatingData data, ItemStack stack, Player player, AbstractArrow arrow) {
        if (!PlatingUtil.canExtractEther(stack, Config.platingNoGravityEtherPerArrow)) return;
        PlatingUtil.extractEther(stack, Config.platingNoGravityEtherPerArrow);
        arrow.setNoGravity(true);
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add src/main/java/studio/fantasyit/ether_craft/plating/effects/NoGravityPlatingEffect.java
git commit -m "feat: add NoGravityPlatingEffect"
```

---

### Task 20: Create CoyoteTimePlatingEffect

**Files:**
- Create: `plating/effects/CoyoteTimePlatingEffect.java`

- [ ] **Step 1: Implement CoyoteTimePlatingEffect**

```java
package studio.fantasyit.ether_craft.plating.effects;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import studio.fantasyit.ether_craft.Config;
import studio.fantasyit.ether_craft.plating.PlatingData;
import studio.fantasyit.ether_craft.plating.PlatingUtil;
import studio.fantasyit.ether_craft.plating.attachment.PlatingPlayerAttachment;
import studio.fantasyit.ether_craft.plating.trigger.IPlatingHoldTickTrigger;
import studio.fantasyit.ether_craft.plating.trigger.IPlatingJumpTrigger;
import studio.fantasyit.ether_craft.register.AttachmentDataRegistry;

public class CoyoteTimePlatingEffect implements IPlatingJumpTrigger, IPlatingHoldTickTrigger {

    private static final long COYOTE_WINDOW = 40L;

    @Override
    public double getEffectByEther(long ether) {
        return Math.sqrt(ether) / 10.0;
    }

    @Override
    public void onHoldTick(PlatingData data, ItemStack stack, Player player) {
        if (!(player.level() instanceof ServerLevel level)) return;
        if (player.onGround()) {
            var attachment = player.getData(AttachmentDataRegistry.PLATING_PLAYER);
            attachment.lastOnGroundTick = level.getGameTime();
        }
    }

    @Override
    public boolean canJump(PlatingData data, ItemStack stack, Player player) {
        if (!(player.level() instanceof ServerLevel level)) return false;

        var attachment = player.getData(AttachmentDataRegistry.PLATING_PLAYER);
        long now = level.getGameTime();
        if (now - attachment.lastOnGroundTick > COYOTE_WINDOW) return false;

        if (!PlatingUtil.canExtractEther(stack, Config.platingCoyoteTimeEtherPerJump)) return false;
        PlatingUtil.extractEther(stack, Config.platingCoyoteTimeEtherPerJump);
        return true;
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add src/main/java/studio/fantasyit/ether_craft/plating/effects/CoyoteTimePlatingEffect.java
git commit -m "feat: add CoyoteTimePlatingEffect"
```

---

### Task 21: Update PlatingManager — register new effects

**Files:**
- Modify: `plating/PlatingManager.java`

- [ ] **Step 1: Register 5 new effects**

Replace `init()` content:

```java
    public static void init() {
        register(EtherCraft.id("damage"), new DamagePlatingEffect());
        register(EtherCraft.id("dash"), new DashPlatingEffect());
        register(EtherCraft.id("high_jump"), new HighJumpPlatingEffect());
        register(EtherCraft.id("soul_projection"), new SoulProjectionPlatingEffect());
        register(EtherCraft.id("no_gravity"), new NoGravityPlatingEffect());
        register(EtherCraft.id("coyote_time"), new CoyoteTimePlatingEffect());
    }
```

Update imports to include the new effect classes.

- [ ] **Step 2: Commit**

```bash
git add src/main/java/studio/fantasyit/ether_craft/plating/PlatingManager.java
git commit -m "feat: register 5 new plating effects"
```

---

### Task 22: Create PlatingClientEventHandler

**Files:**
- Create: `plating/client/PlatingClientEventHandler.java`

- [ ] **Step 1: Implement client event handler**

```java
package studio.fantasyit.ether_craft.plating.client;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.network.c2s.PlatingTriggerC2S;

@EventBusSubscriber(modid = EtherCraft.MODID, value = Dist.CLIENT)
public class PlatingClientEventHandler {

    private static boolean soulActive = false;
    private static double soulX, soulY, soulZ;

    public static void onSoulStateChanged(boolean active) {
        soulActive = active;
        if (active) {
            var player = Minecraft.getInstance().player;
            if (player != null) {
                soulX = player.getX();
                soulY = player.getY() + player.getEyeHeight();
                soulZ = player.getZ();
                Minecraft.getInstance().setCameraEntity(null);
            }
        } else {
            Minecraft.getInstance().setCameraEntity(Minecraft.getInstance().player);
        }
    }

    @SubscribeEvent
    public static void onCameraSetup(ViewportEvent.ComputeCameraAngles event) {
        if (!soulActive) return;
        event.getCamera().setPosition(soulX, soulY, soulZ);
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (!soulActive) return;
        var mc = Minecraft.getInstance();
        if (mc.player == null) return;

        float speed = 0.3f;
        var input = mc.player.input;
        var look = mc.player.getLookAngle();

        double forward = (input.forwardImpulse > 0 ? 1 : input.forwardImpulse < 0 ? -1 : 0);
        double strafe = (input.leftImpulse > 0 ? 1 : input.leftImpulse < 0 ? -1 : 0);
        double up = (mc.options.keyJump.isDown() ? 1 : 0) + (mc.options.keyShift.isDown() ? -1 : 0);

        soulX += look.x * forward * speed + look.z * strafe * speed * 0.5; // simplified
        soulY += up * speed;
        soulZ += look.z * forward * speed - look.x * strafe * speed * 0.5; // simplified

        // Toggle off on right-click
        while (mc.options.keyUse.wasPressed()) {
            PacketDistributor.sendToServer(new PlatingTriggerC2S(
                    net.minecraft.resources.Identifier.fromNamespaceAndPath(EtherCraft.MODID, "soul_projection")
            ));
        }
    }

    public static boolean isSoulActive() {
        return soulActive;
    }
}
```

Wait — the movement math is wrong. Let me fix it. For WASD-style flight where W=forward, S=back, A=left, D=right relative to player's look direction:

```java
    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (!soulActive) return;
        var mc = Minecraft.getInstance();
        if (mc.player == null) return;

        float speed = 0.5f;
        var input = mc.player.input;
        var look = mc.player.getLookAngle();
        Vec3 forward = look.multiply(1, 0, 1).normalize();
        Vec3 right = forward.yRot((float) Math.PI / 2);

        double fwd = input.forwardImpulse;
        double str = input.leftImpulse;

        soulX += (forward.x * fwd + right.x * str) * speed;
        soulZ += (forward.z * fwd + right.z * str) * speed;

        if (mc.options.keyJump.isDown()) soulY += speed;
        if (mc.options.keyShift.isDown()) soulY -= speed;

        while (mc.options.keyUse.wasPressed()) {
            PacketDistributor.sendToServer(new PlatingTriggerC2S(
                    net.minecraft.resources.Identifier.fromNamespaceAndPath(EtherCraft.MODID, "soul_projection")
            ));
        }
    }
```

Let me rewrite the full file more carefully:

```java
package studio.fantasyit.ether_craft.plating.client;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.network.c2s.PlatingTriggerC2S;

@EventBusSubscriber(modid = EtherCraft.MODID, value = Dist.CLIENT)
public class PlatingClientEventHandler {

    @SubscribeEvent
    public static void onCameraSetup(ViewportEvent.ComputeCameraAngles event) {
        if (!PlatingSoulStateS2C.isClientSoulActive()) return;
        event.getCamera().setPosition(
                PlatingSoulStateS2C.getClientSoulX(),
                PlatingSoulStateS2C.getClientSoulY(),
                PlatingSoulStateS2C.getClientSoulZ()
        );
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (!PlatingSoulStateS2C.isClientSoulActive()) return;
        var mc = Minecraft.getInstance();
        if (mc.player == null) return;

        float speed = 0.5f;
        var look = mc.player.getLookAngle();
        Vec3 forward = new Vec3(look.x, 0, look.z).normalize();
        if (forward.lengthSqr() < 0.001) forward = new Vec3(0, 0, 1);
        Vec3 right = new Vec3(-forward.z, 0, forward.x);

        float fwd = mc.player.input.forwardImpulse;
        float str = mc.player.input.leftImpulse;

        double x = PlatingSoulStateS2C.getClientSoulX();
        double y = PlatingSoulStateS2C.getClientSoulY();
        double z = PlatingSoulStateS2C.getClientSoulZ();

        x += (forward.x * fwd + right.x * str) * speed;
        z += (forward.z * fwd + right.z * str) * speed;

        if (mc.options.keyJump.isDown()) y += speed;
        if (mc.options.keyShift.isDown()) y -= speed;

        PlatingSoulStateS2C.updateClientSoulPos(x, y, z);

        while (mc.options.keyUse.wasPressed()) {
            PacketDistributor.sendToServer(new PlatingTriggerC2S(
                    Identifier.fromNamespaceAndPath(EtherCraft.MODID, "soul_projection")
            ));
        }
    }
}
```

Note: remove old static fields (`soulActive`, `soulX`, `soulY`, `soulZ`) and `onSoulStateChanged()` / `isSoulActive()` methods — state is now in `PlatingSoulStateS2C`. Imports needed:
```java
import studio.fantasyit.ether_craft.network.s2c.PlatingSoulStateS2C;
```

- [ ] **Step 2: Commit**

```bash
git add src/main/java/studio/fantasyit/ether_craft/plating/client/PlatingClientEventHandler.java
git commit -m "feat: add PlatingClientEventHandler (soul projection camera)"
```

---

### Task 23: Create LivingEntityJumpMixin

**Files:**
- Create: `mixin/plating/LivingEntityJumpMixin.java`

- [ ] **Step 1: Create mixin**

```java
package studio.fantasyit.ether_craft.mixin.plating;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import studio.fantasyit.ether_craft.plating.event.PlatingEventHandler;

@Mixin(LivingEntity.class)
public class LivingEntityJumpMixin {

    @Inject(method = "jumpFromGround", at = @At("HEAD"), cancellable = true)
    private void ether_craft$onJumpFromGround(CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self instanceof Player player) {
            if (player.level().isClientSide()) return;

            if (!player.onGround() && PlatingEventHandler.tryJump(player)) {
                // Let vanilla jumpFromGround proceed to apply jump velocity
                return;
            }

            if (!player.onGround()) {
                ci.cancel();
            }
        }
    }
}
```

Wait — this is wrong. `jumpFromGround()` is called by the server when a jump packet arrives. It checks `onGround()` internally. We want to allow jumping even when `!onGround()` if coyote time is active.

Actually, looking at Minecraft's `LivingEntity.jumpFromGround()`:
- It calls `onGround()` check
- It applies the jump velocity

The mixin should intercept at HEAD, check if the player has coyote time, and if so, let the method run (apply jump velocity) even though they're not on ground.

The issue is that `jumpFromGround` itself checks `onGround`. Let me use a different approach:

```java
@Inject(method = "jumpFromGround", at = @At("HEAD"), cancellable = true)
private void ether_craft$onJumpFromGround(CallbackInfo ci) {
    LivingEntity self = (LivingEntity) (Object) this;
    if (!(self instanceof Player player)) return;
    if (player.level().isClientSide()) return;

    if (player.onGround()) return; // normal case

    if (PlatingEventHandler.tryJump(player)) {
        // Allow the jump — apply velocity manually
        player.setDeltaMovement(player.getDeltaMovement().add(0, player.getJumpPower(), 0));
        player.hasImpulse = true;
        ci.cancel();
    }
}
```

Actually, `jumpFromGround()` is called from the server jump handler which calls it regardless of onGround. But `jumpFromGround` itself: Let me think about this differently.

The mixin should be at the HEAD of `jumpFromGround`. If the player is NOT onGround but has coyote time plating, we apply the jump velocity and cancel (preventing vanilla's internal onGround check from blocking).

Actually, the simplest approach: cancel vanilla's `jumpFromGround()` when not onGround but coyote time allows jump, and manually apply the jump velocity. Let me revise:

```java
package studio.fantasyit.ether_craft.mixin.plating;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import studio.fantasyit.ether_craft.plating.event.PlatingEventHandler;

@Mixin(LivingEntity.class)
public class LivingEntityJumpMixin {

    @Inject(method = "jumpFromGround", at = @At("HEAD"), cancellable = true)
    private void ether_craft$onJumpFromGround(CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!(self instanceof Player player)) return;
        if (player.level().isClientSide()) return;
        if (player.onGround()) return;

        if (PlatingEventHandler.tryJump(player)) {
            self.setDeltaMovement(self.getDeltaMovement().add(0, self.getJumpPower(), 0));
            self.hasImpulse = true;
            ci.cancel();
        }
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add src/main/java/studio/fantasyit/ether_craft/mixin/plating/LivingEntityJumpMixin.java
git commit -m "feat: add LivingEntityJumpMixin for coyote time"
```

---

### Task 24: Update mixins.json

**Files:**
- Modify: `src/main/resources/ether_craft.mixins.json`

- [ ] **Step 1: Register the mixin**

In `"mixins": []` → `"mixins": ["plating.LivingEntityJumpMixin"]`

The mixin package is `studio.fantasyit.ether_craft.mixin` so the reference is `plating.LivingEntityJumpMixin`.

```json
{
  "required": true,
  "minVersion": "0.8",
  "package": "studio.fantasyit.ether_craft.mixin",
  "compatibilityLevel": "JAVA_21",
  "mixins": [
    "plating.LivingEntityJumpMixin"
  ],
  "client": [
    "BufferBuilderAccessor"
  ],
  "injectors": {
    "defaultRequire": 1
  },
  "overwrites": {
    "requireAnnotations": true
  }
}
```

- [ ] **Step 2: Commit**

```bash
git add src/main/resources/ether_craft.mixins.json
git commit -m "feat: register LivingEntityJumpMixin in mixins.json"
```

---

### Task 25: Final build and verification

- [ ] **Step 1: Full project build**

Run: `idea_build_project` with `rebuild=true`

Expected: No errors.

- [ ] **Step 2: Fix any compile errors**

Read each error, fix imports, type issues, etc.

- [ ] **Step 3: Commit any fixes**

```bash
git add -u
git commit -m "fix: compile errors from plating effects expansion"
```

---

### Task 26 (Final): End-to-end review

- [ ] **Step 1: Verify all files are in correct packages**

Check:
- `event/` no longer contains `PlatingEventHandler.java` or `PlatingTooltipHandler.java`
- `plating/event/` has `PlatingEventHandler.java`, `PlatingEventHelper.java`, `PlatingItemEntityTicker.java`
- `plating/client/` has `PlatingTooltipHandler.java`, `PlatingClientEventHandler.java`
- `plating/trigger/` has all 9 interfaces
- `plating/effects/` has all 6 effect implementations
- `plating/attachment/` has `PlatingPlayerAttachment.java`
- `mixin/plating/` has `LivingEntityJumpMixin.java`
- `network/c2s/` has `PlatingTriggerC2S.java`, `PlatingTriggerC2SHandler.java`
- `network/s2c/` has `PlatingSoulStateS2C.java`
- `Config.java` has 8 new entries
- `AttachmentDataRegistry.java` has `PLATING_PLAYER`
- `PlatingManager.init()` registers 6 effects
- `Network.java` registers 2 new packets

- [ ] **Step 2: Run final build**

Run: `idea_build_project` with `rebuild=true`
Expected: BUILD SUCCESSFUL.

---

## Plan Summary

| Phase | Tasks | Files |
|-------|-------|-------|
| Scaffolding | 1-11 | 14 new, 5 modified |
| Effects | 16-21 | 5 new, 1 modified |
| Client | 13, 22 | 2 new |
| Mixin | 23-24 | 1 new, 1 modified |
| Config | 15 | 1 modified |
| Verify | 25-26 | — |
| **Total** | **26 tasks** | **19 new, 13 modified** |
