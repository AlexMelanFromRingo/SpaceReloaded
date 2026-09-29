# Changelog

All notable changes to SpaceReloaded. Each release line lists the feature branches it contains; the
full specifications live in `specs/<feature>/`.

## [1.0.1] — 2026-09-29

Playtest fixes.

- **Gears at right angles:** two large gears on perpendicular axes, offset diagonally in the plane of
  both axes, now mesh 1:1 as a bevel pair (the large gear rim has a 45° chamfer).
- **Networks no longer reset when a part is attached:** the new network keeps its generalized
  momentum Σ I·r·ω, so a spinning flywheel only slows a little to spin up the new part.
- **Friction:** bearing friction now scales with rotor weight (μ·m·g·r, less on the Moon and in
  orbit), static friction is 1.5× the running friction, and gear discs and flywheels feel air drag
  (none in vacuum). A coasting network no longer falls asleep while still spinning, so it now
  actually slows down.
- **Screens** close with the inventory key as well as Esc, and a late refresh no longer reopens a
  screen you just closed.
- **Motor speed:** armature voltage 10–100 % from the motor screen (no-load speed ∝ U).
- **Batteries** power a machine placed right next to them, without a cable.
- **Rover:** the wheels sit symmetrically at the sides of the deck (one pair used to hang far off it).

## [1.0.0] — 2026-09-25

First release. Minecraft 26.2, Fabric Loader ≥ 0.19.3, Java 25.

### Rockets and flight (001–002)
- Free-form rockets assembled from blocks on a launch pad; mass, thrust, centre of mass and torque from
  the real parts. Scan report with Δv, TWR and an orbit verdict.
- Multi-stage rockets with sequential Tsiolkovsky, stage separation, falling spent stages that crash
  or land softly as reusable boosters.
- Attitude control with a gyroscope PD loop, gravity turns, exponential atmospheres with quadratic
  drag, Sutton–Graves entry heating, a numerical ascent simulation.
- Three propellants (kerolox, hydrolox, methalox) as real fluids; hermetic sealing by 26-direction
  flood fill; airlocks, decompression.

### Logistics (003)
- Earth orbit and the Moon at 1:8 scale, orbital platforms, ISRU refuelling, docking and undocking,
  unmanned flights with a route planner, cargo lines between terminals, the wet workshop.

### Lunar industry (004)
- Electromagnetic mass driver and mass catcher, regolith electrolysis reactor, lava tubes and crash
  sites, resource progression across bodies.

### Engineering (005)
- Shafts, gears, gearboxes, clutches, flywheels and motor-generators with real torque and speed;
  press and lathe with tolerances that change engine quality; engineer's hammer and datapack
  multiblock templates.

### Materials and electronics (006)
- Silicon from sand to a flight computer: metallurgical silicon, trichlorosilane, Siemens,
  Czochralski, wafer saw, cleanroom fab with yield e^(−D·A); acids, alumina, lithium, nickel, alloys.

### Station life (007)
- Zone air as masses of O₂/N₂/CO₂ from gas tanks, NASA BVAD breathing, CO₂ scrubbers, airlock pump,
  hydroponics, 0.02 g orbit and spin rings, a Bekker-terramechanics rover, an imaging satellite.

### Heavy industry (008)
- ECLSS rack, Kilopower reactor with point kinetics and a regulator, uranium chemistry and a gas
  centrifuge cascade, cryogenic air separation column, electric arc furnace, deep space antenna — all
  with smooth animations of real mechanisms.

### Navigation and survey (009)
- JPL ephemerides and Lambert transfers: the cost of an interplanetary transfer follows the date;
  Δv flight map and porkchop plot; hyperspectral mineral mapping satellite with a PbS detector;
  ground-penetrating radar on the rover.

### Polish (010)
- Status screens instead of chat for Mission Control (rockets, lines, satellites), cargo terminals,
  telemetry screens, kinetic machines, spin hubs and chemical machines.
- Orbital cannon recoil and muzzle flash, capacitor glow; rocket exhaust plumes coloured by
  propellant that open up as ambient pressure falls, with shock diamonds near the ground.
- Release metadata, changelog, Modrinth page text, tag-driven release workflow.
