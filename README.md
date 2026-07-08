# SpaceReloaded

[![build](https://github.com/AlexMelanFromRingo/SpaceReloaded/actions/workflows/build.yml/badge.svg)](https://github.com/AlexMelanFromRingo/SpaceReloaded/actions/workflows/build.yml)
![Minecraft 26.2](https://img.shields.io/badge/Minecraft-26.2-62b47a)
![Fabric](https://img.shields.io/badge/loader-Fabric-dbd0b4)
![Java 25](https://img.shields.io/badge/Java-25-e76f00)
[![License: MIT](https://img.shields.io/badge/license-MIT-blue)](LICENSE)
[![recipe book](https://img.shields.io/badge/recipe%20book-online-6fd5e8)](https://alexmelanfromringo.github.io/SpaceReloaded/recipes.html)

Space exploration for Minecraft 26.2 built around one rule: **the physics are
real**. Rockets are entities assembled from blocks you placed yourself.
Delta-v comes from the Tsiolkovsky equation, thrust-to-weight is computed from
actual part masses, and a lopsided rocket tips over on ascent.

[Документация на русском](README.ru.md)

<p align="center">
  <img src="docs/img/creative-tab.png" width="46%" alt="SpaceReloaded creative tab">
  <img src="docs/img/airlock.png" width="52%" alt="Airlock with interlocked hermetic hatches">
</p>

<p align="center">
  <b><a href="https://alexmelanfromringo.github.io/SpaceReloaded/recipes.html">Recipe book</a></b>: every recipe rendered as cards, from crushing to the assembly table.
</p>

## What it does

- **Free-form rockets.** Build any shape on a launch pad, sneak-click the
  assembly pylon and the structure lifts off as a single entity. Mass, thrust,
  center of mass and torque come from the real blocks. TWR below 1 stays on
  the pad. Fuel is drawn from the actual tank blocks you filled. A plain
  click gives you a scan report first: delta-v, TWR and a verdict on whether
  the stack reaches orbit.
- **Two propellants with different characters.** Kerolox (dense, high thrust)
  is refined from oil shale; hydrolox (high specific impulse) is electrolyzed
  from ice. Engines burn one type, so a mixed stack won't assemble.
- **Hermetic sealing that takes geometry seriously.** Room checking is a
  26-direction flood fill: a diagonal corner gap counts as a leak, exactly
  like an incomplete portal frame. Airlocks interlock. A breach causes
  decompression that drags you toward the hole.
- **Earth orbit and the Moon.** Coordinate-scaled dimensions (1:8), an orbital
  platform per launch site, ISRU refueling on the Moon: ice becomes hydrolox
  plus oxygen for your canisters.
- **Docking.** A docking clamp block marks the separation plane. Undock a
  parked stack into carrier and lander, fly the lander down, refuel, return
  and dock again. Capture works within a 3-block radius, no pixel-perfect
  parking. Propellant splits and merges by real tank capacity.
- **Unmanned flights.** Write a flight program (destination plus landing
  beacon), upload it to a parked rocket, trigger the launch remotely. The
  autopilot climbs, transfers and lands on the beacon with a terminal
  retro-burn.
- **Orbital kinetic bombardment.** A cannon that only works in orbit fires
  tungsten rods along an honest entry trajectory. Crater size comes from
  E = ½mv² with cube-root scaling; obsidian-class blocks and water survive.
  Aim and fire remotely with a bound designator from any dimension.
- **Energy.** Team Reborn Energy units: coal generators to bootstrap, solar
  panels (x1.5 in vacuum), RTGs for the shadowed side, cable networks,
  batteries.
- **A guided progression.** Sixteen advancements walk you from the first
  steel ingot to closing the interplanetary loop.

## No teleport magic

Rockets never turn into inventory items. Returning home means refueling via
ISRU, docking with your carrier, or building a titanium return capsule that
survives touchdown up to 25 m/s. Cross-dimension events (kinetic strikes,
unmanned flights) hold auto-expiring chunk tickets, so flights finish even
with nobody around and survive server restarts mid-air.

## Documentation

- [Player guide](https://alexmelanfromringo.github.io/SpaceReloaded/): controls, fueling, oxygen, docking,
  unmanned flights, the cannon (Russian).
- [Recipe book](https://alexmelanfromringo.github.io/SpaceReloaded/recipes.html):
  every recipe rendered as cards, from crushing to the assembly table.
- [Progression](specs/001-space-mod-core/progression.md): the full arc from
  iron to the closed interplanetary loop.
- [Design docs](specs/001-space-mod-core/): spec, plan, data model, an API
  cheat sheet for Minecraft 26.2 internals, and a research-backed
  [backlog](specs/001-space-mod-core/inspiration-backlog.md) of mechanics
  adapted from Advanced Rocketry and Galacticraft.

## Building from source

Requires JDK 25 (Temurin).

```bash
./gradlew build                     # everything plus unit tests
./gradlew :core:test                # physics core tests, no Minecraft
./gradlew :mod:runClientGametest    # E2E rig: real client, 12 scenarios, ~3 min
./gradlew :mod:runClient            # dev client
```

The jar lands in `mod/build/libs/`.

## Architecture

- `core/` is pure Java physics: flood fill, Tsiolkovsky/TWR calculator,
  flight integrator with gyro feedforward, ballistics. No Minecraft imports,
  unit tested.
- `mod/` is the Fabric layer: entities, machines, dimensions, networking.
  Parts, fuels and planets are datapack registries, so addon packs can add
  planets or engines with JSON only.
- Server-authoritative everywhere. Sealing recalculation runs on background
  threads over palette snapshots taken on the main thread.
- The E2E rig (`mod/src/gametest/`) boots a real client and runs sealing,
  industry, assembly, cross-dimension bombardment, docking and flight-program
  scenarios on every change.

## License

[MIT](LICENSE).
