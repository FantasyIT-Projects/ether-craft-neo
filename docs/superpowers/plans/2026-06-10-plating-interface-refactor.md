# Plating Interface Refactor

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Refactor plating interfaces: remove `IPlatingEffect` inheritance from trigger interfaces, add static `ID` to effects, delete `getEffectByEther`, change `Player`→`LivingEntity` throughout, add `hasPlating(stack, id)` variant.

**Architecture:** Trigger interfaces become pure behavioral contracts (no `IPlatingEffect` inheritance). Each effect class gets a static `ID` constant and explicitly `implements IPlatingEffect` with `getId()` returning it. `PlatingManager.init()` uses static IDs. All `Player` params become `LivingEntity` with `(Player)` casts at 3 call sites where Player-only methods are needed.

**Tech Stack:** Java 25, NeoForge, Minecraft 26.1.2

---

### File change overview

| Category | Files | Change |
|---|---|---|
| Trigger interfaces (13) | `trigger/IPlating*Trigger.java` etc. | Remove `extends IPlatingEffect`, `Player`→`LivingEntity` |
| Trigger interfaces (2) | `trigger/IPlatingBlockingTrigger.java`, `trigger/IWithoutContextPlayerTicking.java` | `Player`→`LivingEntity` only |
| Effect classes (14) | `effects/*PlatingEffect.java` | Add `ID` static, `getId()`, remove `getEffectByEther`, `Player`→`LivingEntity`, add `implements IPlatingEffect` where missing |
| Helper | `helper/PlatingUtil.java` | Add `hasPlating(ItemStack, Identifier)` |
| Event helper | `event/PlatingEventHelper.java` | `PlatingTrigger`+forEach methods: `Player`→`LivingEntity` |
| Event handler | `event/PlatingEventHandler.java` | Lambda params: `Player`→`LivingEntity` |
| Manager | `PlatingManager.java` | `init()`: use static `XxxEffect.ID` |
| Interface | `effects/IPlatingEffect.java` | No change (keep `getId()`) |

---

### Task 1: PlatingUtil — add hasPlating(stack, id) variant

**Files:**
- Modify: `src/main/java/studio/fantasyit/ether_craft/plating/helper/PlatingUtil.java`

- [ ] **Step 1: Add hasPlating(ItemStack, Identifier) method**

Add after the existing `hasPlating(ItemStack)` method (currently line 14):

```java
public static boolean hasPlating(ItemStack stack, Identifier id) {
    for (PlatingData d : getPlatingData(stack)) {
        if (d.id().equals(id)) return true;
    }
    return false;
}
```

---

### Task 2: Trigger interfaces — remove extends IPlatingEffect, Player→LivingEntity

**Files:** 15 files in `src/main/java/studio/fantasyit/ether_craft/plating/trigger/`

- [ ] **Step 1: Modify IPlatingAttackTrigger.java**

Replace content:

```java
package studio.fantasyit.ether_craft.plating.trigger;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import studio.fantasyit.ether_craft.plating.PlatingData;

public interface IPlatingAttackTrigger {
    default boolean onAttack(PlatingData data, ItemStack stack, LivingEntity entity, Entity target) {
        return false;
    }
}
```

- [ ] **Step 2: Modify IPlatingRightClickTrigger.java**

Replace content:

```java
package studio.fantasyit.ether_craft.plating.trigger;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import studio.fantasyit.ether_craft.plating.PlatingData;

public interface IPlatingRightClickTrigger {
    boolean onRightClick(PlatingData data, ItemStack stack, LivingEntity entity);
}
```

- [ ] **Step 3: Modify IPlatingTickEquippedTrigger.java**

Replace content:

```java
package studio.fantasyit.ether_craft.plating.trigger;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import studio.fantasyit.ether_craft.plating.PlatingData;

public interface IPlatingTickEquippedTrigger {
    void onHoldTick(PlatingData data, ItemStack stack, LivingEntity entity);
}
```

- [ ] **Step 4: Modify IPlatingKillTrigger.java**

Replace content:

```java
package studio.fantasyit.ether_craft.plating.trigger;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import studio.fantasyit.ether_craft.plating.PlatingData;

public interface IPlatingKillTrigger {
    void onKill(PlatingData data, ItemStack stack, LivingEntity entity, LivingEntity target, LivingDropsEvent event);
}
```

- [ ] **Step 5: Modify IPlatingCritTrigger.java**

Replace content:

```java
package studio.fantasyit.ether_craft.plating.trigger;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.player.CriticalHitEvent;
import studio.fantasyit.ether_craft.plating.PlatingData;

public interface IPlatingCritTrigger {
    void onCriticalHit(PlatingData data, ItemStack stack, LivingEntity entity, CriticalHitEvent event);
}
```

- [ ] **Step 6: Modify IPlatingCritDamageModifier.java**

Replace content:

```java
package studio.fantasyit.ether_craft.plating.trigger;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.player.CriticalHitEvent;
import studio.fantasyit.ether_craft.plating.PlatingData;

public interface IPlatingCritDamageModifier {
    void onCriticalHit(PlatingData data, ItemStack stack, LivingEntity entity, CriticalHitEvent event);
}
```

- [ ] **Step 7: Modify IPlatingArrowShotTrigger.java**

Replace content:

```java
package studio.fantasyit.ether_craft.plating.trigger;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import studio.fantasyit.ether_craft.plating.PlatingData;

public interface IPlatingArrowShotTrigger {
    void onArrowShot(PlatingData data, ItemStack stack, LivingEntity entity, AbstractArrow arrow);
}
```

- [ ] **Step 8: Modify IPlatingBlockDropsTrigger.java**

Replace content:

```java
package studio.fantasyit.ether_craft.plating.trigger;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import studio.fantasyit.ether_craft.plating.PlatingData;

public interface IPlatingBlockDropsTrigger {
    void onBlockDrops(PlatingData data, ItemStack stack, LivingEntity entity, BlockDropsEvent event);
}
```

- [ ] **Step 9: Modify IPlatingBreakBlockTrigger.java**

Replace content:

```java
package studio.fantasyit.ether_craft.plating.trigger;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import studio.fantasyit.ether_craft.plating.PlatingData;

public interface IPlatingBreakBlockTrigger {
    default boolean onBreakBlock(PlatingData data, ItemStack stack, LivingEntity entity, BlockPos pos, BlockState state) {
        return false;
    }
}
```

- [ ] **Step 10: Modify IPlatingVirtualWalkableProvider.java**

Replace content:

```java
package studio.fantasyit.ether_craft.plating.trigger;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import studio.fantasyit.ether_craft.plating.PlatingData;

public interface IPlatingVirtualWalkableProvider {
    int providerVirtualWalkableAt(PlatingData data, ItemStack stack, Level level, LivingEntity entity, BlockPos pos, @Nullable BlockPos lastGroundPos);

    void tickOnBlock(PlatingData data, ItemStack stack, Level level, LivingEntity entity, BlockPos pos);
}
```

- [ ] **Step 11: Modify IPlatingUseOnBlockTrigger.java**

Replace content:

```java
package studio.fantasyit.ether_craft.plating.trigger;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import studio.fantasyit.ether_craft.plating.PlatingData;

public interface IPlatingUseOnBlockTrigger {
    @Nullable InteractionResult onUseOnBlock(PlatingData data, ItemStack stack, LivingEntity entity, BlockPos pos, BlockState state);
}
```

- [ ] **Step 12: Modify IPlatingUseOnEntityTrigger.java**

Replace content:

```java
package studio.fantasyit.ether_craft.plating.trigger;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import studio.fantasyit.ether_craft.plating.PlatingData;

public interface IPlatingUseOnEntityTrigger {
    default void onUseOnEntity(PlatingData data, ItemStack stack, LivingEntity entity, Entity target) {
    }
}
```

- [ ] **Step 13: Modify IPlatingUseTrigger.java**

Replace content:

```java
package studio.fantasyit.ether_craft.plating.trigger;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import studio.fantasyit.ether_craft.plating.PlatingData;

public interface IPlatingUseTrigger {
    default void onUse(PlatingData data, ItemStack stack, LivingEntity entity) {
    }
}
```

- [ ] **Step 14: Modify IPlatingBlockingTrigger.java**

Replace content:

```java
package studio.fantasyit.ether_craft.plating.trigger;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import studio.fantasyit.ether_craft.plating.PlatingData;

public interface IPlatingBlockingTrigger {
    void blocked(PlatingData data, LivingEntity entity, ItemStack stack, DamageContainer damage);
}
```

- [ ] **Step 15: Modify IWithoutContextPlayerTicking.java**

Replace content:

```java
package studio.fantasyit.ether_craft.plating.trigger;

import net.minecraft.world.entity.LivingEntity;

public interface IWithoutContextPlayerTicking {
    void tickPlayer(LivingEntity entity);
}
```

---

### Task 3: Effect classes — Damage, Dash, HighJump, NoGravity, CoyoteTime

**Files:** 5 files in `src/main/java/studio/fantasyit/ether_craft/plating/effects/`

- [ ] **Step 1: Modify DamagePlatingEffect.java** (needs `(Player)` cast for `playerAttack`)

Add `import net.minecraft.resources.Identifier;` and `import studio.fantasyit.ether_craft.EtherCraft;`.
Add `implements IPlatingEffect, IPlatingAttackTrigger`.
Add `public static final Identifier ID = EtherCraft.id("damage");` + `getId()`.
Remove `getEffectByEther`.
Change `Player player` → `LivingEntity entity` in `onAttack`, add `instanceof Player` cast where needed.

Replace entire file:

```java
package studio.fantasyit.ether_craft.plating.effects;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.plating.PlatingData;
import studio.fantasyit.ether_craft.plating.helper.PlatingUtil;
import studio.fantasyit.ether_craft.plating.trigger.IPlatingAttackTrigger;

public class DamagePlatingEffect implements IPlatingEffect, IPlatingAttackTrigger {
    public static final Identifier ID = EtherCraft.id("damage");

    @Override
    public Identifier getId() {
        return ID;
    }

    @Override
    public boolean onAttack(PlatingData data, ItemStack stack, LivingEntity entity, Entity target) {
        if (!(target instanceof LivingEntity living)) return false;
        if (!(entity instanceof Player player)) return false;
        if (!PlatingUtil.canExtractEther(stack, 1)) return false;
        PlatingUtil.extractEther(stack, 1);
        living.hurt(living.damageSources().playerAttack(player), (float) data.effect());
        return true;
    }
}
```

- [ ] **Step 2: Modify DashPlatingEffect.java**

Add `import net.minecraft.resources.Identifier;` and `import studio.fantasyit.ether_craft.EtherCraft;`.
Add `implements IPlatingEffect, IPlatingRightClickTrigger`.
Add `public static final Identifier ID = EtherCraft.id("dash");` + `getId()`.
Remove `getEffectByEther`.
Change `Player player` → `LivingEntity entity` in `onRightClick`.

Replace entire file:

```java
package studio.fantasyit.ether_craft.plating.effects;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import studio.fantasyit.ether_craft.Config;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.plating.PlatingData;
import studio.fantasyit.ether_craft.plating.helper.PlatingUtil;
import studio.fantasyit.ether_craft.plating.trigger.IPlatingRightClickTrigger;

public class DashPlatingEffect implements IPlatingEffect, IPlatingRightClickTrigger {
    public static final Identifier ID = EtherCraft.id("dash");

    @Override
    public Identifier getId() {
        return ID;
    }

    @Override
    public boolean onRightClick(PlatingData data, ItemStack stack, LivingEntity entity) {
        if (!(entity.level() instanceof ServerLevel level)) return false;
        if (data.isCd(level)) return false;

        if (!PlatingUtil.canExtractEther(stack, Config.platingDashEtherCost)) return false;
        PlatingUtil.extractEther(stack, Config.platingDashEtherCost);

        Vec3 look = entity.getLookAngle();
        double distance = data.effect() * 0.5;
        entity.setDeltaMovement(look.x * distance, 0.1, look.z * distance);
        entity.hurtMarked = true;

        PlatingData updated = data.copyWithCoolDown(level, Config.platingDashCdTicks);
        PlatingUtil.updatePlatingData(stack, updated);

        return true;
    }
}
```

- [ ] **Step 3: Modify HighJumpPlatingEffect.java**

Add `import net.minecraft.resources.Identifier;` and `import studio.fantasyit.ether_craft.EtherCraft;`.
Add `implements IPlatingEffect, IPlatingRightClickTrigger`.
Add `public static final Identifier ID = EtherCraft.id("high_jump");` + `getId()`.
Remove `getEffectByEther`.
Change `Player player` → `LivingEntity entity` in `onRightClick`.

Replace entire file:

```java
package studio.fantasyit.ether_craft.plating.effects;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import studio.fantasyit.ether_craft.Config;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.plating.PlatingData;
import studio.fantasyit.ether_craft.plating.helper.PlatingUtil;
import studio.fantasyit.ether_craft.plating.trigger.IPlatingRightClickTrigger;

public class HighJumpPlatingEffect implements IPlatingEffect, IPlatingRightClickTrigger {
    public static final Identifier ID = EtherCraft.id("high_jump");

    @Override
    public Identifier getId() {
        return ID;
    }

    @Override
    public boolean onRightClick(PlatingData data, ItemStack stack, LivingEntity entity) {
        if (!(entity.level() instanceof ServerLevel level)) return false;
        if (data.isCd(level)) return false;

        if (!PlatingUtil.canExtractEther(stack, Config.platingHighJumpEtherCost)) return false;
        PlatingUtil.extractEther(stack, Config.platingHighJumpEtherCost);

        double height = data.effect() * 1.0;
        entity.setDeltaMovement(entity.getDeltaMovement().x, height, entity.getDeltaMovement().z);
        entity.hurtMarked = true;

        entity.addEffect(new MobEffectInstance(MobEffects.JUMP_BOOST, 60, 0, false, false));

        PlatingData updated = data.copyWithCoolDown(level, Config.platingHighJumpCdTicks);
        PlatingUtil.updatePlatingData(stack, updated);

        return true;
    }
}
```

- [ ] **Step 4: Modify NoGravityPlatingEffect.java**

Add `import net.minecraft.resources.Identifier;` and `import studio.fantasyit.ether_craft.EtherCraft;`.
Add `implements IPlatingEffect, IPlatingArrowShotTrigger`.
Add `public static final Identifier ID = EtherCraft.id("no_gravity");` + `getId()`.
Remove `getEffectByEther`.
Change `Player player` → `LivingEntity entity` in `onArrowShot`.

Replace entire file:

```java
package studio.fantasyit.ether_craft.plating.effects;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import studio.fantasyit.ether_craft.Config;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.plating.PlatingData;
import studio.fantasyit.ether_craft.plating.helper.PlatingUtil;
import studio.fantasyit.ether_craft.plating.trigger.IPlatingArrowShotTrigger;

public class NoGravityPlatingEffect implements IPlatingEffect, IPlatingArrowShotTrigger {
    public static final Identifier ID = EtherCraft.id("no_gravity");

    @Override
    public Identifier getId() {
        return ID;
    }

    @Override
    public void onArrowShot(PlatingData data, ItemStack stack, LivingEntity entity, AbstractArrow arrow) {
        if (!PlatingUtil.canExtractEther(stack, Config.platingNoGravityEtherPerArrow)) return;
        PlatingUtil.extractEther(stack, Config.platingNoGravityEtherPerArrow);
        arrow.setNoGravity(true);
    }
}
```

- [ ] **Step 5: Modify CoyoteTimePlatingEffect.java**

Add `import net.minecraft.resources.Identifier;` and `import studio.fantasyit.ether_craft.EtherCraft;`.
Add `implements IPlatingEffect, IPlatingVirtualWalkableProvider`.
Add `public static final Identifier ID = EtherCraft.id("coyote_time");` + `getId()`.
Remove `getEffectByEther`.
Change `Player player` → `LivingEntity entity` in both methods.

Replace entire file:

```java
package studio.fantasyit.ether_craft.plating.effects;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import studio.fantasyit.ether_craft.Config;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.plating.PlatingData;
import studio.fantasyit.ether_craft.plating.helper.PlatingUtil;
import studio.fantasyit.ether_craft.plating.trigger.IPlatingVirtualWalkableProvider;

public class CoyoteTimePlatingEffect implements IPlatingEffect, IPlatingVirtualWalkableProvider {
    public static final Identifier ID = EtherCraft.id("coyote_time");
    private static final long COYOTE_WINDOW = 40L;

    @Override
    public Identifier getId() {
        return ID;
    }

    @Override
    public int providerVirtualWalkableAt(PlatingData data, ItemStack stack, Level level, LivingEntity entity, BlockPos pos, @Nullable BlockPos jumpStartAt) {
        if (jumpStartAt == null) return Integer.MIN_VALUE;
        if (data.hasCd() && !data.isCd(level)) return Integer.MIN_VALUE;
        if (!PlatingUtil.canExtractEther(stack, Config.platingCoyoteTimeEtherPerJump)) return Integer.MIN_VALUE;
        PlatingUtil.extractEther(stack, Config.platingCoyoteTimeEtherPerJump);
        if (!data.hasCd())
            PlatingUtil.updatePlatingData(stack, data.copyWithCoolDown(level, COYOTE_WINDOW));
        return jumpStartAt.getY();
    }

    @Override
    public void tickOnBlock(PlatingData data, ItemStack stack, Level level, LivingEntity entity, BlockPos pos) {
        if (data.hasCd())
            PlatingUtil.updatePlatingData(stack, data.copyClearCoolDown());
    }
}
```

---

### Task 4: Effect classes — Crit, CritDamage, HeadHunt, Tracking

**Files:** 4 files in `src/main/java/studio/fantasyit/ether_craft/plating/effects/`

- [ ] **Step 1: Modify CritPlatingEffect.java**

Add `import net.minecraft.resources.Identifier;` and `import studio.fantasyit.ether_craft.EtherCraft;`.
Add `implements IPlatingEffect, IPlatingCritTrigger`.
Add `public static final Identifier ID = EtherCraft.id("crit");` + `getId()`.
Remove `getEffectByEther`.
Change `Player player` → `LivingEntity entity` in `onCriticalHit`.

Replace entire file:

```java
package studio.fantasyit.ether_craft.plating.effects;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.player.CriticalHitEvent;
import studio.fantasyit.ether_craft.Config;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.plating.PlatingData;
import studio.fantasyit.ether_craft.plating.helper.PlatingUtil;
import studio.fantasyit.ether_craft.plating.trigger.IPlatingCritTrigger;

public class CritPlatingEffect implements IPlatingEffect, IPlatingCritTrigger {
    public static final Identifier ID = EtherCraft.id("crit");

    @Override
    public Identifier getId() {
        return ID;
    }

    @Override
    public void onCriticalHit(PlatingData data, ItemStack stack, LivingEntity entity, CriticalHitEvent event) {
        if (event.isCriticalHit()) return;
        if (!PlatingUtil.canExtractEther(stack, Config.platingCritEtherPerAttack)) return;

        double chance = data.effect();
        if (entity.getRandom().nextDouble() < chance) {
            event.setCriticalHit(true);
            PlatingUtil.extractEther(stack, Config.platingCritEtherPerAttack);
        }
    }
}
```

- [ ] **Step 2: Modify CritDamagePlatingEffect.java**

Add `import net.minecraft.resources.Identifier;` and `import studio.fantasyit.ether_craft.EtherCraft;`.
Add `implements IPlatingEffect, IPlatingCritDamageModifier`.
Add `public static final Identifier ID = EtherCraft.id("crit_damage");` + `getId()`.
Remove `getEffectByEther`.
Change `Player player` → `LivingEntity entity` in `onCriticalHit`.

Replace entire file:

```java
package studio.fantasyit.ether_craft.plating.effects;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.player.CriticalHitEvent;
import studio.fantasyit.ether_craft.Config;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.plating.PlatingData;
import studio.fantasyit.ether_craft.plating.helper.PlatingUtil;
import studio.fantasyit.ether_craft.plating.trigger.IPlatingCritDamageModifier;

public class CritDamagePlatingEffect implements IPlatingEffect, IPlatingCritDamageModifier {
    public static final Identifier ID = EtherCraft.id("crit_damage");

    @Override
    public Identifier getId() {
        return ID;
    }

    @Override
    public void onCriticalHit(PlatingData data, ItemStack stack, LivingEntity entity, CriticalHitEvent event) {
        if (!event.isCriticalHit()) return;
        if (data.effect() <= 0) return;
        if (!PlatingUtil.canExtractEther(stack, Config.platingCritDamageEtherPerAttack)) return;

        event.setDamageMultiplier(event.getDamageMultiplier() + (float) data.effect());
        PlatingUtil.extractEther(stack, Config.platingCritDamageEtherPerAttack);
    }
}
```

- [ ] **Step 3: Modify HeadHuntPlatingEffect.java**

Add `import net.minecraft.resources.Identifier;` and `import studio.fantasyit.ether_craft.EtherCraft;`.
Add `implements IPlatingEffect, IPlatingKillTrigger`.
Add `public static final Identifier ID = EtherCraft.id("head_hunt");` + `getId()`.
Remove `getEffectByEther`.
Change `Player player` → `LivingEntity entity` in `onKill`.

Replace entire file (keeping commented-out implementation):

```java
package studio.fantasyit.ether_craft.plating.effects;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.plating.PlatingData;
import studio.fantasyit.ether_craft.plating.trigger.IPlatingKillTrigger;

public class HeadHuntPlatingEffect implements IPlatingEffect, IPlatingKillTrigger {
    public static final Identifier ID = EtherCraft.id("head_hunt");

    @Override
    public Identifier getId() {
        return ID;
    }

    @Override
    public void onKill(PlatingData data, ItemStack stack, LivingEntity entity, LivingEntity target, LivingDropsEvent event) {
//        if (!PlatingUtil.canExtractEther(stack, Config.platingHeadHuntEtherPerKill)) return;
//
//        double chance = data.effect();
//        if (entity.getRandom().nextDouble() >= chance) return;
//
//        ItemStack head = HEAD_MAP.get(target.getType());
//        if (head == null || head.isEmpty()) return;
//
//        Level level = target.level();
//        ItemEntity headEntity = new ItemEntity(level, target.getX(), target.getY(), target.getZ(), head.copy());
//        event.getDrops().add(headEntity);
//        PlatingUtil.extractEther(stack, Config.platingHeadHuntEtherPerKill);
    }
}
```

- [ ] **Step 4: Modify TrackingPlatingEffect.java**

Add `import net.minecraft.resources.Identifier;` and `import studio.fantasyit.ether_craft.EtherCraft;`.
Add `implements IPlatingEffect, IPlatingArrowShotTrigger`.
Add `public static final Identifier ID = EtherCraft.id("tracking");` + `getId()`.
Remove `getEffectByEther`.
Change `Player player` → `LivingEntity entity` in `onArrowShot`.

Replace entire file:

```java
package studio.fantasyit.ether_craft.plating.effects;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import studio.fantasyit.ether_craft.Config;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.plating.PlatingData;
import studio.fantasyit.ether_craft.plating.TrackingData;
import studio.fantasyit.ether_craft.plating.helper.PlatingUtil;
import studio.fantasyit.ether_craft.plating.trigger.IPlatingArrowShotTrigger;
import studio.fantasyit.ether_craft.register.AttachmentDataRegistry;

public class TrackingPlatingEffect implements IPlatingEffect, IPlatingArrowShotTrigger {
    public static final Identifier ID = EtherCraft.id("tracking");

    @Override
    public Identifier getId() {
        return ID;
    }

    @Override
    public void onArrowShot(PlatingData data, ItemStack stack, LivingEntity entity, AbstractArrow arrow) {
        if (!PlatingUtil.canExtractEther(stack, Config.platingTrackingEtherPerArrow)) return;

        PlatingUtil.extractEther(stack, Config.platingTrackingEtherPerArrow);
        arrow.setData(AttachmentDataRegistry.ARROW_TRACKING.get(),
                new TrackingData(Config.platingTrackingRange, Config.platingTrackingStrength));
    }
}
```

---

### Task 5: Effect classes — BreakToInv, KillToInv, StoneAbsorb

**Files:** 3 files in `src/main/java/studio/fantasyit/ether_craft/plating/effects/`

- [ ] **Step 1: Modify BreakToInventoryPlatingEffect.java** (needs `(Player)` cast for `getInventory()`)

Add `import net.minecraft.resources.Identifier;`, `import studio.fantasyit.ether_craft.EtherCraft;`, `import net.minecraft.world.entity.player.Player;`.
Add `implements IPlatingEffect, IPlatingBlockDropsTrigger`.
Add `public static final Identifier ID = EtherCraft.id("break_to_inv");` + `getId()`.
Remove `getEffectByEther`.
Change `Player player` → `LivingEntity entity` in `onBlockDrops`, add `instanceof Player` cast.

Replace entire file:

```java
package studio.fantasyit.ether_craft.plating.effects;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import studio.fantasyit.ether_craft.Config;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.plating.PlatingData;
import studio.fantasyit.ether_craft.plating.helper.PlatingUtil;
import studio.fantasyit.ether_craft.plating.trigger.IPlatingBlockDropsTrigger;

import java.util.ArrayList;
import java.util.List;

public class BreakToInventoryPlatingEffect implements IPlatingEffect, IPlatingBlockDropsTrigger {
    public static final Identifier ID = EtherCraft.id("break_to_inv");

    @Override
    public Identifier getId() {
        return ID;
    }

    @Override
    public void onBlockDrops(PlatingData data, ItemStack stack, LivingEntity entity, BlockDropsEvent event) {
        if (event.getDrops().isEmpty()) return;
        if (!(entity instanceof Player player)) return;
        if (!PlatingUtil.canExtractEther(stack, Config.platingBreakToInvEtherPerBlock)) return;

        List<ItemEntity> absorbed = new ArrayList<>();
        for (ItemEntity dropEntity : event.getDrops()) {
            if (player.getInventory().add(dropEntity.getItem())) {
                absorbed.add(dropEntity);
            }
        }
        event.getDrops().removeAll(absorbed);
        if (!absorbed.isEmpty()) {
            PlatingUtil.extractEther(stack, Config.platingBreakToInvEtherPerBlock);
        }
    }
}
```

- [ ] **Step 2: Modify KillToInventoryPlatingEffect.java** (needs `(Player)` cast for `getInventory()`)

Add `import net.minecraft.resources.Identifier;`, `import studio.fantasyit.ether_craft.EtherCraft;`, `import net.minecraft.world.entity.player.Player;`.
Add `implements IPlatingEffect, IPlatingKillTrigger`.
Add `public static final Identifier ID = EtherCraft.id("kill_to_inv");` + `getId()`.
Remove `getEffectByEther`.
Change `Player player` → `LivingEntity entity` in `onKill`, add `instanceof Player` cast.

Replace entire file:

```java
package studio.fantasyit.ether_craft.plating.effects;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import studio.fantasyit.ether_craft.Config;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.plating.PlatingData;
import studio.fantasyit.ether_craft.plating.helper.PlatingUtil;
import studio.fantasyit.ether_craft.plating.trigger.IPlatingKillTrigger;

import java.util.ArrayList;
import java.util.List;

public class KillToInventoryPlatingEffect implements IPlatingEffect, IPlatingKillTrigger {
    public static final Identifier ID = EtherCraft.id("kill_to_inv");

    @Override
    public Identifier getId() {
        return ID;
    }

    @Override
    public void onKill(PlatingData data, ItemStack stack, LivingEntity entity, LivingEntity target, LivingDropsEvent event) {
        if (event.getDrops().isEmpty()) return;
        if (!(entity instanceof Player player)) return;
        if (!PlatingUtil.canExtractEther(stack, Config.platingKillToInvEtherPerKill)) return;

        List<ItemEntity> absorbed = new ArrayList<>();
        for (ItemEntity dropEntity : event.getDrops()) {
            if (player.getInventory().add(dropEntity.getItem())) {
                absorbed.add(dropEntity);
            }
        }
        event.getDrops().removeAll(absorbed);
        if (!absorbed.isEmpty()) {
            PlatingUtil.extractEther(stack, Config.platingKillToInvEtherPerKill);
        }
    }
}
```

- [ ] **Step 3: Modify StoneAbsorbPlatingEffect.java**

Add `import net.minecraft.resources.Identifier;` and `import studio.fantasyit.ether_craft.EtherCraft;`.
Add `implements IPlatingEffect, IPlatingBlockDropsTrigger`.
Add `public static final Identifier ID = EtherCraft.id("stone_absorb");` + `getId()`.
Remove `getEffectByEther`.
Change `Player player` → `LivingEntity entity` in `onBlockDrops`.

Replace entire file:

```java
package studio.fantasyit.ether_craft.plating.effects;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import studio.fantasyit.ether_craft.Config;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.plating.PlatingData;
import studio.fantasyit.ether_craft.plating.helper.PlatingUtil;
import studio.fantasyit.ether_craft.plating.trigger.IPlatingBlockDropsTrigger;

public class StoneAbsorbPlatingEffect implements IPlatingEffect, IPlatingBlockDropsTrigger {
    public static final Identifier ID = EtherCraft.id("stone_absorb");

    private static final TagKey<Block> STONE_ABSORBABLE = TagKey.create(
            Registries.BLOCK, EtherCraft.id("stone_absorbable"));

    @Override
    public Identifier getId() {
        return ID;
    }

    @Override
    public void onBlockDrops(PlatingData data, ItemStack stack, LivingEntity entity, BlockDropsEvent event) {
        if (!event.getState().is(STONE_ABSORBABLE)) return;
        if (event.getDrops().isEmpty()) return;

        event.getDrops().clear();
        event.setDroppedExperience(0);
        PlatingUtil.addEther(stack, Config.platingStoneAbsorbEtherPerBlock);
    }
}
```

---

### Task 6: Effect classes — BlockPlatingEffect, CamouflagePlatingEffect

**Files:** 2 files in `src/main/java/studio/fantasyit/ether_craft/plating/effects/`

- [ ] **Step 1: Modify BlockPlatingEffect.java** (uses `IPlatingEffect`, `IWithoutContextPlayerTicking`, `IPlatingBlockingTrigger`)

Add `import net.minecraft.resources.Identifier;` and `import studio.fantasyit.ether_craft.EtherCraft;`.
Add `public static final Identifier ID = EtherCraft.id("block");` + `getId()`.
Remove `getEffectByEther`.
Change `Player player` → `LivingEntity entity` in all methods.
Fix `hasPlating(stack, ID)` call.

Replace entire file:

```java
package studio.fantasyit.ether_craft.plating.effects;

import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlocksAttacks;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import studio.fantasyit.ether_craft.Config;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.plating.PlatingData;
import studio.fantasyit.ether_craft.plating.helper.PlatingUtil;
import studio.fantasyit.ether_craft.plating.trigger.IInstanceTrigger;
import studio.fantasyit.ether_craft.plating.trigger.IPlatingBlockingTrigger;
import studio.fantasyit.ether_craft.plating.trigger.IWithoutContextPlayerTicking;
import studio.fantasyit.ether_craft.register.DataComponentRegistry;

import java.util.List;
import java.util.Optional;

public class BlockPlatingEffect implements IPlatingEffect, IInstanceTrigger, IWithoutContextPlayerTicking, IPlatingBlockingTrigger {
    public static final Identifier ID = EtherCraft.id("block");

    @Override
    public Identifier getId() {
        return ID;
    }

    @Override
    public void onPlatted(PlatingData data, ItemStack stack) {
        stack.set(DataComponentRegistry.TEMP_BLOCKING, true);
        stack.set(DataComponents.BLOCKS_ATTACKS, new BlocksAttacks(
                0.0f,
                1.0f,
                List.of(new BlocksAttacks.DamageReduction(
                        90f,
                        Optional.empty(),
                        0f,
                        (float) Config.platingBlockDamageReduction
                )),
                BlocksAttacks.ItemDamageFunction.DEFAULT,
                Optional.empty(),
                Optional.of(SoundEvents.SHIELD_BLOCK),
                Optional.of(SoundEvents.SHIELD_BREAK)
        ));
    }

    private static void removeBlocking(ItemStack stack) {
        stack.remove(DataComponentRegistry.TEMP_BLOCKING);
        stack.remove(DataComponents.BLOCKS_ATTACKS);
    }

    @Override
    public void tickPlayer(LivingEntity entity) {
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = entity.getItemInHand(hand);
            if (stack.has(DataComponents.BLOCKS_ATTACKS) && stack.has(DataComponentRegistry.TEMP_BLOCKING)) {
                if (!PlatingUtil.hasPlating(stack, ID)) {
                    removeBlocking(stack);
                }
            }
        }
    }

    @Override
    public void blocked(PlatingData data, LivingEntity entity, ItemStack stack, DamageContainer damage) {
        PlatingUtil.extractEther(stack, Config.platingBlockEtherPerTick);
        if (!PlatingUtil.canExtractEther(stack, Config.platingBlockEtherPerTick)) {
            removeBlocking(stack);
        }
    }
}
```

- [ ] **Step 2: Modify CamouflagePlatingEffect.java**

Add `import net.minecraft.resources.Identifier;` and `import studio.fantasyit.ether_craft.EtherCraft;`.
No `implements` change needed (already has `IPlatingEffect, IPlatingTickEquippedTrigger`).
Add `public static final Identifier ID = EtherCraft.id("camouflage");` + `getId()`.
Remove `getEffectByEther`.
Change `Player player` → `LivingEntity entity` in `onHoldTick`.

Replace entire file:

```java
package studio.fantasyit.ether_craft.plating.effects;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import studio.fantasyit.ether_craft.Config;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.plating.CamouflageState;
import studio.fantasyit.ether_craft.plating.PlatingData;
import studio.fantasyit.ether_craft.plating.helper.PlatingUtil;
import studio.fantasyit.ether_craft.plating.trigger.IPlatingTickEquippedTrigger;
import studio.fantasyit.ether_craft.register.AttachmentDataRegistry;

import java.util.List;

public class CamouflagePlatingEffect implements IPlatingEffect, IPlatingTickEquippedTrigger {
    public static final Identifier ID = EtherCraft.id("camouflage");

    private static final int MOB_CLEAR_INTERVAL = 20;

    @Override
    public Identifier getId() {
        return ID;
    }

    @Override
    public void onHoldTick(PlatingData data, ItemStack stack, LivingEntity entity) {
        if (!(entity instanceof Player player)) return;

        CamouflageState state = player.getExistingData(AttachmentDataRegistry.CAMOUFLAGE_STATE.get())
                .orElse(CamouflageState.INACTIVE);

        Vec3 pos = player.position();
        long posHash = hashPosition(pos);

        if (posHash != state.lastPosHash()) {
            if (state.isActive()) {
                deactivate(player);
            }
            player.setData(AttachmentDataRegistry.CAMOUFLAGE_STATE.get(),
                    new CamouflageState(false, 0, BlockPos.ZERO, 0f, posHash));
            return;
        }

        int newTicks = state.standStillTicks() + 1;

        if (!state.isActive()) {
            if (newTicks >= Config.platingCamouflageStandDuration) {
                PlatingUtil.extractEther(stack, Config.platingCamouflageEtherCost);
                activate(player, player.blockPosition(), player.getYRot(), posHash);
            } else {
                player.setData(AttachmentDataRegistry.CAMOUFLAGE_STATE.get(),
                        new CamouflageState(false, newTicks, BlockPos.ZERO, 0f, posHash));
            }
        } else {
            if (newTicks % MOB_CLEAR_INTERVAL == 0) {
                clearMobTargets(player);
            }

            player.setData(AttachmentDataRegistry.CAMOUFLAGE_STATE.get(),
                    new CamouflageState(true, newTicks, state.camouflagePos(), state.camouflageYaw(), posHash));
        }
    }

    private void activate(Player player, BlockPos pos, float yaw, long posHash) {
        player.setInvisible(true);
        clearMobTargets(player);
        player.setData(AttachmentDataRegistry.CAMOUFLAGE_STATE.get(),
                new CamouflageState(true, 0, pos, yaw, posHash));
    }

    private void deactivate(Player player) {
        player.setInvisible(false);
        player.setData(AttachmentDataRegistry.CAMOUFLAGE_STATE.get(), CamouflageState.INACTIVE);
    }

    private void clearMobTargets(Player player) {
        List<Mob> mobs = player.level().getEntitiesOfClass(
                Mob.class, player.getBoundingBox().inflate(32));
        for (Mob mob : mobs) {
            if (mob.getTarget() == player) {
                mob.setTarget(null);
            }
        }
    }

    private static long hashPosition(Vec3 pos) {
        long x = (long) (pos.x * 1000.0);
        long y = (long) (pos.y * 1000.0);
        long z = (long) (pos.z * 1000.0);
        return x ^ (y << 11) ^ (z << 22);
    }
}
```

---

### Task 7: PlatingEventHelper — Player→LivingEntity

**Files:**
- Modify: `src/main/java/studio/fantasyit/ether_craft/plating/event/PlatingEventHelper.java`

- [ ] **Step 1: Replace PlatingEventHelper content**

Replace entire file:

```java
package studio.fantasyit.ether_craft.plating.event;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import studio.fantasyit.ether_craft.plating.PlatingData;
import studio.fantasyit.ether_craft.plating.PlatingManager;
import studio.fantasyit.ether_craft.plating.helper.PlatingUtil;
import studio.fantasyit.ether_craft.plating.effects.IPlatingEffect;

import java.util.List;

public class PlatingEventHelper {

    public static ItemStack[] getPlatedEquipment(LivingEntity entity) {
        return new ItemStack[]{
                entity.getMainHandItem(),
                entity.getOffhandItem(),
                entity.getItemBySlot(EquipmentSlot.HEAD),
                entity.getItemBySlot(EquipmentSlot.CHEST),
                entity.getItemBySlot(EquipmentSlot.LEGS),
                entity.getItemBySlot(EquipmentSlot.FEET)
        };
    }

    @Nullable
    public static IPlatingEffect getEffect(Identifier id) {
        return PlatingManager.getEffect(id);
    }

    @FunctionalInterface
    public interface PlatingTrigger {
        void apply(IPlatingEffect effect, PlatingData data, ItemStack stack, LivingEntity entity);
    }

    public static void forEachPlating(ItemStack stack, LivingEntity entity, PlatingTrigger trigger) {
        List<PlatingData> data = PlatingUtil.getPlatingData(stack);
        if (data.isEmpty()) return;
        for (PlatingData d : data) {
            IPlatingEffect effect = PlatingManager.getEffect(d.id());
            if (effect != null) {
                trigger.apply(effect, d, stack, entity);
            }
        }
    }

    public static void forEachPlatingOnEquipment(LivingEntity entity, PlatingTrigger trigger) {
        for (ItemStack stack : getPlatedEquipment(entity)) {
            forEachPlating(stack, entity, trigger);
        }
    }
}
```

---

### Task 8: PlatingEventHandler — adapt lambda params

**Files:**
- Modify: `src/main/java/studio/fantasyit/ether_craft/plating/event/PlatingEventHandler.java`

- [ ] **Step 1: Change onPlayerTick lambda — line 51**

Replace:
```java
        PlatingEventHelper.forEachPlatingOnEquipment(player, (effect, data, stack, p) -> {
```
with:
```java
        PlatingEventHelper.forEachPlatingOnEquipment(player, (effect, data, stack, entity) -> {
```

- [ ] **Step 2: Change onRightClickItem lambda — line 84**

Replace:
```java
            PlatingEventHelper.forEachPlating(stack, player, (effect, data, s, p) -> {
```
with:
```java
            PlatingEventHelper.forEachPlating(stack, player, (effect, data, s, entity) -> {
```

- [ ] **Step 3: Fix onRightClickItem lambda body — line 85**

Replace:
```java
                if (effect instanceof IPlatingRightClickTrigger rt && rt.onRightClick(data, s, p)) {
```
with:
```java
                if (effect instanceof IPlatingRightClickTrigger rt && rt.onRightClick(data, s, entity)) {
```

- [ ] **Step 4: Change onUseItem lambda — line 97**

Replace:
```java
        PlatingEventHelper.forEachPlating(stack, player, (effect, data, s, p) -> {
            if (effect instanceof IPlatingUseTrigger use) {
                use.onUse(data, s, p);
            }
        });
```
with:
```java
        PlatingEventHelper.forEachPlating(stack, player, (effect, data, s, entity) -> {
            if (effect instanceof IPlatingUseTrigger use) {
                use.onUse(data, s, entity);
            }
        });
```

- [ ] **Step 5: Change onAttack lambda — lines 109-113**

Replace:
```java
        PlatingEventHelper.forEachPlating(stack, player, (effect, data, s, p) -> {
            if (effect instanceof IPlatingAttackTrigger attack && attack.onAttack(data, s, p, event.getTarget())) {
                event.setCanceled(true);
            }
        });
```
with:
```java
        PlatingEventHelper.forEachPlating(stack, player, (effect, data, s, entity) -> {
            if (effect instanceof IPlatingAttackTrigger attack && attack.onAttack(data, s, entity, event.getTarget())) {
                event.setCanceled(true);
            }
        });
```

- [ ] **Step 6: Change onBreakBlock lambda — lines 121-127**

Replace:
```java
        PlatingEventHelper.forEachPlating(stack, player, (effect, data, s, p) -> {
            if (effect instanceof IPlatingBreakBlockTrigger breakBlock) {
                if (breakBlock.onBreakBlock(data, s, p, event.getPos(), event.getState())) {
                    event.setCanceled(true);
                }
            }
        });
```
with:
```java
        PlatingEventHelper.forEachPlating(stack, player, (effect, data, s, entity) -> {
            if (effect instanceof IPlatingBreakBlockTrigger breakBlock) {
                if (breakBlock.onBreakBlock(data, s, entity, event.getPos(), event.getState())) {
                    event.setCanceled(true);
                }
            }
        });
```

- [ ] **Step 7: Change onUseOnBlock lambda — lines 136-143**

Replace:
```java
        PlatingEventHelper.forEachPlating(stack, player, (effect, data, s, p) -> {
            if (effect instanceof IPlatingUseOnBlockTrigger useOnBlock) {
                @Nullable InteractionResult result = useOnBlock.onUseOnBlock(data, s, p, event.getPos(), event.getLevel().getBlockState(event.getPos()));
                if (result != null) {
                    event.cancelWithResult(result);
                }
            }
        });
```
with:
```java
        PlatingEventHelper.forEachPlating(stack, player, (effect, data, s, entity) -> {
            if (effect instanceof IPlatingUseOnBlockTrigger useOnBlock) {
                @Nullable InteractionResult result = useOnBlock.onUseOnBlock(data, s, entity, event.getPos(), event.getLevel().getBlockState(event.getPos()));
                if (result != null) {
                    event.cancelWithResult(result);
                }
            }
        });
```

- [ ] **Step 8: Change onArrowShot lambda — lines 159-163**

Replace:
```java
        PlatingEventHelper.forEachPlating(held, player, (effect, data, stack, p) -> {
            if (effect instanceof IPlatingArrowShotTrigger arrowShot) {
                arrowShot.onArrowShot(data, stack, player, arrow);
            }
        });
```
with:
```java
        PlatingEventHelper.forEachPlating(held, player, (effect, data, stack, entity) -> {
            if (effect instanceof IPlatingArrowShotTrigger arrowShot) {
                arrowShot.onArrowShot(data, stack, player, arrow);
            }
        });
```

Note: `player` is still used on line 161 for `onArrowShot(data, stack, player, arrow)` because the event's owner is already a `Player` (checked at line 150). No change needed to the call itself — the lambda param `p`→`entity` rename is just for consistency, but `player` from the event is passed separately.

- [ ] **Step 9: Change onCriticalHit lambdas — lines 171-183**

Replace:
```java
        PlatingEventHelper.forEachPlating(stack, player, (effect, data, s, p) -> {
            if (effect instanceof IPlatingCritTrigger trigger) {
                trigger.onCriticalHit(data, stack, player, event);
            }
        });
```
with:
```java
        PlatingEventHelper.forEachPlating(stack, player, (effect, data, s, entity) -> {
            if (effect instanceof IPlatingCritTrigger trigger) {
                trigger.onCriticalHit(data, stack, player, event);
            }
        });
```

And:
```java
        PlatingEventHelper.forEachPlating(stack, player, (effect, data, s, p) -> {
            if (effect instanceof IPlatingCritDamageModifier modifier) {
                modifier.onCriticalHit(data, stack, player, event);
            }
        });
```
with:
```java
        PlatingEventHelper.forEachPlating(stack, player, (effect, data, s, entity) -> {
            if (effect instanceof IPlatingCritDamageModifier modifier) {
                modifier.onCriticalHit(data, stack, player, event);
            }
        });
```

Again, `player` from the event is passed to the methods, not the lambda param (which is now unused, just renamed for consistency).

- [ ] **Step 10: Change onLivingDrops lambda — lines 192-196**

Replace:
```java
        PlatingEventHelper.forEachPlatingOnEquipment(player, (effect, data, stack, p) -> {
            if (effect instanceof IPlatingKillTrigger kill) {
                kill.onKill(data, stack, player, event.getEntity(), event);
            }
        });
```
with:
```java
        PlatingEventHelper.forEachPlatingOnEquipment(player, (effect, data, stack, entity) -> {
            if (effect instanceof IPlatingKillTrigger kill) {
                kill.onKill(data, stack, player, event.getEntity(), event);
            }
        });
```

- [ ] **Step 11: Change onBlockDrops lambda — lines 204-208**

Replace:
```java
        PlatingEventHelper.forEachPlatingOnEquipment(player, (effect, data, stack, p) -> {
            if (effect instanceof IPlatingBlockDropsTrigger trigger) {
                trigger.onBlockDrops(data, stack, player, event);
            }
        });
```
with:
```java
        PlatingEventHelper.forEachPlatingOnEquipment(player, (effect, data, stack, entity) -> {
            if (effect instanceof IPlatingBlockDropsTrigger trigger) {
                trigger.onBlockDrops(data, stack, player, event);
            }
        });
```

- [ ] **Step 12: Change onEntityShieldBlock lambda — lines 275-277**

Replace:
```java
                PlatingEventHelper.forEachPlating(stack, player, (a, b, c, d) -> {
                    if (a instanceof IPlatingBlockingTrigger p) p.blocked(b, d, c, event.getDamageContainer());
                });
```
with:
```java
                PlatingEventHelper.forEachPlating(stack, player, (effect, data, s, entity) -> {
                    if (effect instanceof IPlatingBlockingTrigger blockingTrigger)
                        blockingTrigger.blocked(data, entity, s, event.getDamageContainer());
                });
```

---

### Task 9: PlatingManager — use static IDs

**Files:**
- Modify: `src/main/java/studio/fantasyit/ether_craft/plating/PlatingManager.java`

- [ ] **Step 1: Rewrite PlatingManager.init()**

Replace the `init()` method to use static `ID` fields from each effect class:

Replace from `register(EtherCraft.id("damage"), ...` to the end of `init()`, with:

```java
    public static void init() {
        register(DamagePlatingEffect.ID, new DamagePlatingEffect());
        register(DashPlatingEffect.ID, new DashPlatingEffect());
        register(HighJumpPlatingEffect.ID, new HighJumpPlatingEffect());
        register(NoGravityPlatingEffect.ID, new NoGravityPlatingEffect());
        register(CoyoteTimePlatingEffect.ID, new CoyoteTimePlatingEffect());
        register(CamouflagePlatingEffect.ID, new CamouflagePlatingEffect());
        register(BlockPlatingEffect.ID, new BlockPlatingEffect());
        register(CritPlatingEffect.ID, new CritPlatingEffect());
        register(CritDamagePlatingEffect.ID, new CritDamagePlatingEffect());
        register(HeadHuntPlatingEffect.ID, new HeadHuntPlatingEffect());
        register(TrackingPlatingEffect.ID, new TrackingPlatingEffect());
        register(BreakToInventoryPlatingEffect.ID, new BreakToInventoryPlatingEffect());
        register(KillToInventoryPlatingEffect.ID, new KillToInventoryPlatingEffect());
        register(StoneAbsorbPlatingEffect.ID, new StoneAbsorbPlatingEffect());
    }
```

Also remove unused `import studio.fantasyit.ether_craft.EtherCraft;` from PlatingManager if it was only used for `EtherCraft.id()` calls (it was). All imports should now resolve through the effect class static fields.

- [ ] **Step 2: Verify PlatingManager imports**

The file after changes should have these imports:

```java
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;
import studio.fantasyit.ether_craft.plating.effects.BlockPlatingEffect;
import studio.fantasyit.ether_craft.plating.effects.BreakToInventoryPlatingEffect;
import studio.fantasyit.ether_craft.plating.effects.CamouflagePlatingEffect;
import studio.fantasyit.ether_craft.plating.effects.CoyoteTimePlatingEffect;
import studio.fantasyit.ether_craft.plating.effects.CritDamagePlatingEffect;
import studio.fantasyit.ether_craft.plating.effects.CritPlatingEffect;
import studio.fantasyit.ether_craft.plating.effects.DamagePlatingEffect;
import studio.fantasyit.ether_craft.plating.effects.DashPlatingEffect;
import studio.fantasyit.ether_craft.plating.effects.HeadHuntPlatingEffect;
import studio.fantasyit.ether_craft.plating.effects.HighJumpPlatingEffect;
import studio.fantasyit.ether_craft.plating.effects.IPlatingEffect;
import studio.fantasyit.ether_craft.plating.effects.KillToInventoryPlatingEffect;
import studio.fantasyit.ether_craft.plating.effects.NoGravityPlatingEffect;
import studio.fantasyit.ether_craft.plating.effects.StoneAbsorbPlatingEffect;
import studio.fantasyit.ether_craft.plating.effects.TrackingPlatingEffect;
```

(remove `import studio.fantasyit.ether_craft.EtherCraft;`)

---

### Task 10: Build and verify

- [ ] **Step 1: Build the project**

Run: `idea_build_project` with `rebuild: true`

Expected: No compile errors.

- [ ] **Step 2: Fix any remaining compile errors**

Check the build output. Common things to watch:
- Missing `import net.minecraft.resources.Identifier;` in effect classes
- Missing `import studio.fantasyit.ether_craft.EtherCraft;` in effect classes
- Missing `import net.minecraft.world.entity.player.Player;` where `(Player)` cast is used
- `EventHandler` lines 161, 273, etc. where `player` from event context may need type adjustment

Fix any errors, rebuild, repeat until clean.
