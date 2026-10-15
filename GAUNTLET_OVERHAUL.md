# Infinity Gauntlet Overhaul

## Status: implemented (uncommitted, not yet play-tested)

All phases below are implemented. Where the build differs from the plan:

- **Time Surge stays.** Time has five abilities (Chronostasis, Temporal Dilation, Time Field, Time Surge, Rewind) instead of Surge being replaced by Rewind.
- **Rate limits.** Anthology clamps time rates to 5%–300%, so Temporal Dilation and Time Field use that range instead of 0–10x / 0.1–20x. Fast Time Fields (over 100%) also run the Surge growth/machine acceleration inside the field.
- **Gravity Field.** Anthology only supports up/down gravity, so the field inverts gravity instead of picking any direction. Crouch flips your own gravity.
- **Mind Sight** moved to the Soul stone as **Soul Sight** (Soul IV). It draws through-wall outline boxes for creatures and ores rather than a glow shader.
- **Icons.** There is no new art; new abilities reuse the Infinity Gauntlet icons, flipped so each ability looks different.
- **Feedback** uses particles, sounds, arm swings and Anthology camera shake. There is no FOV punch.
- **Overcharge** has no crouch variant.
- **Soul World** was cut after play-testing.
- **Gravity Field** applies Anthology's Inverted effect so effect immunities apply.
- **Time Field** pins every entity inside its radius to the chosen rate, on top of Anthology's falloff field, because the falloff alone barely slowed anything past the centre.

## Part 1: Audit of the new abilities (uncommitted work)

### Bugs that affect both new abilities (adding a 4th ability slot)

| # | Severity | Where | Problem |
|---|---|---|---|
| 1 | **Crash** | `InfinityGauntletTooltipEvents.java:15,37` | `ABILITY_COLORS` has 3 entries. The loop now runs to 4 for reality/time, so `ABILITY_COLORS[3]` throws `ArrayIndexOutOfBoundsException` when you hover a Reality or Time stone. |
| 2 | UI | `GauntletSelectionToast.java:33-42` | The `default` branch makes ability 4 show as "III" in orange. |
| 3 | UI | `GauntletHudOverlay.java:28-36` | `ABILITY_ICONS` has 3 icons for reality/time, so the clamp shows ability 3's icon for ability 4. Time already reuses icon 13 for both abilities 2 and 3. |
| 4 | Fragile | Several files | Ability counts are hardcoded in 4 places: `abilityCount`, the tooltip, the toast, and the HUD. That's why 1–3 slipped through. (Part 2 has a fix.) |

### Time Surge (`TimeSurgeManager`)

| # | Problem | Detail |
|---|---|---|
| T1 | **Barely does anything** | By default vanilla gives 3 random ticks per 16³ section per tick, which is about 1 in 1,365 blocks per tick. Surge adds `speed*3 = 9` samples per player spread over a 65×17×65 box (about 71.8k blocks), which is about 1 in 7,980. That's only **~17% faster**, so crops look unchanged. |
| T2 | Sky jitters | `setDayTime` runs on the server, but clients only get `ClientboundSetTimePacket` once a second. The sun moves at 1x and then jumps every 20 ticks. |
| T3 | Conflicts with other Time abilities | Surge keeps fast-forwarding while Chronostasis freezes the dimension or while the dimension Time Field slows it to 0.25x. |
| T4 | Too narrow | Only ±8 Y, no fluid random ticks, no block entities (furnaces, brewing, crops in modded machines), no mob aging. Accelerating time only touches part of the world. |
| T5 | Permanent uptime | Cooldown (200) is shorter than the duration (400), so you can keep refreshing it forever. It's also dimension-wide, so in multiplayer it affects everyone. |
| T6 | No feedback | No particles, sound, or sky effect, so it reads as a chat message and nothing else. |
| T7 | Cleanup | `@EventBusSubscriber` ticks on its own, and `GauntletBeamEvents` also calls `clearExpired`. One of them is redundant. |

### Reality Copy (`activateReality` ability 3)

| # | Problem | Detail |
|---|---|---|
| R1 | **Stuck in paste mode** | Once something is copied, every later activation pastes. You can only re-copy by dying or changing dimension. |
| R2 | **Paste overwrites the block you're aiming at** | It pastes at `hit.getBlockPos()`, not `relative(hit.getDirection())`. Pasting while aiming at your chest replaces the chest and spills its contents. |
| R3 | Half-blocks | Copying one half of a door, bed, double chest, tall grass, or piston head pastes a broken half-block. |
| R4 | No rotation | Stairs, logs, and furnaces keep their original facing no matter which way you paste. |
| R5 | Bypasses protection | Raw `setBlock` skips spawn protection, `mayBuild`, and claim mods (no `BlockEvent.EntityPlaceEvent`). Reality Rewrite has the same issue. |
| R6 | Copies anything | Command blocks (with their commands), structure/jigsaw blocks, end portal frames, bedrock, and spawners with NBT can all be copied. |
| R7 | Infinite item duplication | Copying a full chest or shulker clones its contents. That may be intended, but servers need a config switch for it. |
| R8 | No cost or feedback | No cooldown, sound, particles, or preview of what's on the clipboard. |
| R9 | Small leak | `REALITY_COPIES` is never cleared on logout (`clearPlayerSession` only runs on death). |

---

## Part 2: Making these two feel good

### Time Surge → "Temporal Acceleration"
1. **Make it strong.** Drop the per-player sampling and loop over loaded chunks within N chunks of the target. For each, call `level.tickChunk(chunk, randomTickSpeed * multiplier)` (public on `ServerLevel`), or run vanilla's per-section loop yourself. This scales correctly with crop farms, saplings, copper aging, and ice/snow. Default multiplier: 20x.
2. **Tick block entities.** For each block entity in range, get `state.getTicker(level, type)` and run it extra times per tick. Furnaces smelt a stack in seconds, brewing finishes instantly, and spawners pump.
3. **Age living things.** `AgeableMob.ageUp` for babies, faster breeding cooldowns, zombie-villager cure timers, and egg hatching.
4. **Smooth, spinning sky.** Send `ClientboundSetTimePacket` every tick while active and step day time by 20–50 per tick. The sun visibly races across the sky, and that alone sells the effect.
5. **Make it a field, not the whole dimension.** Use the Time Field aim logic (the block you're looking at, 24-block radius) so other players aren't affected. **Crouch = whole dimension**, same as Time Field.
6. **Hold to channel.** The longer you hold, the more the multiplier ramps (like beam charging). Release to stop. This replaces the duration/cooldown mismatch.
7. **Rewind variant.** Crouch-scroll changes the direction. Negative winds day time backwards and reverts recently placed or broken blocks (see the Time rework below).
8. **Respect other Time abilities.** Pause while Chronostasis is active in that dimension, and multiply rather than fight the dilation rate.
9. **Feedback.** Use swirling `ENCHANT` / `REVERSE_PORTAL` particles at the field edge, an ambient tick-tock sound whose pitch rises with the multiplier, and green bonemeal-style sparkles on blocks that got ticked.

### Reality Copy → "Reality Clipboard"
1. **Fix the controls.** Crouch + activate copies and always overwrites the clipboard. Activate pastes onto the **clicked face**. The clipboard persists until you replace it.
2. **Region copy.** Crouch + activate on two corners selects a box (configurable max, default 32³), and a third crouch-activate copies it. Paste rotates with your facing (`BlockState.rotate(Rotation)`) and handles multi-block structures, which also fixes R3. In practice it puts WorldEdit inside the gauntlet.
3. **Ghost preview.** Sync the clipboard to the client and render a translucent outline/ghost at the paste spot while Reality Clipboard is selected. Scroll rotates the paste.
4. **Undo.** Keep the last 5 pastes, each with the blocks it replaced. Crouch-scroll down undoes.
5. **Copy entities too.** Activate on a mob to copy it (NBT minus UUID) and paste to spawn a clone, giving you a Reality illusion army.
6. **Safety valves (config).** Add an `integrated_superheroes:reality_uncopyable` block tag (command/structure/jigsaw blocks, end portal), `allowBlockEntityContents` (dupe toggle), `maxRegionVolume`, and fire `BlockEvent.EntityPlaceEvent` / `BreakEvent` so claim mods and spawn protection work.
7. **Feedback.** Use red/black "reality ink" particles that sweep across the pasted region layer by layer over a few ticks instead of all at once. A staggered build looks far better and avoids the lag spike.

---

## Part 3: Full Gauntlet overhaul — "completely broken"

### Phase 0: Bug fixes (do these first, small)
- Fix crash #1, toast #2, and icons #3; fix R1, R2, and R9; fix T1 and T2.

### Phase 1: Ability framework (enables everything else)
The controller is about 920 lines of `if (stone.equals(...) && ability == n)`, and beam validation is duplicated three times. Replace it with:
- `GauntletAbility` record/interface: `id`, `stone`, `activation` (TAP / HOLD / CHARGE / SCROLL), `crouchVariant`, `cooldownTicks`, `icon`, `color`, and the handler.
- `GauntletAbilities` registry, ordered per stone. `abilityCount`, tooltip, toast, HUD, config, and key handling all read from it, so adding a 5th ability takes one line.
- `GauntletConfig`: per-ability `enabled` **and** `cooldown` plus a global `powerMultiplier`. Add a `griefing` category (terrain destruction, block placement, chunk-wide effects) that servers can turn off in one place.
- **Infinity tier.** When all 6 stones are in a full gauntlet, every ability gets an amplified version (bigger radius, no cooldown, extra effect, listed below as ★). That makes completing the gauntlet a real power spike instead of just unlocking Snap.
- A shared "juice" helper: stone-colored particle burst, sound, small screen shake and FOV punch (client payload), and a cast animation on the hand.

### Phase 2: Per-stone overhaul

**Power (purple)**
- I. Strength Surge → **Overcharge**: hold to absorb all incoming damage as stored power, then release a shockwave whose radius and damage scale with what you absorbed. ★ Each charge stage also cracks the ground in a ring.
- II. Annihilation Beam: keep it. Crouch = **sweeping beam** that drags along your aim for its whole duration and carves trenches. ★ Twice the range, and it pierces bedrock (config).
- III (new). **Moon Toss**: rips a sphere of terrain from under your target spot and drops it on them as falling blocks from the sky (Thanos throwing the moon).

**Space (blue)**
- I. Space Fold: instant blink. Chain up to 3 blinks before the cooldown starts. Crouch = **swap places** with the aimed entity.
- II. Dimensional Rift: keep it. ★ Rifts suck nearby entities and items through.
- III (new). **Singularity**: a black hole at the aim point pulls every entity and loose block (as falling blocks) within 20 blocks into a crushing point for 6s, then detonates.
- IV (new). **Banish**: throw the aimed entity to a random point 5,000 blocks away, or into the void of the End.

**Reality (red)**
- I. Rewrite → **Transmute**: sphere of blocks around the hit (scroll radius 1–8) turns into the clipboard block, or the existing cycle if the clipboard is empty. Temporary by default; crouch makes it permanent. ★ Transmute mobs: turn the target into another entity type (chicken, bubbles/particles, a block statue).
- II. Gravity Inversion → **Gravity Field**: an area where you pick the direction (up/north/etc. by look), affecting every entity in range. ★ Also affects falling blocks and items.
- III. Size Shift: add crouch = **resize yourself**; scale reach and step height with size.
- IV. Copy → **Reality Clipboard** (Part 2).

**Soul (orange)**
- I. Soul Exchange: keep it. ★ Drains every hostile within 12 blocks at once.
- II. Soul Sever: the severed body keeps fighting for you while the soul is out.
- III. Reap/Restore → restored mobs return as **soul-bound allies** that follow and fight for you (a cap of 8, config).
- IV (new). **Soul World**: pull the target into an orange void pocket (a temporary platform far above the world, or its own dimension) for 15s, where they can't act and you can hit freely.

**Time (green)**
- I. Chronostasis: keep the hold-to-freeze. ★ You can break and place blocks and hit frozen entities, and the damage resolves all at once when time resumes.
- II. Temporal Dilation: widen the range from 0.05–3.0 to 0.0–10.0, so 0 = frozen target and 10 = blur. Crouch-activate = **Age**: babies become adults, adults wither/decay (Ronan crumbling), crops jump to max.
- III. Time Field: keep it, merged with the Surge field. Scroll the rate between 0.1x and 20x, so one ability covers both slow and fast.
- IV. Surge → **Rewind**: a ring buffer records entity positions/health and block changes within 32 blocks for the last 10s. Activate to visibly scrub everything back. ★ **Time Loop**: if you die while holding the Time stone, rewind yourself 10s instead (with a long cooldown, the Dormammu bargain).

**Mind (yellow)**
- I. Command: keep it. ★ **Mass Command**: every mob in a cone becomes a thrall for 60s.
- II. Mind Sight: show entities through walls (client glow outline instead of the server-side `GLOWING` effect, so other players don't see it) and nearby ores within 16 blocks for 15s.
- III. Mind Beam: targets hit by it turn hostile to their own allies for 10s.
- IV (new). **Pacify / Frenzy**: crouch makes everything in 32 blocks forget its target, and activating makes them all attack each other.

**Snap (white/gold)**
- I. Infinity Beam: rainbow 6-stone beam with each stone's effect on impact (freeze, transmute, banish, drain, etc.).
- II. Life-Halving Snap: keep it. Store snapped entities' NBT in `SavedData`.
- III (new). **Restore Snap** (Hulk snap): bring back everything stored from the last snap.
- IV (new). **Selective Snap**: choose a target category (hostiles / a mob type / a block type in the clipboard) and erase all of it within loaded chunks. Snapping away every stone block in range is completely broken in the fun way.
- Every snap takes a heavy toll: the gauntlet item goes to a "scorched" model, the caster takes damage and their arm is weakened (a debuff for a few minutes). The cost is part of how the power feels.

### Phase 3: Polish
- Stone-specific cast sounds and particles everywhere (see the juice helper).
- HUD: show a hint for the crouch variant and the clipboard contents or rewind buffer bar.
- New icons for every added ability (the `infinity_gauntlet` textures only cover the current set).
- Update the README and CURSEFORGE.md ability tables from the registry.

### Suggested order
1. Phase 0 fixes (one session)
2. Phase 1 framework, porting existing abilities with no behavior change, then verify in game
3. Time and Reality reworks (they share the field and clipboard pieces)
4. Remaining stones, one per session
5. Phase 3 polish and textures
