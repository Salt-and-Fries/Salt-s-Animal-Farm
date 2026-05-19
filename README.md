# Salt's Animal Farm Features

Idea was created by Reddit user [u/Axoladdy](https://www.reddit.com/user/Axoladdy/).

This mod does not add new blocks, items, entities, recipes, biomes, or dimensions. It changes farm animal behavior, animal rewards, weather reactions, fear reactions, and adds debug tools for testing those systems.

The entire mod can be turned off with `enableMod` in the config. It defaults to `true`. When set to `false`, the mod does not add animal goals, alter loot, track farm data, react to rain or fear, register debug commands, or render debug labels.

## Farm Animal Weight

Configured farm animals receive a persistent `weight` value from `0` to `8`.

- Weight is saved to the animal and survives world reloads.
- Weight controls how many times the animal's normal loot table rolls when it dies.
- Weight `0` means the animal drops no loot from its normal loot table.
- Weight `1` through `8` rerolls the animal's normal loot table that many times.
- Successful comfort behavior builds a success streak.
- The first successful comfort task starts the streak.
- The second and later consecutive successful comfort tasks add `+1` weight each, up to `8`.
- Failed comfort tasks remove `1` weight, add to the fail counter, and reset the streak.
- Player damage, player-caused fear, hostile scares, and long rain exposure can also reduce weight.
- Right-clicking a configured farm animal with an empty main hand shows that animal's current weight as a short subtitle.

Default configured farm animals:

- `minecraft:cow`
- `minecraft:mooshroom`
- `minecraft:pig`
- `minecraft:sheep`
- `minecraft:chicken`
- `minecraft:rabbit`

## Comfort Tasks

Farm animals periodically try comfort tasks. While a comfort task is active, most normal goal interruptions are blocked so the animal can actually finish the task. Danger can still interrupt comfort behavior.

Default timing:

- Average task delay: `4000` ticks.
- Delay jitter: `2000` ticks.
- Search radius: `12` blocks.
- Vertical search: `4` blocks.
- Random search samples: `28`.
- Maximum task duration: `600` ticks.
- Comfort movement speed: `1.0`.

### Day Tasks

During daytime, animals can attempt:

- `shade`: find a spot without visible sky.
- `sunlight`: find a spot with visible sky.
- `water`: stand on dry land adjacent to water, including water one block below neighboring shoreline blocks.
- `space`: find open 3x3 standing space without nearby animals or mobs.
- `friend`: move close enough to another same-type animal.

### Night Tasks

At night, animals can attempt:

- `light`: find a spot with block light level `10` or higher above the animal.
- `nap`: stand on a configured soft block.
- `stars`: find a spot with visible sky.
- `water`: stand on dry land adjacent to water.
- `space`: find open space.
- `friend`: move close enough to another same-type animal.

Default soft blocks:

- `#minecraft:wool`
- `#minecraft:wool_carpets`
- `minecraft:hay_block`
- `minecraft:clay`
- `minecraft:moss_block`
- `minecraft:moss_carpet`
- `minecraft:pale_moss_block`
- `minecraft:pale_moss_carpet`

## Rain And Cover

This is also special rain behavior.

- When it is raining in a biome that supports rain, exposed animals urgently try the `cover` task.
- Rain behavior can be turned off with `enableRainBehavior` in the config. When disabled, animals ignore rain entirely.
- Deserts and other non-rain biomes do not trigger rain cover behavior.
- Cover means the animal's full body footprint is out of falling rain.
- Cover targets must be dry, non-water standing spots.
- Cover search prefers deeper shelter instead of shallow roof edges.
- Cover target paths must reach the actual target block, not just somewhere near it.
- If an animal stops while still exposed, it retargets to deeper reachable cover.
- Completing `cover` is neutral: it stops rain exposure but does not grant normal comfort success or weight.
- While raining, `space` is removed from the normal task pool so animals do not punish themselves for crowding under shelter.
- If an animal stays exposed to rain for `2400` ticks, about 2 minutes, it loses `1` weight.

Once an animal is fully covered during rain and rain behavior is enabled, vanilla pathfinding treats exposed rain blocks as blocked for that animal. This keeps sheltered animals on dry paths while still allowing them to move inside cover. This restriction turns off if the animal is hurt, frantic, or can see a scary mob.

## Fear And Hostiles

Farm animals react to danger.

- Configured scary mobs are scanned near farm animals.
- When a scary mob is detected, the animal flees and can lose `1` weight on scare cooldown.
- If a player damages a configured farm animal, that animal loses `2` weight and becomes frantic.
- If a player kills a configured farm animal, nearby farm animal witnesses with line of sight lose `2` weight and become frantic.
- Frantic animals run around with faster movement and periodically pick new random positions.

Default scary mobs:

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

Default fear settings:

- Hostile scare radius: `16`.
- Hostile scan interval: `40` ticks.
- Hostile scan random offset: `20` ticks.
- Hostile scare cooldown: `200` ticks.
- Hostile flee speed: `1.3`.
- Kill witness radius: `32`.
- Maximum kill witnesses: `64`.
- Frantic duration: `200` ticks.
- Frantic repath interval: `20` ticks.
- Frantic movement speed: `1.35`.

## Loot Changes

Configured farm animal loot is replaced by weight-scaled loot rolling.

- The animal's normal loot table is still used.
- The mod cancels the original single loot roll.
- It then rolls the same loot table once per current weight value.
- Player luck and normal loot context are preserved when player-caused loot applies.

Examples:

- Weight `0`: no normal animal loot.
- Weight `1`: one normal loot roll.
- Weight `4`: four normal loot rolls.
- Weight `8`: eight normal loot rolls.

## Debug Overlay

The mod adds a client/server debug overlay for farm animals.

When enabled, nearby configured farm animals show stacked text above their heads:

- Animal type.
- Weight and age.
- Current task and last result.
- Successful task streak.
- Total successes and failures.
- Frantic ticks and scare cooldown.
- Rain exposure ticks.

Debug data is sent from the server every tick for enabled players and covers animals within `48` blocks.

Command:

```mcfunction
/animalfarm render_debug_farm_data <true|false>
```

## Debug And Testing Commands

Commands are rooted at `/animalfarm`.

Render debug labels:

```mcfunction
/animalfarm render_debug_farm_data true
/animalfarm render_debug_farm_data false
```

Force nearby farm animals within `32` blocks to attempt a random task:

```mcfunction
/animalfarm task
```

Force nearby farm animals to attempt a specific task:

```mcfunction
/animalfarm task <task_name>
```

Force only a limited number of nearby farm animals to attempt a specific task:

```mcfunction
/animalfarm task <task_name> <amount>
```

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

Adjust the weight of the farm animal the player is looking at within `8` blocks:

```mcfunction
/animalfarm task weight add
/animalfarm task weight add <amount>
/animalfarm task weight set <0-8>
/animalfarm task weight subtract
/animalfarm task weight subtract <amount>
```

## Config File

The mod creates and rewrites a config file at:

```text
config/salts_animal_farm.json
```

Configurable values include:

- Total mod enable toggle.
- Farm animal entity list.
- Scary mob entity list.
- Soft block list.
- Rain behavior toggle.
- Comfort task timing.
- Comfort task search radius and vertical range.
- Comfort movement speed.
- Hostile detection and flee settings.
- Kill witness radius and witness cap.
- Frantic duration, repath speed, and movement speed.
- Detailed debug logging toggle.

Registry list entries can be direct registry IDs like `minecraft:cow` or tag entries prefixed with `#`. Invalid entries are ignored and logged.

## Networking And Rendering

The mod registers custom clientbound payloads for:

- Toggling farm debug label rendering.
- Sending per-animal debug label data to clients.

The client renderer replaces normal animal name tag rendering with stacked debug lines only when the debug overlay is enabled.

## Mixins And Integration Points

The mod uses mixins and Fabric events to add its behavior:

- Animal save data stores weight, streaks, task state, fear state, and rain exposure.
- Mob construction injects farm animal goals.
- Living entity loot is replaced for configured farm animals.
- Walk node evaluation blocks exposed rain paths for sheltered animals while rain behavior is enabled.
- Client entity rendering shows debug labels.
- Fabric server events handle damage, death witnesses, ticking debug data, and commands.
