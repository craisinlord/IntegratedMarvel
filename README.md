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
- [Thematic](#thematic)
- [For developers](#for-developers)

# How it works

## Getting a power

Most heroes come from a **serum**. Each serum is a shapeless recipe:

> Marvel Legacy **empty syringe** + **villager blood sample** or **X-Genes** + two hero items

- **Blood samples:** right-click an adult villager or wandering trader with an empty syringe. It gets 2 minutes of
  Nausea and can't be drawn from again until that wears off.
- **X-Genes:** mutant serums need these instead of blood. Find them in Hydra base and outpost chests, or as a rare
  drop from witches you kill. The chances are in the `xGenes` section of the heroes config.
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

- **Healing factor:** you regenerate quickly. Dying knocks you down for a few seconds instead of killing you,
  once every 5 minutes. Click **"Nah."** to get up early.
- **Read the Script:** dodges the first hit from a mob that just targeted you.
- **Guns:** +25% damage with guns.
- **Narration:** fourth-wall captions comment on what you're doing.
- **Unload:** an auto-aimed volley from the gun you're holding. Needs a gun.
- **Pull the Pin:** throws a grenade. There's a small chance you fumble it at your own feet.
- **Retcon:** rewinds your position and health by 5 seconds.
- **Fourth Wall (wheel):**
  - **Smash Cut:** teleports you home.
  - **Pause Menu:** stuns nearby mobs.
  - **Rage Quit:** a screen-cracking area punch.

### Wolverine and Spider-Man

Their abilities come from Marvel Legacy. Wolverine also gets a healing factor with a knock-down instead of
a death once every 5 minutes, and claw damage scaling.

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
  - **Hulk:** huge, strong and fast, with no fall damage. He's built like the Hulk the moment he
    transforms: wide chest, massive arms, short thick legs, a small head and a forward hunch.
- **Rage meter:** fills as you deal and take damage. It boosts damage and cuts cooldowns. At full rage you
  Hulk out whether you want to or not. Rage also makes the Hulk a little bigger and a brighter green, and
  at high rage gamma particles drift off him.
- **Last stand:** a hit that would kill Banner turns him into the Hulk instead.
- **Hulk Out:** switches forms.
- **Thunderclap:** a cone shockwave (Hulk form).
- **Hulk Smash:** a ring shockwave (Hulk form).
- **Gamma Leap:** a steerable crouch-jump that damages everything where you land.
- **Rampage:** the Hulk smashes through terrain, and each broken block flashes gamma green. The angrier he is,
  the harder the blocks he can break and the wider the path:
  - **Calm:** dirt, sand, leaves, glass, fences and doors.
  - **Angry (25 rage):** stone, wood and ores.
  - **Furious (60 rage):** metal blocks, deepslate and most modded blocks.
  - **Unstoppable (full rage):** everything short of obsidian and bedrock.

  He breaks through walls when he sprints or walks into them, and through walls and ceilings during a Gamma
  Leap. Landings leave craters. Hulk Smash digs trenches and Thunderclap throws the ground at enemies, both
  from Furious rage. Rampage follows the `superPowerGriefing` gamerule and claim protection, and never
  breaks containers or other block entities. It has its own toggle and config settings.

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
- **Ice Beam:** a few seconds of frost that follows your aim. It slows what it hits, freezes targets solid if you hold it on them, and turns water to ice when superpower griefing is on.

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
- **Staff Sweep:** spins the staff to hit and push back everything around you.

### Kitty Pryde

- **Phase (toggle):** walk through walls and take 55% less damage, while a meter drains. You can't attack while phased,
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

### Gambit

- **Passives:** agility, Speed and Jump, less fall damage, and **Cajun Charm** (Hero of the Village).
- **Kinetic charge:** everything Gambit throws spins through the air glowing purple and explodes in a purple
  kinetic burst instead of a TNT blast. He's immune to his own bursts.
- **What he can throw:** almost anything. How it behaves depends on its item tag, under
  `integrated_superheroes:gambit/`:
  - `cards` (paper, books, maps): fly fast and straight, with a smaller burst.
  - `heavy` (anvils, storage blocks, hammers): lob in an arc, with a bigger burst.
  - `explosive` (TNT, gunpowder, end crystals, grenades, uranium): a much bigger burst.
  - `incendiary`, `frost`, `shock`: the burst sets things on fire, freezes them, or calls down lightning.
  - `teleport` (ender pearls, chorus fruit): you teleport to the burst.
  - `potions`: the burst leaves a cloud of that potion.
  - `fireworks`: the burst also sets off a firework show.
  - `returns` (weapons, tools, shields, armor): the item drops where it lands after the burst, losing a little
    durability. Thrown weapons also add their attack damage to the hit.
  - `unthrowable`: never thrown (gauntlets, stones, serums, shulker boxes, nether stars).
  - Everything else is used up.
- **Kinetic Throw:** throws one of whatever you're holding. With an empty hand you throw a free phantom card.
- **Card Fan:** five cards (or five of the held stack) in a fan.
- **Charge Touch:** charges the mob or block you're looking at. It crackles purple and blows up after a moment,
  or right away if you punch it.
- **Bo Vault:** a kinetic pole-vault leap. Holding a staff (`gambit/staffs`: sticks, bamboo, blaze and breeze
  rods, staffs and rods from other mods) vaults further and slams on landing.
- **Full House:** marks up to five enemies in view, then throws a homing card at each.
- **Charged Ammo** (toggle): every projectile you fire gets charged and bursts on impact, including arrows,
  tridents, snowballs, potions and Mjolnir, Stormbreaker or the shield (which still come back). Each one costs a
  little hunger.

### Rogue

- **Passives:** agility, Strength and extra toughness.
- **Gloves** (toggle, on by default): take them off to use her power. While they're off:
  - Punching a mob bare-handed drains it (bonus damage, Weakness and Slowness, and you heal) and **absorbs** a
    memory of it.
  - Anything that hits you in melee gets drained a little.
- **Memories:** three slots, each showing what it holds and how long is left. A memory gives an active ability,
  a passive, or both, and fades after 3 minutes (1 minute for bosses, which also make you dizzy). Absorbing a
  fourth replaces the one closest to fading. Mobs without their own memory give **Stolen Vigor**.
- **Memory I / II / III:** use that memory's active ability.
- **Siphon Touch:** lunge at the nearest enemy in front of you and absorb it, gloves or not.
- **Absorbing players:** you borrow their hero abilities for 60 seconds, and their abilities go on a
  15-second cooldown. Absorbing Wolverine gives his healing factor, Spider-Man his wall-crawling, and another Rogue
  her current memories.
- **Memories include:**
  - **Vanilla:** Creeper (Detonate), Charged Creeper, Skeleton, Stray and Bogged volleys, Spider web shots and
    wall-climbing, Enderman Blink, Blaze Volley, Ghast Fireball, Slime and Magma Cube slams, Witch Hex Brew,
    Evoker Fang Line, Vindicator, Pillager, Illusioner, Ravager Roar, Vex, Shulker bullets, Guardian lasers, Warden
    Sonic Boom, Breeze Wind Charge, Iron Golem Toss, Drowned tridents, Ender Dragon breath, Wither skulls, and
    passives from most animals.
  - **Marvel:** Hydra agents, Baron Zemo, the Winter Soldier, Red Skull and Iron Man sentries.
  - **Antarchy:** the Queen's Black Hole, the King's Royal Volley, the Prince's Royal Bolts, the Princess's Ice
    Spikes, Basilisk gaze, Lucid bolts, Nightmare Dread Howl, Kraken Slam, Toreterror Seismic Leap, Molevore Spin
    Drill, Hercules Beetle Launch, Brutalfly orbs, Vortex winds, Octopus Bombs, mantis scythes, scorpion and
    manticore stings, and more.
  - **Alex's Caves:** Tremorzilla's Atomic Breath, the Nucleeper's Mini Nuke, Luxtructosaurus tephra, Deep One
    Mage waves, Licowitch Sugar Hex, Forsaken Sonic Roar, magnet pulls, dinosaur charges and stomps, gumballs,
    and a memory for every other mob.
  - **Mutant Monsters:** Mutated Blast, Seismic Slam, Mutant Arrows, Mutant Scream, Ice Barrage.
  - **Ice and Fire:** dragon charges and elemental immunity, Gorgon petrification, Cockatrice gaze, hydra,
    siren, troll, cyclops, stymphalian, dread and pixie memories.

### Jubilee

- **Passives:** agility, and her own fireworks never hurt her. Firework rockets she boosts an elytra with last
  longer.
- **Paff:** a fast firework bolt that pops on impact.
- **Firework Volley:** five rockets that home in on nearby enemies.
- **Flashbang:** a blinding burst. Nearby enemies are blinded, slowed, briefly stunned and lose track of you.
- **Pyro Jump:** a firework-powered leap with no fall damage. While gliding, it fires a long elytra boost.
- **Grand Finale:** fireworks go up around you, then rain down on enemies in the area for four seconds.
- **Palette (wheel):** Rainbow, X-Men Blue & Gold, Classic Jubilee, Red, White & Blue, or Gambit Purple.
- **Light Show** (toggle): your hands sparkle and you light up the area around you.

### Viltrumite *(needs Viltrumite)*

- **Serum:** an empty syringe, two Viltrumite Blood Samples and a Nether Star. Blood comes from Viltrumites:
  Conquest and the Lucan, Vidor and Thula squad drop samples when they die.
- **Powers:** the full Viltrumite kit from the Viltrumite mod, on its own keys (ability keys, ability menu and
  dashboard). That includes flight, Dash, Sonic Punch, Sky Breaker, Barrage, Thunderclap, Grab & Carry, Block,
  Lethal Chop and Super Speed.
- **Balance:** damage, damage reduction, healing and flight speed come from this mod's config instead of the
  Viltrumite mod's own settings. Damage reduction defaults to 55% instead of 97%.
- **Suit:** you wear an Omni-Man skin and cape while the power is injected. Both can be changed or turned off in the
  config.
- **Scourge Virus:** dying with it burns the power out for good. The serum is used up.
- **Serum only:** the Viltrumite race prompt and default race are turned off, so the serum is the only way to
  become a Viltrumite. This can be switched off in the config.
- **Rogue:** absorbing a Viltrumite grants the Viltrumite kit for the memory's duration (with weaker stats) and a
  Viltrumite Rush dash.
- **Animations:** some hero moves borrow Viltrumite's arm and body animations. Hulk's Thunderclap plays the
  thunderclap, Hulk punches through walls while he rampages, Thing's Clobberin' Time plays the Sonic Punch and Black Panther's Claw Swipe plays the Lethal Chop.
  Superman's Flying Punch plays the punch, and Homelander's Intimidate and Wonder Woman's Bracelet Clash play the thunderclap.
  Their sounds play too, for you and for anyone nearby. They only change how the moves look and sound, not what
  they do. They can be turned off in the config, and without Viltrumite the moves keep their usual animations.
- **Viltrumite flight:** Viltrumites, Captain Marvel, Superman, Homelander, Martian Manhunter and Shazam (only while
  powered) double-tap jump into Viltrumite flight, with its camera roll, sonic boom, wind, reentry glow, speed and
  block-breaking takeoff. Their Flight toggle has to be on, they get a Supersonic toggle, and a Kryptonian near
  kryptonite drops out of it. The list is `viltrumiteFlightHeroes` in the config. Other flying heroes like Magneto,
  Storm, Scarlet Witch and the Human Torch keep their own flight, and creative mode flight works as normal for
  anyone who isn't a Viltrumite. While a hero is in Viltrumite flight they use Viltrumite's flight animations instead
  of the usual flight pose. Without Viltrumite, hovering heroes get a light camera lean into turns
  (`heroCameraRoll` in `integrated_superheroes-flight-client.toml`).
- **Non-Viltrumites:** Viltrumite's always-visible empty left hand only shows for Viltrumites, and only Viltrumites
  bleed when hurt. Human blood is off by default (`humansBleed` in the config), since Human Blood Samples have no use.
- **Invader fix:** Conquest and the squad no longer crash the game with mods that sync data to every player, like
  AppleSkin.

### Superman (Kryptonian)

- **Serum:** villager blood, a Nether Star and Kryptonian Steel (a Netherite Ingot without Thematic).
- **Passives:** tank stats, no fall damage, immune to fire and freezing. The sun charges you: you hit harder and
  heal in full daylight under open sky, and hit softer at night or underground.
- **Toggles:** Flight, Super Speed, and Heightened Senses (x-ray: hostiles and players glow through walls, plus
  night vision).
- **Abilities:** Heat Vision (twin beams that melt blocks with superpower griefing on), Super Breath (a freezing cone
  that pushes mobs back) and Flying Punch (a dash).
- **Weakness:** anyone holding something in `#integrated_superheroes:kryptonite` within 8 blocks (or you holding it)
  drains you. You lose flight, speed and your abilities, take double damage and a little poison-like damage.
- **Skins:** Supergirl, Power Girl, Superboy, Jon Kent, Cyborg Superman and Bizarro (freeze vision and fire breath
  instead).

### Martian Manhunter (Martian)

- **Serum:** villager blood, chorus fruit and a phantom membrane.
- **Passives:** flight, no fall damage, agility.
- **Abilities:** Density Shift (Kitty Pryde's phasing on a timer), Camouflage (Invisible Woman's invisibility), Mind
  Blast (stuns and confuses a target), Mind Link (hostiles nearby glow for you and every player close by) and,
  with Morph installed, Shapeshift (take the shape of the creature you're looking at, press again to change back).
- **Weakness:** fire. Damage taken while burning, and fire damage, is doubled, and you can't phase or hide while
  burning.
- **Skin:** Miss Martian.

### Shazam

- **Serum:** villager blood, a lightning rod and a Nether Star. With Thematic, using Shazam's Throne on the Engraved
  Lodestone in the Rock of Eternity also gives one, once per player per lodestone.
- **Say the word:** the Shazam! ability calls down a lightning bolt and switches you between your powered form and a
  normal player. The other abilities only work while powered.
- **Powered:** flight, Super Speed, strength, fire and lightning immunity, Storm's lightning arcs around you,
  Lightning Call (a chaining lightning strike) and Thunder Dash. Dying turns you back.
- **Skins:** Mary Marvel, Black Adam, Darla, Eugene, Freddy and Pedro.

### Aquaman (Atlantean)

- **Serum:** villager blood, a Heart of the Sea and prismarine crystals.
- **Passives:** water breathing, fast swimming, Dolphin's Grace and Conduit Power in water. Hits are stronger in
  water or rain. After ten minutes dry you weaken until you get wet again.
- **Abilities:** Tidal Wave (a shockwave fan that knocks mobs back and puts out fires), Call of the Sea (dolphins
  join the fight for 30 seconds, needs water nearby), Trident Throw and Riptide Surge (a dash, faster in water).
- **Skins:** Mera, Ocean Master, Aqualad and The Deep.

### Wonder Woman (Amazon)

- **Serum:** villager blood, a Totem of Undying and a block of gold.
- **Passives:** strength, agility, and a Super Leap toggle.
- **Abilities:** Lasso of Truth (pulls and pacifies a mob, or grapples you to a block), Bracelet Clash (a ringing
  blast around you) and Bracelets Up (while up, projectiles from the front bounce back and melee hits are halved).
- **Skins:** Wonder Girl, Queen Maeve and Big Barda.

### Black Canary (Sonic)

- **Serum:** villager blood, an echo shard and a goat horn.
- **Abilities:** Canary Cry (a cone that knocks back, stuns and makes mobs lose track of you, and shatters glass
  when superpower griefing is on and you're allowed to build there), Sonic Burst (all around you) and Flying Kick.
- **Skin:** Silver Banshee.

### Homelander (Compound V)

- **Serum:** villager blood, Compound V and a milk bucket (a Nether Star instead of Compound V without Thematic).
- **Passives:** tank stats and no fall damage. No kryptonite weakness and no sun charge either.
- **Milk:** drinking milk heals you and gives a short Strength boost instead of clearing your effects. Once every 15
  seconds.
- **Toggles:** Flight (faster than Superman's) and Super Hearing (players and mobs nearby glow through walls).
- **Abilities:** Laser Sweep (red eye lasers that sweep left to right across whatever's in front of you, cutting
  soft blocks like dirt, wood and glass with superpower griefing on), Intimidate (mobs nearby run away for a few
  seconds and players get Weakness) and Sonic Flight (a sonic boom dash that slams into whatever's in the way).

### Serum skins

A skin keeps your serum's power and gives it another hero's look and colours (Flash, Reverse-Flash and Zoom for
Quicksilver, Red Hulk and She-Hulk for Hulk, Plastic Man for Mr. Fantastic, and so on). There are no skin items, you
pick one with a command:

| Command | What it does |
| --- | --- |
| `/heroskin` | Lists the skins for the serum you have in you |
| `/heroskin <skin>` | Picks a skin (tab completes) |
| `/heroskin default` | Back to the base hero |
| `/heroskin off` | Hides the costume and uses the default colours |
| `/heroskin <skin> <players>` | Operators only: sets it for someone else |

With Thematic installed, the DC serums and Homelander also use Thematic's effects and sounds. Heat Vision and
Laser Sweep draw Thematic's heat vision beams (white core, red glow) from your eyes, Bizarro's freeze vision uses its
ice vision, Super Breath blows its frost cone, Canary Cry uses its sonic scream, Shazam calls its forked lightning,
Tidal Wave raises whirlpools, the Martian's powers use its void effects and Bracelet Clash sends out its slam wave.
The damage, cooldowns and griefing rules stay this mod's. Without Thematic the old particles and beams are used.

A new injection always starts on the default look, and extracting clears it. Skins change what this mod draws itself
(trails, arcs, particles, Hulk's tint, the phase meter) and, with Thematic, the costume. Red Hulk is the exception:
rage makes him hotter instead of bigger, his hits set things on fire and fire and lava can't hurt him.
`serumSkins.enabled` in the heroes config turns skins off.

With Thematic installed, a serum draws its hero's Thematic costume on you while it's injected: Superman for the
Kryptonian serum, Hulk, Quicksilver, Wonder Woman and so on, or the skin's own costume (Flash, Red Hulk, Power Girl).
The costume is only a look. Your chest slot is untouched, it grants no Thematic abilities or stats, and it hides any
vanilla armour you're wearing. A real Thematic suit or a modded chestplate is shown instead. Viltrumite keeps the
Viltrumite mod's own Omni-Man suit. `serumSkins.automaticLooks` in the heroes config turns costumes off for everyone,
and `integrated_superheroes-thematic-client.toml` has `showOwn` and `showOthers` to hide them on your screen.

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
  Gauntlet** in your inventory.
- **Getting a gauntlet:** the Nano Gauntlet is crafted. Upgrade it into the Infinity Gauntlet at a smithing table with
  an **Uru Block** and a **Nether Star**. Any stones already installed carry over.
- **Finding the stones:**

| Stone | Where | How |
| --- | --- | --- |
| Power | Power Temple (ocean floor) or the Milano (End) | Mine the Orb and put it through a stonecutter |
| Space | Odin's Vault (lush and dripstone caves) or Red Skull's room in a Hydra base | Stonecut Odin's Tesseract, or craft Red Skull's Tesseract into the stone |
| Reality | The Collector's Vault (deserts, savannas and badlands) | Mine the Reality Aether and stonecut it |
| Soul | The Soul Pit (snowy plains) | Sacrifice a player or one of your pets there |
| Time | Ancient city centres | Right-click the Eye of Agamotto, then stonecut the Eye you get |
| Mind | Hydra Bunker (lush caves) | Right-click the Sceptre stand |
- **Choosing:** with a gauntlet in either hand, switch stones with **← / →** and abilities with **↑ / ↓**.
- **Using:** right-click.
  - **Charged beams:** hold, then release. A longer charge goes farther and hits harder.
  - **Crouching:** many abilities do something different when you crouch.
  - **Left Alt + scroll:** tunes abilities that have a setting, like a radius, size, speed or paste rotation. The
    modifier is its own key (Gauntlet Scroll Modifier in Controls), so tuning never counts as crouching.
- **Controls card:** a short card under the gauntlet panel lists the selected ability's controls. Hold **Left Alt** for
  the full description. The card can be set to full, controls-only or off in the client config.
- **Infinity Tier:** with all six stones in the full Infinity Gauntlet, cooldowns are halved, power goes up by half and
  many abilities gain an extra effect.

**Power Stone**

- **Overcharge:** soak up every hit for 6 seconds, then release it all as a cratering shockwave. Use it again to
  detonate early.
- **Annihilation Beam:** a charged beam that tears through terrain. Crouch for a wide beam.
- **Moon Toss:** rips up the ground under your target and drops it on them from the sky.

**Space Stone**

- **Space Fold:** teleport to any block you aim at, up to 320 blocks away. Chain up to 3 folds before the cooldown
  starts. Crouch to swap places with your target.
- **Dimensional Rift:** opens a two-way portal to your saved destination for 60 seconds. Crouch to go straight there.
- **Singularity:** a black hole at the aimed point drags everything in for 6 seconds, then detonates.
- **Banish:** hurls a target thousands of blocks across the world. Crouch to cast it into the void.
- **Destination screen (O):** pick any dimension and coordinates, or use your current position. The second button
  depends on whether Infinite Dimensions is installed:
  - **With Infinite Dimensions: "Generate new dimension"** makes a brand-new random dimension and saves it.
  - **Without it: "Random dimension"** picks the Overworld, the Nether or the End.

**Reality Stone**

- **Transmute:** rewrites a sphere of blocks into your clipboard block, or new forms, for 10 seconds. Crouch to make
  it permanent. Aimed mobs turn into animals; crouch to pop them instead. **Alt + scroll** sets the radius.
- **Gravity Field:** gives every creature around the aimed point the Inverted effect, so effect immunities apply.
  Crouch to toggle Inverted on yourself.
- **Size Shift:** **Alt + scroll** to resize a mob, or yourself when you aim at nothing, from 0.01× to 100×. Crouch
  and use it to reset to 1×.
- **Reality Clipboard:** crouch-use to copy a block or mob, or crouch-use twice to copy a whole region, contents
  included. Use to paste on the aimed face. **Alt + scroll** rotates the paste (up) or undoes the last one (down).

**Soul Stone**

- **Soul Exchange:** drain a target's life to heal yourself. Crouch to give your life away instead.
- **Soul Sever:** locks a target for 8 seconds. Every hit it takes lands twice as hard when it wakes.
- **Reap / Restore:** catches the next soul that dies near you. Restore it as an ally that follows and fights for
  you. Crouch to reap a living target.
- **Soul Sight:** sense every soul nearby, and the ores around you, through walls for 15 seconds.
- **Getting the Soul Stone:** sacrifice a player or one of your pets at the Soul Pit. Packs can add more sites to
  the `integrated_superheroes:soul_ritual_sites` structure tag.

**Time Stone**

- **Chronostasis:** freezes everything in your dimension while you hold the button.
- **Temporal Dilation:** **Alt + scroll** at a mob to set its speed between 5% and 300%. Crouch to age a mob or
  grow plants in an instant.
- **Time Field:** a field at the aimed point that slows or speeds up everything inside. **Alt + scroll** picks the
  rate. Fast fields also speed up crops and machines. Crouch to slow the whole dimension instead.
- **Time Surge:** fast-forwards the world for 20 seconds. The sun races across the sky while crops, furnaces and baby
  animals run many times faster.
- **Rewind:** mobs, broken blocks and fresh deaths near you scrub back 10 seconds. Crouch to include yourself.

**Mind Stone**

- **Command:** the target copies your movement and attacks for 15 seconds. This works on players too.
- **Mind Beam:** a charged beam that turns its victims against each other.
- **Frenzy:** every mob nearby attacks the closest other mob. Crouch to pacify them instead.
- **Mind Link:** possess a creature or player for 20 seconds. See through its eyes, move it, jump and attack with it,
  and press F5 to change view. Use again to return. Your body stays behind, and getting hurt snaps you back.

**All six stones (Infinity Gauntlet only)**

- **Infinity Beam:** a beam carrying every stone. It slows, withers, flips gravity and drains life.
- **Snaps:** press twice within 5 seconds to confirm. Every snap takes a toll: half your health, a few minutes of
  Weakness, Mining Fatigue and Slowness, and a scorched gauntlet.
  - **Life-Halving Snap:** erases half of all loaded life in every dimension.
  - **Restore Snap:** brings back every mob the last Life-Halving Snap erased.
  - **Selective Snap:** erases every mob of the aimed type, or every hostile if you aim at nothing. Crouch to erase
    your clipboard block nearby.

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
- **Roles:** riflemen, assault, breachers, snipers (they hold their ground at range), grenadiers, troopers,
  crossbowmen, officers, heavies with a gatling gun and rocketeers. Rocketeers won't fire while another agent
  is standing near their target, and Hydra explosions leave blocks alone when `mobGriefing` is off.

**With Thematic installed**

These fights run on Thematic's own mob AI. This mod adds a boss bar, a damage cap, a leash, a title card,
spawns and drops (all in `[thematic_villains]` of `integrated_superheroes-villains.toml`).

- **Ultron:** use the **Mind Stone** on an Ultron Regeneration Cradle in Thematic's Foundry. Ultron takes the
  stone into his scepter and climbs out. He unlocks more of his moves as he loses health and calls in rogue
  sentinels at two thirds and one third. Kill him for the stone back and an **Ultron chip** (the Ultron suit
  needs it). If he despawns, the stone goes back to whoever woke him. One Ultron per cradle.
- **Black Adam:** rarely appears near players on the surface of a desert. Drops the **Scarab of Black Adam**.
- **Reverse Flash:** rarely shows up in a lightning flash during a thunderstorm. He dodges and blinks around
  your attacks. He drops 2 or 3 **Lightning Shards**, which make Shazam's Throne.
- **Penguin and Two-Face:** at night in a village or an Abandoned Street, one of their crews can start a raid with
  objectives, waves and the boss himself. Operators can start one with `/heroraid <player> [penguin|two_face]`. Each player can run each raid once every two hours.
- **Phantom Zone Escapee:** can crash down near players outdoors on clear Overworld nights. It drops **Kryptonian
  Blood**, and the **Manhunter** drops **Martian Blood**. Arkillo's arena no longer drops his spawn egg.

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
  - Thematic 1.7.2 and Thematic Heroes 1.1.2 (any 1.7.x and 1.1.x). See [Thematic](#thematic)
- **Optional:**

| Mod | Adds |
| --- | --- |
| **Antarchy** | Ant-Man and Wasp colonies, Antarchy-themed effects for Storm's gusts, Space rifts and size changes, titanium merging, Antarchy ingredients in some recipes, and Rogue memories for every Antarchy mob and boss |
| **Infinite Dimensions** | "Generate new dimension" for the Space Stone |
| **Alex's Caves** | Magneto, magnetic block physics for Telekinesis and Rip and Toss, the real nuke and depth charge, Rogue memories for every Alex's Caves mob, extra purple magic on Gambit's bursts, and a rainbow trail for Jubilee |
| **Weather2** | Extra weather for Storm |
| **Scorched Guns** | Guns for Deadpool and Hydra, real grenades and flashbangs, and grenades Gambit can charge and throw |
| **Ice and Fire** | Real freezing, dragon charges, gorgon and siren protection, Rogue memories for its creatures, and dragon items that give Gambit's bursts fire, frost or lightning |
| **Mutant Monsters** | Better shockwaves, and Rogue memories for the mutants |
| **Viltrumite** (ViltrumiteCore + ViltrumiteFlight) | The Viltrumite serum, balanced Viltrumite stats, weaker Viltrumite NPCs that drop blood, a Rogue memory, a Viltrumite advancement tab and Viltrumite animations for some hero moves |
| **Better Combat**, **Celestisynth** | Extra animations, plus extra firework sparkle for Gambit and Jubilee with Celestisynth |

Everything without its optional mod still works, using a vanilla stand-in.

# Thematic

Thematic and its Thematic Heroes content are required. The version range is kept tight because this mod patches
Thematic's internals, so a new Thematic minor version needs checking before the range is raised.

## What this mod changes in Thematic

- **Starters:** Thematic's starter character picker (Steve, Alex, Toby, Herobrine) is off, and `/thematic starter`
  only works for operators.
- **First-join items:** new players don't get Thematic's Tablet and Wallet.
- **Gamerules:** when the server starts, Thematic's potion and golden apple cooldowns, its Resistance III cap on
  every entity and its tripwire ban are switched off. A gamerule is only changed while it still holds Thematic's
  default, so a value an admin set by hand stays.
- **Death messages and name tags:** vanilla death messages stay, and name tags show at the normal distance
  instead of disappearing past a few blocks.
- **Flight:** Thematic grounds any flying player who isn't wearing one of its flying suits. That's patched, so
  serum heroes, Iron Man suits and Viltrumites keep their flight.
- **Kryptonite:** Thematic's kryptonite is in the `integrated_superheroes:kryptonite` tag, so it weakens
  Kryptonians.
- **Effects:** hero and suit abilities can use Thematic's visual effects in place of their own particles. If an
  effect is missing or a Thematic update breaks it, the normal particles play instead.

## Materials

**Merges** (`thematicMerges`, on by default). Thematic's copies turn into the versions this mod already uses:

- Vibranium, adamantium and carbonadium become Marvel's vibranium and adamantium. Gold-titanium becomes Marvel's.
- Titanium becomes Antarchy titanium, or Marvel's without Antarchy.
- Fabrics and reinforced leather become Marvel Legacy cloths, and vials become Legacy's empty vial.
- Mutant genes become X-Genes, blood vials become villager blood samples and pym tech becomes Pym Particles.
- Silver becomes Ice and Fire silver (iron without it) and electrum becomes Oritech electrum (gold without it).
- The merged ores stop generating.

**Extras** (`thematicExtras`, on by default). Cuts Thematic's extra materials:

- Tin and crimson iron become copper; magnesium, dark iron and steel become iron; amazonium becomes gold;
  promethium becomes Oritech prometheum (netherite without Oritech); krypt steel becomes netherite; hardened steel
  becomes titanium; concentrated carbon becomes a coal block; white dwarf star alloy becomes uru.
- Peridot becomes emerald. Ruby, scarlet emerald, ruby quartz and blood diamond become Antarchy blood crystal
  (redstone without Antarchy), and their ores stop. Sapphire becomes Antarchy bluestone (lapis without Antarchy).
  Dark quartz becomes quartz.
- Thunder-infused rock and electrolytic solution become Lightning Shards, and the ore stops.
- Nth metal becomes Antarchy antimetal (iron without Antarchy) and infernal fossil becomes Nether Expansion fossil
  fuel (blaze powder without it).
- Plates become the ingot they're pressed from. The Plate Press, Alloy Smelter and Centrifuge are retired.
- The Leather Jacket becomes a black leather chestplate, and recipes ask for a black-dyed one. Dog tags become a
  name tag, the magnifying glass and batnoculars a spyglass, the voice changer a note block, the blueprint paper.
- Alien blood vials and splatters become Alien Blood, so breaking the splatters in Spaceships gives it straight away.
- Limestone becomes diorite and slate becomes deepslate.
- Fibers become string. The diamond cube, bat wing, mysterious acid, gamma sample, Cyclops visors, ancient relic,
  plasma ball, lobster thermidor, cosmic debris, the unused emotion shards, the blue, rose and smoky quartz
  families, fiber clusters and the shapes of every cut material are removed. Compound V is crafted with diamond
  blocks.

**Pack swaps** (`thematicPackSwaps`, off by default). Flavour swaps onto other mods' materials:

- With Alex's Caves, kryptonite becomes uranium and filled ooze canisters become toxic paste. Empty canisters are
  removed, and ooze lakes and the kryptonite ore stop.
- With Ice and Fire, the Atlantean plate becomes blue sea serpent scales and the nth feather becomes a
  stymphalian bird feather.
- With Pastel, cosmic debris becomes star fragments and its block becomes a stardust block.
- The chili pot and the storage network's data cores, cards, terminal link and construction projector are removed.

Both groups follow the same removal rules as the rest of the cleanup config.

## Suits

- **Crafting:** Thematic suits are made at Marvel's **Suit Table**. The base suit starts from a vanilla
  chestplate that matches its tier (leather for tiers 1 and 2, chainmail 3 and 4, iron 5 and 6, diamond 7 and 8,
  netherite 9 and 10), plus up to nine parts picked for that hero. Thematic's Suit Bench is retired
  (`thematicSuitTable`). Every suit has its own hand-written recipe; the chestplate still follows the suit's tier.
- **Reskins:** every alternate look of a suit is a free swap in the Suit Table's Variants tab, in any direction.
  A reskin never changes powers: the suit keeps the same abilities and stats, and it keeps its rarity, shine and
  enchantments.
- **Alts with their own kit:** a handful of alternate suits play really differently. Those are separate crafts at
  the Suit Table, made from any look of the base suit plus their own parts.
- **Lanterns:** each corps has one base suit. Guy Gardner, John Stewart, Kyle Rayner, Jessica Cruz, Simon Baz,
  Kilowog and Tomar-Re are reskins of Hal Jordan and share his kit.
- **Serum-hero suits are costumes:** suits of heroes who come from a serum here (Hulk, Storm, Wolverine,
  Spider-Man, Superman, the Flash, Wonder Woman, Aquaman, Shazam and the rest) have no abilities, stats or
  flight. They're cheap: a leather chestplate and two or three cloths in the hero's colours. The power comes from
  the serum. Zod's suit can't be crafted, since the Phantom Zone Escapee wears it.
- **Vision:** his Suit Table recipe takes the Mind Stone instead of his A.I. chip.
- **Quantum Realm:** shrink all the way down in Marvel's Ant-Man or Wasp suit and keep crouching for
  5 seconds to fall into the Quantum Realm. The Quantum Tunnel still works too.
- **Rarity:** new suits get a fixed rarity instead of Thematic's random roll, Rare by default (`fixedRarity`).
  Each step adds 2 to the suit's stats. Combining two identical suits in an anvil still upgrades them.

## Serum looks

A serum shows its hero's costume automatically while the power is active. The `/heroskin` command lists the
looks you can use, picks one, goes back to the default, or turns the look off. See [Serum skins](#serum-skins).

## Villains

The living villains are covered under [Villains](#villains): Ultron (wake him by using the Mind Stone on the
Ultron Regeneration Cradle), Black Adam, Reverse Flash, the Penguin's and Two-Face's village raids, and Kryptonian
and Martian Blood from the Phantom Zone Escapee and the Manhunter. Their settings are in `[thematic_villains]` of
`integrated_superheroes-villains.toml`:

| Option | Default | What it does |
| --- | --- | --- |
| `enabled` | true | Boss bars, damage caps, leashes, title cards, spawns and drops for all of them |
| `leashRadius` | 40 | How far a living villain can get from where it appeared before it's pulled back |
| `perHitCap` | 40 | Most damage one hit can deal |
| `dpsCap` | 10 | Damage per second over 5 seconds before further damage is cut to 10% |
| `ultronFromCradle` | true | The Mind Stone wakes Ultron on a cradle |
| `ultronAfterPrimeChance` | 0 | Chance Ultron also turns up when Sentinel Prime dies |
| `blackAdamChance` | 0.004 | Chance per check, every 30 seconds, for a player on a desert surface |
| `reverseFlashChance` | 0.02 | Chance per check, every 30 seconds, for a player outdoors in a thunderstorm |
| `livingVillainCooldownMinutes` | 120 | Real minutes before a player can meet Black Adam, Reverse Flash or a Phantom Zone Escapee again |
| `villageCrewChance` | 0.01 | Chance per check, every 30 seconds, for a player in a village at night |
| `streetCrewChance` | 0.05 | Same, for a player inside an Abandoned Street at night |
| `fearShardDrops` | true | Player-killed Wardens drop 2-4 Fear Shards, Arkillo 3-5 |
| `shazamFromThrone` | true | Shazam's Throne on the Rock of Eternity's Engraved Lodestone gives a Shazam serum, once per player per lodestone |
| `escapeeSpawns` | true | A Phantom Zone Escapee can crash down near players outdoors on clear Overworld nights |
| `escapeeChance` | 0.003 | Chance per check, every 30 seconds, for a player outdoors on a clear Overworld night |
| `kryptonianBloodChance` | 0.5 | Kryptonian Blood drop chance from the Phantom Zone Escapee |
| `martianBloodChance` | 1.0 | Martian Blood drop chance from the Manhunter |

## Performance

- **Quiet debug log:** Thematic's debug lines (every ability tick, every entity created or saved) used to be most
  of `debug.log`. Its logger is raised to INFO.
- **Trimmed mob sync:** only players, their copies, Thematic's own mobs and mobs wearing a suit send Thematic's
  speed and component packets. Plain mobs no longer send them when their speed changes or a player starts
  tracking them. If marks, captures or construct visuals go missing on vanilla mobs, turn `trimMobSync` off.
- **Item trait cache:** Thematic's per-tick item trait checks no longer build a new string every time. Always on.

## What needs Thematic

- **Needs it:** Thematic suits and their Suit Table recipes and reskins, serum costume looks, Ultron, Black Adam,
  Reverse Flash, the village raids, the blood drops, the material merges and the gamerule, flight and
  performance fixes.
- **Works without it:** every serum (Superman, Wonder Woman and the other new ones included), serum skins, Iron
  Man suits, the Gauntlet and the Marvel villains. Kryptonian Blood, Martian Blood and the Scarab of Black Adam
  always exist as items, but only Thematic's villains drop them. There's no Ultron without Thematic.

## Thematic configs

**`integrated_superheroes-thematic.toml`** (only read when Thematic is installed):

| Section | Option | Default | What it does |
| --- | --- | --- | --- |
| `onboarding` | `disableStarters` | true | Turns off the starter picker and `/thematic starter` for non-operators |
| `onboarding` | `noStartItems` | true | No Tablet or Wallet on first join |
| `rules` | `vanillaDeathMessages` | true | Keeps vanilla death messages |
| `rules` | `showNameTags` | true | Name tags at the normal distance |
| `rules` | `noConsumableCooldowns` | true | Turns off potion and golden apple cooldowns (`disableConsumableCooldowns`) |
| `rules` | `noResistanceCap` | true | Lifts the Resistance III cap (`maxResistanceAmplifier` set to -1) |
| `rules` | `allowTripwire` | true | Lets players place tripwire (`disableTripWire` false) |
| `rules` | `keepOtherFlight` | true | Stops Thematic taking away flight and fly speed that come from somewhere else. Its grounding effects still apply |
| `rules` | `otherElytras` | true | Other mods' elytras work alongside Thematic gliders, and Thematic's elytra tick only runs for its own gliders and flyers |
| `rules` | `vanillaArmor` | true | Vanilla armour, toughness and armour wear apply again; a suit's defence divides on top |
| `rules` | `vanillaExplosionKnockback` | true | Thematic only damps explosion knockback for explosions caused by someone in a suit |
| `rules` | `attackEvents` | true | Suited punches fire the normal attack event first, so protection and frozen time can stop them |
| `rules` | `vanillaStatusEffects` | true | Vanilla Regeneration, Poison, Wither and Instant Health for suited entities (`oldStatusEffects` true) |
| `rules` | `otherShields` | true | Thematic only handles shield damage for its own shields |
| `suits` | `fixedRarity` | 3 | Rarity for new suits: 1 Common to 5 Legendary, 0 keeps the random roll |
| `suits` | `quantumDive` | true | Fully shrunk in an Ant-Man or Wasp suit, keep crouching to dive into the Quantum Realm |
| `suits` | `quantumDiveTicks` | 100 | Ticks of crouching before the dive |
| `performance` | `quietDebugLog` | true | Raises Thematic's logger to INFO |
| `performance` | `trimMobSync` | true | Only syncs Thematic data for players, copies, Thematic mobs and suited mobs |
| `performance` | `trimEffectBroadcasts` | true | Only Thematic's own effects are sent to every nearby player |
| `safety` | `ownContentOnly` | true | Thematic only loads items, blocks, entities, guns and the like from the `thematic` and `integrated_superheroes` namespaces |
| `visuals` | `thematicVisuals` | true | Abilities use Thematic's effects where one fits |
| `visuals` | `legacyGunModels` | true | Gun models on the old `forge:separate_transforms` loader load through NeoForge's (restart needed) |

**`integrated_superheroes-cleanup.toml`**, under `removals` (a restart is required):

- `thematicMerges` (on): the material merges above.
- `thematicExtras` (on): the extra material cuts above.
- `thematicPackSwaps` (off): the pack swaps above.
- `thematicSuitTable` (on): suits at the Suit Table, Suit Bench and its recipes retired, Book of Champions removed.
- `thematicDuplicates` (on): Thematic's copies of Marvel things (two Mjolnirs, Cap's shield, Deadpool's katana, the Thor, Iron Man, War Machine, Ant-Man, Wasp, Winter Soldier and Killmonger suits, Hydra eggs) are removed or merged.
- `thematicMoney` (on): Thematic's bills, money stacks, wallet, cash register and coins are removed; criminals drop no cash.

## Licence note

This mod ships none of Thematic's code, models, textures or sounds. It refers to Thematic's suits, mobs, items
and effects by id at runtime. The Thematic files it does ship are its own recipes, item tags and suit overrides
that trim kits, and they only do anything when Thematic is installed.

## Credits

The Witherstorm models are by csdani50, licensed under CC BY 4.0
(https://creativecommons.org/licenses/by/4.0/), converted to GeckoLib (geometry, animations and texture) and given
extra animations. The three beam pyramid meshes of the big model were dropped because GeckoLib only renders cubes.

- "Wither storm": https://sketchfab.com/3d-models/wither-storm-7c5ce4488d1948e4a80feb1410db4681
- "Wither Storm (Storm)": https://sketchfab.com/3d-models/wither-storm-storm-b2da01b12cb4435dbdd5b2eed39754c5

---

# For developers

## Optional mod rules

- **Flags:** optional mods are checked through `compat.ModCompat`.
- **Optional-mod code:** lives in its own class under `compat/` and is only touched behind a flag.
- **Antarchy content** (rift, wind vortex, size ray, ants) is only reached through `compat.Antarchy*` classes.
- **Recipes:** the ones that need Antarchy items carry a `neoforge:mod_loaded` condition, with vanilla fallback
  recipes beside them.
- **Viltrumite content** is only reached through `compat.ViltrumiteBridge`, `compat.ViltrumiteCompat` and
  `compat.ViltrumiteAnimationsClient`.
- **Mixin names:** mixins for an optional mod are prefixed with its name (`Weather2`, `ScorchedGuns`, `IceAndFire`,
  `MutantMonsters`, `BetterCombat`, `Celestisynth`, `Viltrumite`).
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
- **`api.RogueAbsorption`:** give Rogue a memory for any mob. Build one with
  `RogueMemory.builder(id, "Name").active("Ability", player -> cooldownSeconds).passive(...)`, then call
  `RogueAbsorption.register(entityType, memory)` during common setup. Use the factory overload to pick a memory
  per mob instance, or `registerMatcher` to match many mobs at once. Your registrations win over the built-in
  ones. `RogueAbsorption.grant` gives a Rogue player a memory directly.
- **`api.RogueAbsorbEvent`:** posted on the NeoForge event bus when Rogue absorbs a mob. Cancel it, or swap the
  memory or its duration.
- **Item tags `integrated_superheroes:gambit/*`:** `cards`, `heavy`, `explosive`, `incendiary`, `frost`, `shock`,
  `teleport`, `potions`, `fireworks`, `returns`, `staffs` and `unthrowable` decide how Gambit's thrown items
  behave. Add your own items to them.

## Advancements

Nearly all of them are awarded from code through `hero/HeroAdvancements`.

- **Casts:** an ability wraps its damage in a cast, for example
  `HeroAdvancements.cast(player).hits(6, path).run(...)`. The cast counts the mobs it hits and the kills credited
  to it.
- **Later ticks:** dashes, beams and shockwaves carry their cast forward.
- **Long-running totals:** kept in the player's persisted NBT.
- **Magneto's branch:** only loads with Alex's Caves.
- **Viltrumite tab:** only loads with Viltrumite.
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