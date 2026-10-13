# Integrated Superheroes

Superhero powers, Iron Man suit abilities, Marvel villain boss fights and a reworked Infinity Gauntlet for
NeoForge 1.21.1, built on top of [Marvel](https://www.curseforge.com/minecraft/mc-mods/marvel-superheroes),
[Marvel Legacy](https://www.curseforge.com/minecraft/mc-mods/marvel-legacy) and [Infinity Gauntlet Core](https://www.curseforge.com/minecraft/mc-mods/infinity-gauntlet-core)

- [How it works](#how-it-works)
- [Heroes](#heroes)
- [Iron Man suits](#iron-man-suits)
- [The Infinity Gauntlet](#the-infinity-gauntlet)
- [Villains](#villains)
- [Other features](#other-features)
- [For developers](#for-developers)

# How it works

## Getting a power

Most heroes come from a **serum**. Each serum is a shapeless recipe:

> Marvel Legacy **empty syringe** + **villager blood sample** or **X-Genes** + two hero items

- **Blood samples:** right-click an adult villager or wandering trader with an empty syringe. It gets 2 minutes of
  Nausea and can't be drawn from again until that wears off.
- **Injecting:** hold right-click with the serum in your main hand for a second, then let go. You can only have
  one serum power at a time.
- **Swapping powers:** hold right-click with an empty syringe for a second. Your power comes back out as its
  serum, and then you can inject a different one.

Costumes are cosmetic. Wall climbing, claws, healing and every other power comes from the serum, not the suit. 
There are some exceptions to this like Iron Man Suits, Ant Man, Black Panther

## Controls

You have **10 ability slots**, shown in a list at the bottom right with each key and cooldown.

| Slots | Default keys |
| --- | --- |
| 1–5 | **R, G, X, C, V** (Marvel Legacy's keys) |
| 6–10 | **Alt+R, Alt+G, Alt+X, Alt+C, Alt+V** |

- **Order:** hero abilities come first, then suit abilities.
- **Wheels:** some abilities open a radial wheel. Hold the key, then pick an option. Examples are Deadpool's
  Fourth Wall, Storm's Weather Control and Squirrel Girl's Swarm Orders.
- **Hero Abilities slot:** heroes with toggles (speed, jump boost, flight, senses) get this slot. Hold it to turn
  each one on or off.
- **Super jump:** heroes with a jump toggle super-leap by jumping while crouched. Sprint for extra distance.
- **Rebinding:** all keys are in Controls under Marvel Legacy's category.

# Heroes

### Captain America (Super Soldier Serum)

- **Passives:** Strength, Speed, Jump Boost and Resistance, plus agility.
- **Shield Charge:** a dash that damages and knocks back. It hits harder with a shield in your offhand.
- **Rally:** you and nearby players get Strength I and Resistance II.

### Black Panther

- **Passives:** Night Vision, Strength II, Speed II and Jump II. You also have a chance to deflect arrows.
- **Heightened Senses:** nearby hostiles glow.
- **Panther Pounce:** a leaping strike.
- **Claw Swipe:** a cone that damages and makes targets bleed.
- **Ancestral Plane:** once every 10 minutes, a fatal hit revives you at half health with Regeneration and
  Resistance.

### Deadpool

- **Healing factor:** you regenerate quickly. Dying knocks you down for a few seconds instead of killing you, up
  to 3 times; the charges come back over time. Click **"Nah."** to get up early.
- **Read the Script:** dodges the first hit from a mob that just targeted you.
- **Guns:** +25% damage with guns.
- **Narration:** fourth-wall captions comment on what you're doing.
- **Unload:** an auto-aimed volley of shots. Needs a gun.
- **Pull the Pin:** throws a grenade. There's a small chance you fumble it at your own feet.
- **Retcon:** rewinds your position and health by 5 seconds.
- **Fourth Wall (wheel):**
  - **Smash Cut:** teleports you home.
  - **Pause Menu:** stuns nearby mobs.
  - **Rage Quit:** a screen-cracking area punch.

### Wolverine and Spider-Man

Their abilities come from Marvel Legacy. Wolverine also gets a healing factor with 2 knock-downs instead of
deaths, and claw damage scaling.

### Quicksilver

- **Speed Force (toggle):** you speed up time for yourself, from 2× up to 10×. Change the rate with
  **Shift + scroll**. It burns hunger and turns off when you run out of food.
- **Bullet Time:** slows the world around you.
- **Sonic Boom:** a blast that gets stronger the faster you're running.

### Cyclops

- **Without his visor** (any Cyclops helmet): an uncontrolled beam fires constantly and carves up the terrain.
- **With the visor:**
  - **Optic Blast:** a terrain-breaking beam.
  - **Wide Blast:** five beams in a fan.

### Captain Marvel

- **Passives:** Strength, Resistance, fire immunity and no fall damage.
- **Flight** (toggle).
- **Photon Blast:** a terrain-breaking beam.
- **Binary Mode:** for a time, you glow and get Resistance IV, a damage aura and doubled Photon Blast.
- **Cosmic Dash:** a flying dash attack.

### Hulk

- **Two forms:**
  - **Banner:** weak.
  - **Hulk:** huge, strong and fast, with no fall damage.
- **Rage meter:** fills as you deal and take damage. It boosts damage and cuts cooldowns. At full rage you
  Hulk out whether you want to or not.
- **Last stand:** a hit that would kill Banner turns him into the Hulk instead.
- **Hulk Out:** switches forms.
- **Thunderclap:** a cone shockwave (Hulk form).
- **Hulk Smash:** a ring shockwave (Hulk form).
- **Gamma Leap:** a steerable crouch-jump that damages everything where you land.

### Storm

- **Flight** (toggle): taking off kicks up a gust.
- **Storm Aura** (toggle): lightning immunity, and bonus damage while it's raining.
- **Lightning Strike:** a bolt that chains to nearby hostiles. It fires 3 bolts in a thunderstorm.
- **Gale:** a wind blast.
- **Weather Control (wheel):**
  - Without Weather2: clear skies, rain or thunder.
  - With Weather2: also hail, blizzard, sandstorm, wind and tornadoes.

### Iceman

- **Ice Slide:** sprinting lays down an ice path.
- **Ice Form:** Resistance and freeze immunity, and you can walk on water.
- **Freeze Blast:** a freezing cone.
- **Cryogenesis:** a freezing burst that puts out fires and freezes water.
- **Ice Wall:** a temporary wall of ice.

### Scarlet Witch

- **Hex Field:** a chance to cancel attacks from enemies near you.
- **Probability dodge:** cancels a death once a minute.
- **Flight.**
- **Hex Bolt:** a homing bolt that applies a random hex: Jinxed, Misfire, Turned, Pratfall, Bad Luck, Unravel or
  Dazed.
- **Telekinesis:** lift a mob or blocks, then press again to throw.
- **Reality Warp:** turns nearby hostiles into harmless animals.

### Colossus

- **Steel Form (toggle):** big stats, and bullets and arrows ricochet off you. You sink in water.
- **Covering allies:** crouch in Steel Form to shield players standing behind you.
- **Fastball Special:** throw a mob or an ally. A thrown ally's first hit gets bonus damage.
- **Steel Charge:** smashes through leaves and glass (steel only).
- **Iron Slam:** a slam with a shockwave (steel only).

### Daredevil

- **Passives:** immune to Blindness and Darkness, a chance to dodge projectiles, and agility.
- **Radar Sense:** outlines everything around you, with a longer range in the dark.
- **Billy Club:** a thrown club that bounces between enemies and comes back.
- **Grapple Line:** swing to a point.
- **Takedown:** a lunge that stuns.

### Kitty Pryde

- **Phase (toggle):** walk through walls and take no damage, while a meter drains. You can't attack while phased,
  and mobs can't see you. When the meter runs out you're pushed back to safe ground.
- **Blocks you can't phase through:** anything tagged `integrated_superheroes:phase_blocking`.
- **Phase Strike:** an attack that ends your phase.
- **Phase Sink:** sinks the target into the ground and stuns it.
- **Phase Others:** take a nearby player through walls with you.

### Squirrel Girl

- **Tippy-Toe:** a companion squirrel who rides your shoulder and jumps down to attack whoever hurts you. If she
  dies, she comes back after a minute.
- **Call the Swarm:** 8 flying squirrels fight for you. Lost squirrels are replaced over time. When idle they
  climb trees, glide and wander nearby.
- **Sic 'Em:** aim at a mob to focus the swarm on it, or at a block to make them guard that spot.
- **Squirrel Pile:** the whole swarm piles onto one target and stuns it.
- **Swarm Orders (wheel):**
  - **Defend:** the default. They protect you.
  - **Forage:** they bring you dropped items.
  - **Gnaw:** they chew through the block you aim at and every connected block of the same type, up to 64.
  - **Stay:** they stay where they are.

### Human Torch

- **Fire immunity**, always on.
- **Flame On (toggle):** gives you flight and fire melee, and you set alight any hostile you touch. Water and
  rain put you out.
- **Fireball.**
- **Flame Cage:** traps and burns a target.
- **Nova Flare:** a huge fire blast that burns you out for a while afterward.

### Invisible Woman

- **Vanish:** go invisible and lose aggro. Attacking reveals you.
- **Force Field:** a temporary wall.
- **Force Bubble:** cuts damage taken, reflects projectiles and pushes mobs away.
- **Force Blast:** a knockback cone.

### Mr. Fantastic

- **Bouncy landings:** you never take fall damage, and hard landings bounce you back up unless you sneak.
- **Stretch Punch:** a long-range punch.
- **Stretch Hammer:** a delayed slam at the spot you aim at.
- **Stretch Sweep:** knocks back everything around you.
- **Shape Shift (wheel) / Use Shape:** turn into one of these:
  - A **boat**, or a **sofa** that friends can sit on.
  - A **slingshot**.
  - A **bouncy ball**.
  - A **periscope neck**.
  - A **parachute**.
  - A **pogo stick**.

### The Thing

- **Rock plating:** absorption hearts that regenerate.
- **Combos:** every third punch launches the target.
- **Crashing through:** sprinting smashes through glass, leaves, fences and wooden doors.
- **Landings:** heavy landings leave craters.
- **It's Clobberin' Time!:** a launching punch.
- **Wrestle:** lift a target and slam it down behind you.
- **Rip and Toss:** tear up blocks and throw them.
- **Brace:** take 70% less damage and knock attackers back.

### Magneto *(needs Alex's Caves)*

- **Flight:** only works while wearing ferromagnetic armor.
- **Magnetic Shield:** sends arrows and bullets back at the shooter.
- **Magnetic Walk:** walk on magnetic walls and ceilings.
- **Magnetic Grip:** grab metal mobs or blocks, then throw them.
- **Magnetic Crush:** damage that scales with the target's armor.
- **Weapon Swarm:** launches the metal weapons on your hotbar.

# Iron Man suits

Wearing a **full Iron Man, War Machine or Iron Patriot suit** adds that mark's abilities to your slots.

- **Getting suits:** build them through Marvel's Suit Table upgrade chain, or craft each piece directly. A direct
  recipe is the matching vanilla armor piece surrounded by that mark's upgrade materials. Marks 2–7 use iron,
  Marks 11–24 use diamond, and Marks 25 and up use netherite.
- **Energy:** suits run on FE.
  - **Charging:** Marvel's Suit Charger, any FE item charger, or slowly from Veronica.
  - **Ability cost:** each ability uses part of the charge.

| Suit | Abilities |
| --- | --- |
| Mk1 | Shoulder Rocket |
| Mk2 | Afterburner |
| Mk3, Mk5 | Afterburner, Flares & Flashbang |
| Mk6 | Afterburner, Flares, Laser Ring |
| Mk7, 11, 19, 20, 22, 28 | Afterburner, Micro-Missiles, Superhero Landing |
| Mk15 | Afterburner, Micro-Missiles, Stealth |
| Mk17 | Afterburner, Micro-Missiles, Overcharge |
| Mk21 *Midas* | Afterburner, Cryo Charge, Landing |
| Mk23 *Shades* | Afterburner, Fire Charge, Hellfire Bomb |
| Mk24 *Tank* | Brace, Micro-Missiles, Landing |
| Mk25 *Striker* | Afterburner, Hydraulic Punch, Landing |
| Mk30 *Blue Steel* | Afterburner, Arc Charge, Landing |
| Mk33 *Silver Centurion* | Afterburner, Micro-Missiles, Magnet Pulse |
| Mk35 *Red Snapper* | Afterburner, Micro-Missiles, Grapple |
| Mk37 *Hammerhead* | Depth Charge, Micro-Missiles, Landing |
| Mk38 *Igor* | Heavy Throw, Micro-Missiles, Landing |
| Mk39 *Gemini* | Starboost, Micro-Missiles, Landing |
| Mk42 | Afterburner, Micro-Missiles, Rocket Punch |
| Mk43 | Afterburner, Micro-Missiles, Proton Cannon |
| War Machine Mk1 | Afterburner, Ordnance Drop, Grenade Launcher |
| War Machine Mk2 | Micro-Missiles, Ordnance Drop, Nuclear Missile |
| Iron Patriot | Micro-Missiles, Ordnance Drop, Rally |

**Suit passives:**

| Suit | Passive |
| --- | --- |
| Mk21 | Immune to freezing |
| Mk23 | Immune to fire |
| Mk28 | Immune to poison, wither and radiation |
| Mk37 | Works underwater |
| Every helmet except Mk1 | Protects against gorgons, flashbangs and deafening |

**Ant-Man and Wasp** *(needs Antarchy)*

- **Helmet only:** you only need the helmet to command Antarchy's ants or wasps.
- **Ant-Man:** summon a colony, recruit wild ants, Sic 'Em, Overrun and Colony Orders (Defend, Attack, Forage,
  Chew, Stay).
- **Wasp:** summon a hive, recruit wild wasps, Sic 'Em, Sting Storm and Hive Orders (Defend, Attack, Screen,
  Stay).
- **Anthony:** Ant-Man also gets a big ant he can ride while shrunk.

# The Infinity Gauntlet

- **Installing stones:** right-click with an Infinity Stone while you have the **Infinity Gauntlet** or **Nano
  Gauntlet** in your inventory. The Nano Gauntlet is crafted.
- **Choosing:** with a gauntlet in either hand, switch stones with **← / →** and abilities with **↑ / ↓**.
- **Using:** right-click.
  - **Charged beams:** Power, Mind and Infinity beams charge for up to 2 seconds. Hold, then release; a longer
    charge goes farther and hits harder.
  - **Crouching:** many abilities do something different when you crouch.

**Power Stone**

- **Strength Surge:** Strength X for 20 seconds.
- **Annihilation Beam:** a beam that tears through the terrain.

**Space Stone**

- **Space Fold:** teleport next to any block you aim at, up to 320 blocks away.
- **Dimensional Rift:** opens a two-way portal to your saved destination for a minute. Crouch to go straight there
  instead.
- **Destination screen (O):** pick any dimension and coordinates, or use your current position. The second button
  depends on whether Infinite Dimensions is installed:
  - **With Infinite Dimensions: "Generate new dimension"** makes a brand-new random dimension and saves it.
  - **Without it: "Random dimension"** picks the Overworld, the Nether or the End.

**Reality Stone**

- **Reality Rewrite:** turns a block into a different one for 10 seconds.
- **Gravity Inversion:** flips a target's gravity for 15 seconds.
- **Size Shift:** **Shift + scroll** on a mob to resize it, from 0.01× to 100×.

**Soul Stone**

- **Soul Exchange:** drain health from a target. Crouch to give your health away instead.
- **Soul Sever:** freezes a target's soul for 8 seconds.
- **Reap / Restore:** catches the next soul that dies near you, and you can bring it back. Crouch to reap a living
  target.
- **Getting the Soul Stone:** sacrifice a player or one of your pets at the Soul Temple.

**Time Stone**

- **Chronostasis:** freezes time in your dimension while you hold the button.
- **Temporal Dilation:** **Shift + scroll** at a mob to set its speed between 5% and 300%.
- **Time Field:** slows everything nearby. Crouch to slow the whole dimension instead.

**Mind Stone**

- **Command:** the target copies your movement and attacks for 15 seconds. This works on players too.
- **Mind Sight:** see a target's stats, or make all life near you glow.
- **Mind Beam:** a charged beam.

**All six stones (Infinity Gauntlet only)**

- **Infinity Beam:** a charged beam.
- **The Snap:** use it twice within 5 seconds to confirm. It wipes out half of all loaded life in every dimension.

# Villains

Marvel's villains get new boss AI. Each boss has phases with warnings before big moves and a health bar that
scales with nearby players. Each one also stays near where the fight started, and its damage taken is capped so
it can't be killed in one burst.

**Baron Zemo**

- **Phase 1:** a sword duelist who parries. He has an anti-serum dart that shuts off your powers, an EMP that
  drains suits and grounds flight, and Hydra backup.
- **Below half health:** he fights dirty. Beat him without serums or suits and he fights fair to the end.
- **Reward:** everyone nearby gets **The Red Book**.

**Winter Soldier**

- **Moves:** knife combos, a sniper rifle, a launcher and his metal arm.
- **Turning him:** once he's weak, read him the ten words from the Red Book. He fights at your side for 5 minutes,
  then leaves behind his gear and a **Captain America serum**.

**Red Skull**

- **Phases:** three, with Tesseract volleys, blinking and swapping places with you.
- **Portals:** they spawn troopers. Hit a portal 3 times to close it.
- **Channelled attacks:** interrupt them by hitting him.
- **Space Stone:** he hunts players carrying it.

**Hydra agents**

- **Weapons:** guns (with Scorched Guns), crossbows or blades.
- **Alarm:** if one sees you for a few seconds it raises an alarm, and everyone within 48 blocks converges.
  Officers call reinforcements.

# Other features

- **Thrown weapons:** you can set the damage of thrown Mjolnir, Stormbreaker and shields in the config.
- **Advancements:**
  - **Deadpool** has his own tab.
  - **Every other hero** has a branch in one of four team tabs: **X-Men**, **Fantastic Four**, **Avengers** and
    **Street Level**.
- **One creative tab** holds Marvel, Marvel Legacy, Infinity Gauntlet and this mod in a sensible order. JEI uses
  the same order.
- **Duplicate items merged** across the three mods: gauntlets, stones, uru, titanium, palladium,
  proto-adamantium and syringes. Old copies in inventories, chests and loot turn into the kept version. Each merge
  can be switched off in the config.

## Requirements

- **Required:**
  - NeoForge 1.21.1
  - Marvel 2.1.0-pre6+
  - Marvel Legacy 1.1.1-alpha+
  - Infinity Gauntlet 1.5.1+
  - **Anthology** 1.0.0+
  - GeckoLib 4.7+
- **Optional:**

| Mod | Adds |
| --- | --- |
| **Antarchy** | Ant-Man and Wasp colonies, Antarchy-themed effects for Storm's gusts, Space rifts and size changes, titanium merging, and Antarchy ingredients in some recipes |
| **Infinite Dimensions** | "Generate new dimension" for the Space Stone |
| **Alex's Caves** | Magneto, magnetic block physics for Telekinesis and Rip and Toss, the real nuke and depth charge |
| **Weather2** | Extra weather for Storm |
| **Scorched Guns** | Guns for Deadpool and Hydra, plus real grenades and flashbangs |
| **Ice and Fire** | Real freezing, dragon charges, gorgon and siren protection |
| **Mutant Monsters** | Better shockwaves |
| **Better Combat**, **Celestisynth** | Extra animations |

Everything without its optional mod still works, using a vanilla stand-in.

---

# For developers

## Optional mod rules

- **Flags:** optional mods are checked through `compat.ModCompat`.
- **Optional-mod code:** lives in its own class under `compat/` and is only touched behind a flag.
- **Antarchy content** (rift, wind vortex, size ray, ants) is only reached through `compat.Antarchy*` classes.
- **Recipes:** the ones that need Antarchy items carry a `neoforge:mod_loaded` condition, with vanilla fallback
  recipes beside them.
- **Mixin names:** mixins for an optional mod are prefixed with its name (`Weather2`, `ScorchedGuns`, `IceAndFire`,
  `MutantMonsters`, `BetterCombat`, `Celestisynth`).
- **Mixin plugin:** `IntegratedSuperheroesMixinPlugin` skips those mixins when their mod is missing, and skips any
  mixin whose target class doesn't exist.

## Hooks for other mods

- **`api.GauntletSnapEvent`:** posted on the NeoForge event bus after a Snap, with the player and the victim count.
- **`api.HeroSensesEvent`:** posted when Black Panther's senses pulse, with the player and the radius.
- **Entity type tag `integrated_superheroes:deadpool_watchers`:** mobs that trigger Deadpool's "being watched"
  narration.
- **Block tags:**
  - `integrated_superheroes:unliftable`: blocks Scarlet Witch's telekinesis can't lift. Antarchy or a pack can
    fill it.
  - `integrated_superheroes:phase_blocking`: blocks Kitty Pryde can't phase through.
- **`Villains.ECHO_TAGS`:** entity scoreboard tags that mark summoned copies, which villains treat as echoes.

## Advancements

Nearly all of them are awarded from code through `hero/HeroAdvancements`.

- **Casts:** an ability wraps its damage in a cast, for example
  `HeroAdvancements.cast(player).hits(6, path).run(...)`. The cast counts the mobs it hits and the kills credited
  to it.
- **Later ticks:** dashes, beams and shockwaves carry their cast forward.
- **Long-running totals:** kept in the player's persisted NBT.
- **Magneto's branch:** only loads with Alex's Caves.
- **Villain advancements:** hidden, and awarded from code.

## Configs

- **`integrated_superheroes-heroes.toml`:**
  - Per-hero stats and ability tuning.
  - The `ironMan` section: per-suit stats, abilities, energy and Ant-Man/Wasp.
  - Gauntlet numbers are not in it; they're constants in `gauntlet/GauntletAbilityController`.
- **`integrated_superheroes-villains.toml`:** master switch, boss stats, Red Book ally time, Hydra arming,
  alarms and reinforcements.
- **`integrated_superheroes-weapons.toml`:** thrown Mjolnir, Stormbreaker and shield damage. `-1` keeps
  Marvel's own damage.
- **`integrated_superheroes-cleanup.toml`:** which overlaps between the three mods get merged, plus the unified
  creative tab. A restart is required. Each group is on by default:
  - Infinity Gauntlet's own gauntlets and stones become this mod's.
  - Uru becomes Marvel's uru ingot, and the rest of its uru materials are removed.
  - Its Mjolnir becomes Marvel's, and the Cosmi Rods, Sceptre, Stone Tracker and Suit-Case are removed.
  - Marvel titanium becomes Antarchy titanium (only with Antarchy).
  - Palladium ingots and blocks become netherite, the rest of palladium is removed, and the Palladium Arc
    Reactor becomes the Netherite Arc Reactor.
  - Proto-adamantium becomes adamantium.
  - The Hydra bricks are removed.
  - Marvel's syringe and reinforced leather become Legacy's, its web shooter and fluid are hidden, and
    Legacy's empty syringe draws villager blood.

  **What removal does:**
  - The item is hidden from JEI and creative tabs and taken out of item tags.
  - Recipes that make it are dropped. Recipes that use it switch to the replacement, or are dropped when there
    is none.
  - In loot, dropped items, player inventories and opened containers, it turns into the replacement or
    vanishes.
  - Its ore stops generating.

  **Unified creative tab:** follows `assets/integrated_superheroes/creative_order.json`.
  - Section order: workstations, metals, materials, blocks, gadgets, weapons, suits by hero, the Gauntlet,
    serums, spawn eggs.
  - Items the file doesn't name land at the end of their best-guess section.
  - `hideSuitAbilityItems` hides the items suits hand out for their abilities.
