# Ether Stream Label Capability Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a label capability to ether streams that renders 3D text, orthogonal to movement direction, right-aligned and clipped at start position.

**Architecture:** New `EtherStreamLabelCapability` (IStreamCapability) stores Component + Vec3. `EtherStreamEntityRenderer` reads the cap into the render state, then renders text to an offscreen framebuffer and draws a quad with clipped UVs in world space.

**Tech Stack:** Minecraft NeoForge 26.1, Java 25, Font.drawInBatch, RenderTarget, PoseStack, SubmitNodeCollector

---

### Task 1: Create EtherStreamLabelCapability

**Files:**
- Create: `src/main/java/studio/fantasyit/ether_craft/stream/EtherStreamLabelCapability.java`

- [ ] **Step 1: Write the class**

```java
package studio.fantasyit.ether_craft.stream;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.entity.EtherStreamEntity;

public class EtherStreamLabelCapability implements IStreamCapability {
    public static final Identifier ID = EtherCraft.id("label");

    @Nullable
    private Component label;
    @Nullable
    private Vec3 startPos;
    private int color = 0xFFFFFFFF;

    @Nullable
    public Component getLabel() {
        return label;
    }

    public void setLabel(@Nullable Component label) {
        this.label = label;
    }

    @Nullable
    public Vec3 getStartPos() {
        return startPos;
    }

    public void setStartPos(@Nullable Vec3 startPos) {
        this.startPos = startPos;
    }

    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        this.color = color;
    }

    @Override
    public Identifier getId() {
        return ID;
    }

    @Override
    public int getConsumption() {
        return 0;
    }

    @Override
    public void tick(EtherStreamEntity streamEntity) {
    }

    @Override
    public boolean hitEntity(ServerLevel level, EtherStreamEntity streamEntity, EntityHitResult hit, Entity entity) {
        return false;
    }

    @Override
    public boolean hitBlock(ServerLevel level, EtherStreamEntity streamEntity, BlockHitResult hit, BlockState blockState) {
        return false;
    }

    @Override
    public void onDestroy(EtherStreamEntity streamEntity) {
    }

    @Override
    public void serialize(ValueOutput output) {
        if (label != null) {
            output.store("label", ComponentSerialization.CODEC, label);
        }
        if (startPos != null) {
            output.putDouble("startX", startPos.x);
            output.putDouble("startY", startPos.y);
            output.putDouble("startZ", startPos.z);
        }
        output.putInt("color", color);
    }

    @Override
    public void deserialize(ValueInput input) {
        this.label = input.read("label", ComponentSerialization.CODEC).orElse(null);
        double sx = input.getDoubleOr("startX", Double.NaN);
        double sy = input.getDoubleOr("startY", Double.NaN);
        double sz = input.getDoubleOr("startZ", Double.NaN);
        if (!Double.isNaN(sx) && !Double.isNaN(sy) && !Double.isNaN(sz)) {
            this.startPos = new Vec3(sx, sy, sz);
        } else {
            this.startPos = null;
        }
        this.color = input.getIntOr("color", 0xFFFFFFFF);
    }
}
```

- [ ] **Step 2: Build**

Run: `idea_build_project` with `filesToRebuild=["src/main/java/studio/fantasyit/ether_craft/stream/EtherStreamLabelCapability.java"]`

- [ ] **Step 3: Commit**

```bash
git add src/main/java/studio/fantasyit/ether_craft/stream/EtherStreamLabelCapability.java
git commit -m "feat: add EtherStreamLabelCapability"
```

---

### Task 2: Extend EtherStreamEntityRenderState

**Files:**
- Modify: `src/main/java/studio/fantasyit/ether_craft/entity/render/EtherStreamEntityRenderState.java`

- [ ] **Step 1: Add label fields**

Current file:
```java
package studio.fantasyit.ether_craft.entity.render;

import net.minecraft.client.renderer.entity.state.EntityRenderState;
import studio.fantasyit.ether_craft.entity.EtherStreamEntity;

public class EtherStreamEntityRenderState extends EntityRenderState {
    public final double[] tailX;
    public final double[] tailY;
    public final double[] tailZ;
    public final float[] tailSize;
    public int tailCount;

    public EtherStreamEntityRenderState() {
        tailX = new double[EtherStreamEntity.MAX_TAIL];
        tailY = new double[EtherStreamEntity.MAX_TAIL];
        tailZ = new double[EtherStreamEntity.MAX_TAIL];
        tailSize = new float[EtherStreamEntity.MAX_TAIL];
    }
}
```

Replace with:
```java
package studio.fantasyit.ether_craft.entity.render;

import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import studio.fantasyit.ether_craft.entity.EtherStreamEntity;

public class EtherStreamEntityRenderState extends EntityRenderState {
    public final double[] tailX;
    public final double[] tailY;
    public final double[] tailZ;
    public final float[] tailSize;
    public int tailCount;

    @Nullable
    public Component label;
    @Nullable
    public Vec3 startPos;
    public Vec3 motion = Vec3.ZERO;
    public int labelColor = 0xFFFFFFFF;

    public EtherStreamEntityRenderState() {
        tailX = new double[EtherStreamEntity.MAX_TAIL];
        tailY = new double[EtherStreamEntity.MAX_TAIL];
        tailZ = new double[EtherStreamEntity.MAX_TAIL];
        tailSize = new float[EtherStreamEntity.MAX_TAIL];
    }
}
```

- [ ] **Step 2: Build**

Run: `idea_build_project` with `filesToRebuild=["src/main/java/studio/fantasyit/ether_craft/entity/render/EtherStreamEntityRenderState.java"]`

- [ ] **Step 3: Commit**

```bash
git add src/main/java/studio/fantasyit/ether_craft/entity/render/EtherStreamEntityRenderState.java
git commit -m "feat: add label fields to EtherStreamEntityRenderState"
```

---

### Task 3: Add label extraction to extractRenderState

**Files:**
- Modify: `src/main/java/studio/fantasyit/ether_craft/entity/render/EtherStreamEntityRenderer.java` (extractRenderState method)

- [ ] **Step 1: Add import and extend extractRenderState**

Add import at top:
```java
import studio.fantasyit.ether_craft.stream.EtherStreamLabelCapability;
```

Modify `extractRenderState` — after the tail extraction loop, add label extraction:

```java
@Override
public void extractRenderState(EtherStreamEntity entity, EtherStreamEntityRenderState state, float partialTicks) {
    super.extractRenderState(entity, state, partialTicks);
    state.tailCount = entity.tailCount;
    for (int i = 0; i < entity.tailCount; i++) {
        int idx = (entity.tailHead - i + EtherStreamEntity.MAX_TAIL) % EtherStreamEntity.MAX_TAIL;
        state.tailX[i] = entity.tailX[idx];
        state.tailY[i] = entity.tailY[idx];
        state.tailZ[i] = entity.tailZ[idx];
        state.tailSize[i] = entity.tailSize[idx];
    }
    // --- Label extraction ---
    entity.getCapability(EtherStreamLabelCapability.ID).ifPresent(cap -> {
        if (cap instanceof EtherStreamLabelCapability labelCap) {
            state.label = labelCap.getLabel();
            state.startPos = labelCap.getStartPos();
            state.labelColor = labelCap.getColor();
        }
    });
    state.motion = entity.getDeltaMovement();
    // --- End label extraction ---
}
```

- [ ] **Step 2: Build**

Run: `idea_build_project` with `filesToRebuild=["src/main/java/studio/fantasyit/ether_craft/entity/render/EtherStreamEntityRenderer.java"]`

- [ ] **Step 3: Commit**

```bash
git add src/main/java/studio/fantasyit/ether_craft/entity/render/EtherStreamEntityRenderer.java
git commit -m "feat: extract label cap data in extractRenderState"
```

---

### Task 4: Add label rendering logic

**Files:**
- Modify: `src/main/java/studio/fantasyit/ether_craft/entity/render/EtherStreamEntityRenderer.java` (add renderLabel + helpers after submit)
- Modify: `src/main/java/studio/fantasyit/ether_craft/entity/render/EtherStreamRenderPipeline.java` (register label pipeline)

- [ ] **Step 1: Register label pipeline in EtherStreamRenderPipeline.java**

Add field and registration in `EtherStreamRenderPipeline.java`:

Add before the `register` method:
```java
public static final RenderPipeline LABEL_QUAD_PIPELINE = RenderPipeline.builder(RenderPipelines.POSITION_TEX_SNIPPET)
        .withLocation(EtherCraft.id("pipeline/label_quad"))
        .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
        .withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, false))
        .withCull(false)
        .build();
```

Add in `register()` method:
```java
event.registerPipeline(LABEL_QUAD_PIPELINE);
```

Add missing imports for `BlendFunction`, `ColorTargetState`, `CompareOp`, `DepthStencilState`.

- [ ] **Step 2: Add imports to EtherStreamEntityRenderer.java**

Add these imports at top (after existing imports):
```java
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.Tesselator;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import studio.fantasyit.ether_craft.stream.EtherStreamLabelCapability;
```

- [ ] **Step 3: Add label rendering fields and methods to renderer class**

Add fields inside the class body:
```java
private static final float LABEL_SCALE = 0.010416667F;

private com.mojang.blaze3d.pipeline.RenderTarget labelFb;
private int lastLabelHash;
private int lastLineHeight;
```

Add `renderLabel` call at end of `submit()` method, before `super.submit(...)`:
```java
renderLabel(state, poseStack, collector, camera, Minecraft.getInstance().font);
```

- [ ] **Step 4: Add renderLabel method**

Add after `submit()` method:
```java
private void renderLabel(EtherStreamEntityRenderState state, PoseStack poseStack,
                         SubmitNodeCollector collector, CameraRenderState camera, Font font) {
    if (state.label == null || state.startPos == null) return;
    Vec3 motion = state.motion;
    if (motion.lengthSqr() < 0.0001) return;

    int textWidth = font.width(state.label);
    if (textWidth == 0) return;

    // Clipping: compute ratio of text that must be hidden
    double worldDist = state.startPos.distanceTo(new Vec3(state.x, state.y, state.z));
    float fontUnitsAvail = (float) (worldDist / LABEL_SCALE);
    float clipRatio = 1f - fontUnitsAvail / textWidth;
    if (clipRatio < 0f) clipRatio = 0f;
    if (clipRatio >= 1f) return;

    // 1. Render full text to offscreen framebuffer
    updateLabelFramebuffer(font, state.label, textWidth, state.labelColor);
    if (labelFb == null) return;

    // 2. Set up PoseStack: position at entity, rotate to orthogonal-of-motion plane
    poseStack.pushPose();

    Vec3 dir = motion.normalize();
    Vec3 up = new Vec3(0.0, 1.0, 0.0);
    Vec3 normal;
    if (Math.abs(dir.dot(up)) > 0.999) {
        normal = dir.cross(new Vec3(1.0, 0.0, 0.0)).normalize();
    } else {
        normal = dir.cross(up).normalize();
    }
    Quaternionf rotation = new Quaternionf().rotateTo(
            new org.joml.Vector3f(0, 0, 1),
            new org.joml.Vector3f((float) normal.x, (float) normal.y, (float) normal.z));
    poseStack.mulPose(rotation);
    poseStack.scale(LABEL_SCALE, -LABEL_SCALE, LABEL_SCALE);

    // 3. Draw quad with clipped UV using Tesselator (immediate, not deferred)
    RenderSystem.enableBlend();
    RenderSystem.defaultBlendFunc();
    RenderSystem.setShaderTexture(0, labelFb.getColorTextureId());

    float quadLeft = -textWidth * (1f - clipRatio);
    float quadRight = 0;
    float quadBottom = 0;
    float quadTop = font.lineHeight;
    float u0 = clipRatio;
    float u1 = 1f;

    Matrix4f poseMatrix = poseStack.last().pose();
    BufferBuilder buffer = Tesselator.getInstance()
            .begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
    buffer.addVertex(poseMatrix, quadLeft, quadBottom, 0).setUv(u0, 1f);
    buffer.addVertex(poseMatrix, quadRight, quadBottom, 0).setUv(u1, 1f);
    buffer.addVertex(poseMatrix, quadRight, quadTop, 0).setUv(u1, 0f);
    buffer.addVertex(poseMatrix, quadLeft, quadTop, 0).setUv(u0, 0f);
    BufferUploader.drawWithShader(buffer.buildOrThrow());

    RenderSystem.disableBlend();
    poseStack.popPose();
}
```

- [ ] **Step 5: Add framebuffer helper methods**

Add after `renderLabel`:
```java
private void updateLabelFramebuffer(Font font, net.minecraft.network.chat.Component label,
                                     int textWidth, int color) {
    int lineHeight = font.lineHeight;
    int hash = label.hashCode();
    if (labelFb != null && hash == lastLabelHash && lineHeight == lastLineHeight) {
        renderLabelToFb(font, label, color);
        return;
    }
    if (labelFb != null) {
        labelFb.destroyBuffers();
    }
    labelFb = new com.mojang.blaze3d.pipeline.RenderTarget(textWidth, lineHeight, true, Minecraft.ON_OSX);
    lastLabelHash = hash;
    lastLineHeight = lineHeight;
    renderLabelToFb(font, label, color);
}

private void renderLabelToFb(Font font, net.minecraft.network.chat.Component label, int color) {
    if (labelFb == null) return;
    labelFb.bindWrite(true);
    labelFb.clear(Minecraft.ON_OSX);
    Matrix4f orthoMatrix = new Matrix4f().setOrtho(
            0, (float) labelFb.width, (float) labelFb.height, 0, -1, 1);
    MultiBufferSource.BufferSource buf = MultiBufferSource.immediate(new ByteBufferBuilder(256));
    font.drawInBatch(label, 0, 0, color, false, orthoMatrix,
            buf, Font.DisplayMode.NORMAL, 0, LightTexture.FULL_BRIGHT);
    buf.endBatch();
    labelFb.unbindWrite();
}
```

- [ ] **Step 6: Build**

Run partial build first (renderer):
```java
// Verify BufferBuilder + BufferUploader usage correct per MC 26.1 API
```
Run: `idea_build_project` with `filesToRebuild=["src/main/java/studio/fantasyit/ether_craft/entity/render/EtherStreamRenderPipeline.java","src/main/java/studio/fantasyit/ether_craft/entity/render/EtherStreamEntityRenderer.java"]`

Fix any compile errors reported.

- [ ] **Step 7: Commit**

```bash
git add src/main/java/studio/fantasyit/ether_craft/entity/render/EtherStreamEntityRenderer.java src/main/java/studio/fantasyit/ether_craft/entity/render/EtherStreamRenderPipeline.java
git commit -m "feat: add label rendering with offscreen framebuffer + UV clipped quad"
```

- [ ] **Step 2: Build**

Run: `idea_build_project` with `filesToRebuild=["src/main/java/studio/fantasyit/ether_craft/entity/render/EtherStreamEntityRenderer.java"]`

- [ ] **Step 3: Commit**

```bash
git add src/main/java/studio/fantasyit/ether_craft/entity/render/EtherStreamEntityRenderer.java
git commit -m "feat: add label rendering with offscreen framebuffer + UV clipping"
```

---

### Task 5: Full build and verify

- [ ] **Step 1: Full project build**

Run: `idea_build_project` with `rebuild=true`, `timeout=120000`

Expected: BUILD SUCCESSFUL, no compile errors.

- [ ] **Step 2: Verify render pipeline registration** (optional, manual)

Check that the new `LABEL_QUAD_PIPELINE` is registered by `EtherStreamRenderPipeline.register()` — this pipeline does NOT need separate registration because it's defined as a static field in the renderer class that extends the ETHER_STREAM_ENTITY_PIPELINE snippet. The pipeline system handles the registration automatically via its `.build()` call.

- [ ] **Step 3: Commit** (if any fix was needed)

```bash
git commit -m "build: full build verification after label rendering implementation"
```
