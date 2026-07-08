package org.alex_melan.spacereloaded.core.rocketry;

import org.junit.jupiter.api.Test;
import org.alex_melan.spacereloaded.core.geometry.PackedPos;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Стыковочный узел (T071): чистая масса, не ломает расчёт характеристик. */
class ClampRoleTest {

    @Test
    void clampContributesMassOnly() {
        PartProperties clamp = new PartProperties(300, PartRole.CLAMP, 0, 0, PartProperties.NO_FUEL, 0, 0);
        PartProperties engine = PartProperties.engine(400, 60_000, 300, "kerolox");
        PartProperties tank = PartProperties.tank(200, 1000, "kerolox");
        RocketStructure structure = new RocketStructure(List.of(
                new PlacedPart(PackedPos.pack(0, 0, 0), engine, 0),
                new PlacedPart(PackedPos.pack(0, 1, 0), tank, 1000),
                new PlacedPart(PackedPos.pack(0, 2, 0), clamp, 0)));
        RocketPerformance performance = PerformanceCalculator.calculate(structure, 9.81);
        assertEquals(400 + 200 + 300 + 1000, performance.totalMassKg(), 1e-9);
        assertEquals(400 + 200 + 300, performance.dryMassKg(), 1e-9);
    }
}
