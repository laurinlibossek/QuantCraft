# Cocaine Item Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a `quantcraft:cocaine` food item with a `cocaine_high` status effect carrier, a 2-minute crash on removal, a shapeless crafting recipe, and a Mixin that prevents milk from curing the crash.

**Architecture:** `CocaineHighEffect` is a custom `StatusEffect` whose `onRemoved` fires the crash and tags the player NBT. `CocaineItem.finishUsing` applies buffs and the carrier effect. `MilkMixin` injects into `MilkBucketItem.finishUsing` to snapshot and reapply crash effects that milk would otherwise clear.

**Tech Stack:** Fabric MC 1.20.4, Java 17, Mixin, JUnit 5 (unit tests cover pure logic only — Minecraft class instantiation is not feasible in unit tests; Mixin and item behavior verified by in-game smoke test checklist)

---

## File Map

| Action | Path | Responsibility |
|--------|------|----------------|
| Create | `src/main/java/com/quantcraft/item/CocaineHighEffect.java` | Custom StatusEffect; crash logic in `onRemoved` |
| Create | `src/main/java/com/quantcraft/item/CocaineItem.java` | Food item; applies buffs + carrier effect in `finishUsing` |
| Create | `src/main/java/com/quantcraft/registry/ModEffects.java` | Registers `COCAINE_HIGH` effect |
| Create | `src/main/java/com/quantcraft/mixin/MilkMixin.java` | Prevents milk from clearing crash effects |
| Modify | `src/main/java/com/quantcraft/registry/ModItems.java` | Add `COCAINE` constant + `reg()` call |
| Modify | `src/main/java/com/quantcraft/registry/ModItemGroups.java` | Add cocaine to creative tab |
| Modify | `src/main/java/com/quantcraft/QuantCraftMod.java` | Call `ModEffects.register()` before `ModItems.register()` |
| Modify | `src/main/resources/quantcraft.mixins.json` | Add `MilkMixin` to `"mixins"` array |
| Create | `src/main/resources/assets/quantcraft/models/item/cocaine.json` | Standard generated item model |
| Create | `src/main/resources/assets/quantcraft/textures/item/cocaine.png` | 16x16 texture |
| Modify | `src/main/resources/assets/quantcraft/lang/en_us.json` | Add display name |
| Create | `src/main/resources/data/quantcraft/recipes/cocaine.json` | Shapeless recipe |

---

## Task 1: `CocaineHighEffect` — status effect with crash logic

**Files:**
- Create: `src/main/java/com/quantcraft/item/CocaineHighEffect.java`

- [ ] **Step 1: Create `CocaineHighEffect.java`**

```java
package com.quantcraft.item;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.AttributeContainer;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffectType;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;

public class CocaineHighEffect extends StatusEffect {

    public CocaineHighEffect() {
        super(StatusEffectType.BENEFICIAL, 0xE8A000);
    }

    @Override
    public void onRemoved(LivingEntity entity, AttributeContainer attributes, int amplifier) {
        entity.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS,       2400, 1, false, true, true));
        entity.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS,       2400, 0, false, true, true));
        entity.addStatusEffect(new StatusEffectInstance(StatusEffects.MINING_FATIGUE, 2400, 0, false, true, true));
        entity.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA,         2400, 0, false, true, true));
        entity.addStatusEffect(new StatusEffectInstance(StatusEffects.HUNGER,         2400, 0, false, true, true));

        if (entity instanceof PlayerEntity player) {
            player.getCustomData().putBoolean("CocaineCrash", true);
        }
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add src/main/java/com/quantcraft/item/CocaineHighEffect.java
git commit -m "Add CocaineHighEffect with crash logic on removal"
```

---

## Task 2: `ModEffects` — register the effect

**Files:**
- Create: `src/main/java/com/quantcraft/registry/ModEffects.java`
- Modify: `src/main/java/com/quantcraft/QuantCraftMod.java` (line ~33–36)

- [ ] **Step 1: Create `ModEffects.java`**

```java
package com.quantcraft.registry;

import com.quantcraft.QuantCraftMod;
import com.quantcraft.item.CocaineHighEffect;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModEffects {
    public static final StatusEffect COCAINE_HIGH = new CocaineHighEffect();

    public static void register() {
        Registry.register(Registries.STATUS_EFFECT,
                new Identifier(QuantCraftMod.MOD_ID, "cocaine_high"),
                COCAINE_HIGH);
    }
}
```

- [ ] **Step 2: Call `ModEffects.register()` in `QuantCraftMod.onInitialize()`**

In `src/main/java/com/quantcraft/QuantCraftMod.java`, the current order in `onInitialize()` is:
```java
QuantCraftConfig.load();
StockRegistry.initialize();
ModItems.register();
```

Change it to:
```java
QuantCraftConfig.load();
StockRegistry.initialize();
ModEffects.register();
ModItems.register();
```

Add the import at the top of the file:
```java
import com.quantcraft.registry.ModEffects;
```

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/quantcraft/registry/ModEffects.java \
        src/main/java/com/quantcraft/QuantCraftMod.java
git commit -m "Register cocaine_high status effect"
```

---

## Task 3: `CocaineItem` — food item with buff application

**Files:**
- Create: `src/main/java/com/quantcraft/item/CocaineItem.java`

- [ ] **Step 1: Create `CocaineItem.java`**

```java
package com.quantcraft.item;

import com.quantcraft.registry.ModEffects;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.FoodComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

public class CocaineItem extends Item {

    public CocaineItem() {
        super(new Settings()
                .maxCount(16)
                .food(new FoodComponent.Builder()
                        .alwaysEdible()
                        .hunger(0)
                        .saturationModifier(0f)
                        .build()));
    }

    @Override
    public ItemStack finishUsing(ItemStack stack, World world, LivingEntity user) {
        if (!world.isClient) {
            user.addStatusEffect(new StatusEffectInstance(ModEffects.COCAINE_HIGH,    1200, 0, false, true, true));
            user.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED,        1200, 1, false, true, true));
            user.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH,     1200, 0, false, true, true));
            user.addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE,        1200, 1, false, true, true));
            user.addStatusEffect(new StatusEffectInstance(StatusEffects.JUMP_BOOST,   1200, 1, false, true, true));
            user.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 1200, 0, false, true, true));
            user.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, 1200, 0, false, true, true));
        }
        return super.finishUsing(stack, world, user);
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add src/main/java/com/quantcraft/item/CocaineItem.java
git commit -m "Add CocaineItem food item with 1-minute buffs"
```

---

## Task 4: Register item in `ModItems` and creative tab

**Files:**
- Modify: `src/main/java/com/quantcraft/registry/ModItems.java`
- Modify: `src/main/java/com/quantcraft/registry/ModItemGroups.java`

- [ ] **Step 1: Add to `ModItems.java`**

Add import at the top:
```java
import com.quantcraft.item.CocaineItem;
```

Add constant after the `NP_EMERALD_CARTEL` declaration:
```java
public static final Item COCAINE = new CocaineItem();
```

Add inside `register()` after the last `reg(...)` call:
```java
reg("cocaine", COCAINE);
```

- [ ] **Step 2: Add to creative tab in `ModItemGroups.java`**

Inside the `entries` lambda, after `entries.add(ModItems.NP_EMERALD_CARTEL);`, add:
```java
entries.add(ModItems.COCAINE);
```

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/quantcraft/registry/ModItems.java \
        src/main/java/com/quantcraft/registry/ModItemGroups.java
git commit -m "Register cocaine item and add to QuantCraft creative tab"
```

---

## Task 5: Assets — model, texture, lang

**Files:**
- Create: `src/main/resources/assets/quantcraft/models/item/cocaine.json`
- Create: `src/main/resources/assets/quantcraft/textures/item/cocaine.png`
- Modify: `src/main/resources/assets/quantcraft/lang/en_us.json`

- [ ] **Step 1: Create item model JSON at `src/main/resources/assets/quantcraft/models/item/cocaine.json`**

```json
{
  "parent": "minecraft:item/generated",
  "textures": {
    "layer0": "quantcraft:item/cocaine"
  }
}
```

- [ ] **Step 2: Create texture**

Paint a 16×16 PNG at `src/main/resources/assets/quantcraft/textures/item/cocaine.png`.

Pixel design:
- Background: green `#2D6A2D` (dollar-bill color) filling the entire 16×16
- Thin border: `#1A3D1A` on the outer 1-pixel edge
- White powder heap: scatter `#FFFFFF` and `#E8E8E8` pixels in a rough oval, roughly 8×5 px centered at (8, 10) — concentrated in the middle, sparse at edges

Any 16×16 PNG works for the build; refine the art later if needed.

- [ ] **Step 3: Add lang entry to `src/main/resources/assets/quantcraft/lang/en_us.json`**

The current last item entry is `"itemGroup.quantcraft": "QuantCraft"`. Add before the closing `}`:
```json
"item.quantcraft.cocaine": "Cocaine"
```
Make sure the previous line has a trailing comma. Final lines should look like:
```json
  "itemGroup.quantcraft":         "QuantCraft",
  "item.quantcraft.cocaine":      "Cocaine"
}
```

- [ ] **Step 4: Commit**

```bash
git add src/main/resources/assets/quantcraft/models/item/cocaine.json \
        src/main/resources/assets/quantcraft/textures/item/cocaine.png \
        src/main/resources/assets/quantcraft/lang/en_us.json
git commit -m "Add cocaine item model, texture, and lang entry"
```

---

## Task 6: Crafting recipe

**Files:**
- Create: `src/main/resources/data/quantcraft/recipes/cocaine.json`

- [ ] **Step 1: Create `src/main/resources/data/quantcraft/recipes/cocaine.json`**

```json
{
  "type": "minecraft:crafting_shapeless",
  "ingredients": [
    {"item": "quantcraft:dollar_bill"},
    {"item": "quantcraft:dollar_bill"},
    {"item": "quantcraft:dollar_bill"},
    {"item": "quantcraft:dollar_bill"},
    {"item": "quantcraft:dollar_bill"},
    {"item": "minecraft:sugar"},
    {"item": "minecraft:sugar"},
    {"item": "minecraft:sugar"},
    {"item": "minecraft:sugar"}
  ],
  "result": {"item": "quantcraft:cocaine", "count": 4}
}
```

- [ ] **Step 2: Commit**

```bash
git add src/main/resources/data/quantcraft/recipes/cocaine.json
git commit -m "Add cocaine shapeless recipe (5x dollar_bill + 4x sugar → 4x cocaine)"
```

---

## Task 7: `MilkMixin` — prevent milk curing the crash

**Files:**
- Create: `src/main/java/com/quantcraft/mixin/MilkMixin.java` (new package directory)
- Modify: `src/main/resources/quantcraft.mixins.json`

- [ ] **Step 1: Create `src/main/java/com/quantcraft/mixin/MilkMixin.java`**

```java
package com.quantcraft.mixin;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.MilkBucketItem;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Mixin(MilkBucketItem.class)
public class MilkMixin {

    @Unique
    private static final ThreadLocal<List<StatusEffectInstance>> CRASH_SNAPSHOT =
            ThreadLocal.withInitial(ArrayList::new);

    @Unique
    private static final Set<StatusEffect> CRASH_EFFECTS = Set.of(
            StatusEffects.SLOWNESS,
            StatusEffects.WEAKNESS,
            StatusEffects.MINING_FATIGUE,
            StatusEffects.NAUSEA,
            StatusEffects.HUNGER
    );

    @Inject(method = "finishUsing", at = @At("HEAD"))
    private void snapshotCrashEffects(ItemStack stack, World world, LivingEntity user,
                                      CallbackInfoReturnable<ItemStack> cir) {
        if (world.isClient) return;
        if (!(user instanceof PlayerEntity player)) return;
        if (!player.getCustomData().getBoolean("CocaineCrash")) return;

        List<StatusEffectInstance> snapshot = CRASH_SNAPSHOT.get();
        snapshot.clear();
        for (StatusEffectInstance instance : player.getStatusEffects()) {
            if (CRASH_EFFECTS.contains(instance.getEffectType())) {
                snapshot.add(new StatusEffectInstance(
                        instance.getEffectType(),
                        instance.getDuration(),
                        instance.getAmplifier(),
                        instance.isAmbient(),
                        instance.shouldShowParticles(),
                        instance.shouldShowIcon()
                ));
            }
        }
    }

    @Inject(method = "finishUsing", at = @At("RETURN"))
    private void reapplyCrashEffects(ItemStack stack, World world, LivingEntity user,
                                     CallbackInfoReturnable<ItemStack> cir) {
        if (world.isClient) return;
        if (!(user instanceof ServerPlayerEntity sp)) return;

        List<StatusEffectInstance> snapshot = CRASH_SNAPSHOT.get();
        if (snapshot.isEmpty()) return;

        for (StatusEffectInstance instance : snapshot) {
            sp.addStatusEffect(instance);
        }
        snapshot.clear();

        sp.sendMessage(Text.literal("§c...unfortunately, that doesn't work here. §o(Trust us, we tried)"), true);
    }
}
```

- [ ] **Step 2: Update `src/main/resources/quantcraft.mixins.json`**

Change `"mixins": []` to:
```json
"mixins": ["MilkMixin"],
```

Full file after the change:
```json
{
  "required": true,
  "package": "com.quantcraft.mixin",
  "compatibilityLevel": "JAVA_17",
  "mixins": ["MilkMixin"],
  "client": [],
  "injectors": { "defaultRequire": 1 }
}
```

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/quantcraft/mixin/MilkMixin.java \
        src/main/resources/quantcraft.mixins.json
git commit -m "Add MilkMixin: reapply crash effects after drinking milk"
```

---

## Task 8: Build and smoke test

- [ ] **Step 1: Build the mod**

```bash
cd /Users/laurinlibossek/coding/mc/QuantCraft
./gradlew build
```
Expected: `BUILD SUCCESSFUL`. Fix any compilation errors before continuing.

- [ ] **Step 2: Run unit tests**

```bash
./gradlew test
```
Expected: all existing tests pass (no new unit tests — cocaine logic depends on Minecraft classes).

- [ ] **Step 3: In-game smoke test**

Load the mod (`./gradlew runClient` or copy jar to a modded instance) and verify:

1. **Creative tab:** `Cocaine` item appears in the QuantCraft tab
2. **Recipe:** Crafting table — 5× dollar_bill + 4× sugar (any arrangement) → 4× cocaine
3. **Use cocaine:** Right-click cocaine → 32-tick eat animation → on finish: Speed II, Strength I, Haste II, Jump Boost II, Regen I, Night Vision I, and `cocaine_high` all appear in effects HUD
4. **Buff duration:** Effects last ~60 seconds real time, then disappear
5. **Crash fires:** Immediately after `cocaine_high` expires, crash appears: Slowness II, Weakness I, Mining Fatigue I, Nausea, Hunger I
6. **Crash duration:** Crash effects last ~120 seconds real time
7. **Milk during crash:** Drink milk while crash is active → effects reapply instantly, actionbar shows `...unfortunately, that doesn't work here. (Trust us, we tried)`
8. **Milk outside crash:** Drink milk with no cocaine involved → milk clears effects normally, no actionbar message

- [ ] **Step 4: Commit any smoke-test fixes**

```bash
git add -p
git commit -m "Fix: <describe what was wrong>"
```
