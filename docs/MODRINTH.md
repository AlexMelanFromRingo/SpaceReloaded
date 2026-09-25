# SpaceReloaded

**A space program on real physics.** Build rockets out of blocks and fly them with honest Δv, keep a
sealed base breathing, spin a ring station for gravity, time your Mars transfer by celestial
mechanics, and grow an industry from sand to a Kilopower reactor.

![Heavy industry](https://raw.githubusercontent.com/AlexMelanFromRingo/SpaceReloaded/main/docs/img/industry.png)

## What makes it different

- **Nothing is a magic number.** Transfer costs come from Lambert problems on JPL ephemerides,
  rockets from Tsiolkovsky and a numerical ascent, reactors from point kinetics, dishes from a link
  budget, radar echoes from t = 2d√ε/c. The core physics is plain Java with JUnit tests against
  textbook and mission values.
- **Rockets are the blocks you placed.** Mass, thrust, centre of mass, stages and propellant come
  from the structure; spent stages fall and crash; plumes open up in vacuum.
- **Bases breathe.** Zones hold masses of O₂, N₂ and CO₂; the crew breathes to NASA BVAD rates; CO₂
  kills before oxygen runs out; an ECLSS rack closes half of the oxygen loop, like on the ISS.
- **Industry is chemistry.** Silicon from sand through Siemens and Czochralski to a flight computer,
  uranium from pitchblende through UF₆ and a centrifuge cascade to a fuel basket, steel in an electric
  arc furnace.
- **Survey is honest.** A hyperspectral satellite maps exposed minerals at the 2 µm diffraction
  limit; ore under the soil takes a ground-penetrating radar on a rover.

## Requirements

Minecraft 26.2 · Fabric Loader ≥ 0.19.3 · Fabric API · Java 25. Optional: Jade.

## Documentation

- [Player guide (Russian)](https://github.com/AlexMelanFromRingo/SpaceReloaded/blob/main/docs/GUIDE.ru.md)
- [Datapack and addon guide](https://github.com/AlexMelanFromRingo/SpaceReloaded/blob/main/docs/ADDONS.md)
- [Recipes and multiblocks](https://alexmelanfromringo.github.io/SpaceReloaded/)
- [Changelog](https://github.com/AlexMelanFromRingo/SpaceReloaded/blob/main/CHANGELOG.md)

MIT licensed.
