# Cocaine Item — Design Spec

**Date:** 2026-05-17  
**Namespace:** `quantcraft`  
**MC version:** 1.20.4 / Fabric

---

## 1. Item Registration

**Class:** `com.quantcraft.item.CocaineItem` extends `Item`

Settings:
- `FoodComponent.Builder().alwaysEdible().hunger(0).saturationModifier(0f).build()`
- `maxCount(16)`
- `useAction` → `UseAction.EAT`, `maxUseTime` → 32 ticks

`ModItems` additions:
- `public static final Item COCAINE = new CocaineItem(...)`
- `reg("cocaine", COCAINE)` in `register()`

`ModEffects` (new class `com.quantcraft.registry.ModEffects`) registers `COCAINE_HIGH` and is called from `QuantCraftMod.onInitialize()` before `ModItems.register()`.

**Assets needed:**
- `assets/quantcraft/textures/item/cocaine.png` — 16×16: white powder heap on green dollar-bill background
- `assets/quantcraft/models/item/cocaine.json` — standard generated item model
- `assets/quantcraft/lang/en_us.json` — add `"item.quantcraft.cocaine": "Cocaine"`

---

## 2. Status Effect: `cocaine_high`

**Class:** `com.quantcraft.item.CocaineHighEffect` extends `StatusEffect`
- Type: `StatusEffectType.BENEFICIAL`
- Color: `0xE8A000` (amber)

**`onRemoved(LivingEntity entity)`** applies crash effects (all 2400 ticks = 2 real minutes, server-side):

| Effect           | Amplifier | Level in-game |
|------------------|-----------|---------------|
| `slowness`       | 1         | II            |
| `weakness`       | 0         | I             |
| `mining_fatigue` | 0         | I             |
| `nausea`         | 0         | I             |
| `hunger`         | 0         | I             |

Also sets NBT tag `CocaineCrash:1b` on `entity.getCustomData()` so the milk mixin can detect an active crash.

---

## 3. `CocaineItem.finishUsing()`

Applies to the player (all 1200 ticks = 1 real minute):

| Effect         | Amplifier | Level in-game |
|----------------|-----------|---------------|
| `cocaine_high` | 0         | I             |
| `speed`        | 1         | II            |
| `strength`     | 0         | I             |
| `haste`        | 1         | II            |
| `jump_boost`   | 1         | II            |
| `regeneration` | 0         | I             |
| `night_vision` | 0         | I             |

Returns `stack.finishUsing(world, user)`.

---

## 4. Recipe

**File:** `data/quantcraft/recipes/cocaine.json`

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

---

## 5. MilkMixin

**Class:** `com.quantcraft.mixin.MilkMixin`  
`@Mixin(MilkBucketItem.class)`

### Logic

**HEAD injector** (`finishUsing` at `HEAD`):
- Check if entity is a `ServerPlayerEntity`
- Check `player.getCustomData().getBoolean("CocaineCrash")`
- If true: iterate `player.getStatusEffects()` and snapshot any effect whose type is in the crash set (slowness, weakness, mining_fatigue, nausea, hunger)
- Store snapshot in a `@Unique private final ThreadLocal<List<StatusEffectInstance>>` field

**RETURN injector** (`finishUsing` at `RETURN`):
- If snapshot list is non-empty: re-apply each `StatusEffectInstance` with its original remaining duration
- Send actionbar message: `"§c...unfortunately, that doesn't work here. §o(Trust us, we tried)"`
- Clear the ThreadLocal

**`quantcraft.mixins.json`** — add `"MilkMixin"` to the `"mixins"` array.

### NBT tag lifecycle
- `CocaineCrash` is set to `1b` when `CocaineHighEffect.onRemoved()` fires (crash starts)
- Not actively cleared after crash wears off — the mixin re-checks actual active effects before snapshotting, so a stale tag with no live crash effects is a no-op
