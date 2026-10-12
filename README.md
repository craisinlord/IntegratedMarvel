# Integrated Superheroes

Serum superheroes, Iron Man suit abilities, Marvel villains and Infinity Gauntlet additions for NeoForge 1.21.1, built on Marvel Legacy
and Marvel. Being split out of Boogaloo Core; the plan lives in the Crazy Craft pack at
`docs/integrated-superheroes-split-plan.md`.

## Build

Plain NeoForge project (ModDevGradle, mojmap), single module.

```bash
JAVA_HOME="/c/Program Files/Java/jdk-21.0.10" ./gradlew build
```

Output: `build/libs/integrated_superheroes-neoforge-1.0.0+1.21.1.jar`.

## Dependencies

Compile jars sit in `libs/`, copied from the pack's `mods/` so they match the versions the pack runs.

- **Required:** Marvel Legacy, Marvel, Infinity Gauntlet, Antarchy.
- **Alex's Caves (optional):** Magneto only exists with it installed. Without it, Scarlet Witch and the Thing
  lift blocks as vanilla falling blocks, and suit abilities that borrow its effects, depth charge or nuke use
  vanilla stand-ins.
- **Optional:** Weather2 (with CoroUtil), Scorched Guns, Ice and Fire, Mutant Monsters, Celestisynth, Better Combat.
- **GeckoLib** is there for compiling against Marvel's GeckoLib classes.

Jars dropped in `libs/runtime/` load in `runClient`.

## Optional mod rules

- Code for an optional mod lives in its own class and is only touched behind `ModList.get().isLoaded(...)`.
- Mixins for an optional mod are named with its prefix: `Weather2`, `ScorchedGuns`, `IceAndFire`,
  `MutantMonsters`, `BetterCombat`, `Celestisynth`. `IntegratedSuperheroesMixinPlugin` skips them when that mod
  is missing, and skips any mixin whose target class doesn't exist.

## Hooks for other mods

- **`api.GauntletSnapEvent`:** posted on the NeoForge event bus after a Life-Halving Snap. It carries the
  player and the victim count.
- **`api.HeroSensesEvent`:** posted when Black Panther's senses pulse. It carries the player and the radius.
- **`integrated_superheroes:deadpool_watchers` entity type tag:** mobs that trigger Deadpool's "being watched"
  narration.
- **`Villains.ECHO_TAGS`:** add entity scoreboard tags here to mark summoned copies that villains treat as
  echoes.

## Default recipes

- **Serums:** every serum, including Marvel Legacy's Spider-Man and Wolverine serums, is a shapeless recipe:
  empty syringe + villager blood sample or X-Genes + two hero items. Legacy's own serum recipes are dropped.
- **Adamantium:** netherite + titanium (replaces Marvel's iron + netherite + magma cream).
- **Iron Man:** Marvel's Suit Table upgrade chain stays as the cheap path. Every Iron Man, War Machine and Iron
  Patriot piece also has a direct crafting recipe in `recipe/iron_man_direct/`. The direct recipe is the
  matching vanilla armor (iron for Marks 2–7 and War Machine Mk1, diamond for Marks 11–24, War Machine Mk2 and
  Iron Patriot, netherite for Marks 25+) ringed by that mark's upgrade materials.
- **Cloth and costumes:** wool or a truffalo tuft gives 3 cloth. Legacy costumes are shaped from cloth, and
  Spider-Man and Wolverine suits swap styles across both mods. Marvel's own Spider-Man and Wolverine suits
  have no crafting recipe: craft a Legacy costume and swap it into them. Costumes are cosmetic, and wall
  climbing, claws and healing come from the serums.

## Advancements

Deadpool has his own tab. Every other hero has a branch in one of four team tabs: X-Men, Fantastic Four, Avengers
and Street Level. Each branch unlocks when you first hold that power, or for Iron Man, when you wear a full suit.
Nearly all of them are awarded from code through `hero/HeroAdvancements`:
- An ability wraps its damage in a *cast*, for example `HeroAdvancements.cast(player).hits(6, path).run(...)`.
  The cast counts the mobs it hits and the kills credited to it.
- Dashes, beams and shockwaves carry their cast to later ticks.
- Long-running totals are kept in the player's persisted NBT.

Magneto's branch only loads when Alex's Caves is installed.

## Configs

- **`integrated_superheroes-heroes.toml`:** hero, suit and gauntlet balance.
- **`integrated_superheroes-villains.toml`:** villains.
- **`integrated_superheroes-weapons.toml`:** thrown Mjolnir, Stormbreaker and shield damage. -1 keeps Marvel's
  own damage.

- **`integrated_superheroes-cleanup.toml`:** which overlaps between the three mods get merged, plus the unified
  creative tab. Each group is on by default:
  - Infinity Gauntlet's own gauntlets and stones become this mod's.
  - Uru becomes Marvel's uru ingot, and the rest of its uru materials are removed.
  - Its Mjolnir becomes Marvel's, and the Cosmi Rods, Sceptre, Stone Tracker and Suit-Case are removed.
  - Marvel titanium becomes Antarchy titanium.
  - Palladium ingots and blocks become netherite, the rest of palladium is removed, and the Palladium Arc
    Reactor becomes the Netherite Arc Reactor.
  - Proto-adamantium becomes adamantium.
  - The Hydra bricks are removed.
  - Marvel's syringe and reinforced leather become Legacy's, its web shooter and fluid are hidden, and
    Legacy's empty syringe draws villager blood.

  A removed item is hidden from JEI and creative tabs and taken out of item tags. Recipes that make it are
  dropped, and recipes that use it switch to the replacement (or are dropped when there's none). In loot,
  dropped items, player inventories and opened containers it turns into the replacement or vanishes. Its ore
  stops generating. The unified tab puts all four mods in one creative tab, in the order set by
  `assets/integrated_superheroes/creative_order.json`: workstations, metals, materials, blocks, gadgets, weapons,
  suits by hero, the Gauntlet, serums, spawn eggs. Items the file doesn't name land at the end of their
  best-guess section. JEI follows the same order. `hideSuitAbilityItems` hides the items suits hand out for
  their abilities.

## Pack-specific glue

Anything tied to Crazy Craft (Infinity Gauntlet stones, Herobrine, AntOS) stays in Boogaloo Core. Boogaloo
reaches into this mod through its hooks, or mixes into it directly if needed.
