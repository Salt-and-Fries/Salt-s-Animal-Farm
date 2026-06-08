# Salt's Animal Farm

Idea created by Reddit user [u/Axoladdy](https://www.reddit.com/user/Axoladdy/).

Salt's Animal Farm is a vanilla-plus farm animal behavior mod. It does not add new
blocks, items, entities, recipes, biomes, dimensions, or loot tables. Instead, it
changes how configured farm animals behave, how they react to their environment,
how they respond to danger, and how much their normal loot is worth after being
cared for.

The mod is built around a simple loop:

1. Farm animals periodically try to satisfy comfort needs.
2. Successful comfort behavior improves the animal's weight.
3. Stress, fear, rain exposure, and failed comfort behavior can reduce weight.
4. Weight changes how many times the animal's normal loot table rolls.
5. In settings where weight can reach `0`, animals can become sick and need better
   care before they are useful again.

## Supported Project Targets

The project currently defines builds for:

- Minecraft `26.1.2`
  - Fabric
  - NeoForge
- Minecraft `1.21.11`
  - Fabric
  - NeoForge
- Minecraft `1.21.1`
  - Fabric
  - NeoForge
- Minecraft `1.20.1`
  - Fabric
  - Forge

Fabric builds require Fabric API. The `26.1.2` Fabric target also exposes a Mod
Menu config screen entry when Mod Menu is present. NeoForge targets expose a mod
config screen through the NeoForge config screen extension point.

## What The Mod Does Not Add

Salt's Animal Farm intentionally does not add content objects.

- No new blocks.
- No new items.
- No new entities.
- No new recipes.
- No new dimensions.
- No new biomes.
- No custom loot tables.
- No custom animal variants.

All gameplay changes are applied to existing configured animals and existing
Minecraft systems.

## Master Enable Toggle

The config field `enableMod` is the master switch.

Default:

```json
"enableMod": true
```

When `enableMod` is `false`:

- Configured farm animal checks return false.
- Comfort tasks do not run.
- Hostile fear checks do not affect animals.
- Rain cover behavior does not affect animals.
- Weighted loot replacement does not affect animals.
- Empty-hand weight inspection does nothing.
- Sick state syncing sends empty state data.
- Debug overlay data contains no configured animals.
- Existing saved Salt's Animal Farm data is not updated while the mod is disabled.

Loader hooks, mixins, and commands are still registered as part of the installed
mod, but the gameplay behavior gates itself off through the config.

## Default Farm Animals

By default, these animals receive Salt's Animal Farm behavior:

- `minecraft:cow`
- `minecraft:mooshroom`
- `minecraft:pig`
- `minecraft:sheep`
- `minecraft:chicken`
- `minecraft:rabbit`

The list is configurable through `Farm Animals` in
`config/salts_animal_farm.json` or through the in-game config screen.

The list accepts:

- Direct entity ids, such as `minecraft:cow`.
- Entity type tags, prefixed with `#`, such as `#minecraft:skeletons`.

Entries with invalid identifier syntax are ignored and logged. Valid identifiers
for entities that do not exist in a particular Minecraft version simply match
nothing.

## Persistent Animal Data

Configured animals store extra per-animal data. This data is saved with the
animal and survives world reloads.

Saved values include:

- Current weight.
- Current successful comfort task streak.
- Current failed comfort task streak.
- Total successful comfort tasks.
- Total failed comfort tasks.
- Current comfort task name.
- Last comfort task name.
- Last comfort task result.
- Frantic ticks remaining.
- Hostile scare cooldown ticks remaining.
- Rain exposure ticks.
- Whether the animal is allowed to become sick.

New or existing animals without saved Salt's Animal Farm data are clamped into
the current effective weight range when loaded or ticked. In default settings,
that usually means they start at weight `1`.

## Difficulty Presets

The config field `preset` controls several important weight, fear, and difficulty
values.

Default:

```json
"preset": "dynamic"
```

Available presets:

- `dynamic`
- `easy`
- `normal`
- `hard`
- `custom`

### Dynamic Preset

`dynamic` follows the current world difficulty:

- Peaceful and Easy use the Easy preset values.
- Normal uses the Normal preset values.
- Hard uses the Hard preset values.

### Easy Preset

Easy is forgiving and rewards comfort quickly.

| Value | Effective setting |
| --- | --- |
| Minimum weight | `1` |
| Maximum weight | `10` |
| Successful task streak threshold | `1` |
| Failed task streak threshold | `2` |
| Hostile scare radius | `6` |
| Lose weight when hit by player | `false` |
| Lose weight when witnessing player-caused farm animal death | `false` |

### Normal Preset

Normal is the default middle ground.

| Value | Effective setting |
| --- | --- |
| Minimum weight | `1` |
| Maximum weight | `8` |
| Successful task streak threshold | `2` |
| Failed task streak threshold | `1` |
| Hostile scare radius | `12` |
| Lose weight when hit by player | `true` |
| Lose weight when witnessing player-caused farm animal death | `false` |

### Hard Preset

Hard is harsher and allows sickness for animals that are allowed to fall to
weight `0`.

| Value | Effective setting |
| --- | --- |
| Minimum weight | `0` |
| Maximum weight | `8` |
| Successful task streak threshold | `2` |
| Failed task streak threshold | `1` |
| Hostile scare radius | `16` |
| Lose weight when hit by player | `true` |
| Lose weight when witnessing player-caused farm animal death | `true` |

### Custom Preset

`custom` uses the matching values directly from the config file:

- `minimumWeight`
- `maximumWeight`
- `positiveTaskStreakThreshold`
- `negativeTaskStreakThreshold`
- `hostileScareRadius`
- `loseWeightWhenHitByPlayer`
- `loseWeightWhenWitnessingAnimalDeath`

The in-game config screen locks preset-controlled fields unless `custom` is
selected.

## Weight System

Each configured farm animal has a weight value.

Weight controls the number of normal loot table rolls when that animal dies.

Examples:

- Weight `0`: no normal animal loot.
- Weight `1`: one normal loot roll.
- Weight `4`: four normal loot rolls.
- Weight `8`: eight normal loot rolls.
- Weight `10`: ten normal loot rolls, possible by default on Easy.

Weight is clamped to the effective minimum and maximum from the current preset or
custom config.

### Checking Weight

Players can check an animal's current weight in-game.

Requirements:

- The mod must be enabled.
- The target must be a configured farm animal.
- The player must use the main hand.
- The main hand must be empty.
- The player must not be in spectator mode.
- The interaction must not be the active leash-holder interaction.

When successful, the player receives an action bar message:

```text
Weight: <value>
```

### Weight Gain

When an animal completes a normal comfort task successfully:

1. Its successful task streak increases by `1`.
2. Its failed task streak resets to `0`.
3. Its total successful task count increases by `1`.
4. Its last task and last result fields update.
5. If the successful streak is greater than or equal to the effective positive
   streak threshold, the animal gains `+1` weight.

This means a threshold of `1` gives weight on every success. A threshold of `2`
means the first success starts the streak and the second consecutive success
starts adding weight.

### Weight Loss From Failed Comfort

When an animal fails a normal comfort task:

1. Its failed task streak increases by `1`.
2. Its successful task streak resets to `0`.
3. Its total failed task count increases by `1`.
4. Its last task and last result fields update.
5. If the failed streak is greater than or equal to the effective negative streak
   threshold, the animal loses `1` weight.

With the default Normal and Hard threshold of `1`, every recorded comfort failure
removes weight.

### Other Weight Loss Sources

Animals can also lose weight from:

- Seeing scary mobs and successfully starting a flee path.
- Being damaged by a player, when enabled by the current preset or custom config.
- Witnessing a player-caused farm animal death, when enabled by the current
  preset or custom config.
- Staying exposed to rain for too long while rain behavior is enabled.
- Test/debug commands that directly change weight.

## Weighted Loot Replacement

For configured farm animals, the mod replaces normal animal loot rolling.

The original single loot roll is cancelled. The same vanilla loot table is then
rolled once per current weight value.

Important details:

- The animal's normal loot table is still used.
- The mod does not add a custom loot table.
- Loot context still includes the entity, origin, damage source, attacking
  entity, direct attacking entity, and player luck when player-caused loot applies.
- If the animal has a fixed loot table seed, that seed is respected.
- If weight is `0` or lower, no normal loot is dropped.
- Sick configured farm animals also give `0` experience.

## Sickness

An animal is sick when its weight is `0` or lower.

In default Dynamic settings, sickness mainly appears on Hard difficulty and only
for animals that are allowed to become sick. Bred children are marked as allowed
to become sick. Non-bred animals, such as natural spawns, spawn-egg animals, and
command-spawned animals, are protected from reaching weight `0` unless
`allowNonBredAnimalsToBecomeSick` is enabled.

### Bred Animals And Sickness

When a configured farm animal child is created through breeding:

- The child is marked as able to become sick.
- The child starts at at least weight `1`, even if the effective minimum is `0`.

This allows long-term farm management to matter more for bred farm populations
without immediately making newborn animals sick.

### Non-Bred Animals And Sickness

Default:

```json
"allowNonBredAnimalsToBecomeSick": false
```

When this is `false`, non-bred animals cannot be clamped below weight `1` even if
the active preset has `minimumWeight` set to `0`.

When this is `true`, all configured farm animals can fall to weight `0` if the
effective minimum weight allows it.

### Bred Animals Only Mode

Default:

```json
"onlyBredAnimalsUseWeightAndComfort": false
```

When this is `true`, only configured farm animals born from breeding use the
weight, comfort, sickness, fear, and weighted loot systems. Natural spawns,
spawn-egg animals, and command-spawned animals act like vanilla animals.

Existing animals from older saves that were already marked as bred sickness
candidates are treated as bred for this option.

### Sick Animal Effects

Sick configured farm animals:

- Are tinted green on the client.
- Move more slowly.
- Cannot enter love mode.
- Cannot mate.
- Reset love state while sick.
- Drop no normal animal loot.
- Give `0` experience.

The default sick movement speed multiplier is:

```json
"sickMovementSpeedMultiplier": 0.7
```

Sick animals can recover if they successfully complete comfort tasks and regain
weight.

## Comfort Task System

Configured farm animals periodically attempt comfort tasks.

Default timing:

| Config field | Default | Meaning |
| --- | ---: | --- |
| `comfortTaskAverageDelayTicks` | `4000` | Average delay between normal comfort task attempts. |
| `comfortTaskDelayJitterTicks` | `2000` | Random timing spread added around the average. |
| `comfortSearchRadius` | `12` | Horizontal target search radius. |
| `comfortVerticalSearch` | `4` | Vertical target search range above and below the animal. |
| `comfortSearchSamples` | `28` | Extra random target samples after deterministic search. |
| `comfortSpaceRetargetIntervalTicks` | `40` | Minimum delay before an active space task searches for a better target. |
| `comfortMaxPathChecksPerSearch` | `12` | Maximum pathfinding checks one comfort target search can spend before deferring. |
| `comfortPathfindingBudgetPerTick` | `64` | Shared comfort pathfinding checks allowed each server tick. |
| `comfortOutcomeCopyRadius` | `0` | Radius for copying recent direct comfort outcomes from nearby farm animals. `0` disables copying. |
| `comfortLingerTicks` | `20` | Stored and configurable linger value. Current task completion records success as soon as the condition is reached. |
| `comfortTaskReachTimeoutTicks` | `200` | Time allowed to reach or satisfy a target. |
| `comfortMaxTaskTicks` | `600` | Configured maximum task duration. |
| `comfortMoveSpeed` | `1.0` | Movement speed while traveling to comfort targets. |

The current effective task timeout is the smaller of
`comfortTaskReachTimeoutTicks` and `comfortMaxTaskTicks`. With defaults, that is
`200` ticks.

### Comfort Scheduling

The next normal comfort attempt is scheduled as:

```text
current tick + max(20, average delay - jitter) + random value from 0 to jitter * 2
```

With the defaults, normal attempts happen after:

```text
2000 to 6000 ticks
```

Twenty ticks is about one second.

### Comfort Task Priority

Comfort behavior runs at lower priority than frantic panic and hostile fleeing.

Comfort tasks do not start when the animal is frantic. Active comfort tasks can
be interrupted by danger:

- The animal becomes frantic.
- The animal was recently hurt.
- The animal can see a configured scary mob within the effective scare radius.

Danger interruptions are neutral. They do not count as normal comfort success or
failure unless the task condition had already been satisfied.

### Comfort Feedback

Successful normal comfort tasks:

- Show happy villager particles.
- Play the animal's ambient sound.
- Record a successful task.
- Potentially add weight.

Failed normal comfort tasks:

- Show angry villager particles.
- Play the animal's hurt sound.
- Record a failed task.
- Potentially remove weight.

Rain cover completion is neutral:

- It shows happy particles and plays the ambient sound.
- It records the last task result as `Covered`.
- It does not add weight.
- It does not count as a normal success.

## Day Comfort Tasks

During daytime, animals can attempt these normal tasks:

- `shade`
- `sunlight`
- `water`
- `space`
- `friend`

Daytime is based on overworld clock time from `0` through `11999`.

## Night Comfort Tasks

At night, animals can attempt these normal tasks:

- `light`
- `nap`
- `stars`
- `water`
- `space`
- `friend`

Night is any overworld clock time outside the daytime range.

## Rain-Adjusted Comfort Task Pool

When it is raining in a biome position that uses rain precipitation, `space` is
removed from the random normal task pool.

Day rain pool:

- `shade`
- `sunlight`
- `water`
- `friend`

Night rain pool:

- `light`
- `nap`
- `stars`
- `water`
- `friend`

This prevents animals from punishing themselves for crowding under shelter during
bad weather.

## Individual Comfort Tasks

### `shade`

The animal looks for a valid standing spot without visible sky.

A position counts as seeing the sky if either of these positions can see sky:

- One block above the standing position.
- Two blocks above the standing position.

### `sunlight`

The animal looks for a valid standing spot with visible sky.

This is a daytime task.

### `stars`

The animal looks for a valid standing spot with visible sky.

This is a nighttime task.

### `water`

The animal looks for a valid dry standing spot with water nearby.

The water proximity check looks for water:

- At the target position.
- Below the target position.
- In a horizontal neighbor block.
- One block below a horizontal neighbor block.

The animal still needs a dry standing spot, so accepted targets are effectively
dry shoreline-style spots with water beside them or just below a neighboring
shoreline block.

### `space`

The animal looks for a valid spot with open room.

The target must have:

- A clear `3x3` area around the standing position.
- Passable blocks at the standing level.
- Passable blocks above the standing level.
- No other living animals within the local clear area.
- No other living mobs within the local clear area.

The space task scores candidates so animals prefer moving away from a nearby
crowd center. While the task is active, it refreshes the target periodically so
the animal can keep looking for a better open spot if the area changes.

### `friend`

The animal looks for another living animal of the same entity type within the
comfort search radius.

When a friend is found:

- A random same-type friend is selected.
- The animal tries to move to a valid standing spot adjacent to that friend.
- If no adjacent standing spot works, the friend's current block can be used if
  it is a valid standing spot.
- If the friend moves, the target can update.

The task completes when the animal is close enough to the selected friend or
another same-type animal is nearby.

### `light`

The animal looks for a valid standing spot where the block light level above the
spot is at least `10`.

This is a nighttime task.

### `nap`

The animal looks for a valid standing spot on top of a configured soft block.

Default soft blocks:

- `#minecraft:wool`
- `#minecraft:wool_carpets`
- `minecraft:hay_block`
- `minecraft:clay`
- `minecraft:moss_block`
- `minecraft:moss_carpet`
- `minecraft:pale_moss_block`
- `minecraft:pale_moss_carpet`

Blocks that do not exist in a target Minecraft version simply never match.

### `cover`

`cover` is the urgent rain task. It is not part of the normal random task pool.

The animal attempts `cover` when:

- Rain behavior is enabled.
- The level is raining.
- The animal's current biome position uses rain precipitation.
- The animal is not fully covered.

Cover means the animal's full body footprint is out of falling rain. The mod
checks the center and corners of the animal's footprint.

Cover targets must be:

- Valid standing spots.
- Dry.
- Out of falling rain.
- Under cover for the animal's body footprint.
- Reachable by an exact path to the target block.

Cover search prefers deeper shelter. Candidates score higher when neighboring
positions are also covered and score lower when neighboring positions are still
exposed to rain.

If an animal reaches an exposed edge or navigation ends before it is actually
covered, it can retarget deeper into shelter.

## Valid Standing Spots

Most comfort tasks require a valid standing spot.

A valid standing spot must be:

- Inside world bounds.
- Loaded.
- Dry at the standing position.
- Dry above the standing position.
- Dry below the standing position.
- Passable at the standing position.
- Passable above the standing position.
- Considered a stable navigation destination.

Passable blocks include air, replaceable blocks, land-pathfindable blocks, or
blocks with empty collision shapes.

## Comfort Target Search

For most tasks, the animal searches in two phases.

### Deterministic Search

The animal scans outward from radius `1` to `comfortSearchRadius`, checking the
outside edge of each square around the animal and the configured vertical range.

The animal only accepts a target if it:

- Is a valid standing spot.
- Matches the selected task.
- Has a path that can reach it.

### Random Search

If deterministic search fails, the animal checks `comfortSearchSamples` random
positions inside the configured horizontal and vertical range.

The same validity, task match, and path checks apply.

If a comfort search reaches its per-search path cap or the shared per-tick
pathfinding budget, the animal defers the attempt briefly instead of recording a
comfort failure.

When `comfortOutcomeCopyRadius` is greater than `0`, a normal scheduled comfort
task can copy a recent direct success or failure from another configured farm
animal in that radius before doing target search work. Copied outcomes apply the
same result effects but are not eligible for other animals to copy.

### Special Search Rules

`friend`, `space`, and `cover` use specialized targeting:

- `friend` searches for same-type animals and tries to stand adjacent to one.
- `space` scores reachable open spots and prefers moving away from crowds.
- `cover` scores fully covered targets and prefers deeper shelter with exact
  reachable paths.

## Rain Behavior

Rain behavior is controlled by:

```json
"enableRainBehavior": false
```

When enabled, configured farm animals react to rain in rain-biome positions.

The behavior does not trigger:

- When the mod is disabled.
- When rain behavior is disabled.
- When the level is not raining.
- In positions where the biome precipitation is not rain.
- When the animal is already fully covered.

Dry biomes and snowy precipitation positions do not trigger rain cover behavior.

### Rain Exposure Weight Loss

If an animal should seek cover and remains exposed, its rain exposure counter
increases every tick.

At `2400` rain exposure ticks, about two minutes, the animal loses `1` weight and
the rain exposure counter resets.

The rain exposure counter resets to `0` when the animal no longer needs rain
cover.

### Sheltered Pathing

When a configured farm animal is fully covered during rain, vanilla land
pathfinding treats exposed rain blocks as blocked for that animal.

This helps sheltered animals stay under cover instead of wandering back into the
rain.

The rain path restriction turns off when:

- The animal is not fully covered.
- The animal is hurt.
- The animal is frantic.
- The animal can see a configured scary mob.
- The animal is not in a position where rain precipitation applies.
- Rain behavior is disabled.

## Fear And Hostiles

Configured farm animals can react to scary mobs and player-caused danger.

## Default Scary Mobs

Default scary mob config:

- `#minecraft:skeletons`
- `#minecraft:arthropod`
- `#minecraft:illager`
- `minecraft:zombie`
- `minecraft:zombie_villager`
- `minecraft:husk`
- `minecraft:drowned`
- `minecraft:creeper`
- `minecraft:enderman`
- `minecraft:witch`
- `minecraft:ravager`
- `minecraft:warden`

The `Scary Mobs` list accepts direct entity ids and entity type tags.

## Hostile Flee Behavior

Animals periodically scan for visible scary mobs.

Default hostile settings:

| Config field | Default | Meaning |
| --- | ---: | --- |
| `hostileScareRadius` | `16` | Custom fallback radius. Effective radius is preset-controlled unless using `custom`. |
| `hostileScanIntervalTicks` | `40` | Base scan interval. |
| `hostileScanRandomOffsetTicks` | `20` | Random scan delay offset. |
| `hostileScareCooldownTicks` | `200` | Cooldown before another hostile scare can remove weight. |
| `hostileFleeSpeed` | `1.3` | Movement speed while fleeing scary mobs. |

The effective scare radius comes from the active preset unless `custom` is used:

- Easy: `6`
- Normal: `12`
- Hard: `16`

Animals only react to scary mobs that are:

- Alive.
- Inside the effective scare radius.
- Visible through line of sight.
- Included in the configured scary mob list.

The nearest visible scary mob is selected. If the animal can find a flee position
away from the mob, it runs away at `hostileFleeSpeed`.

When the hostile scare cooldown allows it, starting a hostile flee removes `1`
weight, resets comfort streaks, and starts the scare cooldown.

Hostile fleeing does not start the frantic state by itself.

## Player Damage Fear

When enabled by the active preset or custom config, damaging a configured farm
animal as a player frightens that animal.

The animal:

- Loses `2` weight.
- Resets comfort streaks.
- Becomes frantic for `franticDurationTicks`.

This only applies when the animal survives the damage event and the damage taken
is greater than `0`.

Preset defaults:

- Easy: disabled.
- Normal: enabled.
- Hard: enabled.

## Witnessed Death Fear

When enabled by the active preset or custom config, nearby configured farm
animals can panic when they see a player kill another configured farm animal.

Default witness settings:

| Config field | Default | Meaning |
| --- | ---: | --- |
| `killWitnessRadius` | `32` | Radius around the killed animal where witnesses are checked. |
| `maxKillWitnesses` | `64` | Maximum number of nearby animals processed as witnesses. |

Each witness must:

- Be a configured farm animal.
- Be alive.
- Be inside the witness radius.
- Have line of sight to the killed animal.

Each valid witness:

- Loses `2` weight.
- Resets comfort streaks.
- Becomes frantic for `franticDurationTicks`.

Preset defaults:

- Easy: disabled.
- Normal: disabled.
- Hard: enabled.

## Frantic State

Frantic animals run around in panic.

Default frantic settings:

| Config field | Default | Meaning |
| --- | ---: | --- |
| `franticDurationTicks` | `200` | How long the frantic state lasts. |
| `franticRepathTicks` | `20` | Base delay before selecting another panic movement target. |
| `franticMoveSpeed` | `1.35` | Movement speed while frantic. |

While frantic:

- The animal cannot start comfort tasks.
- Active comfort tasks are interrupted.
- The animal repeatedly picks random nearby land positions.
- Movement uses `franticMoveSpeed`.
- The frantic timer counts down every server AI tick.

Frantic repathing uses:

```text
franticRepathTicks + random value from 0 to franticRepathTicks
```

## Debug Overlay

The mod includes a client/server debug overlay for farm animals.

Enable it for yourself with:

```mcfunction
/animalfarm render_debug_farm_data true
```

Disable it with:

```mcfunction
/animalfarm render_debug_farm_data false
```

When enabled, the server sends debug data every tick for configured farm animals
within `48` blocks of that player.

Displayed debug lines:

- `Farm Debug: <animal type>`
- `Weight: <weight> | Age: <age>`
- `Task: <current task> | Last: <last task> <last result>`
- `Streak: +<successful streak> / -<failed streak> | Success: <total successes> | Fail: <total failures>`
- `Frantic: <frantic ticks> | Scare CD: <scare cooldown ticks>`
- `Rain Exposure: <rain exposure ticks>`

Client rendering replaces the normal name tag display with stacked debug lines
only while the debug overlay is enabled.

Debug payload limits:

- Maximum debug entries per payload: `128`.
- Maximum lines per animal: `8`.

## Sick State Sync And Rendering

The server sends nearby sick-state data to clients every `10` ticks.

State sync details:

- Radius: `64` blocks around each player.
- Maximum entries per payload: `256`.
- Payload contains entity id and whether that animal is sick.
- On disconnect, the server sends an empty state payload to clear client state.

The client uses this data to tint sick configured farm animals green.

## Commands

All commands are rooted at:

```mcfunction
/animalfarm
```

Commands require a player source when they need to open a screen, toggle client
debug rendering, force nearby animal tasks, or change the weight of the animal
being looked at.

The command tree is registered by the mod. It does not add an explicit permission
requirement in code, although server command settings and mod loaders may still
affect who can run commands.

### Open Config Screen

```mcfunction
/animalfarm config
```

This sends a clientbound request to open the Salt's Animal Farm config screen.

On Fabric, the command can fail if the server cannot send the payload to that
client, such as when the client does not have the mod installed.

### Toggle Debug Overlay

```mcfunction
/animalfarm render_debug_farm_data true
/animalfarm render_debug_farm_data false
```

This toggles the farm debug overlay for the executing player.

### Force Random Comfort Tasks

```mcfunction
/animalfarm task
```

Forces nearby configured farm animals within `32` blocks to attempt a random
comfort task when their comfort goal next checks.

Nearby animals are sorted from closest to farthest.

### Force A Specific Comfort Task

```mcfunction
/animalfarm task <task_name>
```

Forces nearby configured farm animals within `32` blocks to attempt a specific
task.

Valid task names:

- `shade`
- `sunlight`
- `water`
- `space`
- `friend`
- `cover`
- `light`
- `nap`
- `stars`

Unknown task names are rejected by the command.

### Force A Specific Task On A Limited Number Of Animals

```mcfunction
/animalfarm task <task_name> <amount>
```

Forces the nearest configured farm animals, up to `<amount>`, to attempt the task.

`amount` must be at least `1`.

### Change Looked-At Animal Weight

The weight commands affect the configured farm animal the player is looking at
within `8` blocks.

Add weight:

```mcfunction
/animalfarm task weight add
/animalfarm task weight add <amount>
```

Set weight:

```mcfunction
/animalfarm task weight set <value>
```

Subtract weight:

```mcfunction
/animalfarm task weight subtract
/animalfarm task weight subtract <amount>
```

Notes:

- `add` without an amount adds `1`.
- `subtract` without an amount subtracts `1`.
- Add and subtract amounts must be at least `1`.
- Set values can be any integer, then clamp to the animal's effective weight
  range.
- The command reports the old and new clamped weight.

## In-Game Config Screen

The mod includes an in-game config screen.

It can be opened through:

- `/animalfarm config`
- NeoForge's mod config screen integration.
- Mod Menu on the `26.1.2` Fabric target when Mod Menu is installed.

The config screen:

- Applies changes immediately.
- Saves changes to `config/salts_animal_farm.json`.
- Shows a status message at the bottom.
- Has a `Reset All` button.
- Has list-specific reset buttons for farm animals, scary mobs, and soft blocks.

The config screen edits the local mod config for the environment where the
screen is running. In single-player, that can affect the active integrated
server. On a dedicated server, gameplay uses the server's own config file; the
client screen does not push config changes back to the dedicated server.

### Main Config Sections

The main screen contains these sections:

- General
- Lists
- Comfort Timing
- Comfort Search
- Hostile Fear
- Frantic Fear
- Debug

### Mob List Pickers

The farm animal and scary mob list screens include:

- Search.
- A passive-mob filter for farm animals.
- A filter to hide passive mobs for scary mobs.
- Selectable known mob entries.
- Custom entity id entries.
- Reset list button.
- Entity previews when a renderer is available.

Selecting a mob that is currently included by a tag expands that tag into its
individual entries except for the mob being toggled. This lets the screen remove
a specific mob from a tag-backed selection.

Custom mob entries accept direct entity ids like:

```text
modname:entity
```

Custom mob entries do not accept tags through the custom-entry dialog.

### Soft Block Picker

The soft block picker includes:

- Search.
- Known block tags.
- Known blocks.
- Custom direct block entries.
- Custom block tag entries.
- Item previews for blocks and tags when possible.
- Reset list button.

Custom soft block entries accept:

```text
modname:block
#modname:tag
```

For tag previews, the screen tries registry tag contents first and can also read
block tag JSON resources from available resource/classpath data.

## Config File

The config file path is:

```text
config/salts_animal_farm.json
```

If the file does not exist, the mod writes the default config.

If the file loads successfully, the mod sanitizes it and writes the sanitized
version back to disk.

If the file cannot be read or parsed, the mod logs a warning and uses defaults
for that run.

The config file is loaded during mod initialization. Manual edits made while the
game or server is running require a restart to take effect unless the same
runtime changes the config through the in-game config screen.

## Full Default Config Values

Default values:

```json
{
  "enableMod": true,
  "Farm Animals": [
    "minecraft:cow",
    "minecraft:mooshroom",
    "minecraft:pig",
    "minecraft:sheep",
    "minecraft:chicken",
    "minecraft:rabbit"
  ],
  "Scary Mobs": [
    "#minecraft:skeletons",
    "#minecraft:arthropod",
    "#minecraft:illager",
    "minecraft:zombie",
    "minecraft:zombie_villager",
    "minecraft:husk",
    "minecraft:drowned",
    "minecraft:creeper",
    "minecraft:enderman",
    "minecraft:witch",
    "minecraft:ravager",
    "minecraft:warden"
  ],
  "Soft Blocks": [
    "#minecraft:wool",
    "#minecraft:wool_carpets",
    "minecraft:hay_block",
    "minecraft:clay",
    "minecraft:moss_block",
    "minecraft:moss_carpet",
    "minecraft:pale_moss_block",
    "minecraft:pale_moss_carpet"
  ],
  "preset": "dynamic",
  "minimumWeight": 1,
  "maximumWeight": 8,
  "positiveTaskStreakThreshold": 2,
  "negativeTaskStreakThreshold": 1,
  "loseWeightWhenHitByPlayer": true,
  "loseWeightWhenWitnessingAnimalDeath": false,
  "sickMovementSpeedMultiplier": 0.7,
  "allowNonBredAnimalsToBecomeSick": false,
  "onlyBredAnimalsUseWeightAndComfort": false,
  "enableRainBehavior": false,
  "comfortTaskAverageDelayTicks": 4000,
  "comfortTaskDelayJitterTicks": 2000,
  "comfortSearchRadius": 12,
  "comfortVerticalSearch": 4,
  "comfortSearchSamples": 28,
  "comfortSpaceRetargetIntervalTicks": 40,
  "comfortMaxPathChecksPerSearch": 12,
  "comfortPathfindingBudgetPerTick": 64,
  "comfortOutcomeCopyRadius": 0,
  "comfortLingerTicks": 20,
  "comfortTaskReachTimeoutTicks": 200,
  "comfortMaxTaskTicks": 600,
  "comfortMoveSpeed": 1.0,
  "hostileScareRadius": 16,
  "hostileScanIntervalTicks": 40,
  "hostileScanRandomOffsetTicks": 20,
  "hostileScareCooldownTicks": 200,
  "hostileFleeSpeed": 1.3,
  "killWitnessRadius": 32,
  "maxKillWitnesses": 64,
  "franticDurationTicks": 200,
  "franticRepathTicks": 20,
  "franticMoveSpeed": 1.35,
  "enable_detailed_debug_information": false
}
```

## Config Sanitization

Config values are sanitized on load and save.

| Field | Sanitization |
| --- | --- |
| `enableMod` | `null` falls back to default. |
| `Farm Animals` | `null` falls back to default. Blank entries are removed and remaining entries are trimmed. |
| `Scary Mobs` | `null` falls back to default. Blank entries are removed and remaining entries are trimmed. |
| `Soft Blocks` | `null` falls back to default. Blank entries are removed and remaining entries are trimmed. |
| `preset` | Invalid or missing preset falls back to `dynamic`. |
| `minimumWeight` | At least `0`. |
| `maximumWeight` | Must be greater than `minimumWeight`; otherwise becomes at least the default maximum or the sanitized minimum. |
| `positiveTaskStreakThreshold` | At least `1`; `null` falls back to default. |
| `negativeTaskStreakThreshold` | At least `1`; `null` falls back to default. |
| `loseWeightWhenHitByPlayer` | `null` falls back to default. |
| `loseWeightWhenWitnessingAnimalDeath` | `null` falls back to default. |
| `sickMovementSpeedMultiplier` | At least `0.05`; `null` falls back to default. |
| `allowNonBredAnimalsToBecomeSick` | `null` falls back to default. |
| `onlyBredAnimalsUseWeightAndComfort` | `null` falls back to default. |
| `enableRainBehavior` | `null` falls back to default. |
| `comfortTaskAverageDelayTicks` | At least `200`. |
| `comfortTaskDelayJitterTicks` | At least `0`. |
| `comfortSearchRadius` | At least `2`. |
| `comfortVerticalSearch` | At least `1`. |
| `comfortSearchSamples` | At least `4`. |
| `comfortSpaceRetargetIntervalTicks` | Missing or non-positive values fall back to default; otherwise at least `10`. |
| `comfortMaxPathChecksPerSearch` | Missing or non-positive values fall back to default; otherwise at least `1`. |
| `comfortPathfindingBudgetPerTick` | Missing or non-positive values fall back to default; otherwise at least `8`. |
| `comfortOutcomeCopyRadius` | Missing values fall back to default. Negative values become `0`. |
| `comfortLingerTicks` | At least `1`. |
| `comfortTaskReachTimeoutTicks` | At least `20`. |
| `comfortMaxTaskTicks` | At least `20`. |
| `comfortMoveSpeed` | At least `0.1`. |
| `hostileScareRadius` | At least `4`. |
| `hostileScanIntervalTicks` | At least `5`. |
| `hostileScanRandomOffsetTicks` | At least `0`. |
| `hostileScareCooldownTicks` | At least `1`. |
| `hostileFleeSpeed` | At least `0.1`. |
| `killWitnessRadius` | At least `4`. |
| `maxKillWitnesses` | At least `1`. |
| `franticDurationTicks` | At least `20`. |
| `franticRepathTicks` | At least `5`. |
| `franticMoveSpeed` | At least `0.1`. |
| `enable_detailed_debug_information` | Boolean as provided. |

## Detailed Debug Logging

The config field:

```json
"enable_detailed_debug_information": false
```

When enabled, the server logs verbose comfort-task diagnostics.

Logged details include:

- Task scheduling.
- Forced task requests.
- Task selection.
- Target search candidates.
- Why candidates were accepted or rejected.
- Standing spot checks.
- Task condition checks.
- Pathing success or failure.
- Task ticking.
- Completion reasons.
- Failure reasons.
- Rain cover retargeting.
- Space retargeting.
- Friend retargeting.

This is intended for testing and tuning. It can be very noisy.

## Networking Payloads

The mod registers clientbound custom payloads for:

- Opening the config screen.
- Toggling debug farm data rendering.
- Sending per-animal debug lines.
- Sending per-animal sick state.

NeoForge protocol version:

```text
1
```

Payload ids:

- `salts_animal_farm:open_config_screen`
- `salts_animal_farm:render_debug_farm_data`
- `salts_animal_farm:farm_debug_data`
- `salts_animal_farm:farm_animal_state`

## Rendering Changes

Client rendering changes include:

- Sick configured farm animals are tinted green.
- When the debug overlay is enabled, normal name tag rendering for debugged
  farm animals is replaced with stacked debug lines.
- The config screen uses entity and item previews where available.

## Mixin And Event Integration Summary

Runtime behavior is added through mixins plus loader-specific event adapters.

Common mixin behavior:

- `AnimalMixin`
  - Stores Salt's Animal Farm data on animals.
  - Ticks frantic/scare/rain/sick state.
  - Clamps weight.
  - Applies sick movement speed.
  - Prevents sick animals from entering love mode.
  - Prevents mating if either configured parent is sick.
  - Marks bred children as able to become sick.
- `MobMixin`
  - Adds Salt's Animal Farm goals to server-side animals.
- `LivingEntityLootMixin`
  - Replaces configured farm animal loot with weight-scaled loot rolls.
  - Prevents sick configured farm animals from dropping experience.
- `WalkNodeEvaluatorMixin`
  - Blocks exposed rain path nodes for sheltered configured animals during rain.
- `LivingEntitySoundAccessor`
  - Allows comfort failure feedback to use the animal's hurt sound.
- Client renderer mixins
  - Apply sick tint.
  - Render stacked debug labels.
  - Expose entity renderers for config-screen previews on newer targets.

Loader event behavior:

- Registers commands.
- Sends sick state data every `10` ticks.
- Sends debug data for players who enabled the overlay.
- Clears state when players disconnect.
- Handles player damage fear.
- Handles player-caused death witnesses.
- Handles empty-hand weight inspection.

## Practical Farm Design Notes

Because animals now look for comfort, good farms benefit from:

- Roofed shelter for rain.
- Open pasture space.
- Water along the edges of pens.
- Shade and sunlight access.
- Soft bedding blocks for nighttime naps.
- Bright areas for nighttime comfort.
- Enough animals of the same type for friend tasks.
- Protection from hostile mobs.
- Avoiding overcrowded pens.

The mod makes animal pens more than storage boxes. A better designed farm can
produce healthier, heavier, more valuable animals.
